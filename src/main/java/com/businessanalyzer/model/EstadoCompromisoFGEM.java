package com.businessanalyzer.model;

/** Situación de un compromiso acordado en una visita. */
public enum EstadoCompromisoFGEM {

    PENDIENTE("Pendiente"),
    CUMPLIDO("Cumplido"),
    INCUMPLIDO("No cumplido");

    private final String descripcionFGEM;

    EstadoCompromisoFGEM(String descripcionFGEM) {
        this.descripcionFGEM = descripcionFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
