package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Gasto de una campaña agrícola. */
public record CostoCampania(long id, long campaniaId, LocalDate fecha, RubroCosto rubro, String descripcion,
                            BigDecimal monto, boolean anulado, String registradoPor) {
}
