package com.businessanalyzer.analytics;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Resultado completo del análisis financiero de un período.
 *
 * @param evolucionFGEM estado de resultados de cada uno de los últimos meses (para el gráfico)
 */
public record AnalisisFinancieroFGEM(
        PeriodoFGEM periodoFGEM,
        PeriodoFGEM periodoAnteriorFGEM,
        EstadoResultadosFGEM actualFGEM,
        EstadoResultadosFGEM anteriorFGEM,
        PuntoEquilibrioFGEM equilibrioFGEM,
        List<RentabilidadProductoFGEM> productosFGEM,
        List<ResultadoMensualFGEM> evolucionFGEM) {

    public record ResultadoMensualFGEM(YearMonth mesFGEM, EstadoResultadosFGEM estadoFGEM) {
    }

    public AnalisisFinancieroFGEM {
        productosFGEM = List.copyOf(productosFGEM);
        evolucionFGEM = List.copyOf(evolucionFGEM);
    }

    /** Margen bruto total de los productos vendidos (para calcular participaciones). */
    public BigDecimal margenTotalProductosFGEM() {
        return productosFGEM.stream().map(RentabilidadProductoFGEM::margenFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
