package com.businessanalyzer.analytics.diagnostico;

import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.RentabilidadProducto;
import com.businessanalyzer.analytics.ResumenFinanciero;
import com.businessanalyzer.analytics.ResumenOperaciones;
import com.businessanalyzer.model.CategoriaFinanciera;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.TipoItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Negocio sano (3 meses): ventas 30 000, costo 21 000 (margen bruto 30 %), alquiler 4 500,
 * fletes 600, intereses 150, impuesto 900 -> utilidad neta 2 850 (9.5 %), margen de seguridad 44.6 %.
 */
class MotorDiagnosticoTest {

    private static final Periodo PERIODO = new Periodo(YearMonth.of(2025, 10).atDay(1), YearMonth.of(2025, 12).atEndOfMonth());

    @Test
    void negocioSanoQuedaEnVerdeYSinRecomendaciones() {
        Diagnostico d = MotorDiagnostico.evaluar(datos("21000", "30000", crecientes(12), productosSanos()));

        assertEquals(NivelSalud.SALUDABLE, d.nivel());
        assertEquals(100, d.puntaje());
        assertEquals(12, d.hallazgos().size());
        assertEquals(0, d.contar(Semaforo.ROJO) + d.contar(Semaforo.AMARILLO), d.hallazgos().toString());
        assertTrue(d.recomendaciones().isEmpty());
        assertEquals("9.5 %", hallazgo(d, TipoIndicador.MARGEN_NETO).valor());
        assertEquals("44.6 %", hallazgo(d, TipoIndicador.MARGEN_SEGURIDAD).valor());
        assertEquals(Semaforo.VERDE, hallazgo(d, TipoIndicador.TENDENCIA_VENTAS).semaforo());
        assertNull(hallazgo(d, TipoIndicador.MARGEN_NETO).recomendacion());
    }

    @Test
    void conPerdidaLaCalificacionSeLimitaYSeRecomiendaSubirPrecios() {
        // Costo 27 000: margen bruto 10 % y pérdida.
        Diagnostico d = MotorDiagnostico.evaluar(datos("27000", "30000", crecientes(12), productosSanos()));

        Hallazgo neto = hallazgo(d, TipoIndicador.MARGEN_NETO);
        assertEquals(Semaforo.ROJO, neto.semaforo());
        assertTrue(neto.recomendacion().contains("subir los precios"), neto.recomendacion());
        assertEquals(Semaforo.ROJO, hallazgo(d, TipoIndicador.MARGEN_BRUTO).semaforo());
        assertTrue(d.nivel() == NivelSalud.EN_RIESGO || d.nivel() == NivelSalud.CRITICO);
        assertEquals(TipoIndicador.MARGEN_NETO.peso() + 20, d.recomendaciones().get(0).prioridad());
        assertEquals(Semaforo.ROJO, d.recomendaciones().get(0).semaforo());
    }

    @Test
    void indicadorClaveEnRojoLimitaLaCalificacion() {
        // Margen de seguridad bajo (costos fijos altos) con el resto en verde.
        DatosDiagnostico base = datos("21000", "30000", crecientes(12), productosSanos());
        ResumenFinanciero fijosAltos = finanzas("8200", "600", "150", "0");
        DatosDiagnostico d = new DatosDiagnostico(base.sector(), base.moneda(), base.periodo(), 3, base.operaciones(),
                fijosAltos, base.operacionesAnteriores(), base.meses(), base.ventasMensuales(), base.utilidadMensual(),
                base.productos());

        Diagnostico resultado = MotorDiagnostico.evaluar(d);

        assertEquals(Semaforo.ROJO, hallazgo(resultado, TipoIndicador.MARGEN_SEGURIDAD).semaforo());
        assertEquals(NivelSalud.EN_RIESGO, resultado.nivel());
        assertTrue(resultado.resumen().stream().anyMatch(f -> f.contains("se limita")));
    }

    @Test
    void productoVendidoBajoCostoYDependencia() {
        List<RentabilidadProducto> productos = List.of(
                producto(1, "Principal", "20000", "13000"),
                producto(2, "Gancho", "1000", "1300"),
                producto(3, "Otro", "9000", "6700"));
        Diagnostico d = MotorDiagnostico.evaluar(datos("21000", "30000", crecientes(12), productos));

        Hallazgo perdida = hallazgo(d, TipoIndicador.PRODUCTOS_CON_PERDIDA);
        assertEquals(Semaforo.AMARILLO, perdida.semaforo(), "3.3 % de las ventas: atención");
        assertTrue(perdida.lectura().contains("\"Gancho\""));
        Hallazgo concentracion = hallazgo(d, TipoIndicador.CONCENTRACION);
        assertEquals(Semaforo.ROJO, concentracion.semaforo(), "66.7 % de las ventas en un producto");
    }

    @Test
    void ventasEnCaidaSostenida() {
        double[] serie = new double[12];
        for (int i = 0; i < serie.length; i++) {
            serie[i] = 14000 - i * 500;
        }
        Diagnostico d = MotorDiagnostico.evaluar(datos("21000", "30000", serie, productosSanos()));

        assertEquals(Semaforo.ROJO, hallazgo(d, TipoIndicador.TENDENCIA_VENTAS).semaforo());
        assertTrue(hallazgo(d, TipoIndicador.PROYECCION_VENTAS).semaforo() != Semaforo.VERDE);
    }

    @Test
    void conPocaHistoriaAlgunosIndicadoresQuedanSinDatos() {
        Diagnostico d = MotorDiagnostico.evaluar(datos("21000", "30000", crecientes(3), productosSanos()));

        assertEquals(Semaforo.SIN_DATOS, hallazgo(d, TipoIndicador.TENDENCIA_VENTAS).semaforo());
        assertEquals(Semaforo.SIN_DATOS, hallazgo(d, TipoIndicador.REGULARIDAD_VENTAS).semaforo());
        assertTrue(d.evaluado());
        assertFalse(d.puntajePorArea().isEmpty());
    }

    @Test
    void sinVentasNoHayDiagnostico() {
        DatosDiagnostico vacio = new DatosDiagnostico(Sector.COMERCIO, Moneda.PEN, PERIODO, 3, ResumenOperaciones.VACIO,
                ResumenFinanciero.VACIO, ResumenOperaciones.VACIO, List.of(), new double[0], new double[0], List.of());
        Diagnostico d = MotorDiagnostico.evaluar(vacio);

        assertEquals(NivelSalud.SIN_DATOS, d.nivel());
        assertFalse(d.evaluado());
        assertTrue(d.resumen().get(0).startsWith("No hay ventas"));
    }

    @Test
    void puntajePonderadoYNiveles() {
        List<Hallazgo> hallazgos = List.of(
                new Hallazgo(TipoIndicador.MARGEN_NETO, Semaforo.VERDE, "", null, "", null),
                new Hallazgo(TipoIndicador.CONCENTRACION, Semaforo.ROJO, "", null, "", "x"),
                Hallazgo.sinDatos(TipoIndicador.TENDENCIA_VENTAS, ""));
        assertEquals(75, MotorDiagnostico.puntaje(hallazgos), "(3·100 + 1·0) / 4");
        assertEquals(NivelSalud.SALUDABLE, NivelSalud.de(75));
        assertEquals(NivelSalud.ESTABLE, NivelSalud.de(74));
        assertEquals(NivelSalud.EN_RIESGO, NivelSalud.de(35));
        assertEquals(NivelSalud.CRITICO, NivelSalud.de(34));
    }

    // ------------------------------------------------------------------ datos de prueba

    private static DatosDiagnostico datos(String costoVentas, String ventas, double[] serie,
                                          List<RentabilidadProducto> productos) {
        ResumenOperaciones ops = new ResumenOperaciones(bd(ventas), bd(costoVentas), bd(costoVentas), 30, 3);
        ResumenOperaciones anteriores = new ResumenOperaciones(bd("29000"), bd("20300"), bd("20300"), 29, 3);
        List<YearMonth> meses = new ArrayList<>();
        double[] utilidad = new double[serie.length];
        for (int i = 0; i < serie.length; i++) {
            meses.add(YearMonth.of(2025, 12).minusMonths(serie.length - 1L - i));
            utilidad[i] = serie[i] * 0.09;
        }
        return new DatosDiagnostico(Sector.COMERCIO, Moneda.PEN, PERIODO, 3, ops, finanzas("4500", "600", "150", "900"),
                anteriores, meses, serie, utilidad, productos);
    }

    private static ResumenFinanciero finanzas(String alquiler, String fletes, String intereses, String impuesto) {
        Map<CategoriaFinanciera, BigDecimal> categorias = new EnumMap<>(CategoriaFinanciera.class);
        categorias.put(CategoriaFinanciera.ALQUILER, bd(alquiler));
        categorias.put(CategoriaFinanciera.TRANSPORTE, bd(fletes));
        categorias.put(CategoriaFinanciera.INTERESES_PRESTAMOS, bd(intereses));
        categorias.put(CategoriaFinanciera.IMPUESTO_RENTA, bd(impuesto));
        BigDecimal fijos = bd(alquiler).add(bd(intereses));
        BigDecimal gastos = fijos.add(bd(fletes)).add(bd(impuesto));
        return new ResumenFinanciero(BigDecimal.ZERO, gastos, fijos.add(bd(impuesto)), bd(fletes), fijos, bd(fletes),
                categorias, 4);
    }

    private static double[] crecientes(int meses) {
        double[] serie = new double[meses];
        for (int i = 0; i < meses; i++) {
            serie[i] = 9000 + i * 100;
        }
        return serie;
    }

    private static List<RentabilidadProducto> productosSanos() {
        return List.of(producto(1, "Arroz", "12000", "8400"), producto(2, "Aceite", "10000", "7000"),
                producto(3, "Azúcar", "8000", "5600"));
    }

    private static RentabilidadProducto producto(long id, String nombre, String ventas, String costo) {
        return new RentabilidadProducto(id, null, nombre, TipoItem.PRODUCTO, null, BigDecimal.TEN, bd(ventas), bd(costo));
    }

    private static Hallazgo hallazgo(Diagnostico d, TipoIndicador tipo) {
        return d.hallazgos().stream().filter(h -> h.tipo() == tipo).findFirst().orElseThrow();
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }
}
