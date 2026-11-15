package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Parte de la producción cosechada con su destino.
 *
 * @param cantidad kilos
 * @param precio   S/ por kg de la venta; {@code null} si no es venta o no se indicó
 */
public record Cosecha(long id, long campaniaId, LocalDate fecha, BigDecimal cantidad, DestinoCosecha destino,
                      BigDecimal precio, String observacion, boolean anulado, String registradoPor) {
}
