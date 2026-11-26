package com.businessanalyzer.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ResumenOperacionesFGEM(BigDecimal ventasNetasFGEM, BigDecimal costoVentasFGEM, BigDecimal comprasNetasFGEM,
                                 int cantidadVentasFGEM, int cantidadComprasFGEM) {

    public static final ResumenOperacionesFGEM VACIO_FGEM =
            new ResumenOperacionesFGEM(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);

    public BigDecimal margenBrutoFGEM() {
        return ventasNetasFGEM.subtract(costoVentasFGEM);
    }

    public BigDecimal margenBrutoSobreVentasFGEM() {
        return ventasNetasFGEM.signum() == 0
                ? null
                : margenBrutoFGEM().divide(ventasNetasFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
    }

    public BigDecimal ticketPromedioFGEM() {
        return cantidadVentasFGEM == 0
                ? null
                : ventasNetasFGEM.divide(BigDecimal.valueOf(cantidadVentasFGEM), 2, RoundingMode.HALF_UP);
    }
}
