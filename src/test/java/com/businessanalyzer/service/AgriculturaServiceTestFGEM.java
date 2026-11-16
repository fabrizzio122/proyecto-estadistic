package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.agricultura.ResultadoCampaniaFGEM;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneralFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CampaniaFGEM;
import com.businessanalyzer.model.CultivoFGEM;
import com.businessanalyzer.model.DatosCampaniaFGEM;
import com.businessanalyzer.model.DatosEmpresaFGEM;
import com.businessanalyzer.model.DatosParcelaFGEM;
import com.businessanalyzer.model.DestinoCosechaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EstadoCampaniaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.ParcelaFGEM;
import com.businessanalyzer.model.RubroCostoFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.TamanoEmpresaFGEM;
import com.businessanalyzer.model.TenenciaFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgriculturaServiceTestFGEM {

    /** El reloj de pruebas está en 2026-01-15. */
    private static final LocalDate HOY_FGEM = LocalDate.of(2026, 1, 15);
    private static final LocalDate SIEMBRA_FGEM = LocalDate.of(2025, 10, 10);

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private AgriculturaServiceFGEM servicioFGEM;
    private EmpresaFGEM fundoFGEM;
    private ParcelaFGEM parcelaFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = entornoFGEM.agriculturaFGEM;
        entornoFGEM.administradorConectadoFGEM();
        fundoFGEM = entornoFGEM.empresaServiceFGEM.crearFGEM(new DatosEmpresaFGEM("Fundo Los Andes", null, null, SectorFGEM.AGROPECUARIO,
                TamanoEmpresaFGEM.MICRO, MonedaFGEM.PEN, 1, null, null, null, null));
        entornoFGEM.empresaServiceFGEM.seleccionarEmpresaActivaFGEM(fundoFGEM.idFGEM());
        parcelaFGEM = servicioFGEM.registrarParcelaFGEM(new DatosParcelaFGEM("Parcela Alta", new BigDecimal("2"), TenenciaFGEM.PROPIA,
                false, null));
    }

    @Test
    void soloFuncionaEnEmpresasAgropecuariasFGEM() {
        entornoFGEM.crearEmpresaActivaFGEM("Bodega");
        assertInvalidoFGEM(() -> servicioFGEM.listarParcelasFGEM(false));
    }

    @Test
    void validaLasParcelasFGEM() {
        assertInvalidoFGEM(() -> servicioFGEM.registrarParcelaFGEM(new DatosParcelaFGEM("parcela alta", BigDecimal.ONE, TenenciaFGEM.PROPIA,
                false, null)));                                                                   // nombre repetido
        assertInvalidoFGEM(() -> servicioFGEM.registrarParcelaFGEM(new DatosParcelaFGEM("Otra", BigDecimal.ZERO, TenenciaFGEM.PROPIA, false, null)));
        assertInvalidoFGEM(() -> servicioFGEM.registrarParcelaFGEM(new DatosParcelaFGEM("Otra", BigDecimal.ONE, null, false, null)));

        ParcelaFGEM inactivaFGEM = servicioFGEM.cambiarParcelaActivaFGEM(parcelaFGEM.idFGEM(), false);
        assertFalse(inactivaFGEM.activaFGEM());
        assertEquals(0, servicioFGEM.listarParcelasFGEM(false).size());
        assertInvalidoFGEM(() -> campaniaFGEM(CultivoFGEM.PAPA, "1"));                                   // parcela desactivada
    }

    @Test
    void registraUnaCampaniaCompletaYCalculaSuResultadoFGEM() {
        CampaniaFGEM papaFGEM = campaniaFGEM(CultivoFGEM.PAPA, "1.5");
        assertEquals(0, new BigDecimal("15000").compareTo(papaFGEM.rendimientoReferenciaFGEM())); // propuesto por el cultivo

        servicioFGEM.registrarCostoFGEM(papaFGEM.idFGEM(), SIEMBRA_FGEM.minusDays(20), RubroCostoFGEM.PREPARACION, "Yunta", new BigDecimal("600"));
        servicioFGEM.registrarCostoFGEM(papaFGEM.idFGEM(), SIEMBRA_FGEM, RubroCostoFGEM.SEMILLA, "Semilla", new BigDecimal("3000"));
        servicioFGEM.registrarCosechaFGEM(papaFGEM.idFGEM(), HOY_FGEM, new BigDecimal("15000"), DestinoCosechaFGEM.VENTA, new BigDecimal("0.80"), null);
        ResultadoCampaniaFGEM rFGEM = servicioFGEM.registrarCosechaFGEM(papaFGEM.idFGEM(), HOY_FGEM, new BigDecimal("3000"),
                DestinoCosechaFGEM.AUTOCONSUMO, new BigDecimal("9"), null);

        assertEquals(0, new BigDecimal("3600").compareTo(rFGEM.costoTotalFGEM()));
        assertEquals(0, new BigDecimal("12000").compareTo(rFGEM.rendimientoFGEM()));    // 18 000 kg / 1.5 ha
        assertEquals(0, new BigDecimal("12000").compareTo(rFGEM.ingresoVentasFGEM()));

        CampaniaFGEM cerradaFGEM = servicioFGEM.cerrarCampaniaFGEM(papaFGEM.idFGEM(), EstadoCampaniaFGEM.COSECHADA, HOY_FGEM, null);
        assertEquals(EstadoCampaniaFGEM.COSECHADA, cerradaFGEM.estadoFGEM());
        assertInvalidoFGEM(() -> servicioFGEM.registrarCosechaFGEM(papaFGEM.idFGEM(), HOY_FGEM, BigDecimal.ONE, DestinoCosechaFGEM.VENTA, null, null));
        assertEquals(EstadoCampaniaFGEM.EN_CURSO, servicioFGEM.reabrirCampaniaFGEM(papaFGEM.idFGEM()).estadoFGEM());
    }

    @Test
    void validaCampaniasCostosYCosechasFGEM() {
        assertInvalidoFGEM(() -> campaniaFGEM(CultivoFGEM.PAPA, "3"));                                   // más que la parcela
        CampaniaFGEM quinuaFGEM = campaniaFGEM(CultivoFGEM.QUINUA, "1");
        assertInvalidoFGEM(() -> servicioFGEM.registrarCostoFGEM(quinuaFGEM.idFGEM(), SIEMBRA_FGEM.minusMonths(7), RubroCostoFGEM.SEMILLA, "Semilla",
                BigDecimal.TEN));                                                                // muy anterior a la siembra
        assertInvalidoFGEM(() -> servicioFGEM.registrarCostoFGEM(quinuaFGEM.idFGEM(), HOY_FGEM, RubroCostoFGEM.SEMILLA, "Semilla", BigDecimal.ZERO));
        assertInvalidoFGEM(() -> servicioFGEM.registrarCosechaFGEM(quinuaFGEM.idFGEM(), SIEMBRA_FGEM.minusDays(1), BigDecimal.TEN,
                DestinoCosechaFGEM.VENTA, null, null));                                              // antes de la siembra
        assertInvalidoFGEM(() -> servicioFGEM.cerrarCampaniaFGEM(quinuaFGEM.idFGEM(), EstadoCampaniaFGEM.COSECHADA, HOY_FGEM, null)); // sin cosechas
        assertInvalidoFGEM(() -> servicioFGEM.cerrarCampaniaFGEM(quinuaFGEM.idFGEM(), EstadoCampaniaFGEM.PERDIDA, HOY_FGEM, null));   // sin causa

        CampaniaFGEM perdidaFGEM = servicioFGEM.cerrarCampaniaFGEM(quinuaFGEM.idFGEM(), EstadoCampaniaFGEM.PERDIDA, HOY_FGEM, "Helada");
        assertEquals("Helada", perdidaFGEM.motivoFGEM());
    }

    @Test
    void anularRegistrosYCampaniasFGEM() {
        CampaniaFGEM papaFGEM = campaniaFGEM(CultivoFGEM.PAPA, "1");
        servicioFGEM.registrarCostoFGEM(papaFGEM.idFGEM(), SIEMBRA_FGEM, RubroCostoFGEM.SEMILLA, "Semilla", new BigDecimal("1000"));
        long costoIdFGEM = servicioFGEM.listarCostosFGEM(papaFGEM.idFGEM()).get(0).idFGEM();

        servicioFGEM.anularCostoFGEM(costoIdFGEM);
        assertEquals(0, servicioFGEM.resultadoFGEM(papaFGEM.idFGEM()).costoTotalFGEM().signum());
        assertInvalidoFGEM(() -> servicioFGEM.anularCostoFGEM(costoIdFGEM));

        servicioFGEM.anularCampaniaFGEM(papaFGEM.idFGEM(), "Registrada por error");
        assertEquals(0, servicioFGEM.listarCampaniasFGEM(false).size());
        assertEquals(1, servicioFGEM.listarCampaniasFGEM(true).size());
        assertInvalidoFGEM(() -> servicioFGEM.registrarCostoFGEM(papaFGEM.idFGEM(), HOY_FGEM, RubroCostoFGEM.OTROS, "Algo", BigDecimal.ONE));
    }

    @Test
    void corregirCostosYCosechasFGEM() {
        CampaniaFGEM papaFGEM = campaniaFGEM(CultivoFGEM.PAPA, "1");
        servicioFGEM.registrarCostoFGEM(papaFGEM.idFGEM(), SIEMBRA_FGEM, RubroCostoFGEM.SEMILLA, "Semilla", new BigDecimal("1000"));
        long costoIdFGEM = servicioFGEM.listarCostosFGEM(papaFGEM.idFGEM()).get(0).idFGEM();
        servicioFGEM.corregirCostoFGEM(costoIdFGEM, SIEMBRA_FGEM, RubroCostoFGEM.SEMILLA, "Semilla certificada", new BigDecimal("1200"));
        assertEquals(0, new BigDecimal("1200").compareTo(servicioFGEM.resultadoFGEM(papaFGEM.idFGEM()).costoTotalFGEM()));
        assertInvalidoFGEM(() -> servicioFGEM.corregirCostoFGEM(costoIdFGEM, SIEMBRA_FGEM, RubroCostoFGEM.SEMILLA, "Otra", BigDecimal.ONE));

        servicioFGEM.registrarCosechaFGEM(papaFGEM.idFGEM(), HOY_FGEM, new BigDecimal("100"), DestinoCosechaFGEM.VENTA, null, null);
        servicioFGEM.cerrarCampaniaFGEM(papaFGEM.idFGEM(), EstadoCampaniaFGEM.COSECHADA, HOY_FGEM, null);
        long cosechaIdFGEM = servicioFGEM.listarCosechasFGEM(papaFGEM.idFGEM()).get(0).idFGEM();
        // Con la campaña cerrada se puede corregir el dato, aunque no agregar cosechas nuevas.
        servicioFGEM.corregirCosechaFGEM(cosechaIdFGEM, HOY_FGEM, new BigDecimal("150"), DestinoCosechaFGEM.VENTA, new BigDecimal("1.2"), null);
        assertEquals(0, new BigDecimal("150").compareTo(servicioFGEM.resultadoFGEM(papaFGEM.idFGEM()).produccionFGEM()));
        assertInvalidoFGEM(() -> servicioFGEM.registrarCosechaFGEM(papaFGEM.idFGEM(), HOY_FGEM, BigDecimal.TEN, DestinoCosechaFGEM.VENTA, null, null));
        long nuevaIdFGEM = servicioFGEM.listarCosechasFGEM(papaFGEM.idFGEM()).stream().filter(hFGEM -> !hFGEM.anuladoFGEM()).findFirst().orElseThrow().idFGEM();
        assertInvalidoFGEM(() -> servicioFGEM.corregirCosechaFGEM(nuevaIdFGEM, SIEMBRA_FGEM.minusDays(1), BigDecimal.TEN,
                DestinoCosechaFGEM.VENTA, null, null));
    }

    @Test
    void losCultivosEnCursoEntranAlBalanceFGEM() {
        CampaniaFGEM papaFGEM = campaniaFGEM(CultivoFGEM.PAPA, "1");
        servicioFGEM.registrarCostoFGEM(papaFGEM.idFGEM(), SIEMBRA_FGEM, RubroCostoFGEM.SEMILLA, "Semilla", new BigDecimal("2500"));

        BalanceGeneralFGEM bFGEM = entornoFGEM.patrimonioFGEM.situacionFGEM().balanceFGEM();
        assertEquals(0, new BigDecimal("2500").compareTo(bFGEM.cultivosFGEM()));
        assertTrue(bFGEM.activoCorrienteFGEM().stream().anyMatch(pFGEM -> pFGEM.conceptoFGEM().equals(BalanceGeneralFGEM.CULTIVOS_FGEM)));

        servicioFGEM.registrarCosechaFGEM(papaFGEM.idFGEM(), HOY_FGEM, new BigDecimal("100"), DestinoCosechaFGEM.VENTA, null, null);
        servicioFGEM.cerrarCampaniaFGEM(papaFGEM.idFGEM(), EstadoCampaniaFGEM.COSECHADA, HOY_FGEM, null);
        assertEquals(0, entornoFGEM.patrimonioFGEM.situacionFGEM().balanceFGEM().cultivosFGEM().signum());
    }

    @Test
    void respetaLosPermisosFGEM() {
        campaniaFGEM(CultivoFGEM.PAPA, "1");
        UsuarioFGEM analistaFGEM = entornoFGEM.crearUsuarioFGEM("analista", "ANALISTA");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(fundoFGEM.idFGEM(), Set.of(analistaFGEM.idFGEM()));
        entornoFGEM.entrarComoFGEM("analista", "Temporal123");
        entornoFGEM.empresaServiceFGEM.seleccionarEmpresaActivaFGEM(fundoFGEM.idFGEM());

        assertEquals(1, servicioFGEM.listarCampaniasFGEM(false).size());
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.registrarParcelaFGEM(new DatosParcelaFGEM("Nueva",
                BigDecimal.ONE, TenenciaFGEM.PROPIA, false, null)));
    }

    // ------------------------------------------------------------------ utilidades

    private CampaniaFGEM campaniaFGEM(CultivoFGEM cultivoFGEM, String areaFGEM) {
        return servicioFGEM.registrarCampaniaFGEM(new DatosCampaniaFGEM(parcelaFGEM.idFGEM(), cultivoFGEM, null, SIEMBRA_FGEM, new BigDecimal(areaFGEM),
                null, null, null));
    }

    private static void assertInvalidoFGEM(Runnable accionFGEM) {
        assertThrows(ValidacionExceptionFGEM.class, accionFGEM::run);
    }
}
