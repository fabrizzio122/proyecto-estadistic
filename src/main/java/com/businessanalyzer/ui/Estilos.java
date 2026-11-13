package com.businessanalyzer.ui;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.util.Objects;

/**
 * Aplica la hoja de estilos del tema. Centralizarlo aqui permite agregar el tema
 * claro en el futuro cambiando un solo lugar.
 */
public final class Estilos {

    public static final String TEMA_OSCURO = "/css/tema-oscuro.css";

    private Estilos() {
    }

    public static void aplicar(Scene escena) {
        escena.getStylesheets().setAll(hojaActual());
    }

    public static void aplicar(DialogPane dialogo) {
        dialogo.getStylesheets().setAll(hojaActual());
    }

    private static String hojaActual() {
        return Objects.requireNonNull(Estilos.class.getResource(TEMA_OSCURO),
                "No se encontro " + TEMA_OSCURO).toExternalForm();
    }
}
