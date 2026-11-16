package com.businessanalyzer.model;

/** Situación de una campaña agrícola. */
public enum EstadoCampaniaFGEM {

    EN_CURSO("En curso"),
    COSECHADA("Cosechada"),
    PERDIDA("Perdida"),
    ANULADA("Anulada");

    private final String descripcionFGEM;

    EstadoCampaniaFGEM(String descripcionFGEM) {
        this.descripcionFGEM = descripcionFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    /** Terminó (cosechada o perdida); sus resultados ya son definitivos. */
    public boolean cerradaFGEM() {
        return this == COSECHADA || this == PERDIDA;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
