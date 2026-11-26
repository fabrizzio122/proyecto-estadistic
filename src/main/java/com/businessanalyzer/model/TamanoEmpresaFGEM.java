package com.businessanalyzer.model;

public enum TamanoEmpresaFGEM {

    MICRO("Microempresa"),
    PEQUENA("Pequeña empresa"),
    MEDIANA("Mediana empresa"),
    GRANDE("Gran empresa");

    private final String descripcionFGEM;

    TamanoEmpresaFGEM(String descripcionFGEM) {
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
