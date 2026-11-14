package com.businessanalyzer.analytics.estadistica;

import java.util.ArrayList;
import java.util.List;

/**
 * Distribución de frecuencias en intervalos de igual ancho. El número de intervalos sigue
 * la regla de Sturges (1 + log2 n), con un máximo indicado.
 */
public record Histograma(List<Clase> clases) {

    /** Intervalo [desde, hasta) — el último incluye el máximo. */
    public record Clase(double desde, double hasta, int frecuencia) {
    }

    public Histograma {
        clases = List.copyOf(clases);
    }

    public static Histograma de(double[] valores, int maximoClases) {
        if (valores.length == 0) {
            return new Histograma(List.of());
        }
        double minimo = Double.POSITIVE_INFINITY;
        double maximo = Double.NEGATIVE_INFINITY;
        for (double v : valores) {
            minimo = Math.min(minimo, v);
            maximo = Math.max(maximo, v);
        }
        if (minimo == maximo) {
            return new Histograma(List.of(new Clase(minimo, maximo, valores.length)));
        }
        int sturges = (int) Math.ceil(1 + Math.log(valores.length) / Math.log(2));
        int k = Math.max(1, Math.min(maximoClases, sturges));
        double ancho = (maximo - minimo) / k;
        int[] frecuencias = new int[k];
        for (double v : valores) {
            int indice = (int) ((v - minimo) / ancho);
            frecuencias[Math.min(indice, k - 1)]++;
        }
        List<Clase> clases = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            clases.add(new Clase(minimo + i * ancho, minimo + (i + 1) * ancho, frecuencias[i]));
        }
        return new Histograma(clases);
    }
}
