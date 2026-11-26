package com.businessanalyzer.model;

import java.math.BigDecimal;

public enum CultivoFGEM {

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

    private final String descripcionFGEM;
    private final int rendimientoReferenciaFGEM;

    CultivoFGEM(String descripcionFGEM, int rendimientoReferenciaFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.rendimientoReferenciaFGEM = rendimientoReferenciaFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public BigDecimal rendimientoReferenciaFGEM() {
        return rendimientoReferenciaFGEM == 0 ? null : BigDecimal.valueOf(rendimientoReferenciaFGEM);
    }

    public boolean forrajeFGEM() {
        return this == AVENA_FORRAJERA || this == ALFALFA;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
