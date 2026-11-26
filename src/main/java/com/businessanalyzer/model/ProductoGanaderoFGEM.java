package com.businessanalyzer.model;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public enum ProductoGanaderoFGEM {

    LECHE("Leche", "litros", "l", "vacas en ordeño", Set.of(EspecieFGEM.VACUNO)),
    FIBRA("Fibra", "kilos", "kg", "animales esquilados", Set.of(EspecieFGEM.ALPACA, EspecieFGEM.LLAMA)),
    LANA("Lana", "kilos", "kg", "animales esquilados", Set.of(EspecieFGEM.OVINO));

    private final String descripcionFGEM;
    private final String unidadFGEM;
    private final String abreviaturaFGEM;
    private final String animalesFGEM;
    private final Set<EspecieFGEM> especiesFGEM;

    ProductoGanaderoFGEM(String descripcionFGEM, String unidadFGEM, String abreviaturaFGEM, String animalesFGEM, Set<EspecieFGEM> especiesFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.unidadFGEM = unidadFGEM;
        this.abreviaturaFGEM = abreviaturaFGEM;
        this.animalesFGEM = animalesFGEM;
        this.especiesFGEM = especiesFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public String unidadFGEM() {
        return unidadFGEM;
    }

    public String abreviaturaFGEM() {
        return abreviaturaFGEM;
    }

    public String animalesFGEM() {
        return animalesFGEM;
    }

    public boolean admiteFGEM(EspecieFGEM especieFGEM) {
        return especiesFGEM.contains(especieFGEM);
    }

    public boolean porDiaFGEM() {
        return this == LECHE;
    }

    public static List<ProductoGanaderoFGEM> deFGEM(EspecieFGEM especieFGEM) {
        return Arrays.stream(values()).filter(pFGEM -> pFGEM.admiteFGEM(especieFGEM)).toList();
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
