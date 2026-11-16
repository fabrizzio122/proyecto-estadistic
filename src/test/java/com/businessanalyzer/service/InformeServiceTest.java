package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.InformeGenerado;
import com.businessanalyzer.model.TipoInforme;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.repository.InformeRepository;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
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
class InformeServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2025, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2025, 12, 31);

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private InformeService servicio;
    private Path salida;

    @BeforeEach
    void preparar() throws Exception {
        entorno = new EntornoPrueba(Files.createDirectories(carpeta.resolve("bd")));
        salida = Files.createDirectories(carpeta.resolve("informes"));
        AnalisisService analisis = new AnalisisService(entorno.database, new OperacionRepository(),
                new MovimientoRepository(), entorno.empresaService, entorno.sesion);
        DiagnosticoService diagnostico = new DiagnosticoService(entorno.database, new OperacionRepository(),
                new MovimientoRepository(), entorno.empresaService, entorno.sesion, entorno.reloj);
        servicio = new InformeService(entorno.database, new InformeRepository(), analisis, diagnostico,
                entorno.operaciones, entorno.finanzas, entorno.empresaService, entorno.sesion, entorno.reloj);
        entorno.administradorConectado();
        new DatosDemoService(entorno.empresaService, entorno.catalogo, entorno.operaciones, entorno.finanzas,
                entorno.patrimonio, entorno.tesoreria,
                entorno.sesion, entorno.reloj).crearEmpresaDemo();
    }

    @Test
    void informeEjecutivoEnPdf() throws Exception {
        Path pdf = servicio.generar(TipoInforme.INFORME_EJECUTIVO, DESDE, HASTA, salida.resolve("ejecutivo"));

        assertTrue(pdf.getFileName().toString().endsWith(".pdf"), "Se agrega la extensión");
        String texto = textoPdf(pdf);
        assertTrue(texto.contains("Bodega Demo"), texto);
        assertTrue(texto.contains("Informe ejecutivo"));
        assertTrue(texto.contains("Estado de resultados"));
        assertTrue(texto.contains("Diagnóstico empresarial"), "Las tildes se conservan en el PDF");
        assertTrue(texto.contains("Recomendaciones"));
        assertTrue(texto.contains("Agua mineral"));
        assertFalse(Files.exists(salida.resolve("ejecutivo.pdf.tmp")), "No quedan temporales");
    }

    @Test
    void estadoDeResultadosYRentabilidadEnPdf() throws Exception {
        String estado = textoPdf(servicio.generar(TipoInforme.ESTADO_RESULTADOS, DESDE, HASTA, salida.resolve("estado.pdf")));
        assertTrue(estado.contains("Utilidad antes de impuestos"));
        assertTrue(estado.contains("Márgenes"));

        String productos = textoPdf(servicio.generar(TipoInforme.RENTABILIDAD_PRODUCTOS, DESDE, HASTA,
                salida.resolve("productos.pdf")));
        assertTrue(productos.contains("Arroz 5 kg"));
        assertTrue(productos.contains("por debajo de su costo"));
    }

    @Test
    void libroDeExcelConHojasYFormulasValidas() throws Exception {
        Path xlsx = servicio.generar(TipoInforme.LIBRO_EXCEL, DESDE, HASTA, salida.resolve("datos"));

        assertTrue(xlsx.getFileName().toString().endsWith(".xlsx"));
        try (InputStream entrada = Files.newInputStream(xlsx); XSSFWorkbook libro = new XSSFWorkbook(entrada)) {
            assertEquals(List.of("Resumen", "Evolución mensual", "Productos", "Ventas", "Compras", "Ingresos y gastos"),
                    List.of(libro.getSheetName(0), libro.getSheetName(1), libro.getSheetName(2), libro.getSheetName(3),
                            libro.getSheetName(4), libro.getSheetName(5)));
            FormulaEvaluator evaluador = libro.getCreationHelper().createFormulaEvaluator();
            evaluador.evaluateAll();

            Sheet ventas = libro.getSheet("Ventas");
            Row encabezado = ventas.getRow(3);
            assertEquals("Número", encabezado.getCell(0).getStringCellValue());
            Row primera = ventas.getRow(4);
            assertEquals(CellType.NUMERIC, primera.getCell(1).getCellType(), "La fecha es una fecha de Excel");
            assertEquals(CellType.NUMERIC, primera.getCell(8).getCellType(), "El total es un número");

            // Total de ventas registradas en la hoja = ventas netas del estado de resultados (hoja Resumen).
            Row total = ventas.getRow(ventas.getLastRowNum());
            double totalSubtotal = evaluador.evaluate(total.getCell(6)).getNumberValue();
            double ventasResumen = libro.getSheet("Resumen").getRow(4).getCell(1).getNumericCellValue();
            assertEquals(ventasResumen, totalSubtotal, 0.01);
            assertTrue(totalSubtotal > 0);

            Sheet movimientos = libro.getSheet("Ingresos y gastos");
            Cell gastos = movimientos.getRow(movimientos.getLastRowNum()).getCell(7);
            assertTrue(evaluador.evaluate(gastos).getNumberValue() > 0);
        }
    }

    @Test
    void registraElHistorial() {
        servicio.generar(TipoInforme.ESTADO_RESULTADOS, DESDE, HASTA, salida.resolve("a.pdf"));
        servicio.generar(TipoInforme.LIBRO_EXCEL, DESDE, HASTA, salida.resolve("b.xlsx"));

        List<InformeGenerado> historial = servicio.historial();
        assertEquals(2, historial.size());
        assertTrue(historial.stream().anyMatch(i -> i.tipo() == TipoInforme.LIBRO_EXCEL
                && i.archivo().getFileName().toString().equals("b.xlsx") && i.desde().equals(DESDE)));
        assertNotNull(historial.get(0).generadoPor());
    }

    @Test
    void validaPeriodoYDestino() {
        assertThrows(ValidacionException.class, () -> servicio.generar(TipoInforme.ESTADO_RESULTADOS,
                HASTA, DESDE, salida.resolve("x.pdf")));
        assertThrows(ValidacionException.class, () -> servicio.generar(TipoInforme.ESTADO_RESULTADOS,
                DESDE, LocalDate.of(2026, 2, 1), salida.resolve("x.pdf")), "No se informa el futuro");
        assertThrows(ValidacionException.class, () -> servicio.generar(TipoInforme.ESTADO_RESULTADOS,
                DESDE, HASTA, carpeta.resolve("no-existe").resolve("x.pdf")));
        assertTrue(servicio.historial().isEmpty());
    }

    @Test
    void nombreSugeridoSinCaracteresProblematicos() {
        Empresa empresa = entorno.sesion.empresaActiva().orElseThrow();
        assertEquals("Informe-ejecutivo_Bodega-Demo_2025-10-01_2025-12-31.pdf",
                InformeService.nombreSugerido(TipoInforme.INFORME_EJECUTIVO, empresa, DESDE, HASTA));
        assertEquals("Libro-de-datos-en-Excel_Bodega-Demo_2025-10-01_2025-12-31.xlsx",
                InformeService.nombreSugerido(TipoInforme.LIBRO_EXCEL, empresa, DESDE, HASTA));
    }

    @Test
    void elOperadorNoGeneraInformes() {
        Empresa empresa = entorno.sesion.empresaActiva().orElseThrow();
        Usuario operador = entorno.crearUsuario("operador", "OPERADOR");
        entorno.empresaService.asignarAccesos(empresa.id(), Set.of(operador.id()));
        entorno.entrarComo("operador", "Temporal123");
        entorno.empresaService.restaurarEmpresaActiva();

        assertThrows(AccesoDenegadoException.class, () -> servicio.generar(TipoInforme.ESTADO_RESULTADOS,
                DESDE, HASTA, salida.resolve("x.pdf")));
        assertThrows(AccesoDenegadoException.class, servicio::historial);
    }

    private static String textoPdf(Path pdf) throws Exception {
        byte[] bytes = Files.readAllBytes(pdf);
        assertEquals("%PDF", new String(bytes, 0, 4));
        PdfReader lector = new PdfReader(bytes);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(lector);
            StringBuilder texto = new StringBuilder();
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                texto.append(extractor.getTextFromPage(pagina)).append('\n');
            }
            return texto.toString();
        } finally {
            lector.close();
        }
    }
}
