package com.businessanalyzer.analytics.simulacion;

import com.businessanalyzer.analytics.MargenesFGEM;
import com.businessanalyzer.analytics.PuntoEquilibrioFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Resultados mensuales de un escenario. */
public record ResultadoEscenarioFGEM(
        BigDecimal ventasFGEM,
        BigDecimal costoVentasFGEM,
        BigDecimal gastosVariablesFGEM,
        BigDecimal costosFijosFGEM,
        BigDecimal otrosIngresosFGEM,
        BigDecimal utilidadAntesImpuestosFGEM,
        BigDecimal impuestoFGEM,
        BigDecimal utilidadNetaFGEM) {

    /** Ventas − costo de ventas − gastos variables. */
    public BigDecimal margenContribucionFGEM() {
        return ventasFGEM.subtract(costoVentasFGEM).subtract(gastosVariablesFGEM);
    }

    public BigDecimal costosTotalesFGEM() {
        return costoVentasFGEM.add(gastosVariablesFGEM).add(costosFijosFGEM);
    }

    /** Utilidad neta ÷ ventas; {@code null} sin ventas. */
    public BigDecimal margenNetoFGEM() {
        return ventasFGEM.signum() == 0 ? null : utilidadNetaFGEM.divide(ventasFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
    }

    public PuntoEquilibrioFGEM equilibrioFGEM() {
        return PuntoEquilibrioFGEM.calcularFGEM(ventasFGEM, costoVentasFGEM.add(gastosVariablesFGEM), costosFijosFGEM);
    }
}
