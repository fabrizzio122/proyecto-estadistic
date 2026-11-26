package com.businessanalyzer.model;

public enum RubroCostoFGEM {

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

    private final String descripcionFGEM;
    private final String ayudaFGEM;

    RubroCostoFGEM(String descripcionFGEM, String ayudaFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.ayudaFGEM = ayudaFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public String ayudaFGEM() {
        return ayudaFGEM;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
