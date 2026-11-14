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
public record MatrizCorrelacion(List<String> variables, double[][] coeficientes, double[][] valoresP, int observaciones) {

    public static final int MINIMO_OBSERVACIONES = 3;

    /** Par de variables con su coeficiente. */
    public record Par(String variable1, String variable2, double r, double p) {
    }

    /** @param series nombre de la variable → valores (todas del mismo largo) */
    public static MatrizCorrelacion de(Map<String, double[]> series) {
        List<String> nombres = new ArrayList<>(series.keySet());
        int k = nombres.size();
        int n = k == 0 ? 0 : series.get(nombres.get(0)).length;
        double[][] r = matrizNaN(k);
        double[][] p = matrizNaN(k);
        if (n >= MINIMO_OBSERVACIONES && k >= 2) {
            double[][] datos = new double[n][k];
            for (int j = 0; j < k; j++) {
                double[] columna = series.get(nombres.get(j));
                for (int i = 0; i < n; i++) {
                    datos[i][j] = columna[i];
                }
            }
            PearsonsCorrelation pearson = new PearsonsCorrelation(datos);
            r = pearson.getCorrelationMatrix().getData();
            try {
                p = pearson.getCorrelationPValues().getData();
            } catch (RuntimeException sinValoresP) {
                // Ocurre con series constantes; los coeficientes ya son NaN en ese caso.
            }
        }
        return new MatrizCorrelacion(nombres, r, p, n);
    }

    public double r(int i, int j) {
        return coeficientes[i][j];
    }

    /** Pares distintos ordenados por la fuerza de la relación (|r| de mayor a menor). */
    public List<Par> paresOrdenados() {
        List<Par> pares = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            for (int j = i + 1; j < variables.size(); j++) {
                if (!Double.isNaN(coeficientes[i][j])) {
                    pares.add(new Par(variables.get(i), variables.get(j), coeficientes[i][j], valoresP[i][j]));
                }
            }
        }
        pares.sort(Comparator.comparingDouble((Par par) -> Math.abs(par.r())).reversed());
        return pares;
    }

    /** Interpretación de la fuerza: fuerte (≥ 0.7), moderada (≥ 0.4), débil (≥ 0.2) o nula. */
    public static String fuerza(double r) {
        double abs = Math.abs(r);
        if (Double.isNaN(r)) {
            return "sin datos";
        }
        if (abs >= 0.7) {
            return "fuerte";
        }
        if (abs >= 0.4) {
            return "moderada";
        }
        return abs >= 0.2 ? "débil" : "nula";
    }

    private static double[][] matrizNaN(int k) {
        double[][] m = new double[k][k];
        for (double[] fila : m) {
            java.util.Arrays.fill(fila, Double.NaN);
        }
        return m;
    }
}
