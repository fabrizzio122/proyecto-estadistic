package com.businessanalyzer.analytics.patrimonio;

import com.businessanalyzer.analytics.EstadoResultadosFGEM;
import com.businessanalyzer.model.ActivoFGEM;
import com.businessanalyzer.model.CategoriaActivoFGEM;
import com.businessanalyzer.model.CategoriaDeudaFGEM;
import com.businessanalyzer.model.DeudaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceGeneralTestFGEM {

    private static final LocalDate CORTE_FGEM = LocalDate.of(2026, 1, 15);

    // ------------------------------------------------------------------ depreciación

    @Test
    void depreciaEnLineaRectaPorMesesCompletosFGEM() {
        // 12 000 a 10 años = 100 al mes; 12 meses completos -> 1 200.
        DepreciacionFGEM dFGEM = DepreciacionFGEM.deFGEM(activoFGEM(CategoriaActivoFGEM.MAQUINARIA, "12000", LocalDate.of(2025, 1, 15), 10, "0"), CORTE_FGEM);
        assertEquals(12, dFGEM.mesesTranscurridosFGEM());
        igualFGEM("1200", dFGEM.acumuladaFGEM());
        igualFGEM("100", dFGEM.mensualFGEM());
        igualFGEM("10800", dFGEM.valorEnLibrosFGEM());

        // Un día antes de cumplir el mes 12 solo cuentan 11 meses.
        DepreciacionFGEM antesFGEM = DepreciacionFGEM.deFGEM(activoFGEM(CategoriaActivoFGEM.MAQUINARIA, "12000", LocalDate.of(2025, 1, 16), 10, "0"), CORTE_FGEM);
        igualFGEM("1100", antesFGEM.acumuladaFGEM());
    }

    @Test
    void elValorResidualNoSeDepreciaFGEM() {
        // (5 500 − 800) / 60 meses = 78.3333 al mes.
        DepreciacionFGEM dFGEM = DepreciacionFGEM.deFGEM(activoFGEM(CategoriaActivoFGEM.VEHICULOS, "5500", LocalDate.of(2025, 1, 15), 5, "800"), CORTE_FGEM);
        igualFGEM("78.33", dFGEM.mensualFGEM());
        igualFGEM("940", dFGEM.acumuladaFGEM());
    }

    @Test
    void unBienTotalmenteDepreciadoConservaSuValorResidualFGEM() {
        DepreciacionFGEM dFGEM = DepreciacionFGEM.deFGEM(activoFGEM(CategoriaActivoFGEM.EQUIPOS_COMPUTO, "3000", LocalDate.of(2019, 6, 1), 4, "200"), CORTE_FGEM);
        igualFGEM("2800", dFGEM.acumuladaFGEM());
        igualFGEM("0", dFGEM.mensualFGEM());
        igualFGEM("200", dFGEM.valorEnLibrosFGEM());
    }

    @Test
    void terrenosYActivosSinVidaUtilNoSeDeprecianFGEM() {
        DepreciacionFGEM terrenoFGEM = DepreciacionFGEM.deFGEM(activoFGEM(CategoriaActivoFGEM.TERRENOS, "80000", LocalDate.of(2010, 1, 1), 20, "0"), CORTE_FGEM);
        igualFGEM("80000", terrenoFGEM.valorEnLibrosFGEM());
        DepreciacionFGEM sinVidaFGEM = DepreciacionFGEM.deFGEM(activoFGEM(CategoriaActivoFGEM.MAQUINARIA, "5000", LocalDate.of(2010, 1, 1), null, "0"), CORTE_FGEM);
        igualFGEM("5000", sinVidaFGEM.valorEnLibrosFGEM());
        assertNull(DepreciacionFGEM.avanceFGEM(activoFGEM(CategoriaActivoFGEM.TERRENOS, "1", CORTE_FGEM, null, "0"), CORTE_FGEM));
    }

    // ------------------------------------------------------------------ balance

    @Test
    void armaElBalanceConCortoYLargoPlazoFGEM() {
        List<ActivoFGEM> activosFGEM = List.of(
                activoFGEM(CategoriaActivoFGEM.CAJA_BANCOS, "5000", CORTE_FGEM, null, "0"),
                activoFGEM(CategoriaActivoFGEM.MAQUINARIA, "12000", LocalDate.of(2025, 1, 15), 10, "0"));
        List<DeudaFGEM> deudasFGEM = List.of(
                deudaFGEM(CategoriaDeudaFGEM.PROVEEDORES, "2000", "0", null, null, false),
                // Vence en 48 meses: 12/48 del saldo es de corto plazo.
                deudaFGEM(CategoriaDeudaFGEM.PRESTAMO_BANCARIO, "30000", "6000", "900", CORTE_FGEM.plusMonths(48), false),
                deudaFGEM(CategoriaDeudaFGEM.OTRAS, "999", "0", null, null, true),     // anulada
                deudaFGEM(CategoriaDeudaFGEM.OTRAS, "500", "500", null, null, false)); // cancelada

        BalanceGeneralFGEM bFGEM = BalanceGeneralFGEM.deFGEM(CORTE_FGEM, activosFGEM, new BigDecimal("3000"), deudasFGEM);

        assertEquals(List.of("Caja y bancos", BalanceGeneralFGEM.INVENTARIO_FGEM),
                bFGEM.activoCorrienteFGEM().stream().map(BalanceGeneralFGEM.PartidaFGEM::conceptoFGEM).toList());
        igualFGEM("8000", bFGEM.totalActivoCorrienteFGEM());
        igualFGEM("10800", bFGEM.totalActivoNoCorrienteFGEM());
        igualFGEM("18800", bFGEM.totalActivoFGEM());
        igualFGEM("8000", bFGEM.totalPasivoCorrienteFGEM());      // 2 000 + 24 000 × 12/48
        igualFGEM("18000", bFGEM.totalPasivoNoCorrienteFGEM());
        igualFGEM("26000", bFGEM.totalPasivoFGEM());
        igualFGEM("-7200", bFGEM.patrimonioFGEM());
        igualFGEM("900", bFGEM.cuotasMensualesFGEM());
        igualFGEM("100", bFGEM.depreciacionMensualFGEM());
        assertEquals(2, bFGEM.activosRegistradosFGEM());
        assertEquals(2, bFGEM.deudasPendientesFGEM());
        assertFalse(bFGEM.sinRegistrosFGEM());
    }

    @Test
    void unaDeudaQueVenceDentroDeUnAnoEsTodaDeCortoPlazoFGEM() {
        igualFGEM("10000", BalanceGeneralFGEM.porcionCorrienteFGEM(
                deudaFGEM(CategoriaDeudaFGEM.PRESTAMO_BANCARIO, "10000", "0", null, CORTE_FGEM.plusMonths(12), false), CORTE_FGEM));
        igualFGEM("10000", BalanceGeneralFGEM.porcionCorrienteFGEM(
                deudaFGEM(CategoriaDeudaFGEM.PRESTAMO_BANCARIO, "10000", "0", null, CORTE_FGEM.minusDays(3), false), CORTE_FGEM));
        assertEquals(13, BalanceGeneralFGEM.mesesHastaFGEM(CORTE_FGEM, CORTE_FGEM.plusMonths(12).plusDays(1)));
    }

    @Test
    void ignoraLosActivosDadosDeBajaYLosFuturosFGEM() {
        ActivoFGEM vendidoFGEM = new ActivoFGEM(1, 1, CategoriaActivoFGEM.VEHICULOS, "Moto", LocalDate.of(2024, 1, 1),
                new BigDecimal("5000"), 5, BigDecimal.ZERO, true, LocalDate.of(2025, 12, 1), "Vendida",
                "admin", null, null, null);
        ActivoFGEM futuroFGEM = activoFGEM(CategoriaActivoFGEM.CAJA_BANCOS, "100", CORTE_FGEM.plusDays(1), null, "0");
        BalanceGeneralFGEM bFGEM = BalanceGeneralFGEM.deFGEM(CORTE_FGEM, List.of(vendidoFGEM, futuroFGEM), BigDecimal.ZERO, List.of());
        igualFGEM("0", bFGEM.totalActivoFGEM());
        assertTrue(bFGEM.sinRegistrosFGEM());
    }

    // ------------------------------------------------------------------ indicadores

    @Test
    void calculaLiquidezEndeudamientoYRentabilidadFGEM() {
        List<ActivoFGEM> activosFGEM = List.of(
                activoFGEM(CategoriaActivoFGEM.CAJA_BANCOS, "6000", CORTE_FGEM, null, "0"),
                activoFGEM(CategoriaActivoFGEM.EDIFICIOS, "40000", CORTE_FGEM, 20, "0"));
        List<DeudaFGEM> deudasFGEM = List.of(deudaFGEM(CategoriaDeudaFGEM.PRESTAMO_BANCARIO, "4000", "0", "1000", null, false));
        BalanceGeneralFGEM bFGEM = BalanceGeneralFGEM.deFGEM(CORTE_FGEM, activosFGEM, new BigDecimal("2000"), deudasFGEM);
        // 6 meses con utilidad neta 3 000 y utilidad operativa 4 800.
        EstadoResultadosFGEM rFGEM = resultadosFGEM("4800", "3000");

        IndicadoresPatrimonioFGEM iFGEM = IndicadoresPatrimonioFGEM.deFGEM(bFGEM, rFGEM, 6);

        igualFGEM("2", iFGEM.liquidezCorrienteFGEM());               // 8 000 / 4 000
        igualFGEM("1.5", iFGEM.pruebaAcidaFGEM());                   // 6 000 / 4 000
        igualFGEM("4000", iFGEM.capitalTrabajoFGEM());
        igualFGEM("0.083333", iFGEM.endeudamientoFGEM());            // 4 000 / 48 000
        igualFGEM("0.090909", iFGEM.apalancamientoFGEM());           // 4 000 / 44 000
        igualFGEM("6000", iFGEM.utilidadAnualFGEM());
        igualFGEM("0.125", iFGEM.rentabilidadActivosFGEM());         // 6 000 / 48 000
        igualFGEM("0.136364", iFGEM.rentabilidadPatrimonioFGEM());   // 6 000 / 44 000
        igualFGEM("800", iFGEM.utilidadOperativaMensualFGEM());
        igualFGEM("0.8", iFGEM.coberturaCuotasFGEM());               // 800 / 1 000
        assertEquals("2.00", IndicadoresPatrimonioFGEM.vecesFGEM(iFGEM.liquidezCorrienteFGEM()));
    }

    @Test
    void sinDeudasNiVentasLosRatiosQuedanVaciosFGEM() {
        BalanceGeneralFGEM bFGEM = BalanceGeneralFGEM.deFGEM(CORTE_FGEM, List.of(activoFGEM(CategoriaActivoFGEM.CAJA_BANCOS, "100", CORTE_FGEM, null, "0")),
                BigDecimal.ZERO, List.of());
        IndicadoresPatrimonioFGEM iFGEM = IndicadoresPatrimonioFGEM.deFGEM(bFGEM, null, 0);
        assertNull(iFGEM.liquidezCorrienteFGEM());
        assertNull(iFGEM.coberturaCuotasFGEM());
        assertNull(iFGEM.rentabilidadPatrimonioFGEM());
        igualFGEM("0", iFGEM.endeudamientoFGEM());
        assertEquals(0, iFGEM.mesesResultadosFGEM());
    }

    @Test
    void laLecturaAvisaDelPatrimonioNegativoYDeLaFaltaDeCapacidadDePagoFGEM() {
        BalanceGeneralFGEM bFGEM = BalanceGeneralFGEM.deFGEM(CORTE_FGEM, List.of(activoFGEM(CategoriaActivoFGEM.CAJA_BANCOS, "1000", CORTE_FGEM, null, "0")),
                BigDecimal.ZERO, List.of(deudaFGEM(CategoriaDeudaFGEM.PRESTAMO_BANCARIO, "5000", "0", "600", null, false)));
        SituacionPatrimonialFGEM sFGEM = new SituacionPatrimonialFGEM(bFGEM, IndicadoresPatrimonioFGEM.deFGEM(bFGEM, resultadosFGEM("1500", "900"), 3), null);
        String textoFGEM = String.join(" ", LecturaPatrimonioFGEM.generarFGEM(sFGEM, MonedaFGEM.PEN));
        assertTrue(textoFGEM.contains("patrimonio es negativo"), textoFGEM);
        assertTrue(textoFGEM.contains("no alcanza para pagar las cuotas"), textoFGEM);

        BalanceGeneralFGEM vacioFGEM = BalanceGeneralFGEM.deFGEM(CORTE_FGEM, List.of(), BigDecimal.ZERO, List.of());
        assertTrue(LecturaPatrimonioFGEM.generarFGEM(new SituacionPatrimonialFGEM(vacioFGEM, IndicadoresPatrimonioFGEM.deFGEM(vacioFGEM, null, 0), null),
                MonedaFGEM.PEN).get(0).startsWith("Aún no hay activos"));
    }

    // ------------------------------------------------------------------ utilidades

    static ActivoFGEM activoFGEM(CategoriaActivoFGEM categoriaFGEM, String valorFGEM, LocalDate fechaFGEM, Integer vidaFGEM, String residualFGEM) {
        return new ActivoFGEM(1, 1, categoriaFGEM, categoriaFGEM.descripcionFGEM(), fechaFGEM, new BigDecimal(valorFGEM), vidaFGEM,
                new BigDecimal(residualFGEM), false, null, null, "admin", null, null, null);
    }

    static DeudaFGEM deudaFGEM(CategoriaDeudaFGEM categoriaFGEM, String montoFGEM, String pagadoFGEM, String cuotaFGEM, LocalDate vencimientoFGEM,
                       boolean anuladaFGEM) {
        return new DeudaFGEM(1, 1, categoriaFGEM, "Acreedor", null, CORTE_FGEM.minusMonths(1), new BigDecimal(montoFGEM),
                new BigDecimal(pagadoFGEM), null, cuotaFGEM == null ? null : new BigDecimal(cuotaFGEM), vencimientoFGEM, anuladaFGEM,
                null, "admin", null, null, null);
    }

    private static EstadoResultadosFGEM resultadosFGEM(String operativaFGEM, String netaFGEM) {
        BigDecimal zFGEM = BigDecimal.ZERO;
        return new EstadoResultadosFGEM(zFGEM, zFGEM, zFGEM, zFGEM, new BigDecimal(operativaFGEM), zFGEM, zFGEM, zFGEM, zFGEM, zFGEM, new BigDecimal(netaFGEM));
    }

    private static void igualFGEM(String esperadoFGEM, BigDecimal realFGEM) {
        assertEquals(0, new BigDecimal(esperadoFGEM).compareTo(realFGEM), "esperado " + esperadoFGEM + " pero fue " + realFGEM);
    }
}
