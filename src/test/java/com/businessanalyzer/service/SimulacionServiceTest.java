package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.simulacion.BaseSimulacion;
import com.businessanalyzer.analytics.simulacion.EscenarioPredefinido;
import com.businessanalyzer.analytics.simulacion.Supuestos;
import com.businessanalyzer.analytics.simulacion.Variable;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CategoriaFinanciera;
import com.businessanalyzer.model.DatosMovimiento;
import com.businessanalyzer.model.DatosOperacion;
import com.businessanalyzer.model.DatosOperacion.DatosLinea;
import com.businessanalyzer.model.DatosProducto;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Escenario;
import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.TipoItem;
import com.businessanalyzer.model.TipoMovimiento;
import com.businessanalyzer.model.TipoOperacion;
import com.businessanalyzer.model.UnidadMedida;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.repository.EscenarioRepository;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
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
class SimulacionServiceTest {

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private SimulacionService servicio;
    private AnalisisService analisis;
    private Empresa empresa;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = new SimulacionService(entorno.database, new OperacionRepository(), new MovimientoRepository(),
                new EscenarioRepository(), entorno.empresaService, entorno.sesion, entorno.reloj);
        analisis = new AnalisisService(entorno.database, new OperacionRepository(), new MovimientoRepository(),
                entorno.empresaService, entorno.sesion);
        entorno.administradorConectado();
        empresa = entorno.crearEmpresaActiva("Comercial Andina");

        Producto p = entorno.catalogo.crearProducto(new DatosProducto(TipoItem.SERVICIO, null, "Servicio", null, null,
                UnidadMedida.SERVICIO, new BigDecimal("60"), new BigDecimal("100")));
        entorno.operaciones.registrar(new DatosOperacion(TipoOperacion.VENTA, LocalDate.of(2025, 12, 10), null, null,
                null, BigDecimal.ZERO, null, List.of(new DatosLinea(p.id(), BigDecimal.TEN, new BigDecimal("100"))), false), false);
        movimiento(TipoMovimiento.GASTO, CategoriaFinanciera.ALQUILER, "200");
        movimiento(TipoMovimiento.GASTO, CategoriaFinanciera.TRANSPORTE, "50");
        movimiento(TipoMovimiento.GASTO, CategoriaFinanciera.IMPUESTO_RENTA, "30");
        movimiento(TipoMovimiento.INGRESO, CategoriaFinanciera.INTERESES_GANADOS, "10");
    }

    @Test
    void laBaseCoincideConElEstadoDeResultados() {
        BaseSimulacion base = servicio.base(SimulacionService.TipoBase.ULTIMO_MES);

        assertMonto("1000", base.ventas());
        assertMonto("600", base.costoVentas());
        assertMonto("50", base.gastosVariables());
        assertMonto("200", base.costosFijos());
        assertMonto("10", base.otrosIngresos());
        assertMonto("160", base.utilidadAntesImpuestos());
        BigDecimal uaiEstado = analisis.resumen(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31))
                .estado().utilidadAntesImpuestos();
        assertMonto(uaiEstado.toPlainString(), base.utilidadAntesImpuestos());
        assertEquals(new BigDecimal("0.187500"), base.tasaImpuestoEfectiva());
    }

    @Test
    void laBasePromedioDivideEntreLosMeses() {
        BaseSimulacion base = servicio.base(SimulacionService.TipoBase.PROMEDIO_3_MESES);

        assertEquals(3, base.meses());
        assertEquals(LocalDate.of(2025, 10, 1), base.periodo().desde());
        assertMonto("333.33", base.ventas());
    }

    @Test
    void guardaActualizaYEliminaEscenarios() {
        Escenario pesimista = servicio.guardar("Pesimista", "Caída de ventas",
                EscenarioPredefinido.PESIMISTA.aplicar(new BigDecimal("0.295")));
        assertEquals(0, new BigDecimal("-0.15").compareTo(pesimista.supuestos().volumen()));
        assertEquals(0, new BigDecimal("0.295").compareTo(pesimista.supuestos().tasaImpuesto()));

        Escenario actualizado = servicio.guardar("PESIMISTA", null,
                Supuestos.neutros(BigDecimal.ZERO).con(Variable.VOLUMEN, new BigDecimal("-0.25")));
        assertEquals(pesimista.id(), actualizado.id(), "El mismo nombre reemplaza al escenario");
        assertEquals(1, servicio.listarEscenarios().size());
        assertEquals(0, new BigDecimal("-0.25").compareTo(servicio.listarEscenarios().get(0).supuestos().volumen()));

        servicio.eliminar(pesimista.id());
        assertTrue(servicio.listarEscenarios().isEmpty());
    }

    @Test
    void validaLosSupuestosYElNombre() {
        Supuestos neutros = Supuestos.neutros(BigDecimal.ZERO);
        assertThrows(ValidacionException.class, () -> servicio.guardar("", null, neutros));
        assertThrows(ValidacionException.class, () -> servicio.guardar("Mal", null,
                neutros.con(Variable.PRECIO, new BigDecimal("-1.5"))));
        assertThrows(ValidacionException.class, () -> servicio.guardar("Mal", null,
                neutros.conTasaImpuesto(new BigDecimal("1.2"))));
    }

    @Test
    void losEscenariosSonDeCadaEmpresa() {
        Escenario e = servicio.guardar("Optimista", null, EscenarioPredefinido.OPTIMISTA.aplicar(BigDecimal.ZERO));
        entorno.crearEmpresaActiva("Otra empresa");

        assertTrue(servicio.listarEscenarios().isEmpty());
        assertThrows(ValidacionException.class, () -> servicio.eliminar(e.id()));
    }

    @Test
    void requiereElPermisoDeAnalisis() {
        Usuario operador = entorno.crearUsuario("operador", "OPERADOR");
        entorno.empresaService.asignarAccesos(empresa.id(), Set.of(operador.id()));
        entorno.entrarComo("operador", "Temporal123");
        entorno.empresaService.restaurarEmpresaActiva();

        assertThrows(AccesoDenegadoException.class, () -> servicio.base(SimulacionService.TipoBase.ULTIMO_MES));
        assertThrows(AccesoDenegadoException.class, () -> servicio.listarEscenarios());
    }

    private void movimiento(TipoMovimiento tipo, CategoriaFinanciera categoria, String monto) {
        entorno.finanzas.registrar(new DatosMovimiento(tipo, categoria, null, LocalDate.of(2025, 12, 20),
                categoria.descripcion(), new BigDecimal(monto), null));
    }

    private static void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "se esperaba " + esperado + " y se obtuvo " + real);
    }
}
