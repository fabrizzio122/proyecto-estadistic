package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DatosOperacionFGEM(
        TipoOperacionFGEM tipoFGEM,
        LocalDate fechaFGEM,
        String contraparteFGEM,
        String documentoContraparteFGEM,
        String comprobanteFGEM,
        BigDecimal tasaImpuestoFGEM,
        String observacionesFGEM,
        List<DatosLineaFGEM> lineasFGEM,
        boolean actualizarCostosFGEM,
        CondicionPagoFGEM condicionFGEM,
        Long cuentaIdFGEM,
        LocalDate vencimientoFGEM) {

    public DatosOperacionFGEM(TipoOperacionFGEM tipoFGEM, LocalDate fechaFGEM, String contraparteFGEM, String documentoContraparteFGEM,
                          String comprobanteFGEM, BigDecimal tasaImpuestoFGEM, String observacionesFGEM, List<DatosLineaFGEM> lineasFGEM,
                          boolean actualizarCostosFGEM) {
        this(tipoFGEM, fechaFGEM, contraparteFGEM, documentoContraparteFGEM, comprobanteFGEM, tasaImpuestoFGEM, observacionesFGEM, lineasFGEM,
                actualizarCostosFGEM, CondicionPagoFGEM.CONTADO, null, null);
    }

    public record DatosLineaFGEM(long productoIdFGEM, BigDecimal cantidadFGEM, BigDecimal precioUnitarioFGEM) {
    }
}
