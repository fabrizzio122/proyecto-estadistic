package com.businessanalyzer.model;

public enum TipoCuentaFGEM {

    CAJA("Caja (efectivo)"),
    BANCO("Cuenta bancaria"),
    BILLETERA("Billetera digital (Yape, Plin…)");

    private final String descripcionFGEM;

    TipoCuentaFGEM(String descripcionFGEM) {
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
