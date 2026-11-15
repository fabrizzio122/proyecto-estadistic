package com.businessanalyzer.model;

/** Forma de tenencia de una parcela. */
public enum Tenencia {

    PROPIA("Propia"),
    ALQUILADA("Alquilada"),
    COMUNAL("Comunal"),
    AL_PARTIR("Al partir (aparcería)");

    private final String descripcion;

    Tenencia(String descripcion) {
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
