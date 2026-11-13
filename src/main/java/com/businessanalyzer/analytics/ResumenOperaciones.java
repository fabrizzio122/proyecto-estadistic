package com.businessanalyzer.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Totales de ventas y compras registradas en un período (sin impuestos, sin anuladas).
 *
 * @param ventasNetas  suma de subtotales de venta
 * @param costoVentas  costo de lo vendido
 * @param comprasNetas suma de subtotales de compra
 */
public record ResumenOperaciones(BigDecimal ventasNetas, BigDecimal costoVentas, BigDecimal comprasNetas,
                                 int cantidadVentas, int cantidadCompras) {

    public static final ResumenOperaciones VACIO =
            new ResumenOperaciones(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);

    /** Ventas netas − costo de lo vendido. */
    public BigDecimal margenBruto() {
        return ventasNetas.subtract(costoVentas);
    }

    /** Margen bruto ÷ ventas netas; {@code null} si no hubo ventas. */
    public BigDecimal margenBrutoSobreVentas() {
        return ventasNetas.signum() == 0
                ? null
                : margenBruto().divide(ventasNetas, Margenes.ESCALA_RATIO, RoundingMode.HALF_UP);
    }

    /** Venta promedio por operación; {@code null} si no hubo ventas. */
    public BigDecimal ticketPromedio() {
        return cantidadVentas == 0
                ? null
                : ventasNetas.divide(BigDecimal.valueOf(cantidadVentas), 2, RoundingMode.HALF_UP);
    }
}
