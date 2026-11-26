package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonialFGEM;
import com.businessanalyzer.analytics.simulacion.BaseSimulacionFGEM;
import com.businessanalyzer.analytics.tesoreria.SaldoCuentaFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.repository.EscenarioRepositoryFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatosDemoServiceTestFGEM {

    @TempDir
    Path carpetaFGEM;

    @Test
    void creaUnaEmpresaActivaConHistoriaYNoSeDuplicaFGEM() {
        EntornoPruebaFGEM entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        entornoFGEM.administradorConectadoFGEM();
        DatosDemoServiceFGEM demoFGEM = new DatosDemoServiceFGEM(entornoFGEM.empresaServiceFGEM, entornoFGEM.catalogoFGEM, entornoFGEM.operacionesFGEM,
                entornoFGEM.finanzasFGEM, entornoFGEM.patrimonioFGEM, entornoFGEM.tesoreriaFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM);

        EmpresaFGEM empresaFGEM = demoFGEM.crearEmpresaDemoFGEM();

        assertEquals(empresaFGEM.idFGEM(), entornoFGEM.sesionFGEM.empresaActivaFGEM().orElseThrow().idFGEM());
        SimulacionServiceFGEM simulacionFGEM = new SimulacionServiceFGEM(entornoFGEM.databaseFGEM, new OperacionRepositoryFGEM(),
                new MovimientoRepositoryFGEM(), new EscenarioRepositoryFGEM(), entornoFGEM.empresaServiceFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM);
        BaseSimulacionFGEM baseFGEM = simulacionFGEM.baseFGEM(SimulacionServiceFGEM.TipoBaseFGEM.PROMEDIO_6_MESES);
        assertTrue(baseFGEM.ventasFGEM().signum() > 0);
        assertTrue(baseFGEM.utilidadAntesImpuestosFGEM().signum() > 0, "La demo debe ser rentable: " + baseFGEM.utilidadAntesImpuestosFGEM());
        assertTrue(baseFGEM.tasaImpuestoEfectivaFGEM().signum() > 0);

        SituacionPatrimonialFGEM patrimonioFGEM = entornoFGEM.patrimonioFGEM.situacionFGEM();
        assertEquals(4, patrimonioFGEM.balanceFGEM().activosRegistradosFGEM());
        assertEquals(2, patrimonioFGEM.balanceFGEM().deudasPendientesFGEM());
        assertTrue(patrimonioFGEM.balanceFGEM().inventarioFGEM().signum() > 0);
        assertTrue(patrimonioFGEM.balanceFGEM().patrimonioFGEM().signum() > 0);
        assertTrue(patrimonioFGEM.indicadoresFGEM().liquidezCorrienteFGEM().compareTo(BigDecimal.ONE) > 0);
        assertTrue(patrimonioFGEM.indicadoresFGEM().coberturaCuotasFGEM().compareTo(BigDecimal.ONE) > 0);

        List<SaldoCuentaFGEM> saldosFGEM = entornoFGEM.tesoreriaFGEM.listarCuentasFGEM(false);
        assertEquals(3, saldosFGEM.size());
        assertTrue(saldosFGEM.stream().noneMatch(SaldoCuentaFGEM::negativoFGEM), "Ninguna cuenta debe quedar en negativo");
        assertEquals(0, SaldoCuentaFGEM.totalFGEM(saldosFGEM).compareTo(patrimonioFGEM.balanceFGEM().efectivoFGEM()));
        assertTrue(entornoFGEM.tesoreriaFGEM.evolucionFGEM(13).stream().allMatch(mFGEM -> mFGEM.saldoCierreFGEM().signum() > 0));
        TesoreriaServiceFGEM.ResumenFGEM resumenFGEM = entornoFGEM.tesoreriaFGEM.resumenFGEM();
        assertTrue(resumenFGEM.porCobrarFGEM().signum() > 0);
        assertTrue(resumenFGEM.porCobrarVencidoFGEM().signum() > 0, "La venta al restaurante está vencida");
        assertTrue(resumenFGEM.porPagarFGEM().signum() > 0);
        assertEquals(0, resumenFGEM.porPagarVencidoFGEM().signum());
        assertTrue(entornoFGEM.tesoreriaFGEM.proyeccionFGEM().suficienteFGEM());
        assertThrows(ValidacionExceptionFGEM.class, demoFGEM::crearEmpresaDemoFGEM);
    }
}
