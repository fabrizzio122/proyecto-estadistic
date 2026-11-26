package com.businessanalyzer.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmpresaFGEM(
        long idFGEM,
        String razonSocialFGEM,
        String nombreComercialFGEM,
        String identificacionFiscalFGEM,
        SectorFGEM sectorFGEM,
        TamanoEmpresaFGEM tamanoFGEM,
        MonedaFGEM monedaFGEM,
        int mesInicioEjercicioFGEM,
        LocalDate fechaInicioOperacionesFGEM,
        String direccionFGEM,
        String telefonoFGEM,
        String emailFGEM,
        boolean activaFGEM,
        LocalDateTime creadoEnFGEM,
        LocalDateTime actualizadoEnFGEM) {

    public String nombreVisibleFGEM() {
        return nombreComercialFGEM != null && !nombreComercialFGEM.isBlank() ? nombreComercialFGEM : razonSocialFGEM;
    }

    @Override
    public String toString() {
        return nombreVisibleFGEM();
    }
}
