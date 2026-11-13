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
public final class Margenes {

    /** Decimales de los ratios (0.250000 = 25 %). */
    public static final int ESCALA_RATIO = 6;

    /**
     * @param unitario    precio − costo
     * @param sobrePrecio fracción del precio; {@code null} si el precio es cero
     * @param sobreCosto  fracción del costo; {@code null} si el costo es cero
     */
    public record Margen(BigDecimal unitario, BigDecimal sobrePrecio, BigDecimal sobreCosto) {

        public boolean negativo() {
            return unitario.signum() < 0;
        }
    }

    private Margenes() {
    }

    public static Margen calcular(BigDecimal costo, BigDecimal precio) {
        Objects.requireNonNull(costo, "costo");
        Objects.requireNonNull(precio, "precio");
        BigDecimal unitario = precio.subtract(costo);
        BigDecimal sobrePrecio = precio.signum() == 0
                ? null
                : unitario.divide(precio, ESCALA_RATIO, RoundingMode.HALF_UP);
        BigDecimal sobreCosto = costo.signum() == 0
                ? null
                : unitario.divide(costo, ESCALA_RATIO, RoundingMode.HALF_UP);
        return new Margen(unitario, sobrePrecio, sobreCosto);
    }
}
