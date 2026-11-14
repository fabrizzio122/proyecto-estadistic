package com.businessanalyzer.analytics.diagnostico;

/** Estado de un indicador del diagnóstico. */
public enum Semaforo {

    VERDE("Bien", 100),
    AMARILLO("Atención", 50),
    ROJO("Riesgo", 0),
    SIN_DATOS("Sin datos", -1);

    private final String descripcion;
    private final int puntaje;

    Semaforo(String descripcion, int puntaje) {
        this.descripcion = descripcion;
        this.puntaje = puntaje;
    }

    public String descripcion() {
        return descripcion;
    }

    /** Puntaje del indicador (0 a 100); −1 si no se pudo evaluar. */
    public int puntaje() {
        return puntaje;
    }

    public boolean evaluado() {
        return this != SIN_DATOS;
    }
}
