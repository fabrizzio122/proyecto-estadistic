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
public record Tendencia(int n, double pendiente, double intercepto, double r2, double significancia, double media) {

    public static final int MINIMO_PUNTOS = 3;
    public static final double NIVEL_SIGNIFICANCIA = 0.05;

    public enum Direccion {
        CRECIENTE("Creciente"),
        DECRECIENTE("Decreciente"),
        ESTABLE("Sin tendencia clara"),
        INSUFICIENTE("Datos insuficientes");

        private final String descripcion;

        Direccion(String descripcion) {
            this.descripcion = descripcion;
        }

        public String descripcion() {
            return descripcion;
        }
    }

    public static Tendencia de(double[] valores) {
        double media = 0;
        for (double v : valores) {
            media += v;
        }
        media = valores.length == 0 ? Double.NaN : media / valores.length;
        if (valores.length < MINIMO_PUNTOS) {
            return new Tendencia(valores.length, Double.NaN, Double.NaN, Double.NaN, Double.NaN, media);
        }
        SimpleRegression regresion = new SimpleRegression();
        for (int t = 0; t < valores.length; t++) {
            regresion.addData(t, valores[t]);
        }
        return new Tendencia(valores.length, regresion.getSlope(), regresion.getIntercept(),
                regresion.getRSquare(), regresion.getSignificance(), media);
    }

    public Direccion direccion() {
        if (n < MINIMO_PUNTOS) {
            return Direccion.INSUFICIENTE;
        }
        if (pendiente == 0 || Double.isNaN(significancia) || significancia >= NIVEL_SIGNIFICANCIA) {
            return Direccion.ESTABLE;
        }
        return pendiente > 0 ? Direccion.CRECIENTE : Direccion.DECRECIENTE;
    }

    /** Pendiente ÷ media: crecimiento mensual relativo (0.05 = +5 % al mes). */
    public double crecimientoMensual() {
        return Double.isNaN(pendiente) || media == 0 ? Double.NaN : pendiente / media;
    }

    /** Valor de la recta en el mes {@code t}. */
    public double valorAjustado(int t) {
        return intercepto + pendiente * t;
    }
}
