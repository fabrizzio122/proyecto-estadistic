package com.businessanalyzer.analytics.valoracion;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Negocio con utilidad de 12 000 al año, depreciación de 100 al mes y patrimonio de 30 000. */
class ValoracionNegocioTestFGEM {

    private static final SupuestosValoracionFGEM BASE_FGEM = new SupuestosValoracionFGEM(new BigDecimal("0.20"),
            new BigDecimal("0.05"), new BigDecimal("0.02"), 5);

    @Test
    void valoraPorLosTresMetodosFGEM() {
        ValoracionNegocioFGEM vFGEM = ValoracionNegocioFGEM.calcularFGEM(BASE_FGEM, bdFGEM("30000"), bdFGEM("12000"), bdFGEM("100"), 12);

        igualFGEM("60000", vFGEM.capitalizacionFGEM());                // 12 000 / 0.20
        igualFGEM("13200", vFGEM.flujoAnualFGEM());                    // 12 000 + 1 200 de depreciación
        double esperadoFGEM = flujosDescontadosFGEM(13200, 0.20, 0.05, 0.02, 5);
        assertEquals(esperadoFGEM, vFGEM.flujosDescontadosFGEM().doubleValue(), 0.05);
        assertEquals(5, vFGEM.proyeccionFGEM().size());
        igualFGEM("13860", vFGEM.proyeccionFGEM().get(0).flujoFGEM());      // 13 200 × 1.05
        igualFGEM("11550", vFGEM.proyeccionFGEM().get(0).valorPresenteFGEM()); // 13 860 / 1.20

        BigDecimal rentabilidadFGEM = vFGEM.capitalizacionFGEM().add(vFGEM.flujosDescontadosFGEM()).divide(bdFGEM("2"));
        igualFGEM(rentabilidadFGEM.toPlainString(), vFGEM.valorRentabilidadFGEM());
        assertEquals(ValoracionNegocioFGEM.BaseFGEM.RENTABILIDAD, vFGEM.baseFGEM());
        igualFGEM(vFGEM.valorRentabilidadFGEM().subtract(bdFGEM("30000")).toPlainString(), vFGEM.fondoComercioFGEM());
        igualFGEM("30000", vFGEM.minimoFGEM());
        igualFGEM(vFGEM.flujosDescontadosFGEM().max(vFGEM.capitalizacionFGEM()).toPlainString(), vFGEM.maximoFGEM());
        assertTrue(String.join(" ", vFGEM.lecturaFGEM(MonedaFGEM.PEN)).contains("fondo de comercio"));
    }

    @Test
    void sinUtilidadSoloValeSuPatrimonioFGEM() {
        ValoracionNegocioFGEM perdidaFGEM = ValoracionNegocioFGEM.calcularFGEM(BASE_FGEM, bdFGEM("25000"), bdFGEM("-5000"), bdFGEM("100"), 12);
        assertNull(perdidaFGEM.capitalizacionFGEM());
        assertNull(perdidaFGEM.flujosDescontadosFGEM());
        assertEquals(ValoracionNegocioFGEM.BaseFGEM.PATRIMONIO, perdidaFGEM.baseFGEM());
        igualFGEM("25000", perdidaFGEM.valorReferenciaFGEM());
        assertTrue(String.join(" ", perdidaFGEM.lecturaFGEM(MonedaFGEM.PEN)).contains("no tuvo utilidad"));

        ValoracionNegocioFGEM sinVentasFGEM = ValoracionNegocioFGEM.calcularFGEM(BASE_FGEM, bdFGEM("8000"), null, bdFGEM("50"), 0);
        assertNull(sinVentasFGEM.flujosDescontadosFGEM());
        igualFGEM("8000", sinVentasFGEM.valorReferenciaFGEM());
    }

    @Test
    void siGanaPocoParaLoQueTieneValeSuPatrimonioFGEM() {
        ValoracionNegocioFGEM vFGEM = ValoracionNegocioFGEM.calcularFGEM(BASE_FGEM, bdFGEM("200000"), bdFGEM("6000"), BigDecimal.ZERO, 12);
        assertEquals(ValoracionNegocioFGEM.BaseFGEM.PATRIMONIO, vFGEM.baseFGEM());
        igualFGEM("200000", vFGEM.valorReferenciaFGEM());
        assertTrue(vFGEM.fondoComercioFGEM().signum() < 0);
    }

    @Test
    void laSensibilidadBajaConLaTasaYSubeConElCrecimientoFGEM() {
        ValoracionNegocioFGEM vFGEM = ValoracionNegocioFGEM.calcularFGEM(BASE_FGEM, bdFGEM("30000"), bdFGEM("12000"), bdFGEM("100"), 12);
        ValoracionNegocioFGEM.SensibilidadFGEM sFGEM = vFGEM.sensibilidadFGEM();
        igualFGEM(vFGEM.flujosDescontadosFGEM().toPlainString(), sFGEM.valoresFGEM()[1][1]);
        assertTrue(sFGEM.valoresFGEM()[0][1].compareTo(sFGEM.valoresFGEM()[1][1]) > 0, "Con menos tasa vale más");
        assertTrue(sFGEM.valoresFGEM()[2][1].compareTo(sFGEM.valoresFGEM()[1][1]) < 0, "Con más tasa vale menos");
        assertTrue(sFGEM.valoresFGEM()[1][2].compareTo(sFGEM.valoresFGEM()[1][1]) > 0, "Con más crecimiento vale más");
    }

    @Test
    void conPocosMesesAvisaQueEsOrientativaFGEM() {
        ValoracionNegocioFGEM vFGEM = ValoracionNegocioFGEM.calcularFGEM(BASE_FGEM, bdFGEM("30000"), bdFGEM("12000"), BigDecimal.ZERO, 3);
        assertTrue(vFGEM.pocosDatosFGEM());
        assertTrue(String.join(" ", vFGEM.lecturaFGEM(MonedaFGEM.PEN)).contains("solo 3 meses"));
    }

    @Test
    void validaLosSupuestosFGEM() {
        assertThrows(ValidacionExceptionFGEM.class, () -> SupuestosValoracionFGEM.conFGEM(bdFGEM("0.04"), BigDecimal.ZERO));
        assertThrows(ValidacionExceptionFGEM.class, () -> SupuestosValoracionFGEM.conFGEM(bdFGEM("0.20"), bdFGEM("0.60")));
        assertThrows(ValidacionExceptionFGEM.class, () -> new SupuestosValoracionFGEM(bdFGEM("0.10"), BigDecimal.ZERO, bdFGEM("0.10"), 5));
        assertEquals(0, bdFGEM("0.02").compareTo(SupuestosValoracionFGEM.conFGEM(bdFGEM("0.20"), BigDecimal.ZERO).crecimientoLargoPlazoFGEM()));
    }

    /** Cálculo independiente con números de punto flotante. */
    private static double flujosDescontadosFGEM(double flujoFGEM, double rFGEM, double gFGEM, double gLFGEM, int nFGEM) {
        double sumaFGEM = 0;
        double ultimoFGEM = flujoFGEM;
        for (int tFGEM = 1; tFGEM <= nFGEM; tFGEM++) {
            ultimoFGEM = flujoFGEM * Math.pow(1 + gFGEM, tFGEM);
            sumaFGEM += ultimoFGEM / Math.pow(1 + rFGEM, tFGEM);
        }
        double terminalFGEM = ultimoFGEM * (1 + gLFGEM) / (rFGEM - gLFGEM);
        return sumaFGEM + terminalFGEM / Math.pow(1 + rFGEM, nFGEM);
    }

    private static BigDecimal bdFGEM(String valorFGEM) {
        return new BigDecimal(valorFGEM);
    }

    private static void igualFGEM(String esperadoFGEM, BigDecimal realFGEM) {
        assertEquals(0, new BigDecimal(esperadoFGEM).compareTo(realFGEM), "esperado " + esperadoFGEM + " pero fue " + realFGEM);
    }
}
