package com.businessanalyzer.analytics.tesoreria;

import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.model.CategoriaDeuda;
import com.businessanalyzer.model.CuentaDinero;
import com.businessanalyzer.model.Deuda;
import com.businessanalyzer.model.TipoCuenta;
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

class TesoreriaAnaliticaTest {

    private static final CuentaDinero CAJA = cuenta(1, "Caja", "500");
    private static final CuentaDinero BANCO = cuenta(2, "Banco", "2000");

    /** Marzo: ventas, gastos, un depósito de caja al banco, compra de un equipo y la cuota de un préstamo. */
    private static final List<FlujoDinero> FLUJOS = List.of(
            flujo("2026-02-20", 1, "300", RubroFlujo.COBROS_VENTAS),
            flujo("2026-03-02", 1, "1200", RubroFlujo.COBROS_VENTAS),
            flujo("2026-03-05", 2, "-800", RubroFlujo.PAGOS_COMPRAS),
            flujo("2026-03-10", 2, "-150", RubroFlujo.GASTOS),
            flujo("2026-03-10", 2, "-40", RubroFlujo.INTERESES),
            flujo("2026-03-10", 2, "-260", RubroFlujo.PAGO_DEUDAS),
            flujo("2026-03-15", 2, "-900", RubroFlujo.COMPRA_ACTIVOS),
            flujo("2026-03-31", 1, "-1000", RubroFlujo.TRANSFERENCIAS),
            flujo("2026-03-31", 2, "1000", RubroFlujo.TRANSFERENCIAS),
            flujo("2026-04-01", 1, "-100", RubroFlujo.RETIROS));

    @Test
    void elSaldoDeCadaCuentaEsElInicialMasEntradasMenosSalidas() {
        List<SaldoCuenta> saldos = SaldoCuenta.de(List.of(CAJA, BANCO), FLUJOS, LocalDate.of(2026, 3, 31));

        igual("1000", saldos.get(0).saldo());   // 500 + 300 + 1200 − 1000
        igual("850", saldos.get(1).saldo());    // 2000 − 800 − 150 − 40 − 260 − 900 + 1000
        igual("1850", SaldoCuenta.total(saldos));
        assertEquals(LocalDate.of(2026, 3, 31), saldos.get(0).ultimoMovimiento());
        // El retiro de abril no cuenta antes de su fecha.
        igual("900", SaldoCuenta.de(List.of(CAJA), FLUJOS, LocalDate.of(2026, 4, 1)).get(0).saldo());
    }

    @Test
    void elFlujoDeEfectivoCuadraConLosSaldosYNoMuestraLasTransferencias() {
        FlujoEfectivo marzo = FlujoEfectivo.de(Periodo.mes(YearMonth.of(2026, 3)), List.of(CAJA, BANCO), FLUJOS);

        igual("2800", marzo.saldoInicial());    // 500 + 2000 + 300 de febrero
        igual("210", marzo.neto(Actividad.OPERACION));       // 1200 − 800 − 150 − 40
        igual("-900", marzo.neto(Actividad.INVERSION));
        igual("-260", marzo.neto(Actividad.FINANCIAMIENTO));
        igual("1850", marzo.saldoFinal());
        igual("-950", marzo.variacion());
        assertFalse(marzo.porRubro().containsKey(RubroFlujo.TRANSFERENCIAS));
        // El saldo final del estado coincide con la suma de los saldos de las cuentas.
        igual(SaldoCuenta.total(SaldoCuenta.de(List.of(CAJA, BANCO), FLUJOS, LocalDate.of(2026, 3, 31))),
                marzo.saldoFinal());
    }

    @Test
    void laSerieMensualAcumulaElSaldoDeCierre() {
        List<FlujoMensual> serie = FlujoMensual.serie(YearMonth.of(2026, 2), YearMonth.of(2026, 4),
                List.of(CAJA, BANCO), FLUJOS);

        assertEquals(3, serie.size());
        igual("2800", serie.get(0).saldoCierre());
        igual("1200", serie.get(1).entradas());   // sin la transferencia
        igual("2150", serie.get(1).salidas());
        igual("210", serie.get(1).operacion());
        igual("1850", serie.get(1).saldoCierre());
        igual("1750", serie.get(2).saldoCierre());
    }

    @Test
    void sinCuatroMesesDeHistoriaNoSeProyecta() {
        ProyeccionCaja p = ProyeccionCaja.calcular(LocalDate.of(2026, 3, 15), new BigDecimal("1000"),
                new double[]{100, 200, 300}, List.of());

        assertFalse(p.suficiente());
        assertTrue(p.meses().isEmpty());
        assertEquals(3, p.mesesHistoria());
    }

    @Test
    void avisaElPrimerMesEnQueSeAcabaElDinero() {
        // Cada mes la operación pierde 500; con 1 200 en caja el dinero alcanza para dos meses.
        double[] historia = {-500, -500, -500, -500, -500, -500};
        ProyeccionCaja p = ProyeccionCaja.calcular(LocalDate.of(2026, 3, 31), new BigDecimal("1200"), historia,
                List.of());

        assertTrue(p.suficiente());
        assertEquals(ProyeccionCaja.HORIZONTE + 1, p.meses().size());
        igual("0", p.meses().get(0).operacion());   // del mes en curso ya no queda ningún día
        igual("700", p.meses().get(1).saldo());
        igual("200", p.meses().get(2).saldo());
        assertEquals(YearMonth.of(2026, 6), p.primerNegativo());
        assertTrue(!p.primerNegativoPesimista().isAfter(p.primerNegativo()));
    }

    @Test
    void conFlujosPositivosNoHayAlerta() {
        double[] historia = {800, 820, 790, 810, 805, 815};
        ProyeccionCaja p = ProyeccionCaja.calcular(LocalDate.of(2026, 3, 10), new BigDecimal("500"), historia,
                List.of());

        assertNull(p.primerNegativo());
        assertTrue(p.meses().get(ProyeccionCaja.HORIZONTE).saldo().compareTo(new BigDecimal("5000")) > 0);
    }

    @Test
    void lasCuotasDescuentanSoloElCapitalHastaCancelarLaDeuda() {
        // Saldo 1 000 al 12 % anual (1 % mensual) con cuota de 400:
        //   abril: interés 10,00 → capital 390,00 (queda 610,00)
        //   mayo:  interés  6,10 → capital 393,90 (queda 216,10)
        //   junio: interés  2,16 → la cuota alcanza para el saldo: 216,10
        Deuda prestamo = deuda("1000", "12", "400", null);
        Deuda proveedor = deuda("300", null, null, LocalDate.of(2026, 5, 20));
        Map<YearMonth, BigDecimal> capital = ProyeccionCaja.capitalPorMes(List.of(prestamo, proveedor),
                YearMonth.of(2026, 4), 6);

        igual("390", capital.get(YearMonth.of(2026, 4)));
        igual("693.90", capital.get(YearMonth.of(2026, 5)));  // 393,90 + 300 del proveedor
        igual("216.10", capital.get(YearMonth.of(2026, 6)));
        assertNull(capital.get(YearMonth.of(2026, 7)));
    }

    // ------------------------------------------------------------------ utilidades

    private static CuentaDinero cuenta(long id, String nombre, String saldoInicial) {
        return new CuentaDinero(id, 1, nombre, TipoCuenta.CAJA, null, new BigDecimal(saldoInicial), true, "admin",
                null, null, null);
    }

    private static FlujoDinero flujo(String fecha, long cuenta, String monto, RubroFlujo rubro) {
        return new FlujoDinero(LocalDate.parse(fecha), cuenta, new BigDecimal(monto), rubro, rubro.descripcion());
    }

    private static Deuda deuda(String monto, String tasa, String cuota, LocalDate vencimiento) {
        return new Deuda(1, 1, CategoriaDeuda.PRESTAMO_BANCARIO, "Banco", null, LocalDate.of(2026, 1, 1),
                new BigDecimal(monto), BigDecimal.ZERO, tasa == null ? null : new BigDecimal(tasa),
                cuota == null ? null : new BigDecimal(cuota), vencimiento, false, null, "admin", null, null, null);
    }

    private static void igual(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "Esperado " + esperado + " pero fue " + real);
    }

    private static void igual(BigDecimal esperado, BigDecimal real) {
        assertEquals(0, esperado.compareTo(real), "Esperado " + esperado + " pero fue " + real);
    }
}
