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
 * @param intervaloDiarioFGEM   intervalo de confianza de la venta diaria promedio
 * @param anovaDiasFGEM        ANOVA de las ventas diarias por día de la semana
 * @param periodoAnteriorFGEM  período con el que se compara (el anterior equivalente)
 * @param comparacionFGEM      prueba t de las ventas diarias del período contra las del anterior
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
        boolean mesActualExcluidoFGEM,
        InferenciaFGEM.IntervaloConfianzaFGEM intervaloDiarioFGEM,
        InferenciaFGEM.AnovaFGEM anovaDiasFGEM,
        PeriodoFGEM periodoAnteriorFGEM,
        InferenciaFGEM.ComparacionFGEM comparacionFGEM) {

    /** Valores de un mes para tendencia y correlaciones. */
    public record PuntoMensualFGEM(YearMonth mesFGEM, double ventasFGEM, double margenBrutoPorcentajeFGEM, double gastosOperativosFGEM,
                               double publicidadFGEM, double utilidadNetaFGEM) {
    }

    public AnalisisEstadisticoFGEM {
        serieMensualFGEM = List.copyOf(serieMensualFGEM);
    }
}
