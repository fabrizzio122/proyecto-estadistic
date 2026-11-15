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

class ModuloTest {

    @Test
    void todosLosIconosExistenEnFeather() {
        for (Modulo modulo : Modulo.values()) {
            assertDoesNotThrow(() -> Feather.findByDescription(modulo.icono()),
                    "Icono inexistente en " + modulo);
        }
    }

    @Test
    void todasLasVistasExisten() {
        for (Modulo modulo : Modulo.values()) {
            assertNotNull(Modulo.class.getResource(modulo.vista()),
                    "No existe la vista " + modulo.vista() + " de " + modulo);
        }
    }

    @Test
    void lasSeccionesDelMenuNoSeIntercalan() {
        Set<Modulo.Seccion> cerradas = new HashSet<>();
        Modulo.Seccion actual = null;
        for (Modulo modulo : Modulo.values()) {
            if (modulo.seccion() != actual) {
                assertFalse(cerradas.contains(modulo.seccion()),
                        "La seccion " + modulo.seccion() + " aparece separada en el menu");
                if (actual != null) {
                    cerradas.add(actual);
                }
                actual = modulo.seccion();
            }
        }
    }

    @Test
    void cadaModuloTieneUnaFaseValidaYUnPermiso() {
        for (Modulo modulo : Modulo.values()) {
            assertTrue(modulo.fase() >= 1 && modulo.fase() <= 20, "Fase invalida en " + modulo);
            assertNotNull(modulo.permiso(), "Falta el permiso de " + modulo);
        }
    }

    /**
     * En FXML un texto que empieza con "%" se interpreta como clave de traducción y la vista
     * no carga ("No resources specified"). Debe escribirse "\%".
     */
    @Test
    void ningunTextoDeLasVistasEmpiezaConPorcentajeSinEscapar() throws Exception {
        java.nio.file.Path carpeta = java.nio.file.Path.of(Modulo.class.getResource("/fxml").toURI());
        try (var archivos = java.nio.file.Files.list(carpeta)) {
            for (java.nio.file.Path archivo : archivos.filter(a -> a.toString().endsWith(".fxml")).toList()) {
                String contenido = java.nio.file.Files.readString(archivo);
                assertFalse(java.util.regex.Pattern.compile("=\"%").matcher(contenido).find(),
                        "Texto con % sin escapar en " + archivo.getFileName());
            }
        }
    }

    @Test
    void desdeNombreToleraValoresDesconocidos() {
        assertEquals(Modulo.EMPRESAS, Modulo.desdeNombre("EMPRESAS").orElseThrow());
        assertTrue(Modulo.desdeNombre("NO_EXISTE").isEmpty());
        assertTrue(Modulo.desdeNombre(null).isEmpty());
    }
}
