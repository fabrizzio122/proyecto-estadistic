package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.prediccion.LecturaPrediccionFGEM;
import com.businessanalyzer.analytics.prediccion.PronosticoFGEM;
import com.businessanalyzer.analytics.prediccion.ResultadoPrediccionFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM.DatosLineaFGEM;
import com.businessanalyzer.model.DatosProductoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reloj de pruebas en 2026-01-15. Ventas de julio a diciembre de 2025: 1000, 1200 ... 2000
 * (crecen 200 por mes) con un costo del 60 % -> utilidad 400, 480 ... 800.
 * En enero (mes en curso) hay una venta que no debe usarse como historia.
 */
class PrediccionServiceTestFGEM {

    private static final double TOLERANCIA_FGEM = 1e-6;

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private PrediccionServiceFGEM servicioFGEM;
    private EmpresaFGEM empresaFGEM;
    private ProductoFGEM productoFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = new PrediccionServiceFGEM(entornoFGEM.databaseFGEM, new OperacionRepositoryFGEM(), new MovimientoRepositoryFGEM(),
                entornoFGEM.empresaServiceFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM);
        entornoFGEM.administradorConectadoFGEM();
        empresaFGEM = entornoFGEM.crearEmpresaActivaFGEM("Comercial Andina");
        productoFGEM = entornoFGEM.catalogoFGEM.crearProductoFGEM(new DatosProductoFGEM(TipoItemFGEM.SERVICIO, null, "Servicio", null, null,
                UnidadMedidaFGEM.SERVICIO, new BigDecimal("60"), new BigDecimal("100")));
        for (int kFGEM = 0; kFGEM < 6; kFGEM++) {
            venderFGEM(YearMonth.of(2025, 7).plusMonths(kFGEM).atDay(15), 10 + 2 * kFGEM);
        }
        venderFGEM(LocalDate.of(2026, 1, 10), 3);
    }

    @Test
    void proyectaLasVentasDesdeElMesEnCursoFGEM() {
        ResultadoPrediccionFGEM rFGEM = servicioFGEM.predecirFGEM(3);

        assertEquals(6, rFGEM.mesesFGEM().size(), "Julio a diciembre; enero está incompleto");
        assertEquals(YearMonth.of(2026, 1), rFGEM.primerMesProyectadoFGEM(null));
        PronosticoFGEM ventasFGEM = rFGEM.pronosticoVentasFGEM();
        assertEquals("Tendencia lineal", ventasFGEM.elegidaFGEM().metodoFGEM().nombreFGEM());
        assertEquals(PronosticoFGEM.ConfiabilidadFGEM.ALTA, ventasFGEM.confiabilidadFGEM());
        assertEquals(2200, ventasFGEM.puntosFGEM().get(0).valorFGEM(), TOLERANCIA_FGEM);
        assertEquals(2600, ventasFGEM.puntosFGEM().get(2).valorFGEM(), TOLERANCIA_FGEM);
        assertEquals(880, rFGEM.pronosticoUtilidadFGEM().puntosFGEM().get(0).valorFGEM(), TOLERANCIA_FGEM);
    }

    @Test
    void comparaConLosUltimosMesesFGEM() {
        ResultadoPrediccionFGEM rFGEM = servicioFGEM.predecirFGEM(3);
        assertEquals(1600 + 1800 + 2000, rFGEM.ventasUltimosMesesFGEM(), TOLERANCIA_FGEM);

        List<String> frasesFGEM = LecturaPrediccionFGEM.generarFGEM(rFGEM, MonedaFGEM.PEN);
        assertTrue(frasesFGEM.get(0).contains("sumarían alrededor de S/ 7,200.00"), frasesFGEM.toString());
        assertTrue(frasesFGEM.get(1).startsWith("Eso es 33.3 % más"), frasesFGEM.toString());
    }

    @Test
    void conPocaHistoriaNoProyectaFGEM() {
        entornoFGEM.crearEmpresaActivaFGEM("Negocio nuevo");
        ResultadoPrediccionFGEM rFGEM = servicioFGEM.predecirFGEM(6);

        assertFalse(rFGEM.pronosticoVentasFGEM().suficienteFGEM());
        assertTrue(LecturaPrediccionFGEM.generarFGEM(rFGEM, MonedaFGEM.PEN).get(0).startsWith("Se necesitan al menos 4 meses"));
    }

    @Test
    void validaElHorizonteYLosPermisosFGEM() {
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.predecirFGEM(0));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.predecirFGEM(13));

        UsuarioFGEM operadorFGEM = entornoFGEM.crearUsuarioFGEM("operador", "OPERADOR");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of(operadorFGEM.idFGEM()));
        entornoFGEM.entrarComoFGEM("operador", "Temporal123");
        entornoFGEM.empresaServiceFGEM.restaurarEmpresaActivaFGEM();
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.predecirFGEM(3));
    }

    private void venderFGEM(LocalDate fechaFGEM, int cantidadFGEM) {
        entornoFGEM.operacionesFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.VENTA, fechaFGEM, null, null, null, BigDecimal.ZERO,
                null, List.of(new DatosLineaFGEM(productoFGEM.idFGEM(), BigDecimal.valueOf(cantidadFGEM), new BigDecimal("100"))), false), false);
    }
}
