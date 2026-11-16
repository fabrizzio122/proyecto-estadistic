package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.estadistica.AnalisisAbcFGEM;
import com.businessanalyzer.analytics.estadistica.AnalisisEstadisticoFGEM;
import com.businessanalyzer.analytics.estadistica.EstacionalidadSemanalFGEM;
import com.businessanalyzer.analytics.estadistica.EstadisticaDescriptivaFGEM;
import com.businessanalyzer.analytics.estadistica.LecturaEstadisticaFGEM;
import com.businessanalyzer.analytics.estadistica.MatrizCorrelacionFGEM;
import com.businessanalyzer.analytics.estadistica.TendenciaFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.ui.CamposFechaFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.GraficosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.TareasFGEM;
import com.businessanalyzer.ui.TarjetaIndicadorFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.MontosFGEM;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/** Módulo "Análisis estadístico": descriptiva, tendencia, días de la semana, ABC y correlaciones. */
public class EstadisticaControllerFGEM implements VistaModuloFGEM {

    private final AppContextFGEM contextoFGEM;
    private MonedaFGEM monedaFGEM = MonedaFGEM.PEN;
    private long solicitudActualFGEM;

    private final TarjetaIndicadorFGEM kpiVentasFGEM = new TarjetaIndicadorFGEM("Ventas registradas", "fth-shopping-bag");
    private final TarjetaIndicadorFGEM kpiTicketFGEM = new TarjetaIndicadorFGEM("Venta típica (mediana)", "fth-tag");
    private final TarjetaIndicadorFGEM kpiTendenciaFGEM = new TarjetaIndicadorFGEM("Tendencia mensual", "fth-trending-up");
    private final TarjetaIndicadorFGEM kpiClaseAFGEM = new TarjetaIndicadorFGEM("Productos clase A", "fth-star");

    @FXML private DatePicker dpDesdeFGEM;
    @FXML private DatePicker dpHastaFGEM;
    @FXML private ProgressIndicator progresoFGEM;
    @FXML private HBox indicadoresFGEM;
    @FXML private GridPane gridTicketFGEM;
    @FXML private GridPane gridDiariasFGEM;
    @FXML private StackPane contenedorHistogramaFGEM;
    @FXML private VBox lecturaResumenFGEM;
    @FXML private StackPane contenedorTendenciaFGEM;
    @FXML private GridPane gridTendenciaFGEM;
    @FXML private VBox lecturaTendenciaFGEM;
    @FXML private StackPane contenedorDiasFGEM;
    @FXML private GridPane gridDiasFGEM;
    @FXML private VBox lecturaDiasFGEM;
    @FXML private javafx.scene.layout.FlowPane resumenAbcFGEM;
    @FXML private TableView<AnalisisAbcFGEM.ItemFGEM> tablaAbcFGEM;
    @FXML private TableColumn<AnalisisAbcFGEM.ItemFGEM, AnalisisAbcFGEM.ItemFGEM> colAbcClaseFGEM;
    @FXML private TableColumn<AnalisisAbcFGEM.ItemFGEM, String> colAbcNombreFGEM;
    @FXML private TableColumn<AnalisisAbcFGEM.ItemFGEM, String> colAbcVentasFGEM;
    @FXML private TableColumn<AnalisisAbcFGEM.ItemFGEM, String> colAbcParticipacionFGEM;
    @FXML private TableColumn<AnalisisAbcFGEM.ItemFGEM, String> colAbcAcumuladoFGEM;
    @FXML private VBox lecturaAbcFGEM;
    @FXML private StackPane contenedorParetoFGEM;
    @FXML private GridPane gridCorrelacionFGEM;
    @FXML private VBox lecturaCorrelacionesFGEM;

    public EstadisticaControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        indicadoresFGEM.getChildren().addAll(kpiVentasFGEM, kpiTicketFGEM, kpiTendenciaFGEM, kpiClaseAFGEM);
        CamposFechaFGEM.configurarFGEM(dpDesdeFGEM);
        CamposFechaFGEM.configurarFGEM(dpHastaFGEM);
        fijarUltimosMesesFGEM(6);
        dpDesdeFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarFGEM());
        dpHastaFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarFGEM());
        for (GridPane grillaFGEM : List.of(gridTicketFGEM, gridDiariasFGEM, gridTendenciaFGEM, gridDiasFGEM)) {
            ColumnConstraints etiquetasFGEM = new ColumnConstraints();
            etiquetasFGEM.setMinWidth(125);
            ColumnConstraints valoresFGEM = new ColumnConstraints();
            valoresFGEM.setHgrow(Priority.ALWAYS);
            valoresFGEM.setHalignment(javafx.geometry.HPos.RIGHT);
            grillaFGEM.getColumnConstraints().setAll(etiquetasFGEM, valoresFGEM);
        }
        configurarTablaAbcFGEM();
    }

    @Override
    public void alMostrarFGEM() {
        cargarFGEM();
    }

    // ------------------------------------------------------------------ período

    @FXML
    private void ultimos3FGEM() {
        fijarUltimosMesesFGEM(3);
    }

    @FXML
    private void ultimos6FGEM() {
        fijarUltimosMesesFGEM(6);
    }

    @FXML
    private void ultimos12FGEM() {
        fijarUltimosMesesFGEM(12);
    }

    /** Desde el día 1 de hace (n − 1) meses hasta el fin del mes actual. */
    private void fijarUltimosMesesFGEM(int mesesFGEM) {
        YearMonth actualFGEM = YearMonth.now();
        LocalDate desdeFGEM = actualFGEM.minusMonths(mesesFGEM - 1L).atDay(1);
        LocalDate hastaFGEM = actualFGEM.atEndOfMonth();
        if (dpHastaFGEM.getValue() != null && desdeFGEM.isAfter(dpHastaFGEM.getValue())) {
            dpHastaFGEM.setValue(hastaFGEM);
            dpDesdeFGEM.setValue(desdeFGEM);
        } else {
            dpDesdeFGEM.setValue(desdeFGEM);
            dpHastaFGEM.setValue(hastaFGEM);
        }
    }

    // ------------------------------------------------------------------ carga

    private void cargarFGEM() {
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        LocalDate desdeFGEM = dpDesdeFGEM.getValue();
        LocalDate hastaFGEM = dpHastaFGEM.getValue();
        if (empresaFGEM == null) {
            tablaAbcFGEM.getItems().clear();
            tablaAbcFGEM.setPlaceholder(new Label("Selecciona una empresa en la barra superior."));
            return;
        }
        if (desdeFGEM == null || hastaFGEM == null || desdeFGEM.isAfter(hastaFGEM)) {
            return;
        }
        monedaFGEM = empresaFGEM.monedaFGEM();
        progresoFGEM.setVisible(true);
        long numeroFGEM = ++solicitudActualFGEM;
        TareasFGEM.ejecutarFGEM(() -> contextoFGEM.estadisticaFGEM().analizarFGEM(desdeFGEM, hastaFGEM),
                analisisFGEM -> {
                    if (numeroFGEM == solicitudActualFGEM) {
                        progresoFGEM.setVisible(false);
                        mostrarFGEM(analisisFGEM);
                    }
                },
                errorFGEM -> {
                    if (numeroFGEM == solicitudActualFGEM) {
                        progresoFGEM.setVisible(false);
                        NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(errorFGEM));
                    }
                });
    }

    private void mostrarFGEM(AnalisisEstadisticoFGEM aFGEM) {
        List<String> resumenFGEM = LecturaEstadisticaFGEM.resumenFGEM(aFGEM, monedaFGEM);
        contextoFGEM.mostrarFraseFGEM(com.businessanalyzer.ui.ModuloFGEM.ESTADISTICA, resumenFGEM.isEmpty() ? null : resumenFGEM.get(0));
        mostrarIndicadoresFGEM(aFGEM);
        mostrarResumenFGEM(aFGEM);
        mostrarTendenciaFGEM(aFGEM);
        mostrarDiasFGEM(aFGEM);
        mostrarAbcFGEM(aFGEM);
        mostrarCorrelacionesFGEM(aFGEM);
    }

    private void mostrarIndicadoresFGEM(AnalisisEstadisticoFGEM aFGEM) {
        EstadisticaDescriptivaFGEM tFGEM = aFGEM.ticketFGEM();
        kpiVentasFGEM.actualizarFGEM(String.valueOf(tFGEM.nFGEM()), tFGEM.nFGEM() == 1 ? "venta en el período" : "ventas en el período");
        kpiTicketFGEM.actualizarFGEM(dineroFGEM(tFGEM.medianaFGEM()), "Promedio " + dineroFGEM(tFGEM.mediaFGEM()));
        TendenciaFGEM tendenciaFGEM = aFGEM.tendenciaVentasFGEM();
        kpiTendenciaFGEM.actualizarFGEM(tendenciaFGEM.direccionFGEM().descripcionFGEM(), switch (tendenciaFGEM.direccionFGEM()) {
            case INSUFICIENTE -> "Se necesitan 3 meses completos";
            case ESTABLE -> "Variaciones atribuibles al azar";
            default -> (tendenciaFGEM.pendienteFGEM() > 0 ? "+" : "") + porcentajeFGEM(tendenciaFGEM.crecimientoMensualFGEM())
                    + " al mes · R² " + decimalFGEM(tendenciaFGEM.r2FGEM());
        });
        AnalisisAbcFGEM abcFGEM = aFGEM.abcFGEM();
        kpiClaseAFGEM.actualizarFGEM(abcFGEM.itemsFGEM().isEmpty() ? TarjetaIndicadorFGEM.SIN_VALOR_FGEM
                        : abcFGEM.cantidadFGEM(AnalisisAbcFGEM.ClaseFGEM.A) + " de " + abcFGEM.itemsFGEM().size(),
                abcFGEM.itemsFGEM().isEmpty() ? "Sin ventas" : "Generan " + MontosFGEM.porcentajeFGEM(abcFGEM.participacionFGEM(AnalisisAbcFGEM.ClaseFGEM.A))
                        + " de las ventas");
    }

    // ------------------------------------------------------------------ resumen descriptivo

    private void mostrarResumenFGEM(AnalisisEstadisticoFGEM aFGEM) {
        llenarDescriptivaFGEM(gridTicketFGEM, aFGEM.ticketFGEM());
        llenarDescriptivaFGEM(gridDiariasFGEM, aFGEM.ventasDiariasFGEM());
        contenedorHistogramaFGEM.getChildren().setAll(GraficosFGEM.visorFGEM(GraficosFGEM.histogramaFGEM(aFGEM.histogramaTicketsFGEM(), monedaFGEM)));
        lecturaFGEM(lecturaResumenFGEM, "Interpretación", LecturaEstadisticaFGEM.resumenFGEM(aFGEM, monedaFGEM));
    }

    private void llenarDescriptivaFGEM(GridPane grillaFGEM, EstadisticaDescriptivaFGEM eFGEM) {
        grillaFGEM.getChildren().clear();
        int filaFGEM = 0;
        filaFGEM = datoFGEM(grillaFGEM, filaFGEM, "Observaciones", String.valueOf(eFGEM.nFGEM()), "Cantidad de datos analizados");
        filaFGEM = datoFGEM(grillaFGEM, filaFGEM, "Media", dineroFGEM(eFGEM.mediaFGEM()), "Promedio");
        filaFGEM = datoFGEM(grillaFGEM, filaFGEM, "Mediana", dineroFGEM(eFGEM.medianaFGEM()), "La mitad de los valores está por debajo");
        filaFGEM = datoFGEM(grillaFGEM, filaFGEM, "Desviación estándar", dineroFGEM(eFGEM.desviacionFGEM()), "Cuánto se alejan los valores del promedio");
        filaFGEM = datoFGEM(grillaFGEM, filaFGEM, "Coef. de variación", porcentajeFGEM(eFGEM.coeficienteVariacionFGEM()), "Dispersión relativa: menos de 30 % es estable");
        filaFGEM = datoFGEM(grillaFGEM, filaFGEM, "Mínimo", dineroFGEM(eFGEM.minimoFGEM()), null);
        filaFGEM = datoFGEM(grillaFGEM, filaFGEM, "Cuartil 1 (25 %)", dineroFGEM(eFGEM.q1FGEM()), "El 25 % de los valores está por debajo");
        filaFGEM = datoFGEM(grillaFGEM, filaFGEM, "Cuartil 3 (75 %)", dineroFGEM(eFGEM.q3FGEM()), "El 75 % de los valores está por debajo");
        filaFGEM = datoFGEM(grillaFGEM, filaFGEM, "Máximo", dineroFGEM(eFGEM.maximoFGEM()), null);
        datoFGEM(grillaFGEM, filaFGEM, "Asimetría", decimalFGEM(eFGEM.asimetriaFGEM()), "Positiva: pocos valores muy altos estiran la distribución");
    }

    // ------------------------------------------------------------------ tendencia

    private void mostrarTendenciaFGEM(AnalisisEstadisticoFGEM aFGEM) {
        TendenciaFGEM tFGEM = aFGEM.tendenciaVentasFGEM();
        contenedorTendenciaFGEM.getChildren().setAll(GraficosFGEM.visorFGEM(GraficosFGEM.tendenciaFGEM(aFGEM.serieMensualFGEM(), tFGEM, monedaFGEM)));
        gridTendenciaFGEM.getChildren().clear();
        int filaFGEM = 0;
        filaFGEM = datoFGEM(gridTendenciaFGEM, filaFGEM, "Meses analizados", String.valueOf(tFGEM.nFGEM()), "Meses completos con ventas");
        filaFGEM = datoFGEM(gridTendenciaFGEM, filaFGEM, "Dirección", tFGEM.direccionFGEM().descripcionFGEM(), null);
        filaFGEM = datoFGEM(gridTendenciaFGEM, filaFGEM, "Cambio por mes", dineroFGEM(tFGEM.pendienteFGEM()), "Pendiente de la recta");
        filaFGEM = datoFGEM(gridTendenciaFGEM, filaFGEM, "Crecimiento mensual", porcentajeFGEM(tFGEM.crecimientoMensualFGEM()), "Pendiente ÷ venta mensual promedio");
        filaFGEM = datoFGEM(gridTendenciaFGEM, filaFGEM, "R²", decimalFGEM(tFGEM.r2FGEM()), "Qué parte de la variación explica la recta (0 a 1)");
        datoFGEM(gridTendenciaFGEM, filaFGEM, "Valor p", valorPFGEM(tFGEM.significanciaFGEM()), "Menor a 0.05: tendencia estadísticamente significativa");
        lecturaFGEM(lecturaTendenciaFGEM, null, LecturaEstadisticaFGEM.tendenciaFGEM(aFGEM, monedaFGEM));
    }

    // ------------------------------------------------------------------ días de la semana

    private void mostrarDiasFGEM(AnalisisEstadisticoFGEM aFGEM) {
        EstacionalidadSemanalFGEM eFGEM = aFGEM.estacionalidadFGEM();
        contenedorDiasFGEM.getChildren().setAll(GraficosFGEM.visorFGEM(GraficosFGEM.diasDeLaSemanaFGEM(eFGEM, monedaFGEM)));
        gridDiasFGEM.getChildren().clear();
        int filaFGEM = 0;
        for (DayOfWeek diaFGEM : DayOfWeek.values()) {
            Double promedioFGEM = eFGEM.promedioPorDiaFGEM().get(diaFGEM);
            double indiceFGEM = eFGEM.indiceFGEM(diaFGEM);
            String textoFGEM = promedioFGEM == null ? "—" : dineroFGEM(promedioFGEM) + "  ·  " + decimalFGEM(indiceFGEM);
            String nombreFGEM = LecturaEstadisticaFGEM.nombreFGEM(diaFGEM);
            filaFGEM = datoFGEM(gridDiasFGEM, filaFGEM, Character.toUpperCase(nombreFGEM.charAt(0)) + nombreFGEM.substring(1), textoFGEM,
                    "Venta promedio · índice (1.00 = día promedio)");
        }
        lecturaFGEM(lecturaDiasFGEM, null, LecturaEstadisticaFGEM.estacionalidadFGEM(aFGEM, monedaFGEM));
    }

    // ------------------------------------------------------------------ ABC

    private void mostrarAbcFGEM(AnalisisEstadisticoFGEM aFGEM) {
        AnalisisAbcFGEM abcFGEM = aFGEM.abcFGEM();
        tablaAbcFGEM.setItems(FXCollections.observableArrayList(abcFGEM.itemsFGEM()));
        tablaAbcFGEM.setPlaceholder(new Label("No hubo ventas en el período."));
        resumenAbcFGEM.getChildren().clear();
        for (AnalisisAbcFGEM.ClaseFGEM claseFGEM : AnalisisAbcFGEM.ClaseFGEM.values()) {
            Label chipFGEM = new Label("Clase " + claseFGEM + ":  " + abcFGEM.cantidadFGEM(claseFGEM) + " ítems · "
                    + MontosFGEM.porcentajeFGEM(abcFGEM.participacionFGEM(claseFGEM)) + " de las ventas");
            chipFGEM.getStyleClass().addAll("chip", "chip-clase-" + claseFGEM.name().toLowerCase());
            chipFGEM.setMinWidth(Region.USE_PREF_SIZE);
            resumenAbcFGEM.getChildren().add(chipFGEM);
        }
        contenedorParetoFGEM.getChildren().setAll(GraficosFGEM.visorFGEM(GraficosFGEM.paretoFGEM(abcFGEM, 15, monedaFGEM)));
        lecturaFGEM(lecturaAbcFGEM, null, LecturaEstadisticaFGEM.abcFGEM(aFGEM));
    }

    private void configurarTablaAbcFGEM() {
        colAbcClaseFGEM.setCellValueFactory(cFGEM -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colAbcClaseFGEM.setCellFactory(colFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(AnalisisAbcFGEM.ItemFGEM itemFGEM, boolean vacioFGEM) {
                super.updateItem(itemFGEM, vacioFGEM);
                setText(null);
                if (vacioFGEM || itemFGEM == null) {
                    setGraphic(null);
                    return;
                }
                Label insigniaFGEM = new Label(itemFGEM.claseFGEM().name());
                insigniaFGEM.getStyleClass().addAll("badge", "badge-clase-" + itemFGEM.claseFGEM().name().toLowerCase());
                setGraphic(insigniaFGEM);
            }
        });
        colAbcNombreFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().productoFGEM().nombreFGEM()));
        colAbcVentasFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(MontosFGEM.formatearFGEM(cFGEM.getValue().productoFGEM().ventasFGEM(), monedaFGEM)));
        colAbcParticipacionFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(MontosFGEM.porcentajeFGEM(cFGEM.getValue().participacionFGEM())));
        colAbcAcumuladoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(MontosFGEM.porcentajeFGEM(cFGEM.getValue().acumuladoFGEM())));
    }

    // ------------------------------------------------------------------ correlaciones

    private void mostrarCorrelacionesFGEM(AnalisisEstadisticoFGEM aFGEM) {
        MatrizCorrelacionFGEM mFGEM = aFGEM.correlacionesFGEM();
        gridCorrelacionFGEM.getChildren().clear();
        List<String> variablesFGEM = mFGEM.variablesFGEM();
        for (int jFGEM = 0; jFGEM < variablesFGEM.size(); jFGEM++) {
            gridCorrelacionFGEM.add(encabezadoFGEM(variablesFGEM.get(jFGEM), Pos.CENTER), jFGEM + 1, 0);
        }
        for (int iFGEM = 0; iFGEM < variablesFGEM.size(); iFGEM++) {
            gridCorrelacionFGEM.add(encabezadoFGEM(variablesFGEM.get(iFGEM), Pos.CENTER_LEFT), 0, iFGEM + 1);
            for (int jFGEM = 0; jFGEM < variablesFGEM.size(); jFGEM++) {
                gridCorrelacionFGEM.add(celdaCorrelacionFGEM(mFGEM.rFGEM(iFGEM, jFGEM), mFGEM.valoresPFGEM()[iFGEM][jFGEM], iFGEM == jFGEM), jFGEM + 1, iFGEM + 1);
            }
        }
        lecturaFGEM(lecturaCorrelacionesFGEM, null, LecturaEstadisticaFGEM.correlacionesFGEM(aFGEM));
    }

    private static Label encabezadoFGEM(String textoFGEM, Pos alineacionFGEM) {
        Label labelFGEM = new Label(textoFGEM);
        labelFGEM.getStyleClass().add("info-key");
        labelFGEM.setWrapText(true);
        labelFGEM.setAlignment(alineacionFGEM);
        labelFGEM.setMinWidth(105);
        labelFGEM.setMaxWidth(Double.MAX_VALUE);
        labelFGEM.setMinHeight(Region.USE_PREF_SIZE);
        return labelFGEM;
    }

    /** Celda coloreada: azul si r es positivo, rojo si es negativo; más opaca cuanto más fuerte. */
    private static Label celdaCorrelacionFGEM(double rFGEM, double pFGEM, boolean diagonalFGEM) {
        Label celdaFGEM = new Label(Double.isNaN(rFGEM) ? "—" : String.format(Locale.US, "%.2f", rFGEM));
        celdaFGEM.setMinSize(105, 44);
        celdaFGEM.setMaxWidth(Double.MAX_VALUE);
        celdaFGEM.setAlignment(Pos.CENTER);
        celdaFGEM.getStyleClass().add("celda-correlacion");
        if (!Double.isNaN(rFGEM)) {
            double opacidadFGEM = diagonalFGEM ? 0.12 : 0.15 + 0.65 * Math.abs(rFGEM);
            String colorFGEM = rFGEM >= 0 ? "99,214,164" : "255,143,115";
            celdaFGEM.setStyle("-fx-background-color: rgba(" + colorFGEM + "," + String.format(Locale.US, "%.2f", opacidadFGEM) + ");");
            if (!diagonalFGEM) {
                celdaFGEM.setTooltip(new Tooltip("Relación " + MatrizCorrelacionFGEM.fuerzaFGEM(rFGEM) + " (valor p " + valorPFGEM(pFGEM) + ")"));
            }
        }
        return celdaFGEM;
    }

    // ------------------------------------------------------------------ utilidades

    private static int datoFGEM(GridPane grillaFGEM, int filaFGEM, String etiquetaFGEM, String valorFGEM, String ayudaFGEM) {
        Label claveFGEM = new Label(etiquetaFGEM);
        claveFGEM.getStyleClass().add("info-key");
        Label datoFGEM = new Label(valorFGEM);
        datoFGEM.getStyleClass().add("info-value");
        datoFGEM.setMinWidth(Region.USE_PREF_SIZE);
        if (ayudaFGEM != null) {
            Tooltip tooltipFGEM = new Tooltip(ayudaFGEM);
            claveFGEM.setTooltip(tooltipFGEM);
            datoFGEM.setTooltip(tooltipFGEM);
        }
        grillaFGEM.add(claveFGEM, 0, filaFGEM);
        grillaFGEM.add(datoFGEM, 1, filaFGEM);
        return filaFGEM + 1;
    }

    private static void lecturaFGEM(VBox contenedorFGEM, String tituloFGEM, List<String> frasesFGEM) {
        contenedorFGEM.getChildren().clear();
        if (tituloFGEM != null) {
            Label encabezadoFGEM = new Label(tituloFGEM);
            encabezadoFGEM.getStyleClass().add("card-title");
            contenedorFGEM.getChildren().add(encabezadoFGEM);
        }
        for (String fraseFGEM : frasesFGEM) {
            Label puntoFGEM = new Label("•  " + fraseFGEM);
            puntoFGEM.setWrapText(true);
            puntoFGEM.setMinHeight(Region.USE_PREF_SIZE);
            puntoFGEM.getStyleClass().add("lectura");
            contenedorFGEM.getChildren().add(puntoFGEM);
        }
    }

    private String dineroFGEM(double valorFGEM) {
        return Double.isNaN(valorFGEM) ? "—" : MontosFGEM.formatearFGEM(BigDecimal.valueOf(valorFGEM).setScale(2, RoundingMode.HALF_UP), monedaFGEM);
    }

    private static String porcentajeFGEM(double fraccionFGEM) {
        return Double.isNaN(fraccionFGEM) ? "—" : MontosFGEM.porcentajeFGEM(BigDecimal.valueOf(fraccionFGEM));
    }

    private static String decimalFGEM(double valorFGEM) {
        return Double.isNaN(valorFGEM) ? "—" : String.format(Locale.US, "%.2f", valorFGEM);
    }

    private static String valorPFGEM(double pFGEM) {
        return Double.isNaN(pFGEM) ? "—" : pFGEM < 0.001 ? "< 0.001" : String.format(Locale.US, "%.3f", pFGEM);
    }
}
