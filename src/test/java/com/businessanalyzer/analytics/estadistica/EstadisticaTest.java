package com.businessanalyzer.analytics.estadistica;

import com.businessanalyzer.analytics.RentabilidadProducto;
import com.businessanalyzer.model.TipoItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Casos de libro con resultados conocidos para cada técnica estadística. */
class EstadisticaTest {

    private static final double TOLERANCIA = 1e-4;

    @Test
    void descriptivaDeUnaMuestraConocida() {
        EstadisticaDescriptiva e = EstadisticaDescriptiva.de(new double[]{2, 4, 4, 4, 5, 5, 7, 9});

        assertEquals(8, e.n());
        assertEquals(5.0, e.media(), TOLERANCIA);
        assertEquals(4.5, e.mediana(), TOLERANCIA);
        assertEquals(Math.sqrt(32.0 / 7), e.desviacion(), TOLERANCIA, "Desviación muestral (n − 1)");
        assertEquals(2.0, e.minimo(), TOLERANCIA);
        assertEquals(9.0, e.maximo(), TOLERANCIA);
        assertEquals(4.0, e.q1(), TOLERANCIA);
        assertEquals(6.5, e.q3(), TOLERANCIA);
        assertEquals(Math.sqrt(32.0 / 7) / 5, e.coeficienteVariacion(), TOLERANCIA);
        assertTrue(e.asimetria() > 0, "La cola derecha (7, 9) hace la asimetría positiva");
    }

    @Test
    void descriptivaConPocosDatos() {
        assertTrue(EstadisticaDescriptiva.de(new double[0]).vacia());
        EstadisticaDescriptiva uno = EstadisticaDescriptiva.de(new double[]{50});
        assertEquals(50, uno.media(), TOLERANCIA);
        assertTrue(Double.isNaN(uno.desviacion()));
        assertTrue(Double.isNaN(uno.coeficienteVariacion()));
    }

    @Test
    void tendenciaCrecientePerfecta() {
        Tendencia t = Tendencia.de(new double[]{10, 12, 14, 16});

        assertEquals(2.0, t.pendiente(), TOLERANCIA);
        assertEquals(10.0, t.intercepto(), TOLERANCIA);
        assertEquals(1.0, t.r2(), TOLERANCIA);
        assertEquals(Tendencia.Direccion.CRECIENTE, t.direccion());
        assertEquals(2.0 / 13, t.crecimientoMensual(), TOLERANCIA);
        assertEquals(18.0, t.valorAjustado(4), TOLERANCIA);
    }

    @Test
    void tendenciaDecrecienteEstableEInsuficiente() {
        assertEquals(Tendencia.Direccion.DECRECIENTE, Tendencia.de(new double[]{50, 40, 30, 20}).direccion());

        Tendencia ruido = Tendencia.de(new double[]{100, 90, 110, 95, 105});
        assertEquals(1.5, ruido.pendiente(), TOLERANCIA);
        assertTrue(ruido.significancia() > 0.05);
        assertEquals(Tendencia.Direccion.ESTABLE, ruido.direccion(), "Una pendiente pequeña con mucho ruido no es tendencia");

        assertEquals(Tendencia.Direccion.INSUFICIENTE, Tendencia.de(new double[]{1, 2}).direccion());
    }

    @Test
    void clasificacionAbc() {
        AnalisisAbc abc = AnalisisAbc.de(List.of(producto("D", "40"), producto("A", "700"),
                producto("C", "60"), producto("B", "200")));

        assertEquals(List.of("A", "B", "C", "D"), abc.items().stream().map(i -> i.producto().nombre()).toList());
        assertEquals(List.of(AnalisisAbc.Clase.A, AnalisisAbc.Clase.A, AnalisisAbc.Clase.B, AnalisisAbc.Clase.C),
                abc.items().stream().map(AnalisisAbc.Item::clase).toList());
        assertEquals(0, new BigDecimal("0.9").compareTo(abc.participacion(AnalisisAbc.Clase.A)));
        assertEquals(0, BigDecimal.ONE.compareTo(abc.items().get(3).acumulado()));
        assertTrue(AnalisisAbc.de(List.of()).items().isEmpty());
    }

    @Test
    void correlacionDePearson() {
        Map<String, double[]> series = new LinkedHashMap<>();
        series.put("x", new double[]{1, 2, 3, 4, 5});
        series.put("doble", new double[]{2, 4, 6, 8, 10});
        series.put("inversa", new double[]{-1, -2, -3, -4, -5});
        series.put("constante", new double[]{7, 7, 7, 7, 7});

        MatrizCorrelacion m = MatrizCorrelacion.de(series);

        assertEquals(1.0, m.r(0, 1), TOLERANCIA);
        assertEquals(-1.0, m.r(0, 2), TOLERANCIA);
        assertTrue(Double.isNaN(m.r(0, 3)), "Una serie constante no tiene correlación");
        assertEquals(3, m.paresOrdenados().size(), "Los pares con la constante se omiten");
        assertEquals("fuerte", MatrizCorrelacion.fuerza(-0.85));
        assertEquals("moderada", MatrizCorrelacion.fuerza(0.5));
        assertEquals("nula", MatrizCorrelacion.fuerza(0.1));

        Map<String, double[]> pocos = new LinkedHashMap<>();
        pocos.put("a", new double[]{1, 2});
        pocos.put("b", new double[]{3, 4});
        assertTrue(Double.isNaN(MatrizCorrelacion.de(pocos).r(0, 1)), "Con menos de 3 datos no se calcula");
    }

    @Test
    void histogramaConReglaDeSturges() {
        Histograma h = Histograma.de(new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, 10);

        assertEquals(5, h.clases().size(), "1 + log2(10) = 4.3 -> 5 clases");
        h.clases().forEach(c -> assertEquals(2, c.frecuencia()));
        assertEquals(1, Histograma.de(new double[]{3, 3, 3}, 10).clases().size());
        assertTrue(Histograma.de(new double[0], 10).clases().isEmpty());
    }

    @Test
    void estacionalidadPorDiaDeLaSemana() {
        Map<LocalDate, Double> dias = new TreeMap<>();
        LocalDate lunes = LocalDate.of(2026, 1, 5);
        for (int i = 0; i < 14; i++) {
            LocalDate dia = lunes.plusDays(i);
            double venta = switch (dia.getDayOfWeek()) {
                case MONDAY -> 100;
                case SATURDAY -> 300;
                default -> 0;
            };
            dias.put(dia, venta);
        }

        EstacionalidadSemanal e = EstacionalidadSemanal.de(dias);

        assertEquals(800.0 / 14, e.promedioGeneral(), TOLERANCIA);
        assertEquals(DayOfWeek.SATURDAY, e.mejorDia());
        assertEquals(DayOfWeek.TUESDAY, e.peorDia(), "En empate se elige el primero de la semana");
        assertEquals(300 / (800.0 / 14), e.indice(DayOfWeek.SATURDAY), TOLERANCIA);
        assertEquals(2, e.diasObservados().get(DayOfWeek.MONDAY));
    }

    private static RentabilidadProducto producto(String nombre, String ventas) {
        return new RentabilidadProducto(nombre.hashCode(), nombre, nombre, TipoItem.PRODUCTO, null,
                BigDecimal.ONE, new BigDecimal(ventas), BigDecimal.ZERO);
    }
}
