package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.TipoInformeFGEM;
import com.businessanalyzer.model.VisitaFGEM;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InformeTecnicoServiceTestFGEM {

    @TempDir
    Path carpetaFGEM;

    @Test
    void generaElInformeTecnicoDeLaVisitaMasRecienteFGEM() throws Exception {
        EntornoPruebaFGEM entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        entornoFGEM.administradorConectadoFGEM();
        EmpresaFGEM fundoFGEM = new DatosDemoAgroServiceFGEM(entornoFGEM.empresaServiceFGEM, entornoFGEM.ganaderiaFGEM, entornoFGEM.agriculturaFGEM,
                entornoFGEM.patrimonioFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM, entornoFGEM.visitasFGEM, entornoFGEM.finanzasFGEM, entornoFGEM.tesoreriaFGEM).crearFundoDemoFGEM();
        VisitaFGEM recienteFGEM = entornoFGEM.visitasFGEM.listarFGEM(false).get(0);
        Path fotoFGEM = carpetaFGEM.resolve("foto.png");
        ImageIO.write(new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB), "png", fotoFGEM.toFile());
        entornoFGEM.visitasFGEM.agregarFotoFGEM(recienteFGEM.idFGEM(), fotoFGEM, "Cobertizo de crías");

        String contenidoFGEM = entornoFGEM.informeTecnicoFGEM.redactarFGEM(fundoFGEM, recienteFGEM).textoPlanoFGEM();
        assertTrue(contenidoFGEM.contains("Verificación en campo"), contenidoFGEM);
        assertTrue(contenidoFGEM.contains("Dosificar a todas las alpacas contra parásitos"), contenidoFGEM);
        assertTrue(contenidoFGEM.contains("Terminar el cobertizo de crías"), contenidoFGEM);
        assertTrue(contenidoFGEM.contains("Natalidad"), contenidoFGEM);
        assertTrue(contenidoFGEM.contains("Quinua"), contenidoFGEM);
        assertTrue(contenidoFGEM.contains("Patrimonio"), contenidoFGEM);
        assertTrue(contenidoFGEM.contains("Registro fotográfico"), contenidoFGEM);

        Path pdfFGEM = entornoFGEM.informeTecnicoFGEM.generarFGEM(recienteFGEM.idFGEM(), carpetaFGEM.resolve("informe"));
        assertTrue(pdfFGEM.getFileName().toString().endsWith(".pdf"));
        byte[] bytesFGEM = Files.readAllBytes(pdfFGEM);
        assertEquals("%PDF", new String(bytesFGEM, 0, 4));
        PdfReader lectorFGEM = new PdfReader(bytesFGEM);
        String textoFGEM = new PdfTextExtractor(lectorFGEM).getTextFromPage(1);
        lectorFGEM.close();
        assertTrue(textoFGEM.contains("Informe técnico de visita"), textoFGEM);
        assertTrue(textoFGEM.contains("Ing. Rosa Quispe"), textoFGEM);

        assertTrue(entornoFGEM.databaseFGEM.conConexionFGEM(cFGEM -> new com.businessanalyzer.repository.InformeRepositoryFGEM()
                .listarFGEM(cFGEM, fundoFGEM.idFGEM(), 10)).stream().anyMatch(iFGEM -> iFGEM.tipoFGEM() == TipoInformeFGEM.INFORME_TECNICO));
        assertTrue(InformeTecnicoServiceFGEM.nombreSugeridoFGEM(fundoFGEM, recienteFGEM).startsWith("Informe-tecnico-Fundo-Demo-"));
    }

    @Test
    void noSeGeneraDesdeElModuloDeInformesFGEM() {
        EntornoPruebaFGEM entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        entornoFGEM.administradorConectadoFGEM();
        entornoFGEM.crearEmpresaActivaFGEM("Bodega");
        InformeServiceFGEM informesFGEM = new InformeServiceFGEM(entornoFGEM.databaseFGEM, new com.businessanalyzer.repository.InformeRepositoryFGEM(),
                null, null, entornoFGEM.operacionesFGEM, entornoFGEM.finanzasFGEM, entornoFGEM.empresaServiceFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM);
        assertThrows(ValidacionExceptionFGEM.class, () -> informesFGEM.generarFGEM(TipoInformeFGEM.INFORME_TECNICO,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15), carpetaFGEM.resolve("x.pdf")));
    }
}
