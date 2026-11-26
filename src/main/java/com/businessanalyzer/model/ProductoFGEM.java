package com.businessanalyzer.model;

import com.businessanalyzer.analytics.MargenesFGEM;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        LocalDateTime actualizadoEnFGEM,
        boolean perecibleFGEM) {

    public ProductoFGEM(long idFGEM, long empresaIdFGEM, Long categoriaIdFGEM, String categoriaNombreFGEM, TipoItemFGEM tipoFGEM,
                        String codigoFGEM, String nombreFGEM, String descripcionFGEM, UnidadMedidaFGEM unidadFGEM, BigDecimal costoUnitarioFGEM,
                        BigDecimal precioVentaFGEM, BigDecimal stockFGEM, boolean activoFGEM, LocalDateTime creadoEnFGEM, LocalDateTime actualizadoEnFGEM) {
        this(idFGEM, empresaIdFGEM, categoriaIdFGEM, categoriaNombreFGEM, tipoFGEM, codigoFGEM, nombreFGEM, descripcionFGEM, unidadFGEM,
                costoUnitarioFGEM, precioVentaFGEM, stockFGEM, activoFGEM, creadoEnFGEM, actualizadoEnFGEM, false);
    }

    public MargenesFGEM.MargenFGEM margenFGEM() {
        return MargenesFGEM.calcularFGEM(costoUnitarioFGEM, precioVentaFGEM);
    }

    @Override
    public String toString() {
        return codigoFGEM + " — " + nombreFGEM;
    }
}
