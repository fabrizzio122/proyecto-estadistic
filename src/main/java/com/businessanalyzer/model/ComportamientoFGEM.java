package com.businessanalyzer.model;

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
