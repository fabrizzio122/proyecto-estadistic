package com.businessanalyzer.ui;

import com.businessanalyzer.config.AppContextFGEM;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Muestra la vista de cada modulo en el area central. Cada vista se carga una sola vez
 * y se reutiliza, de modo que conserva su estado al cambiar de modulo.
 */
public final class NavegadorFGEM {

    private record VistaCargadaFGEM(Parent raizFGEM, Object controladorFGEM) {
    }

    private final StackPane contenedorFGEM;
    private final AppContextFGEM contextoFGEM;
    private final Map<ModuloFGEM, VistaCargadaFGEM> cargadasFGEM = new EnumMap<>(ModuloFGEM.class);

    public NavegadorFGEM(StackPane contenedorFGEM, AppContextFGEM contextoFGEM) {
        this.contenedorFGEM = contenedorFGEM;
        this.contextoFGEM = contextoFGEM;
    }

    public void mostrarFGEM(ModuloFGEM moduloFGEM) throws IOException {
        VistaCargadaFGEM vistaFGEM = cargadasFGEM.get(moduloFGEM);
        if (vistaFGEM == null) {
            vistaFGEM = cargarFGEM(moduloFGEM);
            cargadasFGEM.put(moduloFGEM, vistaFGEM);
        }
        contenedorFGEM.getChildren().setAll(vistaFGEM.raizFGEM());
        if (vistaFGEM.controladorFGEM() instanceof VistaModuloFGEM vmFGEM) {
            vmFGEM.alMostrarFGEM();
        }
    }

    /**
     * Tras cambiar la empresa activa: descarta las vistas en memoria (se recargarán con los
     * datos de la nueva empresa cuando se abran) y refresca la que está a la vista.
     */
    public void alCambiarEmpresaFGEM(ModuloFGEM visibleFGEM) {
        cargadasFGEM.keySet().removeIf(moduloFGEM -> moduloFGEM != visibleFGEM);
        VistaCargadaFGEM vistaFGEM = cargadasFGEM.get(visibleFGEM);
        if (vistaFGEM != null && vistaFGEM.controladorFGEM() instanceof VistaModuloFGEM vmFGEM) {
            vmFGEM.alMostrarFGEM();
        }
    }

    private VistaCargadaFGEM cargarFGEM(ModuloFGEM moduloFGEM) throws IOException {
        FXMLLoader loaderFGEM = VistasFGEM.cargadorFGEM(moduloFGEM.vistaFGEM(), contextoFGEM);
        Parent raizFGEM = loaderFGEM.load();
        Object controladorFGEM = loaderFGEM.getController();
        if (controladorFGEM instanceof VistaModuloFGEM vmFGEM) {
            vmFGEM.inicializarFGEM(moduloFGEM);
        }
        return new VistaCargadaFGEM(raizFGEM, controladorFGEM);
    }
}
