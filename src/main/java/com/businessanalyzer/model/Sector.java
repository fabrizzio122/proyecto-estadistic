package com.businessanalyzer.model;

/** Sector económico de la empresa. Orienta los análisis y diagnósticos de fases posteriores. */
public enum Sector {

    COMERCIO("Comercio"),
    TIENDA("Tienda minorista"),
    RESTAURANTE("Restaurante y alimentos"),
    SERVICIOS("Servicios"),
    PRODUCCION("Producción y manufactura"),
    EMPRENDIMIENTO("Emprendimiento"),
    OTRO("Otro");

    private final String descripcion;

    Sector(String descripcion) {
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
