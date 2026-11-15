package com.businessanalyzer.model;

import com.businessanalyzer.analytics.diagnostico.Semaforo;

import java.math.BigDecimal;

/** Resultado de un aspecto verificado. */
public enum ResultadoAspecto {

    CUMPLE("Cumple", Semaforo.VERDE, BigDecimal.ONE),
    PARCIAL("Cumple en parte", Semaforo.AMARILLO, new BigDecimal("0.5")),
    NO_CUMPLE("No cumple", Semaforo.ROJO, BigDecimal.ZERO),
    NO_APLICA("No aplica", Semaforo.SIN_DATOS, null);

    private final String descripcion;
    private final Semaforo semaforo;
    private final BigDecimal puntaje;

    ResultadoAspecto(String descripcion, Semaforo semaforo, BigDecimal puntaje) {
        this.descripcion = descripcion;
        this.semaforo = semaforo;
        this.puntaje = puntaje;
    }

    public String descripcion() {
        return descripcion;
    }

    public Semaforo semaforo() {
        return semaforo;
    }

    /** 1, 0.5 o 0; {@code null} si no aplica (no cuenta para el porcentaje de cumplimiento). */
    public BigDecimal puntaje() {
        return puntaje;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
