package com.businessanalyzer.model;

import java.math.BigDecimal;

/**
 * Línea de detalle de una venta o compra.
 *
 * @param costoUnitarioFGEM en ventas, el costo del producto en el momento de vender (así el margen
 *                      histórico no cambia si luego cambia el costo); en compras, el precio pagado
 */
public record LineaOperacionFGEM(
        long idFGEM,
        long productoIdFGEM,
        String codigoFGEM,
        String nombreFGEM,
        UnidadMedidaFGEM unidadFGEM,
        BigDecimal cantidadFGEM,
        BigDecimal precioUnitarioFGEM,
        BigDecimal costoUnitarioFGEM,
        BigDecimal subtotalFGEM) {
}
