package com.businessanalyzer.importacion;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.importacion.DatosImportacionFGEM.ContenidoFGEM;
import com.businessanalyzer.importacion.DatosImportacionFGEM.ErrorFilaFGEM;
import com.businessanalyzer.importacion.DatosImportacionFGEM.FilaMovimientoFGEM;
import com.businessanalyzer.importacion.DatosImportacionFGEM.FilaOperacionFGEM;
import com.businessanalyzer.importacion.DatosImportacionFGEM.FilaProductoFGEM;
import com.businessanalyzer.importacion.PlantillaImportacionFGEM.ColumnaFGEM;
import com.businessanalyzer.importacion.PlantillaImportacionFGEM.HojaFGEM;
import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.ComportamientoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lee un libro de importación (.xlsx o .xls) y convierte cada fila en datos de Java, anotando los
 * errores de formato con su hoja y fila. No toca la base de datos: las reglas de negocio (stock,
 * códigos repetidos, fechas futuras…) las aplica después el servicio de importación.
 */
public final class LectorImportacionFGEM {

    /** Más filas que esto en una hoja probablemente no son datos de un negocio pequeño. */
    static final int MAXIMO_FILAS_FGEM = 50_000;
    private static final long TAMANO_MAXIMO_FGEM = 30L * 1024 * 1024;
    private static final List<DateTimeFormatter> FORMATOS_FECHA_FGEM = List.of(
            DateTimeFormatter.ofPattern("d/M/uuuu"), DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ofPattern("uuuu-M-d"), DateTimeFormatter.ofPattern("d.M.uuuu"),
            DateTimeFormatter.ofPattern("d/M/uu"));

    /** Palabras frecuentes de un gasto y su categoría (se buscan dentro del texto escrito). */
    private static final Map<String, CategoriaFinancieraFGEM> PALABRAS_GASTO_FGEM = new LinkedHashMap<>();

    static {
        for (String pFGEM : List.of("sueldo", "salario", "planilla", "personal", "ayudante", "trabajador")) {
            PALABRAS_GASTO_FGEM.put(pFGEM, CategoriaFinancieraFGEM.SUELDOS);
        }
        for (String pFGEM : List.of("luz", "agua", "internet", "telefono", "celular", "electricidad", "cable", "servicios basicos")) {
            PALABRAS_GASTO_FGEM.put(pFGEM, CategoriaFinancieraFGEM.SERVICIOS_BASICOS);
        }
        PALABRAS_GASTO_FGEM.put("alquiler", CategoriaFinancieraFGEM.ALQUILER);
        PALABRAS_GASTO_FGEM.put("renta del local", CategoriaFinancieraFGEM.ALQUILER);
        PALABRAS_GASTO_FGEM.put("mantenimiento", CategoriaFinancieraFGEM.MANTENIMIENTO);
        PALABRAS_GASTO_FGEM.put("reparacion", CategoriaFinancieraFGEM.MANTENIMIENTO);
        PALABRAS_GASTO_FGEM.put("seguro", CategoriaFinancieraFGEM.SEGUROS);
        for (String pFGEM : List.of("contador", "contabilidad", "asesoria")) {
            PALABRAS_GASTO_FGEM.put(pFGEM, CategoriaFinancieraFGEM.SERVICIOS_PROFESIONALES);
        }
        for (String pFGEM : List.of("rus", "tributo", "arbitrio", "licencia", "municipal", "sunat", "impuesto")) {
            PALABRAS_GASTO_FGEM.put(pFGEM, CategoriaFinancieraFGEM.TRIBUTOS);
        }
        PALABRAS_GASTO_FGEM.put("depreciacion", CategoriaFinancieraFGEM.DEPRECIACION);
        for (String pFGEM : List.of("publicidad", "marketing", "volante", "letrero")) {
            PALABRAS_GASTO_FGEM.put(pFGEM, CategoriaFinancieraFGEM.MARKETING);
        }
        for (String pFGEM : List.of("transporte", "flete", "movilidad", "taxi", "pasaje", "combustible", "gasolina")) {
            PALABRAS_GASTO_FGEM.put(pFGEM, CategoriaFinancieraFGEM.TRANSPORTE);
        }
        PALABRAS_GASTO_FGEM.put("comision bancaria", CategoriaFinancieraFGEM.COMISIONES_BANCARIAS);
        PALABRAS_GASTO_FGEM.put("banco", CategoriaFinancieraFGEM.COMISIONES_BANCARIAS);
        PALABRAS_GASTO_FGEM.put("comision", CategoriaFinancieraFGEM.COMISIONES_VENTA);
        for (String pFGEM : List.of("bolsa", "empaque", "suministro", "envase")) {
            PALABRAS_GASTO_FGEM.put(pFGEM, CategoriaFinancieraFGEM.EMPAQUES);
        }
        PALABRAS_GASTO_FGEM.put("interes", CategoriaFinancieraFGEM.INTERESES_PRESTAMOS);
        PALABRAS_GASTO_FGEM.put("otro", CategoriaFinancieraFGEM.OTROS_GASTOS);
    }

    private final List<FilaProductoFGEM> productosFGEM = new ArrayList<>();
    private final List<FilaOperacionFGEM> operacionesFGEM = new ArrayList<>();
    private final List<FilaMovimientoFGEM> movimientosFGEM = new ArrayList<>();
    private final List<ErrorFilaFGEM> erroresFGEM = new ArrayList<>();

    private LectorImportacionFGEM() {
    }

    /**
     * Lee el archivo completo.
     *
     * @throws ValidacionExceptionFGEM si el archivo no se puede abrir o no tiene ninguna hoja reconocible
     */
    public static ContenidoFGEM leerFGEM(Path archivoFGEM) {
        try {
            if (!Files.isRegularFile(archivoFGEM)) {
                throw new ValidacionExceptionFGEM("No se encontró el archivo " + archivoFGEM.getFileName() + ".");
            }
            if (Files.size(archivoFGEM) > TAMANO_MAXIMO_FGEM) {
                throw new ValidacionExceptionFGEM("El archivo pesa más de 30 MB. Divide los datos en varios archivos.");
            }
        } catch (IOException eFGEM) {
            throw new ValidacionExceptionFGEM("No se pudo leer el archivo: " + eFGEM.getMessage());
        }
        try (InputStream entradaFGEM = Files.newInputStream(archivoFGEM);
             Workbook libroFGEM = WorkbookFactory.create(entradaFGEM)) {
            LectorImportacionFGEM lectorFGEM = new LectorImportacionFGEM();
            lectorFGEM.leerLibroFGEM(libroFGEM);
            return new ContenidoFGEM(List.copyOf(lectorFGEM.productosFGEM), List.copyOf(lectorFGEM.operacionesFGEM),
                    List.copyOf(lectorFGEM.movimientosFGEM), List.copyOf(lectorFGEM.erroresFGEM));
        } catch (EncryptedDocumentException eFGEM) {
            throw new ValidacionExceptionFGEM("El archivo tiene contraseña. Guárdalo sin contraseña e inténtalo de nuevo.");
        } catch (IOException | RuntimeException eFGEM) {
            if (eFGEM instanceof ValidacionExceptionFGEM vFGEM) {
                throw vFGEM;
            }
            throw new ValidacionExceptionFGEM("El archivo no es un libro de Excel válido (.xlsx o .xls). "
                    + "Si está abierto en Excel, ciérralo e inténtalo de nuevo.");
        }
    }

    private void leerLibroFGEM(Workbook libroFGEM) {
        Map<String, Sheet> porNombreFGEM = new HashMap<>();
        for (Sheet hojaFGEM : libroFGEM) {
            porNombreFGEM.putIfAbsent(PlantillaImportacionFGEM.normalizarFGEM(hojaFGEM.getSheetName()), hojaFGEM);
        }
        boolean algunaFGEM = false;
        for (HojaFGEM hFGEM : HojaFGEM.values()) {
            Sheet hojaFGEM = porNombreFGEM.get(PlantillaImportacionFGEM.normalizarFGEM(hFGEM.nombreFGEM()));
            if (hojaFGEM == null && hFGEM == HojaFGEM.GASTOS) {
                hojaFGEM = porNombreFGEM.get("gastos e ingresos");
            }
            if (hojaFGEM != null) {
                algunaFGEM = true;
                leerHojaFGEM(hFGEM, hojaFGEM, libroFGEM);
            }
        }
        if (!algunaFGEM) {
            throw new ValidacionExceptionFGEM("El archivo no tiene ninguna de las hojas «Productos», «Ventas», «Compras» o «Gastos». "
                    + "Descarga la plantilla desde la aplicación y copia ahí tus datos.");
        }
    }

    private void leerHojaFGEM(HojaFGEM hFGEM, Sheet hojaFGEM, Workbook libroFGEM) {
        Row encabezadoFGEM = hojaFGEM.getRow(hojaFGEM.getFirstRowNum());
        if (encabezadoFGEM == null || hojaFGEM.getPhysicalNumberOfRows() == 0) {
            return;
        }
        Map<ColumnaFGEM, Integer> indicesFGEM = new HashMap<>();
        Map<String, Integer> encabezadosFGEM = new HashMap<>();
        for (Cell celdaFGEM : encabezadoFGEM) {
            String textoFGEM = PlantillaImportacionFGEM.claveEncabezadoFGEM(textoCrudoFGEM(celdaFGEM));
            if (!textoFGEM.isEmpty()) {
                encabezadosFGEM.putIfAbsent(textoFGEM, celdaFGEM.getColumnIndex());
            }
        }
        List<String> faltantesFGEM = new ArrayList<>();
        for (ColumnaFGEM cFGEM : hFGEM.columnasFGEM()) {
            cFGEM.nombresAceptadosFGEM().stream().filter(encabezadosFGEM::containsKey).findFirst()
                    .ifPresent(nFGEM -> indicesFGEM.put(cFGEM, encabezadosFGEM.get(nFGEM)));
            if (cFGEM.obligatoriaFGEM() && !indicesFGEM.containsKey(cFGEM)) {
                faltantesFGEM.add("«" + cFGEM.tituloFGEM() + "»");
            }
        }
        int primeraFGEM = encabezadoFGEM.getRowNum() + 1;
        boolean conDatosFGEM = false;
        for (int iFGEM = primeraFGEM; iFGEM <= hojaFGEM.getLastRowNum(); iFGEM++) {
            if (!filaVaciaFGEM(hojaFGEM.getRow(iFGEM))) {
                conDatosFGEM = true;
                break;
            }
        }
        if (!conDatosFGEM) {
            return;
        }
        if (!faltantesFGEM.isEmpty()) {
            erroresFGEM.add(new ErrorFilaFGEM(hFGEM.nombreFGEM(), 0, "Falta la columna " + String.join(", ", faltantesFGEM)
                    + " en la primera fila. Usa los encabezados de la plantilla."));
            return;
        }
        if (hojaFGEM.getLastRowNum() - primeraFGEM + 1 > MAXIMO_FILAS_FGEM) {
            erroresFGEM.add(new ErrorFilaFGEM(hFGEM.nombreFGEM(), 0, "Tiene más de " + MAXIMO_FILAS_FGEM
                    + " filas. Divide los datos en varios archivos."));
            return;
        }
        var evaluadorFGEM = libroFGEM.getCreationHelper().createFormulaEvaluator();
        for (int iFGEM = primeraFGEM; iFGEM <= hojaFGEM.getLastRowNum(); iFGEM++) {
            Row rowFGEM = hojaFGEM.getRow(iFGEM);
            if (filaVaciaFGEM(rowFGEM)) {
                continue;
            }
            FilaExcelFGEM filaFGEM = new FilaExcelFGEM(hFGEM, rowFGEM, indicesFGEM, evaluadorFGEM);
            try {
                switch (hFGEM) {
                    case PRODUCTOS -> productosFGEM.add(productoFGEM(filaFGEM));
                    case VENTAS -> operacionesFGEM.add(operacionFGEM(filaFGEM, TipoOperacionFGEM.VENTA));
                    case COMPRAS -> operacionesFGEM.add(operacionFGEM(filaFGEM, TipoOperacionFGEM.COMPRA));
                    case GASTOS -> movimientosFGEM.add(movimientoFGEM(filaFGEM));
                }
            } catch (ValorInvalidoFGEM eFGEM) {
                erroresFGEM.add(new ErrorFilaFGEM(hFGEM.nombreFGEM(), filaFGEM.numeroFGEM(), eFGEM.getMessage()));
            }
        }
    }

    // ------------------------------------------------------------------ conversión de cada hoja

    private static FilaProductoFGEM productoFGEM(FilaExcelFGEM fFGEM) {
        String nombreFGEM = fFGEM.textoFGEM(1, true);
        TipoItemFGEM tipoFGEM = tipoItemFGEM(fFGEM.textoFGEM(2, false));
        String unidadTextoFGEM = fFGEM.textoFGEM(4, false);
        UnidadMedidaFGEM unidadFGEM = unidadTextoFGEM == null
                ? (tipoFGEM == TipoItemFGEM.SERVICIO ? UnidadMedidaFGEM.SERVICIO : UnidadMedidaFGEM.UNIDAD)
                : unidadFGEM(unidadTextoFGEM);
        return new FilaProductoFGEM(fFGEM.numeroFGEM(), fFGEM.textoFGEM(0, false), nombreFGEM, tipoFGEM, fFGEM.textoFGEM(3, false), unidadFGEM,
                fFGEM.decimalFGEM(5, true), fFGEM.decimalFGEM(6, true));
    }

    private static FilaOperacionFGEM operacionFGEM(FilaExcelFGEM fFGEM, TipoOperacionFGEM tipoFGEM) {
        boolean ventaFGEM = tipoFGEM == TipoOperacionFGEM.VENTA;
        // Ventas: Fecha, Documento, Cliente, Producto, Cantidad, Precio, IGV
        // Compras: Fecha, Comprobante, Proveedor, RUC, Producto, Cantidad, Costo, IGV
        int desplazamientoFGEM = ventaFGEM ? 0 : 1;
        LocalDate fechaFGEM = fFGEM.fechaFGEM(0);
        String documentoFGEM = fFGEM.textoFGEM(1, false);
        String contraparteFGEM = fFGEM.textoFGEM(2, false);
        String rucFGEM = ventaFGEM ? null : fFGEM.textoFGEM(3, false);
        String productoFGEM = fFGEM.textoFGEM(3 + desplazamientoFGEM, true);
        BigDecimal cantidadFGEM = fFGEM.decimalFGEM(4 + desplazamientoFGEM, true);
        if (cantidadFGEM.signum() <= 0) {
            throw new ValorInvalidoFGEM("La cantidad debe ser mayor que cero.");
        }
        BigDecimal precioFGEM = fFGEM.decimalFGEM(5 + desplazamientoFGEM, false);
        BigDecimal tasaFGEM = fFGEM.porcentajeFGEM(6 + desplazamientoFGEM);
        return new FilaOperacionFGEM(fFGEM.numeroFGEM(), tipoFGEM, fechaFGEM, documentoFGEM, contraparteFGEM, rucFGEM, productoFGEM,
                cantidadFGEM, precioFGEM, tasaFGEM);
    }

    private static FilaMovimientoFGEM movimientoFGEM(FilaExcelFGEM fFGEM) {
        LocalDate fechaFGEM = fFGEM.fechaFGEM(0);
        CategoriaFinancieraFGEM categoriaFGEM = categoriaFinancieraFGEM(fFGEM.textoFGEM(1, true));
        String descripcionFGEM = fFGEM.textoFGEM(2, true);
        BigDecimal montoFGEM = fFGEM.decimalFGEM(3, true);
        String comportamientoTextoFGEM = fFGEM.textoFGEM(4, false);
        ComportamientoFGEM comportamientoFGEM = comportamientoTextoFGEM == null ? null : comportamientoFGEM(comportamientoTextoFGEM);
        return new FilaMovimientoFGEM(fFGEM.numeroFGEM(), fechaFGEM, categoriaFGEM, descripcionFGEM, montoFGEM, comportamientoFGEM,
                fFGEM.textoFGEM(5, false));
    }

    // ------------------------------------------------------------------ valores con nombre

    static TipoItemFGEM tipoItemFGEM(String textoFGEM) {
        String nFGEM = PlantillaImportacionFGEM.normalizarFGEM(textoFGEM);
        return switch (nFGEM) {
            case "", "producto", "productos", "p", "bien" -> TipoItemFGEM.PRODUCTO;
            case "servicio", "servicios", "s" -> TipoItemFGEM.SERVICIO;
            default -> throw new ValorInvalidoFGEM("Tipo «" + textoFGEM + "» no válido: escribe Producto o Servicio.");
        };
    }

    static UnidadMedidaFGEM unidadFGEM(String textoFGEM) {
        String nFGEM = PlantillaImportacionFGEM.normalizarFGEM(textoFGEM);
        for (String candidatoFGEM : List.of(nFGEM, nFGEM.replaceAll("(es|s)$", ""))) {
            for (UnidadMedidaFGEM uFGEM : UnidadMedidaFGEM.values()) {
                if (candidatoFGEM.equals(PlantillaImportacionFGEM.normalizarFGEM(uFGEM.descripcionFGEM()))
                        || candidatoFGEM.equals(PlantillaImportacionFGEM.normalizarFGEM(uFGEM.abreviaturaFGEM()))
                        || candidatoFGEM.equals(PlantillaImportacionFGEM.normalizarFGEM(uFGEM.name()))) {
                    return uFGEM;
                }
            }
        }
        return switch (nFGEM) {
            case "u", "un", "unid", "uni", "pieza", "piezas", "botella", "botellas", "lata", "latas", "bolsa", "bolsas" -> UnidadMedidaFGEM.UNIDAD;
            case "kilo", "kilos", "kgs" -> UnidadMedidaFGEM.KILOGRAMO;
            case "lt", "lts" -> UnidadMedidaFGEM.LITRO;
            case "pqt", "pack" -> UnidadMedidaFGEM.PAQUETE;
            default -> throw new ValorInvalidoFGEM("Unidad «" + textoFGEM + "» no reconocida. Usa una de la lista: Unidad, Kilogramo, "
                    + "Litro, Paquete, Caja, Docena…");
        };
    }

    static CategoriaFinancieraFGEM categoriaFinancieraFGEM(String textoFGEM) {
        String nFGEM = PlantillaImportacionFGEM.normalizarFGEM(textoFGEM);
        for (CategoriaFinancieraFGEM cFGEM : CategoriaFinancieraFGEM.values()) {
            if (nFGEM.equals(PlantillaImportacionFGEM.normalizarFGEM(cFGEM.descripcionFGEM()))
                    || nFGEM.equals(PlantillaImportacionFGEM.normalizarFGEM(cFGEM.name()))) {
                return cFGEM;
            }
        }
        String conEspaciosFGEM = " " + nFGEM + " ";
        for (Map.Entry<String, CategoriaFinancieraFGEM> eFGEM : PALABRAS_GASTO_FGEM.entrySet()) {
            // Palabra completa o comienzo de palabra: «sueldos» contiene «sueldo», pero «agua» no está dentro de «paraguas».
            if (conEspaciosFGEM.contains(" " + eFGEM.getKey())) {
                return eFGEM.getValue();
            }
        }
        throw new ValorInvalidoFGEM("Categoría «" + textoFGEM + "» no reconocida. Elige una de la lista desplegable "
                + "(por ejemplo: Alquiler de local, Sueldos y salarios, Luz, agua, internet y teléfono).");
    }

    static ComportamientoFGEM comportamientoFGEM(String textoFGEM) {
        return switch (PlantillaImportacionFGEM.normalizarFGEM(textoFGEM)) {
            case "fijo", "f" -> ComportamientoFGEM.FIJO;
            case "variable", "v" -> ComportamientoFGEM.VARIABLE;
            default -> throw new ValorInvalidoFGEM("«" + textoFGEM + "» no válido: escribe Fijo o Variable.");
        };
    }

    // ------------------------------------------------------------------ celdas

    private static boolean filaVaciaFGEM(Row rowFGEM) {
        if (rowFGEM == null) {
            return true;
        }
        for (Cell celdaFGEM : rowFGEM) {
            String textoFGEM = textoCrudoFGEM(celdaFGEM);
            if (textoFGEM != null && !textoFGEM.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String textoCrudoFGEM(Cell celdaFGEM) {
        if (celdaFGEM == null) {
            return null;
        }
        CellType tipoFGEM = celdaFGEM.getCellType() == CellType.FORMULA ? celdaFGEM.getCachedFormulaResultType() : celdaFGEM.getCellType();
        return switch (tipoFGEM) {
            case STRING -> celdaFGEM.getStringCellValue();
            case NUMERIC -> String.valueOf(celdaFGEM.getNumericCellValue());
            case BOOLEAN -> String.valueOf(celdaFGEM.getBooleanCellValue());
            default -> null;
        };
    }

    /** Valor de una celda que no se puede usar; el mensaje se muestra tal cual al usuario. */
    static final class ValorInvalidoFGEM extends RuntimeException {
        ValorInvalidoFGEM(String mensajeFGEM) {
            super(mensajeFGEM, null, false, false);
        }
    }

    /** Acceso a las celdas de una fila por posición de columna en la plantilla. */
    private static final class FilaExcelFGEM {
        private final HojaFGEM hojaFGEM;
        private final Row rowFGEM;
        private final Map<ColumnaFGEM, Integer> indicesFGEM;
        private final org.apache.poi.ss.usermodel.FormulaEvaluator evaluadorFGEM;

        FilaExcelFGEM(HojaFGEM hojaFGEM, Row rowFGEM, Map<ColumnaFGEM, Integer> indicesFGEM,
                      org.apache.poi.ss.usermodel.FormulaEvaluator evaluadorFGEM) {
            this.hojaFGEM = hojaFGEM;
            this.rowFGEM = rowFGEM;
            this.indicesFGEM = indicesFGEM;
            this.evaluadorFGEM = evaluadorFGEM;
        }

        int numeroFGEM() {
            return rowFGEM.getRowNum() + 1;
        }

        private ColumnaFGEM columnaFGEM(int posicionFGEM) {
            return hojaFGEM.columnasFGEM().get(posicionFGEM);
        }

        private Cell celdaFGEM(int posicionFGEM) {
            Integer indiceFGEM = indicesFGEM.get(columnaFGEM(posicionFGEM));
            return indiceFGEM == null ? null : rowFGEM.getCell(indiceFGEM);
        }

        private CellType tipoFGEM(Cell celdaFGEM) {
            if (celdaFGEM == null) {
                return CellType.BLANK;
            }
            if (celdaFGEM.getCellType() == CellType.FORMULA) {
                try {
                    return evaluadorFGEM.evaluateFormulaCell(celdaFGEM);
                } catch (RuntimeException eFGEM) {
                    return celdaFGEM.getCachedFormulaResultType();
                }
            }
            return celdaFGEM.getCellType();
        }

        private ValorInvalidoFGEM faltaFGEM(int posicionFGEM) {
            return new ValorInvalidoFGEM("Falta «" + columnaFGEM(posicionFGEM).tituloFGEM() + "».");
        }

        String textoFGEM(int posicionFGEM, boolean obligatorioFGEM) {
            Cell celdaFGEM = celdaFGEM(posicionFGEM);
            String textoFGEM = switch (tipoFGEM(celdaFGEM)) {
                case STRING -> celdaFGEM.getStringCellValue();
                // Códigos o RUC escritos como número: 7751234 y no 7751234.0
                case NUMERIC -> BigDecimal.valueOf(celdaFGEM.getNumericCellValue()).stripTrailingZeros().toPlainString();
                case BOOLEAN -> String.valueOf(celdaFGEM.getBooleanCellValue());
                case ERROR -> throw new ValorInvalidoFGEM("«" + columnaFGEM(posicionFGEM).tituloFGEM() + "» tiene un error de fórmula.");
                default -> null;
            };
            textoFGEM = textoFGEM == null ? null : textoFGEM.replace(' ', ' ').trim();
            if (textoFGEM == null || textoFGEM.isEmpty()) {
                if (obligatorioFGEM) {
                    throw faltaFGEM(posicionFGEM);
                }
                return null;
            }
            return textoFGEM;
        }

        BigDecimal decimalFGEM(int posicionFGEM, boolean obligatorioFGEM) {
            Cell celdaFGEM = celdaFGEM(posicionFGEM);
            String tituloFGEM = columnaFGEM(posicionFGEM).tituloFGEM();
            BigDecimal valorFGEM = switch (tipoFGEM(celdaFGEM)) {
                case NUMERIC -> BigDecimal.valueOf(celdaFGEM.getNumericCellValue());
                case STRING -> numeroDesdeTextoFGEM(celdaFGEM.getStringCellValue(), tituloFGEM);
                case ERROR -> throw new ValorInvalidoFGEM("«" + tituloFGEM + "» tiene un error de fórmula.");
                default -> null;
            };
            if (valorFGEM == null) {
                if (obligatorioFGEM) {
                    throw faltaFGEM(posicionFGEM);
                }
                return null;
            }
            if (valorFGEM.signum() < 0) {
                throw new ValorInvalidoFGEM("«" + tituloFGEM + "» no puede ser negativo.");
            }
            // Los cálculos de Excel dejan decimales binarios (0.1 + 0.2 = 0.30000000000000004).
            return valorFGEM.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
        }

        /** Acepta 18, «18 %» o una celda con formato de porcentaje (0,18). */
        BigDecimal porcentajeFGEM(int posicionFGEM) {
            Cell celdaFGEM = celdaFGEM(posicionFGEM);
            if (tipoFGEM(celdaFGEM) == CellType.NUMERIC && celdaFGEM.getCellStyle().getDataFormatString().contains("%")) {
                return BigDecimal.valueOf(celdaFGEM.getNumericCellValue()).movePointRight(2).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
            }
            BigDecimal valorFGEM = decimalFGEM(posicionFGEM, false);
            return valorFGEM == null ? null : valorFGEM.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        }

        LocalDate fechaFGEM(int posicionFGEM) {
            Cell celdaFGEM = celdaFGEM(posicionFGEM);
            String tituloFGEM = columnaFGEM(posicionFGEM).tituloFGEM();
            return switch (tipoFGEM(celdaFGEM)) {
                case NUMERIC -> {
                    double serialFGEM = celdaFGEM.getNumericCellValue();
                    if (!DateUtil.isValidExcelDate(serialFGEM) || serialFGEM < 1) {
                        throw new ValorInvalidoFGEM("«" + tituloFGEM + "» no es una fecha válida.");
                    }
                    yield DateUtil.getLocalDateTime(serialFGEM).toLocalDate();
                }
                case STRING -> fechaDesdeTextoFGEM(celdaFGEM.getStringCellValue(), tituloFGEM);
                case BLANK -> throw faltaFGEM(posicionFGEM);
                default -> throw new ValorInvalidoFGEM("«" + tituloFGEM + "» no es una fecha válida.");
            };
        }
    }

    static BigDecimal numeroDesdeTextoFGEM(String textoFGEM, String tituloFGEM) {
        String limpioFGEM = textoFGEM.replace(' ', ' ').replaceAll("(?i)s/\\.?|soles|us\\$|\\$|%|\\s", "");
        if (limpioFGEM.isEmpty() || limpioFGEM.equals("-")) {
            return null;
        }
        int comaFGEM = limpioFGEM.lastIndexOf(',');
        int puntoFGEM = limpioFGEM.lastIndexOf('.');
        if (comaFGEM >= 0 && puntoFGEM >= 0) {
            // El último separador es el decimal: 1.234,50 o 1,234.50
            limpioFGEM = comaFGEM > puntoFGEM ? limpioFGEM.replace(".", "").replace(',', '.') : limpioFGEM.replace(",", "");
        } else if (comaFGEM >= 0) {
            limpioFGEM = limpioFGEM.indexOf(',') == comaFGEM ? limpioFGEM.replace(',', '.') : limpioFGEM.replace(",", "");
        }
        try {
            return new BigDecimal(limpioFGEM);
        } catch (NumberFormatException eFGEM) {
            throw new ValorInvalidoFGEM("«" + tituloFGEM + "» debe ser un número (se leyó «" + textoFGEM.trim() + "»).");
        }
    }

    static LocalDate fechaDesdeTextoFGEM(String textoFGEM, String tituloFGEM) {
        String limpioFGEM = textoFGEM.trim();
        if (limpioFGEM.isEmpty()) {
            throw new ValorInvalidoFGEM("Falta «" + tituloFGEM + "».");
        }
        for (DateTimeFormatter formatoFGEM : FORMATOS_FECHA_FGEM) {
            try {
                LocalDate fechaFGEM = LocalDate.parse(limpioFGEM, formatoFGEM);
                return fechaFGEM.getYear() < 100 ? fechaFGEM.plusYears(2000) : fechaFGEM;
            } catch (DateTimeParseException eFGEM) {
                // probar el siguiente formato
            }
        }
        throw new ValorInvalidoFGEM("«" + tituloFGEM + "» no es una fecha válida (se leyó «" + limpioFGEM + "»). Usa dd/mm/aaaa.");
    }
}
