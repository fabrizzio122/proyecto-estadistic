package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.diagnostico.DiagnosticoFGEM;
import com.businessanalyzer.analytics.diagnostico.NivelSaludFGEM;
import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;
import com.businessanalyzer.analytics.diagnostico.TipoIndicadorFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
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

class DiagnosticoServiceTestFGEM {

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private DiagnosticoServiceFGEM servicioFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = new DiagnosticoServiceFGEM(entornoFGEM.databaseFGEM, new OperacionRepositoryFGEM(), new MovimientoRepositoryFGEM(),
                entornoFGEM.empresaServiceFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM);
        entornoFGEM.administradorConectadoFGEM();
    }

    @Test
    void empresaSinVentasNoTieneDiagnosticoFGEM() {
        entornoFGEM.crearEmpresaActivaFGEM("Nueva");
        DiagnosticoFGEM dFGEM = servicioFGEM.diagnosticarFGEM();

        assertEquals(NivelSaludFGEM.SIN_DATOS, dFGEM.nivelFGEM());
        assertEquals(LocalDate.of(2025, 10, 1), dFGEM.periodoFGEM().desdeFGEM());
        assertEquals(LocalDate.of(2025, 12, 31), dFGEM.periodoFGEM().hastaFGEM());
    }

    @Test
    void diagnosticaLaEmpresaDeDemostracionFGEM() {
        new DatosDemoServiceFGEM(entornoFGEM.empresaServiceFGEM, entornoFGEM.catalogoFGEM, entornoFGEM.operacionesFGEM, entornoFGEM.finanzasFGEM,
                entornoFGEM.patrimonioFGEM, entornoFGEM.tesoreriaFGEM,
                entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM).crearEmpresaDemoFGEM();

        DiagnosticoFGEM dFGEM = servicioFGEM.diagnosticarFGEM();

        assertTrue(dFGEM.evaluadoFGEM());
        assertEquals(java.util.Arrays.stream(TipoIndicadorFGEM.values()).filter(tFGEM -> !tFGEM.soloAgropecuarioFGEM()).count(), (long) dFGEM.hallazgosFGEM().size());
        assertTrue(dFGEM.hallazgosFGEM().stream().allMatch(hFGEM -> hFGEM.semaforoFGEM().evaluadoFGEM()), dFGEM.hallazgosFGEM().toString());
        assertTrue(dFGEM.hallazgosFGEM().stream().anyMatch(hFGEM -> hFGEM.tipoFGEM() == TipoIndicadorFGEM.PRODUCTOS_CON_PERDIDA
                && hFGEM.semaforoFGEM() != SemaforoFGEM.VERDE && hFGEM.lecturaFGEM().contains("Agua mineral")));
        assertFalse(dFGEM.recomendacionesFGEM().isEmpty());
        assertTrue(dFGEM.puntajeFGEM() >= 0 && dFGEM.puntajeFGEM() <= 100);
    }

    @Test
    void requiereElPermisoDeAnalisisFGEM() {
        EmpresaFGEM empresaFGEM = entornoFGEM.crearEmpresaActivaFGEM("Comercial");
        UsuarioFGEM operadorFGEM = entornoFGEM.crearUsuarioFGEM("operador", "OPERADOR");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of(operadorFGEM.idFGEM()));
        entornoFGEM.entrarComoFGEM("operador", "Temporal123");
        entornoFGEM.empresaServiceFGEM.restaurarEmpresaActivaFGEM();

        assertThrows(AccesoDenegadoExceptionFGEM.class, servicioFGEM::diagnosticarFGEM);
    }
}
