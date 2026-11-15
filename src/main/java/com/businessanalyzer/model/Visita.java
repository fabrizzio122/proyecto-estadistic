package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Visita de inspección a un productor.
 *
 * @param latitud    grados decimales; {@code null} si no se registró
 * @param aspectos   resultado de cada aspecto verificado (los no evaluados no aparecen)
 * @param notas      observación de cada aspecto (opcional)
 */
public record Visita(long id, long empresaId, LocalDate fecha, TipoVisita tipo, String inspector, String lugar,
                     Double latitud, Double longitud, String atendidoPor, String observaciones,
                     ConclusionVisita conclusion, LocalDate proximaVisita, boolean anulada, String motivoAnulacion,
                     String registradoPor, Map<AspectoInspeccion, ResultadoAspecto> aspectos,
                     Map<AspectoInspeccion, String> notas) {

    public Visita {
        aspectos = Map.copyOf(aspectos);
        notas = Map.copyOf(notas);
    }

    /** Parte de los aspectos aplicables que se cumplen (en parte vale la mitad); {@code null} si ninguno aplica. */
    public BigDecimal cumplimiento() {
        return cumplimiento(aspectos.values());
    }

    public static BigDecimal cumplimiento(java.util.Collection<ResultadoAspecto> resultados) {
        List<BigDecimal> puntajes = resultados.stream().map(ResultadoAspecto::puntaje).filter(p -> p != null).toList();
        if (puntajes.isEmpty()) {
            return null;
        }
        return puntajes.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(puntajes.size()), 6, RoundingMode.HALF_UP);
    }

    /** "-12.0464, -75.2048" o {@code null}. */
    public String coordenadas() {
        return latitud == null || longitud == null ? null
                : String.format(java.util.Locale.ROOT, "%.5f, %.5f", latitud, longitud);
    }
}
