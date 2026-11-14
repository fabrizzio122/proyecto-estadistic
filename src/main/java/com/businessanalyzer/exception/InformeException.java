package com.businessanalyzer.exception;

/** No se pudo generar o guardar un informe. El mensaje está pensado para mostrarse al usuario. */
public class InformeException extends RuntimeException {

    public InformeException(String mensaje) {
        super(mensaje);
    }

    public InformeException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
