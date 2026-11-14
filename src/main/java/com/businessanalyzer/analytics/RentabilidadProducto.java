package com.businessanalyzer.analytics;

import com.businessanalyzer.model.TipoItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cuánto vendió y cuánto ganó (margen bruto) cada producto o servicio en un período.
 * El costo es el que tenía el producto en el momento de cada venta.
 */
public record RentabilidadProducto(
        long productoId,
        String codigo,
        String nombre,
        TipoItem tipo,
        String categoria,
        BigDecimal cantidad,
        BigDecimal ventas,
        BigDecimal costo) {

    /** Una línea de venta registrada (dato de entrada). */
    public record Venta(long productoId, String codigo, String nombre, TipoItem tipo, String categoria,
                        BigDecimal cantidad, BigDecimal subtotal, BigDecimal costoUnitario) {
    }

    public BigDecimal margen() {
        return ventas.subtract(costo);
    }

    /** Margen ÷ ventas del producto; {@code null} si no hubo ventas. */
    public BigDecimal margenSobreVentas() {
        return ventas.signum() == 0 ? null : margen().divide(ventas, Margenes.ESCALA_RATIO, RoundingMode.HALF_UP);
    }

    /** Agrupa las líneas por producto y ordena de mayor a menor margen. */
    public static List<RentabilidadProducto> agrupar(List<Venta> ventas) {
        Map<Long, RentabilidadProducto> porProducto = new LinkedHashMap<>();
        for (Venta v : ventas) {
            BigDecimal costoLinea = CalculadoraOperacion.subtotalLinea(v.cantidad(), v.costoUnitario());
            porProducto.merge(v.productoId(),
                    new RentabilidadProducto(v.productoId(), v.codigo(), v.nombre(), v.tipo(), v.categoria(),
                            v.cantidad(), v.subtotal(), costoLinea),
                    (a, b) -> new RentabilidadProducto(a.productoId, a.codigo, a.nombre, a.tipo, a.categoria,
                            a.cantidad.add(b.cantidad), a.ventas.add(b.ventas), a.costo.add(b.costo)));
        }
        List<RentabilidadProducto> resultado = new ArrayList<>(porProducto.values());
        resultado.sort(Comparator.comparing(RentabilidadProducto::margen).reversed()
                .thenComparing(RentabilidadProducto::nombre));
        return resultado;
    }
}
