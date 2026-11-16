package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un cultivo sembrado en una parcela, desde la siembra hasta la cosecha.
 *
 * @param areaFGEM                  hectáreas sembradas
 * @param rendimientoReferenciaFGEM kg/ha con que se compara; {@code null} si no hay referencia
 * @param precioChacraFGEM          S/ por kg para valorizar lo que no se vende; {@code null} si no se indicó
 * @param fechaCierreFGEM           fecha de la cosecha final o de la pérdida; {@code null} en curso
 * @param motivoFGEM                causa de la pérdida o de la anulación
 */
public record CampaniaFGEM(long idFGEM, long empresaIdFGEM, long parcelaIdFGEM, String parcelaNombreFGEM, CultivoFGEM cultivoFGEM,
                       String variedadFGEM, LocalDate fechaSiembraFGEM, BigDecimal areaFGEM, BigDecimal rendimientoReferenciaFGEM,
                       BigDecimal precioChacraFGEM, EstadoCampaniaFGEM estadoFGEM, LocalDate fechaCierreFGEM, String motivoFGEM,
                       String observacionFGEM, String registradoPorFGEM) {

    /** "Papa (Yungay) · Parcela Alta · 2025". */
    public String nombreFGEM() {
        return cultivoFGEM.descripcionFGEM() + (variedadFGEM == null ? "" : " (" + variedadFGEM + ")") + " · " + parcelaNombreFGEM
                + " · " + fechaSiembraFGEM.getYear();
    }
}
