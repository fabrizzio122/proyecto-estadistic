package com.businessanalyzer.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class MargenesFGEM {

    public static final int ESCALA_RATIO_FGEM = 6;

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
