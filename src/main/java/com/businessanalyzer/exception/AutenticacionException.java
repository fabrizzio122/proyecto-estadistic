package com.businessanalyzer.exception;

/** No se pudo iniciar sesión. El mensaje se muestra tal cual en la pantalla de acceso. */
public class AutenticacionException extends RuntimeException {

    public AutenticacionException(String mensaje) {
        super(mensaje);
    }
}
