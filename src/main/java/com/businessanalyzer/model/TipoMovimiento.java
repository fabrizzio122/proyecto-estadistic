package com.businessanalyzer.model;

/** Un movimiento financiero es un ingreso o un gasto. */
public enum TipoMovimiento {

    INGRESO("Ingreso", "Ingresos"),
    GASTO("Gasto", "Gastos");

    private final String descripcion;
    private final String plural;

    TipoMovimiento(String descripcion, String plural) {
        this.descripcion = descripcion;
        this.plural = plural;
    }

    public String descripcion() {
        return descripcion;
    }

    public String plural() {
        return plural;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
