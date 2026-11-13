package com.businessanalyzer.model;

import java.math.BigDecimal;

/**
 * Datos editables de un producto o servicio.
 *
 * @param codigo      vacío para que el sistema asigne uno (P-0001, S-0001...)
 * @param categoriaId {@code null} si no tiene categoría
 */
public record DatosProducto(
        TipoItem tipo,
        String codigo,
        String nombre,
        String descripcion,
        Long categoriaId,
        UnidadMedida unidad,
        BigDecimal costoUnitario,
        BigDecimal precioVenta) {

    public DatosProducto conCodigo(String nuevoCodigo) {
        return new DatosProducto(tipo, nuevoCodigo, nombre, descripcion, categoriaId, unidad,
                costoUnitario, precioVenta);
    }

    public DatosProducto conCategoria(Long nuevaCategoriaId) {
        return new DatosProducto(tipo, codigo, nombre, descripcion, nuevaCategoriaId, unidad,
                costoUnitario, precioVenta);
    }
}
