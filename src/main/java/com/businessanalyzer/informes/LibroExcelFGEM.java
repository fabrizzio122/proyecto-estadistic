package com.businessanalyzer.informes;

import com.businessanalyzer.exception.InformeExceptionFGEM;
import com.businessanalyzer.model.MonedaFGEM;
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
public final class LibroExcelFGEM implements AutoCloseable {

    /** Porcentaje expresado como fracción (0.125 = 12.5 %). */
    public record PorcentajeFGEM(BigDecimal valorFGEM) {
    }

    /** Cantidad (no monetaria). */
    public record CantidadFGEM(BigDecimal valorFGEM) {
    }

    /** Fórmula de Excel sin el signo "=" inicial. */
    public record FormulaFGEM(String expresionFGEM) {
    }

    private static final int ANCHO_MAXIMO_FGEM = 60 * 256;

    private final XSSFWorkbook libroFGEM = new XSSFWorkbook();
    private final CellStyle tituloFGEM;
    private final CellStyle subtituloFGEM;
    private final CellStyle encabezadoFGEM;
    private final CellStyle textoFGEM;
    private final CellStyle dineroFGEM;
    private final CellStyle porcentajeFGEM;
    private final CellStyle cantidadFGEM;
    private final CellStyle fechaFGEM;
    private final CellStyle totalTextoFGEM;
    private final CellStyle totalDineroFGEM;
    private final CellStyle totalPorcentajeFGEM;

    public LibroExcelFGEM(MonedaFGEM monedaFGEM) {
        String formatoDineroFGEM = "\"" + monedaFGEM.simboloFGEM() + "\" #,##0.00;[Red]-\"" + monedaFGEM.simboloFGEM() + "\" #,##0.00";

        Font negritaFGEM = libroFGEM.createFont();
        negritaFGEM.setBold(true);
        Font fuenteTituloFGEM = libroFGEM.createFont();
        fuenteTituloFGEM.setBold(true);
        fuenteTituloFGEM.setFontHeightInPoints((short) 14);
        fuenteTituloFGEM.setColor(IndexedColors.DARK_BLUE.getIndex());
        Font fuenteSubtituloFGEM = libroFGEM.createFont();
        fuenteSubtituloFGEM.setItalic(true);
        fuenteSubtituloFGEM.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        Font fuenteEncabezadoFGEM = libroFGEM.createFont();
        fuenteEncabezadoFGEM.setBold(true);
        fuenteEncabezadoFGEM.setColor(IndexedColors.WHITE.getIndex());

        tituloFGEM = libroFGEM.createCellStyle();
        tituloFGEM.setFont(fuenteTituloFGEM);
        subtituloFGEM = libroFGEM.createCellStyle();
        subtituloFGEM.setFont(fuenteSubtituloFGEM);
        encabezadoFGEM = libroFGEM.createCellStyle();
        encabezadoFGEM.setFont(fuenteEncabezadoFGEM);
        encabezadoFGEM.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        encabezadoFGEM.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        encabezadoFGEM.setWrapText(true);
        textoFGEM = libroFGEM.createCellStyle();
        dineroFGEM = conFormatoFGEM(formatoDineroFGEM);
        porcentajeFGEM = conFormatoFGEM("0.0%");
        cantidadFGEM = conFormatoFGEM("#,##0.##");
        fechaFGEM = conFormatoFGEM("dd/mm/yyyy");
        totalTextoFGEM = totalFGEM(null, negritaFGEM);
        totalDineroFGEM = totalFGEM(formatoDineroFGEM, negritaFGEM);
        totalPorcentajeFGEM = totalFGEM("0.0%", negritaFGEM);
    }

    public HojaFGEM hojaFGEM(String nombreFGEM) {
        return new HojaFGEM(libroFGEM.createSheet(WorkbookUtil.createSafeSheetName(nombreFGEM)));
    }

    /** Guarda el libro en {@code destino} (primero en un temporal, para no dejar archivos a medias). */
    public void guardarFGEM(Path destinoFGEM) {
        Path temporalFGEM = destinoFGEM.resolveSibling(destinoFGEM.getFileName() + ".tmp");
        try (OutputStream salidaFGEM = Files.newOutputStream(temporalFGEM)) {
            libroFGEM.write(salidaFGEM);
        } catch (IOException eFGEM) {
            EscrituraFGEM.borrarFGEM(temporalFGEM);
            throw new InformeExceptionFGEM("No se pudo escribir el libro de Excel.", eFGEM);
        }
        EscrituraFGEM.reemplazarFGEM(temporalFGEM, destinoFGEM);
    }

    @Override
    public void close() {
        try {
            libroFGEM.close();
        } catch (IOException ignoradoFGEM) {
            // Libro en memoria: no hay recursos externos que liberar.
        }
    }

    private CellStyle conFormatoFGEM(String formatoFGEM) {
        CellStyle estiloFGEM = libroFGEM.createCellStyle();
        estiloFGEM.setDataFormat(libroFGEM.createDataFormat().getFormat(formatoFGEM));
        return estiloFGEM;
    }

    private CellStyle totalFGEM(String formatoFGEM, Font negritaFGEM) {
        CellStyle estiloFGEM = formatoFGEM == null ? libroFGEM.createCellStyle() : conFormatoFGEM(formatoFGEM);
        estiloFGEM.setFont(negritaFGEM);
        estiloFGEM.setBorderTop(BorderStyle.THIN);
        estiloFGEM.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estiloFGEM.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estiloFGEM;
    }

    /** Una hoja del libro. Las filas se agregan de arriba hacia abajo. */
    public final class HojaFGEM {

        private final Sheet hojaFGEM;
        private int filaActualFGEM;
        private int filaEncabezadoFGEM = -1;
        private int primeraFilaDatosFGEM = -1;
        private int columnasFGEM;

        private HojaFGEM(Sheet hojaFGEM) {
            this.hojaFGEM = hojaFGEM;
        }

        public HojaFGEM tituloFGEM(String textoFGEM, String detalleFGEM) {
            celdaTextoFGEM(hojaFGEM.createRow(filaActualFGEM++), 0, textoFGEM, tituloFGEM);
            if (detalleFGEM != null) {
                celdaTextoFGEM(hojaFGEM.createRow(filaActualFGEM++), 0, detalleFGEM, subtituloFGEM);
            }
            filaActualFGEM++;
            return this;
        }

        public HojaFGEM encabezadosFGEM(String... nombresFGEM) {
            Row filaFGEM = hojaFGEM.createRow(filaActualFGEM);
            filaFGEM.setHeightInPoints(20);
            for (int iFGEM = 0; iFGEM < nombresFGEM.length; iFGEM++) {
                celdaTextoFGEM(filaFGEM, iFGEM, nombresFGEM[iFGEM], encabezadoFGEM);
            }
            filaEncabezadoFGEM = filaActualFGEM++;
            primeraFilaDatosFGEM = filaActualFGEM;
            columnasFGEM = Math.max(columnasFGEM, nombresFGEM.length);
            return this;
        }

        /** Fila de datos. Los valores pueden ser texto, números, montos, fechas, porcentajes o fórmulas. */
        public HojaFGEM filaFGEM(Object... valoresFGEM) {
            escribirFGEM(valoresFGEM, false);
            return this;
        }

        /** Fila de totales resaltada (no entra en el filtro automático). */
        public HojaFGEM totalFGEM(Object... valoresFGEM) {
            escribirFGEM(valoresFGEM, true);
            return this;
        }

        public HojaFGEM espacioFGEM() {
            filaActualFGEM++;
            return this;
        }

        /** Suma de una columna de datos solo en las filas donde otras dos columnas tienen los valores indicados. */
        public FormulaFGEM sumaSiFGEM(int columnaFGEM, int columna1FGEM, String valor1FGEM, int columna2FGEM, String valor2FGEM) {
            if (primeraFilaDatosFGEM < 0 || filaActualFGEM <= primeraFilaDatosFGEM) {
                return new FormulaFGEM("0");
            }
            return new FormulaFGEM("SUMIFS(" + rangoFGEM(columnaFGEM) + "," + rangoFGEM(columna1FGEM) + ",\"" + valor1FGEM + "\","
                    + rangoFGEM(columna2FGEM) + ",\"" + valor2FGEM + "\")");
        }

        /** Fórmula que suma una columna de datos (0 = columna A), opcionalmente solo si otra columna coincide. */
        public FormulaFGEM sumaFGEM(int columnaFGEM, Integer columnaCondicionFGEM, String condicionFGEM) {
            if (primeraFilaDatosFGEM < 0 || filaActualFGEM <= primeraFilaDatosFGEM) {
                return new FormulaFGEM("0");
            }
            String rangoFGEM = rangoFGEM(columnaFGEM);
            return columnaCondicionFGEM == null
                    ? new FormulaFGEM("SUM(" + rangoFGEM + ")")
                    : new FormulaFGEM("SUMIF(" + rangoFGEM(columnaCondicionFGEM) + ",\"" + condicionFGEM + "\"," + rangoFGEM + ")");
        }

        /** Rango de datos de una columna, por ejemplo {@code C5:C40}. */
        private String rangoFGEM(int columnaFGEM) {
            String letraFGEM = CellReference.convertNumToColString(columnaFGEM);
            return letraFGEM + (primeraFilaDatosFGEM + 1) + ":" + letraFGEM + filaActualFGEM;
        }

        /** Filtro automático, encabezados inmovilizados y ancho de columnas ajustado. */
        public void terminarFGEM(int ultimaFilaDatosExclusivaFGEM) {
            if (filaEncabezadoFGEM >= 0) {
                hojaFGEM.createFreezePane(0, filaEncabezadoFGEM + 1);
                int ultimaFGEM = Math.max(filaEncabezadoFGEM, ultimaFilaDatosExclusivaFGEM - 1);
                hojaFGEM.setAutoFilter(new CellRangeAddress(filaEncabezadoFGEM, ultimaFGEM, 0, columnasFGEM - 1));
            }
            for (int iFGEM = 0; iFGEM < columnasFGEM; iFGEM++) {
                hojaFGEM.autoSizeColumn(iFGEM);
                hojaFGEM.setColumnWidth(iFGEM, Math.min(ANCHO_MAXIMO_FGEM, Math.max(10 * 256, hojaFGEM.getColumnWidth(iFGEM) + 512)));
            }
        }

        /** Fila siguiente a la última escrita (para indicar el final de los datos al filtro). */
        public int filaActualFGEM() {
            return filaActualFGEM;
        }

        private void escribirFGEM(Object[] valoresFGEM, boolean esTotalFGEM) {
            Row filaFGEM = hojaFGEM.createRow(filaActualFGEM++);
            columnasFGEM = Math.max(columnasFGEM, valoresFGEM.length);
            for (int iFGEM = 0; iFGEM < valoresFGEM.length; iFGEM++) {
                Object vFGEM = valoresFGEM[iFGEM];
                if (vFGEM == null) {
                    continue;
                }
                Cell celdaFGEM = filaFGEM.createCell(iFGEM);
                if (vFGEM instanceof BigDecimal montoFGEM) {
                    celdaFGEM.setCellValue(montoFGEM.doubleValue());
                    celdaFGEM.setCellStyle(esTotalFGEM ? totalDineroFGEM : dineroFGEM);
                } else if (vFGEM instanceof PorcentajeFGEM pFGEM) {
                    if (pFGEM.valorFGEM() != null) {
                        celdaFGEM.setCellValue(pFGEM.valorFGEM().doubleValue());
                    }
                    celdaFGEM.setCellStyle(esTotalFGEM ? totalPorcentajeFGEM : porcentajeFGEM);
                } else if (vFGEM instanceof CantidadFGEM cFGEM) {
                    celdaFGEM.setCellValue(cFGEM.valorFGEM().doubleValue());
                    celdaFGEM.setCellStyle(esTotalFGEM ? totalTextoFGEM : cantidadFGEM);
                } else if (vFGEM instanceof LocalDate fFGEM) {
                    celdaFGEM.setCellValue(fFGEM);
                    celdaFGEM.setCellStyle(fechaFGEM);
                } else if (vFGEM instanceof Number nFGEM) {
                    celdaFGEM.setCellValue(nFGEM.doubleValue());
                    celdaFGEM.setCellStyle(esTotalFGEM ? totalTextoFGEM : textoFGEM);
                } else if (vFGEM instanceof FormulaFGEM fFGEM) {
                    celdaFGEM.setCellFormula(fFGEM.expresionFGEM());
                    celdaFGEM.setCellStyle(esTotalFGEM ? totalDineroFGEM : dineroFGEM);
                } else {
                    celdaFGEM.setCellValue(vFGEM.toString());
                    celdaFGEM.setCellStyle(esTotalFGEM ? totalTextoFGEM : textoFGEM);
                }
            }
            if (esTotalFGEM) {
                for (int iFGEM = 0; iFGEM < columnasFGEM; iFGEM++) {
                    if (filaFGEM.getCell(iFGEM) == null) {
                        filaFGEM.createCell(iFGEM).setCellStyle(totalTextoFGEM);
                    }
                }
            }
        }

        private void celdaTextoFGEM(Row filaFGEM, int columnaFGEM, String valorFGEM, CellStyle estiloFGEM) {
            Cell celdaFGEM = filaFGEM.createCell(columnaFGEM);
            celdaFGEM.setCellValue(valorFGEM);
            celdaFGEM.setCellStyle(estiloFGEM);
        }
    }
}
