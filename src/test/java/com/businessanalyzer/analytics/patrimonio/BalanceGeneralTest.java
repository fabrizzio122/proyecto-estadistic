package com.businessanalyzer.analytics.patrimonio;

import com.businessanalyzer.analytics.EstadoResultados;
import com.businessanalyzer.model.Activo;
import com.businessanalyzer.model.CategoriaActivo;
import com.businessanalyzer.model.CategoriaDeuda;
import com.businessanalyzer.model.Deuda;
import com.businessanalyzer.model.Moneda;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceGeneralTest {

    private static final LocalDate CORTE = LocalDate.of(2026, 1, 15);

    // ------------------------------------------------------------------ depreciación

    @Test
    void depreciaEnLineaRectaPorMesesCompletos() {
        // 12 000 a 10 años = 100 al mes; 12 meses completos -> 1 200.
        Depreciacion d = Depreciacion.de(activo(CategoriaActivo.MAQUINARIA, "12000", LocalDate.of(2025, 1, 15), 10, "0"), CORTE);
        assertEquals(12, d.mesesTranscurridos());
        igual("1200", d.acumulada());
        igual("100", d.mensual());
        igual("10800", d.valorEnLibros());

        // Un día antes de cumplir el mes 12 solo cuentan 11 meses.
        Depreciacion antes = Depreciacion.de(activo(CategoriaActivo.MAQUINARIA, "12000", LocalDate.of(2025, 1, 16), 10, "0"), CORTE);
        igual("1100", antes.acumulada());
    }

    @Test
    void elValorResidualNoSeDeprecia() {
        // (5 500 − 800) / 60 meses = 78.3333 al mes.
        Depreciacion d = Depreciacion.de(activo(CategoriaActivo.VEHICULOS, "5500", LocalDate.of(2025, 1, 15), 5, "800"), CORTE);
        igual("78.33", d.mensual());
        igual("940", d.acumulada());
    }

    @Test
    void unBienTotalmenteDepreciadoConservaSuValorResidual() {
        Depreciacion d = Depreciacion.de(activo(CategoriaActivo.EQUIPOS_COMPUTO, "3000", LocalDate.of(2019, 6, 1), 4, "200"), CORTE);
        igual("2800", d.acumulada());
        igual("0", d.mensual());
        igual("200", d.valorEnLibros());
    }

    @Test
    void terrenosYActivosSinVidaUtilNoSeDeprecian() {
        Depreciacion terreno = Depreciacion.de(activo(CategoriaActivo.TERRENOS, "80000", LocalDate.of(2010, 1, 1), 20, "0"), CORTE);
        igual("80000", terreno.valorEnLibros());
        Depreciacion sinVida = Depreciacion.de(activo(CategoriaActivo.MAQUINARIA, "5000", LocalDate.of(2010, 1, 1), null, "0"), CORTE);
        igual("5000", sinVida.valorEnLibros());
        assertNull(Depreciacion.avance(activo(CategoriaActivo.TERRENOS, "1", CORTE, null, "0"), CORTE));
    }

    // ------------------------------------------------------------------ balance

    @Test
    void armaElBalanceConCortoYLargoPlazo() {
        List<Activo> activos = List.of(
                activo(CategoriaActivo.CAJA_BANCOS, "5000", CORTE, null, "0"),
                activo(CategoriaActivo.MAQUINARIA, "12000", LocalDate.of(2025, 1, 15), 10, "0"));
        List<Deuda> deudas = List.of(
                deuda(CategoriaDeuda.PROVEEDORES, "2000", "0", null, null, false),
                // Vence en 48 meses: 12/48 del saldo es de corto plazo.
                deuda(CategoriaDeuda.PRESTAMO_BANCARIO, "30000", "6000", "900", CORTE.plusMonths(48), false),
                deuda(CategoriaDeuda.OTRAS, "999", "0", null, null, true),     // anulada
                deuda(CategoriaDeuda.OTRAS, "500", "500", null, null, false)); // cancelada

        BalanceGeneral b = BalanceGeneral.de(CORTE, activos, new BigDecimal("3000"), deudas);

        assertEquals(List.of("Caja y bancos", BalanceGeneral.INVENTARIO),
                b.activoCorriente().stream().map(BalanceGeneral.Partida::concepto).toList());
        igual("8000", b.totalActivoCorriente());
        igual("10800", b.totalActivoNoCorriente());
        igual("18800", b.totalActivo());
        igual("8000", b.totalPasivoCorriente());      // 2 000 + 24 000 × 12/48
        igual("18000", b.totalPasivoNoCorriente());
        igual("26000", b.totalPasivo());
        igual("-7200", b.patrimonio());
        igual("900", b.cuotasMensuales());
        igual("100", b.depreciacionMensual());
        assertEquals(2, b.activosRegistrados());
        assertEquals(2, b.deudasPendientes());
        assertFalse(b.sinRegistros());
    }

    @Test
    void unaDeudaQueVenceDentroDeUnAnoEsTodaDeCortoPlazo() {
        igual("10000", BalanceGeneral.porcionCorriente(
                deuda(CategoriaDeuda.PRESTAMO_BANCARIO, "10000", "0", null, CORTE.plusMonths(12), false), CORTE));
        igual("10000", BalanceGeneral.porcionCorriente(
                deuda(CategoriaDeuda.PRESTAMO_BANCARIO, "10000", "0", null, CORTE.minusDays(3), false), CORTE));
        assertEquals(13, BalanceGeneral.mesesHasta(CORTE, CORTE.plusMonths(12).plusDays(1)));
    }

    @Test
    void ignoraLosActivosDadosDeBajaYLosFuturos() {
        Activo vendido = new Activo(1, 1, CategoriaActivo.VEHICULOS, "Moto", LocalDate.of(2024, 1, 1),
                new BigDecimal("5000"), 5, BigDecimal.ZERO, true, LocalDate.of(2025, 12, 1), "Vendida",
                "admin", null, null, null);
        Activo futuro = activo(CategoriaActivo.CAJA_BANCOS, "100", CORTE.plusDays(1), null, "0");
        BalanceGeneral b = BalanceGeneral.de(CORTE, List.of(vendido, futuro), BigDecimal.ZERO, List.of());
        igual("0", b.totalActivo());
        assertTrue(b.sinRegistros());
    }

    // ------------------------------------------------------------------ indicadores

    @Test
    void calculaLiquidezEndeudamientoYRentabilidad() {
        List<Activo> activos = List.of(
                activo(CategoriaActivo.CAJA_BANCOS, "6000", CORTE, null, "0"),
                activo(CategoriaActivo.EDIFICIOS, "40000", CORTE, 20, "0"));
        List<Deuda> deudas = List.of(deuda(CategoriaDeuda.PRESTAMO_BANCARIO, "4000", "0", "1000", null, false));
        BalanceGeneral b = BalanceGeneral.de(CORTE, activos, new BigDecimal("2000"), deudas);
        // 6 meses con utilidad neta 3 000 y utilidad operativa 4 800.
        EstadoResultados r = resultados("4800", "3000");

        IndicadoresPatrimonio i = IndicadoresPatrimonio.de(b, r, 6);

        igual("2", i.liquidezCorriente());               // 8 000 / 4 000
        igual("1.5", i.pruebaAcida());                   // 6 000 / 4 000
        igual("4000", i.capitalTrabajo());
        igual("0.083333", i.endeudamiento());            // 4 000 / 48 000
        igual("0.090909", i.apalancamiento());           // 4 000 / 44 000
        igual("6000", i.utilidadAnual());
        igual("0.125", i.rentabilidadActivos());         // 6 000 / 48 000
        igual("0.136364", i.rentabilidadPatrimonio());   // 6 000 / 44 000
        igual("800", i.utilidadOperativaMensual());
        igual("0.8", i.coberturaCuotas());               // 800 / 1 000
        assertEquals("2.00", IndicadoresPatrimonio.veces(i.liquidezCorriente()));
    }

    @Test
    void sinDeudasNiVentasLosRatiosQuedanVacios() {
        BalanceGeneral b = BalanceGeneral.de(CORTE, List.of(activo(CategoriaActivo.CAJA_BANCOS, "100", CORTE, null, "0")),
                BigDecimal.ZERO, List.of());
        IndicadoresPatrimonio i = IndicadoresPatrimonio.de(b, null, 0);
        assertNull(i.liquidezCorriente());
        assertNull(i.coberturaCuotas());
        assertNull(i.rentabilidadPatrimonio());
        igual("0", i.endeudamiento());
        assertEquals(0, i.mesesResultados());
    }

    @Test
    void laLecturaAvisaDelPatrimonioNegativoYDeLaFaltaDeCapacidadDePago() {
        BalanceGeneral b = BalanceGeneral.de(CORTE, List.of(activo(CategoriaActivo.CAJA_BANCOS, "1000", CORTE, null, "0")),
                BigDecimal.ZERO, List.of(deuda(CategoriaDeuda.PRESTAMO_BANCARIO, "5000", "0", "600", null, false)));
        SituacionPatrimonial s = new SituacionPatrimonial(b, IndicadoresPatrimonio.de(b, resultados("1500", "900"), 3), null);
        String texto = String.join(" ", LecturaPatrimonio.generar(s, Moneda.PEN));
        assertTrue(texto.contains("patrimonio es negativo"), texto);
        assertTrue(texto.contains("no alcanza para pagar las cuotas"), texto);

        BalanceGeneral vacio = BalanceGeneral.de(CORTE, List.of(), BigDecimal.ZERO, List.of());
        assertTrue(LecturaPatrimonio.generar(new SituacionPatrimonial(vacio, IndicadoresPatrimonio.de(vacio, null, 0), null),
                Moneda.PEN).get(0).startsWith("Aún no hay activos"));
    }

    // ------------------------------------------------------------------ utilidades

    static Activo activo(CategoriaActivo categoria, String valor, LocalDate fecha, Integer vida, String residual) {
        return new Activo(1, 1, categoria, categoria.descripcion(), fecha, new BigDecimal(valor), vida,
                new BigDecimal(residual), false, null, null, "admin", null, null, null);
    }

    static Deuda deuda(CategoriaDeuda categoria, String monto, String pagado, String cuota, LocalDate vencimiento,
                       boolean anulada) {
        return new Deuda(1, 1, categoria, "Acreedor", null, CORTE.minusMonths(1), new BigDecimal(monto),
                new BigDecimal(pagado), null, cuota == null ? null : new BigDecimal(cuota), vencimiento, anulada,
                null, "admin", null, null, null);
    }

    private static EstadoResultados resultados(String operativa, String neta) {
        BigDecimal z = BigDecimal.ZERO;
        return new EstadoResultados(z, z, z, z, new BigDecimal(operativa), z, z, z, z, z, new BigDecimal(neta));
    }

    private static void igual(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }
}
