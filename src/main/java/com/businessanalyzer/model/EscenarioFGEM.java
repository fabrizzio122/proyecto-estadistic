package com.businessanalyzer.model;

import com.businessanalyzer.analytics.simulacion.SupuestosFGEM;

import java.time.LocalDateTime;

public record EscenarioFGEM(long idFGEM, long empresaIdFGEM, String nombreFGEM, String descripcionFGEM, SupuestosFGEM supuestosFGEM,
                        String creadoPorFGEM, LocalDateTime actualizadoEnFGEM) {

    @Override
    public String toString() {
        return nombreFGEM;
    }
}
