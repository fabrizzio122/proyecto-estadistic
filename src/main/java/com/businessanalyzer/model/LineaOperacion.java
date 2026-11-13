package com.businessanalyzer.model;

import java.math.BigDecimal;

/**
 * Línea de detalle de una venta o compra.
 *
 * @param costoUnitario en ventas, el costo del producto en el momento de vender (así el margen
 *                      histórico no cambia si luego cambia el costo); en compras, el precio pagado
 */
public record LineaOperacion(
        long id,
        long productoId,
        String codigo,
        String nombre,
        UnidadMedida unidad,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal costoUnitario,
        BigDecimal subtotal) {
}
