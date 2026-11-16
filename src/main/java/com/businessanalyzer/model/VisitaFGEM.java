package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Visita de inspección a un productor.
 *
 * @param latitudFGEM    grados decimales; {@code null} si no se registró
 * @param aspectosFGEM   resultado de cada aspecto verificado (los no evaluados no aparecen)
 * @param notasFGEM      observación de cada aspecto (opcional)
 */
public record VisitaFGEM(long idFGEM, long empresaIdFGEM, LocalDate fechaFGEM, TipoVisitaFGEM tipoFGEM, String inspectorFGEM, String lugarFGEM,
                     Double latitudFGEM, Double longitudFGEM, String atendidoPorFGEM, String observacionesFGEM,
                     ConclusionVisitaFGEM conclusionFGEM, LocalDate proximaVisitaFGEM, boolean anuladaFGEM, String motivoAnulacionFGEM,
                     String registradoPorFGEM, Map<AspectoInspeccionFGEM, ResultadoAspectoFGEM> aspectosFGEM,
                     Map<AspectoInspeccionFGEM, String> notasFGEM) {

    public VisitaFGEM {
        aspectosFGEM = Map.copyOf(aspectosFGEM);
        notasFGEM = Map.copyOf(notasFGEM);
    }

    /** Parte de los aspectos aplicables que se cumplen (en parte vale la mitad); {@code null} si ninguno aplica. */
    public BigDecimal cumplimientoFGEM() {
        return cumplimientoFGEM(aspectosFGEM.values());
    }

    public static BigDecimal cumplimientoFGEM(java.util.Collection<ResultadoAspectoFGEM> resultadosFGEM) {
        List<BigDecimal> puntajesFGEM = resultadosFGEM.stream().map(ResultadoAspectoFGEM::puntajeFGEM).filter(pFGEM -> pFGEM != null).toList();
        if (puntajesFGEM.isEmpty()) {
            return null;
        }
        return puntajesFGEM.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(puntajesFGEM.size()), 6, RoundingMode.HALF_UP);
    }

    /** "-12.0464, -75.2048" o {@code null}. */
    public String coordenadasFGEM() {
        return latitudFGEM == null || longitudFGEM == null ? null
                : String.format(java.util.Locale.ROOT, "%.5f, %.5f", latitudFGEM, longitudFGEM);
    }
}
