package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro de producción de leche, fibra o lana.
 *
 * @param diasFGEM           días que abarca el registro (1 = un día; 30 = total del mes)
 * @param animalesFGEM       vacas en ordeño o animales esquilados; {@code null} si no se indicó
 * @param precioUnitarioFGEM precio por litro o kilo; {@code null} si no se indicó
 */
public record ProduccionGanaderaFGEM(
        long idFGEM,
        long empresaIdFGEM,
        EspecieFGEM especieFGEM,
        ProductoGanaderoFGEM productoFGEM,
        LocalDate fechaFGEM,
        int diasFGEM,
        BigDecimal cantidadFGEM,
        Integer animalesFGEM,
        BigDecimal precioUnitarioFGEM,
        String observacionFGEM,
        boolean anuladoFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM) {

    /** Cantidad × precio; {@code null} si no hay precio. */
    public BigDecimal valorFGEM() {
        return precioUnitarioFGEM == null ? null : cantidadFGEM.multiply(precioUnitarioFGEM).setScale(2, RoundingMode.HALF_UP);
    }
}
