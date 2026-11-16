package com.businessanalyzer.service;

import com.businessanalyzer.analytics.EstadoResultadosFGEM;
import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.estadistica.TendenciaFGEM;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneralFGEM;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonialFGEM;
import com.businessanalyzer.analytics.valoracion.SupuestosValoracionFGEM;
import com.businessanalyzer.analytics.valoracion.ValoracionNegocioFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.informes.ContenidoInformeFGEM;
import com.businessanalyzer.informes.GeneradorPdfFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.TipoInformeFGEM;
import com.businessanalyzer.repository.ActivoRepositoryFGEM;
import com.businessanalyzer.repository.DeudaRepositoryFGEM;
import com.businessanalyzer.repository.InformeRepositoryFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.businessanalyzer.repository.ProductoRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;

/**
 * Valoración del negocio de la empresa activa con los datos de hoy: balance general (incluye ganado
 * y cultivos de los productores) y utilidad de los últimos 12 meses completos. Requiere
 * {@link PermisoFGEM#ANALISIS_VER}; exportar el informe, {@link PermisoFGEM#INFORMES_GENERAR}.
 * <p>
 * Los supuestos (tasa de descuento y crecimiento) se guardan por empresa. Si no se eligió un
 * crecimiento, se propone el de la tendencia de las ventas del último año, acotado entre −10 % y 10 %.
 */
public class ValoracionServiceFGEM {

    private static final String CLAVE_FGEM = "valoracion.";
    static final BigDecimal CRECIMIENTO_MINIMO_FGEM = new BigDecimal("-0.10");
    static final BigDecimal CRECIMIENTO_MAXIMO_FGEM = new BigDecimal("0.10");

    /** Valoración con los datos que la sustentan. */
    public record ResultadoFGEM(ValoracionNegocioFGEM valoracionFGEM, SituacionPatrimonialFGEM situacionFGEM, BigDecimal crecimientoSugeridoFGEM,
                            PeriodoFGEM periodoResultadosFGEM) {
    }

    private final DatabaseFGEM databaseFGEM;
    private final OperacionRepositoryFGEM operacionesFGEM;
    private final MovimientoRepositoryFGEM movimientosFGEM;
    private final ConsultaPatrimonioFGEM patrimonioFGEM;
    private final InformeRepositoryFGEM informesFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final ConfiguracionServiceFGEM configuracionFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public ValoracionServiceFGEM(DatabaseFGEM databaseFGEM, OperacionRepositoryFGEM operacionesFGEM, MovimientoRepositoryFGEM movimientosFGEM,
                             InformeRepositoryFGEM informesFGEM, EmpresaServiceFGEM empresasFGEM, ConfiguracionServiceFGEM configuracionFGEM,
                             SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.operacionesFGEM = operacionesFGEM;
        this.movimientosFGEM = movimientosFGEM;
        this.patrimonioFGEM = new ConsultaPatrimonioFGEM(new ActivoRepositoryFGEM(), new DeudaRepositoryFGEM(), new ProductoRepositoryFGEM(),
                operacionesFGEM, movimientosFGEM);
        this.informesFGEM = informesFGEM;
        this.empresasFGEM = empresasFGEM;
        this.configuracionFGEM = configuracionFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    /** Supuestos guardados para la empresa activa, o los propuestos si aún no se guardaron. */
    public SupuestosValoracionFGEM supuestosFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.ANALISIS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        BigDecimal tasaFGEM = leerFGEM(empresaFGEM, "tasa", SupuestosValoracionFGEM.TASA_PREDETERMINADA_FGEM);
        BigDecimal crecimientoFGEM = leerFGEM(empresaFGEM, "crecimiento", null);
        if (crecimientoFGEM == null) {
            crecimientoFGEM = databaseFGEM.conConexionFGEM(cFGEM -> crecimientoSugeridoFGEM(cFGEM, empresaFGEM.idFGEM()));
        }
        try {
            return SupuestosValoracionFGEM.conFGEM(tasaFGEM, crecimientoFGEM);
        } catch (ValidacionExceptionFGEM eFGEM) {
            return SupuestosValoracionFGEM.conFGEM(SupuestosValoracionFGEM.TASA_PREDETERMINADA_FGEM, BigDecimal.ZERO);
        }
    }

    /** Guarda los supuestos de la empresa activa ({@code null} en el crecimiento vuelve al sugerido). */
    public void guardarSupuestosFGEM(BigDecimal tasaFGEM, BigDecimal crecimientoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.ANALISIS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        SupuestosValoracionFGEM.conFGEM(tasaFGEM, crecimientoFGEM == null ? BigDecimal.ZERO : crecimientoFGEM); // valida
        configuracionFGEM.guardarFGEM(CLAVE_FGEM + empresaFGEM.idFGEM() + ".tasa", tasaFGEM.toPlainString());
        configuracionFGEM.guardarFGEM(CLAVE_FGEM + empresaFGEM.idFGEM() + ".crecimiento", crecimientoFGEM == null ? "" : crecimientoFGEM.toPlainString());
    }

    public ResultadoFGEM valorarFGEM(SupuestosValoracionFGEM supuestosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.ANALISIS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        LocalDate hoyFGEM = LocalDate.now(relojFGEM);
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            SituacionPatrimonialFGEM sFGEM = patrimonioFGEM.situacionFGEM(cFGEM, empresaFGEM.idFGEM(), hoyFGEM);
            BalanceGeneralFGEM bFGEM = sFGEM.balanceFGEM();
            ValoracionNegocioFGEM vFGEM = ValoracionNegocioFGEM.calcularFGEM(supuestosFGEM, bFGEM.patrimonioFGEM(), sFGEM.indicadoresFGEM().utilidadAnualFGEM(),
                    bFGEM.depreciacionMensualFGEM(), sFGEM.indicadoresFGEM().mesesResultadosFGEM());
            return new ResultadoFGEM(vFGEM, sFGEM, crecimientoSugeridoFGEM(cFGEM, empresaFGEM.idFGEM()), sFGEM.periodoResultadosFGEM());
        });
    }

    /**
     * Crecimiento anual de la tendencia de las ventas de los últimos 12 meses completos (desde el primer
     * mes con ventas). Sin tendencia clara o con menos de 6 meses, 0 %.
     */
    BigDecimal crecimientoSugeridoFGEM(Connection cFGEM, long empresaIdFGEM) throws SQLException {
        YearMonth ultimoFGEM = YearMonth.from(LocalDate.now(relojFGEM)).minusMonths(1);
        double[] ventasFGEM = ConsultaMensualFGEM.porMesFGEM(cFGEM, operacionesFGEM, movimientosFGEM, empresaIdFGEM, ultimoFGEM.minusMonths(11), ultimoFGEM)
                .stream().mapToDouble(mFGEM -> EstadoResultadosFGEM.deFGEM(mFGEM.operacionesFGEM(), mFGEM.finanzasFGEM()).ventasNetasFGEM().doubleValue())
                .toArray();
        int inicioFGEM = 0;
        while (inicioFGEM < ventasFGEM.length && ventasFGEM[inicioFGEM] == 0) {
            inicioFGEM++;
        }
        double[] serieFGEM = Arrays.copyOfRange(ventasFGEM, inicioFGEM, ventasFGEM.length);
        if (serieFGEM.length < 6) {
            return BigDecimal.ZERO;
        }
        TendenciaFGEM tFGEM = TendenciaFGEM.deFGEM(serieFGEM);
        if (tFGEM.direccionFGEM() != TendenciaFGEM.DireccionFGEM.CRECIENTE && tFGEM.direccionFGEM() != TendenciaFGEM.DireccionFGEM.DECRECIENTE) {
            return BigDecimal.ZERO;
        }
        double anualFGEM = Math.pow(1 + tFGEM.crecimientoMensualFGEM(), 12) - 1;
        return BigDecimal.valueOf(anualFGEM).setScale(4, RoundingMode.HALF_UP).max(CRECIMIENTO_MINIMO_FGEM).min(CRECIMIENTO_MAXIMO_FGEM);
    }

    // ------------------------------------------------------------------ informe

    /** Informe PDF de la valoración con los supuestos indicados; queda en el historial de informes. */
    public Path exportarFGEM(SupuestosValoracionFGEM supuestosFGEM, Path destinoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.INFORMES_GENERAR);
        if (destinoFGEM == null || destinoFGEM.getFileName() == null) {
            throw new ValidacionExceptionFGEM("Elige dónde guardar el informe.");
        }
        Path archivoFGEM = destinoFGEM.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")
                ? destinoFGEM : destinoFGEM.resolveSibling(destinoFGEM.getFileName() + ".pdf");
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        ResultadoFGEM rFGEM = valorarFGEM(supuestosFGEM);
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        LocalDate hoyFGEM = ahoraFGEM.toLocalDate();
        GeneradorPdfFGEM.generarFGEM(new GeneradorPdfFGEM.EncabezadoFGEM(empresaFGEM.nombreVisibleFGEM(), empresaFGEM.razonSocialFGEM() + "  ·  "
                        + empresaFGEM.sectorFGEM().descripcionFGEM(), TipoInformeFGEM.VALORACION.tituloFGEM(),
                        "Valoración al " + FechasFGEM.paraMostrarFGEM(hoyFGEM),
                        "Generado el " + FechasFGEM.paraMostrarFGEM(ahoraFGEM.truncatedTo(ChronoUnit.MINUTES)) + " por "
                                + sesionFGEM.usuarioFGEM().nombreCompletoFGEM()),
                contenidoFGEM(rFGEM, empresaFGEM.monedaFGEM()), archivoFGEM);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        databaseFGEM.enTransaccionFGEM(cFGEM -> {
            informesFGEM.registrarFGEM(cFGEM, empresaFGEM.idFGEM(), TipoInformeFGEM.VALORACION, hoyFGEM, hoyFGEM, archivoFGEM, usuarioFGEM, ahoraFGEM);
            return null;
        });
        return archivoFGEM;
    }

    /** Contenido del informe (público para las pruebas). */
    public static ContenidoInformeFGEM contenidoFGEM(ResultadoFGEM rFGEM, MonedaFGEM monedaFGEM) {
        ValoracionNegocioFGEM vFGEM = rFGEM.valoracionFGEM();
        SupuestosValoracionFGEM sFGEM = vFGEM.supuestosFGEM();
        BalanceGeneralFGEM bFGEM = rFGEM.situacionFGEM().balanceFGEM();
        ContenidoInformeFGEM cFGEM = new ContenidoInformeFGEM();
        cFGEM.indicadoresFGEM("Valor de referencia", MontosFGEM.formatearFGEM(vFGEM.valorReferenciaFGEM(), monedaFGEM),
                "Valor patrimonial", MontosFGEM.formatearFGEM(vFGEM.patrimonialFGEM(), monedaFGEM),
                "Por rentabilidad", vFGEM.valorRentabilidadFGEM() == null ? "—" : MontosFGEM.formatearFGEM(vFGEM.valorRentabilidadFGEM(), monedaFGEM),
                "Fondo de comercio", vFGEM.fondoComercioFGEM() == null ? "—" : MontosFGEM.formatearFGEM(vFGEM.fondoComercioFGEM(), monedaFGEM));
        cFGEM.seccionFGEM("1. Resumen");
        vFGEM.lecturaFGEM(monedaFGEM).forEach(cFGEM::textoFGEM);

        cFGEM.seccionFGEM("2. Valor patrimonial");
        cFGEM.filaFGEM("Total activos", MontosFGEM.formatearFGEM(bFGEM.totalActivoFGEM(), monedaFGEM));
        cFGEM.filaFGEM("(−) Total deudas", MontosFGEM.formatearFGEM(bFGEM.totalPasivoFGEM(), monedaFGEM));
        cFGEM.totalFGEM("= Valor patrimonial", MontosFGEM.formatearFGEM(vFGEM.patrimonialFGEM(), monedaFGEM));
        cFGEM.notaFGEM("Activos fijos a su valor actual (descontada la depreciación); ganado a precio de mercado; cultivos en "
                + "proceso al costo; inventario al costo.");

        cFGEM.seccionFGEM("3. Valor por rentabilidad");
        cFGEM.datoFGEM("Utilidad neta anual", MontosFGEM.formatearFGEM(vFGEM.utilidadAnualFGEM(), monedaFGEM)
                + (rFGEM.periodoResultadosFGEM() == null ? " (sin ventas en los últimos 12 meses)"
                : "  ·  " + rFGEM.periodoResultadosFGEM().descripcionFGEM() + ", llevada a 12 meses"));
        cFGEM.datoFGEM("Depreciación anual", MontosFGEM.formatearFGEM(vFGEM.depreciacionAnualFGEM(), monedaFGEM) + "  ·  se suma porque no es salida de dinero");
        cFGEM.datoFGEM("Supuestos", "Tasa de descuento " + MontosFGEM.porcentajeFGEM(sFGEM.tasaDescuentoFGEM()) + "  ·  crecimiento "
                + MontosFGEM.porcentajeFGEM(sFGEM.crecimientoFGEM()) + " anual por " + sFGEM.aniosFGEM() + " años  ·  después "
                + MontosFGEM.porcentajeFGEM(sFGEM.crecimientoLargoPlazoFGEM()));
        cFGEM.filaFGEM("Capitalización de utilidades (utilidad ÷ tasa)",
                vFGEM.capitalizacionFGEM() == null ? "—" : MontosFGEM.formatearFGEM(vFGEM.capitalizacionFGEM(), monedaFGEM));
        cFGEM.filaFGEM("Flujos descontados", vFGEM.flujosDescontadosFGEM() == null ? "—" : MontosFGEM.formatearFGEM(vFGEM.flujosDescontadosFGEM(), monedaFGEM));
        if (!vFGEM.proyeccionFGEM().isEmpty()) {
            cFGEM.encabezadoFGEM("Proyección", "Flujo", "Factor", "Valor presente");
            vFGEM.proyeccionFGEM().forEach(pFGEM -> cFGEM.filaFGEM("Año " + pFGEM.anioFGEM(), MontosFGEM.formatearFGEM(pFGEM.flujoFGEM(), monedaFGEM),
                    pFGEM.factorFGEM().toPlainString(), MontosFGEM.formatearFGEM(pFGEM.valorPresenteFGEM(), monedaFGEM)));
            cFGEM.filaFGEM("Valor terminal", MontosFGEM.formatearFGEM(vFGEM.valorTerminalFGEM(), monedaFGEM), "",
                    MontosFGEM.formatearFGEM(vFGEM.valorTerminalPresenteFGEM(), monedaFGEM));
            cFGEM.totalFGEM("Total", "", "", MontosFGEM.formatearFGEM(vFGEM.flujosDescontadosFGEM(), monedaFGEM));

            ValoracionNegocioFGEM.SensibilidadFGEM sensFGEM = vFGEM.sensibilidadFGEM();
            cFGEM.seccionFGEM("4. Sensibilidad (flujos descontados)");
            String[] encabezadoFGEM = sensFGEM.crecimientosFGEM().stream().map(gFGEM -> "Crec. " + MontosFGEM.porcentajeFGEM(gFGEM)).toArray(String[]::new);
            cFGEM.encabezadoFGEM("Tasa de descuento", encabezadoFGEM);
            for (int iFGEM = 0; iFGEM < sensFGEM.tasasFGEM().size(); iFGEM++) {
                String[] filaFGEM = new String[sensFGEM.crecimientosFGEM().size()];
                for (int jFGEM = 0; jFGEM < filaFGEM.length; jFGEM++) {
                    filaFGEM[jFGEM] = sensFGEM.valoresFGEM()[iFGEM][jFGEM] == null ? "—" : MontosFGEM.formatearFGEM(sensFGEM.valoresFGEM()[iFGEM][jFGEM], monedaFGEM);
                }
                cFGEM.filaFGEM(MontosFGEM.porcentajeFGEM(sensFGEM.tasasFGEM().get(iFGEM)), filaFGEM);
            }
        }
        cFGEM.notaFGEM("La valoración es una estimación con la información registrada en el sistema; no reemplaza una tasación "
                + "profesional. Una tasa de descuento mayor refleja más riesgo y da un valor menor.");
        return cFGEM;
    }

    // ------------------------------------------------------------------ utilidades

    private BigDecimal leerFGEM(EmpresaFGEM empresaFGEM, String campoFGEM, BigDecimal porDefectoFGEM) {
        String textoFGEM = configuracionFGEM.obtenerFGEM(CLAVE_FGEM + empresaFGEM.idFGEM() + "." + campoFGEM, "");
        if (textoFGEM.isBlank()) {
            return porDefectoFGEM;
        }
        try {
            return new BigDecimal(textoFGEM);
        } catch (NumberFormatException eFGEM) {
            return porDefectoFGEM;
        }
    }
}
