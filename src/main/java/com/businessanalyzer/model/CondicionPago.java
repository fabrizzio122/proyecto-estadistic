package com.businessanalyzer.model;

/**
 * Cómo se paga una venta o compra: al contado (el dinero se mueve el mismo día) o al crédito
 * (queda por cobrar o por pagar hasta que se registran los cobros o pagos).
 */
public enum CondicionPago {

    CONTADO("Al contado"),
    CREDITO("Al crédito");

    private final String descripcion;

    CondicionPago(String descripcion) {
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
