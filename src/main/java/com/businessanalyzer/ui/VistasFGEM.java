package com.businessanalyzer.ui;

import com.businessanalyzer.config.AppContextFGEM;
import javafx.fxml.FXMLLoader;

import java.net.URL;

/**
 * Carga archivos FXML creando sus controladores con las dependencias de la aplicacion.
 * <p>
 * El controlador puede declarar uno de estos constructores (se prueban en este orden):
 * {@code (AppContext, Ventana)}, {@code (AppContext)} o sin parametros.
 */
public final class VistasFGEM {

    public static final String PRINCIPAL_FGEM = "/fxml/main-view.fxml";
    public static final String ACCESO_FGEM = "/fxml/acceso-view.fxml";
    public static final String CONFIGURACION_INICIAL_FGEM = "/fxml/configuracion-inicial-view.fxml";

    private VistasFGEM() {
    }

    public static FXMLLoader cargadorFGEM(String rutaFGEM, AppContextFGEM contextoFGEM) {
        return cargadorFGEM(rutaFGEM, contextoFGEM, null);
    }

    public static FXMLLoader cargadorFGEM(String rutaFGEM, AppContextFGEM contextoFGEM, VentanaFGEM ventanaFGEM) {
        URL urlFGEM = VistasFGEM.class.getResource(rutaFGEM);
        if (urlFGEM == null) {
            throw new IllegalArgumentException("No existe la vista: " + rutaFGEM);
        }
        FXMLLoader loaderFGEM = new FXMLLoader(urlFGEM);
        loaderFGEM.setControllerFactory(tipoFGEM -> crearControladorFGEM(tipoFGEM, contextoFGEM, ventanaFGEM));
        return loaderFGEM;
    }

    private static Object crearControladorFGEM(Class<?> tipoFGEM, AppContextFGEM contextoFGEM, VentanaFGEM ventanaFGEM) {
        try {
            if (ventanaFGEM != null) {
                try {
                    return tipoFGEM.getConstructor(AppContextFGEM.class, VentanaFGEM.class).newInstance(contextoFGEM, ventanaFGEM);
                } catch (NoSuchMethodException ignoradaFGEM) {
                    // se prueba el siguiente constructor
                }
            }
            try {
                return tipoFGEM.getConstructor(AppContextFGEM.class).newInstance(contextoFGEM);
            } catch (NoSuchMethodException sinContextoFGEM) {
                return tipoFGEM.getConstructor().newInstance();
            }
        } catch (ReflectiveOperationException eFGEM) {
            throw new IllegalStateException("No se pudo crear el controlador " + tipoFGEM.getName(), eFGEM);
        }
    }
}
