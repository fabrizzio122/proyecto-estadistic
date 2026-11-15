package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.diagnostico.Diagnostico;
import com.businessanalyzer.analytics.diagnostico.NivelSalud;
import com.businessanalyzer.analytics.diagnostico.Semaforo;
import com.businessanalyzer.analytics.diagnostico.TipoIndicador;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reloj de pruebas en 2026-01-15: se evalúan octubre a diciembre de 2025. */
class DiagnosticoServiceTest {

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private DiagnosticoService servicio;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = new DiagnosticoService(entorno.database, new OperacionRepository(), new MovimientoRepository(),
                entorno.empresaService, entorno.sesion, entorno.reloj);
        entorno.administradorConectado();
    }

    @Test
    void empresaSinVentasNoTieneDiagnostico() {
        entorno.crearEmpresaActiva("Nueva");
        Diagnostico d = servicio.diagnosticar();

        assertEquals(NivelSalud.SIN_DATOS, d.nivel());
        assertEquals(LocalDate.of(2025, 10, 1), d.periodo().desde());
        assertEquals(LocalDate.of(2025, 12, 31), d.periodo().hasta());
    }

    @Test
    void diagnosticaLaEmpresaDeDemostracion() {
        new DatosDemoService(entorno.empresaService, entorno.catalogo, entorno.operaciones, entorno.finanzas,
                entorno.patrimonio,
                entorno.sesion, entorno.reloj).crearEmpresaDemo();

        Diagnostico d = servicio.diagnosticar();

        assertTrue(d.evaluado());
        assertEquals(TipoIndicador.values().length, d.hallazgos().size());
        assertTrue(d.hallazgos().stream().allMatch(h -> h.semaforo().evaluado()), d.hallazgos().toString());
        // La demo vende el agua mineral por debajo de su costo.
        assertTrue(d.hallazgos().stream().anyMatch(h -> h.tipo() == TipoIndicador.PRODUCTOS_CON_PERDIDA
                && h.semaforo() != Semaforo.VERDE && h.lectura().contains("Agua mineral")));
        assertFalse(d.recomendaciones().isEmpty());
        assertTrue(d.puntaje() >= 0 && d.puntaje() <= 100);
    }

    @Test
    void requiereElPermisoDeAnalisis() {
        Empresa empresa = entorno.crearEmpresaActiva("Comercial");
        Usuario operador = entorno.crearUsuario("operador", "OPERADOR");
        entorno.empresaService.asignarAccesos(empresa.id(), Set.of(operador.id()));
        entorno.entrarComo("operador", "Temporal123");
        entorno.empresaService.restaurarEmpresaActiva();

        assertThrows(AccesoDenegadoException.class, servicio::diagnosticar);
    }
}
