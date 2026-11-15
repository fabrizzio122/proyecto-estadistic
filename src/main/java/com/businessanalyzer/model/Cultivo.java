package com.businessanalyzer.model;

import java.math.BigDecimal;

/**
 * Cultivos frecuentes en la agricultura familiar y mediana del Perú, con un rendimiento de
 * referencia aproximado (promedio nacional, kg por hectárea). En cada campaña se puede ajustar
 * la referencia a la de la zona.
 */
public enum Cultivo {

    PAPA("Papa", 15_000),
    MAIZ_AMILACEO("Maíz amiláceo", 1_400),
    MAIZ_AMARILLO("Maíz amarillo duro", 4_800),
    MAIZ_CHOCLO("Maíz choclo", 9_000),
    QUINUA("Quinua", 1_200),
    CEBADA("Cebada grano", 1_500),
    TRIGO("Trigo", 1_500),
    HABA("Haba grano seco", 1_400),
    ARVEJA("Arveja grano seco", 1_200),
    FRIJOL("Frijol grano seco", 1_200),
    OLLUCO("Olluco", 7_000),
    OCA("Oca", 7_500),
    AVENA_FORRAJERA("Avena forrajera (verde)", 20_000),
    ALFALFA("Alfalfa (verde)", 40_000),
    OTRO("Otro cultivo", 0);

    private final String descripcion;
    private final int rendimientoReferencia;

    Cultivo(String descripcion, int rendimientoReferencia) {
        this.descripcion = descripcion;
        this.rendimientoReferencia = rendimientoReferencia;
    }

    public String descripcion() {
        return descripcion;
    }

    /** Rendimiento de referencia en kg/ha; {@code null} si no hay referencia (otro cultivo). */
    public BigDecimal rendimientoReferencia() {
        return rendimientoReferencia == 0 ? null : BigDecimal.valueOf(rendimientoReferencia);
    }

    /** Forrajes: se cosechan para alimentar al ganado. */
    public boolean forraje() {
        return this == AVENA_FORRAJERA || this == ALFALFA;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
