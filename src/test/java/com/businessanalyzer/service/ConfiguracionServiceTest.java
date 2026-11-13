package com.businessanalyzer.service;

import com.businessanalyzer.config.AppConfig;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.exception.ValidacionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfiguracionServiceTest {

    @TempDir
    Path carpeta;

    private ConfiguracionService servicio;

    @BeforeEach
    void preparar() throws Exception {
        servicio = AppContext.crear(new AppConfig(carpeta)).configuracion();
    }

    @Test
    void devuelveElValorPorDefectoSiLaClaveNoExiste() {
        assertEquals("DASHBOARD", servicio.obtener("ui.no_existe", "DASHBOARD"));
    }

    @Test
    void guardaYRecuperaUnValor() {
        servicio.guardar(ConfiguracionService.CLAVE_ULTIMO_MODULO, "EMPRESAS");
        assertEquals("EMPRESAS", servicio.obtener(ConfiguracionService.CLAVE_ULTIMO_MODULO, ""));
    }

    @Test
    void guardarDosVecesActualizaElValor() {
        servicio.guardar("ui.prueba", "uno");
        servicio.guardar("ui.prueba", "dos");
        assertEquals("dos", servicio.obtener("ui.prueba", ""));
    }

    @Test
    void leeLosValoresInicialesDelEsquema() {
        assertEquals("oscuro", servicio.obtener(ConfiguracionService.CLAVE_TEMA, "claro"));
    }

    @Test
    void rechazaClavesInvalidas() {
        assertThrows(ValidacionException.class, () -> servicio.guardar(null, "x"));
        assertThrows(ValidacionException.class, () -> servicio.guardar("  ", "x"));
        assertThrows(ValidacionException.class, () -> servicio.guardar("Clave Con Espacios", "x"));
        assertThrows(ValidacionException.class, () -> servicio.guardar("x'; DROP TABLE configuracion; --", "x"));
        assertThrows(ValidacionException.class, () -> servicio.obtener("a".repeat(101), ""));
    }

    @Test
    void rechazaValoresNulosODemasiadoLargos() {
        assertThrows(ValidacionException.class, () -> servicio.guardar("ui.prueba", null));
        assertThrows(ValidacionException.class, () -> servicio.guardar("ui.prueba", "x".repeat(1_001)));
    }

    @Test
    void guardaTextosConCaracteresEspecialesSinAlterarlos() {
        String valor = "O'Brien; \"comillas\" -- áéíóú ñ";
        servicio.guardar("ui.texto", valor);
        assertEquals(valor, servicio.obtener("ui.texto", ""));
    }
}
