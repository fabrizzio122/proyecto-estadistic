package com.businessanalyzer.model;

/** Rubros de costo de una campaña agrícola. */
public enum RubroCosto {

    PREPARACION("Preparación del terreno", "Arado, rastra, surcado (yunta o tractor)."),
    SEMILLA("Semilla", "Semilla comprada o propia valorizada."),
    FERTILIZANTE("Fertilizantes", "Urea, fosfato, potasio y otros abonos químicos."),
    ABONO_ORGANICO("Abono orgánico", "Guano, compost, estiércol."),
    FITOSANITARIOS("Control de plagas y enfermedades", "Fungicidas, insecticidas, herbicidas."),
    MANO_OBRA("Mano de obra", "Jornales pagados y, si quieres, la mano de obra familiar valorizada."),
    RIEGO("Riego", "Tarifa de agua, combustible de bomba."),
    COSECHA("Cosecha y poscosecha", "Jornales de cosecha, sacos, selección, almacenamiento."),
    TRANSPORTE("Transporte y flete", "Traslado de insumos y de la cosecha."),
    ALQUILER_TERRENO("Alquiler del terreno", "Pago por la parcela alquilada."),
    OTROS("Otros", "Cualquier otro gasto de la campaña.");

    private final String descripcion;
    private final String ayuda;

    RubroCosto(String descripcion, String ayuda) {
        this.descripcion = descripcion;
        this.ayuda = ayuda;
    }

    public String descripcion() {
        return descripcion;
    }

    public String ayuda() {
        return ayuda;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
