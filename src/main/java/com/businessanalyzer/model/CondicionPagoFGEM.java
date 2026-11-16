package com.businessanalyzer.model;

/**
 * Cómo se paga una venta o compra: al contado (el dinero se mueve el mismo día) o al crédito
 * (queda por cobrar o por pagar hasta que se registran los cobros o pagos).
 */
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
