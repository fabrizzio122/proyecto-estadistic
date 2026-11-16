package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Hecho que cambia las existencias del hato.
 *
 * @param categoriaDestinoFGEM solo en los cambios de categoría
 * @param montoFGEM            valor total de la compra o venta; {@code null} si no se indicó
 */
public record MovimientoGanadoFGEM(
        long idFGEM,
        long empresaIdFGEM,
        LocalDate fechaFGEM,
        TipoMovimientoGanadoFGEM tipoFGEM,
        CategoriaGanadoFGEM categoriaFGEM,
        int cantidadFGEM,
        CategoriaGanadoFGEM categoriaDestinoFGEM,
        BigDecimal montoFGEM,
        String causaFGEM,
        String observacionFGEM,
        boolean anuladoFGEM,
        String motivoAnulacionFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM) {

    public EspecieFGEM especieFGEM() {
        return categoriaFGEM.especieFGEM();
    }

    /** Variación de existencias que produce en la categoría indicada. */
    public int efectoEnFGEM(CategoriaGanadoFGEM cFGEM) {
        if (anuladoFGEM) {
            return 0;
        }
        if (tipoFGEM == TipoMovimientoGanadoFGEM.CAMBIO_CATEGORIA) {
            return (cFGEM == categoriaDestinoFGEM ? cantidadFGEM : 0) - (cFGEM == categoriaFGEM ? cantidadFGEM : 0);
        }
        return cFGEM == categoriaFGEM ? tipoFGEM.signoFGEM() * cantidadFGEM : 0;
    }
}
