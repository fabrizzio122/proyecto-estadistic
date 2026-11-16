package com.businessanalyzer.analytics.estadistica;

import java.util.ArrayList;
import java.util.List;

/**
 * Distribución de frecuencias en intervalos de igual ancho. El número de intervalos sigue
 * la regla de Sturges (1 + log2 n), con un máximo indicado.
 */
public record HistogramaFGEM(List<ClaseFGEM> clasesFGEM) {

    /** Intervalo [desde, hasta) — el último incluye el máximo. */
    public record ClaseFGEM(double desdeFGEM, double hastaFGEM, int frecuenciaFGEM) {
    }

    public HistogramaFGEM {
        clasesFGEM = List.copyOf(clasesFGEM);
    }

    public static HistogramaFGEM deFGEM(double[] valoresFGEM, int maximoClasesFGEM) {
        if (valoresFGEM.length == 0) {
            return new HistogramaFGEM(List.of());
        }
        double minimoFGEM = Double.POSITIVE_INFINITY;
        double maximoFGEM = Double.NEGATIVE_INFINITY;
        for (double vFGEM : valoresFGEM) {
            minimoFGEM = Math.min(minimoFGEM, vFGEM);
            maximoFGEM = Math.max(maximoFGEM, vFGEM);
        }
        if (minimoFGEM == maximoFGEM) {
            return new HistogramaFGEM(List.of(new ClaseFGEM(minimoFGEM, maximoFGEM, valoresFGEM.length)));
        }
        int sturgesFGEM = (int) Math.ceil(1 + Math.log(valoresFGEM.length) / Math.log(2));
        int kFGEM = Math.max(1, Math.min(maximoClasesFGEM, sturgesFGEM));
        double anchoFGEM = (maximoFGEM - minimoFGEM) / kFGEM;
        int[] frecuenciasFGEM = new int[kFGEM];
        for (double vFGEM : valoresFGEM) {
            int indiceFGEM = (int) ((vFGEM - minimoFGEM) / anchoFGEM);
            frecuenciasFGEM[Math.min(indiceFGEM, kFGEM - 1)]++;
        }
        List<ClaseFGEM> clasesFGEM = new ArrayList<>();
        for (int iFGEM = 0; iFGEM < kFGEM; iFGEM++) {
            clasesFGEM.add(new ClaseFGEM(minimoFGEM + iFGEM * anchoFGEM, minimoFGEM + (iFGEM + 1) * anchoFGEM, frecuenciasFGEM[iFGEM]));
        }
        return new HistogramaFGEM(clasesFGEM);
    }
}
