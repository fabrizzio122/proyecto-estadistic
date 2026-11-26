package com.businessanalyzer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

public final class RestauracionPendienteFGEM {

    public static final String ARCHIVO_FGEM = "restauracion-pendiente.db";
    public static final String CARPETA_FOTOS_FGEM = "restauracion-pendiente-fotos";

    private static final Logger logFGEM = LoggerFactory.getLogger(RestauracionPendienteFGEM.class);

    private RestauracionPendienteFGEM() {
    }

    public static Path archivoPendienteFGEM(Path archivoBaseDatosFGEM) {
        return archivoBaseDatosFGEM.resolveSibling(ARCHIVO_FGEM);
    }

    public static Path carpetaFotosPendienteFGEM(Path archivoBaseDatosFGEM) {
        return archivoBaseDatosFGEM.resolveSibling(CARPETA_FOTOS_FGEM);
    }

    public static boolean hayPendienteFGEM(Path archivoBaseDatosFGEM) {
        return Files.isRegularFile(archivoPendienteFGEM(archivoBaseDatosFGEM));
    }

    public static boolean aplicarFGEM(Path archivoBaseDatosFGEM) throws IOException {
        Path pendienteFGEM = archivoPendienteFGEM(archivoBaseDatosFGEM);
        if (!Files.isRegularFile(pendienteFGEM)) {
            return false;
        }
        Files.deleteIfExists(archivoBaseDatosFGEM.resolveSibling(archivoBaseDatosFGEM.getFileName() + "-wal"));
        Files.deleteIfExists(archivoBaseDatosFGEM.resolveSibling(archivoBaseDatosFGEM.getFileName() + "-shm"));
        Files.move(pendienteFGEM, archivoBaseDatosFGEM, StandardCopyOption.REPLACE_EXISTING);
        logFGEM.warn("Se restauró una copia de seguridad sobre {}", archivoBaseDatosFGEM);

        Path fotosPendientesFGEM = carpetaFotosPendienteFGEM(archivoBaseDatosFGEM);
        if (Files.isDirectory(fotosPendientesFGEM)) {
            Path fotosFGEM = archivoBaseDatosFGEM.resolveSibling(AppConfigFGEM.CARPETA_FOTOS_FGEM);
            borrarFGEM(fotosFGEM);
            Files.move(fotosPendientesFGEM, fotosFGEM);
            logFGEM.warn("Se restauraron las fotos de las visitas en {}", fotosFGEM);
        }
        return true;
    }

    public static void borrarFGEM(Path carpetaFGEM) throws IOException {
        if (!Files.exists(carpetaFGEM)) {
            return;
        }
        try (Stream<Path> recorridoFGEM = Files.walk(carpetaFGEM)) {
            for (Path pFGEM : recorridoFGEM.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(pFGEM);
            }
        }
    }
}
