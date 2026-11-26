package com.businessanalyzer.analytics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CalculadoraOperacionTestFGEM {

    @Test
    void calculaSubtotalImpuestoTotalYCostoFGEM() {
        CalculadoraOperacionFGEM.TotalesFGEM tFGEM = CalculadoraOperacionFGEM.calcularFGEM(List.of(
                lineaFGEM("3", "24.90", "18.50"),
                lineaFGEM("2.5", "4.50", "3.20")), new BigDecimal("18"));

        assertEquals(new BigDecimal("85.95"), tFGEM.subtotalFGEM());
        assertEquals(new BigDecimal("15.47"), tFGEM.impuestoFGEM());
        assertEquals(new BigDecimal("101.42"), tFGEM.totalFGEM());
        assertEquals(new BigDecimal("63.50"), tFGEM.costoTotalFGEM(), "3 x 18.50 + 2.5 x 3.20");
        assertEquals(new BigDecimal("22.45"), tFGEM.margenBrutoFGEM());
    }

    @Test
    void redondeaCadaLineaA2DecimalesConRedondeoComercialFGEM() {
        assertEquals(new BigDecimal("0.13"), CalculadoraOperacionFGEM.subtotalLineaFGEM(new BigDecimal("0.5"), new BigDecimal("0.25")));
        assertEquals(new BigDecimal("3.33"), CalculadoraOperacionFGEM.subtotalLineaFGEM(new BigDecimal("1"), new BigDecimal("3.3333")));
    }

    @Test
    void sinImpuestoFGEM() {
        CalculadoraOperacionFGEM.TotalesFGEM tFGEM = CalculadoraOperacionFGEM.calcularFGEM(
                List.of(lineaFGEM("1", "100", "60")), BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), tFGEM.impuestoFGEM());
        assertEquals(new BigDecimal("100.00"), tFGEM.totalFGEM());
    }

    @Test
    void resumenDeOperacionesFGEM() {
        ResumenOperacionesFGEM rFGEM = new ResumenOperacionesFGEM(new BigDecimal("1000"), new BigDecimal("700"),
                new BigDecimal("500"), 4, 1);
        assertEquals(new BigDecimal("300"), rFGEM.margenBrutoFGEM());
        assertEquals(new BigDecimal("0.300000"), rFGEM.margenBrutoSobreVentasFGEM());
        assertEquals(new BigDecimal("250.00"), rFGEM.ticketPromedioFGEM());
        assertNull(ResumenOperacionesFGEM.VACIO_FGEM.margenBrutoSobreVentasFGEM());
        assertNull(ResumenOperacionesFGEM.VACIO_FGEM.ticketPromedioFGEM());
    }

    private static CalculadoraOperacionFGEM.LineaFGEM lineaFGEM(String cantidadFGEM, String precioFGEM, String costoFGEM) {
        return new CalculadoraOperacionFGEM.LineaFGEM(new BigDecimal(cantidadFGEM), new BigDecimal(precioFGEM), new BigDecimal(costoFGEM));
    }
}
