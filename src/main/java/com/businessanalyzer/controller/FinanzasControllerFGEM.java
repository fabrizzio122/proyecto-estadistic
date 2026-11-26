package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.MargenesFGEM;
import com.businessanalyzer.analytics.ResumenFinancieroFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.ComportamientoFGEM;
import com.businessanalyzer.model.DatosMovimientoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.MovimientoFinancieroFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.TipoMovimientoFGEM;
import com.businessanalyzer.service.FinanzasServiceFGEM;
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
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class FinanzasControllerFGEM implements VistaModuloFGEM {

    private static final String TODOS_FGEM = "Todos";

    private final AppContextFGEM contextoFGEM;
    private final ObservableList<MovimientoFinancieroFGEM> movimientosFGEM = FXCollections.observableArrayList();
    private final FilteredList<MovimientoFinancieroFGEM> filtradosFGEM = new FilteredList<>(movimientosFGEM);
    private MonedaFGEM monedaFGEM = MonedaFGEM.PEN;

    private final TarjetaIndicadorFGEM kpiGastosFGEM = new TarjetaIndicadorFGEM("Gastos del período", "fth-credit-card");
    private final TarjetaIndicadorFGEM kpiFijosFGEM = new TarjetaIndicadorFGEM("Costos fijos", "fth-lock");
    private final TarjetaIndicadorFGEM kpiVariablesFGEM = new TarjetaIndicadorFGEM("Gastos variables", "fth-activity");
    private final TarjetaIndicadorFGEM kpiIngresosFGEM = new TarjetaIndicadorFGEM("Otros ingresos", "fth-plus-circle");

    @FXML private HBox indicadoresFGEM;
    @FXML private DatePicker dpDesdeFGEM;
    @FXML private DatePicker dpHastaFGEM;
    @FXML private ComboBox<String> cmbTipoFGEM;
    @FXML private TextField txtBuscarFGEM;
    @FXML private CheckBox chkAnuladosFGEM;
    @FXML private Button btnCopiarFGEM;
    @FXML private Button btnEditarFGEM;
    @FXML private Button btnAnularFGEM;
    @FXML private Button btnNuevoIngresoFGEM;
    @FXML private Button btnNuevoGastoFGEM;
    @FXML private TableView<MovimientoFinancieroFGEM> tablaFGEM;
    @FXML private TableColumn<MovimientoFinancieroFGEM, LocalDate> colFechaFGEM;
    @FXML private TableColumn<MovimientoFinancieroFGEM, String> colCategoriaFGEM;
    @FXML private TableColumn<MovimientoFinancieroFGEM, String> colDescripcionFGEM;
    @FXML private TableColumn<MovimientoFinancieroFGEM, String> colComportamientoFGEM;
    @FXML private TableColumn<MovimientoFinancieroFGEM, String> colCuentaFGEM;
    @FXML private TableColumn<MovimientoFinancieroFGEM, MovimientoFinancieroFGEM> colMontoFGEM;
    @FXML private VBox desgloseFGEM;

    public FinanzasControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        indicadoresFGEM.getChildren().addAll(kpiGastosFGEM, kpiFijosFGEM, kpiVariablesFGEM, kpiIngresosFGEM);

        CamposFechaFGEM.configurarFGEM(dpDesdeFGEM);
        CamposFechaFGEM.configurarFGEM(dpHastaFGEM);
        LocalDate hoyFGEM = LocalDate.now();
        dpDesdeFGEM.setValue(hoyFGEM.withDayOfMonth(1));
        dpHastaFGEM.setValue(YearMonth.from(hoyFGEM).atEndOfMonth());
        dpDesdeFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarFGEM());
        dpHastaFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarFGEM());

        cmbTipoFGEM.getItems().setAll(TODOS_FGEM, TipoMovimientoFGEM.GASTO.pluralFGEM(), TipoMovimientoFGEM.INGRESO.pluralFGEM());
        cmbTipoFGEM.setValue(TODOS_FGEM);
        cmbTipoFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> filtrarFGEM());
        txtBuscarFGEM.textProperty().addListener((oFGEM, aFGEM, nFGEM) -> filtrarFGEM());

        configurarTablaFGEM();
        boolean gestionaFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        for (Node nodoFGEM : List.of(btnCopiarFGEM, btnEditarFGEM, btnAnularFGEM, btnNuevoIngresoFGEM, btnNuevoGastoFGEM)) {
            nodoFGEM.setVisible(gestionaFGEM);
            nodoFGEM.setManaged(gestionaFGEM);
        }
    }

    @Override
    public void alMostrarFGEM() {
        cargarFGEM();
    }

    @FXML
    private void esteMesFGEM() {
        LocalDate hoyFGEM = LocalDate.now();
        dpHastaFGEM.setValue(YearMonth.from(hoyFGEM).atEndOfMonth());
        dpDesdeFGEM.setValue(hoyFGEM.withDayOfMonth(1));
    }

    @FXML
    private void cargarFGEM() {
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        for (Button botonFGEM : List.of(btnCopiarFGEM, btnNuevoIngresoFGEM, btnNuevoGastoFGEM)) {
            botonFGEM.setDisable(empresaFGEM == null);
        }
        if (empresaFGEM == null) {
            movimientosFGEM.clear();
            tablaFGEM.setPlaceholder(new Label("Selecciona una empresa en la barra superior para ver sus finanzas."));
            mostrarResumenFGEM(ResumenFinancieroFGEM.deFGEM(List.of()));
            return;
        }
        monedaFGEM = empresaFGEM.monedaFGEM();
        tablaFGEM.setPlaceholder(new Label("No hay ingresos ni gastos en el período elegido."));
        if (dpDesdeFGEM.getValue() == null || dpHastaFGEM.getValue() == null) {
            return;
        }
        try {
            movimientosFGEM.setAll(servicioFGEM().listarFGEM(dpDesdeFGEM.getValue(), dpHastaFGEM.getValue(), chkAnuladosFGEM.isSelected()));
        } catch (RuntimeException eFGEM) {
            movimientosFGEM.clear();
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
        mostrarResumenFGEM(ResumenFinancieroFGEM.deFGEM(movimientosFGEM));
        filtrarFGEM();
        actualizarAccionesFGEM();
    }

    private void mostrarResumenFGEM(ResumenFinancieroFGEM rFGEM) {
        kpiGastosFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(rFGEM.gastosFGEM(), monedaFGEM),
                rFGEM.cantidadMovimientosFGEM() + (rFGEM.cantidadMovimientosFGEM() == 1 ? " movimiento" : " movimientos"));
        kpiFijosFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(rFGEM.gastosFijosFGEM(), monedaFGEM),
                MontosFGEM.porcentajeFGEM(rFGEM.participacionFijosFGEM()) + " del total de gastos");
        kpiVariablesFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(rFGEM.gastosVariablesFGEM(), monedaFGEM), "Sin costo de mercadería");
        kpiIngresosFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(rFGEM.ingresosFGEM(), monedaFGEM), "No provienen de ventas");
        construirDesgloseFGEM(rFGEM);
    }

    private void construirDesgloseFGEM(ResumenFinancieroFGEM rFGEM) {
        desgloseFGEM.getChildren().clear();
        List<Map.Entry<CategoriaFinancieraFGEM, BigDecimal>> gastosFGEM = rFGEM.porCategoriaFGEM().entrySet().stream()
                .filter(eFGEM -> eFGEM.getKey().tipoFGEM() == TipoMovimientoFGEM.GASTO)
                .sorted(Map.Entry.<CategoriaFinancieraFGEM, BigDecimal>comparingByValue().reversed())
                .toList();
        if (gastosFGEM.isEmpty()) {
            Label vacioFGEM = new Label("Sin gastos en el período.");
            vacioFGEM.getStyleClass().add("muted");
            desgloseFGEM.getChildren().add(vacioFGEM);
            return;
        }
        for (Map.Entry<CategoriaFinancieraFGEM, BigDecimal> eFGEM : gastosFGEM) {
            BigDecimal participacionFGEM = eFGEM.getValue().divide(rFGEM.gastosFGEM(), MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
            Label nombreFGEM = new Label(eFGEM.getKey().descripcionFGEM());
            nombreFGEM.getStyleClass().add("info-value");
            nombreFGEM.setWrapText(true);
            Label montoFGEM = new Label(MontosFGEM.formatearFGEM(eFGEM.getValue(), monedaFGEM));
            montoFGEM.getStyleClass().add("info-value");
            montoFGEM.setMinWidth(Region.USE_PREF_SIZE);
            Region espacioFGEM = new Region();
            HBox.setHgrow(espacioFGEM, Priority.ALWAYS);
            HBox cabeceraFGEM = new HBox(8, nombreFGEM, espacioFGEM, montoFGEM);

            ProgressBar barraFGEM = new ProgressBar(participacionFGEM.doubleValue());
            barraFGEM.setMaxWidth(Double.MAX_VALUE);
            barraFGEM.getStyleClass().add("barra-categoria");
            Label porcentajeFGEM = new Label(MontosFGEM.porcentajeFGEM(participacionFGEM) + " de los gastos · "
                    + eFGEM.getKey().grupoFGEM().descripcionFGEM().toLowerCase());
            porcentajeFGEM.getStyleClass().add("hint");
            desgloseFGEM.getChildren().add(new VBox(4, cabeceraFGEM, barraFGEM, porcentajeFGEM));
        }
    }

    private void filtrarFGEM() {
        String textoFGEM = txtBuscarFGEM.getText() == null ? "" : txtBuscarFGEM.getText().strip().toLowerCase(Locale.ROOT);
        String tipoFGEM = cmbTipoFGEM.getValue();
        filtradosFGEM.setPredicate(mFGEM -> {
            boolean coincideTipoFGEM = tipoFGEM == null || TODOS_FGEM.equals(tipoFGEM) || mFGEM.tipoFGEM().pluralFGEM().equals(tipoFGEM);
            boolean coincideTextoFGEM = textoFGEM.isEmpty()
                    || mFGEM.descripcionFGEM().toLowerCase(Locale.ROOT).contains(textoFGEM)
                    || mFGEM.categoriaFGEM().descripcionFGEM().toLowerCase(Locale.ROOT).contains(textoFGEM)
                    || (mFGEM.comprobanteFGEM() != null && mFGEM.comprobanteFGEM().toLowerCase(Locale.ROOT).contains(textoFGEM));
            return coincideTipoFGEM && coincideTextoFGEM;
        });
    }

    @FXML
    private void nuevoGastoFGEM() {
        nuevoFGEM(TipoMovimientoFGEM.GASTO);
    }

    @FXML
    private void nuevoIngresoFGEM() {
        nuevoFGEM(TipoMovimientoFGEM.INGRESO);
    }

    private void nuevoFGEM(TipoMovimientoFGEM tipoFGEM) {
        FormularioMovimientoFGEM formularioFGEM = new FormularioMovimientoFGEM(tipoFGEM, null);
        MovimientoFinancieroFGEM[] creadoFGEM = new MovimientoFinancieroFGEM[1];
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Nuevo " + tipoFGEM.descripcionFGEM().toLowerCase(),
                "Monto sin impuestos.", formularioFGEM.contenidoFGEM(), "Registrar",
                () -> creadoFGEM[0] = servicioFGEM().registrarFGEM(formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            seleccionarFGEM(creadoFGEM[0].idFGEM());
            NotificadorFGEM.exitoFGEM(tipoFGEM.descripcionFGEM() + " registrado: " + MontosFGEM.formatearFGEM(creadoFGEM[0].montoFGEM(), monedaFGEM) + ".");
        }
    }

    @FXML
    private void editarFGEM() {
        MovimientoFinancieroFGEM mFGEM = seleccionadoFGEM();
        if (mFGEM == null || mFGEM.anuladoFGEM() || !contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.FINANZAS_GESTIONAR)) {
            return;
        }
        if (mFGEM.dePagoDeudaFGEM()) {
            NotificadorFGEM.infoFGEM("Este gasto lo generó el pago de una deuda: corrígelo desde el pago, en «Activos y deudas».");
            return;
        }
        FormularioMovimientoFGEM formularioFGEM = new FormularioMovimientoFGEM(mFGEM.tipoFGEM(), mFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Editar " + mFGEM.tipoFGEM().descripcionFGEM().toLowerCase(), null,
                formularioFGEM.contenidoFGEM(), "Guardar cambios", () -> servicioFGEM().actualizarFGEM(mFGEM.idFGEM(), formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Movimiento actualizado.");
        }
    }

    @FXML
    private void anularFGEM() {
        MovimientoFinancieroFGEM mFGEM = seleccionadoFGEM();
        if (mFGEM == null || mFGEM.anuladoFGEM()) {
            return;
        }
        TextField motivoFGEM = new TextField();
        motivoFGEM.setPromptText("Ej.: registrado dos veces");
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Motivo *", motivoFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Anular movimiento",
                "\"" + mFGEM.descripcionFGEM() + "\" (" + MontosFGEM.formatearFGEM(mFGEM.montoFGEM(), monedaFGEM) + ") dejará de contar en los "
                        + "totales y análisis, pero se conservará como anulado.",
                grillaFGEM, "Anular", () -> servicioFGEM().anularFGEM(mFGEM.idFGEM(), motivoFGEM.getText()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Movimiento anulado.");
        }
    }

    @FXML
    private void copiarFijosFGEM() {
        YearMonth destinoFGEM = YearMonth.now();
        YearMonth origenFGEM = destinoFGEM.minusMonths(1);
        String nombreOrigenFGEM = FechasFGEM.nombreMesFGEM(origenFGEM.getMonthValue()).toLowerCase() + " " + origenFGEM.getYear();
        String nombreDestinoFGEM = FechasFGEM.nombreMesFGEM(destinoFGEM.getMonthValue()).toLowerCase() + " " + destinoFGEM.getYear();
        if (!DialogosFGEM.confirmarFGEM(ventanaFGEM(), "Copiar gastos fijos",
                "Se copiarán a " + nombreDestinoFGEM + " los gastos fijos registrados en " + nombreOrigenFGEM
                        + " (alquiler, sueldos, servicios...). Los que ya existan con la misma categoría, "
                        + "descripción y monto no se duplicarán. Luego puedes editar los montos que cambien.")) {
            return;
        }
        try {
            int copiadosFGEM = servicioFGEM().copiarGastosFijosFGEM(origenFGEM, destinoFGEM);
            esteMesFGEM();
            cargarFGEM();
            if (copiadosFGEM == 0) {
                NotificadorFGEM.infoFGEM("No había gastos fijos nuevos que copiar desde " + nombreOrigenFGEM + ".");
            } else {
                NotificadorFGEM.exitoFGEM(copiadosFGEM + (copiadosFGEM == 1 ? " gasto fijo copiado" : " gastos fijos copiados")
                        + " a " + nombreDestinoFGEM + ".");
            }
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    private void configurarTablaFGEM() {
        colFechaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().fechaFGEM()));
        colFechaFGEM.setCellFactory(colFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fechaFGEM, boolean vacioFGEM) {
                super.updateItem(fechaFGEM, vacioFGEM);
                setText(vacioFGEM || fechaFGEM == null ? null : FechasFGEM.paraMostrarFGEM(fechaFGEM));
            }
        });
        colCategoriaFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().categoriaFGEM().descripcionFGEM()));
        colDescripcionFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().descripcionFGEM()
                + (cFGEM.getValue().anuladoFGEM() ? "  (anulado)" : "")));
        colComportamientoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().comportamientoFGEM() == null
                ? "Ingreso" : cFGEM.getValue().comportamientoFGEM().descripcionFGEM()));
        colCuentaFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().cuentaFGEM() == null ? "—" : cFGEM.getValue().cuentaFGEM()));

        colMontoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colMontoFGEM.setComparator(Comparator.comparing(MovimientoFinancieroFGEM::montoFGEM));
        colMontoFGEM.setCellFactory(colFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(MovimientoFinancieroFGEM mFGEM, boolean vacioFGEM) {
                super.updateItem(mFGEM, vacioFGEM);
                getStyleClass().remove("margen-ok");
                if (vacioFGEM || mFGEM == null) {
                    setText(null);
                    return;
                }
                boolean ingresoFGEM = mFGEM.tipoFGEM() == TipoMovimientoFGEM.INGRESO;
                setText((ingresoFGEM ? "+" : "") + MontosFGEM.formatearFGEM(mFGEM.montoFGEM(), monedaFGEM));
                if (ingresoFGEM) {
                    getStyleClass().add("margen-ok");
                }
            }
        });

        SortedList<MovimientoFinancieroFGEM> ordenadosFGEM = new SortedList<>(filtradosFGEM);
        ordenadosFGEM.comparatorProperty().bind(tablaFGEM.comparatorProperty());
        tablaFGEM.setItems(ordenadosFGEM);
        tablaFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarAccionesFGEM());
        tablaFGEM.setRowFactory(tFGEM -> {
            TableRow<MovimientoFinancieroFGEM> filaFGEM = new TableRow<>() {
                @Override
                protected void updateItem(MovimientoFinancieroFGEM mFGEM, boolean vacioFGEM) {
                    super.updateItem(mFGEM, vacioFGEM);
                    boolean anuladoFGEM = !vacioFGEM && mFGEM != null && mFGEM.anuladoFGEM();
                    pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("archivado"), anuladoFGEM);
                    setTooltip(anuladoFGEM ? new Tooltip("Anulado por " + mFGEM.anuladoPorFGEM() + ": " + mFGEM.motivoAnulacionFGEM()) : null);
                }
            };
            filaFGEM.setOnMouseClicked(eFGEM -> {
                if (eFGEM.getButton() == MouseButton.PRIMARY && eFGEM.getClickCount() == 2 && !filaFGEM.isEmpty()) {
                    editarFGEM();
                }
            });
            return filaFGEM;
        });
        actualizarAccionesFGEM();
    }

    private void actualizarAccionesFGEM() {
        MovimientoFinancieroFGEM mFGEM = seleccionadoFGEM();
        boolean deshabilitarFGEM = mFGEM == null || mFGEM.anuladoFGEM() || mFGEM.dePagoDeudaFGEM();
        btnEditarFGEM.setDisable(deshabilitarFGEM);
        btnAnularFGEM.setDisable(deshabilitarFGEM);
    }

    private void seleccionarFGEM(long idFGEM) {
        tablaFGEM.getItems().stream().filter(mFGEM -> mFGEM.idFGEM() == idFGEM).findFirst().ifPresent(mFGEM -> {
            tablaFGEM.getSelectionModel().select(mFGEM);
            tablaFGEM.scrollTo(mFGEM);
        });
    }

    private MovimientoFinancieroFGEM seleccionadoFGEM() {
        return tablaFGEM.getSelectionModel().getSelectedItem();
    }

    private FinanzasServiceFGEM servicioFGEM() {
        return contextoFGEM.finanzasFGEM();
    }

    private Window ventanaFGEM() {
        return tablaFGEM.getScene().getWindow();
    }

    private static ReadOnlyStringWrapper textoFGEM(String valorFGEM) {
        return new ReadOnlyStringWrapper(valorFGEM);
    }

    private final class FormularioMovimientoFGEM {
        private final TipoMovimientoFGEM tipoFGEM;
        private final ComboBox<CategoriaFinancieraFGEM> cmbCategoriaFGEM;
        private final ComboBox<ComportamientoFGEM> cmbComportamientoFGEM =
                new ComboBox<>(FXCollections.observableArrayList(ComportamientoFGEM.values()));
        private final Label lblComportamientoFGEM = new Label();
        private final DatePicker dpFechaFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        private final TextField txtDescripcionFGEM = new TextField();
        private final TextField txtMontoFGEM = new TextField();
        private final TextField txtComprobanteFGEM = new TextField();
        private final SelectorCuentaFGEM cmbCuentaFGEM;

        FormularioMovimientoFGEM(TipoMovimientoFGEM tipoFGEM, MovimientoFinancieroFGEM mFGEM) {
            this.tipoFGEM = tipoFGEM;
            boolean antiguoFGEM = mFGEM != null && mFGEM.cuentaIdFGEM() == null;
            cmbCuentaFGEM = new SelectorCuentaFGEM(contextoFGEM.tesoreriaFGEM().cuentasActivasFGEM(),
                    antiguoFGEM ? "Sin cuenta (registro anterior a Tesorería)" : null, mFGEM == null ? null : mFGEM.cuentaIdFGEM());
            cmbCategoriaFGEM = new ComboBox<>(FXCollections.observableArrayList(CategoriaFinancieraFGEM.deFGEM(tipoFGEM)));
            cmbCategoriaFGEM.setPromptText("Selecciona la categoría");
            cmbCategoriaFGEM.setVisibleRowCount(12);
            txtDescripcionFGEM.setPromptText(tipoFGEM == TipoMovimientoFGEM.GASTO ? "Ej.: Alquiler de noviembre" : "Ej.: Intereses de la cuenta");
            txtMontoFGEM.setPromptText("0.00");
            txtComprobanteFGEM.setPromptText("Opcional");
            lblComportamientoFGEM.getStyleClass().add("hint");
            lblComportamientoFGEM.setWrapText(true);
            lblComportamientoFGEM.setMinHeight(Region.USE_PREF_SIZE);

            cmbCategoriaFGEM.valueProperty().addListener((oFGEM, anteriorFGEM, categoriaFGEM) -> {
                if (categoriaFGEM != null && categoriaFGEM.comportamientoSugeridoFGEM() != null) {
                    cmbComportamientoFGEM.setValue(categoriaFGEM.comportamientoSugeridoFGEM());
                }
            });
            cmbComportamientoFGEM.valueProperty().addListener((oFGEM, aFGEM, cFGEM) ->
                    lblComportamientoFGEM.setText(cFGEM == null ? "" : cFGEM.explicacionFGEM()));

            if (mFGEM != null) {
                cmbCategoriaFGEM.setValue(mFGEM.categoriaFGEM());
                cmbComportamientoFGEM.setValue(mFGEM.comportamientoFGEM());
                dpFechaFGEM.setValue(mFGEM.fechaFGEM());
                txtDescripcionFGEM.setText(mFGEM.descripcionFGEM());
                txtMontoFGEM.setText(MontosFGEM.paraEditarFGEM(mFGEM.montoFGEM()));
                txtComprobanteFGEM.setText(Objects.requireNonNullElse(mFGEM.comprobanteFGEM(), ""));
            }
        }

        GridPane contenidoFGEM() {
            GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
            FormulariosFGEM.filaFGEM(grillaFGEM, "Categoría *", cmbCategoriaFGEM);
            if (tipoFGEM == TipoMovimientoFGEM.GASTO) {
                FormulariosFGEM.filaFGEM(grillaFGEM, "Tipo de costo *", cmbComportamientoFGEM);
                FormulariosFGEM.filaFGEM(grillaFGEM, "", lblComportamientoFGEM);
            }
            FormulariosFGEM.filaFGEM(grillaFGEM, "Fecha *", dpFechaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Descripción *", txtDescripcionFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Monto (" + monedaFGEM.simboloFGEM() + ") *", txtMontoFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, tipoFGEM == TipoMovimientoFGEM.GASTO ? "Pagado desde *" : "Cobrado en *", cmbCuentaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Comprobante", txtComprobanteFGEM);
            return grillaFGEM;
        }

        DatosMovimientoFGEM datosFGEM() {
            LocalDate fechaFGEM = CamposFechaFGEM.leerFGEM(dpFechaFGEM, "el movimiento");
            return new DatosMovimientoFGEM(tipoFGEM, cmbCategoriaFGEM.getValue(),
                    tipoFGEM == TipoMovimientoFGEM.GASTO ? cmbComportamientoFGEM.getValue() : null,
                    fechaFGEM, txtDescripcionFGEM.getText(), MontosFGEM.leerFGEM(txtMontoFGEM.getText(), "Monto"),
                    txtComprobanteFGEM.getText(), cmbCuentaFGEM.cuentaIdFGEM());
        }
    }
}
