package com.businessanalyzer.model;

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

    public boolean usaOrigenFGEM() {
        return usaOrigenFGEM;
    }

    public boolean usaDestinoFGEM() {
        return usaDestinoFGEM;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
