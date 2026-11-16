package com.businessanalyzer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionAppTestFGEM {

    @Test
    void laVersionVieneDelPomYaFiltradaFGEM() {
        assertTrue(VersionAppFGEM.NUMERO_FGEM.matches("\\d+\\.\\d+\\.\\d+"),
                "Maven debe reemplazar ${project.version}: " + VersionAppFGEM.NUMERO_FGEM);
        assertTrue(!VersionAppFGEM.NUMERO_FGEM.equals("0.0.0"), "No se encontró version.properties filtrado");
        assertEquals("Desktop " + VersionAppFGEM.CORTA_FGEM, VersionAppFGEM.SUBTITULO_FGEM);
        assertEquals("Business Analyzer " + VersionAppFGEM.SUBTITULO_FGEM, VersionAppFGEM.PRODUCTO_FGEM);
    }

    @Test
    void versionCortaFGEM() {
        assertEquals("1.0", VersionAppFGEM.cortaFGEM("1.0.0"));
        assertEquals("2.13", VersionAppFGEM.cortaFGEM("2.13.7"));
        assertEquals("x", VersionAppFGEM.cortaFGEM("x"));
    }
}
