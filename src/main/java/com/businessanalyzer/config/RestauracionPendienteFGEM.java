package com.businessanalyzer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Restauración de una copia de seguridad en dos pasos, para no reemplazar la base de datos
 * mientras la aplicación la está usando:
 * <ol>
 *     <li>Desde Configuración, la copia elegida se valida y se deja como
 *         {@code restauracion-pendiente.db} junto a la base de datos (y sus fotos, si las trae, en
 *         {@code restauracion-pendiente-fotos/}); luego la aplicación se cierra.</li>
 *     <li>Al iniciar de nuevo, antes de abrir cualquier conexión, {@link #aplicarFGEM(Path)} reemplaza la
 *         base de datos por la copia, descarta los archivos temporales de SQLite (-wal, -shm) de la
 *         base anterior y, si hay fotos pendientes, reemplaza la carpeta de fotos. Después el migrador
 *         actualiza el esquema si la copia era de una versión anterior.</li>
 * </ol>
 */
public final class RestauracionPendienteFGEM {

    public static final String ARCHIVO_FGEM = "restauracion-pendiente.db";
    public static final String CARPETA_FOTOS_FGEM = "restauracion-pendiente-fotos";

    private static final Logger logFGEM = LoggerFactory.getLogger(RestauracionPendienteFGEM.class);

    private RestauracionPendienteFGEM() {
    }

    /** Archivo donde se deja la copia que se aplicará al próximo inicio. */
    public static Path archivoPendienteFGEM(Path archivoBaseDatosFGEM) {
        return archivoBaseDatosFGEM.resolveSibling(ARCHIVO_FGEM);
    }

    /** Carpeta donde se dejan las fotos de la copia que se aplicará al próximo inicio. */
    public static Path carpetaFotosPendienteFGEM(Path archivoBaseDatosFGEM) {
        return archivoBaseDatosFGEM.resolveSibling(CARPETA_FOTOS_FGEM);
    }

    public static boolean hayPendienteFGEM(Path archivoBaseDatosFGEM) {
        return Files.isRegularFile(archivoPendienteFGEM(archivoBaseDatosFGEM));
    }

    /**
     * Aplica la restauración pendiente, si la hay.
     *
     * @return {@code true} si se reemplazó la base de datos
     */
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

    /** Borra una carpeta y todo su contenido, si existe. */
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
