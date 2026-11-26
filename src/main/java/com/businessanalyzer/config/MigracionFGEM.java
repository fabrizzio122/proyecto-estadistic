package com.businessanalyzer.config;

import java.util.Objects;

public record MigracionFGEM(int versionFGEM, String descripcionFGEM, String recursoFGEM) {

    public MigracionFGEM {
        if (versionFGEM < 1) {
            throw new IllegalArgumentException("La version de una migracion debe ser mayor que cero");
        }
        Objects.requireNonNull(descripcionFGEM, "descripcion");
        Objects.requireNonNull(recursoFGEM, "recurso");
    }
}
