package com.businessanalyzer.model;

/** Tipos de deuda (pasivos) de una empresa. */
public enum CategoriaDeudaFGEM {

    PRESTAMO_BANCARIO("Préstamo de banco o financiera"),
    PRESTAMO_TERCEROS("Préstamo de familiares o terceros"),
    PROVEEDORES("Deuda con proveedores"),
    TRIBUTOS("Impuestos por pagar"),
    REMUNERACIONES("Sueldos y beneficios por pagar"),
    TARJETA_CREDITO("Tarjeta de crédito"),
    OTRAS("Otras deudas");

    private final String descripcionFGEM;

    CategoriaDeudaFGEM(String descripcionFGEM) {
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
