package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Hecho que cambia las existencias del hato.
 *
 * @param categoriaDestino solo en los cambios de categoría
 * @param monto            valor total de la compra o venta; {@code null} si no se indicó
 */
public record MovimientoGanado(
        long id,
        long empresaId,
        LocalDate fecha,
        TipoMovimientoGanado tipo,
        CategoriaGanado categoria,
        int cantidad,
        CategoriaGanado categoriaDestino,
        BigDecimal monto,
        String causa,
        String observacion,
        boolean anulado,
        String motivoAnulacion,
        String registradoPor,
        LocalDateTime registradoEn) {

    public Especie especie() {
        return categoria.especie();
    }

    /** Variación de existencias que produce en la categoría indicada. */
    public int efectoEn(CategoriaGanado c) {
        if (anulado) {
            return 0;
        }
        if (tipo == TipoMovimientoGanado.CAMBIO_CATEGORIA) {
            return (c == categoriaDestino ? cantidad : 0) - (c == categoria ? cantidad : 0);
        }
        return c == categoria ? tipo.signo() * cantidad : 0;
    }
}
