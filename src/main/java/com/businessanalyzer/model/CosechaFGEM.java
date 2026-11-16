package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Parte de la producción cosechada con su destino.
 *
 * @param cantidadFGEM kilos
 * @param precioFGEM   S/ por kg de la venta; {@code null} si no es venta o no se indicó
 */
public record CosechaFGEM(long idFGEM, long campaniaIdFGEM, LocalDate fechaFGEM, BigDecimal cantidadFGEM, DestinoCosechaFGEM destinoFGEM,
                      BigDecimal precioFGEM, String observacionFGEM, boolean anuladoFGEM, String registradoPorFGEM) {
}
