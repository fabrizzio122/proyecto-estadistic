package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Venta o compra registrada.
 *
 * @param tasaImpuesto porcentaje aplicado (18 = 18 %)
 * @param subtotal     suma de las líneas, sin impuestos
 * @param costoTotal   costo de lo vendido (en compras coincide con el subtotal)
 * @param cantidadItems número de líneas
 * @param lineas       detalle; vacío cuando se obtiene desde un listado
 */
public record Operacion(
        long id,
        long empresaId,
        TipoOperacion tipo,
        String numero,
        LocalDate fecha,
        String contraparte,
        String documentoContraparte,
        String comprobante,
        BigDecimal tasaImpuesto,
        BigDecimal subtotal,
        BigDecimal impuesto,
        BigDecimal total,
        BigDecimal costoTotal,
        EstadoOperacion estado,
        String observaciones,
        String registradoPor,
        LocalDateTime registradoEn,
        String motivoAnulacion,
        String anuladoPor,
        LocalDateTime anuladoEn,
        int cantidadItems,
        List<LineaOperacion> lineas) {

    public Operacion {
        lineas = lineas == null ? List.of() : List.copyOf(lineas);
    }

    public boolean anulada() {
        return estado == EstadoOperacion.ANULADA;
    }

    /** Ganancia bruta de una venta: lo vendido sin impuestos menos su costo. */
    public BigDecimal margenBruto() {
        return subtotal.subtract(costoTotal);
    }
}
