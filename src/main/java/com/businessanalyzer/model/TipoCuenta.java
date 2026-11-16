package com.businessanalyzer.model;

/** Dónde está el dinero de la empresa. */
public enum TipoCuenta {

    CAJA("Caja (efectivo)"),
    BANCO("Cuenta bancaria"),
    BILLETERA("Billetera digital (Yape, Plin…)");

    private final String descripcion;

    TipoCuenta(String descripcion) {
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
