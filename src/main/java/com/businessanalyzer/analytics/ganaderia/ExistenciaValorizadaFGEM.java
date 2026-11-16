package com.businessanalyzer.analytics.ganaderia;

import com.businessanalyzer.model.CategoriaGanadoFGEM;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cabezas de una categoría con su precio de mercado por cabeza.
 *
 * @param precioFGEM precio por cabeza; {@code null} si aún no se indicó
 * @param valorFGEM  cabezas × precio; {@code null} sin precio
 */
public record ExistenciaValorizadaFGEM(CategoriaGanadoFGEM categoriaFGEM, int cabezasFGEM, BigDecimal precioFGEM, BigDecimal valorFGEM) {

    /** Una fila por categoría con animales, en el orden de las especies y categorías. */
    public static List<ExistenciaValorizadaFGEM> deFGEM(Map<CategoriaGanadoFGEM, Integer> existenciasFGEM,
                                                Map<CategoriaGanadoFGEM, BigDecimal> preciosFGEM) {
        List<ExistenciaValorizadaFGEM> filasFGEM = new ArrayList<>();
        for (CategoriaGanadoFGEM cFGEM : CategoriaGanadoFGEM.values()) {
            int cabezasFGEM = existenciasFGEM.getOrDefault(cFGEM, 0);
            if (cabezasFGEM <= 0) {
                continue;
            }
            BigDecimal precioFGEM = preciosFGEM.get(cFGEM);
            filasFGEM.add(new ExistenciaValorizadaFGEM(cFGEM, cabezasFGEM, precioFGEM,
                    precioFGEM == null ? null : precioFGEM.multiply(BigDecimal.valueOf(cabezasFGEM))));
        }
        return filasFGEM;
    }

    /** Valor total de las categorías con precio. */
    public static BigDecimal totalFGEM(List<ExistenciaValorizadaFGEM> filasFGEM) {
        return filasFGEM.stream().map(ExistenciaValorizadaFGEM::valorFGEM).filter(vFGEM -> vFGEM != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
