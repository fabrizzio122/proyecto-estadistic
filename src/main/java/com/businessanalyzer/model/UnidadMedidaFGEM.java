package com.businessanalyzer.model;

public enum UnidadMedidaFGEM {

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

    private final String descripcionFGEM;
    private final String abreviaturaFGEM;

    UnidadMedidaFGEM(String descripcionFGEM, String abreviaturaFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.abreviaturaFGEM = abreviaturaFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public String abreviaturaFGEM() {
        return abreviaturaFGEM;
    }

    @Override
    public String toString() {
        return descripcionFGEM + " (" + abreviaturaFGEM + ")";
    }
}
