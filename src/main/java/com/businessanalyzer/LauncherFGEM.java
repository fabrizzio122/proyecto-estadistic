package com.businessanalyzer;

import javafx.application.Application;

/**
 * Punto de entrada del programa.
 * <p>
 * Se usa una clase separada que no hereda de {@link Application} para que la aplicacion
 * arranque tambien desde IntelliJ o desde un JAR sin configurar el module-path de JavaFX.
 */
public final class LauncherFGEM {

    private LauncherFGEM() {
    }

    public static void main(String[] argsFGEM) {
        Application.launch(BusinessAnalyzerAppFGEM.class, argsFGEM);
    }
}
