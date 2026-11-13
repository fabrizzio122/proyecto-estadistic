package com.businessanalyzer.ui;

import com.businessanalyzer.config.AppContext;
import javafx.fxml.FXMLLoader;

import java.net.URL;

/**
 * Carga archivos FXML creando sus controladores con las dependencias de la aplicacion.
 * <p>
 * El controlador puede declarar uno de estos constructores (se prueban en este orden):
 * {@code (AppContext, Ventana)}, {@code (AppContext)} o sin parametros.
 */
public final class Vistas {

    public static final String PRINCIPAL = "/fxml/main-view.fxml";
    public static final String ACCESO = "/fxml/acceso-view.fxml";
    public static final String CONFIGURACION_INICIAL = "/fxml/configuracion-inicial-view.fxml";

    private Vistas() {
    }

    public static FXMLLoader cargador(String ruta, AppContext contexto) {
        return cargador(ruta, contexto, null);
    }

    public static FXMLLoader cargador(String ruta, AppContext contexto, Ventana ventana) {
        URL url = Vistas.class.getResource(ruta);
        if (url == null) {
            throw new IllegalArgumentException("No existe la vista: " + ruta);
        }
        FXMLLoader loader = new FXMLLoader(url);
        loader.setControllerFactory(tipo -> crearControlador(tipo, contexto, ventana));
        return loader;
    }

    private static Object crearControlador(Class<?> tipo, AppContext contexto, Ventana ventana) {
        try {
            if (ventana != null) {
                try {
                    return tipo.getConstructor(AppContext.class, Ventana.class).newInstance(contexto, ventana);
                } catch (NoSuchMethodException ignorada) {
                    // se prueba el siguiente constructor
                }
            }
            try {
                return tipo.getConstructor(AppContext.class).newInstance(contexto);
            } catch (NoSuchMethodException sinContexto) {
                return tipo.getConstructor().newInstance();
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No se pudo crear el controlador " + tipo.getName(), e);
        }
    }
}
