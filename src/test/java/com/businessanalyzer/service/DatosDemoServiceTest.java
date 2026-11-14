package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.simulacion.BaseSimulacion;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.repository.EscenarioRepository;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

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
                entorno.finanzas, entorno.sesion, entorno.reloj);

        Empresa empresa = demo.crearEmpresaDemo();

        assertEquals(empresa.id(), entorno.sesion.empresaActiva().orElseThrow().id());
        SimulacionService simulacion = new SimulacionService(entorno.database, new OperacionRepository(),
                new MovimientoRepository(), new EscenarioRepository(), entorno.empresaService, entorno.sesion, entorno.reloj);
        BaseSimulacion base = simulacion.base(SimulacionService.TipoBase.PROMEDIO_6_MESES);
        assertTrue(base.ventas().signum() > 0);
        assertTrue(base.utilidadAntesImpuestos().signum() > 0, "La demo debe ser rentable: " + base.utilidadAntesImpuestos());
        assertTrue(base.tasaImpuestoEfectiva().signum() > 0);
        assertThrows(ValidacionException.class, demo::crearEmpresaDemo);
    }
}
