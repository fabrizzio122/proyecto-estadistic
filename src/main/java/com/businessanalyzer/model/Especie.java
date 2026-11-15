package com.businessanalyzer.model;

import java.math.BigDecimal;

/**
 * Especies ganaderas que se pueden registrar, con los umbrales de referencia para el semáforo
 * de sus indicadores (valores habituales en la ganadería familiar y mediana del Perú).
 */
public enum Especie {

    VACUNO("Vacunos", "vacas", pct(70), pct(55), pct(3), pct(6), pct(5), pct(10)),
    OVINO("Ovinos", "ovejas", pct(80), pct(65), pct(5), pct(10), pct(10), pct(20)),
    ALPACA("Alpacas", "madres", pct(60), pct(45), pct(5), pct(10), pct(10), pct(20)),
    LLAMA("Llamas", "madres", pct(60), pct(45), pct(5), pct(10), pct(10), pct(20));

    private final String descripcion;
    private final String hembras;
    private final BigDecimal natalidadVerde;
    private final BigDecimal natalidadAmarillo;
    private final BigDecimal mortalidadVerde;
    private final BigDecimal mortalidadAmarillo;
    private final BigDecimal mortalidadCriasVerde;
    private final BigDecimal mortalidadCriasAmarillo;

    Especie(String descripcion, String hembras, BigDecimal natalidadVerde, BigDecimal natalidadAmarillo,
            BigDecimal mortalidadVerde, BigDecimal mortalidadAmarillo,
            BigDecimal mortalidadCriasVerde, BigDecimal mortalidadCriasAmarillo) {
        this.descripcion = descripcion;
        this.hembras = hembras;
        this.natalidadVerde = natalidadVerde;
        this.natalidadAmarillo = natalidadAmarillo;
        this.mortalidadVerde = mortalidadVerde;
        this.mortalidadAmarillo = mortalidadAmarillo;
        this.mortalidadCriasVerde = mortalidadCriasVerde;
        this.mortalidadCriasAmarillo = mortalidadCriasAmarillo;
    }

    private static BigDecimal pct(int valor) {
        return BigDecimal.valueOf(valor, 2);
    }

    public String descripcion() {
        return descripcion;
    }

    /** Nombre de las hembras reproductoras en plural ("vacas", "ovejas", "madres"). */
    public String hembras() {
        return hembras;
    }

    public BigDecimal natalidadVerde() {
        return natalidadVerde;
    }

    public BigDecimal natalidadAmarillo() {
        return natalidadAmarillo;
    }

    public BigDecimal mortalidadVerde() {
        return mortalidadVerde;
    }

    public BigDecimal mortalidadAmarillo() {
        return mortalidadAmarillo;
    }

    public BigDecimal mortalidadCriasVerde() {
        return mortalidadCriasVerde;
    }

    public BigDecimal mortalidadCriasAmarillo() {
        return mortalidadCriasAmarillo;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
