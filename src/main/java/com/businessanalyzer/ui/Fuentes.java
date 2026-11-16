package com.businessanalyzer.ui;

import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Carga las tipografías incluidas en la aplicación (licencia SIL Open Font License, ver
 * {@code resources/fonts}): Inter para el texto y Space Grotesk para títulos y cifras. Se incluyen
 * porque la aplicación funciona sin Internet. Si una no carga, JavaFX usa la tipografía del sistema.
 */
public final class Fuentes {

    public static final String TEXTO = "Inter";
    public static final String TITULOS = "Space Grotesk";

    private static final String[] ARCHIVOS = {
            "inter-400.ttf", "inter-500.ttf", "inter-600.ttf", "inter-700.ttf",
            "space-grotesk-500.ttf", "space-grotesk-700.ttf"
    };

    private static final Logger log = LoggerFactory.getLogger(Fuentes.class);
    private static boolean cargadas;

    private Fuentes() {
    }

    /** Registra las tipografías en JavaFX una sola vez. */
    public static synchronized void cargar() {
        if (cargadas) {
            return;
        }
        cargadas = true;
        for (String archivo : ARCHIVOS) {
            try (InputStream entrada = Fuentes.class.getResourceAsStream("/fonts/" + archivo)) {
                if (entrada == null || Font.loadFont(entrada, 13) == null) {
                    log.warn("No se pudo cargar la tipografía {}", archivo);
                }
            } catch (IOException e) {
                log.warn("No se pudo cargar la tipografía {}", archivo, e);
            }
        }
    }
}
