package com.businessanalyzer.model;

/** A dónde va lo cosechado. */
public enum DestinoCosecha {

    VENTA("Venta"),
    AUTOCONSUMO("Consumo familiar"),
    SEMILLA("Semilla para la próxima campaña"),
    ANIMALES("Alimento para los animales"),
    TRUEQUE("Trueque o pago en especie"),
    MERMA("Pérdida poscosecha");

    private final String descripcion;

    DestinoCosecha(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
