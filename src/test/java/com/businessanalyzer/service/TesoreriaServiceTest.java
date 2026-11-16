package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.tesoreria.Actividad;
import com.businessanalyzer.analytics.tesoreria.FlujoEfectivo;
import com.businessanalyzer.analytics.tesoreria.RubroFlujo;
import com.businessanalyzer.analytics.tesoreria.SaldoCuenta;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CategoriaActivo;
import com.businessanalyzer.model.CategoriaDeuda;
import com.businessanalyzer.model.CategoriaFinanciera;
import com.businessanalyzer.model.CondicionPago;
import com.businessanalyzer.model.CuentaDinero;
import com.businessanalyzer.model.DatosActivo;
import com.businessanalyzer.model.DatosCuenta;
import com.businessanalyzer.model.DatosDeuda;
import com.businessanalyzer.model.DatosMovimiento;
import com.businessanalyzer.model.DatosMovimientoTesoreria;
import com.businessanalyzer.model.DatosOperacion;
import com.businessanalyzer.model.DatosOperacion.DatosLinea;
import com.businessanalyzer.model.DatosProducto;
import com.businessanalyzer.model.Deuda;
import com.businessanalyzer.model.MovimientoFinanciero;
import com.businessanalyzer.model.Operacion;
import com.businessanalyzer.model.PagoDeuda;
import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.TipoCuenta;
import com.businessanalyzer.model.TipoItem;
import com.businessanalyzer.model.TipoMovimiento;
import com.businessanalyzer.model.TipoMovimientoTesoreria;
import com.businessanalyzer.model.TipoOperacion;
import com.businessanalyzer.model.UnidadMedida;
import com.businessanalyzer.model.Usuario;
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

class TesoreriaServiceTest {

    /** El reloj de pruebas está en 2026-01-15. */
    private static final LocalDate HOY = LocalDate.of(2026, 1, 15);

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private TesoreriaService servicio;
    private CuentaDinero caja;
    private CuentaDinero banco;
    private Producto arroz;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = entorno.tesoreria;
        entorno.administradorConectado();
        entorno.crearEmpresaActiva("Comercial Andina");
        caja = servicio.cuentasActivas().get(0);
        servicio.actualizarCuenta(caja.id(), new DatosCuenta("Caja", TipoCuenta.CAJA, null, new BigDecimal("1000")));
        banco = servicio.registrarCuenta(new DatosCuenta("BCP", TipoCuenta.BANCO, "193-123", new BigDecimal("5000")));
        arroz = entorno.catalogo.crearProducto(new DatosProducto(TipoItem.PRODUCTO, null, "Arroz 5 kg", null, null,
                UnidadMedida.UNIDAD, new BigDecimal("20"), new BigDecimal("25")));
    }

    @Test
    void cadaEmpresaNuevaEmpiezaConSuCaja() {
        assertEquals(EmpresaService.CUENTA_INICIAL, caja.nombre());
        assertEquals(TipoCuenta.CAJA, caja.tipo());
        assertEquals(2, servicio.cuentasActivas().size());
        assertInvalido(() -> servicio.registrarCuenta(new DatosCuenta("bcp", TipoCuenta.BANCO, null, null)));
    }

    @Test
    void lasVentasYComprasAlContadoMuevenSusCuentas() {
        // Compra de 10 × 20 = 200 + IGV 36 desde el banco; venta de 4 × 25 = 100 + IGV 18 a la caja.
        compra(CondicionPago.CONTADO, banco.id(), "10", null);
        Operacion venta = venta(CondicionPago.CONTADO, null, "4", null);

        assertEquals(caja.id(), venta.cuentaId(), "Sin cuenta indicada, la venta entra a la cuenta principal");
        igual("1118", saldo(caja));
        igual("4764", saldo(banco));
        igual("5882", entorno.patrimonio.situacion().balance().efectivo());
    }

    @Test
    void unaVentaAlCreditoQuedaPorCobrarHastaQueSeCobra() {
        compra(CondicionPago.CONTADO, banco.id(), "10", null);
        Operacion venta = venta(CondicionPago.CREDITO, null, "4", HOY.plusDays(15));

        assertTrue(venta.alCredito());
        assertEquals(null, venta.cuentaId());
        igual("118", venta.pendiente());
        igual("1000", saldo(caja));
        assertEquals(1, servicio.pendientes(TipoOperacion.VENTA).size());
        igual("118", entorno.patrimonio.situacion().balance().porCobrar());

        assertInvalido(() -> servicio.registrarPago(venta.id(), caja.id(), HOY, new BigDecimal("200"), null));
        assertInvalido(() -> servicio.registrarPago(venta.id(), caja.id(), HOY.minusDays(1), new BigDecimal("50"), null));
        Operacion parcial = servicio.registrarPago(venta.id(), caja.id(), HOY, new BigDecimal("50"), "Adelanto");
        igual("68", parcial.pendiente());
        igual("1050", saldo(caja));

        // Con cobros registrados la venta no se anula ni se corrige directamente.
        assertInvalido(() -> entorno.operaciones.anular(venta.id(), "Error de registro"));
        Operacion completa = servicio.registrarPago(venta.id(), banco.id(), HOY, new BigDecimal("68"), null);
        igual("0", completa.pendiente());
        assertTrue(servicio.pendientes(TipoOperacion.VENTA).isEmpty());

        // Al anular un cobro, lo pendiente vuelve.
        long cobro = servicio.listarPagos(venta.id()).get(0).id();
        igual("68", servicio.anularPago(cobro).pendiente());
    }

    @Test
    void unaVentaAlCreditoNecesitaElCliente() {
        assertInvalido(() -> entorno.operaciones.registrar(new DatosOperacion(TipoOperacion.VENTA, HOY, null, null,
                null, new BigDecimal("18"), null, List.of(linea("1")), false, CondicionPago.CREDITO, null, null), true));
        assertInvalido(() -> entorno.operaciones.registrar(new DatosOperacion(TipoOperacion.VENTA, HOY, "Juan",
                null, null, new BigDecimal("18"), null, List.of(linea("1")), false, CondicionPago.CREDITO, null,
                HOY.minusDays(1)), true));
    }

    @Test
    void lasTransferenciasNoCambianElTotalYLosRetirosSi() {
        servicio.registrarMovimiento(new DatosMovimientoTesoreria(TipoMovimientoTesoreria.TRANSFERENCIA, HOY,
                caja.id(), banco.id(), new BigDecimal("600"), null));
        igual("400", saldo(caja));
        igual("5600", saldo(banco));
        igual("6000", SaldoCuenta.total(servicio.listarCuentas(false)));

        servicio.registrarMovimiento(new DatosMovimientoTesoreria(TipoMovimientoTesoreria.RETIRO, HOY,
                banco.id(), null, new BigDecimal("500"), null));
        servicio.registrarMovimiento(new DatosMovimientoTesoreria(TipoMovimientoTesoreria.APORTE, HOY,
                null, caja.id(), new BigDecimal("200"), "Aporte para el vuelto"));
        igual("5700", SaldoCuenta.total(servicio.listarCuentas(false)));

        FlujoEfectivo enero = servicio.flujoEfectivo(HOY.withDayOfMonth(1), HOY);
        igual("-500", enero.rubro(RubroFlujo.RETIROS));
        igual("200", enero.rubro(RubroFlujo.APORTES));
        igual("0", enero.rubro(RubroFlujo.TRANSFERENCIAS));
        igual("-300", enero.neto(Actividad.FINANCIAMIENTO));

        assertInvalido(() -> servicio.registrarMovimiento(new DatosMovimientoTesoreria(
                TipoMovimientoTesoreria.TRANSFERENCIA, HOY, caja.id(), caja.id(), BigDecimal.TEN, null)));
        assertInvalido(() -> servicio.registrarMovimiento(new DatosMovimientoTesoreria(
                TipoMovimientoTesoreria.RETIRO, HOY.plusDays(1), caja.id(), null, BigDecimal.TEN, null)));
        long retiro = servicio.listarMovimientos(HOY, HOY, false).stream()
                .filter(m -> m.tipo() == TipoMovimientoTesoreria.RETIRO).findFirst().orElseThrow().id();
        servicio.anularMovimiento(retiro, "Retiro registrado dos veces");
        igual("6200", SaldoCuenta.total(servicio.listarCuentas(false)));
    }

    @Test
    void unaCuentaConSaldoNoSeDesactiva() {
        assertInvalido(() -> servicio.cambiarCuentaActiva(banco.id(), false));
        servicio.registrarMovimiento(new DatosMovimientoTesoreria(TipoMovimientoTesoreria.TRANSFERENCIA, HOY,
                banco.id(), caja.id(), new BigDecimal("5000"), null));
        assertFalse(servicio.cambiarCuentaActiva(banco.id(), false).activa());
        // La caja queda como única cuenta activa y no se puede desactivar.
        servicio.registrarMovimiento(new DatosMovimientoTesoreria(TipoMovimientoTesoreria.RETIRO, HOY,
                caja.id(), null, new BigDecimal("6000"), null));
        assertInvalido(() -> servicio.cambiarCuentaActiva(caja.id(), false));
        // No se registran movimientos en una cuenta desactivada.
        assertInvalido(() -> entorno.finanzas.registrar(gasto("100", banco.id())));
    }

    @Test
    void elPagoDeUnaCuotaSeparaCapitalEInteres() {
        Deuda prestamo = entorno.patrimonio.registrarDeuda(new DatosDeuda(CategoriaDeuda.PRESTAMO_BANCARIO, "Caja Huancayo",
                "Capital de trabajo", HOY.minusDays(10), new BigDecimal("3000"), new BigDecimal("24"),
                new BigDecimal("300"), null, banco.id()));
        igual("8000", saldo(banco));
        igual("60", PatrimonioService.interesSugerido(prestamo));   // 3 000 × 24 % / 12

        Deuda despues = entorno.patrimonio.registrarPago(prestamo.id(), HOY, new BigDecimal("240"),
                new BigDecimal("60"), banco.id(), "Cuota 1");
        igual("2760", despues.saldo());
        igual("7700", saldo(banco));

        // El interés quedó como gasto financiero (entra en el estado de resultados) y no se edita aparte.
        MovimientoFinanciero interes = entorno.finanzas.listar(HOY, HOY, false).get(0);
        assertEquals(CategoriaFinanciera.INTERESES_PRESTAMOS, interes.categoria());
        assertTrue(interes.dePagoDeuda());
        igual("60", interes.monto());
        assertInvalido(() -> entorno.finanzas.anular(interes.id(), "No corresponde"));

        FlujoEfectivo enero = servicio.flujoEfectivo(HOY.withDayOfMonth(1), HOY);
        igual("3000", enero.rubro(RubroFlujo.PRESTAMOS));
        igual("-240", enero.rubro(RubroFlujo.PAGO_DEUDAS));
        igual("-60", enero.rubro(RubroFlujo.INTERESES));

        // Al anular el pago se anula también su interés y el dinero vuelve a la cuenta.
        PagoDeuda pago = entorno.patrimonio.listarPagos(prestamo.id()).get(0);
        igual("300", pago.total());
        entorno.patrimonio.anularPago(pago.id());
        assertTrue(entorno.finanzas.listar(HOY, HOY, false).isEmpty());
        igual("8000", saldo(banco));
    }

    @Test
    void laCompraDeUnActivoSaleDeLaCuentaElegida() {
        entorno.patrimonio.registrarActivo(new DatosActivo(CategoriaActivo.EQUIPOS_COMPUTO, "Laptop", HOY,
                new BigDecimal("2500"), 4, null, banco.id()));
        entorno.patrimonio.registrarActivo(new DatosActivo(CategoriaActivo.MOBILIARIO, "Vitrina heredada", HOY,
                new BigDecimal("800"), 10, null));
        igual("2500", saldo(banco));
        igual("-2500", servicio.flujoEfectivo(HOY, HOY).neto(Actividad.INVERSION));
    }

    @Test
    void losIngresosYGastosUsanLaCuentaIndicada() {
        entorno.finanzas.registrar(gasto("150", banco.id()));
        entorno.finanzas.registrar(new DatosMovimiento(TipoMovimiento.INGRESO, CategoriaFinanciera.ALQUILERES_COBRADOS,
                null, HOY, "Alquiler de cochera", new BigDecimal("200"), null));
        igual("4850", saldo(banco));
        igual("1200", saldo(caja));
    }

    @Test
    void respetaLosPermisosYLaEmpresaActiva() {
        Usuario analista = entorno.crearUsuario("analista", "ANALISTA");
        entorno.empresaService.asignarAccesos(entorno.sesion.empresaActiva().orElseThrow().id(), Set.of(analista.id()));
        entorno.entrarComo("analista", "Temporal123");
        entorno.empresaService.restaurarEmpresaActiva();

        assertEquals(2, servicio.listarCuentas(false).size());
        assertThrows(AccesoDenegadoException.class,
                () -> servicio.registrarCuenta(new DatosCuenta("Yape", TipoCuenta.BILLETERA, null, null)));
        assertThrows(AccesoDenegadoException.class, () -> servicio.registrarMovimiento(new DatosMovimientoTesoreria(
                TipoMovimientoTesoreria.APORTE, HOY, null, caja.id(), BigDecimal.TEN, null)));
    }

    @Test
    void conPocosMesesNoHayProyeccion() {
        venta(CondicionPago.CONTADO, caja.id(), "1", null);
        assertFalse(servicio.proyeccion().suficiente());
        igual("6029.50", servicio.proyeccion().saldoActual());   // caja 1 029,50 + banco 5 000
    }

    // ------------------------------------------------------------------ utilidades

    private Operacion venta(CondicionPago condicion, Long cuenta, String cantidad, LocalDate vence) {
        return entorno.operaciones.registrar(new DatosOperacion(TipoOperacion.VENTA, HOY, "Restaurante Sol", null, null,
                new BigDecimal("18"), null, List.of(new DatosLinea(arroz.id(), new BigDecimal(cantidad),
                new BigDecimal("25"))), false, condicion, cuenta, vence), true);
    }

    private Operacion compra(CondicionPago condicion, Long cuenta, String cantidad, LocalDate vence) {
        return entorno.operaciones.registrar(new DatosOperacion(TipoOperacion.COMPRA, HOY, "Molino Norte", null, null,
                new BigDecimal("18"), null, List.of(new DatosLinea(arroz.id(), new BigDecimal(cantidad),
                new BigDecimal("20"))), false, condicion, cuenta, vence), true);
    }

    private DatosLinea linea(String cantidad) {
        return new DatosLinea(arroz.id(), new BigDecimal(cantidad), new BigDecimal("25"));
    }

    private static DatosMovimiento gasto(String monto, Long cuenta) {
        return new DatosMovimiento(TipoMovimiento.GASTO, CategoriaFinanciera.SERVICIOS_BASICOS, null, HOY,
                "Luz del local", new BigDecimal(monto), null, cuenta);
    }

    private BigDecimal saldo(CuentaDinero cuenta) {
        return servicio.listarCuentas(true).stream().filter(s -> s.cuenta().id() == cuenta.id())
                .findFirst().orElseThrow().saldo();
    }

    private static void assertInvalido(Executable accion) {
        assertThrows(ValidacionException.class, accion);
    }

    private static void igual(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "Esperado " + esperado + " pero fue " + real);
    }
}
