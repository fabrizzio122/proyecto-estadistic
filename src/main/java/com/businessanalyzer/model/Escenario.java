package com.businessanalyzer.model;

import com.businessanalyzer.analytics.simulacion.Supuestos;

import java.time.LocalDateTime;

/** Escenario de simulación guardado con un nombre para volver a usarlo y compararlo. */
public record Escenario(long id, long empresaId, String nombre, String descripcion, Supuestos supuestos,
                        String creadoPor, LocalDateTime actualizadoEn) {

    @Override
    public String toString() {
        return nombre;
    }
}
