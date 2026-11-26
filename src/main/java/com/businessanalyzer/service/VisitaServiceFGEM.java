package com.businessanalyzer.service;

import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.AspectoInspeccionFGEM;
import com.businessanalyzer.model.CompromisoFGEM;
import com.businessanalyzer.model.DatosVisitaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EstadoCompromisoFGEM;
import com.businessanalyzer.model.FotoVisitaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.ResultadoAspectoFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.VisitaFGEM;
import com.businessanalyzer.repository.VisitaRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;

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

public class VisitaServiceFGEM {

    public static final String CARPETA_FOTOS_FGEM = com.businessanalyzer.config.AppConfigFGEM.CARPETA_FOTOS_FGEM;
    static final long TAMANO_MAXIMO_FOTO_FGEM = 15L * 1024 * 1024;
    private static final LocalDate FECHA_MINIMA_FGEM = LocalDate.of(2000, 1, 1);

    private final DatabaseFGEM databaseFGEM;
    private final VisitaRepositoryFGEM repositorioFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;
    private final Path carpetaDatosFGEM;

    public VisitaServiceFGEM(DatabaseFGEM databaseFGEM, VisitaRepositoryFGEM repositorioFGEM, EmpresaServiceFGEM empresasFGEM, SesionFGEM sesionFGEM,
                         Clock relojFGEM, Path carpetaDatosFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.repositorioFGEM = repositorioFGEM;
        this.empresasFGEM = empresasFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
        this.carpetaDatosFGEM = carpetaDatosFGEM;
    }

    public List<VisitaFGEM> listarFGEM(boolean incluirAnuladasFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_VER);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> repositorioFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), incluirAnuladasFGEM));
    }

    public VisitaFGEM buscarFGEM(long idFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_VER);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> exigirVisitaFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM()));
    }

    public VisitaFGEM registrarFGEM(DatosVisitaFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.VISITAS_REGISTRAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        DatosVisitaFGEM dFGEM = validarFGEM(datosFGEM);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            long idFGEM = repositorioFGEM.insertarFGEM(cFGEM, empresaFGEM.idFGEM(), dFGEM, usuarioFGEM, ahoraFGEM);
            for (DatosVisitaFGEM.NuevoCompromisoFGEM kFGEM : dFGEM.compromisosFGEM()) {
                repositorioFGEM.insertarCompromisoFGEM(cFGEM, empresaFGEM.idFGEM(), idFGEM, kFGEM.descripcionFGEM(), kFGEM.plazoFGEM());
            }
            return repositorioFGEM.buscarFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public VisitaFGEM actualizarFGEM(long idFGEM, DatosVisitaFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.VISITAS_REGISTRAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        DatosVisitaFGEM dFGEM = validarFGEM(datosFGEM);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            VisitaFGEM actualFGEM = exigirVisitaFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            if (actualFGEM.anuladaFGEM()) {
                throw new ValidacionExceptionFGEM("Una visita anulada no se puede modificar.");
            }
            repositorioFGEM.actualizarFGEM(cFGEM, idFGEM, dFGEM, usuarioFGEM, ahoraFGEM);
            for (DatosVisitaFGEM.NuevoCompromisoFGEM kFGEM : dFGEM.compromisosFGEM()) {
                repositorioFGEM.insertarCompromisoFGEM(cFGEM, empresaFGEM.idFGEM(), idFGEM, kFGEM.descripcionFGEM(), kFGEM.plazoFGEM());
            }
            return repositorioFGEM.buscarFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public VisitaFGEM anularFGEM(long idFGEM, String motivoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.VISITAS_REGISTRAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        String motivoLimpioFGEM = ValidacionesFGEM.requeridoFGEM(motivoFGEM, "Motivo de la anulación", 5, 200);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            VisitaFGEM actualFGEM = exigirVisitaFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            if (actualFGEM.anuladaFGEM()) {
                throw new ValidacionExceptionFGEM("La visita ya está anulada.");
            }
            repositorioFGEM.anularFGEM(cFGEM, idFGEM, motivoLimpioFGEM);
            return repositorioFGEM.buscarFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public List<CompromisoFGEM> compromisosFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_VER);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> repositorioFGEM.listarCompromisosFGEM(cFGEM, empresaFGEM.idFGEM()));
    }

    public CompromisoFGEM verificarCompromisoFGEM(long idFGEM, EstadoCompromisoFGEM estadoFGEM, LocalDate fechaFGEM, Long visitaIdFGEM,
                                          String observacionFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.VISITAS_REGISTRAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        if (estadoFGEM == null) {
            throw new ValidacionExceptionFGEM("Indica si el compromiso se cumplió.");
        }
        LocalDate diaFGEM = estadoFGEM == EstadoCompromisoFGEM.PENDIENTE ? null : fechaFGEM(fechaFGEM, "La fecha de verificación");
        String notaFGEM = ValidacionesFGEM.opcionalFGEM(observacionFGEM, "Observación", 200);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            CompromisoFGEM actualFGEM = repositorioFGEM.buscarCompromisoFGEM(cFGEM, idFGEM).filter(kFGEM -> kFGEM.empresaIdFGEM() == empresaFGEM.idFGEM())
                    .orElseThrow(() -> new ValidacionExceptionFGEM("El compromiso no existe en la empresa activa."));
            if (diaFGEM != null && diaFGEM.isBefore(actualFGEM.fechaVisitaFGEM())) {
                throw new ValidacionExceptionFGEM("La verificación no puede ser anterior a la visita en que se acordó.");
            }
            Long visitaFGEM = null;
            if (visitaIdFGEM != null && estadoFGEM != EstadoCompromisoFGEM.PENDIENTE) {
                VisitaFGEM vFGEM = exigirVisitaFGEM(cFGEM, visitaIdFGEM, empresaFGEM.idFGEM());
                if (vFGEM.idFGEM() == actualFGEM.visitaIdFGEM()) {
                    throw new ValidacionExceptionFGEM("El compromiso se verifica en una visita posterior a la que lo acordó.");
                }
                visitaFGEM = vFGEM.idFGEM();
            }
            repositorioFGEM.verificarCompromisoFGEM(cFGEM, idFGEM, estadoFGEM, diaFGEM, visitaFGEM, notaFGEM, usuarioFGEM);
            return repositorioFGEM.buscarCompromisoFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public List<FotoVisitaFGEM> fotosFGEM(long visitaIdFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_VER);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            exigirVisitaFGEM(cFGEM, visitaIdFGEM, empresaFGEM.idFGEM());
            return repositorioFGEM.listarFotosFGEM(cFGEM, visitaIdFGEM);
        });
    }

    public Path archivoFGEM(FotoVisitaFGEM fotoFGEM) {
        return carpetaDatosFGEM.resolve(fotoFGEM.archivoFGEM());
    }

    public FotoVisitaFGEM agregarFotoFGEM(long visitaIdFGEM, Path origenFGEM, String descripcionFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.VISITAS_REGISTRAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        String notaFGEM = ValidacionesFGEM.opcionalFGEM(descripcionFGEM, "Descripción de la foto", 150);
        String extensionFGEM = extensionFGEM(origenFGEM);
        try {
            if (Files.size(origenFGEM) > TAMANO_MAXIMO_FOTO_FGEM) {
                throw new ValidacionExceptionFGEM("La foto pesa más de 15 MB. Redúcela antes de agregarla.");
            }
            if (ImageIO.read(origenFGEM.toFile()) == null) {
                throw new ValidacionExceptionFGEM("El archivo no es una imagen válida.");
            }
        } catch (IOException eFGEM) {
            throw new ValidacionExceptionFGEM("No se pudo leer la foto: " + eFGEM.getMessage());
        }
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        String relativoFGEM = CARPETA_FOTOS_FGEM + "/" + visitaIdFGEM + "/" + UUID.randomUUID() + "." + extensionFGEM;
        Path destinoFGEM = carpetaDatosFGEM.resolve(relativoFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            VisitaFGEM visitaFGEM = exigirVisitaFGEM(cFGEM, visitaIdFGEM, empresaFGEM.idFGEM());
            if (visitaFGEM.anuladaFGEM()) {
                throw new ValidacionExceptionFGEM("La visita está anulada.");
            }
            try {
                Files.createDirectories(destinoFGEM.getParent());
                Files.copy(origenFGEM, destinoFGEM);
            } catch (IOException eFGEM) {
                throw new DataAccessExceptionFGEM("No se pudo copiar la foto a la carpeta de datos", eFGEM);
            }
            long idFGEM = repositorioFGEM.insertarFotoFGEM(cFGEM, visitaIdFGEM, relativoFGEM, notaFGEM, ahoraFGEM);
            return repositorioFGEM.buscarFotoFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public void eliminarFotoFGEM(long fotoIdFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.VISITAS_REGISTRAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        FotoVisitaFGEM fotoFGEM = databaseFGEM.enTransaccionFGEM(cFGEM -> {
            FotoVisitaFGEM fFGEM = repositorioFGEM.buscarFotoFGEM(cFGEM, fotoIdFGEM)
                    .orElseThrow(() -> new ValidacionExceptionFGEM("La foto ya no existe."));
            exigirVisitaFGEM(cFGEM, fFGEM.visitaIdFGEM(), empresaFGEM.idFGEM());
            repositorioFGEM.eliminarFotoFGEM(cFGEM, fotoIdFGEM);
            return fFGEM;
        });
        try {
            Files.deleteIfExists(archivoFGEM(fotoFGEM));
        } catch (IOException eFGEM) {
        }
    }

    private DatosVisitaFGEM validarFGEM(DatosVisitaFGEM dFGEM) {
        if (dFGEM == null) {
            throw new ValidacionExceptionFGEM("Faltan los datos de la visita.");
        }
        LocalDate fechaFGEM = fechaFGEM(dFGEM.fechaFGEM(), "La fecha de la visita");
        if (dFGEM.tipoFGEM() == null) {
            throw new ValidacionExceptionFGEM("Indica el tipo de visita.");
        }
        String inspectorFGEM = ValidacionesFGEM.requeridoFGEM(dFGEM.inspectorFGEM(), "Inspector", 3, 100);
        String lugarFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.lugarFGEM(), "Lugar", 150);
        if ((dFGEM.latitudFGEM() == null) != (dFGEM.longitudFGEM() == null)) {
            throw new ValidacionExceptionFGEM("Indica la latitud y la longitud, o deja las dos vacías.");
        }
        if (dFGEM.latitudFGEM() != null && (Math.abs(dFGEM.latitudFGEM()) > 90 || Math.abs(dFGEM.longitudFGEM()) > 180)) {
            throw new ValidacionExceptionFGEM("Las coordenadas no son válidas (latitud −90 a 90, longitud −180 a 180).");
        }
        String atendidoFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.atendidoPorFGEM(), "Atendido por", 100);
        String observacionesFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.observacionesFGEM(), "Observaciones", 2000);
        if (dFGEM.conclusionFGEM() == null) {
            throw new ValidacionExceptionFGEM("Indica la conclusión de la visita.");
        }
        if (dFGEM.proximaVisitaFGEM() != null && !dFGEM.proximaVisitaFGEM().isAfter(fechaFGEM)) {
            throw new ValidacionExceptionFGEM("La próxima visita debe ser posterior a esta.");
        }
        Map<AspectoInspeccionFGEM, ResultadoAspectoFGEM> aspectosFGEM = new EnumMap<>(AspectoInspeccionFGEM.class);
        Map<AspectoInspeccionFGEM, String> notasFGEM = new EnumMap<>(AspectoInspeccionFGEM.class);
        if (dFGEM.aspectosFGEM() != null) {
            dFGEM.aspectosFGEM().forEach((aFGEM, rFGEM) -> {
                if (aFGEM != null && rFGEM != null) {
                    aspectosFGEM.put(aFGEM, rFGEM);
                }
            });
        }
        if (dFGEM.notasFGEM() != null) {
            dFGEM.notasFGEM().forEach((aFGEM, nFGEM) -> {
                String limpiaFGEM = ValidacionesFGEM.opcionalFGEM(nFGEM, "Observación de «" + aFGEM.descripcionFGEM() + "»", 300);
                if (limpiaFGEM != null && aspectosFGEM.containsKey(aFGEM)) {
                    notasFGEM.put(aFGEM, limpiaFGEM);
                }
            });
        }
        List<DatosVisitaFGEM.NuevoCompromisoFGEM> compromisosFGEM = new ArrayList<>();
        if (dFGEM.compromisosFGEM() != null) {
            for (DatosVisitaFGEM.NuevoCompromisoFGEM kFGEM : dFGEM.compromisosFGEM()) {
                String textoFGEM = ValidacionesFGEM.requeridoFGEM(kFGEM.descripcionFGEM(), "Compromiso", 5, 200);
                if (kFGEM.plazoFGEM() != null && kFGEM.plazoFGEM().isBefore(fechaFGEM)) {
                    throw new ValidacionExceptionFGEM("El plazo del compromiso no puede ser anterior a la visita.");
                }
                compromisosFGEM.add(new DatosVisitaFGEM.NuevoCompromisoFGEM(textoFGEM, kFGEM.plazoFGEM()));
            }
        }
        return new DatosVisitaFGEM(fechaFGEM, dFGEM.tipoFGEM(), inspectorFGEM, lugarFGEM, dFGEM.latitudFGEM(), dFGEM.longitudFGEM(), atendidoFGEM, observacionesFGEM,
                dFGEM.conclusionFGEM(), dFGEM.proximaVisitaFGEM(), aspectosFGEM, notasFGEM, compromisosFGEM);
    }

    private LocalDate fechaFGEM(LocalDate fechaFGEM, String campoFGEM) {
        if (fechaFGEM == null) {
            throw new ValidacionExceptionFGEM(campoFGEM + " es obligatoria.");
        }
        if (fechaFGEM.isBefore(FECHA_MINIMA_FGEM) || fechaFGEM.isAfter(LocalDate.now(relojFGEM))) {
            throw new ValidacionExceptionFGEM(campoFGEM + " debe estar entre el año 2000 y hoy.");
        }
        return fechaFGEM;
    }

    private static String extensionFGEM(Path archivoFGEM) {
        String nombreFGEM = archivoFGEM == null || archivoFGEM.getFileName() == null ? "" : archivoFGEM.getFileName().toString();
        int puntoFGEM = nombreFGEM.lastIndexOf('.');
        String extFGEM = puntoFGEM < 0 ? "" : nombreFGEM.substring(puntoFGEM + 1).toLowerCase(Locale.ROOT);
        if (!List.of("jpg", "jpeg", "png").contains(extFGEM)) {
            throw new ValidacionExceptionFGEM("Solo se aceptan fotos JPG o PNG.");
        }
        return extFGEM;
    }

    private VisitaFGEM exigirVisitaFGEM(Connection cFGEM, long idFGEM, long empresaIdFGEM) throws SQLException {
        return repositorioFGEM.buscarFGEM(cFGEM, idFGEM).filter(vFGEM -> vFGEM.empresaIdFGEM() == empresaIdFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM("La visita no existe en la empresa activa."));
    }

    private EmpresaFGEM empresaAgropecuariaFGEM() {
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        if (empresaFGEM.sectorFGEM() != SectorFGEM.AGROPECUARIO) {
            throw new ValidacionExceptionFGEM("Las visitas de inspección son para empresas del sector agropecuario.");
        }
        return empresaFGEM;
    }
}
