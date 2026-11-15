package com.businessanalyzer.model;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Productos que se obtienen del ganado sin venderlo: leche, fibra y lana. */
public enum ProductoGanadero {

    LECHE("Leche", "litros", "l", "vacas en ordeño", Set.of(Especie.VACUNO)),
    FIBRA("Fibra", "kilos", "kg", "animales esquilados", Set.of(Especie.ALPACA, Especie.LLAMA)),
    LANA("Lana", "kilos", "kg", "animales esquilados", Set.of(Especie.OVINO));

    private final String descripcion;
    private final String unidad;
    private final String abreviatura;
    private final String animales;
    private final Set<Especie> especies;

    ProductoGanadero(String descripcion, String unidad, String abreviatura, String animales, Set<Especie> especies) {
        this.descripcion = descripcion;
        this.unidad = unidad;
        this.abreviatura = abreviatura;
        this.animales = animales;
        this.especies = especies;
    }

    public String descripcion() {
        return descripcion;
    }

    /** Unidad en plural ("litros", "kilos"). */
    public String unidad() {
        return unidad;
    }

    public String abreviatura() {
        return abreviatura;
    }

    /** Qué animales se cuentan para el promedio ("vacas en ordeño"). */
    public String animales() {
        return animales;
    }

    public boolean admite(Especie especie) {
        return especies.contains(especie);
    }

    /** La leche se promedia por animal y por día; la fibra y la lana, por animal esquilado. */
    public boolean porDia() {
        return this == LECHE;
    }

    public static List<ProductoGanadero> de(Especie especie) {
        return Arrays.stream(values()).filter(p -> p.admite(especie)).toList();
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
