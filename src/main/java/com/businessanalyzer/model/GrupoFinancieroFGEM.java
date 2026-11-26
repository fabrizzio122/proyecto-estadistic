package com.businessanalyzer.model;

public enum GrupoFinancieroFGEM {

    GASTO_OPERATIVO("Gastos operativos"),
    GASTO_FINANCIERO("Gastos financieros"),
    IMPUESTO_RENTA("Impuesto a la renta"),
    INGRESO_FINANCIERO("Ingresos financieros"),
    OTRO_INGRESO("Otros ingresos");

    private final String descripcionFGEM;

    GrupoFinancieroFGEM(String descripcionFGEM) {
        this.descripcionFGEM = descripcionFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }
}
