package com.businessanalyzer.model;

import java.nio.file.Path;
import java.time.LocalDateTime;

/** Copia de seguridad (base de datos y fotos de las visitas) guardada en la carpeta de copias. */
public record CopiaSeguridadFGEM(Path archivoFGEM, LocalDateTime fechaFGEM, TipoFGEM tipoFGEM, long bytesFGEM) {

    /** Origen de la copia; forma parte del nombre del archivo. */
    public enum TipoFGEM {
        MANUAL("Manual", "manual"),
        AUTOMATICA("Automática", "auto"),
        PREVIA_RESTAURACION("Antes de restaurar", "previa");

        private final String descripcionFGEM;
        private final String sufijoFGEM;

        TipoFGEM(String descripcionFGEM, String sufijoFGEM) {
            this.descripcionFGEM = descripcionFGEM;
            this.sufijoFGEM = sufijoFGEM;
        }

        public String descripcionFGEM() {
            return descripcionFGEM;
        }

        public String sufijoFGEM() {
            return sufijoFGEM;
        }

        public static TipoFGEM desdeSufijoFGEM(String sufijoFGEM) {
            for (TipoFGEM tipoFGEM : values()) {
                if (tipoFGEM.sufijoFGEM.equals(sufijoFGEM)) {
                    return tipoFGEM;
                }
            }
            throw new IllegalArgumentException("Tipo de copia desconocido: " + sufijoFGEM);
        }
    }

    /** Resumen del contenido de un archivo de copia, obtenido al validarlo ({@code fotos}: fotos de visitas incluidas). */
    public record ContenidoFGEM(int versionEsquemaFGEM, int empresasFGEM, int usuariosFGEM, int operacionesFGEM, int fotosFGEM) {
    }
}
