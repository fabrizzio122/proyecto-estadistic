package com.businessanalyzer.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Comparación de un valor con el período anterior (análisis horizontal). */
public record VariacionFGEM(BigDecimal actualFGEM, BigDecimal anteriorFGEM) {

    public BigDecimal absolutaFGEM() {
        return actualFGEM.subtract(anteriorFGEM);
    }

    /**
     * Variación relativa sobre el valor anterior (0.25 = +25 %). Se usa el valor absoluto del
     * anterior para que pasar de −100 a −50 se lea como mejora. {@code null} si el anterior es cero.
     */
    public BigDecimal relativaFGEM() {
        return anteriorFGEM.signum() == 0
                ? null
                : absolutaFGEM().divide(anteriorFGEM.abs(), MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
    }
}
