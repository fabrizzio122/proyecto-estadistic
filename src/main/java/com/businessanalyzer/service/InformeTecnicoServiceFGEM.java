package com.businessanalyzer.service;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.agricultura.ResultadoCampaniaFGEM;
import com.businessanalyzer.analytics.ganaderia.ExistenciaValorizadaFGEM;
import com.businessanalyzer.analytics.ganaderia.IndicadorGanaderoFGEM;
import com.businessanalyzer.analytics.ganaderia.IndicadoresEspecieFGEM;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneralFGEM;
import com.businessanalyzer.analytics.patrimonio.IndicadoresPatrimonioFGEM;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonialFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.informes.ContenidoInformeFGEM;
import com.businessanalyzer.informes.GeneradorPdfFGEM;
import com.businessanalyzer.model.AspectoInspeccionFGEM;
import com.businessanalyzer.model.CompromisoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EstadoCampaniaFGEM;
import com.businessanalyzer.model.EstadoCompromisoFGEM;
import com.businessanalyzer.model.FotoVisitaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.ResultadoAspectoFGEM;
import com.businessanalyzer.model.TipoInformeFGEM;
import com.businessanalyzer.model.VisitaFGEM;
import com.businessanalyzer.repository.InformeRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class InformeTecnicoServiceFGEM {

    static final int MAXIMO_FOTOS_FGEM = 6;

    private final DatabaseFGEM databaseFGEM;
    private final InformeRepositoryFGEM informesFGEM;
    private final VisitaServiceFGEM visitasFGEM;
    private final GanaderiaServiceFGEM ganaderiaFGEM;
    private final AgriculturaServiceFGEM agriculturaFGEM;
    private final PatrimonioServiceFGEM patrimonioFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public InformeTecnicoServiceFGEM(DatabaseFGEM databaseFGEM, InformeRepositoryFGEM informesFGEM, VisitaServiceFGEM visitasFGEM,
                                 GanaderiaServiceFGEM ganaderiaFGEM, AgriculturaServiceFGEM agriculturaFGEM, PatrimonioServiceFGEM patrimonioFGEM,
                                 EmpresaServiceFGEM empresasFGEM, SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.informesFGEM = informesFGEM;
        this.visitasFGEM = visitasFGEM;
        this.ganaderiaFGEM = ganaderiaFGEM;
        this.agriculturaFGEM = agriculturaFGEM;
        this.patrimonioFGEM = patrimonioFGEM;
        this.empresasFGEM = empresasFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    public static String nombreSugeridoFGEM(EmpresaFGEM empresaFGEM, VisitaFGEM visitaFGEM) {
        String limpioFGEM = java.text.Normalizer.normalize(empresaFGEM.nombreVisibleFGEM(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return "Informe-tecnico-" + (limpioFGEM.isEmpty() ? "productor" : limpioFGEM) + "-" + visitaFGEM.fechaFGEM() + ".pdf";
    }

    public Path generarFGEM(long visitaIdFGEM, Path destinoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.INFORMES_GENERAR);
        if (destinoFGEM == null || destinoFGEM.getFileName() == null) {
            throw new ValidacionExceptionFGEM("Elige dónde guardar el informe.");
        }
        Path archivoFGEM = destinoFGEM.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")
                ? destinoFGEM : destinoFGEM.resolveSibling(destinoFGEM.getFileName() + ".pdf");
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        VisitaFGEM visitaFGEM = visitasFGEM.buscarFGEM(visitaIdFGEM);
        if (visitaFGEM.anuladaFGEM()) {
            throw new ValidacionExceptionFGEM("La visita está anulada.");
        }
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        ContenidoInformeFGEM contenidoFGEM = redactarFGEM(empresaFGEM, visitaFGEM);
        GeneradorPdfFGEM.generarFGEM(encabezadoFGEM(empresaFGEM, visitaFGEM, ahoraFGEM), contenidoFGEM, archivoFGEM);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        databaseFGEM.enTransaccionFGEM(cFGEM -> {
            informesFGEM.registrarFGEM(cFGEM, empresaFGEM.idFGEM(), TipoInformeFGEM.INFORME_TECNICO, visitaFGEM.fechaFGEM(), visitaFGEM.fechaFGEM(), archivoFGEM,
                    usuarioFGEM, ahoraFGEM);
            return null;
        });
        return archivoFGEM;
    }

    public ContenidoInformeFGEM redactarFGEM(EmpresaFGEM empresaFGEM, VisitaFGEM visitaFGEM) {
        MonedaFGEM monedaFGEM = empresaFGEM.monedaFGEM();
        PeriodoFGEM periodoFGEM = new PeriodoFGEM(visitaFGEM.fechaFGEM().minusYears(1).plusDays(1), visitaFGEM.fechaFGEM());
        List<ExistenciaValorizadaFGEM> hatoFGEM = ganaderiaFGEM.hatoFGEM();
        List<IndicadoresEspecieFGEM> especiesFGEM = ganaderiaFGEM.indicadoresFGEM(periodoFGEM);
        List<ResultadoCampaniaFGEM> campaniasFGEM = agriculturaFGEM.listarCampaniasFGEM(false).stream()
                .filter(rFGEM -> !rFGEM.campaniaFGEM().fechaSiembraFGEM().isAfter(visitaFGEM.fechaFGEM()))
                .filter(rFGEM -> rFGEM.campaniaFGEM().estadoFGEM() == EstadoCampaniaFGEM.EN_CURSO || rFGEM.campaniaFGEM().fechaCierreFGEM() == null
                        || !rFGEM.campaniaFGEM().fechaCierreFGEM().isBefore(periodoFGEM.desdeFGEM()))
                .toList();
        SituacionPatrimonialFGEM situacionFGEM = patrimonioFGEM.situacionFGEM();
        List<CompromisoFGEM> compromisosFGEM = visitasFGEM.compromisosFGEM();
        List<FotoVisitaFGEM> fotosFGEM = visitasFGEM.fotosFGEM(visitaFGEM.idFGEM());

        ContenidoInformeFGEM cFGEM = new ContenidoInformeFGEM();

        int cabezasFGEM = hatoFGEM.stream().mapToInt(ExistenciaValorizadaFGEM::cabezasFGEM).sum();
        BigDecimal areaFGEM = campaniasFGEM.stream().filter(rFGEM -> rFGEM.campaniaFGEM().estadoFGEM() == EstadoCampaniaFGEM.EN_CURSO)
                .map(rFGEM -> rFGEM.campaniaFGEM().areaFGEM()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cumplimientoFGEM = visitaFGEM.cumplimientoFGEM();
        cFGEM.indicadoresFGEM("Cumplimiento en campo", cumplimientoFGEM == null ? "—" : MontosFGEM.porcentajeFGEM(cumplimientoFGEM),
                "Conclusión", visitaFGEM.conclusionFGEM().descripcionFGEM(),
                "Animales", String.valueOf(cabezasFGEM),
                "Área sembrada", MontosFGEM.cantidadFGEM(areaFGEM) + " ha");
        cFGEM.seccionFGEM("1. Datos de la visita");
        cFGEM.datoFGEM("Productor", empresaFGEM.razonSocialFGEM());
        if (empresaFGEM.identificacionFiscalFGEM() != null && !empresaFGEM.identificacionFiscalFGEM().isBlank()) {
            cFGEM.datoFGEM("RUC / DNI", empresaFGEM.identificacionFiscalFGEM());
        }
        if (empresaFGEM.direccionFGEM() != null && !empresaFGEM.direccionFGEM().isBlank()) {
            cFGEM.datoFGEM("Dirección", empresaFGEM.direccionFGEM());
        }
        cFGEM.datoFGEM("Fecha y tipo de visita", FechasFGEM.paraMostrarFGEM(visitaFGEM.fechaFGEM()) + " · " + visitaFGEM.tipoFGEM().descripcionFGEM());
        cFGEM.datoFGEM("Inspector", visitaFGEM.inspectorFGEM());
        cFGEM.datoFGEM("Atendido por", Objects.requireNonNullElse(visitaFGEM.atendidoPorFGEM(), "—"));
        cFGEM.datoFGEM("Lugar", Objects.requireNonNullElse(visitaFGEM.lugarFGEM(), "—"));
        if (visitaFGEM.coordenadasFGEM() != null) {
            cFGEM.datoFGEM("Coordenadas (lat, long)", visitaFGEM.coordenadasFGEM());
        }
        if (visitaFGEM.proximaVisitaFGEM() != null) {
            cFGEM.datoFGEM("Próxima visita", FechasFGEM.paraMostrarFGEM(visitaFGEM.proximaVisitaFGEM()));
        }

        cFGEM.seccionFGEM("2. Verificación en campo");
        if (visitaFGEM.aspectosFGEM().isEmpty()) {
            cFGEM.textoFGEM("No se registró la verificación de aspectos en esta visita.");
        }
        for (AspectoInspeccionFGEM.AreaFGEM areaInspeccionFGEM : AspectoInspeccionFGEM.AreaFGEM.values()) {
            List<AspectoInspeccionFGEM> evaluadosFGEM = java.util.Arrays.stream(AspectoInspeccionFGEM.values())
                    .filter(aFGEM -> aFGEM.areaFGEM() == areaInspeccionFGEM && visitaFGEM.aspectosFGEM().containsKey(aFGEM)).toList();
            if (evaluadosFGEM.isEmpty()) {
                continue;
            }
            cFGEM.encabezadoFGEM(areaInspeccionFGEM.descripcionFGEM(), "Resultado");
            for (AspectoInspeccionFGEM aFGEM : evaluadosFGEM) {
                ResultadoAspectoFGEM rFGEM = visitaFGEM.aspectosFGEM().get(aFGEM);
                cFGEM.semaforoFGEM(aFGEM.descripcionFGEM(), rFGEM.semaforoFGEM(), rFGEM.descripcionFGEM(),
                        Objects.requireNonNullElse(visitaFGEM.notasFGEM().get(aFGEM), aFGEM.guiaFGEM()));
            }
        }
        if (cumplimientoFGEM != null) {
            cFGEM.notaFGEM("Cumplimiento: «cumple» vale 1, «cumple en parte» 0.5 y «no cumple» 0; no se cuentan los aspectos que no aplican.");
        }

        List<CompromisoFGEM> verificadosFGEM = compromisosFGEM.stream()
                .filter(kFGEM -> Objects.equals(kFGEM.visitaVerificacionIdFGEM(), visitaFGEM.idFGEM())).toList();
        List<CompromisoFGEM> pendientesPreviosFGEM = compromisosFGEM.stream()
                .filter(kFGEM -> kFGEM.visitaIdFGEM() != visitaFGEM.idFGEM() && kFGEM.estadoFGEM() == EstadoCompromisoFGEM.PENDIENTE
                        && kFGEM.fechaVisitaFGEM().isBefore(visitaFGEM.fechaFGEM())).toList();
        List<CompromisoFGEM> nuevosFGEM = compromisosFGEM.stream().filter(kFGEM -> kFGEM.visitaIdFGEM() == visitaFGEM.idFGEM()).toList();
        cFGEM.seccionFGEM("3. Compromisos");
        if (verificadosFGEM.isEmpty() && pendientesPreviosFGEM.isEmpty() && nuevosFGEM.isEmpty()) {
            cFGEM.textoFGEM("No hay compromisos registrados.");
        }
        if (!verificadosFGEM.isEmpty() || !pendientesPreviosFGEM.isEmpty()) {
            cFGEM.encabezadoFGEM("De visitas anteriores", "Acordado", "Plazo", "Estado");
            verificadosFGEM.forEach(kFGEM -> cFGEM.filaFGEM(kFGEM.descripcionFGEM(), FechasFGEM.paraMostrarFGEM(kFGEM.fechaVisitaFGEM()),
                    FechasFGEM.paraMostrarFGEM(kFGEM.plazoFGEM()), kFGEM.estadoFGEM().descripcionFGEM()));
            pendientesPreviosFGEM.forEach(kFGEM -> cFGEM.filaFGEM(kFGEM.descripcionFGEM(), FechasFGEM.paraMostrarFGEM(kFGEM.fechaVisitaFGEM()),
                    FechasFGEM.paraMostrarFGEM(kFGEM.plazoFGEM()), kFGEM.vencidoFGEM(visitaFGEM.fechaFGEM()) ? "Pendiente (vencido)" : "Pendiente"));
        }
        if (!nuevosFGEM.isEmpty()) {
            cFGEM.encabezadoFGEM("Acordados en esta visita", "Plazo", "Estado");
            nuevosFGEM.forEach(kFGEM -> cFGEM.filaFGEM(kFGEM.descripcionFGEM(), FechasFGEM.paraMostrarFGEM(kFGEM.plazoFGEM()), kFGEM.estadoFGEM().descripcionFGEM()));
        }

        if (!especiesFGEM.isEmpty() || !hatoFGEM.isEmpty()) {
            cFGEM.seccionFGEM("4. Ganadería (" + periodoFGEM.descripcionFGEM() + ")");
            cFGEM.indicadoresFGEM("Animales hoy", String.valueOf(cabezasFGEM),
                    "Valor del hato", MontosFGEM.formatearFGEM(ExistenciaValorizadaFGEM.totalFGEM(hatoFGEM), monedaFGEM));
            for (IndicadoresEspecieFGEM eFGEM : especiesFGEM) {
                cFGEM.encabezadoFGEM(eFGEM.especieFGEM().descripcionFGEM() + ": de " + eFGEM.existenciaInicialFGEM() + " a " + eFGEM.existenciaFinalFGEM()
                        + " animales", "Valor", "Estado");
                for (IndicadorGanaderoFGEM iFGEM : eFGEM.evaluarFGEM(monedaFGEM)) {
                    if (iFGEM.semaforoFGEM().evaluadoFGEM()) {
                        cFGEM.semaforoFGEM(iFGEM.nombreFGEM(), iFGEM.semaforoFGEM(), iFGEM.valorFGEM(), iFGEM.lecturaFGEM());
                    } else {
                        cFGEM.datoFGEM(iFGEM.nombreFGEM(), iFGEM.valorFGEM() + "  ·  " + iFGEM.lecturaFGEM());
                    }
                }
            }
        }

        if (!campaniasFGEM.isEmpty()) {
            cFGEM.seccionFGEM("5. Agricultura");
            cFGEM.encabezadoFGEM("Campaña", "Área (ha)", "Rendimiento", "% referencia", "Margen");
            for (ResultadoCampaniaFGEM rFGEM : campaniasFGEM) {
                BigDecimal rendFGEM = rFGEM.rendimientoFGEM();
                BigDecimal avanceFGEM = rFGEM.avanceReferenciaFGEM();
                boolean conResultadoFGEM = rFGEM.produccionFGEM().signum() > 0 || rFGEM.campaniaFGEM().estadoFGEM().cerradaFGEM();
                cFGEM.filaFGEM(rFGEM.campaniaFGEM().cultivoFGEM().descripcionFGEM() + " · " + rFGEM.campaniaFGEM().parcelaNombreFGEM() + " ("
                                + rFGEM.campaniaFGEM().estadoFGEM().descripcionFGEM().toLowerCase() + ")",
                        MontosFGEM.cantidadFGEM(rFGEM.campaniaFGEM().areaFGEM()),
                        rendFGEM == null ? "—" : MontosFGEM.cantidadFGEM(rendFGEM.setScale(0, RoundingMode.HALF_UP)) + " kg/ha",
                        MontosFGEM.porcentajeFGEM(avanceFGEM),
                        conResultadoFGEM ? MontosFGEM.formatearFGEM(rFGEM.margenFGEM(), monedaFGEM) : "En curso");
            }
            campaniasFGEM.stream().filter(rFGEM -> rFGEM.campaniaFGEM().estadoFGEM() == EstadoCampaniaFGEM.PERDIDA).forEach(rFGEM ->
                    cFGEM.notaFGEM(rFGEM.campaniaFGEM().cultivoFGEM().descripcionFGEM() + " perdido: " + rFGEM.campaniaFGEM().motivoFGEM() + "."));
        }

        BalanceGeneralFGEM bFGEM = situacionFGEM.balanceFGEM();
        IndicadoresPatrimonioFGEM ipFGEM = situacionFGEM.indicadoresFGEM();
        if (!bFGEM.sinRegistrosFGEM()) {
            cFGEM.seccionFGEM("6. Situación patrimonial (al " + FechasFGEM.paraMostrarFGEM(bFGEM.corteFGEM()) + ")");
            cFGEM.filaFGEM("Total activos", MontosFGEM.formatearFGEM(bFGEM.totalActivoFGEM(), monedaFGEM));
            cFGEM.filaFGEM("Total deudas", MontosFGEM.formatearFGEM(bFGEM.totalPasivoFGEM(), monedaFGEM));
            cFGEM.totalFGEM("Patrimonio", MontosFGEM.formatearFGEM(bFGEM.patrimonioFGEM(), monedaFGEM));
            cFGEM.filaFGEM("Liquidez corriente", ipFGEM.liquidezCorrienteFGEM() == null ? "Sin deudas de corto plazo"
                    : IndicadoresPatrimonioFGEM.vecesFGEM(ipFGEM.liquidezCorrienteFGEM()));
            cFGEM.filaFGEM("Endeudamiento (deudas ÷ activos)", MontosFGEM.porcentajeFGEM(ipFGEM.endeudamientoFGEM()));
            if (bFGEM.cuotasMensualesFGEM().signum() > 0) {
                cFGEM.filaFGEM("Cuotas mensuales", MontosFGEM.formatearFGEM(bFGEM.cuotasMensualesFGEM(), monedaFGEM));
            }
        }

        cFGEM.seccionFGEM("7. Observaciones y conclusión");
        cFGEM.textoFGEM(visitaFGEM.observacionesFGEM() == null ? "Sin observaciones adicionales." : visitaFGEM.observacionesFGEM());
        cFGEM.recomendacionFGEM(visitaFGEM.conclusionFGEM().descripcionFGEM(), visitaFGEM.conclusionFGEM().semaforoFGEM(), "Conclusión del inspector",
                conclusionTextoFGEM(visitaFGEM));

        int incluidasFGEM = 0;
        for (FotoVisitaFGEM fFGEM : fotosFGEM) {
            if (incluidasFGEM == MAXIMO_FOTOS_FGEM) {
                break;
            }
            BufferedImage imagenFGEM = leerFGEM(visitasFGEM.archivoFGEM(fFGEM));
            if (imagenFGEM == null) {
                continue;
            }
            if (incluidasFGEM == 0) {
                cFGEM.seccionFGEM("8. Registro fotográfico");
            }
            cFGEM.graficoFGEM(imagenFGEM);
            cFGEM.notaFGEM("Foto " + (incluidasFGEM + 1) + (fFGEM.descripcionFGEM() == null ? "" : ": " + fFGEM.descripcionFGEM()));
            incluidasFGEM++;
        }

        cFGEM.espacioFGEM();
        cFGEM.firmasFGEM("Inspector\n" + visitaFGEM.inspectorFGEM(),
                "Productor\n" + Objects.requireNonNullElse(visitaFGEM.atendidoPorFGEM(), empresaFGEM.razonSocialFGEM()));
        return cFGEM;
    }

    private static String conclusionTextoFGEM(VisitaFGEM visitaFGEM) {
        BigDecimal cumplimientoFGEM = visitaFGEM.cumplimientoFGEM();
        long noCumpleFGEM = visitaFGEM.aspectosFGEM().values().stream().filter(rFGEM -> rFGEM == ResultadoAspectoFGEM.NO_CUMPLE).count();
        String baseFGEM = cumplimientoFGEM == null ? "No se evaluaron aspectos en campo."
                : "El productor cumple el " + MontosFGEM.porcentajeFGEM(cumplimientoFGEM) + " de los aspectos verificados"
                + (noCumpleFGEM == 0 ? "." : ", con " + noCumpleFGEM + (noCumpleFGEM == 1 ? " aspecto que no cumple." : " aspectos que no cumplen."));
        return baseFGEM + (visitaFGEM.proximaVisitaFGEM() == null ? "" : " Próxima visita: " + FechasFGEM.paraMostrarFGEM(visitaFGEM.proximaVisitaFGEM()) + ".");
    }

    private static BufferedImage leerFGEM(Path archivoFGEM) {
        try {
            return Files.exists(archivoFGEM) ? ImageIO.read(archivoFGEM.toFile()) : null;
        } catch (IOException eFGEM) {
            return null;
        }
    }

    private GeneradorPdfFGEM.EncabezadoFGEM encabezadoFGEM(EmpresaFGEM empresaFGEM, VisitaFGEM visitaFGEM, LocalDateTime ahoraFGEM) {
        StringBuilder detalleFGEM = new StringBuilder(empresaFGEM.razonSocialFGEM());
        if (empresaFGEM.identificacionFiscalFGEM() != null && !empresaFGEM.identificacionFiscalFGEM().isBlank()) {
            detalleFGEM.append("  ·  ").append(empresaFGEM.identificacionFiscalFGEM());
        }
        detalleFGEM.append("  ·  ").append(empresaFGEM.sectorFGEM().descripcionFGEM());
        String subtituloFGEM = visitaFGEM.tipoFGEM().descripcionFGEM() + " del " + FechasFGEM.paraMostrarFGEM(visitaFGEM.fechaFGEM())
                + "  ·  Inspector: " + visitaFGEM.inspectorFGEM();
        String generadoFGEM = "Generado el " + FechasFGEM.paraMostrarFGEM(ahoraFGEM.truncatedTo(ChronoUnit.MINUTES)) + " por "
                + sesionFGEM.usuarioFGEM().nombreCompletoFGEM();
        return new GeneradorPdfFGEM.EncabezadoFGEM(empresaFGEM.nombreVisibleFGEM(), detalleFGEM.toString(),
                TipoInformeFGEM.INFORME_TECNICO.tituloFGEM(), subtituloFGEM, generadoFGEM);
    }
}
