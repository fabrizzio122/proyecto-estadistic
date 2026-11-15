package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneral;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonial;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Activo;
import com.businessanalyzer.model.CategoriaActivo;
import com.businessanalyzer.model.CategoriaDeuda;
import com.businessanalyzer.model.DatosActivo;
import com.businessanalyzer.model.DatosDeuda;
import com.businessanalyzer.model.DatosOperacion;
import com.businessanalyzer.model.DatosOperacion.DatosLinea;
import com.businessanalyzer.model.DatosProducto;
import com.businessanalyzer.model.Deuda;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.PagoDeuda;
import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.TipoItem;
import com.businessanalyzer.model.TipoOperacion;
import com.businessanalyzer.model.UnidadMedida;
import com.businessanalyzer.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatrimonioServiceTest {

    /** El reloj de pruebas está en 2026-01-15. */
    private static final LocalDate HOY = LocalDate.of(2026, 1, 15);

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private PatrimonioService servicio;
    private Empresa empresa;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = entorno.patrimonio;
        entorno.administradorConectado();
        empresa = entorno.crearEmpresaActiva("Ferretería El Clavo");
    }

    @Test
    void elBalanceIncluyeActivosInventarioYDeudas() {
        activo(CategoriaActivo.CAJA_BANCOS, "Caja", HOY, "5000", null, null);
        activo(CategoriaActivo.MAQUINARIA, "Cortadora", LocalDate.of(2025, 1, 15), "12000", 10, null);
        Producto clavos = entorno.catalogo.crearProducto(new DatosProducto(TipoItem.PRODUCTO, null, "Clavos 1 kg",
                null, null, UnidadMedida.UNIDAD, new BigDecimal("6"), new BigDecimal("9")));
        entorno.operaciones.registrar(new DatosOperacion(TipoOperacion.COMPRA, HOY, null, null, null,
                new BigDecimal("18"), null, List.of(new DatosLinea(clavos.id(), new BigDecimal("500"),
                new BigDecimal("6"))), false), false);
        servicio.registrarDeuda(deuda(CategoriaDeuda.PROVEEDORES, "Aceros SAC", HOY, "2000", null, null));

        SituacionPatrimonial s = servicio.situacion();
        BalanceGeneral b = s.balance();

        igual("3000", b.inventario());              // 500 × 6
        igual("8000", b.totalActivoCorriente());
        igual("10800", b.totalActivoNoCorriente()); // 12 meses de depreciación
        igual("2000", b.totalPasivo());
        igual("16800", b.patrimonio());
        igual("4", s.indicadores().liquidezCorriente());
        assertEquals(HOY, b.corte());
        assertNull(s.periodoResultados());
    }

    @Test
    void validaLosActivos() {
        assertInvalido(() -> activo(CategoriaActivo.CAJA_BANCOS, "Caja", HOY.plusDays(1), "10", null, null));
        assertInvalido(() -> activo(CategoriaActivo.VEHICULOS, "Moto", HOY, "0", 5, null));
        assertInvalido(() -> activo(CategoriaActivo.VEHICULOS, "Moto", HOY, "5000", 0, null));
        assertInvalido(() -> activo(CategoriaActivo.VEHICULOS, "Moto", HOY, "5000", 5, "6000"));
        assertInvalido(() -> activo(null, "Moto", HOY, "5000", 5, null));

        // En los activos corrientes y terrenos se ignoran la vida útil y el valor residual.
        Activo caja = activo(CategoriaActivo.CAJA_BANCOS, "Caja", HOY, "0", 5, "10");
        assertNull(caja.vidaUtilAnios());
        igual("0", caja.valorResidual());
        assertNull(activo(CategoriaActivo.TERRENOS, "Lote", HOY, "50000", 20, null).vidaUtilAnios());
    }

    @Test
    void unActivoDadoDeBajaSaleDelBalanceYNoSePuedeEditar() {
        Activo moto = activo(CategoriaActivo.VEHICULOS, "Moto", LocalDate.of(2025, 6, 1), "5000", 5, null);
        assertInvalido(() -> servicio.darDeBaja(moto.id(), LocalDate.of(2025, 5, 1), "Vendida a un vecino"));

        Activo baja = servicio.darDeBaja(moto.id(), HOY, "Vendida a un vecino");

        assertTrue(baja.dadoDeBaja());
        assertEquals("Vendida a un vecino", baja.motivoBaja());
        assertTrue(servicio.situacion().balance().sinRegistros());
        assertEquals(0, servicio.listarActivos(false).size());
        assertEquals(1, servicio.listarActivos(true).size());
        assertInvalido(() -> servicio.actualizarActivo(moto.id(), new DatosActivo(CategoriaActivo.VEHICULOS, "Moto",
                HOY, new BigDecimal("1"), 5, null)));
        assertInvalido(() -> servicio.darDeBaja(moto.id(), HOY, "Otra vez"));
    }

    @Test
    void losPagosReducenElSaldoHastaCancelarLaDeuda() {
        Deuda prestamo = servicio.registrarDeuda(deuda(CategoriaDeuda.PRESTAMO_BANCARIO, "Caja Huancayo",
                LocalDate.of(2025, 10, 1), "3000", "450", LocalDate.of(2026, 6, 1)));

        Deuda conPago = servicio.registrarPago(prestamo.id(), LocalDate.of(2025, 11, 1), new BigDecimal("1000"), "Cuota 1");
        igual("1000", conPago.pagado());
        igual("2000", conPago.saldo());

        assertInvalido(() -> servicio.registrarPago(prestamo.id(), HOY, new BigDecimal("2500"), null));      // supera el saldo
        assertInvalido(() -> servicio.registrarPago(prestamo.id(), LocalDate.of(2025, 9, 1), BigDecimal.ONE, null)); // antes del inicio
        assertInvalido(() -> servicio.registrarPago(prestamo.id(), HOY.plusDays(1), BigDecimal.ONE, null));  // futuro
        assertInvalido(() -> servicio.actualizarDeuda(prestamo.id(), deuda(CategoriaDeuda.PRESTAMO_BANCARIO,
                "Caja Huancayo", LocalDate.of(2025, 10, 1), "900", null, null)));                             // menor que lo pagado
        assertInvalido(() -> servicio.actualizarDeuda(prestamo.id(), deuda(CategoriaDeuda.PRESTAMO_BANCARIO,
                "Caja Huancayo", LocalDate.of(2025, 12, 1), "3000", null, null)));                            // inicio después del pago

        Deuda cancelada = servicio.registrarPago(prestamo.id(), HOY, new BigDecimal("2000"), null);
        assertTrue(cancelada.cancelada());
        assertEquals("Cancelada", cancelada.estadoTexto());
        assertInvalido(() -> servicio.registrarPago(prestamo.id(), HOY, BigDecimal.ONE, null));
        assertTrue(servicio.situacion().balance().sinRegistros());

        // Anular un pago devuelve el saldo.
        PagoDeuda ultimo = servicio.listarPagos(prestamo.id()).get(0);
        Deuda reabierta = servicio.anularPago(ultimo.id());
        igual("2000", reabierta.saldo());
        assertTrue(servicio.listarPagos(prestamo.id()).get(0).anulado());
        assertInvalido(() -> servicio.anularPago(ultimo.id()));
    }

    @Test
    void separaLaDeudaDeCortoYLargoPlazoSegunElVencimiento() {
        servicio.registrarDeuda(deuda(CategoriaDeuda.PRESTAMO_BANCARIO, "Banco", HOY, "24000", "800", HOY.plusMonths(24)));
        BalanceGeneral b = servicio.situacion().balance();
        igual("12000", b.totalPasivoCorriente());
        igual("12000", b.totalPasivoNoCorriente());
        igual("800", b.cuotasMensuales());
    }

    @Test
    void validaLasDeudas() {
        assertInvalido(() -> servicio.registrarDeuda(deuda(null, "Banco", HOY, "100", null, null)));
        assertInvalido(() -> servicio.registrarDeuda(deuda(CategoriaDeuda.OTRAS, " ", HOY, "100", null, null)));
        assertInvalido(() -> servicio.registrarDeuda(deuda(CategoriaDeuda.OTRAS, "Banco", HOY, "0", null, null)));
        assertInvalido(() -> servicio.registrarDeuda(deuda(CategoriaDeuda.OTRAS, "Banco", HOY, "100", null, HOY)));
        assertInvalido(() -> servicio.registrarDeuda(new DatosDeuda(CategoriaDeuda.OTRAS, "Banco", null, HOY,
                new BigDecimal("100"), new BigDecimal("500"), null, null)));
    }

    @Test
    void unaDeudaAnuladaNoCuentaNiAdmitePagos() {
        Deuda d = servicio.registrarDeuda(deuda(CategoriaDeuda.TRIBUTOS, "SUNAT", HOY, "540", null, null));
        assertInvalido(() -> servicio.anularDeuda(d.id(), "x"));

        Deuda anulada = servicio.anularDeuda(d.id(), "Registrada dos veces");

        assertTrue(anulada.anulada());
        assertEquals(0, servicio.listarDeudas(false).size());
        assertEquals(1, servicio.listarDeudas(true).size());
        assertTrue(servicio.situacion().balance().sinRegistros());
        assertInvalido(() -> servicio.registrarPago(d.id(), HOY, BigDecimal.ONE, null));
    }

    @Test
    void losDatosSonDeLaEmpresaActiva() {
        Deuda d = servicio.registrarDeuda(deuda(CategoriaDeuda.OTRAS, "Banco", HOY, "100", null, null));
        Activo a = activo(CategoriaActivo.CAJA_BANCOS, "Caja", HOY, "50", null, null);
        entorno.crearEmpresaActiva("Otra empresa");

        assertEquals(0, servicio.listarDeudas(true).size());
        assertEquals(0, servicio.listarActivos(true).size());
        assertInvalido(() -> servicio.registrarPago(d.id(), HOY, BigDecimal.ONE, null));
        assertInvalido(() -> servicio.darDeBaja(a.id(), HOY, "No es mío"));
        assertInvalido(() -> servicio.listarPagos(d.id()));
    }

    @Test
    void respetaLosPermisos() {
        activo(CategoriaActivo.CAJA_BANCOS, "Caja", HOY, "50", null, null);
        Usuario analista = entorno.crearUsuario("analista", "ANALISTA");
        entorno.empresaService.asignarAccesos(empresa.id(), Set.of(analista.id()));
        entorno.entrarComo("analista", "Temporal123");
        entorno.empresaService.restaurarEmpresaActiva();

        assertEquals(1, servicio.listarActivos(false).size());
        igual("50", servicio.situacion().balance().totalActivo());
        assertThrows(AccesoDenegadoException.class,
                () -> activo(CategoriaActivo.CAJA_BANCOS, "Caja 2", HOY, "50", null, null));
        assertThrows(AccesoDenegadoException.class,
                () -> servicio.registrarDeuda(deuda(CategoriaDeuda.OTRAS, "Banco", HOY, "100", null, null)));
    }

    // ------------------------------------------------------------------ utilidades

    private Activo activo(CategoriaActivo categoria, String descripcion, LocalDate fecha, String valor, Integer vida,
                          String residual) {
        return servicio.registrarActivo(new DatosActivo(categoria, descripcion, fecha, new BigDecimal(valor), vida,
                residual == null ? null : new BigDecimal(residual)));
    }

    private static DatosDeuda deuda(CategoriaDeuda categoria, String acreedor, LocalDate fecha, String monto,
                                    String cuota, LocalDate vencimiento) {
        return new DatosDeuda(categoria, acreedor, null, fecha, new BigDecimal(monto), null,
                cuota == null ? null : new BigDecimal(cuota), vencimiento);
    }

    private static void assertInvalido(Runnable accion) {
        assertThrows(ValidacionException.class, accion::run);
    }

    private static void igual(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }
}
