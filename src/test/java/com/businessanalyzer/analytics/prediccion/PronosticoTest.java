package com.businessanalyzer.analytics.prediccion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PronosticoTest {

    private static final double TOLERANCIA = 1e-9;

    @Test
    void tendenciaLinealProlongaLaRecta() {
        double[] pronostico = new TendenciaLineal().pronosticar(new double[]{10, 12, 14, 16, 18}, 3);
        assertArrayEquals(new double[]{20, 22, 24}, pronostico, TOLERANCIA);
    }

    @Test
    void promedioMovilProyectaElPromedioDeLosUltimosMeses() {
        double[] pronostico = new PromedioMovil(3).pronosticar(new double[]{1, 2, 3, 4, 5, 6}, 2);
        assertArrayEquals(new double[]{5, 5}, pronostico, TOLERANCIA);
    }

    @Test
    void holtSigueUnaTendenciaPerfecta() {
        double[] pronostico = new SuavizadoHolt().pronosticar(new double[]{10, 12, 14, 16, 18, 20}, 2);
        assertArrayEquals(new double[]{22, 24}, pronostico, TOLERANCIA);
    }

    @Test
    void holtAjustaSusParametrosEntre01y09() {
        SuavizadoHolt.Parametros p = new SuavizadoHolt().ajustar(new double[]{100, 130, 90, 140, 110, 150, 120});
        assertTrue(p.alfa() >= 0.1 && p.alfa() <= 0.9);
        assertTrue(p.beta() >= 0.1 && p.beta() <= 0.9);
    }

    @Test
    void laValidacionHistoricaMideElErrorDeCadaMetodo() {
        // Con [1, 2, 3] se pronostica el 4.º mes (real 10): promedio móvil 2 (error 8), tendencia 4 (error 6)
        double[] y = {1, 2, 3, 10};
        Pronostico.Evaluacion movil = Pronosticador.evaluar(new PromedioMovil(3), y);
        Pronostico.Evaluacion lineal = Pronosticador.evaluar(new TendenciaLineal(), y);

        assertEquals(8, movil.mae(), TOLERANCIA);
        assertEquals(6, lineal.mae(), TOLERANCIA);
        assertEquals(1, lineal.mesesEvaluados());

        Pronostico p = Pronosticador.pronosticar("Ventas", y, 3, true);
        assertEquals("Tendencia lineal", p.elegida().metodo().nombre(), "Menor error; en empate gana el más simple");
        assertEquals(Pronostico.Confiabilidad.BAJA, p.confiabilidad(), "Solo un mes validado");
    }

    @Test
    void conUnaSeriePerfectaLaConfiabilidadEsAltaYElIntervaloNulo() {
        Pronostico p = Pronosticador.pronosticar("Ventas", new double[]{100, 110, 120, 130, 140, 150, 160, 170}, 2, true);

        assertEquals(0, p.elegida().mae(), TOLERANCIA);
        assertEquals(Pronostico.Confiabilidad.ALTA, p.confiabilidad());
        assertEquals(180, p.puntos().get(0).valor(), TOLERANCIA);
        assertEquals(p.puntos().get(0).inferior(), p.puntos().get(0).superior(), TOLERANCIA);
        assertEquals(370, p.total(), TOLERANCIA);
    }

    @Test
    void elIntervaloCreceConLaDistancia() {
        // Sin recortar en cero, para medir el ancho completo del intervalo
        Pronostico p = Pronosticador.pronosticar("Ventas", new double[]{100, 140, 90, 150, 95, 160, 105, 150}, 4, false);
        double ancho1 = p.puntos().get(0).superior() - p.puntos().get(0).inferior();
        double ancho4 = p.puntos().get(3).superior() - p.puntos().get(3).inferior();
        assertEquals(2 * ancho1, ancho4, 1e-6, "Proporcional a la raíz de h: √4 = 2");
    }

    @Test
    void lasVentasProyectadasNoSonNegativas() {
        Pronostico p = Pronosticador.pronosticar("Ventas", new double[]{50, 40, 30, 20, 10}, 3, true);
        p.puntos().forEach(punto -> {
            assertTrue(punto.valor() >= 0);
            assertTrue(punto.inferior() >= 0);
        });
        Pronostico utilidad = Pronosticador.pronosticar("Utilidad", new double[]{50, 40, 30, 20, 10}, 3, false);
        assertEquals(-20, utilidad.puntos().get(2).valor(), TOLERANCIA, "La utilidad sí puede ser negativa");
    }

    @Test
    void conPocosDatosNoSePronostica() {
        Pronostico p = Pronosticador.pronosticar("Ventas", new double[]{100, 120, 130}, 3, true);
        assertFalse(p.suficiente());
        assertTrue(p.puntos().isEmpty());
        assertEquals(3, p.datos());
    }
}
