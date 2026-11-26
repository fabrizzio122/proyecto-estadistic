package com.businessanalyzer.analytics.diagnostico;

public enum SemaforoFGEM {

    VERDE("Bien", 100),
    AMARILLO("Atención", 50),
    ROJO("Riesgo", 0),
    SIN_DATOS("Sin datos", -1);

    private final String descripcionFGEM;
    private final int puntajeFGEM;

    SemaforoFGEM(String descripcionFGEM, int puntajeFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.puntajeFGEM = puntajeFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public int puntajeFGEM() {
        return puntajeFGEM;
    }

    public boolean evaluadoFGEM() {
        return this != SIN_DATOS;
    }
}
