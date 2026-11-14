package com.businessanalyzer.analytics.diagnostico;

/** Calificación global del negocio según su puntaje (0 a 100). */
public enum NivelSalud {

    SALUDABLE("Saludable", 75, Semaforo.VERDE),
    ESTABLE("Estable, con alertas", 55, Semaforo.AMARILLO),
    EN_RIESGO("En riesgo", 35, Semaforo.ROJO),
    CRITICO("Crítico", 0, Semaforo.ROJO),
    SIN_DATOS("Sin datos suficientes", -1, Semaforo.SIN_DATOS);

    private final String descripcion;
    private final int puntajeMinimo;
    private final Semaforo color;

    NivelSalud(String descripcion, int puntajeMinimo, Semaforo color) {
        this.descripcion = descripcion;
        this.puntajeMinimo = puntajeMinimo;
        this.color = color;
    }

    public static NivelSalud de(int puntaje) {
        for (NivelSalud nivel : values()) {
            if (nivel != SIN_DATOS && puntaje >= nivel.puntajeMinimo) {
                return nivel;
            }
        }
        return CRITICO;
    }

    public String descripcion() {
        return descripcion;
    }

    public int puntajeMinimo() {
        return puntajeMinimo;
    }

    public Semaforo color() {
        return color;
    }
}
