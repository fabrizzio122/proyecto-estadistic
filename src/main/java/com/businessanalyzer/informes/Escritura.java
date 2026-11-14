package com.businessanalyzer.informes;

import com.businessanalyzer.exception.InformeException;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Los informes se escriben primero en un archivo temporal y luego reemplazan al destino, para no
 * dejar un archivo a medias si algo falla.
 */
final class Escritura {

    private Escritura() {
    }

    static void reemplazar(Path temporal, Path destino) {
        try {
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (AccessDeniedException e) {
            borrar(temporal);
            throw new InformeException("No se pudo guardar \"" + destino.getFileName() + "\": el archivo está "
                    + "abierto en otro programa o la carpeta no permite escribir. Ciérralo e inténtalo de nuevo.", e);
        } catch (FileSystemException e) {
            borrar(temporal);
            throw new InformeException("No se pudo guardar \"" + destino.getFileName() + "\": " + e.getReason()
                    + ". Si el archivo está abierto en otro programa, ciérralo e inténtalo de nuevo.", e);
        } catch (IOException e) {
            borrar(temporal);
            throw new InformeException("No se pudo guardar \"" + destino.getFileName() + "\".", e);
        }
    }

    static void borrar(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignorado) {
            // El temporal quedará en la carpeta; no impide continuar.
        }
    }
}
