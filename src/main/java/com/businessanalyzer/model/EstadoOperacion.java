package com.businessanalyzer.model;

/** Las operaciones no se eliminan: se anulan y dejan de contar en stock y análisis. */
public enum EstadoOperacion {

    REGISTRADA("Registrada"),
    ANULADA("Anulada");

    private final String descripcion;

    EstadoOperacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
