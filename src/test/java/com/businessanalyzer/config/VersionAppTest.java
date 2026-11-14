package com.businessanalyzer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionAppTest {

    @Test
    void laVersionVieneDelPomYaFiltrada() {
        assertTrue(VersionApp.NUMERO.matches("\\d+\\.\\d+\\.\\d+"),
                "Maven debe reemplazar ${project.version}: " + VersionApp.NUMERO);
        assertTrue(!VersionApp.NUMERO.equals("0.0.0"), "No se encontró version.properties filtrado");
        assertEquals("Desktop " + VersionApp.CORTA, VersionApp.SUBTITULO);
        assertEquals("Business Analyzer " + VersionApp.SUBTITULO, VersionApp.PRODUCTO);
    }

    @Test
    void versionCorta() {
        assertEquals("1.0", VersionApp.corta("1.0.0"));
        assertEquals("2.13", VersionApp.corta("2.13.7"));
        assertEquals("x", VersionApp.corta("x"));
    }
}
