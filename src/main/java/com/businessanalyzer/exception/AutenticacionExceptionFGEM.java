package com.businessanalyzer.exception;

/** No se pudo iniciar sesión. El mensaje se muestra tal cual en la pantalla de acceso. */
public class AutenticacionExceptionFGEM extends RuntimeException {

    public AutenticacionExceptionFGEM(String mensajeFGEM) {
        super(mensajeFGEM);
    }
}
