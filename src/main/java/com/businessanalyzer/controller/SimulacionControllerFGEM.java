package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.PuntoEquilibrioFGEM;
import com.businessanalyzer.analytics.simulacion.BaseSimulacionFGEM;
import com.businessanalyzer.analytics.simulacion.EscenarioPredefinidoFGEM;
import com.businessanalyzer.analytics.simulacion.ResultadoEscenarioFGEM;
import com.businessanalyzer.analytics.simulacion.SimuladorFGEM;
import com.businessanalyzer.analytics.simulacion.SupuestosFGEM;
import com.businessanalyzer.analytics.simulacion.VariableFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EscenarioFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.service.SimulacionServiceFGEM;
import com.businessanalyzer.service.SimulacionServiceFGEM.TipoBaseFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.FormulariosFGEM;
import com.businessanalyzer.ui.GraficosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.TareasFGEM;
import com.businessanalyzer.ui.TarjetaIndicadorFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.MontosFGEM;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class SimulacionControllerFGEM implements VistaModuloFGEM {

    private record FilaResultadoFGEM(String conceptoFGEM, String baseFGEM, String escenarioFGEM, String diferenciaFGEM,
                                 String claseDiferenciaFGEM, boolean destacadaFGEM) {
    }

    private record FilaMetricaFGEM(String conceptoFGEM, List<String> valoresFGEM) {
    }

    private static final PseudoClass SUBTOTAL_FGEM = PseudoClass.getPseudoClass("subtotal");

    private final AppContextFGEM contextoFGEM;
    private final Map<VariableFGEM, ControlCambioFGEM> controlesFGEM = new EnumMap<>(VariableFGEM.class);
    private final TextField txtCostoAdicionalFGEM = new TextField("0");
    private final TextField txtTasaFGEM = new TextField("0");
    private BigDecimal tasaExactaFGEM;
    private String textoTasaExactaFGEM;
    private final Label lblErrorSupuestosFGEM = new Label();
    private BaseSimulacionFGEM baseFGEM;
    private List<EscenarioFGEM> guardadosFGEM = List.of();
    private MonedaFGEM monedaFGEM = MonedaFGEM.PEN;
    private boolean cargandoFGEM;
    private long solicitudActualFGEM;

    private final TarjetaIndicadorFGEM kpiUtilidadFGEM = new TarjetaIndicadorFGEM("Utilidad neta del escenario", "fth-dollar-sign");
    private final TarjetaIndicadorFGEM kpiMargenFGEM = new TarjetaIndicadorFGEM("Margen neto", "fth-percent");
    private final TarjetaIndicadorFGEM kpiEquilibrioFGEM = new TarjetaIndicadorFGEM("Punto de equilibrio", "fth-target");
    private final TarjetaIndicadorFGEM kpiSeguridadFGEM = new TarjetaIndicadorFGEM("Margen de seguridad", "fth-shield");

    @FXML private ComboBox<TipoBaseFGEM> cmbBaseFGEM;
    @FXML private Label lblBaseFGEM;
    @FXML private ProgressIndicator progresoFGEM;
    @FXML private HBox indicadoresFGEM;
    @FXML private VBox panelSupuestosFGEM;
    @FXML private TableView<FilaResultadoFGEM> tablaResultadosFGEM;
    @FXML private TableColumn<FilaResultadoFGEM, String> colConceptoFGEM;
    @FXML private TableColumn<FilaResultadoFGEM, String> colBaseFGEM;
    @FXML private TableColumn<FilaResultadoFGEM, String> colEscenarioFGEM;
    @FXML private TableColumn<FilaResultadoFGEM, FilaResultadoFGEM> colVariacionFGEM;
    @FXML private StackPane contenedorTornadoFGEM;
    @FXML private VBox lecturaSensibilidadFGEM;
    @FXML private TextField txtMetaFGEM;
    @FXML private VBox resultadoMetaFGEM;
    @FXML private ListView<EscenarioFGEM> listaEscenariosFGEM;
    @FXML private Button btnCargarFGEM;
    @FXML private Button btnEliminarFGEM;
    @FXML private TableView<FilaMetricaFGEM> tablaComparacionFGEM;
    @FXML private StackPane contenedorComparacionFGEM;

    public SimulacionControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        indicadoresFGEM.getChildren().addAll(kpiUtilidadFGEM, kpiMargenFGEM, kpiEquilibrioFGEM, kpiSeguridadFGEM);
        cmbBaseFGEM.getItems().setAll(TipoBaseFGEM.values());
        cmbBaseFGEM.setValue(TipoBaseFGEM.PROMEDIO_3_MESES);
        cmbBaseFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarBaseFGEM());
        construirPanelSupuestosFGEM();
        configurarTablaResultadosFGEM();
        listaEscenariosFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> {
            btnCargarFGEM.setDisable(nFGEM == null);
            btnEliminarFGEM.setDisable(nFGEM == null);
        });
        btnCargarFGEM.setDisable(true);
        btnEliminarFGEM.setDisable(true);
        listaEscenariosFGEM.setPlaceholder(new Label("Guarda un escenario para compararlo."));
    }

    @Override
    public void alMostrarFGEM() {
        cargarBaseFGEM();
    }

    private void cargarBaseFGEM() {
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        if (empresaFGEM == null) {
            lblBaseFGEM.setText("Selecciona una empresa en la barra superior.");
            baseFGEM = null;
            return;
        }
        monedaFGEM = empresaFGEM.monedaFGEM();
        TipoBaseFGEM tipoFGEM = cmbBaseFGEM.getValue();
        progresoFGEM.setVisible(true);
        long numeroFGEM = ++solicitudActualFGEM;
        TareasFGEM.ejecutarFGEM(() -> new Object[]{contextoFGEM.simulacionFGEM().baseFGEM(tipoFGEM), contextoFGEM.simulacionFGEM().listarEscenariosFGEM()},
                datosFGEM -> {
                    if (numeroFGEM != solicitudActualFGEM) {
                        return;
                    }
                    progresoFGEM.setVisible(false);
                    boolean primeraVezFGEM = baseFGEM == null;
                    baseFGEM = (BaseSimulacionFGEM) datosFGEM[0];
                    @SuppressWarnings("unchecked")
                    List<EscenarioFGEM> escenariosFGEM = (List<EscenarioFGEM>) datosFGEM[1];
                    guardadosFGEM = escenariosFGEM;
                    listaEscenariosFGEM.getItems().setAll(escenariosFGEM);
                    lblBaseFGEM.setText(baseFGEM.periodoFGEM().descripcionFGEM() + (baseFGEM.mesesFGEM() > 1 ? " (promedio mensual)" : "")
                            + (baseFGEM.sinVentasFGEM() ? "  ·  sin ventas en ese período" : ""));
                    if (primeraVezFGEM) {
                        aplicarFGEM(SupuestosFGEM.neutrosFGEM(baseFGEM.tasaImpuestoEfectivaFGEM()));
                    } else {
                        recalcularFGEM();
                    }
                },
                errorFGEM -> {
                    if (numeroFGEM == solicitudActualFGEM) {
                        progresoFGEM.setVisible(false);
                        NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(errorFGEM));
                    }
                });
    }

    private void construirPanelSupuestosFGEM() {
        Label tituloFGEM = new Label("Supuestos del escenario");
        tituloFGEM.getStyleClass().add("card-title");
        Label ayudaFGEM = new Label("Mueve las barras o escribe el porcentaje. Los resultados se recalculan al instante.");
        ayudaFGEM.getStyleClass().add("hint");
        ayudaFGEM.setWrapText(true);
        ayudaFGEM.setMinHeight(Region.USE_PREF_SIZE);

        FlowPane predefinidosFGEM = new FlowPane(8, 8);
        for (EscenarioPredefinidoFGEM pFGEM : EscenarioPredefinidoFGEM.values()) {
            Button botonFGEM = new Button(pFGEM.nombreFGEM());
            botonFGEM.getStyleClass().add("button-secondary");
            botonFGEM.setTooltip(new Tooltip(pFGEM.descripcionFGEM()));
            botonFGEM.setOnAction(eFGEM -> aplicarFGEM(pFGEM.aplicarFGEM(tasaActualFGEM())));
            predefinidosFGEM.getChildren().add(botonFGEM);
        }

        panelSupuestosFGEM.getChildren().addAll(tituloFGEM, ayudaFGEM, predefinidosFGEM);
        for (VariableFGEM variableFGEM : VariableFGEM.values()) {
            ControlCambioFGEM controlFGEM = new ControlCambioFGEM(variableFGEM);
            controlesFGEM.put(variableFGEM, controlFGEM);
            panelSupuestosFGEM.getChildren().add(controlFGEM.nodoFGEM());
        }

        txtCostoAdicionalFGEM.setPrefWidth(110);
        txtTasaFGEM.setPrefWidth(70);
        confirmarAlSalirFGEM(txtCostoAdicionalFGEM);
        confirmarAlSalirFGEM(txtTasaFGEM);
        GridPane extrasFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(extrasFGEM, "Costo fijo adicional (por mes)", txtCostoAdicionalFGEM);
        FormulariosFGEM.notaFGEM(extrasFGEM, "Ej.: 1500 si contratas a alguien; negativo si es un ahorro.");
        FormulariosFGEM.filaFGEM(extrasFGEM, "Impuesto a la renta (%)", txtTasaFGEM);
        extrasFGEM.getColumnConstraints().get(0).setMinWidth(170);
        extrasFGEM.getColumnConstraints().get(1).setMinWidth(100);

        lblErrorSupuestosFGEM.getStyleClass().add("form-error");
        lblErrorSupuestosFGEM.setWrapText(true);
        lblErrorSupuestosFGEM.setVisible(false);
        lblErrorSupuestosFGEM.managedProperty().bind(lblErrorSupuestosFGEM.visibleProperty());
        panelSupuestosFGEM.getChildren().addAll(extrasFGEM, lblErrorSupuestosFGEM);
    }

    private void confirmarAlSalirFGEM(TextField campoFGEM) {
        campoFGEM.setOnAction(eFGEM -> recalcularFGEM());
        campoFGEM.focusedProperty().addListener((oFGEM, aFGEM, enfocadoFGEM) -> {
            if (!enfocadoFGEM) {
                recalcularFGEM();
            }
        });
    }

    private void aplicarFGEM(SupuestosFGEM sFGEM) {
        cargandoFGEM = true;
        for (VariableFGEM variableFGEM : VariableFGEM.values()) {
            controlesFGEM.get(variableFGEM).establecerFGEM(sFGEM.valorFGEM(variableFGEM));
        }
        txtCostoAdicionalFGEM.setText(MontosFGEM.paraEditarFGEM(sFGEM.costoFijoAdicionalFGEM()).replace(".00", ""));
        txtTasaFGEM.setText(textoPorcentajeFGEM(sFGEM.tasaImpuestoFGEM().movePointRight(2).doubleValue()));
        tasaExactaFGEM = sFGEM.tasaImpuestoFGEM();
        textoTasaExactaFGEM = txtTasaFGEM.getText();
        cargandoFGEM = false;
        recalcularFGEM();
    }

    @FXML
    private void restablecerFGEM() {
        if (baseFGEM != null) {
            aplicarFGEM(SupuestosFGEM.neutrosFGEM(baseFGEM.tasaImpuestoEfectivaFGEM()));
        }
    }

    private BigDecimal tasaDelCampoFGEM() {
        if (tasaExactaFGEM != null && txtTasaFGEM.getText().equals(textoTasaExactaFGEM)) {
            return tasaExactaFGEM;
        }
        return MontosFGEM.leerFGEM(txtTasaFGEM.getText().replace("%", ""), "Impuesto a la renta").movePointLeft(2);
    }

    private BigDecimal tasaActualFGEM() {
        try {
            return tasaDelCampoFGEM();
        } catch (ValidacionExceptionFGEM eFGEM) {
            return baseFGEM == null ? BigDecimal.ZERO : baseFGEM.tasaImpuestoEfectivaFGEM();
        }
    }

    private SupuestosFGEM supuestosActualesFGEM() {
        SupuestosFGEM sFGEM = new SupuestosFGEM(
                controlesFGEM.get(VariableFGEM.PRECIO).fraccionFGEM(),
                controlesFGEM.get(VariableFGEM.VOLUMEN).fraccionFGEM(),
                controlesFGEM.get(VariableFGEM.COSTO_UNITARIO).fraccionFGEM(),
                controlesFGEM.get(VariableFGEM.GASTOS_VARIABLES).fraccionFGEM(),
                controlesFGEM.get(VariableFGEM.COSTOS_FIJOS).fraccionFGEM(),
                txtCostoAdicionalFGEM.getText().isBlank() ? BigDecimal.ZERO
                        : MontosFGEM.leerFGEM(txtCostoAdicionalFGEM.getText(), "Costo fijo adicional"),
                txtTasaFGEM.getText().isBlank() ? BigDecimal.ZERO : tasaDelCampoFGEM());
        SimulacionServiceFGEM.validarFGEM(sFGEM);
        return sFGEM;
    }

    private void recalcularFGEM() {
        if (cargandoFGEM || baseFGEM == null) {
            return;
        }
        SupuestosFGEM sFGEM;
        try {
            sFGEM = supuestosActualesFGEM();
            lblErrorSupuestosFGEM.setVisible(false);
        } catch (ValidacionExceptionFGEM eFGEM) {
            lblErrorSupuestosFGEM.setText(eFGEM.getMessage());
            lblErrorSupuestosFGEM.setVisible(true);
            return;
        }
        ResultadoEscenarioFGEM rBaseFGEM = SimuladorFGEM.simularFGEM(baseFGEM, SupuestosFGEM.neutrosFGEM(baseFGEM.tasaImpuestoEfectivaFGEM()));
        ResultadoEscenarioFGEM rEscenarioFGEM = SimuladorFGEM.simularFGEM(baseFGEM, sFGEM);
        mostrarIndicadoresFGEM(rBaseFGEM, rEscenarioFGEM);
        mostrarTablaFGEM(rBaseFGEM, rEscenarioFGEM);
        mostrarSensibilidadFGEM(sFGEM);
        mostrarComparacionFGEM(rBaseFGEM, rEscenarioFGEM);
        resultadoMetaFGEM.getChildren().clear();
    }

    private void mostrarIndicadoresFGEM(ResultadoEscenarioFGEM bFGEM, ResultadoEscenarioFGEM eFGEM) {
        BigDecimal diferenciaFGEM = eFGEM.utilidadNetaFGEM().subtract(bFGEM.utilidadNetaFGEM());
        kpiUtilidadFGEM.actualizarFGEM(dineroFGEM(eFGEM.utilidadNetaFGEM()), "Base " + dineroFGEM(bFGEM.utilidadNetaFGEM()) + "  ("
                + (diferenciaFGEM.signum() >= 0 ? "+" : "") + dineroFGEM(diferenciaFGEM) + ")");
        kpiMargenFGEM.actualizarFGEM(MontosFGEM.porcentajeFGEM(eFGEM.margenNetoFGEM()), "Base " + MontosFGEM.porcentajeFGEM(bFGEM.margenNetoFGEM()));
        PuntoEquilibrioFGEM peEscenarioFGEM = eFGEM.equilibrioFGEM();
        PuntoEquilibrioFGEM peBaseFGEM = bFGEM.equilibrioFGEM();
        kpiEquilibrioFGEM.actualizarFGEM(peEscenarioFGEM.ventasEquilibrioFGEM() == null ? "No alcanzable" : dineroFGEM(peEscenarioFGEM.ventasEquilibrioFGEM()),
                "Base " + (peBaseFGEM.ventasEquilibrioFGEM() == null ? "no alcanzable" : dineroFGEM(peBaseFGEM.ventasEquilibrioFGEM())));
        kpiSeguridadFGEM.actualizarFGEM(MontosFGEM.porcentajeFGEM(peEscenarioFGEM.margenSeguridadFGEM()),
                peEscenarioFGEM.alcanzadoFGEM() ? "Por encima del equilibrio" : "Por debajo del equilibrio");
    }

    private void mostrarTablaFGEM(ResultadoEscenarioFGEM bFGEM, ResultadoEscenarioFGEM eFGEM) {
        List<FilaResultadoFGEM> filasFGEM = new ArrayList<>();
        filasFGEM.add(montoFGEM("Ventas", bFGEM, eFGEM, ResultadoEscenarioFGEM::ventasFGEM, false, false));
        filasFGEM.add(montoFGEM("(−) Costo de ventas", bFGEM, eFGEM, ResultadoEscenarioFGEM::costoVentasFGEM, true, false));
        filasFGEM.add(montoFGEM("(−) Gastos variables", bFGEM, eFGEM, ResultadoEscenarioFGEM::gastosVariablesFGEM, true, false));
        filasFGEM.add(montoFGEM("= Margen de contribución", bFGEM, eFGEM, ResultadoEscenarioFGEM::margenContribucionFGEM, false, true));
        filasFGEM.add(montoFGEM("(−) Costos fijos", bFGEM, eFGEM, ResultadoEscenarioFGEM::costosFijosFGEM, true, false));
        filasFGEM.add(montoFGEM("(+) Otros ingresos", bFGEM, eFGEM, ResultadoEscenarioFGEM::otrosIngresosFGEM, false, false));
        filasFGEM.add(montoFGEM("= Utilidad antes de impuestos", bFGEM, eFGEM, ResultadoEscenarioFGEM::utilidadAntesImpuestosFGEM, false, true));
        filasFGEM.add(montoFGEM("(−) Impuesto a la renta", bFGEM, eFGEM, ResultadoEscenarioFGEM::impuestoFGEM, true, false));
        filasFGEM.add(montoFGEM("= Utilidad neta", bFGEM, eFGEM, ResultadoEscenarioFGEM::utilidadNetaFGEM, false, true));
        filasFGEM.add(porcentajeFilaFGEM("Margen neto", bFGEM.margenNetoFGEM(), eFGEM.margenNetoFGEM()));
        BigDecimal peBaseFGEM = bFGEM.equilibrioFGEM().ventasEquilibrioFGEM();
        BigDecimal peEscFGEM = eFGEM.equilibrioFGEM().ventasEquilibrioFGEM();
        String difPeFGEM = peBaseFGEM == null || peEscFGEM == null ? "—" : signoFGEM(peEscFGEM.subtract(peBaseFGEM)) + dineroFGEM(peEscFGEM.subtract(peBaseFGEM));
        String clasePeFGEM = peBaseFGEM == null || peEscFGEM == null || peEscFGEM.compareTo(peBaseFGEM) == 0 ? null
                : peEscFGEM.compareTo(peBaseFGEM) < 0 ? "margen-ok" : "margen-negativo";
        filasFGEM.add(new FilaResultadoFGEM("Punto de equilibrio", peBaseFGEM == null ? "No alcanzable" : dineroFGEM(peBaseFGEM),
                peEscFGEM == null ? "No alcanzable" : dineroFGEM(peEscFGEM), difPeFGEM, clasePeFGEM, false));
        filasFGEM.add(porcentajeFilaFGEM("Margen de seguridad", bFGEM.equilibrioFGEM().margenSeguridadFGEM(), eFGEM.equilibrioFGEM().margenSeguridadFGEM()));
        tablaResultadosFGEM.getItems().setAll(filasFGEM);
    }

    private FilaResultadoFGEM montoFGEM(String conceptoFGEM, ResultadoEscenarioFGEM bFGEM, ResultadoEscenarioFGEM eFGEM,
                                Function<ResultadoEscenarioFGEM, BigDecimal> valorFGEM, boolean esCostoFGEM, boolean destacadaFGEM) {
        BigDecimal vbFGEM = valorFGEM.apply(bFGEM);
        BigDecimal veFGEM = valorFGEM.apply(eFGEM);
        BigDecimal diferenciaFGEM = veFGEM.subtract(vbFGEM);
        String claseFGEM = diferenciaFGEM.signum() == 0 ? null
                : (diferenciaFGEM.signum() > 0) != esCostoFGEM ? "margen-ok" : "margen-negativo";
        return new FilaResultadoFGEM(conceptoFGEM, dineroFGEM(vbFGEM), dineroFGEM(veFGEM),
                diferenciaFGEM.signum() == 0 ? "—" : signoFGEM(diferenciaFGEM) + dineroFGEM(diferenciaFGEM), claseFGEM, destacadaFGEM);
    }

    private static FilaResultadoFGEM porcentajeFilaFGEM(String conceptoFGEM, BigDecimal baseFGEM, BigDecimal escenarioFGEM) {
        String diferenciaFGEM = "—";
        String claseFGEM = null;
        if (baseFGEM != null && escenarioFGEM != null && baseFGEM.compareTo(escenarioFGEM) != 0) {
            BigDecimal puntosFGEM = escenarioFGEM.subtract(baseFGEM).movePointRight(2).setScale(1, RoundingMode.HALF_UP);
            diferenciaFGEM = signoFGEM(puntosFGEM) + puntosFGEM + " pp";
            claseFGEM = puntosFGEM.signum() > 0 ? "margen-ok" : "margen-negativo";
        }
        return new FilaResultadoFGEM(conceptoFGEM, MontosFGEM.porcentajeFGEM(baseFGEM), MontosFGEM.porcentajeFGEM(escenarioFGEM), diferenciaFGEM, claseFGEM, false);
    }

    private void configurarTablaResultadosFGEM() {
        colConceptoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().conceptoFGEM()));
        colBaseFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().baseFGEM()));
        colEscenarioFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().escenarioFGEM()));
        colVariacionFGEM.setCellValueFactory(cFGEM -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colVariacionFGEM.setCellFactory(colFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(FilaResultadoFGEM filaFGEM, boolean vacioFGEM) {
                super.updateItem(filaFGEM, vacioFGEM);
                getStyleClass().removeAll("margen-ok", "margen-negativo");
                if (vacioFGEM || filaFGEM == null) {
                    setText(null);
                    return;
                }
                setText(filaFGEM.diferenciaFGEM());
                if (filaFGEM.claseDiferenciaFGEM() != null) {
                    getStyleClass().add(filaFGEM.claseDiferenciaFGEM());
                }
            }
        });
        tablaResultadosFGEM.setRowFactory(tFGEM -> new TableRow<>() {
            @Override
            protected void updateItem(FilaResultadoFGEM filaFGEM, boolean vacioFGEM) {
                super.updateItem(filaFGEM, vacioFGEM);
                pseudoClassStateChanged(SUBTOTAL_FGEM, !vacioFGEM && filaFGEM != null && filaFGEM.destacadaFGEM());
            }
        });
        tablaResultadosFGEM.setPlaceholder(new Label("Cargando la base..."));
    }

    private void mostrarSensibilidadFGEM(SupuestosFGEM sFGEM) {
        List<SimuladorFGEM.SensibilidadFGEM> sensibilidadFGEM = SimuladorFGEM.sensibilidadFGEM(baseFGEM, sFGEM);
        contenedorTornadoFGEM.getChildren().setAll(GraficosFGEM.visorFGEM(
                GraficosFGEM.tornadoFGEM(sensibilidadFGEM, SimuladorFGEM.VARIACION_SENSIBILIDAD_TEXTO_FGEM, monedaFGEM)));
        lecturaSensibilidadFGEM.getChildren().clear();
        if (baseFGEM.sinVentasFGEM()) {
            lecturaSensibilidadFGEM.getChildren().add(fraseFGEM("Sin ventas en la base, la sensibilidad no es representativa."));
            return;
        }
        SimuladorFGEM.SensibilidadFGEM mayorFGEM = sensibilidadFGEM.get(0);
        SimuladorFGEM.SensibilidadFGEM menorFGEM = sensibilidadFGEM.get(sensibilidadFGEM.size() - 1);
        lecturaSensibilidadFGEM.getChildren().addAll(
                fraseFGEM("Lo que más pesa es \"" + mayorFGEM.variableFGEM().descripcionFGEM() + "\": si sube "
                        + SimuladorFGEM.VARIACION_SENSIBILIDAD_TEXTO_FGEM + ", la utilidad neta mensual queda en "
                        + dineroFGEM(mayorFGEM.utilidadSiSubeFGEM()) + "; si baja, en " + dineroFGEM(mayorFGEM.utilidadSiBajaFGEM()) + "."),
                fraseFGEM("Lo que menos pesa es \"" + menorFGEM.variableFGEM().descripcionFGEM() + "\". Enfoca tus esfuerzos en las barras más largas."));
    }

    @FXML
    private void calcularMetaFGEM() {
        resultadoMetaFGEM.getChildren().clear();
        if (baseFGEM == null) {
            return;
        }
        BigDecimal metaFGEM;
        SupuestosFGEM sFGEM;
        try {
            metaFGEM = MontosFGEM.leerFGEM(txtMetaFGEM.getText(), "Utilidad neta deseada");
            sFGEM = supuestosActualesFGEM();
        } catch (ValidacionExceptionFGEM eFGEM) {
            resultadoMetaFGEM.getChildren().add(fraseFGEM(eFGEM.getMessage()));
            return;
        }
        Optional<BigDecimal> precioFGEM = SimuladorFGEM.cambioPrecioParaMetaFGEM(baseFGEM, sFGEM, metaFGEM);
        Optional<BigDecimal> volumenFGEM = SimuladorFGEM.cambioVolumenParaMetaFGEM(baseFGEM, sFGEM, metaFGEM);
        resultadoMetaFGEM.getChildren().add(resultadoMetaFilaFGEM("Solo con precios", precioFGEM, sFGEM.precioFGEM(),
                "No es posible: no hay ventas en la base."));
        resultadoMetaFGEM.getChildren().add(resultadoMetaFilaFGEM("Solo con volumen", volumenFGEM, sFGEM.volumenFGEM(),
                "No es posible: con estos precios y costos cada venta no deja margen, o la meta ya se supera aun sin vender."));
        resultadoMetaFGEM.getChildren().add(fraseFGEM("Los porcentajes son frente a la base. Puedes llevarlos al simulador para ver el detalle."));
    }

    private VBox resultadoMetaFilaFGEM(String tituloFGEM, Optional<BigDecimal> necesarioFGEM, BigDecimal actualFGEM, String imposibleFGEM) {
        Label encabezadoFGEM = new Label(tituloFGEM);
        encabezadoFGEM.getStyleClass().add("step-title");
        VBox cajaFGEM = new VBox(2, encabezadoFGEM);
        if (necesarioFGEM.isEmpty()) {
            cajaFGEM.getChildren().add(fraseFGEM(imposibleFGEM));
            return cajaFGEM;
        }
        BigDecimal valorFGEM = necesarioFGEM.get();
        Label cifraFGEM = new Label((valorFGEM.signum() >= 0 ? "+" : "") + MontosFGEM.porcentajeFGEM(valorFGEM));
        cifraFGEM.getStyleClass().add("total-valor-destacado");
        BigDecimal adicionalFGEM = valorFGEM.subtract(actualFGEM);
        cajaFGEM.getChildren().addAll(cifraFGEM, fraseFGEM(adicionalFGEM.signum() <= 0
                ? "El escenario actual ya alcanza la meta por este lado."
                : "Hace falta " + MontosFGEM.porcentajeFGEM(adicionalFGEM) + " más que en el escenario actual."));
        return cajaFGEM;
    }

    private void mostrarComparacionFGEM(ResultadoEscenarioFGEM rBaseFGEM, ResultadoEscenarioFGEM rActualFGEM) {
        List<String> nombresFGEM = new ArrayList<>(List.of("Base", "Escenario actual"));
        List<ResultadoEscenarioFGEM> resultadosFGEM = new ArrayList<>(List.of(rBaseFGEM, rActualFGEM));
        for (EscenarioFGEM eFGEM : guardadosFGEM) {
            nombresFGEM.add(eFGEM.nombreFGEM());
            resultadosFGEM.add(SimuladorFGEM.simularFGEM(baseFGEM, eFGEM.supuestosFGEM()));
        }
        tablaComparacionFGEM.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tablaComparacionFGEM.getColumns().clear();
        TableColumn<FilaMetricaFGEM, String> conceptoFGEM = new TableColumn<>("Concepto");
        conceptoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().conceptoFGEM()));
        conceptoFGEM.setPrefWidth(140);
        conceptoFGEM.setMinWidth(130);
        conceptoFGEM.setSortable(false);
        tablaComparacionFGEM.getColumns().add(conceptoFGEM);
        for (int iFGEM = 0; iFGEM < nombresFGEM.size(); iFGEM++) {
            int indiceFGEM = iFGEM;
            TableColumn<FilaMetricaFGEM, String> columnaFGEM = new TableColumn<>(nombresFGEM.get(iFGEM));
            columnaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().valoresFGEM().get(indiceFGEM)));
            columnaFGEM.getStyleClass().add("col-numero");
            columnaFGEM.setPrefWidth(118);
            columnaFGEM.setMinWidth(110);
            columnaFGEM.setSortable(false);
            tablaComparacionFGEM.getColumns().add(columnaFGEM);
        }
        tablaComparacionFGEM.getItems().setAll(
                metricaFGEM("Ventas", resultadosFGEM, rFGEM -> dineroFGEM(rFGEM.ventasFGEM())),
                metricaFGEM("Costos totales", resultadosFGEM, rFGEM -> dineroFGEM(rFGEM.costosTotalesFGEM())),
                metricaFGEM("Utilidad neta", resultadosFGEM, rFGEM -> dineroFGEM(rFGEM.utilidadNetaFGEM())),
                metricaFGEM("Margen neto", resultadosFGEM, rFGEM -> MontosFGEM.porcentajeFGEM(rFGEM.margenNetoFGEM())),
                metricaFGEM("Punto de equilibrio", resultadosFGEM, rFGEM -> rFGEM.equilibrioFGEM().ventasEquilibrioFGEM() == null
                        ? "No alcanzable" : dineroFGEM(rFGEM.equilibrioFGEM().ventasEquilibrioFGEM())));
        org.jfree.chart.fx.ChartViewer visorFGEM = GraficosFGEM.visorFGEM(GraficosFGEM.comparacionEscenariosFGEM(nombresFGEM, resultadosFGEM, monedaFGEM));
        visorFGEM.setMinSize(200, 100);
        contenedorComparacionFGEM.getChildren().setAll(visorFGEM);
    }

    private static FilaMetricaFGEM metricaFGEM(String conceptoFGEM, List<ResultadoEscenarioFGEM> resultadosFGEM,
                                       Function<ResultadoEscenarioFGEM, String> valorFGEM) {
        return new FilaMetricaFGEM(conceptoFGEM, resultadosFGEM.stream().map(valorFGEM).toList());
    }

    @FXML
    private void guardarEscenarioFGEM() {
        SupuestosFGEM sFGEM;
        try {
            sFGEM = supuestosActualesFGEM();
        } catch (ValidacionExceptionFGEM eFGEM) {
            NotificadorFGEM.errorFGEM(eFGEM.getMessage());
            return;
        }
        TextField nombreFGEM = new TextField();
        TextField descripcionFGEM = new TextField();
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Nombre *", nombreFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Descripción", descripcionFGEM);
        EscenarioFGEM[] guardadoFGEM = new EscenarioFGEM[1];
        boolean okFGEM = DialogosFGEM.formularioFGEM(tablaResultadosFGEM.getScene().getWindow(), "Guardar escenario",
                "Si ya existe un escenario con ese nombre, se reemplazará.", grillaFGEM, "Guardar",
                () -> guardadoFGEM[0] = contextoFGEM.simulacionFGEM().guardarFGEM(nombreFGEM.getText(), descripcionFGEM.getText(), sFGEM));
        if (okFGEM) {
            NotificadorFGEM.exitoFGEM("Escenario \"" + guardadoFGEM[0].nombreFGEM() + "\" guardado.");
            cargarBaseFGEM();
        }
    }

    @FXML
    private void cargarEscenarioFGEM() {
        EscenarioFGEM eFGEM = listaEscenariosFGEM.getSelectionModel().getSelectedItem();
        if (eFGEM != null) {
            aplicarFGEM(eFGEM.supuestosFGEM());
            NotificadorFGEM.infoFGEM("Escenario \"" + eFGEM.nombreFGEM() + "\" cargado en el simulador.");
        }
    }

    @FXML
    private void eliminarEscenarioFGEM() {
        EscenarioFGEM eFGEM = listaEscenariosFGEM.getSelectionModel().getSelectedItem();
        if (eFGEM == null || !DialogosFGEM.confirmarFGEM(tablaResultadosFGEM.getScene().getWindow(), "Eliminar escenario",
                "¿Eliminar el escenario \"" + eFGEM.nombreFGEM() + "\"?")) {
            return;
        }
        try {
            contextoFGEM.simulacionFGEM().eliminarFGEM(eFGEM.idFGEM());
            NotificadorFGEM.exitoFGEM("Escenario eliminado.");
            cargarBaseFGEM();
        } catch (RuntimeException exFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(exFGEM));
        }
    }

    private String dineroFGEM(BigDecimal valorFGEM) {
        return MontosFGEM.formatearFGEM(valorFGEM.setScale(2, RoundingMode.HALF_UP), monedaFGEM);
    }

    private static String signoFGEM(BigDecimal valorFGEM) {
        return valorFGEM.signum() > 0 ? "+" : "";
    }

    private static Label fraseFGEM(String textoFGEM) {
        Label labelFGEM = new Label("•  " + textoFGEM);
        labelFGEM.getStyleClass().add("lectura");
        labelFGEM.setWrapText(true);
        labelFGEM.setMinHeight(Region.USE_PREF_SIZE);
        return labelFGEM;
    }

    private static String textoPorcentajeFGEM(double valorFGEM) {
        String textoFGEM = String.format(Locale.US, "%.2f", valorFGEM);
        return textoFGEM.replaceAll("\\.?0+$", "");
    }

    private final class ControlCambioFGEM {
        private final VariableFGEM variableFGEM;
        private final Slider barraFGEM = new Slider(-50, 100, 0);
        private final TextField textoFGEM = new TextField("0");
        private double valorFGEM;
        private boolean moviendoFGEM;

        ControlCambioFGEM(VariableFGEM variableFGEM) {
            this.variableFGEM = variableFGEM;
            barraFGEM.setBlockIncrement(1);
            barraFGEM.setMajorTickUnit(25);
            barraFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> {
                if (!moviendoFGEM) {
                    fijarFGEM(Math.round(nFGEM.doubleValue()), false);
                }
            });
            textoFGEM.setPrefWidth(64);
            textoFGEM.setAlignment(Pos.CENTER_RIGHT);
            textoFGEM.setOnAction(eFGEM -> confirmarTextoFGEM());
            textoFGEM.focusedProperty().addListener((oFGEM, aFGEM, enfocadoFGEM) -> {
                if (!enfocadoFGEM) {
                    confirmarTextoFGEM();
                }
            });
        }

        VBox nodoFGEM() {
            Label nombreFGEM = new Label(variableFGEM.descripcionFGEM());
            nombreFGEM.getStyleClass().add("info-value");
            Region espacioFGEM = new Region();
            HBox.setHgrow(espacioFGEM, Priority.ALWAYS);
            Label signoPorcentajeFGEM = new Label("%");
            signoPorcentajeFGEM.getStyleClass().add("muted");
            HBox cabeceraFGEM = new HBox(6, nombreFGEM, espacioFGEM, textoFGEM, signoPorcentajeFGEM);
            cabeceraFGEM.setAlignment(Pos.CENTER_LEFT);
            Tooltip ayudaFGEM = new Tooltip(variableFGEM.ayudaFGEM());
            nombreFGEM.setTooltip(ayudaFGEM);
            barraFGEM.setTooltip(ayudaFGEM);
            return new VBox(4, cabeceraFGEM, barraFGEM);
        }

        BigDecimal fraccionFGEM() {
            return BigDecimal.valueOf(valorFGEM).movePointLeft(2);
        }

        void establecerFGEM(BigDecimal fraccionFGEM) {
            fijarFGEM(fraccionFGEM.movePointRight(2).doubleValue(), true);
        }

        private void confirmarTextoFGEM() {
            try {
                fijarFGEM(MontosFGEM.leerFGEM(textoFGEM.getText().replace("%", "").replace("+", ""), variableFGEM.descripcionFGEM()).doubleValue(), true);
            } catch (ValidacionExceptionFGEM eFGEM) {
                textoFGEM.setText(textoPorcentajeFGEM(valorFGEM));
            }
        }

        private void fijarFGEM(double nuevoFGEM, boolean moverBarraFGEM) {
            valorFGEM = nuevoFGEM;
            moviendoFGEM = true;
            if (moverBarraFGEM) {
                barraFGEM.setValue(Math.max(barraFGEM.getMin(), Math.min(barraFGEM.getMax(), nuevoFGEM)));
            }
            textoFGEM.setText(textoPorcentajeFGEM(nuevoFGEM));
            moviendoFGEM = false;
            recalcularFGEM();
        }
    }
}
