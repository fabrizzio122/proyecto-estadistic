package com.businessanalyzer.model;

import java.math.BigDecimal;

/**
 * Datos editables de un producto o servicio.
 *
 * @param codigoFGEM      vacío para que el sistema asigne uno (P-0001, S-0001...)
 * @param categoriaIdFGEM {@code null} si no tiene categoría
 * @param perecibleFGEM   se vence pronto (pan, lácteos frescos, verduras)
 */
public record DatosProductoFGEM(
        TipoItemFGEM tipoFGEM,
        String codigoFGEM,
        String nombreFGEM,
        String descripcionFGEM,
        Long categoriaIdFGEM,
        UnidadMedidaFGEM unidadFGEM,
        BigDecimal costoUnitarioFGEM,
        BigDecimal precioVentaFGEM,
        boolean perecibleFGEM) {

    /** Producto no perecible. */
    public DatosProductoFGEM(TipoItemFGEM tipoFGEM, String codigoFGEM, String nombreFGEM, String descripcionFGEM, Long categoriaIdFGEM,
                             UnidadMedidaFGEM unidadFGEM, BigDecimal costoUnitarioFGEM, BigDecimal precioVentaFGEM) {
        this(tipoFGEM, codigoFGEM, nombreFGEM, descripcionFGEM, categoriaIdFGEM, unidadFGEM, costoUnitarioFGEM, precioVentaFGEM, false);
    }

    public DatosProductoFGEM conCodigoFGEM(String nuevoCodigoFGEM) {
        return new DatosProductoFGEM(tipoFGEM, nuevoCodigoFGEM, nombreFGEM, descripcionFGEM, categoriaIdFGEM, unidadFGEM,
                costoUnitarioFGEM, precioVentaFGEM, perecibleFGEM);
    }

    public DatosProductoFGEM conCategoriaFGEM(Long nuevaCategoriaIdFGEM) {
        return new DatosProductoFGEM(tipoFGEM, codigoFGEM, nombreFGEM, descripcionFGEM, nuevaCategoriaIdFGEM, unidadFGEM,
                costoUnitarioFGEM, precioVentaFGEM, perecibleFGEM);
    }
}
