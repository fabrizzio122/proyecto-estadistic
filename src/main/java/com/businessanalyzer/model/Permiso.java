package com.businessanalyzer.model;

/**
 * Acciones que el sistema controla. Cada rol tiene un conjunto de permisos
 * (tabla {@code rol_permiso}). Los codigos deben coincidir con la tabla {@code permiso}.
 */
public enum Permiso {

    DASHBOARD_VER("Ver el dashboard"),
    EMPRESAS_VER("Ver empresas"),
    EMPRESAS_GESTIONAR("Registrar y modificar empresas"),
    PRODUCTOS_VER("Ver productos y servicios"),
    PRODUCTOS_GESTIONAR("Registrar y modificar productos y servicios"),
    OPERACIONES_VER("Ver ventas y compras"),
    OPERACIONES_GESTIONAR("Registrar y modificar ventas y compras"),
    FINANZAS_VER("Ver ingresos, gastos y costos"),
    FINANZAS_GESTIONAR("Registrar y modificar ingresos, gastos y costos"),
    ANALISIS_VER("Ver análisis, predicciones, escenarios y diagnóstico"),
    INFORMES_GENERAR("Generar y exportar informes"),
    USUARIOS_GESTIONAR("Administrar usuarios y revisar accesos"),
    CONFIGURACION_GESTIONAR("Configuración y copias de seguridad"),
    AGRO_VER("Ver ganadería y agricultura"),
    AGRO_GESTIONAR("Registrar ganado, producción y campañas agrícolas");

    private final String descripcion;

    Permiso(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
