package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.ganaderia.ExistenciaValorizada;
import com.businessanalyzer.analytics.ganaderia.IndicadoresEspecie;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneral;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Especie;
import com.businessanalyzer.model.Sector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatosDemoAgroServiceTest {

    @TempDir
    Path carpeta;

    @Test
    void creaUnProductorConGanadoProduccionYBalance() {
        EntornoPrueba entorno = new EntornoPrueba(carpeta);
        entorno.administradorConectado();
        DatosDemoAgroService demo = new DatosDemoAgroService(entorno.empresaService, entorno.ganaderia, entorno.agricultura,
                entorno.patrimonio, entorno.sesion, entorno.reloj);

        Empresa fundo = demo.crearFundoDemo();

        assertEquals(Sector.AGROPECUARIO, fundo.sector());
        assertEquals(fundo.id(), entorno.sesion.empresaActiva().orElseThrow().id());
        List<ExistenciaValorizada> hato = entorno.ganaderia.hato();
        assertTrue(ExistenciaValorizada.total(hato).signum() > 0);

        LocalDate hoy = LocalDate.now(entorno.reloj);
        List<IndicadoresEspecie> indicadores = entorno.ganaderia.indicadores(new Periodo(hoy.minusYears(1).plusDays(1), hoy));
        assertEquals(List.of(Especie.VACUNO, Especie.OVINO, Especie.ALPACA),
                indicadores.stream().map(IndicadoresEspecie::especie).toList());
        IndicadoresEspecie vacunos = indicadores.get(0);
        assertTrue(vacunos.natalidad().signum() > 0);
        assertEquals(1, vacunos.produccion().size());

        BalanceGeneral balance = entorno.patrimonio.situacion().balance();
        assertTrue(balance.ganado().signum() > 0);
        assertTrue(balance.patrimonio().signum() > 0);
        assertTrue(balance.cultivos().signum() > 0);    // la papa recién sembrada

        var campanias = entorno.agricultura.listarCampanias(false);
        assertEquals(5, campanias.size());
        assertEquals(3, entorno.agricultura.listarParcelas(false).size());
        assertEquals(1, campanias.stream().filter(r -> r.campania().estado()
                == com.businessanalyzer.model.EstadoCampania.PERDIDA).count());
        assertTrue(campanias.stream().filter(r -> r.campania().estado()
                == com.businessanalyzer.model.EstadoCampania.COSECHADA).allMatch(r -> r.rendimiento().signum() > 0));
        assertThrows(ValidacionException.class, demo::crearFundoDemo);
    }
}
