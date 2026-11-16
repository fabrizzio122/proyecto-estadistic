package com.businessanalyzer.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Totales de ventas y compras registradas en un período (sin impuestos, sin anuladas).
 *
 * @param ventasNetasFGEM  suma de subtotales de venta
 * @param costoVentasFGEM  costo de lo vendido
 * @param comprasNetasFGEM suma de subtotales de compra
 */
public record ResumenOperacionesFGEM(BigDecimal ventasNetasFGEM, BigDecimal costoVentasFGEM, BigDecimal comprasNetasFGEM,
                                 int cantidadVentasFGEM, int cantidadComprasFGEM) {

    public static final ResumenOperacionesFGEM VACIO_FGEM =
            new ResumenOperacionesFGEM(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);

    /** Ventas netas − costo de lo vendido. */
    public BigDecimal margenBrutoFGEM() {
        return ventasNetasFGEM.subtract(costoVentasFGEM);
    }

    /** Margen bruto ÷ ventas netas; {@code null} si no hubo ventas. */
    public BigDecimal margenBrutoSobreVentasFGEM() {
        return ventasNetasFGEM.signum() == 0
                ? null
                : margenBrutoFGEM().divide(ventasNetasFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
    }

    /** Venta promedio por operación; {@code null} si no hubo ventas. */
    public BigDecimal ticketPromedioFGEM() {
        return cantidadVentasFGEM == 0
                ? null
                : ventasNetasFGEM.divide(BigDecimal.valueOf(cantidadVentasFGEM), 2, RoundingMode.HALF_UP);
    }
}
