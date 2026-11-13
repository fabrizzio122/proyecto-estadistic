package com.businessanalyzer.exception;

/** Error al leer o escribir en la base de datos. Envuelve la {@link java.sql.SQLException} original. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String mensaje) {
        super(mensaje);
    }

    public DataAccessException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
