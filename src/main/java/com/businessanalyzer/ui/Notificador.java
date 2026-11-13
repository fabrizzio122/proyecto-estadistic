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
public final class Notificador {

    private enum Tipo {
        EXITO("toast-exito", "fth-check-circle"),
        INFO("toast-info", "fth-info"),
        ADVERTENCIA("toast-advertencia", "fth-alert-triangle"),
        ERROR("toast-error", "fth-x-circle");

        private final String estilo;
        private final String icono;

        Tipo(String estilo, String icono) {
            this.estilo = estilo;
            this.icono = icono;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(Notificador.class);
    private static final int MAXIMO_VISIBLES = 4;
    private static final Duration DURACION_VISIBLE = Duration.seconds(3.5);
    private static final Duration DURACION_FUNDIDO = Duration.millis(250);

    private static VBox contenedor;

    private Notificador() {
    }

    /** Lo llama la ventana principal al construirse. */
    public static void instalar(VBox contenedorNotificaciones) {
        contenedor = contenedorNotificaciones;
    }

    public static void exito(String mensaje) {
        mostrar(Tipo.EXITO, mensaje);
    }

    public static void info(String mensaje) {
        mostrar(Tipo.INFO, mensaje);
    }

    public static void advertencia(String mensaje) {
        mostrar(Tipo.ADVERTENCIA, mensaje);
    }

    public static void error(String mensaje) {
        mostrar(Tipo.ERROR, mensaje);
    }

    private static void mostrar(Tipo tipo, String mensaje) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> mostrar(tipo, mensaje));
            return;
        }
        if (contenedor == null) {
            log.warn("Notificacion sin ventana principal: {}", mensaje);
            return;
        }

        Label texto = new Label(mensaje);
        texto.setWrapText(true);
        HBox toast = new HBox(10, new FontIcon(tipo.icono), texto);
        toast.getStyleClass().addAll("toast", tipo.estilo);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setMaxWidth(380);
        toast.setOpacity(0);

        if (contenedor.getChildren().size() >= MAXIMO_VISIBLES) {
            contenedor.getChildren().remove(0);
        }
        contenedor.getChildren().add(toast);

        FadeTransition entrada = new FadeTransition(DURACION_FUNDIDO, toast);
        entrada.setToValue(1);
        FadeTransition salida = new FadeTransition(DURACION_FUNDIDO, toast);
        salida.setToValue(0);
        SequentialTransition animacion = new SequentialTransition(
                entrada, new PauseTransition(DURACION_VISIBLE), salida);
        animacion.setOnFinished(e -> contenedor.getChildren().remove(toast));
        toast.setOnMouseClicked(e -> {
            animacion.stop();
            contenedor.getChildren().remove(toast);
        });
        animacion.play();
    }
}
