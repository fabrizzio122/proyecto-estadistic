package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.ganaderia.ExistenciaValorizada;
import com.businessanalyzer.analytics.ganaderia.IndicadoresEspecie;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneral;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CategoriaGanado;
import com.businessanalyzer.model.DatosEmpresa;
import com.businessanalyzer.model.DatosMovimientoGanado;
import com.businessanalyzer.model.DatosProduccion;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Especie;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.MovimientoGanado;
import com.businessanalyzer.model.ProduccionGanadera;
import com.businessanalyzer.model.ProductoGanadero;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.TamanoEmpresa;
import com.businessanalyzer.model.TipoMovimientoGanado;
import com.businessanalyzer.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.businessanalyzer.model.CategoriaGanado.*;
import static com.businessanalyzer.model.TipoMovimientoGanado.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GanaderiaServiceTest {

    /** El reloj de pruebas está en 2026-01-15. */
    private static final LocalDate HOY = LocalDate.of(2026, 1, 15);

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private GanaderiaService servicio;
    private Empresa fundo;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = entorno.ganaderia;
        entorno.administradorConectado();
        fundo = entorno.empresaService.crear(new DatosEmpresa("Fundo Los Andes", null, null, Sector.AGROPECUARIO,
                TamanoEmpresa.MICRO, Moneda.PEN, 1, null, null, null, null));
        entorno.empresaService.seleccionarEmpresaActiva(fundo.id());
    }

    @Test
    void soloFuncionaEnEmpresasAgropecuarias() {
        entorno.crearEmpresaActiva("Bodega");
        assertInvalido(servicio::hato);
        assertInvalido(() -> mov("2026-01-01", INVENTARIO_INICIAL, VACA, 5, null));
    }

    @Test
    void registraElHatoYCalculaLasExistencias() {
        mov("2025-06-01", INVENTARIO_INICIAL, VACA, 10, null);
        mov("2025-06-01", INVENTARIO_INICIAL, TORO, 1, null);
        mov("2025-09-10", NACIMIENTO, TERNERA, 3, null);
        mov("2025-12-01", CAMBIO_CATEGORIA, TERNERA, 1, VAQUILLONA);
        MovimientoGanado venta = servicio.registrarMovimiento(new DatosMovimientoGanado(HOY, VENTA, VACA, 2, null,
                new BigDecimal("5000"), "ignorada", "Feria de Huancayo"));

        assertNull(venta.causa());                    // la causa solo aplica a muertes y pérdidas
        assertEquals("admin", venta.registradoPor());
        Map<CategoriaGanado, Integer> hato = existencias();
        assertEquals(Map.of(VACA, 8, TORO, 1, TERNERA, 2, VAQUILLONA, 1), hato);
        assertEquals(5, servicio.listarMovimientos(LocalDate.of(2025, 1, 1), HOY, false).size());
        assertEquals(VENTA, servicio.listarMovimientos(LocalDate.of(2025, 1, 1), HOY, false).get(0).tipo());
    }

    @Test
    void noPermiteQuedarConExistenciasNegativas() {
        mov("2025-06-01", INVENTARIO_INICIAL, VACA, 5, null);
        mov("2025-12-01", VENTA, VACA, 5, null);

        // Vender antes de la venta de diciembre dejaría sin vacas en esa fecha.
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> mov("2025-08-01", MUERTE, VACA, 1, null));
        assertTrue(e.getMessage().contains("faltarían 1"), e.getMessage());
        assertInvalido(() -> mov("2025-07-01", VENTA, TORO, 1, null));
        // Y no se registró nada de lo rechazado.
        assertEquals(2, servicio.listarMovimientos(LocalDate.of(2025, 1, 1), HOY, false).size());
    }

    @Test
    void anularUnaEntradaNoPuedeDejarSinAnimales() {
        MovimientoGanado inicial = mov("2025-06-01", INVENTARIO_INICIAL, OVEJA, 10, null);
        MovimientoGanado muerte = mov("2025-07-01", MUERTE, OVEJA, 4, null);

        assertInvalido(() -> servicio.anularMovimiento(inicial.id(), "Error de registro"));
        MovimientoGanado anulada = servicio.anularMovimiento(muerte.id(), "Error de registro");

        assertTrue(anulada.anulado());
        assertEquals(Map.of(OVEJA, 10), existencias());
        assertInvalido(() -> servicio.anularMovimiento(muerte.id(), "Otra vez"));
    }

    @Test
    void validaLosMovimientos() {
        mov("2025-06-01", INVENTARIO_INICIAL, ALPACA_MADRE, 30, null);
        assertInvalido(() -> mov("2025-07-01", NACIMIENTO, ALPACA_MADRE, 1, null));          // no es cría
        assertInvalido(() -> mov("2025-07-01", CAMBIO_CATEGORIA, ALPACA_MADRE, 1, null));    // sin destino
        assertInvalido(() -> mov("2025-07-01", CAMBIO_CATEGORIA, ALPACA_MADRE, 1, LLAMA_MADRE)); // otra especie
        assertInvalido(() -> mov("2026-02-01", NACIMIENTO, ALPACA_CRIA, 1, null));           // fecha futura
        assertInvalido(() -> mov("2025-07-01", NACIMIENTO, ALPACA_CRIA, 0, null));           // cantidad
    }

    @Test
    void registraLaProduccionYValidaElProducto() {
        mov("2025-06-01", INVENTARIO_INICIAL, VACA, 10, null);
        ProduccionGanadera leche = servicio.registrarProduccion(new DatosProduccion(Especie.VACUNO,
                ProductoGanadero.LECHE, LocalDate.of(2025, 12, 31), 31, new BigDecimal("1550"), 10,
                new BigDecimal("1.25"), null));
        assertEquals(31, leche.dias());
        assertEquals(0, new BigDecimal("1937.50").compareTo(leche.valor()));

        assertInvalido(() -> servicio.registrarProduccion(new DatosProduccion(Especie.VACUNO, ProductoGanadero.FIBRA,
                HOY, 1, BigDecimal.TEN, null, null, null)));
        assertInvalido(() -> servicio.registrarProduccion(new DatosProduccion(Especie.ALPACA, ProductoGanadero.FIBRA,
                HOY, 1, BigDecimal.ZERO, null, null, null)));

        servicio.anularProduccion(leche.id());
        assertEquals(0, servicio.listarProduccion(LocalDate.of(2025, 1, 1), HOY, false).size());
        assertEquals(1, servicio.listarProduccion(LocalDate.of(2025, 1, 1), HOY, true).size());
    }

    @Test
    void elValorDelHatoEntraAlBalanceGeneral() {
        mov("2025-06-01", INVENTARIO_INICIAL, VACA, 10, null);
        mov("2025-06-01", INVENTARIO_INICIAL, TERNERO, 4, null);
        Map<CategoriaGanado, BigDecimal> precios = new EnumMap<>(CategoriaGanado.class);
        precios.put(VACA, new BigDecimal("2500"));
        precios.put(TERNERO, new BigDecimal("900"));
        servicio.guardarPrecios(precios);

        List<ExistenciaValorizada> hato = servicio.hato();
        assertEquals(0, new BigDecimal("28600").compareTo(ExistenciaValorizada.total(hato)));

        BalanceGeneral b = entorno.patrimonio.situacion().balance();
        assertEquals(0, new BigDecimal("28600").compareTo(b.ganado()));
        assertTrue(b.activoNoCorriente().stream().anyMatch(p -> p.concepto().equals(BalanceGeneral.GANADO)));
        assertTrue(b.tieneActivos());

        // Borrar un precio deja la categoría sin valorizar.
        precios.put(TERNERO, null);
        servicio.guardarPrecios(precios);
        assertEquals(0, new BigDecimal("25000").compareTo(ExistenciaValorizada.total(servicio.hato())));
    }

    @Test
    void calculaLosIndicadoresDelPeriodo() {
        mov("2024-12-01", INVENTARIO_INICIAL, ALPACA_MADRE, 50, null);
        mov("2025-02-01", NACIMIENTO, ALPACA_CRIA, 30, null);
        mov("2025-03-01", MUERTE, ALPACA_CRIA, 3, null);

        List<IndicadoresEspecie> lista = servicio.indicadores(new Periodo(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));

        assertEquals(1, lista.size());
        IndicadoresEspecie alpacas = lista.get(0);
        assertEquals(Especie.ALPACA, alpacas.especie());
        assertEquals(0, new BigDecimal("0.6").compareTo(alpacas.natalidad()));
        assertEquals(0, new BigDecimal("0.1").compareTo(alpacas.mortalidadCrias()));
    }

    @Test
    void respetaLosPermisos() {
        mov("2025-06-01", INVENTARIO_INICIAL, VACA, 10, null);
        Usuario analista = entorno.crearUsuario("analista", "ANALISTA");
        Usuario productor = entorno.crearUsuario("productor", "OPERADOR");
        entorno.empresaService.asignarAccesos(fundo.id(), Set.of(analista.id(), productor.id()));

        entorno.entrarComo("analista", "Temporal123");
        entorno.empresaService.restaurarEmpresaActiva();
        entorno.empresaService.seleccionarEmpresaActiva(fundo.id());
        assertEquals(1, servicio.hato().size());
        assertThrows(AccesoDenegadoException.class, () -> mov("2025-07-01", NACIMIENTO, TERNERA, 1, null));

        // El operador (el productor) sí registra.
        entorno.entrarComo("productor", "Temporal123");
        entorno.empresaService.seleccionarEmpresaActiva(fundo.id());
        mov("2025-07-01", NACIMIENTO, TERNERA, 1, null);
        assertEquals(2, servicio.hato().size());
    }

    // ------------------------------------------------------------------ utilidades

    private MovimientoGanado mov(String fecha, TipoMovimientoGanado tipo, CategoriaGanado categoria, int cantidad,
                                 CategoriaGanado destino) {
        return servicio.registrarMovimiento(new DatosMovimientoGanado(LocalDate.parse(fecha), tipo, categoria, cantidad,
                destino, null, null, null));
    }

    private Map<CategoriaGanado, Integer> existencias() {
        Map<CategoriaGanado, Integer> mapa = new EnumMap<>(CategoriaGanado.class);
        servicio.hato().forEach(e -> mapa.put(e.categoria(), e.cabezas()));
        return mapa;
    }

    private static void assertInvalido(Runnable accion) {
        assertThrows(ValidacionException.class, accion::run);
    }
}
