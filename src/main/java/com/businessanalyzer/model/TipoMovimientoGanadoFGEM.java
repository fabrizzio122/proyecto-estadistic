package com.businessanalyzer.model;

public enum TipoMovimientoGanadoFGEM {

    INVENTARIO_INICIAL("Inventario inicial", 1, "Animales que ya tenía al empezar a usar el sistema."),
    NACIMIENTO("Nacimiento", 1, "Crías nacidas en el hato."),
    COMPRA("Compra", 1, "Animales comprados."),
    VENTA("Venta", -1, "Animales vendidos."),
    MUERTE("Muerte", -1, "Animales muertos por enfermedad, clima, depredadores u otra causa."),
    PERDIDA("Pérdida o robo", -1, "Animales perdidos o robados (abigeato)."),
    CONSUMO("Consumo familiar", -1, "Animales beneficiados para el consumo de la familia."),
    CAMBIO_CATEGORIA("Cambio de categoría", 0, "Animales que pasan a otra categoría: de ternera a vaquillona, de tui a madre...");

    private final String descripcionFGEM;
    private final int signoFGEM;
    private final String ayudaFGEM;

    TipoMovimientoGanadoFGEM(String descripcionFGEM, int signoFGEM, String ayudaFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.signoFGEM = signoFGEM;
        this.ayudaFGEM = ayudaFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public int signoFGEM() {
        return signoFGEM;
    }

    public String ayudaFGEM() {
        return ayudaFGEM;
    }

    public boolean salidaFGEM() {
        return signoFGEM < 0;
    }

    public boolean llevaMontoFGEM() {
        return this == VENTA || this == COMPRA;
    }

    public boolean llevaCausaFGEM() {
        return this == MUERTE || this == PERDIDA;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
