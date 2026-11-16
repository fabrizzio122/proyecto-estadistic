package com.businessanalyzer.analytics.tesoreria;

/** Rubro del estado de flujo de efectivo (método directo), en el orden en que se presenta. */
public enum RubroFlujo {

    COBROS_VENTAS(Actividad.OPERACION, "Cobros por ventas"),
    OTROS_INGRESOS(Actividad.OPERACION, "Otros ingresos cobrados"),
    PAGOS_COMPRAS(Actividad.OPERACION, "Pagos a proveedores por compras"),
    GASTOS(Actividad.OPERACION, "Gastos pagados"),
    INTERESES(Actividad.OPERACION, "Intereses pagados"),
    COMPRA_ACTIVOS(Actividad.INVERSION, "Compra de activos"),
    PRESTAMOS(Actividad.FINANCIAMIENTO, "Préstamos recibidos"),
    PAGO_DEUDAS(Actividad.FINANCIAMIENTO, "Pago de deudas (capital)"),
    APORTES(Actividad.FINANCIAMIENTO, "Aportes del dueño"),
    RETIROS(Actividad.FINANCIAMIENTO, "Retiros del dueño"),
    TRANSFERENCIAS(Actividad.INTERNA, "Transferencias entre cuentas");

    private final Actividad actividad;
    private final String descripcion;

    RubroFlujo(Actividad actividad, String descripcion) {
        this.actividad = actividad;
        this.descripcion = descripcion;
    }

    public Actividad actividad() {
        return actividad;
    }

    public String descripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
