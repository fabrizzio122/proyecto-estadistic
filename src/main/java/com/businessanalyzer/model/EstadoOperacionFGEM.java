package com.businessanalyzer.model;

/** Las operaciones no se eliminan: se anulan y dejan de contar en stock y análisis. */
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
