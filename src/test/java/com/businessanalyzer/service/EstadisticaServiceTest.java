package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.estadistica.AnalisisAbc;
import com.businessanalyzer.analytics.estadistica.AnalisisEstadistico;
import com.businessanalyzer.analytics.estadistica.LecturaEstadistica;
import com.businessanalyzer.analytics.estadistica.Tendencia;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.model.CategoriaFinanciera;
import com.businessanalyzer.model.DatosMovimiento;
import com.businessanalyzer.model.DatosOperacion;
import com.businessanalyzer.model.DatosOperacion.DatosLinea;
import com.businessanalyzer.model.DatosProducto;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.TipoItem;
import com.businessanalyzer.model.TipoMovimiento;
import com.businessanalyzer.model.TipoOperacion;
import com.businessanalyzer.model.UnidadMedida;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Escenario (reloj de pruebas en 2026-01-15):
 * <pre>
 * Ago–Dic 2025: una venta mensual de P1 de 1000, 1200, 1400, 1600 y 1800 (crecimiento lineal de 200)
 *               y publicidad de 100, 120, 140, 160 y 180 (proporcional a las ventas).
 * Ene 2026 (mes en curso, incompleto): ventas de P2 por 500 y de P3 por 250.
 * Total del período: 7750 -> P1 90.3 % (A), P2 6.5 % (B), P3 3.2 % (C).
 * </pre>
 */
class EstadisticaServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2025, 8, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 1, 31);
    private static final double TOLERANCIA = 1e-6;

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private EstadisticaService servicio;
    private Empresa empresa;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = new EstadisticaService(entorno.database, new OperacionRepository(), new MovimientoRepository(),
                entorno.empresaService, entorno.sesion, entorno.reloj);
        entorno.administradorConectado();
        empresa = entorno.crearEmpresaActiva("Comercial Andina");

        Producto p1 = producto("P1", "60", "100");
        Producto p2 = producto("P2", "30", "50");
        Producto p3 = producto("P3", "5", "10");
        operacion(TipoOperacion.COMPRA, DESDE, linea(p1, "100", "60"), linea(p2, "50", "30"), linea(p3, "100", "5"));
        for (int k = 0; k < 5; k++) {
            YearMonth mes = YearMonth.of(2025, 8).plusMonths(k);
            operacion(TipoOperacion.VENTA, mes.atDay(10), linea(p1, String.valueOf(10 + 2 * k), "100"));
            entorno.finanzas.registrar(new DatosMovimiento(TipoMovimiento.GASTO, CategoriaFinanciera.MARKETING, null,
                    mes.atDay(12), "Publicidad", BigDecimal.valueOf(100 + 20L * k), null));
        }
        operacion(TipoOperacion.VENTA, LocalDate.of(2026, 1, 10), linea(p2, "10", "50"));
        operacion(TipoOperacion.VENTA, LocalDate.of(2026, 1, 12), linea(p3, "25", "10"));
    }

    @Test
    void estadisticaDeLosTickets() {
        AnalisisEstadistico a = servicio.analizar(DESDE, HASTA);

        assertEquals(7, a.ticket().n());
        assertEquals(7750.0 / 7, a.ticket().media(), TOLERANCIA);
        assertEquals(1200.0, a.ticket().mediana(), TOLERANCIA, "250, 500, 1000, [1200], 1400, 1600, 1800");
        assertEquals(250.0, a.ticket().minimo(), TOLERANCIA);
        assertEquals(1800.0, a.ticket().maximo(), TOLERANCIA);
        assertEquals(168, a.ventasDiarias().n(), "Días del 1 de agosto al 15 de enero (hoy), incluidos los días sin ventas");
    }

    @Test
    void laTendenciaUsaSoloMesesCompletos() {
        AnalisisEstadistico a = servicio.analizar(DESDE, HASTA);

        assertTrue(a.mesActualExcluido());
        assertEquals(5, a.serieMensual().size(), "Ago–Dic; se omiten los meses previos sin ventas y enero (incompleto)");
        assertEquals(YearMonth.of(2025, 8), a.serieMensual().get(0).mes());
        Tendencia t = a.tendenciaVentas();
        assertEquals(200.0, t.pendiente(), TOLERANCIA);
        assertEquals(1.0, t.r2(), TOLERANCIA);
        assertEquals(Tendencia.Direccion.CRECIENTE, t.direccion());
    }

    @Test
    void correlacionEntreVentasYPublicidad() {
        AnalisisEstadistico a = servicio.analizar(DESDE, HASTA);

        assertEquals(5, a.correlaciones().observaciones());
        int ventas = a.correlaciones().variables().indexOf("Ventas netas");
        int publicidad = a.correlaciones().variables().indexOf("Publicidad");
        assertEquals(1.0, a.correlaciones().r(ventas, publicidad), TOLERANCIA);
    }

    @Test
    void clasificacionAbcDelPeriodo() {
        AnalisisAbc abc = servicio.analizar(DESDE, HASTA).abc();

        assertEquals(List.of("P1", "P2", "P3"), abc.items().stream().map(i -> i.producto().nombre()).toList());
        assertEquals(List.of(AnalisisAbc.Clase.A, AnalisisAbc.Clase.B, AnalisisAbc.Clase.C),
                abc.items().stream().map(AnalisisAbc.Item::clase).toList());
        assertEquals(0, new BigDecimal("7750").compareTo(abc.ventasTotales()));
    }

    @Test
    void lasLecturasExplicanLosResultados() {
        AnalisisEstadistico a = servicio.analizar(DESDE, HASTA);

        List<String> tendencia = LecturaEstadistica.tendencia(a, Moneda.PEN);
        assertTrue(tendencia.get(0).startsWith("Las ventas crecen en promedio S/ 200.00 por mes"), tendencia.toString());
        assertTrue(tendencia.contains("El mes en curso no se incluye porque aún no termina."));
        assertTrue(LecturaEstadistica.abc(a).get(0).startsWith("1 de 3 productos"), LecturaEstadistica.abc(a).toString());
        assertTrue(LecturaEstadistica.correlaciones(a).stream().anyMatch(f -> f.contains("positiva fuerte")));
        assertTrue(LecturaEstadistica.resumen(a, Moneda.PEN).get(0).contains("mediana) fue de S/ 1,200.00"),
                LecturaEstadistica.resumen(a, Moneda.PEN).toString());
    }

    @Test
    void unPeriodoPasadoUsaSusPropiosMeses() {
        AnalisisEstadistico a = servicio.analizar(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 10, 31));

        assertTrue(!a.mesActualExcluido());
        assertEquals(YearMonth.of(2025, 10), a.serieMensual().get(a.serieMensual().size() - 1).mes());
        assertEquals(3, a.ticket().n());
    }

    @Test
    void requiereElPermisoDeAnalisis() {
        Usuario operador = entorno.crearUsuario("operador", "OPERADOR");
        entorno.empresaService.asignarAccesos(empresa.id(), Set.of(operador.id()));
        entorno.entrarComo("operador", "Temporal123");
        entorno.empresaService.restaurarEmpresaActiva();

        assertThrows(AccesoDenegadoException.class, () -> servicio.analizar(DESDE, HASTA));
    }

    private Producto producto(String nombre, String costo, String precio) {
        return entorno.catalogo.crearProducto(new DatosProducto(TipoItem.PRODUCTO, null, nombre, null, null,
                UnidadMedida.UNIDAD, new BigDecimal(costo), new BigDecimal(precio)));
    }

    private void operacion(TipoOperacion tipo, LocalDate fecha, DatosLinea... lineas) {
        entorno.operaciones.registrar(new DatosOperacion(tipo, fecha, null, null, null, BigDecimal.ZERO,
                null, List.of(lineas), false), false);
    }

    private static DatosLinea linea(Producto p, String cantidad, String precio) {
        return new DatosLinea(p.id(), new BigDecimal(cantidad), new BigDecimal(precio));
    }
}
