package com.businessanalyzer.analytics.tesoreria;

public enum ActividadFGEM {

    OPERACION("Actividades de operación"),
    INVERSION("Actividades de inversión"),
    FINANCIAMIENTO("Actividades de financiamiento"),
    INTERNA("Transferencias entre cuentas");

    private final String descripcionFGEM;

    ActividadFGEM(String descripcionFGEM) {
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
