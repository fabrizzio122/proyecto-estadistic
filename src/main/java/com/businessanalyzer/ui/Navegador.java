package com.businessanalyzer.ui;

import com.businessanalyzer.config.AppContext;
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
public final class Navegador {

    private record VistaCargada(Parent raiz, Object controlador) {
    }

    private final StackPane contenedor;
    private final AppContext contexto;
    private final Map<Modulo, VistaCargada> cargadas = new EnumMap<>(Modulo.class);

    public Navegador(StackPane contenedor, AppContext contexto) {
        this.contenedor = contenedor;
        this.contexto = contexto;
    }

    public void mostrar(Modulo modulo) throws IOException {
        VistaCargada vista = cargadas.get(modulo);
        if (vista == null) {
            vista = cargar(modulo);
            cargadas.put(modulo, vista);
        }
        contenedor.getChildren().setAll(vista.raiz());
        if (vista.controlador() instanceof VistaModulo vm) {
            vm.alMostrar();
        }
    }

    private VistaCargada cargar(Modulo modulo) throws IOException {
        FXMLLoader loader = Vistas.cargador(modulo.vista(), contexto);
        Parent raiz = loader.load();
        Object controlador = loader.getController();
        if (controlador instanceof VistaModulo vm) {
            vm.inicializar(modulo);
        }
        return new VistaCargada(raiz, controlador);
    }
}
