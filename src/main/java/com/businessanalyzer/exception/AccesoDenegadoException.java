package com.businessanalyzer.exception;

/** El usuario de la sesión no tiene permiso para la operación solicitada. */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
