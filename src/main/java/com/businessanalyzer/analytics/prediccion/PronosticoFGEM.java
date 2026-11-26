package com.businessanalyzer.analytics.prediccion;

import java.util.List;

public record PronosticoFGEM(String serieFGEM, int datosFGEM, EvaluacionFGEM elegidaFGEM, List<EvaluacionFGEM> evaluacionesFGEM,
                         List<PuntoFGEM> puntosFGEM, ConfiabilidadFGEM confiabilidadFGEM, double errorRelativoFGEM) {

    public record PuntoFGEM(int hFGEM, double valorFGEM, double inferiorFGEM, double superiorFGEM) {
    }

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
