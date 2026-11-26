package com.businessanalyzer.model;

import java.math.BigDecimal;

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
