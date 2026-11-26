package com.businessanalyzer.util;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MontosTestFGEM {

    @Test
    void conviertenAEnteroYDeVueltaSinPerderPrecisionFGEM() {
        assertEquals(125_000L, MontosFGEM.aEnteroFGEM(new BigDecimal("12.5")));
        assertEquals(1L, MontosFGEM.aEnteroFGEM(new BigDecimal("0.0001")));
        assertEquals(0, new BigDecimal("12.5").compareTo(MontosFGEM.desdeEnteroFGEM(125_000L)));
        long sumaFGEM = MontosFGEM.aEnteroFGEM(new BigDecimal("0.1")) + MontosFGEM.aEnteroFGEM(new BigDecimal("0.2"));
        assertEquals(0, new BigDecimal("0.3").compareTo(MontosFGEM.desdeEnteroFGEM(sumaFGEM)));
    }

    @Test
    void leeLosFormatosQueEscribeElUsuarioFGEM() {
        assertEquals(new BigDecimal("1250.50"), MontosFGEM.leerFGEM("1250.50", "x"));
        assertEquals(new BigDecimal("1250.50"), MontosFGEM.leerFGEM("1,250.50", "x"));
        assertEquals(new BigDecimal("1234567"), MontosFGEM.leerFGEM("1,234,567", "x"));
        assertEquals(new BigDecimal("12.5"), MontosFGEM.leerFGEM("12,5", "x"));
        assertEquals(new BigDecimal("15"), MontosFGEM.leerFGEM(" S/ 15 ", "x"));
        assertThrows(ValidacionExceptionFGEM.class, () -> MontosFGEM.leerFGEM("", "x"));
        assertThrows(ValidacionExceptionFGEM.class, () -> MontosFGEM.leerFGEM("doce", "x"));
        assertThrows(ValidacionExceptionFGEM.class, () -> MontosFGEM.leerFGEM("1.2.3", "x"));
    }

    @Test
    void daFormatoParaMostrarFGEM() {
        assertEquals("S/ 1,234.50", MontosFGEM.formatearFGEM(new BigDecimal("1234.5"), MonedaFGEM.PEN));
        assertEquals("-US$ 5.00", MontosFGEM.formatearFGEM(new BigDecimal("-5"), MonedaFGEM.USD));
        assertEquals("S/ 0.1235", MontosFGEM.formatearFGEM(new BigDecimal("0.12345"), MonedaFGEM.PEN));
        assertEquals("25.5 %", MontosFGEM.porcentajeFGEM(new BigDecimal("0.255")));
        assertEquals("—", MontosFGEM.porcentajeFGEM(null));
        assertEquals("12.50", MontosFGEM.paraEditarFGEM(new BigDecimal("12.5000")));
        assertEquals("100.00", MontosFGEM.paraEditarFGEM(new BigDecimal("100")));
        assertEquals("0.125", MontosFGEM.paraEditarFGEM(new BigDecimal("0.1250")));
    }
}
