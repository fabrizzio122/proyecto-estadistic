package com.businessanalyzer.analytics.simulacion;

import com.businessanalyzer.analytics.Periodo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base mensual: ventas 10 000, costo de ventas 6 000, gastos variables 1 000, costos fijos 2 000,
 * impuesto 30 % -> UAI 1 000, utilidad neta 700, punto de equilibrio 6 666.67.
 */
class SimuladorTest {

    private static final BigDecimal TASA = new BigDecimal("0.30");
    private static final BaseSimulacion BASE = new BaseSimulacion(Periodo.mes(YearMonth.of(2026, 1)), 1,
            bd("10000"), bd("6000"), bd("1000"), bd("2000"), BigDecimal.ZERO, bd("300"));

    @Test
    void sinCambiosReproduceLaBase() {
        ResultadoEscenario r = Simulador.simular(BASE, Supuestos.neutros(TASA));

        assertMonto("1000", r.utilidadAntesImpuestos());
        assertMonto("300", r.impuesto());
        assertMonto("700", r.utilidadNeta());
        assertMonto("3000", r.margenContribucion());
        assertEquals(bd("6666.67"), r.equilibrio().ventasEquilibrio());
        assertEquals(bd("0.300000"), BASE.tasaImpuestoEfectiva());
    }

    @Test
    void subirPreciosAumentaVentasSinCambiarCostos() {
        ResultadoEscenario r = Simulador.simular(BASE, Supuestos.neutros(TASA).con(Variable.PRECIO, bd("0.10")));

        assertMonto("11000", r.ventas());
        assertMonto("6000", r.costoVentas());
        assertMonto("1400", r.utilidadNeta());
    }

    @Test
    void venderMasMueveTambienLosCostosVariables() {
        ResultadoEscenario r = Simulador.simular(BASE, Supuestos.neutros(TASA).con(Variable.VOLUMEN, bd("0.10")));

        assertMonto("11000", r.ventas());
        assertMonto("6600", r.costoVentas());
        assertMonto("1100", r.gastosVariables());
        assertMonto("2000", r.costosFijos());
        assertMonto("910", r.utilidadNeta());
    }

    @Test
    void costosFijosConAhorroYCostoAdicional() {
        Supuestos s = Supuestos.neutros(TASA).con(Variable.COSTOS_FIJOS, bd("-0.10")).conCostoFijoAdicional(bd("500"));
        ResultadoEscenario r = Simulador.simular(BASE, s);

        assertMonto("2300", r.costosFijos());
        assertMonto("490", r.utilidadNeta());
    }

    @Test
    void conPerdidaNoSePagaImpuesto() {
        ResultadoEscenario r = Simulador.simular(BASE, Supuestos.neutros(TASA).con(Variable.COSTOS_FIJOS, BigDecimal.ONE));

        assertMonto("-1000", r.utilidadAntesImpuestos());
        assertMonto("0", r.impuesto());
        assertMonto("-1000", r.utilidadNeta());
    }

    @Test
    void metaDeUtilidadPorPrecioOVolumen() {
        Supuestos neutros = Supuestos.neutros(TASA);

        BigDecimal precio = Simulador.cambioPrecioParaMeta(BASE, neutros, bd("1400")).orElseThrow();
        assertEquals(0, bd("0.10").compareTo(precio), "Para ganar 1 400 hay que subir precios 10 %");
        assertMonto("1400", Simulador.simular(BASE, neutros.con(Variable.PRECIO, precio)).utilidadNeta());

        BigDecimal volumen = Simulador.cambioVolumenParaMeta(BASE, neutros, bd("910")).orElseThrow();
        assertEquals(0, bd("0.10").compareTo(volumen), "Para ganar 910 hay que vender 10 % más");

        BigDecimal equilibrio = Simulador.cambioVolumenParaMeta(BASE, neutros, BigDecimal.ZERO).orElseThrow();
        assertEquals(0, bd("-0.333333").compareTo(equilibrio), "Con 33.3 % menos de volumen se llega a cero");
    }

    @Test
    void sinMargenDeContribucionNingunVolumenAlcanzaLaMeta() {
        Supuestos caro = Supuestos.neutros(TASA).con(Variable.COSTO_UNITARIO, bd("0.60"));
        assertTrue(Simulador.cambioVolumenParaMeta(BASE, caro, bd("500")).isEmpty());
    }

    @Test
    void sensibilidadOrdenadaPorImpacto() {
        List<Simulador.Sensibilidad> s = Simulador.sensibilidad(BASE, Supuestos.neutros(TASA));

        assertEquals(List.of(Variable.PRECIO, Variable.COSTO_UNITARIO, Variable.VOLUMEN, Variable.COSTOS_FIJOS,
                Variable.GASTOS_VARIABLES), s.stream().map(Simulador.Sensibilidad::variable).toList());
        Simulador.Sensibilidad precio = s.get(0);
        assertMonto("0", precio.utilidadSiBaja());
        assertMonto("1400", precio.utilidadSiSube());
        assertMonto("700", precio.utilidadEscenario());
    }

    @Test
    void escenariosPredefinidos() {
        Supuestos pesimista = EscenarioPredefinido.PESIMISTA.aplicar(TASA);
        assertEquals(0, bd("-0.15").compareTo(pesimista.volumen()));
        assertEquals(0, bd("0.05").compareTo(pesimista.costoUnitario()));
        assertTrue(Simulador.simular(BASE, pesimista).utilidadNeta().compareTo(bd("700")) < 0);
        assertTrue(Simulador.simular(BASE, EscenarioPredefinido.OPTIMISTA.aplicar(TASA)).utilidadNeta()
                .compareTo(bd("700")) > 0);
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    private static void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, bd(esperado).compareTo(real), "se esperaba " + esperado + " y se obtuvo " + real);
    }
}
