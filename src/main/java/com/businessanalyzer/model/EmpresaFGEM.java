package com.businessanalyzer.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Empresa o negocio que se analiza.
 *
 * @param mesInicioEjercicioFGEM mes (1-12) en que empieza su año fiscal; los análisis anuales lo usan
 * @param activaFGEM             {@code false} si está archivada (no aparece en el selector)
 */
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

    /** Nombre comercial si existe; si no, la razón social. */
    public String nombreVisibleFGEM() {
        return nombreComercialFGEM != null && !nombreComercialFGEM.isBlank() ? nombreComercialFGEM : razonSocialFGEM;
    }

    @Override
    public String toString() {
        return nombreVisibleFGEM();
    }
}
