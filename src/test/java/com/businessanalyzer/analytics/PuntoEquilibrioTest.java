package com.businessanalyzer.analytics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuntoEquilibrioTest {

    @Test
    void casoClasicoDeLibro() {
        // Ventas 1000, costos variables 600 -> razón de contribución 40 %; fijos 300 -> equilibrio 750
        PuntoEquilibrio pe = PuntoEquilibrio.calcular(bd("1000"), bd("600"), bd("300"));

        assertEquals(0, bd("400").compareTo(pe.margenContribucion()));
        assertEquals(bd("0.400000"), pe.razonContribucion());
        assertEquals(bd("750.00"), pe.ventasEquilibrio());
        assertEquals(bd("0.250000"), pe.margenSeguridad(), "Las ventas pueden caer 25 %");
        assertEquals(bd("1.333333"), pe.cobertura());
        assertEquals(0, BigDecimal.ZERO.compareTo(pe.ventasFaltantes()));
        assertTrue(pe.alcanzado());
    }

    @Test
    void cuandoNoSeAlcanzaIndicaCuantoFalta() {
        PuntoEquilibrio pe = PuntoEquilibrio.calcular(bd("500"), bd("300"), bd("300"));

        assertEquals(bd("750.00"), pe.ventasEquilibrio());
        assertEquals(0, bd("250").compareTo(pe.ventasFaltantes()));
        assertEquals(bd("-0.500000"), pe.margenSeguridad());
        assertFalse(pe.alcanzado());
    }

    @Test
    void sinMargenDeContribucionNoHayEquilibrioPosible() {
        PuntoEquilibrio pe = PuntoEquilibrio.calcular(bd("100"), bd("120"), bd("50"));
        assertNull(pe.ventasEquilibrio());
        assertNull(pe.margenSeguridad());
        assertFalse(pe.alcanzado());

        PuntoEquilibrio sinVentas = PuntoEquilibrio.calcular(BigDecimal.ZERO, BigDecimal.ZERO, bd("50"));
        assertNull(sinVentas.razonContribucion());
        assertNull(sinVentas.ventasEquilibrio());
    }

    @Test
    void sinCostosFijosElEquilibrioEsCero() {
        PuntoEquilibrio pe = PuntoEquilibrio.calcular(bd("100"), bd("60"), BigDecimal.ZERO);
        assertEquals(0, BigDecimal.ZERO.compareTo(pe.ventasEquilibrio()));
        assertTrue(pe.alcanzado());
    }

    @Test
    void elPeriodoAnteriorRespetaMesesCompletosODias() {
        Periodo noviembre = Periodo.mes(YearMonth.of(2026, 11));
        assertEquals(Periodo.mes(YearMonth.of(2026, 10)), noviembre.anterior());
        assertEquals("noviembre 2026", noviembre.descripcion());

        Periodo primerTrimestre = new Periodo(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
        assertEquals(new Periodo(LocalDate.of(2025, 10, 1), LocalDate.of(2025, 12, 31)), primerTrimestre.anterior());
        assertEquals("enero – marzo 2026", primerTrimestre.descripcion());

        Periodo parcial = new Periodo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 13));
        assertEquals(new Periodo(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 31)), parcial.anterior());
        assertEquals(13, parcial.anterior().dias());
    }

    @Test
    void variacionRespectoAlPeriodoAnterior() {
        assertEquals(bd("0.250000"), new Variacion(bd("125"), bd("100")).relativa());
        assertEquals(bd("0.500000"), new Variacion(bd("-50"), bd("-100")).relativa(), "Perder menos es mejorar");
        assertNull(new Variacion(bd("10"), BigDecimal.ZERO).relativa());
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }
}
