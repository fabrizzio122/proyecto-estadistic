package com.businessanalyzer.analytics.estadistica;

import com.businessanalyzer.analytics.RentabilidadProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
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

class EstadisticaTestFGEM {

    private static final double TOLERANCIA_FGEM = 1e-4;

    @Test
    void descriptivaDeUnaMuestraConocidaFGEM() {
        EstadisticaDescriptivaFGEM eFGEM = EstadisticaDescriptivaFGEM.deFGEM(new double[]{2, 4, 4, 4, 5, 5, 7, 9});

        assertEquals(8, eFGEM.nFGEM());
        assertEquals(5.0, eFGEM.mediaFGEM(), TOLERANCIA_FGEM);
        assertEquals(4.5, eFGEM.medianaFGEM(), TOLERANCIA_FGEM);
        assertEquals(Math.sqrt(32.0 / 7), eFGEM.desviacionFGEM(), TOLERANCIA_FGEM, "Desviación muestral (n − 1)");
        assertEquals(2.0, eFGEM.minimoFGEM(), TOLERANCIA_FGEM);
        assertEquals(9.0, eFGEM.maximoFGEM(), TOLERANCIA_FGEM);
        assertEquals(4.0, eFGEM.q1FGEM(), TOLERANCIA_FGEM);
        assertEquals(6.5, eFGEM.q3FGEM(), TOLERANCIA_FGEM);
        assertEquals(Math.sqrt(32.0 / 7) / 5, eFGEM.coeficienteVariacionFGEM(), TOLERANCIA_FGEM);
        assertTrue(eFGEM.asimetriaFGEM() > 0, "La cola derecha (7, 9) hace la asimetría positiva");
    }

    @Test
    void descriptivaConPocosDatosFGEM() {
        assertTrue(EstadisticaDescriptivaFGEM.deFGEM(new double[0]).vaciaFGEM());
        EstadisticaDescriptivaFGEM unoFGEM = EstadisticaDescriptivaFGEM.deFGEM(new double[]{50});
        assertEquals(50, unoFGEM.mediaFGEM(), TOLERANCIA_FGEM);
        assertTrue(Double.isNaN(unoFGEM.desviacionFGEM()));
        assertTrue(Double.isNaN(unoFGEM.coeficienteVariacionFGEM()));
    }

    @Test
    void tendenciaCrecientePerfectaFGEM() {
        TendenciaFGEM tFGEM = TendenciaFGEM.deFGEM(new double[]{10, 12, 14, 16});

        assertEquals(2.0, tFGEM.pendienteFGEM(), TOLERANCIA_FGEM);
        assertEquals(10.0, tFGEM.interceptoFGEM(), TOLERANCIA_FGEM);
        assertEquals(1.0, tFGEM.r2FGEM(), TOLERANCIA_FGEM);
        assertEquals(TendenciaFGEM.DireccionFGEM.CRECIENTE, tFGEM.direccionFGEM());
        assertEquals(2.0 / 13, tFGEM.crecimientoMensualFGEM(), TOLERANCIA_FGEM);
        assertEquals(18.0, tFGEM.valorAjustadoFGEM(4), TOLERANCIA_FGEM);
    }

    @Test
    void tendenciaDecrecienteEstableEInsuficienteFGEM() {
        assertEquals(TendenciaFGEM.DireccionFGEM.DECRECIENTE, TendenciaFGEM.deFGEM(new double[]{50, 40, 30, 20}).direccionFGEM());

        TendenciaFGEM ruidoFGEM = TendenciaFGEM.deFGEM(new double[]{100, 90, 110, 95, 105});
        assertEquals(1.5, ruidoFGEM.pendienteFGEM(), TOLERANCIA_FGEM);
        assertTrue(ruidoFGEM.significanciaFGEM() > 0.05);
        assertEquals(TendenciaFGEM.DireccionFGEM.ESTABLE, ruidoFGEM.direccionFGEM(), "Una pendiente pequeña con mucho ruido no es tendencia");

        assertEquals(TendenciaFGEM.DireccionFGEM.INSUFICIENTE, TendenciaFGEM.deFGEM(new double[]{1, 2}).direccionFGEM());
    }

    @Test
    void clasificacionAbcFGEM() {
        AnalisisAbcFGEM abcFGEM = AnalisisAbcFGEM.deFGEM(List.of(productoFGEM("D", "40"), productoFGEM("A", "700"),
                productoFGEM("C", "60"), productoFGEM("B", "200")));

        assertEquals(List.of("A", "B", "C", "D"), abcFGEM.itemsFGEM().stream().map(iFGEM -> iFGEM.productoFGEM().nombreFGEM()).toList());
        assertEquals(List.of(AnalisisAbcFGEM.ClaseFGEM.A, AnalisisAbcFGEM.ClaseFGEM.A, AnalisisAbcFGEM.ClaseFGEM.B, AnalisisAbcFGEM.ClaseFGEM.C),
                abcFGEM.itemsFGEM().stream().map(AnalisisAbcFGEM.ItemFGEM::claseFGEM).toList());
        assertEquals(0, new BigDecimal("0.9").compareTo(abcFGEM.participacionFGEM(AnalisisAbcFGEM.ClaseFGEM.A)));
        assertEquals(0, BigDecimal.ONE.compareTo(abcFGEM.itemsFGEM().get(3).acumuladoFGEM()));
        assertTrue(AnalisisAbcFGEM.deFGEM(List.of()).itemsFGEM().isEmpty());
    }

    @Test
    void correlacionDePearsonFGEM() {
        Map<String, double[]> seriesFGEM = new LinkedHashMap<>();
        seriesFGEM.put("x", new double[]{1, 2, 3, 4, 5});
        seriesFGEM.put("doble", new double[]{2, 4, 6, 8, 10});
        seriesFGEM.put("inversa", new double[]{-1, -2, -3, -4, -5});
        seriesFGEM.put("constante", new double[]{7, 7, 7, 7, 7});

        MatrizCorrelacionFGEM mFGEM = MatrizCorrelacionFGEM.deFGEM(seriesFGEM);

        assertEquals(1.0, mFGEM.rFGEM(0, 1), TOLERANCIA_FGEM);
        assertEquals(-1.0, mFGEM.rFGEM(0, 2), TOLERANCIA_FGEM);
        assertTrue(Double.isNaN(mFGEM.rFGEM(0, 3)), "Una serie constante no tiene correlación");
        assertEquals(3, mFGEM.paresOrdenadosFGEM().size(), "Los pares con la constante se omiten");
        assertEquals("fuerte", MatrizCorrelacionFGEM.fuerzaFGEM(-0.85));
        assertEquals("moderada", MatrizCorrelacionFGEM.fuerzaFGEM(0.5));
        assertEquals("nula", MatrizCorrelacionFGEM.fuerzaFGEM(0.1));

        Map<String, double[]> pocosFGEM = new LinkedHashMap<>();
        pocosFGEM.put("a", new double[]{1, 2});
        pocosFGEM.put("b", new double[]{3, 4});
        assertTrue(Double.isNaN(MatrizCorrelacionFGEM.deFGEM(pocosFGEM).rFGEM(0, 1)), "Con menos de 3 datos no se calcula");
    }

    @Test
    void histogramaConReglaDeSturgesFGEM() {
        HistogramaFGEM hFGEM = HistogramaFGEM.deFGEM(new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, 10);

        assertEquals(5, hFGEM.clasesFGEM().size(), "1 + log2(10) = 4.3 -> 5 clases");
        hFGEM.clasesFGEM().forEach(cFGEM -> assertEquals(2, cFGEM.frecuenciaFGEM()));
        assertEquals(1, HistogramaFGEM.deFGEM(new double[]{3, 3, 3}, 10).clasesFGEM().size());
        assertTrue(HistogramaFGEM.deFGEM(new double[0], 10).clasesFGEM().isEmpty());
    }

    @Test
    void estacionalidadPorDiaDeLaSemanaFGEM() {
        Map<LocalDate, Double> diasFGEM = new TreeMap<>();
        LocalDate lunesFGEM = LocalDate.of(2026, 1, 5);
        for (int iFGEM = 0; iFGEM < 14; iFGEM++) {
            LocalDate diaFGEM = lunesFGEM.plusDays(iFGEM);
            double ventaFGEM = switch (diaFGEM.getDayOfWeek()) {
                case MONDAY -> 100;
                case SATURDAY -> 300;
                default -> 0;
            };
            diasFGEM.put(diaFGEM, ventaFGEM);
        }

        EstacionalidadSemanalFGEM eFGEM = EstacionalidadSemanalFGEM.deFGEM(diasFGEM);

        assertEquals(800.0 / 14, eFGEM.promedioGeneralFGEM(), TOLERANCIA_FGEM);
        assertEquals(DayOfWeek.SATURDAY, eFGEM.mejorDiaFGEM());
        assertEquals(DayOfWeek.TUESDAY, eFGEM.peorDiaFGEM(), "En empate se elige el primero de la semana");
        assertEquals(300 / (800.0 / 14), eFGEM.indiceFGEM(DayOfWeek.SATURDAY), TOLERANCIA_FGEM);
        assertEquals(2, eFGEM.diasObservadosFGEM().get(DayOfWeek.MONDAY));
    }

    private static RentabilidadProductoFGEM productoFGEM(String nombreFGEM, String ventasFGEM) {
        return new RentabilidadProductoFGEM(nombreFGEM.hashCode(), nombreFGEM, nombreFGEM, TipoItemFGEM.PRODUCTO, null,
                BigDecimal.ONE, new BigDecimal(ventasFGEM), BigDecimal.ZERO);
    }
}
