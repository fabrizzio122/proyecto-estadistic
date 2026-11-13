package com.businessanalyzer.config;

import java.util.Objects;

/**
 * Un cambio versionado del esquema de la base de datos.
 *
 * @param version     numero correlativo (1, 2, 3...). Nunca se reutiliza.
 * @param descripcion texto que queda registrado en la tabla schema_version.
 * @param recurso     ruta del script SQL dentro de src/main/resources.
 */
public record Migracion(int version, String descripcion, String recurso) {

    public Migracion {
        if (version < 1) {
            throw new IllegalArgumentException("La version de una migracion debe ser mayor que cero");
        }
        Objects.requireNonNull(descripcion, "descripcion");
        Objects.requireNonNull(recurso, "recurso");
    }
}
