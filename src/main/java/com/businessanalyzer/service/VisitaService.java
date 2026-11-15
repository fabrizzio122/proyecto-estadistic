package com.businessanalyzer.service;

import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.AspectoInspeccion;
import com.businessanalyzer.model.Compromiso;
import com.businessanalyzer.model.DatosVisita;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.EstadoCompromiso;
import com.businessanalyzer.model.FotoVisita;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.ResultadoAspecto;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.Visita;
import com.businessanalyzer.repository.VisitaRepository;
import com.businessanalyzer.seguridad.Sesion;
import com.businessanalyzer.util.Validaciones;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Visitas de inspección a productores agropecuarios: verificación en campo, compromisos y fotos.
 * <p>
 * Ver visitas y compromisos requiere {@link Permiso#AGRO_VER}; registrarlas, {@link Permiso#VISITAS_REGISTRAR}
 * (administrador, gerente y analista, que es el rol del inspector). Las fotos se copian a la carpeta
 * de datos, en {@code fotos-visitas/<visita>/}.
 */
public class VisitaService {

    /** Carpeta de las fotos dentro de la carpeta de datos. */
    public static final String CARPETA_FOTOS = "fotos-visitas";
    static final long TAMANO_MAXIMO_FOTO = 15L * 1024 * 1024;
    private static final LocalDate FECHA_MINIMA = LocalDate.of(2000, 1, 1);

    private final Database database;
    private final VisitaRepository repositorio;
    private final EmpresaService empresas;
    private final Sesion sesion;
    private final Clock reloj;
    private final Path carpetaDatos;

    public VisitaService(Database database, VisitaRepository repositorio, EmpresaService empresas, Sesion sesion,
                         Clock reloj, Path carpetaDatos) {
        this.database = database;
        this.repositorio = repositorio;
        this.empresas = empresas;
        this.sesion = sesion;
        this.reloj = reloj;
        this.carpetaDatos = carpetaDatos;
    }

    // ------------------------------------------------------------------ visitas

    public List<Visita> listar(boolean incluirAnuladas) {
        sesion.requerir(Permiso.AGRO_VER);
        Empresa empresa = empresaAgropecuaria();
        return database.conConexion(c -> repositorio.listar(c, empresa.id(), incluirAnuladas));
    }

    public Visita buscar(long id) {
        sesion.requerir(Permiso.AGRO_VER);
        Empresa empresa = empresaAgropecuaria();
        return database.conConexion(c -> exigirVisita(c, id, empresa.id()));
    }

    /** Registra la visita con su verificación en campo y los compromisos acordados. */
    public Visita registrar(DatosVisita datos) {
        sesion.requerir(Permiso.VISITAS_REGISTRAR);
        Empresa empresa = empresaAgropecuaria();
        DatosVisita d = validar(datos);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            long id = repositorio.insertar(c, empresa.id(), d, usuario, ahora);
            for (DatosVisita.NuevoCompromiso k : d.compromisos()) {
                repositorio.insertarCompromiso(c, empresa.id(), id, k.descripcion(), k.plazo());
            }
            return repositorio.buscar(c, id).orElseThrow();
        });
    }

    /** Corrige la visita. Los compromisos nuevos se agregan a los que ya tenía. */
    public Visita actualizar(long id, DatosVisita datos) {
        sesion.requerir(Permiso.VISITAS_REGISTRAR);
        Empresa empresa = empresaAgropecuaria();
        DatosVisita d = validar(datos);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            Visita actual = exigirVisita(c, id, empresa.id());
            if (actual.anulada()) {
                throw new ValidacionException("Una visita anulada no se puede modificar.");
            }
            repositorio.actualizar(c, id, d, usuario, ahora);
            for (DatosVisita.NuevoCompromiso k : d.compromisos()) {
                repositorio.insertarCompromiso(c, empresa.id(), id, k.descripcion(), k.plazo());
            }
            return repositorio.buscar(c, id).orElseThrow();
        });
    }

    public Visita anular(long id, String motivo) {
        sesion.requerir(Permiso.VISITAS_REGISTRAR);
        Empresa empresa = empresaAgropecuaria();
        String motivoLimpio = Validaciones.requerido(motivo, "Motivo de la anulación", 5, 200);
        return database.enTransaccion(c -> {
            Visita actual = exigirVisita(c, id, empresa.id());
            if (actual.anulada()) {
                throw new ValidacionException("La visita ya está anulada.");
            }
            repositorio.anular(c, id, motivoLimpio);
            return repositorio.buscar(c, id).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ compromisos

    /** Compromisos de las visitas vigentes, de la visita más reciente a la más antigua. */
    public List<Compromiso> compromisos() {
        sesion.requerir(Permiso.AGRO_VER);
        Empresa empresa = empresaAgropecuaria();
        return database.conConexion(c -> repositorio.listarCompromisos(c, empresa.id()));
    }

    /**
     * Registra si el compromiso se cumplió. {@link EstadoCompromiso#PENDIENTE} lo vuelve a dejar pendiente.
     *
     * @param visitaId visita en que se verificó (opcional)
     */
    public Compromiso verificarCompromiso(long id, EstadoCompromiso estado, LocalDate fecha, Long visitaId,
                                          String observacion) {
        sesion.requerir(Permiso.VISITAS_REGISTRAR);
        Empresa empresa = empresaAgropecuaria();
        if (estado == null) {
            throw new ValidacionException("Indica si el compromiso se cumplió.");
        }
        LocalDate dia = estado == EstadoCompromiso.PENDIENTE ? null : fecha(fecha, "La fecha de verificación");
        String nota = Validaciones.opcional(observacion, "Observación", 200);
        String usuario = sesion.usuario().nombreUsuario();
        return database.enTransaccion(c -> {
            Compromiso actual = repositorio.buscarCompromiso(c, id).filter(k -> k.empresaId() == empresa.id())
                    .orElseThrow(() -> new ValidacionException("El compromiso no existe en la empresa activa."));
            if (dia != null && dia.isBefore(actual.fechaVisita())) {
                throw new ValidacionException("La verificación no puede ser anterior a la visita en que se acordó.");
            }
            Long visita = null;
            if (visitaId != null && estado != EstadoCompromiso.PENDIENTE) {
                Visita v = exigirVisita(c, visitaId, empresa.id());
                if (v.id() == actual.visitaId()) {
                    throw new ValidacionException("El compromiso se verifica en una visita posterior a la que lo acordó.");
                }
                visita = v.id();
            }
            repositorio.verificarCompromiso(c, id, estado, dia, visita, nota, usuario);
            return repositorio.buscarCompromiso(c, id).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ fotos

    public List<FotoVisita> fotos(long visitaId) {
        sesion.requerir(Permiso.AGRO_VER);
        Empresa empresa = empresaAgropecuaria();
        return database.conConexion(c -> {
            exigirVisita(c, visitaId, empresa.id());
            return repositorio.listarFotos(c, visitaId);
        });
    }

    /** Ruta del archivo de la foto en la carpeta de datos. */
    public Path archivo(FotoVisita foto) {
        return carpetaDatos.resolve(foto.archivo());
    }

    /** Copia la imagen (JPG o PNG, hasta 15 MB) a la carpeta de datos y la asocia a la visita. */
    public FotoVisita agregarFoto(long visitaId, Path origen, String descripcion) {
        sesion.requerir(Permiso.VISITAS_REGISTRAR);
        Empresa empresa = empresaAgropecuaria();
        String nota = Validaciones.opcional(descripcion, "Descripción de la foto", 150);
        String extension = extension(origen);
        try {
            if (Files.size(origen) > TAMANO_MAXIMO_FOTO) {
                throw new ValidacionException("La foto pesa más de 15 MB. Redúcela antes de agregarla.");
            }
            if (ImageIO.read(origen.toFile()) == null) {
                throw new ValidacionException("El archivo no es una imagen válida.");
            }
        } catch (IOException e) {
            throw new ValidacionException("No se pudo leer la foto: " + e.getMessage());
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String relativo = CARPETA_FOTOS + "/" + visitaId + "/" + UUID.randomUUID() + "." + extension;
        Path destino = carpetaDatos.resolve(relativo);
        return database.enTransaccion(c -> {
            Visita visita = exigirVisita(c, visitaId, empresa.id());
            if (visita.anulada()) {
                throw new ValidacionException("La visita está anulada.");
            }
            try {
                Files.createDirectories(destino.getParent());
                Files.copy(origen, destino);
            } catch (IOException e) {
                throw new DataAccessException("No se pudo copiar la foto a la carpeta de datos", e);
            }
            long id = repositorio.insertarFoto(c, visitaId, relativo, nota, ahora);
            return repositorio.buscarFoto(c, id).orElseThrow();
        });
    }

    public void eliminarFoto(long fotoId) {
        sesion.requerir(Permiso.VISITAS_REGISTRAR);
        Empresa empresa = empresaAgropecuaria();
        FotoVisita foto = database.enTransaccion(c -> {
            FotoVisita f = repositorio.buscarFoto(c, fotoId)
                    .orElseThrow(() -> new ValidacionException("La foto ya no existe."));
            exigirVisita(c, f.visitaId(), empresa.id());
            repositorio.eliminarFoto(c, fotoId);
            return f;
        });
        try {
            Files.deleteIfExists(archivo(foto));
        } catch (IOException e) {
            // El registro ya se eliminó; el archivo huérfano no afecta al sistema.
        }
    }

    // ------------------------------------------------------------------ validación

    private DatosVisita validar(DatosVisita d) {
        if (d == null) {
            throw new ValidacionException("Faltan los datos de la visita.");
        }
        LocalDate fecha = fecha(d.fecha(), "La fecha de la visita");
        if (d.tipo() == null) {
            throw new ValidacionException("Indica el tipo de visita.");
        }
        String inspector = Validaciones.requerido(d.inspector(), "Inspector", 3, 100);
        String lugar = Validaciones.opcional(d.lugar(), "Lugar", 150);
        if ((d.latitud() == null) != (d.longitud() == null)) {
            throw new ValidacionException("Indica la latitud y la longitud, o deja las dos vacías.");
        }
        if (d.latitud() != null && (Math.abs(d.latitud()) > 90 || Math.abs(d.longitud()) > 180)) {
            throw new ValidacionException("Las coordenadas no son válidas (latitud −90 a 90, longitud −180 a 180).");
        }
        String atendido = Validaciones.opcional(d.atendidoPor(), "Atendido por", 100);
        String observaciones = Validaciones.opcional(d.observaciones(), "Observaciones", 2000);
        if (d.conclusion() == null) {
            throw new ValidacionException("Indica la conclusión de la visita.");
        }
        if (d.proximaVisita() != null && !d.proximaVisita().isAfter(fecha)) {
            throw new ValidacionException("La próxima visita debe ser posterior a esta.");
        }
        Map<AspectoInspeccion, ResultadoAspecto> aspectos = new EnumMap<>(AspectoInspeccion.class);
        Map<AspectoInspeccion, String> notas = new EnumMap<>(AspectoInspeccion.class);
        if (d.aspectos() != null) {
            d.aspectos().forEach((a, r) -> {
                if (a != null && r != null) {
                    aspectos.put(a, r);
                }
            });
        }
        if (d.notas() != null) {
            d.notas().forEach((a, n) -> {
                String limpia = Validaciones.opcional(n, "Observación de «" + a.descripcion() + "»", 300);
                if (limpia != null && aspectos.containsKey(a)) {
                    notas.put(a, limpia);
                }
            });
        }
        List<DatosVisita.NuevoCompromiso> compromisos = new ArrayList<>();
        if (d.compromisos() != null) {
            for (DatosVisita.NuevoCompromiso k : d.compromisos()) {
                String texto = Validaciones.requerido(k.descripcion(), "Compromiso", 5, 200);
                if (k.plazo() != null && k.plazo().isBefore(fecha)) {
                    throw new ValidacionException("El plazo del compromiso no puede ser anterior a la visita.");
                }
                compromisos.add(new DatosVisita.NuevoCompromiso(texto, k.plazo()));
            }
        }
        return new DatosVisita(fecha, d.tipo(), inspector, lugar, d.latitud(), d.longitud(), atendido, observaciones,
                d.conclusion(), d.proximaVisita(), aspectos, notas, compromisos);
    }

    private LocalDate fecha(LocalDate fecha, String campo) {
        if (fecha == null) {
            throw new ValidacionException(campo + " es obligatoria.");
        }
        if (fecha.isBefore(FECHA_MINIMA) || fecha.isAfter(LocalDate.now(reloj))) {
            throw new ValidacionException(campo + " debe estar entre el año 2000 y hoy.");
        }
        return fecha;
    }

    private static String extension(Path archivo) {
        String nombre = archivo == null || archivo.getFileName() == null ? "" : archivo.getFileName().toString();
        int punto = nombre.lastIndexOf('.');
        String ext = punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
        if (!List.of("jpg", "jpeg", "png").contains(ext)) {
            throw new ValidacionException("Solo se aceptan fotos JPG o PNG.");
        }
        return ext;
    }

    private Visita exigirVisita(Connection c, long id, long empresaId) throws SQLException {
        return repositorio.buscar(c, id).filter(v -> v.empresaId() == empresaId)
                .orElseThrow(() -> new ValidacionException("La visita no existe en la empresa activa."));
    }

    private Empresa empresaAgropecuaria() {
        Empresa empresa = empresas.empresaDeTrabajo();
        if (empresa.sector() != Sector.AGROPECUARIO) {
            throw new ValidacionException("Las visitas de inspección son para empresas del sector agropecuario.");
        }
        return empresa;
    }
}
