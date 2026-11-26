package com.businessanalyzer.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record VariacionFGEM(BigDecimal actualFGEM, BigDecimal anteriorFGEM) {

    public BigDecimal absolutaFGEM() {
        return actualFGEM.subtract(anteriorFGEM);
    }

    public BigDecimal relativaFGEM() {
        return anteriorFGEM.signum() == 0
                ? null
                : absolutaFGEM().divide(anteriorFGEM.abs(), MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
    }
}
