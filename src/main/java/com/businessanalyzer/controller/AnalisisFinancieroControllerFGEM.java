package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.AnalisisFinancieroFGEM;
import com.businessanalyzer.analytics.EstadoResultadosFGEM;
import com.businessanalyzer.analytics.LecturaFinancieraFGEM;
import com.businessanalyzer.analytics.MargenesFGEM;
import com.businessanalyzer.analytics.PuntoEquilibrioFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM;
import com.businessanalyzer.analytics.VariacionFGEM;
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
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Módulo "Análisis financiero": estado de resultados, márgenes, punto de equilibrio y rentabilidad. */
public class AnalisisFinancieroControllerFGEM implements VistaModuloFGEM {

    /** Tipo de fila del estado de resultados (define el estilo). */
    private enum TipoFilaFGEM { DETALLE, SUBTOTAL, RESULTADO }

    /**
     * Fila del estado de resultados.
     *
     * @param esCostoFGEM si sube, empeora el resultado (se pinta en rojo)
     */
    private record FilaEstadoFGEM(String conceptoFGEM, BigDecimal actualFGEM, BigDecimal anteriorFGEM, BigDecimal sobreVentasFGEM,
                              TipoFilaFGEM tipoFGEM, boolean esCostoFGEM) {
        VariacionFGEM variacionFGEM() {
            return new VariacionFGEM(actualFGEM, anteriorFGEM);
        }

        boolean mejoraFGEM() {
            int signoFGEM = variacionFGEM().absolutaFGEM().signum();
            return esCostoFGEM ? signoFGEM < 0 : signoFGEM > 0;
        }
    }

    private static final PseudoClass SUBTOTAL_FGEM = PseudoClass.getPseudoClass("subtotal");
    private static final PseudoClass RESULTADO_FGEM = PseudoClass.getPseudoClass("resultado");

    private final AppContextFGEM contextoFGEM;
    private MonedaFGEM monedaFGEM = MonedaFGEM.PEN;

    private final TarjetaIndicadorFGEM kpiUtilidadFGEM = new TarjetaIndicadorFGEM("Utilidad neta", "fth-dollar-sign");
    private final TarjetaIndicadorFGEM kpiMargenFGEM = new TarjetaIndicadorFGEM("Margen neto", "fth-percent");
    private final TarjetaIndicadorFGEM kpiEquilibrioFGEM = new TarjetaIndicadorFGEM("Punto de equilibrio", "fth-target");
    private final TarjetaIndicadorFGEM kpiSeguridadFGEM = new TarjetaIndicadorFGEM("Margen de seguridad", "fth-shield");

    @FXML private DatePicker dpDesdeFGEM;
    @FXML private DatePicker dpHastaFGEM;
    @FXML private ProgressIndicator progresoFGEM;
    @FXML private Label lblComparacionFGEM;
    @FXML private HBox indicadoresFGEM;
    @FXML private TableView<FilaEstadoFGEM> tablaEstadoFGEM;
    @FXML private TableColumn<FilaEstadoFGEM, String> colConceptoFGEM;
    @FXML private TableColumn<FilaEstadoFGEM, FilaEstadoFGEM> colActualFGEM;
    @FXML private TableColumn<FilaEstadoFGEM, String> colPorcentajeFGEM;
    @FXML private TableColumn<FilaEstadoFGEM, FilaEstadoFGEM> colAnteriorFGEM;
    @FXML private TableColumn<FilaEstadoFGEM, FilaEstadoFGEM> colVariacionPctFGEM;
    @FXML private HBox margenesFGEM;
    @FXML private StackPane contenedorEvolucionFGEM;
    @FXML private VBox lecturaFGEM;
    @FXML private GridPane gridEquilibrioFGEM;
    @FXML private StackPane contenedorEquilibrioFGEM;
    @FXML private TableView<RentabilidadProductoFGEM> tablaProductosFGEM;
    @FXML private TableColumn<RentabilidadProductoFGEM, String> colProdCodigoFGEM;
    @FXML private TableColumn<RentabilidadProductoFGEM, String> colProdNombreFGEM;
    @FXML private TableColumn<RentabilidadProductoFGEM, String> colProdCantidadFGEM;
    @FXML private TableColumn<RentabilidadProductoFGEM, BigDecimal> colProdVentasFGEM;
    @FXML private TableColumn<RentabilidadProductoFGEM, BigDecimal> colProdMargenFGEM;
    @FXML private TableColumn<RentabilidadProductoFGEM, BigDecimal> colProdMargenPctFGEM;
    @FXML private TableColumn<RentabilidadProductoFGEM, BigDecimal> colProdAporteFGEM;
    private BigDecimal margenTotalProductosFGEM = BigDecimal.ZERO;
    private long solicitudActualFGEM;

    public AnalisisFinancieroControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        indicadoresFGEM.getChildren().addAll(kpiUtilidadFGEM, kpiMargenFGEM, kpiEquilibrioFGEM, kpiSeguridadFGEM);
        CamposFechaFGEM.configurarFGEM(dpDesdeFGEM);
        CamposFechaFGEM.configurarFGEM(dpHastaFGEM);
        // Mes en curso hasta hoy: se compara con los mismos días del mes anterior.
        dpDesdeFGEM.setValue(LocalDate.now().withDayOfMonth(1));
        dpHastaFGEM.setValue(LocalDate.now());
        dpDesdeFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarFGEM());
        dpHastaFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarFGEM());
        configurarTablaEstadoFGEM();
        configurarTablaProductosFGEM();

        ColumnConstraints conceptosFGEM = new ColumnConstraints();
        conceptosFGEM.setMinWidth(185);
        ColumnConstraints valoresFGEM = new ColumnConstraints();
        valoresFGEM.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        gridEquilibrioFGEM.getColumnConstraints().setAll(conceptosFGEM, valoresFGEM);
    }

    @Override
    public void alMostrarFGEM() {
        cargarFGEM();
    }

    // ------------------------------------------------------------------ período

    @FXML
    private void esteMesFGEM() {
        fijarPeriodoFGEM(LocalDate.now().withDayOfMonth(1), LocalDate.now());
    }

    @FXML
    private void mesAnteriorFGEM() {
        YearMonth anteriorFGEM = YearMonth.now().minusMonths(1);
        fijarPeriodoFGEM(anteriorFGEM.atDay(1), anteriorFGEM.atEndOfMonth());
    }

    @FXML
    private void esteAnioFGEM() {
        LocalDate hoyFGEM = LocalDate.now();
        fijarPeriodoFGEM(hoyFGEM.withDayOfYear(1), hoyFGEM);
    }

    /** Cambia ambas fechas y carga una sola vez. */
    private void fijarPeriodoFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM) {
        if (desdeFGEM.isAfter(dpHastaFGEM.getValue())) {
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
            limpiarFGEM("Selecciona una empresa en la barra superior para ver su análisis.");
            return;
        }
        if (desdeFGEM == null || hastaFGEM == null || desdeFGEM.isAfter(hastaFGEM)) {
            return;
        }
        monedaFGEM = empresaFGEM.monedaFGEM();
        progresoFGEM.setVisible(true);
        // Si el usuario cambia el período antes de que termine el cálculo, se descarta el resultado viejo.
        long numeroFGEM = ++solicitudActualFGEM;
        TareasFGEM.ejecutarFGEM(() -> contextoFGEM.analisisFGEM().analizarFGEM(desdeFGEM, hastaFGEM),
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

    private void limpiarFGEM(String mensajeFGEM) {
        tablaEstadoFGEM.getItems().clear();
        tablaEstadoFGEM.setPlaceholder(new Label(mensajeFGEM));
        tablaProductosFGEM.getItems().clear();
        for (TarjetaIndicadorFGEM kpiFGEM : List.of(kpiUtilidadFGEM, kpiMargenFGEM, kpiEquilibrioFGEM, kpiSeguridadFGEM)) {
            kpiFGEM.actualizarFGEM(TarjetaIndicadorFGEM.SIN_VALOR_FGEM, "");
        }
        contenedorEvolucionFGEM.getChildren().clear();
        contenedorEquilibrioFGEM.getChildren().clear();
        margenesFGEM.getChildren().clear();
        lecturaFGEM.getChildren().clear();
        contextoFGEM.mostrarFraseFGEM(com.businessanalyzer.ui.ModuloFGEM.ANALISIS_FINANCIERO, null);
        gridEquilibrioFGEM.getChildren().clear();
        lblComparacionFGEM.setText("");
    }

    private void mostrarFGEM(AnalisisFinancieroFGEM aFGEM) {
        lblComparacionFGEM.setText("Comparado con " + aFGEM.periodoAnteriorFGEM().descripcionFGEM());
        mostrarIndicadoresFGEM(aFGEM);
        mostrarEstadoFGEM(aFGEM);
        mostrarEquilibrioFGEM(aFGEM);
        mostrarProductosFGEM(aFGEM);
    }

    private void mostrarIndicadoresFGEM(AnalisisFinancieroFGEM aFGEM) {
        EstadoResultadosFGEM eFGEM = aFGEM.actualFGEM();
        VariacionFGEM variacionFGEM = new VariacionFGEM(eFGEM.utilidadNetaFGEM(), aFGEM.anteriorFGEM().utilidadNetaFGEM());
        kpiUtilidadFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(eFGEM.utilidadNetaFGEM(), monedaFGEM),
                variacionFGEM.relativaFGEM() == null ? "Sin datos del período anterior"
                        : signoFGEM(variacionFGEM.relativaFGEM()) + MontosFGEM.porcentajeFGEM(variacionFGEM.relativaFGEM()) + " vs. período anterior");
        kpiMargenFGEM.actualizarFGEM(MontosFGEM.porcentajeFGEM(eFGEM.margenNetoFGEM()),
                "Bruto " + MontosFGEM.porcentajeFGEM(eFGEM.margenBrutoFGEM()) + " · operativo " + MontosFGEM.porcentajeFGEM(eFGEM.margenOperativoFGEM()));

        PuntoEquilibrioFGEM peFGEM = aFGEM.equilibrioFGEM();
        if (peFGEM.ventasEquilibrioFGEM() == null) {
            kpiEquilibrioFGEM.actualizarFGEM("No alcanzable", eFGEM.ventasNetasFGEM().signum() == 0
                    ? "Sin ventas en el período" : "Las ventas no cubren sus costos variables");
            kpiSeguridadFGEM.actualizarFGEM(TarjetaIndicadorFGEM.SIN_VALOR_FGEM, "Requiere un punto de equilibrio");
        } else {
            kpiEquilibrioFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(peFGEM.ventasEquilibrioFGEM(), monedaFGEM), peFGEM.alcanzadoFGEM()
                    ? "Alcanzado · ventas al " + MontosFGEM.porcentajeFGEM(peFGEM.coberturaFGEM()) + " del equilibrio"
                    : "Faltan " + MontosFGEM.formatearFGEM(peFGEM.ventasFaltantesFGEM(), monedaFGEM) + " en ventas");
            kpiSeguridadFGEM.actualizarFGEM(MontosFGEM.porcentajeFGEM(peFGEM.margenSeguridadFGEM()), peFGEM.alcanzadoFGEM()
                    ? "Lo que pueden caer las ventas sin pérdida"
                    : "Ventas por debajo del equilibrio");
        }
    }

    // ------------------------------------------------------------------ estado de resultados

    private void mostrarEstadoFGEM(AnalisisFinancieroFGEM aFGEM) {
        EstadoResultadosFGEM eFGEM = aFGEM.actualFGEM();
        EstadoResultadosFGEM pFGEM = aFGEM.anteriorFGEM();
        List<FilaEstadoFGEM> filasFGEM = new ArrayList<>();
        filasFGEM.add(filaFGEM("Ventas netas", eFGEM, pFGEM, EstadoResultadosFGEM::ventasNetasFGEM, TipoFilaFGEM.DETALLE, false));
        filasFGEM.add(filaFGEM("(−) Costo de ventas", eFGEM, pFGEM, EstadoResultadosFGEM::costoVentasFGEM, TipoFilaFGEM.DETALLE, true));
        filasFGEM.add(filaFGEM("= Utilidad bruta", eFGEM, pFGEM, EstadoResultadosFGEM::utilidadBrutaFGEM, TipoFilaFGEM.SUBTOTAL, false));
        filasFGEM.add(filaFGEM("(−) Gastos operativos", eFGEM, pFGEM, EstadoResultadosFGEM::gastosOperativosFGEM, TipoFilaFGEM.DETALLE, true));
        filasFGEM.add(filaFGEM("= Utilidad operativa", eFGEM, pFGEM, EstadoResultadosFGEM::utilidadOperativaFGEM, TipoFilaFGEM.SUBTOTAL, false));
        filasFGEM.add(filaFGEM("(+) Ingresos financieros", eFGEM, pFGEM, EstadoResultadosFGEM::ingresosFinancierosFGEM, TipoFilaFGEM.DETALLE, false));
        filasFGEM.add(filaFGEM("(−) Gastos financieros", eFGEM, pFGEM, EstadoResultadosFGEM::gastosFinancierosFGEM, TipoFilaFGEM.DETALLE, true));
        filasFGEM.add(filaFGEM("(+) Otros ingresos", eFGEM, pFGEM, EstadoResultadosFGEM::otrosIngresosFGEM, TipoFilaFGEM.DETALLE, false));
        filasFGEM.add(filaFGEM("= Utilidad antes de impuestos", eFGEM, pFGEM, EstadoResultadosFGEM::utilidadAntesImpuestosFGEM, TipoFilaFGEM.SUBTOTAL, false));
        filasFGEM.add(filaFGEM("(−) Impuesto a la renta", eFGEM, pFGEM, EstadoResultadosFGEM::impuestoRentaFGEM, TipoFilaFGEM.DETALLE, true));
        filasFGEM.add(filaFGEM("= Utilidad neta", eFGEM, pFGEM, EstadoResultadosFGEM::utilidadNetaFGEM, TipoFilaFGEM.RESULTADO, false));
        tablaEstadoFGEM.getItems().setAll(filasFGEM);

        margenesFGEM.getChildren().setAll(
                chipMargenFGEM("Bruto", eFGEM.margenBrutoFGEM(), pFGEM.margenBrutoFGEM()),
                chipMargenFGEM("Operativo", eFGEM.margenOperativoFGEM(), pFGEM.margenOperativoFGEM()),
                chipMargenFGEM("Neto", eFGEM.margenNetoFGEM(), pFGEM.margenNetoFGEM()));

        contenedorEvolucionFGEM.getChildren().setAll(GraficosFGEM.visorFGEM(GraficosFGEM.evolucionMensualFGEM(aFGEM.evolucionFGEM(), monedaFGEM)));

        lecturaFGEM.getChildren().clear();
        List<String> frasesFGEM = LecturaFinancieraFGEM.generarFGEM(aFGEM, monedaFGEM);
        contextoFGEM.mostrarFraseFGEM(com.businessanalyzer.ui.ModuloFGEM.ANALISIS_FINANCIERO, frasesFGEM.isEmpty() ? null : frasesFGEM.get(0));
        for (String fraseFGEM : frasesFGEM) {
            Label puntoFGEM = new Label("•  " + fraseFGEM);
            puntoFGEM.setWrapText(true);
            puntoFGEM.getStyleClass().add("lectura");
            lecturaFGEM.getChildren().add(puntoFGEM);
        }
    }

    private static FilaEstadoFGEM filaFGEM(String conceptoFGEM, EstadoResultadosFGEM actualFGEM, EstadoResultadosFGEM anteriorFGEM,
                                   Function<EstadoResultadosFGEM, BigDecimal> valorFGEM, TipoFilaFGEM tipoFGEM, boolean esCostoFGEM) {
        BigDecimal montoFGEM = valorFGEM.apply(actualFGEM);
        return new FilaEstadoFGEM(conceptoFGEM, montoFGEM, valorFGEM.apply(anteriorFGEM), actualFGEM.sobreVentasFGEM(montoFGEM), tipoFGEM, esCostoFGEM);
    }

    /** "Margen bruto 33.3 % (+2.1 pp)": pp = puntos porcentuales respecto al período anterior. */
    private static Label chipMargenFGEM(String nombreFGEM, BigDecimal actualFGEM, BigDecimal anteriorFGEM) {
        String textoFGEM = "Margen " + nombreFGEM.toLowerCase() + "  " + MontosFGEM.porcentajeFGEM(actualFGEM);
        if (actualFGEM != null && anteriorFGEM != null) {
            BigDecimal puntosFGEM = actualFGEM.subtract(anteriorFGEM).movePointRight(2).setScale(1, RoundingMode.HALF_UP);
            textoFGEM += "  (" + (puntosFGEM.signum() >= 0 ? "+" : "") + puntosFGEM + " pp)";
        }
        Label chipFGEM = new Label(textoFGEM);
        chipFGEM.getStyleClass().add("chip");
        chipFGEM.setMinWidth(Region.USE_PREF_SIZE);
        chipFGEM.setTooltip(new javafx.scene.control.Tooltip("pp = puntos porcentuales de diferencia con el período anterior"));
        return chipFGEM;
    }

    private void configurarTablaEstadoFGEM() {
        colConceptoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().conceptoFGEM()));
        colPorcentajeFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(MontosFGEM.porcentajeFGEM(cFGEM.getValue().sobreVentasFGEM())));
        configurarCeldaFilaFGEM(colActualFGEM, fFGEM -> MontosFGEM.formatearFGEM(fFGEM.actualFGEM(), monedaFGEM), fFGEM -> fFGEM.actualFGEM().signum() < 0 && fFGEM.tipoFGEM() != TipoFilaFGEM.DETALLE ? "margen-negativo" : null);
        configurarCeldaFilaFGEM(colAnteriorFGEM, fFGEM -> MontosFGEM.formatearFGEM(fFGEM.anteriorFGEM(), monedaFGEM), fFGEM -> null);
        configurarCeldaFilaFGEM(colVariacionPctFGEM, fFGEM -> {
            BigDecimal relativaFGEM = fFGEM.variacionFGEM().relativaFGEM();
            return relativaFGEM == null ? "—" : signoFGEM(relativaFGEM) + MontosFGEM.porcentajeFGEM(relativaFGEM);
        }, this::claseVariacionFGEM);

        tablaEstadoFGEM.setRowFactory(tFGEM -> new TableRow<>() {
            @Override
            protected void updateItem(FilaEstadoFGEM filaFGEM, boolean vacioFGEM) {
                super.updateItem(filaFGEM, vacioFGEM);
                pseudoClassStateChanged(SUBTOTAL_FGEM, !vacioFGEM && filaFGEM != null && filaFGEM.tipoFGEM() == TipoFilaFGEM.SUBTOTAL);
                pseudoClassStateChanged(RESULTADO_FGEM, !vacioFGEM && filaFGEM != null && filaFGEM.tipoFGEM() == TipoFilaFGEM.RESULTADO);
            }
        });
        tablaEstadoFGEM.setPlaceholder(new Label("Elige un período para ver el estado de resultados."));
    }

    private String claseVariacionFGEM(FilaEstadoFGEM fFGEM) {
        if (fFGEM.variacionFGEM().absolutaFGEM().signum() == 0) {
            return null;
        }
        return fFGEM.mejoraFGEM() ? "margen-ok" : "margen-negativo";
    }

    private static void configurarCeldaFilaFGEM(TableColumn<FilaEstadoFGEM, FilaEstadoFGEM> columnaFGEM,
                                            Function<FilaEstadoFGEM, String> textoFGEM,
                                            Function<FilaEstadoFGEM, String> claseFGEM) {
        columnaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        columnaFGEM.setCellFactory(colFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(FilaEstadoFGEM filaFGEM, boolean vacioFGEM) {
                super.updateItem(filaFGEM, vacioFGEM);
                getStyleClass().removeAll("margen-ok", "margen-negativo");
                if (vacioFGEM || filaFGEM == null) {
                    setText(null);
                    return;
                }
                setText(textoFGEM.apply(filaFGEM));
                String estiloFGEM = claseFGEM.apply(filaFGEM);
                if (estiloFGEM != null) {
                    getStyleClass().add(estiloFGEM);
                }
            }
        });
    }

    // ------------------------------------------------------------------ punto de equilibrio

    private void mostrarEquilibrioFGEM(AnalisisFinancieroFGEM aFGEM) {
        PuntoEquilibrioFGEM peFGEM = aFGEM.equilibrioFGEM();
        BigDecimal costoVentasFGEM = aFGEM.actualFGEM().costoVentasFGEM();
        BigDecimal gastosVariablesFGEM = peFGEM.costosVariablesFGEM().subtract(costoVentasFGEM);

        gridEquilibrioFGEM.getChildren().clear();
        int filaFGEM = 0;
        filaFGEM = filaEquilibrioFGEM(filaFGEM, "Ventas netas", MontosFGEM.formatearFGEM(peFGEM.ventasFGEM(), monedaFGEM), false);
        filaFGEM = filaEquilibrioFGEM(filaFGEM, "(−) Costo de lo vendido", MontosFGEM.formatearFGEM(costoVentasFGEM, monedaFGEM), false);
        filaFGEM = filaEquilibrioFGEM(filaFGEM, "(−) Gastos variables", MontosFGEM.formatearFGEM(gastosVariablesFGEM, monedaFGEM), false);
        filaFGEM = filaEquilibrioFGEM(filaFGEM, "= Margen de contribución", MontosFGEM.formatearFGEM(peFGEM.margenContribucionFGEM(), monedaFGEM), true);
        filaFGEM = filaEquilibrioFGEM(filaFGEM, "Razón de contribución", MontosFGEM.porcentajeFGEM(peFGEM.razonContribucionFGEM())
                + " de cada venta", false);
        filaFGEM = filaEquilibrioFGEM(filaFGEM, "Costos fijos", MontosFGEM.formatearFGEM(peFGEM.costosFijosFGEM(), monedaFGEM), false);
        filaFGEM = filaEquilibrioFGEM(filaFGEM, "Punto de equilibrio", peFGEM.ventasEquilibrioFGEM() == null
                ? "No alcanzable" : MontosFGEM.formatearFGEM(peFGEM.ventasEquilibrioFGEM(), monedaFGEM), true);
        Label formulaFGEM = new Label("= costos fijos ÷ razón de contribución");
        formulaFGEM.getStyleClass().add("hint");
        formulaFGEM.setWrapText(true);
        gridEquilibrioFGEM.add(formulaFGEM, 1, filaFGEM++);
        filaEquilibrioFGEM(filaFGEM, "Margen de seguridad", MontosFGEM.porcentajeFGEM(peFGEM.margenSeguridadFGEM()), false);

        contenedorEquilibrioFGEM.getChildren().setAll(GraficosFGEM.visorFGEM(GraficosFGEM.puntoEquilibrioFGEM(peFGEM, monedaFGEM)));
    }

    private int filaEquilibrioFGEM(int filaFGEM, String conceptoFGEM, String valorFGEM, boolean destacadoFGEM) {
        Label etiquetaFGEM = new Label(conceptoFGEM);
        etiquetaFGEM.getStyleClass().add(destacadoFGEM ? "step-title" : "info-key");
        Label datoFGEM = new Label(valorFGEM);
        datoFGEM.getStyleClass().add(destacadoFGEM ? "total-valor-destacado" : "info-value");
        gridEquilibrioFGEM.add(etiquetaFGEM, 0, filaFGEM);
        gridEquilibrioFGEM.add(datoFGEM, 1, filaFGEM);
        return filaFGEM + 1;
    }

    // ------------------------------------------------------------------ rentabilidad por producto

    private void mostrarProductosFGEM(AnalisisFinancieroFGEM aFGEM) {
        margenTotalProductosFGEM = aFGEM.margenTotalProductosFGEM();
        tablaProductosFGEM.setItems(FXCollections.observableArrayList(aFGEM.productosFGEM()));
        tablaProductosFGEM.setPlaceholder(new Label("No hubo ventas en el período."));
        contenedorProductosFGEM.getChildren().setAll(GraficosFGEM.visorFGEM(GraficosFGEM.rentabilidadProductosFGEM(aFGEM.productosFGEM(), 8, monedaFGEM)));
    }

    @FXML private StackPane contenedorProductosFGEM;

    private void configurarTablaProductosFGEM() {
        colProdCodigoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().codigoFGEM()));
        colProdNombreFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().nombreFGEM()));
        colProdCantidadFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(MontosFGEM.cantidadFGEM(cFGEM.getValue().cantidadFGEM())));
        colProdVentasFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().ventasFGEM()));
        colProdMargenFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().margenFGEM()));
        colProdMargenPctFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().margenSobreVentasFGEM()));
        colProdAporteFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(margenTotalProductosFGEM.signum() == 0 ? null
                : cFGEM.getValue().margenFGEM().divide(margenTotalProductosFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP)));
        colProdVentasFGEM.setCellFactory(colFGEM -> celdaNumeroFGEM(vFGEM -> MontosFGEM.formatearFGEM(vFGEM, monedaFGEM)));
        colProdMargenFGEM.setCellFactory(colFGEM -> celdaNumeroFGEM(vFGEM -> MontosFGEM.formatearFGEM(vFGEM, monedaFGEM)));
        colProdMargenPctFGEM.setCellFactory(colFGEM -> celdaNumeroFGEM(MontosFGEM::porcentajeFGEM));
        colProdAporteFGEM.setCellFactory(colFGEM -> celdaNumeroFGEM(MontosFGEM::porcentajeFGEM));
    }

    private static TableCell<RentabilidadProductoFGEM, BigDecimal> celdaNumeroFGEM(Function<BigDecimal, String> formatoFGEM) {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal valorFGEM, boolean vacioFGEM) {
                super.updateItem(valorFGEM, vacioFGEM);
                getStyleClass().remove("margen-negativo");
                if (vacioFGEM) {
                    setText(null);
                    return;
                }
                setText(formatoFGEM.apply(valorFGEM));
                if (valorFGEM != null && valorFGEM.signum() < 0) {
                    getStyleClass().add("margen-negativo");
                }
            }
        };
    }

    private static String signoFGEM(BigDecimal valorFGEM) {
        return valorFGEM.signum() > 0 ? "+" : "";
    }
}
