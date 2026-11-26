package com.businessanalyzer.informes;

import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.ReposicionFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.UrgenciaFGEM;
import com.businessanalyzer.informes.ListaComprasFGEM.GrupoFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListaComprasTestFGEM {

    @TempDir
    Path carpetaFGEM;

    private final ReposicionFGEM lecheFGEM = itemFGEM(1, "Leche evaporada", "Lácteos", false, UrgenciaFGEM.PRONTO, "12", "3.50");
    private final ReposicionFGEM yogurFGEM = itemFGEM(2, "Yogur", "Lácteos", true, UrgenciaFGEM.AGOTADO, "6", "4.00");
    private final ReposicionFGEM arrozFGEM = itemFGEM(3, "Arroz", "Abarrotes", false, UrgenciaFGEM.URGENTE, "20", "3.80");
    private final ReposicionFGEM velasFGEM = itemFGEM(4, "Velas", null, false, UrgenciaFGEM.PRONTO, "10", "0.50");

    @Test
    void agrupaPorCategoriaConSinCategoriaAlFinalYLosAgotadosPrimeroFGEM() {
        List<GrupoFGEM> gruposFGEM = ListaComprasFGEM.agruparFGEM(List.of(lecheFGEM, velasFGEM, yogurFGEM, arrozFGEM));

        assertEquals(List.of("Abarrotes", "Lácteos", ListaComprasFGEM.SIN_CATEGORIA_FGEM),
                gruposFGEM.stream().map(GrupoFGEM::categoriaFGEM).toList());
        assertEquals(List.of("Yogur", "Leche evaporada"),
                gruposFGEM.get(1).itemsFGEM().stream().map(rFGEM -> rFGEM.productoFGEM().nombreFGEM()).toList());
        assertEquals(0, new BigDecimal("66.00").compareTo(gruposFGEM.get(1).subtotalFGEM()), "12 × 3.50 + 6 × 4.00");
    }

    @Test
    void elPdfTieneSubtotalesYTotalFGEM() {
        String textoFGEM = ListaComprasFGEM.contenidoFGEM(List.of(lecheFGEM, yogurFGEM, arrozFGEM, velasFGEM), MonedaFGEM.PEN,
                "según las ventas de los últimos 60 días").textoPlanoFGEM();

        assertTrue(textoFGEM.contains("Subtotal Lácteos"), textoFGEM);
        assertTrue(textoFGEM.contains("Yogur (perecible)"), textoFGEM);
        assertTrue(textoFGEM.contains("12 und"), textoFGEM);
        assertTrue(textoFGEM.contains("Total de la compra") && textoFGEM.contains("147.00"), textoFGEM);
    }

    @Test
    void sinNadaQueComprarLoDiceFGEM() {
        assertTrue(ListaComprasFGEM.contenidoFGEM(List.of(), MonedaFGEM.PEN, "").textoPlanoFGEM().contains("No hay nada que comprar"));
    }

    @Test
    void elExcelCalculaElCostoConFormulasFGEM() throws Exception {
        Path archivoFGEM = carpetaFGEM.resolve("lista.xlsx");
        try (LibroExcelFGEM libroFGEM = ListaComprasFGEM.libroFGEM(List.of(lecheFGEM, arrozFGEM), MonedaFGEM.PEN, "Lista de compras", "detalle")) {
            libroFGEM.guardarFGEM(archivoFGEM);
        }
        try (InputStream entradaFGEM = Files.newInputStream(archivoFGEM); XSSFWorkbook libroFGEM = new XSSFWorkbook(entradaFGEM)) {
            Sheet hojaFGEM = libroFGEM.getSheet("Lista de compras");
            assertEquals("Arroz", hojaFGEM.getRow(4).getCell(1).getStringCellValue());
            assertEquals("ROUND(G5*H5,2)", hojaFGEM.getRow(4).getCell(8).getCellFormula());
            assertEquals("SUM(I5:I6)", hojaFGEM.getRow(6).getCell(8).getCellFormula());
            libroFGEM.getCreationHelper().createFormulaEvaluator().evaluateAll();
            assertEquals(118.0, hojaFGEM.getRow(6).getCell(8).getNumericCellValue(), 0.001, "20 × 3.80 + 12 × 3.50");
        }
    }

    private static ReposicionFGEM itemFGEM(long idFGEM, String nombreFGEM, String categoriaFGEM, boolean perecibleFGEM, UrgenciaFGEM urgenciaFGEM,
                                           String cantidadFGEM, String costoFGEM) {
        ProductoFGEM pFGEM = new ProductoFGEM(idFGEM, 1, categoriaFGEM == null ? null : idFGEM, categoriaFGEM, TipoItemFGEM.PRODUCTO, "P-" + idFGEM,
                nombreFGEM, null, UnidadMedidaFGEM.UNIDAD, new BigDecimal(costoFGEM), new BigDecimal(costoFGEM).add(BigDecimal.ONE),
                BigDecimal.ONE, true, null, null, perecibleFGEM);
        BigDecimal cantidadBdFGEM = new BigDecimal(cantidadFGEM);
        return new ReposicionFGEM(pFGEM, urgenciaFGEM, BigDecimal.ONE, BigDecimal.TEN, 60, new BigDecimal("0.5"), new BigDecimal("2"), cantidadBdFGEM,
                cantidadBdFGEM.multiply(new BigDecimal(costoFGEM)).setScale(2), false);
    }
}
