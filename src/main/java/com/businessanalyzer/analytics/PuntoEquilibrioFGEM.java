package com.businessanalyzer.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record PuntoEquilibrioFGEM(
        BigDecimal ventasFGEM,
        BigDecimal costosVariablesFGEM,
        BigDecimal costosFijosFGEM,
        BigDecimal margenContribucionFGEM,
        BigDecimal razonContribucionFGEM,
        BigDecimal ventasEquilibrioFGEM,
        BigDecimal margenSeguridadFGEM) {

    public static PuntoEquilibrioFGEM deFGEM(ResumenOperacionesFGEM operacionesFGEM, ResumenFinancieroFGEM finanzasFGEM) {
        return calcularFGEM(operacionesFGEM.ventasNetasFGEM(),
                operacionesFGEM.costoVentasFGEM().add(finanzasFGEM.costosVariablesEquilibrioFGEM()),
                finanzasFGEM.costosFijosEquilibrioFGEM());
    }

    public static PuntoEquilibrioFGEM calcularFGEM(BigDecimal ventasFGEM, BigDecimal costosVariablesFGEM, BigDecimal costosFijosFGEM) {
        BigDecimal margenFGEM = ventasFGEM.subtract(costosVariablesFGEM);
        BigDecimal razonFGEM = ventasFGEM.signum() == 0
                ? null
                : margenFGEM.divide(ventasFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
        BigDecimal equilibrioFGEM = razonFGEM == null || razonFGEM.signum() <= 0 || margenFGEM.signum() <= 0
                ? null
                : costosFijosFGEM.multiply(ventasFGEM).divide(margenFGEM, 2, RoundingMode.HALF_UP);
        BigDecimal seguridadFGEM = equilibrioFGEM == null
                ? null
                : ventasFGEM.subtract(equilibrioFGEM).divide(ventasFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
        return new PuntoEquilibrioFGEM(ventasFGEM, costosVariablesFGEM, costosFijosFGEM, margenFGEM, razonFGEM, equilibrioFGEM, seguridadFGEM);
    }

    public boolean alcanzadoFGEM() {
        return ventasEquilibrioFGEM != null && ventasFGEM.compareTo(ventasEquilibrioFGEM) >= 0;
    }

    public BigDecimal coberturaFGEM() {
        return ventasEquilibrioFGEM == null || ventasEquilibrioFGEM.signum() == 0
                ? null
                : ventasFGEM.divide(ventasEquilibrioFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
    }

    public BigDecimal ventasFaltantesFGEM() {
        return ventasEquilibrioFGEM == null ? null : ventasEquilibrioFGEM.subtract(ventasFGEM).max(BigDecimal.ZERO);
    }

    public BigDecimal razonCostoVariableFGEM() {
        return razonContribucionFGEM == null ? null : BigDecimal.ONE.subtract(razonContribucionFGEM);
    }
}
