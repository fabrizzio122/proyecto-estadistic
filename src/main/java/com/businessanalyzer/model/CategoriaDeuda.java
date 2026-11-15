package com.businessanalyzer.model;

/** Tipos de deuda (pasivos) de una empresa. */
public enum CategoriaDeuda {

    PRESTAMO_BANCARIO("Préstamo de banco o financiera"),
    PRESTAMO_TERCEROS("Préstamo de familiares o terceros"),
    PROVEEDORES("Deuda con proveedores"),
    TRIBUTOS("Impuestos por pagar"),
    REMUNERACIONES("Sueldos y beneficios por pagar"),
    TARJETA_CREDITO("Tarjeta de crédito"),
    OTRAS("Otras deudas");

    private final String descripcion;

    CategoriaDeuda(String descripcion) {
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
