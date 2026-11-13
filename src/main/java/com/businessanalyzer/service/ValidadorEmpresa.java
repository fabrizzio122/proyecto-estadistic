package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosEmpresa;
import com.businessanalyzer.util.Validaciones;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

/** Valida y normaliza los datos de una empresa. */
final class ValidadorEmpresa {

    private static final Pattern IDENTIFICACION = Pattern.compile("[A-Z0-9-]{5,20}");
    private static final Pattern TELEFONO = Pattern.compile("[0-9+()\\- ]{6,30}");

    private ValidadorEmpresa() {
    }

    static DatosEmpresa normalizar(DatosEmpresa d, LocalDate hoy) {
        if (d == null) {
            throw new ValidacionException("Faltan los datos de la empresa.");
        }
        String razonSocial = Validaciones.requerido(d.razonSocial(), "Razón social", 3, 150);
        String nombreComercial = Validaciones.opcional(d.nombreComercial(), "Nombre comercial", 150);

        String identificacion = Validaciones.opcional(d.identificacionFiscal(), "RUC / Identificación fiscal", 20);
        if (identificacion != null) {
            identificacion = identificacion.toUpperCase(Locale.ROOT);
            if (!IDENTIFICACION.matcher(identificacion).matches()) {
                throw new ValidacionException("El RUC o identificación fiscal debe tener entre 5 y 20 caracteres: "
                        + "solo letras, números y guiones.");
            }
        }

        if (d.sector() == null) {
            throw new ValidacionException("Selecciona el sector de la empresa.");
        }
        if (d.tamano() == null) {
            throw new ValidacionException("Selecciona el tamaño de la empresa.");
        }
        if (d.moneda() == null) {
            throw new ValidacionException("Selecciona la moneda de la empresa.");
        }

        int mes = d.mesInicioEjercicio() == null ? 1 : d.mesInicioEjercicio();
        if (mes < 1 || mes > 12) {
            throw new ValidacionException("El mes de inicio del ejercicio fiscal debe estar entre 1 y 12.");
        }
        if (d.fechaInicioOperaciones() != null && d.fechaInicioOperaciones().isAfter(hoy)) {
            throw new ValidacionException("La fecha de inicio de operaciones no puede ser futura.");
        }

        String direccion = Validaciones.opcional(d.direccion(), "Dirección", 200);
        String telefono = Validaciones.opcional(d.telefono(), "Teléfono", 30);
        if (telefono != null && !TELEFONO.matcher(telefono).matches()) {
            throw new ValidacionException("El teléfono solo puede tener números, espacios y los signos + ( ) -.");
        }
        String email = Validaciones.email(d.email(), "Correo electrónico");

        return new DatosEmpresa(razonSocial, nombreComercial, identificacion, d.sector(), d.tamano(), d.moneda(),
                mes, d.fechaInicioOperaciones(), direccion, telefono, email);
    }
}
