package com.businessanalyzer.model;

import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;

import java.math.BigDecimal;

/** Conclusión del inspector sobre la visita. */
public enum ConclusionVisitaFGEM {

    FAVORABLE("Favorable", SemaforoFGEM.VERDE),
    CON_OBSERVACIONES("Favorable con observaciones", SemaforoFGEM.AMARILLO),
    DESFAVORABLE("Desfavorable", SemaforoFGEM.ROJO);

    private final String descripcionFGEM;
    private final SemaforoFGEM semaforoFGEM;

    ConclusionVisitaFGEM(String descripcionFGEM, SemaforoFGEM semaforoFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.semaforoFGEM = semaforoFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public SemaforoFGEM semaforoFGEM() {
        return semaforoFGEM;
    }

    /** Conclusión que se propone según el cumplimiento: desde 80 % favorable; desde 50 %, con observaciones. */
    public static ConclusionVisitaFGEM sugeridaFGEM(BigDecimal cumplimientoFGEM) {
        if (cumplimientoFGEM == null || cumplimientoFGEM.compareTo(new BigDecimal("0.80")) >= 0) {
            return FAVORABLE;
        }
        return cumplimientoFGEM.compareTo(new BigDecimal("0.50")) >= 0 ? CON_OBSERVACIONES : DESFAVORABLE;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
