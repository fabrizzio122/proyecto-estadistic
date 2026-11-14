package com.businessanalyzer.analytics.prediccion;

import java.util.List;

/**
 * Pronóstico de una serie con el método elegido y la comparación de todos los métodos.
 *
 * @param elegida       evaluación del método elegido; {@code null} si no hay datos suficientes
 * @param errorRelativo error medio del método elegido ÷ valor medio de la serie
 */
public record Pronostico(String serie, int datos, Evaluacion elegida, List<Evaluacion> evaluaciones,
                         List<Punto> puntos, Confiabilidad confiabilidad, double errorRelativo) {

    /**
     * Valor proyectado para el período {@code h} (1 = el siguiente) con su intervalo aproximado al 95 %.
     */
    public record Punto(int h, double valor, double inferior, double superior) {
    }

    /**
     * Error histórico de un método al pronosticar cada mes con los datos anteriores a él.
     *
     * @param mae  error absoluto medio (en dinero)
     * @param rmse raíz del error cuadrático medio (penaliza más los errores grandes)
     */
    public record Evaluacion(MetodoPronostico metodo, double mae, double rmse, int mesesEvaluados) {
    }

    public enum Confiabilidad {
        ALTA("Alta"), MEDIA("Media"), BAJA("Baja");

        private final String descripcion;

        Confiabilidad(String descripcion) {
            this.descripcion = descripcion;
        }

        public String descripcion() {
            return descripcion;
        }
    }

    public Pronostico {
        evaluaciones = List.copyOf(evaluaciones);
        puntos = List.copyOf(puntos);
    }

    public static Pronostico insuficiente(String serie, int datos) {
        return new Pronostico(serie, datos, null, List.of(), List.of(), Confiabilidad.BAJA, Double.NaN);
    }

    public boolean suficiente() {
        return elegida != null;
    }

    public double total() {
        return puntos.stream().mapToDouble(Punto::valor).sum();
    }

    public double totalInferior() {
        return puntos.stream().mapToDouble(Punto::inferior).sum();
    }

    public double totalSuperior() {
        return puntos.stream().mapToDouble(Punto::superior).sum();
    }
}
