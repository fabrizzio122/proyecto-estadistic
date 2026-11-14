package com.businessanalyzer.analytics.diagnostico;

/** Áreas en las que se agrupan los indicadores del diagnóstico. */
public enum Area {

    RENTABILIDAD("Rentabilidad", "la", "¿El negocio gana dinero?"),
    SOLIDEZ("Solidez", "la", "¿Aguanta una caída de ventas o un mes malo?"),
    CRECIMIENTO("Crecimiento", "el", "¿Las ventas van hacia arriba o hacia abajo?"),
    RIESGO_COMERCIAL("Riesgo comercial", "el", "¿Depende de pocos productos o de ventas irregulares?");

    private final String descripcion;
    private final String articulo;
    private final String pregunta;

    Area(String descripcion, String articulo, String pregunta) {
        this.descripcion = descripcion;
        this.articulo = articulo;
        this.pregunta = pregunta;
    }

    public String descripcion() {
        return descripcion;
    }

    /** Nombre en minúsculas con su artículo: "la rentabilidad", "el crecimiento". */
    public String conArticulo() {
        return articulo + " " + descripcion.toLowerCase();
    }

    public String pregunta() {
        return pregunta;
    }
}
