package com.businessanalyzer.model;

/** Aspectos que el inspector verifica en campo, agrupados por área. */
public enum AspectoInspeccion {

    SANIDAD_ANIMAL(Area.GANADERIA, "Sanidad del ganado", "Animales sin signos de enfermedad, sin cojeras ni diarreas."),
    VACUNACION(Area.GANADERIA, "Vacunas y dosificaciones al día", "Calendario sanitario cumplido y registrado."),
    ALIMENTACION(Area.GANADERIA, "Alimentación y pastos", "Pastos suficientes, forraje conservado, agua para los animales."),
    INSTALACIONES(Area.GANADERIA, "Corrales, cobertizos y bebederos", "Instalaciones limpias y en buen estado."),
    ESTADO_CULTIVOS(Area.AGRICULTURA, "Estado de los cultivos", "Desarrollo acorde a la fecha de siembra."),
    PLAGAS(Area.AGRICULTURA, "Control de plagas y enfermedades", "Sin daños importantes; controles oportunos."),
    SUELO(Area.AGRICULTURA, "Manejo del suelo y abonamiento", "Abonamiento, rotación y conservación del suelo."),
    RIEGO(Area.AGRICULTURA, "Riego y uso del agua", "Riego oportuno y sin desperdicio."),
    REGISTROS(Area.GENERAL, "Registros del productor al día", "Movimientos del hato, costos y cosechas registrados."),
    USO_CREDITO(Area.GENERAL, "Uso del crédito según lo previsto", "Los fondos se usaron en lo que se solicitó."),
    COMPROMISOS(Area.GENERAL, "Cumplimiento de compromisos anteriores", "Recomendaciones de la visita anterior atendidas."),
    AMBIENTE(Area.GENERAL, "Manejo ambiental", "Envases de agroquímicos, residuos y estiércol bien manejados.");

    /** Área de la verificación. */
    public enum Area {
        GANADERIA("Ganadería"),
        AGRICULTURA("Agricultura"),
        GENERAL("Aspectos generales");

        private final String descripcion;

        Area(String descripcion) {
            this.descripcion = descripcion;
        }

        public String descripcion() {
            return descripcion;
        }
    }

    private final Area area;
    private final String descripcion;
    private final String guia;

    AspectoInspeccion(Area area, String descripcion, String guia) {
        this.area = area;
        this.descripcion = descripcion;
        this.guia = guia;
    }

    public Area area() {
        return area;
    }

    public String descripcion() {
        return descripcion;
    }

    /** Qué mirar para evaluarlo. */
    public String guia() {
        return guia;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
