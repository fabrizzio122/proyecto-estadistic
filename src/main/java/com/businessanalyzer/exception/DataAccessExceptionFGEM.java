package com.businessanalyzer.exception;

public class DataAccessExceptionFGEM extends RuntimeException {

    public DataAccessExceptionFGEM(String mensajeFGEM) {
        super(mensajeFGEM);
    }

    public DataAccessExceptionFGEM(String mensajeFGEM, Throwable causaFGEM) {
        super(mensajeFGEM, causaFGEM);
    }
}
