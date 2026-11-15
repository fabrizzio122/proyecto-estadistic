package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro de producción de leche, fibra o lana.
 *
 * @param dias           días que abarca el registro (1 = un día; 30 = total del mes)
 * @param animales       vacas en ordeño o animales esquilados; {@code null} si no se indicó
 * @param precioUnitario precio por litro o kilo; {@code null} si no se indicó
 */
public record ProduccionGanadera(
        long id,
        long empresaId,
        Especie especie,
        ProductoGanadero producto,
        LocalDate fecha,
        int dias,
        BigDecimal cantidad,
        Integer animales,
        BigDecimal precioUnitario,
        String observacion,
        boolean anulado,
        String registradoPor,
        LocalDateTime registradoEn) {

    /** Cantidad × precio; {@code null} si no hay precio. */
    public BigDecimal valor() {
        return precioUnitario == null ? null : cantidad.multiply(precioUnitario).setScale(2, RoundingMode.HALF_UP);
    }
}
