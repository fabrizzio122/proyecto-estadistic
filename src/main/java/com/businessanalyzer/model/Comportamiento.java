package com.businessanalyzer.model;

/**
 * Cómo se comporta un costo frente al volumen de ventas. Es la base del punto de equilibrio.
 * <ul>
 *     <li><b>Fijo</b>: se paga igual aunque se venda poco o mucho (alquiler, sueldos fijos).</li>
 *     <li><b>Variable</b>: sube o baja con las ventas (comisiones, fletes, empaques).</li>
 * </ul>
 */
public enum Comportamiento {

    FIJO("Fijo", "No cambia con el volumen de ventas (alquiler, sueldos, seguros)."),
    VARIABLE("Variable", "Sube o baja con las ventas (comisiones, fletes, empaques).");

    private final String descripcion;
    private final String explicacion;

    Comportamiento(String descripcion, String explicacion) {
        this.descripcion = descripcion;
        this.explicacion = explicacion;
    }

    public String descripcion() {
        return descripcion;
    }

    public String explicacion() {
        return explicacion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
