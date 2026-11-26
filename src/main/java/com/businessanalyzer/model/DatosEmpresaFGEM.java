package com.businessanalyzer.model;

import java.time.LocalDate;

public record DatosEmpresaFGEM(
        String razonSocialFGEM,
        String nombreComercialFGEM,
        String identificacionFiscalFGEM,
        SectorFGEM sectorFGEM,
        TamanoEmpresaFGEM tamanoFGEM,
        MonedaFGEM monedaFGEM,
        Integer mesInicioEjercicioFGEM,
        LocalDate fechaInicioOperacionesFGEM,
        String direccionFGEM,
        String telefonoFGEM,
        String emailFGEM) {
}
