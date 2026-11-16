package com.businessanalyzer.config;

import java.util.Objects;

/**
 * Un cambio versionado del esquema de la base de datos.
 *
 * @param versionFGEM     numero correlativo (1, 2, 3...). Nunca se reutiliza.
 * @param descripcionFGEM texto que queda registrado en la tabla schema_version.
 * @param recursoFGEM     ruta del script SQL dentro de src/main/resources.
 */
public record MigracionFGEM(int versionFGEM, String descripcionFGEM, String recursoFGEM) {

    public MigracionFGEM {
        if (versionFGEM < 1) {
            throw new IllegalArgumentException("La version de una migracion debe ser mayor que cero");
        }
        Objects.requireNonNull(descripcionFGEM, "descripcion");
        Objects.requireNonNull(recursoFGEM, "recurso");
    }
}
