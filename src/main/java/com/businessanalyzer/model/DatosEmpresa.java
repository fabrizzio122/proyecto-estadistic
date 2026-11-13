package com.businessanalyzer.model;

import java.time.LocalDate;

/** Datos editables de una empresa, tal como llegan desde el formulario. */
public record DatosEmpresa(
        String razonSocial,
        String nombreComercial,
        String identificacionFiscal,
        Sector sector,
        TamanoEmpresa tamano,
        Moneda moneda,
        Integer mesInicioEjercicio,
        LocalDate fechaInicioOperaciones,
        String direccion,
        String telefono,
        String email) {
}
