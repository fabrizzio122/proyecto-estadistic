package com.businessanalyzer.analytics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CalculadoraOperacionTest {

    @Test
    void calculaSubtotalImpuestoTotalYCosto() {
        // 3 x 24.90 = 74.70 ; 2.5 x 4.50 = 11.25 ; subtotal 85.95 ; IGV 18 % = 15.471 -> 15.47
        CalculadoraOperacion.Totales t = CalculadoraOperacion.calcular(List.of(
                linea("3", "24.90", "18.50"),
                linea("2.5", "4.50", "3.20")), new BigDecimal("18"));

        assertEquals(new BigDecimal("85.95"), t.subtotal());
        assertEquals(new BigDecimal("15.47"), t.impuesto());
        assertEquals(new BigDecimal("101.42"), t.total());
        assertEquals(new BigDecimal("63.50"), t.costoTotal(), "3 x 18.50 + 2.5 x 3.20");
        assertEquals(new BigDecimal("22.45"), t.margenBruto());
    }

    @Test
    void redondeaCadaLineaA2DecimalesConRedondeoComercial() {
        assertEquals(new BigDecimal("0.13"), CalculadoraOperacion.subtotalLinea(new BigDecimal("0.5"), new BigDecimal("0.25")));
        assertEquals(new BigDecimal("3.33"), CalculadoraOperacion.subtotalLinea(new BigDecimal("1"), new BigDecimal("3.3333")));
    }

    @Test
    void sinImpuesto() {
        CalculadoraOperacion.Totales t = CalculadoraOperacion.calcular(
                List.of(linea("1", "100", "60")), BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), t.impuesto());
        assertEquals(new BigDecimal("100.00"), t.total());
    }

    @Test
    void resumenDeOperaciones() {
        ResumenOperaciones r = new ResumenOperaciones(new BigDecimal("1000"), new BigDecimal("700"),
                new BigDecimal("500"), 4, 1);
        assertEquals(new BigDecimal("300"), r.margenBruto());
        assertEquals(new BigDecimal("0.300000"), r.margenBrutoSobreVentas());
        assertEquals(new BigDecimal("250.00"), r.ticketPromedio());
        assertNull(ResumenOperaciones.VACIO.margenBrutoSobreVentas());
        assertNull(ResumenOperaciones.VACIO.ticketPromedio());
    }

    private static CalculadoraOperacion.Linea linea(String cantidad, String precio, String costo) {
        return new CalculadoraOperacion.Linea(new BigDecimal(cantidad), new BigDecimal(precio), new BigDecimal(costo));
    }
}
