package com.businessanalyzer.exception;

/**
 * Dato ingresado que no cumple las reglas del negocio. Su mensaje esta pensado
 * para mostrarse directamente al usuario.
 */
public class ValidacionException extends RuntimeException {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
