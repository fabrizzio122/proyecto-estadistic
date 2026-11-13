package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Datos de una venta o compra nueva, tal como llegan desde el formulario.
 *
 * @param tasaImpuesto     porcentaje (18 = 18 %); 0 si no aplica
 * @param actualizarCostos solo en compras: usar los precios pagados como nuevo costo del catálogo
 */
public record DatosOperacion(
        TipoOperacion tipo,
        LocalDate fecha,
        String contraparte,
        String documentoContraparte,
        String comprobante,
        BigDecimal tasaImpuesto,
        String observaciones,
        List<DatosLinea> lineas,
        boolean actualizarCostos) {

    /** Una línea: qué producto, cuánto y a qué precio unitario (sin impuestos). */
    public record DatosLinea(long productoId, BigDecimal cantidad, BigDecimal precioUnitario) {
    }
}
