package com.businessanalyzer.importacion;

import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.ComportamientoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.TipoMovimientoFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class PlantillaImportacionFGEM {

    private static final int FILAS_CON_LISTA_FGEM = 5_000;

    private PlantillaImportacionFGEM() {
    }

    public record ColumnaFGEM(String tituloFGEM, boolean obligatoriaFGEM, int anchoFGEM, String ayudaFGEM, List<String> aliasFGEM) {

        public List<String> nombresAceptadosFGEM() {
            List<String> nombresFGEM = new ArrayList<>();
            nombresFGEM.add(claveEncabezadoFGEM(tituloFGEM));
            aliasFGEM.forEach(aFGEM -> nombresFGEM.add(claveEncabezadoFGEM(aFGEM)));
            return nombresFGEM;
        }
    }

    private static final java.util.Set<String> RELLENO_ENCABEZADO_FGEM = java.util.Set.of("n", "no", "nro", "num", "numero", "de", "del", "la", "el");

    public static String claveEncabezadoFGEM(String textoFGEM) {
        String[] palabrasFGEM = normalizarFGEM(textoFGEM).split(" ");
        String claveFGEM = Arrays.stream(palabrasFGEM).filter(pFGEM -> !RELLENO_ENCABEZADO_FGEM.contains(pFGEM))
                .collect(java.util.stream.Collectors.joining(" "));
        return claveFGEM.isEmpty() ? normalizarFGEM(textoFGEM) : claveFGEM;
    }

    private static ColumnaFGEM colFGEM(String tituloFGEM, boolean obligatoriaFGEM, int anchoFGEM, String ayudaFGEM, String... aliasFGEM) {
        return new ColumnaFGEM(tituloFGEM, obligatoriaFGEM, anchoFGEM, ayudaFGEM, List.of(aliasFGEM));
    }

    public enum HojaFGEM {
        PRODUCTOS("Productos", "Catálogo: lo que vendes, cuánto te cuesta y a cuánto lo vendes.", List.of(
                colFGEM("Código", false, 14, "Opcional. Vacío = el sistema asigna P-0001, P-0002…", "cod", "codigo del producto", "sku"),
                colFGEM("Nombre", true, 34, "Obligatorio. Ej.: Arroz Costeño 5 kg", "producto", "nombre del producto", "descripcion"),
                colFGEM("Tipo", false, 12, "Producto o Servicio. Vacío = Producto", "tipo de item"),
                colFGEM("Categoría", false, 18, "Ej.: Abarrotes, Bebidas. Se crea si no existe", "rubro", "familia", "linea"),
                colFGEM("Unidad", false, 14, "Unidad, Kilogramo, Litro, Paquete… Vacío = Unidad", "unidad de medida", "um", "medida"),
                colFGEM("Costo unitario", true, 15, "Obligatorio. Lo que te cuesta una unidad", "costo", "precio de compra", "costo sin igv"),
                colFGEM("Precio de venta", true, 15, "Obligatorio. A cuánto vendes una unidad", "precio", "pvp", "precio sin igv"),
                colFGEM("Perecible", false, 11, "Sí si se vence pronto (pan, lácteos frescos, verduras). Vacío = No",
                        "se vence", "vence", "perecedero"))),
        VENTAS("Ventas", "Una fila por producto vendido. Las filas con la misma fecha y documento forman una sola venta.", List.of(
                colFGEM("Fecha", true, 12, "Obligatorio. dd/mm/aaaa", "dia"),
                colFGEM("Documento", false, 15, "Boleta o número de ticket. Vacío = cada fila es una venta",
                        "n documento", "numero de documento", "boleta", "comprobante", "ticket", "n boleta"),
                colFGEM("Cliente", false, 22, "Opcional", "nombre del cliente"),
                colFGEM("Producto", true, 30, "Obligatorio. Código o nombre exacto del producto",
                        "producto codigo o nombre", "codigo de producto", "codigo", "nombre del producto"),
                colFGEM("Cantidad", true, 10, "Obligatorio", "cant", "unidades"),
                colFGEM("Precio unitario", false, 14, "Vacío = precio del catálogo", "precio", "precio de venta"),
                colFGEM("IGV %", false, 8, "18 o 0. Vacío = el habitual de la empresa", "igv", "impuesto", "impuesto %"))),
        COMPRAS("Compras", "Una fila por producto comprado. Las filas con la misma fecha y comprobante forman una sola compra.", List.of(
                colFGEM("Fecha", true, 12, "Obligatorio. dd/mm/aaaa", "dia"),
                colFGEM("Comprobante", false, 15, "Factura o boleta del proveedor. Vacío = cada fila es una compra",
                        "documento", "factura", "n comprobante", "numero de comprobante"),
                colFGEM("Proveedor", false, 24, "Opcional", "nombre del proveedor"),
                colFGEM("RUC", false, 13, "Opcional. RUC del proveedor", "ruc del proveedor"),
                colFGEM("Producto", true, 30, "Obligatorio. Código o nombre exacto del producto",
                        "producto codigo o nombre", "codigo de producto", "codigo", "nombre del producto"),
                colFGEM("Cantidad", true, 10, "Obligatorio", "cant", "unidades"),
                colFGEM("Costo unitario", false, 14, "Vacío = costo del catálogo", "costo", "precio unitario", "precio"),
                colFGEM("IGV %", false, 8, "18 o 0. Vacío = el habitual de la empresa", "igv", "impuesto", "impuesto %"))),
        GASTOS("Gastos", "Gastos que no son compras de mercadería: alquiler, luz, sueldos… También acepta ingresos extra.", List.of(
                colFGEM("Fecha", true, 12, "Obligatorio. dd/mm/aaaa", "dia"),
                colFGEM("Categoría", true, 30, "Obligatorio. Elige de la lista o escribe: alquiler, luz, sueldo…", "tipo de gasto", "concepto"),
                colFGEM("Descripción", true, 30, "Obligatorio. Ej.: Recibo de luz de marzo", "detalle", "glosa"),
                colFGEM("Monto", true, 12, "Obligatorio", "importe", "total"),
                colFGEM("Fijo o variable", false, 14, "Vacío = el habitual de la categoría", "comportamiento", "fijo variable"),
                colFGEM("Comprobante", false, 15, "Opcional", "documento", "recibo")));

        private final String nombreFGEM;
        private final String descripcionFGEM;
        private final List<ColumnaFGEM> columnasFGEM;

        HojaFGEM(String nombreFGEM, String descripcionFGEM, List<ColumnaFGEM> columnasFGEM) {
            this.nombreFGEM = nombreFGEM;
            this.descripcionFGEM = descripcionFGEM;
            this.columnasFGEM = columnasFGEM;
        }

        public String nombreFGEM() {
            return nombreFGEM;
        }

        public String descripcionFGEM() {
            return descripcionFGEM;
        }

        public List<ColumnaFGEM> columnasFGEM() {
            return columnasFGEM;
        }
    }

    public static String normalizarFGEM(String textoFGEM) {
        if (textoFGEM == null) {
            return "";
        }
        String sinTildesFGEM = Normalizer.normalize(textoFGEM, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildesFGEM.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    public static void crearFGEM(Path destinoFGEM, boolean conEjemploFGEM, LocalDate hoyFGEM) throws IOException {
        try (XSSFWorkbook libroFGEM = new XSSFWorkbook()) {
            EstilosFGEM estilosFGEM = new EstilosFGEM(libroFGEM);
            crearInstruccionesFGEM(libroFGEM, estilosFGEM);
            Map<HojaFGEM, Sheet> hojasFGEM = new LinkedHashMap<>();
            for (HojaFGEM hojaFGEM : HojaFGEM.values()) {
                hojasFGEM.put(hojaFGEM, crearHojaFGEM(libroFGEM, hojaFGEM, estilosFGEM));
            }
            crearListasFGEM(libroFGEM, hojasFGEM);
            if (conEjemploFGEM) {
                new EjemploBodegaFGEM(hoyFGEM).escribirFGEM(hojasFGEM, estilosFGEM);
            }
            libroFGEM.setActiveSheet(0);
            Path carpetaFGEM = destinoFGEM.toAbsolutePath().getParent();
            if (carpetaFGEM != null) {
                Files.createDirectories(carpetaFGEM);
            }
            try (OutputStream salidaFGEM = Files.newOutputStream(destinoFGEM)) {
                libroFGEM.write(salidaFGEM);
            }
        }
    }

    private static void crearInstruccionesFGEM(Workbook libroFGEM, EstilosFGEM estilosFGEM) {
        Sheet hojaFGEM = libroFGEM.createSheet("Instrucciones");
        hojaFGEM.setColumnWidth(0, 110 * 256);
        List<String[]> lineasFGEM = new ArrayList<>();
        lineasFGEM.add(new String[]{"t", "Importar datos a Business Analyzer"});
        lineasFGEM.add(new String[]{"", "Llena las hojas que necesites (puedes dejar vacías las demás) y luego, en la aplicación, "
                + "abre «Importar datos» y elige este archivo."});
        lineasFGEM.add(new String[]{"", "Primero se revisa todo el archivo: si alguna fila tiene un error, no se guarda nada y verás "
                + "la hoja y la fila que debes corregir. Los datos van a la empresa activa."});
        lineasFGEM.add(new String[]{"", ""});
        for (HojaFGEM hFGEM : HojaFGEM.values()) {
            lineasFGEM.add(new String[]{"s", "Hoja «" + hFGEM.nombreFGEM() + "»"});
            lineasFGEM.add(new String[]{"", hFGEM.descripcionFGEM()});
            for (ColumnaFGEM cFGEM : hFGEM.columnasFGEM()) {
                lineasFGEM.add(new String[]{"", "   • " + cFGEM.tituloFGEM() + ": " + cFGEM.ayudaFGEM()});
            }
            lineasFGEM.add(new String[]{"", ""});
        }
        lineasFGEM.add(new String[]{"s", "Consejos para una bodega"});
        lineasFGEM.add(new String[]{"", "• Si estás en el Nuevo RUS (no cobras IGV), escribe 0 en «IGV %» y registra los precios tal como cobras y pagas."});
        lineasFGEM.add(new String[]{"", "• Si no anotas cada producto vendido, crea un producto «Venta del día» con costo 0 y registra el total "
                + "diario como precio; los análisis de rentabilidad por producto serán menos precisos."});
        lineasFGEM.add(new String[]{"", "• El stock se calcula con compras − ventas. Registra al inicio una compra «Inventario inicial» "
                + "con lo que tenías en la tienda, o el stock saldrá negativo (se avisa, pero se permite)."});
        lineasFGEM.add(new String[]{"", "• Los productos que ya existen en la empresa (mismo código o nombre) no se modifican: solo se usan."});
        lineasFGEM.add(new String[]{"", "• Fechas en formato dd/mm/aaaa. Montos con punto o coma decimal (12.50 o 12,50)."});

        int filaFGEM = 0;
        for (String[] lineaFGEM : lineasFGEM) {
            Row rowFGEM = hojaFGEM.createRow(filaFGEM++);
            var celdaFGEM = rowFGEM.createCell(0);
            celdaFGEM.setCellValue(lineaFGEM[1]);
            celdaFGEM.setCellStyle(switch (lineaFGEM[0]) {
                case "t" -> estilosFGEM.tituloFGEM;
                case "s" -> estilosFGEM.subtituloFGEM;
                default -> estilosFGEM.textoLargoFGEM;
            });
        }
    }

    private static Sheet crearHojaFGEM(Workbook libroFGEM, HojaFGEM hFGEM, EstilosFGEM estilosFGEM) {
        Sheet hojaFGEM = libroFGEM.createSheet(hFGEM.nombreFGEM());
        Row encabezadoFGEM = hojaFGEM.createRow(0);
        for (int iFGEM = 0; iFGEM < hFGEM.columnasFGEM().size(); iFGEM++) {
            ColumnaFGEM cFGEM = hFGEM.columnasFGEM().get(iFGEM);
            var celdaFGEM = encabezadoFGEM.createCell(iFGEM);
            celdaFGEM.setCellValue(cFGEM.tituloFGEM());
            celdaFGEM.setCellStyle(cFGEM.obligatoriaFGEM() ? estilosFGEM.encabezadoObligatorioFGEM : estilosFGEM.encabezadoFGEM);
            hojaFGEM.setColumnWidth(iFGEM, cFGEM.anchoFGEM() * 256);
        }
        hojaFGEM.createFreezePane(0, 1);
        return hojaFGEM;
    }

    private static void crearListasFGEM(Workbook libroFGEM, Map<HojaFGEM, Sheet> hojasFGEM) {
        Sheet listasFGEM = libroFGEM.createSheet("Listas");
        List<List<String>> columnasFGEM = List.of(
                Arrays.stream(TipoItemFGEM.values()).map(TipoItemFGEM::descripcionFGEM).toList(),
                Arrays.stream(UnidadMedidaFGEM.values()).map(UnidadMedidaFGEM::descripcionFGEM).toList(),
                Arrays.stream(CategoriaFinancieraFGEM.values())
                        .sorted((aFGEM, bFGEM) -> aFGEM.tipoFGEM() == bFGEM.tipoFGEM() ? 0 : aFGEM.tipoFGEM() == TipoMovimientoFGEM.GASTO ? -1 : 1)
                        .map(CategoriaFinancieraFGEM::descripcionFGEM).toList(),
                Arrays.stream(ComportamientoFGEM.values()).map(ComportamientoFGEM::descripcionFGEM).toList());
        for (int cFGEM = 0; cFGEM < columnasFGEM.size(); cFGEM++) {
            List<String> valoresFGEM = columnasFGEM.get(cFGEM);
            for (int fFGEM = 0; fFGEM < valoresFGEM.size(); fFGEM++) {
                Row rowFGEM = listasFGEM.getRow(fFGEM) != null ? listasFGEM.getRow(fFGEM) : listasFGEM.createRow(fFGEM);
                rowFGEM.createCell(cFGEM).setCellValue(valoresFGEM.get(fFGEM));
            }
        }
        libroFGEM.setSheetHidden(libroFGEM.getSheetIndex(listasFGEM), true);

        listaFGEM(hojasFGEM.get(HojaFGEM.PRODUCTOS), 2, "Listas!$A$1:$A$" + columnasFGEM.get(0).size());
        listaFGEM(hojasFGEM.get(HojaFGEM.PRODUCTOS), 4, "Listas!$B$1:$B$" + columnasFGEM.get(1).size());
        listaFGEM(hojasFGEM.get(HojaFGEM.GASTOS), 1, "Listas!$C$1:$C$" + columnasFGEM.get(2).size());
        listaFGEM(hojasFGEM.get(HojaFGEM.GASTOS), 4, "Listas!$D$1:$D$" + columnasFGEM.get(3).size());
    }

    private static void listaFGEM(Sheet hojaFGEM, int columnaFGEM, String rangoFGEM) {
        DataValidationHelper ayudanteFGEM = hojaFGEM.getDataValidationHelper();
        DataValidation validacionFGEM = ayudanteFGEM.createValidation(ayudanteFGEM.createFormulaListConstraint(rangoFGEM),
                new CellRangeAddressList(1, FILAS_CON_LISTA_FGEM, columnaFGEM, columnaFGEM));
        validacionFGEM.setShowErrorBox(false);
        hojaFGEM.addValidationData(validacionFGEM);
    }

    static final class EstilosFGEM {
        final CellStyle tituloFGEM;
        final CellStyle subtituloFGEM;
        final CellStyle textoLargoFGEM;
        final CellStyle encabezadoFGEM;
        final CellStyle encabezadoObligatorioFGEM;
        final CellStyle fechaFGEM;
        final CellStyle montoFGEM;

        EstilosFGEM(Workbook libroFGEM) {
            Font grandeFGEM = libroFGEM.createFont();
            grandeFGEM.setBold(true);
            grandeFGEM.setFontHeightInPoints((short) 15);
            tituloFGEM = libroFGEM.createCellStyle();
            tituloFGEM.setFont(grandeFGEM);

            Font negritaFGEM = libroFGEM.createFont();
            negritaFGEM.setBold(true);
            subtituloFGEM = libroFGEM.createCellStyle();
            subtituloFGEM.setFont(negritaFGEM);

            textoLargoFGEM = libroFGEM.createCellStyle();
            textoLargoFGEM.setWrapText(true);

            Font blancaFGEM = libroFGEM.createFont();
            blancaFGEM.setBold(true);
            blancaFGEM.setColor(IndexedColors.WHITE.getIndex());
            encabezadoFGEM = libroFGEM.createCellStyle();
            encabezadoFGEM.setFont(blancaFGEM);
            encabezadoFGEM.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            encabezadoFGEM.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            encabezadoObligatorioFGEM = libroFGEM.createCellStyle();
            encabezadoObligatorioFGEM.cloneStyleFrom(encabezadoFGEM);
            encabezadoObligatorioFGEM.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());

            fechaFGEM = libroFGEM.createCellStyle();
            fechaFGEM.setDataFormat(libroFGEM.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));
            montoFGEM = libroFGEM.createCellStyle();
            montoFGEM.setDataFormat(libroFGEM.getCreationHelper().createDataFormat().getFormat("#,##0.00"));
        }
    }

    static final class EjemploBodegaFGEM {

        private record ProductoEjemploFGEM(String codigoFGEM, String nombreFGEM, String categoriaFGEM, UnidadMedidaFGEM unidadFGEM,
                                           String costoFGEM, String precioFGEM, int maximoPorVentaFGEM, int popularidadFGEM, int stockObjetivoFGEM) {
        }

        private static final List<ProductoEjemploFGEM> PRODUCTOS_FGEM = List.of(
                new ProductoEjemploFGEM("ARR-5K", "Arroz Costeño 5 kg", "Abarrotes", UnidadMedidaFGEM.UNIDAD, "19.50", "23.00", 1, 3, 25),
                new ProductoEjemploFGEM("AZU-1K", "Azúcar rubia 1 kg", "Abarrotes", UnidadMedidaFGEM.UNIDAD, "3.60", "4.50", 2, 4, 40),
                new ProductoEjemploFGEM("ACE-1L", "Aceite vegetal 1 L", "Abarrotes", UnidadMedidaFGEM.UNIDAD, "9.20", "11.00", 1, 3, 25),
                new ProductoEjemploFGEM("FID-500", "Fideos spaghetti 500 g", "Abarrotes", UnidadMedidaFGEM.UNIDAD, "2.70", "3.50", 3, 4, 50),
                new ProductoEjemploFGEM("ATU-170", "Atún en lata 170 g", "Abarrotes", UnidadMedidaFGEM.UNIDAD, "5.20", "6.50", 2, 2, 30),
                new ProductoEjemploFGEM("LEC-400", "Leche evaporada 400 g", "Lácteos y huevos", UnidadMedidaFGEM.UNIDAD, "3.30", "4.00", 3, 5, 60),
                new ProductoEjemploFGEM("HUE-KG", "Huevos a granel", "Lácteos y huevos", UnidadMedidaFGEM.KILOGRAMO, "7.80", "9.50", 2, 4, 30),
                new ProductoEjemploFGEM("GAS-500", "Gaseosa 500 ml", "Bebidas", UnidadMedidaFGEM.UNIDAD, "1.90", "2.50", 3, 6, 72),
                new ProductoEjemploFGEM("AGU-625", "Agua mineral 625 ml", "Bebidas", UnidadMedidaFGEM.UNIDAD, "1.10", "1.50", 2, 4, 48),
                new ProductoEjemploFGEM("CER-650", "Cerveza 650 ml", "Bebidas", UnidadMedidaFGEM.UNIDAD, "5.30", "7.00", 6, 2, 48),
                new ProductoEjemploFGEM("GAL-SODA", "Galletas de soda", "Snacks y golosinas", UnidadMedidaFGEM.PAQUETE, "0.75", "1.00", 4, 5, 80),
                new ProductoEjemploFGEM("DET-900", "Detergente 900 g", "Limpieza", UnidadMedidaFGEM.UNIDAD, "9.50", "11.50", 1, 1, 15),
                new ProductoEjemploFGEM("PAP-4", "Papel higiénico x4", "Limpieza", UnidadMedidaFGEM.PAQUETE, "4.80", "6.00", 1, 2, 20),
                new ProductoEjemploFGEM("PAN-UN", "Pan francés", "Panadería", UnidadMedidaFGEM.UNIDAD, "0.25", "0.40", 10, 6, 0));

        private static final String DISTRIBUIDOR_FGEM = "Distribuidora Lima Norte S.A.C.";
        private static final String RUC_DISTRIBUIDOR_FGEM = "20512345678";
        private static final String PANADERIA_FGEM = "Panadería San Martín";
        private static final int PANES_DIARIOS_FGEM = 120;

        private final LocalDate desdeFGEM;
        private final LocalDate hastaFGEM;
        private final Random azarFGEM = new Random(2026);

        EjemploBodegaFGEM(LocalDate hoyFGEM) {
            YearMonth ultimoMesFGEM = YearMonth.from(hoyFGEM).minusMonths(1);
            this.desdeFGEM = ultimoMesFGEM.minusMonths(2).atDay(1);
            this.hastaFGEM = ultimoMesFGEM.atEndOfMonth();
        }

        void escribirFGEM(Map<HojaFGEM, Sheet> hojasFGEM, EstilosFGEM estilosFGEM) {
            escribirProductosFGEM(hojasFGEM.get(HojaFGEM.PRODUCTOS));
            escribirOperacionesFGEM(hojasFGEM.get(HojaFGEM.VENTAS), hojasFGEM.get(HojaFGEM.COMPRAS), estilosFGEM);
            escribirGastosFGEM(hojasFGEM.get(HojaFGEM.GASTOS), estilosFGEM);
        }

        private void escribirProductosFGEM(Sheet hojaFGEM) {
            int filaFGEM = 1;
            for (ProductoEjemploFGEM pFGEM : PRODUCTOS_FGEM) {
                Row rowFGEM = hojaFGEM.createRow(filaFGEM++);
                rowFGEM.createCell(0).setCellValue(pFGEM.codigoFGEM());
                rowFGEM.createCell(1).setCellValue(pFGEM.nombreFGEM());
                rowFGEM.createCell(2).setCellValue(TipoItemFGEM.PRODUCTO.descripcionFGEM());
                rowFGEM.createCell(3).setCellValue(pFGEM.categoriaFGEM());
                rowFGEM.createCell(4).setCellValue(pFGEM.unidadFGEM().descripcionFGEM());
                rowFGEM.createCell(5).setCellValue(Double.parseDouble(pFGEM.costoFGEM()));
                rowFGEM.createCell(6).setCellValue(Double.parseDouble(pFGEM.precioFGEM()));
                if (pFGEM.unidadFGEM() != UnidadMedidaFGEM.PAQUETE && pFGEM.codigoFGEM().startsWith("PAN")) {
                    rowFGEM.createCell(7).setCellValue("Sí");
                }
            }
        }

        private void escribirOperacionesFGEM(Sheet ventasFGEM, Sheet comprasFGEM, EstilosFGEM estilosFGEM) {
            Map<String, BigDecimal> stockFGEM = new LinkedHashMap<>();
            PRODUCTOS_FGEM.forEach(pFGEM -> stockFGEM.put(pFGEM.codigoFGEM(), BigDecimal.ZERO));
            int filaVentaFGEM = 1;
            int filaCompraFGEM = 1;
            int boletaFGEM = 1;
            int facturaFGEM = 3100;

            for (LocalDate diaFGEM = desdeFGEM; !diaFGEM.isAfter(hastaFGEM); diaFGEM = diaFGEM.plusDays(1)) {
                if (diaFGEM.equals(desdeFGEM) || diaFGEM.getDayOfWeek() == DayOfWeek.MONDAY) {
                    String comprobanteFGEM = "F001-" + (facturaFGEM++);
                    for (ProductoEjemploFGEM pFGEM : PRODUCTOS_FGEM) {
                        BigDecimal faltaFGEM = BigDecimal.valueOf(pFGEM.stockObjetivoFGEM()).subtract(stockFGEM.get(pFGEM.codigoFGEM()));
                        if (faltaFGEM.signum() > 0) {
                            filaCompraFGEM = compraFGEM(comprasFGEM, filaCompraFGEM, diaFGEM, comprobanteFGEM, DISTRIBUIDOR_FGEM,
                                    RUC_DISTRIBUIDOR_FGEM, pFGEM, faltaFGEM, estilosFGEM);
                            stockFGEM.merge(pFGEM.codigoFGEM(), faltaFGEM, BigDecimal::add);
                        }
                    }
                }
                ProductoEjemploFGEM panFGEM = PRODUCTOS_FGEM.get(PRODUCTOS_FGEM.size() - 1);
                BigDecimal panesFGEM = BigDecimal.valueOf(PANES_DIARIOS_FGEM).subtract(stockFGEM.get(panFGEM.codigoFGEM()));
                if (panesFGEM.signum() > 0) {
                    filaCompraFGEM = compraFGEM(comprasFGEM, filaCompraFGEM, diaFGEM, "PAN-" + diaFGEM.getDayOfMonth() + "/" + diaFGEM.getMonthValue(),
                            PANADERIA_FGEM, null, panFGEM, panesFGEM, estilosFGEM);
                    stockFGEM.merge(panFGEM.codigoFGEM(), panesFGEM, BigDecimal::add);
                }

                boolean finDeSemanaFGEM = diaFGEM.getDayOfWeek().getValue() >= 6;
                int boletasFGEM = (finDeSemanaFGEM ? 7 : 4) + azarFGEM.nextInt(4);
                for (int bFGEM = 0; bFGEM < boletasFGEM; bFGEM++) {
                    String documentoFGEM = "B001-" + String.format(Locale.ROOT, "%05d", boletaFGEM++);
                    int lineasFGEM = 1 + azarFGEM.nextInt(4);
                    List<ProductoEjemploFGEM> elegidosFGEM = new ArrayList<>();
                    while (elegidosFGEM.size() < lineasFGEM) {
                        ProductoEjemploFGEM pFGEM = elegirFGEM();
                        if (!elegidosFGEM.contains(pFGEM)) {
                            elegidosFGEM.add(pFGEM);
                        }
                    }
                    for (ProductoEjemploFGEM pFGEM : elegidosFGEM) {
                        BigDecimal cantidadFGEM = pFGEM.unidadFGEM() == UnidadMedidaFGEM.KILOGRAMO
                                ? BigDecimal.valueOf(25 + azarFGEM.nextInt(4) * 25L, 2)
                                : BigDecimal.valueOf(1 + azarFGEM.nextInt(pFGEM.maximoPorVentaFGEM()));
                        BigDecimal disponibleFGEM = stockFGEM.get(pFGEM.codigoFGEM());
                        if (disponibleFGEM.compareTo(cantidadFGEM) < 0) {
                            continue;
                        }
                        stockFGEM.put(pFGEM.codigoFGEM(), disponibleFGEM.subtract(cantidadFGEM));
                        Row rowFGEM = ventasFGEM.createRow(filaVentaFGEM++);
                        fechaFGEM(rowFGEM, 0, diaFGEM, estilosFGEM);
                        rowFGEM.createCell(1).setCellValue(documentoFGEM);
                        rowFGEM.createCell(3).setCellValue(pFGEM.codigoFGEM());
                        rowFGEM.createCell(4).setCellValue(cantidadFGEM.doubleValue());
                        montoFGEM(rowFGEM, 5, pFGEM.precioFGEM(), estilosFGEM);
                        rowFGEM.createCell(6).setCellValue(0);
                    }
                }
            }
        }

        private static int compraFGEM(Sheet hojaFGEM, int filaFGEM, LocalDate diaFGEM, String comprobanteFGEM, String proveedorFGEM, String rucFGEM,
                                      ProductoEjemploFGEM pFGEM, BigDecimal cantidadFGEM, EstilosFGEM estilosFGEM) {
            Row rowFGEM = hojaFGEM.createRow(filaFGEM);
            fechaFGEM(rowFGEM, 0, diaFGEM, estilosFGEM);
            rowFGEM.createCell(1).setCellValue(comprobanteFGEM);
            rowFGEM.createCell(2).setCellValue(proveedorFGEM);
            if (rucFGEM != null) {
                rowFGEM.createCell(3).setCellValue(rucFGEM);
            }
            rowFGEM.createCell(4).setCellValue(pFGEM.codigoFGEM());
            rowFGEM.createCell(5).setCellValue(cantidadFGEM.doubleValue());
            montoFGEM(rowFGEM, 6, pFGEM.costoFGEM(), estilosFGEM);
            rowFGEM.createCell(7).setCellValue(0);
            return filaFGEM + 1;
        }

        private ProductoEjemploFGEM elegirFGEM() {
            int totalFGEM = PRODUCTOS_FGEM.stream().mapToInt(ProductoEjemploFGEM::popularidadFGEM).sum();
            int puntoFGEM = azarFGEM.nextInt(totalFGEM);
            for (ProductoEjemploFGEM pFGEM : PRODUCTOS_FGEM) {
                puntoFGEM -= pFGEM.popularidadFGEM();
                if (puntoFGEM < 0) {
                    return pFGEM;
                }
            }
            return PRODUCTOS_FGEM.get(0);
        }

        private void escribirGastosFGEM(Sheet hojaFGEM, EstilosFGEM estilosFGEM) {
            int filaFGEM = 1;
            for (YearMonth mesFGEM = YearMonth.from(desdeFGEM); !mesFGEM.isAfter(YearMonth.from(hastaFGEM)); mesFGEM = mesFGEM.plusMonths(1)) {
                String nombreMesFGEM = mesFGEM.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.forLanguageTag("es"));
                filaFGEM = gastoFGEM(hojaFGEM, filaFGEM, mesFGEM.atDay(1), CategoriaFinancieraFGEM.ALQUILER, "Alquiler del local – " + nombreMesFGEM, "800", estilosFGEM);
                filaFGEM = gastoFGEM(hojaFGEM, filaFGEM, mesFGEM.atDay(10), CategoriaFinancieraFGEM.SERVICIOS_BASICOS, "Recibo de luz",
                        String.valueOf(165 + azarFGEM.nextInt(30)), estilosFGEM);
                filaFGEM = gastoFGEM(hojaFGEM, filaFGEM, mesFGEM.atDay(12), CategoriaFinancieraFGEM.SERVICIOS_BASICOS, "Recibo de agua", "45", estilosFGEM);
                filaFGEM = gastoFGEM(hojaFGEM, filaFGEM, mesFGEM.atDay(15), CategoriaFinancieraFGEM.SERVICIOS_BASICOS, "Internet y celular", "70", estilosFGEM);
                filaFGEM = gastoFGEM(hojaFGEM, filaFGEM, mesFGEM.atDay(15), CategoriaFinancieraFGEM.TRIBUTOS, "Cuota Nuevo RUS", "20", estilosFGEM);
                filaFGEM = gastoFGEM(hojaFGEM, filaFGEM, mesFGEM.atDay(20), CategoriaFinancieraFGEM.EMPAQUES, "Bolsas plásticas",
                        String.valueOf(50 + azarFGEM.nextInt(20)), estilosFGEM);
                filaFGEM = gastoFGEM(hojaFGEM, filaFGEM, mesFGEM.atEndOfMonth(), CategoriaFinancieraFGEM.SUELDOS, "Sueldo del ayudante", "1130", estilosFGEM);
            }
        }

        private static int gastoFGEM(Sheet hojaFGEM, int filaFGEM, LocalDate fechaFGEM, CategoriaFinancieraFGEM categoriaFGEM, String descripcionFGEM,
                                     String montoFGEM, EstilosFGEM estilosFGEM) {
            Row rowFGEM = hojaFGEM.createRow(filaFGEM);
            fechaFGEM(rowFGEM, 0, fechaFGEM, estilosFGEM);
            rowFGEM.createCell(1).setCellValue(categoriaFGEM.descripcionFGEM());
            rowFGEM.createCell(2).setCellValue(descripcionFGEM);
            montoFGEM(rowFGEM, 3, montoFGEM, estilosFGEM);
            return filaFGEM + 1;
        }

        private static void fechaFGEM(Row rowFGEM, int columnaFGEM, LocalDate fechaFGEM, EstilosFGEM estilosFGEM) {
            var celdaFGEM = rowFGEM.createCell(columnaFGEM);
            celdaFGEM.setCellValue(fechaFGEM);
            celdaFGEM.setCellStyle(estilosFGEM.fechaFGEM);
        }

        private static void montoFGEM(Row rowFGEM, int columnaFGEM, String montoFGEM, EstilosFGEM estilosFGEM) {
            var celdaFGEM = rowFGEM.createCell(columnaFGEM);
            celdaFGEM.setCellValue(Double.parseDouble(montoFGEM));
            celdaFGEM.setCellStyle(estilosFGEM.montoFGEM);
        }
    }
}
