package com.businessanalyzer.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.value.WritableValue;
import javafx.scene.Node;
import javafx.util.Duration;

public final class AnimacionesFGEM {

    private static volatile boolean reducidasFGEM;

    private AnimacionesFGEM() {
    }

    public static boolean reducidasFGEM() {
        return reducidasFGEM;
    }

    public static void reducirFGEM(boolean valorFGEM) {
        reducidasFGEM = valorFGEM;
    }

    public static <T> void haciaFGEM(WritableValue<T> valorFGEM, T destinoFGEM, Duration duracionFGEM, Duration retrasoFGEM) {
        if (reducidasFGEM) {
            valorFGEM.setValue(destinoFGEM);
            return;
        }
        Timeline tFGEM = new Timeline(new KeyFrame(duracionFGEM, new KeyValue(valorFGEM, destinoFGEM, Interpolator.EASE_OUT)));
        tFGEM.setDelay(retrasoFGEM);
        tFGEM.play();
    }

    public static void opacidadFGEM(Node nodoFGEM, double destinoFGEM, Duration duracionFGEM) {
        if (reducidasFGEM) {
            nodoFGEM.setOpacity(destinoFGEM);
            return;
        }
        FadeTransition fFGEM = new FadeTransition(duracionFGEM, nodoFGEM);
        fFGEM.setToValue(destinoFGEM);
        fFGEM.play();
    }

    public static void aparecerFGEM(Node nodoFGEM, double desplazamientoFGEM, Duration duracionFGEM) {
        if (reducidasFGEM) {
            nodoFGEM.setOpacity(1);
            nodoFGEM.setTranslateY(0);
            return;
        }
        nodoFGEM.setOpacity(0);
        nodoFGEM.setTranslateY(desplazamientoFGEM);
        FadeTransition fFGEM = new FadeTransition(duracionFGEM, nodoFGEM);
        fFGEM.setToValue(1);
        TranslateTransition tFGEM = new TranslateTransition(duracionFGEM, nodoFGEM);
        tFGEM.setToY(0);
        tFGEM.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fFGEM, tFGEM).play();
    }

    public static void repetirFGEM(Animation animacionFGEM) {
        if (!reducidasFGEM) {
            animacionFGEM.setCycleCount(Animation.INDEFINITE);
            animacionFGEM.play();
        }
    }
}
