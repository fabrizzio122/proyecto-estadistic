package com.businessanalyzer.analytics.diagnostico;

/** Áreas en las que se agrupan los indicadores del diagnóstico. */
public enum Area {

    RENTABILIDAD("Rentabilidad", "¿El negocio gana dinero?"),
    SOLIDEZ("Solidez", "¿Aguanta una caída de ventas o un mes malo?"),
    CRECIMIENTO("Crecimiento", "¿Las ventas van hacia arriba o hacia abajo?"),
    RIESGO_COMERCIAL("Riesgo comercial", "¿Depende de pocos productos o de ventas irregulares?");

    private final String descripcion;
    private final String pregunta;

    Area(String descripcion, String pregunta) {
        this.descripcion = descripcion;
        this.pregunta = pregunta;
    }

    public String descripcion() {
        return descripcion;
    }

    public String pregunta() {
        return pregunta;
    }
}
