package com.businessanalyzer.analytics.diagnostico;

/** Áreas en las que se agrupan los indicadores del diagnóstico. */
public enum AreaFGEM {

    RENTABILIDAD("Rentabilidad", "la", "¿El negocio gana dinero?"),
    SOLIDEZ("Solidez", "la", "¿Aguanta una caída de ventas o un mes malo?"),
    CRECIMIENTO("Crecimiento", "el", "¿Las ventas van hacia arriba o hacia abajo?"),
    RIESGO_COMERCIAL("Riesgo comercial", "el", "¿Depende de pocos productos o de ventas irregulares?"),
    AGROPECUARIO("Producción agropecuaria", "la", "¿El hato y los cultivos rinden lo esperado?");

    private final String descripcionFGEM;
    private final String articuloFGEM;
    private final String preguntaFGEM;

    AreaFGEM(String descripcionFGEM, String articuloFGEM, String preguntaFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.articuloFGEM = articuloFGEM;
        this.preguntaFGEM = preguntaFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    /** Nombre en minúsculas con su artículo: "la rentabilidad", "el crecimiento". */
    public String conArticuloFGEM() {
        return articuloFGEM + " " + descripcionFGEM.toLowerCase();
    }

    public String preguntaFGEM() {
        return preguntaFGEM;
    }
}
