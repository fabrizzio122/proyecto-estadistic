package com.businessanalyzer.model;

import java.time.LocalDate;

/** Datos editables de una empresa, tal como llegan desde el formulario. */
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
