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
public final class FuentesFGEM {

    public static final String TEXTO_FGEM = "Inter";
    public static final String TITULOS_FGEM = "Space Grotesk";

    private static final String[] ARCHIVOS_FGEM = {
            "inter-400.ttf", "inter-500.ttf", "inter-600.ttf", "inter-700.ttf",
            "space-grotesk-500.ttf", "space-grotesk-700.ttf"
    };

    private static final Logger logFGEM = LoggerFactory.getLogger(FuentesFGEM.class);
    private static boolean cargadasFGEM;

    private FuentesFGEM() {
    }

    /** Registra las tipografías en JavaFX una sola vez. */
    public static synchronized void cargarFGEM() {
        if (cargadasFGEM) {
            return;
        }
        cargadasFGEM = true;
        for (String archivoFGEM : ARCHIVOS_FGEM) {
            try (InputStream entradaFGEM = FuentesFGEM.class.getResourceAsStream("/fonts/" + archivoFGEM)) {
                if (entradaFGEM == null || Font.loadFont(entradaFGEM, 13) == null) {
                    logFGEM.warn("No se pudo cargar la tipografía {}", archivoFGEM);
                }
            } catch (IOException eFGEM) {
                logFGEM.warn("No se pudo cargar la tipografía {}", archivoFGEM, eFGEM);
            }
        }
    }
}
