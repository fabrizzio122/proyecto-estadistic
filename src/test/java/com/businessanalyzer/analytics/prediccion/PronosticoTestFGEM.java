package com.businessanalyzer.analytics.prediccion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PronosticoTestFGEM {

    private static final double TOLERANCIA_FGEM = 1e-9;

    @Test
    void tendenciaLinealProlongaLaRectaFGEM() {
        double[] pronosticoFGEM = new TendenciaLinealFGEM().pronosticarFGEM(new double[]{10, 12, 14, 16, 18}, 3);
        assertArrayEquals(new double[]{20, 22, 24}, pronosticoFGEM, TOLERANCIA_FGEM);
    }

    @Test
    void promedioMovilProyectaElPromedioDeLosUltimosMesesFGEM() {
        double[] pronosticoFGEM = new PromedioMovilFGEM(3).pronosticarFGEM(new double[]{1, 2, 3, 4, 5, 6}, 2);
        assertArrayEquals(new double[]{5, 5}, pronosticoFGEM, TOLERANCIA_FGEM);
    }

    @Test
    void holtSigueUnaTendenciaPerfectaFGEM() {
        double[] pronosticoFGEM = new SuavizadoHoltFGEM().pronosticarFGEM(new double[]{10, 12, 14, 16, 18, 20}, 2);
        assertArrayEquals(new double[]{22, 24}, pronosticoFGEM, TOLERANCIA_FGEM);
    }

    @Test
    void holtAjustaSusParametrosEntre01y09FGEM() {
        SuavizadoHoltFGEM.ParametrosFGEM pFGEM = new SuavizadoHoltFGEM().ajustarFGEM(new double[]{100, 130, 90, 140, 110, 150, 120});
        assertTrue(pFGEM.alfaFGEM() >= 0.1 && pFGEM.alfaFGEM() <= 0.9);
        assertTrue(pFGEM.betaFGEM() >= 0.1 && pFGEM.betaFGEM() <= 0.9);
    }

    @Test
    void laValidacionHistoricaMideElErrorDeCadaMetodoFGEM() {
        double[] yFGEM = {1, 2, 3, 10};
        PronosticoFGEM.EvaluacionFGEM movilFGEM = PronosticadorFGEM.evaluarFGEM(new PromedioMovilFGEM(3), yFGEM);
        PronosticoFGEM.EvaluacionFGEM linealFGEM = PronosticadorFGEM.evaluarFGEM(new TendenciaLinealFGEM(), yFGEM);

        assertEquals(8, movilFGEM.maeFGEM(), TOLERANCIA_FGEM);
        assertEquals(6, linealFGEM.maeFGEM(), TOLERANCIA_FGEM);
        assertEquals(1, linealFGEM.mesesEvaluadosFGEM());

        PronosticoFGEM pFGEM = PronosticadorFGEM.pronosticarFGEM("Ventas", yFGEM, 3, true);
        assertEquals("Tendencia lineal", pFGEM.elegidaFGEM().metodoFGEM().nombreFGEM(), "Menor error; en empate gana el más simple");
        assertEquals(PronosticoFGEM.ConfiabilidadFGEM.BAJA, pFGEM.confiabilidadFGEM(), "Solo un mes validado");
    }

    @Test
    void conUnaSeriePerfectaLaConfiabilidadEsAltaYElIntervaloNuloFGEM() {
        PronosticoFGEM pFGEM = PronosticadorFGEM.pronosticarFGEM("Ventas", new double[]{100, 110, 120, 130, 140, 150, 160, 170}, 2, true);

        assertEquals(0, pFGEM.elegidaFGEM().maeFGEM(), TOLERANCIA_FGEM);
        assertEquals(PronosticoFGEM.ConfiabilidadFGEM.ALTA, pFGEM.confiabilidadFGEM());
        assertEquals(180, pFGEM.puntosFGEM().get(0).valorFGEM(), TOLERANCIA_FGEM);
        assertEquals(pFGEM.puntosFGEM().get(0).inferiorFGEM(), pFGEM.puntosFGEM().get(0).superiorFGEM(), TOLERANCIA_FGEM);
        assertEquals(370, pFGEM.totalFGEM(), TOLERANCIA_FGEM);
    }

    @Test
    void elIntervaloCreceConLaDistanciaFGEM() {
        PronosticoFGEM pFGEM = PronosticadorFGEM.pronosticarFGEM("Ventas", new double[]{100, 140, 90, 150, 95, 160, 105, 150}, 4, false);
        double ancho1FGEM = pFGEM.puntosFGEM().get(0).superiorFGEM() - pFGEM.puntosFGEM().get(0).inferiorFGEM();
        double ancho4FGEM = pFGEM.puntosFGEM().get(3).superiorFGEM() - pFGEM.puntosFGEM().get(3).inferiorFGEM();
        assertEquals(2 * ancho1FGEM, ancho4FGEM, 1e-6, "Proporcional a la raíz de h: √4 = 2");
    }

    @Test
    void lasVentasProyectadasNoSonNegativasFGEM() {
        PronosticoFGEM pFGEM = PronosticadorFGEM.pronosticarFGEM("Ventas", new double[]{50, 40, 30, 20, 10}, 3, true);
        pFGEM.puntosFGEM().forEach(puntoFGEM -> {
            assertTrue(puntoFGEM.valorFGEM() >= 0);
            assertTrue(puntoFGEM.inferiorFGEM() >= 0);
        });
        PronosticoFGEM utilidadFGEM = PronosticadorFGEM.pronosticarFGEM("Utilidad", new double[]{50, 40, 30, 20, 10}, 3, false);
        assertEquals(-20, utilidadFGEM.puntosFGEM().get(2).valorFGEM(), TOLERANCIA_FGEM, "La utilidad sí puede ser negativa");
    }

    @Test
    void conPocosDatosNoSePronosticaFGEM() {
        PronosticoFGEM pFGEM = PronosticadorFGEM.pronosticarFGEM("Ventas", new double[]{100, 120, 130}, 3, true);
        assertFalse(pFGEM.suficienteFGEM());
        assertTrue(pFGEM.puntosFGEM().isEmpty());
        assertEquals(3, pFGEM.datosFGEM());
    }
}
