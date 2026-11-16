package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.ResumenFinancieroFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.ComportamientoFGEM;
import com.businessanalyzer.model.DatosMovimientoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.GrupoFinancieroFGEM;
import com.businessanalyzer.model.MovimientoFinancieroFGEM;
import com.businessanalyzer.model.TipoMovimientoFGEM;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanzasServiceTestFGEM {

    /** El reloj de pruebas está en 2026-01-15. */
    private static final LocalDate HOY_FGEM = LocalDate.of(2026, 1, 15);
    private static final YearMonth DICIEMBRE_FGEM = YearMonth.of(2025, 12);
    private static final YearMonth ENERO_FGEM = YearMonth.of(2026, 1);

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private FinanzasServiceFGEM servicioFGEM;
    private EmpresaFGEM empresaFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = entornoFGEM.finanzasFGEM;
        entornoFGEM.administradorConectadoFGEM();
        empresaFGEM = entornoFGEM.crearEmpresaActivaFGEM("Comercial Andina");
    }

    @Test
    void registraUnGastoConElComportamientoSugeridoPorLaCategoriaFGEM() {
        MovimientoFinancieroFGEM alquilerFGEM = gastoFGEM(CategoriaFinancieraFGEM.ALQUILER, null, HOY_FGEM, "Alquiler enero", "1500");
        MovimientoFinancieroFGEM fleteFGEM = gastoFGEM(CategoriaFinancieraFGEM.TRANSPORTE, null, HOY_FGEM, "Flete Lima", "80");

        assertEquals(ComportamientoFGEM.FIJO, alquilerFGEM.comportamientoFGEM());
        assertEquals(ComportamientoFGEM.VARIABLE, fleteFGEM.comportamientoFGEM());
        assertEquals("admin", alquilerFGEM.registradoPorFGEM());
        assertEquals(empresaFGEM.idFGEM(), alquilerFGEM.empresaIdFGEM());
    }

    @Test
    void elUsuarioPuedeCambiarElComportamientoSugeridoFGEM() {
        MovimientoFinancieroFGEM comisionFGEM = gastoFGEM(CategoriaFinancieraFGEM.SUELDOS, ComportamientoFGEM.VARIABLE, HOY_FGEM,
                "Bono por ventas", "300");
        assertEquals(ComportamientoFGEM.VARIABLE, comisionFGEM.comportamientoFGEM());
    }

    @Test
    void losIngresosNoTienenComportamientoFGEM() {
        MovimientoFinancieroFGEM ingresoFGEM = servicioFGEM.registrarFGEM(new DatosMovimientoFGEM(TipoMovimientoFGEM.INGRESO,
                CategoriaFinancieraFGEM.INTERESES_GANADOS, ComportamientoFGEM.FIJO, HOY_FGEM, "Intereses cuenta", new BigDecimal("12.40"), null));
        assertNull(ingresoFGEM.comportamientoFGEM());
    }

    @Test
    void validaLosDatosFGEM() {
        assertInvalidoFGEM(new DatosMovimientoFGEM(TipoMovimientoFGEM.GASTO, CategoriaFinancieraFGEM.OTROS_INGRESOS, null, HOY_FGEM, "Mal", BigDecimal.TEN, null));
        assertInvalidoFGEM(new DatosMovimientoFGEM(TipoMovimientoFGEM.GASTO, null, null, HOY_FGEM, "Sin categoría", BigDecimal.TEN, null));
        assertInvalidoFGEM(new DatosMovimientoFGEM(TipoMovimientoFGEM.GASTO, CategoriaFinancieraFGEM.ALQUILER, null, HOY_FGEM, "Cero", BigDecimal.ZERO, null));
        assertInvalidoFGEM(new DatosMovimientoFGEM(TipoMovimientoFGEM.GASTO, CategoriaFinancieraFGEM.ALQUILER, null, HOY_FGEM, "Negativo", new BigDecimal("-5"), null));
        assertInvalidoFGEM(new DatosMovimientoFGEM(TipoMovimientoFGEM.GASTO, CategoriaFinancieraFGEM.ALQUILER, null, HOY_FGEM, "", BigDecimal.TEN, null));
        assertInvalidoFGEM(new DatosMovimientoFGEM(TipoMovimientoFGEM.GASTO, CategoriaFinancieraFGEM.ALQUILER, null,
                LocalDate.of(2026, 2, 1), "Mes siguiente", BigDecimal.TEN, null));
        assertInvalidoFGEM(new DatosMovimientoFGEM(null, CategoriaFinancieraFGEM.ALQUILER, null, HOY_FGEM, "Sin tipo", BigDecimal.TEN, null));
    }

    @Test
    void permiteRegistrarGastosDelMesEnCursoPorAdelantadoFGEM() {
        MovimientoFinancieroFGEM sueldosFGEM = gastoFGEM(CategoriaFinancieraFGEM.SUELDOS, null, LocalDate.of(2026, 1, 31),
                "Planilla de enero", "4200");
        assertEquals(LocalDate.of(2026, 1, 31), sueldosFGEM.fechaFGEM());
    }

    @Test
    void elResumenSeparaFijosVariablesEIngresosFGEM() {
        gastoFGEM(CategoriaFinancieraFGEM.ALQUILER, null, HOY_FGEM, "Alquiler", "1500");
        gastoFGEM(CategoriaFinancieraFGEM.SUELDOS, null, HOY_FGEM, "Sueldos", "4000");
        gastoFGEM(CategoriaFinancieraFGEM.TRANSPORTE, null, HOY_FGEM, "Fletes", "250.50");
        gastoFGEM(CategoriaFinancieraFGEM.INTERESES_PRESTAMOS, null, HOY_FGEM, "Interés préstamo", "120");
        MovimientoFinancieroFGEM anuladoFGEM = gastoFGEM(CategoriaFinancieraFGEM.MARKETING, null, HOY_FGEM, "Volantes", "300");
        servicioFGEM.anularFGEM(anuladoFGEM.idFGEM(), "Registrado dos veces");
        servicioFGEM.registrarFGEM(new DatosMovimientoFGEM(TipoMovimientoFGEM.INGRESO, CategoriaFinancieraFGEM.ALQUILERES_COBRADOS,
                null, HOY_FGEM, "Alquiler de cochera", new BigDecimal("200"), null));
        gastoFGEM(CategoriaFinancieraFGEM.ALQUILER, null, LocalDate.of(2025, 12, 5), "Alquiler diciembre", "1500");

        ResumenFinancieroFGEM rFGEM = servicioFGEM.resumenFGEM(ENERO_FGEM.atDay(1), ENERO_FGEM.atEndOfMonth());

        assertEquals(5, rFGEM.cantidadMovimientosFGEM());
        assertEquals(0, new BigDecimal("5870.50").compareTo(rFGEM.gastosFGEM()));
        assertEquals(0, new BigDecimal("5620").compareTo(rFGEM.gastosFijosFGEM()));
        assertEquals(0, new BigDecimal("250.50").compareTo(rFGEM.gastosVariablesFGEM()));
        assertEquals(0, new BigDecimal("200").compareTo(rFGEM.ingresosFGEM()));
        assertEquals(0, new BigDecimal("5750.50").compareTo(rFGEM.porGrupoFGEM(GrupoFinancieroFGEM.GASTO_OPERATIVO)));
        assertEquals(0, new BigDecimal("120").compareTo(rFGEM.porGrupoFGEM(GrupoFinancieroFGEM.GASTO_FINANCIERO)));
        assertTrue(rFGEM.participacionFijosFGEM().compareTo(new BigDecimal("0.95")) > 0);
    }

    @Test
    void editarCorrigeElMovimientoYRegistraQuienLoHizoFGEM() {
        MovimientoFinancieroFGEM mFGEM = gastoFGEM(CategoriaFinancieraFGEM.SERVICIOS_BASICOS, null, HOY_FGEM, "Luz", "180");

        MovimientoFinancieroFGEM corregidoFGEM = servicioFGEM.actualizarFGEM(mFGEM.idFGEM(), new DatosMovimientoFGEM(TipoMovimientoFGEM.GASTO,
                CategoriaFinancieraFGEM.SERVICIOS_BASICOS, null, HOY_FGEM, "Luz de enero", new BigDecimal("185.40"), "R-123"));

        assertEquals("Luz de enero", corregidoFGEM.descripcionFGEM());
        assertEquals(0, new BigDecimal("185.40").compareTo(corregidoFGEM.montoFGEM()));
        assertEquals("admin", corregidoFGEM.actualizadoPorFGEM());
    }

    @Test
    void unMovimientoAnuladoNoSeEditaNiSeAnulaDosVecesFGEM() {
        MovimientoFinancieroFGEM mFGEM = gastoFGEM(CategoriaFinancieraFGEM.MARKETING, null, HOY_FGEM, "Volantes", "300");
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.anularFGEM(mFGEM.idFGEM(), "x"), "Motivo muy corto");
        servicioFGEM.anularFGEM(mFGEM.idFGEM(), "Registro duplicado");

        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.anularFGEM(mFGEM.idFGEM(), "Otra vez"));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.actualizarFGEM(mFGEM.idFGEM(), new DatosMovimientoFGEM(
                TipoMovimientoFGEM.GASTO, CategoriaFinancieraFGEM.MARKETING, null, HOY_FGEM, "Volantes", BigDecimal.TEN, null)));
        assertEquals(1, servicioFGEM.listarFGEM(ENERO_FGEM.atDay(1), ENERO_FGEM.atEndOfMonth(), true).size());
        assertTrue(servicioFGEM.listarFGEM(ENERO_FGEM.atDay(1), ENERO_FGEM.atEndOfMonth(), false).isEmpty());
    }

    @Test
    void copiaLosGastosFijosDelMesAnteriorSinDuplicarFGEM() {
        gastoFGEM(CategoriaFinancieraFGEM.ALQUILER, null, LocalDate.of(2025, 12, 5), "Alquiler local", "1500");
        gastoFGEM(CategoriaFinancieraFGEM.SUELDOS, null, LocalDate.of(2025, 12, 31), "Planilla", "4000");
        gastoFGEM(CategoriaFinancieraFGEM.TRANSPORTE, null, LocalDate.of(2025, 12, 10), "Fletes", "300");

        assertEquals(2, servicioFGEM.copiarGastosFijosFGEM(DICIEMBRE_FGEM, ENERO_FGEM), "Solo los fijos");
        assertEquals(0, servicioFGEM.copiarGastosFijosFGEM(DICIEMBRE_FGEM, ENERO_FGEM), "Repetir no duplica");

        List<MovimientoFinancieroFGEM> eneroFGEM = servicioFGEM.listarFGEM(ENERO_FGEM.atDay(1), ENERO_FGEM.atEndOfMonth(), false);
        assertEquals(2, eneroFGEM.size());
        assertTrue(eneroFGEM.stream().anyMatch(mFGEM -> mFGEM.fechaFGEM().equals(LocalDate.of(2026, 1, 31))));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.copiarGastosFijosFGEM(ENERO_FGEM, YearMonth.of(2026, 2)),
                "No se copia hacia meses futuros");
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.copiarGastosFijosFGEM(ENERO_FGEM, DICIEMBRE_FGEM));
    }

    @Test
    void cadaEmpresaVeSoloSusMovimientosFGEM() {
        MovimientoFinancieroFGEM mFGEM = gastoFGEM(CategoriaFinancieraFGEM.ALQUILER, null, HOY_FGEM, "Alquiler", "1500");
        entornoFGEM.crearEmpresaActivaFGEM("Otra empresa");

        assertTrue(servicioFGEM.listarFGEM(ENERO_FGEM.atDay(1), ENERO_FGEM.atEndOfMonth(), true).isEmpty());
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.anularFGEM(mFGEM.idFGEM(), "No es de esta empresa"));
    }

    @Test
    void respetaLosPermisosFGEM() {
        gastoFGEM(CategoriaFinancieraFGEM.ALQUILER, null, HOY_FGEM, "Alquiler", "1500");
        UsuarioFGEM analistaFGEM = entornoFGEM.crearUsuarioFGEM("analista", "ANALISTA");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of(analistaFGEM.idFGEM()));
        entornoFGEM.entrarComoFGEM("analista", "Temporal123");
        entornoFGEM.empresaServiceFGEM.restaurarEmpresaActivaFGEM();

        assertEquals(1, servicioFGEM.listarFGEM(ENERO_FGEM.atDay(1), ENERO_FGEM.atEndOfMonth(), false).size());
        assertThrows(AccesoDenegadoExceptionFGEM.class,
                () -> gastoFGEM(CategoriaFinancieraFGEM.ALQUILER, null, HOY_FGEM, "Intento", "10"));
    }

    private MovimientoFinancieroFGEM gastoFGEM(CategoriaFinancieraFGEM categoriaFGEM, ComportamientoFGEM comportamientoFGEM,
                                       LocalDate fechaFGEM, String descripcionFGEM, String montoFGEM) {
        return servicioFGEM.registrarFGEM(new DatosMovimientoFGEM(TipoMovimientoFGEM.GASTO, categoriaFGEM, comportamientoFGEM, fechaFGEM,
                descripcionFGEM, new BigDecimal(montoFGEM), null));
    }

    private void assertInvalidoFGEM(DatosMovimientoFGEM datosFGEM) {
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.registrarFGEM(datosFGEM));
    }
}
