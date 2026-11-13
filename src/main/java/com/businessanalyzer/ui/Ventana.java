package com.businessanalyzer.ui;

import com.businessanalyzer.config.AppContext;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Ventana única de la aplicación. Alterna entre las pantallas de acceso y la principal
 * sobre la misma escena; encima de todas queda la capa de notificaciones flotantes.
 */
public final class Ventana {

    private final Stage stage;
    private final AppContext contexto;
    private final StackPane capaContenido = new StackPane();

    public Ventana(Stage stage, AppContext contexto) {
        this.stage = stage;
        this.contexto = contexto;

        VBox capaNotificaciones = new VBox();
        capaNotificaciones.getStyleClass().add("toast-container");
        capaNotificaciones.setPickOnBounds(false);
        capaNotificaciones.setMaxWidth(420);
        capaNotificaciones.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(capaNotificaciones, Pos.TOP_RIGHT);
        Notificador.instalar(capaNotificaciones);

        StackPane raiz = new StackPane(capaContenido, capaNotificaciones);
        raiz.getStyleClass().add("app-root");
        Scene escena = new Scene(raiz, 1280, 800);
        Estilos.aplicar(escena);
        stage.setScene(escena);
    }

    public Stage stage() {
        return stage;
    }

    /** Muestra la creación del primer administrador o, si ya hay usuarios, el inicio de sesión. */
    public void mostrarAcceso() {
        mostrar(contexto.autenticacion().requiereConfiguracionInicial()
                ? Vistas.CONFIGURACION_INICIAL
                : Vistas.ACCESO);
    }

    public void mostrarPrincipal() {
        mostrar(Vistas.PRINCIPAL);
    }

    private void mostrar(String ruta) {
        try {
            Parent vista = Vistas.cargador(ruta, contexto, this).load();
            capaContenido.getChildren().setAll(vista);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la pantalla " + ruta, e);
        }
    }
}
