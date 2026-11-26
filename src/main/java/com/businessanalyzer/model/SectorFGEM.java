package com.businessanalyzer.model;

public enum SectorFGEM {

    COMERCIO("Comercio"),
    TIENDA("Tienda minorista"),
    RESTAURANTE("Restaurante y alimentos"),
    SERVICIOS("Servicios"),
    PRODUCCION("Producción y manufactura"),
    EMPRENDIMIENTO("Emprendimiento"),
    AGROPECUARIO("Agropecuario"),
    OTRO("Otro");

    private final String descripcionFGEM;

    SectorFGEM(String descripcionFGEM) {
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
