package com.businessanalyzer.analytics.tesoreria;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.model.CategoriaDeudaFGEM;
import com.businessanalyzer.model.CuentaDineroFGEM;
import com.businessanalyzer.model.DeudaFGEM;
import com.businessanalyzer.model.TipoCuentaFGEM;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TesoreriaAnaliticaTestFGEM {

    private static final CuentaDineroFGEM CAJA_FGEM = cuentaFGEM(1, "Caja", "500");
    private static final CuentaDineroFGEM BANCO_FGEM = cuentaFGEM(2, "Banco", "2000");

    private static final List<FlujoDineroFGEM> FLUJOS_FGEM = List.of(
            flujoFGEM("2026-02-20", 1, "300", RubroFlujoFGEM.COBROS_VENTAS),
            flujoFGEM("2026-03-02", 1, "1200", RubroFlujoFGEM.COBROS_VENTAS),
            flujoFGEM("2026-03-05", 2, "-800", RubroFlujoFGEM.PAGOS_COMPRAS),
            flujoFGEM("2026-03-10", 2, "-150", RubroFlujoFGEM.GASTOS),
            flujoFGEM("2026-03-10", 2, "-40", RubroFlujoFGEM.INTERESES),
            flujoFGEM("2026-03-10", 2, "-260", RubroFlujoFGEM.PAGO_DEUDAS),
            flujoFGEM("2026-03-15", 2, "-900", RubroFlujoFGEM.COMPRA_ACTIVOS),
            flujoFGEM("2026-03-31", 1, "-1000", RubroFlujoFGEM.TRANSFERENCIAS),
            flujoFGEM("2026-03-31", 2, "1000", RubroFlujoFGEM.TRANSFERENCIAS),
            flujoFGEM("2026-04-01", 1, "-100", RubroFlujoFGEM.RETIROS));

    @Test
    void elSaldoDeCadaCuentaEsElInicialMasEntradasMenosSalidasFGEM() {
        List<SaldoCuentaFGEM> saldosFGEM = SaldoCuentaFGEM.deFGEM(List.of(CAJA_FGEM, BANCO_FGEM), FLUJOS_FGEM, LocalDate.of(2026, 3, 31));

        igualFGEM("1000", saldosFGEM.get(0).saldoFGEM());
        igualFGEM("850", saldosFGEM.get(1).saldoFGEM());
        igualFGEM("1850", SaldoCuentaFGEM.totalFGEM(saldosFGEM));
        assertEquals(LocalDate.of(2026, 3, 31), saldosFGEM.get(0).ultimoMovimientoFGEM());
        igualFGEM("900", SaldoCuentaFGEM.deFGEM(List.of(CAJA_FGEM), FLUJOS_FGEM, LocalDate.of(2026, 4, 1)).get(0).saldoFGEM());
    }

    @Test
    void elFlujoDeEfectivoCuadraConLosSaldosYNoMuestraLasTransferenciasFGEM() {
        FlujoEfectivoFGEM marzoFGEM = FlujoEfectivoFGEM.deFGEM(PeriodoFGEM.mesFGEM(YearMonth.of(2026, 3)), List.of(CAJA_FGEM, BANCO_FGEM), FLUJOS_FGEM);

        igualFGEM("2800", marzoFGEM.saldoInicialFGEM());
        igualFGEM("210", marzoFGEM.netoFGEM(ActividadFGEM.OPERACION));
        igualFGEM("-900", marzoFGEM.netoFGEM(ActividadFGEM.INVERSION));
        igualFGEM("-260", marzoFGEM.netoFGEM(ActividadFGEM.FINANCIAMIENTO));
        igualFGEM("1850", marzoFGEM.saldoFinalFGEM());
        igualFGEM("-950", marzoFGEM.variacionFGEM());
        assertFalse(marzoFGEM.porRubroFGEM().containsKey(RubroFlujoFGEM.TRANSFERENCIAS));
        igualFGEM(SaldoCuentaFGEM.totalFGEM(SaldoCuentaFGEM.deFGEM(List.of(CAJA_FGEM, BANCO_FGEM), FLUJOS_FGEM, LocalDate.of(2026, 3, 31))),
                marzoFGEM.saldoFinalFGEM());
    }

    @Test
    void laSerieMensualAcumulaElSaldoDeCierreFGEM() {
        List<FlujoMensualFGEM> serieFGEM = FlujoMensualFGEM.serieFGEM(YearMonth.of(2026, 2), YearMonth.of(2026, 4),
                List.of(CAJA_FGEM, BANCO_FGEM), FLUJOS_FGEM);

        assertEquals(3, serieFGEM.size());
        igualFGEM("2800", serieFGEM.get(0).saldoCierreFGEM());
        igualFGEM("1200", serieFGEM.get(1).entradasFGEM());
        igualFGEM("2150", serieFGEM.get(1).salidasFGEM());
        igualFGEM("210", serieFGEM.get(1).operacionFGEM());
        igualFGEM("1850", serieFGEM.get(1).saldoCierreFGEM());
        igualFGEM("1750", serieFGEM.get(2).saldoCierreFGEM());
    }

    @Test
    void sinCuatroMesesDeHistoriaNoSeProyectaFGEM() {
        ProyeccionCajaFGEM pFGEM = ProyeccionCajaFGEM.calcularFGEM(LocalDate.of(2026, 3, 15), new BigDecimal("1000"),
                new double[]{100, 200, 300}, List.of());

        assertFalse(pFGEM.suficienteFGEM());
        assertTrue(pFGEM.mesesFGEM().isEmpty());
        assertEquals(3, pFGEM.mesesHistoriaFGEM());
    }

    @Test
    void avisaElPrimerMesEnQueSeAcabaElDineroFGEM() {
        double[] historiaFGEM = {-500, -500, -500, -500, -500, -500};
        ProyeccionCajaFGEM pFGEM = ProyeccionCajaFGEM.calcularFGEM(LocalDate.of(2026, 3, 31), new BigDecimal("1200"), historiaFGEM,
                List.of());

        assertTrue(pFGEM.suficienteFGEM());
        assertEquals(ProyeccionCajaFGEM.HORIZONTE_FGEM + 1, pFGEM.mesesFGEM().size());
        igualFGEM("0", pFGEM.mesesFGEM().get(0).operacionFGEM());
        igualFGEM("700", pFGEM.mesesFGEM().get(1).saldoFGEM());
        igualFGEM("200", pFGEM.mesesFGEM().get(2).saldoFGEM());
        assertEquals(YearMonth.of(2026, 6), pFGEM.primerNegativoFGEM());
        assertTrue(!pFGEM.primerNegativoPesimistaFGEM().isAfter(pFGEM.primerNegativoFGEM()));
    }

    @Test
    void conFlujosPositivosNoHayAlertaFGEM() {
        double[] historiaFGEM = {800, 820, 790, 810, 805, 815};
        ProyeccionCajaFGEM pFGEM = ProyeccionCajaFGEM.calcularFGEM(LocalDate.of(2026, 3, 10), new BigDecimal("500"), historiaFGEM,
                List.of());

        assertNull(pFGEM.primerNegativoFGEM());
        assertTrue(pFGEM.mesesFGEM().get(ProyeccionCajaFGEM.HORIZONTE_FGEM).saldoFGEM().compareTo(new BigDecimal("5000")) > 0);
    }

    @Test
    void lasCuotasDescuentanSoloElCapitalHastaCancelarLaDeudaFGEM() {
        DeudaFGEM prestamoFGEM = deudaFGEM("1000", "12", "400", null);
        DeudaFGEM proveedorFGEM = deudaFGEM("300", null, null, LocalDate.of(2026, 5, 20));
        Map<YearMonth, BigDecimal> capitalFGEM = ProyeccionCajaFGEM.capitalPorMesFGEM(List.of(prestamoFGEM, proveedorFGEM),
                YearMonth.of(2026, 4), 6);

        igualFGEM("390", capitalFGEM.get(YearMonth.of(2026, 4)));
        igualFGEM("693.90", capitalFGEM.get(YearMonth.of(2026, 5)));
        igualFGEM("216.10", capitalFGEM.get(YearMonth.of(2026, 6)));
        assertNull(capitalFGEM.get(YearMonth.of(2026, 7)));
    }

    private static CuentaDineroFGEM cuentaFGEM(long idFGEM, String nombreFGEM, String saldoInicialFGEM) {
        return new CuentaDineroFGEM(idFGEM, 1, nombreFGEM, TipoCuentaFGEM.CAJA, null, new BigDecimal(saldoInicialFGEM), true, "admin",
                null, null, null);
    }

    private static FlujoDineroFGEM flujoFGEM(String fechaFGEM, long cuentaFGEM, String montoFGEM, RubroFlujoFGEM rubroFGEM) {
        return new FlujoDineroFGEM(LocalDate.parse(fechaFGEM), cuentaFGEM, new BigDecimal(montoFGEM), rubroFGEM, rubroFGEM.descripcionFGEM());
    }

    private static DeudaFGEM deudaFGEM(String montoFGEM, String tasaFGEM, String cuotaFGEM, LocalDate vencimientoFGEM) {
        return new DeudaFGEM(1, 1, CategoriaDeudaFGEM.PRESTAMO_BANCARIO, "Banco", null, LocalDate.of(2026, 1, 1),
                new BigDecimal(montoFGEM), BigDecimal.ZERO, tasaFGEM == null ? null : new BigDecimal(tasaFGEM),
                cuotaFGEM == null ? null : new BigDecimal(cuotaFGEM), vencimientoFGEM, false, null, "admin", null, null, null);
    }

    private static void igualFGEM(String esperadoFGEM, BigDecimal realFGEM) {
        assertEquals(0, new BigDecimal(esperadoFGEM).compareTo(realFGEM), "Esperado " + esperadoFGEM + " pero fue " + realFGEM);
    }

    private static void igualFGEM(BigDecimal esperadoFGEM, BigDecimal realFGEM) {
        assertEquals(0, esperadoFGEM.compareTo(realFGEM), "Esperado " + esperadoFGEM + " pero fue " + realFGEM);
    }
}
