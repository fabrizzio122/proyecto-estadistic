package com.businessanalyzer.analytics.tesoreria;

public enum RubroFlujoFGEM {

    COBROS_VENTAS(ActividadFGEM.OPERACION, "Cobros por ventas"),
    OTROS_INGRESOS(ActividadFGEM.OPERACION, "Otros ingresos cobrados"),
    PAGOS_COMPRAS(ActividadFGEM.OPERACION, "Pagos a proveedores por compras"),
    GASTOS(ActividadFGEM.OPERACION, "Gastos pagados"),
    INTERESES(ActividadFGEM.OPERACION, "Intereses pagados"),
    COMPRA_ACTIVOS(ActividadFGEM.INVERSION, "Compra de activos"),
    PRESTAMOS(ActividadFGEM.FINANCIAMIENTO, "Préstamos recibidos"),
    PAGO_DEUDAS(ActividadFGEM.FINANCIAMIENTO, "Pago de deudas (capital)"),
    APORTES(ActividadFGEM.FINANCIAMIENTO, "Aportes del dueño"),
    RETIROS(ActividadFGEM.FINANCIAMIENTO, "Retiros del dueño"),
    TRANSFERENCIAS(ActividadFGEM.INTERNA, "Transferencias entre cuentas");

    private final ActividadFGEM actividadFGEM;
    private final String descripcionFGEM;

    RubroFlujoFGEM(ActividadFGEM actividadFGEM, String descripcionFGEM) {
        this.actividadFGEM = actividadFGEM;
        this.descripcionFGEM = descripcionFGEM;
    }

    public ActividadFGEM actividadFGEM() {
        return actividadFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
