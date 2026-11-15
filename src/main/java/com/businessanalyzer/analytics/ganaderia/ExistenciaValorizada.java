package com.businessanalyzer.analytics.ganaderia;

import com.businessanalyzer.model.CategoriaGanado;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cabezas de una categoría con su precio de mercado por cabeza.
 *
 * @param precio precio por cabeza; {@code null} si aún no se indicó
 * @param valor  cabezas × precio; {@code null} sin precio
 */
public record ExistenciaValorizada(CategoriaGanado categoria, int cabezas, BigDecimal precio, BigDecimal valor) {

    /** Una fila por categoría con animales, en el orden de las especies y categorías. */
    public static List<ExistenciaValorizada> de(Map<CategoriaGanado, Integer> existencias,
                                                Map<CategoriaGanado, BigDecimal> precios) {
        List<ExistenciaValorizada> filas = new ArrayList<>();
        for (CategoriaGanado c : CategoriaGanado.values()) {
            int cabezas = existencias.getOrDefault(c, 0);
            if (cabezas <= 0) {
                continue;
            }
            BigDecimal precio = precios.get(c);
            filas.add(new ExistenciaValorizada(c, cabezas, precio,
                    precio == null ? null : precio.multiply(BigDecimal.valueOf(cabezas))));
        }
        return filas;
    }

    /** Valor total de las categorías con precio. */
    public static BigDecimal total(List<ExistenciaValorizada> filas) {
        return filas.stream().map(ExistenciaValorizada::valor).filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
