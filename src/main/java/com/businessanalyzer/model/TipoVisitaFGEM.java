package com.businessanalyzer.model;

/** Motivo de la visita al productor. */
public enum TipoVisitaFGEM {

    EVALUACION_INICIAL("Evaluación inicial"),
    SUPERVISION("Supervisión"),
    SEGUIMIENTO_CREDITO("Seguimiento de crédito"),
    ASISTENCIA_TECNICA("Asistencia técnica"),
    CIERRE("Evaluación final");

    private final String descripcionFGEM;

    TipoVisitaFGEM(String descripcionFGEM) {
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
