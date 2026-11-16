package com.businessanalyzer.analytics.estadistica;

import com.businessanalyzer.analytics.PeriodoFGEM;

import java.time.YearMonth;
import java.util.List;

/**
 * Resultado del análisis estadístico de un período.
 *
 * @param ticketFGEM            estadística del monto de cada venta (sin impuestos)
 * @param ventasDiariasFGEM     estadística de las ventas de cada día (incluye días sin ventas)
 * @param serieMensualFGEM      meses completos usados para tendencia y correlaciones
 * @param mesActualExcluidoFGEM el mes en curso no se usó en la tendencia por estar incompleto
 */
public record AnalisisEstadisticoFGEM(
        PeriodoFGEM periodoFGEM,
        EstadisticaDescriptivaFGEM ticketFGEM,
        EstadisticaDescriptivaFGEM ventasDiariasFGEM,
        HistogramaFGEM histogramaTicketsFGEM,
        EstacionalidadSemanalFGEM estacionalidadFGEM,
        List<PuntoMensualFGEM> serieMensualFGEM,
        TendenciaFGEM tendenciaVentasFGEM,
        MatrizCorrelacionFGEM correlacionesFGEM,
        AnalisisAbcFGEM abcFGEM,
        boolean mesActualExcluidoFGEM) {

    /** Valores de un mes para tendencia y correlaciones. */
    public record PuntoMensualFGEM(YearMonth mesFGEM, double ventasFGEM, double margenBrutoPorcentajeFGEM, double gastosOperativosFGEM,
                               double publicidadFGEM, double utilidadNetaFGEM) {
    }

    public AnalisisEstadisticoFGEM {
        serieMensualFGEM = List.copyOf(serieMensualFGEM);
    }
}
