package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.simulacion.BaseSimulacionFGEM;
import com.businessanalyzer.analytics.simulacion.EscenarioPredefinidoFGEM;
import com.businessanalyzer.analytics.simulacion.SupuestosFGEM;
import com.businessanalyzer.analytics.simulacion.VariableFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.DatosMovimientoFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM.DatosLineaFGEM;
import com.businessanalyzer.model.DatosProductoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EscenarioFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.TipoMovimientoFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.repository.EscenarioRepositoryFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reloj de pruebas en 2026-01-15: el último mes completo es diciembre de 2025.
 * Diciembre: ventas 1000 (costo 600), alquiler 200 (fijo), flete 50 (variable),
 * intereses ganados 10, impuesto a la renta 30 -> UAI 160.
 */
class SimulacionServiceTestFGEM {

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private SimulacionServiceFGEM servicioFGEM;
    private AnalisisServiceFGEM analisisFGEM;
    private EmpresaFGEM empresaFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = new SimulacionServiceFGEM(entornoFGEM.databaseFGEM, new OperacionRepositoryFGEM(), new MovimientoRepositoryFGEM(),
                new EscenarioRepositoryFGEM(), entornoFGEM.empresaServiceFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM);
        analisisFGEM = new AnalisisServiceFGEM(entornoFGEM.databaseFGEM, new OperacionRepositoryFGEM(), new MovimientoRepositoryFGEM(),
                entornoFGEM.empresaServiceFGEM, entornoFGEM.sesionFGEM);
        entornoFGEM.administradorConectadoFGEM();
        empresaFGEM = entornoFGEM.crearEmpresaActivaFGEM("Comercial Andina");

        ProductoFGEM pFGEM = entornoFGEM.catalogoFGEM.crearProductoFGEM(new DatosProductoFGEM(TipoItemFGEM.SERVICIO, null, "Servicio", null, null,
                UnidadMedidaFGEM.SERVICIO, new BigDecimal("60"), new BigDecimal("100")));
        entornoFGEM.operacionesFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.VENTA, LocalDate.of(2025, 12, 10), null, null,
                null, BigDecimal.ZERO, null, List.of(new DatosLineaFGEM(pFGEM.idFGEM(), BigDecimal.TEN, new BigDecimal("100"))), false), false);
        movimientoFGEM(TipoMovimientoFGEM.GASTO, CategoriaFinancieraFGEM.ALQUILER, "200");
        movimientoFGEM(TipoMovimientoFGEM.GASTO, CategoriaFinancieraFGEM.TRANSPORTE, "50");
        movimientoFGEM(TipoMovimientoFGEM.GASTO, CategoriaFinancieraFGEM.IMPUESTO_RENTA, "30");
        movimientoFGEM(TipoMovimientoFGEM.INGRESO, CategoriaFinancieraFGEM.INTERESES_GANADOS, "10");
    }

    @Test
    void laBaseCoincideConElEstadoDeResultadosFGEM() {
        BaseSimulacionFGEM baseFGEM = servicioFGEM.baseFGEM(SimulacionServiceFGEM.TipoBaseFGEM.ULTIMO_MES);

        assertMontoFGEM("1000", baseFGEM.ventasFGEM());
        assertMontoFGEM("600", baseFGEM.costoVentasFGEM());
        assertMontoFGEM("50", baseFGEM.gastosVariablesFGEM());
        assertMontoFGEM("200", baseFGEM.costosFijosFGEM());
        assertMontoFGEM("10", baseFGEM.otrosIngresosFGEM());
        assertMontoFGEM("160", baseFGEM.utilidadAntesImpuestosFGEM());
        BigDecimal uaiEstadoFGEM = analisisFGEM.resumenFGEM(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31))
                .estadoFGEM().utilidadAntesImpuestosFGEM();
        assertMontoFGEM(uaiEstadoFGEM.toPlainString(), baseFGEM.utilidadAntesImpuestosFGEM());
        assertEquals(new BigDecimal("0.187500"), baseFGEM.tasaImpuestoEfectivaFGEM());
    }

    @Test
    void laBasePromedioDivideEntreLosMesesFGEM() {
        BaseSimulacionFGEM baseFGEM = servicioFGEM.baseFGEM(SimulacionServiceFGEM.TipoBaseFGEM.PROMEDIO_3_MESES);

        assertEquals(3, baseFGEM.mesesFGEM());
        assertEquals(LocalDate.of(2025, 10, 1), baseFGEM.periodoFGEM().desdeFGEM());
        // 1000 ÷ 3 se guarda con precisión completa (333.3333…) y se redondea solo al mostrar.
        assertMontoFGEM("333.33", baseFGEM.ventasFGEM().setScale(2, java.math.RoundingMode.HALF_UP));
        assertTrue(baseFGEM.ventasFGEM().scale() > 2);
    }

    @Test
    void guardaActualizaYEliminaEscenariosFGEM() {
        EscenarioFGEM pesimistaFGEM = servicioFGEM.guardarFGEM("Pesimista", "Caída de ventas",
                EscenarioPredefinidoFGEM.PESIMISTA.aplicarFGEM(new BigDecimal("0.295")));
        assertEquals(0, new BigDecimal("-0.15").compareTo(pesimistaFGEM.supuestosFGEM().volumenFGEM()));
        assertEquals(0, new BigDecimal("0.295").compareTo(pesimistaFGEM.supuestosFGEM().tasaImpuestoFGEM()));

        EscenarioFGEM actualizadoFGEM = servicioFGEM.guardarFGEM("PESIMISTA", null,
                SupuestosFGEM.neutrosFGEM(BigDecimal.ZERO).conFGEM(VariableFGEM.VOLUMEN, new BigDecimal("-0.25")));
        assertEquals(pesimistaFGEM.idFGEM(), actualizadoFGEM.idFGEM(), "El mismo nombre reemplaza al escenario");
        assertEquals(1, servicioFGEM.listarEscenariosFGEM().size());
        assertEquals(0, new BigDecimal("-0.25").compareTo(servicioFGEM.listarEscenariosFGEM().get(0).supuestosFGEM().volumenFGEM()));

        servicioFGEM.eliminarFGEM(pesimistaFGEM.idFGEM());
        assertTrue(servicioFGEM.listarEscenariosFGEM().isEmpty());
    }

    @Test
    void validaLosSupuestosYElNombreFGEM() {
        SupuestosFGEM neutrosFGEM = SupuestosFGEM.neutrosFGEM(BigDecimal.ZERO);
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.guardarFGEM("", null, neutrosFGEM));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.guardarFGEM("Mal", null,
                neutrosFGEM.conFGEM(VariableFGEM.PRECIO, new BigDecimal("-1.5"))));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.guardarFGEM("Mal", null,
                neutrosFGEM.conTasaImpuestoFGEM(new BigDecimal("1.2"))));
    }

    @Test
    void losEscenariosSonDeCadaEmpresaFGEM() {
        EscenarioFGEM eFGEM = servicioFGEM.guardarFGEM("Optimista", null, EscenarioPredefinidoFGEM.OPTIMISTA.aplicarFGEM(BigDecimal.ZERO));
        entornoFGEM.crearEmpresaActivaFGEM("Otra empresa");

        assertTrue(servicioFGEM.listarEscenariosFGEM().isEmpty());
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.eliminarFGEM(eFGEM.idFGEM()));
    }

    @Test
    void requiereElPermisoDeAnalisisFGEM() {
        UsuarioFGEM operadorFGEM = entornoFGEM.crearUsuarioFGEM("operador", "OPERADOR");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of(operadorFGEM.idFGEM()));
        entornoFGEM.entrarComoFGEM("operador", "Temporal123");
        entornoFGEM.empresaServiceFGEM.restaurarEmpresaActivaFGEM();

        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.baseFGEM(SimulacionServiceFGEM.TipoBaseFGEM.ULTIMO_MES));
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.listarEscenariosFGEM());
    }

    private void movimientoFGEM(TipoMovimientoFGEM tipoFGEM, CategoriaFinancieraFGEM categoriaFGEM, String montoFGEM) {
        entornoFGEM.finanzasFGEM.registrarFGEM(new DatosMovimientoFGEM(tipoFGEM, categoriaFGEM, null, LocalDate.of(2025, 12, 20),
                categoriaFGEM.descripcionFGEM(), new BigDecimal(montoFGEM), null));
    }

    private static void assertMontoFGEM(String esperadoFGEM, BigDecimal realFGEM) {
        assertEquals(0, new BigDecimal(esperadoFGEM).compareTo(realFGEM), "se esperaba " + esperadoFGEM + " y se obtuvo " + realFGEM);
    }
}
