package com.businessanalyzer.analytics.simulacion;

import com.businessanalyzer.analytics.Margenes;
import com.businessanalyzer.analytics.PuntoEquilibrio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Resultados mensuales de un escenario. */
public record ResultadoEscenario(
        BigDecimal ventas,
        BigDecimal costoVentas,
        BigDecimal gastosVariables,
        BigDecimal costosFijos,
        BigDecimal otrosIngresos,
        BigDecimal utilidadAntesImpuestos,
        BigDecimal impuesto,
        BigDecimal utilidadNeta) {

    /** Ventas − costo de ventas − gastos variables. */
    public BigDecimal margenContribucion() {
        return ventas.subtract(costoVentas).subtract(gastosVariables);
    }

    public BigDecimal costosTotales() {
        return costoVentas.add(gastosVariables).add(costosFijos);
    }

    /** Utilidad neta ÷ ventas; {@code null} sin ventas. */
    public BigDecimal margenNeto() {
        return ventas.signum() == 0 ? null : utilidadNeta.divide(ventas, Margenes.ESCALA_RATIO, RoundingMode.HALF_UP);
    }

    public PuntoEquilibrio equilibrio() {
        return PuntoEquilibrio.calcular(ventas, costoVentas.add(gastosVariables), costosFijos);
    }
}
