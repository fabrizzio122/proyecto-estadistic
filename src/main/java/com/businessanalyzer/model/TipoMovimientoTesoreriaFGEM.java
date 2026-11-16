package com.businessanalyzer.model;

/**
 * Movimientos que mueven dinero sin ser ingresos ni gastos: no cambian la utilidad, solo dónde está
 * el dinero o cuánto puso o sacó el dueño.
 */
public enum TipoMovimientoTesoreriaFGEM {

    TRANSFERENCIA("Transferencia entre cuentas", true, true),
    APORTE("Aporte del dueño", false, true),
    RETIRO("Retiro del dueño", true, false);

    private final String descripcionFGEM;
    private final boolean usaOrigenFGEM;
    private final boolean usaDestinoFGEM;

    TipoMovimientoTesoreriaFGEM(String descripcionFGEM, boolean usaOrigenFGEM, boolean usaDestinoFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.usaOrigenFGEM = usaOrigenFGEM;
        this.usaDestinoFGEM = usaDestinoFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    /** El dinero sale de una cuenta. */
    public boolean usaOrigenFGEM() {
        return usaOrigenFGEM;
    }

    /** El dinero entra a una cuenta. */
    public boolean usaDestinoFGEM() {
        return usaDestinoFGEM;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
