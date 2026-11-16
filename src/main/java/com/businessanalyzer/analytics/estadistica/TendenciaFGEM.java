package com.businessanalyzer.analytics.estadistica;

import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * Tendencia lineal de una serie mensual por mínimos cuadrados: y = intercepto + pendiente · t
 * (t = 0, 1, 2... para cada mes).
 * <ul>
 *     <li><b>Pendiente</b>: cuánto cambia el valor, en promedio, de un mes al siguiente.</li>
 *     <li><b>R²</b>: qué parte de la variación explica la recta (0 = nada, 1 = todo).</li>
 *     <li><b>Significancia (valor p)</b>: probabilidad de ver esa pendiente por azar si en realidad
 *         no hubiera tendencia. Con p &lt; 0.05 la tendencia se considera real.</li>
 * </ul>
 */
public record TendenciaFGEM(int nFGEM, double pendienteFGEM, double interceptoFGEM, double r2FGEM, double significanciaFGEM, double mediaFGEM) {

    public static final int MINIMO_PUNTOS_FGEM = 3;
    public static final double NIVEL_SIGNIFICANCIA_FGEM = 0.05;

    public enum DireccionFGEM {
        CRECIENTE("Creciente"),
        DECRECIENTE("Decreciente"),
        ESTABLE("Sin tendencia clara"),
        INSUFICIENTE("Datos insuficientes");

        private final String descripcionFGEM;

        DireccionFGEM(String descripcionFGEM) {
            this.descripcionFGEM = descripcionFGEM;
        }

        public String descripcionFGEM() {
            return descripcionFGEM;
        }
    }

    public static TendenciaFGEM deFGEM(double[] valoresFGEM) {
        double mediaFGEM = 0;
        for (double vFGEM : valoresFGEM) {
            mediaFGEM += vFGEM;
        }
        mediaFGEM = valoresFGEM.length == 0 ? Double.NaN : mediaFGEM / valoresFGEM.length;
        if (valoresFGEM.length < MINIMO_PUNTOS_FGEM) {
            return new TendenciaFGEM(valoresFGEM.length, Double.NaN, Double.NaN, Double.NaN, Double.NaN, mediaFGEM);
        }
        SimpleRegression regresionFGEM = new SimpleRegression();
        for (int tFGEM = 0; tFGEM < valoresFGEM.length; tFGEM++) {
            regresionFGEM.addData(tFGEM, valoresFGEM[tFGEM]);
        }
        return new TendenciaFGEM(valoresFGEM.length, regresionFGEM.getSlope(), regresionFGEM.getIntercept(),
                regresionFGEM.getRSquare(), regresionFGEM.getSignificance(), mediaFGEM);
    }

    public DireccionFGEM direccionFGEM() {
        if (nFGEM < MINIMO_PUNTOS_FGEM) {
            return DireccionFGEM.INSUFICIENTE;
        }
        if (pendienteFGEM == 0 || Double.isNaN(significanciaFGEM) || significanciaFGEM >= NIVEL_SIGNIFICANCIA_FGEM) {
            return DireccionFGEM.ESTABLE;
        }
        return pendienteFGEM > 0 ? DireccionFGEM.CRECIENTE : DireccionFGEM.DECRECIENTE;
    }

    /** Pendiente ÷ media: crecimiento mensual relativo (0.05 = +5 % al mes). */
    public double crecimientoMensualFGEM() {
        return Double.isNaN(pendienteFGEM) || mediaFGEM == 0 ? Double.NaN : pendienteFGEM / mediaFGEM;
    }

    /** Valor de la recta en el mes {@code t}. */
    public double valorAjustadoFGEM(int tFGEM) {
        return interceptoFGEM + pendienteFGEM * tFGEM;
    }
}
