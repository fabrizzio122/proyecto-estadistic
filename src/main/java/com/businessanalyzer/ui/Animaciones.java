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

/**
 * Animaciones de la interfaz. Solo hay animaciones de entrada y cambios de estado cortos (0,2 a 0,8 s);
 * con «Reducir animaciones» activado (Configuración) todo se muestra en su estado final al instante.
 * JavaFX no lee esa opción de Windows, por eso la aplicación tiene la suya.
 */
public final class Animaciones {

    private static volatile boolean reducidas;

    private Animaciones() {
    }

    public static boolean reducidas() {
        return reducidas;
    }

    public static void reducir(boolean valor) {
        reducidas = valor;
    }

    /** Lleva un valor hasta su destino en el tiempo indicado (o de inmediato, si se reducen las animaciones). */
    public static <T> void hacia(WritableValue<T> valor, T destino, Duration duracion, Duration retraso) {
        if (reducidas) {
            valor.setValue(destino);
            return;
        }
        Timeline t = new Timeline(new KeyFrame(duracion, new KeyValue(valor, destino, Interpolator.EASE_OUT)));
        t.setDelay(retraso);
        t.play();
    }

    /** Cambia la opacidad de un nodo. */
    public static void opacidad(Node nodo, double destino, Duration duracion) {
        if (reducidas) {
            nodo.setOpacity(destino);
            return;
        }
        FadeTransition f = new FadeTransition(duracion, nodo);
        f.setToValue(destino);
        f.play();
    }

    /** Aparición con un deslizamiento corto hacia arriba. */
    public static void aparecer(Node nodo, double desplazamiento, Duration duracion) {
        if (reducidas) {
            nodo.setOpacity(1);
            nodo.setTranslateY(0);
            return;
        }
        nodo.setOpacity(0);
        nodo.setTranslateY(desplazamiento);
        FadeTransition f = new FadeTransition(duracion, nodo);
        f.setToValue(1);
        TranslateTransition t = new TranslateTransition(duracion, nodo);
        t.setToY(0);
        t.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(f, t).play();
    }

    /** Animación en bucle (solo la lámpara del inicio de sesión); no se inicia si se reducen las animaciones. */
    public static void repetir(Animation animacion) {
        if (!reducidas) {
            animacion.setCycleCount(Animation.INDEFINITE);
            animacion.play();
        }
    }
}
