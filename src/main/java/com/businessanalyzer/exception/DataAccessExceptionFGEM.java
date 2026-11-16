package com.businessanalyzer.exception;

/** Error al leer o escribir en la base de datos. Envuelve la {@link java.sql.SQLException} original. */
public class DataAccessExceptionFGEM extends RuntimeException {

    public DataAccessExceptionFGEM(String mensajeFGEM) {
        super(mensajeFGEM);
    }

    public DataAccessExceptionFGEM(String mensajeFGEM, Throwable causaFGEM) {
        super(mensajeFGEM, causaFGEM);
    }
}
