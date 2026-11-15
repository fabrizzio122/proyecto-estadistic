package com.businessanalyzer.model;

import java.time.LocalDate;

/**
 * Compromiso o recomendación acordada en una visita.
 *
 * @param visitaId             visita en que se acordó
 * @param visitaVerificacionId visita en que se verificó; {@code null} si se verificó fuera de una visita o sigue pendiente
 */
public record Compromiso(long id, long empresaId, long visitaId, LocalDate fechaVisita, String descripcion,
                         LocalDate plazo, EstadoCompromiso estado, LocalDate fechaVerificacion,
                         Long visitaVerificacionId, String observacion, String verificadoPor) {

    /** Pendiente con el plazo ya cumplido. */
    public boolean vencido(LocalDate hoy) {
        return estado == EstadoCompromiso.PENDIENTE && plazo != null && plazo.isBefore(hoy);
    }
}
