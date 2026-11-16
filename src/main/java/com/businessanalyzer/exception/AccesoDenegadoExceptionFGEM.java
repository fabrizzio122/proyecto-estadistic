package com.businessanalyzer.exception;

/** El usuario de la sesión no tiene permiso para la operación solicitada. */
public class AccesoDenegadoExceptionFGEM extends RuntimeException {

    public AccesoDenegadoExceptionFGEM(String mensajeFGEM) {
        super(mensajeFGEM);
    }
}
