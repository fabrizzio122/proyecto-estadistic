package com.businessanalyzer.informes;

import com.businessanalyzer.config.VersionAppFGEM;
import com.businessanalyzer.exception.InformeExceptionFGEM;
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

public final class GeneradorPdfFGEM {

    private static final String PLANTILLA_FGEM = "/informes/informe.jrxml";
    private static volatile JasperReport plantillaFGEM;

    private GeneradorPdfFGEM() {
    }

    public record EncabezadoFGEM(String empresaFGEM, String detalleEmpresaFGEM, String tituloFGEM, String subtituloFGEM, String generadoFGEM) {
    }

    public static void generarFGEM(EncabezadoFGEM encabezadoFGEM, ContenidoInformeFGEM contenidoFGEM, Path destinoFGEM) {
        try {
            Map<String, Object> parametrosFGEM = new HashMap<>();
            parametrosFGEM.put("EMPRESA", encabezadoFGEM.empresaFGEM());
            parametrosFGEM.put("DETALLE_EMPRESA", encabezadoFGEM.detalleEmpresaFGEM());
            parametrosFGEM.put("TITULO", encabezadoFGEM.tituloFGEM());
            parametrosFGEM.put("SUBTITULO", encabezadoFGEM.subtituloFGEM());
            parametrosFGEM.put("GENERADO", encabezadoFGEM.generadoFGEM());
            parametrosFGEM.put("APLICACION", VersionAppFGEM.PRODUCTO_FGEM);
            JasperPrint impresionFGEM = JasperFillManager.fillReport(plantillaFGEM(), parametrosFGEM,
                    new JRMapCollectionDataSource(contenidoFGEM.bloquesFGEM()));

            SimplePdfExporterConfiguration configuracionFGEM = new SimplePdfExporterConfiguration();
            configuracionFGEM.setMetadataTitle(encabezadoFGEM.tituloFGEM() + " - " + encabezadoFGEM.empresaFGEM());
            configuracionFGEM.setMetadataAuthor("Business Analyzer Desktop");
            configuracionFGEM.setMetadataCreator(VersionAppFGEM.PRODUCTO_FGEM + " (" + VersionAppFGEM.NUMERO_FGEM + ")");
            configuracionFGEM.setMetadataSubject(encabezadoFGEM.subtituloFGEM());

            Path temporalFGEM = destinoFGEM.resolveSibling(destinoFGEM.getFileName() + ".tmp");
            try (OutputStream salidaFGEM = Files.newOutputStream(temporalFGEM)) {
                JRPdfExporter exportadorFGEM = new JRPdfExporter();
                exportadorFGEM.setExporterInput(new SimpleExporterInput(impresionFGEM));
                exportadorFGEM.setExporterOutput(new SimpleOutputStreamExporterOutput(salidaFGEM));
                exportadorFGEM.setConfiguration(configuracionFGEM);
                exportadorFGEM.exportReport();
            }
            EscrituraFGEM.reemplazarFGEM(temporalFGEM, destinoFGEM);
        } catch (JRException | IOException eFGEM) {
            EscrituraFGEM.borrarFGEM(destinoFGEM.resolveSibling(destinoFGEM.getFileName() + ".tmp"));
            throw new InformeExceptionFGEM("No se pudo generar el PDF. El detalle quedó registrado en el log.", eFGEM);
        }
    }

    private static JasperReport plantillaFGEM() throws JRException, IOException {
        JasperReport compiladaFGEM = plantillaFGEM;
        if (compiladaFGEM == null) {
            synchronized (GeneradorPdfFGEM.class) {
                compiladaFGEM = plantillaFGEM;
                if (compiladaFGEM == null) {
                    try (InputStream jrxmlFGEM = GeneradorPdfFGEM.class.getResourceAsStream(PLANTILLA_FGEM)) {
                        if (jrxmlFGEM == null) {
                            throw new IOException("No se encontró la plantilla " + PLANTILLA_FGEM);
                        }
                        compiladaFGEM = JasperCompileManager.compileReport(jrxmlFGEM);
                    }
                    plantillaFGEM = compiladaFGEM;
                }
            }
        }
        return compiladaFGEM;
    }
}
