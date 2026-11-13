package com.businessanalyzer.model;

/** Moneda en la que la empresa registra sus operaciones (código ISO 4217). */
public enum Moneda {

    PEN("Sol peruano", "S/"),
    USD("Dólar estadounidense", "US$"),
    EUR("Euro", "€"),
    MXN("Peso mexicano", "MX$"),
    COP("Peso colombiano", "COL$"),
    CLP("Peso chileno", "CLP$"),
    ARS("Peso argentino", "AR$"),
    BOB("Boliviano", "Bs");

    private final String nombre;
    private final String simbolo;

    Moneda(String nombre, String simbolo) {
        this.nombre = nombre;
        this.simbolo = simbolo;
    }

    public String nombre() {
        return nombre;
    }

    public String simbolo() {
        return simbolo;
    }

    @Override
    public String toString() {
        return name() + " — " + nombre;
    }
}
