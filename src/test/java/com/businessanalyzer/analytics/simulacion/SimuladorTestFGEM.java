package com.businessanalyzer.analytics.simulacion;

import com.businessanalyzer.analytics.PeriodoFGEM;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base mensual: ventas 10 000, costo de ventas 6 000, gastos variables 1 000, costos fijos 2 000,
 * impuesto 30 % -> UAI 1 000, utilidad neta 700, punto de equilibrio 6 666.67.
 */
class SimuladorTestFGEM {

    private static final BigDecimal TASA_FGEM = new BigDecimal("0.30");
    private static final BaseSimulacionFGEM BASE_FGEM = new BaseSimulacionFGEM(PeriodoFGEM.mesFGEM(YearMonth.of(2026, 1)), 1,
            bdFGEM("10000"), bdFGEM("6000"), bdFGEM("1000"), bdFGEM("2000"), BigDecimal.ZERO, bdFGEM("300"));

    @Test
    void sinCambiosReproduceLaBaseFGEM() {
        ResultadoEscenarioFGEM rFGEM = SimuladorFGEM.simularFGEM(BASE_FGEM, SupuestosFGEM.neutrosFGEM(TASA_FGEM));

        assertMontoFGEM("1000", rFGEM.utilidadAntesImpuestosFGEM());
        assertMontoFGEM("300", rFGEM.impuestoFGEM());
        assertMontoFGEM("700", rFGEM.utilidadNetaFGEM());
        assertMontoFGEM("3000", rFGEM.margenContribucionFGEM());
        assertEquals(bdFGEM("6666.67"), rFGEM.equilibrioFGEM().ventasEquilibrioFGEM());
        assertEquals(bdFGEM("0.300000"), BASE_FGEM.tasaImpuestoEfectivaFGEM());
    }

    @Test
    void subirPreciosAumentaVentasSinCambiarCostosFGEM() {
        ResultadoEscenarioFGEM rFGEM = SimuladorFGEM.simularFGEM(BASE_FGEM, SupuestosFGEM.neutrosFGEM(TASA_FGEM).conFGEM(VariableFGEM.PRECIO, bdFGEM("0.10")));

        assertMontoFGEM("11000", rFGEM.ventasFGEM());
        assertMontoFGEM("6000", rFGEM.costoVentasFGEM());
        assertMontoFGEM("1400", rFGEM.utilidadNetaFGEM());
    }

    @Test
    void venderMasMueveTambienLosCostosVariablesFGEM() {
        ResultadoEscenarioFGEM rFGEM = SimuladorFGEM.simularFGEM(BASE_FGEM, SupuestosFGEM.neutrosFGEM(TASA_FGEM).conFGEM(VariableFGEM.VOLUMEN, bdFGEM("0.10")));

        assertMontoFGEM("11000", rFGEM.ventasFGEM());
        assertMontoFGEM("6600", rFGEM.costoVentasFGEM());
        assertMontoFGEM("1100", rFGEM.gastosVariablesFGEM());
        assertMontoFGEM("2000", rFGEM.costosFijosFGEM());
        assertMontoFGEM("910", rFGEM.utilidadNetaFGEM());
    }

    @Test
    void costosFijosConAhorroYCostoAdicionalFGEM() {
        SupuestosFGEM sFGEM = SupuestosFGEM.neutrosFGEM(TASA_FGEM).conFGEM(VariableFGEM.COSTOS_FIJOS, bdFGEM("-0.10")).conCostoFijoAdicionalFGEM(bdFGEM("500"));
        ResultadoEscenarioFGEM rFGEM = SimuladorFGEM.simularFGEM(BASE_FGEM, sFGEM);

        assertMontoFGEM("2300", rFGEM.costosFijosFGEM());
        assertMontoFGEM("490", rFGEM.utilidadNetaFGEM());
    }

    @Test
    void conPerdidaNoSePagaImpuestoFGEM() {
        ResultadoEscenarioFGEM rFGEM = SimuladorFGEM.simularFGEM(BASE_FGEM, SupuestosFGEM.neutrosFGEM(TASA_FGEM).conFGEM(VariableFGEM.COSTOS_FIJOS, BigDecimal.ONE));

        assertMontoFGEM("-1000", rFGEM.utilidadAntesImpuestosFGEM());
        assertMontoFGEM("0", rFGEM.impuestoFGEM());
        assertMontoFGEM("-1000", rFGEM.utilidadNetaFGEM());
    }

    @Test
    void metaDeUtilidadPorPrecioOVolumenFGEM() {
        SupuestosFGEM neutrosFGEM = SupuestosFGEM.neutrosFGEM(TASA_FGEM);

        BigDecimal precioFGEM = SimuladorFGEM.cambioPrecioParaMetaFGEM(BASE_FGEM, neutrosFGEM, bdFGEM("1400")).orElseThrow();
        assertEquals(0, bdFGEM("0.10").compareTo(precioFGEM), "Para ganar 1 400 hay que subir precios 10 %");
        assertMontoFGEM("1400", SimuladorFGEM.simularFGEM(BASE_FGEM, neutrosFGEM.conFGEM(VariableFGEM.PRECIO, precioFGEM)).utilidadNetaFGEM());

        BigDecimal volumenFGEM = SimuladorFGEM.cambioVolumenParaMetaFGEM(BASE_FGEM, neutrosFGEM, bdFGEM("910")).orElseThrow();
        assertEquals(0, bdFGEM("0.10").compareTo(volumenFGEM), "Para ganar 910 hay que vender 10 % más");

        BigDecimal equilibrioFGEM = SimuladorFGEM.cambioVolumenParaMetaFGEM(BASE_FGEM, neutrosFGEM, BigDecimal.ZERO).orElseThrow();
        assertEquals(0, bdFGEM("-0.333333").compareTo(equilibrioFGEM), "Con 33.3 % menos de volumen se llega a cero");
    }

    @Test
    void sinMargenDeContribucionNingunVolumenAlcanzaLaMetaFGEM() {
        SupuestosFGEM caroFGEM = SupuestosFGEM.neutrosFGEM(TASA_FGEM).conFGEM(VariableFGEM.COSTO_UNITARIO, bdFGEM("0.60"));
        assertTrue(SimuladorFGEM.cambioVolumenParaMetaFGEM(BASE_FGEM, caroFGEM, bdFGEM("500")).isEmpty());
    }

    @Test
    void sensibilidadOrdenadaPorImpactoFGEM() {
        List<SimuladorFGEM.SensibilidadFGEM> sFGEM = SimuladorFGEM.sensibilidadFGEM(BASE_FGEM, SupuestosFGEM.neutrosFGEM(TASA_FGEM));

        assertEquals(List.of(VariableFGEM.PRECIO, VariableFGEM.COSTO_UNITARIO, VariableFGEM.VOLUMEN, VariableFGEM.COSTOS_FIJOS,
                VariableFGEM.GASTOS_VARIABLES), sFGEM.stream().map(SimuladorFGEM.SensibilidadFGEM::variableFGEM).toList());
        SimuladorFGEM.SensibilidadFGEM precioFGEM = sFGEM.get(0);
        assertMontoFGEM("0", precioFGEM.utilidadSiBajaFGEM());
        assertMontoFGEM("1400", precioFGEM.utilidadSiSubeFGEM());
        assertMontoFGEM("700", precioFGEM.utilidadEscenarioFGEM());
    }

    @Test
    void escenariosPredefinidosFGEM() {
        SupuestosFGEM pesimistaFGEM = EscenarioPredefinidoFGEM.PESIMISTA.aplicarFGEM(TASA_FGEM);
        assertEquals(0, bdFGEM("-0.15").compareTo(pesimistaFGEM.volumenFGEM()));
        assertEquals(0, bdFGEM("0.05").compareTo(pesimistaFGEM.costoUnitarioFGEM()));
        assertTrue(SimuladorFGEM.simularFGEM(BASE_FGEM, pesimistaFGEM).utilidadNetaFGEM().compareTo(bdFGEM("700")) < 0);
        assertTrue(SimuladorFGEM.simularFGEM(BASE_FGEM, EscenarioPredefinidoFGEM.OPTIMISTA.aplicarFGEM(TASA_FGEM)).utilidadNetaFGEM()
                .compareTo(bdFGEM("700")) > 0);
    }

    /**
     * Con un margen de contribución pequeño (como una bodega que pierde dinero), redondear los promedios mensuales
     * a céntimos movía el punto de equilibrio varios soles. El simulador debe coincidir con el cálculo directo
     * sobre los totales del período.
     */
    @Test
    void conMargenPequenoCoincideAlCentimoConElCalculoDirectoFGEM() {
        // Totales de 3 meses: el promedio mensual tiene decimales infinitos (÷ 3).
        BigDecimal ventasFGEM = bdFGEM("7362.01");
        BigDecimal variablesFGEM = bdFGEM("7110.43");
        BigDecimal fijosFGEM = bdFGEM("2728.07");
        BigDecimal tresFGEM = BigDecimal.valueOf(3);
        java.math.MathContext mcFGEM = new java.math.MathContext(30);
        BaseSimulacionFGEM baseFGEM = new BaseSimulacionFGEM(PeriodoFGEM.mesFGEM(YearMonth.of(2026, 1)), 3,
                ventasFGEM.divide(tresFGEM, mcFGEM), variablesFGEM.divide(tresFGEM, mcFGEM), BigDecimal.ZERO,
                fijosFGEM.divide(tresFGEM, mcFGEM), BigDecimal.ZERO, BigDecimal.ZERO);

        ResultadoEscenarioFGEM rFGEM = SimuladorFGEM.simularFGEM(baseFGEM, SupuestosFGEM.neutrosFGEM(BigDecimal.ZERO));

        // Directo: (ventas − variables − fijos) ÷ 3 y punto de equilibrio mensual = fijos × ventas ÷ margen ÷ 3.
        BigDecimal utilidadFGEM = ventasFGEM.subtract(variablesFGEM).subtract(fijosFGEM).divide(tresFGEM, 2, java.math.RoundingMode.HALF_UP);
        BigDecimal equilibrioFGEM = fijosFGEM.multiply(ventasFGEM).divide(ventasFGEM.subtract(variablesFGEM), mcFGEM)
                .divide(tresFGEM, 2, java.math.RoundingMode.HALF_UP);
        assertMontoFGEM(utilidadFGEM.toPlainString(), rFGEM.utilidadNetaFGEM().setScale(2, java.math.RoundingMode.HALF_UP));
        assertMontoFGEM(equilibrioFGEM.toPlainString(), rFGEM.equilibrioFGEM().ventasEquilibrioFGEM());
    }

    private static BigDecimal bdFGEM(String valorFGEM) {
        return new BigDecimal(valorFGEM);
    }

    private static void assertMontoFGEM(String esperadoFGEM, BigDecimal realFGEM) {
        assertEquals(0, bdFGEM(esperadoFGEM).compareTo(realFGEM), "se esperaba " + esperadoFGEM + " y se obtuvo " + realFGEM);
    }
}
