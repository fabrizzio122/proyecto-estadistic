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
public record EstadisticaDescriptiva(int n, double media, double mediana, double desviacion, double minimo,
                                     double maximo, double q1, double q3, double asimetria) {

    public static final EstadisticaDescriptiva VACIA = new EstadisticaDescriptiva(0, Double.NaN, Double.NaN,
            Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

    public static EstadisticaDescriptiva de(double[] valores) {
        if (valores.length == 0) {
            return VACIA;
        }
        DescriptiveStatistics s = new DescriptiveStatistics(valores);
        return new EstadisticaDescriptiva(
                valores.length,
                s.getMean(),
                s.getPercentile(50),
                valores.length > 1 ? s.getStandardDeviation() : Double.NaN,
                s.getMin(),
                s.getMax(),
                s.getPercentile(25),
                s.getPercentile(75),
                valores.length > 2 ? s.getSkewness() : Double.NaN);
    }

    public boolean vacia() {
        return n == 0;
    }

    /** Desviación ÷ media; {@code NaN} si no se puede calcular. */
    public double coeficienteVariacion() {
        return Double.isNaN(desviacion) || media == 0 ? Double.NaN : desviacion / media;
    }

    /** Q3 − Q1: rango donde está el 50 % central de los valores. */
    public double rangoIntercuartil() {
        return q3 - q1;
    }
}
