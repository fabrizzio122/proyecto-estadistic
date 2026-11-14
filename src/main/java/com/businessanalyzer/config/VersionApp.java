package com.businessanalyzer.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Versión de la aplicación. Se define solo en el {@code pom.xml}; Maven la escribe en
 * {@code version.properties} al compilar y desde aquí la usan la ventana, la configuración y los informes.
 */
public final class VersionApp {

    /** Versión completa, por ejemplo {@code 1.0.0}. */
    public static final String NUMERO = leer();

    /** Versión mayor y menor, por ejemplo {@code 1.0} (se muestra junto al nombre del producto). */
    public static final String CORTA = corta(NUMERO);

    /** Nombre del producto con su versión corta: {@code Desktop 1.0}. */
    public static final String SUBTITULO = "Desktop " + CORTA;

    /** {@code Business Analyzer Desktop 1.0}. */
    public static final String PRODUCTO = "Business Analyzer " + SUBTITULO;

    private static final String SIN_VERSION = "0.0.0";

    private VersionApp() {
    }

    private static String leer() {
        try (InputStream entrada = VersionApp.class.getResourceAsStream("/version.properties")) {
            if (entrada == null) {
                return SIN_VERSION;
            }
            Properties propiedades = new Properties();
            propiedades.load(entrada);
            String version = propiedades.getProperty("version", "").trim();
            // Sin el filtrado de Maven el archivo conserva el marcador ${project.version}.
            return version.isEmpty() || version.contains("${") ? SIN_VERSION : version;
        } catch (IOException e) {
            return SIN_VERSION;
        }
    }

    static String corta(String version) {
        Matcher m = Pattern.compile("^(\\d+)\\.(\\d+)").matcher(version);
        return m.find() ? m.group(1) + "." + m.group(2) : version;
    }
}
