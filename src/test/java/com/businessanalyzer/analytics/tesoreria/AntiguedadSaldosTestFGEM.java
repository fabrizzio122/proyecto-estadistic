package com.businessanalyzer.analytics.tesoreria;

import com.businessanalyzer.analytics.tesoreria.AntiguedadSaldosFGEM.ResultadoFGEM;
import com.businessanalyzer.analytics.tesoreria.AntiguedadSaldosFGEM.SaldoContraparteFGEM;
import com.businessanalyzer.analytics.tesoreria.AntiguedadSaldosFGEM.SaldoTramoFGEM;
import com.businessanalyzer.analytics.tesoreria.AntiguedadSaldosFGEM.TramoFGEM;
import com.businessanalyzer.model.CondicionPagoFGEM;
import com.businessanalyzer.model.EstadoOperacionFGEM;
import com.businessanalyzer.model.OperacionFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AntiguedadSaldosTestFGEM {

    private static final LocalDate HOY_FGEM = LocalDate.of(2026, 11, 24);

    @Test
    void reparteLoPendienteEnTramosSegunLosDiasDeAtrasoFGEM() {
        ResultadoFGEM rFGEM = AntiguedadSaldosFGEM.calcularFGEM(List.of(
                ventaFGEM(1, "Juana", "100", "0", HOY_FGEM.plusDays(5)),          // al día
                ventaFGEM(2, "Pedro", "50", "0", null),                            // sin fecha límite: al día
                ventaFGEM(3, "Juana", "200", "50", HOY_FGEM.minusDays(30)),        // 150 vencido, 30 días
                ventaFGEM(4, "Luis", "80", "0", HOY_FGEM.minusDays(31)),           // 31 días
                ventaFGEM(5, " juana  ", "70", "0", HOY_FGEM.minusDays(90)),       // la misma Juana, 90 días
                ventaFGEM(6, "Rosa", "40", "40", HOY_FGEM.minusDays(100))),        // pagada: no cuenta
                HOY_FGEM);

        assertEquals(0, new BigDecimal("450").compareTo(rFGEM.totalFGEM()));
        assertEquals(0, new BigDecimal("300").compareTo(rFGEM.vencidoFGEM()));
        assertEquals(List.of(TramoFGEM.AL_DIA, TramoFGEM.VENCIDO_30, TramoFGEM.VENCIDO_60, TramoFGEM.VENCIDO_MAS_60),
                rFGEM.tramosFGEM().stream().map(SaldoTramoFGEM::tramoFGEM).toList());
        assertEquals(List.of("150", "150", "80", "70"),
                rFGEM.tramosFGEM().stream().map(tFGEM -> tFGEM.montoFGEM().stripTrailingZeros().toPlainString()).toList());
        assertEquals(List.of(2, 1, 1, 1), rFGEM.tramosFGEM().stream().map(SaldoTramoFGEM::operacionesFGEM).toList());
        assertEquals(0, new BigDecimal("0.3333").compareTo(rFGEM.tramosFGEM().get(0).porcentajeFGEM()));
        assertEquals(0, new BigDecimal("0.6667").compareTo(rFGEM.porcentajeVencidoFGEM()));

        // Juana (sin importar mayúsculas ni espacios) debe más dinero vencido: va primero.
        SaldoContraparteFGEM juanaFGEM = rFGEM.contrapartesFGEM().get(0);
        assertEquals("Juana", juanaFGEM.contraparteFGEM());
        assertEquals(3, juanaFGEM.operacionesFGEM());
        assertEquals(0, new BigDecimal("320").compareTo(juanaFGEM.pendienteFGEM()));
        assertEquals(0, new BigDecimal("220").compareTo(juanaFGEM.vencidoFGEM()));
        assertEquals(90, juanaFGEM.mayorAtrasoFGEM());
        assertEquals(TramoFGEM.VENCIDO_MAS_60, juanaFGEM.tramoFGEM());
        assertEquals(List.of("Juana", "Luis", "Pedro"), rFGEM.contrapartesFGEM().stream().map(SaldoContraparteFGEM::contraparteFGEM).toList());
    }

    @Test
    void sinPendientesTodoEnCeroYSinNombreSeAgrupaFGEM() {
        ResultadoFGEM vacioFGEM = AntiguedadSaldosFGEM.calcularFGEM(List.of(), HOY_FGEM);
        assertEquals(4, vacioFGEM.tramosFGEM().size());
        assertEquals(0, vacioFGEM.porcentajeVencidoFGEM().signum());

        ResultadoFGEM rFGEM = AntiguedadSaldosFGEM.calcularFGEM(List.of(ventaFGEM(1, null, "10", "0", null), ventaFGEM(2, " ", "5", "0", null)),
                HOY_FGEM);
        assertEquals(1, rFGEM.contrapartesFGEM().size());
        assertEquals(AntiguedadSaldosFGEM.SIN_NOMBRE_FGEM, rFGEM.contrapartesFGEM().get(0).contraparteFGEM());
    }

    @Test
    void tramoPorDiasFGEM() {
        assertEquals(TramoFGEM.AL_DIA, TramoFGEM.deFGEM(0));
        assertEquals(TramoFGEM.VENCIDO_30, TramoFGEM.deFGEM(1));
        assertEquals(TramoFGEM.VENCIDO_30, TramoFGEM.deFGEM(30));
        assertEquals(TramoFGEM.VENCIDO_60, TramoFGEM.deFGEM(60));
        assertEquals(TramoFGEM.VENCIDO_MAS_60, TramoFGEM.deFGEM(61));
    }

    private static OperacionFGEM ventaFGEM(long idFGEM, String clienteFGEM, String totalFGEM, String pagadoFGEM, LocalDate venceFGEM) {
        BigDecimal tFGEM = new BigDecimal(totalFGEM);
        return new OperacionFGEM(idFGEM, 1, TipoOperacionFGEM.VENTA, "V-" + idFGEM, HOY_FGEM.minusDays(120), clienteFGEM, null, null,
                BigDecimal.ZERO, tFGEM, BigDecimal.ZERO, tFGEM, BigDecimal.ZERO, EstadoOperacionFGEM.REGISTRADA, null, "admin", null,
                null, null, null, 1, List.of(), CondicionPagoFGEM.CREDITO, null, null, venceFGEM, new BigDecimal(pagadoFGEM));
    }
}
