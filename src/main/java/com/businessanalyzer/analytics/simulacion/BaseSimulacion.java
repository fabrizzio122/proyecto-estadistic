package com.businessanalyzer.analytics.simulacion;

import com.businessanalyzer.analytics.Margenes;
import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.ResumenFinanciero;
import com.businessanalyzer.analytics.ResumenOperaciones;
import com.businessanalyzer.model.GrupoFinanciero;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Situación de partida de una simulación: resultados reales de un mes (o el promedio mensual
 * de varios), separados según su comportamiento.
 * <pre>
 * Utilidad antes de impuestos = ventas − costo de ventas − gastos variables − costos fijos + otros ingresos
 * </pre>
 * Coincide con el estado de resultados: los gastos operativos y financieros se reparten entre
 * gastos variables y costos fijos; el impuesto a la renta va aparte.
 */
public record BaseSimulacion(
        Periodo periodo,
        int meses,
        BigDecimal ventas,
        BigDecimal costoVentas,
        BigDecimal gastosVariables,
        BigDecimal costosFijos,
        BigDecimal otrosIngresos,
        BigDecimal impuestoRenta) {

    /** Promedio mensual de los totales del período. */
    public static BaseSimulacion de(Periodo periodo, int meses, ResumenOperaciones operaciones,
                                    ResumenFinanciero finanzas) {
        BigDecimal divisor = BigDecimal.valueOf(Math.max(1, meses));
        return new BaseSimulacion(periodo, meses,
                mensual(operaciones.ventasNetas(), divisor),
                mensual(operaciones.costoVentas(), divisor),
                mensual(finanzas.costosVariablesEquilibrio(), divisor),
                mensual(finanzas.costosFijosEquilibrio(), divisor),
                mensual(finanzas.porGrupo(GrupoFinanciero.INGRESO_FINANCIERO)
                        .add(finanzas.porGrupo(GrupoFinanciero.OTRO_INGRESO)), divisor),
                mensual(finanzas.porGrupo(GrupoFinanciero.IMPUESTO_RENTA), divisor));
    }

    public BigDecimal utilidadAntesImpuestos() {
        return ventas.subtract(costoVentas).subtract(gastosVariables).subtract(costosFijos).add(otrosIngresos);
    }

    /** Impuesto ÷ utilidad antes de impuestos (0 si no hubo utilidad o impuesto). */
    public BigDecimal tasaImpuestoEfectiva() {
        BigDecimal uai = utilidadAntesImpuestos();
        if (uai.signum() <= 0 || impuestoRenta.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return impuestoRenta.divide(uai, Margenes.ESCALA_RATIO, RoundingMode.HALF_UP).min(BigDecimal.ONE);
    }

    public boolean sinVentas() {
        return ventas.signum() == 0;
    }

    private static BigDecimal mensual(BigDecimal total, BigDecimal divisor) {
        return total.divide(divisor, 2, RoundingMode.HALF_UP);
    }
}
