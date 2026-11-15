package com.businessanalyzer.analytics.valoracion;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Moneda;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Negocio con utilidad de 12 000 al año, depreciación de 100 al mes y patrimonio de 30 000. */
class ValoracionNegocioTest {

    private static final SupuestosValoracion BASE = new SupuestosValoracion(new BigDecimal("0.20"),
            new BigDecimal("0.05"), new BigDecimal("0.02"), 5);

    @Test
    void valoraPorLosTresMetodos() {
        ValoracionNegocio v = ValoracionNegocio.calcular(BASE, bd("30000"), bd("12000"), bd("100"), 12);

        igual("60000", v.capitalizacion());                // 12 000 / 0.20
        igual("13200", v.flujoAnual());                    // 12 000 + 1 200 de depreciación
        double esperado = flujosDescontados(13200, 0.20, 0.05, 0.02, 5);
        assertEquals(esperado, v.flujosDescontados().doubleValue(), 0.05);
        assertEquals(5, v.proyeccion().size());
        igual("13860", v.proyeccion().get(0).flujo());      // 13 200 × 1.05
        igual("11550", v.proyeccion().get(0).valorPresente()); // 13 860 / 1.20

        BigDecimal rentabilidad = v.capitalizacion().add(v.flujosDescontados()).divide(bd("2"));
        igual(rentabilidad.toPlainString(), v.valorRentabilidad());
        assertEquals(ValoracionNegocio.Base.RENTABILIDAD, v.base());
        igual(v.valorRentabilidad().subtract(bd("30000")).toPlainString(), v.fondoComercio());
        igual("30000", v.minimo());
        igual(v.flujosDescontados().max(v.capitalizacion()).toPlainString(), v.maximo());
        assertTrue(String.join(" ", v.lectura(Moneda.PEN)).contains("fondo de comercio"));
    }

    @Test
    void sinUtilidadSoloValeSuPatrimonio() {
        ValoracionNegocio perdida = ValoracionNegocio.calcular(BASE, bd("25000"), bd("-5000"), bd("100"), 12);
        assertNull(perdida.capitalizacion());
        assertNull(perdida.flujosDescontados());
        assertEquals(ValoracionNegocio.Base.PATRIMONIO, perdida.base());
        igual("25000", perdida.valorReferencia());
        assertTrue(String.join(" ", perdida.lectura(Moneda.PEN)).contains("no tuvo utilidad"));

        ValoracionNegocio sinVentas = ValoracionNegocio.calcular(BASE, bd("8000"), null, bd("50"), 0);
        assertNull(sinVentas.flujosDescontados());
        igual("8000", sinVentas.valorReferencia());
    }

    @Test
    void siGanaPocoParaLoQueTieneValeSuPatrimonio() {
        ValoracionNegocio v = ValoracionNegocio.calcular(BASE, bd("200000"), bd("6000"), BigDecimal.ZERO, 12);
        assertEquals(ValoracionNegocio.Base.PATRIMONIO, v.base());
        igual("200000", v.valorReferencia());
        assertTrue(v.fondoComercio().signum() < 0);
    }

    @Test
    void laSensibilidadBajaConLaTasaYSubeConElCrecimiento() {
        ValoracionNegocio v = ValoracionNegocio.calcular(BASE, bd("30000"), bd("12000"), bd("100"), 12);
        ValoracionNegocio.Sensibilidad s = v.sensibilidad();
        igual(v.flujosDescontados().toPlainString(), s.valores()[1][1]);
        assertTrue(s.valores()[0][1].compareTo(s.valores()[1][1]) > 0, "Con menos tasa vale más");
        assertTrue(s.valores()[2][1].compareTo(s.valores()[1][1]) < 0, "Con más tasa vale menos");
        assertTrue(s.valores()[1][2].compareTo(s.valores()[1][1]) > 0, "Con más crecimiento vale más");
    }

    @Test
    void conPocosMesesAvisaQueEsOrientativa() {
        ValoracionNegocio v = ValoracionNegocio.calcular(BASE, bd("30000"), bd("12000"), BigDecimal.ZERO, 3);
        assertTrue(v.pocosDatos());
        assertTrue(String.join(" ", v.lectura(Moneda.PEN)).contains("solo 3 meses"));
    }

    @Test
    void validaLosSupuestos() {
        assertThrows(ValidacionException.class, () -> SupuestosValoracion.con(bd("0.04"), BigDecimal.ZERO));
        assertThrows(ValidacionException.class, () -> SupuestosValoracion.con(bd("0.20"), bd("0.60")));
        assertThrows(ValidacionException.class, () -> new SupuestosValoracion(bd("0.10"), BigDecimal.ZERO, bd("0.10"), 5));
        assertEquals(0, bd("0.02").compareTo(SupuestosValoracion.con(bd("0.20"), BigDecimal.ZERO).crecimientoLargoPlazo()));
    }

    /** Cálculo independiente con números de punto flotante. */
    private static double flujosDescontados(double flujo, double r, double g, double gL, int n) {
        double suma = 0;
        double ultimo = flujo;
        for (int t = 1; t <= n; t++) {
            ultimo = flujo * Math.pow(1 + g, t);
            suma += ultimo / Math.pow(1 + r, t);
        }
        double terminal = ultimo * (1 + gL) / (r - gL);
        return suma + terminal / Math.pow(1 + r, n);
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    private static void igual(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }
}
