package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneralFGEM;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonialFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.ActivoFGEM;
import com.businessanalyzer.model.CategoriaActivoFGEM;
import com.businessanalyzer.model.CategoriaDeudaFGEM;
import com.businessanalyzer.model.CuentaDineroFGEM;
import com.businessanalyzer.model.DatosCuentaFGEM;
import com.businessanalyzer.model.TipoCuentaFGEM;
import com.businessanalyzer.model.DatosActivoFGEM;
import com.businessanalyzer.model.DatosDeudaFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM.DatosLineaFGEM;
import com.businessanalyzer.model.DatosProductoFGEM;
import com.businessanalyzer.model.DeudaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.PagoDeudaFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatrimonioServiceTestFGEM {

    /** El reloj de pruebas está en 2026-01-15. */
    private static final LocalDate HOY_FGEM = LocalDate.of(2026, 1, 15);

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private PatrimonioServiceFGEM servicioFGEM;
    private EmpresaFGEM empresaFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = entornoFGEM.patrimonioFGEM;
        entornoFGEM.administradorConectadoFGEM();
        empresaFGEM = entornoFGEM.crearEmpresaActivaFGEM("Ferretería El Clavo");
    }

    @Test
    void elBalanceIncluyeActivosInventarioYDeudasFGEM() {
        // La caja empieza con 8 540; la compra de 3 540 (con IGV) sale de ella y quedan 5 000.
        CuentaDineroFGEM cajaFGEM = entornoFGEM.tesoreriaFGEM.cuentasActivasFGEM().get(0);
        entornoFGEM.tesoreriaFGEM.actualizarCuentaFGEM(cajaFGEM.idFGEM(), new DatosCuentaFGEM("Caja", TipoCuentaFGEM.CAJA, null, new BigDecimal("8540")));
        activoFGEM(CategoriaActivoFGEM.MAQUINARIA, "Cortadora", LocalDate.of(2025, 1, 15), "12000", 10, null);
        ProductoFGEM clavosFGEM = entornoFGEM.catalogoFGEM.crearProductoFGEM(new DatosProductoFGEM(TipoItemFGEM.PRODUCTO, null, "Clavos 1 kg",
                null, null, UnidadMedidaFGEM.UNIDAD, new BigDecimal("6"), new BigDecimal("9")));
        entornoFGEM.operacionesFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.COMPRA, HOY_FGEM, null, null, null,
                new BigDecimal("18"), null, List.of(new DatosLineaFGEM(clavosFGEM.idFGEM(), new BigDecimal("500"),
                new BigDecimal("6"))), false), false);
        servicioFGEM.registrarDeudaFGEM(deudaFGEM(CategoriaDeudaFGEM.PROVEEDORES, "Aceros SAC", HOY_FGEM, "2000", null, null));

        SituacionPatrimonialFGEM sFGEM = servicioFGEM.situacionFGEM();
        BalanceGeneralFGEM bFGEM = sFGEM.balanceFGEM();

        igualFGEM("3000", bFGEM.inventarioFGEM());              // 500 × 6
        igualFGEM("8000", bFGEM.totalActivoCorrienteFGEM());
        igualFGEM("10800", bFGEM.totalActivoNoCorrienteFGEM()); // 12 meses de depreciación
        igualFGEM("2000", bFGEM.totalPasivoFGEM());
        igualFGEM("16800", bFGEM.patrimonioFGEM());
        igualFGEM("4", sFGEM.indicadoresFGEM().liquidezCorrienteFGEM());
        assertEquals(HOY_FGEM, bFGEM.corteFGEM());
        assertNull(sFGEM.periodoResultadosFGEM());
    }

    @Test
    void validaLosActivosFGEM() {
        assertInvalidoFGEM(() -> activoFGEM(CategoriaActivoFGEM.CUENTAS_POR_COBRAR, "Préstamo a un tercero", HOY_FGEM.plusDays(1), "10", null, null));
        assertInvalidoFGEM(() -> activoFGEM(CategoriaActivoFGEM.VEHICULOS, "Moto", HOY_FGEM, "0", 5, null));
        assertInvalidoFGEM(() -> activoFGEM(CategoriaActivoFGEM.VEHICULOS, "Moto", HOY_FGEM, "5000", 0, null));
        assertInvalidoFGEM(() -> activoFGEM(CategoriaActivoFGEM.VEHICULOS, "Moto", HOY_FGEM, "5000", 5, "6000"));
        assertInvalidoFGEM(() -> activoFGEM(null, "Moto", HOY_FGEM, "5000", 5, null));

        // En los activos corrientes y terrenos se ignoran la vida útil y el valor residual.
        ActivoFGEM cajaFGEM = activoFGEM(CategoriaActivoFGEM.CUENTAS_POR_COBRAR, "Préstamo a un tercero", HOY_FGEM, "0", 5, "10");
        // El efectivo ya no es un activo escrito a mano: se registra en «Tesorería».
        assertInvalidoFGEM(() -> activoFGEM(CategoriaActivoFGEM.CAJA_BANCOS, "Caja", HOY_FGEM, "100", null, null));
        assertNull(cajaFGEM.vidaUtilAniosFGEM());
        igualFGEM("0", cajaFGEM.valorResidualFGEM());
        assertNull(activoFGEM(CategoriaActivoFGEM.TERRENOS, "Lote", HOY_FGEM, "50000", 20, null).vidaUtilAniosFGEM());
    }

    @Test
    void unActivoDadoDeBajaSaleDelBalanceYNoSePuedeEditarFGEM() {
        ActivoFGEM motoFGEM = activoFGEM(CategoriaActivoFGEM.VEHICULOS, "Moto", LocalDate.of(2025, 6, 1), "5000", 5, null);
        assertInvalidoFGEM(() -> servicioFGEM.darDeBajaFGEM(motoFGEM.idFGEM(), LocalDate.of(2025, 5, 1), "Vendida a un vecino"));

        ActivoFGEM bajaFGEM = servicioFGEM.darDeBajaFGEM(motoFGEM.idFGEM(), HOY_FGEM, "Vendida a un vecino");

        assertTrue(bajaFGEM.dadoDeBajaFGEM());
        assertEquals("Vendida a un vecino", bajaFGEM.motivoBajaFGEM());
        assertTrue(servicioFGEM.situacionFGEM().balanceFGEM().sinRegistrosFGEM());
        assertEquals(0, servicioFGEM.listarActivosFGEM(false).size());
        assertEquals(1, servicioFGEM.listarActivosFGEM(true).size());
        assertInvalidoFGEM(() -> servicioFGEM.actualizarActivoFGEM(motoFGEM.idFGEM(), new DatosActivoFGEM(CategoriaActivoFGEM.VEHICULOS, "Moto",
                HOY_FGEM, new BigDecimal("1"), 5, null)));
        assertInvalidoFGEM(() -> servicioFGEM.darDeBajaFGEM(motoFGEM.idFGEM(), HOY_FGEM, "Otra vez"));
    }

    @Test
    void losPagosReducenElSaldoHastaCancelarLaDeudaFGEM() {
        DeudaFGEM prestamoFGEM = servicioFGEM.registrarDeudaFGEM(deudaFGEM(CategoriaDeudaFGEM.PRESTAMO_BANCARIO, "Caja Huancayo",
                LocalDate.of(2025, 10, 1), "3000", "450", LocalDate.of(2026, 6, 1)));

        DeudaFGEM conPagoFGEM = servicioFGEM.registrarPagoFGEM(prestamoFGEM.idFGEM(), LocalDate.of(2025, 11, 1), new BigDecimal("1000"), "Cuota 1");
        igualFGEM("1000", conPagoFGEM.pagadoFGEM());
        igualFGEM("2000", conPagoFGEM.saldoFGEM());

        assertInvalidoFGEM(() -> servicioFGEM.registrarPagoFGEM(prestamoFGEM.idFGEM(), HOY_FGEM, new BigDecimal("2500"), null));      // supera el saldo
        assertInvalidoFGEM(() -> servicioFGEM.registrarPagoFGEM(prestamoFGEM.idFGEM(), LocalDate.of(2025, 9, 1), BigDecimal.ONE, null)); // antes del inicio
        assertInvalidoFGEM(() -> servicioFGEM.registrarPagoFGEM(prestamoFGEM.idFGEM(), HOY_FGEM.plusDays(1), BigDecimal.ONE, null));  // futuro
        assertInvalidoFGEM(() -> servicioFGEM.actualizarDeudaFGEM(prestamoFGEM.idFGEM(), deudaFGEM(CategoriaDeudaFGEM.PRESTAMO_BANCARIO,
                "Caja Huancayo", LocalDate.of(2025, 10, 1), "900", null, null)));                             // menor que lo pagado
        assertInvalidoFGEM(() -> servicioFGEM.actualizarDeudaFGEM(prestamoFGEM.idFGEM(), deudaFGEM(CategoriaDeudaFGEM.PRESTAMO_BANCARIO,
                "Caja Huancayo", LocalDate.of(2025, 12, 1), "3000", null, null)));                            // inicio después del pago

        DeudaFGEM canceladaFGEM = servicioFGEM.registrarPagoFGEM(prestamoFGEM.idFGEM(), HOY_FGEM, new BigDecimal("2000"), null);
        assertTrue(canceladaFGEM.canceladaFGEM());
        assertEquals("Cancelada", canceladaFGEM.estadoTextoFGEM());
        assertInvalidoFGEM(() -> servicioFGEM.registrarPagoFGEM(prestamoFGEM.idFGEM(), HOY_FGEM, BigDecimal.ONE, null));
        // Sin deudas ni activos registrados, pero los pagos salieron de la caja de tesorería: el balance lo refleja.
        var balanceFGEM = servicioFGEM.situacionFGEM().balanceFGEM();
        assertEquals(0, balanceFGEM.deudasPendientesFGEM());
        igualFGEM("-3000", balanceFGEM.efectivoFGEM());
        assertFalse(balanceFGEM.sinRegistrosFGEM());

        // Anular un pago devuelve el saldo.
        PagoDeudaFGEM ultimoFGEM = servicioFGEM.listarPagosFGEM(prestamoFGEM.idFGEM()).get(0);
        DeudaFGEM reabiertaFGEM = servicioFGEM.anularPagoFGEM(ultimoFGEM.idFGEM());
        igualFGEM("2000", reabiertaFGEM.saldoFGEM());
        assertTrue(servicioFGEM.listarPagosFGEM(prestamoFGEM.idFGEM()).get(0).anuladoFGEM());
        assertInvalidoFGEM(() -> servicioFGEM.anularPagoFGEM(ultimoFGEM.idFGEM()));
    }

    @Test
    void separaLaDeudaDeCortoYLargoPlazoSegunElVencimientoFGEM() {
        servicioFGEM.registrarDeudaFGEM(deudaFGEM(CategoriaDeudaFGEM.PRESTAMO_BANCARIO, "Banco", HOY_FGEM, "24000", "800", HOY_FGEM.plusMonths(24)));
        BalanceGeneralFGEM bFGEM = servicioFGEM.situacionFGEM().balanceFGEM();
        igualFGEM("12000", bFGEM.totalPasivoCorrienteFGEM());
        igualFGEM("12000", bFGEM.totalPasivoNoCorrienteFGEM());
        igualFGEM("800", bFGEM.cuotasMensualesFGEM());
    }

    @Test
    void validaLasDeudasFGEM() {
        assertInvalidoFGEM(() -> servicioFGEM.registrarDeudaFGEM(deudaFGEM(null, "Banco", HOY_FGEM, "100", null, null)));
        assertInvalidoFGEM(() -> servicioFGEM.registrarDeudaFGEM(deudaFGEM(CategoriaDeudaFGEM.OTRAS, " ", HOY_FGEM, "100", null, null)));
        assertInvalidoFGEM(() -> servicioFGEM.registrarDeudaFGEM(deudaFGEM(CategoriaDeudaFGEM.OTRAS, "Banco", HOY_FGEM, "0", null, null)));
        assertInvalidoFGEM(() -> servicioFGEM.registrarDeudaFGEM(deudaFGEM(CategoriaDeudaFGEM.OTRAS, "Banco", HOY_FGEM, "100", null, HOY_FGEM)));
        assertInvalidoFGEM(() -> servicioFGEM.registrarDeudaFGEM(new DatosDeudaFGEM(CategoriaDeudaFGEM.OTRAS, "Banco", null, HOY_FGEM,
                new BigDecimal("100"), new BigDecimal("500"), null, null)));
    }

    @Test
    void unaDeudaAnuladaNoCuentaNiAdmitePagosFGEM() {
        DeudaFGEM dFGEM = servicioFGEM.registrarDeudaFGEM(deudaFGEM(CategoriaDeudaFGEM.TRIBUTOS, "SUNAT", HOY_FGEM, "540", null, null));
        assertInvalidoFGEM(() -> servicioFGEM.anularDeudaFGEM(dFGEM.idFGEM(), "x"));

        DeudaFGEM anuladaFGEM = servicioFGEM.anularDeudaFGEM(dFGEM.idFGEM(), "Registrada dos veces");

        assertTrue(anuladaFGEM.anuladaFGEM());
        assertEquals(0, servicioFGEM.listarDeudasFGEM(false).size());
        assertEquals(1, servicioFGEM.listarDeudasFGEM(true).size());
        assertTrue(servicioFGEM.situacionFGEM().balanceFGEM().sinRegistrosFGEM());
        assertInvalidoFGEM(() -> servicioFGEM.registrarPagoFGEM(dFGEM.idFGEM(), HOY_FGEM, BigDecimal.ONE, null));
    }

    @Test
    void losDatosSonDeLaEmpresaActivaFGEM() {
        DeudaFGEM dFGEM = servicioFGEM.registrarDeudaFGEM(deudaFGEM(CategoriaDeudaFGEM.OTRAS, "Banco", HOY_FGEM, "100", null, null));
        ActivoFGEM aFGEM = activoFGEM(CategoriaActivoFGEM.CUENTAS_POR_COBRAR, "Adelanto a proveedor", HOY_FGEM, "50", null, null);
        entornoFGEM.crearEmpresaActivaFGEM("Otra empresa");

        assertEquals(0, servicioFGEM.listarDeudasFGEM(true).size());
        assertEquals(0, servicioFGEM.listarActivosFGEM(true).size());
        assertInvalidoFGEM(() -> servicioFGEM.registrarPagoFGEM(dFGEM.idFGEM(), HOY_FGEM, BigDecimal.ONE, null));
        assertInvalidoFGEM(() -> servicioFGEM.darDeBajaFGEM(aFGEM.idFGEM(), HOY_FGEM, "No es mío"));
        assertInvalidoFGEM(() -> servicioFGEM.listarPagosFGEM(dFGEM.idFGEM()));
    }

    @Test
    void respetaLosPermisosFGEM() {
        activoFGEM(CategoriaActivoFGEM.CUENTAS_POR_COBRAR, "Adelanto a proveedor", HOY_FGEM, "50", null, null);
        UsuarioFGEM analistaFGEM = entornoFGEM.crearUsuarioFGEM("analista", "ANALISTA");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of(analistaFGEM.idFGEM()));
        entornoFGEM.entrarComoFGEM("analista", "Temporal123");
        entornoFGEM.empresaServiceFGEM.restaurarEmpresaActivaFGEM();

        assertEquals(1, servicioFGEM.listarActivosFGEM(false).size());
        igualFGEM("50", servicioFGEM.situacionFGEM().balanceFGEM().totalActivoFGEM());
        assertThrows(AccesoDenegadoExceptionFGEM.class,
                () -> activoFGEM(CategoriaActivoFGEM.CUENTAS_POR_COBRAR, "Adelanto a proveedor 2", HOY_FGEM, "50", null, null));
        assertThrows(AccesoDenegadoExceptionFGEM.class,
                () -> servicioFGEM.registrarDeudaFGEM(deudaFGEM(CategoriaDeudaFGEM.OTRAS, "Banco", HOY_FGEM, "100", null, null)));
    }

    // ------------------------------------------------------------------ utilidades

    private ActivoFGEM activoFGEM(CategoriaActivoFGEM categoriaFGEM, String descripcionFGEM, LocalDate fechaFGEM, String valorFGEM, Integer vidaFGEM,
                          String residualFGEM) {
        return servicioFGEM.registrarActivoFGEM(new DatosActivoFGEM(categoriaFGEM, descripcionFGEM, fechaFGEM, new BigDecimal(valorFGEM), vidaFGEM,
                residualFGEM == null ? null : new BigDecimal(residualFGEM)));
    }

    private static DatosDeudaFGEM deudaFGEM(CategoriaDeudaFGEM categoriaFGEM, String acreedorFGEM, LocalDate fechaFGEM, String montoFGEM,
                                    String cuotaFGEM, LocalDate vencimientoFGEM) {
        return new DatosDeudaFGEM(categoriaFGEM, acreedorFGEM, null, fechaFGEM, new BigDecimal(montoFGEM), null,
                cuotaFGEM == null ? null : new BigDecimal(cuotaFGEM), vencimientoFGEM);
    }

    private static void assertInvalidoFGEM(Runnable accionFGEM) {
        assertThrows(ValidacionExceptionFGEM.class, accionFGEM::run);
    }

    private static void igualFGEM(String esperadoFGEM, BigDecimal realFGEM) {
        assertEquals(0, new BigDecimal(esperadoFGEM).compareTo(realFGEM), "esperado " + esperadoFGEM + " pero fue " + realFGEM);
    }
}
