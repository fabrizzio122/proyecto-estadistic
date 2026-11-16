package com.businessanalyzer.analytics;

import com.businessanalyzer.analytics.FraseMesFGEM.TonoFGEM;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FraseMesTestFGEM {

    @Test
    void ventasArribaYCostosCubiertosVariasVecesFGEM() {
        FraseMesFGEM fFGEM = FraseMesFGEM.deFGEM(new BigDecimal("18114.20"), new BigDecimal("16245"), "octubre",
                new BigDecimal("2.15"), "S/ 0.00");
        assertEquals("Vas 11.5 % arriba de octubre y ya cubriste tus costos del mes más de dos veces.", fFGEM.textoFGEM());
        assertEquals(TonoFGEM.POSITIVO, fFGEM.partesFGEM().get(1).tonoFGEM());
    }

    @Test
    void ventasAbajoYCostosSinCubrirFGEM() {
        FraseMesFGEM fFGEM = FraseMesFGEM.deFGEM(new BigDecimal("800"), new BigDecimal("1000"), "octubre",
                new BigDecimal("0.6"), "S/ 533.33");
        assertEquals("Vas 20.0 % abajo de octubre y te faltan S/ 533.33 para cubrir tus costos del mes.", fFGEM.textoFGEM());
        assertEquals(TonoFGEM.NEGATIVO, fFGEM.partesFGEM().get(1).tonoFGEM());
        assertEquals(TonoFGEM.NEGATIVO, fFGEM.partesFGEM().get(4).tonoFGEM());
    }

    @Test
    void casosLimiteFGEM() {
        assertEquals("Aún no hay ventas registradas este mes.",
                FraseMesFGEM.deFGEM(BigDecimal.ZERO, BigDecimal.TEN, "octubre", null, "").textoFGEM());
        assertEquals("Vas igual que en octubre y ya cubriste tus costos del mes.",
                FraseMesFGEM.deFGEM(new BigDecimal("1004"), new BigDecimal("1000"), "octubre", new BigDecimal("1.3"), "").textoFGEM());
        assertEquals("Ya empezaste a vender este mes, pero tus ventas aún no cubren sus costos.",
                FraseMesFGEM.deFGEM(new BigDecimal("50"), BigDecimal.ZERO, "octubre", null, "").textoFGEM());
    }
}
