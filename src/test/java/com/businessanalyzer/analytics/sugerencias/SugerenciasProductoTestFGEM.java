package com.businessanalyzer.analytics.sugerencias;

import com.businessanalyzer.analytics.RentabilidadProductoFGEM.VentaFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.MotivoPrecioFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.NotaPrecioFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.PeriodoVentasFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.PrecioFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.ReposicionFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.ResultadoFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.SinMovimientoFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.SituacionFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.UrgenciaFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SugerenciasProductoTestFGEM {

    private static final BigDecimal VEINTE_FGEM = new BigDecimal("0.20");

    @Test
    void elPrecioSugeridoDejaElMargenObjetivoRedondeadoAlAlzaA10CentimosFGEM() {
        igualFGEM("4.50", SugerenciasProductoFGEM.precioParaMargenFGEM(bdFGEM("3.60"), VEINTE_FGEM));
        igualFGEM("4.80", SugerenciasProductoFGEM.precioParaMargenFGEM(bdFGEM("3.80"), VEINTE_FGEM));
        igualFGEM("0.40", SugerenciasProductoFGEM.precioParaMargenFGEM(bdFGEM("0.25"), VEINTE_FGEM));
    }

    @Test
    void sugierePreciosParaLoQueSeVendeConPerdidaOConPocoMargenFGEM() {
        ProductoFGEM aguaFGEM = productoFGEM(1, "Agua mineral", "1.10", "1.00", "10");
        ProductoFGEM arrozFGEM = productoFGEM(2, "Arroz 5 kg", "19.50", "21.00", "5");
        ProductoFGEM gaseosaFGEM = productoFGEM(3, "Gaseosa", "1.90", "2.50", "40");
        ResultadoFGEM rFGEM = calcularFGEM(List.of(aguaFGEM, arrozFGEM, gaseosaFGEM), 30,
                ventasFGEM(aguaFGEM, 60, arrozFGEM, 30, gaseosaFGEM, 30));

        assertEquals(2, rFGEM.preciosFGEM().size());
        PrecioFGEM primeroFGEM = rFGEM.preciosFGEM().get(0);
        assertEquals("Arroz 5 kg", primeroFGEM.productoFGEM().nombreFGEM());
        assertEquals(MotivoPrecioFGEM.MARGEN_BAJO, primeroFGEM.motivoFGEM());
        assertNull(primeroFGEM.notaFGEM());
        igualFGEM("24.40", primeroFGEM.precioSugeridoFGEM());
        igualFGEM("102.00", primeroFGEM.gananciaExtraMesFGEM());
        PrecioFGEM aguaSugeridaFGEM = rFGEM.preciosFGEM().get(1);
        assertEquals(MotivoPrecioFGEM.PERDIDA, aguaSugeridaFGEM.motivoFGEM());
        igualFGEM("1.40", aguaSugeridaFGEM.precioSugeridoFGEM());
        igualFGEM("24.00", aguaSugeridaFGEM.gananciaExtraMesFGEM());
        igualFGEM("126.00", rFGEM.gananciaExtraMesFGEM());
    }

    @Test
    void lasSugerenciasDePrecioDeProductosQueNoSeVendenSeMarcanYVanAlFinalFGEM() {
        ProductoFGEM aceiteFGEM = productoFGEM(1, "Aceite Primor", "9.20", "10.00", "8");
        ProductoFGEM arrozFGEM = productoFGEM(2, "Arroz extra", "19.50", "21.00", "5");
        ResultadoFGEM rFGEM = calcularFGEM(List.of(aceiteFGEM, arrozFGEM), 60, ventasFGEM(arrozFGEM, 60));

        assertEquals(List.of("Arroz extra", "Aceite Primor"), rFGEM.preciosFGEM().stream().map(pFGEM -> pFGEM.productoFGEM().nombreFGEM()).toList());
        assertEquals(NotaPrecioFGEM.SIN_VENTAS, rFGEM.preciosFGEM().get(1).notaFGEM());
        assertEquals(1, rFGEM.preciosPrioritariosFGEM());
    }

    @Test
    void indicaQueReponerYCuantoComprarParaDosSemanasFGEM() {
        ProductoFGEM lecheFGEM = productoFGEM(1, "Leche", "3.30", "4.00", "0");
        ProductoFGEM fideosFGEM = productoFGEM(2, "Fideos", "2.70", "3.50", "8");
        ProductoFGEM azucarFGEM = productoFGEM(3, "Azúcar", "3.60", "4.50", "15");
        ProductoFGEM aceiteFGEM = productoFGEM(4, "Aceite", "9.20", "11.00", "60");
        ResultadoFGEM rFGEM = calcularFGEM(List.of(lecheFGEM, fideosFGEM, azucarFGEM, aceiteFGEM), 30,
                ventasFGEM(lecheFGEM, 90, fideosFGEM, 60, azucarFGEM, 45, aceiteFGEM, 30));

        List<ReposicionFGEM> reponerFGEM = rFGEM.reposicionFGEM();
        assertEquals(List.of("Leche", "Fideos", "Azúcar"), reponerFGEM.stream().map(rFGEM2 -> rFGEM2.productoFGEM().nombreFGEM()).toList());
        assertEquals(UrgenciaFGEM.AGOTADO, reponerFGEM.get(0).urgenciaFGEM());
        igualFGEM("42", reponerFGEM.get(0).cantidadFGEM());
        assertEquals(UrgenciaFGEM.URGENTE, reponerFGEM.get(1).urgenciaFGEM());
        igualFGEM("20", reponerFGEM.get(1).cantidadFGEM());
        assertEquals(UrgenciaFGEM.PRONTO, reponerFGEM.get(2).urgenciaFGEM());
        igualFGEM("6", reponerFGEM.get(2).cantidadFGEM());
        igualFGEM("54", reponerFGEM.get(1).costoFGEM());
    }

    @Test
    void unProductoAgotadoSinVentasRecientesUsaSuRitmoMientrasTuvoStockFGEM() {
        ProductoFGEM cafeFGEM = productoFGEM(1, "Café instantáneo", "9.00", "12.00", "0");
        java.time.LocalDate desdeFGEM = java.time.LocalDate.of(2026, 5, 29);
        PeriodoVentasFGEM historialFGEM = new PeriodoVentasFGEM(ventasFGEM(cafeFGEM, 46), 180, desdeFGEM,
                java.util.Map.of(cafeFGEM.idFGEM(), desdeFGEM.plusDays(99)));
        ResultadoFGEM rFGEM = SugerenciasProductoFGEM.calcularFGEM(List.of(cafeFGEM), new PeriodoVentasFGEM(List.of(), 60), historialFGEM,
                VEINTE_FGEM);

        assertEquals(1, rFGEM.reposicionFGEM().size());
        ReposicionFGEM cafeReponerFGEM = rFGEM.reposicionFGEM().get(0);
        assertEquals(UrgenciaFGEM.AGOTADO, cafeReponerFGEM.urgenciaFGEM());
        assertTrue(cafeReponerFGEM.segunHistorialFGEM());
        assertEquals(100, cafeReponerFGEM.diasBaseFGEM());
        igualFGEM("0.46", cafeReponerFGEM.ventaDiariaFGEM());
        igualFGEM("7", cafeReponerFGEM.cantidadFGEM());
        igualFGEM("63.00", cafeReponerFGEM.costoFGEM());

        ResultadoFGEM sinFechaFGEM = SugerenciasProductoFGEM.calcularFGEM(List.of(cafeFGEM), new PeriodoVentasFGEM(List.of(), 60),
                new PeriodoVentasFGEM(ventasFGEM(cafeFGEM, 36), 180), VEINTE_FGEM);
        igualFGEM("3", sinFechaFGEM.reposicionFGEM().get(0).cantidadFGEM());
    }

    @Test
    void separaLosProductosSinVentasDeLosDeVentaLentaConElHistorialDe180DiasFGEM() {
        ProductoFGEM detergenteFGEM = productoFGEM(1, "Detergente", "9.50", "11.50", "12");
        ProductoFGEM papelFGEM = productoFGEM(2, "Papel higiénico", "4.80", "6.00", "40");
        ProductoFGEM harinaFGEM = productoFGEM(3, "Harina", "4.00", "5.00", "4");
        ProductoFGEM panFGEM = productoFGEM(4, "Pan", "0.25", "0.40", "100");
        List<ProductoFGEM> productosFGEM = List.of(detergenteFGEM, papelFGEM, harinaFGEM, panFGEM);
        ResultadoFGEM rFGEM = SugerenciasProductoFGEM.calcularFGEM(productosFGEM, new PeriodoVentasFGEM(ventasFGEM(papelFGEM, 6, panFGEM, 3000), 60),
                new PeriodoVentasFGEM(ventasFGEM(papelFGEM, 20, harinaFGEM, 5, panFGEM, 9000), 180), VEINTE_FGEM);

        List<SinMovimientoFGEM> paradoFGEM = rFGEM.sinMovimientoFGEM();
        assertEquals(List.of(SituacionFGEM.SIN_VENTAS, SituacionFGEM.VENTA_LENTA), paradoFGEM.stream().map(SinMovimientoFGEM::situacionFGEM).toList());
        igualFGEM("114.00", paradoFGEM.get(0).valorFGEM());
        igualFGEM("360", paradoFGEM.get(1).diasCoberturaFGEM());
        igualFGEM("306.00", rFGEM.valorInmovilizadoFGEM());
        igualFGEM("347.00", rFGEM.valorInventarioFGEM());
        igualFGEM("0.881844", rFGEM.porcentajeInmovilizadoFGEM());
        assertEquals(1, rFGEM.cantidadFGEM(SituacionFGEM.SIN_VENTAS));
    }

    @Test
    void unPerecibleQueNoSeVendeATiempoEsUrgenteYSeReponeParaPocosDiasFGEM() {
        ProductoFGEM yogurtFGEM = perecibleFGEM(1, "Yogurt fresa", "2.50", "3.00", "20");
        ProductoFGEM panFGEM = perecibleFGEM(2, "Pan francés", "0.25", "0.40", "50");
        ResultadoFGEM rFGEM = calcularFGEM(List.of(yogurtFGEM, panFGEM), 60, ventasFGEM(yogurtFGEM, 11, panFGEM, 6000));

        assertEquals(1, rFGEM.sinMovimientoFGEM().size());
        SinMovimientoFGEM yogurtParadoFGEM = rFGEM.sinMovimientoFGEM().get(0);
        assertEquals(SituacionFGEM.PERECIBLE_EN_RIESGO, yogurtParadoFGEM.situacionFGEM());
        assertTrue(yogurtParadoFGEM.situacionFGEM().sugerenciaFGEM().contains("promoción"));
        ReposicionFGEM panReponerFGEM = rFGEM.reposicionFGEM().get(0);
        assertEquals(UrgenciaFGEM.URGENTE, panReponerFGEM.urgenciaFGEM());
        igualFGEM("250", panReponerFGEM.cantidadFGEM());
        assertEquals(NotaPrecioFGEM.VENTA_LENTA, rFGEM.preciosFGEM().stream()
                .filter(pFGEM -> pFGEM.productoFGEM().nombreFGEM().equals("Yogurt fresa")).findFirst().map(PrecioFGEM::notaFGEM).orElse(null));
    }

    @Test
    void ignoraServiciosEnElStockYProductosSinCostoEnLosPreciosFGEM() {
        ProductoFGEM deliveryFGEM = new ProductoFGEM(1, 1, null, null, TipoItemFGEM.SERVICIO, "S-0001", "Delivery", null, UnidadMedidaFGEM.SERVICIO,
                bdFGEM("0"), bdFGEM("3.00"), null, true, null, null);
        ProductoFGEM regaloFGEM = productoFGEM(2, "Caramelo de cortesía", "0", "0", "100");
        ResultadoFGEM rFGEM = calcularFGEM(List.of(deliveryFGEM, regaloFGEM), 5, ventasFGEM(deliveryFGEM, 10));

        assertTrue(rFGEM.preciosFGEM().isEmpty());
        assertTrue(rFGEM.reposicionFGEM().isEmpty());
        assertEquals(1, rFGEM.sinMovimientoFGEM().size());
        assertTrue(rFGEM.pocosDatosFGEM(), "Con 5 días de ventas las cantidades son orientativas");
        assertFalse(calcularFGEM(List.of(), 30, List.of()).pocosDatosFGEM());
        assertNull(rFGEM.sinMovimientoFGEM().get(0).diasCoberturaFGEM());
        assertNull(rFGEM.porcentajeInmovilizadoFGEM(), "Sin costo, el inventario vale cero");
    }

    private static ResultadoFGEM calcularFGEM(List<ProductoFGEM> productosFGEM, int diasFGEM, List<VentaFGEM> ventasFGEM) {
        PeriodoVentasFGEM periodoFGEM = new PeriodoVentasFGEM(ventasFGEM, diasFGEM);
        return SugerenciasProductoFGEM.calcularFGEM(productosFGEM, periodoFGEM, periodoFGEM, VEINTE_FGEM);
    }

    @Test
    void laCompraSugeridaPoneLoAgotadoPrimeroYUsaElCostoRegistradoFGEM() {
        ProductoFGEM atunFGEM = productoFGEM(1, "Atún", "4.20", "5.50", "3");
        ProductoFGEM arrozFGEM = productoFGEM(2, "Arroz", "3.80", "4.50", "0");
        List<ReposicionFGEM> reposicionFGEM = List.of(
                new ReposicionFGEM(atunFGEM, UrgenciaFGEM.PRONTO, bdFGEM("3"), bdFGEM("20"), 60, bdFGEM("0.3"), bdFGEM("9"), bdFGEM("2"), bdFGEM("8.40"), false),
                new ReposicionFGEM(arrozFGEM, UrgenciaFGEM.AGOTADO, bdFGEM("0"), bdFGEM("60"), 60, bdFGEM("1"), bdFGEM("0"), bdFGEM("14"), bdFGEM("53.20"), false));

        List<com.businessanalyzer.model.DatosOperacionFGEM.DatosLineaFGEM> lineasFGEM = SugerenciasProductoFGEM.lineasCompraFGEM(reposicionFGEM);

        assertEquals(List.of(2L, 1L), lineasFGEM.stream().map(lFGEM -> lFGEM.productoIdFGEM()).toList());
        assertEquals(0, bdFGEM("14").compareTo(lineasFGEM.get(0).cantidadFGEM()));
        assertEquals(0, bdFGEM("3.80").compareTo(lineasFGEM.get(0).precioUnitarioFGEM()));
    }

    private static ProductoFGEM productoFGEM(long idFGEM, String nombreFGEM, String costoFGEM, String precioFGEM, String stockFGEM) {
        return new ProductoFGEM(idFGEM, 1, null, null, TipoItemFGEM.PRODUCTO, "P-" + idFGEM, nombreFGEM, null, UnidadMedidaFGEM.UNIDAD,
                bdFGEM(costoFGEM), bdFGEM(precioFGEM), bdFGEM(stockFGEM), true, null, null);
    }

    private static ProductoFGEM perecibleFGEM(long idFGEM, String nombreFGEM, String costoFGEM, String precioFGEM, String stockFGEM) {
        return new ProductoFGEM(idFGEM, 1, null, null, TipoItemFGEM.PRODUCTO, "P-" + idFGEM, nombreFGEM, null, UnidadMedidaFGEM.UNIDAD,
                bdFGEM(costoFGEM), bdFGEM(precioFGEM), bdFGEM(stockFGEM), true, null, null, true);
    }

    private static List<VentaFGEM> ventasFGEM(Object... paresFGEM) {
        List<VentaFGEM> ventasFGEM = new ArrayList<>();
        for (int iFGEM = 0; iFGEM < paresFGEM.length; iFGEM += 2) {
            ProductoFGEM pFGEM = (ProductoFGEM) paresFGEM[iFGEM];
            BigDecimal cantidadFGEM = BigDecimal.valueOf((Integer) paresFGEM[iFGEM + 1]);
            ventasFGEM.add(new VentaFGEM(pFGEM.idFGEM(), pFGEM.codigoFGEM(), pFGEM.nombreFGEM(), pFGEM.tipoFGEM(), null, cantidadFGEM,
                    cantidadFGEM.multiply(pFGEM.precioVentaFGEM()), pFGEM.costoUnitarioFGEM()));
        }
        return ventasFGEM;
    }

    private static BigDecimal bdFGEM(String valorFGEM) {
        return new BigDecimal(valorFGEM);
    }

    private static void igualFGEM(String esperadoFGEM, BigDecimal realFGEM) {
        assertEquals(0, bdFGEM(esperadoFGEM).compareTo(realFGEM), "esperado " + esperadoFGEM + " pero fue " + realFGEM);
    }
}
