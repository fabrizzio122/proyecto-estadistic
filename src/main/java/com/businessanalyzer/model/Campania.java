package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un cultivo sembrado en una parcela, desde la siembra hasta la cosecha.
 *
 * @param area                  hectáreas sembradas
 * @param rendimientoReferencia kg/ha con que se compara; {@code null} si no hay referencia
 * @param precioChacra          S/ por kg para valorizar lo que no se vende; {@code null} si no se indicó
 * @param fechaCierre           fecha de la cosecha final o de la pérdida; {@code null} en curso
 * @param motivo                causa de la pérdida o de la anulación
 */
public record Campania(long id, long empresaId, long parcelaId, String parcelaNombre, Cultivo cultivo,
                       String variedad, LocalDate fechaSiembra, BigDecimal area, BigDecimal rendimientoReferencia,
                       BigDecimal precioChacra, EstadoCampania estado, LocalDate fechaCierre, String motivo,
                       String observacion, String registradoPor) {

    /** "Papa (Yungay) · Parcela Alta · 2025". */
    public String nombre() {
        return cultivo.descripcion() + (variedad == null ? "" : " (" + variedad + ")") + " · " + parcelaNombre
                + " · " + fechaSiembra.getYear();
    }
}
