package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.RentabilidadProducto;
import com.businessanalyzer.analytics.ResumenOperaciones;
import com.businessanalyzer.analytics.agricultura.ComparacionCultivos;
import com.businessanalyzer.analytics.diagnostico.Area;
import com.businessanalyzer.analytics.diagnostico.Diagnostico;
import com.businessanalyzer.analytics.diagnostico.Hallazgo;
import com.businessanalyzer.analytics.diagnostico.Semaforo;
import com.businessanalyzer.analytics.diagnostico.TipoIndicador;
import com.businessanalyzer.analytics.ganaderia.EvolucionHato;
import com.businessanalyzer.model.Campania;
import com.businessanalyzer.model.CategoriaGanado;
import com.businessanalyzer.model.Cultivo;
import com.businessanalyzer.model.DatosCampania;
import com.businessanalyzer.model.DatosEmpresa;
import com.businessanalyzer.model.DatosMovimientoGanado;
import com.businessanalyzer.model.DatosParcela;
import com.businessanalyzer.model.DatosProduccion;
import com.businessanalyzer.model.DestinoCosecha;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Especie;
import com.businessanalyzer.model.EstadoCampania;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Parcela;
import com.businessanalyzer.model.ProductoGanadero;
import com.businessanalyzer.model.RubroCosto;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.TamanoEmpresa;
import com.businessanalyzer.model.Tenencia;
import com.businessanalyzer.model.TipoMovimientoGanado;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La producción agropecuaria llega al estado de resultados, a la estadística, a la comparación y al diagnóstico. */
class IntegracionAgroTest {

    @TempDir
    Path carpeta;

    @Test
    void lasVentasAgropecuariasEntranAlResumenDeLosAnalisis() {
        EntornoPrueba entorno = new EntornoPrueba(carpeta);
        entorno.administradorConectado();
        Empresa fundo = entorno.empresaService.crear(new DatosEmpresa("Fundo Los Andes", null, null, Sector.AGROPECUARIO,
                TamanoEmpresa.MICRO, Moneda.PEN, 1, null, null, null, null));
        entorno.empresaService.seleccionarEmpresaActiva(fundo.id());

        entorno.ganaderia.registrarMovimiento(new DatosMovimientoGanado(LocalDate.of(2025, 6, 1),
                TipoMovimientoGanado.INVENTARIO_INICIAL, CategoriaGanado.VACA, 10, null, null, null, null));
        entorno.ganaderia.registrarMovimiento(new DatosMovimientoGanado(LocalDate.of(2025, 12, 10),
                TipoMovimientoGanado.VENTA, CategoriaGanado.VACA, 2, null, new BigDecimal("5000"), null, null));
        entorno.ganaderia.registrarProduccion(new DatosProduccion(Especie.VACUNO, ProductoGanadero.LECHE,
                LocalDate.of(2025, 12, 31), 31, new BigDecimal("1000"), 8, new BigDecimal("1.20"), null));

        // Papa: costo 3 000; 10 000 kg cosechados: 8 000 vendidos a 0.80, 1 000 para la familia y 1 000 de semilla.
        Parcela parcela = entorno.agricultura.registrarParcela(new DatosParcela("Alta", BigDecimal.ONE, Tenencia.PROPIA, false, null));
        Campania papa = entorno.agricultura.registrarCampania(new DatosCampania(parcela.id(), Cultivo.PAPA, null,
                LocalDate.of(2025, 9, 1), BigDecimal.ONE, null, null, null));
        entorno.agricultura.registrarCosto(papa.id(), LocalDate.of(2025, 9, 1), RubroCosto.SEMILLA, "Semilla", new BigDecimal("3000"));
        LocalDate cosecha = LocalDate.of(2025, 12, 15);
        entorno.agricultura.registrarCosecha(papa.id(), cosecha, new BigDecimal("8000"), DestinoCosecha.VENTA, new BigDecimal("0.80"), null);
        entorno.agricultura.registrarCosecha(papa.id(), cosecha, new BigDecimal("1000"), DestinoCosecha.AUTOCONSUMO, null, null);
        entorno.agricultura.registrarCosecha(papa.id(), cosecha, new BigDecimal("1000"), DestinoCosecha.SEMILLA, null, null);
        entorno.agricultura.cerrarCampania(papa.id(), EstadoCampania.COSECHADA, LocalDate.of(2025, 12, 20), null);

        LocalDate desde = LocalDate.of(2025, 12, 1);
        LocalDate hasta = LocalDate.of(2025, 12, 31);
        OperacionRepository operaciones = new OperacionRepository();
        ResumenOperaciones r = entorno.database.conConexion(c -> operaciones.resumen(c, fundo.id(), desde, hasta));

        igual("12600", r.ventasNetas());     // 5 000 + 1 200 + 6 400
        igual("2700", r.costoVentas());      // 90 % del costo: lo de la familia no es costo del negocio
        assertEquals(3, r.cantidadVentas());
        igual("0", entorno.operaciones.resumen(desde, hasta).ventasNetas());   // «Ventas y compras» no cambia
        igual("12600", entorno.operaciones.resumenConAgro(desde, hasta).ventasNetas());

        List<RentabilidadProducto> productos = RentabilidadProducto.agrupar(
                entorno.database.conConexion(c -> operaciones.lineasVendidas(c, fundo.id(), desde, hasta)));
        assertEquals(Set.of("Venta de vacunos", "Leche (vacunos)", "Papa"),
                productos.stream().map(RentabilidadProducto::nombre).collect(Collectors.toSet()));
        RentabilidadProducto papaVendida = productos.stream().filter(p -> p.nombre().equals("Papa")).findFirst().orElseThrow();
        igual("6400", papaVendida.ventas());

        assertEquals(1, entorno.database.conConexion(c -> operaciones.resumenMensual(c, fundo.id(),
                LocalDate.of(2025, 9, 1), hasta)).size());   // solo diciembre tiene movimiento
    }

    @Test
    void elFundoDemoTieneDiagnosticoAgropecuarioEvolucionYComparacion() {
        EntornoPrueba entorno = new EntornoPrueba(carpeta);
        entorno.administradorConectado();
        new DatosDemoAgroService(entorno.empresaService, entorno.ganaderia, entorno.agricultura, entorno.patrimonio,
                entorno.sesion, entorno.reloj, entorno.visitas, entorno.finanzas, entorno.tesoreria).crearFundoDemo();

        DiagnosticoService diagnostico = new DiagnosticoService(entorno.database, new OperacionRepository(),
                new MovimientoRepository(), entorno.empresaService, entorno.sesion, entorno.reloj);
        Diagnostico d = diagnostico.diagnosticar();
        assertTrue(d.evaluado(), "Con las ventas agropecuarias el productor ya tiene diagnóstico");
        List<Hallazgo> agro = d.deArea(Area.AGROPECUARIO);
        assertEquals(List.of(TipoIndicador.NATALIDAD, TipoIndicador.MORTALIDAD_CRIAS, TipoIndicador.RENDIMIENTO_CULTIVOS,
                TipoIndicador.COMPROMISOS_INSPECCION), agro.stream().map(Hallazgo::tipo).toList());
        assertTrue(agro.stream().allMatch(h -> h.semaforo() != Semaforo.SIN_DATOS), agro.toString());
        assertTrue(d.puntajePorArea().containsKey(Area.AGROPECUARIO));

        YearMonth hasta = YearMonth.from(LocalDate.now(entorno.reloj));
        EvolucionHato e = entorno.ganaderia.evolucion(hasta.minusMonths(23), hasta);
        assertEquals(24, e.meses().size());
        assertEquals(Set.of(Especie.VACUNO, Especie.OVINO, Especie.ALPACA), e.existencias().keySet());
        assertNotNull(e.tendencia(ProductoGanadero.LECHE));
        assertEquals(196, e.totalAnimales()[23]);
        assertTrue(e.lectura().size() >= 3, e.lectura().toString());

        List<ComparacionCultivos> comparacion = ComparacionCultivos.de(entorno.agricultura.listarCampanias(false));
        assertEquals(4, comparacion.size());            // papa, quinua, avena y maíz (la papa en curso no cuenta)
        assertEquals(Cultivo.QUINUA, comparacion.get(0).cultivo());   // mayor margen por hectárea
        ComparacionCultivos maiz = comparacion.stream().filter(x -> x.cultivo() == Cultivo.MAIZ_AMILACEO).findFirst().orElseThrow();
        assertEquals(1, maiz.perdidas());
        igual("0", maiz.rendimiento());
    }

    private static void igual(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }
}
