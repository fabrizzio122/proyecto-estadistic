package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.AnalisisFinanciero;
import com.businessanalyzer.analytics.EstadoResultados;
import com.businessanalyzer.analytics.LecturaFinanciera;
import com.businessanalyzer.analytics.PuntoEquilibrio;
import com.businessanalyzer.analytics.RentabilidadProducto;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.model.CategoriaFinanciera;
import com.businessanalyzer.model.DatosMovimiento;
import com.businessanalyzer.model.DatosOperacion;
import com.businessanalyzer.model.DatosOperacion.DatosLinea;
import com.businessanalyzer.model.DatosProducto;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Operacion;
import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.TipoItem;
import com.businessanalyzer.model.TipoMovimiento;
import com.businessanalyzer.model.TipoOperacion;
import com.businessanalyzer.model.UnidadMedida;
import com.businessanalyzer.model.Usuario;
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
 * Escenario con valores verificables a mano (reloj de pruebas en 2026-01-15):
 * <pre>
 * Enero:  ventas 1800 (10 A a 100 + 20 B a 40), costo de ventas 1200 (10 x 60 + 20 x 30)
 *         gastos operativos 440 (alquiler 200 fijo, sueldos 150 fijo, flete 90 variable)
 *         intereses pagados 30 (fijo), intereses ganados 10, otros ingresos 5, impuesto a la renta 20
 *  -> utilidad bruta 600, operativa 160, antes de impuestos 145, neta 125
 *  -> costos variables 1290, fijos 380, razón de contribución 510/1800, equilibrio 1341.18
 * Diciembre: ventas 500 (5 A), costo 300, alquiler 200 -> utilidad neta 0
 * </pre>
 */
class AnalisisServiceTest {

    private static final YearMonth ENERO = YearMonth.of(2026, 1);
    private static final YearMonth DICIEMBRE = YearMonth.of(2025, 12);

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private AnalisisService servicio;
    private Empresa empresa;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = new AnalisisService(entorno.database, new com.businessanalyzer.repository.OperacionRepository(),
                new com.businessanalyzer.repository.MovimientoRepository(), entorno.empresaService, entorno.sesion);
        entorno.administradorConectado();
        empresa = entorno.crearEmpresaActiva("Comercial Andina");

        Producto a = producto("Producto A", "60", "100");
        Producto b = producto("Producto B", "30", "40");
        operacion(TipoOperacion.COMPRA, LocalDate.of(2025, 12, 1), linea(a, "100", "60"), linea(b, "100", "30"));
        operacion(TipoOperacion.VENTA, LocalDate.of(2025, 12, 10), linea(a, "5", "100"));
        gasto(CategoriaFinanciera.ALQUILER, LocalDate.of(2025, 12, 5), "200");

        operacion(TipoOperacion.VENTA, LocalDate.of(2026, 1, 5), linea(a, "10", "100"));
        operacion(TipoOperacion.VENTA, LocalDate.of(2026, 1, 8), linea(b, "20", "40"));
        Operacion anulada = operacion(TipoOperacion.VENTA, LocalDate.of(2026, 1, 9), linea(a, "3", "100"));
        entorno.operaciones.anular(anulada.id(), "Venta registrada por error");
        gasto(CategoriaFinanciera.ALQUILER, LocalDate.of(2026, 1, 5), "200");
        gasto(CategoriaFinanciera.SUELDOS, LocalDate.of(2026, 1, 30), "150");
        gasto(CategoriaFinanciera.TRANSPORTE, LocalDate.of(2026, 1, 7), "90");
        gasto(CategoriaFinanciera.INTERESES_PRESTAMOS, LocalDate.of(2026, 1, 10), "30");
        gasto(CategoriaFinanciera.IMPUESTO_RENTA, LocalDate.of(2026, 1, 12), "20");
        ingreso(CategoriaFinanciera.INTERESES_GANADOS, "10");
        ingreso(CategoriaFinanciera.OTROS_INGRESOS, "5");
    }

    @Test
    void armaElEstadoDeResultados() {
        EstadoResultados e = servicio.analizar(ENERO.atDay(1), ENERO.atEndOfMonth()).actual();

        assertMonto("1800", e.ventasNetas());
        assertMonto("1200", e.costoVentas());
        assertMonto("600", e.utilidadBruta());
        assertMonto("440", e.gastosOperativos());
        assertMonto("160", e.utilidadOperativa());
        assertMonto("10", e.ingresosFinancieros());
        assertMonto("30", e.gastosFinancieros());
        assertMonto("5", e.otrosIngresos());
        assertMonto("145", e.utilidadAntesImpuestos());
        assertMonto("20", e.impuestoRenta());
        assertMonto("125", e.utilidadNeta());
        assertEquals(new BigDecimal("0.333333"), e.margenBruto());
        assertEquals(new BigDecimal("0.069444"), e.margenNeto());
    }

    @Test
    void calculaElPuntoDeEquilibrio() {
        PuntoEquilibrio pe = servicio.analizar(ENERO.atDay(1), ENERO.atEndOfMonth()).equilibrio();

        assertMonto("1290", pe.costosVariables());
        assertMonto("380", pe.costosFijos(), "Sin el impuesto a la renta");
        assertMonto("510", pe.margenContribucion());
        assertEquals(new BigDecimal("0.283333"), pe.razonContribucion());
        assertEquals(new BigDecimal("1341.18"), pe.ventasEquilibrio());
        assertTrue(pe.alcanzado());
    }

    @Test
    void comparaConElMesAnterior() {
        AnalisisFinanciero a = servicio.analizar(ENERO.atDay(1), ENERO.atEndOfMonth());

        assertEquals(DICIEMBRE.atDay(1), a.periodoAnterior().desde());
        assertMonto("500", a.anterior().ventasNetas());
        assertMonto("0", a.anterior().utilidadNeta());
    }

    @Test
    void rentabilidadPorProductoOrdenadaPorMargen() {
        List<RentabilidadProducto> productos = servicio.analizar(ENERO.atDay(1), ENERO.atEndOfMonth()).productos();

        assertEquals(2, productos.size(), "La venta anulada no cuenta");
        assertEquals("Producto A", productos.get(0).nombre());
        assertMonto("10", productos.get(0).cantidad());
        assertMonto("400", productos.get(0).margen());
        assertMonto("200", productos.get(1).margen());
        assertEquals(new BigDecimal("0.250000"), productos.get(1).margenSobreVentas());
    }

    @Test
    void evolucionDeLosUltimosDoceMeses() {
        List<AnalisisFinanciero.ResultadoMensual> evolucion =
                servicio.analizar(ENERO.atDay(1), ENERO.atEndOfMonth()).evolucion();

        assertEquals(12, evolucion.size());
        assertEquals(ENERO, evolucion.get(11).mes());
        assertMonto("1800", evolucion.get(11).estado().ventasNetas());
        assertMonto("125", evolucion.get(11).estado().utilidadNeta());
        assertMonto("500", evolucion.get(10).estado().ventasNetas());
        assertMonto("0", evolucion.get(0).estado().ventasNetas());
    }

    @Test
    void laLecturaResumeLoImportante() {
        List<String> frases = LecturaFinanciera.generar(servicio.analizar(ENERO.atDay(1), ENERO.atEndOfMonth()), Moneda.PEN);

        assertTrue(frases.stream().anyMatch(f -> f.startsWith("El negocio ganó S/ 125.00")), frases.toString());
        assertTrue(frases.stream().anyMatch(f -> f.contains("subieron 260.0 %")), frases.toString());
        assertTrue(frases.stream().anyMatch(f -> f.contains("superan el punto de equilibrio")), frases.toString());
        assertTrue(frases.stream().anyMatch(f -> f.startsWith("\"Producto A\" es el que más aporta")), frases.toString());
    }

    @Test
    void elResumenDelDashboardCoincideConElAnalisis() {
        AnalisisService.ResumenPeriodo r = servicio.resumen(ENERO.atDay(1), ENERO.atEndOfMonth());
        assertMonto("125", r.estado().utilidadNeta());
        assertEquals(new BigDecimal("1341.18"), r.equilibrio().ventasEquilibrio());
    }

    @Test
    void requiereElPermisoDeAnalisis() {
        Usuario operador = entorno.crearUsuario("operador", "OPERADOR");
        entorno.empresaService.asignarAccesos(empresa.id(), Set.of(operador.id()));
        entorno.entrarComo("operador", "Temporal123");
        entorno.empresaService.restaurarEmpresaActiva();

        assertThrows(AccesoDenegadoException.class, () -> servicio.analizar(ENERO.atDay(1), ENERO.atEndOfMonth()));
        assertThrows(AccesoDenegadoException.class, () -> servicio.resumen(ENERO.atDay(1), ENERO.atEndOfMonth()));
    }

    // ------------------------------------------------------------------ utilidades

    private Producto producto(String nombre, String costo, String precio) {
        return entorno.catalogo.crearProducto(new DatosProducto(TipoItem.PRODUCTO, null, nombre, null, null,
                UnidadMedida.UNIDAD, new BigDecimal(costo), new BigDecimal(precio)));
    }

    private Operacion operacion(TipoOperacion tipo, LocalDate fecha, DatosLinea... lineas) {
        return entorno.operaciones.registrar(new DatosOperacion(tipo, fecha, null, null, null, new BigDecimal("18"),
                null, List.of(lineas), false), false);
    }

    private static DatosLinea linea(Producto p, String cantidad, String precio) {
        return new DatosLinea(p.id(), new BigDecimal(cantidad), new BigDecimal(precio));
    }

    private void gasto(CategoriaFinanciera categoria, LocalDate fecha, String monto) {
        entorno.finanzas.registrar(new DatosMovimiento(TipoMovimiento.GASTO, categoria, null, fecha,
                categoria.descripcion(), new BigDecimal(monto), null));
    }

    private void ingreso(CategoriaFinanciera categoria, String monto) {
        entorno.finanzas.registrar(new DatosMovimiento(TipoMovimiento.INGRESO, categoria, null,
                LocalDate.of(2026, 1, 14), categoria.descripcion(), new BigDecimal(monto), null));
    }

    private static void assertMonto(String esperado, BigDecimal real) {
        assertMonto(esperado, real, null);
    }

    private static void assertMonto(String esperado, BigDecimal real, String mensaje) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                (mensaje == null ? "" : mensaje + ": ") + "se esperaba " + esperado + " y se obtuvo " + real);
    }
}
