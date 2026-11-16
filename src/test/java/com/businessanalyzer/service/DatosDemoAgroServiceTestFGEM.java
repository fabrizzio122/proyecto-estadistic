package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.ganaderia.ExistenciaValorizadaFGEM;
import com.businessanalyzer.analytics.ganaderia.IndicadoresEspecieFGEM;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneralFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EspecieFGEM;
import com.businessanalyzer.model.SectorFGEM;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatosDemoAgroServiceTestFGEM {

    @TempDir
    Path carpetaFGEM;

    @Test
    void creaUnProductorConGanadoProduccionYBalanceFGEM() {
        EntornoPruebaFGEM entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        entornoFGEM.administradorConectadoFGEM();
        DatosDemoAgroServiceFGEM demoFGEM = new DatosDemoAgroServiceFGEM(entornoFGEM.empresaServiceFGEM, entornoFGEM.ganaderiaFGEM, entornoFGEM.agriculturaFGEM,
                entornoFGEM.patrimonioFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM, entornoFGEM.visitasFGEM, entornoFGEM.finanzasFGEM, entornoFGEM.tesoreriaFGEM);

        EmpresaFGEM fundoFGEM = demoFGEM.crearFundoDemoFGEM();

        assertEquals(SectorFGEM.AGROPECUARIO, fundoFGEM.sectorFGEM());
        assertEquals(fundoFGEM.idFGEM(), entornoFGEM.sesionFGEM.empresaActivaFGEM().orElseThrow().idFGEM());
        List<ExistenciaValorizadaFGEM> hatoFGEM = entornoFGEM.ganaderiaFGEM.hatoFGEM();
        assertTrue(ExistenciaValorizadaFGEM.totalFGEM(hatoFGEM).signum() > 0);

        LocalDate hoyFGEM = LocalDate.now(entornoFGEM.relojFGEM);
        List<IndicadoresEspecieFGEM> indicadoresFGEM = entornoFGEM.ganaderiaFGEM.indicadoresFGEM(new PeriodoFGEM(hoyFGEM.minusYears(1).plusDays(1), hoyFGEM));
        assertEquals(List.of(EspecieFGEM.VACUNO, EspecieFGEM.OVINO, EspecieFGEM.ALPACA),
                indicadoresFGEM.stream().map(IndicadoresEspecieFGEM::especieFGEM).toList());
        IndicadoresEspecieFGEM vacunosFGEM = indicadoresFGEM.get(0);
        assertTrue(vacunosFGEM.natalidadFGEM().signum() > 0);
        assertEquals(1, vacunosFGEM.produccionFGEM().size());

        BalanceGeneralFGEM balanceFGEM = entornoFGEM.patrimonioFGEM.situacionFGEM().balanceFGEM();
        assertTrue(balanceFGEM.ganadoFGEM().signum() > 0);
        assertTrue(balanceFGEM.patrimonioFGEM().signum() > 0);
        assertTrue(balanceFGEM.cultivosFGEM().signum() > 0);    // la papa recién sembrada

        var campaniasFGEM = entornoFGEM.agriculturaFGEM.listarCampaniasFGEM(false);
        assertEquals(5, campaniasFGEM.size());
        assertEquals(3, entornoFGEM.agriculturaFGEM.listarParcelasFGEM(false).size());
        assertEquals(1, campaniasFGEM.stream().filter(rFGEM -> rFGEM.campaniaFGEM().estadoFGEM()
                == com.businessanalyzer.model.EstadoCampaniaFGEM.PERDIDA).count());
        assertTrue(campaniasFGEM.stream().filter(rFGEM -> rFGEM.campaniaFGEM().estadoFGEM()
                == com.businessanalyzer.model.EstadoCampaniaFGEM.COSECHADA).allMatch(rFGEM -> rFGEM.rendimientoFGEM().signum() > 0));
        assertThrows(ValidacionExceptionFGEM.class, demoFGEM::crearFundoDemoFGEM);
    }
}
