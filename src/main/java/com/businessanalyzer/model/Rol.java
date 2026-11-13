package com.businessanalyzer.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Rol de usuario con sus permisos. */
public record Rol(long id, String codigo, String nombre, String descripcion, Set<Permiso> permisos) {

    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    public Rol {
        permisos = permisos.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(permisos));
    }

    public boolean tiene(Permiso permiso) {
        return permisos.contains(permiso);
    }

    public boolean esAdministrador() {
        return ADMINISTRADOR.equals(codigo);
    }

    /** Texto mostrado en listas desplegables. */
    @Override
    public String toString() {
        return nombre;
    }
}
