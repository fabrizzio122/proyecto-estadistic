package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosEmpresaFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

final class ValidadorEmpresaFGEM {

    private static final Pattern IDENTIFICACION_FGEM = Pattern.compile("[A-Z0-9-]{5,20}");
    private static final Pattern TELEFONO_FGEM = Pattern.compile("[0-9+()\\- ]{6,30}");

    private ValidadorEmpresaFGEM() {
    }

    static DatosEmpresaFGEM normalizarFGEM(DatosEmpresaFGEM dFGEM, LocalDate hoyFGEM) {
        if (dFGEM == null) {
            throw new ValidacionExceptionFGEM("Faltan los datos de la empresa.");
        }
        String razonSocialFGEM = ValidacionesFGEM.requeridoFGEM(dFGEM.razonSocialFGEM(), "Razón social", 3, 150);
        String nombreComercialFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.nombreComercialFGEM(), "Nombre comercial", 150);

        String identificacionFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.identificacionFiscalFGEM(), "RUC / Identificación fiscal", 20);
        if (identificacionFGEM != null) {
            identificacionFGEM = identificacionFGEM.toUpperCase(Locale.ROOT);
            if (!IDENTIFICACION_FGEM.matcher(identificacionFGEM).matches()) {
                throw new ValidacionExceptionFGEM("El RUC o identificación fiscal debe tener entre 5 y 20 caracteres: "
                        + "solo letras, números y guiones.");
            }
        }

        if (dFGEM.sectorFGEM() == null) {
            throw new ValidacionExceptionFGEM("Selecciona el sector de la empresa.");
        }
        if (dFGEM.tamanoFGEM() == null) {
            throw new ValidacionExceptionFGEM("Selecciona el tamaño de la empresa.");
        }
        if (dFGEM.monedaFGEM() == null) {
            throw new ValidacionExceptionFGEM("Selecciona la moneda de la empresa.");
        }

        int mesFGEM = dFGEM.mesInicioEjercicioFGEM() == null ? 1 : dFGEM.mesInicioEjercicioFGEM();
        if (mesFGEM < 1 || mesFGEM > 12) {
            throw new ValidacionExceptionFGEM("El mes de inicio del ejercicio fiscal debe estar entre 1 y 12.");
        }
        if (dFGEM.fechaInicioOperacionesFGEM() != null && dFGEM.fechaInicioOperacionesFGEM().isAfter(hoyFGEM)) {
            throw new ValidacionExceptionFGEM("La fecha de inicio de operaciones no puede ser futura.");
        }

        String direccionFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.direccionFGEM(), "Dirección", 200);
        String telefonoFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.telefonoFGEM(), "Teléfono", 30);
        if (telefonoFGEM != null && !TELEFONO_FGEM.matcher(telefonoFGEM).matches()) {
            throw new ValidacionExceptionFGEM("El teléfono solo puede tener números, espacios y los signos + ( ) -.");
        }
        String emailFGEM = ValidacionesFGEM.emailFGEM(dFGEM.emailFGEM(), "Correo electrónico");

        return new DatosEmpresaFGEM(razonSocialFGEM, nombreComercialFGEM, identificacionFGEM, dFGEM.sectorFGEM(), dFGEM.tamanoFGEM(), dFGEM.monedaFGEM(),
                mesFGEM, dFGEM.fechaInicioOperacionesFGEM(), direccionFGEM, telefonoFGEM, emailFGEM);
    }
}
