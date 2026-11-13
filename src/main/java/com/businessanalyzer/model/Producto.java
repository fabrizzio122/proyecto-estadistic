package com.businessanalyzer.model;

import com.businessanalyzer.analytics.Margenes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Producto o servicio del catálogo de una empresa.
 *
 * @param costoUnitario costo de adquirir o producir una unidad (costo variable unitario)
 * @param precioVenta   precio de venta de una unidad, sin impuestos
 * @param stock         unidades disponibles (compras − ventas registradas); {@code null} en servicios
 */
public record Producto(
        long id,
        long empresaId,
        Long categoriaId,
        String categoriaNombre,
        TipoItem tipo,
        String codigo,
        String nombre,
        String descripcion,
        UnidadMedida unidad,
        BigDecimal costoUnitario,
        BigDecimal precioVenta,
        BigDecimal stock,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn) {

    public Margenes.Margen margen() {
        return Margenes.calcular(costoUnitario, precioVenta);
    }

    @Override
    public String toString() {
        return codigo + " — " + nombre;
    }
}
