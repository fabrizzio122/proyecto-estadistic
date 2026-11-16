package com.businessanalyzer.model;

import java.time.LocalDate;

/**
 * Compromiso o recomendación acordada en una visita.
 *
 * @param visitaIdFGEM             visita en que se acordó
 * @param visitaVerificacionIdFGEM visita en que se verificó; {@code null} si se verificó fuera de una visita o sigue pendiente
 */
public record CompromisoFGEM(long idFGEM, long empresaIdFGEM, long visitaIdFGEM, LocalDate fechaVisitaFGEM, String descripcionFGEM,
                         LocalDate plazoFGEM, EstadoCompromisoFGEM estadoFGEM, LocalDate fechaVerificacionFGEM,
                         Long visitaVerificacionIdFGEM, String observacionFGEM, String verificadoPorFGEM) {

    /** Pendiente con el plazo ya cumplido. */
    public boolean vencidoFGEM(LocalDate hoyFGEM) {
        return estadoFGEM == EstadoCompromisoFGEM.PENDIENTE && plazoFGEM != null && plazoFGEM.isBefore(hoyFGEM);
    }
}
