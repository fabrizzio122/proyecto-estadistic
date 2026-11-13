package com.businessanalyzer.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Ubicacion de los archivos locales de la aplicacion.
 * <p>
 * Por defecto los datos se guardan en {@code %USERPROFILE%\BusinessAnalyzer}, fuera de la
 * carpeta del programa, para que no se pierdan al actualizar o reinstalar.
 * Se puede cambiar con la propiedad del sistema {@code -Dba.data.dir=C:\ruta}.
 */
public final class AppConfig {

    public static final String PROPIEDAD_DIRECTORIO_DATOS = "ba.data.dir";
    private static final String ARCHIVO_BASE_DATOS = "business-analyzer.db";

    private final Path directorioDatos;

    public AppConfig(Path directorioDatos) {
        this.directorioDatos = Objects.requireNonNull(directorioDatos, "directorioDatos").toAbsolutePath();
    }

    public static AppConfig desdeSistema() {
        String personalizado = System.getProperty(PROPIEDAD_DIRECTORIO_DATOS);
        Path directorio = (personalizado != null && !personalizado.isBlank())
                ? Path.of(personalizado)
                : Path.of(System.getProperty("user.home"), "BusinessAnalyzer");
        return new AppConfig(directorio);
    }

    public Path directorioDatos() {
        return directorioDatos;
    }

    public Path archivoBaseDatos() {
        return directorioDatos.resolve(ARCHIVO_BASE_DATOS);
    }

    public void prepararDirectorios() throws IOException {
        Files.createDirectories(directorioDatos);
    }
}
