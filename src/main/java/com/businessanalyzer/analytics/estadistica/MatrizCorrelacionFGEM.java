package com.businessanalyzer.analytics.estadistica;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Correlación de Pearson entre series mensuales (Apache Commons Math).
 * <ul>
 *     <li>r cerca de +1: suben juntas. Cerca de −1: cuando una sube, la otra baja. Cerca de 0: sin relación lineal.</li>
 *     <li>El valor p indica si la relación podría deberse al azar (se considera real con p &lt; 0.05).</li>
 *     <li>Correlación no implica causalidad.</li>
 * </ul>
 * Una serie constante no tiene correlación definida ({@code NaN}).
 */
public record MatrizCorrelacionFGEM(List<String> variablesFGEM, double[][] coeficientesFGEM, double[][] valoresPFGEM, int observacionesFGEM) {

    public static final int MINIMO_OBSERVACIONES_FGEM = 3;

    /** Par de variables con su coeficiente. */
    public record ParFGEM(String variable1FGEM, String variable2FGEM, double rFGEM, double pFGEM) {
    }

    /** @param seriesFGEM nombre de la variable → valores (todas del mismo largo) */
    public static MatrizCorrelacionFGEM deFGEM(Map<String, double[]> seriesFGEM) {
        List<String> nombresFGEM = new ArrayList<>(seriesFGEM.keySet());
        int kFGEM = nombresFGEM.size();
        int nFGEM = kFGEM == 0 ? 0 : seriesFGEM.get(nombresFGEM.get(0)).length;
        double[][] rFGEM = matrizNaNFGEM(kFGEM);
        double[][] pFGEM = matrizNaNFGEM(kFGEM);
        if (nFGEM >= MINIMO_OBSERVACIONES_FGEM && kFGEM >= 2) {
            double[][] datosFGEM = new double[nFGEM][kFGEM];
            for (int jFGEM = 0; jFGEM < kFGEM; jFGEM++) {
                double[] columnaFGEM = seriesFGEM.get(nombresFGEM.get(jFGEM));
                for (int iFGEM = 0; iFGEM < nFGEM; iFGEM++) {
                    datosFGEM[iFGEM][jFGEM] = columnaFGEM[iFGEM];
                }
            }
            PearsonsCorrelation pearsonFGEM = new PearsonsCorrelation(datosFGEM);
            rFGEM = pearsonFGEM.getCorrelationMatrix().getData();
            try {
                pFGEM = pearsonFGEM.getCorrelationPValues().getData();
            } catch (RuntimeException sinValoresPFGEM) {
                // Ocurre con series constantes; los coeficientes ya son NaN en ese caso.
            }
        }
        return new MatrizCorrelacionFGEM(nombresFGEM, rFGEM, pFGEM, nFGEM);
    }

    public double rFGEM(int iFGEM, int jFGEM) {
        return coeficientesFGEM[iFGEM][jFGEM];
    }

    /** Pares distintos ordenados por la fuerza de la relación (|r| de mayor a menor). */
    public List<ParFGEM> paresOrdenadosFGEM() {
        List<ParFGEM> paresFGEM = new ArrayList<>();
        for (int iFGEM = 0; iFGEM < variablesFGEM.size(); iFGEM++) {
            for (int jFGEM = iFGEM + 1; jFGEM < variablesFGEM.size(); jFGEM++) {
                if (!Double.isNaN(coeficientesFGEM[iFGEM][jFGEM])) {
                    paresFGEM.add(new ParFGEM(variablesFGEM.get(iFGEM), variablesFGEM.get(jFGEM), coeficientesFGEM[iFGEM][jFGEM], valoresPFGEM[iFGEM][jFGEM]));
                }
            }
        }
        paresFGEM.sort(Comparator.comparingDouble((ParFGEM parFGEM) -> Math.abs(parFGEM.rFGEM())).reversed());
        return paresFGEM;
    }

    /** Interpretación de la fuerza: fuerte (≥ 0.7), moderada (≥ 0.4), débil (≥ 0.2) o nula. */
    public static String fuerzaFGEM(double rFGEM) {
        double absFGEM = Math.abs(rFGEM);
        if (Double.isNaN(rFGEM)) {
            return "sin datos";
        }
        if (absFGEM >= 0.7) {
            return "fuerte";
        }
        if (absFGEM >= 0.4) {
            return "moderada";
        }
        return absFGEM >= 0.2 ? "débil" : "nula";
    }

    private static double[][] matrizNaNFGEM(int kFGEM) {
        double[][] mFGEM = new double[kFGEM][kFGEM];
        for (double[] filaFGEM : mFGEM) {
            java.util.Arrays.fill(filaFGEM, Double.NaN);
        }
        return mFGEM;
    }
}
