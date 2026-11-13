package com.businessanalyzer.model;

/** Lugar que ocupa cada categoría en el estado de resultados (Fase 7). */
public enum GrupoFinanciero {

    GASTO_OPERATIVO("Gastos operativos"),
    GASTO_FINANCIERO("Gastos financieros"),
    IMPUESTO_RENTA("Impuesto a la renta"),
    INGRESO_FINANCIERO("Ingresos financieros"),
    OTRO_INGRESO("Otros ingresos");

    private final String descripcion;

    GrupoFinanciero(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
