package com.businessanalyzer.model;

import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;

import java.math.BigDecimal;

public enum ResultadoAspectoFGEM {

    CUMPLE("Cumple", SemaforoFGEM.VERDE, BigDecimal.ONE),
    PARCIAL("Cumple en parte", SemaforoFGEM.AMARILLO, new BigDecimal("0.5")),
    NO_CUMPLE("No cumple", SemaforoFGEM.ROJO, BigDecimal.ZERO),
    NO_APLICA("No aplica", SemaforoFGEM.SIN_DATOS, null);

    private final String descripcionFGEM;
    private final SemaforoFGEM semaforoFGEM;
    private final BigDecimal puntajeFGEM;

    ResultadoAspectoFGEM(String descripcionFGEM, SemaforoFGEM semaforoFGEM, BigDecimal puntajeFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.semaforoFGEM = semaforoFGEM;
        this.puntajeFGEM = puntajeFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public SemaforoFGEM semaforoFGEM() {
        return semaforoFGEM;
    }

    public BigDecimal puntajeFGEM() {
        return puntajeFGEM;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
