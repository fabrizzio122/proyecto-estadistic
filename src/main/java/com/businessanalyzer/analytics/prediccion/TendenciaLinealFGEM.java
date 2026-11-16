package com.businessanalyzer.analytics.prediccion;

import org.apache.commons.math3.stat.regression.SimpleRegression;

/** Prolonga la recta de regresión (mínimos cuadrados) de toda la historia. */
public final class TendenciaLinealFGEM implements MetodoPronosticoFGEM {

    @Override
    public String nombreFGEM() {
        return "Tendencia lineal";
    }

    @Override
    public String explicacionFGEM() {
        return "Prolonga la línea recta que mejor se ajusta a toda la historia. Funciona bien cuando "
                + "las ventas crecen o bajan a un ritmo parejo.";
    }

    @Override
    public double[] pronosticarFGEM(double[] historiaFGEM, int horizonteFGEM) {
        SimpleRegression regresionFGEM = new SimpleRegression();
        for (int tFGEM = 0; tFGEM < historiaFGEM.length; tFGEM++) {
            regresionFGEM.addData(tFGEM, historiaFGEM[tFGEM]);
        }
        double[] pronosticoFGEM = new double[horizonteFGEM];
        for (int hFGEM = 1; hFGEM <= horizonteFGEM; hFGEM++) {
            pronosticoFGEM[hFGEM - 1] = regresionFGEM.getIntercept() + regresionFGEM.getSlope() * (historiaFGEM.length - 1 + hFGEM);
        }
        return pronosticoFGEM;
    }
}
