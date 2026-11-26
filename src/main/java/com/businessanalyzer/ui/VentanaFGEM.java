package com.businessanalyzer.ui;

import com.businessanalyzer.config.AppContextFGEM;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class VentanaFGEM {

    private final Stage stageFGEM;
    private final AppContextFGEM contextoFGEM;
    private final StackPane capaContenidoFGEM = new StackPane();

    public VentanaFGEM(Stage stageFGEM, AppContextFGEM contextoFGEM) {
        this.stageFGEM = stageFGEM;
        this.contextoFGEM = contextoFGEM;

        VBox capaNotificacionesFGEM = new VBox();
        capaNotificacionesFGEM.getStyleClass().add("toast-container");
        capaNotificacionesFGEM.setPickOnBounds(false);
        capaNotificacionesFGEM.setMaxWidth(420);
        capaNotificacionesFGEM.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(capaNotificacionesFGEM, Pos.TOP_RIGHT);
        NotificadorFGEM.instalarFGEM(capaNotificacionesFGEM);

        StackPane raizFGEM = new StackPane(capaContenidoFGEM, capaNotificacionesFGEM);
        raizFGEM.getStyleClass().add("app-root");
        Scene escenaFGEM = new Scene(raizFGEM, 1280, 800);
        EstilosFGEM.aplicarFGEM(escenaFGEM);
        stageFGEM.setScene(escenaFGEM);
    }

    public Stage stageFGEM() {
        return stageFGEM;
    }

    public static void aplicarIconoFGEM(Stage stageFGEM) {
        for (int tamanoFGEM : new int[]{16, 32, 48, 256}) {
            var recursoFGEM = VentanaFGEM.class.getResource("/icono/icono-" + tamanoFGEM + ".png");
            if (recursoFGEM != null) {
                stageFGEM.getIcons().add(new javafx.scene.image.Image(recursoFGEM.toExternalForm()));
            }
        }
    }

    public void mostrarAccesoFGEM() {
        mostrarFGEM(contextoFGEM.autenticacionFGEM().requiereConfiguracionInicialFGEM()
                ? VistasFGEM.CONFIGURACION_INICIAL_FGEM
                : VistasFGEM.ACCESO_FGEM);
    }

    public void mostrarPrincipalFGEM() {
        mostrarFGEM(VistasFGEM.PRINCIPAL_FGEM);
    }

    private void mostrarFGEM(String rutaFGEM) {
        try {
            Parent vistaFGEM = VistasFGEM.cargadorFGEM(rutaFGEM, contextoFGEM, this).load();
            capaContenidoFGEM.getChildren().setAll(vistaFGEM);
        } catch (IOException eFGEM) {
            throw new UncheckedIOException("No se pudo cargar la pantalla " + rutaFGEM, eFGEM);
        }
    }
}
