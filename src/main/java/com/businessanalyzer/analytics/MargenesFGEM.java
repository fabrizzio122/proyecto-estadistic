package com.businessanalyzer.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Cálculo de márgenes de un producto o servicio.
 * <ul>
 *     <li><b>Margen unitario</b> = precio − costo.</li>
 *     <li><b>Margen sobre el precio</b> = (precio − costo) / precio. Es la parte de cada venta
 *         que queda como ganancia bruta (también llamado margen bruto).</li>
 *     <li><b>Margen sobre el costo</b> (markup) = (precio − costo) / costo. Es cuánto se
 *         recarga sobre el costo.</li>
 * </ul>
 * Ejemplo: costo 75, precio 100 → margen 25, 25 % sobre el precio y 33.3 % sobre el costo.
 */
public final class MargenesFGEM {

    /** Decimales de los ratios (0.250000 = 25 %). */
    public static final int ESCALA_RATIO_FGEM = 6;

    /**
     * @param unitarioFGEM    precio − costo
     * @param sobrePrecioFGEM fracción del precio; {@code null} si el precio es cero
     * @param sobreCostoFGEM  fracción del costo; {@code null} si el costo es cero
     */
    public record MargenFGEM(BigDecimal unitarioFGEM, BigDecimal sobrePrecioFGEM, BigDecimal sobreCostoFGEM) {

        public boolean negativoFGEM() {
            return unitarioFGEM.signum() < 0;
        }
    }

    private MargenesFGEM() {
    }

    public static MargenFGEM calcularFGEM(BigDecimal costoFGEM, BigDecimal precioFGEM) {
        Objects.requireNonNull(costoFGEM, "costo");
        Objects.requireNonNull(precioFGEM, "precio");
        BigDecimal unitarioFGEM = precioFGEM.subtract(costoFGEM);
        BigDecimal sobrePrecioFGEM = precioFGEM.signum() == 0
                ? null
                : unitarioFGEM.divide(precioFGEM, ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
        BigDecimal sobreCostoFGEM = costoFGEM.signum() == 0
                ? null
                : unitarioFGEM.divide(costoFGEM, ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
        return new MargenFGEM(unitarioFGEM, sobrePrecioFGEM, sobreCostoFGEM);
    }
}
