package com.businessanalyzer.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * Lámpara colgante del inicio de sesión: cordón, pantalla, foco y tirador, dibujados con formas de
 * JavaFX. Se mece ±2,5° en un ciclo de 6 s (rota sobre el punto de anclaje, arriba del cordón) y al
 * hacer clic se enciende o se apaga. El foco y su halo toman el color de acento por CSS.
 */
public final class Lampara extends Group {

    private static final Duration TRANSICION = Duration.millis(550);

    private final BooleanProperty encendida = new SimpleBooleanProperty(this, "encendida", true);
    private final Circle foco;
    private final Circle halo;

    /** @param cordon largo del cordón en píxeles (desde el borde superior del panel) */
    public Lampara(double cordon) {
        Line linea = new Line(0, 0, 0, cordon);
        linea.getStyleClass().add("lampara-cordon");

        Polygon pantalla = new Polygon(-13, cordon, 13, cordon, 27, cordon + 30, -27, cordon + 30);
        pantalla.getStyleClass().add("lampara-pantalla");

        double centroFoco = cordon + 38;
        halo = new Circle(0, centroFoco, 30);
        halo.getStyleClass().add("lampara-halo");
        halo.setEffect(new GaussianBlur(26));
        halo.setMouseTransparent(true);

        Circle focoApagado = new Circle(0, centroFoco, 12);
        focoApagado.getStyleClass().add("lampara-foco-apagado");
        foco = new Circle(0, centroFoco, 12);
        foco.getStyleClass().add("lampara-foco");

        Line cadena = new Line(20, cordon + 30, 20, cordon + 56);
        cadena.getStyleClass().add("lampara-cordon");
        Circle tirador = new Circle(20, cordon + 59, 3.5);
        tirador.getStyleClass().add("lampara-tirador");

        getChildren().addAll(linea, halo, pantalla, focoApagado, foco, cadena, tirador);

        Rotate giro = new Rotate(0, 0, 0);
        getTransforms().add(giro);
        Timeline vaiven = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(giro.angleProperty(), -2.5, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(3), new KeyValue(giro.angleProperty(), 2.5, Interpolator.EASE_BOTH)));
        vaiven.setAutoReverse(true);
        Animaciones.repetir(vaiven);

        setCursor(Cursor.HAND);
        setFocusTraversable(true);
        Tooltip.install(this, new Tooltip("Encender o apagar la luz"));
        setOnMouseClicked(e -> alternar());
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                alternar();
            }
        });
        encendida.addListener((o, antes, ahora) -> {
            Animaciones.opacidad(foco, ahora ? 1 : 0, TRANSICION);
            Animaciones.opacidad(halo, ahora ? 0.85 : 0, TRANSICION);
        });
        halo.setOpacity(0.85);
    }

    public void alternar() {
        encendida.set(!encendida.get());
    }

    public void encender() {
        encendida.set(true);
    }

    public BooleanProperty encendidaProperty() {
        return encendida;
    }

    public boolean isEncendida() {
        return encendida.get();
    }
}
