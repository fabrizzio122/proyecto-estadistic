package com.businessanalyzer.analytics.estadistica;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

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

    public double coeficienteVariacionFGEM() {
        return Double.isNaN(desviacionFGEM) || mediaFGEM == 0 ? Double.NaN : desviacionFGEM / mediaFGEM;
    }

    public double rangoIntercuartilFGEM() {
        return q3FGEM - q1FGEM;
    }
}
