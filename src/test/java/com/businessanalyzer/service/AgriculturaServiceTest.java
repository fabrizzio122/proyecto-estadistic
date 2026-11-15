package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.agricultura.ResultadoCampania;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneral;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Campania;
import com.businessanalyzer.model.Cultivo;
import com.businessanalyzer.model.DatosCampania;
import com.businessanalyzer.model.DatosEmpresa;
import com.businessanalyzer.model.DatosParcela;
import com.businessanalyzer.model.DestinoCosecha;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.EstadoCampania;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Parcela;
import com.businessanalyzer.model.RubroCosto;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.TamanoEmpresa;
import com.businessanalyzer.model.Tenencia;
import com.businessanalyzer.model.Usuario;
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

class AgriculturaServiceTest {

    /** El reloj de pruebas está en 2026-01-15. */
    private static final LocalDate HOY = LocalDate.of(2026, 1, 15);
    private static final LocalDate SIEMBRA = LocalDate.of(2025, 10, 10);

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private AgriculturaService servicio;
    private Empresa fundo;
    private Parcela parcela;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = entorno.agricultura;
        entorno.administradorConectado();
        fundo = entorno.empresaService.crear(new DatosEmpresa("Fundo Los Andes", null, null, Sector.AGROPECUARIO,
                TamanoEmpresa.MICRO, Moneda.PEN, 1, null, null, null, null));
        entorno.empresaService.seleccionarEmpresaActiva(fundo.id());
        parcela = servicio.registrarParcela(new DatosParcela("Parcela Alta", new BigDecimal("2"), Tenencia.PROPIA,
                false, null));
    }

    @Test
    void soloFuncionaEnEmpresasAgropecuarias() {
        entorno.crearEmpresaActiva("Bodega");
        assertInvalido(() -> servicio.listarParcelas(false));
    }

    @Test
    void validaLasParcelas() {
        assertInvalido(() -> servicio.registrarParcela(new DatosParcela("parcela alta", BigDecimal.ONE, Tenencia.PROPIA,
                false, null)));                                                                   // nombre repetido
        assertInvalido(() -> servicio.registrarParcela(new DatosParcela("Otra", BigDecimal.ZERO, Tenencia.PROPIA, false, null)));
        assertInvalido(() -> servicio.registrarParcela(new DatosParcela("Otra", BigDecimal.ONE, null, false, null)));

        Parcela inactiva = servicio.cambiarParcelaActiva(parcela.id(), false);
        assertFalse(inactiva.activa());
        assertEquals(0, servicio.listarParcelas(false).size());
        assertInvalido(() -> campania(Cultivo.PAPA, "1"));                                   // parcela desactivada
    }

    @Test
    void registraUnaCampaniaCompletaYCalculaSuResultado() {
        Campania papa = campania(Cultivo.PAPA, "1.5");
        assertEquals(0, new BigDecimal("15000").compareTo(papa.rendimientoReferencia())); // propuesto por el cultivo

        servicio.registrarCosto(papa.id(), SIEMBRA.minusDays(20), RubroCosto.PREPARACION, "Yunta", new BigDecimal("600"));
        servicio.registrarCosto(papa.id(), SIEMBRA, RubroCosto.SEMILLA, "Semilla", new BigDecimal("3000"));
        servicio.registrarCosecha(papa.id(), HOY, new BigDecimal("15000"), DestinoCosecha.VENTA, new BigDecimal("0.80"), null);
        ResultadoCampania r = servicio.registrarCosecha(papa.id(), HOY, new BigDecimal("3000"),
                DestinoCosecha.AUTOCONSUMO, new BigDecimal("9"), null);

        assertEquals(0, new BigDecimal("3600").compareTo(r.costoTotal()));
        assertEquals(0, new BigDecimal("12000").compareTo(r.rendimiento()));    // 18 000 kg / 1.5 ha
        assertEquals(0, new BigDecimal("12000").compareTo(r.ingresoVentas()));

        Campania cerrada = servicio.cerrarCampania(papa.id(), EstadoCampania.COSECHADA, HOY, null);
        assertEquals(EstadoCampania.COSECHADA, cerrada.estado());
        assertInvalido(() -> servicio.registrarCosecha(papa.id(), HOY, BigDecimal.ONE, DestinoCosecha.VENTA, null, null));
        assertEquals(EstadoCampania.EN_CURSO, servicio.reabrirCampania(papa.id()).estado());
    }

    @Test
    void validaCampaniasCostosYCosechas() {
        assertInvalido(() -> campania(Cultivo.PAPA, "3"));                                   // más que la parcela
        Campania quinua = campania(Cultivo.QUINUA, "1");
        assertInvalido(() -> servicio.registrarCosto(quinua.id(), SIEMBRA.minusMonths(7), RubroCosto.SEMILLA, "Semilla",
                BigDecimal.TEN));                                                                // muy anterior a la siembra
        assertInvalido(() -> servicio.registrarCosto(quinua.id(), HOY, RubroCosto.SEMILLA, "Semilla", BigDecimal.ZERO));
        assertInvalido(() -> servicio.registrarCosecha(quinua.id(), SIEMBRA.minusDays(1), BigDecimal.TEN,
                DestinoCosecha.VENTA, null, null));                                              // antes de la siembra
        assertInvalido(() -> servicio.cerrarCampania(quinua.id(), EstadoCampania.COSECHADA, HOY, null)); // sin cosechas
        assertInvalido(() -> servicio.cerrarCampania(quinua.id(), EstadoCampania.PERDIDA, HOY, null));   // sin causa

        Campania perdida = servicio.cerrarCampania(quinua.id(), EstadoCampania.PERDIDA, HOY, "Helada");
        assertEquals("Helada", perdida.motivo());
    }

    @Test
    void anularRegistrosYCampanias() {
        Campania papa = campania(Cultivo.PAPA, "1");
        servicio.registrarCosto(papa.id(), SIEMBRA, RubroCosto.SEMILLA, "Semilla", new BigDecimal("1000"));
        long costoId = servicio.listarCostos(papa.id()).get(0).id();

        servicio.anularCosto(costoId);
        assertEquals(0, servicio.resultado(papa.id()).costoTotal().signum());
        assertInvalido(() -> servicio.anularCosto(costoId));

        servicio.anularCampania(papa.id(), "Registrada por error");
        assertEquals(0, servicio.listarCampanias(false).size());
        assertEquals(1, servicio.listarCampanias(true).size());
        assertInvalido(() -> servicio.registrarCosto(papa.id(), HOY, RubroCosto.OTROS, "Algo", BigDecimal.ONE));
    }

    @Test
    void losCultivosEnCursoEntranAlBalance() {
        Campania papa = campania(Cultivo.PAPA, "1");
        servicio.registrarCosto(papa.id(), SIEMBRA, RubroCosto.SEMILLA, "Semilla", new BigDecimal("2500"));

        BalanceGeneral b = entorno.patrimonio.situacion().balance();
        assertEquals(0, new BigDecimal("2500").compareTo(b.cultivos()));
        assertTrue(b.activoCorriente().stream().anyMatch(p -> p.concepto().equals(BalanceGeneral.CULTIVOS)));

        servicio.registrarCosecha(papa.id(), HOY, new BigDecimal("100"), DestinoCosecha.VENTA, null, null);
        servicio.cerrarCampania(papa.id(), EstadoCampania.COSECHADA, HOY, null);
        assertEquals(0, entorno.patrimonio.situacion().balance().cultivos().signum());
    }

    @Test
    void respetaLosPermisos() {
        campania(Cultivo.PAPA, "1");
        Usuario analista = entorno.crearUsuario("analista", "ANALISTA");
        entorno.empresaService.asignarAccesos(fundo.id(), Set.of(analista.id()));
        entorno.entrarComo("analista", "Temporal123");
        entorno.empresaService.seleccionarEmpresaActiva(fundo.id());

        assertEquals(1, servicio.listarCampanias(false).size());
        assertThrows(AccesoDenegadoException.class, () -> servicio.registrarParcela(new DatosParcela("Nueva",
                BigDecimal.ONE, Tenencia.PROPIA, false, null)));
    }

    // ------------------------------------------------------------------ utilidades

    private Campania campania(Cultivo cultivo, String area) {
        return servicio.registrarCampania(new DatosCampania(parcela.id(), cultivo, null, SIEMBRA, new BigDecimal(area),
                null, null, null));
    }

    private static void assertInvalido(Runnable accion) {
        assertThrows(ValidacionException.class, accion::run);
    }
}
