package com.businessanalyzer.exception;

/** No se pudo generar o guardar un informe. El mensaje está pensado para mostrarse al usuario. */
public class InformeExceptionFGEM extends RuntimeException {

    public InformeExceptionFGEM(String mensajeFGEM) {
        super(mensajeFGEM);
    }

    public InformeExceptionFGEM(String mensajeFGEM, Throwable causaFGEM) {
        super(mensajeFGEM, causaFGEM);
    }
}
