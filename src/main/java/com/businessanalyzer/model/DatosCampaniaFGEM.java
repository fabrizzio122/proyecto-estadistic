package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DatosCampaniaFGEM(Long parcelaIdFGEM, CultivoFGEM cultivoFGEM, String variedadFGEM, LocalDate fechaSiembraFGEM, BigDecimal areaFGEM,
                            BigDecimal rendimientoReferenciaFGEM, BigDecimal precioChacraFGEM, String observacionFGEM) {
}
