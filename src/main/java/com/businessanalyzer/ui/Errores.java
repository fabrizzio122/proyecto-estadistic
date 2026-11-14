package com.businessanalyzer.ui;

import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.AutenticacionException;
import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convierte una excepción en un mensaje para el usuario. Los errores de negocio se muestran
 * tal cual; los técnicos se registran en el log y se muestran con un texto genérico.
 */
public final class Errores {

    private static final Logger log = LoggerFactory.getLogger(Errores.class);

    private Errores() {
    }

    public static String mensaje(Throwable error) {
        if (error instanceof ValidacionException
                || error instanceof AutenticacionException
                || error instanceof AccesoDenegadoException) {
            return error.getMessage();
        }
        if (error instanceof com.businessanalyzer.exception.InformeException) {
            log.error("Error al generar un informe", error);
            return error.getMessage();
        }
        if (error instanceof DataAccessException) {
            log.error("Error de base de datos", error);
            return "No se pudo completar la operación en la base de datos. El detalle quedó registrado en el log.";
        }
        log.error("Error inesperado", error);
        return "Ocurrió un error inesperado. El detalle quedó registrado en el log.";
    }
}
