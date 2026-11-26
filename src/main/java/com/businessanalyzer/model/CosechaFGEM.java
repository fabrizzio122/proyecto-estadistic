package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CosechaFGEM(long idFGEM, long campaniaIdFGEM, LocalDate fechaFGEM, BigDecimal cantidadFGEM, DestinoCosechaFGEM destinoFGEM,
                      BigDecimal precioFGEM, String observacionFGEM, boolean anuladoFGEM, String registradoPorFGEM) {
}
