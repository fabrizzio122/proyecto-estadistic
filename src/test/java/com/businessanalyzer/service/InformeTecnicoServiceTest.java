package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.TipoInforme;
import com.businessanalyzer.model.Visita;
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

class InformeTecnicoServiceTest {

    @TempDir
    Path carpeta;

    @Test
    void generaElInformeTecnicoDeLaVisitaMasReciente() throws Exception {
        EntornoPrueba entorno = new EntornoPrueba(carpeta);
        entorno.administradorConectado();
        Empresa fundo = new DatosDemoAgroService(entorno.empresaService, entorno.ganaderia, entorno.agricultura,
                entorno.patrimonio, entorno.sesion, entorno.reloj, entorno.visitas, entorno.finanzas, entorno.tesoreria).crearFundoDemo();
        Visita reciente = entorno.visitas.listar(false).get(0);
        Path foto = carpeta.resolve("foto.png");
        ImageIO.write(new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB), "png", foto.toFile());
        entorno.visitas.agregarFoto(reciente.id(), foto, "Cobertizo de crías");

        String contenido = entorno.informeTecnico.redactar(fundo, reciente).textoPlano();
        assertTrue(contenido.contains("Verificación en campo"), contenido);
        assertTrue(contenido.contains("Dosificar a todas las alpacas contra parásitos"), contenido);   // verificado
        assertTrue(contenido.contains("Terminar el cobertizo de crías"), contenido);                  // nuevo
        assertTrue(contenido.contains("Natalidad"), contenido);
        assertTrue(contenido.contains("Quinua"), contenido);
        assertTrue(contenido.contains("Patrimonio"), contenido);
        assertTrue(contenido.contains("Registro fotográfico"), contenido);

        Path pdf = entorno.informeTecnico.generar(reciente.id(), carpeta.resolve("informe"));
        assertTrue(pdf.getFileName().toString().endsWith(".pdf"));
        byte[] bytes = Files.readAllBytes(pdf);
        assertEquals("%PDF", new String(bytes, 0, 4));
        PdfReader lector = new PdfReader(bytes);
        String texto = new PdfTextExtractor(lector).getTextFromPage(1);
        lector.close();
        assertTrue(texto.contains("Informe técnico de visita"), texto);
        assertTrue(texto.contains("Ing. Rosa Quispe"), texto);

        assertTrue(entorno.database.conConexion(c -> new com.businessanalyzer.repository.InformeRepository()
                .listar(c, fundo.id(), 10)).stream().anyMatch(i -> i.tipo() == TipoInforme.INFORME_TECNICO));
        assertTrue(InformeTecnicoService.nombreSugerido(fundo, reciente).startsWith("Informe-tecnico-Fundo-Demo-"));
    }

    @Test
    void noSeGeneraDesdeElModuloDeInformes() {
        EntornoPrueba entorno = new EntornoPrueba(carpeta);
        entorno.administradorConectado();
        entorno.crearEmpresaActiva("Bodega");
        InformeService informes = new InformeService(entorno.database, new com.businessanalyzer.repository.InformeRepository(),
                null, null, entorno.operaciones, entorno.finanzas, entorno.empresaService, entorno.sesion, entorno.reloj);
        assertThrows(ValidacionException.class, () -> informes.generar(TipoInforme.INFORME_TECNICO,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15), carpeta.resolve("x.pdf")));
    }
}
