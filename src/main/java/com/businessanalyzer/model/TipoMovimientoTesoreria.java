package com.businessanalyzer.model;

/**
 * Movimientos que mueven dinero sin ser ingresos ni gastos: no cambian la utilidad, solo dónde está
 * el dinero o cuánto puso o sacó el dueño.
 */
public enum TipoMovimientoTesoreria {

    TRANSFERENCIA("Transferencia entre cuentas", true, true),
    APORTE("Aporte del dueño", false, true),
    RETIRO("Retiro del dueño", true, false);

    private final String descripcion;
    private final boolean usaOrigen;
    private final boolean usaDestino;

    TipoMovimientoTesoreria(String descripcion, boolean usaOrigen, boolean usaDestino) {
        this.descripcion = descripcion;
        this.usaOrigen = usaOrigen;
        this.usaDestino = usaDestino;
    }

    public String descripcion() {
        return descripcion;
    }

    /** El dinero sale de una cuenta. */
    public boolean usaOrigen() {
        return usaOrigen;
    }

    /** El dinero entra a una cuenta. */
    public boolean usaDestino() {
        return usaDestino;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
