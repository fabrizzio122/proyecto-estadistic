package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.ResumenOperaciones;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.StockInsuficienteException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosOperacion;
import com.businessanalyzer.model.DatosOperacion.DatosLinea;
import com.businessanalyzer.model.DatosProducto;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Operacion;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperacionServiceTest {

    /** El reloj de pruebas está en 2026-01-15. */
    private static final LocalDate HOY = LocalDate.of(2026, 1, 15);
    private static final LocalDate INICIO_MES = LocalDate.of(2026, 1, 1);

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private OperacionService servicio;
    private Empresa empresa;
    private Producto arroz;
    private Producto azucar;
    private Producto delivery;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = entorno.operaciones;
        entorno.administradorConectado();
        empresa = entorno.crearEmpresaActiva("Comercial Andina");
        arroz = producto(TipoItem.PRODUCTO, "Arroz 5 kg", "18.50", "24.90");
        azucar = producto(TipoItem.PRODUCTO, "Azúcar 1 kg", "3.20", "4.50");
        delivery = producto(TipoItem.SERVICIO, "Delivery", "2.00", "5.00");
    }

    @Test
    void unaCompraAumentaElStockYUnaVentaLoDisminuye() {
        comprar(arroz, "10", "18.50");
        assertEquals(0, new BigDecimal("10").compareTo(stock(arroz)));

        vender(arroz, "3", "24.90");
        assertEquals(0, new BigDecimal("7").compareTo(stock(arroz)));
        assertNull(entorno.catalogo.listarProductos(false).stream()
                .filter(p -> p.id() == delivery.id()).findFirst().orElseThrow().stock(), "Los servicios no tienen stock");
    }

    @Test
    void laVentaCalculaTotalesYGuardaElCostoDelMomento() {
        comprar(arroz, "10", "18.50");
        comprar(azucar, "10", "3.20");

        Operacion venta = servicio.registrar(new DatosOperacion(TipoOperacion.VENTA, HOY, "María Pérez", "45678912",
                "B001-123", new BigDecimal("18"), null, List.of(
                linea(arroz, "3", "24.90"), linea(azucar, "2.5", "4.50")), false), false);

        assertEquals("V-000001", venta.numero());
        assertEquals(0, new BigDecimal("85.95").compareTo(venta.subtotal()));
        assertEquals(0, new BigDecimal("15.47").compareTo(venta.impuesto()));
        assertEquals(0, new BigDecimal("101.42").compareTo(venta.total()));
        assertEquals(0, new BigDecimal("63.50").compareTo(venta.costoTotal()));
        assertEquals(2, venta.lineas().size());
        assertEquals("Arroz 5 kg", venta.lineas().get(0).nombre());

        // Si después sube el costo, la venta ya registrada conserva su margen
        entorno.catalogo.actualizarProducto(arroz.id(), new DatosProducto(TipoItem.PRODUCTO, arroz.codigo(),
                arroz.nombre(), null, null, UnidadMedida.UNIDAD, new BigDecimal("30"), new BigDecimal("40")));
        assertEquals(0, new BigDecimal("63.50").compareTo(servicio.obtener(venta.id()).costoTotal()));
    }

    @Test
    void laNumeracionEsCorrelativaPorTipoYEmpresa() {
        assertEquals("C-000001", comprar(arroz, "5", "18").numero());
        assertEquals("C-000002", comprar(arroz, "5", "18").numero());
        assertEquals("V-000001", vender(arroz, "1", "25").numero());

        entorno.crearEmpresaActiva("Otra empresa");
        Producto otro = producto(TipoItem.SERVICIO, "Asesoría", "0", "100");
        assertEquals("V-000001", vender(otro, "1", "100").numero(), "Cada empresa tiene su propia numeración");
    }

    @Test
    void venderSinStockPideConfirmacion() {
        comprar(arroz, "2", "18.50");

        StockInsuficienteException error = assertThrows(StockInsuficienteException.class,
                () -> vender(arroz, "5", "24.90"));
        assertEquals(1, error.faltantes().size());
        assertTrue(error.getMessage().contains("disponible 2"));
        assertEquals(0, new BigDecimal("2").compareTo(stock(arroz)), "No se registró nada");

        servicio.registrar(datos(TipoOperacion.VENTA, linea(arroz, "5", "24.90")), true);
        assertEquals(0, new BigDecimal("-3").compareTo(stock(arroz)), "Con confirmación queda stock negativo");
    }

    @Test
    void losServiciosNoControlanStock() {
        Operacion venta = vender(delivery, "3", "5");
        assertEquals(0, new BigDecimal("15.00").compareTo(venta.subtotal()));
    }

    @Test
    void anularRevierteElStockYSaleDeLosTotales() {
        comprar(arroz, "10", "18.50");
        Operacion venta = vender(arroz, "4", "25");

        assertThrows(ValidacionException.class, () -> servicio.anular(venta.id(), "x"), "El motivo es obligatorio");
        Operacion anulada = servicio.anular(venta.id(), "Cliente devolvió la mercadería");

        assertTrue(anulada.anulada());
        assertEquals("admin", anulada.anuladoPor());
        assertEquals(0, new BigDecimal("10").compareTo(stock(arroz)));
        assertEquals(0, servicio.resumen(INICIO_MES, HOY).cantidadVentas());
        assertTrue(servicio.listar(TipoOperacion.VENTA, INICIO_MES, HOY, false).isEmpty());
        assertEquals(1, servicio.listar(TipoOperacion.VENTA, INICIO_MES, HOY, true).size());
        assertThrows(ValidacionException.class, () -> servicio.anular(venta.id(), "Segunda anulación"));
    }

    @Test
    void unaCompraPuedeActualizarElCostoDelCatalogo() {
        servicio.registrar(new DatosOperacion(TipoOperacion.COMPRA, HOY, "Distribuidora Lima", null, null,
                new BigDecimal("18"), null, List.of(linea(arroz, "10", "19.40")), true), false);
        servicio.registrar(new DatosOperacion(TipoOperacion.COMPRA, HOY, null, null, null,
                BigDecimal.ZERO, null, List.of(linea(azucar, "10", "9.99")), false), false);

        Producto arrozActual = buscar(arroz);
        assertEquals(0, new BigDecimal("19.40").compareTo(arrozActual.costoUnitario()));
        assertEquals(2, entorno.catalogo.historialPrecios(arroz.id()).size(), "El cambio quedó en el historial");
        assertEquals(0, new BigDecimal("3.20").compareTo(buscar(azucar).costoUnitario()),
                "Sin la opción, el costo no cambia");
    }

    @Test
    void elResumenSumaSoloElPeriodoYSinImpuestos() {
        comprar(arroz, "10", "18.50");
        vender(arroz, "2", "25");
        vender(arroz, "1", "25");
        servicio.registrar(new DatosOperacion(TipoOperacion.VENTA, LocalDate.of(2025, 12, 31), null, null, null,
                new BigDecimal("18"), null, List.of(linea(arroz, "1", "25")), false), false);

        ResumenOperaciones resumen = servicio.resumen(INICIO_MES, HOY);

        assertEquals(2, resumen.cantidadVentas());
        assertEquals(1, resumen.cantidadCompras());
        assertEquals(0, new BigDecimal("75").compareTo(resumen.ventasNetas()));
        assertEquals(0, new BigDecimal("55.50").compareTo(resumen.costoVentas()));
        assertEquals(0, new BigDecimal("185").compareTo(resumen.comprasNetas()));
        assertEquals(0, new BigDecimal("37.50").compareTo(resumen.ticketPromedio()));
    }

    @Test
    void validaLaOperacion() {
        assertInvalida(datos(TipoOperacion.VENTA));
        assertInvalida(datos(TipoOperacion.VENTA, linea(delivery, "0", "5")));
        assertInvalida(datos(TipoOperacion.VENTA, linea(delivery, "1", "-5")));
        assertInvalida(datos(TipoOperacion.VENTA, linea(delivery, "1", "5"), linea(delivery, "2", "5")));
        assertInvalida(new DatosOperacion(TipoOperacion.VENTA, HOY.plusDays(1), null, null, null, BigDecimal.ZERO,
                null, List.of(linea(delivery, "1", "5")), false));
        assertInvalida(new DatosOperacion(TipoOperacion.VENTA, HOY, null, null, null, new BigDecimal("150"),
                null, List.of(linea(delivery, "1", "5")), false));
        assertInvalida(new DatosOperacion(TipoOperacion.VENTA, null, null, null, null, BigDecimal.ZERO,
                null, List.of(linea(delivery, "1", "5")), false));
        assertThrows(ValidacionException.class, () -> servicio.listar(TipoOperacion.VENTA, HOY, INICIO_MES, false),
                "El período no puede estar invertido");
    }

    @Test
    void noSeUsanProductosArchivadosNiDeOtraEmpresa() {
        entorno.catalogo.cambiarEstadoProducto(azucar.id(), false);
        assertInvalida(datos(TipoOperacion.COMPRA, linea(azucar, "1", "3")));

        entorno.crearEmpresaActiva("Otra empresa");
        assertInvalida(datos(TipoOperacion.COMPRA, linea(arroz, "1", "3")));
    }

    @Test
    void laTransaccionNoDejaOperacionesAMedias() {
        comprar(arroz, "1", "18");
        // La segunda línea falla (producto inexistente): no debe quedar nada de la operación
        assertThrows(ValidacionException.class, () -> servicio.registrar(datos(TipoOperacion.COMPRA,
                linea(arroz, "5", "18"), new DatosLinea(9_999, BigDecimal.ONE, BigDecimal.ONE)), false));

        assertEquals(1, servicio.listar(TipoOperacion.COMPRA, INICIO_MES, HOY, true).size());
        assertEquals(0, BigDecimal.ONE.compareTo(stock(arroz)));
        assertEquals("C-000002", comprar(arroz, "1", "18").numero(), "La numeración no salta");
    }

    @Test
    void respetaLosPermisos() {
        vender(delivery, "1", "5");
        Usuario analista = entorno.crearUsuario("analista", "ANALISTA");
        entorno.empresaService.asignarAccesos(empresa.id(), Set.of(analista.id()));
        entorno.entrarComo("analista", "Temporal123");
        entorno.empresaService.restaurarEmpresaActiva();

        assertEquals(1, servicio.listar(TipoOperacion.VENTA, INICIO_MES, HOY, false).size());
        assertThrows(AccesoDenegadoException.class, () -> vender(delivery, "1", "5"));
        Operacion existente = servicio.listar(TipoOperacion.VENTA, INICIO_MES, HOY, false).get(0);
        assertThrows(AccesoDenegadoException.class, () -> servicio.anular(existente.id(), "Sin permiso"));
    }

    @Test
    void recuerdaLaTasaDeImpuestoPorEmpresa() {
        assertEquals(0, new BigDecimal("18").compareTo(servicio.tasaImpuestoSugerida()));
        servicio.registrar(new DatosOperacion(TipoOperacion.VENTA, HOY, null, null, null, new BigDecimal("10.5"),
                null, List.of(linea(delivery, "1", "5")), false), false);
        assertEquals(0, new BigDecimal("10.5").compareTo(servicio.tasaImpuestoSugerida()));
        assertFalse(servicio.obtener(servicio.listar(TipoOperacion.VENTA, INICIO_MES, HOY, false).get(0).id())
                .anulada());
    }

    // ------------------------------------------------------------------ utilidades

    private Operacion comprar(Producto producto, String cantidad, String precio) {
        return servicio.registrar(datos(TipoOperacion.COMPRA, linea(producto, cantidad, precio)), false);
    }

    private Operacion vender(Producto producto, String cantidad, String precio) {
        return servicio.registrar(datos(TipoOperacion.VENTA, linea(producto, cantidad, precio)), false);
    }

    private static DatosOperacion datos(TipoOperacion tipo, DatosLinea... lineas) {
        return new DatosOperacion(tipo, HOY, null, null, null, new BigDecimal("18"), null, List.of(lineas), false);
    }

    private static DatosLinea linea(Producto producto, String cantidad, String precio) {
        return new DatosLinea(producto.id(), new BigDecimal(cantidad), new BigDecimal(precio));
    }

    private void assertInvalida(DatosOperacion datos) {
        assertThrows(ValidacionException.class, () -> servicio.registrar(datos, false));
    }

    private Producto producto(TipoItem tipo, String nombre, String costo, String precio) {
        return entorno.catalogo.crearProducto(new DatosProducto(tipo, null, nombre, null, null,
                tipo == TipoItem.SERVICIO ? UnidadMedida.SERVICIO : UnidadMedida.UNIDAD,
                new BigDecimal(costo), new BigDecimal(precio)));
    }

    private Producto buscar(Producto producto) {
        return entorno.catalogo.listarProductos(true).stream()
                .filter(p -> p.id() == producto.id()).findFirst().orElseThrow();
    }

    private BigDecimal stock(Producto producto) {
        return buscar(producto).stock();
    }
}
