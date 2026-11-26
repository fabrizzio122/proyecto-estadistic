package com.businessanalyzer.model;

import java.time.LocalDate;

public record CompromisoFGEM(long idFGEM, long empresaIdFGEM, long visitaIdFGEM, LocalDate fechaVisitaFGEM, String descripcionFGEM,
                         LocalDate plazoFGEM, EstadoCompromisoFGEM estadoFGEM, LocalDate fechaVerificacionFGEM,
                         Long visitaVerificacionIdFGEM, String observacionFGEM, String verificadoPorFGEM) {

    public boolean vencidoFGEM(LocalDate hoyFGEM) {
        return estadoFGEM == EstadoCompromisoFGEM.PENDIENTE && plazoFGEM != null && plazoFGEM.isBefore(hoyFGEM);
    }
}
