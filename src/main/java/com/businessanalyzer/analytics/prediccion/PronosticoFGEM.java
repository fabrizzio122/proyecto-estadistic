package com.businessanalyzer.analytics.prediccion;

import java.util.List;

/**
 * Pronóstico de una serie con el método elegido y la comparación de todos los métodos.
 *
 * @param elegidaFGEM       evaluación del método elegido; {@code null} si no hay datos suficientes
 * @param errorRelativoFGEM error medio del método elegido ÷ valor medio de la serie
 */
public record PronosticoFGEM(String serieFGEM, int datosFGEM, EvaluacionFGEM elegidaFGEM, List<EvaluacionFGEM> evaluacionesFGEM,
                         List<PuntoFGEM> puntosFGEM, ConfiabilidadFGEM confiabilidadFGEM, double errorRelativoFGEM) {

    /**
     * Valor proyectado para el período {@code h} (1 = el siguiente) con su intervalo aproximado al 95 %.
     */
    public record PuntoFGEM(int hFGEM, double valorFGEM, double inferiorFGEM, double superiorFGEM) {
    }

    /**
     * Error histórico de un método al pronosticar cada mes con los datos anteriores a él.
     *
     * @param maeFGEM  error absoluto medio (en dinero)
     * @param rmseFGEM raíz del error cuadrático medio (penaliza más los errores grandes)
     */
    public record EvaluacionFGEM(MetodoPronosticoFGEM metodoFGEM, double maeFGEM, double rmseFGEM, int mesesEvaluadosFGEM) {
    }

    public enum ConfiabilidadFGEM {
        ALTA("Alta"), MEDIA("Media"), BAJA("Baja");

        private final String descripcionFGEM;

        ConfiabilidadFGEM(String descripcionFGEM) {
            this.descripcionFGEM = descripcionFGEM;
        }

        public String descripcionFGEM() {
            return descripcionFGEM;
        }
    }

    public PronosticoFGEM {
        evaluacionesFGEM = List.copyOf(evaluacionesFGEM);
        puntosFGEM = List.copyOf(puntosFGEM);
    }

    public static PronosticoFGEM insuficienteFGEM(String serieFGEM, int datosFGEM) {
        return new PronosticoFGEM(serieFGEM, datosFGEM, null, List.of(), List.of(), ConfiabilidadFGEM.BAJA, Double.NaN);
    }

    public boolean suficienteFGEM() {
        return elegidaFGEM != null;
    }

    public double totalFGEM() {
        return puntosFGEM.stream().mapToDouble(PuntoFGEM::valorFGEM).sum();
    }

    public double totalInferiorFGEM() {
        return puntosFGEM.stream().mapToDouble(PuntoFGEM::inferiorFGEM).sum();
    }

    public double totalSuperiorFGEM() {
        return puntosFGEM.stream().mapToDouble(PuntoFGEM::superiorFGEM).sum();
    }
}
