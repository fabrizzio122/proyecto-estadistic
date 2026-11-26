package com.businessanalyzer.model;

public enum TenenciaFGEM {

    PROPIA("Propia"),
    ALQUILADA("Alquilada"),
    COMUNAL("Comunal"),
    AL_PARTIR("Al partir (aparcería)");

    private final String descripcionFGEM;

    TenenciaFGEM(String descripcionFGEM) {
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
