package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.ResumenOperacionesFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.StockInsuficienteExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM.DatosLineaFGEM;
import com.businessanalyzer.model.DatosProductoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.OperacionFGEM;
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

class OperacionServiceTestFGEM {

    private static final LocalDate HOY_FGEM = LocalDate.of(2026, 1, 15);
    private static final LocalDate INICIO_MES_FGEM = LocalDate.of(2026, 1, 1);

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private OperacionServiceFGEM servicioFGEM;
    private EmpresaFGEM empresaFGEM;
    private ProductoFGEM arrozFGEM;
    private ProductoFGEM azucarFGEM;
    private ProductoFGEM deliveryFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = entornoFGEM.operacionesFGEM;
        entornoFGEM.administradorConectadoFGEM();
        empresaFGEM = entornoFGEM.crearEmpresaActivaFGEM("Comercial Andina");
        arrozFGEM = productoFGEM(TipoItemFGEM.PRODUCTO, "Arroz 5 kg", "18.50", "24.90");
        azucarFGEM = productoFGEM(TipoItemFGEM.PRODUCTO, "Azúcar 1 kg", "3.20", "4.50");
        deliveryFGEM = productoFGEM(TipoItemFGEM.SERVICIO, "Delivery", "2.00", "5.00");
    }

    @Test
    void unaCompraAumentaElStockYUnaVentaLoDisminuyeFGEM() {
        comprarFGEM(arrozFGEM, "10", "18.50");
        assertEquals(0, new BigDecimal("10").compareTo(stockFGEM(arrozFGEM)));

        venderFGEM(arrozFGEM, "3", "24.90");
        assertEquals(0, new BigDecimal("7").compareTo(stockFGEM(arrozFGEM)));
        assertNull(entornoFGEM.catalogoFGEM.listarProductosFGEM(false).stream()
                .filter(pFGEM -> pFGEM.idFGEM() == deliveryFGEM.idFGEM()).findFirst().orElseThrow().stockFGEM(), "Los servicios no tienen stock");
    }

    @Test
    void laVentaCalculaTotalesYGuardaElCostoDelMomentoFGEM() {
        comprarFGEM(arrozFGEM, "10", "18.50");
        comprarFGEM(azucarFGEM, "10", "3.20");

        OperacionFGEM ventaFGEM = servicioFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.VENTA, HOY_FGEM, "María Pérez", "45678912",
                "B001-123", new BigDecimal("18"), null, List.of(
                lineaFGEM(arrozFGEM, "3", "24.90"), lineaFGEM(azucarFGEM, "2.5", "4.50")), false), false);

        assertEquals("V-000001", ventaFGEM.numeroFGEM());
        assertEquals(0, new BigDecimal("85.95").compareTo(ventaFGEM.subtotalFGEM()));
        assertEquals(0, new BigDecimal("15.47").compareTo(ventaFGEM.impuestoFGEM()));
        assertEquals(0, new BigDecimal("101.42").compareTo(ventaFGEM.totalFGEM()));
        assertEquals(0, new BigDecimal("63.50").compareTo(ventaFGEM.costoTotalFGEM()));
        assertEquals(2, ventaFGEM.lineasFGEM().size());
        assertEquals("Arroz 5 kg", ventaFGEM.lineasFGEM().get(0).nombreFGEM());

        entornoFGEM.catalogoFGEM.actualizarProductoFGEM(arrozFGEM.idFGEM(), new DatosProductoFGEM(TipoItemFGEM.PRODUCTO, arrozFGEM.codigoFGEM(),
                arrozFGEM.nombreFGEM(), null, null, UnidadMedidaFGEM.UNIDAD, new BigDecimal("30"), new BigDecimal("40")));
        assertEquals(0, new BigDecimal("63.50").compareTo(servicioFGEM.obtenerFGEM(ventaFGEM.idFGEM()).costoTotalFGEM()));
    }

    @Test
    void laNumeracionEsCorrelativaPorTipoYEmpresaFGEM() {
        assertEquals("C-000001", comprarFGEM(arrozFGEM, "5", "18").numeroFGEM());
        assertEquals("C-000002", comprarFGEM(arrozFGEM, "5", "18").numeroFGEM());
        assertEquals("V-000001", venderFGEM(arrozFGEM, "1", "25").numeroFGEM());

        entornoFGEM.crearEmpresaActivaFGEM("Otra empresa");
        ProductoFGEM otroFGEM = productoFGEM(TipoItemFGEM.SERVICIO, "Asesoría", "0", "100");
        assertEquals("V-000001", venderFGEM(otroFGEM, "1", "100").numeroFGEM(), "Cada empresa tiene su propia numeración");
    }

    @Test
    void venderSinStockPideConfirmacionFGEM() {
        comprarFGEM(arrozFGEM, "2", "18.50");

        StockInsuficienteExceptionFGEM errorFGEM = assertThrows(StockInsuficienteExceptionFGEM.class,
                () -> venderFGEM(arrozFGEM, "5", "24.90"));
        assertEquals(1, errorFGEM.faltantesFGEM().size());
        assertTrue(errorFGEM.getMessage().contains("disponible 2"));
        assertEquals(0, new BigDecimal("2").compareTo(stockFGEM(arrozFGEM)), "No se registró nada");

        servicioFGEM.registrarFGEM(datosFGEM(TipoOperacionFGEM.VENTA, lineaFGEM(arrozFGEM, "5", "24.90")), true);
        assertEquals(0, new BigDecimal("-3").compareTo(stockFGEM(arrozFGEM)), "Con confirmación queda stock negativo");
    }

    @Test
    void losServiciosNoControlanStockFGEM() {
        OperacionFGEM ventaFGEM = venderFGEM(deliveryFGEM, "3", "5");
        assertEquals(0, new BigDecimal("15.00").compareTo(ventaFGEM.subtotalFGEM()));
    }

    @Test
    void anularRevierteElStockYSaleDeLosTotalesFGEM() {
        comprarFGEM(arrozFGEM, "10", "18.50");
        OperacionFGEM ventaFGEM = venderFGEM(arrozFGEM, "4", "25");

        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.anularFGEM(ventaFGEM.idFGEM(), "x"), "El motivo es obligatorio");
        OperacionFGEM anuladaFGEM = servicioFGEM.anularFGEM(ventaFGEM.idFGEM(), "Cliente devolvió la mercadería");

        assertTrue(anuladaFGEM.anuladaFGEM());
        assertEquals("admin", anuladaFGEM.anuladoPorFGEM());
        assertEquals(0, new BigDecimal("10").compareTo(stockFGEM(arrozFGEM)));
        assertEquals(0, servicioFGEM.resumenFGEM(INICIO_MES_FGEM, HOY_FGEM).cantidadVentasFGEM());
        assertTrue(servicioFGEM.listarFGEM(TipoOperacionFGEM.VENTA, INICIO_MES_FGEM, HOY_FGEM, false).isEmpty());
        assertEquals(1, servicioFGEM.listarFGEM(TipoOperacionFGEM.VENTA, INICIO_MES_FGEM, HOY_FGEM, true).size());
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.anularFGEM(ventaFGEM.idFGEM(), "Segunda anulación"));
    }

    @Test
    void corregirAnulaLaOriginalYRegistraLaNuevaFGEM() {
        comprarFGEM(arrozFGEM, "10", "18.50");
        OperacionFGEM ventaFGEM = venderFGEM(arrozFGEM, "8", "25");

        OperacionFGEM corregidaFGEM = servicioFGEM.corregirFGEM(ventaFGEM.idFGEM(), datosFGEM(TipoOperacionFGEM.VENTA, lineaFGEM(arrozFGEM, "10", "24")), false);

        assertTrue(servicioFGEM.obtenerFGEM(ventaFGEM.idFGEM()).anuladaFGEM());
        assertTrue(servicioFGEM.obtenerFGEM(ventaFGEM.idFGEM()).motivoAnulacionFGEM().contains(corregidaFGEM.numeroFGEM()));
        assertTrue(corregidaFGEM.observacionesFGEM().startsWith("Corrige " + ventaFGEM.numeroFGEM()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stockFGEM(arrozFGEM)));
        assertEquals(1, servicioFGEM.resumenFGEM(INICIO_MES_FGEM, HOY_FGEM).cantidadVentasFGEM());
        assertEquals(0, new BigDecimal("240.00").compareTo(corregidaFGEM.subtotalFGEM()));

        assertThrows(ValidacionExceptionFGEM.class,
                () -> servicioFGEM.corregirFGEM(ventaFGEM.idFGEM(), datosFGEM(TipoOperacionFGEM.VENTA, lineaFGEM(arrozFGEM, "1", "24")), false),
                "Una anulada no se corrige");
        assertThrows(ValidacionExceptionFGEM.class,
                () -> servicioFGEM.corregirFGEM(corregidaFGEM.idFGEM(), datosFGEM(TipoOperacionFGEM.COMPRA, lineaFGEM(arrozFGEM, "1", "24")), false),
                "No cambia de tipo");
        assertThrows(StockInsuficienteExceptionFGEM.class,
                () -> servicioFGEM.corregirFGEM(corregidaFGEM.idFGEM(), datosFGEM(TipoOperacionFGEM.VENTA, lineaFGEM(arrozFGEM, "11", "24")), false));
        assertTrue(!servicioFGEM.obtenerFGEM(corregidaFGEM.idFGEM()).anuladaFGEM(), "Si falla, la original sigue registrada");
    }

    @Test
    void unaCompraPuedeActualizarElCostoDelCatalogoFGEM() {
        servicioFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.COMPRA, HOY_FGEM, "Distribuidora Lima", null, null,
                new BigDecimal("18"), null, List.of(lineaFGEM(arrozFGEM, "10", "19.40")), true), false);
        servicioFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.COMPRA, HOY_FGEM, null, null, null,
                BigDecimal.ZERO, null, List.of(lineaFGEM(azucarFGEM, "10", "9.99")), false), false);

        ProductoFGEM arrozActualFGEM = buscarFGEM(arrozFGEM);
        assertEquals(0, new BigDecimal("19.40").compareTo(arrozActualFGEM.costoUnitarioFGEM()));
        assertEquals(2, entornoFGEM.catalogoFGEM.historialPreciosFGEM(arrozFGEM.idFGEM()).size(), "El cambio quedó en el historial");
        assertEquals(0, new BigDecimal("3.20").compareTo(buscarFGEM(azucarFGEM).costoUnitarioFGEM()),
                "Sin la opción, el costo no cambia");
    }

    @Test
    void elResumenSumaSoloElPeriodoYSinImpuestosFGEM() {
        comprarFGEM(arrozFGEM, "10", "18.50");
        venderFGEM(arrozFGEM, "2", "25");
        venderFGEM(arrozFGEM, "1", "25");
        servicioFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.VENTA, LocalDate.of(2025, 12, 31), null, null, null,
                new BigDecimal("18"), null, List.of(lineaFGEM(arrozFGEM, "1", "25")), false), false);

        ResumenOperacionesFGEM resumenFGEM = servicioFGEM.resumenFGEM(INICIO_MES_FGEM, HOY_FGEM);

        assertEquals(2, resumenFGEM.cantidadVentasFGEM());
        assertEquals(1, resumenFGEM.cantidadComprasFGEM());
        assertEquals(0, new BigDecimal("75").compareTo(resumenFGEM.ventasNetasFGEM()));
        assertEquals(0, new BigDecimal("55.50").compareTo(resumenFGEM.costoVentasFGEM()));
        assertEquals(0, new BigDecimal("185").compareTo(resumenFGEM.comprasNetasFGEM()));
        assertEquals(0, new BigDecimal("37.50").compareTo(resumenFGEM.ticketPromedioFGEM()));
    }

    @Test
    void validaLaOperacionFGEM() {
        assertInvalidaFGEM(datosFGEM(TipoOperacionFGEM.VENTA));
        assertInvalidaFGEM(datosFGEM(TipoOperacionFGEM.VENTA, lineaFGEM(deliveryFGEM, "0", "5")));
        assertInvalidaFGEM(datosFGEM(TipoOperacionFGEM.VENTA, lineaFGEM(deliveryFGEM, "1", "-5")));
        assertInvalidaFGEM(datosFGEM(TipoOperacionFGEM.VENTA, lineaFGEM(deliveryFGEM, "1", "5"), lineaFGEM(deliveryFGEM, "2", "5")));
        assertInvalidaFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.VENTA, HOY_FGEM.plusDays(1), null, null, null, BigDecimal.ZERO,
                null, List.of(lineaFGEM(deliveryFGEM, "1", "5")), false));
        assertInvalidaFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.VENTA, HOY_FGEM, null, null, null, new BigDecimal("150"),
                null, List.of(lineaFGEM(deliveryFGEM, "1", "5")), false));
        assertInvalidaFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.VENTA, null, null, null, null, BigDecimal.ZERO,
                null, List.of(lineaFGEM(deliveryFGEM, "1", "5")), false));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.listarFGEM(TipoOperacionFGEM.VENTA, HOY_FGEM, INICIO_MES_FGEM, false),
                "El período no puede estar invertido");
    }

    @Test
    void noSeUsanProductosArchivadosNiDeOtraEmpresaFGEM() {
        entornoFGEM.catalogoFGEM.cambiarEstadoProductoFGEM(azucarFGEM.idFGEM(), false);
        assertInvalidaFGEM(datosFGEM(TipoOperacionFGEM.COMPRA, lineaFGEM(azucarFGEM, "1", "3")));

        entornoFGEM.crearEmpresaActivaFGEM("Otra empresa");
        assertInvalidaFGEM(datosFGEM(TipoOperacionFGEM.COMPRA, lineaFGEM(arrozFGEM, "1", "3")));
    }

    @Test
    void laTransaccionNoDejaOperacionesAMediasFGEM() {
        comprarFGEM(arrozFGEM, "1", "18");
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.registrarFGEM(datosFGEM(TipoOperacionFGEM.COMPRA,
                lineaFGEM(arrozFGEM, "5", "18"), new DatosLineaFGEM(9_999, BigDecimal.ONE, BigDecimal.ONE)), false));

        assertEquals(1, servicioFGEM.listarFGEM(TipoOperacionFGEM.COMPRA, INICIO_MES_FGEM, HOY_FGEM, true).size());
        assertEquals(0, BigDecimal.ONE.compareTo(stockFGEM(arrozFGEM)));
        assertEquals("C-000002", comprarFGEM(arrozFGEM, "1", "18").numeroFGEM(), "La numeración no salta");
    }

    @Test
    void respetaLosPermisosFGEM() {
        venderFGEM(deliveryFGEM, "1", "5");
        UsuarioFGEM analistaFGEM = entornoFGEM.crearUsuarioFGEM("analista", "ANALISTA");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of(analistaFGEM.idFGEM()));
        entornoFGEM.entrarComoFGEM("analista", "Temporal123");
        entornoFGEM.empresaServiceFGEM.restaurarEmpresaActivaFGEM();

        assertEquals(1, servicioFGEM.listarFGEM(TipoOperacionFGEM.VENTA, INICIO_MES_FGEM, HOY_FGEM, false).size());
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> venderFGEM(deliveryFGEM, "1", "5"));
        OperacionFGEM existenteFGEM = servicioFGEM.listarFGEM(TipoOperacionFGEM.VENTA, INICIO_MES_FGEM, HOY_FGEM, false).get(0);
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.anularFGEM(existenteFGEM.idFGEM(), "Sin permiso"));
    }

    @Test
    void recuerdaLaTasaDeImpuestoPorEmpresaFGEM() {
        assertEquals(0, new BigDecimal("18").compareTo(servicioFGEM.tasaImpuestoSugeridaFGEM()));
        servicioFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.VENTA, HOY_FGEM, null, null, null, new BigDecimal("10.5"),
                null, List.of(lineaFGEM(deliveryFGEM, "1", "5")), false), false);
        assertEquals(0, new BigDecimal("10.5").compareTo(servicioFGEM.tasaImpuestoSugeridaFGEM()));
        assertFalse(servicioFGEM.obtenerFGEM(servicioFGEM.listarFGEM(TipoOperacionFGEM.VENTA, INICIO_MES_FGEM, HOY_FGEM, false).get(0).idFGEM())
                .anuladaFGEM());
    }

    private OperacionFGEM comprarFGEM(ProductoFGEM productoFGEM, String cantidadFGEM, String precioFGEM) {
        return servicioFGEM.registrarFGEM(datosFGEM(TipoOperacionFGEM.COMPRA, lineaFGEM(productoFGEM, cantidadFGEM, precioFGEM)), false);
    }

    private OperacionFGEM venderFGEM(ProductoFGEM productoFGEM, String cantidadFGEM, String precioFGEM) {
        return servicioFGEM.registrarFGEM(datosFGEM(TipoOperacionFGEM.VENTA, lineaFGEM(productoFGEM, cantidadFGEM, precioFGEM)), false);
    }

    private static DatosOperacionFGEM datosFGEM(TipoOperacionFGEM tipoFGEM, DatosLineaFGEM... lineasFGEM) {
        return new DatosOperacionFGEM(tipoFGEM, HOY_FGEM, null, null, null, new BigDecimal("18"), null, List.of(lineasFGEM), false);
    }

    private static DatosLineaFGEM lineaFGEM(ProductoFGEM productoFGEM, String cantidadFGEM, String precioFGEM) {
        return new DatosLineaFGEM(productoFGEM.idFGEM(), new BigDecimal(cantidadFGEM), new BigDecimal(precioFGEM));
    }

    private void assertInvalidaFGEM(DatosOperacionFGEM datosFGEM) {
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.registrarFGEM(datosFGEM, false));
    }

    private ProductoFGEM productoFGEM(TipoItemFGEM tipoFGEM, String nombreFGEM, String costoFGEM, String precioFGEM) {
        return entornoFGEM.catalogoFGEM.crearProductoFGEM(new DatosProductoFGEM(tipoFGEM, null, nombreFGEM, null, null,
                tipoFGEM == TipoItemFGEM.SERVICIO ? UnidadMedidaFGEM.SERVICIO : UnidadMedidaFGEM.UNIDAD,
                new BigDecimal(costoFGEM), new BigDecimal(precioFGEM)));
    }

    private ProductoFGEM buscarFGEM(ProductoFGEM productoFGEM) {
        return entornoFGEM.catalogoFGEM.listarProductosFGEM(true).stream()
                .filter(pFGEM -> pFGEM.idFGEM() == productoFGEM.idFGEM()).findFirst().orElseThrow();
    }

    private BigDecimal stockFGEM(ProductoFGEM productoFGEM) {
        return buscarFGEM(productoFGEM).stockFGEM();
    }
}
