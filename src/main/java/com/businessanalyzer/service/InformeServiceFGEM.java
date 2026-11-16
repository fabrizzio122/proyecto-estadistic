package com.businessanalyzer.service;

import com.businessanalyzer.analytics.AnalisisFinancieroFGEM;
import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.informes.ContenidoInformeFGEM;
import com.businessanalyzer.informes.DatosInformeFGEM;
import com.businessanalyzer.informes.GeneradorPdfFGEM;
import com.businessanalyzer.informes.LibroExcelFGEM;
import com.businessanalyzer.informes.RedactorInformesFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.InformeGeneradoFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.TipoInformeFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.repository.InformeRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
import com.businessanalyzer.util.FechasFGEM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * Genera los informes en PDF (JasperReports) y el libro de Excel (Apache POI) de la empresa
 * activa, y lleva el historial de lo generado.
 * <p>
 * Requiere {@link PermisoFGEM#INFORMES_GENERAR}. Además, cada servicio consultado aplica su propio
 * permiso (por ejemplo, el libro de Excel incluye las ventas, así que exige poder verlas).
 */
public class InformeServiceFGEM {

    /** Informes que se muestran en el historial. */
    public static final int LIMITE_HISTORIAL_FGEM = 50;
    /** Período máximo de un informe, para que el PDF y el libro sigan siendo manejables. */
    public static final int DIAS_MAXIMOS_FGEM = 3 * 366;

    private static final Logger logFGEM = LoggerFactory.getLogger(InformeServiceFGEM.class);

    private final DatabaseFGEM databaseFGEM;
    private final InformeRepositoryFGEM informesFGEM;
    private final AnalisisServiceFGEM analisisFGEM;
    private final DiagnosticoServiceFGEM diagnosticoFGEM;
    private final OperacionServiceFGEM operacionesFGEM;
    private final FinanzasServiceFGEM finanzasFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public InformeServiceFGEM(DatabaseFGEM databaseFGEM, InformeRepositoryFGEM informesFGEM, AnalisisServiceFGEM analisisFGEM,
                          DiagnosticoServiceFGEM diagnosticoFGEM, OperacionServiceFGEM operacionesFGEM, FinanzasServiceFGEM finanzasFGEM,
                          EmpresaServiceFGEM empresasFGEM, SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.informesFGEM = informesFGEM;
        this.analisisFGEM = analisisFGEM;
        this.diagnosticoFGEM = diagnosticoFGEM;
        this.operacionesFGEM = operacionesFGEM;
        this.finanzasFGEM = finanzasFGEM;
        this.empresasFGEM = empresasFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    /**
     * Genera el informe y lo guarda en {@code destino}; si el nombre no tiene la extensión del
     * formato, se le agrega. Devuelve la ruta final del archivo.
     */
    public Path generarFGEM(TipoInformeFGEM tipoFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM, Path destinoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.INFORMES_GENERAR);
        if (tipoFGEM == null) {
            throw new ValidacionExceptionFGEM("Elige el informe que quieres generar.");
        }
        if (tipoFGEM.desdeOtroModuloFGEM()) {
            throw new ValidacionExceptionFGEM("Este informe se genera desde su propio módulo («Visitas» o «Valoración del negocio»).");
        }
        PeriodoFGEM periodoFGEM = new PeriodoFGEM(desdeFGEM, hastaFGEM);
        if (periodoFGEM.diasFGEM() > DIAS_MAXIMOS_FGEM) {
            throw new ValidacionExceptionFGEM("El período no puede superar los 3 años.");
        }
        if (hastaFGEM.isAfter(LocalDate.now(relojFGEM))) {
            throw new ValidacionExceptionFGEM("La fecha final no puede ser posterior a hoy.");
        }
        Path archivoFGEM = conExtensionFGEM(destinoFGEM, tipoFGEM);
        if (archivoFGEM.getParent() == null || !Files.isDirectory(archivoFGEM.getParent())) {
            throw new ValidacionExceptionFGEM("La carpeta de destino no existe.");
        }
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();

        AnalisisFinancieroFGEM analisisFinancieroFGEM = analisisFGEM.analizarFGEM(desdeFGEM, hastaFGEM);
        DatosInformeFGEM datosFGEM = switch (tipoFGEM) {
            case INFORME_EJECUTIVO -> new DatosInformeFGEM(empresaFGEM, analisisFinancieroFGEM, diagnosticoFGEM.diagnosticarFGEM(),
                    null, null, null);
            case LIBRO_EXCEL -> new DatosInformeFGEM(empresaFGEM, analisisFinancieroFGEM, null,
                    operacionesFGEM.listarFGEM(TipoOperacionFGEM.VENTA, desdeFGEM, hastaFGEM, true),
                    operacionesFGEM.listarFGEM(TipoOperacionFGEM.COMPRA, desdeFGEM, hastaFGEM, true),
                    finanzasFGEM.listarFGEM(desdeFGEM, hastaFGEM, true));
            default -> new DatosInformeFGEM(empresaFGEM, analisisFinancieroFGEM, null, null, null, null);
        };

        RedactorInformesFGEM redactorFGEM = new RedactorInformesFGEM(datosFGEM);
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        if (tipoFGEM == TipoInformeFGEM.LIBRO_EXCEL) {
            try (LibroExcelFGEM libroFGEM = redactorFGEM.libroExcelFGEM()) {
                libroFGEM.guardarFGEM(archivoFGEM);
            }
        } else {
            ContenidoInformeFGEM contenidoFGEM = switch (tipoFGEM) {
                case INFORME_EJECUTIVO -> redactorFGEM.informeEjecutivoFGEM();
                case ESTADO_RESULTADOS -> redactorFGEM.estadoResultadosFGEM();
                default -> redactorFGEM.rentabilidadProductosFGEM();
            };
            GeneradorPdfFGEM.generarFGEM(encabezadoFGEM(empresaFGEM, tipoFGEM, periodoFGEM, ahoraFGEM), contenidoFGEM, archivoFGEM);
        }

        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        databaseFGEM.enTransaccionFGEM(cFGEM -> {
            informesFGEM.registrarFGEM(cFGEM, empresaFGEM.idFGEM(), tipoFGEM, desdeFGEM, hastaFGEM, archivoFGEM, usuarioFGEM, ahoraFGEM);
            return null;
        });
        logFGEM.info("Informe {} generado por {} para la empresa {}: {}", tipoFGEM, usuarioFGEM, empresaFGEM.idFGEM(), archivoFGEM);
        return archivoFGEM;
    }

    public List<InformeGeneradoFGEM> historialFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.INFORMES_GENERAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> informesFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), LIMITE_HISTORIAL_FGEM));
    }

    /** Nombre de archivo sugerido, por ejemplo {@code Informe-ejecutivo_Bodega-Demo_2026-08-01_2026-10-31.pdf}. */
    public static String nombreSugeridoFGEM(TipoInformeFGEM tipoFGEM, EmpresaFGEM empresaFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM) {
        return seguroFGEM(tipoFGEM.tituloFGEM()) + "_" + seguroFGEM(empresaFGEM.nombreVisibleFGEM()) + "_" + desdeFGEM + "_" + hastaFGEM
                + "." + tipoFGEM.formatoFGEM().extensionFGEM();
    }

    static Path conExtensionFGEM(Path destinoFGEM, TipoInformeFGEM tipoFGEM) {
        if (destinoFGEM == null || destinoFGEM.getFileName() == null) {
            throw new ValidacionExceptionFGEM("Elige dónde guardar el informe.");
        }
        String nombreFGEM = destinoFGEM.getFileName().toString();
        String extensionFGEM = "." + tipoFGEM.formatoFGEM().extensionFGEM();
        return nombreFGEM.toLowerCase(Locale.ROOT).endsWith(extensionFGEM) ? destinoFGEM : destinoFGEM.resolveSibling(nombreFGEM + extensionFGEM);
    }

    private GeneradorPdfFGEM.EncabezadoFGEM encabezadoFGEM(EmpresaFGEM empresaFGEM, TipoInformeFGEM tipoFGEM, PeriodoFGEM periodoFGEM, LocalDateTime ahoraFGEM) {
        StringBuilder detalleFGEM = new StringBuilder(empresaFGEM.razonSocialFGEM());
        if (empresaFGEM.identificacionFiscalFGEM() != null && !empresaFGEM.identificacionFiscalFGEM().isBlank()) {
            detalleFGEM.append("  ·  RUC ").append(empresaFGEM.identificacionFiscalFGEM());
        }
        detalleFGEM.append("  ·  ").append(empresaFGEM.sectorFGEM().descripcionFGEM()).append("  ·  Moneda: ").append(empresaFGEM.monedaFGEM().nombreFGEM());
        String subtituloFGEM = "Período: " + periodoFGEM.descripcionFGEM()
                + "  (" + FechasFGEM.paraMostrarFGEM(periodoFGEM.desdeFGEM()) + " al "
                + FechasFGEM.paraMostrarFGEM(periodoFGEM.hastaFGEM()) + ")";
        String generadoFGEM = "Generado el " + FechasFGEM.paraMostrarFGEM(ahoraFGEM.truncatedTo(ChronoUnit.MINUTES)) + " por "
                + sesionFGEM.usuarioFGEM().nombreCompletoFGEM();
        return new GeneradorPdfFGEM.EncabezadoFGEM(empresaFGEM.nombreVisibleFGEM(), detalleFGEM.toString(), tipoFGEM.tituloFGEM(), subtituloFGEM, generadoFGEM);
    }

    private static String seguroFGEM(String textoFGEM) {
        String limpioFGEM = java.text.Normalizer.normalize(textoFGEM, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return limpioFGEM.isEmpty() ? "informe" : limpioFGEM;
    }
}
