package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos de una campaña tal como llegan desde el formulario. */
public record DatosCampaniaFGEM(Long parcelaIdFGEM, CultivoFGEM cultivoFGEM, String variedadFGEM, LocalDate fechaSiembraFGEM, BigDecimal areaFGEM,
                            BigDecimal rendimientoReferenciaFGEM, BigDecimal precioChacraFGEM, String observacionFGEM) {
}
