package com.businessanalyzer.analytics.tesoreria;

/**
 * Actividades del estado de flujo de efectivo (NIC 7). Las transferencias entre cuentas son internas:
 * cambian dónde está el dinero, no cuánto hay.
 */
public enum Actividad {

    OPERACION("Actividades de operación"),
    INVERSION("Actividades de inversión"),
    FINANCIAMIENTO("Actividades de financiamiento"),
    INTERNA("Transferencias entre cuentas");

    private final String descripcion;

    Actividad(String descripcion) {
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
