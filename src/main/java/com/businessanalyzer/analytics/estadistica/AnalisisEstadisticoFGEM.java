package com.businessanalyzer.analytics.estadistica;

import com.businessanalyzer.analytics.PeriodoFGEM;

import java.time.YearMonth;
import java.util.List;

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

    public record PuntoMensualFGEM(YearMonth mesFGEM, double ventasFGEM, double margenBrutoPorcentajeFGEM, double gastosOperativosFGEM,
                               double publicidadFGEM, double utilidadNetaFGEM) {
    }

    public AnalisisEstadisticoFGEM {
        serieMensualFGEM = List.copyOf(serieMensualFGEM);
    }
}
