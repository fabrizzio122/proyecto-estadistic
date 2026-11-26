package com.businessanalyzer.analytics.estadistica;

import com.businessanalyzer.analytics.estadistica.InferenciaFGEM.AnovaFGEM;
import com.businessanalyzer.analytics.estadistica.InferenciaFGEM.ComparacionFGEM;
import com.businessanalyzer.analytics.estadistica.InferenciaFGEM.IntervaloConfianzaFGEM;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InferenciaTestFGEM {

    private static final double TOLERANCIA_FGEM = 1e-3;

    @Test
    void intervaloDeConfianzaConTDeStudentFGEM() {
        IntervaloConfianzaFGEM iFGEM = InferenciaFGEM.intervaloFGEM(new double[]{2, 4, 6, 8, 10});

        assertEquals(6, iFGEM.mediaFGEM(), TOLERANCIA_FGEM);
        assertEquals(3.9264, iFGEM.margenFGEM(), TOLERANCIA_FGEM);
        assertEquals(2.0736, iFGEM.inferiorFGEM(), TOLERANCIA_FGEM);
        assertEquals(9.9264, iFGEM.superiorFGEM(), TOLERANCIA_FGEM);
        assertFalse(InferenciaFGEM.intervaloFGEM(new double[]{5}).calculadoFGEM(), "Con un solo dato no hay desviación");
    }

    @Test
    void anovaPorDiaDeLaSemanaFGEM() {
        Map<LocalDate, Double> ventasFGEM = new LinkedHashMap<>();
        LocalDate lunesFGEM = LocalDate.of(2026, 11, 2);
        double[] lunesValoresFGEM = {1, 2, 3};
        double[] martesValoresFGEM = {4, 5, 6};
        for (int iFGEM = 0; iFGEM < 3; iFGEM++) {
            ventasFGEM.put(lunesFGEM.plusWeeks(iFGEM), lunesValoresFGEM[iFGEM]);
            ventasFGEM.put(lunesFGEM.plusWeeks(iFGEM).plusDays(1), martesValoresFGEM[iFGEM]);
        }

        AnovaFGEM aFGEM = InferenciaFGEM.anovaDiaSemanaFGEM(ventasFGEM);

        assertEquals(2, aFGEM.gruposFGEM());
        assertEquals(1, aFGEM.glEntreFGEM());
        assertEquals(4, aFGEM.glDentroFGEM());
        assertEquals(13.5, aFGEM.fFGEM(), TOLERANCIA_FGEM);
        assertEquals(0.0213, aFGEM.pFGEM(), TOLERANCIA_FGEM);
        assertEquals(13.5 / 17.5, aFGEM.etaCuadradoFGEM(), TOLERANCIA_FGEM);
        assertTrue(aFGEM.significativoFGEM());
    }

    @Test
    void anovaSinDatosSuficientesFGEM() {
        Map<LocalDate, Double> unaSemanaFGEM = new LinkedHashMap<>();
        for (int iFGEM = 0; iFGEM < 7; iFGEM++) {
            unaSemanaFGEM.put(LocalDate.of(2026, 11, 2).plusDays(iFGEM), 100.0 + iFGEM);
        }
        assertFalse(InferenciaFGEM.anovaDiaSemanaFGEM(unaSemanaFGEM).calculadoFGEM(), "Con una semana, cada día tiene un solo dato");
    }

    @Test
    void pruebaTDeWelchEntreDosPeriodosFGEM() {
        ComparacionFGEM cFGEM = InferenciaFGEM.compararFGEM(new double[]{4, 5, 6}, new double[]{1, 2, 3});

        assertEquals(5, cFGEM.mediaActualFGEM(), TOLERANCIA_FGEM);
        assertEquals(2, cFGEM.mediaAnteriorFGEM(), TOLERANCIA_FGEM);
        assertEquals(1.5, cFGEM.variacionFGEM(), TOLERANCIA_FGEM);
        assertEquals(3.674, cFGEM.tFGEM(), TOLERANCIA_FGEM);
        assertEquals(0.0213, cFGEM.pFGEM(), TOLERANCIA_FGEM);
        assertTrue(cFGEM.significativoFGEM());

        ComparacionFGEM parecidosFGEM = InferenciaFGEM.compararFGEM(new double[]{10, 12, 9, 11}, new double[]{11, 9, 12, 10});
        assertTrue(parecidosFGEM.calculadoFGEM());
        assertFalse(parecidosFGEM.significativoFGEM(), "Mismas ventas en otro orden: no hay diferencia");
        assertFalse(InferenciaFGEM.compararFGEM(new double[]{10, 12}, new double[0]).calculadoFGEM());
    }
}
