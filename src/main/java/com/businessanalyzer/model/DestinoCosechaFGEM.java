package com.businessanalyzer.model;

/** A dónde va lo cosechado. */
public enum DestinoCosechaFGEM {

    VENTA("Venta"),
    AUTOCONSUMO("Consumo familiar"),
    SEMILLA("Semilla para la próxima campaña"),
    ANIMALES("Alimento para los animales"),
    TRUEQUE("Trueque o pago en especie"),
    MERMA("Pérdida poscosecha");

    private final String descripcionFGEM;

    DestinoCosechaFGEM(String descripcionFGEM) {
        this.descripcionFGEM = descripcionFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
