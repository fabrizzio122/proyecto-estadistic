package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonialFGEM;
import com.businessanalyzer.analytics.valoracion.SupuestosValoracionFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.TipoInformeFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.repository.InformeRepositoryFGEM;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValoracionServiceTestFGEM {

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private ValoracionServiceFGEM servicioFGEM;
    private EmpresaFGEM bodegaFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = entornoFGEM.valoracionFGEM;
        entornoFGEM.administradorConectadoFGEM();
        bodegaFGEM = new DatosDemoServiceFGEM(entornoFGEM.empresaServiceFGEM, entornoFGEM.catalogoFGEM, entornoFGEM.operacionesFGEM, entornoFGEM.finanzasFGEM,
                entornoFGEM.patrimonioFGEM, entornoFGEM.tesoreriaFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM).crearEmpresaDemoFGEM();
    }

    @Test
    void valoraConElBalanceYLaUtilidadDelNegocioFGEM() {
        SupuestosValoracionFGEM sFGEM = servicioFGEM.supuestosFGEM();
        assertEquals(0, SupuestosValoracionFGEM.TASA_PREDETERMINADA_FGEM.compareTo(sFGEM.tasaDescuentoFGEM()));
        assertTrue(sFGEM.crecimientoFGEM().compareTo(ValoracionServiceFGEM.CRECIMIENTO_MINIMO_FGEM) >= 0
                && sFGEM.crecimientoFGEM().compareTo(ValoracionServiceFGEM.CRECIMIENTO_MAXIMO_FGEM) <= 0);

        ValoracionServiceFGEM.ResultadoFGEM rFGEM = servicioFGEM.valorarFGEM(sFGEM);
        SituacionPatrimonialFGEM situacionFGEM = entornoFGEM.patrimonioFGEM.situacionFGEM();
        assertEquals(0, situacionFGEM.balanceFGEM().patrimonioFGEM().compareTo(rFGEM.valoracionFGEM().patrimonialFGEM()));
        assertEquals(0, situacionFGEM.indicadoresFGEM().utilidadAnualFGEM().compareTo(rFGEM.valoracionFGEM().utilidadAnualFGEM()));
        assertNotNull(rFGEM.valoracionFGEM().flujosDescontadosFGEM(), "La bodega demo es rentable");
        assertEquals(12, rFGEM.valoracionFGEM().mesesResultadosFGEM());
    }

    @Test
    void guardaLosSupuestosPorEmpresaFGEM() {
        servicioFGEM.guardarSupuestosFGEM(new BigDecimal("0.25"), new BigDecimal("0.03"));
        SupuestosValoracionFGEM sFGEM = servicioFGEM.supuestosFGEM();
        assertEquals(0, new BigDecimal("0.25").compareTo(sFGEM.tasaDescuentoFGEM()));
        assertEquals(0, new BigDecimal("0.03").compareTo(sFGEM.crecimientoFGEM()));

        entornoFGEM.crearEmpresaActivaFGEM("Otra");
        assertEquals(0, SupuestosValoracionFGEM.TASA_PREDETERMINADA_FGEM.compareTo(servicioFGEM.supuestosFGEM().tasaDescuentoFGEM()));
        assertThrows(RuntimeException.class, () -> servicioFGEM.guardarSupuestosFGEM(new BigDecimal("0.90"), BigDecimal.ZERO));
    }

    @Test
    void exportaElInformeYLoRegistraFGEM() throws Exception {
        Path pdfFGEM = servicioFGEM.exportarFGEM(servicioFGEM.supuestosFGEM(), carpetaFGEM.resolve("valoracion"));

        byte[] bytesFGEM = Files.readAllBytes(pdfFGEM);
        assertEquals("%PDF", new String(bytesFGEM, 0, 4));
        PdfReader lectorFGEM = new PdfReader(bytesFGEM);
        String textoFGEM = new PdfTextExtractor(lectorFGEM).getTextFromPage(1);
        lectorFGEM.close();
        assertTrue(textoFGEM.contains("Valoración del negocio"), textoFGEM);
        assertTrue(textoFGEM.contains("Valor patrimonial"), textoFGEM);
        assertTrue(entornoFGEM.databaseFGEM.conConexionFGEM(cFGEM -> new InformeRepositoryFGEM().listarFGEM(cFGEM, bodegaFGEM.idFGEM(), 10)).stream()
                .anyMatch(iFGEM -> iFGEM.tipoFGEM() == TipoInformeFGEM.VALORACION));
    }

    @Test
    void elOperadorNoVeLaValoracionFGEM() {
        UsuarioFGEM operadorFGEM = entornoFGEM.crearUsuarioFGEM("operador", "OPERADOR");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(bodegaFGEM.idFGEM(), Set.of(operadorFGEM.idFGEM()));
        entornoFGEM.entrarComoFGEM("operador", "Temporal123");
        entornoFGEM.empresaServiceFGEM.seleccionarEmpresaActivaFGEM(bodegaFGEM.idFGEM());
        assertThrows(AccesoDenegadoExceptionFGEM.class, servicioFGEM::supuestosFGEM);
    }
}
