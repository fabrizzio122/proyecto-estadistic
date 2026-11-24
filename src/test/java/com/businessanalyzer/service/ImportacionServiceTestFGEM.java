package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.importacion.DatosImportacionFGEM.ErrorFilaFGEM;
import com.businessanalyzer.importacion.DatosImportacionFGEM.ResultadoFGEM;
import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.ComportamientoFGEM;
import com.businessanalyzer.model.MovimientoFinancieroFGEM;
import com.businessanalyzer.model.OperacionFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportacionServiceTestFGEM {

    /** El reloj del entorno de prueba marca el 15/01/2026. */
    private static final LocalDate DESDE_FGEM = LocalDate.of(2025, 1, 1);
    private static final LocalDate HASTA_FGEM = LocalDate.of(2026, 1, 31);

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private ImportacionServiceFGEM importacionFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        entornoFGEM.administradorConectadoFGEM();
        entornoFGEM.crearEmpresaActivaFGEM("Bodega Doña Carmen");
        importacionFGEM = new ImportacionServiceFGEM(entornoFGEM.databaseFGEM, entornoFGEM.catalogoFGEM, entornoFGEM.operacionesFGEM,
                entornoFGEM.finanzasFGEM, entornoFGEM.empresaServiceFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM);
    }

    @Test
    void elEjemploDeBodegaSeRevisaSinGuardarYLuegoSeImportaCompletoFGEM() {
        Path archivoFGEM = carpetaFGEM.resolve("ejemplo.xlsx");
        importacionFGEM.crearPlantillaFGEM(archivoFGEM, true);

        ResultadoFGEM revisionFGEM = importacionFGEM.revisarFGEM(archivoFGEM);
        assertTrue(revisionFGEM.sinErroresFGEM(), () -> "Errores: " + revisionFGEM.erroresFGEM());
        assertFalse(revisionFGEM.aplicadoFGEM());
        assertEquals(14, revisionFGEM.productosFGEM());
        assertTrue(entornoFGEM.catalogoFGEM.listarProductosFGEM(true).isEmpty(), "La revisión no debe guardar nada");
        assertTrue(entornoFGEM.catalogoFGEM.listarCategoriasFGEM().isEmpty());

        ResultadoFGEM resultadoFGEM = importacionFGEM.importarFGEM(archivoFGEM);
        assertTrue(resultadoFGEM.aplicadoFGEM());
        assertEquals(revisionFGEM.registrosFGEM(), resultadoFGEM.registrosFGEM(), "La revisión anticipa exactamente lo que se importa");
        assertEquals(6, resultadoFGEM.categoriasFGEM());
        assertTrue(resultadoFGEM.advertenciasFGEM().isEmpty(), () -> "Advertencias: " + resultadoFGEM.advertenciasFGEM());
        // Tres meses completos anteriores al mes en curso: octubre a diciembre de 2025.
        assertEquals(LocalDate.of(2025, 10, 1), resultadoFGEM.desdeFGEM());
        assertEquals(LocalDate.of(2025, 12, 31), resultadoFGEM.hastaFGEM());
        assertEquals(21, resultadoFGEM.gastosFGEM());

        List<OperacionFGEM> ventasFGEM = entornoFGEM.operacionesFGEM.listarFGEM(TipoOperacionFGEM.VENTA, DESDE_FGEM, HASTA_FGEM, false);
        assertEquals(resultadoFGEM.ventasFGEM(), ventasFGEM.size());
        assertTrue(ventasFGEM.size() > 400, "Unas cinco boletas por día durante tres meses");
        assertTrue(ventasFGEM.stream().allMatch(vFGEM -> vFGEM.impuestoFGEM().signum() == 0), "Bodega en el Nuevo RUS: sin IGV");
        assertTrue(entornoFGEM.catalogoFGEM.listarProductosFGEM(false).stream()
                .allMatch(pFGEM -> pFGEM.stockFGEM().signum() >= 0), "El ejemplo nunca deja stock negativo");

        var resumenFGEM = entornoFGEM.operacionesFGEM.resumenFGEM(DESDE_FGEM, HASTA_FGEM);
        assertTrue(resumenFGEM.margenBrutoFGEM().signum() > 0);
    }

    @Test
    void siUnaFilaFallaNoSeGuardaNadaYSeInformanTodasLasFilasFGEM() throws Exception {
        Path archivoFGEM = libroFGEM(libroFGEM -> {
            hojaFGEM(libroFGEM, "Productos", new Object[]{"Código", "Nombre", "Costo unitario", "Precio de venta"},
                    new Object[]{"ARR", "Arroz", 3.2, 4.0},
                    new Object[]{"ARR", "Arroz repetido", 3.2, 4.0},
                    new Object[]{"AZU", "Azúcar", "tres", 4.5});
            hojaFGEM(libroFGEM, "Ventas", new Object[]{"Fecha", "Producto", "Cantidad"},
                    new Object[]{"10/01/2026", "ARR", 2},
                    new Object[]{"10/01/2026", "Quinua", 1},
                    new Object[]{"20/01/2026", "ARR", 1});
            hojaFGEM(libroFGEM, "Gastos", new Object[]{"Fecha", "Categoría", "Descripción", "Monto"},
                    new Object[]{"05/01/2026", "Pago a la NASA", "Algo raro", 10});
        });

        ResultadoFGEM resultadoFGEM = importacionFGEM.importarFGEM(archivoFGEM);

        assertFalse(resultadoFGEM.aplicadoFGEM());
        List<String> ubicacionesFGEM = resultadoFGEM.erroresFGEM().stream().map(ErrorFilaFGEM::ubicacionFGEM).toList();
        assertEquals(List.of("Productos, fila 3", "Productos, fila 4", "Ventas, fila 3", "Ventas, fila 4", "Gastos, fila 2"), ubicacionesFGEM);
        assertTrue(resultadoFGEM.erroresFGEM().get(2).mensajeFGEM().contains("Quinua"));
        assertTrue(resultadoFGEM.erroresFGEM().get(3).mensajeFGEM().contains("futura"), resultadoFGEM.erroresFGEM().get(3).mensajeFGEM());
        assertTrue(entornoFGEM.catalogoFGEM.listarProductosFGEM(true).isEmpty(), "Con errores no se guarda nada");
        assertTrue(entornoFGEM.operacionesFGEM.listarFGEM(TipoOperacionFGEM.VENTA, DESDE_FGEM, HASTA_FGEM, true).isEmpty());
    }

    @Test
    void entiendeColumnasEnOtroOrdenYFormatosHabitualesFGEM() throws Exception {
        Path archivoFGEM = libroFGEM(libroFGEM -> {
            CellStyle porcentajeFGEM = libroFGEM.createCellStyle();
            porcentajeFGEM.setDataFormat(libroFGEM.createDataFormat().getFormat("0%"));
            hojaFGEM(libroFGEM, "PRODUCTOS", new Object[]{"Precio", "Unidad de medida", "NOMBRE", "Costo", "Categoria", "Cód"},
                    new Object[]{"S/ 1.234,50", "kilos", "Queso fresco", "900,00", "Lácteos", 7751234},
                    new Object[]{4.5, "", "Pan  ", 0.3, "", ""});
            Sheet ventasFGEM = hojaFGEM(libroFGEM, "ventas", new Object[]{"N° de boleta", "Día", "Producto (código o nombre)", "Cant.", "IGV"},
                    new Object[]{"B-1", "05/01/2026", "7751234", "0,5", null},
                    new Object[]{"B-1", "05/01/2026", "pan", 3, null},
                    new Object[]{"B-1", "05/01/2026", "PAN", 2, null});
            Cell igvFGEM = ventasFGEM.getRow(1).createCell(4);
            igvFGEM.setCellValue(0.18);
            igvFGEM.setCellStyle(porcentajeFGEM);
            hojaFGEM(libroFGEM, "Gastos e ingresos", new Object[]{"Fecha", "Concepto", "Detalle", "Importe", "Fijo o variable"},
                    new Object[]{LocalDate.of(2026, 1, 3), "Luz", "Recibo de luz", "180", "variable"},
                    new Object[]{LocalDate.of(2026, 1, 4), "Intereses ganados", "Ahorros", 12, null});
        });

        ResultadoFGEM resultadoFGEM = importacionFGEM.importarFGEM(archivoFGEM);

        assertTrue(resultadoFGEM.aplicadoFGEM(), () -> "Errores: " + resultadoFGEM.erroresFGEM());
        ProductoFGEM quesoFGEM = entornoFGEM.catalogoFGEM.listarProductosFGEM(false).stream()
                .filter(pFGEM -> pFGEM.codigoFGEM().equals("7751234")).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("1234.50").compareTo(quesoFGEM.precioVentaFGEM()));
        assertEquals(0, new BigDecimal("900").compareTo(quesoFGEM.costoUnitarioFGEM()));
        assertEquals(UnidadMedidaFGEM.KILOGRAMO, quesoFGEM.unidadFGEM());
        assertEquals("Lácteos", quesoFGEM.categoriaNombreFGEM());

        // Las tres filas de la boleta B-1 forman una sola venta y el pan repetido se suma.
        List<OperacionFGEM> ventasFGEM = entornoFGEM.operacionesFGEM.listarFGEM(TipoOperacionFGEM.VENTA, DESDE_FGEM, HASTA_FGEM, false);
        assertEquals(1, ventasFGEM.size());
        OperacionFGEM ventaFGEM = entornoFGEM.operacionesFGEM.obtenerFGEM(ventasFGEM.get(0).idFGEM());
        assertEquals(2, ventaFGEM.lineasFGEM().size());
        assertEquals(0, new BigDecimal("18").compareTo(ventaFGEM.tasaImpuestoFGEM()));
        assertEquals(0, new BigDecimal("5").compareTo(ventaFGEM.lineasFGEM().stream()
                .filter(lFGEM -> lFGEM.nombreFGEM().equals("Pan")).findFirst().orElseThrow().cantidadFGEM()));
        assertFalse(resultadoFGEM.advertenciasFGEM().isEmpty(), "Se vendió sin compras: el stock queda en negativo y se avisa");

        List<MovimientoFinancieroFGEM> movimientosFGEM = entornoFGEM.finanzasFGEM.listarFGEM(DESDE_FGEM, HASTA_FGEM, false);
        assertEquals(2, movimientosFGEM.size());
        MovimientoFinancieroFGEM luzFGEM = movimientosFGEM.stream().filter(mFGEM -> mFGEM.categoriaFGEM() == CategoriaFinancieraFGEM.SERVICIOS_BASICOS)
                .findFirst().orElseThrow();
        assertEquals(ComportamientoFGEM.VARIABLE, luzFGEM.comportamientoFGEM());
        assertTrue(movimientosFGEM.stream().anyMatch(mFGEM -> mFGEM.categoriaFGEM() == CategoriaFinancieraFGEM.INTERESES_GANADOS));
    }

    @Test
    void lasComprasSeRegistranAntesQueLasVentasDelMismoDiaAunqueVenganDespuesFGEM() throws Exception {
        Path archivoFGEM = libroFGEM(libroFGEM -> {
            hojaFGEM(libroFGEM, "Productos", new Object[]{"Código", "Nombre", "Costo unitario", "Precio de venta"},
                    new Object[]{"GAS", "Gaseosa", 1.9, 2.5});
            hojaFGEM(libroFGEM, "Ventas", new Object[]{"Fecha", "Producto", "Cantidad"},
                    new Object[]{"02/01/2026", "GAS", 10},
                    new Object[]{"03/01/2026", "GAS", 5});
            hojaFGEM(libroFGEM, "Compras", new Object[]{"Fecha", "Producto", "Cantidad", "Costo unitario", "IGV %"},
                    new Object[]{"02/01/2026", "GAS", 12, 2.0, 0},
                    new Object[]{"03/01/2026", "GAS", 12, 2.1, 0});
        });

        ResultadoFGEM resultadoFGEM = importacionFGEM.importarFGEM(archivoFGEM);

        assertTrue(resultadoFGEM.aplicadoFGEM(), () -> "Errores: " + resultadoFGEM.erroresFGEM());
        assertTrue(resultadoFGEM.advertenciasFGEM().isEmpty(), () -> "No debería faltar stock: " + resultadoFGEM.advertenciasFGEM());
        ProductoFGEM gaseosaFGEM = entornoFGEM.catalogoFGEM.listarProductosFGEM(false).get(0);
        assertEquals(0, new BigDecimal("9").compareTo(gaseosaFGEM.stockFGEM()));
        assertEquals(0, new BigDecimal("2.1").compareTo(gaseosaFGEM.costoUnitarioFGEM()), "El costo queda en el de la última compra");
        // Cada venta usa el costo vigente ese día: 10 × 2.00 + 5 × 2.10
        var resumenFGEM = entornoFGEM.operacionesFGEM.resumenFGEM(DESDE_FGEM, HASTA_FGEM);
        assertEquals(0, new BigDecimal("30.5").compareTo(resumenFGEM.costoVentasFGEM()));
    }

    @Test
    void losProductosQueYaExistenNoSeDuplicanNiSeModificanFGEM() throws Exception {
        Path archivoFGEM = libroFGEM(libroFGEM -> hojaFGEM(libroFGEM, "Productos",
                new Object[]{"Código", "Nombre", "Costo unitario", "Precio de venta"},
                new Object[]{"ARR", "Arroz", 3.2, 4.0},
                new Object[]{"", "Azúcar", 3.0, 4.0}));
        assertTrue(importacionFGEM.importarFGEM(archivoFGEM).aplicadoFGEM());

        Path segundoFGEM = libroFGEM(libroFGEM -> hojaFGEM(libroFGEM, "Productos",
                new Object[]{"Código", "Nombre", "Costo unitario", "Precio de venta"},
                new Object[]{"arr", "Arroz", 9.9, 9.9},
                new Object[]{"", "AZÚCAR", 9.9, 9.9}));
        ResultadoFGEM resultadoFGEM = importacionFGEM.importarFGEM(segundoFGEM);

        assertTrue(resultadoFGEM.aplicadoFGEM());
        assertEquals(0, resultadoFGEM.productosFGEM());
        assertEquals(1, resultadoFGEM.advertenciasFGEM().size());
        List<ProductoFGEM> productosFGEM = entornoFGEM.catalogoFGEM.listarProductosFGEM(true);
        assertEquals(2, productosFGEM.size());
        assertTrue(productosFGEM.stream().allMatch(pFGEM -> pFGEM.precioVentaFGEM().compareTo(new BigDecimal("4")) == 0));
    }

    @Test
    void rechazaArchivosQueNoSonPlantillasFGEM() throws Exception {
        Path textoFGEM = carpetaFGEM.resolve("notas.xlsx");
        Files.writeString(textoFGEM, "esto no es Excel");
        assertThrows(ValidacionExceptionFGEM.class, () -> importacionFGEM.revisarFGEM(textoFGEM));

        Path otroFGEM = libroFGEM(libroFGEM -> hojaFGEM(libroFGEM, "Hoja1", new Object[]{"A"}, new Object[]{"1"}));
        assertThrows(ValidacionExceptionFGEM.class, () -> importacionFGEM.revisarFGEM(otroFGEM));

        Path vacioFGEM = carpetaFGEM.resolve("vacia.xlsx");
        importacionFGEM.crearPlantillaFGEM(vacioFGEM, false);
        assertThrows(ValidacionExceptionFGEM.class, () -> importacionFGEM.revisarFGEM(vacioFGEM));
    }

    @Test
    void unaColumnaObligatoriaFaltanteSeIndicaPorHojaFGEM() throws Exception {
        Path archivoFGEM = libroFGEM(libroFGEM -> hojaFGEM(libroFGEM, "Gastos", new Object[]{"Fecha", "Descripción", "Monto"},
                new Object[]{"02/01/2026", "Luz", 100}));

        ResultadoFGEM resultadoFGEM = importacionFGEM.revisarFGEM(archivoFGEM);

        assertEquals(1, resultadoFGEM.erroresFGEM().size());
        assertEquals("Gastos", resultadoFGEM.erroresFGEM().get(0).ubicacionFGEM());
        assertTrue(resultadoFGEM.erroresFGEM().get(0).mensajeFGEM().contains("Categoría"));
    }

    // ------------------------------------------------------------------ importar dos veces

    @Test
    void volverAImportarElMismoArchivoNoDuplicaNadaFGEM() {
        Path archivoFGEM = carpetaFGEM.resolve("ejemplo.xlsx");
        importacionFGEM.crearPlantillaFGEM(archivoFGEM, true);
        ResultadoFGEM primeraFGEM = importacionFGEM.importarFGEM(archivoFGEM);
        assertTrue(primeraFGEM.aplicadoFGEM());

        ResultadoFGEM segundaFGEM = importacionFGEM.importarFGEM(archivoFGEM);

        assertFalse(segundaFGEM.aplicadoFGEM());
        // Cada boleta, factura y gasto repetido es un error; los productos repetidos solo se reutilizan.
        assertEquals(primeraFGEM.ventasFGEM() + primeraFGEM.comprasFGEM() + primeraFGEM.gastosFGEM(), segundaFGEM.erroresFGEM().size());
        assertTrue(segundaFGEM.erroresFGEM().stream().noneMatch(eFGEM -> eFGEM.hojaFGEM().equals("Productos")));
        assertTrue(segundaFGEM.advertenciasFGEM().stream().anyMatch(aFGEM -> aFGEM.contains("ya tiene")), segundaFGEM.advertenciasFGEM()::toString);
        assertEquals(primeraFGEM.ventasFGEM(),
                entornoFGEM.operacionesFGEM.listarFGEM(TipoOperacionFGEM.VENTA, DESDE_FGEM, HASTA_FGEM, false).size());
    }

    /** El caso real: primero un archivo incompleto y después uno completo, cada uno con su inventario inicial. */
    @Test
    void unSegundoInventarioInicialSeRechazaAunqueTengaOtraFechaFGEM() throws Exception {
        Path incompletoFGEM = libroFGEM(libroFGEM -> {
            hojaFGEM(libroFGEM, "Productos", new Object[]{"Nombre", "Costo unitario", "Precio de venta"},
                    new Object[]{"Arroz", 3.2, 4.0});
            hojaFGEM(libroFGEM, "Compras", new Object[]{"Fecha", "Comprobante", "Proveedor", "Producto", "Cantidad"},
                    new Object[]{"14/01/2026", "Inventario inicial", "Inventario inicial", "Arroz", 50});
        });
        assertTrue(importacionFGEM.importarFGEM(incompletoFGEM).aplicadoFGEM());

        // El archivo completo trae códigos (el primero no) y otro inventario inicial con otra fecha.
        Path completoFGEM = libroFGEM(libroFGEM -> {
            hojaFGEM(libroFGEM, "Productos", new Object[]{"Código", "Nombre", "Costo unitario", "Precio de venta"},
                    new Object[]{"ARR-1K", "Arroz", 3.2, 4.0},
                    new Object[]{"AZU-1K", "Azúcar", 3.0, 4.0});
            hojaFGEM(libroFGEM, "Compras", new Object[]{"Fecha", "Comprobante", "Proveedor", "Producto", "Cantidad"},
                    new Object[]{"01/01/2026", "Inventario inicial", "Inventario inicial", "ARR-1K", 40},
                    new Object[]{"01/01/2026", "Inventario inicial", "Inventario inicial", "AZU-1K", 30});
            hojaFGEM(libroFGEM, "Ventas", new Object[]{"Fecha", "Producto", "Cantidad"},
                    new Object[]{"05/01/2026", "ARR-1K", 2});
        });

        ResultadoFGEM resultadoFGEM = importacionFGEM.revisarFGEM(completoFGEM);

        assertFalse(resultadoFGEM.sinErroresFGEM());
        assertEquals(1, resultadoFGEM.erroresFGEM().size(), resultadoFGEM.erroresFGEM()::toString);
        ErrorFilaFGEM errorFGEM = resultadoFGEM.erroresFGEM().get(0);
        assertEquals("Compras, fila 2", errorFGEM.ubicacionFGEM());
        assertTrue(errorFGEM.mensajeFGEM().contains("inventario inicial (C-000001)"), errorFGEM.mensajeFGEM());
        // «Arroz» con el código nuevo se reconoce como el mismo producto: solo el azúcar es nuevo,
        // y la venta que usa el código ARR-1K lo encuentra.
        assertEquals(1, resultadoFGEM.productosFGEM());
        assertEquals(1, resultadoFGEM.ventasFGEM());
    }

    @Test
    void dosGastosIgualesDelMismoArchivoSePermitenFGEM() throws Exception {
        Path archivoFGEM = libroFGEM(libroFGEM -> hojaFGEM(libroFGEM, "Gastos",
                new Object[]{"Fecha", "Categoría", "Descripción", "Monto"},
                new Object[]{"05/01/2026", "Movilidad", "Taxi al mercado", 5},
                new Object[]{"05/01/2026", "Movilidad", "Taxi al mercado", 5}));

        ResultadoFGEM resultadoFGEM = importacionFGEM.importarFGEM(archivoFGEM);

        assertTrue(resultadoFGEM.aplicadoFGEM(), resultadoFGEM.erroresFGEM()::toString);
        assertEquals(2, resultadoFGEM.gastosFGEM());
        assertFalse(importacionFGEM.revisarFGEM(archivoFGEM).sinErroresFGEM(), "Pero volver a importarlos sí se detecta");
    }

    // ------------------------------------------------------------------ utilidades

    private interface LlenadoFGEM {
        void llenarFGEM(XSSFWorkbook libroFGEM);
    }

    private int archivosFGEM;

    private Path libroFGEM(LlenadoFGEM llenadoFGEM) throws Exception {
        Path archivoFGEM = carpetaFGEM.resolve("datos-" + (++archivosFGEM) + ".xlsx");
        try (XSSFWorkbook libroFGEM = new XSSFWorkbook(); OutputStream salidaFGEM = Files.newOutputStream(archivoFGEM)) {
            llenadoFGEM.llenarFGEM(libroFGEM);
            libroFGEM.write(salidaFGEM);
        }
        return archivoFGEM;
    }

    private static Sheet hojaFGEM(XSSFWorkbook libroFGEM, String nombreFGEM, Object[] encabezadosFGEM, Object[]... filasFGEM) {
        Sheet hojaFGEM = libroFGEM.createSheet(nombreFGEM);
        CellStyle fechaFGEM = libroFGEM.createCellStyle();
        fechaFGEM.setDataFormat(libroFGEM.createDataFormat().getFormat("dd/mm/yyyy"));
        escribirFGEM(hojaFGEM.createRow(0), encabezadosFGEM, fechaFGEM);
        for (int iFGEM = 0; iFGEM < filasFGEM.length; iFGEM++) {
            escribirFGEM(hojaFGEM.createRow(iFGEM + 1), filasFGEM[iFGEM], fechaFGEM);
        }
        return hojaFGEM;
    }

    private static void escribirFGEM(Row rowFGEM, Object[] valoresFGEM, CellStyle fechaFGEM) {
        for (int iFGEM = 0; iFGEM < valoresFGEM.length; iFGEM++) {
            Object vFGEM = valoresFGEM[iFGEM];
            if (vFGEM == null) {
                continue;
            }
            Cell celdaFGEM = rowFGEM.createCell(iFGEM);
            if (vFGEM instanceof Number nFGEM) {
                celdaFGEM.setCellValue(nFGEM.doubleValue());
            } else if (vFGEM instanceof LocalDate dFGEM) {
                celdaFGEM.setCellValue(dFGEM);
                celdaFGEM.setCellStyle(fechaFGEM);
            } else {
                celdaFGEM.setCellValue(vFGEM.toString());
            }
        }
    }
}
