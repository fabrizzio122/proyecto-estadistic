package com.businessanalyzer.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class AppConfigFGEM {

    public static final String PROPIEDAD_DIRECTORIO_DATOS_FGEM = "ba.data.dir";
    private static final String ARCHIVO_BASE_DATOS_FGEM = "business-analyzer.db";
    public static final String CARPETA_FOTOS_FGEM = "fotos-visitas";

    private final Path directorioDatosFGEM;

    public AppConfigFGEM(Path directorioDatosFGEM) {
        this.directorioDatosFGEM = Objects.requireNonNull(directorioDatosFGEM, "directorioDatos").toAbsolutePath();
    }

    public static AppConfigFGEM desdeSistemaFGEM() {
        String personalizadoFGEM = System.getProperty(PROPIEDAD_DIRECTORIO_DATOS_FGEM);
        Path directorioFGEM = (personalizadoFGEM != null && !personalizadoFGEM.isBlank())
                ? Path.of(personalizadoFGEM)
                : Path.of(System.getProperty("user.home"), "BusinessAnalyzer");
        return new AppConfigFGEM(directorioFGEM);
    }

    public Path directorioDatosFGEM() {
        return directorioDatosFGEM;
    }

    public Path archivoBaseDatosFGEM() {
        return directorioDatosFGEM.resolve(ARCHIVO_BASE_DATOS_FGEM);
    }

    public void prepararDirectoriosFGEM() throws IOException {
        Files.createDirectories(directorioDatosFGEM);
    }
}
