package com.businessanalyzer.model;

/** Aspectos que el inspector verifica en campo, agrupados por área. */
public enum AspectoInspeccionFGEM {

    SANIDAD_ANIMAL(AreaFGEM.GANADERIA, "Sanidad del ganado", "Animales sin signos de enfermedad, sin cojeras ni diarreas."),
    VACUNACION(AreaFGEM.GANADERIA, "Vacunas y dosificaciones al día", "Calendario sanitario cumplido y registrado."),
    ALIMENTACION(AreaFGEM.GANADERIA, "Alimentación y pastos", "Pastos suficientes, forraje conservado, agua para los animales."),
    INSTALACIONES(AreaFGEM.GANADERIA, "Corrales, cobertizos y bebederos", "Instalaciones limpias y en buen estado."),
    ESTADO_CULTIVOS(AreaFGEM.AGRICULTURA, "Estado de los cultivos", "Desarrollo acorde a la fecha de siembra."),
    PLAGAS(AreaFGEM.AGRICULTURA, "Control de plagas y enfermedades", "Sin daños importantes; controles oportunos."),
    SUELO(AreaFGEM.AGRICULTURA, "Manejo del suelo y abonamiento", "Abonamiento, rotación y conservación del suelo."),
    RIEGO(AreaFGEM.AGRICULTURA, "Riego y uso del agua", "Riego oportuno y sin desperdicio."),
    REGISTROS(AreaFGEM.GENERAL, "Registros del productor al día", "Movimientos del hato, costos y cosechas registrados."),
    USO_CREDITO(AreaFGEM.GENERAL, "Uso del crédito según lo previsto", "Los fondos se usaron en lo que se solicitó."),
    COMPROMISOS(AreaFGEM.GENERAL, "Cumplimiento de compromisos anteriores", "Recomendaciones de la visita anterior atendidas."),
    AMBIENTE(AreaFGEM.GENERAL, "Manejo ambiental", "Envases de agroquímicos, residuos y estiércol bien manejados.");

    /** Área de la verificación. */
    public enum AreaFGEM {
        GANADERIA("Ganadería"),
        AGRICULTURA("Agricultura"),
        GENERAL("Aspectos generales");

        private final String descripcionFGEM;

        AreaFGEM(String descripcionFGEM) {
            this.descripcionFGEM = descripcionFGEM;
        }

        public String descripcionFGEM() {
            return descripcionFGEM;
        }
    }

    private final AreaFGEM areaFGEM;
    private final String descripcionFGEM;
    private final String guiaFGEM;

    AspectoInspeccionFGEM(AreaFGEM areaFGEM, String descripcionFGEM, String guiaFGEM) {
        this.areaFGEM = areaFGEM;
        this.descripcionFGEM = descripcionFGEM;
        this.guiaFGEM = guiaFGEM;
    }

    public AreaFGEM areaFGEM() {
        return areaFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    /** Qué mirar para evaluarlo. */
    public String guiaFGEM() {
        return guiaFGEM;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
