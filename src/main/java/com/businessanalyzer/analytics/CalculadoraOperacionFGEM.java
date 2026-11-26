package com.businessanalyzer.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class CalculadoraOperacionFGEM {

    private static final int DECIMALES_MONEDA_FGEM = 2;
    private static final BigDecimal CIEN_FGEM = BigDecimal.valueOf(100);

    public record LineaFGEM(BigDecimal cantidadFGEM, BigDecimal precioUnitarioFGEM, BigDecimal costoUnitarioFGEM) {
    }

    public record TotalesFGEM(BigDecimal subtotalFGEM, BigDecimal impuestoFGEM, BigDecimal totalFGEM, BigDecimal costoTotalFGEM) {

        public BigDecimal margenBrutoFGEM() {
            return subtotalFGEM.subtract(costoTotalFGEM);
        }
    }

    private CalculadoraOperacionFGEM() {
    }

    public static BigDecimal subtotalLineaFGEM(BigDecimal cantidadFGEM, BigDecimal precioUnitarioFGEM) {
        return cantidadFGEM.multiply(precioUnitarioFGEM).setScale(DECIMALES_MONEDA_FGEM, RoundingMode.HALF_UP);
    }

    public static TotalesFGEM calcularFGEM(List<LineaFGEM> lineasFGEM, BigDecimal tasaPorcentajeFGEM) {
        BigDecimal subtotalFGEM = BigDecimal.ZERO.setScale(DECIMALES_MONEDA_FGEM);
        BigDecimal costoFGEM = BigDecimal.ZERO.setScale(DECIMALES_MONEDA_FGEM);
        for (LineaFGEM lineaFGEM : lineasFGEM) {
            subtotalFGEM = subtotalFGEM.add(subtotalLineaFGEM(lineaFGEM.cantidadFGEM(), lineaFGEM.precioUnitarioFGEM()));
            costoFGEM = costoFGEM.add(subtotalLineaFGEM(lineaFGEM.cantidadFGEM(), lineaFGEM.costoUnitarioFGEM()));
        }
        BigDecimal impuestoFGEM = subtotalFGEM.multiply(tasaPorcentajeFGEM)
                .divide(CIEN_FGEM, DECIMALES_MONEDA_FGEM, RoundingMode.HALF_UP);
        return new TotalesFGEM(subtotalFGEM, impuestoFGEM, subtotalFGEM.add(impuestoFGEM), costoFGEM);
    }
}
