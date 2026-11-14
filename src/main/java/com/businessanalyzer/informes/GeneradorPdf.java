package com.businessanalyzer.informes;

import com.businessanalyzer.config.VersionApp;
import com.businessanalyzer.exception.InformeException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Genera archivos PDF con JasperReports a partir de la plantilla {@code /informes/informe.jrxml}.
 * La plantilla se compila una sola vez (la primera vez que se usa) y se reutiliza.
 */
public final class GeneradorPdf {

    private static final String PLANTILLA = "/informes/informe.jrxml";
    private static volatile JasperReport plantilla;

    private GeneradorPdf() {
    }

    /** Datos de la portada y del pie de página. */
    public record Encabezado(String empresa, String detalleEmpresa, String titulo, String subtitulo, String generado) {
    }

    public static void generar(Encabezado encabezado, ContenidoInforme contenido, Path destino) {
        try {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("EMPRESA", encabezado.empresa());
            parametros.put("DETALLE_EMPRESA", encabezado.detalleEmpresa());
            parametros.put("TITULO", encabezado.titulo());
            parametros.put("SUBTITULO", encabezado.subtitulo());
            parametros.put("GENERADO", encabezado.generado());
            parametros.put("APLICACION", VersionApp.PRODUCTO);
            JasperPrint impresion = JasperFillManager.fillReport(plantilla(), parametros,
                    new JRMapCollectionDataSource(contenido.bloques()));

            SimplePdfExporterConfiguration configuracion = new SimplePdfExporterConfiguration();
            configuracion.setMetadataTitle(encabezado.titulo() + " - " + encabezado.empresa());
            configuracion.setMetadataAuthor("Business Analyzer Desktop");
            configuracion.setMetadataCreator(VersionApp.PRODUCTO + " (" + VersionApp.NUMERO + ")");
            configuracion.setMetadataSubject(encabezado.subtitulo());

            Path temporal = destino.resolveSibling(destino.getFileName() + ".tmp");
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                JRPdfExporter exportador = new JRPdfExporter();
                exportador.setExporterInput(new SimpleExporterInput(impresion));
                exportador.setExporterOutput(new SimpleOutputStreamExporterOutput(salida));
                exportador.setConfiguration(configuracion);
                exportador.exportReport();
            }
            Escritura.reemplazar(temporal, destino);
        } catch (JRException | IOException e) {
            Escritura.borrar(destino.resolveSibling(destino.getFileName() + ".tmp"));
            throw new InformeException("No se pudo generar el PDF. El detalle quedó registrado en el log.", e);
        }
    }

    private static JasperReport plantilla() throws JRException, IOException {
        JasperReport compilada = plantilla;
        if (compilada == null) {
            synchronized (GeneradorPdf.class) {
                compilada = plantilla;
                if (compilada == null) {
                    try (InputStream jrxml = GeneradorPdf.class.getResourceAsStream(PLANTILLA)) {
                        if (jrxml == null) {
                            throw new IOException("No se encontró la plantilla " + PLANTILLA);
                        }
                        compilada = JasperCompileManager.compileReport(jrxml);
                    }
                    plantilla = compilada;
                }
            }
        }
        return compilada;
    }
}
