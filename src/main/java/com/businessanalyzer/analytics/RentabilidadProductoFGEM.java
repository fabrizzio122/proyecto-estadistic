package com.businessanalyzer.analytics;

import com.businessanalyzer.model.TipoItemFGEM;

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
public record RentabilidadProductoFGEM(
        long productoIdFGEM,
        String codigoFGEM,
        String nombreFGEM,
        TipoItemFGEM tipoFGEM,
        String categoriaFGEM,
        BigDecimal cantidadFGEM,
        BigDecimal ventasFGEM,
        BigDecimal costoFGEM) {

    /** Una línea de venta registrada (dato de entrada). */
    public record VentaFGEM(long productoIdFGEM, String codigoFGEM, String nombreFGEM, TipoItemFGEM tipoFGEM, String categoriaFGEM,
                        BigDecimal cantidadFGEM, BigDecimal subtotalFGEM, BigDecimal costoUnitarioFGEM) {
    }

    public BigDecimal margenFGEM() {
        return ventasFGEM.subtract(costoFGEM);
    }

    /** Margen ÷ ventas del producto; {@code null} si no hubo ventas. */
    public BigDecimal margenSobreVentasFGEM() {
        return ventasFGEM.signum() == 0 ? null : margenFGEM().divide(ventasFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
    }

    /** Agrupa las líneas por producto y ordena de mayor a menor margen. */
    public static List<RentabilidadProductoFGEM> agruparFGEM(List<VentaFGEM> ventasFGEM) {
        Map<Long, RentabilidadProductoFGEM> porProductoFGEM = new LinkedHashMap<>();
        for (VentaFGEM vFGEM : ventasFGEM) {
            BigDecimal costoLineaFGEM = CalculadoraOperacionFGEM.subtotalLineaFGEM(vFGEM.cantidadFGEM(), vFGEM.costoUnitarioFGEM());
            porProductoFGEM.merge(vFGEM.productoIdFGEM(),
                    new RentabilidadProductoFGEM(vFGEM.productoIdFGEM(), vFGEM.codigoFGEM(), vFGEM.nombreFGEM(), vFGEM.tipoFGEM(), vFGEM.categoriaFGEM(),
                            vFGEM.cantidadFGEM(), vFGEM.subtotalFGEM(), costoLineaFGEM),
                    (aFGEM, bFGEM) -> new RentabilidadProductoFGEM(aFGEM.productoIdFGEM, aFGEM.codigoFGEM, aFGEM.nombreFGEM, aFGEM.tipoFGEM, aFGEM.categoriaFGEM,
                            aFGEM.cantidadFGEM.add(bFGEM.cantidadFGEM), aFGEM.ventasFGEM.add(bFGEM.ventasFGEM), aFGEM.costoFGEM.add(bFGEM.costoFGEM)));
        }
        List<RentabilidadProductoFGEM> resultadoFGEM = new ArrayList<>(porProductoFGEM.values());
        resultadoFGEM.sort(Comparator.comparing(RentabilidadProductoFGEM::margenFGEM).reversed()
                .thenComparing(RentabilidadProductoFGEM::nombreFGEM));
        return resultadoFGEM;
    }
}
