package com.businessanalyzer.analytics.diagnostico;

/** Calificación global del negocio según su puntaje (0 a 100). */
public enum NivelSaludFGEM {

    SALUDABLE("Saludable", 75, SemaforoFGEM.VERDE),
    ESTABLE("Estable, con alertas", 55, SemaforoFGEM.AMARILLO),
    EN_RIESGO("En riesgo", 35, SemaforoFGEM.ROJO),
    CRITICO("Crítico", 0, SemaforoFGEM.ROJO),
    SIN_DATOS("Sin datos suficientes", -1, SemaforoFGEM.SIN_DATOS);

    private final String descripcionFGEM;
    private final int puntajeMinimoFGEM;
    private final SemaforoFGEM colorFGEM;

    NivelSaludFGEM(String descripcionFGEM, int puntajeMinimoFGEM, SemaforoFGEM colorFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.puntajeMinimoFGEM = puntajeMinimoFGEM;
        this.colorFGEM = colorFGEM;
    }

    public static NivelSaludFGEM deFGEM(int puntajeFGEM) {
        for (NivelSaludFGEM nivelFGEM : values()) {
            if (nivelFGEM != SIN_DATOS && puntajeFGEM >= nivelFGEM.puntajeMinimoFGEM) {
                return nivelFGEM;
            }
        }
        return CRITICO;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public int puntajeMinimoFGEM() {
        return puntajeMinimoFGEM;
    }

    public SemaforoFGEM colorFGEM() {
        return colorFGEM;
    }
}
