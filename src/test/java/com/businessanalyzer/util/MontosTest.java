package com.businessanalyzer.util;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Moneda;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MontosTest {

    @Test
    void conviertenAEnteroYDeVueltaSinPerderPrecision() {
        assertEquals(125_000L, Montos.aEntero(new BigDecimal("12.5")));
        assertEquals(1L, Montos.aEntero(new BigDecimal("0.0001")));
        assertEquals(0, new BigDecimal("12.5").compareTo(Montos.desdeEntero(125_000L)));
        // 0.1 + 0.2 con double daría 0.30000000000000004
        long suma = Montos.aEntero(new BigDecimal("0.1")) + Montos.aEntero(new BigDecimal("0.2"));
        assertEquals(0, new BigDecimal("0.3").compareTo(Montos.desdeEntero(suma)));
    }

    @Test
    void leeLosFormatosQueEscribeElUsuario() {
        assertEquals(new BigDecimal("1250.50"), Montos.leer("1250.50", "x"));
        assertEquals(new BigDecimal("1250.50"), Montos.leer("1,250.50", "x"));
        assertEquals(new BigDecimal("1234567"), Montos.leer("1,234,567", "x"));
        assertEquals(new BigDecimal("12.5"), Montos.leer("12,5", "x"));
        assertEquals(new BigDecimal("15"), Montos.leer(" S/ 15 ", "x"));
        assertThrows(ValidacionException.class, () -> Montos.leer("", "x"));
        assertThrows(ValidacionException.class, () -> Montos.leer("doce", "x"));
        assertThrows(ValidacionException.class, () -> Montos.leer("1.2.3", "x"));
    }

    @Test
    void daFormatoParaMostrar() {
        assertEquals("S/ 1,234.50", Montos.formatear(new BigDecimal("1234.5"), Moneda.PEN));
        assertEquals("-US$ 5.00", Montos.formatear(new BigDecimal("-5"), Moneda.USD));
        assertEquals("S/ 0.1235", Montos.formatear(new BigDecimal("0.12345"), Moneda.PEN));
        assertEquals("25.5 %", Montos.porcentaje(new BigDecimal("0.255")));
        assertEquals("—", Montos.porcentaje(null));
        assertEquals("12.50", Montos.paraEditar(new BigDecimal("12.5000")));
        assertEquals("100.00", Montos.paraEditar(new BigDecimal("100")));
        assertEquals("0.125", Montos.paraEditar(new BigDecimal("0.1250")));
    }
}
