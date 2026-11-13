package com.businessanalyzer.model;

/** Unidad en la que se vende y se cuenta un producto o servicio. */
public enum UnidadMedida {

    UNIDAD("Unidad", "und"),
    DOCENA("Docena", "doc"),
    CAJA("Caja", "caja"),
    PAQUETE("Paquete", "paq"),
    KILOGRAMO("Kilogramo", "kg"),
    GRAMO("Gramo", "g"),
    LITRO("Litro", "l"),
    MILILITRO("Mililitro", "ml"),
    METRO("Metro", "m"),
    METRO_CUADRADO("Metro cuadrado", "m²"),
    HORA("Hora", "h"),
    DIA("Día", "día"),
    MES("Mes", "mes"),
    SERVICIO("Servicio", "serv");

    private final String descripcion;
    private final String abreviatura;

    UnidadMedida(String descripcion, String abreviatura) {
        this.descripcion = descripcion;
        this.abreviatura = abreviatura;
    }

    public String descripcion() {
        return descripcion;
    }

    public String abreviatura() {
        return abreviatura;
    }

    @Override
    public String toString() {
        return descripcion + " (" + abreviatura + ")";
    }
}
