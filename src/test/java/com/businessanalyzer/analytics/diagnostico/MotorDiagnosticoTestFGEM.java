package com.businessanalyzer.analytics.diagnostico;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM;
import com.businessanalyzer.analytics.ResumenFinancieroFGEM;
import com.businessanalyzer.analytics.ResumenOperacionesFGEM;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneralFGEM;
import com.businessanalyzer.analytics.patrimonio.IndicadoresPatrimonioFGEM;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonialFGEM;
import com.businessanalyzer.analytics.tesoreria.ProyeccionCajaFGEM;
import com.businessanalyzer.model.ActivoFGEM;
import com.businessanalyzer.model.CategoriaActivoFGEM;
import com.businessanalyzer.model.CategoriaDeudaFGEM;
import com.businessanalyzer.model.DeudaFGEM;
import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Negocio sano (3 meses): ventas 30 000, costo 21 000 (margen bruto 30 %), alquiler 4 500,
 * fletes 600, intereses 150, impuesto 900 -> utilidad neta 2 850 (9.5 %), margen de seguridad 44.6 %.
 */
class MotorDiagnosticoTestFGEM {

    private static final PeriodoFGEM PERIODO_FGEM = new PeriodoFGEM(YearMonth.of(2025, 10).atDay(1), YearMonth.of(2025, 12).atEndOfMonth());

    @Test
    void negocioSanoQuedaEnVerdeYSinRecomendacionesFGEM() {
        DiagnosticoFGEM dFGEM = MotorDiagnosticoFGEM.evaluarFGEM(datosFGEM("21000", "30000", crecientesFGEM(12), productosSanosFGEM()));

        assertEquals(NivelSaludFGEM.SALUDABLE, dFGEM.nivelFGEM());
        assertEquals(100, dFGEM.puntajeFGEM());
        assertEquals(java.util.Arrays.stream(TipoIndicadorFGEM.values()).filter(tFGEM -> !tFGEM.soloAgropecuarioFGEM()).count(), (long) dFGEM.hallazgosFGEM().size());
        assertEquals(0, dFGEM.contarFGEM(SemaforoFGEM.ROJO) + dFGEM.contarFGEM(SemaforoFGEM.AMARILLO), dFGEM.hallazgosFGEM().toString());
        assertTrue(dFGEM.recomendacionesFGEM().isEmpty());
        assertEquals("9.5 %", hallazgoFGEM(dFGEM, TipoIndicadorFGEM.MARGEN_NETO).valorFGEM());
        assertEquals("44.6 %", hallazgoFGEM(dFGEM, TipoIndicadorFGEM.MARGEN_SEGURIDAD).valorFGEM());
        assertEquals(SemaforoFGEM.VERDE, hallazgoFGEM(dFGEM, TipoIndicadorFGEM.TENDENCIA_VENTAS).semaforoFGEM());
        assertNull(hallazgoFGEM(dFGEM, TipoIndicadorFGEM.MARGEN_NETO).recomendacionFGEM());
    }

    @Test
    void conPerdidaLaCalificacionSeLimitaYSeRecomiendaSubirPreciosFGEM() {
        // Costo 27 000: margen bruto 10 % y pérdida.
        DiagnosticoFGEM dFGEM = MotorDiagnosticoFGEM.evaluarFGEM(datosFGEM("27000", "30000", crecientesFGEM(12), productosSanosFGEM()));

        HallazgoFGEM netoFGEM = hallazgoFGEM(dFGEM, TipoIndicadorFGEM.MARGEN_NETO);
        assertEquals(SemaforoFGEM.ROJO, netoFGEM.semaforoFGEM());
        assertTrue(netoFGEM.recomendacionFGEM().contains("subir los precios"), netoFGEM.recomendacionFGEM());
        assertEquals(SemaforoFGEM.ROJO, hallazgoFGEM(dFGEM, TipoIndicadorFGEM.MARGEN_BRUTO).semaforoFGEM());
        assertTrue(dFGEM.nivelFGEM() == NivelSaludFGEM.EN_RIESGO || dFGEM.nivelFGEM() == NivelSaludFGEM.CRITICO);
        assertEquals(TipoIndicadorFGEM.MARGEN_NETO.pesoFGEM() + 20, dFGEM.recomendacionesFGEM().get(0).prioridadFGEM());
        assertEquals(SemaforoFGEM.ROJO, dFGEM.recomendacionesFGEM().get(0).semaforoFGEM());
    }

    @Test
    void sinBalanceLaLiquidezYElEndeudamientoNoSeEvaluanFGEM() {
        DiagnosticoFGEM dFGEM = MotorDiagnosticoFGEM.evaluarFGEM(datosFGEM("21000", "30000", crecientesFGEM(12), productosSanosFGEM()));
        assertEquals(SemaforoFGEM.SIN_DATOS, hallazgoFGEM(dFGEM, TipoIndicadorFGEM.LIQUIDEZ).semaforoFGEM());
        assertEquals(SemaforoFGEM.SIN_DATOS, hallazgoFGEM(dFGEM, TipoIndicadorFGEM.ENDEUDAMIENTO).semaforoFGEM());
    }

    @Test
    void evaluaLiquidezYEndeudamientoConElBalanceFGEM() {
        // Holgado: caja 9 000 y local 20 000 frente a 3 000 de deuda de corto plazo.
        DiagnosticoFGEM sanoFGEM = MotorDiagnosticoFGEM.evaluarFGEM(conBalanceFGEM(List.of(
                activoFGEM(CategoriaActivoFGEM.CAJA_BANCOS, "9000"), activoFGEM(CategoriaActivoFGEM.EDIFICIOS, "20000")), "3000"));
        assertEquals(SemaforoFGEM.VERDE, hallazgoFGEM(sanoFGEM, TipoIndicadorFGEM.LIQUIDEZ).semaforoFGEM());
        assertEquals("3.00", hallazgoFGEM(sanoFGEM, TipoIndicadorFGEM.LIQUIDEZ).valorFGEM());
        assertEquals(SemaforoFGEM.VERDE, hallazgoFGEM(sanoFGEM, TipoIndicadorFGEM.ENDEUDAMIENTO).semaforoFGEM());
        assertEquals(100, sanoFGEM.puntajeFGEM());

        // Apretado: caja 2 000 frente a 2 500 que vencen en el año (liquidez 0.80); deudas = 62.5 % de los activos.
        DiagnosticoFGEM apretadoFGEM = MotorDiagnosticoFGEM.evaluarFGEM(conBalanceFGEM(List.of(
                activoFGEM(CategoriaActivoFGEM.CAJA_BANCOS, "2000"), activoFGEM(CategoriaActivoFGEM.EDIFICIOS, "2000")), "2500"));
        HallazgoFGEM liquidezFGEM = hallazgoFGEM(apretadoFGEM, TipoIndicadorFGEM.LIQUIDEZ);
        assertEquals(SemaforoFGEM.ROJO, liquidezFGEM.semaforoFGEM());
        assertTrue(liquidezFGEM.recomendacionFGEM().contains("S/ 500.00"), liquidezFGEM.recomendacionFGEM());
        assertEquals(SemaforoFGEM.AMARILLO, hallazgoFGEM(apretadoFGEM, TipoIndicadorFGEM.ENDEUDAMIENTO).semaforoFGEM());
        assertTrue(apretadoFGEM.puntajeFGEM() < 100);
    }

    @Test
    void conPatrimonioNegativoElEndeudamientoEstaEnRojoFGEM() {
        DiagnosticoFGEM dFGEM = MotorDiagnosticoFGEM.evaluarFGEM(conBalanceFGEM(List.of(activoFGEM(CategoriaActivoFGEM.CAJA_BANCOS, "1000")), "4000"));
        HallazgoFGEM hFGEM = hallazgoFGEM(dFGEM, TipoIndicadorFGEM.ENDEUDAMIENTO);
        assertEquals(SemaforoFGEM.ROJO, hFGEM.semaforoFGEM());
        assertTrue(hFGEM.lecturaFGEM().contains("patrimonio es negativo"), hFGEM.lecturaFGEM());
    }

    private static DatosDiagnosticoFGEM conBalanceFGEM(List<ActivoFGEM> activosFGEM, String deudaCortoPlazoFGEM) {
        DatosDiagnosticoFGEM baseFGEM = datosFGEM("21000", "30000", crecientesFGEM(12), productosSanosFGEM());
        LocalDate corteFGEM = PERIODO_FGEM.hastaFGEM();
        DeudaFGEM deudaFGEM = new DeudaFGEM(1, 1, CategoriaDeudaFGEM.PROVEEDORES, "Proveedor", null, corteFGEM, bdFGEM(deudaCortoPlazoFGEM),
                BigDecimal.ZERO, null, null, null, false, null, "admin", null, null, null);
        BalanceGeneralFGEM balanceFGEM = BalanceGeneralFGEM.deFGEM(corteFGEM, activosFGEM, BigDecimal.ZERO, List.of(deudaFGEM));
        SituacionPatrimonialFGEM sFGEM = new SituacionPatrimonialFGEM(balanceFGEM, IndicadoresPatrimonioFGEM.deFGEM(balanceFGEM, null, 0), null);
        return new DatosDiagnosticoFGEM(baseFGEM.sectorFGEM(), baseFGEM.monedaFGEM(), baseFGEM.periodoFGEM(), 3, baseFGEM.operacionesFGEM(), baseFGEM.finanzasFGEM(),
                baseFGEM.operacionesAnterioresFGEM(), baseFGEM.mesesFGEM(), baseFGEM.ventasMensualesFGEM(), baseFGEM.utilidadMensualFGEM(),
                baseFGEM.productosFGEM(), sFGEM);
    }

    @Test
    void laCajaProyectadaAvisaCuandoElDineroNoAlcanzaFGEM() {
        DatosDiagnosticoFGEM baseFGEM = datosFGEM("21000", "30000", crecientesFGEM(12), productosSanosFGEM());
        LocalDate hoyFGEM = LocalDate.of(2026, 1, 31);

        // Sin historia en tesorería no se evalúa.
        assertEquals(SemaforoFGEM.SIN_DATOS, hallazgoFGEM(MotorDiagnosticoFGEM.evaluarFGEM(baseFGEM), TipoIndicadorFGEM.CAJA_PROYECTADA).semaforoFGEM());

        // La operación pierde 400 al mes y hay 1 000 en las cuentas: falta dinero en abril.
        ProyeccionCajaFGEM pierdeFGEM = ProyeccionCajaFGEM.calcularFGEM(hoyFGEM, bdFGEM("1000"),
                new double[]{-400, -400, -400, -400, -400, -400}, List.of());
        HallazgoFGEM rojoFGEM = hallazgoFGEM(MotorDiagnosticoFGEM.evaluarFGEM(conCajaFGEM(baseFGEM, pierdeFGEM)), TipoIndicadorFGEM.CAJA_PROYECTADA);
        assertEquals(SemaforoFGEM.ROJO, rojoFGEM.semaforoFGEM());
        assertTrue(rojoFGEM.valorFGEM().contains("abril 2026"), rojoFGEM.valorFGEM());
        assertTrue(rojoFGEM.recomendacionFGEM().contains("adicionales"), rojoFGEM.recomendacionFGEM());

        // Flujos positivos y estables: alcanza incluso en el escenario pesimista.
        ProyeccionCajaFGEM ganaFGEM = ProyeccionCajaFGEM.calcularFGEM(hoyFGEM, bdFGEM("1000"),
                new double[]{800, 810, 790, 805, 800, 795}, List.of());
        assertEquals(SemaforoFGEM.VERDE,
                hallazgoFGEM(MotorDiagnosticoFGEM.evaluarFGEM(conCajaFGEM(baseFGEM, ganaFGEM)), TipoIndicadorFGEM.CAJA_PROYECTADA).semaforoFGEM());
    }

    private static DatosDiagnosticoFGEM conCajaFGEM(DatosDiagnosticoFGEM baseFGEM, ProyeccionCajaFGEM cajaFGEM) {
        return new DatosDiagnosticoFGEM(baseFGEM.sectorFGEM(), baseFGEM.monedaFGEM(), baseFGEM.periodoFGEM(), 3, baseFGEM.operacionesFGEM(), baseFGEM.finanzasFGEM(),
                baseFGEM.operacionesAnterioresFGEM(), baseFGEM.mesesFGEM(), baseFGEM.ventasMensualesFGEM(), baseFGEM.utilidadMensualFGEM(),
                baseFGEM.productosFGEM(), null, null, cajaFGEM);
    }

    private static ActivoFGEM activoFGEM(CategoriaActivoFGEM categoriaFGEM, String valorFGEM) {
        return new ActivoFGEM(1, 1, categoriaFGEM, categoriaFGEM.descripcionFGEM(), PERIODO_FGEM.hastaFGEM(), bdFGEM(valorFGEM), null, BigDecimal.ZERO,
                false, null, null, "admin", null, null, null);
    }

    @Test
    void indicadorClaveEnRojoLimitaLaCalificacionFGEM() {
        // Margen de seguridad bajo (costos fijos altos) con el resto en verde.
        DatosDiagnosticoFGEM baseFGEM = datosFGEM("21000", "30000", crecientesFGEM(12), productosSanosFGEM());
        ResumenFinancieroFGEM fijosAltosFGEM = finanzasFGEM("8200", "600", "150", "0");
        DatosDiagnosticoFGEM dFGEM = new DatosDiagnosticoFGEM(baseFGEM.sectorFGEM(), baseFGEM.monedaFGEM(), baseFGEM.periodoFGEM(), 3, baseFGEM.operacionesFGEM(),
                fijosAltosFGEM, baseFGEM.operacionesAnterioresFGEM(), baseFGEM.mesesFGEM(), baseFGEM.ventasMensualesFGEM(), baseFGEM.utilidadMensualFGEM(),
                baseFGEM.productosFGEM());

        DiagnosticoFGEM resultadoFGEM = MotorDiagnosticoFGEM.evaluarFGEM(dFGEM);

        assertEquals(SemaforoFGEM.ROJO, hallazgoFGEM(resultadoFGEM, TipoIndicadorFGEM.MARGEN_SEGURIDAD).semaforoFGEM());
        assertEquals(NivelSaludFGEM.EN_RIESGO, resultadoFGEM.nivelFGEM());
        assertTrue(resultadoFGEM.resumenFGEM().stream().anyMatch(fFGEM -> fFGEM.contains("se limita")));
    }

    @Test
    void productoVendidoBajoCostoYDependenciaFGEM() {
        List<RentabilidadProductoFGEM> productosFGEM = List.of(
                productoFGEM(1, "Principal", "20000", "13000"),
                productoFGEM(2, "Gancho", "1000", "1300"),
                productoFGEM(3, "Otro", "9000", "6700"));
        DiagnosticoFGEM dFGEM = MotorDiagnosticoFGEM.evaluarFGEM(datosFGEM("21000", "30000", crecientesFGEM(12), productosFGEM));

        HallazgoFGEM perdidaFGEM = hallazgoFGEM(dFGEM, TipoIndicadorFGEM.PRODUCTOS_CON_PERDIDA);
        assertEquals(SemaforoFGEM.AMARILLO, perdidaFGEM.semaforoFGEM(), "3.3 % de las ventas: atención");
        assertTrue(perdidaFGEM.lecturaFGEM().contains("\"Gancho\""));
        HallazgoFGEM concentracionFGEM = hallazgoFGEM(dFGEM, TipoIndicadorFGEM.CONCENTRACION);
        assertEquals(SemaforoFGEM.ROJO, concentracionFGEM.semaforoFGEM(), "66.7 % de las ventas en un producto");
    }

    @Test
    void ventasEnCaidaSostenidaFGEM() {
        double[] serieFGEM = new double[12];
        for (int iFGEM = 0; iFGEM < serieFGEM.length; iFGEM++) {
            serieFGEM[iFGEM] = 14000 - iFGEM * 500;
        }
        DiagnosticoFGEM dFGEM = MotorDiagnosticoFGEM.evaluarFGEM(datosFGEM("21000", "30000", serieFGEM, productosSanosFGEM()));

        assertEquals(SemaforoFGEM.ROJO, hallazgoFGEM(dFGEM, TipoIndicadorFGEM.TENDENCIA_VENTAS).semaforoFGEM());
        assertTrue(hallazgoFGEM(dFGEM, TipoIndicadorFGEM.PROYECCION_VENTAS).semaforoFGEM() != SemaforoFGEM.VERDE);
    }

    @Test
    void conPocaHistoriaAlgunosIndicadoresQuedanSinDatosFGEM() {
        DiagnosticoFGEM dFGEM = MotorDiagnosticoFGEM.evaluarFGEM(datosFGEM("21000", "30000", crecientesFGEM(3), productosSanosFGEM()));

        assertEquals(SemaforoFGEM.SIN_DATOS, hallazgoFGEM(dFGEM, TipoIndicadorFGEM.TENDENCIA_VENTAS).semaforoFGEM());
        assertEquals(SemaforoFGEM.SIN_DATOS, hallazgoFGEM(dFGEM, TipoIndicadorFGEM.REGULARIDAD_VENTAS).semaforoFGEM());
        assertTrue(dFGEM.evaluadoFGEM());
        assertFalse(dFGEM.puntajePorAreaFGEM().isEmpty());
    }

    @Test
    void sinVentasNoHayDiagnosticoFGEM() {
        DatosDiagnosticoFGEM vacioFGEM = new DatosDiagnosticoFGEM(SectorFGEM.COMERCIO, MonedaFGEM.PEN, PERIODO_FGEM, 3, ResumenOperacionesFGEM.VACIO_FGEM,
                ResumenFinancieroFGEM.VACIO_FGEM, ResumenOperacionesFGEM.VACIO_FGEM, List.of(), new double[0], new double[0], List.of());
        DiagnosticoFGEM dFGEM = MotorDiagnosticoFGEM.evaluarFGEM(vacioFGEM);

        assertEquals(NivelSaludFGEM.SIN_DATOS, dFGEM.nivelFGEM());
        assertFalse(dFGEM.evaluadoFGEM());
        assertTrue(dFGEM.resumenFGEM().get(0).startsWith("No hay ventas"));
    }

    @Test
    void puntajePonderadoYNivelesFGEM() {
        List<HallazgoFGEM> hallazgosFGEM = List.of(
                new HallazgoFGEM(TipoIndicadorFGEM.MARGEN_NETO, SemaforoFGEM.VERDE, "", null, "", null),
                new HallazgoFGEM(TipoIndicadorFGEM.CONCENTRACION, SemaforoFGEM.ROJO, "", null, "", "x"),
                HallazgoFGEM.sinDatosFGEM(TipoIndicadorFGEM.TENDENCIA_VENTAS, ""));
        assertEquals(75, MotorDiagnosticoFGEM.puntajeFGEM(hallazgosFGEM), "(3·100 + 1·0) / 4");
        assertEquals(NivelSaludFGEM.SALUDABLE, NivelSaludFGEM.deFGEM(75));
        assertEquals(NivelSaludFGEM.ESTABLE, NivelSaludFGEM.deFGEM(74));
        assertEquals(NivelSaludFGEM.EN_RIESGO, NivelSaludFGEM.deFGEM(35));
        assertEquals(NivelSaludFGEM.CRITICO, NivelSaludFGEM.deFGEM(34));
    }

    // ------------------------------------------------------------------ datos de prueba

    private static DatosDiagnosticoFGEM datosFGEM(String costoVentasFGEM, String ventasFGEM, double[] serieFGEM,
                                          List<RentabilidadProductoFGEM> productosFGEM) {
        ResumenOperacionesFGEM opsFGEM = new ResumenOperacionesFGEM(bdFGEM(ventasFGEM), bdFGEM(costoVentasFGEM), bdFGEM(costoVentasFGEM), 30, 3);
        ResumenOperacionesFGEM anterioresFGEM = new ResumenOperacionesFGEM(bdFGEM("29000"), bdFGEM("20300"), bdFGEM("20300"), 29, 3);
        List<YearMonth> mesesFGEM = new ArrayList<>();
        double[] utilidadFGEM = new double[serieFGEM.length];
        for (int iFGEM = 0; iFGEM < serieFGEM.length; iFGEM++) {
            mesesFGEM.add(YearMonth.of(2025, 12).minusMonths(serieFGEM.length - 1L - iFGEM));
            utilidadFGEM[iFGEM] = serieFGEM[iFGEM] * 0.09;
        }
        return new DatosDiagnosticoFGEM(SectorFGEM.COMERCIO, MonedaFGEM.PEN, PERIODO_FGEM, 3, opsFGEM, finanzasFGEM("4500", "600", "150", "900"),
                anterioresFGEM, mesesFGEM, serieFGEM, utilidadFGEM, productosFGEM);
    }

    private static ResumenFinancieroFGEM finanzasFGEM(String alquilerFGEM, String fletesFGEM, String interesesFGEM, String impuestoFGEM) {
        Map<CategoriaFinancieraFGEM, BigDecimal> categoriasFGEM = new EnumMap<>(CategoriaFinancieraFGEM.class);
        categoriasFGEM.put(CategoriaFinancieraFGEM.ALQUILER, bdFGEM(alquilerFGEM));
        categoriasFGEM.put(CategoriaFinancieraFGEM.TRANSPORTE, bdFGEM(fletesFGEM));
        categoriasFGEM.put(CategoriaFinancieraFGEM.INTERESES_PRESTAMOS, bdFGEM(interesesFGEM));
        categoriasFGEM.put(CategoriaFinancieraFGEM.IMPUESTO_RENTA, bdFGEM(impuestoFGEM));
        BigDecimal fijosFGEM = bdFGEM(alquilerFGEM).add(bdFGEM(interesesFGEM));
        BigDecimal gastosFGEM = fijosFGEM.add(bdFGEM(fletesFGEM)).add(bdFGEM(impuestoFGEM));
        return new ResumenFinancieroFGEM(BigDecimal.ZERO, gastosFGEM, fijosFGEM.add(bdFGEM(impuestoFGEM)), bdFGEM(fletesFGEM), fijosFGEM, bdFGEM(fletesFGEM),
                categoriasFGEM, 4);
    }

    private static double[] crecientesFGEM(int mesesFGEM) {
        double[] serieFGEM = new double[mesesFGEM];
        for (int iFGEM = 0; iFGEM < mesesFGEM; iFGEM++) {
            serieFGEM[iFGEM] = 9000 + iFGEM * 100;
        }
        return serieFGEM;
    }

    private static List<RentabilidadProductoFGEM> productosSanosFGEM() {
        return List.of(productoFGEM(1, "Arroz", "12000", "8400"), productoFGEM(2, "Aceite", "10000", "7000"),
                productoFGEM(3, "Azúcar", "8000", "5600"));
    }

    private static RentabilidadProductoFGEM productoFGEM(long idFGEM, String nombreFGEM, String ventasFGEM, String costoFGEM) {
        return new RentabilidadProductoFGEM(idFGEM, null, nombreFGEM, TipoItemFGEM.PRODUCTO, null, BigDecimal.TEN, bdFGEM(ventasFGEM), bdFGEM(costoFGEM));
    }

    private static HallazgoFGEM hallazgoFGEM(DiagnosticoFGEM dFGEM, TipoIndicadorFGEM tipoFGEM) {
        return dFGEM.hallazgosFGEM().stream().filter(hFGEM -> hFGEM.tipoFGEM() == tipoFGEM).findFirst().orElseThrow();
    }

    private static BigDecimal bdFGEM(String valorFGEM) {
        return new BigDecimal(valorFGEM);
    }
}
