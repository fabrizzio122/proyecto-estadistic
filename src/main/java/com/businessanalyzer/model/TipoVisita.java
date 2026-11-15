package com.businessanalyzer.model;

/** Motivo de la visita al productor. */
public enum TipoVisita {

    EVALUACION_INICIAL("Evaluación inicial"),
    SUPERVISION("Supervisión"),
    SEGUIMIENTO_CREDITO("Seguimiento de crédito"),
    ASISTENCIA_TECNICA("Asistencia técnica"),
    CIERRE("Evaluación final");

    private final String descripcion;

    TipoVisita(String descripcion) {
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
