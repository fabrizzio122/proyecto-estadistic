package com.businessanalyzer.analytics.diagnostico;

/**
 * Resultado de evaluar un indicador.
 *
 * @param valorFGEM          valor del indicador ya formateado ("12.4 %", "2 de 6")
 * @param criterioFGEM       umbrales con los que se evaluó ("Bien desde 5 %")
 * @param lecturaFGEM        qué significa el valor para este negocio
 * @param recomendacionFGEM  qué hacer; {@code null} si el indicador está en verde o sin datos
 */
public record HallazgoFGEM(TipoIndicadorFGEM tipoFGEM, SemaforoFGEM semaforoFGEM, String valorFGEM, String criterioFGEM, String lecturaFGEM,
                       String recomendacionFGEM) {

    public static HallazgoFGEM sinDatosFGEM(TipoIndicadorFGEM tipoFGEM, String motivoFGEM) {
        return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.SIN_DATOS, "—", null, motivoFGEM, null);
    }

    /** Prioridad para ordenar las recomendaciones: primero los rojos y, dentro de ellos, los de más peso. */
    public int prioridadFGEM() {
        int gravedadFGEM = switch (semaforoFGEM) {
            case ROJO -> 2;
            case AMARILLO -> 1;
            default -> 0;
        };
        return gravedadFGEM * 10 + tipoFGEM.pesoFGEM();
    }
}
