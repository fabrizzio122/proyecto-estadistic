package com.businessanalyzer.service;

import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.config.MigradorFGEM;
import com.businessanalyzer.config.RestauracionPendienteFGEM;
import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CopiaSeguridadFGEM;
import com.businessanalyzer.model.CopiaSeguridadFGEM.ContenidoFGEM;
import com.businessanalyzer.model.CopiaSeguridadFGEM.TipoFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
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
 *         lista para aplicarse al reiniciar (ver {@link RestauracionPendienteFGEM}).</li>
 * </ul>
 * Requiere {@link PermisoFGEM#CONFIGURACION_GESTIONAR}, salvo la copia automática del inicio.
 */
public class CopiaSeguridadServiceFGEM {

    public static final String CLAVE_CARPETA_FGEM = "copias.carpeta";
    public static final String CLAVE_AUTOMATICA_FGEM = "copias.automatica";
    public static final String CLAVE_CONSERVAR_FGEM = "copias.conservar";
    public static final String CLAVE_ULTIMA_AUTOMATICA_FGEM = "copias.ultima_automatica";
    public static final int CONSERVAR_PREDETERMINADO_FGEM = 10;
    public static final int CONSERVAR_MAXIMO_FGEM = 100;

    private static final String PREFIJO_FGEM = "business-analyzer_";
    private static final DateTimeFormatter FORMATO_NOMBRE_FGEM = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
    private static final Pattern NOMBRE_FGEM = Pattern.compile(
            "business-analyzer_(\\d{4}-\\d{2}-\\d{2})_(\\d{6})_(manual|auto|previa|importacion)(-\\d+)?\\.(db|zip)");
    private static final Duration INTERVALO_AUTOMATICA_FGEM = Duration.ofHours(20);
    private static final byte[] CABECERA_SQLITE_FGEM = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);

    private static final Logger logFGEM = LoggerFactory.getLogger(CopiaSeguridadServiceFGEM.class);

    private final DatabaseFGEM databaseFGEM;
    private final Path carpetaPredeterminadaFGEM;
    private final ConfiguracionServiceFGEM configuracionFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public CopiaSeguridadServiceFGEM(DatabaseFGEM databaseFGEM, Path carpetaPredeterminadaFGEM, ConfiguracionServiceFGEM configuracionFGEM,
                                 SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.carpetaPredeterminadaFGEM = carpetaPredeterminadaFGEM.toAbsolutePath();
        this.configuracionFGEM = configuracionFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    // ------------------------------------------------------------------ configuración

    /** Carpeta donde se guardan las copias (la elegida por el usuario o la predeterminada). */
    public Path carpetaFGEM() {
        String elegidaFGEM = configuracionFGEM.obtenerFGEM(CLAVE_CARPETA_FGEM, "");
        return elegidaFGEM.isBlank() ? carpetaPredeterminadaFGEM : Path.of(elegidaFGEM);
    }

    public void cambiarCarpetaFGEM(Path nuevaFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.CONFIGURACION_GESTIONAR);
        if (nuevaFGEM == null || !Files.isDirectory(nuevaFGEM)) {
            throw new ValidacionExceptionFGEM("Elige una carpeta existente.");
        }
        if (!Files.isWritable(nuevaFGEM)) {
            throw new ValidacionExceptionFGEM("No se puede escribir en la carpeta elegida.");
        }
        configuracionFGEM.guardarFGEM(CLAVE_CARPETA_FGEM, nuevaFGEM.toAbsolutePath().toString());
    }

    public boolean automaticaActivaFGEM() {
        return !"false".equals(configuracionFGEM.obtenerFGEM(CLAVE_AUTOMATICA_FGEM, "true"));
    }

    public int copiasAConservarFGEM() {
        try {
            int valorFGEM = Integer.parseInt(configuracionFGEM.obtenerFGEM(CLAVE_CONSERVAR_FGEM, String.valueOf(CONSERVAR_PREDETERMINADO_FGEM)));
            return Math.max(1, Math.min(CONSERVAR_MAXIMO_FGEM, valorFGEM));
        } catch (NumberFormatException eFGEM) {
            return CONSERVAR_PREDETERMINADO_FGEM;
        }
    }

    public void configurarAutomaticaFGEM(boolean activaFGEM, int conservarFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.CONFIGURACION_GESTIONAR);
        if (conservarFGEM < 1 || conservarFGEM > CONSERVAR_MAXIMO_FGEM) {
            throw new ValidacionExceptionFGEM("Se pueden conservar entre 1 y " + CONSERVAR_MAXIMO_FGEM + " copias automáticas.");
        }
        configuracionFGEM.guardarFGEM(CLAVE_AUTOMATICA_FGEM, String.valueOf(activaFGEM));
        configuracionFGEM.guardarFGEM(CLAVE_CONSERVAR_FGEM, String.valueOf(conservarFGEM));
    }

    // ------------------------------------------------------------------ creación

    public CopiaSeguridadFGEM crearManualFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.CONFIGURACION_GESTIONAR);
        return crearFGEM(TipoFGEM.MANUAL);
    }

    /**
     * Copia previa a una importación desde Excel, para poder volver atrás si se importó el archivo
     * equivocado. Basta con poder registrar datos; como las manuales, nunca se borra sola.
     */
    public CopiaSeguridadFGEM crearAntesDeImportarFGEM() {
        if (!sesionFGEM.tienePermisoFGEM(PermisoFGEM.PRODUCTOS_GESTIONAR) && !sesionFGEM.tienePermisoFGEM(PermisoFGEM.OPERACIONES_GESTIONAR)
                && !sesionFGEM.tienePermisoFGEM(PermisoFGEM.FINANZAS_GESTIONAR)) {
            sesionFGEM.requerirFGEM(PermisoFGEM.OPERACIONES_GESTIONAR);
        }
        return crearFGEM(TipoFGEM.PREVIA_IMPORTACION);
    }

    /**
     * Copia automática del inicio: se crea si está activada y si la última tiene más de 20 horas.
     * Nunca lanza excepciones (un fallo aquí no debe impedir abrir la aplicación).
     */
    public Optional<CopiaSeguridadFGEM> crearAutomaticaSiCorrespondeFGEM() {
        try {
            if (!automaticaActivaFGEM()) {
                return Optional.empty();
            }
            LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
            String ultimaFGEM = configuracionFGEM.obtenerFGEM(CLAVE_ULTIMA_AUTOMATICA_FGEM, "");
            if (!ultimaFGEM.isBlank() && LocalDateTime.parse(ultimaFGEM).plus(INTERVALO_AUTOMATICA_FGEM).isAfter(ahoraFGEM)) {
                return Optional.empty();
            }
            // La fecha se registra ANTES de copiar para que la propia copia la incluya: así, si más tarde
            // se restaura esta copia, el inicio siguiente no crea otra automática el mismo día.
            configuracionFGEM.guardarFGEM(CLAVE_ULTIMA_AUTOMATICA_FGEM, ahoraFGEM.withNano(0).toString());
            CopiaSeguridadFGEM copiaFGEM;
            try {
                copiaFGEM = crearFGEM(TipoFGEM.AUTOMATICA);
            } catch (RuntimeException eFGEM) {
                configuracionFGEM.guardarFGEM(CLAVE_ULTIMA_AUTOMATICA_FGEM, ultimaFGEM); // se reintentará en el próximo inicio
                throw eFGEM;
            }
            aplicarRetencionFGEM();
            return Optional.of(copiaFGEM);
        } catch (RuntimeException eFGEM) {
            logFGEM.error("No se pudo crear la copia de seguridad automática", eFGEM);
            return Optional.empty();
        }
    }

    private CopiaSeguridadFGEM crearFGEM(TipoFGEM tipoFGEM) {
        Path carpetaFGEM = carpetaFGEM();
        try {
            Files.createDirectories(carpetaFGEM);
        } catch (IOException eFGEM) {
            throw new ValidacionExceptionFGEM("No se pudo crear la carpeta de copias " + carpetaFGEM
                    + ". Elige otra carpeta en Configuración.");
        }
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM).withNano(0);
        String baseFGEM = PREFIJO_FGEM + ahoraFGEM.format(FORMATO_NOMBRE_FGEM) + "_" + tipoFGEM.sufijoFGEM();
        Path destinoFGEM = carpetaFGEM.resolve(baseFGEM + ".zip");
        for (int nFGEM = 2; Files.exists(destinoFGEM) || Files.exists(destinoFGEM.resolveSibling(baseFGEM + ".db")); nFGEM++) {
            destinoFGEM = carpetaFGEM.resolve(baseFGEM + "-" + nFGEM + ".zip");
        }
        Path archivoFGEM = destinoFGEM;
        Path baseDatosFGEM = carpetaFGEM.resolve(archivoFGEM.getFileName() + ".db.tmp");
        Path temporalFGEM = carpetaFGEM.resolve(archivoFGEM.getFileName() + ".tmp");
        try {
            databaseFGEM.conConexionFGEM(cFGEM -> {
                try (PreparedStatement psFGEM = cFGEM.prepareStatement("VACUUM INTO ?")) {
                    psFGEM.setString(1, baseDatosFGEM.toString());
                    psFGEM.execute();
                }
                return null;
            });
            try {
                leerBaseDatosFGEM(baseDatosFGEM, 0);
            } catch (RuntimeException eFGEM) {
                throw new DataAccessExceptionFGEM("La copia creada no pasó la verificación de integridad.", eFGEM);
            }
            int fotosFGEM = ArchivoCopiaFGEM.crearFGEM(baseDatosFGEM, carpetaFotosFGEM(), temporalFGEM);
            Files.move(temporalFGEM, archivoFGEM, StandardCopyOption.REPLACE_EXISTING);
            logFGEM.info("Copia de seguridad {} creada con {} fotos: {}", tipoFGEM, fotosFGEM, archivoFGEM);
        } catch (IOException eFGEM) {
            borrarFGEM(archivoFGEM);
            throw new DataAccessExceptionFGEM("No se pudo escribir la copia de seguridad en " + carpetaFGEM, eFGEM);
        } finally {
            borrarFGEM(baseDatosFGEM);
            borrarFGEM(temporalFGEM);
        }
        return describirFGEM(archivoFGEM).orElseThrow();
    }

    /** Carpeta de las fotos de las visitas, junto a la base de datos. */
    private Path carpetaFotosFGEM() {
        return databaseFGEM.archivoFGEM().toAbsolutePath().resolveSibling(com.businessanalyzer.config.AppConfigFGEM.CARPETA_FOTOS_FGEM);
    }

    /** Borra las copias automáticas más antiguas que excedan la cantidad a conservar. */
    void aplicarRetencionFGEM() {
        List<CopiaSeguridadFGEM> automaticasFGEM = listarSinPermisoFGEM().stream()
                .filter(cFGEM -> cFGEM.tipoFGEM() == TipoFGEM.AUTOMATICA)
                .toList();
        int conservarFGEM = copiasAConservarFGEM();
        for (int iFGEM = conservarFGEM; iFGEM < automaticasFGEM.size(); iFGEM++) {
            borrarFGEM(automaticasFGEM.get(iFGEM).archivoFGEM());
            logFGEM.info("Copia automática antigua eliminada: {}", automaticasFGEM.get(iFGEM).archivoFGEM());
        }
    }

    // ------------------------------------------------------------------ consulta y eliminación

    /** Copias de la carpeta, de la más reciente a la más antigua. */
    public List<CopiaSeguridadFGEM> listarFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.CONFIGURACION_GESTIONAR);
        return listarSinPermisoFGEM();
    }

    public Optional<CopiaSeguridadFGEM> ultimaFGEM() {
        return listarFGEM().stream().findFirst();
    }

    private List<CopiaSeguridadFGEM> listarSinPermisoFGEM() {
        Path carpetaFGEM = carpetaFGEM();
        if (!Files.isDirectory(carpetaFGEM)) {
            return List.of();
        }
        List<CopiaSeguridadFGEM> copiasFGEM = new ArrayList<>();
        try (Stream<Path> archivosFGEM = Files.list(carpetaFGEM)) {
            archivosFGEM.forEach(aFGEM -> describirFGEM(aFGEM).ifPresent(copiasFGEM::add));
        } catch (IOException eFGEM) {
            throw new DataAccessExceptionFGEM("No se pudo leer la carpeta de copias " + carpetaFGEM, eFGEM);
        }
        copiasFGEM.sort(Comparator.comparing(CopiaSeguridadFGEM::fechaFGEM).reversed()
                .thenComparing(cFGEM -> cFGEM.archivoFGEM().getFileName().toString(), Comparator.reverseOrder()));
        return copiasFGEM;
    }

    public void eliminarFGEM(CopiaSeguridadFGEM copiaFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.CONFIGURACION_GESTIONAR);
        Path archivoFGEM = copiaFGEM.archivoFGEM().toAbsolutePath();
        if (!archivoFGEM.getParent().equals(carpetaFGEM().toAbsolutePath()) || describirFGEM(archivoFGEM).isEmpty()) {
            throw new ValidacionExceptionFGEM("Solo se pueden eliminar copias de la carpeta de copias.");
        }
        try {
            Files.deleteIfExists(archivoFGEM);
        } catch (IOException eFGEM) {
            throw new ValidacionExceptionFGEM("No se pudo eliminar la copia. ¿Está abierta en otro programa?");
        }
    }

    private static Optional<CopiaSeguridadFGEM> describirFGEM(Path archivoFGEM) {
        Matcher mFGEM = NOMBRE_FGEM.matcher(archivoFGEM.getFileName().toString());
        if (!mFGEM.matches() || !Files.isRegularFile(archivoFGEM)) {
            return Optional.empty();
        }
        try {
            LocalDateTime fechaFGEM = LocalDate.parse(mFGEM.group(1)).atTime(
                    LocalTime.parse(mFGEM.group(2), DateTimeFormatter.ofPattern("HHmmss")));
            return Optional.of(new CopiaSeguridadFGEM(archivoFGEM.toAbsolutePath(), fechaFGEM, TipoFGEM.desdeSufijoFGEM(mFGEM.group(3)),
                    Files.size(archivoFGEM)));
        } catch (IOException | RuntimeException eFGEM) {
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------ validación y restauración

    /**
     * Comprueba que el archivo sea una copia íntegra de Business Analyzer y de una versión que esta
     * aplicación pueda abrir. Devuelve un resumen de su contenido.
     */
    public ContenidoFGEM validarFGEM(Path archivoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.CONFIGURACION_GESTIONAR);
        ContenidoFGEM contenidoFGEM = leerContenidoFGEM(archivoFGEM);
        if (contenidoFGEM.versionEsquemaFGEM() > MigradorFGEM.ultimaVersionFGEM()) {
            throw new ValidacionExceptionFGEM("La copia fue creada con una versión más nueva de Business Analyzer "
                    + "(esquema " + contenidoFGEM.versionEsquemaFGEM() + "). Actualiza la aplicación para restaurarla.");
        }
        return contenidoFGEM;
    }

    /**
     * Deja la copia lista para aplicarse al reiniciar. Antes guarda una copia del estado actual,
     * para poder volver atrás. Después de llamar a este método la aplicación debe cerrarse.
     */
    public ContenidoFGEM programarRestauracionFGEM(Path origenFGEM) {
        ContenidoFGEM contenidoFGEM = validarFGEM(origenFGEM);
        crearFGEM(TipoFGEM.PREVIA_RESTAURACION);
        Path pendienteFGEM = RestauracionPendienteFGEM.archivoPendienteFGEM(databaseFGEM.archivoFGEM());
        Path fotosPendientesFGEM = RestauracionPendienteFGEM.carpetaFotosPendienteFGEM(databaseFGEM.archivoFGEM());
        Path temporalFGEM = pendienteFGEM.resolveSibling(pendienteFGEM.getFileName() + ".tmp");
        try {
            RestauracionPendienteFGEM.borrarFGEM(fotosPendientesFGEM);
            if (ArchivoCopiaFGEM.esZipFGEM(origenFGEM)) {
                // Copia nueva: base de datos y fotos. La carpeta de fotos se reemplaza al reiniciar.
                Files.createDirectories(fotosPendientesFGEM);
                ArchivoCopiaFGEM.extraerFGEM(origenFGEM, temporalFGEM, fotosPendientesFGEM);
            } else {
                // Copia antigua (solo la base de datos): las fotos actuales se conservan.
                Files.copy(origenFGEM, temporalFGEM, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(temporalFGEM, pendienteFGEM, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException eFGEM) {
            borrarFGEM(temporalFGEM);
            try {
                RestauracionPendienteFGEM.borrarFGEM(fotosPendientesFGEM);
            } catch (IOException ignoradoFGEM) {
                // Se limpiará en la próxima restauración.
            }
            throw new DataAccessExceptionFGEM("No se pudo preparar la restauración.", eFGEM);
        }
        logFGEM.warn("Restauración programada desde {} por {}", origenFGEM, sesionFGEM.usuarioFGEM().nombreUsuarioFGEM());
        return contenidoFGEM;
    }

    /** Ejecuta {@code PRAGMA integrity_check} sobre la base de datos en uso. */
    public List<String> verificarIntegridadFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.CONFIGURACION_GESTIONAR);
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            List<String> resultadoFGEM = new ArrayList<>();
            try (Statement stFGEM = cFGEM.createStatement(); ResultSet rsFGEM = stFGEM.executeQuery("PRAGMA integrity_check")) {
                while (rsFGEM.next()) {
                    resultadoFGEM.add(rsFGEM.getString(1));
                }
            }
            return resultadoFGEM;
        });
    }

    /** Lee una copia nueva (ZIP con base de datos y fotos) o antigua (solo la base de datos). */
    private static ContenidoFGEM leerContenidoFGEM(Path archivoFGEM) {
        if (archivoFGEM == null || !Files.isRegularFile(archivoFGEM)) {
            throw new ValidacionExceptionFGEM("El archivo de copia no existe.");
        }
        if (!ArchivoCopiaFGEM.esZipFGEM(archivoFGEM)) {
            return leerBaseDatosFGEM(archivoFGEM, 0);
        }
        Path temporalFGEM = null;
        try {
            temporalFGEM = Files.createTempFile("ba-validar-", ".db");
            int fotosFGEM = ArchivoCopiaFGEM.extraerFGEM(archivoFGEM, temporalFGEM, null);
            return leerBaseDatosFGEM(temporalFGEM, fotosFGEM);
        } catch (IOException eFGEM) {
            throw new ValidacionExceptionFGEM("No se pudo leer el archivo de copia (¿está dañado?).");
        } finally {
            if (temporalFGEM != null) {
                borrarFGEM(temporalFGEM);
            }
        }
    }

    private static ContenidoFGEM leerBaseDatosFGEM(Path archivoFGEM, int fotosFGEM) {
        try (InputStream entradaFGEM = Files.newInputStream(archivoFGEM)) {
            byte[] cabeceraFGEM = entradaFGEM.readNBytes(CABECERA_SQLITE_FGEM.length);
            if (!java.util.Arrays.equals(cabeceraFGEM, CABECERA_SQLITE_FGEM)) {
                throw new ValidacionExceptionFGEM("El archivo elegido no es una base de datos de Business Analyzer.");
            }
        } catch (IOException eFGEM) {
            throw new ValidacionExceptionFGEM("No se pudo leer el archivo de copia.");
        }
        SQLiteConfig configFGEM = new SQLiteConfig();
        configFGEM.setReadOnly(true);
        try (Connection cFGEM = DriverManager.getConnection("jdbc:sqlite:" + archivoFGEM.toAbsolutePath(), configFGEM.toProperties());
             Statement stFGEM = cFGEM.createStatement()) {
            try (ResultSet rsFGEM = stFGEM.executeQuery("PRAGMA quick_check")) {
                if (!rsFGEM.next() || !"ok".equalsIgnoreCase(rsFGEM.getString(1))) {
                    throw new ValidacionExceptionFGEM("La copia está dañada (falló la verificación de integridad).");
                }
            }
            int versionFGEM;
            try (ResultSet rsFGEM = stFGEM.executeQuery("SELECT MAX(version) FROM schema_version")) {
                versionFGEM = rsFGEM.next() ? rsFGEM.getInt(1) : 0;
            }
            if (versionFGEM < 1) {
                throw new ValidacionExceptionFGEM("El archivo no contiene datos de Business Analyzer.");
            }
            return new ContenidoFGEM(versionFGEM, contarFGEM(stFGEM, "empresa"), contarFGEM(stFGEM, "usuario"), contarFGEM(stFGEM, "operacion"), fotosFGEM);
        } catch (SQLException eFGEM) {
            throw new ValidacionExceptionFGEM("El archivo elegido no es una copia válida de Business Analyzer.");
        }
    }

    private static int contarFGEM(Statement stFGEM, String tablaFGEM) {
        try (ResultSet rsFGEM = stFGEM.executeQuery("SELECT COUNT(*) FROM " + tablaFGEM)) {
            return rsFGEM.next() ? rsFGEM.getInt(1) : 0;
        } catch (SQLException eFGEM) {
            return 0; // la tabla no existe en copias de versiones anteriores
        }
    }

    private static void borrarFGEM(Path archivoFGEM) {
        try {
            Files.deleteIfExists(archivoFGEM);
        } catch (IOException ignoradoFGEM) {
            // Se intentará de nuevo en la próxima limpieza.
        }
    }
}
