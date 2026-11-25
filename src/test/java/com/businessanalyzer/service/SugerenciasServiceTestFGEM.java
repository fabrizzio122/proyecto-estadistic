package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.ResultadoFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.businessanalyzer.repository.ProductoRepositoryFGEM;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SugerenciasServiceTestFGEM {

    @TempDir
    Path carpetaFGEM;

    @Test
    void conLaBodegaDeEjemploSugierePreciosYRecuerdaElMargenFGEM() {
        EntornoPruebaFGEM entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        entornoFGEM.administradorConectadoFGEM();
        entornoFGEM.crearEmpresaActivaFGEM("Bodega de ejemplo");
        ImportacionServiceFGEM importacionFGEM = new ImportacionServiceFGEM(entornoFGEM.databaseFGEM, entornoFGEM.catalogoFGEM,
                entornoFGEM.operacionesFGEM, entornoFGEM.finanzasFGEM, entornoFGEM.empresaServiceFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM);
        Path archivoFGEM = carpetaFGEM.resolve("ejemplo.xlsx");
        importacionFGEM.crearPlantillaFGEM(archivoFGEM, true);
        assertTrue(importacionFGEM.importarFGEM(archivoFGEM).aplicadoFGEM());
        SugerenciasServiceFGEM servicioFGEM = new SugerenciasServiceFGEM(entornoFGEM.databaseFGEM, new ProductoRepositoryFGEM(),
                new OperacionRepositoryFGEM(), new com.businessanalyzer.repository.MovimientoRepositoryFGEM(), entornoFGEM.empresaServiceFGEM, entornoFGEM.configuracionFGEM, entornoFGEM.sesionFGEM,
                entornoFGEM.relojFGEM);
        assertEquals(0, SugerenciasProductoFGEM.MARGEN_OBJETIVO_PREDETERMINADO_FGEM.compareTo(servicioFGEM.margenObjetivoFGEM()));

        // Con 25 % de margen objetivo, varios productos de la bodega (márgenes de 15 % a 25 %) quedan cortos.
        ResultadoFGEM resultadoFGEM = servicioFGEM.sugerenciasFGEM(new BigDecimal("0.25"));

        assertEquals(SugerenciasProductoFGEM.DIAS_ANALISIS_FGEM, resultadoFGEM.diasAnalizadosFGEM());
        assertTrue(resultadoFGEM.preciosFGEM().size() >= 5, "Productos con margen menor a 25 %: " + resultadoFGEM.preciosFGEM().size());
        assertTrue(resultadoFGEM.gananciaExtraMesFGEM().signum() > 0);
        assertTrue(resultadoFGEM.preciosFGEM().stream().allMatch(pFGEM -> pFGEM.margenSugeridoFGEM().compareTo(new BigDecimal("0.25")) >= 0));
        assertEquals(0, new BigDecimal("0.25").compareTo(servicioFGEM.margenObjetivoFGEM()), "Se recuerda el margen elegido");
        // La utilidad mensual es la del diagnóstico (promedio de octubre a diciembre).
        assertTrue(resultadoFGEM.utilidadMensualFGEM() != null);
        // El pan del ejemplo viene marcado como perecible en la plantilla.
        assertTrue(entornoFGEM.catalogoFGEM.listarProductosFGEM(false).stream()
                .anyMatch(pFGEM -> pFGEM.nombreFGEM().equals("Pan francés") && pFGEM.perecibleFGEM()));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.sugerenciasFGEM(new BigDecimal("0.95")));
    }
}
