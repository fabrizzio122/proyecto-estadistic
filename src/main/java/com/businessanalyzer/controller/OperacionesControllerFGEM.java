package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.CalculadoraOperacionFGEM;
import com.businessanalyzer.analytics.ResumenOperacionesFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.exception.StockInsuficienteExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CondicionPagoFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM.DatosLineaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.LineaOperacionFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.OperacionFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.service.OperacionServiceFGEM;
import com.businessanalyzer.ui.CamposFechaFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.FormulariosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.SelectorCuentaFGEM;
import com.businessanalyzer.ui.TarjetaIndicadorFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/** Módulo "Ventas y compras": registro, consulta y anulación de operaciones comerciales. */
public class OperacionesControllerFGEM implements VistaModuloFGEM {

    private final AppContextFGEM contextoFGEM;
    private final ObservableList<OperacionFGEM> operacionesFGEM = FXCollections.observableArrayList();
    private final FilteredList<OperacionFGEM> filtradasFGEM = new FilteredList<>(operacionesFGEM);
    private final ToggleGroup grupoTipoFGEM = new ToggleGroup();
    private MonedaFGEM monedaFGEM = MonedaFGEM.PEN;

    private final TarjetaIndicadorFGEM kpiVentasFGEM = new TarjetaIndicadorFGEM("Ventas netas", "fth-trending-up");
    private final TarjetaIndicadorFGEM kpiMargenFGEM = new TarjetaIndicadorFGEM("Margen bruto", "fth-percent");
    private final TarjetaIndicadorFGEM kpiComprasFGEM = new TarjetaIndicadorFGEM("Compras netas", "fth-truck");
    private final TarjetaIndicadorFGEM kpiDiferenciaFGEM = new TarjetaIndicadorFGEM("Ventas − compras", "fth-repeat");

    @FXML private HBox indicadoresFGEM;
    @FXML private ToggleButton tglVentasFGEM;
    @FXML private ToggleButton tglComprasFGEM;
    @FXML private DatePicker dpDesdeFGEM;
    @FXML private DatePicker dpHastaFGEM;
    @FXML private TextField txtBuscarFGEM;
    @FXML private CheckBox chkAnuladasFGEM;
    @FXML private TableView<OperacionFGEM> tablaFGEM;
    @FXML private TableColumn<OperacionFGEM, String> colNumeroFGEM;
    @FXML private TableColumn<OperacionFGEM, LocalDate> colFechaFGEM;
    @FXML private TableColumn<OperacionFGEM, String> colContraparteFGEM;
    @FXML private TableColumn<OperacionFGEM, String> colComprobanteFGEM;
    @FXML private TableColumn<OperacionFGEM, Number> colItemsFGEM;
    @FXML private TableColumn<OperacionFGEM, BigDecimal> colSubtotalFGEM;
    @FXML private TableColumn<OperacionFGEM, BigDecimal> colImpuestoFGEM;
    @FXML private TableColumn<OperacionFGEM, BigDecimal> colTotalFGEM;
    @FXML private TableColumn<OperacionFGEM, BigDecimal> colMargenFGEM;
    @FXML private TableColumn<OperacionFGEM, OperacionFGEM> colPagoFGEM;
    @FXML private TableColumn<OperacionFGEM, OperacionFGEM> colEstadoFGEM;
    @FXML private Button btnDetalleFGEM;
    @FXML private Button btnAnularFGEM;
    @FXML private Button btnEditarFGEM;
    @FXML private Button btnNuevaFGEM;
    @FXML private Label lblPieFGEM;

    public OperacionesControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        indicadoresFGEM.getChildren().addAll(kpiVentasFGEM, kpiMargenFGEM, kpiComprasFGEM, kpiDiferenciaFGEM);

        tglVentasFGEM.setToggleGroup(grupoTipoFGEM);
        tglComprasFGEM.setToggleGroup(grupoTipoFGEM);
        grupoTipoFGEM.selectedToggleProperty().addListener((oFGEM, anteriorFGEM, nuevoFGEM) -> {
            if (nuevoFGEM == null) {
                anteriorFGEM.setSelected(true);
            } else {
                cargarFGEM();
            }
        });

        CamposFechaFGEM.configurarFGEM(dpDesdeFGEM);
        CamposFechaFGEM.configurarFGEM(dpHastaFGEM);
        LocalDate hoyFGEM = LocalDate.now();
        dpDesdeFGEM.setValue(hoyFGEM.withDayOfMonth(1));
        dpHastaFGEM.setValue(hoyFGEM);
        dpDesdeFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarFGEM());
        dpHastaFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarFGEM());
        txtBuscarFGEM.textProperty().addListener((oFGEM, aFGEM, nFGEM) -> filtrarFGEM());

        configurarTablaFGEM();
        boolean gestionaFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.OPERACIONES_GESTIONAR);
        for (Node nodoFGEM : List.of(btnNuevaFGEM, btnEditarFGEM, btnAnularFGEM)) {
            nodoFGEM.setVisible(gestionaFGEM);
            nodoFGEM.setManaged(gestionaFGEM);
        }
    }

    @Override
    public void alMostrarFGEM() {
        AppContextFGEM.CompraPropuestaFGEM propuestaFGEM = contextoFGEM.tomarCompraPropuestaFGEM().orElse(null);
        if (propuestaFGEM != null && btnNuevaFGEM.isVisible()) {
            tglComprasFGEM.setSelected(true);
            cargarFGEM();
            // Después de mostrar la pantalla, para que el formulario se abra sobre ella.
            javafx.application.Platform.runLater(() -> abrirNuevaFGEM(propuestaFGEM));
            return;
        }
        cargarFGEM();
    }

    private TipoOperacionFGEM tipoFGEM() {
        return tglComprasFGEM.isSelected() ? TipoOperacionFGEM.COMPRA : TipoOperacionFGEM.VENTA;
    }

    // ------------------------------------------------------------------ carga

    @FXML
    private void esteMesFGEM() {
        LocalDate hoyFGEM = LocalDate.now();
        dpHastaFGEM.setValue(hoyFGEM);
        dpDesdeFGEM.setValue(hoyFGEM.withDayOfMonth(1));
    }

    @FXML
    private void cargarFGEM() {
        TipoOperacionFGEM tipoFGEM = tipoFGEM();
        boolean esVentaFGEM = tipoFGEM == TipoOperacionFGEM.VENTA;
        btnNuevaFGEM.setText("Nueva " + tipoFGEM.descripcionFGEM().toLowerCase());
        colContraparteFGEM.setText(tipoFGEM.contraparteFGEM());
        colMargenFGEM.setVisible(esVentaFGEM);

        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        btnNuevaFGEM.setDisable(empresaFGEM == null);
        if (empresaFGEM == null) {
            operacionesFGEM.clear();
            tablaFGEM.setPlaceholder(new Label("Selecciona una empresa en la barra superior para ver sus operaciones."));
            actualizarIndicadoresFGEM(ResumenOperacionesFGEM.VACIO_FGEM);
            lblPieFGEM.setText("");
            return;
        }
        monedaFGEM = empresaFGEM.monedaFGEM();
        tablaFGEM.setPlaceholder(new Label("No hay " + tipoFGEM.pluralFGEM().toLowerCase() + " en el período elegido."));

        LocalDate desdeFGEM = dpDesdeFGEM.getValue();
        LocalDate hastaFGEM = dpHastaFGEM.getValue();
        if (desdeFGEM == null || hastaFGEM == null) {
            return;
        }
        try {
            operacionesFGEM.setAll(servicioFGEM().listarFGEM(tipoFGEM, desdeFGEM, hastaFGEM, chkAnuladasFGEM.isSelected()));
            actualizarIndicadoresFGEM(servicioFGEM().resumenFGEM(desdeFGEM, hastaFGEM));
        } catch (RuntimeException eFGEM) {
            operacionesFGEM.clear();
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        filtrarFGEM();
        actualizarAccionesFGEM();
    }

    private void actualizarIndicadoresFGEM(ResumenOperacionesFGEM rFGEM) {
        kpiVentasFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(rFGEM.ventasNetasFGEM(), monedaFGEM),
                pluralFGEM(rFGEM.cantidadVentasFGEM(), "venta", "ventas") + " · ticket promedio "
                        + MontosFGEM.formatearFGEM(rFGEM.ticketPromedioFGEM(), monedaFGEM));
        kpiMargenFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(rFGEM.margenBrutoFGEM(), monedaFGEM),
                MontosFGEM.porcentajeFGEM(rFGEM.margenBrutoSobreVentasFGEM()) + " de las ventas netas");
        kpiComprasFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(rFGEM.comprasNetasFGEM(), monedaFGEM),
                pluralFGEM(rFGEM.cantidadComprasFGEM(), "compra", "compras") + " en el período");
        kpiDiferenciaFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(rFGEM.ventasNetasFGEM().subtract(rFGEM.comprasNetasFGEM()), monedaFGEM),
                "Montos sin impuestos y sin anuladas");
    }

    private void filtrarFGEM() {
        String textoFGEM = txtBuscarFGEM.getText() == null ? "" : txtBuscarFGEM.getText().strip().toLowerCase(Locale.ROOT);
        filtradasFGEM.setPredicate(oFGEM -> textoFGEM.isEmpty()
                || oFGEM.numeroFGEM().toLowerCase(Locale.ROOT).contains(textoFGEM)
                || contieneFGEM(oFGEM.contraparteFGEM(), textoFGEM)
                || contieneFGEM(oFGEM.documentoContraparteFGEM(), textoFGEM)
                || contieneFGEM(oFGEM.comprobanteFGEM(), textoFGEM));
        BigDecimal totalFGEM = filtradasFGEM.stream().filter(oFGEM -> !oFGEM.anuladaFGEM())
                .map(OperacionFGEM::totalFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblPieFGEM.setText(filtradasFGEM.size() + " operaciones en la lista · total con impuestos (sin anuladas): "
                + MontosFGEM.formatearFGEM(totalFGEM, monedaFGEM));
    }

    // ------------------------------------------------------------------ acciones

    @FXML
    private void nuevaFGEM() {
        abrirNuevaFGEM(null);
    }

    /** Formulario de nueva operación; con {@code propuesta}, una compra ya llena con la reposición sugerida. */
    private void abrirNuevaFGEM(AppContextFGEM.CompraPropuestaFGEM propuestaFGEM) {
        TipoOperacionFGEM tipoFGEM = propuestaFGEM != null ? TipoOperacionFGEM.COMPRA : tipoFGEM();
        FormularioOperacionFGEM formularioFGEM;
        String avisoFGEM = "Precios unitarios sin impuestos. El número se asigna al registrar.";
        try {
            formularioFGEM = new FormularioOperacionFGEM(tipoFGEM, servicioFGEM().productosDisponiblesFGEM(), servicioFGEM().tasaImpuestoSugeridaFGEM());
            if (propuestaFGEM != null) {
                int faltantesFGEM = formularioFGEM.cargarPropuestaFGEM(propuestaFGEM);
                avisoFGEM = "Compra armada con la reposición sugerida, al costo registrado de cada producto. Revisa las cantidades y los "
                        + "precios del proveedor, quita lo que no vayas a comprar y completa el proveedor."
                        + (faltantesFGEM > 0 ? " " + faltantesFGEM + (faltantesFGEM == 1 ? " producto no se agregó porque está archivado."
                        : " productos no se agregaron porque están archivados.") : "");
            }
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        OperacionFGEM[] registradaFGEM = new OperacionFGEM[1];
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Nueva " + tipoFGEM.descripcionFGEM().toLowerCase(),
                avisoFGEM,
                formularioFGEM.contenidoFGEM(), "Registrar " + tipoFGEM.descripcionFGEM().toLowerCase(), 860, () -> {
                    DatosOperacionFGEM datosFGEM = formularioFGEM.datosFGEM();
                    try {
                        registradaFGEM[0] = servicioFGEM().registrarFGEM(datosFGEM, false);
                    } catch (StockInsuficienteExceptionFGEM eFGEM) {
                        boolean confirmarFGEM = DialogosFGEM.confirmarFGEM(formularioFGEM.ventanaFGEM(), "Stock insuficiente",
                                eFGEM.getMessage() + "\n\n¿Registrar la venta de todas formas? El stock quedará en negativo.");
                        if (!confirmarFGEM) {
                            throw new ValidacionExceptionFGEM("Venta no registrada. Ajusta las cantidades o registra "
                                    + "primero la compra de la mercadería.");
                        }
                        registradaFGEM[0] = servicioFGEM().registrarFGEM(datosFGEM, true);
                    }
                });
        if (okFGEM) {
            cargarFGEM();
            seleccionarFGEM(registradaFGEM[0].idFGEM());
            NotificadorFGEM.exitoFGEM(tipoFGEM.descripcionFGEM() + " " + registradaFGEM[0].numeroFGEM() + " registrada por "
                    + MontosFGEM.formatearFGEM(registradaFGEM[0].totalFGEM(), monedaFGEM) + ".");
        }
    }

    /** Corrige la operación seleccionada: la original queda anulada y se registra la versión corregida. */
    @FXML
    private void editarFGEM() {
        OperacionFGEM seleccionadaFGEM = seleccionadaFGEM();
        if (seleccionadaFGEM == null || seleccionadaFGEM.anuladaFGEM()) {
            return;
        }
        FormularioOperacionFGEM formularioFGEM;
        try {
            OperacionFGEM originalFGEM = servicioFGEM().obtenerFGEM(seleccionadaFGEM.idFGEM());
            formularioFGEM = new FormularioOperacionFGEM(originalFGEM.tipoFGEM(), servicioFGEM().productosDisponiblesFGEM(),
                    originalFGEM.tasaImpuestoFGEM());
            formularioFGEM.cargarDesdeFGEM(originalFGEM);
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        OperacionFGEM[] corregidaFGEM = new OperacionFGEM[1];
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Editar " + seleccionadaFGEM.numeroFGEM(),
                "Al guardar, " + seleccionadaFGEM.numeroFGEM() + " queda anulada como historial y los datos corregidos se "
                        + "registran con un número nuevo. El stock y los totales se recalculan solos.",
                formularioFGEM.contenidoFGEM(), "Guardar cambios", 860, () -> {
                    DatosOperacionFGEM datosFGEM = formularioFGEM.datosFGEM();
                    try {
                        corregidaFGEM[0] = servicioFGEM().corregirFGEM(seleccionadaFGEM.idFGEM(), datosFGEM, false);
                    } catch (StockInsuficienteExceptionFGEM eFGEM) {
                        boolean confirmarFGEM = DialogosFGEM.confirmarFGEM(formularioFGEM.ventanaFGEM(), "Stock insuficiente",
                                eFGEM.getMessage() + "\n\n¿Guardar la venta de todas formas? El stock quedará en negativo.");
                        if (!confirmarFGEM) {
                            throw new ValidacionExceptionFGEM("Cambios no guardados. Ajusta las cantidades.");
                        }
                        corregidaFGEM[0] = servicioFGEM().corregirFGEM(seleccionadaFGEM.idFGEM(), datosFGEM, true);
                    }
                });
        if (okFGEM) {
            cargarFGEM();
            seleccionarFGEM(corregidaFGEM[0].idFGEM());
            NotificadorFGEM.exitoFGEM(seleccionadaFGEM.numeroFGEM() + " corregida: ahora es " + corregidaFGEM[0].numeroFGEM() + ".");
        }
    }

    @FXML
    private void verDetalleFGEM() {
        OperacionFGEM seleccionadaFGEM = seleccionadaFGEM();
        if (seleccionadaFGEM == null) {
            return;
        }
        OperacionFGEM operacionFGEM;
        try {
            operacionFGEM = servicioFGEM().obtenerFGEM(seleccionadaFGEM.idFGEM());
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        DialogosFGEM.informacionFGEM(ventanaFGEM(), operacionFGEM.tipoFGEM().descripcionFGEM() + " " + operacionFGEM.numeroFGEM(), null,
                detalleFGEM(operacionFGEM));
    }

    @FXML
    private void anularFGEM() {
        OperacionFGEM operacionFGEM = seleccionadaFGEM();
        if (operacionFGEM == null || operacionFGEM.anuladaFGEM()) {
            return;
        }
        TextField motivoFGEM = new TextField();
        motivoFGEM.setPromptText("Ej.: devolución del cliente, error de digitación…");
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Motivo *", motivoFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Anular " + operacionFGEM.numeroFGEM(),
                "La operación dejará de contar en el stock y en los totales, pero se conservará como anulada. "
                        + "Esta acción no se puede deshacer.",
                grillaFGEM, "Anular operación", () -> servicioFGEM().anularFGEM(operacionFGEM.idFGEM(), motivoFGEM.getText()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM(operacionFGEM.numeroFGEM() + " anulada.");
        }
    }

    // ------------------------------------------------------------------ tabla

    private void configurarTablaFGEM() {
        colNumeroFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().numeroFGEM()));
        colFechaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().fechaFGEM()));
        colFechaFGEM.setCellFactory(colFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fechaFGEM, boolean vacioFGEM) {
                super.updateItem(fechaFGEM, vacioFGEM);
                setText(vacioFGEM || fechaFGEM == null ? null : FechasFGEM.paraMostrarFGEM(fechaFGEM));
            }
        });
        colContraparteFGEM.setCellValueFactory(cFGEM -> textoFGEM(Objects.requireNonNullElse(cFGEM.getValue().contraparteFGEM(), "")));
        colComprobanteFGEM.setCellValueFactory(cFGEM -> textoFGEM(Objects.requireNonNullElse(cFGEM.getValue().comprobanteFGEM(), "")));
        colItemsFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().cantidadItemsFGEM()));
        configurarMontoFGEM(colSubtotalFGEM, OperacionFGEM::subtotalFGEM);
        configurarMontoFGEM(colImpuestoFGEM, OperacionFGEM::impuestoFGEM);
        configurarMontoFGEM(colTotalFGEM, OperacionFGEM::totalFGEM);
        configurarMontoFGEM(colMargenFGEM, OperacionFGEM::margenBrutoFGEM);
        colPagoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colPagoFGEM.setSortable(false);
        colPagoFGEM.setCellFactory(colFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(OperacionFGEM oFGEM, boolean vacioFGEM) {
                super.updateItem(oFGEM, vacioFGEM);
                getStyleClass().remove("margen-negativo");
                if (vacioFGEM || oFGEM == null) {
                    setText(null);
                    return;
                }
                setText(oFGEM.anuladaFGEM() ? "—" : textoPagoFGEM(oFGEM).replace("Al contado · ", ""));
                if (!oFGEM.anuladaFGEM() && oFGEM.vencidaEnFGEM(LocalDate.now())) {
                    getStyleClass().add("margen-negativo");
                }
            }
        });
        colEstadoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colEstadoFGEM.setSortable(false);
        colEstadoFGEM.setCellFactory(colFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(OperacionFGEM oFGEM, boolean vacioFGEM) {
                super.updateItem(oFGEM, vacioFGEM);
                setText(null);
                if (vacioFGEM || oFGEM == null) {
                    setGraphic(null);
                    return;
                }
                Label estadoFGEM = new Label(oFGEM.estadoFGEM().descripcionFGEM());
                estadoFGEM.getStyleClass().addAll("badge", oFGEM.anuladaFGEM() ? "badge-bloqueado" : "badge-activo");
                setGraphic(estadoFGEM);
            }
        });

        SortedList<OperacionFGEM> ordenadasFGEM = new SortedList<>(filtradasFGEM);
        ordenadasFGEM.comparatorProperty().bind(tablaFGEM.comparatorProperty());
        tablaFGEM.setItems(ordenadasFGEM);
        tablaFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarAccionesFGEM());
        tablaFGEM.setRowFactory(tFGEM -> {
            TableRow<OperacionFGEM> filaFGEM = new TableRow<>() {
                @Override
                protected void updateItem(OperacionFGEM operacionFGEM, boolean vacioFGEM) {
                    super.updateItem(operacionFGEM, vacioFGEM);
                    pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("archivado"),
                            !vacioFGEM && operacionFGEM != null && operacionFGEM.anuladaFGEM());
                }
            };
            filaFGEM.setOnMouseClicked(eFGEM -> {
                if (eFGEM.getButton() == MouseButton.PRIMARY && eFGEM.getClickCount() == 2 && !filaFGEM.isEmpty()) {
                    verDetalleFGEM();
                }
            });
            return filaFGEM;
        });
        actualizarAccionesFGEM();
    }

    private void configurarMontoFGEM(TableColumn<OperacionFGEM, BigDecimal> columnaFGEM, Function<OperacionFGEM, BigDecimal> valorFGEM) {
        columnaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(valorFGEM.apply(cFGEM.getValue())));
        columnaFGEM.setCellFactory(colFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal montoFGEM, boolean vacioFGEM) {
                super.updateItem(montoFGEM, vacioFGEM);
                getStyleClass().remove("margen-negativo");
                setText(vacioFGEM || montoFGEM == null ? null : MontosFGEM.formatearFGEM(montoFGEM, monedaFGEM));
                if (!vacioFGEM && montoFGEM != null && montoFGEM.signum() < 0) {
                    getStyleClass().add("margen-negativo");
                }
            }
        });
    }

    private void actualizarAccionesFGEM() {
        OperacionFGEM operacionFGEM = seleccionadaFGEM();
        btnDetalleFGEM.setDisable(operacionFGEM == null);
        btnAnularFGEM.setDisable(operacionFGEM == null || operacionFGEM.anuladaFGEM());
        btnEditarFGEM.setDisable(operacionFGEM == null || operacionFGEM.anuladaFGEM());
    }

    // ------------------------------------------------------------------ detalle

    private Node detalleFGEM(OperacionFGEM oFGEM) {
        GridPane datosFGEM = new GridPane();
        datosFGEM.setHgap(14);
        datosFGEM.setVgap(8);
        datosFGEM.getColumnConstraints().addAll(new ColumnConstraints(110), new ColumnConstraints(200),
                new ColumnConstraints(110), new ColumnConstraints(180));
        agregarDatoFGEM(datosFGEM, 0, 0, "Fecha", FechasFGEM.paraMostrarFGEM(oFGEM.fechaFGEM()));
        agregarDatoFGEM(datosFGEM, 2, 0, "Estado", oFGEM.estadoFGEM().descripcionFGEM());
        agregarDatoFGEM(datosFGEM, 0, 1, oFGEM.tipoFGEM().contraparteFGEM(), Objects.requireNonNullElse(oFGEM.contraparteFGEM(), "—"));
        agregarDatoFGEM(datosFGEM, 2, 1, "Documento", Objects.requireNonNullElse(oFGEM.documentoContraparteFGEM(), "—"));
        agregarDatoFGEM(datosFGEM, 0, 2, "Comprobante", Objects.requireNonNullElse(oFGEM.comprobanteFGEM(), "—"));
        agregarDatoFGEM(datosFGEM, 2, 2, "Registrado por", oFGEM.registradoPorFGEM() + " · " + FechasFGEM.paraMostrarFGEM(oFGEM.registradoEnFGEM()));
        agregarDatoFGEM(datosFGEM, 0, 3, "Pago", textoPagoFGEM(oFGEM));
        agregarDatoFGEM(datosFGEM, 2, 3, "Vence", oFGEM.alCreditoFGEM() ? FechasFGEM.paraMostrarFGEM(oFGEM.vencimientoFGEM()) : "—");
        int filaFGEM = 4;
        if (oFGEM.observacionesFGEM() != null) {
            agregarDatoFGEM(datosFGEM, 0, filaFGEM++, "Observaciones", oFGEM.observacionesFGEM());
        }
        if (oFGEM.anuladaFGEM()) {
            agregarDatoFGEM(datosFGEM, 0, filaFGEM, "Anulada", oFGEM.motivoAnulacionFGEM() + " (" + oFGEM.anuladoPorFGEM() + ", "
                    + FechasFGEM.paraMostrarFGEM(oFGEM.anuladoEnFGEM()) + ")");
        }

        TableView<LineaOperacionFGEM> lineasFGEM = new TableView<>(FXCollections.observableArrayList(oFGEM.lineasFGEM()));
        lineasFGEM.getColumns().add(columnaFGEM("Código", LineaOperacionFGEM::codigoFGEM, 80, false));
        lineasFGEM.getColumns().add(columnaFGEM("Producto o servicio", LineaOperacionFGEM::nombreFGEM, 220, false));
        lineasFGEM.getColumns().add(columnaFGEM("Cantidad", lFGEM -> MontosFGEM.cantidadFGEM(lFGEM.cantidadFGEM()) + " " + lFGEM.unidadFGEM().abreviaturaFGEM(), 90, true));
        lineasFGEM.getColumns().add(columnaFGEM("Precio unit.", lFGEM -> MontosFGEM.formatearFGEM(lFGEM.precioUnitarioFGEM(), monedaFGEM), 100, true));
        lineasFGEM.getColumns().add(columnaFGEM("Subtotal", lFGEM -> MontosFGEM.formatearFGEM(lFGEM.subtotalFGEM(), monedaFGEM), 100, true));
        lineasFGEM.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        lineasFGEM.setPrefHeight(Math.min(320, 48 + 40.0 * oFGEM.lineasFGEM().size()));

        VBox totalesFGEM = bloqueTotalesFGEM(oFGEM.subtotalFGEM(), oFGEM.tasaImpuestoFGEM(), oFGEM.impuestoFGEM(), oFGEM.totalFGEM(),
                oFGEM.tipoFGEM() == TipoOperacionFGEM.VENTA ? oFGEM.margenBrutoFGEM() : null);
        return new VBox(14, datosFGEM, lineasFGEM, totalesFGEM);
    }

    /** «Al contado · Caja», «Debe S/ 118.00 (vencida)», «Al crédito · pagada»… */
    private String textoPagoFGEM(OperacionFGEM oFGEM) {
        if (!oFGEM.alCreditoFGEM()) {
            return oFGEM.cuentaFGEM() == null ? "Al contado" : "Al contado · " + oFGEM.cuentaFGEM();
        }
        if (oFGEM.anuladaFGEM()) {
            return "Al crédito";
        }
        if (oFGEM.pendienteFGEM().signum() == 0) {
            return "Al crédito · pagada";
        }
        return (oFGEM.tipoFGEM() == TipoOperacionFGEM.VENTA ? "Por cobrar " : "Por pagar ") + MontosFGEM.formatearFGEM(oFGEM.pendienteFGEM(), monedaFGEM)
                + (oFGEM.vencidaEnFGEM(LocalDate.now()) ? " (vencida)" : "");
    }

    private static void agregarDatoFGEM(GridPane grillaFGEM, int columnaFGEM, int filaFGEM, String etiquetaFGEM, String valorFGEM) {
        Label claveFGEM = new Label(etiquetaFGEM);
        claveFGEM.getStyleClass().add("info-key");
        Label datoFGEM = new Label(valorFGEM);
        datoFGEM.getStyleClass().add("info-value");
        datoFGEM.setWrapText(true);
        grillaFGEM.add(claveFGEM, columnaFGEM, filaFGEM);
        grillaFGEM.add(datoFGEM, columnaFGEM + 1, filaFGEM);
        if (columnaFGEM == 0 && (etiquetaFGEM.equals("Observaciones") || etiquetaFGEM.equals("Anulada"))) {
            GridPane.setColumnSpan(datoFGEM, 3);
        }
    }

    /** Subtotal, impuesto, total y (en ventas) margen, alineados a la derecha. */
    private VBox bloqueTotalesFGEM(BigDecimal subtotalFGEM, BigDecimal tasaFGEM, BigDecimal impuestoFGEM, BigDecimal totalFGEM,
                               BigDecimal margenFGEM) {
        VBox cajaFGEM = new VBox(4);
        cajaFGEM.setAlignment(Pos.CENTER_RIGHT);
        cajaFGEM.getChildren().add(lineaTotalFGEM("Subtotal", MontosFGEM.formatearFGEM(subtotalFGEM, monedaFGEM), false));
        cajaFGEM.getChildren().add(lineaTotalFGEM("Impuesto (" + (tasaFGEM == null ? "—" : tasaFGEM.stripTrailingZeros().toPlainString())
                + " %)", MontosFGEM.formatearFGEM(impuestoFGEM, monedaFGEM), false));
        cajaFGEM.getChildren().add(lineaTotalFGEM("Total", MontosFGEM.formatearFGEM(totalFGEM, monedaFGEM), true));
        if (margenFGEM != null) {
            HBox filaFGEM = lineaTotalFGEM("Margen bruto", MontosFGEM.formatearFGEM(margenFGEM, monedaFGEM), false);
            if (margenFGEM.signum() < 0) {
                filaFGEM.getChildren().get(1).getStyleClass().add("margen-negativo");
            }
            cajaFGEM.getChildren().add(filaFGEM);
        }
        return cajaFGEM;
    }

    private static HBox lineaTotalFGEM(String etiquetaFGEM, String valorFGEM, boolean destacadoFGEM) {
        Label claveFGEM = new Label(etiquetaFGEM);
        claveFGEM.getStyleClass().add(destacadoFGEM ? "total-clave-destacado" : "info-key");
        Label datoFGEM = new Label(valorFGEM);
        datoFGEM.getStyleClass().add(destacadoFGEM ? "total-valor-destacado" : "info-value");
        datoFGEM.setMinWidth(120);
        datoFGEM.setAlignment(Pos.CENTER_RIGHT);
        HBox filaFGEM = new HBox(16, claveFGEM, datoFGEM);
        filaFGEM.setAlignment(Pos.CENTER_RIGHT);
        return filaFGEM;
    }

    // ------------------------------------------------------------------ utilidades

    private void seleccionarFGEM(long idFGEM) {
        tablaFGEM.getItems().stream().filter(oFGEM -> oFGEM.idFGEM() == idFGEM).findFirst().ifPresent(oFGEM -> {
            tablaFGEM.getSelectionModel().select(oFGEM);
            tablaFGEM.scrollTo(oFGEM);
        });
    }

    private OperacionFGEM seleccionadaFGEM() {
        return tablaFGEM.getSelectionModel().getSelectedItem();
    }

    private OperacionServiceFGEM servicioFGEM() {
        return contextoFGEM.operacionesFGEM();
    }

    private Window ventanaFGEM() {
        return tablaFGEM.getScene().getWindow();
    }

    private static boolean contieneFGEM(String valorFGEM, String buscadoFGEM) {
        return valorFGEM != null && valorFGEM.toLowerCase(Locale.ROOT).contains(buscadoFGEM);
    }

    private static String pluralFGEM(int cantidadFGEM, String singularFGEM, String pluralFGEM) {
        return cantidadFGEM + " " + (cantidadFGEM == 1 ? singularFGEM : pluralFGEM);
    }

    private static ReadOnlyStringWrapper textoFGEM(String valorFGEM) {
        return new ReadOnlyStringWrapper(valorFGEM);
    }

    private static <T> TableColumn<T, String> columnaFGEM(String tituloFGEM, Function<T, String> valorFGEM, double anchoFGEM,
                                                      boolean numeroFGEM) {
        TableColumn<T, String> columnaFGEM = new TableColumn<>(tituloFGEM);
        columnaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(valorFGEM.apply(cFGEM.getValue())));
        columnaFGEM.setPrefWidth(anchoFGEM);
        columnaFGEM.setSortable(false);
        if (numeroFGEM) {
            columnaFGEM.getStyleClass().add("col-numero");
        }
        return columnaFGEM;
    }

    // ------------------------------------------------------------------ formulario de registro

    /** Línea editable del formulario (aún no registrada). */
    private static final class LineaFormularioFGEM {
        private final ProductoFGEM productoFGEM;
        private BigDecimal cantidadFGEM;
        private BigDecimal precioFGEM;

        LineaFormularioFGEM(ProductoFGEM productoFGEM, BigDecimal cantidadFGEM, BigDecimal precioFGEM) {
            this.productoFGEM = productoFGEM;
            this.cantidadFGEM = cantidadFGEM;
            this.precioFGEM = precioFGEM;
        }
    }

    /** Formulario de venta o compra: cabecera, buscador de productos, líneas y totales en vivo. */
    private final class FormularioOperacionFGEM {
        private final TipoOperacionFGEM tipoFGEM;
        private final List<ProductoFGEM> catalogoFGEM;
        private final DatePicker dpFechaFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        private final TextField txtTasaFGEM = new TextField();
        private final TextField txtContraparteFGEM = new TextField();
        private final TextField txtDocumentoFGEM = new TextField();
        private final TextField txtComprobanteFGEM = new TextField();
        private final TextField txtObservacionesFGEM = new TextField();
        private final ComboBox<CondicionPagoFGEM> cmbCondicionFGEM =
                new ComboBox<>(FXCollections.observableArrayList(CondicionPagoFGEM.values()));
        private final SelectorCuentaFGEM cmbCuentaFGEM = new SelectorCuentaFGEM(contextoFGEM.tesoreriaFGEM().cuentasActivasFGEM(), null, null);
        private final DatePicker dpVencimientoFGEM = CamposFechaFGEM.nuevoFGEM(null);
        private final CheckBox chkActualizarCostosFGEM = new CheckBox("Usar estos precios como nuevo costo del catálogo");
        private final TextField txtFiltroFGEM = new TextField();
        private final ComboBox<ProductoFGEM> cmbProductoFGEM = new ComboBox<>();
        private final TextField txtCantidadFGEM = new TextField("1");
        private final TextField txtPrecioFGEM = new TextField();
        private final Label lblErrorLineaFGEM = new Label();
        private final ObservableList<LineaFormularioFGEM> lineasFGEM = FXCollections.observableArrayList();
        private final TableView<LineaFormularioFGEM> tablaLineasFGEM = new TableView<>(lineasFGEM);
        private final VBox totalesFGEM = new VBox();
        private VBox raizFGEM;

        FormularioOperacionFGEM(TipoOperacionFGEM tipoFGEM, List<ProductoFGEM> catalogoFGEM, BigDecimal tasaSugeridaFGEM) {
            this.tipoFGEM = tipoFGEM;
            this.catalogoFGEM = catalogoFGEM;
            txtTasaFGEM.setText(tasaSugeridaFGEM.stripTrailingZeros().toPlainString());
            txtContraparteFGEM.setPromptText(tipoFGEM == TipoOperacionFGEM.VENTA ? "Opcional" : "Nombre del proveedor");
            txtDocumentoFGEM.setPromptText("RUC o DNI (opcional)");
            txtComprobanteFGEM.setPromptText("Ej.: F001-000123");
            txtObservacionesFGEM.setPromptText("Opcional");
            dpVencimientoFGEM.setPromptText("Fecha límite (opcional)");
            // Al contado el dinero entra o sale de una cuenta; al crédito queda pendiente hasta su vencimiento.
            cmbCondicionFGEM.valueProperty().addListener((oFGEM, aFGEM, condicionFGEM) -> {
                boolean creditoFGEM = condicionFGEM == CondicionPagoFGEM.CREDITO;
                cmbCuentaFGEM.setDisable(creditoFGEM);
                dpVencimientoFGEM.setDisable(!creditoFGEM);
                if (creditoFGEM && dpVencimientoFGEM.getValue() == null && dpFechaFGEM.getValue() != null) {
                    dpVencimientoFGEM.setValue(dpFechaFGEM.getValue().plusDays(30));
                }
                if (!creditoFGEM) {
                    dpVencimientoFGEM.setValue(null);
                }
            });
            cmbCondicionFGEM.setValue(CondicionPagoFGEM.CONTADO);
            chkActualizarCostosFGEM.setSelected(true);
            txtFiltroFGEM.setPromptText("Filtrar productos…");
            txtCantidadFGEM.setPrefWidth(80);
            txtPrecioFGEM.setPrefWidth(100);
            txtPrecioFGEM.setPromptText("Precio");
            lblErrorLineaFGEM.getStyleClass().add("margen-negativo");
            lblErrorLineaFGEM.setManaged(false);

            cmbProductoFGEM.setPrefWidth(330);
            cmbProductoFGEM.setPromptText(catalogoFGEM.isEmpty()
                    ? "No hay productos activos: regístralos en Productos y servicios"
                    : "Elige un producto o servicio");
            cmbProductoFGEM.setCellFactory(listaFGEM -> new CeldaProductoFGEM());
            cmbProductoFGEM.setButtonCell(new CeldaProductoFGEM());
            cmbProductoFGEM.getItems().setAll(catalogoFGEM);
            cmbProductoFGEM.valueProperty().addListener((oFGEM, aFGEM, productoFGEM) -> {
                if (productoFGEM != null) {
                    txtPrecioFGEM.setText(MontosFGEM.paraEditarFGEM(tipoFGEM == TipoOperacionFGEM.VENTA
                            ? productoFGEM.precioVentaFGEM() : productoFGEM.costoUnitarioFGEM()));
                }
            });
            txtFiltroFGEM.textProperty().addListener((oFGEM, aFGEM, textoFGEM) -> filtrarCatalogoFGEM(textoFGEM));

            configurarTablaLineasFGEM();
            txtTasaFGEM.textProperty().addListener((oFGEM, aFGEM, nFGEM) -> recalcularFGEM());
            recalcularFGEM();
        }

        Window ventanaFGEM() {
            return raizFGEM.getScene().getWindow();
        }

        /** Rellena el formulario con una operación ya registrada (para corregirla). */
        void cargarDesdeFGEM(OperacionFGEM oFGEM) {
            dpFechaFGEM.setValue(oFGEM.fechaFGEM());
            txtContraparteFGEM.setText(oFGEM.contraparteFGEM() == null ? "" : oFGEM.contraparteFGEM());
            txtDocumentoFGEM.setText(oFGEM.documentoContraparteFGEM() == null ? "" : oFGEM.documentoContraparteFGEM());
            txtComprobanteFGEM.setText(oFGEM.comprobanteFGEM() == null ? "" : oFGEM.comprobanteFGEM());
            txtObservacionesFGEM.setText(oFGEM.observacionesFGEM() == null ? "" : oFGEM.observacionesFGEM());
            cmbCondicionFGEM.setValue(oFGEM.condicionFGEM());
            if (oFGEM.cuentaIdFGEM() != null) {
                cmbCuentaFGEM.getItems().stream().filter(xFGEM -> oFGEM.cuentaIdFGEM().equals(xFGEM.idFGEM())).findFirst()
                        .ifPresent(cmbCuentaFGEM::setValue);
            }
            dpVencimientoFGEM.setValue(oFGEM.vencimientoFGEM());
            chkActualizarCostosFGEM.setSelected(false);
            for (LineaOperacionFGEM lineaFGEM : oFGEM.lineasFGEM()) {
                ProductoFGEM productoFGEM = catalogoFGEM.stream().filter(pFGEM -> pFGEM.idFGEM() == lineaFGEM.productoIdFGEM()).findFirst()
                        .orElseThrow(() -> new ValidacionExceptionFGEM("\"" + lineaFGEM.nombreFGEM() + "\" está archivado. "
                                + "Reactívalo en Productos y servicios para poder editar " + oFGEM.numeroFGEM() + "."));
                lineasFGEM.add(new LineaFormularioFGEM(productoFGEM, lineaFGEM.cantidadFGEM(), lineaFGEM.precioUnitarioFGEM()));
            }
            recalcularFGEM();
        }

        /** Rellena una compra nueva con la propuesta; devuelve cuántos productos no están en el catálogo activo. */
        int cargarPropuestaFGEM(AppContextFGEM.CompraPropuestaFGEM propuestaFGEM) {
            int faltantesFGEM = 0;
            for (DatosLineaFGEM lineaFGEM : propuestaFGEM.lineasFGEM()) {
                ProductoFGEM productoFGEM = catalogoFGEM.stream().filter(pFGEM -> pFGEM.idFGEM() == lineaFGEM.productoIdFGEM()).findFirst()
                        .orElse(null);
                if (productoFGEM == null) {
                    faltantesFGEM++;
                    continue;
                }
                lineasFGEM.add(new LineaFormularioFGEM(productoFGEM, lineaFGEM.cantidadFGEM(), lineaFGEM.precioUnitarioFGEM()));
            }
            if (propuestaFGEM.observacionesFGEM() != null) {
                txtObservacionesFGEM.setText(propuestaFGEM.observacionesFGEM());
            }
            recalcularFGEM();
            return faltantesFGEM;
        }

        VBox contenidoFGEM() {
            GridPane cabeceraFGEM = new GridPane();
            cabeceraFGEM.setHgap(12);
            cabeceraFGEM.setVgap(10);
            ColumnConstraints etiquetaFGEM = new ColumnConstraints(110);
            ColumnConstraints campoFGEM = new ColumnConstraints();
            campoFGEM.setHgrow(Priority.ALWAYS);
            cabeceraFGEM.getColumnConstraints().addAll(etiquetaFGEM, campoFGEM, new ColumnConstraints(110), campoFGEM);
            campoCabeceraFGEM(cabeceraFGEM, 0, 0, "Fecha *", dpFechaFGEM);
            campoCabeceraFGEM(cabeceraFGEM, 2, 0, "Impuesto (%)", txtTasaFGEM);
            campoCabeceraFGEM(cabeceraFGEM, 0, 1, tipoFGEM.contraparteFGEM(), txtContraparteFGEM);
            campoCabeceraFGEM(cabeceraFGEM, 2, 1, "Documento", txtDocumentoFGEM);
            campoCabeceraFGEM(cabeceraFGEM, 0, 2, "Comprobante", txtComprobanteFGEM);
            campoCabeceraFGEM(cabeceraFGEM, 2, 2, "Observaciones", txtObservacionesFGEM);
            campoCabeceraFGEM(cabeceraFGEM, 0, 3, "Pago *", cmbCondicionFGEM);
            campoCabeceraFGEM(cabeceraFGEM, 2, 3, tipoFGEM == TipoOperacionFGEM.VENTA ? "Entra a" : "Sale de", cmbCuentaFGEM);
            campoCabeceraFGEM(cabeceraFGEM, 0, 4, "Vence", dpVencimientoFGEM);
            Label ayudaPagoFGEM = new Label(tipoFGEM == TipoOperacionFGEM.VENTA
                    ? "Al crédito, la venta queda por cobrar en «Tesorería» hasta que registres los cobros."
                    : "Al crédito, la compra queda por pagar en «Tesorería» hasta que registres los pagos.");
            ayudaPagoFGEM.getStyleClass().add("hint");
            ayudaPagoFGEM.setWrapText(true);
            cabeceraFGEM.add(ayudaPagoFGEM, 2, 4, 2, 1);

            Button agregarFGEM = new Button("Agregar", new org.kordamp.ikonli.javafx.FontIcon("fth-plus"));
            agregarFGEM.getStyleClass().add("button-secondary");
            agregarFGEM.setMinWidth(Region.USE_PREF_SIZE);
            agregarFGEM.setOnAction(eFGEM -> agregarLineaFGEM());
            txtPrecioFGEM.setOnAction(eFGEM -> agregarLineaFGEM());
            txtCantidadFGEM.setOnAction(eFGEM -> agregarLineaFGEM());
            HBox.setHgrow(cmbProductoFGEM, Priority.ALWAYS);
            txtFiltroFGEM.setPrefWidth(150);
            HBox selectorFGEM = new HBox(8, txtFiltroFGEM, cmbProductoFGEM, etiquetaCortaFGEM("Cant."), txtCantidadFGEM,
                    etiquetaCortaFGEM("Precio"), txtPrecioFGEM, agregarFGEM);
            selectorFGEM.setAlignment(Pos.CENTER_LEFT);

            Button quitarFGEM = new Button("Quitar línea", new org.kordamp.ikonli.javafx.FontIcon("fth-trash-2"));
            quitarFGEM.getStyleClass().add("button-secondary");
            quitarFGEM.disableProperty().bind(tablaLineasFGEM.getSelectionModel().selectedItemProperty().isNull());
            quitarFGEM.setOnAction(eFGEM -> {
                lineasFGEM.remove(tablaLineasFGEM.getSelectionModel().getSelectedItem());
                recalcularFGEM();
            });
            Region espacioFGEM = new Region();
            HBox.setHgrow(espacioFGEM, Priority.ALWAYS);
            HBox pieFGEM = new HBox(12, quitarFGEM, espacioFGEM, totalesFGEM);
            pieFGEM.setAlignment(Pos.TOP_LEFT);

            raizFGEM = new VBox(12, cabeceraFGEM, separadorFGEM("Detalle"), selectorFGEM, lblErrorLineaFGEM, tablaLineasFGEM, pieFGEM);
            if (tipoFGEM == TipoOperacionFGEM.COMPRA) {
                raizFGEM.getChildren().add(chkActualizarCostosFGEM);
            }
            return raizFGEM;
        }

        private void configurarTablaLineasFGEM() {
            tablaLineasFGEM.getColumns().add(columnaFGEM("Código", (LineaFormularioFGEM lFGEM) -> lFGEM.productoFGEM.codigoFGEM(), 80, false));
            tablaLineasFGEM.getColumns().add(columnaFGEM("Producto o servicio", (LineaFormularioFGEM lFGEM) -> lFGEM.productoFGEM.nombreFGEM(), 250, false));
            tablaLineasFGEM.getColumns().add(columnaFGEM("Cantidad", (LineaFormularioFGEM lFGEM) ->
                    MontosFGEM.cantidadFGEM(lFGEM.cantidadFGEM) + " " + lFGEM.productoFGEM.unidadFGEM().abreviaturaFGEM(), 90, true));
            if (tipoFGEM == TipoOperacionFGEM.VENTA) {
                tablaLineasFGEM.getColumns().add(columnaFGEM("Stock", (LineaFormularioFGEM lFGEM) ->
                        lFGEM.productoFGEM.stockFGEM() == null ? "—" : MontosFGEM.cantidadFGEM(lFGEM.productoFGEM.stockFGEM()), 70, true));
            }
            tablaLineasFGEM.getColumns().add(columnaFGEM("Precio unit.", (LineaFormularioFGEM lFGEM) -> MontosFGEM.formatearFGEM(lFGEM.precioFGEM, monedaFGEM), 100, true));
            tablaLineasFGEM.getColumns().add(columnaFGEM("Subtotal", (LineaFormularioFGEM lFGEM) ->
                    MontosFGEM.formatearFGEM(CalculadoraOperacionFGEM.subtotalLineaFGEM(lFGEM.cantidadFGEM, lFGEM.precioFGEM), monedaFGEM), 100, true));
            tablaLineasFGEM.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            tablaLineasFGEM.setPrefHeight(220);
            tablaLineasFGEM.setPlaceholder(new Label("Agrega productos o servicios con el selector de arriba."));
        }

        private void filtrarCatalogoFGEM(String textoFGEM) {
            String buscadoFGEM = textoFGEM == null ? "" : textoFGEM.strip().toLowerCase(Locale.ROOT);
            List<ProductoFGEM> coincidentesFGEM = catalogoFGEM.stream()
                    .filter(pFGEM -> buscadoFGEM.isEmpty()
                            || pFGEM.codigoFGEM().toLowerCase(Locale.ROOT).contains(buscadoFGEM)
                            || pFGEM.nombreFGEM().toLowerCase(Locale.ROOT).contains(buscadoFGEM))
                    .toList();
            cmbProductoFGEM.getItems().setAll(coincidentesFGEM);
            if (coincidentesFGEM.size() == 1) {
                cmbProductoFGEM.setValue(coincidentesFGEM.get(0));
            }
        }

        private void agregarLineaFGEM() {
            mostrarErrorLineaFGEM(null);
            ProductoFGEM productoFGEM = cmbProductoFGEM.getValue();
            if (productoFGEM == null) {
                mostrarErrorLineaFGEM("Elige un producto o servicio.");
                return;
            }
            BigDecimal cantidadFGEM;
            BigDecimal precioFGEM;
            try {
                cantidadFGEM = MontosFGEM.leerFGEM(txtCantidadFGEM.getText(), "Cantidad");
                precioFGEM = MontosFGEM.leerFGEM(txtPrecioFGEM.getText(), "Precio");
            } catch (ValidacionExceptionFGEM eFGEM) {
                mostrarErrorLineaFGEM(eFGEM.getMessage());
                return;
            }
            if (cantidadFGEM.signum() <= 0 || precioFGEM.signum() < 0) {
                mostrarErrorLineaFGEM("La cantidad debe ser mayor que cero y el precio no puede ser negativo.");
                return;
            }
            LineaFormularioFGEM existenteFGEM = lineasFGEM.stream().filter(lFGEM -> lFGEM.productoFGEM.idFGEM() == productoFGEM.idFGEM()).findFirst().orElse(null);
            if (existenteFGEM != null) {
                existenteFGEM.cantidadFGEM = existenteFGEM.cantidadFGEM.add(cantidadFGEM);
                existenteFGEM.precioFGEM = precioFGEM;
                tablaLineasFGEM.refresh();
            } else {
                lineasFGEM.add(new LineaFormularioFGEM(productoFGEM, cantidadFGEM, precioFGEM));
            }
            txtCantidadFGEM.setText("1");
            cmbProductoFGEM.setValue(null);
            txtPrecioFGEM.clear();
            txtFiltroFGEM.clear();
            txtFiltroFGEM.requestFocus();
            recalcularFGEM();
        }

        private void recalcularFGEM() {
            BigDecimal tasaFGEM;
            try {
                tasaFGEM = MontosFGEM.leerFGEM(txtTasaFGEM.getText(), "Impuesto");
            } catch (ValidacionExceptionFGEM eFGEM) {
                tasaFGEM = BigDecimal.ZERO;
            }
            List<CalculadoraOperacionFGEM.LineaFGEM> calculoFGEM = lineasFGEM.stream()
                    .map(lFGEM -> new CalculadoraOperacionFGEM.LineaFGEM(lFGEM.cantidadFGEM, lFGEM.precioFGEM,
                            tipoFGEM == TipoOperacionFGEM.VENTA ? lFGEM.productoFGEM.costoUnitarioFGEM() : lFGEM.precioFGEM))
                    .toList();
            CalculadoraOperacionFGEM.TotalesFGEM tFGEM = CalculadoraOperacionFGEM.calcularFGEM(calculoFGEM, tasaFGEM);
            VBox nuevoFGEM = bloqueTotalesFGEM(tFGEM.subtotalFGEM(), tasaFGEM, tFGEM.impuestoFGEM(), tFGEM.totalFGEM(),
                    tipoFGEM == TipoOperacionFGEM.VENTA ? tFGEM.margenBrutoFGEM() : null);
            totalesFGEM.getChildren().setAll(nuevoFGEM.getChildren());
            totalesFGEM.setSpacing(nuevoFGEM.getSpacing());
        }

        DatosOperacionFGEM datosFGEM() {
            LocalDate fechaFGEM = CamposFechaFGEM.leerFGEM(dpFechaFGEM, "la operación");
            BigDecimal tasaFGEM = txtTasaFGEM.getText() == null || txtTasaFGEM.getText().isBlank()
                    ? BigDecimal.ZERO : MontosFGEM.leerFGEM(txtTasaFGEM.getText(), "Impuesto (%)");
            List<DatosLineaFGEM> datosLineasFGEM = new ArrayList<>();
            for (LineaFormularioFGEM lFGEM : lineasFGEM) {
                datosLineasFGEM.add(new DatosLineaFGEM(lFGEM.productoFGEM.idFGEM(), lFGEM.cantidadFGEM, lFGEM.precioFGEM));
            }
            CondicionPagoFGEM condicionFGEM = cmbCondicionFGEM.getValue();
            return new DatosOperacionFGEM(tipoFGEM, fechaFGEM, txtContraparteFGEM.getText(), txtDocumentoFGEM.getText(),
                    txtComprobanteFGEM.getText(), tasaFGEM, txtObservacionesFGEM.getText(), datosLineasFGEM,
                    tipoFGEM == TipoOperacionFGEM.COMPRA && chkActualizarCostosFGEM.isSelected(), condicionFGEM,
                    condicionFGEM == CondicionPagoFGEM.CONTADO ? cmbCuentaFGEM.cuentaIdFGEM() : null,
                    CamposFechaFGEM.leerFGEM(dpVencimientoFGEM, "vencimiento"));
        }

        private void mostrarErrorLineaFGEM(String mensajeFGEM) {
            lblErrorLineaFGEM.setText(mensajeFGEM == null ? "" : mensajeFGEM);
            lblErrorLineaFGEM.setManaged(mensajeFGEM != null);
            lblErrorLineaFGEM.setVisible(mensajeFGEM != null);
        }

        private void campoCabeceraFGEM(GridPane grillaFGEM, int columnaFGEM, int filaFGEM, String etiquetaFGEM, Region campoFGEM) {
            Label labelFGEM = new Label(etiquetaFGEM);
            labelFGEM.getStyleClass().add("form-label");
            campoFGEM.setMaxWidth(Double.MAX_VALUE);
            grillaFGEM.add(labelFGEM, columnaFGEM, filaFGEM);
            grillaFGEM.add(campoFGEM, columnaFGEM + 1, filaFGEM);
        }

        private Label etiquetaCortaFGEM(String textoFGEM) {
            Label labelFGEM = new Label(textoFGEM);
            labelFGEM.getStyleClass().add("form-label");
            labelFGEM.setMinWidth(Region.USE_PREF_SIZE);
            return labelFGEM;
        }

        private Node separadorFGEM(String tituloFGEM) {
            Label labelFGEM = new Label(tituloFGEM);
            labelFGEM.getStyleClass().add("card-title");
            return labelFGEM;
        }
    }

    /** Muestra código, nombre, precio de referencia y (en productos) el stock disponible. */
    private final class CeldaProductoFGEM extends ListCell<ProductoFGEM> {
        @Override
        protected void updateItem(ProductoFGEM pFGEM, boolean vacioFGEM) {
            super.updateItem(pFGEM, vacioFGEM);
            if (vacioFGEM || pFGEM == null) {
                setText(null);
                return;
            }
            String stockFGEM = pFGEM.tipoFGEM() == TipoItemFGEM.PRODUCTO ? " · stock " + MontosFGEM.cantidadFGEM(pFGEM.stockFGEM()) : "";
            setText(pFGEM.codigoFGEM() + " — " + pFGEM.nombreFGEM() + stockFGEM);
        }
    }
}
