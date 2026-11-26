package com.businessanalyzer.ui;

import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.feather.Feather;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuloTestFGEM {

    @Test
    void todosLosIconosExistenEnFeatherFGEM() {
        for (ModuloFGEM moduloFGEM : ModuloFGEM.values()) {
            assertDoesNotThrow(() -> Feather.findByDescription(moduloFGEM.iconoFGEM()),
                    "Icono inexistente en " + moduloFGEM);
        }
    }

    @Test
    void todasLasVistasExistenFGEM() {
        for (ModuloFGEM moduloFGEM : ModuloFGEM.values()) {
            assertNotNull(ModuloFGEM.class.getResource(moduloFGEM.vistaFGEM()),
                    "No existe la vista " + moduloFGEM.vistaFGEM() + " de " + moduloFGEM);
        }
    }

    @Test
    void lasSeccionesDelMenuNoSeIntercalanFGEM() {
        Set<ModuloFGEM.SeccionFGEM> cerradasFGEM = new HashSet<>();
        ModuloFGEM.SeccionFGEM actualFGEM = null;
        for (ModuloFGEM moduloFGEM : ModuloFGEM.values()) {
            if (moduloFGEM.seccionFGEM() != actualFGEM) {
                assertFalse(cerradasFGEM.contains(moduloFGEM.seccionFGEM()),
                        "La seccion " + moduloFGEM.seccionFGEM() + " aparece separada en el menu");
                if (actualFGEM != null) {
                    cerradasFGEM.add(actualFGEM);
                }
                actualFGEM = moduloFGEM.seccionFGEM();
            }
        }
    }

    @Test
    void cadaModuloTieneUnaFaseValidaYUnPermisoFGEM() {
        for (ModuloFGEM moduloFGEM : ModuloFGEM.values()) {
            assertTrue(moduloFGEM.faseFGEM() >= 1 && moduloFGEM.faseFGEM() <= 30, "Fase invalida en " + moduloFGEM);
            assertNotNull(moduloFGEM.permisoFGEM(), "Falta el permiso de " + moduloFGEM);
        }
    }

    @Test
    void ningunTextoDeLasVistasEmpiezaConPorcentajeSinEscaparFGEM() throws Exception {
        java.nio.file.Path carpetaFGEM = java.nio.file.Path.of(ModuloFGEM.class.getResource("/fxml").toURI());
        try (var archivosFGEM = java.nio.file.Files.list(carpetaFGEM)) {
            for (java.nio.file.Path archivoFGEM : archivosFGEM.filter(aFGEM -> aFGEM.toString().endsWith(".fxml")).toList()) {
                String contenidoFGEM = java.nio.file.Files.readString(archivoFGEM);
                assertFalse(java.util.regex.Pattern.compile("=\"%").matcher(contenidoFGEM).find(),
                        "Texto con % sin escapar en " + archivoFGEM.getFileName());
            }
        }
    }

    @Test
    void desdeNombreToleraValoresDesconocidosFGEM() {
        assertEquals(ModuloFGEM.EMPRESAS, ModuloFGEM.desdeNombreFGEM("EMPRESAS").orElseThrow());
        assertTrue(ModuloFGEM.desdeNombreFGEM("NO_EXISTE").isEmpty());
        assertTrue(ModuloFGEM.desdeNombreFGEM(null).isEmpty());
    }
}
