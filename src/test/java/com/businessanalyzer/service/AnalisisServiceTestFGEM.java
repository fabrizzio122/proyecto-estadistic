package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.AnalisisFinancieroFGEM;
import com.businessanalyzer.analytics.EstadoResultadosFGEM;
import com.businessanalyzer.analytics.LecturaFinancieraFGEM;
import com.businessanalyzer.analytics.PuntoEquilibrioFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.DatosMovimientoFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM.DatosLineaFGEM;
import com.businessanalyzer.model.DatosProductoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.OperacionFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.TipoMovimientoFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
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

class AnalisisServiceTestFGEM {

    private static final YearMonth ENERO_FGEM = YearMonth.of(2026, 1);
    private static final YearMonth DICIEMBRE_FGEM = YearMonth.of(2025, 12);

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private AnalisisServiceFGEM servicioFGEM;
    private EmpresaFGEM empresaFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = new AnalisisServiceFGEM(entornoFGEM.databaseFGEM, new com.businessanalyzer.repository.OperacionRepositoryFGEM(),
                new com.businessanalyzer.repository.MovimientoRepositoryFGEM(), entornoFGEM.empresaServiceFGEM, entornoFGEM.sesionFGEM);
        entornoFGEM.administradorConectadoFGEM();
        empresaFGEM = entornoFGEM.crearEmpresaActivaFGEM("Comercial Andina");

        ProductoFGEM aFGEM = productoFGEM("Producto A", "60", "100");
        ProductoFGEM bFGEM = productoFGEM("Producto B", "30", "40");
        operacionFGEM(TipoOperacionFGEM.COMPRA, LocalDate.of(2025, 12, 1), lineaFGEM(aFGEM, "100", "60"), lineaFGEM(bFGEM, "100", "30"));
        operacionFGEM(TipoOperacionFGEM.VENTA, LocalDate.of(2025, 12, 10), lineaFGEM(aFGEM, "5", "100"));
        gastoFGEM(CategoriaFinancieraFGEM.ALQUILER, LocalDate.of(2025, 12, 5), "200");

        operacionFGEM(TipoOperacionFGEM.VENTA, LocalDate.of(2026, 1, 5), lineaFGEM(aFGEM, "10", "100"));
        operacionFGEM(TipoOperacionFGEM.VENTA, LocalDate.of(2026, 1, 8), lineaFGEM(bFGEM, "20", "40"));
        OperacionFGEM anuladaFGEM = operacionFGEM(TipoOperacionFGEM.VENTA, LocalDate.of(2026, 1, 9), lineaFGEM(aFGEM, "3", "100"));
        entornoFGEM.operacionesFGEM.anularFGEM(anuladaFGEM.idFGEM(), "Venta registrada por error");
        gastoFGEM(CategoriaFinancieraFGEM.ALQUILER, LocalDate.of(2026, 1, 5), "200");
        gastoFGEM(CategoriaFinancieraFGEM.SUELDOS, LocalDate.of(2026, 1, 30), "150");
        gastoFGEM(CategoriaFinancieraFGEM.TRANSPORTE, LocalDate.of(2026, 1, 7), "90");
        gastoFGEM(CategoriaFinancieraFGEM.INTERESES_PRESTAMOS, LocalDate.of(2026, 1, 10), "30");
        gastoFGEM(CategoriaFinancieraFGEM.IMPUESTO_RENTA, LocalDate.of(2026, 1, 12), "20");
        ingresoFGEM(CategoriaFinancieraFGEM.INTERESES_GANADOS, "10");
        ingresoFGEM(CategoriaFinancieraFGEM.OTROS_INGRESOS, "5");
    }

    @Test
    void armaElEstadoDeResultadosFGEM() {
        EstadoResultadosFGEM eFGEM = servicioFGEM.analizarFGEM(ENERO_FGEM.atDay(1), ENERO_FGEM.atEndOfMonth()).actualFGEM();

        assertMontoFGEM("1800", eFGEM.ventasNetasFGEM());
        assertMontoFGEM("1200", eFGEM.costoVentasFGEM());
        assertMontoFGEM("600", eFGEM.utilidadBrutaFGEM());
        assertMontoFGEM("440", eFGEM.gastosOperativosFGEM());
        assertMontoFGEM("160", eFGEM.utilidadOperativaFGEM());
        assertMontoFGEM("10", eFGEM.ingresosFinancierosFGEM());
        assertMontoFGEM("30", eFGEM.gastosFinancierosFGEM());
        assertMontoFGEM("5", eFGEM.otrosIngresosFGEM());
        assertMontoFGEM("145", eFGEM.utilidadAntesImpuestosFGEM());
        assertMontoFGEM("20", eFGEM.impuestoRentaFGEM());
        assertMontoFGEM("125", eFGEM.utilidadNetaFGEM());
        assertEquals(new BigDecimal("0.333333"), eFGEM.margenBrutoFGEM());
        assertEquals(new BigDecimal("0.069444"), eFGEM.margenNetoFGEM());
    }

    @Test
    void calculaElPuntoDeEquilibrioFGEM() {
        PuntoEquilibrioFGEM peFGEM = servicioFGEM.analizarFGEM(ENERO_FGEM.atDay(1), ENERO_FGEM.atEndOfMonth()).equilibrioFGEM();

        assertMontoFGEM("1290", peFGEM.costosVariablesFGEM());
        assertMontoFGEM("380", peFGEM.costosFijosFGEM(), "Sin el impuesto a la renta");
        assertMontoFGEM("510", peFGEM.margenContribucionFGEM());
        assertEquals(new BigDecimal("0.283333"), peFGEM.razonContribucionFGEM());
        assertEquals(new BigDecimal("1341.18"), peFGEM.ventasEquilibrioFGEM());
        assertTrue(peFGEM.alcanzadoFGEM());
    }

    @Test
    void comparaConElMesAnteriorFGEM() {
        AnalisisFinancieroFGEM aFGEM = servicioFGEM.analizarFGEM(ENERO_FGEM.atDay(1), ENERO_FGEM.atEndOfMonth());

        assertEquals(DICIEMBRE_FGEM.atDay(1), aFGEM.periodoAnteriorFGEM().desdeFGEM());
        assertMontoFGEM("500", aFGEM.anteriorFGEM().ventasNetasFGEM());
        assertMontoFGEM("0", aFGEM.anteriorFGEM().utilidadNetaFGEM());
    }

    @Test
    void rentabilidadPorProductoOrdenadaPorMargenFGEM() {
        List<RentabilidadProductoFGEM> productosFGEM = servicioFGEM.analizarFGEM(ENERO_FGEM.atDay(1), ENERO_FGEM.atEndOfMonth()).productosFGEM();

        assertEquals(2, productosFGEM.size(), "La venta anulada no cuenta");
        assertEquals("Producto A", productosFGEM.get(0).nombreFGEM());
        assertMontoFGEM("10", productosFGEM.get(0).cantidadFGEM());
        assertMontoFGEM("400", productosFGEM.get(0).margenFGEM());
        assertMontoFGEM("200", productosFGEM.get(1).margenFGEM());
        assertEquals(new BigDecimal("0.250000"), productosFGEM.get(1).margenSobreVentasFGEM());
    }

    @Test
    void evolucionDeLosUltimosDoceMesesFGEM() {
        List<AnalisisFinancieroFGEM.ResultadoMensualFGEM> evolucionFGEM =
                servicioFGEM.analizarFGEM(ENERO_FGEM.atDay(1), ENERO_FGEM.atEndOfMonth()).evolucionFGEM();

        assertEquals(12, evolucionFGEM.size());
        assertEquals(ENERO_FGEM, evolucionFGEM.get(11).mesFGEM());
        assertMontoFGEM("1800", evolucionFGEM.get(11).estadoFGEM().ventasNetasFGEM());
        assertMontoFGEM("125", evolucionFGEM.get(11).estadoFGEM().utilidadNetaFGEM());
        assertMontoFGEM("500", evolucionFGEM.get(10).estadoFGEM().ventasNetasFGEM());
        assertMontoFGEM("0", evolucionFGEM.get(0).estadoFGEM().ventasNetasFGEM());
    }

    @Test
    void laLecturaResumeLoImportanteFGEM() {
        List<String> frasesFGEM = LecturaFinancieraFGEM.generarFGEM(servicioFGEM.analizarFGEM(ENERO_FGEM.atDay(1), ENERO_FGEM.atEndOfMonth()), MonedaFGEM.PEN);

        assertTrue(frasesFGEM.stream().anyMatch(fFGEM -> fFGEM.startsWith("El negocio ganó S/ 125.00")), frasesFGEM.toString());
        assertTrue(frasesFGEM.stream().anyMatch(fFGEM -> fFGEM.contains("subieron 260.0 %")), frasesFGEM.toString());
        assertTrue(frasesFGEM.stream().anyMatch(fFGEM -> fFGEM.contains("superan el punto de equilibrio")), frasesFGEM.toString());
        assertTrue(frasesFGEM.stream().anyMatch(fFGEM -> fFGEM.startsWith("\"Producto A\" es el que más aporta")), frasesFGEM.toString());
    }

    @Test
    void elResumenDelDashboardCoincideConElAnalisisFGEM() {
        AnalisisServiceFGEM.ResumenPeriodoFGEM rFGEM = servicioFGEM.resumenFGEM(ENERO_FGEM.atDay(1), ENERO_FGEM.atEndOfMonth());
        assertMontoFGEM("125", rFGEM.estadoFGEM().utilidadNetaFGEM());
        assertEquals(new BigDecimal("1341.18"), rFGEM.equilibrioFGEM().ventasEquilibrioFGEM());
    }

    @Test
    void requiereElPermisoDeAnalisisFGEM() {
        UsuarioFGEM operadorFGEM = entornoFGEM.crearUsuarioFGEM("operador", "OPERADOR");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of(operadorFGEM.idFGEM()));
        entornoFGEM.entrarComoFGEM("operador", "Temporal123");
        entornoFGEM.empresaServiceFGEM.restaurarEmpresaActivaFGEM();

        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.analizarFGEM(ENERO_FGEM.atDay(1), ENERO_FGEM.atEndOfMonth()));
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.resumenFGEM(ENERO_FGEM.atDay(1), ENERO_FGEM.atEndOfMonth()));
    }

    private ProductoFGEM productoFGEM(String nombreFGEM, String costoFGEM, String precioFGEM) {
        return entornoFGEM.catalogoFGEM.crearProductoFGEM(new DatosProductoFGEM(TipoItemFGEM.PRODUCTO, null, nombreFGEM, null, null,
                UnidadMedidaFGEM.UNIDAD, new BigDecimal(costoFGEM), new BigDecimal(precioFGEM)));
    }

    private OperacionFGEM operacionFGEM(TipoOperacionFGEM tipoFGEM, LocalDate fechaFGEM, DatosLineaFGEM... lineasFGEM) {
        return entornoFGEM.operacionesFGEM.registrarFGEM(new DatosOperacionFGEM(tipoFGEM, fechaFGEM, null, null, null, new BigDecimal("18"),
                null, List.of(lineasFGEM), false), false);
    }

    private static DatosLineaFGEM lineaFGEM(ProductoFGEM pFGEM, String cantidadFGEM, String precioFGEM) {
        return new DatosLineaFGEM(pFGEM.idFGEM(), new BigDecimal(cantidadFGEM), new BigDecimal(precioFGEM));
    }

    private void gastoFGEM(CategoriaFinancieraFGEM categoriaFGEM, LocalDate fechaFGEM, String montoFGEM) {
        entornoFGEM.finanzasFGEM.registrarFGEM(new DatosMovimientoFGEM(TipoMovimientoFGEM.GASTO, categoriaFGEM, null, fechaFGEM,
                categoriaFGEM.descripcionFGEM(), new BigDecimal(montoFGEM), null));
    }

    private void ingresoFGEM(CategoriaFinancieraFGEM categoriaFGEM, String montoFGEM) {
        entornoFGEM.finanzasFGEM.registrarFGEM(new DatosMovimientoFGEM(TipoMovimientoFGEM.INGRESO, categoriaFGEM, null,
                LocalDate.of(2026, 1, 14), categoriaFGEM.descripcionFGEM(), new BigDecimal(montoFGEM), null));
    }

    private static void assertMontoFGEM(String esperadoFGEM, BigDecimal realFGEM) {
        assertMontoFGEM(esperadoFGEM, realFGEM, null);
    }

    private static void assertMontoFGEM(String esperadoFGEM, BigDecimal realFGEM, String mensajeFGEM) {
        assertEquals(0, new BigDecimal(esperadoFGEM).compareTo(realFGEM),
                (mensajeFGEM == null ? "" : mensajeFGEM + ": ") + "se esperaba " + esperadoFGEM + " y se obtuvo " + realFGEM);
    }
}
