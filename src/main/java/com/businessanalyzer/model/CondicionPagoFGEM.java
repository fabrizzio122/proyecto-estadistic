package com.businessanalyzer.model;

public enum CondicionPagoFGEM {

    CONTADO("Al contado"),
    CREDITO("Al crédito");

    private final String descripcionFGEM;

    CondicionPagoFGEM(String descripcionFGEM) {
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
