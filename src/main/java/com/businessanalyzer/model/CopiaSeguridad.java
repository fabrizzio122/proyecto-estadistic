package com.businessanalyzer.model;

import java.nio.file.Path;
import java.time.LocalDateTime;

/** Copia de seguridad de la base de datos guardada en la carpeta de copias. */
public record CopiaSeguridad(Path archivo, LocalDateTime fecha, Tipo tipo, long bytes) {

    /** Origen de la copia; forma parte del nombre del archivo. */
    public enum Tipo {
        MANUAL("Manual", "manual"),
        AUTOMATICA("Automática", "auto"),
        PREVIA_RESTAURACION("Antes de restaurar", "previa");

        private final String descripcion;
        private final String sufijo;

        Tipo(String descripcion, String sufijo) {
            this.descripcion = descripcion;
            this.sufijo = sufijo;
        }

        public String descripcion() {
            return descripcion;
        }

        public String sufijo() {
            return sufijo;
        }

        public static Tipo desdeSufijo(String sufijo) {
            for (Tipo tipo : values()) {
                if (tipo.sufijo.equals(sufijo)) {
                    return tipo;
                }
            }
            throw new IllegalArgumentException("Tipo de copia desconocido: " + sufijo);
        }
    }

    /** Resumen del contenido de un archivo de copia, obtenido al validarlo. */
    public record Contenido(int versionEsquema, int empresas, int usuarios, int operaciones) {
    }
}
