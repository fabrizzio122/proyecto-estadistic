package com.businessanalyzer.analytics.ganaderia;

import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.diagnostico.Semaforo;
import com.businessanalyzer.model.CategoriaGanado;
import com.businessanalyzer.model.Especie;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.MovimientoGanado;
import com.businessanalyzer.model.ProduccionGanadera;
import com.businessanalyzer.model.ProductoGanadero;
import com.businessanalyzer.model.TipoMovimientoGanado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.businessanalyzer.model.CategoriaGanado.*;
import static com.businessanalyzer.model.TipoMovimientoGanado.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndicadoresGanaderiaTest {

    private static final Periodo ANIO_2025 = new Periodo(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
    private long secuencia;

    @Test
    void calculaLasExistenciasConCambiosDeCategoria() {
        List<MovimientoGanado> movs = List.of(
                mov("2025-01-01", INVENTARIO_INICIAL, TERNERA, 5, null),
                mov("2025-03-01", NACIMIENTO, TERNERA, 2, null),
                mov("2025-06-01", CAMBIO_CATEGORIA, TERNERA, 4, VAQUILLONA),
                mov("2025-07-01", VENTA, VAQUILLONA, 1, null));

        assertEquals(Map.of(TERNERA, 7), Hato.existencias(movs, LocalDate.of(2025, 5, 31)));
        assertEquals(Map.of(TERNERA, 3, VAQUILLONA, 3), Hato.existencias(movs, LocalDate.of(2025, 12, 31)));
    }

    @Test
    void detectaCuandoFaltanAnimales() {
        List<MovimientoGanado> movs = new ArrayList<>(List.of(
                mov("2025-01-01", INVENTARIO_INICIAL, VACA, 3, null),
                mov("2025-05-01", VENTA, VACA, 3, null)));
        assertTrue(Hato.primerFaltante(movs).isEmpty());

        // Una venta anterior que deja sin vacas para la venta de mayo.
        movs.add(mov("2025-02-01", VENTA, VACA, 1, null));
        Hato.Faltante f = Hato.primerFaltante(movs).orElseThrow();
        assertEquals(VACA, f.categoria());
        assertEquals(LocalDate.of(2025, 5, 1), f.fecha());
        assertEquals(1, f.cantidad());

        // El mismo día cuentan primero las entradas.
        assertTrue(Hato.primerFaltante(List.of(
                mov("2025-03-01", VENTA, TERNERO, 1, null),
                mov("2025-03-01", NACIMIENTO, TERNERO, 1, null))).isEmpty());
    }

    @Test
    void calculaNatalidadMortalidadYExtraccion() {
        List<MovimientoGanado> movs = List.of(
                mov("2024-06-01", INVENTARIO_INICIAL, VACA, 20, null),
                mov("2024-06-01", INVENTARIO_INICIAL, TORO, 1, null),
                mov("2024-06-01", INVENTARIO_INICIAL, TORETE, 4, null),
                mov("2025-02-01", NACIMIENTO, TERNERA, 8, null),
                mov("2025-03-01", NACIMIENTO, TERNERO, 6, null),
                mov("2025-04-01", MUERTE, TERNERO, 1, null),
                mov("2025-05-01", MUERTE, VACA, 1, null),
                mov("2025-06-01", COMPRA, VACA, 2, null),
                mov("2025-08-01", VENTA, TORETE, 3, null),
                mov("2025-09-01", CONSUMO, TORETE, 1, null),
                mov("2026-01-05", NACIMIENTO, TERNERA, 1, null)); // fuera del período

        IndicadoresEspecie i = IndicadoresEspecie.calcular(Especie.VACUNO, ANIO_2025, movs, List.of());

        assertEquals(25, i.existenciaInicial());
        assertEquals(20, i.hembrasInicio());
        assertEquals(14, i.nacimientos());
        assertEquals(35, i.existenciaFinal());        // 25 + 14 nacimientos + 2 compras − 2 muertes − 3 ventas − 1 consumo
        igual("0.7", i.natalidad());                   // 14 / 20
        igual("0.04878", i.mortalidad(), "0.00001");   // 2 / (25 + 14 + 2)
        igual("0.071429", i.mortalidadCrias());        // 1 / 14
        igual("0.16", i.extraccion());                 // (3 + 1) / 25
        igual("0.4", i.crecimiento());                 // (35 − 25) / 25
        igual("21", i.hembrasPorMacho());              // 21 vacas, 1 toro

        List<IndicadorGanadero> evaluados = i.evaluar(Moneda.PEN);
        assertEquals(Semaforo.VERDE, buscar(evaluados, "Natalidad").semaforo());      // 70 % = umbral verde
        assertEquals(Semaforo.AMARILLO, buscar(evaluados, "Mortalidad").semaforo());  // 4.9 %: entre 3 % y 6 %
        assertEquals(Semaforo.AMARILLO, buscar(evaluados, "Mortalidad de crías").semaforo());
    }

    @Test
    void unInventarioInicialDentroDelPeriodoCuentaComoExistenciaInicial() {
        List<MovimientoGanado> movs = List.of(
                mov("2025-03-01", INVENTARIO_INICIAL, ALPACA_MADRE, 40, null),
                mov("2025-03-01", INVENTARIO_INICIAL, ALPACA_PADRE, 2, null),
                mov("2025-04-01", NACIMIENTO, ALPACA_CRIA, 20, null),
                mov("2025-05-01", MUERTE, ALPACA_CRIA, 6, null));

        IndicadoresEspecie i = IndicadoresEspecie.calcular(Especie.ALPACA, ANIO_2025, movs, List.of());

        assertEquals(42, i.existenciaInicial());
        assertEquals(0, i.compras());
        igual("0.5", i.natalidad());
        igual("0.3", i.mortalidadCrias());
        List<IndicadorGanadero> evaluados = i.evaluar(Moneda.PEN);
        assertEquals(Semaforo.AMARILLO, buscar(evaluados, "Natalidad").semaforo()); // 50 %: entre 45 % y 60 %
        assertEquals(Semaforo.ROJO, buscar(evaluados, "Mortalidad de crías").semaforo());
    }

    @Test
    void promediaLaProduccionPorAnimalYDia() {
        List<MovimientoGanado> movs = List.of(mov("2024-01-01", INVENTARIO_INICIAL, VACA, 10, null));
        List<ProduccionGanadera> produccion = List.of(
                prod(ProductoGanadero.LECHE, "2025-01-31", 30, "1800", 10, "1.20"),  // 6 l por vaca al día
                prod(ProductoGanadero.LECHE, "2025-02-28", 28, "1400", 10, null),    // 5 l
                prod(ProductoGanadero.LECHE, "2025-03-01", 1, "50", null, null));    // sin animales: no entra al promedio

        IndicadoresEspecie.ResumenProduccion leche = IndicadoresEspecie.calcular(Especie.VACUNO, ANIO_2025, movs,
                produccion).produccion().get(0);

        igual("3250", leche.cantidad());
        igual("5.517241", leche.promedio());   // 3 200 / (300 + 280)
        igual("2160", leche.valor());
        assertEquals("5.52 l por vaca al día", leche.promedioTexto());
    }

    @Test
    void sinHembrasLaNatalidadNoSeEvalua() {
        List<MovimientoGanado> movs = List.of(mov("2024-01-01", INVENTARIO_INICIAL, BORREGO, 5, null));
        IndicadoresEspecie i = IndicadoresEspecie.calcular(Especie.OVINO, ANIO_2025, movs, List.of());
        assertNull(i.natalidad());
        assertEquals(Semaforo.SIN_DATOS, buscar(i.evaluar(Moneda.PEN), "Natalidad").semaforo());
        assertTrue(i.tieneDatos());
        assertFalse(IndicadoresEspecie.calcular(Especie.LLAMA, ANIO_2025, movs, List.of()).tieneDatos());
    }

    @Test
    void valorizaElHatoSoloConLasCategoriasConPrecio() {
        List<ExistenciaValorizada> filas = ExistenciaValorizada.de(Map.of(VACA, 10, TERNERO, 4, TORO, 1),
                Map.of(VACA, new BigDecimal("2500"), TORO, new BigDecimal("4000")));
        assertEquals(List.of(VACA, TERNERO, TORO), filas.stream().map(ExistenciaValorizada::categoria).toList());
        assertNull(filas.get(1).valor());
        igual("29000", ExistenciaValorizada.total(filas));
    }

    // ------------------------------------------------------------------ utilidades

    private MovimientoGanado mov(String fecha, TipoMovimientoGanado tipo, CategoriaGanado categoria, int cantidad,
                                 CategoriaGanado destino) {
        return new MovimientoGanado(++secuencia, 1, LocalDate.parse(fecha), tipo, categoria, cantidad, destino, null,
                null, null, false, null, "admin", null);
    }

    private ProduccionGanadera prod(ProductoGanadero producto, String fecha, int dias, String cantidad, Integer animales,
                                    String precio) {
        return new ProduccionGanadera(++secuencia, 1, Especie.VACUNO, producto, LocalDate.parse(fecha), dias,
                new BigDecimal(cantidad), animales, precio == null ? null : new BigDecimal(precio), null, false,
                "admin", null);
    }

    private static IndicadorGanadero buscar(List<IndicadorGanadero> lista, String nombre) {
        return lista.stream().filter(i -> i.nombre().equals(nombre)).findFirst().orElseThrow();
    }

    private static void igual(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }

    private static void igual(String esperado, BigDecimal real, String tolerancia) {
        assertTrue(new BigDecimal(esperado).subtract(real).abs().compareTo(new BigDecimal(tolerancia)) <= 0,
                "esperado " + esperado + " pero fue " + real);
    }
}
