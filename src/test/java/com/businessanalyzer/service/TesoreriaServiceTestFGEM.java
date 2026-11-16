package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.tesoreria.ActividadFGEM;
import com.businessanalyzer.analytics.tesoreria.FlujoEfectivoFGEM;
import com.businessanalyzer.analytics.tesoreria.RubroFlujoFGEM;
import com.businessanalyzer.analytics.tesoreria.SaldoCuentaFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CategoriaActivoFGEM;
import com.businessanalyzer.model.CategoriaDeudaFGEM;
import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.CondicionPagoFGEM;
import com.businessanalyzer.model.CuentaDineroFGEM;
import com.businessanalyzer.model.DatosActivoFGEM;
import com.businessanalyzer.model.DatosCuentaFGEM;
import com.businessanalyzer.model.DatosDeudaFGEM;
import com.businessanalyzer.model.DatosMovimientoFGEM;
import com.businessanalyzer.model.DatosMovimientoTesoreriaFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM.DatosLineaFGEM;
import com.businessanalyzer.model.DatosProductoFGEM;
import com.businessanalyzer.model.DeudaFGEM;
import com.businessanalyzer.model.MovimientoFinancieroFGEM;
import com.businessanalyzer.model.OperacionFGEM;
import com.businessanalyzer.model.PagoDeudaFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoCuentaFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.TipoMovimientoFGEM;
import com.businessanalyzer.model.TipoMovimientoTesoreriaFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TesoreriaServiceTestFGEM {

    /** El reloj de pruebas está en 2026-01-15. */
    private static final LocalDate HOY_FGEM = LocalDate.of(2026, 1, 15);

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private TesoreriaServiceFGEM servicioFGEM;
    private CuentaDineroFGEM cajaFGEM;
    private CuentaDineroFGEM bancoFGEM;
    private ProductoFGEM arrozFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = entornoFGEM.tesoreriaFGEM;
        entornoFGEM.administradorConectadoFGEM();
        entornoFGEM.crearEmpresaActivaFGEM("Comercial Andina");
        cajaFGEM = servicioFGEM.cuentasActivasFGEM().get(0);
        servicioFGEM.actualizarCuentaFGEM(cajaFGEM.idFGEM(), new DatosCuentaFGEM("Caja", TipoCuentaFGEM.CAJA, null, new BigDecimal("1000")));
        bancoFGEM = servicioFGEM.registrarCuentaFGEM(new DatosCuentaFGEM("BCP", TipoCuentaFGEM.BANCO, "193-123", new BigDecimal("5000")));
        arrozFGEM = entornoFGEM.catalogoFGEM.crearProductoFGEM(new DatosProductoFGEM(TipoItemFGEM.PRODUCTO, null, "Arroz 5 kg", null, null,
                UnidadMedidaFGEM.UNIDAD, new BigDecimal("20"), new BigDecimal("25")));
    }

    @Test
    void cadaEmpresaNuevaEmpiezaConSuCajaFGEM() {
        assertEquals(EmpresaServiceFGEM.CUENTA_INICIAL_FGEM, cajaFGEM.nombreFGEM());
        assertEquals(TipoCuentaFGEM.CAJA, cajaFGEM.tipoFGEM());
        assertEquals(2, servicioFGEM.cuentasActivasFGEM().size());
        assertInvalidoFGEM(() -> servicioFGEM.registrarCuentaFGEM(new DatosCuentaFGEM("bcp", TipoCuentaFGEM.BANCO, null, null)));
    }

    @Test
    void lasVentasYComprasAlContadoMuevenSusCuentasFGEM() {
        // Compra de 10 × 20 = 200 + IGV 36 desde el banco; venta de 4 × 25 = 100 + IGV 18 a la caja.
        compraFGEM(CondicionPagoFGEM.CONTADO, bancoFGEM.idFGEM(), "10", null);
        OperacionFGEM ventaFGEM = ventaFGEM(CondicionPagoFGEM.CONTADO, null, "4", null);

        assertEquals(cajaFGEM.idFGEM(), ventaFGEM.cuentaIdFGEM(), "Sin cuenta indicada, la venta entra a la cuenta principal");
        igualFGEM("1118", saldoFGEM(cajaFGEM));
        igualFGEM("4764", saldoFGEM(bancoFGEM));
        igualFGEM("5882", entornoFGEM.patrimonioFGEM.situacionFGEM().balanceFGEM().efectivoFGEM());
    }

    @Test
    void unaVentaAlCreditoQuedaPorCobrarHastaQueSeCobraFGEM() {
        compraFGEM(CondicionPagoFGEM.CONTADO, bancoFGEM.idFGEM(), "10", null);
        OperacionFGEM ventaFGEM = ventaFGEM(CondicionPagoFGEM.CREDITO, null, "4", HOY_FGEM.plusDays(15));

        assertTrue(ventaFGEM.alCreditoFGEM());
        assertEquals(null, ventaFGEM.cuentaIdFGEM());
        igualFGEM("118", ventaFGEM.pendienteFGEM());
        igualFGEM("1000", saldoFGEM(cajaFGEM));
        assertEquals(1, servicioFGEM.pendientesFGEM(TipoOperacionFGEM.VENTA).size());
        igualFGEM("118", entornoFGEM.patrimonioFGEM.situacionFGEM().balanceFGEM().porCobrarFGEM());

        assertInvalidoFGEM(() -> servicioFGEM.registrarPagoFGEM(ventaFGEM.idFGEM(), cajaFGEM.idFGEM(), HOY_FGEM, new BigDecimal("200"), null));
        assertInvalidoFGEM(() -> servicioFGEM.registrarPagoFGEM(ventaFGEM.idFGEM(), cajaFGEM.idFGEM(), HOY_FGEM.minusDays(1), new BigDecimal("50"), null));
        OperacionFGEM parcialFGEM = servicioFGEM.registrarPagoFGEM(ventaFGEM.idFGEM(), cajaFGEM.idFGEM(), HOY_FGEM, new BigDecimal("50"), "Adelanto");
        igualFGEM("68", parcialFGEM.pendienteFGEM());
        igualFGEM("1050", saldoFGEM(cajaFGEM));

        // Con cobros registrados la venta no se anula ni se corrige directamente.
        assertInvalidoFGEM(() -> entornoFGEM.operacionesFGEM.anularFGEM(ventaFGEM.idFGEM(), "Error de registro"));
        OperacionFGEM completaFGEM = servicioFGEM.registrarPagoFGEM(ventaFGEM.idFGEM(), bancoFGEM.idFGEM(), HOY_FGEM, new BigDecimal("68"), null);
        igualFGEM("0", completaFGEM.pendienteFGEM());
        assertTrue(servicioFGEM.pendientesFGEM(TipoOperacionFGEM.VENTA).isEmpty());

        // Al anular un cobro, lo pendiente vuelve.
        long cobroFGEM = servicioFGEM.listarPagosFGEM(ventaFGEM.idFGEM()).get(0).idFGEM();
        igualFGEM("68", servicioFGEM.anularPagoFGEM(cobroFGEM).pendienteFGEM());
    }

    @Test
    void unaVentaAlCreditoNecesitaElClienteFGEM() {
        assertInvalidoFGEM(() -> entornoFGEM.operacionesFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.VENTA, HOY_FGEM, null, null,
                null, new BigDecimal("18"), null, List.of(lineaFGEM("1")), false, CondicionPagoFGEM.CREDITO, null, null), true));
        assertInvalidoFGEM(() -> entornoFGEM.operacionesFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.VENTA, HOY_FGEM, "Juan",
                null, null, new BigDecimal("18"), null, List.of(lineaFGEM("1")), false, CondicionPagoFGEM.CREDITO, null,
                HOY_FGEM.minusDays(1)), true));
    }

    @Test
    void lasTransferenciasNoCambianElTotalYLosRetirosSiFGEM() {
        servicioFGEM.registrarMovimientoFGEM(new DatosMovimientoTesoreriaFGEM(TipoMovimientoTesoreriaFGEM.TRANSFERENCIA, HOY_FGEM,
                cajaFGEM.idFGEM(), bancoFGEM.idFGEM(), new BigDecimal("600"), null));
        igualFGEM("400", saldoFGEM(cajaFGEM));
        igualFGEM("5600", saldoFGEM(bancoFGEM));
        igualFGEM("6000", SaldoCuentaFGEM.totalFGEM(servicioFGEM.listarCuentasFGEM(false)));

        servicioFGEM.registrarMovimientoFGEM(new DatosMovimientoTesoreriaFGEM(TipoMovimientoTesoreriaFGEM.RETIRO, HOY_FGEM,
                bancoFGEM.idFGEM(), null, new BigDecimal("500"), null));
        servicioFGEM.registrarMovimientoFGEM(new DatosMovimientoTesoreriaFGEM(TipoMovimientoTesoreriaFGEM.APORTE, HOY_FGEM,
                null, cajaFGEM.idFGEM(), new BigDecimal("200"), "Aporte para el vuelto"));
        igualFGEM("5700", SaldoCuentaFGEM.totalFGEM(servicioFGEM.listarCuentasFGEM(false)));

        FlujoEfectivoFGEM eneroFGEM = servicioFGEM.flujoEfectivoFGEM(HOY_FGEM.withDayOfMonth(1), HOY_FGEM);
        igualFGEM("-500", eneroFGEM.rubroFGEM(RubroFlujoFGEM.RETIROS));
        igualFGEM("200", eneroFGEM.rubroFGEM(RubroFlujoFGEM.APORTES));
        igualFGEM("0", eneroFGEM.rubroFGEM(RubroFlujoFGEM.TRANSFERENCIAS));
        igualFGEM("-300", eneroFGEM.netoFGEM(ActividadFGEM.FINANCIAMIENTO));

        assertInvalidoFGEM(() -> servicioFGEM.registrarMovimientoFGEM(new DatosMovimientoTesoreriaFGEM(
                TipoMovimientoTesoreriaFGEM.TRANSFERENCIA, HOY_FGEM, cajaFGEM.idFGEM(), cajaFGEM.idFGEM(), BigDecimal.TEN, null)));
        assertInvalidoFGEM(() -> servicioFGEM.registrarMovimientoFGEM(new DatosMovimientoTesoreriaFGEM(
                TipoMovimientoTesoreriaFGEM.RETIRO, HOY_FGEM.plusDays(1), cajaFGEM.idFGEM(), null, BigDecimal.TEN, null)));
        long retiroFGEM = servicioFGEM.listarMovimientosFGEM(HOY_FGEM, HOY_FGEM, false).stream()
                .filter(mFGEM -> mFGEM.tipoFGEM() == TipoMovimientoTesoreriaFGEM.RETIRO).findFirst().orElseThrow().idFGEM();
        servicioFGEM.anularMovimientoFGEM(retiroFGEM, "Retiro registrado dos veces");
        igualFGEM("6200", SaldoCuentaFGEM.totalFGEM(servicioFGEM.listarCuentasFGEM(false)));
    }

    @Test
    void unaCuentaConSaldoNoSeDesactivaFGEM() {
        assertInvalidoFGEM(() -> servicioFGEM.cambiarCuentaActivaFGEM(bancoFGEM.idFGEM(), false));
        servicioFGEM.registrarMovimientoFGEM(new DatosMovimientoTesoreriaFGEM(TipoMovimientoTesoreriaFGEM.TRANSFERENCIA, HOY_FGEM,
                bancoFGEM.idFGEM(), cajaFGEM.idFGEM(), new BigDecimal("5000"), null));
        assertFalse(servicioFGEM.cambiarCuentaActivaFGEM(bancoFGEM.idFGEM(), false).activaFGEM());
        // La caja queda como única cuenta activa y no se puede desactivar.
        servicioFGEM.registrarMovimientoFGEM(new DatosMovimientoTesoreriaFGEM(TipoMovimientoTesoreriaFGEM.RETIRO, HOY_FGEM,
                cajaFGEM.idFGEM(), null, new BigDecimal("6000"), null));
        assertInvalidoFGEM(() -> servicioFGEM.cambiarCuentaActivaFGEM(cajaFGEM.idFGEM(), false));
        // No se registran movimientos en una cuenta desactivada.
        assertInvalidoFGEM(() -> entornoFGEM.finanzasFGEM.registrarFGEM(gastoFGEM("100", bancoFGEM.idFGEM())));
    }

    @Test
    void elPagoDeUnaCuotaSeparaCapitalEInteresFGEM() {
        DeudaFGEM prestamoFGEM = entornoFGEM.patrimonioFGEM.registrarDeudaFGEM(new DatosDeudaFGEM(CategoriaDeudaFGEM.PRESTAMO_BANCARIO, "Caja Huancayo",
                "Capital de trabajo", HOY_FGEM.minusDays(10), new BigDecimal("3000"), new BigDecimal("24"),
                new BigDecimal("300"), null, bancoFGEM.idFGEM()));
        igualFGEM("8000", saldoFGEM(bancoFGEM));
        igualFGEM("60", PatrimonioServiceFGEM.interesSugeridoFGEM(prestamoFGEM));   // 3 000 × 24 % / 12

        DeudaFGEM despuesFGEM = entornoFGEM.patrimonioFGEM.registrarPagoFGEM(prestamoFGEM.idFGEM(), HOY_FGEM, new BigDecimal("240"),
                new BigDecimal("60"), bancoFGEM.idFGEM(), "Cuota 1");
        igualFGEM("2760", despuesFGEM.saldoFGEM());
        igualFGEM("7700", saldoFGEM(bancoFGEM));

        // El interés quedó como gasto financiero (entra en el estado de resultados) y no se edita aparte.
        MovimientoFinancieroFGEM interesFGEM = entornoFGEM.finanzasFGEM.listarFGEM(HOY_FGEM, HOY_FGEM, false).get(0);
        assertEquals(CategoriaFinancieraFGEM.INTERESES_PRESTAMOS, interesFGEM.categoriaFGEM());
        assertTrue(interesFGEM.dePagoDeudaFGEM());
        igualFGEM("60", interesFGEM.montoFGEM());
        assertInvalidoFGEM(() -> entornoFGEM.finanzasFGEM.anularFGEM(interesFGEM.idFGEM(), "No corresponde"));

        FlujoEfectivoFGEM eneroFGEM = servicioFGEM.flujoEfectivoFGEM(HOY_FGEM.withDayOfMonth(1), HOY_FGEM);
        igualFGEM("3000", eneroFGEM.rubroFGEM(RubroFlujoFGEM.PRESTAMOS));
        igualFGEM("-240", eneroFGEM.rubroFGEM(RubroFlujoFGEM.PAGO_DEUDAS));
        igualFGEM("-60", eneroFGEM.rubroFGEM(RubroFlujoFGEM.INTERESES));

        // Al anular el pago se anula también su interés y el dinero vuelve a la cuenta.
        PagoDeudaFGEM pagoFGEM = entornoFGEM.patrimonioFGEM.listarPagosFGEM(prestamoFGEM.idFGEM()).get(0);
        igualFGEM("300", pagoFGEM.totalFGEM());
        entornoFGEM.patrimonioFGEM.anularPagoFGEM(pagoFGEM.idFGEM());
        assertTrue(entornoFGEM.finanzasFGEM.listarFGEM(HOY_FGEM, HOY_FGEM, false).isEmpty());
        igualFGEM("8000", saldoFGEM(bancoFGEM));
    }

    @Test
    void laCompraDeUnActivoSaleDeLaCuentaElegidaFGEM() {
        entornoFGEM.patrimonioFGEM.registrarActivoFGEM(new DatosActivoFGEM(CategoriaActivoFGEM.EQUIPOS_COMPUTO, "Laptop", HOY_FGEM,
                new BigDecimal("2500"), 4, null, bancoFGEM.idFGEM()));
        entornoFGEM.patrimonioFGEM.registrarActivoFGEM(new DatosActivoFGEM(CategoriaActivoFGEM.MOBILIARIO, "Vitrina heredada", HOY_FGEM,
                new BigDecimal("800"), 10, null));
        igualFGEM("2500", saldoFGEM(bancoFGEM));
        igualFGEM("-2500", servicioFGEM.flujoEfectivoFGEM(HOY_FGEM, HOY_FGEM).netoFGEM(ActividadFGEM.INVERSION));
    }

    @Test
    void losIngresosYGastosUsanLaCuentaIndicadaFGEM() {
        entornoFGEM.finanzasFGEM.registrarFGEM(gastoFGEM("150", bancoFGEM.idFGEM()));
        entornoFGEM.finanzasFGEM.registrarFGEM(new DatosMovimientoFGEM(TipoMovimientoFGEM.INGRESO, CategoriaFinancieraFGEM.ALQUILERES_COBRADOS,
                null, HOY_FGEM, "Alquiler de cochera", new BigDecimal("200"), null));
        igualFGEM("4850", saldoFGEM(bancoFGEM));
        igualFGEM("1200", saldoFGEM(cajaFGEM));
    }

    @Test
    void respetaLosPermisosYLaEmpresaActivaFGEM() {
        UsuarioFGEM analistaFGEM = entornoFGEM.crearUsuarioFGEM("analista", "ANALISTA");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(entornoFGEM.sesionFGEM.empresaActivaFGEM().orElseThrow().idFGEM(), Set.of(analistaFGEM.idFGEM()));
        entornoFGEM.entrarComoFGEM("analista", "Temporal123");
        entornoFGEM.empresaServiceFGEM.restaurarEmpresaActivaFGEM();

        assertEquals(2, servicioFGEM.listarCuentasFGEM(false).size());
        assertThrows(AccesoDenegadoExceptionFGEM.class,
                () -> servicioFGEM.registrarCuentaFGEM(new DatosCuentaFGEM("Yape", TipoCuentaFGEM.BILLETERA, null, null)));
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.registrarMovimientoFGEM(new DatosMovimientoTesoreriaFGEM(
                TipoMovimientoTesoreriaFGEM.APORTE, HOY_FGEM, null, cajaFGEM.idFGEM(), BigDecimal.TEN, null)));
    }

    @Test
    void conPocosMesesNoHayProyeccionFGEM() {
        ventaFGEM(CondicionPagoFGEM.CONTADO, cajaFGEM.idFGEM(), "1", null);
        assertFalse(servicioFGEM.proyeccionFGEM().suficienteFGEM());
        igualFGEM("6029.50", servicioFGEM.proyeccionFGEM().saldoActualFGEM());   // caja 1 029,50 + banco 5 000
    }

    // ------------------------------------------------------------------ utilidades

    private OperacionFGEM ventaFGEM(CondicionPagoFGEM condicionFGEM, Long cuentaFGEM, String cantidadFGEM, LocalDate venceFGEM) {
        return entornoFGEM.operacionesFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.VENTA, HOY_FGEM, "Restaurante Sol", null, null,
                new BigDecimal("18"), null, List.of(new DatosLineaFGEM(arrozFGEM.idFGEM(), new BigDecimal(cantidadFGEM),
                new BigDecimal("25"))), false, condicionFGEM, cuentaFGEM, venceFGEM), true);
    }

    private OperacionFGEM compraFGEM(CondicionPagoFGEM condicionFGEM, Long cuentaFGEM, String cantidadFGEM, LocalDate venceFGEM) {
        return entornoFGEM.operacionesFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.COMPRA, HOY_FGEM, "Molino Norte", null, null,
                new BigDecimal("18"), null, List.of(new DatosLineaFGEM(arrozFGEM.idFGEM(), new BigDecimal(cantidadFGEM),
                new BigDecimal("20"))), false, condicionFGEM, cuentaFGEM, venceFGEM), true);
    }

    private DatosLineaFGEM lineaFGEM(String cantidadFGEM) {
        return new DatosLineaFGEM(arrozFGEM.idFGEM(), new BigDecimal(cantidadFGEM), new BigDecimal("25"));
    }

    private static DatosMovimientoFGEM gastoFGEM(String montoFGEM, Long cuentaFGEM) {
        return new DatosMovimientoFGEM(TipoMovimientoFGEM.GASTO, CategoriaFinancieraFGEM.SERVICIOS_BASICOS, null, HOY_FGEM,
                "Luz del local", new BigDecimal(montoFGEM), null, cuentaFGEM);
    }

    private BigDecimal saldoFGEM(CuentaDineroFGEM cuentaFGEM) {
        return servicioFGEM.listarCuentasFGEM(true).stream().filter(sFGEM -> sFGEM.cuentaFGEM().idFGEM() == cuentaFGEM.idFGEM())
                .findFirst().orElseThrow().saldoFGEM();
    }

    private static void assertInvalidoFGEM(Executable accionFGEM) {
        assertThrows(ValidacionExceptionFGEM.class, accionFGEM);
    }

    private static void igualFGEM(String esperadoFGEM, BigDecimal realFGEM) {
        assertEquals(0, new BigDecimal(esperadoFGEM).compareTo(realFGEM), "Esperado " + esperadoFGEM + " pero fue " + realFGEM);
    }
}
