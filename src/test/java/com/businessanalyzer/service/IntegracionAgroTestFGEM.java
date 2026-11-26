package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM;
import com.businessanalyzer.analytics.ResumenOperacionesFGEM;
import com.businessanalyzer.analytics.agricultura.ComparacionCultivosFGEM;
import com.businessanalyzer.analytics.diagnostico.AreaFGEM;
import com.businessanalyzer.analytics.diagnostico.DiagnosticoFGEM;
import com.businessanalyzer.analytics.diagnostico.HallazgoFGEM;
import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;
import com.businessanalyzer.analytics.diagnostico.TipoIndicadorFGEM;
import com.businessanalyzer.analytics.ganaderia.EvolucionHatoFGEM;
import com.businessanalyzer.model.CampaniaFGEM;
import com.businessanalyzer.model.CategoriaGanadoFGEM;
import com.businessanalyzer.model.CultivoFGEM;
import com.businessanalyzer.model.DatosCampaniaFGEM;
import com.businessanalyzer.model.DatosEmpresaFGEM;
import com.businessanalyzer.model.DatosMovimientoGanadoFGEM;
import com.businessanalyzer.model.DatosParcelaFGEM;
import com.businessanalyzer.model.DatosProduccionFGEM;
import com.businessanalyzer.model.DestinoCosechaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EspecieFGEM;
import com.businessanalyzer.model.EstadoCampaniaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.ParcelaFGEM;
import com.businessanalyzer.model.ProductoGanaderoFGEM;
import com.businessanalyzer.model.RubroCostoFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.TamanoEmpresaFGEM;
import com.businessanalyzer.model.TenenciaFGEM;
import com.businessanalyzer.model.TipoMovimientoGanadoFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
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

class IntegracionAgroTestFGEM {

    @TempDir
    Path carpetaFGEM;

    @Test
    void lasVentasAgropecuariasEntranAlResumenDeLosAnalisisFGEM() {
        EntornoPruebaFGEM entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        entornoFGEM.administradorConectadoFGEM();
        EmpresaFGEM fundoFGEM = entornoFGEM.empresaServiceFGEM.crearFGEM(new DatosEmpresaFGEM("Fundo Los Andes", null, null, SectorFGEM.AGROPECUARIO,
                TamanoEmpresaFGEM.MICRO, MonedaFGEM.PEN, 1, null, null, null, null));
        entornoFGEM.empresaServiceFGEM.seleccionarEmpresaActivaFGEM(fundoFGEM.idFGEM());

        entornoFGEM.ganaderiaFGEM.registrarMovimientoFGEM(new DatosMovimientoGanadoFGEM(LocalDate.of(2025, 6, 1),
                TipoMovimientoGanadoFGEM.INVENTARIO_INICIAL, CategoriaGanadoFGEM.VACA, 10, null, null, null, null));
        entornoFGEM.ganaderiaFGEM.registrarMovimientoFGEM(new DatosMovimientoGanadoFGEM(LocalDate.of(2025, 12, 10),
                TipoMovimientoGanadoFGEM.VENTA, CategoriaGanadoFGEM.VACA, 2, null, new BigDecimal("5000"), null, null));
        entornoFGEM.ganaderiaFGEM.registrarProduccionFGEM(new DatosProduccionFGEM(EspecieFGEM.VACUNO, ProductoGanaderoFGEM.LECHE,
                LocalDate.of(2025, 12, 31), 31, new BigDecimal("1000"), 8, new BigDecimal("1.20"), null));

        ParcelaFGEM parcelaFGEM = entornoFGEM.agriculturaFGEM.registrarParcelaFGEM(new DatosParcelaFGEM("Alta", BigDecimal.ONE, TenenciaFGEM.PROPIA, false, null));
        CampaniaFGEM papaFGEM = entornoFGEM.agriculturaFGEM.registrarCampaniaFGEM(new DatosCampaniaFGEM(parcelaFGEM.idFGEM(), CultivoFGEM.PAPA, null,
                LocalDate.of(2025, 9, 1), BigDecimal.ONE, null, null, null));
        entornoFGEM.agriculturaFGEM.registrarCostoFGEM(papaFGEM.idFGEM(), LocalDate.of(2025, 9, 1), RubroCostoFGEM.SEMILLA, "Semilla", new BigDecimal("3000"));
        LocalDate cosechaFGEM = LocalDate.of(2025, 12, 15);
        entornoFGEM.agriculturaFGEM.registrarCosechaFGEM(papaFGEM.idFGEM(), cosechaFGEM, new BigDecimal("8000"), DestinoCosechaFGEM.VENTA, new BigDecimal("0.80"), null);
        entornoFGEM.agriculturaFGEM.registrarCosechaFGEM(papaFGEM.idFGEM(), cosechaFGEM, new BigDecimal("1000"), DestinoCosechaFGEM.AUTOCONSUMO, null, null);
        entornoFGEM.agriculturaFGEM.registrarCosechaFGEM(papaFGEM.idFGEM(), cosechaFGEM, new BigDecimal("1000"), DestinoCosechaFGEM.SEMILLA, null, null);
        entornoFGEM.agriculturaFGEM.cerrarCampaniaFGEM(papaFGEM.idFGEM(), EstadoCampaniaFGEM.COSECHADA, LocalDate.of(2025, 12, 20), null);

        LocalDate desdeFGEM = LocalDate.of(2025, 12, 1);
        LocalDate hastaFGEM = LocalDate.of(2025, 12, 31);
        OperacionRepositoryFGEM operacionesFGEM = new OperacionRepositoryFGEM();
        ResumenOperacionesFGEM rFGEM = entornoFGEM.databaseFGEM.conConexionFGEM(cFGEM -> operacionesFGEM.resumenFGEM(cFGEM, fundoFGEM.idFGEM(), desdeFGEM, hastaFGEM));

        igualFGEM("12600", rFGEM.ventasNetasFGEM());
        igualFGEM("2700", rFGEM.costoVentasFGEM());
        assertEquals(3, rFGEM.cantidadVentasFGEM());
        igualFGEM("0", entornoFGEM.operacionesFGEM.resumenFGEM(desdeFGEM, hastaFGEM).ventasNetasFGEM());
        igualFGEM("12600", entornoFGEM.operacionesFGEM.resumenConAgroFGEM(desdeFGEM, hastaFGEM).ventasNetasFGEM());

        List<RentabilidadProductoFGEM> productosFGEM = RentabilidadProductoFGEM.agruparFGEM(
                entornoFGEM.databaseFGEM.conConexionFGEM(cFGEM -> operacionesFGEM.lineasVendidasFGEM(cFGEM, fundoFGEM.idFGEM(), desdeFGEM, hastaFGEM)));
        assertEquals(Set.of("Venta de vacunos", "Leche (vacunos)", "Papa"),
                productosFGEM.stream().map(RentabilidadProductoFGEM::nombreFGEM).collect(Collectors.toSet()));
        RentabilidadProductoFGEM papaVendidaFGEM = productosFGEM.stream().filter(pFGEM -> pFGEM.nombreFGEM().equals("Papa")).findFirst().orElseThrow();
        igualFGEM("6400", papaVendidaFGEM.ventasFGEM());

        assertEquals(1, entornoFGEM.databaseFGEM.conConexionFGEM(cFGEM -> operacionesFGEM.resumenMensualFGEM(cFGEM, fundoFGEM.idFGEM(),
                LocalDate.of(2025, 9, 1), hastaFGEM)).size());
    }

    @Test
    void elFundoDemoTieneDiagnosticoAgropecuarioEvolucionYComparacionFGEM() {
        EntornoPruebaFGEM entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        entornoFGEM.administradorConectadoFGEM();
        new DatosDemoAgroServiceFGEM(entornoFGEM.empresaServiceFGEM, entornoFGEM.ganaderiaFGEM, entornoFGEM.agriculturaFGEM, entornoFGEM.patrimonioFGEM,
                entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM, entornoFGEM.visitasFGEM, entornoFGEM.finanzasFGEM, entornoFGEM.tesoreriaFGEM).crearFundoDemoFGEM();

        DiagnosticoServiceFGEM diagnosticoFGEM = new DiagnosticoServiceFGEM(entornoFGEM.databaseFGEM, new OperacionRepositoryFGEM(),
                new MovimientoRepositoryFGEM(), entornoFGEM.empresaServiceFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM);
        DiagnosticoFGEM dFGEM = diagnosticoFGEM.diagnosticarFGEM();
        assertTrue(dFGEM.evaluadoFGEM(), "Con las ventas agropecuarias el productor ya tiene diagnóstico");
        List<HallazgoFGEM> agroFGEM = dFGEM.deAreaFGEM(AreaFGEM.AGROPECUARIO);
        assertEquals(List.of(TipoIndicadorFGEM.NATALIDAD, TipoIndicadorFGEM.MORTALIDAD_CRIAS, TipoIndicadorFGEM.RENDIMIENTO_CULTIVOS,
                TipoIndicadorFGEM.COMPROMISOS_INSPECCION), agroFGEM.stream().map(HallazgoFGEM::tipoFGEM).toList());
        assertTrue(agroFGEM.stream().allMatch(hFGEM -> hFGEM.semaforoFGEM() != SemaforoFGEM.SIN_DATOS), agroFGEM.toString());
        assertTrue(dFGEM.puntajePorAreaFGEM().containsKey(AreaFGEM.AGROPECUARIO));

        YearMonth hastaFGEM = YearMonth.from(LocalDate.now(entornoFGEM.relojFGEM));
        EvolucionHatoFGEM eFGEM = entornoFGEM.ganaderiaFGEM.evolucionFGEM(hastaFGEM.minusMonths(23), hastaFGEM);
        assertEquals(24, eFGEM.mesesFGEM().size());
        assertEquals(Set.of(EspecieFGEM.VACUNO, EspecieFGEM.OVINO, EspecieFGEM.ALPACA), eFGEM.existenciasFGEM().keySet());
        assertNotNull(eFGEM.tendenciaFGEM(ProductoGanaderoFGEM.LECHE));
        assertEquals(196, eFGEM.totalAnimalesFGEM()[23]);
        assertTrue(eFGEM.lecturaFGEM().size() >= 3, eFGEM.lecturaFGEM().toString());

        List<ComparacionCultivosFGEM> comparacionFGEM = ComparacionCultivosFGEM.deFGEM(entornoFGEM.agriculturaFGEM.listarCampaniasFGEM(false));
        assertEquals(4, comparacionFGEM.size());
        assertEquals(CultivoFGEM.QUINUA, comparacionFGEM.get(0).cultivoFGEM());
        ComparacionCultivosFGEM maizFGEM = comparacionFGEM.stream().filter(xFGEM -> xFGEM.cultivoFGEM() == CultivoFGEM.MAIZ_AMILACEO).findFirst().orElseThrow();
        assertEquals(1, maizFGEM.perdidasFGEM());
        igualFGEM("0", maizFGEM.rendimientoFGEM());
    }

    private static void igualFGEM(String esperadoFGEM, BigDecimal realFGEM) {
        assertEquals(0, new BigDecimal(esperadoFGEM).compareTo(realFGEM), "esperado " + esperadoFGEM + " pero fue " + realFGEM);
    }
}
