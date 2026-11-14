package com.businessanalyzer.service;

import com.businessanalyzer.config.Database;
import com.businessanalyzer.config.Migrador;
import com.businessanalyzer.config.RestauracionPendiente;
import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CopiaSeguridad;
import com.businessanalyzer.model.CopiaSeguridad.Contenido;
import com.businessanalyzer.model.CopiaSeguridad.Tipo;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.seguridad.Sesion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Copias de seguridad de la base de datos local.
 * <ul>
 *     <li>Las copias se crean con {@code VACUUM INTO}: SQLite escribe una copia consistente y compacta
 *         aunque la aplicación esté en uso. Después se verifica que la copia se pueda abrir y esté íntegra.</li>
 *     <li>La copia automática se hace al iniciar la aplicación, como máximo una vez al día, y solo se
 *         conservan las últimas N automáticas (las manuales nunca se borran solas).</li>
 *     <li>Restaurar valida el archivo, guarda primero una copia del estado actual y deja la copia
 *         lista para aplicarse al reiniciar (ver {@link RestauracionPendiente}).</li>
 * </ul>
 * Requiere {@link Permiso#CONFIGURACION_GESTIONAR}, salvo la copia automática del inicio.
 */
public class CopiaSeguridadService {

    public static final String CLAVE_CARPETA = "copias.carpeta";
    public static final String CLAVE_AUTOMATICA = "copias.automatica";
    public static final String CLAVE_CONSERVAR = "copias.conservar";
    public static final String CLAVE_ULTIMA_AUTOMATICA = "copias.ultima_automatica";
    public static final int CONSERVAR_PREDETERMINADO = 10;
    public static final int CONSERVAR_MAXIMO = 100;

    private static final String PREFIJO = "business-analyzer_";
    private static final DateTimeFormatter FORMATO_NOMBRE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
    private static final Pattern NOMBRE = Pattern.compile(
            "business-analyzer_(\\d{4}-\\d{2}-\\d{2})_(\\d{6})_(manual|auto|previa)(-\\d+)?\\.db");
    private static final Duration INTERVALO_AUTOMATICA = Duration.ofHours(20);
    private static final byte[] CABECERA_SQLITE = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);

    private static final Logger log = LoggerFactory.getLogger(CopiaSeguridadService.class);

    private final Database database;
    private final Path carpetaPredeterminada;
    private final ConfiguracionService configuracion;
    private final Sesion sesion;
    private final Clock reloj;

    public CopiaSeguridadService(Database database, Path carpetaPredeterminada, ConfiguracionService configuracion,
                                 Sesion sesion, Clock reloj) {
        this.database = database;
        this.carpetaPredeterminada = carpetaPredeterminada.toAbsolutePath();
        this.configuracion = configuracion;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ configuración

    /** Carpeta donde se guardan las copias (la elegida por el usuario o la predeterminada). */
    public Path carpeta() {
        String elegida = configuracion.obtener(CLAVE_CARPETA, "");
        return elegida.isBlank() ? carpetaPredeterminada : Path.of(elegida);
    }

    public void cambiarCarpeta(Path nueva) {
        sesion.requerir(Permiso.CONFIGURACION_GESTIONAR);
        if (nueva == null || !Files.isDirectory(nueva)) {
            throw new ValidacionException("Elige una carpeta existente.");
        }
        if (!Files.isWritable(nueva)) {
            throw new ValidacionException("No se puede escribir en la carpeta elegida.");
        }
        configuracion.guardar(CLAVE_CARPETA, nueva.toAbsolutePath().toString());
    }

    public boolean automaticaActiva() {
        return !"false".equals(configuracion.obtener(CLAVE_AUTOMATICA, "true"));
    }

    public int copiasAConservar() {
        try {
            int valor = Integer.parseInt(configuracion.obtener(CLAVE_CONSERVAR, String.valueOf(CONSERVAR_PREDETERMINADO)));
            return Math.max(1, Math.min(CONSERVAR_MAXIMO, valor));
        } catch (NumberFormatException e) {
            return CONSERVAR_PREDETERMINADO;
        }
    }

    public void configurarAutomatica(boolean activa, int conservar) {
        sesion.requerir(Permiso.CONFIGURACION_GESTIONAR);
        if (conservar < 1 || conservar > CONSERVAR_MAXIMO) {
            throw new ValidacionException("Se pueden conservar entre 1 y " + CONSERVAR_MAXIMO + " copias automáticas.");
        }
        configuracion.guardar(CLAVE_AUTOMATICA, String.valueOf(activa));
        configuracion.guardar(CLAVE_CONSERVAR, String.valueOf(conservar));
    }

    // ------------------------------------------------------------------ creación

    public CopiaSeguridad crearManual() {
        sesion.requerir(Permiso.CONFIGURACION_GESTIONAR);
        return crear(Tipo.MANUAL);
    }

    /**
     * Copia automática del inicio: se crea si está activada y si la última tiene más de 20 horas.
     * Nunca lanza excepciones (un fallo aquí no debe impedir abrir la aplicación).
     */
    public Optional<CopiaSeguridad> crearAutomaticaSiCorresponde() {
        try {
            if (!automaticaActiva()) {
                return Optional.empty();
            }
            LocalDateTime ahora = LocalDateTime.now(reloj);
            String ultima = configuracion.obtener(CLAVE_ULTIMA_AUTOMATICA, "");
            if (!ultima.isBlank() && LocalDateTime.parse(ultima).plus(INTERVALO_AUTOMATICA).isAfter(ahora)) {
                return Optional.empty();
            }
            CopiaSeguridad copia = crear(Tipo.AUTOMATICA);
            configuracion.guardar(CLAVE_ULTIMA_AUTOMATICA, ahora.withNano(0).toString());
            aplicarRetencion();
            return Optional.of(copia);
        } catch (RuntimeException e) {
            log.error("No se pudo crear la copia de seguridad automática", e);
            return Optional.empty();
        }
    }

    private CopiaSeguridad crear(Tipo tipo) {
        Path carpeta = carpeta();
        try {
            Files.createDirectories(carpeta);
        } catch (IOException e) {
            throw new ValidacionException("No se pudo crear la carpeta de copias " + carpeta
                    + ". Elige otra carpeta en Configuración.");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj).withNano(0);
        String base = PREFIJO + ahora.format(FORMATO_NOMBRE) + "_" + tipo.sufijo();
        Path destino = carpeta.resolve(base + ".db");
        for (int n = 2; Files.exists(destino); n++) {
            destino = carpeta.resolve(base + "-" + n + ".db");
        }
        Path archivo = destino;
        database.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement("VACUUM INTO ?")) {
                ps.setString(1, archivo.toString());
                ps.execute();
            }
            return null;
        });
        try {
            leerContenido(archivo);
        } catch (RuntimeException e) {
            borrar(archivo);
            throw new DataAccessException("La copia creada no pasó la verificación de integridad.", e);
        }
        log.info("Copia de seguridad {} creada: {}", tipo, archivo);
        return describir(archivo).orElseThrow();
    }

    /** Borra las copias automáticas más antiguas que excedan la cantidad a conservar. */
    void aplicarRetencion() {
        List<CopiaSeguridad> automaticas = listarSinPermiso().stream()
                .filter(c -> c.tipo() == Tipo.AUTOMATICA)
                .toList();
        int conservar = copiasAConservar();
        for (int i = conservar; i < automaticas.size(); i++) {
            borrar(automaticas.get(i).archivo());
            log.info("Copia automática antigua eliminada: {}", automaticas.get(i).archivo());
        }
    }

    // ------------------------------------------------------------------ consulta y eliminación

    /** Copias de la carpeta, de la más reciente a la más antigua. */
    public List<CopiaSeguridad> listar() {
        sesion.requerir(Permiso.CONFIGURACION_GESTIONAR);
        return listarSinPermiso();
    }

    public Optional<CopiaSeguridad> ultima() {
        return listar().stream().findFirst();
    }

    private List<CopiaSeguridad> listarSinPermiso() {
        Path carpeta = carpeta();
        if (!Files.isDirectory(carpeta)) {
            return List.of();
        }
        List<CopiaSeguridad> copias = new ArrayList<>();
        try (Stream<Path> archivos = Files.list(carpeta)) {
            archivos.forEach(a -> describir(a).ifPresent(copias::add));
        } catch (IOException e) {
            throw new DataAccessException("No se pudo leer la carpeta de copias " + carpeta, e);
        }
        copias.sort(Comparator.comparing(CopiaSeguridad::fecha).reversed()
                .thenComparing(c -> c.archivo().getFileName().toString(), Comparator.reverseOrder()));
        return copias;
    }

    public void eliminar(CopiaSeguridad copia) {
        sesion.requerir(Permiso.CONFIGURACION_GESTIONAR);
        Path archivo = copia.archivo().toAbsolutePath();
        if (!archivo.getParent().equals(carpeta().toAbsolutePath()) || describir(archivo).isEmpty()) {
            throw new ValidacionException("Solo se pueden eliminar copias de la carpeta de copias.");
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            throw new ValidacionException("No se pudo eliminar la copia. ¿Está abierta en otro programa?");
        }
    }

    private static Optional<CopiaSeguridad> describir(Path archivo) {
        Matcher m = NOMBRE.matcher(archivo.getFileName().toString());
        if (!m.matches() || !Files.isRegularFile(archivo)) {
            return Optional.empty();
        }
        try {
            LocalDateTime fecha = LocalDate.parse(m.group(1)).atTime(
                    LocalTime.parse(m.group(2), DateTimeFormatter.ofPattern("HHmmss")));
            return Optional.of(new CopiaSeguridad(archivo.toAbsolutePath(), fecha, Tipo.desdeSufijo(m.group(3)),
                    Files.size(archivo)));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------ validación y restauración

    /**
     * Comprueba que el archivo sea una copia íntegra de Business Analyzer y de una versión que esta
     * aplicación pueda abrir. Devuelve un resumen de su contenido.
     */
    public Contenido validar(Path archivo) {
        sesion.requerir(Permiso.CONFIGURACION_GESTIONAR);
        Contenido contenido = leerContenido(archivo);
        if (contenido.versionEsquema() > Migrador.ultimaVersion()) {
            throw new ValidacionException("La copia fue creada con una versión más nueva de Business Analyzer "
                    + "(esquema " + contenido.versionEsquema() + "). Actualiza la aplicación para restaurarla.");
        }
        return contenido;
    }

    /**
     * Deja la copia lista para aplicarse al reiniciar. Antes guarda una copia del estado actual,
     * para poder volver atrás. Después de llamar a este método la aplicación debe cerrarse.
     */
    public Contenido programarRestauracion(Path origen) {
        Contenido contenido = validar(origen);
        crear(Tipo.PREVIA_RESTAURACION);
        Path pendiente = RestauracionPendiente.archivoPendiente(database.archivo());
        Path temporal = pendiente.resolveSibling(pendiente.getFileName() + ".tmp");
        try {
            Files.copy(origen, temporal, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporal, pendiente, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            borrar(temporal);
            throw new DataAccessException("No se pudo preparar la restauración.", e);
        }
        log.warn("Restauración programada desde {} por {}", origen, sesion.usuario().nombreUsuario());
        return contenido;
    }

    /** Ejecuta {@code PRAGMA integrity_check} sobre la base de datos en uso. */
    public List<String> verificarIntegridad() {
        sesion.requerir(Permiso.CONFIGURACION_GESTIONAR);
        return database.conConexion(c -> {
            List<String> resultado = new ArrayList<>();
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("PRAGMA integrity_check")) {
                while (rs.next()) {
                    resultado.add(rs.getString(1));
                }
            }
            return resultado;
        });
    }

    private static Contenido leerContenido(Path archivo) {
        if (archivo == null || !Files.isRegularFile(archivo)) {
            throw new ValidacionException("El archivo de copia no existe.");
        }
        try (InputStream entrada = Files.newInputStream(archivo)) {
            byte[] cabecera = entrada.readNBytes(CABECERA_SQLITE.length);
            if (!java.util.Arrays.equals(cabecera, CABECERA_SQLITE)) {
                throw new ValidacionException("El archivo elegido no es una base de datos de Business Analyzer.");
            }
        } catch (IOException e) {
            throw new ValidacionException("No se pudo leer el archivo de copia.");
        }
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + archivo.toAbsolutePath(), config.toProperties());
             Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("PRAGMA quick_check")) {
                if (!rs.next() || !"ok".equalsIgnoreCase(rs.getString(1))) {
                    throw new ValidacionException("La copia está dañada (falló la verificación de integridad).");
                }
            }
            int version;
            try (ResultSet rs = st.executeQuery("SELECT MAX(version) FROM schema_version")) {
                version = rs.next() ? rs.getInt(1) : 0;
            }
            if (version < 1) {
                throw new ValidacionException("El archivo no contiene datos de Business Analyzer.");
            }
            return new Contenido(version, contar(st, "empresa"), contar(st, "usuario"), contar(st, "operacion"));
        } catch (SQLException e) {
            throw new ValidacionException("El archivo elegido no es una copia válida de Business Analyzer.");
        }
    }

    private static int contar(Statement st, String tabla) {
        try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0; // la tabla no existe en copias de versiones anteriores
        }
    }

    private static void borrar(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignorado) {
            // Se intentará de nuevo en la próxima limpieza.
        }
    }
}
