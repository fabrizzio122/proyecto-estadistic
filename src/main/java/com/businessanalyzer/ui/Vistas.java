package com.businessanalyzer.ui;

import com.businessanalyzer.config.AppContext;
import javafx.fxml.FXMLLoader;

import java.net.URL;

/**
 * Carga archivos FXML creando sus controladores con las dependencias de la aplicacion.
 * <p>
 * Si el controlador tiene un constructor {@code (AppContext)} se usa ese; si no, el
 * constructor sin parametros.
 */
public final class Vistas {

    public static final String PRINCIPAL = "/fxml/main-view.fxml";

    private Vistas() {
    }

    public static FXMLLoader cargador(String ruta, AppContext contexto) {
        URL url = Vistas.class.getResource(ruta);
        if (url == null) {
            throw new IllegalArgumentException("No existe la vista: " + ruta);
        }
        FXMLLoader loader = new FXMLLoader(url);
        loader.setControllerFactory(tipo -> crearControlador(tipo, contexto));
        return loader;
    }

    private static Object crearControlador(Class<?> tipo, AppContext contexto) {
        try {
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
