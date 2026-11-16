package com.businessanalyzer.analytics;

import com.businessanalyzer.analytics.FraseMes.Tono;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FraseMesTest {

    @Test
    void ventasArribaYCostosCubiertosVariasVeces() {
        FraseMes f = FraseMes.de(new BigDecimal("18114.20"), new BigDecimal("16245"), "octubre",
                new BigDecimal("2.15"), "S/ 0.00");
        assertEquals("Vas 11.5 % arriba de octubre y ya cubriste tus costos del mes más de dos veces.", f.texto());
        assertEquals(Tono.POSITIVO, f.partes().get(1).tono());
    }

    @Test
    void ventasAbajoYCostosSinCubrir() {
        FraseMes f = FraseMes.de(new BigDecimal("800"), new BigDecimal("1000"), "octubre",
                new BigDecimal("0.6"), "S/ 533.33");
        assertEquals("Vas 20.0 % abajo de octubre y te faltan S/ 533.33 para cubrir tus costos del mes.", f.texto());
        assertEquals(Tono.NEGATIVO, f.partes().get(1).tono());
        assertEquals(Tono.NEGATIVO, f.partes().get(4).tono());
    }

    @Test
    void casosLimite() {
        assertEquals("Aún no hay ventas registradas este mes.",
                FraseMes.de(BigDecimal.ZERO, BigDecimal.TEN, "octubre", null, "").texto());
        assertEquals("Vas igual que en octubre y ya cubriste tus costos del mes.",
                FraseMes.de(new BigDecimal("1004"), new BigDecimal("1000"), "octubre", new BigDecimal("1.3"), "").texto());
        assertEquals("Ya empezaste a vender este mes, pero tus ventas aún no cubren sus costos.",
                FraseMes.de(new BigDecimal("50"), BigDecimal.ZERO, "octubre", null, "").texto());
    }
}
