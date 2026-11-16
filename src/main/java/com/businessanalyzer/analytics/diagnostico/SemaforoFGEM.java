package com.businessanalyzer.analytics.diagnostico;

/** Estado de un indicador del diagnóstico. */
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

    /** Puntaje del indicador (0 a 100); −1 si no se pudo evaluar. */
    public int puntajeFGEM() {
        return puntajeFGEM;
    }

    public boolean evaluadoFGEM() {
        return this != SIN_DATOS;
    }
}
