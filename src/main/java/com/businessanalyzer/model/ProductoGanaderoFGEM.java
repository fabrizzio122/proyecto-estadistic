package com.businessanalyzer.model;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Productos que se obtienen del ganado sin venderlo: leche, fibra y lana. */
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

    /** Unidad en plural ("litros", "kilos"). */
    public String unidadFGEM() {
        return unidadFGEM;
    }

    public String abreviaturaFGEM() {
        return abreviaturaFGEM;
    }

    /** Qué animales se cuentan para el promedio ("vacas en ordeño"). */
    public String animalesFGEM() {
        return animalesFGEM;
    }

    public boolean admiteFGEM(EspecieFGEM especieFGEM) {
        return especiesFGEM.contains(especieFGEM);
    }

    /** La leche se promedia por animal y por día; la fibra y la lana, por animal esquilado. */
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
