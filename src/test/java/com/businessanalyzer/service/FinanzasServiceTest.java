package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.ResumenFinanciero;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CategoriaFinanciera;
import com.businessanalyzer.model.Comportamiento;
import com.businessanalyzer.model.DatosMovimiento;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.GrupoFinanciero;
import com.businessanalyzer.model.MovimientoFinanciero;
import com.businessanalyzer.model.TipoMovimiento;
import com.businessanalyzer.model.Usuario;
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

class FinanzasServiceTest {

    /** El reloj de pruebas está en 2026-01-15. */
    private static final LocalDate HOY = LocalDate.of(2026, 1, 15);
    private static final YearMonth DICIEMBRE = YearMonth.of(2025, 12);
    private static final YearMonth ENERO = YearMonth.of(2026, 1);

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private FinanzasService servicio;
    private Empresa empresa;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = entorno.finanzas;
        entorno.administradorConectado();
        empresa = entorno.crearEmpresaActiva("Comercial Andina");
    }

    @Test
    void registraUnGastoConElComportamientoSugeridoPorLaCategoria() {
        MovimientoFinanciero alquiler = gasto(CategoriaFinanciera.ALQUILER, null, HOY, "Alquiler enero", "1500");
        MovimientoFinanciero flete = gasto(CategoriaFinanciera.TRANSPORTE, null, HOY, "Flete Lima", "80");

        assertEquals(Comportamiento.FIJO, alquiler.comportamiento());
        assertEquals(Comportamiento.VARIABLE, flete.comportamiento());
        assertEquals("admin", alquiler.registradoPor());
        assertEquals(empresa.id(), alquiler.empresaId());
    }

    @Test
    void elUsuarioPuedeCambiarElComportamientoSugerido() {
        MovimientoFinanciero comision = gasto(CategoriaFinanciera.SUELDOS, Comportamiento.VARIABLE, HOY,
                "Bono por ventas", "300");
        assertEquals(Comportamiento.VARIABLE, comision.comportamiento());
    }

    @Test
    void losIngresosNoTienenComportamiento() {
        MovimientoFinanciero ingreso = servicio.registrar(new DatosMovimiento(TipoMovimiento.INGRESO,
                CategoriaFinanciera.INTERESES_GANADOS, Comportamiento.FIJO, HOY, "Intereses cuenta", new BigDecimal("12.40"), null));
        assertNull(ingreso.comportamiento());
    }

    @Test
    void validaLosDatos() {
        assertInvalido(new DatosMovimiento(TipoMovimiento.GASTO, CategoriaFinanciera.OTROS_INGRESOS, null, HOY, "Mal", BigDecimal.TEN, null));
        assertInvalido(new DatosMovimiento(TipoMovimiento.GASTO, null, null, HOY, "Sin categoría", BigDecimal.TEN, null));
        assertInvalido(new DatosMovimiento(TipoMovimiento.GASTO, CategoriaFinanciera.ALQUILER, null, HOY, "Cero", BigDecimal.ZERO, null));
        assertInvalido(new DatosMovimiento(TipoMovimiento.GASTO, CategoriaFinanciera.ALQUILER, null, HOY, "Negativo", new BigDecimal("-5"), null));
        assertInvalido(new DatosMovimiento(TipoMovimiento.GASTO, CategoriaFinanciera.ALQUILER, null, HOY, "", BigDecimal.TEN, null));
        assertInvalido(new DatosMovimiento(TipoMovimiento.GASTO, CategoriaFinanciera.ALQUILER, null,
                LocalDate.of(2026, 2, 1), "Mes siguiente", BigDecimal.TEN, null));
        assertInvalido(new DatosMovimiento(null, CategoriaFinanciera.ALQUILER, null, HOY, "Sin tipo", BigDecimal.TEN, null));
    }

    @Test
    void permiteRegistrarGastosDelMesEnCursoPorAdelantado() {
        MovimientoFinanciero sueldos = gasto(CategoriaFinanciera.SUELDOS, null, LocalDate.of(2026, 1, 31),
                "Planilla de enero", "4200");
        assertEquals(LocalDate.of(2026, 1, 31), sueldos.fecha());
    }

    @Test
    void elResumenSeparaFijosVariablesEIngresos() {
        gasto(CategoriaFinanciera.ALQUILER, null, HOY, "Alquiler", "1500");
        gasto(CategoriaFinanciera.SUELDOS, null, HOY, "Sueldos", "4000");
        gasto(CategoriaFinanciera.TRANSPORTE, null, HOY, "Fletes", "250.50");
        gasto(CategoriaFinanciera.INTERESES_PRESTAMOS, null, HOY, "Interés préstamo", "120");
        MovimientoFinanciero anulado = gasto(CategoriaFinanciera.MARKETING, null, HOY, "Volantes", "300");
        servicio.anular(anulado.id(), "Registrado dos veces");
        servicio.registrar(new DatosMovimiento(TipoMovimiento.INGRESO, CategoriaFinanciera.ALQUILERES_COBRADOS,
                null, HOY, "Alquiler de cochera", new BigDecimal("200"), null));
        gasto(CategoriaFinanciera.ALQUILER, null, LocalDate.of(2025, 12, 5), "Alquiler diciembre", "1500");

        ResumenFinanciero r = servicio.resumen(ENERO.atDay(1), ENERO.atEndOfMonth());

        assertEquals(5, r.cantidadMovimientos());
        assertEquals(0, new BigDecimal("5870.50").compareTo(r.gastos()));
        assertEquals(0, new BigDecimal("5620").compareTo(r.gastosFijos()));
        assertEquals(0, new BigDecimal("250.50").compareTo(r.gastosVariables()));
        assertEquals(0, new BigDecimal("200").compareTo(r.ingresos()));
        assertEquals(0, new BigDecimal("5750.50").compareTo(r.porGrupo(GrupoFinanciero.GASTO_OPERATIVO)));
        assertEquals(0, new BigDecimal("120").compareTo(r.porGrupo(GrupoFinanciero.GASTO_FINANCIERO)));
        assertTrue(r.participacionFijos().compareTo(new BigDecimal("0.95")) > 0);
    }

    @Test
    void editarCorrigeElMovimientoYRegistraQuienLoHizo() {
        MovimientoFinanciero m = gasto(CategoriaFinanciera.SERVICIOS_BASICOS, null, HOY, "Luz", "180");

        MovimientoFinanciero corregido = servicio.actualizar(m.id(), new DatosMovimiento(TipoMovimiento.GASTO,
                CategoriaFinanciera.SERVICIOS_BASICOS, null, HOY, "Luz de enero", new BigDecimal("185.40"), "R-123"));

        assertEquals("Luz de enero", corregido.descripcion());
        assertEquals(0, new BigDecimal("185.40").compareTo(corregido.monto()));
        assertEquals("admin", corregido.actualizadoPor());
    }

    @Test
    void unMovimientoAnuladoNoSeEditaNiSeAnulaDosVeces() {
        MovimientoFinanciero m = gasto(CategoriaFinanciera.MARKETING, null, HOY, "Volantes", "300");
        assertThrows(ValidacionException.class, () -> servicio.anular(m.id(), "x"), "Motivo muy corto");
        servicio.anular(m.id(), "Registro duplicado");

        assertThrows(ValidacionException.class, () -> servicio.anular(m.id(), "Otra vez"));
        assertThrows(ValidacionException.class, () -> servicio.actualizar(m.id(), new DatosMovimiento(
                TipoMovimiento.GASTO, CategoriaFinanciera.MARKETING, null, HOY, "Volantes", BigDecimal.TEN, null)));
        assertEquals(1, servicio.listar(ENERO.atDay(1), ENERO.atEndOfMonth(), true).size());
        assertTrue(servicio.listar(ENERO.atDay(1), ENERO.atEndOfMonth(), false).isEmpty());
    }

    @Test
    void copiaLosGastosFijosDelMesAnteriorSinDuplicar() {
        gasto(CategoriaFinanciera.ALQUILER, null, LocalDate.of(2025, 12, 5), "Alquiler local", "1500");
        gasto(CategoriaFinanciera.SUELDOS, null, LocalDate.of(2025, 12, 31), "Planilla", "4000");
        gasto(CategoriaFinanciera.TRANSPORTE, null, LocalDate.of(2025, 12, 10), "Fletes", "300");

        assertEquals(2, servicio.copiarGastosFijos(DICIEMBRE, ENERO), "Solo los fijos");
        assertEquals(0, servicio.copiarGastosFijos(DICIEMBRE, ENERO), "Repetir no duplica");

        List<MovimientoFinanciero> enero = servicio.listar(ENERO.atDay(1), ENERO.atEndOfMonth(), false);
        assertEquals(2, enero.size());
        assertTrue(enero.stream().anyMatch(m -> m.fecha().equals(LocalDate.of(2026, 1, 31))));
        assertThrows(ValidacionException.class, () -> servicio.copiarGastosFijos(ENERO, YearMonth.of(2026, 2)),
                "No se copia hacia meses futuros");
        assertThrows(ValidacionException.class, () -> servicio.copiarGastosFijos(ENERO, DICIEMBRE));
    }

    @Test
    void cadaEmpresaVeSoloSusMovimientos() {
        MovimientoFinanciero m = gasto(CategoriaFinanciera.ALQUILER, null, HOY, "Alquiler", "1500");
        entorno.crearEmpresaActiva("Otra empresa");

        assertTrue(servicio.listar(ENERO.atDay(1), ENERO.atEndOfMonth(), true).isEmpty());
        assertThrows(ValidacionException.class, () -> servicio.anular(m.id(), "No es de esta empresa"));
    }

    @Test
    void respetaLosPermisos() {
        gasto(CategoriaFinanciera.ALQUILER, null, HOY, "Alquiler", "1500");
        Usuario analista = entorno.crearUsuario("analista", "ANALISTA");
        entorno.empresaService.asignarAccesos(empresa.id(), Set.of(analista.id()));
        entorno.entrarComo("analista", "Temporal123");
        entorno.empresaService.restaurarEmpresaActiva();

        assertEquals(1, servicio.listar(ENERO.atDay(1), ENERO.atEndOfMonth(), false).size());
        assertThrows(AccesoDenegadoException.class,
                () -> gasto(CategoriaFinanciera.ALQUILER, null, HOY, "Intento", "10"));
    }

    private MovimientoFinanciero gasto(CategoriaFinanciera categoria, Comportamiento comportamiento,
                                       LocalDate fecha, String descripcion, String monto) {
        return servicio.registrar(new DatosMovimiento(TipoMovimiento.GASTO, categoria, comportamiento, fecha,
                descripcion, new BigDecimal(monto), null));
    }

    private void assertInvalido(DatosMovimiento datos) {
        assertThrows(ValidacionException.class, () -> servicio.registrar(datos));
    }
}
