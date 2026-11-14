package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.prediccion.LecturaPrediccion;
import com.businessanalyzer.analytics.prediccion.Pronostico;
import com.businessanalyzer.analytics.prediccion.ResultadoPrediccion;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosOperacion;
import com.businessanalyzer.model.DatosOperacion.DatosLinea;
import com.businessanalyzer.model.DatosProducto;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.TipoItem;
import com.businessanalyzer.model.TipoOperacion;
import com.businessanalyzer.model.UnidadMedida;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reloj de pruebas en 2026-01-15. Ventas de julio a diciembre de 2025: 1000, 1200 ... 2000
 * (crecen 200 por mes) con un costo del 60 % -> utilidad 400, 480 ... 800.
 * En enero (mes en curso) hay una venta que no debe usarse como historia.
 */
class PrediccionServiceTest {

    private static final double TOLERANCIA = 1e-6;

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private PrediccionService servicio;
    private Empresa empresa;
    private Producto producto;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = new PrediccionService(entorno.database, new OperacionRepository(), new MovimientoRepository(),
                entorno.empresaService, entorno.sesion, entorno.reloj);
        entorno.administradorConectado();
        empresa = entorno.crearEmpresaActiva("Comercial Andina");
        producto = entorno.catalogo.crearProducto(new DatosProducto(TipoItem.SERVICIO, null, "Servicio", null, null,
                UnidadMedida.SERVICIO, new BigDecimal("60"), new BigDecimal("100")));
        for (int k = 0; k < 6; k++) {
            vender(YearMonth.of(2025, 7).plusMonths(k).atDay(15), 10 + 2 * k);
        }
        vender(LocalDate.of(2026, 1, 10), 3);
    }

    @Test
    void proyectaLasVentasDesdeElMesEnCurso() {
        ResultadoPrediccion r = servicio.predecir(3);

        assertEquals(6, r.meses().size(), "Julio a diciembre; enero está incompleto");
        assertEquals(YearMonth.of(2026, 1), r.primerMesProyectado(null));
        Pronostico ventas = r.pronosticoVentas();
        assertEquals("Tendencia lineal", ventas.elegida().metodo().nombre());
        assertEquals(Pronostico.Confiabilidad.ALTA, ventas.confiabilidad());
        assertEquals(2200, ventas.puntos().get(0).valor(), TOLERANCIA);
        assertEquals(2600, ventas.puntos().get(2).valor(), TOLERANCIA);
        assertEquals(880, r.pronosticoUtilidad().puntos().get(0).valor(), TOLERANCIA);
    }

    @Test
    void comparaConLosUltimosMeses() {
        ResultadoPrediccion r = servicio.predecir(3);
        assertEquals(1600 + 1800 + 2000, r.ventasUltimosMeses(), TOLERANCIA);

        List<String> frases = LecturaPrediccion.generar(r, Moneda.PEN);
        assertTrue(frases.get(0).contains("sumarían alrededor de S/ 7,200.00"), frases.toString());
        assertTrue(frases.get(1).startsWith("Eso es 33.3 % más"), frases.toString());
    }

    @Test
    void conPocaHistoriaNoProyecta() {
        entorno.crearEmpresaActiva("Negocio nuevo");
        ResultadoPrediccion r = servicio.predecir(6);

        assertFalse(r.pronosticoVentas().suficiente());
        assertTrue(LecturaPrediccion.generar(r, Moneda.PEN).get(0).startsWith("Se necesitan al menos 4 meses"));
    }

    @Test
    void validaElHorizonteYLosPermisos() {
        assertThrows(ValidacionException.class, () -> servicio.predecir(0));
        assertThrows(ValidacionException.class, () -> servicio.predecir(13));

        Usuario operador = entorno.crearUsuario("operador", "OPERADOR");
        entorno.empresaService.asignarAccesos(empresa.id(), Set.of(operador.id()));
        entorno.entrarComo("operador", "Temporal123");
        entorno.empresaService.restaurarEmpresaActiva();
        assertThrows(AccesoDenegadoException.class, () -> servicio.predecir(3));
    }

    private void vender(LocalDate fecha, int cantidad) {
        entorno.operaciones.registrar(new DatosOperacion(TipoOperacion.VENTA, fecha, null, null, null, BigDecimal.ZERO,
                null, List.of(new DatosLinea(producto.id(), BigDecimal.valueOf(cantidad), new BigDecimal("100"))), false), false);
    }
}
