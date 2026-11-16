package com.businessanalyzer.ui;

import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.AutenticacionExceptionFGEM;
import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convierte una excepción en un mensaje para el usuario. Los errores de negocio se muestran
 * tal cual; los técnicos se registran en el log y se muestran con un texto genérico.
 */
public final class ErroresFGEM {

    private static final Logger logFGEM = LoggerFactory.getLogger(ErroresFGEM.class);

    private ErroresFGEM() {
    }

    public static String mensajeFGEM(Throwable errorFGEM) {
        if (errorFGEM instanceof ValidacionExceptionFGEM
                || errorFGEM instanceof AutenticacionExceptionFGEM
                || errorFGEM instanceof AccesoDenegadoExceptionFGEM) {
            return errorFGEM.getMessage();
        }
        if (errorFGEM instanceof com.businessanalyzer.exception.InformeExceptionFGEM) {
            logFGEM.error("Error al generar un informe", errorFGEM);
            return errorFGEM.getMessage();
        }
        if (errorFGEM instanceof DataAccessExceptionFGEM) {
            logFGEM.error("Error de base de datos", errorFGEM);
            return "No se pudo completar la operación en la base de datos. El detalle quedó registrado en el log.";
        }
        logFGEM.error("Error inesperado", errorFGEM);
        return "Ocurrió un error inesperado. El detalle quedó registrado en el log.";
    }
}
