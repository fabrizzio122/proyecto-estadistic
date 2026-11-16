package com.businessanalyzer.ui;

import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Ventas y utilidad de los últimos meses con nodos de JavaFX (se estiliza con el CSS y se anima):
 * <ul>
 *   <li>Barras de ventas en gris neutro; solo el mes en curso en color de acento, dentro de un contorno
 *       punteado que llega hasta el cierre esperado del mes.</li>
 *   <li>Debajo, alineada con los mismos meses, una franja separada con la utilidad neta en verde
 *       (pérdidas en coral).</li>
 * </ul>
 */
public final class GraficoMeses extends VBox {

    private static final double ALTO_VENTAS = 180;
    private static final double ALTO_UTILIDAD = 46;
    private static final double ANCHO_BARRA = 42;

    /**
     * @param ventas            ventas netas de cada mes
     * @param utilidad          utilidad neta de cada mes
     * @param ventasEsperadas   cierre esperado de ventas del mes en curso (último de la lista); {@code null} si no hay
     * @param utilidadEsperada  cierre esperado de utilidad del mes en curso; {@code null} si no hay
     * @param diaHoy            día del mes en curso (para la leyenda)
     */
    public GraficoMeses(List<YearMonth> meses, double[] ventas, double[] utilidad, Double ventasEsperadas,
                        Double utilidadEsperada, YearMonth actual, int diaHoy, Moneda moneda) {
        super(8);
        getStyleClass().add("grafico-meses");
        double maxVentas = 1;
        double maxUtilidad = 1;
        for (int i = 0; i < meses.size(); i++) {
            maxVentas = Math.max(maxVentas, ventas[i]);
            maxUtilidad = Math.max(maxUtilidad, Math.abs(utilidad[i]));
        }
        if (ventasEsperadas != null) {
            maxVentas = Math.max(maxVentas, ventasEsperadas);
        }
        if (utilidadEsperada != null) {
            maxUtilidad = Math.max(maxUtilidad, Math.abs(utilidadEsperada));
        }

        HBox columnasVentas = fila(ALTO_VENTAS);
        HBox etiquetas = new HBox();
        HBox columnasUtilidad = fila(ALTO_UTILIDAD);
        for (int i = 0; i < meses.size(); i++) {
            YearMonth mes = meses.get(i);
            boolean enCurso = mes.equals(actual);
            String tooltip = Fechas.nombreMes(mes.getMonthValue()) + " " + mes.getYear() + "\nVentas: "
                    + Montos.formatear(BigDecimal.valueOf(ventas[i]), moneda) + "\nUtilidad neta: "
                    + Montos.formatear(BigDecimal.valueOf(utilidad[i]), moneda)
                    + (enCurso && ventasEsperadas != null ? "\nCierre esperado: ≈ "
                    + Montos.formatear(BigDecimal.valueOf(Math.round(ventasEsperadas)), moneda) : "");

            columnasVentas.getChildren().add(columna(ventas[i] / maxVentas * ALTO_VENTAS,
                    enCurso && ventasEsperadas != null ? ventasEsperadas / maxVentas * ALTO_VENTAS : null,
                    enCurso ? "barra-mes-actual" : "barra-mes", i, tooltip));
            columnasUtilidad.getChildren().add(columna(Math.abs(utilidad[i]) / maxUtilidad * ALTO_UTILIDAD,
                    enCurso && utilidadEsperada != null && utilidadEsperada > 0
                            ? utilidadEsperada / maxUtilidad * ALTO_UTILIDAD : null,
                    utilidad[i] < 0 ? "barra-perdida" : enCurso ? "barra-utilidad-actual" : "barra-utilidad", i, tooltip));

            Label etiqueta = new Label(etiquetaMes(mes, i == 0));
            etiqueta.getStyleClass().add(enCurso ? "etiqueta-mes-actual" : "etiqueta-mes");
            etiqueta.setMaxWidth(Double.MAX_VALUE);
            etiqueta.setAlignment(Pos.CENTER);
            HBox.setHgrow(etiqueta, Priority.ALWAYS);
            etiquetas.getChildren().add(etiqueta);
        }

        Label tituloVentas = etiquetaSeccion("Ventas netas");
        Label tituloUtilidad = etiquetaSeccion("Utilidad neta");
        tituloUtilidad.setPadding(new javafx.geometry.Insets(8, 0, 0, 0));
        getChildren().addAll(tituloVentas, columnasVentas, etiquetas, tituloUtilidad, columnasUtilidad,
                leyenda(actual, diaHoy, ventasEsperadas != null));
    }

    private static HBox fila(double alto) {
        HBox fila = new HBox();
        fila.setAlignment(Pos.BOTTOM_CENTER);
        fila.setMinHeight(alto);
        fila.setPrefHeight(alto);
        fila.setMaxHeight(alto);
        return fila;
    }

    /** Columna con la barra (que crece desde abajo) y, si corresponde, el contorno del cierre esperado. */
    private static StackPane columna(double alto, Double altoEsperado, String clase, int indice, String tooltip) {
        StackPane columna = new StackPane();
        columna.setAlignment(Pos.BOTTOM_CENTER);
        HBox.setHgrow(columna, Priority.ALWAYS);
        columna.setMaxWidth(Double.MAX_VALUE);
        if (altoEsperado != null && altoEsperado > alto) {
            Region contorno = new Region();
            contorno.getStyleClass().add("contorno-esperado");
            contorno.setPrefSize(ANCHO_BARRA + 6, altoEsperado);
            contorno.setMaxSize(ANCHO_BARRA + 6, altoEsperado);
            columna.getChildren().add(contorno);
        }
        Region barra = new Region();
        barra.getStyleClass().add(clase);
        barra.setPrefWidth(ANCHO_BARRA);
        barra.setMaxWidth(ANCHO_BARRA);
        barra.setMinHeight(0);
        barra.setMaxHeight(Region.USE_PREF_SIZE);
        barra.setPrefHeight(Animaciones.reducidas() ? Math.max(alto, 2) : 0);
        Animaciones.hacia(barra.prefHeightProperty(), Math.max(alto, 2), Duration.millis(800),
                Duration.millis(60L * indice));
        columna.getChildren().add(barra);
        Tooltip.install(columna, new Tooltip(tooltip));
        return columna;
    }

    private static HBox leyenda(YearMonth actual, int diaHoy, boolean conEsperado) {
        Region cuadroActual = new Region();
        cuadroActual.getStyleClass().addAll("leyenda-cuadro", "barra-mes-actual");
        Label actualTexto = new Label(Fechas.nombreMes(actual.getMonthValue()) + " al día " + diaHoy);
        actualTexto.getStyleClass().add("leyenda-texto");
        HBox leyenda = new HBox(8, cuadroActual, actualTexto);
        leyenda.setAlignment(Pos.CENTER_LEFT);
        if (conEsperado) {
            Region cuadroEsperado = new Region();
            cuadroEsperado.getStyleClass().addAll("leyenda-cuadro", "contorno-esperado");
            Label esperadoTexto = new Label("Cierre esperado del mes");
            esperadoTexto.getStyleClass().add("leyenda-texto");
            Region espacio = new Region();
            espacio.setMinWidth(14);
            leyenda.getChildren().addAll(espacio, cuadroEsperado, esperadoTexto);
        }
        leyenda.setPadding(new javafx.geometry.Insets(6, 0, 0, 0));
        return leyenda;
    }

    private static Label etiquetaSeccion(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("grafico-subtitulo");
        return etiqueta;
    }

    /** «dic», «ene 26»…: el año solo en enero o en el primer mes, para no repetir. */
    private static String etiquetaMes(YearMonth mes, boolean primero) {
        String corto = Fechas.nombreMes(mes.getMonthValue()).substring(0, 3).toLowerCase();
        return mes.getMonthValue() == 1 && !primero ? corto + " " + String.valueOf(mes.getYear()).substring(2) : corto;
    }
}
