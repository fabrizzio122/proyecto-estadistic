package com.businessanalyzer.analytics.simulacion;

/** Variables que se pueden cambiar en un escenario (en porcentaje). */
public enum Variable {

    PRECIO("Precio de venta", "Cuánto suben o bajan los precios (con las mismas unidades vendidas)."),
    VOLUMEN("Unidades vendidas", "Cuánto más o menos se vende (los costos variables se mueven igual)."),
    COSTO_UNITARIO("Costo de los productos", "Cuánto cambia lo que cuesta comprar o producir cada producto."),
    GASTOS_VARIABLES("Gastos variables", "Fletes, comisiones, empaques... además del efecto del volumen."),
    COSTOS_FIJOS("Costos fijos", "Alquiler, sueldos, servicios... que no dependen de lo vendido.");

    private final String descripcion;
    private final String ayuda;

    Variable(String descripcion, String ayuda) {
        this.descripcion = descripcion;
        this.ayuda = ayuda;
    }

    public String descripcion() {
        return descripcion;
    }

    public String ayuda() {
        return ayuda;
    }
}
