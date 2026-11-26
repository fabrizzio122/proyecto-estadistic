package com.businessanalyzer.analytics.simulacion;

public enum VariableFGEM {

    PRECIO("Precio de venta", "Cuánto suben o bajan los precios (con las mismas unidades vendidas)."),
    VOLUMEN("Unidades vendidas", "Cuánto más o menos se vende (los costos variables se mueven igual)."),
    COSTO_UNITARIO("Costo de los productos", "Cuánto cambia lo que cuesta comprar o producir cada producto."),
    GASTOS_VARIABLES("Gastos variables", "Fletes, comisiones, empaques... además del efecto del volumen."),
    COSTOS_FIJOS("Costos fijos", "Alquiler, sueldos, servicios... que no dependen de lo vendido.");

    private final String descripcionFGEM;
    private final String ayudaFGEM;

    VariableFGEM(String descripcionFGEM, String ayudaFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.ayudaFGEM = ayudaFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public String ayudaFGEM() {
        return ayudaFGEM;
    }
}
