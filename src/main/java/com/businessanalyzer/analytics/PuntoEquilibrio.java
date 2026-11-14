package com.businessanalyzer.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Punto de equilibrio en ventas (negocios con varios productos).
 * <ul>
 *     <li><b>Costos variables</b> = costo de lo vendido + gastos variables.</li>
 *     <li><b>Margen de contribución</b> = ventas − costos variables: lo que queda de las ventas
 *         para pagar los costos fijos y generar ganancia.</li>
 *     <li><b>Razón de contribución</b> = margen de contribución ÷ ventas.</li>
 *     <li><b>Ventas de equilibrio</b> = costos fijos ÷ razón de contribución: con ese nivel de
 *         ventas no se gana ni se pierde.</li>
 *     <li><b>Margen de seguridad</b> = (ventas − ventas de equilibrio) ÷ ventas: cuánto pueden caer
 *         las ventas antes de entrar en pérdida.</li>
 * </ul>
 * El impuesto a la renta no se incluye. Supone que la mezcla de productos vendidos se mantiene.
 *
 * @param ventasEquilibrio {@code null} si la razón de contribución es cero o negativa
 *                         (ninguna venta cubre sus costos variables)
 */
public record PuntoEquilibrio(
        BigDecimal ventas,
        BigDecimal costosVariables,
        BigDecimal costosFijos,
        BigDecimal margenContribucion,
        BigDecimal razonContribucion,
        BigDecimal ventasEquilibrio,
        BigDecimal margenSeguridad) {

    public static PuntoEquilibrio de(ResumenOperaciones operaciones, ResumenFinanciero finanzas) {
        return calcular(operaciones.ventasNetas(),
                operaciones.costoVentas().add(finanzas.costosVariablesEquilibrio()),
                finanzas.costosFijosEquilibrio());
    }

    public static PuntoEquilibrio calcular(BigDecimal ventas, BigDecimal costosVariables, BigDecimal costosFijos) {
        BigDecimal margen = ventas.subtract(costosVariables);
        BigDecimal razon = ventas.signum() == 0
                ? null
                : margen.divide(ventas, Margenes.ESCALA_RATIO, RoundingMode.HALF_UP);
        BigDecimal equilibrio = razon == null || razon.signum() <= 0
                ? null
                : costosFijos.divide(razon, 2, RoundingMode.HALF_UP);
        BigDecimal seguridad = equilibrio == null
                ? null
                : ventas.subtract(equilibrio).divide(ventas, Margenes.ESCALA_RATIO, RoundingMode.HALF_UP);
        return new PuntoEquilibrio(ventas, costosVariables, costosFijos, margen, razon, equilibrio, seguridad);
    }

    /** Las ventas del período ya cubren todos los costos. */
    public boolean alcanzado() {
        return ventasEquilibrio != null && ventas.compareTo(ventasEquilibrio) >= 0;
    }

    /** Ventas ÷ ventas de equilibrio (1.20 = 20 % por encima); {@code null} si no aplica. */
    public BigDecimal cobertura() {
        return ventasEquilibrio == null || ventasEquilibrio.signum() == 0
                ? null
                : ventas.divide(ventasEquilibrio, Margenes.ESCALA_RATIO, RoundingMode.HALF_UP);
    }

    /** Ventas que faltan para llegar al equilibrio (cero si ya se alcanzó). */
    public BigDecimal ventasFaltantes() {
        return ventasEquilibrio == null ? null : ventasEquilibrio.subtract(ventas).max(BigDecimal.ZERO);
    }

    /** Costos variables ÷ ventas (para dibujar la recta de costos totales). */
    public BigDecimal razonCostoVariable() {
        return razonContribucion == null ? null : BigDecimal.ONE.subtract(razonContribucion);
    }
}
