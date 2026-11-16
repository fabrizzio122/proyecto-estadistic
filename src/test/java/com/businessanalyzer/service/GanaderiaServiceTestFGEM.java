package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.ganaderia.ExistenciaValorizadaFGEM;
import com.businessanalyzer.analytics.ganaderia.IndicadoresEspecieFGEM;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneralFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CategoriaGanadoFGEM;
import com.businessanalyzer.model.DatosEmpresaFGEM;
import com.businessanalyzer.model.DatosMovimientoGanadoFGEM;
import com.businessanalyzer.model.DatosProduccionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EspecieFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.MovimientoGanadoFGEM;
import com.businessanalyzer.model.ProduccionGanaderaFGEM;
import com.businessanalyzer.model.ProductoGanaderoFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.TamanoEmpresaFGEM;
import com.businessanalyzer.model.TipoMovimientoGanadoFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.businessanalyzer.model.CategoriaGanadoFGEM.*;
import static com.businessanalyzer.model.TipoMovimientoGanadoFGEM.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GanaderiaServiceTestFGEM {

    /** El reloj de pruebas está en 2026-01-15. */
    private static final LocalDate HOY_FGEM = LocalDate.of(2026, 1, 15);

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private GanaderiaServiceFGEM servicioFGEM;
    private EmpresaFGEM fundoFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = entornoFGEM.ganaderiaFGEM;
        entornoFGEM.administradorConectadoFGEM();
        fundoFGEM = entornoFGEM.empresaServiceFGEM.crearFGEM(new DatosEmpresaFGEM("Fundo Los Andes", null, null, SectorFGEM.AGROPECUARIO,
                TamanoEmpresaFGEM.MICRO, MonedaFGEM.PEN, 1, null, null, null, null));
        entornoFGEM.empresaServiceFGEM.seleccionarEmpresaActivaFGEM(fundoFGEM.idFGEM());
    }

    @Test
    void soloFuncionaEnEmpresasAgropecuariasFGEM() {
        entornoFGEM.crearEmpresaActivaFGEM("Bodega");
        assertInvalidoFGEM(servicioFGEM::hatoFGEM);
        assertInvalidoFGEM(() -> movFGEM("2026-01-01", INVENTARIO_INICIAL, VACA, 5, null));
    }

    @Test
    void registraElHatoYCalculaLasExistenciasFGEM() {
        movFGEM("2025-06-01", INVENTARIO_INICIAL, VACA, 10, null);
        movFGEM("2025-06-01", INVENTARIO_INICIAL, TORO, 1, null);
        movFGEM("2025-09-10", NACIMIENTO, TERNERA, 3, null);
        movFGEM("2025-12-01", CAMBIO_CATEGORIA, TERNERA, 1, VAQUILLONA);
        MovimientoGanadoFGEM ventaFGEM = servicioFGEM.registrarMovimientoFGEM(new DatosMovimientoGanadoFGEM(HOY_FGEM, VENTA, VACA, 2, null,
                new BigDecimal("5000"), "ignorada", "Feria de Huancayo"));

        assertNull(ventaFGEM.causaFGEM());                    // la causa solo aplica a muertes y pérdidas
        assertEquals("admin", ventaFGEM.registradoPorFGEM());
        Map<CategoriaGanadoFGEM, Integer> hatoFGEM = existenciasFGEM();
        assertEquals(Map.of(VACA, 8, TORO, 1, TERNERA, 2, VAQUILLONA, 1), hatoFGEM);
        assertEquals(5, servicioFGEM.listarMovimientosFGEM(LocalDate.of(2025, 1, 1), HOY_FGEM, false).size());
        assertEquals(VENTA, servicioFGEM.listarMovimientosFGEM(LocalDate.of(2025, 1, 1), HOY_FGEM, false).get(0).tipoFGEM());
    }

    @Test
    void noPermiteQuedarConExistenciasNegativasFGEM() {
        movFGEM("2025-06-01", INVENTARIO_INICIAL, VACA, 5, null);
        movFGEM("2025-12-01", VENTA, VACA, 5, null);

        // Vender antes de la venta de diciembre dejaría sin vacas en esa fecha.
        ValidacionExceptionFGEM eFGEM = assertThrows(ValidacionExceptionFGEM.class,
                () -> movFGEM("2025-08-01", MUERTE, VACA, 1, null));
        assertTrue(eFGEM.getMessage().contains("faltarían 1"), eFGEM.getMessage());
        assertInvalidoFGEM(() -> movFGEM("2025-07-01", VENTA, TORO, 1, null));
        // Y no se registró nada de lo rechazado.
        assertEquals(2, servicioFGEM.listarMovimientosFGEM(LocalDate.of(2025, 1, 1), HOY_FGEM, false).size());
    }

    @Test
    void anularUnaEntradaNoPuedeDejarSinAnimalesFGEM() {
        MovimientoGanadoFGEM inicialFGEM = movFGEM("2025-06-01", INVENTARIO_INICIAL, OVEJA, 10, null);
        MovimientoGanadoFGEM muerteFGEM = movFGEM("2025-07-01", MUERTE, OVEJA, 4, null);

        assertInvalidoFGEM(() -> servicioFGEM.anularMovimientoFGEM(inicialFGEM.idFGEM(), "Error de registro"));
        MovimientoGanadoFGEM anuladaFGEM = servicioFGEM.anularMovimientoFGEM(muerteFGEM.idFGEM(), "Error de registro");

        assertTrue(anuladaFGEM.anuladoFGEM());
        assertEquals(Map.of(OVEJA, 10), existenciasFGEM());
        assertInvalidoFGEM(() -> servicioFGEM.anularMovimientoFGEM(muerteFGEM.idFGEM(), "Otra vez"));
    }

    @Test
    void corregirUnMovimientoRecalculaLasExistenciasFGEM() {
        MovimientoGanadoFGEM inicialFGEM = movFGEM("2025-06-01", INVENTARIO_INICIAL, OVEJA, 10, null);
        MovimientoGanadoFGEM muerteFGEM = movFGEM("2025-07-01", MUERTE, OVEJA, 4, null);

        MovimientoGanadoFGEM corregidoFGEM = servicioFGEM.corregirMovimientoFGEM(muerteFGEM.idFGEM(), new DatosMovimientoGanadoFGEM(
                LocalDate.parse("2025-07-01"), MUERTE, OVEJA, 2, null, null, "Neumonía", null));
        assertEquals(2, corregidoFGEM.cantidadFGEM());
        assertEquals(Map.of(OVEJA, 8), existenciasFGEM());
        assertTrue(servicioFGEM.listarMovimientosFGEM(LocalDate.of(2025, 1, 1), HOY_FGEM, true).stream()
                .anyMatch(mFGEM -> mFGEM.idFGEM() == muerteFGEM.idFGEM() && mFGEM.anuladoFGEM()));

        // Bajar el inventario inicial a 1 dejaría la muerte de 2 sin animales: no se guarda nada.
        assertInvalidoFGEM(() -> servicioFGEM.corregirMovimientoFGEM(inicialFGEM.idFGEM(), new DatosMovimientoGanadoFGEM(
                LocalDate.parse("2025-06-01"), INVENTARIO_INICIAL, OVEJA, 1, null, null, null, null)));
        assertEquals(Map.of(OVEJA, 8), existenciasFGEM());
        assertInvalidoFGEM(() -> servicioFGEM.corregirMovimientoFGEM(muerteFGEM.idFGEM(), new DatosMovimientoGanadoFGEM(
                LocalDate.parse("2025-07-01"), MUERTE, OVEJA, 1, null, null, null, null)));
    }

    @Test
    void corregirLaProduccionFGEM() {
        movFGEM("2025-06-01", INVENTARIO_INICIAL, VACA, 10, null);
        ProduccionGanaderaFGEM lecheFGEM = servicioFGEM.registrarProduccionFGEM(new DatosProduccionFGEM(EspecieFGEM.VACUNO,
                ProductoGanaderoFGEM.LECHE, LocalDate.of(2025, 12, 31), 31, new BigDecimal("1550"), 10, null, null));
        ProduccionGanaderaFGEM corregidaFGEM = servicioFGEM.corregirProduccionFGEM(lecheFGEM.idFGEM(), new DatosProduccionFGEM(EspecieFGEM.VACUNO,
                ProductoGanaderoFGEM.LECHE, LocalDate.of(2025, 12, 31), 31, new BigDecimal("1650"), 10, null, null));
        assertEquals(0, new BigDecimal("1650").compareTo(corregidaFGEM.cantidadFGEM()));
        assertEquals(1, servicioFGEM.listarProduccionFGEM(LocalDate.of(2025, 1, 1), HOY_FGEM, false).size());
    }

    @Test
    void validaLosMovimientosFGEM() {
        movFGEM("2025-06-01", INVENTARIO_INICIAL, ALPACA_MADRE, 30, null);
        assertInvalidoFGEM(() -> movFGEM("2025-07-01", NACIMIENTO, ALPACA_MADRE, 1, null));          // no es cría
        assertInvalidoFGEM(() -> movFGEM("2025-07-01", CAMBIO_CATEGORIA, ALPACA_MADRE, 1, null));    // sin destino
        assertInvalidoFGEM(() -> movFGEM("2025-07-01", CAMBIO_CATEGORIA, ALPACA_MADRE, 1, LLAMA_MADRE)); // otra especie
        assertInvalidoFGEM(() -> movFGEM("2026-02-01", NACIMIENTO, ALPACA_CRIA, 1, null));           // fecha futura
        assertInvalidoFGEM(() -> movFGEM("2025-07-01", NACIMIENTO, ALPACA_CRIA, 0, null));           // cantidad
    }

    @Test
    void registraLaProduccionYValidaElProductoFGEM() {
        movFGEM("2025-06-01", INVENTARIO_INICIAL, VACA, 10, null);
        ProduccionGanaderaFGEM lecheFGEM = servicioFGEM.registrarProduccionFGEM(new DatosProduccionFGEM(EspecieFGEM.VACUNO,
                ProductoGanaderoFGEM.LECHE, LocalDate.of(2025, 12, 31), 31, new BigDecimal("1550"), 10,
                new BigDecimal("1.25"), null));
        assertEquals(31, lecheFGEM.diasFGEM());
        assertEquals(0, new BigDecimal("1937.50").compareTo(lecheFGEM.valorFGEM()));

        assertInvalidoFGEM(() -> servicioFGEM.registrarProduccionFGEM(new DatosProduccionFGEM(EspecieFGEM.VACUNO, ProductoGanaderoFGEM.FIBRA,
                HOY_FGEM, 1, BigDecimal.TEN, null, null, null)));
        assertInvalidoFGEM(() -> servicioFGEM.registrarProduccionFGEM(new DatosProduccionFGEM(EspecieFGEM.ALPACA, ProductoGanaderoFGEM.FIBRA,
                HOY_FGEM, 1, BigDecimal.ZERO, null, null, null)));

        servicioFGEM.anularProduccionFGEM(lecheFGEM.idFGEM());
        assertEquals(0, servicioFGEM.listarProduccionFGEM(LocalDate.of(2025, 1, 1), HOY_FGEM, false).size());
        assertEquals(1, servicioFGEM.listarProduccionFGEM(LocalDate.of(2025, 1, 1), HOY_FGEM, true).size());
    }

    @Test
    void elValorDelHatoEntraAlBalanceGeneralFGEM() {
        movFGEM("2025-06-01", INVENTARIO_INICIAL, VACA, 10, null);
        movFGEM("2025-06-01", INVENTARIO_INICIAL, TERNERO, 4, null);
        Map<CategoriaGanadoFGEM, BigDecimal> preciosFGEM = new EnumMap<>(CategoriaGanadoFGEM.class);
        preciosFGEM.put(VACA, new BigDecimal("2500"));
        preciosFGEM.put(TERNERO, new BigDecimal("900"));
        servicioFGEM.guardarPreciosFGEM(preciosFGEM);

        List<ExistenciaValorizadaFGEM> hatoFGEM = servicioFGEM.hatoFGEM();
        assertEquals(0, new BigDecimal("28600").compareTo(ExistenciaValorizadaFGEM.totalFGEM(hatoFGEM)));

        BalanceGeneralFGEM bFGEM = entornoFGEM.patrimonioFGEM.situacionFGEM().balanceFGEM();
        assertEquals(0, new BigDecimal("28600").compareTo(bFGEM.ganadoFGEM()));
        assertTrue(bFGEM.activoNoCorrienteFGEM().stream().anyMatch(pFGEM -> pFGEM.conceptoFGEM().equals(BalanceGeneralFGEM.GANADO_FGEM)));
        assertTrue(bFGEM.tieneActivosFGEM());

        // Borrar un precio deja la categoría sin valorizar.
        preciosFGEM.put(TERNERO, null);
        servicioFGEM.guardarPreciosFGEM(preciosFGEM);
        assertEquals(0, new BigDecimal("25000").compareTo(ExistenciaValorizadaFGEM.totalFGEM(servicioFGEM.hatoFGEM())));
    }

    @Test
    void calculaLosIndicadoresDelPeriodoFGEM() {
        movFGEM("2024-12-01", INVENTARIO_INICIAL, ALPACA_MADRE, 50, null);
        movFGEM("2025-02-01", NACIMIENTO, ALPACA_CRIA, 30, null);
        movFGEM("2025-03-01", MUERTE, ALPACA_CRIA, 3, null);

        List<IndicadoresEspecieFGEM> listaFGEM = servicioFGEM.indicadoresFGEM(new PeriodoFGEM(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));

        assertEquals(1, listaFGEM.size());
        IndicadoresEspecieFGEM alpacasFGEM = listaFGEM.get(0);
        assertEquals(EspecieFGEM.ALPACA, alpacasFGEM.especieFGEM());
        assertEquals(0, new BigDecimal("0.6").compareTo(alpacasFGEM.natalidadFGEM()));
        assertEquals(0, new BigDecimal("0.1").compareTo(alpacasFGEM.mortalidadCriasFGEM()));
    }

    @Test
    void respetaLosPermisosFGEM() {
        movFGEM("2025-06-01", INVENTARIO_INICIAL, VACA, 10, null);
        UsuarioFGEM analistaFGEM = entornoFGEM.crearUsuarioFGEM("analista", "ANALISTA");
        UsuarioFGEM productorFGEM = entornoFGEM.crearUsuarioFGEM("productor", "OPERADOR");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(fundoFGEM.idFGEM(), Set.of(analistaFGEM.idFGEM(), productorFGEM.idFGEM()));

        entornoFGEM.entrarComoFGEM("analista", "Temporal123");
        entornoFGEM.empresaServiceFGEM.restaurarEmpresaActivaFGEM();
        entornoFGEM.empresaServiceFGEM.seleccionarEmpresaActivaFGEM(fundoFGEM.idFGEM());
        assertEquals(1, servicioFGEM.hatoFGEM().size());
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> movFGEM("2025-07-01", NACIMIENTO, TERNERA, 1, null));

        // El operador (el productor) sí registra.
        entornoFGEM.entrarComoFGEM("productor", "Temporal123");
        entornoFGEM.empresaServiceFGEM.seleccionarEmpresaActivaFGEM(fundoFGEM.idFGEM());
        movFGEM("2025-07-01", NACIMIENTO, TERNERA, 1, null);
        assertEquals(2, servicioFGEM.hatoFGEM().size());
    }

    // ------------------------------------------------------------------ utilidades

    private MovimientoGanadoFGEM movFGEM(String fechaFGEM, TipoMovimientoGanadoFGEM tipoFGEM, CategoriaGanadoFGEM categoriaFGEM, int cantidadFGEM,
                                 CategoriaGanadoFGEM destinoFGEM) {
        return servicioFGEM.registrarMovimientoFGEM(new DatosMovimientoGanadoFGEM(LocalDate.parse(fechaFGEM), tipoFGEM, categoriaFGEM, cantidadFGEM,
                destinoFGEM, null, null, null));
    }

    private Map<CategoriaGanadoFGEM, Integer> existenciasFGEM() {
        Map<CategoriaGanadoFGEM, Integer> mapaFGEM = new EnumMap<>(CategoriaGanadoFGEM.class);
        servicioFGEM.hatoFGEM().forEach(eFGEM -> mapaFGEM.put(eFGEM.categoriaFGEM(), eFGEM.cabezasFGEM()));
        return mapaFGEM;
    }

    private static void assertInvalidoFGEM(Runnable accionFGEM) {
        assertThrows(ValidacionExceptionFGEM.class, accionFGEM::run);
    }
}
