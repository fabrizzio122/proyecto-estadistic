package com.businessanalyzer.analytics.ganaderia;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;
import com.businessanalyzer.model.CategoriaGanadoFGEM;
import com.businessanalyzer.model.EspecieFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.MovimientoGanadoFGEM;
import com.businessanalyzer.model.ProduccionGanaderaFGEM;
import com.businessanalyzer.model.ProductoGanaderoFGEM;
import com.businessanalyzer.model.TipoMovimientoGanadoFGEM;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.businessanalyzer.model.CategoriaGanadoFGEM.*;
import static com.businessanalyzer.model.TipoMovimientoGanadoFGEM.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndicadoresGanaderiaTestFGEM {

    private static final PeriodoFGEM ANIO_2025_FGEM = new PeriodoFGEM(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
    private long secuenciaFGEM;

    @Test
    void calculaLasExistenciasConCambiosDeCategoriaFGEM() {
        List<MovimientoGanadoFGEM> movsFGEM = List.of(
                movFGEM("2025-01-01", INVENTARIO_INICIAL, TERNERA, 5, null),
                movFGEM("2025-03-01", NACIMIENTO, TERNERA, 2, null),
                movFGEM("2025-06-01", CAMBIO_CATEGORIA, TERNERA, 4, VAQUILLONA),
                movFGEM("2025-07-01", VENTA, VAQUILLONA, 1, null));

        assertEquals(Map.of(TERNERA, 7), HatoFGEM.existenciasFGEM(movsFGEM, LocalDate.of(2025, 5, 31)));
        assertEquals(Map.of(TERNERA, 3, VAQUILLONA, 3), HatoFGEM.existenciasFGEM(movsFGEM, LocalDate.of(2025, 12, 31)));
    }

    @Test
    void detectaCuandoFaltanAnimalesFGEM() {
        List<MovimientoGanadoFGEM> movsFGEM = new ArrayList<>(List.of(
                movFGEM("2025-01-01", INVENTARIO_INICIAL, VACA, 3, null),
                movFGEM("2025-05-01", VENTA, VACA, 3, null)));
        assertTrue(HatoFGEM.primerFaltanteFGEM(movsFGEM).isEmpty());

        // Una venta anterior que deja sin vacas para la venta de mayo.
        movsFGEM.add(movFGEM("2025-02-01", VENTA, VACA, 1, null));
        HatoFGEM.FaltanteFGEM fFGEM = HatoFGEM.primerFaltanteFGEM(movsFGEM).orElseThrow();
        assertEquals(VACA, fFGEM.categoriaFGEM());
        assertEquals(LocalDate.of(2025, 5, 1), fFGEM.fechaFGEM());
        assertEquals(1, fFGEM.cantidadFGEM());

        // El mismo día cuentan primero las entradas.
        assertTrue(HatoFGEM.primerFaltanteFGEM(List.of(
                movFGEM("2025-03-01", VENTA, TERNERO, 1, null),
                movFGEM("2025-03-01", NACIMIENTO, TERNERO, 1, null))).isEmpty());
    }

    @Test
    void calculaNatalidadMortalidadYExtraccionFGEM() {
        List<MovimientoGanadoFGEM> movsFGEM = List.of(
                movFGEM("2024-06-01", INVENTARIO_INICIAL, VACA, 20, null),
                movFGEM("2024-06-01", INVENTARIO_INICIAL, TORO, 1, null),
                movFGEM("2024-06-01", INVENTARIO_INICIAL, TORETE, 4, null),
                movFGEM("2025-02-01", NACIMIENTO, TERNERA, 8, null),
                movFGEM("2025-03-01", NACIMIENTO, TERNERO, 6, null),
                movFGEM("2025-04-01", MUERTE, TERNERO, 1, null),
                movFGEM("2025-05-01", MUERTE, VACA, 1, null),
                movFGEM("2025-06-01", COMPRA, VACA, 2, null),
                movFGEM("2025-08-01", VENTA, TORETE, 3, null),
                movFGEM("2025-09-01", CONSUMO, TORETE, 1, null),
                movFGEM("2026-01-05", NACIMIENTO, TERNERA, 1, null)); // fuera del período

        IndicadoresEspecieFGEM iFGEM = IndicadoresEspecieFGEM.calcularFGEM(EspecieFGEM.VACUNO, ANIO_2025_FGEM, movsFGEM, List.of());

        assertEquals(25, iFGEM.existenciaInicialFGEM());
        assertEquals(20, iFGEM.hembrasInicioFGEM());
        assertEquals(14, iFGEM.nacimientosFGEM());
        assertEquals(35, iFGEM.existenciaFinalFGEM());        // 25 + 14 nacimientos + 2 compras − 2 muertes − 3 ventas − 1 consumo
        igualFGEM("0.7", iFGEM.natalidadFGEM());                   // 14 / 20
        igualFGEM("0.04878", iFGEM.mortalidadFGEM(), "0.00001");   // 2 / (25 + 14 + 2)
        igualFGEM("0.071429", iFGEM.mortalidadCriasFGEM());        // 1 / 14
        igualFGEM("0.16", iFGEM.extraccionFGEM());                 // (3 + 1) / 25
        igualFGEM("0.4", iFGEM.crecimientoFGEM());                 // (35 − 25) / 25
        igualFGEM("21", iFGEM.hembrasPorMachoFGEM());              // 21 vacas, 1 toro

        List<IndicadorGanaderoFGEM> evaluadosFGEM = iFGEM.evaluarFGEM(MonedaFGEM.PEN);
        assertEquals(SemaforoFGEM.VERDE, buscarFGEM(evaluadosFGEM, "Natalidad").semaforoFGEM());      // 70 % = umbral verde
        assertEquals(SemaforoFGEM.AMARILLO, buscarFGEM(evaluadosFGEM, "Mortalidad").semaforoFGEM());  // 4.9 %: entre 3 % y 6 %
        assertEquals(SemaforoFGEM.AMARILLO, buscarFGEM(evaluadosFGEM, "Mortalidad de crías").semaforoFGEM());
    }

    @Test
    void unInventarioInicialDentroDelPeriodoCuentaComoExistenciaInicialFGEM() {
        List<MovimientoGanadoFGEM> movsFGEM = List.of(
                movFGEM("2025-03-01", INVENTARIO_INICIAL, ALPACA_MADRE, 40, null),
                movFGEM("2025-03-01", INVENTARIO_INICIAL, ALPACA_PADRE, 2, null),
                movFGEM("2025-04-01", NACIMIENTO, ALPACA_CRIA, 20, null),
                movFGEM("2025-05-01", MUERTE, ALPACA_CRIA, 6, null));

        IndicadoresEspecieFGEM iFGEM = IndicadoresEspecieFGEM.calcularFGEM(EspecieFGEM.ALPACA, ANIO_2025_FGEM, movsFGEM, List.of());

        assertEquals(42, iFGEM.existenciaInicialFGEM());
        assertEquals(0, iFGEM.comprasFGEM());
        igualFGEM("0.5", iFGEM.natalidadFGEM());
        igualFGEM("0.3", iFGEM.mortalidadCriasFGEM());
        List<IndicadorGanaderoFGEM> evaluadosFGEM = iFGEM.evaluarFGEM(MonedaFGEM.PEN);
        assertEquals(SemaforoFGEM.AMARILLO, buscarFGEM(evaluadosFGEM, "Natalidad").semaforoFGEM()); // 50 %: entre 45 % y 60 %
        assertEquals(SemaforoFGEM.ROJO, buscarFGEM(evaluadosFGEM, "Mortalidad de crías").semaforoFGEM());
    }

    @Test
    void promediaLaProduccionPorAnimalYDiaFGEM() {
        List<MovimientoGanadoFGEM> movsFGEM = List.of(movFGEM("2024-01-01", INVENTARIO_INICIAL, VACA, 10, null));
        List<ProduccionGanaderaFGEM> produccionFGEM = List.of(
                prodFGEM(ProductoGanaderoFGEM.LECHE, "2025-01-31", 30, "1800", 10, "1.20"),  // 6 l por vaca al día
                prodFGEM(ProductoGanaderoFGEM.LECHE, "2025-02-28", 28, "1400", 10, null),    // 5 l
                prodFGEM(ProductoGanaderoFGEM.LECHE, "2025-03-01", 1, "50", null, null));    // sin animales: no entra al promedio

        IndicadoresEspecieFGEM.ResumenProduccionFGEM lecheFGEM = IndicadoresEspecieFGEM.calcularFGEM(EspecieFGEM.VACUNO, ANIO_2025_FGEM, movsFGEM,
                produccionFGEM).produccionFGEM().get(0);

        igualFGEM("3250", lecheFGEM.cantidadFGEM());
        igualFGEM("5.517241", lecheFGEM.promedioFGEM());   // 3 200 / (300 + 280)
        igualFGEM("2160", lecheFGEM.valorFGEM());
        assertEquals("5.52 l por vaca al día", lecheFGEM.promedioTextoFGEM());
    }

    @Test
    void sinHembrasLaNatalidadNoSeEvaluaFGEM() {
        List<MovimientoGanadoFGEM> movsFGEM = List.of(movFGEM("2024-01-01", INVENTARIO_INICIAL, BORREGO, 5, null));
        IndicadoresEspecieFGEM iFGEM = IndicadoresEspecieFGEM.calcularFGEM(EspecieFGEM.OVINO, ANIO_2025_FGEM, movsFGEM, List.of());
        assertNull(iFGEM.natalidadFGEM());
        assertEquals(SemaforoFGEM.SIN_DATOS, buscarFGEM(iFGEM.evaluarFGEM(MonedaFGEM.PEN), "Natalidad").semaforoFGEM());
        assertTrue(iFGEM.tieneDatosFGEM());
        assertFalse(IndicadoresEspecieFGEM.calcularFGEM(EspecieFGEM.LLAMA, ANIO_2025_FGEM, movsFGEM, List.of()).tieneDatosFGEM());
    }

    @Test
    void valorizaElHatoSoloConLasCategoriasConPrecioFGEM() {
        List<ExistenciaValorizadaFGEM> filasFGEM = ExistenciaValorizadaFGEM.deFGEM(Map.of(VACA, 10, TERNERO, 4, TORO, 1),
                Map.of(VACA, new BigDecimal("2500"), TORO, new BigDecimal("4000")));
        assertEquals(List.of(VACA, TERNERO, TORO), filasFGEM.stream().map(ExistenciaValorizadaFGEM::categoriaFGEM).toList());
        assertNull(filasFGEM.get(1).valorFGEM());
        igualFGEM("29000", ExistenciaValorizadaFGEM.totalFGEM(filasFGEM));
    }

    // ------------------------------------------------------------------ utilidades

    private MovimientoGanadoFGEM movFGEM(String fechaFGEM, TipoMovimientoGanadoFGEM tipoFGEM, CategoriaGanadoFGEM categoriaFGEM, int cantidadFGEM,
                                 CategoriaGanadoFGEM destinoFGEM) {
        return new MovimientoGanadoFGEM(++secuenciaFGEM, 1, LocalDate.parse(fechaFGEM), tipoFGEM, categoriaFGEM, cantidadFGEM, destinoFGEM, null,
                null, null, false, null, "admin", null);
    }

    private ProduccionGanaderaFGEM prodFGEM(ProductoGanaderoFGEM productoFGEM, String fechaFGEM, int diasFGEM, String cantidadFGEM, Integer animalesFGEM,
                                    String precioFGEM) {
        return new ProduccionGanaderaFGEM(++secuenciaFGEM, 1, EspecieFGEM.VACUNO, productoFGEM, LocalDate.parse(fechaFGEM), diasFGEM,
                new BigDecimal(cantidadFGEM), animalesFGEM, precioFGEM == null ? null : new BigDecimal(precioFGEM), null, false,
                "admin", null);
    }

    private static IndicadorGanaderoFGEM buscarFGEM(List<IndicadorGanaderoFGEM> listaFGEM, String nombreFGEM) {
        return listaFGEM.stream().filter(iFGEM -> iFGEM.nombreFGEM().equals(nombreFGEM)).findFirst().orElseThrow();
    }

    private static void igualFGEM(String esperadoFGEM, BigDecimal realFGEM) {
        assertEquals(0, new BigDecimal(esperadoFGEM).compareTo(realFGEM), "esperado " + esperadoFGEM + " pero fue " + realFGEM);
    }

    private static void igualFGEM(String esperadoFGEM, BigDecimal realFGEM, String toleranciaFGEM) {
        assertTrue(new BigDecimal(esperadoFGEM).subtract(realFGEM).abs().compareTo(new BigDecimal(toleranciaFGEM)) <= 0,
                "esperado " + esperadoFGEM + " pero fue " + realFGEM);
    }
}
