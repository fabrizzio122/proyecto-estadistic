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
 * @param ventasEquilibrioFGEM {@code null} si la razón de contribución es cero o negativa
 *                         (ninguna venta cubre sus costos variables)
 */
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
        BigDecimal equilibrioFGEM = razonFGEM == null || razonFGEM.signum() <= 0
                ? null
                : costosFijosFGEM.divide(razonFGEM, 2, RoundingMode.HALF_UP);
        BigDecimal seguridadFGEM = equilibrioFGEM == null
                ? null
                : ventasFGEM.subtract(equilibrioFGEM).divide(ventasFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
        return new PuntoEquilibrioFGEM(ventasFGEM, costosVariablesFGEM, costosFijosFGEM, margenFGEM, razonFGEM, equilibrioFGEM, seguridadFGEM);
    }

    /** Las ventas del período ya cubren todos los costos. */
    public boolean alcanzadoFGEM() {
        return ventasEquilibrioFGEM != null && ventasFGEM.compareTo(ventasEquilibrioFGEM) >= 0;
    }

    /** Ventas ÷ ventas de equilibrio (1.20 = 20 % por encima); {@code null} si no aplica. */
    public BigDecimal coberturaFGEM() {
        return ventasEquilibrioFGEM == null || ventasEquilibrioFGEM.signum() == 0
                ? null
                : ventasFGEM.divide(ventasEquilibrioFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
    }

    /** Ventas que faltan para llegar al equilibrio (cero si ya se alcanzó). */
    public BigDecimal ventasFaltantesFGEM() {
        return ventasEquilibrioFGEM == null ? null : ventasEquilibrioFGEM.subtract(ventasFGEM).max(BigDecimal.ZERO);
    }

    /** Costos variables ÷ ventas (para dibujar la recta de costos totales). */
    public BigDecimal razonCostoVariableFGEM() {
        return razonContribucionFGEM == null ? null : BigDecimal.ONE.subtract(razonContribucionFGEM);
    }
}
