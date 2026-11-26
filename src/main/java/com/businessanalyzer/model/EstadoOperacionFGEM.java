package com.businessanalyzer.model;

public enum EstadoOperacionFGEM {

    REGISTRADA("Registrada"),
    ANULADA("Anulada");

    private final String descripcionFGEM;

    EstadoOperacionFGEM(String descripcionFGEM) {
        this.descripcionFGEM = descripcionFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }
}
