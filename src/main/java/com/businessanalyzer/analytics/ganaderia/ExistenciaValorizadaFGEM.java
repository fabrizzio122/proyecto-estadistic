package com.businessanalyzer.analytics.ganaderia;

import com.businessanalyzer.model.CategoriaGanadoFGEM;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ExistenciaValorizadaFGEM(CategoriaGanadoFGEM categoriaFGEM, int cabezasFGEM, BigDecimal precioFGEM, BigDecimal valorFGEM) {

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

    public static BigDecimal totalFGEM(List<ExistenciaValorizadaFGEM> filasFGEM) {
        return filasFGEM.stream().map(ExistenciaValorizadaFGEM::valorFGEM).filter(vFGEM -> vFGEM != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
