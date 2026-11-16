package com.businessanalyzer.exception;

/**
 * Dato ingresado que no cumple las reglas del negocio. Su mensaje esta pensado
 * para mostrarse directamente al usuario.
 */
public class ValidacionExceptionFGEM extends RuntimeException {

    public ValidacionExceptionFGEM(String mensajeFGEM) {
        super(mensajeFGEM);
    }
}
