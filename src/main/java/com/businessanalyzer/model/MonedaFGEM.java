package com.businessanalyzer.model;

public enum MonedaFGEM {

    PEN("Sol peruano", "S/"),
    USD("Dólar estadounidense", "US$"),
    EUR("Euro", "€"),
    MXN("Peso mexicano", "MX$"),
    COP("Peso colombiano", "COL$"),
    CLP("Peso chileno", "CLP$"),
    ARS("Peso argentino", "AR$"),
    BOB("Boliviano", "Bs");

    private final String nombreFGEM;
    private final String simboloFGEM;

    MonedaFGEM(String nombreFGEM, String simboloFGEM) {
        this.nombreFGEM = nombreFGEM;
        this.simboloFGEM = simboloFGEM;
    }

    public String nombreFGEM() {
        return nombreFGEM;
    }

    public String simboloFGEM() {
        return simboloFGEM;
    }

    @Override
    public String toString() {
        return name() + " — " + nombreFGEM;
    }
}
