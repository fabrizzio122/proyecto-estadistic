package com.businessanalyzer.model;

import com.businessanalyzer.analytics.diagnostico.Semaforo;

import java.math.BigDecimal;

/** Conclusión del inspector sobre la visita. */
public enum ConclusionVisita {

    FAVORABLE("Favorable", Semaforo.VERDE),
    CON_OBSERVACIONES("Favorable con observaciones", Semaforo.AMARILLO),
    DESFAVORABLE("Desfavorable", Semaforo.ROJO);

    private final String descripcion;
    private final Semaforo semaforo;

    ConclusionVisita(String descripcion, Semaforo semaforo) {
        this.descripcion = descripcion;
        this.semaforo = semaforo;
    }

    public String descripcion() {
        return descripcion;
    }

    public Semaforo semaforo() {
        return semaforo;
    }

    /** Conclusión que se propone según el cumplimiento: desde 80 % favorable; desde 50 %, con observaciones. */
    public static ConclusionVisita sugerida(BigDecimal cumplimiento) {
        if (cumplimiento == null || cumplimiento.compareTo(new BigDecimal("0.80")) >= 0) {
            return FAVORABLE;
        }
        return cumplimiento.compareTo(new BigDecimal("0.50")) >= 0 ? CON_OBSERVACIONES : DESFAVORABLE;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
