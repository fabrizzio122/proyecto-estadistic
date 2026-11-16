package com.businessanalyzer.informes;

import com.businessanalyzer.exception.InformeExceptionFGEM;

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
final class EscrituraFGEM {

    private EscrituraFGEM() {
    }

    static void reemplazarFGEM(Path temporalFGEM, Path destinoFGEM) {
        try {
            Files.move(temporalFGEM, destinoFGEM, StandardCopyOption.REPLACE_EXISTING);
        } catch (AccessDeniedException eFGEM) {
            borrarFGEM(temporalFGEM);
            throw new InformeExceptionFGEM("No se pudo guardar \"" + destinoFGEM.getFileName() + "\": el archivo está "
                    + "abierto en otro programa o la carpeta no permite escribir. Ciérralo e inténtalo de nuevo.", eFGEM);
        } catch (FileSystemException eFGEM) {
            borrarFGEM(temporalFGEM);
            throw new InformeExceptionFGEM("No se pudo guardar \"" + destinoFGEM.getFileName() + "\": " + eFGEM.getReason()
                    + ". Si el archivo está abierto en otro programa, ciérralo e inténtalo de nuevo.", eFGEM);
        } catch (IOException eFGEM) {
            borrarFGEM(temporalFGEM);
            throw new InformeExceptionFGEM("No se pudo guardar \"" + destinoFGEM.getFileName() + "\".", eFGEM);
        }
    }

    static void borrarFGEM(Path archivoFGEM) {
        try {
            Files.deleteIfExists(archivoFGEM);
        } catch (IOException ignoradoFGEM) {
            // El temporal quedará en la carpeta; no impide continuar.
        }
    }
}
