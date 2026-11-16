package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonial;
import com.businessanalyzer.analytics.simulacion.BaseSimulacion;
import com.businessanalyzer.analytics.tesoreria.SaldoCuenta;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.repository.EscenarioRepository;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatosDemoServiceTest {

    @TempDir
    Path carpeta;

    @Test
    void creaUnaEmpresaActivaConHistoriaYNoSeDuplica() {
        EntornoPrueba entorno = new EntornoPrueba(carpeta);
        entorno.administradorConectado();
        DatosDemoService demo = new DatosDemoService(entorno.empresaService, entorno.catalogo, entorno.operaciones,
                entorno.finanzas, entorno.patrimonio, entorno.tesoreria, entorno.sesion, entorno.reloj);

        Empresa empresa = demo.crearEmpresaDemo();

        assertEquals(empresa.id(), entorno.sesion.empresaActiva().orElseThrow().id());
        SimulacionService simulacion = new SimulacionService(entorno.database, new OperacionRepository(),
                new MovimientoRepository(), new EscenarioRepository(), entorno.empresaService, entorno.sesion, entorno.reloj);
        BaseSimulacion base = simulacion.base(SimulacionService.TipoBase.PROMEDIO_6_MESES);
        assertTrue(base.ventas().signum() > 0);
        assertTrue(base.utilidadAntesImpuestos().signum() > 0, "La demo debe ser rentable: " + base.utilidadAntesImpuestos());
        assertTrue(base.tasaImpuestoEfectiva().signum() > 0);

        SituacionPatrimonial patrimonio = entorno.patrimonio.situacion();
        assertEquals(4, patrimonio.balance().activosRegistrados());
        assertEquals(2, patrimonio.balance().deudasPendientes());
        assertTrue(patrimonio.balance().inventario().signum() > 0);
        assertTrue(patrimonio.balance().patrimonio().signum() > 0);
        assertTrue(patrimonio.indicadores().liquidezCorriente().compareTo(BigDecimal.ONE) > 0);
        assertTrue(patrimonio.indicadores().coberturaCuotas().compareTo(BigDecimal.ONE) > 0);

        // El dinero recorre caja, banco y Yape sin que ninguna cuenta quede en negativo.
        List<SaldoCuenta> saldos = entorno.tesoreria.listarCuentas(false);
        assertEquals(3, saldos.size());
        assertTrue(saldos.stream().noneMatch(SaldoCuenta::negativo), "Ninguna cuenta debe quedar en negativo");
        assertEquals(0, SaldoCuenta.total(saldos).compareTo(patrimonio.balance().efectivo()));
        // La evolución mensual tampoco pasa por saldos negativos al cierre de cada mes.
        assertTrue(entorno.tesoreria.evolucion(13).stream().allMatch(m -> m.saldoCierre().signum() > 0));
        TesoreriaService.Resumen resumen = entorno.tesoreria.resumen();
        assertTrue(resumen.porCobrar().signum() > 0);
        assertTrue(resumen.porCobrarVencido().signum() > 0, "La venta al restaurante está vencida");
        assertTrue(resumen.porPagar().signum() > 0);
        assertEquals(0, resumen.porPagarVencido().signum());
        assertTrue(entorno.tesoreria.proyeccion().suficiente());
        assertThrows(ValidacionException.class, demo::crearEmpresaDemo);
    }
}
