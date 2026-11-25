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
        igualFGEM("4.50", SugerenciasProductoFGEM.precioParaMargenFGEM(bdFGEM("3.60"), VEINTE_FGEM));   // 3.60 / 0.8 = 4.50 exacto
        igualFGEM("4.80", SugerenciasProductoFGEM.precioParaMargenFGEM(bdFGEM("3.80"), VEINTE_FGEM));   // 4.75 → 4.80
        igualFGEM("0.40", SugerenciasProductoFGEM.precioParaMargenFGEM(bdFGEM("0.25"), VEINTE_FGEM));   // 0.3125 → 0.40
    }

    @Test
    void sugierePreciosParaLoQueSeVendeConPerdidaOConPocoMargenFGEM() {
        ProductoFGEM aguaFGEM = productoFGEM(1, "Agua mineral", "1.10", "1.00", "10");     // pérdida
        ProductoFGEM arrozFGEM = productoFGEM(2, "Arroz 5 kg", "19.50", "21.00", "5");     // margen 7.1 %
        ProductoFGEM gaseosaFGEM = productoFGEM(3, "Gaseosa", "1.90", "2.50", "40");       // margen 24 %: bien
        // 30 días: 60 aguas (2 por día) y 30 arroces (1 por día).
        ResultadoFGEM rFGEM = calcularFGEM(List.of(aguaFGEM, arrozFGEM, gaseosaFGEM), 30,
                ventasFGEM(aguaFGEM, 60, arrozFGEM, 30, gaseosaFGEM, 30));

        assertEquals(2, rFGEM.preciosFGEM().size());
        PrecioFGEM primeroFGEM = rFGEM.preciosFGEM().get(0);           // el que más suma va primero
        assertEquals("Arroz 5 kg", primeroFGEM.productoFGEM().nombreFGEM());
        assertEquals(MotivoPrecioFGEM.MARGEN_BAJO, primeroFGEM.motivoFGEM());
        assertNull(primeroFGEM.notaFGEM());
        igualFGEM("24.40", primeroFGEM.precioSugeridoFGEM());          // 19.50 / 0.8 = 24.375 → 24.40
        igualFGEM("102.00", primeroFGEM.gananciaExtraMesFGEM());       // (24.40 − 21.00) × 30
        PrecioFGEM aguaSugeridaFGEM = rFGEM.preciosFGEM().get(1);
        assertEquals(MotivoPrecioFGEM.PERDIDA, aguaSugeridaFGEM.motivoFGEM());
        igualFGEM("1.40", aguaSugeridaFGEM.precioSugeridoFGEM());      // 1.10 / 0.8 = 1.375 → 1.40
        igualFGEM("24.00", aguaSugeridaFGEM.gananciaExtraMesFGEM());   // 0.40 × 60
        igualFGEM("126.00", rFGEM.gananciaExtraMesFGEM());
    }

    /** Subir el precio a algo que no se vende no es la prioridad: se marca y va al final. */
    @Test
    void lasSugerenciasDePrecioDeProductosQueNoSeVendenSeMarcanYVanAlFinalFGEM() {
        ProductoFGEM aceiteFGEM = productoFGEM(1, "Aceite Primor", "9.20", "10.00", "8");     // margen 8 %, sin ventas
        ProductoFGEM arrozFGEM = productoFGEM(2, "Arroz extra", "19.50", "21.00", "5");      // margen 7 %, se vende
        ResultadoFGEM rFGEM = calcularFGEM(List.of(aceiteFGEM, arrozFGEM), 60, ventasFGEM(arrozFGEM, 60));

        assertEquals(List.of("Arroz extra", "Aceite Primor"), rFGEM.preciosFGEM().stream().map(pFGEM -> pFGEM.productoFGEM().nombreFGEM()).toList());
        assertEquals(NotaPrecioFGEM.SIN_VENTAS, rFGEM.preciosFGEM().get(1).notaFGEM());
        assertEquals(1, rFGEM.preciosPrioritariosFGEM());
    }

    @Test
    void indicaQueReponerYCuantoComprarParaDosSemanasFGEM() {
        ProductoFGEM lecheFGEM = productoFGEM(1, "Leche", "3.30", "4.00", "0");         // agotada
        ProductoFGEM fideosFGEM = productoFGEM(2, "Fideos", "2.70", "3.50", "8");       // 2 por día → 4 días
        ProductoFGEM azucarFGEM = productoFGEM(3, "Azúcar", "3.60", "4.50", "15");      // 1.5 por día → 10 días
        ProductoFGEM aceiteFGEM = productoFGEM(4, "Aceite", "9.20", "11.00", "60");     // 1 por día → 60 días: no hace falta
        ResultadoFGEM rFGEM = calcularFGEM(List.of(lecheFGEM, fideosFGEM, azucarFGEM, aceiteFGEM), 30,
                ventasFGEM(lecheFGEM, 90, fideosFGEM, 60, azucarFGEM, 45, aceiteFGEM, 30));

        List<ReposicionFGEM> reponerFGEM = rFGEM.reposicionFGEM();
        assertEquals(List.of("Leche", "Fideos", "Azúcar"), reponerFGEM.stream().map(rFGEM2 -> rFGEM2.productoFGEM().nombreFGEM()).toList());
        assertEquals(UrgenciaFGEM.AGOTADO, reponerFGEM.get(0).urgenciaFGEM());
        igualFGEM("42", reponerFGEM.get(0).cantidadFGEM());           // 3 por día × 14
        assertEquals(UrgenciaFGEM.URGENTE, reponerFGEM.get(1).urgenciaFGEM());
        igualFGEM("20", reponerFGEM.get(1).cantidadFGEM());           // 28 − 8
        assertEquals(UrgenciaFGEM.PRONTO, reponerFGEM.get(2).urgenciaFGEM());
        igualFGEM("6", reponerFGEM.get(2).cantidadFGEM());            // 21 − 15
        igualFGEM("54", reponerFGEM.get(1).costoFGEM());              // 20 × 2.70
    }

    /**
     * Caso real: el café se agotó hace meses y por eso no se vendió en los últimos 60 días. Es el que más urge
     * reponer, así que se usa su historial largo. Vendió 46 en los 100 días que tuvo stock (hasta su última venta):
     * el ritmo es 0.46 por día, no 46 ÷ 180.
     */
    @Test
    void unProductoAgotadoSinVentasRecientesUsaSuRitmoMientrasTuvoStockFGEM() {
        ProductoFGEM cafeFGEM = productoFGEM(1, "Café instantáneo", "9.00", "12.00", "0");
        java.time.LocalDate desdeFGEM = java.time.LocalDate.of(2026, 5, 29);
        PeriodoVentasFGEM historialFGEM = new PeriodoVentasFGEM(ventasFGEM(cafeFGEM, 46), 180, desdeFGEM,
                java.util.Map.of(cafeFGEM.idFGEM(), desdeFGEM.plusDays(99)));     // última venta: el día 100
        ResultadoFGEM rFGEM = SugerenciasProductoFGEM.calcularFGEM(List.of(cafeFGEM), new PeriodoVentasFGEM(List.of(), 60), historialFGEM,
                VEINTE_FGEM);

        assertEquals(1, rFGEM.reposicionFGEM().size());
        ReposicionFGEM cafeReponerFGEM = rFGEM.reposicionFGEM().get(0);
        assertEquals(UrgenciaFGEM.AGOTADO, cafeReponerFGEM.urgenciaFGEM());
        assertTrue(cafeReponerFGEM.segunHistorialFGEM());
        assertEquals(100, cafeReponerFGEM.diasBaseFGEM());
        igualFGEM("0.46", cafeReponerFGEM.ventaDiariaFGEM());
        igualFGEM("7", cafeReponerFGEM.cantidadFGEM());               // 0.46 × 14 = 6.44 → 7
        igualFGEM("63.00", cafeReponerFGEM.costoFGEM());

        // Sin la fecha de la última venta se usa todo el período (como antes).
        ResultadoFGEM sinFechaFGEM = SugerenciasProductoFGEM.calcularFGEM(List.of(cafeFGEM), new PeriodoVentasFGEM(List.of(), 60),
                new PeriodoVentasFGEM(ventasFGEM(cafeFGEM, 36), 180), VEINTE_FGEM);
        igualFGEM("3", sinFechaFGEM.reposicionFGEM().get(0).cantidadFGEM());   // 0.2 × 14 = 2.8 → 3
    }

    @Test
    void separaLosProductosSinVentasDeLosDeVentaLentaConElHistorialDe180DiasFGEM() {
        ProductoFGEM detergenteFGEM = productoFGEM(1, "Detergente", "9.50", "11.50", "12");   // no se vendió en 180 días
        ProductoFGEM papelFGEM = productoFGEM(2, "Papel higiénico", "4.80", "6.00", "40");    // 20 en 180 días → 360 días
        ProductoFGEM harinaFGEM = productoFGEM(3, "Harina", "4.00", "5.00", "4");             // 5 en 180 días → 144 días: normal
        ProductoFGEM panFGEM = productoFGEM(4, "Pan", "0.25", "0.40", "100");                 // rota rápido
        List<ProductoFGEM> productosFGEM = List.of(detergenteFGEM, papelFGEM, harinaFGEM, panFGEM);
        ResultadoFGEM rFGEM = SugerenciasProductoFGEM.calcularFGEM(productosFGEM, new PeriodoVentasFGEM(ventasFGEM(papelFGEM, 6, panFGEM, 3000), 60),
                new PeriodoVentasFGEM(ventasFGEM(papelFGEM, 20, harinaFGEM, 5, panFGEM, 9000), 180), VEINTE_FGEM);

        List<SinMovimientoFGEM> paradoFGEM = rFGEM.sinMovimientoFGEM();
        assertEquals(List.of(SituacionFGEM.SIN_VENTAS, SituacionFGEM.VENTA_LENTA), paradoFGEM.stream().map(SinMovimientoFGEM::situacionFGEM).toList());
        igualFGEM("114.00", paradoFGEM.get(0).valorFGEM());            // 12 × 9.50
        igualFGEM("360", paradoFGEM.get(1).diasCoberturaFGEM());
        igualFGEM("306.00", rFGEM.valorInmovilizadoFGEM());
        // Inventario: 114 + 192 + 16 + 25 = 347 → 88.2 % parado
        igualFGEM("347.00", rFGEM.valorInventarioFGEM());
        igualFGEM("0.881844", rFGEM.porcentajeInmovilizadoFGEM());
        assertEquals(1, rFGEM.cantidadFGEM(SituacionFGEM.SIN_VENTAS));
    }

    @Test
    void unPerecibleQueNoSeVendeATiempoEsUrgenteYSeReponeParaPocosDiasFGEM() {
        ProductoFGEM yogurtFGEM = perecibleFGEM(1, "Yogurt fresa", "2.50", "3.00", "20");      // margen 16.7 %; 0.18 por día → 109 días
        ProductoFGEM panFGEM = perecibleFGEM(2, "Pan francés", "0.25", "0.40", "50");          // 100 por día
        ResultadoFGEM rFGEM = calcularFGEM(List.of(yogurtFGEM, panFGEM), 60, ventasFGEM(yogurtFGEM, 11, panFGEM, 6000));

        assertEquals(1, rFGEM.sinMovimientoFGEM().size());
        SinMovimientoFGEM yogurtParadoFGEM = rFGEM.sinMovimientoFGEM().get(0);
        assertEquals(SituacionFGEM.PERECIBLE_EN_RIESGO, yogurtParadoFGEM.situacionFGEM());
        assertTrue(yogurtParadoFGEM.situacionFGEM().sugerenciaFGEM().contains("promoción"));
        // El pan alcanza para medio día: se repone para 3 días (300 − 50), no para 14.
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
        assertEquals(1, rFGEM.sinMovimientoFGEM().size());             // el caramelo tiene stock y no se vendió
        assertTrue(rFGEM.pocosDatosFGEM(), "Con 5 días de ventas las cantidades son orientativas");
        assertFalse(calcularFGEM(List.of(), 30, List.of()).pocosDatosFGEM());
        assertNull(rFGEM.sinMovimientoFGEM().get(0).diasCoberturaFGEM());
        assertNull(rFGEM.porcentajeInmovilizadoFGEM(), "Sin costo, el inventario vale cero");
    }

    // ------------------------------------------------------------------ utilidades

    /** Mismas ventas como período reciente y como historial. */
    private static ResultadoFGEM calcularFGEM(List<ProductoFGEM> productosFGEM, int diasFGEM, List<VentaFGEM> ventasFGEM) {
        PeriodoVentasFGEM periodoFGEM = new PeriodoVentasFGEM(ventasFGEM, diasFGEM);
        return SugerenciasProductoFGEM.calcularFGEM(productosFGEM, periodoFGEM, periodoFGEM, VEINTE_FGEM);
    }

    private static ProductoFGEM productoFGEM(long idFGEM, String nombreFGEM, String costoFGEM, String precioFGEM, String stockFGEM) {
        return new ProductoFGEM(idFGEM, 1, null, null, TipoItemFGEM.PRODUCTO, "P-" + idFGEM, nombreFGEM, null, UnidadMedidaFGEM.UNIDAD,
                bdFGEM(costoFGEM), bdFGEM(precioFGEM), bdFGEM(stockFGEM), true, null, null);
    }

    private static ProductoFGEM perecibleFGEM(long idFGEM, String nombreFGEM, String costoFGEM, String precioFGEM, String stockFGEM) {
        return new ProductoFGEM(idFGEM, 1, null, null, TipoItemFGEM.PRODUCTO, "P-" + idFGEM, nombreFGEM, null, UnidadMedidaFGEM.UNIDAD,
                bdFGEM(costoFGEM), bdFGEM(precioFGEM), bdFGEM(stockFGEM), true, null, null, true);
    }

    /** Pares (producto, cantidad vendida en el período). */
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
