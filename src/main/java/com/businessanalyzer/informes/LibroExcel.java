package com.businessanalyzer.informes;

import com.businessanalyzer.exception.InformeException;
import com.businessanalyzer.model.Moneda;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Libro de Excel (.xlsx) con formato: títulos, encabezados resaltados, montos con el símbolo de la
 * moneda, porcentajes, fechas, filtros automáticos y la fila de encabezados inmovilizada.
 * <p>
 * Los montos y porcentajes se guardan como números (no como texto), para que el usuario pueda
 * sumarlos, filtrarlos o graficarlos en Excel.
 */
public final class LibroExcel implements AutoCloseable {

    /** Porcentaje expresado como fracción (0.125 = 12.5 %). */
    public record Porcentaje(BigDecimal valor) {
    }

    /** Cantidad (no monetaria). */
    public record Cantidad(BigDecimal valor) {
    }

    /** Fórmula de Excel sin el signo "=" inicial. */
    public record Formula(String expresion) {
    }

    private static final int ANCHO_MAXIMO = 60 * 256;

    private final XSSFWorkbook libro = new XSSFWorkbook();
    private final CellStyle titulo;
    private final CellStyle subtitulo;
    private final CellStyle encabezado;
    private final CellStyle texto;
    private final CellStyle dinero;
    private final CellStyle porcentaje;
    private final CellStyle cantidad;
    private final CellStyle fecha;
    private final CellStyle totalTexto;
    private final CellStyle totalDinero;
    private final CellStyle totalPorcentaje;

    public LibroExcel(Moneda moneda) {
        String formatoDinero = "\"" + moneda.simbolo() + "\" #,##0.00;[Red]-\"" + moneda.simbolo() + "\" #,##0.00";

        Font negrita = libro.createFont();
        negrita.setBold(true);
        Font fuenteTitulo = libro.createFont();
        fuenteTitulo.setBold(true);
        fuenteTitulo.setFontHeightInPoints((short) 14);
        fuenteTitulo.setColor(IndexedColors.DARK_BLUE.getIndex());
        Font fuenteSubtitulo = libro.createFont();
        fuenteSubtitulo.setItalic(true);
        fuenteSubtitulo.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        Font fuenteEncabezado = libro.createFont();
        fuenteEncabezado.setBold(true);
        fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());

        titulo = libro.createCellStyle();
        titulo.setFont(fuenteTitulo);
        subtitulo = libro.createCellStyle();
        subtitulo.setFont(fuenteSubtitulo);
        encabezado = libro.createCellStyle();
        encabezado.setFont(fuenteEncabezado);
        encabezado.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        encabezado.setWrapText(true);
        texto = libro.createCellStyle();
        dinero = conFormato(formatoDinero);
        porcentaje = conFormato("0.0%");
        cantidad = conFormato("#,##0.##");
        fecha = conFormato("dd/mm/yyyy");
        totalTexto = total(null, negrita);
        totalDinero = total(formatoDinero, negrita);
        totalPorcentaje = total("0.0%", negrita);
    }

    public Hoja hoja(String nombre) {
        return new Hoja(libro.createSheet(WorkbookUtil.createSafeSheetName(nombre)));
    }

    /** Guarda el libro en {@code destino} (primero en un temporal, para no dejar archivos a medias). */
    public void guardar(Path destino) {
        Path temporal = destino.resolveSibling(destino.getFileName() + ".tmp");
        try (OutputStream salida = Files.newOutputStream(temporal)) {
            libro.write(salida);
        } catch (IOException e) {
            Escritura.borrar(temporal);
            throw new InformeException("No se pudo escribir el libro de Excel.", e);
        }
        Escritura.reemplazar(temporal, destino);
    }

    @Override
    public void close() {
        try {
            libro.close();
        } catch (IOException ignorado) {
            // Libro en memoria: no hay recursos externos que liberar.
        }
    }

    private CellStyle conFormato(String formato) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setDataFormat(libro.createDataFormat().getFormat(formato));
        return estilo;
    }

    private CellStyle total(String formato, Font negrita) {
        CellStyle estilo = formato == null ? libro.createCellStyle() : conFormato(formato);
        estilo.setFont(negrita);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    /** Una hoja del libro. Las filas se agregan de arriba hacia abajo. */
    public final class Hoja {

        private final Sheet hoja;
        private int filaActual;
        private int filaEncabezado = -1;
        private int primeraFilaDatos = -1;
        private int columnas;

        private Hoja(Sheet hoja) {
            this.hoja = hoja;
        }

        public Hoja titulo(String texto, String detalle) {
            celdaTexto(hoja.createRow(filaActual++), 0, texto, titulo);
            if (detalle != null) {
                celdaTexto(hoja.createRow(filaActual++), 0, detalle, subtitulo);
            }
            filaActual++;
            return this;
        }

        public Hoja encabezados(String... nombres) {
            Row fila = hoja.createRow(filaActual);
            fila.setHeightInPoints(20);
            for (int i = 0; i < nombres.length; i++) {
                celdaTexto(fila, i, nombres[i], encabezado);
            }
            filaEncabezado = filaActual++;
            primeraFilaDatos = filaActual;
            columnas = Math.max(columnas, nombres.length);
            return this;
        }

        /** Fila de datos. Los valores pueden ser texto, números, montos, fechas, porcentajes o fórmulas. */
        public Hoja fila(Object... valores) {
            escribir(valores, false);
            return this;
        }

        /** Fila de totales resaltada (no entra en el filtro automático). */
        public Hoja total(Object... valores) {
            escribir(valores, true);
            return this;
        }

        public Hoja espacio() {
            filaActual++;
            return this;
        }

        /** Suma de una columna de datos solo en las filas donde otras dos columnas tienen los valores indicados. */
        public Formula sumaSi(int columna, int columna1, String valor1, int columna2, String valor2) {
            if (primeraFilaDatos < 0 || filaActual <= primeraFilaDatos) {
                return new Formula("0");
            }
            return new Formula("SUMIFS(" + rango(columna) + "," + rango(columna1) + ",\"" + valor1 + "\","
                    + rango(columna2) + ",\"" + valor2 + "\")");
        }

        /** Fórmula que suma una columna de datos (0 = columna A), opcionalmente solo si otra columna coincide. */
        public Formula suma(int columna, Integer columnaCondicion, String condicion) {
            if (primeraFilaDatos < 0 || filaActual <= primeraFilaDatos) {
                return new Formula("0");
            }
            String rango = rango(columna);
            return columnaCondicion == null
                    ? new Formula("SUM(" + rango + ")")
                    : new Formula("SUMIF(" + rango(columnaCondicion) + ",\"" + condicion + "\"," + rango + ")");
        }

        /** Rango de datos de una columna, por ejemplo {@code C5:C40}. */
        private String rango(int columna) {
            String letra = CellReference.convertNumToColString(columna);
            return letra + (primeraFilaDatos + 1) + ":" + letra + filaActual;
        }

        /** Filtro automático, encabezados inmovilizados y ancho de columnas ajustado. */
        public void terminar(int ultimaFilaDatosExclusiva) {
            if (filaEncabezado >= 0) {
                hoja.createFreezePane(0, filaEncabezado + 1);
                int ultima = Math.max(filaEncabezado, ultimaFilaDatosExclusiva - 1);
                hoja.setAutoFilter(new CellRangeAddress(filaEncabezado, ultima, 0, columnas - 1));
            }
            for (int i = 0; i < columnas; i++) {
                hoja.autoSizeColumn(i);
                hoja.setColumnWidth(i, Math.min(ANCHO_MAXIMO, Math.max(10 * 256, hoja.getColumnWidth(i) + 512)));
            }
        }

        /** Fila siguiente a la última escrita (para indicar el final de los datos al filtro). */
        public int filaActual() {
            return filaActual;
        }

        private void escribir(Object[] valores, boolean esTotal) {
            Row fila = hoja.createRow(filaActual++);
            columnas = Math.max(columnas, valores.length);
            for (int i = 0; i < valores.length; i++) {
                Object v = valores[i];
                if (v == null) {
                    continue;
                }
                Cell celda = fila.createCell(i);
                if (v instanceof BigDecimal monto) {
                    celda.setCellValue(monto.doubleValue());
                    celda.setCellStyle(esTotal ? totalDinero : dinero);
                } else if (v instanceof Porcentaje p) {
                    if (p.valor() != null) {
                        celda.setCellValue(p.valor().doubleValue());
                    }
                    celda.setCellStyle(esTotal ? totalPorcentaje : porcentaje);
                } else if (v instanceof Cantidad c) {
                    celda.setCellValue(c.valor().doubleValue());
                    celda.setCellStyle(esTotal ? totalTexto : cantidad);
                } else if (v instanceof LocalDate f) {
                    celda.setCellValue(f);
                    celda.setCellStyle(fecha);
                } else if (v instanceof Number n) {
                    celda.setCellValue(n.doubleValue());
                    celda.setCellStyle(esTotal ? totalTexto : texto);
                } else if (v instanceof Formula f) {
                    celda.setCellFormula(f.expresion());
                    celda.setCellStyle(esTotal ? totalDinero : dinero);
                } else {
                    celda.setCellValue(v.toString());
                    celda.setCellStyle(esTotal ? totalTexto : texto);
                }
            }
            if (esTotal) {
                for (int i = 0; i < columnas; i++) {
                    if (fila.getCell(i) == null) {
                        fila.createCell(i).setCellStyle(totalTexto);
                    }
                }
            }
        }

        private void celdaTexto(Row fila, int columna, String valor, CellStyle estilo) {
            Cell celda = fila.createCell(columna);
            celda.setCellValue(valor);
            celda.setCellStyle(estilo);
        }
    }
}
