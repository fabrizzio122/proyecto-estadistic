package com.businessanalyzer.exception;

public class InformeExceptionFGEM extends RuntimeException {

    public InformeExceptionFGEM(String mensajeFGEM) {
        super(mensajeFGEM);
    }

    public InformeExceptionFGEM(String mensajeFGEM, Throwable causaFGEM) {
        super(mensajeFGEM, causaFGEM);
    }
}
