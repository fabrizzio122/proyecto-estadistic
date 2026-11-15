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
 *     <li>Al iniciar de nuevo, antes de abrir cualquier conexión, {@link #aplicar(Path)} reemplaza la
 *         base de datos por la copia, descarta los archivos temporales de SQLite (-wal, -shm) de la
 *         base anterior y, si hay fotos pendientes, reemplaza la carpeta de fotos. Después el migrador
 *         actualiza el esquema si la copia era de una versión anterior.</li>
 * </ol>
 */
public final class RestauracionPendiente {

    public static final String ARCHIVO = "restauracion-pendiente.db";
    public static final String CARPETA_FOTOS = "restauracion-pendiente-fotos";

    private static final Logger log = LoggerFactory.getLogger(RestauracionPendiente.class);

    private RestauracionPendiente() {
    }

    /** Archivo donde se deja la copia que se aplicará al próximo inicio. */
    public static Path archivoPendiente(Path archivoBaseDatos) {
        return archivoBaseDatos.resolveSibling(ARCHIVO);
    }

    /** Carpeta donde se dejan las fotos de la copia que se aplicará al próximo inicio. */
    public static Path carpetaFotosPendiente(Path archivoBaseDatos) {
        return archivoBaseDatos.resolveSibling(CARPETA_FOTOS);
    }

    public static boolean hayPendiente(Path archivoBaseDatos) {
        return Files.isRegularFile(archivoPendiente(archivoBaseDatos));
    }

    /**
     * Aplica la restauración pendiente, si la hay.
     *
     * @return {@code true} si se reemplazó la base de datos
     */
    public static boolean aplicar(Path archivoBaseDatos) throws IOException {
        Path pendiente = archivoPendiente(archivoBaseDatos);
        if (!Files.isRegularFile(pendiente)) {
            return false;
        }
        Files.deleteIfExists(archivoBaseDatos.resolveSibling(archivoBaseDatos.getFileName() + "-wal"));
        Files.deleteIfExists(archivoBaseDatos.resolveSibling(archivoBaseDatos.getFileName() + "-shm"));
        Files.move(pendiente, archivoBaseDatos, StandardCopyOption.REPLACE_EXISTING);
        log.warn("Se restauró una copia de seguridad sobre {}", archivoBaseDatos);

        Path fotosPendientes = carpetaFotosPendiente(archivoBaseDatos);
        if (Files.isDirectory(fotosPendientes)) {
            Path fotos = archivoBaseDatos.resolveSibling(AppConfig.CARPETA_FOTOS);
            borrar(fotos);
            Files.move(fotosPendientes, fotos);
            log.warn("Se restauraron las fotos de las visitas en {}", fotos);
        }
        return true;
    }

    /** Borra una carpeta y todo su contenido, si existe. */
    public static void borrar(Path carpeta) throws IOException {
        if (!Files.exists(carpeta)) {
            return;
        }
        try (Stream<Path> recorrido = Files.walk(carpeta)) {
            for (Path p : recorrido.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}
