package com.businessanalyzer.analytics;

import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.ComportamientoFGEM;
import com.businessanalyzer.model.GrupoFinancieroFGEM;
import com.businessanalyzer.model.MovimientoFinancieroFGEM;
import com.businessanalyzer.model.TipoMovimientoFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Totales de los ingresos y gastos no comerciales de un período (sin anulados).
 *
 * @param gastosFijosFGEM               gastos marcados como fijos
 * @param gastosVariablesFGEM           gastos marcados como variables (no incluye el costo de lo vendido)
 * @param costosFijosEquilibrioFGEM     gastos fijos sin el impuesto a la renta (para el punto de equilibrio)
 * @param costosVariablesEquilibrioFGEM gastos variables sin el impuesto a la renta (para el punto de equilibrio)
 */
public record ResumenFinancieroFGEM(
        BigDecimal ingresosFGEM,
        BigDecimal gastosFGEM,
        BigDecimal gastosFijosFGEM,
        BigDecimal gastosVariablesFGEM,
        BigDecimal costosFijosEquilibrioFGEM,
        BigDecimal costosVariablesEquilibrioFGEM,
        Map<CategoriaFinancieraFGEM, BigDecimal> porCategoriaFGEM,
        int cantidadMovimientosFGEM) {

    public static final ResumenFinancieroFGEM VACIO_FGEM = deFGEM(java.util.List.of());

    public ResumenFinancieroFGEM {
        porCategoriaFGEM = Collections.unmodifiableMap(new EnumMap<>(porCategoriaFGEM.isEmpty()
                ? new EnumMap<>(CategoriaFinancieraFGEM.class) : porCategoriaFGEM));
    }

    public static ResumenFinancieroFGEM deFGEM(Collection<MovimientoFinancieroFGEM> movimientosFGEM) {
        BigDecimal ingresosFGEM = BigDecimal.ZERO;
        BigDecimal fijosFGEM = BigDecimal.ZERO;
        BigDecimal variablesFGEM = BigDecimal.ZERO;
        BigDecimal fijosEquilibrioFGEM = BigDecimal.ZERO;
        BigDecimal variablesEquilibrioFGEM = BigDecimal.ZERO;
        Map<CategoriaFinancieraFGEM, BigDecimal> porCategoriaFGEM = new EnumMap<>(CategoriaFinancieraFGEM.class);
        int cantidadFGEM = 0;
        for (MovimientoFinancieroFGEM mFGEM : movimientosFGEM) {
            if (mFGEM.anuladoFGEM()) {
                continue;
            }
            cantidadFGEM++;
            porCategoriaFGEM.merge(mFGEM.categoriaFGEM(), mFGEM.montoFGEM(), BigDecimal::add);
            if (mFGEM.tipoFGEM() == TipoMovimientoFGEM.INGRESO) {
                ingresosFGEM = ingresosFGEM.add(mFGEM.montoFGEM());
                continue;
            }
            boolean cuentaParaEquilibrioFGEM = mFGEM.categoriaFGEM().grupoFGEM() != GrupoFinancieroFGEM.IMPUESTO_RENTA;
            if (mFGEM.comportamientoFGEM() == ComportamientoFGEM.FIJO) {
                fijosFGEM = fijosFGEM.add(mFGEM.montoFGEM());
                if (cuentaParaEquilibrioFGEM) {
                    fijosEquilibrioFGEM = fijosEquilibrioFGEM.add(mFGEM.montoFGEM());
                }
            } else {
                variablesFGEM = variablesFGEM.add(mFGEM.montoFGEM());
                if (cuentaParaEquilibrioFGEM) {
                    variablesEquilibrioFGEM = variablesEquilibrioFGEM.add(mFGEM.montoFGEM());
                }
            }
        }
        return new ResumenFinancieroFGEM(ingresosFGEM, fijosFGEM.add(variablesFGEM), fijosFGEM, variablesFGEM, fijosEquilibrioFGEM,
                variablesEquilibrioFGEM, porCategoriaFGEM, cantidadFGEM);
    }

    /** Suma de las categorías de un grupo del estado de resultados. */
    public BigDecimal porGrupoFGEM(GrupoFinancieroFGEM grupoFGEM) {
        return porCategoriaFGEM.entrySet().stream()
                .filter(eFGEM -> eFGEM.getKey().grupoFGEM() == grupoFGEM)
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Proporción de los gastos que son fijos; {@code null} si no hubo gastos. */
    public BigDecimal participacionFijosFGEM() {
        return gastosFGEM.signum() == 0 ? null : gastosFijosFGEM.divide(gastosFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
    }
}
