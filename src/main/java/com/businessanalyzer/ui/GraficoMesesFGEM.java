package com.businessanalyzer.ui;

import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;
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
public final class GraficoMesesFGEM extends VBox {

    private static final double ALTO_VENTAS_FGEM = 180;
    private static final double ALTO_UTILIDAD_FGEM = 46;
    private static final double ANCHO_BARRA_FGEM = 42;

    /**
     * @param ventasFGEM            ventas netas de cada mes
     * @param utilidadFGEM          utilidad neta de cada mes
     * @param ventasEsperadasFGEM   cierre esperado de ventas del mes en curso (último de la lista); {@code null} si no hay
     * @param utilidadEsperadaFGEM  cierre esperado de utilidad del mes en curso; {@code null} si no hay
     * @param diaHoyFGEM            día del mes en curso (para la leyenda)
     */
    public GraficoMesesFGEM(List<YearMonth> mesesFGEM, double[] ventasFGEM, double[] utilidadFGEM, Double ventasEsperadasFGEM,
                        Double utilidadEsperadaFGEM, YearMonth actualFGEM, int diaHoyFGEM, MonedaFGEM monedaFGEM) {
        super(8);
        getStyleClass().add("grafico-meses");
        double maxVentasFGEM = 1;
        double maxUtilidadFGEM = 1;
        for (int iFGEM = 0; iFGEM < mesesFGEM.size(); iFGEM++) {
            maxVentasFGEM = Math.max(maxVentasFGEM, ventasFGEM[iFGEM]);
            maxUtilidadFGEM = Math.max(maxUtilidadFGEM, Math.abs(utilidadFGEM[iFGEM]));
        }
        if (ventasEsperadasFGEM != null) {
            maxVentasFGEM = Math.max(maxVentasFGEM, ventasEsperadasFGEM);
        }
        if (utilidadEsperadaFGEM != null) {
            maxUtilidadFGEM = Math.max(maxUtilidadFGEM, Math.abs(utilidadEsperadaFGEM));
        }

        HBox columnasVentasFGEM = filaFGEM(ALTO_VENTAS_FGEM);
        HBox etiquetasFGEM = new HBox();
        HBox columnasUtilidadFGEM = filaFGEM(ALTO_UTILIDAD_FGEM);
        for (int iFGEM = 0; iFGEM < mesesFGEM.size(); iFGEM++) {
            YearMonth mesFGEM = mesesFGEM.get(iFGEM);
            boolean enCursoFGEM = mesFGEM.equals(actualFGEM);
            String tooltipFGEM = FechasFGEM.nombreMesFGEM(mesFGEM.getMonthValue()) + " " + mesFGEM.getYear() + "\nVentas: "
                    + MontosFGEM.formatearFGEM(BigDecimal.valueOf(ventasFGEM[iFGEM]), monedaFGEM) + "\nUtilidad neta: "
                    + MontosFGEM.formatearFGEM(BigDecimal.valueOf(utilidadFGEM[iFGEM]), monedaFGEM)
                    + (enCursoFGEM && ventasEsperadasFGEM != null ? "\nCierre esperado: ≈ "
                    + MontosFGEM.formatearFGEM(BigDecimal.valueOf(Math.round(ventasEsperadasFGEM)), monedaFGEM) : "");

            columnasVentasFGEM.getChildren().add(columnaFGEM(ventasFGEM[iFGEM] / maxVentasFGEM * ALTO_VENTAS_FGEM,
                    enCursoFGEM && ventasEsperadasFGEM != null ? ventasEsperadasFGEM / maxVentasFGEM * ALTO_VENTAS_FGEM : null,
                    enCursoFGEM ? "barra-mes-actual" : "barra-mes", iFGEM, tooltipFGEM));
            columnasUtilidadFGEM.getChildren().add(columnaFGEM(Math.abs(utilidadFGEM[iFGEM]) / maxUtilidadFGEM * ALTO_UTILIDAD_FGEM,
                    enCursoFGEM && utilidadEsperadaFGEM != null && utilidadEsperadaFGEM > 0
                            ? utilidadEsperadaFGEM / maxUtilidadFGEM * ALTO_UTILIDAD_FGEM : null,
                    utilidadFGEM[iFGEM] < 0 ? "barra-perdida" : enCursoFGEM ? "barra-utilidad-actual" : "barra-utilidad", iFGEM, tooltipFGEM));

            Label etiquetaFGEM = new Label(etiquetaMesFGEM(mesFGEM, iFGEM == 0));
            etiquetaFGEM.getStyleClass().add(enCursoFGEM ? "etiqueta-mes-actual" : "etiqueta-mes");
            etiquetaFGEM.setMaxWidth(Double.MAX_VALUE);
            etiquetaFGEM.setAlignment(Pos.CENTER);
            HBox.setHgrow(etiquetaFGEM, Priority.ALWAYS);
            etiquetasFGEM.getChildren().add(etiquetaFGEM);
        }

        Label tituloVentasFGEM = etiquetaSeccionFGEM("Ventas netas");
        Label tituloUtilidadFGEM = etiquetaSeccionFGEM("Utilidad neta");
        tituloUtilidadFGEM.setPadding(new javafx.geometry.Insets(8, 0, 0, 0));
        getChildren().addAll(tituloVentasFGEM, columnasVentasFGEM, etiquetasFGEM, tituloUtilidadFGEM, columnasUtilidadFGEM,
                leyendaFGEM(actualFGEM, diaHoyFGEM, ventasEsperadasFGEM != null));
    }

    private static HBox filaFGEM(double altoFGEM) {
        HBox filaFGEM = new HBox();
        filaFGEM.setAlignment(Pos.BOTTOM_CENTER);
        filaFGEM.setMinHeight(altoFGEM);
        filaFGEM.setPrefHeight(altoFGEM);
        filaFGEM.setMaxHeight(altoFGEM);
        return filaFGEM;
    }

    /** Columna con la barra (que crece desde abajo) y, si corresponde, el contorno del cierre esperado. */
    private static StackPane columnaFGEM(double altoFGEM, Double altoEsperadoFGEM, String claseFGEM, int indiceFGEM, String tooltipFGEM) {
        StackPane columnaFGEM = new StackPane();
        columnaFGEM.setAlignment(Pos.BOTTOM_CENTER);
        HBox.setHgrow(columnaFGEM, Priority.ALWAYS);
        columnaFGEM.setMaxWidth(Double.MAX_VALUE);
        if (altoEsperadoFGEM != null && altoEsperadoFGEM > altoFGEM) {
            Region contornoFGEM = new Region();
            contornoFGEM.getStyleClass().add("contorno-esperado");
            contornoFGEM.setPrefSize(ANCHO_BARRA_FGEM + 6, altoEsperadoFGEM);
            contornoFGEM.setMaxSize(ANCHO_BARRA_FGEM + 6, altoEsperadoFGEM);
            columnaFGEM.getChildren().add(contornoFGEM);
        }
        Region barraFGEM = new Region();
        barraFGEM.getStyleClass().add(claseFGEM);
        barraFGEM.setPrefWidth(ANCHO_BARRA_FGEM);
        barraFGEM.setMaxWidth(ANCHO_BARRA_FGEM);
        barraFGEM.setMinHeight(0);
        barraFGEM.setMaxHeight(Region.USE_PREF_SIZE);
        barraFGEM.setPrefHeight(AnimacionesFGEM.reducidasFGEM() ? Math.max(altoFGEM, 2) : 0);
        AnimacionesFGEM.haciaFGEM(barraFGEM.prefHeightProperty(), Math.max(altoFGEM, 2), Duration.millis(800),
                Duration.millis(60L * indiceFGEM));
        columnaFGEM.getChildren().add(barraFGEM);
        Tooltip.install(columnaFGEM, new Tooltip(tooltipFGEM));
        return columnaFGEM;
    }

    private static HBox leyendaFGEM(YearMonth actualFGEM, int diaHoyFGEM, boolean conEsperadoFGEM) {
        Region cuadroActualFGEM = new Region();
        cuadroActualFGEM.getStyleClass().addAll("leyenda-cuadro", "barra-mes-actual");
        Label actualTextoFGEM = new Label(FechasFGEM.nombreMesFGEM(actualFGEM.getMonthValue()) + " al día " + diaHoyFGEM);
        actualTextoFGEM.getStyleClass().add("leyenda-texto");
        HBox leyendaFGEM = new HBox(8, cuadroActualFGEM, actualTextoFGEM);
        leyendaFGEM.setAlignment(Pos.CENTER_LEFT);
        if (conEsperadoFGEM) {
            Region cuadroEsperadoFGEM = new Region();
            cuadroEsperadoFGEM.getStyleClass().addAll("leyenda-cuadro", "contorno-esperado");
            Label esperadoTextoFGEM = new Label("Cierre esperado del mes");
            esperadoTextoFGEM.getStyleClass().add("leyenda-texto");
            Region espacioFGEM = new Region();
            espacioFGEM.setMinWidth(14);
            leyendaFGEM.getChildren().addAll(espacioFGEM, cuadroEsperadoFGEM, esperadoTextoFGEM);
        }
        leyendaFGEM.setPadding(new javafx.geometry.Insets(6, 0, 0, 0));
        return leyendaFGEM;
    }

    private static Label etiquetaSeccionFGEM(String textoFGEM) {
        Label etiquetaFGEM = new Label(textoFGEM);
        etiquetaFGEM.getStyleClass().add("grafico-subtitulo");
        return etiquetaFGEM;
    }

    /** «dic», «ene 26»…: el año solo en enero o en el primer mes, para no repetir. */
    private static String etiquetaMesFGEM(YearMonth mesFGEM, boolean primeroFGEM) {
        String cortoFGEM = FechasFGEM.nombreMesFGEM(mesFGEM.getMonthValue()).substring(0, 3).toLowerCase();
        return mesFGEM.getMonthValue() == 1 && !primeroFGEM ? cortoFGEM + " " + String.valueOf(mesFGEM.getYear()).substring(2) : cortoFGEM;
    }
}
