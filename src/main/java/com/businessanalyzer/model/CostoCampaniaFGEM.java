package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CostoCampaniaFGEM(long idFGEM, long campaniaIdFGEM, LocalDate fechaFGEM, RubroCostoFGEM rubroFGEM, String descripcionFGEM,
                            BigDecimal montoFGEM, boolean anuladoFGEM, String registradoPorFGEM) {
}
