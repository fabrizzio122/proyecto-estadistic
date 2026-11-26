package com.businessanalyzer.model;

public enum TipoMovimientoFGEM {

    INGRESO("Ingreso", "Ingresos"),
    GASTO("Gasto", "Gastos");

    private final String descripcionFGEM;
    private final String pluralFGEM;

    TipoMovimientoFGEM(String descripcionFGEM, String pluralFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.pluralFGEM = pluralFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public String pluralFGEM() {
        return pluralFGEM;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
