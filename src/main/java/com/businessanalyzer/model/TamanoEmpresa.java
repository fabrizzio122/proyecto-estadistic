package com.businessanalyzer.model;

/** Tamaño de la empresa según su volumen de operaciones. */
public enum TamanoEmpresa {

    MICRO("Microempresa"),
    PEQUENA("Pequeña empresa"),
    MEDIANA("Mediana empresa"),
    GRANDE("Gran empresa");

    private final String descripcion;

    TamanoEmpresa(String descripcion) {
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
