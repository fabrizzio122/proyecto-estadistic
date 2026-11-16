package com.businessanalyzer.analytics.estadistica;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Medidas descriptivas de una serie de valores (Apache Commons Math).
 * <ul>
 *     <li><b>Media</b>: promedio. <b>Mediana</b>: el valor del medio (la mitad está por debajo).</li>
 *     <li><b>Desviación estándar</b> (muestral): cuánto se alejan los valores de la media.</li>
 *     <li><b>Coeficiente de variación</b> = desviación ÷ media: dispersión relativa
 *         (menos de 30 % se considera estable).</li>
 *     <li><b>Cuartiles</b> Q1 y Q3: el 25 % de los valores está por debajo de Q1 y el 25 % por encima de Q3.</li>
 *     <li><b>Asimetría</b>: positiva si hay unos pocos valores muy altos que estiran la cola derecha.</li>
 * </ul>
 * Los valores que no se pueden calcular (por ejemplo, la desviación con un solo dato) son {@code NaN}.
 */
public record EstadisticaDescriptivaFGEM(int nFGEM, double mediaFGEM, double medianaFGEM, double desviacionFGEM, double minimoFGEM,
                                     double maximoFGEM, double q1FGEM, double q3FGEM, double asimetriaFGEM) {

    public static final EstadisticaDescriptivaFGEM VACIA_FGEM = new EstadisticaDescriptivaFGEM(0, Double.NaN, Double.NaN,
            Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

    public static EstadisticaDescriptivaFGEM deFGEM(double[] valoresFGEM) {
        if (valoresFGEM.length == 0) {
            return VACIA_FGEM;
        }
        DescriptiveStatistics sFGEM = new DescriptiveStatistics(valoresFGEM);
        return new EstadisticaDescriptivaFGEM(
                valoresFGEM.length,
                sFGEM.getMean(),
                sFGEM.getPercentile(50),
                valoresFGEM.length > 1 ? sFGEM.getStandardDeviation() : Double.NaN,
                sFGEM.getMin(),
                sFGEM.getMax(),
                sFGEM.getPercentile(25),
                sFGEM.getPercentile(75),
                valoresFGEM.length > 2 ? sFGEM.getSkewness() : Double.NaN);
    }

    public boolean vaciaFGEM() {
        return nFGEM == 0;
    }

    /** Desviación ÷ media; {@code NaN} si no se puede calcular. */
    public double coeficienteVariacionFGEM() {
        return Double.isNaN(desviacionFGEM) || mediaFGEM == 0 ? Double.NaN : desviacionFGEM / mediaFGEM;
    }

    /** Q3 − Q1: rango donde está el 50 % central de los valores. */
    public double rangoIntercuartilFGEM() {
        return q3FGEM - q1FGEM;
    }
}
