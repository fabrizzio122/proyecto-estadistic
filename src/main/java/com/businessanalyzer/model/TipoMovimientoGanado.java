package com.businessanalyzer.model;

/** Hechos que cambian las existencias del hato. */
public enum TipoMovimientoGanado {

    INVENTARIO_INICIAL("Inventario inicial", 1, "Animales que ya tenía al empezar a usar el sistema."),
    NACIMIENTO("Nacimiento", 1, "Crías nacidas en el hato."),
    COMPRA("Compra", 1, "Animales comprados."),
    VENTA("Venta", -1, "Animales vendidos."),
    MUERTE("Muerte", -1, "Animales muertos por enfermedad, clima, depredadores u otra causa."),
    PERDIDA("Pérdida o robo", -1, "Animales perdidos o robados (abigeato)."),
    CONSUMO("Consumo familiar", -1, "Animales beneficiados para el consumo de la familia."),
    CAMBIO_CATEGORIA("Cambio de categoría", 0, "Animales que pasan a otra categoría: de ternera a vaquillona, de tui a madre...");

    private final String descripcion;
    private final int signo;
    private final String ayuda;

    TipoMovimientoGanado(String descripcion, int signo, String ayuda) {
        this.descripcion = descripcion;
        this.signo = signo;
        this.ayuda = ayuda;
    }

    public String descripcion() {
        return descripcion;
    }

    /** +1 si suma animales, −1 si los resta, 0 si los mueve de categoría. */
    public int signo() {
        return signo;
    }

    public String ayuda() {
        return ayuda;
    }

    public boolean salida() {
        return signo < 0;
    }

    /** Ventas y compras pueden indicar su valor total. */
    public boolean llevaMonto() {
        return this == VENTA || this == COMPRA;
    }

    /** Muertes y pérdidas pueden indicar la causa. */
    public boolean llevaCausa() {
        return this == MUERTE || this == PERDIDA;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
