package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.InformeGeneradoFGEM;
import com.businessanalyzer.model.TipoInformeFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.repository.InformeRepositoryFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Genera los informes reales (PDF y Excel) con los datos de la empresa de demostración. */
class InformeServiceTestFGEM {

    private static final LocalDate DESDE_FGEM = LocalDate.of(2025, 10, 1);
    private static final LocalDate HASTA_FGEM = LocalDate.of(2025, 12, 31);

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private InformeServiceFGEM servicioFGEM;
    private Path salidaFGEM;

    @BeforeEach
    void prepararFGEM() throws Exception {
        entornoFGEM = new EntornoPruebaFGEM(Files.createDirectories(carpetaFGEM.resolve("bd")));
        salidaFGEM = Files.createDirectories(carpetaFGEM.resolve("informes"));
        AnalisisServiceFGEM analisisFGEM = new AnalisisServiceFGEM(entornoFGEM.databaseFGEM, new OperacionRepositoryFGEM(),
                new MovimientoRepositoryFGEM(), entornoFGEM.empresaServiceFGEM, entornoFGEM.sesionFGEM);
        DiagnosticoServiceFGEM diagnosticoFGEM = new DiagnosticoServiceFGEM(entornoFGEM.databaseFGEM, new OperacionRepositoryFGEM(),
                new MovimientoRepositoryFGEM(), entornoFGEM.empresaServiceFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM);
        servicioFGEM = new InformeServiceFGEM(entornoFGEM.databaseFGEM, new InformeRepositoryFGEM(), analisisFGEM, diagnosticoFGEM,
                entornoFGEM.operacionesFGEM, entornoFGEM.finanzasFGEM, entornoFGEM.empresaServiceFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM);
        entornoFGEM.administradorConectadoFGEM();
        new DatosDemoServiceFGEM(entornoFGEM.empresaServiceFGEM, entornoFGEM.catalogoFGEM, entornoFGEM.operacionesFGEM, entornoFGEM.finanzasFGEM,
                entornoFGEM.patrimonioFGEM, entornoFGEM.tesoreriaFGEM,
                entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM).crearEmpresaDemoFGEM();
    }

    @Test
    void informeEjecutivoEnPdfFGEM() throws Exception {
        Path pdfFGEM = servicioFGEM.generarFGEM(TipoInformeFGEM.INFORME_EJECUTIVO, DESDE_FGEM, HASTA_FGEM, salidaFGEM.resolve("ejecutivo"));

        assertTrue(pdfFGEM.getFileName().toString().endsWith(".pdf"), "Se agrega la extensión");
        String textoFGEM = textoPdfFGEM(pdfFGEM);
        assertTrue(textoFGEM.contains("Bodega Demo"), textoFGEM);
        assertTrue(textoFGEM.contains("Informe ejecutivo"));
        assertTrue(textoFGEM.contains("Estado de resultados"));
        assertTrue(textoFGEM.contains("Diagnóstico empresarial"), "Las tildes se conservan en el PDF");
        assertTrue(textoFGEM.contains("Recomendaciones"));
        assertTrue(textoFGEM.contains("Agua mineral"));
        assertFalse(Files.exists(salidaFGEM.resolve("ejecutivo.pdf.tmp")), "No quedan temporales");
    }

    @Test
    void estadoDeResultadosYRentabilidadEnPdfFGEM() throws Exception {
        String estadoFGEM = textoPdfFGEM(servicioFGEM.generarFGEM(TipoInformeFGEM.ESTADO_RESULTADOS, DESDE_FGEM, HASTA_FGEM, salidaFGEM.resolve("estado.pdf")));
        assertTrue(estadoFGEM.contains("Utilidad antes de impuestos"));
        assertTrue(estadoFGEM.contains("Márgenes"));

        String productosFGEM = textoPdfFGEM(servicioFGEM.generarFGEM(TipoInformeFGEM.RENTABILIDAD_PRODUCTOS, DESDE_FGEM, HASTA_FGEM,
                salidaFGEM.resolve("productos.pdf")));
        assertTrue(productosFGEM.contains("Arroz 5 kg"));
        assertTrue(productosFGEM.contains("por debajo de su costo"));
    }

    @Test
    void libroDeExcelConHojasYFormulasValidasFGEM() throws Exception {
        Path xlsxFGEM = servicioFGEM.generarFGEM(TipoInformeFGEM.LIBRO_EXCEL, DESDE_FGEM, HASTA_FGEM, salidaFGEM.resolve("datos"));

        assertTrue(xlsxFGEM.getFileName().toString().endsWith(".xlsx"));
        try (InputStream entradaFGEM = Files.newInputStream(xlsxFGEM); XSSFWorkbook libroFGEM = new XSSFWorkbook(entradaFGEM)) {
            assertEquals(List.of("Resumen", "Evolución mensual", "Productos", "Ventas", "Compras", "Ingresos y gastos"),
                    List.of(libroFGEM.getSheetName(0), libroFGEM.getSheetName(1), libroFGEM.getSheetName(2), libroFGEM.getSheetName(3),
                            libroFGEM.getSheetName(4), libroFGEM.getSheetName(5)));
            FormulaEvaluator evaluadorFGEM = libroFGEM.getCreationHelper().createFormulaEvaluator();
            evaluadorFGEM.evaluateAll();

            Sheet ventasFGEM = libroFGEM.getSheet("Ventas");
            Row encabezadoFGEM = ventasFGEM.getRow(3);
            assertEquals("Número", encabezadoFGEM.getCell(0).getStringCellValue());
            Row primeraFGEM = ventasFGEM.getRow(4);
            assertEquals(CellType.NUMERIC, primeraFGEM.getCell(1).getCellType(), "La fecha es una fecha de Excel");
            assertEquals(CellType.NUMERIC, primeraFGEM.getCell(8).getCellType(), "El total es un número");

            // Total de ventas registradas en la hoja = ventas netas del estado de resultados (hoja Resumen).
            Row totalFGEM = ventasFGEM.getRow(ventasFGEM.getLastRowNum());
            double totalSubtotalFGEM = evaluadorFGEM.evaluate(totalFGEM.getCell(6)).getNumberValue();
            double ventasResumenFGEM = libroFGEM.getSheet("Resumen").getRow(4).getCell(1).getNumericCellValue();
            assertEquals(ventasResumenFGEM, totalSubtotalFGEM, 0.01);
            assertTrue(totalSubtotalFGEM > 0);

            Sheet movimientosFGEM = libroFGEM.getSheet("Ingresos y gastos");
            Cell gastosFGEM = movimientosFGEM.getRow(movimientosFGEM.getLastRowNum()).getCell(7);
            assertTrue(evaluadorFGEM.evaluate(gastosFGEM).getNumberValue() > 0);
        }
    }

    @Test
    void registraElHistorialFGEM() {
        servicioFGEM.generarFGEM(TipoInformeFGEM.ESTADO_RESULTADOS, DESDE_FGEM, HASTA_FGEM, salidaFGEM.resolve("a.pdf"));
        servicioFGEM.generarFGEM(TipoInformeFGEM.LIBRO_EXCEL, DESDE_FGEM, HASTA_FGEM, salidaFGEM.resolve("b.xlsx"));

        List<InformeGeneradoFGEM> historialFGEM = servicioFGEM.historialFGEM();
        assertEquals(2, historialFGEM.size());
        assertTrue(historialFGEM.stream().anyMatch(iFGEM -> iFGEM.tipoFGEM() == TipoInformeFGEM.LIBRO_EXCEL
                && iFGEM.archivoFGEM().getFileName().toString().equals("b.xlsx") && iFGEM.desdeFGEM().equals(DESDE_FGEM)));
        assertNotNull(historialFGEM.get(0).generadoPorFGEM());
    }

    @Test
    void validaPeriodoYDestinoFGEM() {
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.generarFGEM(TipoInformeFGEM.ESTADO_RESULTADOS,
                HASTA_FGEM, DESDE_FGEM, salidaFGEM.resolve("x.pdf")));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.generarFGEM(TipoInformeFGEM.ESTADO_RESULTADOS,
                DESDE_FGEM, LocalDate.of(2026, 2, 1), salidaFGEM.resolve("x.pdf")), "No se informa el futuro");
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.generarFGEM(TipoInformeFGEM.ESTADO_RESULTADOS,
                DESDE_FGEM, HASTA_FGEM, carpetaFGEM.resolve("no-existe").resolve("x.pdf")));
        assertTrue(servicioFGEM.historialFGEM().isEmpty());
    }

    @Test
    void nombreSugeridoSinCaracteresProblematicosFGEM() {
        EmpresaFGEM empresaFGEM = entornoFGEM.sesionFGEM.empresaActivaFGEM().orElseThrow();
        assertEquals("Informe-ejecutivo_Bodega-Demo_2025-10-01_2025-12-31.pdf",
                InformeServiceFGEM.nombreSugeridoFGEM(TipoInformeFGEM.INFORME_EJECUTIVO, empresaFGEM, DESDE_FGEM, HASTA_FGEM));
        assertEquals("Libro-de-datos-en-Excel_Bodega-Demo_2025-10-01_2025-12-31.xlsx",
                InformeServiceFGEM.nombreSugeridoFGEM(TipoInformeFGEM.LIBRO_EXCEL, empresaFGEM, DESDE_FGEM, HASTA_FGEM));
    }

    @Test
    void elOperadorNoGeneraInformesFGEM() {
        EmpresaFGEM empresaFGEM = entornoFGEM.sesionFGEM.empresaActivaFGEM().orElseThrow();
        UsuarioFGEM operadorFGEM = entornoFGEM.crearUsuarioFGEM("operador", "OPERADOR");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of(operadorFGEM.idFGEM()));
        entornoFGEM.entrarComoFGEM("operador", "Temporal123");
        entornoFGEM.empresaServiceFGEM.restaurarEmpresaActivaFGEM();

        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.generarFGEM(TipoInformeFGEM.ESTADO_RESULTADOS,
                DESDE_FGEM, HASTA_FGEM, salidaFGEM.resolve("x.pdf")));
        assertThrows(AccesoDenegadoExceptionFGEM.class, servicioFGEM::historialFGEM);
    }

    private static String textoPdfFGEM(Path pdfFGEM) throws Exception {
        byte[] bytesFGEM = Files.readAllBytes(pdfFGEM);
        assertEquals("%PDF", new String(bytesFGEM, 0, 4));
        PdfReader lectorFGEM = new PdfReader(bytesFGEM);
        try {
            PdfTextExtractor extractorFGEM = new PdfTextExtractor(lectorFGEM);
            StringBuilder textoFGEM = new StringBuilder();
            for (int paginaFGEM = 1; paginaFGEM <= lectorFGEM.getNumberOfPages(); paginaFGEM++) {
                textoFGEM.append(extractorFGEM.getTextFromPage(paginaFGEM)).append('\n');
            }
            return textoFGEM.toString();
        } finally {
            lectorFGEM.close();
        }
    }
}
