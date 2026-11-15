package com.businessanalyzer.analytics.agricultura;

import com.businessanalyzer.analytics.diagnostico.Semaforo;
import com.businessanalyzer.analytics.ganaderia.IndicadorGanadero;
import com.businessanalyzer.model.Campania;
import com.businessanalyzer.model.Cosecha;
import com.businessanalyzer.model.CostoCampania;
import com.businessanalyzer.model.Cultivo;
import com.businessanalyzer.model.DestinoCosecha;
import com.businessanalyzer.model.EstadoCampania;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.RubroCosto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Papa en 1 ha (referencia 15 000 kg/ha, precio en chacra 0.80): costos 8 000; cosecha 9 000 kg vendidos a
 * 0.90, 2 000 de consumo, 1 000 de semilla y 500 de merma.
 */
class ResultadoCampaniaTest {

    private static final LocalDate SIEMBRA = LocalDate.of(2025, 10, 1);

    @Test
    void calculaRendimientoValorYMargen() {
        ResultadoCampania r = ResultadoCampania.de(papa(EstadoCampania.COSECHADA, "0.80"),
                List.of(costo(RubroCosto.SEMILLA, "3000"), costo(RubroCosto.MANO_OBRA, "5000"),
                        costoAnulado()),
                List.of(cosecha("9000", DestinoCosecha.VENTA, "0.90"), cosecha("2000", DestinoCosecha.AUTOCONSUMO, null),
                        cosecha("1000", DestinoCosecha.SEMILLA, null), cosecha("500", DestinoCosecha.MERMA, null)));

        igual("8000", r.costoTotal());
        igual("12500", r.produccion());
        igual("12500", r.rendimiento());
        igual("0.833333", r.avanceReferencia());
        igual("8100", r.ingresoVentas());
        igual("10500", r.valorProduccion());           // 8 100 + (2 000 + 1 000) × 0.80; la merma no vale
        igual("2500", r.margen());
        igual("0.3125", r.rentabilidad());
        igual("0.64", r.costoPorKilo());
        igual("10000", r.rendimientoEquilibrio());     // 8 000 / 0.80
        igual("0.72", r.parteVendida());

        List<IndicadorGanadero> ind = r.evaluar(Moneda.PEN);
        assertEquals(Semaforo.AMARILLO, buscar(ind, "Rendimiento").semaforo());   // 83 % de la referencia
        assertEquals(Semaforo.VERDE, buscar(ind, "Rendimiento para no perder").semaforo());
        assertEquals(Semaforo.VERDE, buscar(ind, "Margen").semaforo());           // 31 % sobre el costo
    }

    @Test
    void sinPrecioEnChacraUsaElPrecioPromedioDeVenta() {
        ResultadoCampania r = ResultadoCampania.de(papa(EstadoCampania.COSECHADA, null), List.of(costo(RubroCosto.SEMILLA, "1000")),
                List.of(cosecha("1000", DestinoCosecha.VENTA, "1.00"), cosecha("1000", DestinoCosecha.VENTA, "0.50"),
                        cosecha("500", DestinoCosecha.ANIMALES, null)));
        igual("0.75", r.precioValorizacion());
        igual("1875", r.valorProduccion());            // 1 500 + 500 × 0.75
    }

    @Test
    void unaCampaniaPerdidaTieneRendimientoCeroYMargenNegativo() {
        ResultadoCampania r = ResultadoCampania.de(papa(EstadoCampania.PERDIDA, "0.80"),
                List.of(costo(RubroCosto.SEMILLA, "2000")), List.of());
        igual("0", r.rendimiento());
        igual("-2000", r.margen());
        List<IndicadorGanadero> ind = r.evaluar(Moneda.PEN);
        assertEquals(Semaforo.ROJO, buscar(ind, "Rendimiento").semaforo());
        assertEquals(Semaforo.ROJO, buscar(ind, "Margen").semaforo());
        assertEquals("Campaña perdida: Helada.", buscar(ind, "Rendimiento").lectura());
    }

    @Test
    void enCursoSinCosechaNoTieneRendimiento() {
        ResultadoCampania r = ResultadoCampania.de(papa(EstadoCampania.EN_CURSO, null), List.of(), List.of());
        assertNull(r.rendimiento());
        assertNull(r.rentabilidad());
        assertNull(r.rendimientoEquilibrio());
        assertEquals(Semaforo.SIN_DATOS, buscar(r.evaluar(Moneda.PEN), "Rendimiento").semaforo());
    }

    // ------------------------------------------------------------------ utilidades

    private static Campania papa(EstadoCampania estado, String precioChacra) {
        return new Campania(1, 1, 1, "Parcela Alta", Cultivo.PAPA, "Yungay", SIEMBRA, BigDecimal.ONE,
                Cultivo.PAPA.rendimientoReferencia(), precioChacra == null ? null : new BigDecimal(precioChacra), estado,
                null, estado == EstadoCampania.PERDIDA ? "Helada" : null, null, "admin");
    }

    private static CostoCampania costo(RubroCosto rubro, String monto) {
        return new CostoCampania(1, 1, SIEMBRA, rubro, rubro.descripcion(), new BigDecimal(monto), false, "admin");
    }

    private static CostoCampania costoAnulado() {
        return new CostoCampania(2, 1, SIEMBRA, RubroCosto.OTROS, "Error", new BigDecimal("999"), true, "admin");
    }

    private static Cosecha cosecha(String kilos, DestinoCosecha destino, String precio) {
        return new Cosecha(1, 1, SIEMBRA.plusMonths(6), new BigDecimal(kilos), destino,
                precio == null ? null : new BigDecimal(precio), null, false, "admin");
    }

    private static IndicadorGanadero buscar(List<IndicadorGanadero> lista, String nombre) {
        return lista.stream().filter(i -> i.nombre().equals(nombre)).findFirst().orElseThrow();
    }

    private static void igual(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }
}
