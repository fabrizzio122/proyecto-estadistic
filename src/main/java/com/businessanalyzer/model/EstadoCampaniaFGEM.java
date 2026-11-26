package com.businessanalyzer.model;

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

    public boolean cerradaFGEM() {
        return this == COSECHADA || this == PERDIDA;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
