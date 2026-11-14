package com.businessanalyzer.analytics;

import com.businessanalyzer.model.CategoriaFinanciera;
import com.businessanalyzer.model.Comportamiento;
import com.businessanalyzer.model.GrupoFinanciero;
import com.businessanalyzer.model.MovimientoFinanciero;
import com.businessanalyzer.model.TipoMovimiento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Totales de los ingresos y gastos no comerciales de un período (sin anulados).
 *
 * @param gastosFijos               gastos marcados como fijos
 * @param gastosVariables           gastos marcados como variables (no incluye el costo de lo vendido)
 * @param costosFijosEquilibrio     gastos fijos sin el impuesto a la renta (para el punto de equilibrio)
 * @param costosVariablesEquilibrio gastos variables sin el impuesto a la renta (para el punto de equilibrio)
 */
public record ResumenFinanciero(
        BigDecimal ingresos,
        BigDecimal gastos,
        BigDecimal gastosFijos,
        BigDecimal gastosVariables,
        BigDecimal costosFijosEquilibrio,
        BigDecimal costosVariablesEquilibrio,
        Map<CategoriaFinanciera, BigDecimal> porCategoria,
        int cantidadMovimientos) {

    public static final ResumenFinanciero VACIO = de(java.util.List.of());

    public ResumenFinanciero {
        porCategoria = Collections.unmodifiableMap(new EnumMap<>(porCategoria.isEmpty()
                ? new EnumMap<>(CategoriaFinanciera.class) : porCategoria));
    }

    public static ResumenFinanciero de(Collection<MovimientoFinanciero> movimientos) {
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal fijos = BigDecimal.ZERO;
        BigDecimal variables = BigDecimal.ZERO;
        BigDecimal fijosEquilibrio = BigDecimal.ZERO;
        BigDecimal variablesEquilibrio = BigDecimal.ZERO;
        Map<CategoriaFinanciera, BigDecimal> porCategoria = new EnumMap<>(CategoriaFinanciera.class);
        int cantidad = 0;
        for (MovimientoFinanciero m : movimientos) {
            if (m.anulado()) {
                continue;
            }
            cantidad++;
            porCategoria.merge(m.categoria(), m.monto(), BigDecimal::add);
            if (m.tipo() == TipoMovimiento.INGRESO) {
                ingresos = ingresos.add(m.monto());
                continue;
            }
            boolean cuentaParaEquilibrio = m.categoria().grupo() != GrupoFinanciero.IMPUESTO_RENTA;
            if (m.comportamiento() == Comportamiento.FIJO) {
                fijos = fijos.add(m.monto());
                if (cuentaParaEquilibrio) {
                    fijosEquilibrio = fijosEquilibrio.add(m.monto());
                }
            } else {
                variables = variables.add(m.monto());
                if (cuentaParaEquilibrio) {
                    variablesEquilibrio = variablesEquilibrio.add(m.monto());
                }
            }
        }
        return new ResumenFinanciero(ingresos, fijos.add(variables), fijos, variables, fijosEquilibrio,
                variablesEquilibrio, porCategoria, cantidad);
    }

    /** Suma de las categorías de un grupo del estado de resultados. */
    public BigDecimal porGrupo(GrupoFinanciero grupo) {
        return porCategoria.entrySet().stream()
                .filter(e -> e.getKey().grupo() == grupo)
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Proporción de los gastos que son fijos; {@code null} si no hubo gastos. */
    public BigDecimal participacionFijos() {
        return gastos.signum() == 0 ? null : gastosFijos.divide(gastos, Margenes.ESCALA_RATIO, RoundingMode.HALF_UP);
    }
}
