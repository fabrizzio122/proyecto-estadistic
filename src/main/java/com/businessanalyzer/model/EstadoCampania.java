package com.businessanalyzer.model;

/** Situación de una campaña agrícola. */
public enum EstadoCampania {

    EN_CURSO("En curso"),
    COSECHADA("Cosechada"),
    PERDIDA("Perdida"),
    ANULADA("Anulada");

    private final String descripcion;

    EstadoCampania(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }

    /** Terminó (cosechada o perdida); sus resultados ya son definitivos. */
    public boolean cerrada() {
        return this == COSECHADA || this == PERDIDA;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
