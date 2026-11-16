package com.businessanalyzer.model;

import com.businessanalyzer.analytics.simulacion.SupuestosFGEM;

import java.time.LocalDateTime;

/** Escenario de simulación guardado con un nombre para volver a usarlo y compararlo. */
public record EscenarioFGEM(long idFGEM, long empresaIdFGEM, String nombreFGEM, String descripcionFGEM, SupuestosFGEM supuestosFGEM,
                        String creadoPorFGEM, LocalDateTime actualizadoEnFGEM) {

    @Override
    public String toString() {
        return nombreFGEM;
    }
}
