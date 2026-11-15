package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos de una campaña tal como llegan desde el formulario. */
public record DatosCampania(Long parcelaId, Cultivo cultivo, String variedad, LocalDate fechaSiembra, BigDecimal area,
                            BigDecimal rendimientoReferencia, BigDecimal precioChacra, String observacion) {
}
