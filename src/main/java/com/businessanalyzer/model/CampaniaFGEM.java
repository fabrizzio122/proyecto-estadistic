package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CampaniaFGEM(long idFGEM, long empresaIdFGEM, long parcelaIdFGEM, String parcelaNombreFGEM, CultivoFGEM cultivoFGEM,
                       String variedadFGEM, LocalDate fechaSiembraFGEM, BigDecimal areaFGEM, BigDecimal rendimientoReferenciaFGEM,
                       BigDecimal precioChacraFGEM, EstadoCampaniaFGEM estadoFGEM, LocalDate fechaCierreFGEM, String motivoFGEM,
                       String observacionFGEM, String registradoPorFGEM) {

    public String nombreFGEM() {
        return cultivoFGEM.descripcionFGEM() + (variedadFGEM == null ? "" : " (" + variedadFGEM + ")") + " · " + parcelaNombreFGEM
                + " · " + fechaSiembraFGEM.getYear();
    }
}
