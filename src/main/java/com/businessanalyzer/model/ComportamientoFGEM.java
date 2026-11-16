package com.businessanalyzer.model;

/**
 * Cómo se comporta un costo frente al volumen de ventas. Es la base del punto de equilibrio.
 * <ul>
 *     <li><b>Fijo</b>: se paga igual aunque se venda poco o mucho (alquiler, sueldos fijos).</li>
 *     <li><b>Variable</b>: sube o baja con las ventas (comisiones, fletes, empaques).</li>
 * </ul>
 */
public enum ComportamientoFGEM {

    FIJO("Fijo", "No cambia con el volumen de ventas (alquiler, sueldos, seguros)."),
    VARIABLE("Variable", "Sube o baja con las ventas (comisiones, fletes, empaques).");

    private final String descripcionFGEM;
    private final String explicacionFGEM;

    ComportamientoFGEM(String descripcionFGEM, String explicacionFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.explicacionFGEM = explicacionFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public String explicacionFGEM() {
        return explicacionFGEM;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
