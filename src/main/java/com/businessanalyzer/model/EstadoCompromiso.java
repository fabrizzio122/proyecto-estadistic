package com.businessanalyzer.model;

/** Situación de un compromiso acordado en una visita. */
public enum EstadoCompromiso {

    PENDIENTE("Pendiente"),
    CUMPLIDO("Cumplido"),
    INCUMPLIDO("No cumplido");

    private final String descripcion;

    EstadoCompromiso(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
