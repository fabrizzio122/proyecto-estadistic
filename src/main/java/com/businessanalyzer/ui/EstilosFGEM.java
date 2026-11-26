package com.businessanalyzer.ui;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Region;

import java.util.Objects;

public final class EstilosFGEM {

    public static final String TEMA_FGEM = "/css/tema.css";

    private EstilosFGEM() {
    }

    public static void aplicarFGEM(Scene escenaFGEM) {
        escenaFGEM.getStylesheets().setAll(hojaFGEM());
    }

    public static void aplicarFGEM(DialogPane dialogoFGEM) {
        dialogoFGEM.getStylesheets().setAll(hojaFGEM());
        ajustarBotonesFGEM(dialogoFGEM);
        dialogoFGEM.getButtonTypes().addListener((javafx.collections.ListChangeListener<ButtonType>) cFGEM -> ajustarBotonesFGEM(dialogoFGEM));
    }

    private static void ajustarBotonesFGEM(DialogPane dialogoFGEM) {
        for (ButtonType tipoFGEM : dialogoFGEM.getButtonTypes()) {
            if (dialogoFGEM.lookupButton(tipoFGEM) instanceof Button botonFGEM) {
                ButtonBar.setButtonUniformSize(botonFGEM, false);
                botonFGEM.setMinWidth(Region.USE_PREF_SIZE);
            }
        }
    }

    private static String hojaFGEM() {
        return Objects.requireNonNull(EstilosFGEM.class.getResource(TEMA_FGEM), "No se encontro " + TEMA_FGEM).toExternalForm();
    }
}
