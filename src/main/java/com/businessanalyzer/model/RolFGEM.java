package com.businessanalyzer.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Rol de usuario con sus permisos. */
public record RolFGEM(long idFGEM, String codigoFGEM, String nombreFGEM, String descripcionFGEM, Set<PermisoFGEM> permisosFGEM) {

    public static final String ADMINISTRADOR_FGEM = "ADMINISTRADOR";

    public RolFGEM {
        permisosFGEM = permisosFGEM.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(permisosFGEM));
    }

    public boolean tieneFGEM(PermisoFGEM permisoFGEM) {
        return permisosFGEM.contains(permisoFGEM);
    }

    public boolean esAdministradorFGEM() {
        return ADMINISTRADOR_FGEM.equals(codigoFGEM);
    }

    /** Texto mostrado en listas desplegables. */
    @Override
    public String toString() {
        return nombreFGEM;
    }
}
