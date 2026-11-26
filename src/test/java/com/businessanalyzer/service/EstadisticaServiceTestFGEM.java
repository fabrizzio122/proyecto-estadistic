package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.estadistica.AnalisisAbcFGEM;
import com.businessanalyzer.analytics.estadistica.AnalisisEstadisticoFGEM;
import com.businessanalyzer.analytics.estadistica.LecturaEstadisticaFGEM;
import com.businessanalyzer.analytics.estadistica.TendenciaFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.DatosMovimientoFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM.DatosLineaFGEM;
import com.businessanalyzer.model.DatosProductoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.TipoMovimientoFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadisticaServiceTestFGEM {

    private static final LocalDate DESDE_FGEM = LocalDate.of(2025, 8, 1);
    private static final LocalDate HASTA_FGEM = LocalDate.of(2026, 1, 31);
    private static final double TOLERANCIA_FGEM = 1e-6;

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private EstadisticaServiceFGEM servicioFGEM;
    private EmpresaFGEM empresaFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = new EstadisticaServiceFGEM(entornoFGEM.databaseFGEM, new OperacionRepositoryFGEM(), new MovimientoRepositoryFGEM(),
                entornoFGEM.empresaServiceFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM);
        entornoFGEM.administradorConectadoFGEM();
        empresaFGEM = entornoFGEM.crearEmpresaActivaFGEM("Comercial Andina");

        ProductoFGEM p1FGEM = productoFGEM("P1", "60", "100");
        ProductoFGEM p2FGEM = productoFGEM("P2", "30", "50");
        ProductoFGEM p3FGEM = productoFGEM("P3", "5", "10");
        operacionFGEM(TipoOperacionFGEM.COMPRA, DESDE_FGEM, lineaFGEM(p1FGEM, "100", "60"), lineaFGEM(p2FGEM, "50", "30"), lineaFGEM(p3FGEM, "100", "5"));
        for (int kFGEM = 0; kFGEM < 5; kFGEM++) {
            YearMonth mesFGEM = YearMonth.of(2025, 8).plusMonths(kFGEM);
            operacionFGEM(TipoOperacionFGEM.VENTA, mesFGEM.atDay(10), lineaFGEM(p1FGEM, String.valueOf(10 + 2 * kFGEM), "100"));
            entornoFGEM.finanzasFGEM.registrarFGEM(new DatosMovimientoFGEM(TipoMovimientoFGEM.GASTO, CategoriaFinancieraFGEM.MARKETING, null,
                    mesFGEM.atDay(12), "Publicidad", BigDecimal.valueOf(100 + 20L * kFGEM), null));
        }
        operacionFGEM(TipoOperacionFGEM.VENTA, LocalDate.of(2026, 1, 10), lineaFGEM(p2FGEM, "10", "50"));
        operacionFGEM(TipoOperacionFGEM.VENTA, LocalDate.of(2026, 1, 12), lineaFGEM(p3FGEM, "25", "10"));
    }

    @Test
    void estadisticaDeLosTicketsFGEM() {
        AnalisisEstadisticoFGEM aFGEM = servicioFGEM.analizarFGEM(DESDE_FGEM, HASTA_FGEM);

        assertEquals(7, aFGEM.ticketFGEM().nFGEM());
        assertEquals(7750.0 / 7, aFGEM.ticketFGEM().mediaFGEM(), TOLERANCIA_FGEM);
        assertEquals(1200.0, aFGEM.ticketFGEM().medianaFGEM(), TOLERANCIA_FGEM, "250, 500, 1000, [1200], 1400, 1600, 1800");
        assertEquals(250.0, aFGEM.ticketFGEM().minimoFGEM(), TOLERANCIA_FGEM);
        assertEquals(1800.0, aFGEM.ticketFGEM().maximoFGEM(), TOLERANCIA_FGEM);
        assertEquals(168, aFGEM.ventasDiariasFGEM().nFGEM(), "Días del 1 de agosto al 15 de enero (hoy), incluidos los días sin ventas");
    }

    @Test
    void laTendenciaUsaSoloMesesCompletosFGEM() {
        AnalisisEstadisticoFGEM aFGEM = servicioFGEM.analizarFGEM(DESDE_FGEM, HASTA_FGEM);

        assertTrue(aFGEM.mesActualExcluidoFGEM());
        assertEquals(5, aFGEM.serieMensualFGEM().size(), "Ago–Dic; se omiten los meses previos sin ventas y enero (incompleto)");
        assertEquals(YearMonth.of(2025, 8), aFGEM.serieMensualFGEM().get(0).mesFGEM());
        TendenciaFGEM tFGEM = aFGEM.tendenciaVentasFGEM();
        assertEquals(200.0, tFGEM.pendienteFGEM(), TOLERANCIA_FGEM);
        assertEquals(1.0, tFGEM.r2FGEM(), TOLERANCIA_FGEM);
        assertEquals(TendenciaFGEM.DireccionFGEM.CRECIENTE, tFGEM.direccionFGEM());
    }

    @Test
    void correlacionEntreVentasYPublicidadFGEM() {
        AnalisisEstadisticoFGEM aFGEM = servicioFGEM.analizarFGEM(DESDE_FGEM, HASTA_FGEM);

        assertEquals(5, aFGEM.correlacionesFGEM().observacionesFGEM());
        int ventasFGEM = aFGEM.correlacionesFGEM().variablesFGEM().indexOf("Ventas netas");
        int publicidadFGEM = aFGEM.correlacionesFGEM().variablesFGEM().indexOf("Publicidad");
        assertEquals(1.0, aFGEM.correlacionesFGEM().rFGEM(ventasFGEM, publicidadFGEM), TOLERANCIA_FGEM);
    }

    @Test
    void clasificacionAbcDelPeriodoFGEM() {
        AnalisisAbcFGEM abcFGEM = servicioFGEM.analizarFGEM(DESDE_FGEM, HASTA_FGEM).abcFGEM();

        assertEquals(List.of("P1", "P2", "P3"), abcFGEM.itemsFGEM().stream().map(iFGEM -> iFGEM.productoFGEM().nombreFGEM()).toList());
        assertEquals(List.of(AnalisisAbcFGEM.ClaseFGEM.A, AnalisisAbcFGEM.ClaseFGEM.B, AnalisisAbcFGEM.ClaseFGEM.C),
                abcFGEM.itemsFGEM().stream().map(AnalisisAbcFGEM.ItemFGEM::claseFGEM).toList());
        assertEquals(0, new BigDecimal("7750").compareTo(abcFGEM.ventasTotalesFGEM()));
    }

    @Test
    void laInferenciaUsaLasVentasDiariasYElPeriodoAnteriorFGEM() {
        AnalisisEstadisticoFGEM aFGEM = servicioFGEM.analizarFGEM(DESDE_FGEM, HASTA_FGEM);

        assertEquals(168, aFGEM.intervaloDiarioFGEM().nFGEM());
        assertTrue(aFGEM.intervaloDiarioFGEM().inferiorFGEM() < aFGEM.ventasDiariasFGEM().mediaFGEM()
                && aFGEM.ventasDiariasFGEM().mediaFGEM() < aFGEM.intervaloDiarioFGEM().superiorFGEM());
        assertEquals(7, aFGEM.anovaDiasFGEM().gruposFGEM());
        assertTrue(aFGEM.anovaDiasFGEM().calculadoFGEM());
        assertEquals(LocalDate.of(2025, 2, 1), aFGEM.periodoAnteriorFGEM().desdeFGEM());
        assertTrue(!aFGEM.comparacionFGEM().calculadoFGEM());
        assertTrue(LecturaEstadisticaFGEM.comparacionFGEM(aFGEM, MonedaFGEM.PEN).get(0).startsWith("No hay ventas suficientes"));
        assertTrue(LecturaEstadisticaFGEM.intervaloFGEM(aFGEM, MonedaFGEM.PEN).get(0).startsWith("Con 95 % de confianza"));
    }

    @Test
    void lasLecturasExplicanLosResultadosFGEM() {
        AnalisisEstadisticoFGEM aFGEM = servicioFGEM.analizarFGEM(DESDE_FGEM, HASTA_FGEM);

        List<String> tendenciaFGEM = LecturaEstadisticaFGEM.tendenciaFGEM(aFGEM, MonedaFGEM.PEN);
        assertTrue(tendenciaFGEM.get(0).startsWith("Las ventas crecen en promedio S/ 200.00 por mes"), tendenciaFGEM.toString());
        assertTrue(tendenciaFGEM.contains("El mes en curso no se incluye porque aún no termina."));
        assertTrue(LecturaEstadisticaFGEM.abcFGEM(aFGEM).get(0).startsWith("1 de 3 productos"), LecturaEstadisticaFGEM.abcFGEM(aFGEM).toString());
        assertTrue(LecturaEstadisticaFGEM.correlacionesFGEM(aFGEM).stream().anyMatch(fFGEM -> fFGEM.contains("positiva fuerte")));
        assertTrue(LecturaEstadisticaFGEM.resumenFGEM(aFGEM, MonedaFGEM.PEN).get(0).contains("mediana) fue de S/ 1,200.00"),
                LecturaEstadisticaFGEM.resumenFGEM(aFGEM, MonedaFGEM.PEN).toString());
    }

    @Test
    void unPeriodoPasadoUsaSusPropiosMesesFGEM() {
        AnalisisEstadisticoFGEM aFGEM = servicioFGEM.analizarFGEM(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 10, 31));

        assertTrue(!aFGEM.mesActualExcluidoFGEM());
        assertEquals(YearMonth.of(2025, 10), aFGEM.serieMensualFGEM().get(aFGEM.serieMensualFGEM().size() - 1).mesFGEM());
        assertEquals(3, aFGEM.ticketFGEM().nFGEM());
    }

    @Test
    void requiereElPermisoDeAnalisisFGEM() {
        UsuarioFGEM operadorFGEM = entornoFGEM.crearUsuarioFGEM("operador", "OPERADOR");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of(operadorFGEM.idFGEM()));
        entornoFGEM.entrarComoFGEM("operador", "Temporal123");
        entornoFGEM.empresaServiceFGEM.restaurarEmpresaActivaFGEM();

        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.analizarFGEM(DESDE_FGEM, HASTA_FGEM));
    }

    private ProductoFGEM productoFGEM(String nombreFGEM, String costoFGEM, String precioFGEM) {
        return entornoFGEM.catalogoFGEM.crearProductoFGEM(new DatosProductoFGEM(TipoItemFGEM.PRODUCTO, null, nombreFGEM, null, null,
                UnidadMedidaFGEM.UNIDAD, new BigDecimal(costoFGEM), new BigDecimal(precioFGEM)));
    }

    private void operacionFGEM(TipoOperacionFGEM tipoFGEM, LocalDate fechaFGEM, DatosLineaFGEM... lineasFGEM) {
        entornoFGEM.operacionesFGEM.registrarFGEM(new DatosOperacionFGEM(tipoFGEM, fechaFGEM, null, null, null, BigDecimal.ZERO,
                null, List.of(lineasFGEM), false), false);
    }

    private static DatosLineaFGEM lineaFGEM(ProductoFGEM pFGEM, String cantidadFGEM, String precioFGEM) {
        return new DatosLineaFGEM(pFGEM.idFGEM(), new BigDecimal(cantidadFGEM), new BigDecimal(precioFGEM));
    }
}
