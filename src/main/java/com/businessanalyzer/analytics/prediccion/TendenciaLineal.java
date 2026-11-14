package com.businessanalyzer.analytics.prediccion;

import org.apache.commons.math3.stat.regression.SimpleRegression;

/** Prolonga la recta de regresión (mínimos cuadrados) de toda la historia. */
public final class TendenciaLineal implements MetodoPronostico {

    @Override
    public String nombre() {
        return "Tendencia lineal";
    }

    @Override
    public String explicacion() {
        return "Prolonga la línea recta que mejor se ajusta a toda la historia. Funciona bien cuando "
                + "las ventas crecen o bajan a un ritmo parejo.";
    }

    @Override
    public double[] pronosticar(double[] historia, int horizonte) {
        SimpleRegression regresion = new SimpleRegression();
        for (int t = 0; t < historia.length; t++) {
            regresion.addData(t, historia[t]);
        }
        double[] pronostico = new double[horizonte];
        for (int h = 1; h <= horizonte; h++) {
            pronostico[h - 1] = regresion.getIntercept() + regresion.getSlope() * (historia.length - 1 + h);
        }
        return pronostico;
    }
}
