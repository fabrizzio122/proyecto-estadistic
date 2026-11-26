package com.businessanalyzer.analytics.agricultura;

import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;
import com.businessanalyzer.analytics.ganaderia.IndicadorGanaderoFGEM;
import com.businessanalyzer.model.CampaniaFGEM;
import com.businessanalyzer.model.CosechaFGEM;
import com.businessanalyzer.model.CostoCampaniaFGEM;
import com.businessanalyzer.model.CultivoFGEM;
import com.businessanalyzer.model.DestinoCosechaFGEM;
import com.businessanalyzer.model.EstadoCampaniaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.RubroCostoFGEM;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResultadoCampaniaTestFGEM {

    private static final LocalDate SIEMBRA_FGEM = LocalDate.of(2025, 10, 1);

    @Test
    void calculaRendimientoValorYMargenFGEM() {
        ResultadoCampaniaFGEM rFGEM = ResultadoCampaniaFGEM.deFGEM(papaFGEM(EstadoCampaniaFGEM.COSECHADA, "0.80"),
                List.of(costoFGEM(RubroCostoFGEM.SEMILLA, "3000"), costoFGEM(RubroCostoFGEM.MANO_OBRA, "5000"),
                        costoAnuladoFGEM()),
                List.of(cosechaFGEM("9000", DestinoCosechaFGEM.VENTA, "0.90"), cosechaFGEM("2000", DestinoCosechaFGEM.AUTOCONSUMO, null),
                        cosechaFGEM("1000", DestinoCosechaFGEM.SEMILLA, null), cosechaFGEM("500", DestinoCosechaFGEM.MERMA, null)));

        igualFGEM("8000", rFGEM.costoTotalFGEM());
        igualFGEM("12500", rFGEM.produccionFGEM());
        igualFGEM("12500", rFGEM.rendimientoFGEM());
        igualFGEM("0.833333", rFGEM.avanceReferenciaFGEM());
        igualFGEM("8100", rFGEM.ingresoVentasFGEM());
        igualFGEM("10500", rFGEM.valorProduccionFGEM());
        igualFGEM("2500", rFGEM.margenFGEM());
        igualFGEM("0.3125", rFGEM.rentabilidadFGEM());
        igualFGEM("0.64", rFGEM.costoPorKiloFGEM());
        igualFGEM("10000", rFGEM.rendimientoEquilibrioFGEM());
        igualFGEM("0.72", rFGEM.parteVendidaFGEM());

        List<IndicadorGanaderoFGEM> indFGEM = rFGEM.evaluarFGEM(MonedaFGEM.PEN);
        assertEquals(SemaforoFGEM.AMARILLO, buscarFGEM(indFGEM, "Rendimiento").semaforoFGEM());
        assertEquals(SemaforoFGEM.VERDE, buscarFGEM(indFGEM, "Rendimiento para no perder").semaforoFGEM());
        assertEquals(SemaforoFGEM.VERDE, buscarFGEM(indFGEM, "Margen").semaforoFGEM());
    }

    @Test
    void sinPrecioEnChacraUsaElPrecioPromedioDeVentaFGEM() {
        ResultadoCampaniaFGEM rFGEM = ResultadoCampaniaFGEM.deFGEM(papaFGEM(EstadoCampaniaFGEM.COSECHADA, null), List.of(costoFGEM(RubroCostoFGEM.SEMILLA, "1000")),
                List.of(cosechaFGEM("1000", DestinoCosechaFGEM.VENTA, "1.00"), cosechaFGEM("1000", DestinoCosechaFGEM.VENTA, "0.50"),
                        cosechaFGEM("500", DestinoCosechaFGEM.ANIMALES, null)));
        igualFGEM("0.75", rFGEM.precioValorizacionFGEM());
        igualFGEM("1875", rFGEM.valorProduccionFGEM());
    }

    @Test
    void unaCampaniaPerdidaTieneRendimientoCeroYMargenNegativoFGEM() {
        ResultadoCampaniaFGEM rFGEM = ResultadoCampaniaFGEM.deFGEM(papaFGEM(EstadoCampaniaFGEM.PERDIDA, "0.80"),
                List.of(costoFGEM(RubroCostoFGEM.SEMILLA, "2000")), List.of());
        igualFGEM("0", rFGEM.rendimientoFGEM());
        igualFGEM("-2000", rFGEM.margenFGEM());
        List<IndicadorGanaderoFGEM> indFGEM = rFGEM.evaluarFGEM(MonedaFGEM.PEN);
        assertEquals(SemaforoFGEM.ROJO, buscarFGEM(indFGEM, "Rendimiento").semaforoFGEM());
        assertEquals(SemaforoFGEM.ROJO, buscarFGEM(indFGEM, "Margen").semaforoFGEM());
        assertEquals("Campaña perdida: Helada.", buscarFGEM(indFGEM, "Rendimiento").lecturaFGEM());
    }

    @Test
    void enCursoSinCosechaNoTieneRendimientoFGEM() {
        ResultadoCampaniaFGEM rFGEM = ResultadoCampaniaFGEM.deFGEM(papaFGEM(EstadoCampaniaFGEM.EN_CURSO, null), List.of(), List.of());
        assertNull(rFGEM.rendimientoFGEM());
        assertNull(rFGEM.rentabilidadFGEM());
        assertNull(rFGEM.rendimientoEquilibrioFGEM());
        assertEquals(SemaforoFGEM.SIN_DATOS, buscarFGEM(rFGEM.evaluarFGEM(MonedaFGEM.PEN), "Rendimiento").semaforoFGEM());
    }

    private static CampaniaFGEM papaFGEM(EstadoCampaniaFGEM estadoFGEM, String precioChacraFGEM) {
        return new CampaniaFGEM(1, 1, 1, "Parcela Alta", CultivoFGEM.PAPA, "Yungay", SIEMBRA_FGEM, BigDecimal.ONE,
                CultivoFGEM.PAPA.rendimientoReferenciaFGEM(), precioChacraFGEM == null ? null : new BigDecimal(precioChacraFGEM), estadoFGEM,
                null, estadoFGEM == EstadoCampaniaFGEM.PERDIDA ? "Helada" : null, null, "admin");
    }

    private static CostoCampaniaFGEM costoFGEM(RubroCostoFGEM rubroFGEM, String montoFGEM) {
        return new CostoCampaniaFGEM(1, 1, SIEMBRA_FGEM, rubroFGEM, rubroFGEM.descripcionFGEM(), new BigDecimal(montoFGEM), false, "admin");
    }

    private static CostoCampaniaFGEM costoAnuladoFGEM() {
        return new CostoCampaniaFGEM(2, 1, SIEMBRA_FGEM, RubroCostoFGEM.OTROS, "Error", new BigDecimal("999"), true, "admin");
    }

    private static CosechaFGEM cosechaFGEM(String kilosFGEM, DestinoCosechaFGEM destinoFGEM, String precioFGEM) {
        return new CosechaFGEM(1, 1, SIEMBRA_FGEM.plusMonths(6), new BigDecimal(kilosFGEM), destinoFGEM,
                precioFGEM == null ? null : new BigDecimal(precioFGEM), null, false, "admin");
    }

    private static IndicadorGanaderoFGEM buscarFGEM(List<IndicadorGanaderoFGEM> listaFGEM, String nombreFGEM) {
        return listaFGEM.stream().filter(iFGEM -> iFGEM.nombreFGEM().equals(nombreFGEM)).findFirst().orElseThrow();
    }

    private static void igualFGEM(String esperadoFGEM, BigDecimal realFGEM) {
        assertEquals(0, new BigDecimal(esperadoFGEM).compareTo(realFGEM), "esperado " + esperadoFGEM + " pero fue " + realFGEM);
    }
}
