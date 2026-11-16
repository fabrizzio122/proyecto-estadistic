package com.businessanalyzer.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notificaciones flotantes (esquina superior derecha) que desaparecen solas.
 * Se pueden llamar desde cualquier hilo y desde cualquier controlador.
 * <pre>{@code Notificador.exito("Empresa registrada correctamente");}</pre>
 */
public final class NotificadorFGEM {

    private enum TipoFGEM {
        EXITO("toast-exito", "fth-check-circle"),
        INFO("toast-info", "fth-info"),
        ADVERTENCIA("toast-advertencia", "fth-alert-triangle"),
        ERROR("toast-error", "fth-x-circle");

        private final String estiloFGEM;
        private final String iconoFGEM;

        TipoFGEM(String estiloFGEM, String iconoFGEM) {
            this.estiloFGEM = estiloFGEM;
            this.iconoFGEM = iconoFGEM;
        }
    }

    private static final Logger logFGEM = LoggerFactory.getLogger(NotificadorFGEM.class);
    private static final int MAXIMO_VISIBLES_FGEM = 4;
    private static final Duration DURACION_VISIBLE_FGEM = Duration.seconds(3.5);
    private static final Duration DURACION_FUNDIDO_FGEM = Duration.millis(250);

    private static VBox contenedorFGEM;

    private NotificadorFGEM() {
    }

    /** Lo llama la ventana principal al construirse. */
    public static void instalarFGEM(VBox contenedorNotificacionesFGEM) {
        contenedorFGEM = contenedorNotificacionesFGEM;
    }

    public static void exitoFGEM(String mensajeFGEM) {
        mostrarFGEM(TipoFGEM.EXITO, mensajeFGEM);
    }

    public static void infoFGEM(String mensajeFGEM) {
        mostrarFGEM(TipoFGEM.INFO, mensajeFGEM);
    }

    public static void advertenciaFGEM(String mensajeFGEM) {
        mostrarFGEM(TipoFGEM.ADVERTENCIA, mensajeFGEM);
    }

    public static void errorFGEM(String mensajeFGEM) {
        mostrarFGEM(TipoFGEM.ERROR, mensajeFGEM);
    }

    private static void mostrarFGEM(TipoFGEM tipoFGEM, String mensajeFGEM) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> mostrarFGEM(tipoFGEM, mensajeFGEM));
            return;
        }
        if (contenedorFGEM == null) {
            logFGEM.warn("Notificacion sin ventana principal: {}", mensajeFGEM);
            return;
        }

        Label textoFGEM = new Label(mensajeFGEM);
        textoFGEM.setWrapText(true);
        HBox toastFGEM = new HBox(10, new FontIcon(tipoFGEM.iconoFGEM), textoFGEM);
        toastFGEM.getStyleClass().addAll("toast", tipoFGEM.estiloFGEM);
        toastFGEM.setAlignment(Pos.CENTER_LEFT);
        toastFGEM.setMaxWidth(380);
        toastFGEM.setOpacity(0);

        if (contenedorFGEM.getChildren().size() >= MAXIMO_VISIBLES_FGEM) {
            contenedorFGEM.getChildren().remove(0);
        }
        contenedorFGEM.getChildren().add(toastFGEM);

        FadeTransition entradaFGEM = new FadeTransition(DURACION_FUNDIDO_FGEM, toastFGEM);
        entradaFGEM.setToValue(1);
        FadeTransition salidaFGEM = new FadeTransition(DURACION_FUNDIDO_FGEM, toastFGEM);
        salidaFGEM.setToValue(0);
        SequentialTransition animacionFGEM = new SequentialTransition(
                entradaFGEM, new PauseTransition(DURACION_VISIBLE_FGEM), salidaFGEM);
        animacionFGEM.setOnFinished(eFGEM -> contenedorFGEM.getChildren().remove(toastFGEM));
        toastFGEM.setOnMouseClicked(eFGEM -> {
            animacionFGEM.stop();
            contenedorFGEM.getChildren().remove(toastFGEM);
        });
        animacionFGEM.play();
    }
}
