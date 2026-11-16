package com.businessanalyzer.model;

import com.businessanalyzer.analytics.MargenesFGEM;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Producto o servicio del catálogo de una empresa.
 *
 * @param costoUnitarioFGEM costo de adquirir o producir una unidad (costo variable unitario)
 * @param precioVentaFGEM   precio de venta de una unidad, sin impuestos
 * @param stockFGEM         unidades disponibles (compras − ventas registradas); {@code null} en servicios
 */
public record ProductoFGEM(
        long idFGEM,
        long empresaIdFGEM,
        Long categoriaIdFGEM,
        String categoriaNombreFGEM,
        TipoItemFGEM tipoFGEM,
        String codigoFGEM,
        String nombreFGEM,
        String descripcionFGEM,
        UnidadMedidaFGEM unidadFGEM,
        BigDecimal costoUnitarioFGEM,
        BigDecimal precioVentaFGEM,
        BigDecimal stockFGEM,
        boolean activoFGEM,
        LocalDateTime creadoEnFGEM,
        LocalDateTime actualizadoEnFGEM) {

    public MargenesFGEM.MargenFGEM margenFGEM() {
        return MargenesFGEM.calcularFGEM(costoUnitarioFGEM, precioVentaFGEM);
    }

    @Override
    public String toString() {
        return codigoFGEM + " — " + nombreFGEM;
    }
}
