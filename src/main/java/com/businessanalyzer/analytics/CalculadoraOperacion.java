package com.businessanalyzer.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Totales de una venta o compra.
 * <ul>
 *     <li>Subtotal de línea = cantidad × precio unitario, redondeado a 2 decimales.</li>
 *     <li>Subtotal = suma de los subtotales de línea (sin impuestos).</li>
 *     <li>Impuesto = subtotal × tasa ÷ 100, redondeado a 2 decimales.</li>
 *     <li>Total = subtotal + impuesto.</li>
 *     <li>Costo total = suma de cantidad × costo unitario (para el margen bruto de las ventas).</li>
 * </ul>
 * Redondeo comercial (mitad hacia arriba).
 */
public final class CalculadoraOperacion {

    private static final int DECIMALES_MONEDA = 2;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public record Linea(BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal costoUnitario) {
    }

    public record Totales(BigDecimal subtotal, BigDecimal impuesto, BigDecimal total, BigDecimal costoTotal) {

        public BigDecimal margenBruto() {
            return subtotal.subtract(costoTotal);
        }
    }

    private CalculadoraOperacion() {
    }

    public static BigDecimal subtotalLinea(BigDecimal cantidad, BigDecimal precioUnitario) {
        return cantidad.multiply(precioUnitario).setScale(DECIMALES_MONEDA, RoundingMode.HALF_UP);
    }

    public static Totales calcular(List<Linea> lineas, BigDecimal tasaPorcentaje) {
        BigDecimal subtotal = BigDecimal.ZERO.setScale(DECIMALES_MONEDA);
        BigDecimal costo = BigDecimal.ZERO.setScale(DECIMALES_MONEDA);
        for (Linea linea : lineas) {
            subtotal = subtotal.add(subtotalLinea(linea.cantidad(), linea.precioUnitario()));
            costo = costo.add(subtotalLinea(linea.cantidad(), linea.costoUnitario()));
        }
        BigDecimal impuesto = subtotal.multiply(tasaPorcentaje)
                .divide(CIEN, DECIMALES_MONEDA, RoundingMode.HALF_UP);
        return new Totales(subtotal, impuesto, subtotal.add(impuesto), costo);
    }
}
