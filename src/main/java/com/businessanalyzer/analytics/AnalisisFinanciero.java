package com.businessanalyzer.analytics;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Resultado completo del análisis financiero de un período.
 *
 * @param evolucion estado de resultados de cada uno de los últimos meses (para el gráfico)
 */
public record AnalisisFinanciero(
        Periodo periodo,
        Periodo periodoAnterior,
        EstadoResultados actual,
        EstadoResultados anterior,
        PuntoEquilibrio equilibrio,
        List<RentabilidadProducto> productos,
        List<ResultadoMensual> evolucion) {

    public record ResultadoMensual(YearMonth mes, EstadoResultados estado) {
    }

    public AnalisisFinanciero {
        productos = List.copyOf(productos);
        evolucion = List.copyOf(evolucion);
    }

    /** Margen bruto total de los productos vendidos (para calcular participaciones). */
    public BigDecimal margenTotalProductos() {
        return productos.stream().map(RentabilidadProducto::margen).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
