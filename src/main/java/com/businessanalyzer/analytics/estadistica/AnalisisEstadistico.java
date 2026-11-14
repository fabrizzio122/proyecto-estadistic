package com.businessanalyzer.analytics.estadistica;

import com.businessanalyzer.analytics.Periodo;

import java.time.YearMonth;
import java.util.List;

/**
 * Resultado del análisis estadístico de un período.
 *
 * @param ticket            estadística del monto de cada venta (sin impuestos)
 * @param ventasDiarias     estadística de las ventas de cada día (incluye días sin ventas)
 * @param serieMensual      meses completos usados para tendencia y correlaciones
 * @param mesActualExcluido el mes en curso no se usó en la tendencia por estar incompleto
 */
public record AnalisisEstadistico(
        Periodo periodo,
        EstadisticaDescriptiva ticket,
        EstadisticaDescriptiva ventasDiarias,
        Histograma histogramaTickets,
        EstacionalidadSemanal estacionalidad,
        List<PuntoMensual> serieMensual,
        Tendencia tendenciaVentas,
        MatrizCorrelacion correlaciones,
        AnalisisAbc abc,
        boolean mesActualExcluido) {

    /** Valores de un mes para tendencia y correlaciones. */
    public record PuntoMensual(YearMonth mes, double ventas, double margenBrutoPorcentaje, double gastosOperativos,
                               double publicidad, double utilidadNeta) {
    }

    public AnalisisEstadistico {
        serieMensual = List.copyOf(serieMensual);
    }
}
