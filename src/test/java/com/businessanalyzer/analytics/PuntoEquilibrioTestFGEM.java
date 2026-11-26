package com.businessanalyzer.analytics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuntoEquilibrioTestFGEM {

    @Test
    void casoClasicoDeLibroFGEM() {
        PuntoEquilibrioFGEM peFGEM = PuntoEquilibrioFGEM.calcularFGEM(bdFGEM("1000"), bdFGEM("600"), bdFGEM("300"));

        assertEquals(0, bdFGEM("400").compareTo(peFGEM.margenContribucionFGEM()));
        assertEquals(bdFGEM("0.400000"), peFGEM.razonContribucionFGEM());
        assertEquals(bdFGEM("750.00"), peFGEM.ventasEquilibrioFGEM());
        assertEquals(bdFGEM("0.250000"), peFGEM.margenSeguridadFGEM(), "Las ventas pueden caer 25 %");
        assertEquals(bdFGEM("1.333333"), peFGEM.coberturaFGEM());
        assertEquals(0, BigDecimal.ZERO.compareTo(peFGEM.ventasFaltantesFGEM()));
        assertTrue(peFGEM.alcanzadoFGEM());
    }

    @Test
    void cuandoNoSeAlcanzaIndicaCuantoFaltaFGEM() {
        PuntoEquilibrioFGEM peFGEM = PuntoEquilibrioFGEM.calcularFGEM(bdFGEM("500"), bdFGEM("300"), bdFGEM("300"));

        assertEquals(bdFGEM("750.00"), peFGEM.ventasEquilibrioFGEM());
        assertEquals(0, bdFGEM("250").compareTo(peFGEM.ventasFaltantesFGEM()));
        assertEquals(bdFGEM("-0.500000"), peFGEM.margenSeguridadFGEM());
        assertFalse(peFGEM.alcanzadoFGEM());
    }

    @Test
    void sinMargenDeContribucionNoHayEquilibrioPosibleFGEM() {
        PuntoEquilibrioFGEM peFGEM = PuntoEquilibrioFGEM.calcularFGEM(bdFGEM("100"), bdFGEM("120"), bdFGEM("50"));
        assertNull(peFGEM.ventasEquilibrioFGEM());
        assertNull(peFGEM.margenSeguridadFGEM());
        assertFalse(peFGEM.alcanzadoFGEM());

        PuntoEquilibrioFGEM sinVentasFGEM = PuntoEquilibrioFGEM.calcularFGEM(BigDecimal.ZERO, BigDecimal.ZERO, bdFGEM("50"));
        assertNull(sinVentasFGEM.razonContribucionFGEM());
        assertNull(sinVentasFGEM.ventasEquilibrioFGEM());
    }

    @Test
    void sinCostosFijosElEquilibrioEsCeroFGEM() {
        PuntoEquilibrioFGEM peFGEM = PuntoEquilibrioFGEM.calcularFGEM(bdFGEM("100"), bdFGEM("60"), BigDecimal.ZERO);
        assertEquals(0, BigDecimal.ZERO.compareTo(peFGEM.ventasEquilibrioFGEM()));
        assertTrue(peFGEM.alcanzadoFGEM());
    }

    @Test
    void elPeriodoAnteriorRespetaMesesCompletosODiasFGEM() {
        PeriodoFGEM noviembreFGEM = PeriodoFGEM.mesFGEM(YearMonth.of(2026, 11));
        assertEquals(PeriodoFGEM.mesFGEM(YearMonth.of(2026, 10)), noviembreFGEM.anteriorFGEM());
        assertEquals("noviembre 2026", noviembreFGEM.descripcionFGEM());

        PeriodoFGEM primerTrimestreFGEM = new PeriodoFGEM(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
        assertEquals(new PeriodoFGEM(LocalDate.of(2025, 10, 1), LocalDate.of(2025, 12, 31)), primerTrimestreFGEM.anteriorFGEM());
        assertEquals("enero – marzo 2026", primerTrimestreFGEM.descripcionFGEM());

        PeriodoFGEM parcialFGEM = new PeriodoFGEM(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 13));
        assertEquals(new PeriodoFGEM(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 13)), parcialFGEM.anteriorFGEM());
        assertEquals("del 1 al 13 de noviembre 2026", parcialFGEM.descripcionFGEM());

        PeriodoFGEM anioEnCursoFGEM = new PeriodoFGEM(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 13));
        assertEquals(new PeriodoFGEM(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 11, 13)), anioEnCursoFGEM.anteriorFGEM());

        PeriodoFGEM libreFGEM = new PeriodoFGEM(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 13));
        assertEquals(new PeriodoFGEM(LocalDate.of(2026, 10, 27), LocalDate.of(2026, 11, 4)), libreFGEM.anteriorFGEM());
    }

    @Test
    void variacionRespectoAlPeriodoAnteriorFGEM() {
        assertEquals(bdFGEM("0.250000"), new VariacionFGEM(bdFGEM("125"), bdFGEM("100")).relativaFGEM());
        assertEquals(bdFGEM("0.500000"), new VariacionFGEM(bdFGEM("-50"), bdFGEM("-100")).relativaFGEM(), "Perder menos es mejorar");
        assertNull(new VariacionFGEM(bdFGEM("10"), BigDecimal.ZERO).relativaFGEM());
    }

    private static BigDecimal bdFGEM(String valorFGEM) {
        return new BigDecimal(valorFGEM);
    }
}
