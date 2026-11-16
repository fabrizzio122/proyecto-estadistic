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
public final class LamparaFGEM extends Group {

    private static final Duration TRANSICION_FGEM = Duration.millis(550);

    private final BooleanProperty encendidaFGEM = new SimpleBooleanProperty(this, "encendida", true);
    private final Circle focoFGEM;
    private final Circle haloFGEM;

    /** @param cordonFGEM largo del cordón en píxeles (desde el borde superior del panel) */
    public LamparaFGEM(double cordonFGEM) {
        Line lineaFGEM = new Line(0, 0, 0, cordonFGEM);
        lineaFGEM.getStyleClass().add("lampara-cordon");

        Polygon pantallaFGEM = new Polygon(-13, cordonFGEM, 13, cordonFGEM, 27, cordonFGEM + 30, -27, cordonFGEM + 30);
        pantallaFGEM.getStyleClass().add("lampara-pantalla");

        double centroFocoFGEM = cordonFGEM + 38;
        haloFGEM = new Circle(0, centroFocoFGEM, 30);
        haloFGEM.getStyleClass().add("lampara-halo");
        haloFGEM.setEffect(new GaussianBlur(26));
        haloFGEM.setMouseTransparent(true);

        Circle focoApagadoFGEM = new Circle(0, centroFocoFGEM, 12);
        focoApagadoFGEM.getStyleClass().add("lampara-foco-apagado");
        focoFGEM = new Circle(0, centroFocoFGEM, 12);
        focoFGEM.getStyleClass().add("lampara-foco");

        Line cadenaFGEM = new Line(20, cordonFGEM + 30, 20, cordonFGEM + 56);
        cadenaFGEM.getStyleClass().add("lampara-cordon");
        Circle tiradorFGEM = new Circle(20, cordonFGEM + 59, 3.5);
        tiradorFGEM.getStyleClass().add("lampara-tirador");

        getChildren().addAll(lineaFGEM, haloFGEM, pantallaFGEM, focoApagadoFGEM, focoFGEM, cadenaFGEM, tiradorFGEM);

        Rotate giroFGEM = new Rotate(0, 0, 0);
        getTransforms().add(giroFGEM);
        Timeline vaivenFGEM = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(giroFGEM.angleProperty(), -2.5, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(3), new KeyValue(giroFGEM.angleProperty(), 2.5, Interpolator.EASE_BOTH)));
        vaivenFGEM.setAutoReverse(true);
        AnimacionesFGEM.repetirFGEM(vaivenFGEM);

        setCursor(Cursor.HAND);
        setFocusTraversable(true);
        Tooltip.install(this, new Tooltip("Encender o apagar la luz"));
        setOnMouseClicked(eFGEM -> alternarFGEM());
        setOnKeyPressed(eFGEM -> {
            if (eFGEM.getCode() == KeyCode.ENTER || eFGEM.getCode() == KeyCode.SPACE) {
                alternarFGEM();
            }
        });
        encendidaFGEM.addListener((oFGEM, antesFGEM, ahoraFGEM) -> {
            AnimacionesFGEM.opacidadFGEM(focoFGEM, ahoraFGEM ? 1 : 0, TRANSICION_FGEM);
            AnimacionesFGEM.opacidadFGEM(haloFGEM, ahoraFGEM ? 0.85 : 0, TRANSICION_FGEM);
        });
        haloFGEM.setOpacity(0.85);
    }

    public void alternarFGEM() {
        encendidaFGEM.set(!encendidaFGEM.get());
    }

    public void encenderFGEM() {
        encendidaFGEM.set(true);
    }

    public BooleanProperty encendidaPropertyFGEM() {
        return encendidaFGEM;
    }

    public boolean isEncendidaFGEM() {
        return encendidaFGEM.get();
    }
}
