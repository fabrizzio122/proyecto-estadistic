package com.businessanalyzer.service;

import com.businessanalyzer.config.AppConfigFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfiguracionServiceTestFGEM {

    @TempDir
    Path carpetaFGEM;

    private ConfiguracionServiceFGEM servicioFGEM;

    @BeforeEach
    void prepararFGEM() throws Exception {
        servicioFGEM = AppContextFGEM.crearFGEM(new AppConfigFGEM(carpetaFGEM)).configuracionFGEM();
    }

    @Test
    void devuelveElValorPorDefectoSiLaClaveNoExisteFGEM() {
        assertEquals("DASHBOARD", servicioFGEM.obtenerFGEM("ui.no_existe", "DASHBOARD"));
    }

    @Test
    void guardaYRecuperaUnValorFGEM() {
        servicioFGEM.guardarFGEM(ConfiguracionServiceFGEM.CLAVE_ULTIMO_MODULO_FGEM, "EMPRESAS");
        assertEquals("EMPRESAS", servicioFGEM.obtenerFGEM(ConfiguracionServiceFGEM.CLAVE_ULTIMO_MODULO_FGEM, ""));
    }

    @Test
    void guardarDosVecesActualizaElValorFGEM() {
        servicioFGEM.guardarFGEM("ui.prueba", "uno");
        servicioFGEM.guardarFGEM("ui.prueba", "dos");
        assertEquals("dos", servicioFGEM.obtenerFGEM("ui.prueba", ""));
    }

    @Test
    void leeLosValoresInicialesDelEsquemaFGEM() {
        assertEquals("oscuro", servicioFGEM.obtenerFGEM(ConfiguracionServiceFGEM.CLAVE_TEMA_FGEM, "claro"));
    }

    @Test
    void rechazaClavesInvalidasFGEM() {
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.guardarFGEM(null, "x"));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.guardarFGEM("  ", "x"));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.guardarFGEM("Clave Con Espacios", "x"));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.guardarFGEM("x'; DROP TABLE configuracion; --", "x"));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.obtenerFGEM("a".repeat(101), ""));
    }

    @Test
    void rechazaValoresNulosODemasiadoLargosFGEM() {
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.guardarFGEM("ui.prueba", null));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.guardarFGEM("ui.prueba", "x".repeat(1_001)));
    }

    @Test
    void guardaTextosConCaracteresEspecialesSinAlterarlosFGEM() {
        String valorFGEM = "O'Brien; \"comillas\" -- áéíóú ñ";
        servicioFGEM.guardarFGEM("ui.texto", valorFGEM);
        assertEquals(valorFGEM, servicioFGEM.obtenerFGEM("ui.texto", ""));
    }
}
