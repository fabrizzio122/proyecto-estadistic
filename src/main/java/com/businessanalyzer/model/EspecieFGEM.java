package com.businessanalyzer.model;

import java.math.BigDecimal;

public enum EspecieFGEM {

    VACUNO("Vacunos", "vacas", pctFGEM(70), pctFGEM(55), pctFGEM(3), pctFGEM(6), pctFGEM(5), pctFGEM(10)),
    OVINO("Ovinos", "ovejas", pctFGEM(80), pctFGEM(65), pctFGEM(5), pctFGEM(10), pctFGEM(10), pctFGEM(20)),
    ALPACA("Alpacas", "madres", pctFGEM(60), pctFGEM(45), pctFGEM(5), pctFGEM(10), pctFGEM(10), pctFGEM(20)),
    LLAMA("Llamas", "madres", pctFGEM(60), pctFGEM(45), pctFGEM(5), pctFGEM(10), pctFGEM(10), pctFGEM(20));

    private final String descripcionFGEM;
    private final String hembrasFGEM;
    private final BigDecimal natalidadVerdeFGEM;
    private final BigDecimal natalidadAmarilloFGEM;
    private final BigDecimal mortalidadVerdeFGEM;
    private final BigDecimal mortalidadAmarilloFGEM;
    private final BigDecimal mortalidadCriasVerdeFGEM;
    private final BigDecimal mortalidadCriasAmarilloFGEM;

    EspecieFGEM(String descripcionFGEM, String hembrasFGEM, BigDecimal natalidadVerdeFGEM, BigDecimal natalidadAmarilloFGEM,
            BigDecimal mortalidadVerdeFGEM, BigDecimal mortalidadAmarilloFGEM,
            BigDecimal mortalidadCriasVerdeFGEM, BigDecimal mortalidadCriasAmarilloFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.hembrasFGEM = hembrasFGEM;
        this.natalidadVerdeFGEM = natalidadVerdeFGEM;
        this.natalidadAmarilloFGEM = natalidadAmarilloFGEM;
        this.mortalidadVerdeFGEM = mortalidadVerdeFGEM;
        this.mortalidadAmarilloFGEM = mortalidadAmarilloFGEM;
        this.mortalidadCriasVerdeFGEM = mortalidadCriasVerdeFGEM;
        this.mortalidadCriasAmarilloFGEM = mortalidadCriasAmarilloFGEM;
    }

    private static BigDecimal pctFGEM(int valorFGEM) {
        return BigDecimal.valueOf(valorFGEM, 2);
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public String hembrasFGEM() {
        return hembrasFGEM;
    }

    public BigDecimal natalidadVerdeFGEM() {
        return natalidadVerdeFGEM;
    }

    public BigDecimal natalidadAmarilloFGEM() {
        return natalidadAmarilloFGEM;
    }

    public BigDecimal mortalidadVerdeFGEM() {
        return mortalidadVerdeFGEM;
    }

    public BigDecimal mortalidadAmarilloFGEM() {
        return mortalidadAmarilloFGEM;
    }

    public BigDecimal mortalidadCriasVerdeFGEM() {
        return mortalidadCriasVerdeFGEM;
    }

    public BigDecimal mortalidadCriasAmarilloFGEM() {
        return mortalidadCriasAmarilloFGEM;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
