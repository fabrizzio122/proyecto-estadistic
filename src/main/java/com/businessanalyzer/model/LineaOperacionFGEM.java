package com.businessanalyzer.model;

import java.math.BigDecimal;

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
