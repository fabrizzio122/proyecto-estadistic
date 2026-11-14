package com.businessanalyzer.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Comparación de un valor con el período anterior (análisis horizontal). */
public record Variacion(BigDecimal actual, BigDecimal anterior) {

    public BigDecimal absoluta() {
        return actual.subtract(anterior);
    }

    /**
     * Variación relativa sobre el valor anterior (0.25 = +25 %). Se usa el valor absoluto del
     * anterior para que pasar de −100 a −50 se lea como mejora. {@code null} si el anterior es cero.
     */
    public BigDecimal relativa() {
        return anterior.signum() == 0
                ? null
                : absoluta().divide(anterior.abs(), Margenes.ESCALA_RATIO, RoundingMode.HALF_UP);
    }
}
