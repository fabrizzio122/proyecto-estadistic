package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.agricultura.ComparacionCultivosFGEM;
import com.businessanalyzer.analytics.agricultura.ResultadoCampaniaFGEM;
import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;
import com.businessanalyzer.analytics.ganaderia.IndicadorGanaderoFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.model.CampaniaFGEM;
import com.businessanalyzer.model.CosechaFGEM;
import com.businessanalyzer.model.CostoCampaniaFGEM;
import com.businessanalyzer.model.CultivoFGEM;
import com.businessanalyzer.model.DatosCampaniaFGEM;
import com.businessanalyzer.model.DatosParcelaFGEM;
import com.businessanalyzer.model.DestinoCosechaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EstadoCampaniaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.ParcelaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.RubroCostoFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.TenenciaFGEM;
import com.businessanalyzer.service.AgriculturaServiceFGEM;
import com.businessanalyzer.ui.CamposFechaFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.FormulariosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.TarjetaIndicadorFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Módulo "Agricultura": parcelas, campañas, costos, cosechas y resultado de cada campaña. */
public class AgriculturaControllerFGEM implements VistaModuloFGEM {

    private static final String TODAS_FGEM = "Todas";
    private static final String EN_CURSO_FGEM = "En curso";
    private static final String CERRADAS_FGEM = "Cerradas";
    private static final PseudoClass ARCHIVADO_FGEM = PseudoClass.getPseudoClass("archivado");

    private final AppContextFGEM contextoFGEM;
    private MonedaFGEM monedaFGEM = MonedaFGEM.PEN;

    private final ObservableList<ResultadoCampaniaFGEM> campaniasFGEM = FXCollections.observableArrayList();
    private final FilteredList<ResultadoCampaniaFGEM> filtradasFGEM = new FilteredList<>(campaniasFGEM);

    private final TarjetaIndicadorFGEM kpiAreaFGEM = new TarjetaIndicadorFGEM("Área sembrada", "fth-map");
    private final TarjetaIndicadorFGEM kpiInvertidoFGEM = new TarjetaIndicadorFGEM("Invertido en curso", "fth-trending-down");
    private final TarjetaIndicadorFGEM kpiCosechadoFGEM = new TarjetaIndicadorFGEM("Cosechado", "fth-package");
    private final TarjetaIndicadorFGEM kpiMargenFGEM = new TarjetaIndicadorFGEM("Margen de campañas cerradas", "fth-dollar-sign");

    @FXML private HBox indicadoresFGEM;
    @FXML private TabPane pestanasFGEM;
    @FXML private ComboBox<String> cmbFiltroFGEM;
    @FXML private CheckBox chkAnuladasFGEM;
    @FXML private Button btnCostoFGEM;
    @FXML private Button btnCosechaFGEM;
    @FXML private Button btnCerrarFGEM;
    @FXML private Button btnRegistrosFGEM;
    @FXML private MenuButton menuMasFGEM;
    @FXML private MenuItem itemEditarFGEM;
    @FXML private MenuItem itemReabrirFGEM;
    @FXML private MenuItem itemAnularFGEM;
    @FXML private Button btnNuevaCampaniaFGEM;
    @FXML private TableView<ResultadoCampaniaFGEM> tablaCampaniasFGEM;
    @FXML private TableColumn<ResultadoCampaniaFGEM, String> colCamCultivoFGEM;
    @FXML private TableColumn<ResultadoCampaniaFGEM, LocalDate> colCamSiembraFGEM;
    @FXML private TableColumn<ResultadoCampaniaFGEM, String> colCamAreaFGEM;
    @FXML private TableColumn<ResultadoCampaniaFGEM, String> colCamEstadoFGEM;
    @FXML private TableColumn<ResultadoCampaniaFGEM, BigDecimal> colCamCostoFGEM;
    @FXML private TableColumn<ResultadoCampaniaFGEM, String> colCamRendimientoFGEM;
    @FXML private TableColumn<ResultadoCampaniaFGEM, BigDecimal> colCamMargenFGEM;
    @FXML private Label lblDetalleTituloFGEM;
    @FXML private VBox detalleFGEM;

    @FXML private CheckBox chkInactivasFGEM;
    @FXML private Button btnEditarParcelaFGEM;
    @FXML private Button btnEstadoParcelaFGEM;
    @FXML private Button btnNuevaParcelaFGEM;
    @FXML private TableView<ParcelaFGEM> tablaParcelasFGEM;
    @FXML private TableColumn<ParcelaFGEM, String> colParNombreFGEM;
    @FXML private TableColumn<ParcelaFGEM, String> colParAreaFGEM;
    @FXML private TableColumn<ParcelaFGEM, String> colParTenenciaFGEM;
    @FXML private TableColumn<ParcelaFGEM, String> colParRiegoFGEM;
    @FXML private TableColumn<ParcelaFGEM, String> colParUbicacionFGEM;

    @FXML private TableView<ComparacionCultivosFGEM> tablaComparacionFGEM;
    @FXML private TableColumn<ComparacionCultivosFGEM, String> colCmpCultivoFGEM;
    @FXML private TableColumn<ComparacionCultivosFGEM, String> colCmpCampaniasFGEM;
    @FXML private TableColumn<ComparacionCultivosFGEM, String> colCmpAreaFGEM;
    @FXML private TableColumn<ComparacionCultivosFGEM, String> colCmpRendimientoFGEM;
    @FXML private TableColumn<ComparacionCultivosFGEM, String> colCmpAvanceFGEM;
    @FXML private TableColumn<ComparacionCultivosFGEM, String> colCmpCostoFGEM;
    @FXML private TableColumn<ComparacionCultivosFGEM, BigDecimal> colCmpMargenFGEM;
    @FXML private TableColumn<ComparacionCultivosFGEM, String> colCmpMejorFGEM;
    @FXML private javafx.scene.layout.StackPane graficoCampaniasFGEM;

    public AgriculturaControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        indicadoresFGEM.getChildren().addAll(kpiAreaFGEM, kpiInvertidoFGEM, kpiCosechadoFGEM, kpiMargenFGEM);
        cmbFiltroFGEM.getItems().setAll(TODAS_FGEM, EN_CURSO_FGEM, CERRADAS_FGEM);
        cmbFiltroFGEM.setValue(TODAS_FGEM);
        cmbFiltroFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> filtrarFGEM());
        configurarTablaCampaniasFGEM();
        configurarTablaParcelasFGEM();
        configurarTablaComparacionFGEM();

        boolean gestionaFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.AGRO_GESTIONAR);
        for (Node nodoFGEM : List.of(btnCostoFGEM, btnCosechaFGEM, btnCerrarFGEM, menuMasFGEM, btnNuevaCampaniaFGEM, btnEditarParcelaFGEM,
                btnEstadoParcelaFGEM, btnNuevaParcelaFGEM)) {
            nodoFGEM.setVisible(gestionaFGEM);
            nodoFGEM.setManaged(gestionaFGEM);
        }
    }

    @Override
    public void alMostrarFGEM() {
        cargarFGEM();
    }

    // ------------------------------------------------------------------ carga

    @FXML
    private void cargarFGEM() {
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        boolean agroFGEM = empresaFGEM != null && empresaFGEM.sectorFGEM() == SectorFGEM.AGROPECUARIO;
        btnNuevaCampaniaFGEM.setDisable(!agroFGEM);
        btnNuevaParcelaFGEM.setDisable(!agroFGEM);
        if (!agroFGEM) {
            String mensajeFGEM = empresaFGEM == null ? "Selecciona una empresa en la barra superior."
                    : "La agricultura está disponible solo para empresas del sector agropecuario.";
            limpiarFGEM(mensajeFGEM);
            return;
        }
        monedaFGEM = empresaFGEM.monedaFGEM();
        Long seleccionadaFGEM = seleccionadaFGEM() == null ? null : seleccionadaFGEM().campaniaFGEM().idFGEM();
        try {
            AgriculturaServiceFGEM servicioFGEM = servicioFGEM();
            tablaParcelasFGEM.getItems().setAll(servicioFGEM.listarParcelasFGEM(chkInactivasFGEM.isSelected()));
            campaniasFGEM.setAll(servicioFGEM.listarCampaniasFGEM(chkAnuladasFGEM.isSelected()));
        } catch (RuntimeException eFGEM) {
            limpiarFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        tablaParcelasFGEM.setPlaceholder(new Label("Aún no hay parcelas. Registra la primera con «Nueva parcela»."));
        tablaCampaniasFGEM.setPlaceholder(new Label(tablaParcelasFGEM.getItems().isEmpty()
                ? "Primero registra tus parcelas en la pestaña «Parcelas»." : "Aún no hay campañas."));
        mostrarResumenFGEM();
        mostrarComparacionFGEM();
        filtrarFGEM();
        if (seleccionadaFGEM != null) {
            tablaCampaniasFGEM.getItems().stream().filter(rFGEM -> rFGEM.campaniaFGEM().idFGEM() == seleccionadaFGEM).findFirst()
                    .ifPresent(rFGEM -> tablaCampaniasFGEM.getSelectionModel().select(rFGEM));
        }
        if (tablaCampaniasFGEM.getSelectionModel().isEmpty() && !tablaCampaniasFGEM.getItems().isEmpty()) {
            tablaCampaniasFGEM.getSelectionModel().selectFirst();
        }
        mostrarDetalleFGEM();
        actualizarAccionesFGEM();
    }

    private void limpiarFGEM(String mensajeFGEM) {
        campaniasFGEM.clear();
        tablaParcelasFGEM.getItems().clear();
        tablaCampaniasFGEM.setPlaceholder(new Label(mensajeFGEM));
        tablaParcelasFGEM.setPlaceholder(new Label(mensajeFGEM));
        detalleFGEM.getChildren().clear();
        for (TarjetaIndicadorFGEM kpiFGEM : List.of(kpiAreaFGEM, kpiInvertidoFGEM, kpiCosechadoFGEM, kpiMargenFGEM)) {
            kpiFGEM.actualizarFGEM(TarjetaIndicadorFGEM.SIN_VALOR_FGEM, "");
        }
    }

    private void mostrarResumenFGEM() {
        List<ResultadoCampaniaFGEM> enCursoFGEM = campaniasFGEM.stream()
                .filter(rFGEM -> rFGEM.campaniaFGEM().estadoFGEM() == EstadoCampaniaFGEM.EN_CURSO).toList();
        List<ResultadoCampaniaFGEM> cerradasFGEM = campaniasFGEM.stream().filter(rFGEM -> rFGEM.campaniaFGEM().estadoFGEM().cerradaFGEM()).toList();
        BigDecimal areaFGEM = enCursoFGEM.stream().map(rFGEM -> rFGEM.campaniaFGEM().areaFGEM()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal invertidoFGEM = enCursoFGEM.stream().map(ResultadoCampaniaFGEM::costoTotalFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cosechadoFGEM = campaniasFGEM.stream().map(ResultadoCampaniaFGEM::produccionFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal margenFGEM = cerradasFGEM.stream().map(ResultadoCampaniaFGEM::margenFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
        kpiAreaFGEM.actualizarFGEM(hectareasFGEM(areaFGEM), enCursoFGEM.size() + (enCursoFGEM.size() == 1 ? " campaña en curso" : " campañas en curso"));
        kpiInvertidoFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(invertidoFGEM, monedaFGEM), "Costos de las campañas en curso");
        kpiCosechadoFGEM.actualizarFGEM(MontosFGEM.cantidadFGEM(cosechadoFGEM.setScale(0, RoundingMode.HALF_UP)) + " kg", "En todas las campañas");
        kpiMargenFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(margenFGEM, monedaFGEM),
                cerradasFGEM.size() + (cerradasFGEM.size() == 1 ? " campaña cerrada" : " campañas cerradas"));
    }

    // ------------------------------------------------------------------ comparación

    private void mostrarComparacionFGEM() {
        tablaComparacionFGEM.getItems().setAll(ComparacionCultivosFGEM.deFGEM(campaniasFGEM));
        tablaComparacionFGEM.setPlaceholder(new Label("Aún no hay campañas cerradas para comparar."));
        List<ResultadoCampaniaFGEM> cerradasFGEM = campaniasFGEM.stream()
                .filter(rFGEM -> rFGEM.campaniaFGEM().estadoFGEM().cerradaFGEM())
                .sorted(java.util.Comparator.comparing((ResultadoCampaniaFGEM rFGEM) -> rFGEM.campaniaFGEM().fechaSiembraFGEM()))
                .toList();
        if (cerradasFGEM.isEmpty()) {
            Label vacioFGEM = new Label("Sin campañas cerradas.");
            vacioFGEM.getStyleClass().add("muted");
            graficoCampaniasFGEM.getChildren().setAll(vacioFGEM);
            return;
        }
        List<String> nombresFGEM = cerradasFGEM.stream().map(rFGEM -> rFGEM.campaniaFGEM().cultivoFGEM().descripcionFGEM().split(" \\(")[0]
                + " " + rFGEM.campaniaFGEM().fechaSiembraFGEM().getYear()).toList();
        double[] rendimientoFGEM = cerradasFGEM.stream().mapToDouble(rFGEM -> rFGEM.rendimientoFGEM() == null ? 0 : rFGEM.rendimientoFGEM().doubleValue()).toArray();
        double[] referenciaFGEM = cerradasFGEM.stream().mapToDouble(rFGEM -> rFGEM.campaniaFGEM().rendimientoReferenciaFGEM() == null ? 0
                : rFGEM.campaniaFGEM().rendimientoReferenciaFGEM().doubleValue()).toArray();
        java.util.Map<String, double[]> seriesFGEM = new java.util.LinkedHashMap<>();
        seriesFGEM.put("Rendimiento", rendimientoFGEM);
        seriesFGEM.put("Referencia", referenciaFGEM);
        graficoCampaniasFGEM.getChildren().setAll(com.businessanalyzer.ui.GraficosFGEM.visorFGEM(
                com.businessanalyzer.ui.GraficosFGEM.barrasCategoriasFGEM(nombresFGEM, seriesFGEM, "#,##0")));
    }

    private void configurarTablaComparacionFGEM() {
        colCmpCultivoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().cultivoFGEM().descripcionFGEM()));
        colCmpCampaniasFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().campaniasFGEM()
                + (cFGEM.getValue().perdidasFGEM() == 0 ? "" : " (" + cFGEM.getValue().perdidasFGEM()
                + (cFGEM.getValue().perdidasFGEM() == 1 ? " perdida)" : " perdidas)"))));
        colCmpAreaFGEM.setCellValueFactory(cFGEM -> textoFGEM(MontosFGEM.cantidadFGEM(cFGEM.getValue().areaFGEM())));
        colCmpRendimientoFGEM.setCellValueFactory(cFGEM -> textoFGEM(MontosFGEM.cantidadFGEM(cFGEM.getValue().rendimientoFGEM().setScale(0, RoundingMode.HALF_UP))));
        colCmpAvanceFGEM.setCellValueFactory(cFGEM -> textoFGEM(MontosFGEM.porcentajeFGEM(cFGEM.getValue().avanceFGEM())));
        colCmpCostoFGEM.setCellValueFactory(cFGEM -> textoFGEM(MontosFGEM.formatearFGEM(cFGEM.getValue().costoPorHaFGEM(), monedaFGEM)));
        colCmpMargenFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().margenPorHaFGEM()));
        colCmpMargenFGEM.setCellFactory(colFGEM -> celdaMontoFGEM(true));
        colCmpMejorFGEM.setCellValueFactory(cFGEM -> {
            ResultadoCampaniaFGEM mFGEM = cFGEM.getValue().mejorFGEM();
            return textoFGEM(mFGEM.campaniaFGEM().fechaSiembraFGEM().getYear() + " · " + mFGEM.campaniaFGEM().parcelaNombreFGEM()
                    + (mFGEM.rendimientoFGEM() == null ? "" : " · " + MontosFGEM.cantidadFGEM(mFGEM.rendimientoFGEM().setScale(0, RoundingMode.HALF_UP)) + " kg/ha"));
        });
    }

    private void filtrarFGEM() {
        String filtroFGEM = cmbFiltroFGEM.getValue();
        filtradasFGEM.setPredicate(rFGEM -> filtroFGEM == null || TODAS_FGEM.equals(filtroFGEM)
                || (EN_CURSO_FGEM.equals(filtroFGEM) && rFGEM.campaniaFGEM().estadoFGEM() == EstadoCampaniaFGEM.EN_CURSO)
                || (CERRADAS_FGEM.equals(filtroFGEM) && rFGEM.campaniaFGEM().estadoFGEM().cerradaFGEM()));
    }

    private void mostrarDetalleFGEM() {
        ResultadoCampaniaFGEM rFGEM = seleccionadaFGEM();
        detalleFGEM.getChildren().clear();
        if (rFGEM == null) {
            lblDetalleTituloFGEM.setText("Resultado de la campaña");
            Label vacioFGEM = new Label("Selecciona una campaña para ver su resultado.");
            vacioFGEM.getStyleClass().add("muted");
            vacioFGEM.setWrapText(true);
            detalleFGEM.getChildren().add(vacioFGEM);
            return;
        }
        CampaniaFGEM cFGEM = rFGEM.campaniaFGEM();
        lblDetalleTituloFGEM.setText(cFGEM.cultivoFGEM().descripcionFGEM() + (cFGEM.variedadFGEM() == null ? "" : " (" + cFGEM.variedadFGEM() + ")"));
        Label subFGEM = new Label(cFGEM.parcelaNombreFGEM() + " · " + hectareasFGEM(cFGEM.areaFGEM()) + " · sembrada el "
                + FechasFGEM.paraMostrarFGEM(cFGEM.fechaSiembraFGEM()) + " · " + cFGEM.estadoFGEM().descripcionFGEM().toLowerCase()
                + (cFGEM.fechaCierreFGEM() == null ? "" : " el " + FechasFGEM.paraMostrarFGEM(cFGEM.fechaCierreFGEM()))
                + (cFGEM.motivoFGEM() == null ? "" : " (" + cFGEM.motivoFGEM() + ")"));
        subFGEM.getStyleClass().add("hint");
        subFGEM.setWrapText(true);
        detalleFGEM.getChildren().add(subFGEM);
        for (IndicadorGanaderoFGEM indFGEM : rFGEM.evaluarFGEM(monedaFGEM)) {
            Label nombreFGEM = new Label(indFGEM.nombreFGEM());
            nombreFGEM.getStyleClass().add("info-key");
            nombreFGEM.setWrapText(true);
            Label valorFGEM = new Label(indFGEM.valorFGEM());
            valorFGEM.getStyleClass().add("info-value");
            valorFGEM.setMinWidth(Region.USE_PREF_SIZE);
            valorFGEM.setStyle("-fx-font-weight: bold;");
            if (indFGEM.semaforoFGEM().evaluadoFGEM() || "—".equals(indFGEM.valorFGEM())) {
                valorFGEM.getStyleClass().add("semaforo-" + switch (indFGEM.semaforoFGEM()) {
                    case VERDE -> "verde";
                    case AMARILLO -> "amarillo";
                    case ROJO -> "rojo";
                    default -> "sin-datos";
                });
            }
            Region espacioFGEM = new Region();
            HBox.setHgrow(espacioFGEM, javafx.scene.layout.Priority.ALWAYS);
            HBox cabeceraFGEM = new HBox(8, nombreFGEM, espacioFGEM, valorFGEM);
            Label lecturaFGEM = new Label(indFGEM.lecturaFGEM());
            lecturaFGEM.getStyleClass().add("muted");
            lecturaFGEM.setWrapText(true);
            VBox bloqueFGEM = new VBox(2, cabeceraFGEM, lecturaFGEM);
            if (indFGEM.criterioFGEM() != null) {
                Tooltip ayudaFGEM = new Tooltip(indFGEM.criterioFGEM());
                Tooltip.install(bloqueFGEM, ayudaFGEM);
            }
            detalleFGEM.getChildren().add(bloqueFGEM);
        }
    }

    // ------------------------------------------------------------------ campañas

    @FXML
    private void nuevaCampaniaFGEM() {
        List<ParcelaFGEM> activasFGEM = tablaParcelasFGEM.getItems().stream().filter(ParcelaFGEM::activaFGEM).toList();
        if (activasFGEM.isEmpty()) {
            NotificadorFGEM.infoFGEM("Primero registra una parcela en la pestaña «Parcelas».");
            pestanasFGEM.getSelectionModel().select(2);
            return;
        }
        FormularioCampaniaFGEM formularioFGEM = new FormularioCampaniaFGEM(activasFGEM, null);
        CampaniaFGEM[] creadaFGEM = new CampaniaFGEM[1];
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Nueva campaña", null, formularioFGEM.contenidoFGEM(), "Registrar", 520,
                () -> creadaFGEM[0] = servicioFGEM().registrarCampaniaFGEM(formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            seleccionarFGEM(creadaFGEM[0].idFGEM());
            NotificadorFGEM.exitoFGEM("Campaña registrada. Ahora registra sus costos y, al final, la cosecha.");
        }
    }

    @FXML
    private void editarCampaniaFGEM() {
        ResultadoCampaniaFGEM rFGEM = seleccionadaFGEM();
        if (rFGEM == null || rFGEM.campaniaFGEM().estadoFGEM() == EstadoCampaniaFGEM.ANULADA) {
            return;
        }
        FormularioCampaniaFGEM formularioFGEM = new FormularioCampaniaFGEM(tablaParcelasFGEM.getItems(), rFGEM.campaniaFGEM());
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Editar campaña", null, formularioFGEM.contenidoFGEM(), "Guardar cambios",
                520, () -> servicioFGEM().actualizarCampaniaFGEM(rFGEM.campaniaFGEM().idFGEM(), formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Campaña actualizada.");
        }
    }

    @FXML
    private void registrarCostoFGEM() {
        ResultadoCampaniaFGEM rFGEM = seleccionadaFGEM();
        if (rFGEM != null && formularioCostoFGEM(rFGEM.campaniaFGEM().nombreFGEM(), rFGEM.campaniaFGEM().idFGEM(), null)) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Costo registrado.");
        }
    }

    /** Formulario de costo: registra uno nuevo en la campaña o, con {@code original}, lo corrige. */
    private boolean formularioCostoFGEM(String campaniaFGEM, long campaniaIdFGEM, CostoCampaniaFGEM originalFGEM) {
        ComboBox<RubroCostoFGEM> rubroFGEM = new ComboBox<>(FXCollections.observableArrayList(RubroCostoFGEM.values()));
        rubroFGEM.setPromptText("Selecciona el rubro");
        rubroFGEM.setVisibleRowCount(11);
        Label ayudaFGEM = notaFGEM();
        rubroFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> ayudaFGEM.setText(nFGEM == null ? "" : nFGEM.ayudaFGEM()));
        DatePicker fechaFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        TextField descripcionFGEM = new TextField();
        descripcionFGEM.setPromptText("Ej.: 4 sacos de urea");
        TextField montoFGEM = new TextField();
        montoFGEM.setPromptText("0.00");
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Rubro *", rubroFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "", ayudaFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Fecha *", fechaFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Descripción *", descripcionFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Monto (" + monedaFGEM.simboloFGEM() + ") *", montoFGEM);
        if (originalFGEM != null) {
            rubroFGEM.setValue(originalFGEM.rubroFGEM());
            fechaFGEM.setValue(originalFGEM.fechaFGEM());
            descripcionFGEM.setText(originalFGEM.descripcionFGEM());
            montoFGEM.setText(MontosFGEM.paraEditarFGEM(originalFGEM.montoFGEM()));
        }
        return DialogosFGEM.formularioFGEM(ventanaFGEM(), originalFGEM == null ? "Registrar costo" : "Editar costo", campaniaFGEM, grillaFGEM,
                originalFGEM == null ? "Registrar" : "Guardar cambios", () -> {
                    LocalDate diaFGEM = CamposFechaFGEM.leerFGEM(fechaFGEM, "el costo");
                    BigDecimal importeFGEM = MontosFGEM.leerFGEM(montoFGEM.getText(), "Monto");
                    if (originalFGEM == null) {
                        servicioFGEM().registrarCostoFGEM(campaniaIdFGEM, diaFGEM, rubroFGEM.getValue(), descripcionFGEM.getText(), importeFGEM);
                    } else {
                        servicioFGEM().corregirCostoFGEM(originalFGEM.idFGEM(), diaFGEM, rubroFGEM.getValue(), descripcionFGEM.getText(), importeFGEM);
                    }
                });
    }

    @FXML
    private void registrarCosechaFGEM() {
        ResultadoCampaniaFGEM rFGEM = seleccionadaFGEM();
        if (rFGEM != null && formularioCosechaFGEM(rFGEM.campaniaFGEM().nombreFGEM(), rFGEM.campaniaFGEM().idFGEM(), null)) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Cosecha registrada.");
        }
    }

    /** Formulario de cosecha: registra una nueva en la campaña o, con {@code original}, la corrige. */
    private boolean formularioCosechaFGEM(String campaniaFGEM, long campaniaIdFGEM, CosechaFGEM originalFGEM) {
        DatePicker fechaFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        TextField kilosFGEM = new TextField();
        kilosFGEM.setPromptText("Kilos");
        ComboBox<DestinoCosechaFGEM> destinoFGEM = new ComboBox<>(FXCollections.observableArrayList(DestinoCosechaFGEM.values()));
        destinoFGEM.setValue(DestinoCosechaFGEM.VENTA);
        TextField precioFGEM = new TextField();
        precioFGEM.setPromptText("Opcional");
        destinoFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> {
            precioFGEM.setDisable(nFGEM != DestinoCosechaFGEM.VENTA);
            if (nFGEM != DestinoCosechaFGEM.VENTA) {
                precioFGEM.clear();
            }
        });
        TextField observacionFGEM = new TextField();
        observacionFGEM.setPromptText("Opcional. Ej.: primera calidad");
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Fecha *", fechaFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Cantidad (kg) *", kilosFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Destino *", destinoFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Precio por kg (" + monedaFGEM.simboloFGEM() + ")", precioFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Observación", observacionFGEM);
        FormulariosFGEM.notaFGEM(grillaFGEM, "Registra por separado lo que vendes, lo que guardas para consumo o semilla y lo que "
                + "das a los animales. Una arroba son 11.5 kg; un quintal, 46 kg.");
        if (originalFGEM != null) {
            fechaFGEM.setValue(originalFGEM.fechaFGEM());
            kilosFGEM.setText(MontosFGEM.paraEditarFGEM(originalFGEM.cantidadFGEM()));
            destinoFGEM.setValue(originalFGEM.destinoFGEM());
            precioFGEM.setText(originalFGEM.precioFGEM() == null ? "" : MontosFGEM.paraEditarFGEM(originalFGEM.precioFGEM()));
            observacionFGEM.setText(originalFGEM.observacionFGEM() == null ? "" : originalFGEM.observacionFGEM());
        }
        return DialogosFGEM.formularioFGEM(ventanaFGEM(), originalFGEM == null ? "Registrar cosecha" : "Editar cosecha", campaniaFGEM, grillaFGEM,
                originalFGEM == null ? "Registrar" : "Guardar cambios", () -> {
                    LocalDate diaFGEM = CamposFechaFGEM.leerFGEM(fechaFGEM, "la cosecha");
                    BigDecimal cantidadFGEM = MontosFGEM.leerFGEM(kilosFGEM.getText(), "Cantidad");
                    BigDecimal precioKgFGEM = precioFGEM.getText() == null || precioFGEM.getText().isBlank() ? null
                            : MontosFGEM.leerFGEM(precioFGEM.getText(), "Precio");
                    if (originalFGEM == null) {
                        servicioFGEM().registrarCosechaFGEM(campaniaIdFGEM, diaFGEM, cantidadFGEM, destinoFGEM.getValue(), precioKgFGEM,
                                observacionFGEM.getText());
                    } else {
                        servicioFGEM().corregirCosechaFGEM(originalFGEM.idFGEM(), diaFGEM, cantidadFGEM, destinoFGEM.getValue(), precioKgFGEM,
                                observacionFGEM.getText());
                    }
                });
    }

    @FXML
    private void cerrarCampaniaFGEM() {
        ResultadoCampaniaFGEM rFGEM = seleccionadaFGEM();
        if (rFGEM == null || rFGEM.campaniaFGEM().estadoFGEM() != EstadoCampaniaFGEM.EN_CURSO) {
            return;
        }
        ComboBox<EstadoCampaniaFGEM> estadoFGEM = new ComboBox<>(FXCollections.observableArrayList(
                EstadoCampaniaFGEM.COSECHADA, EstadoCampaniaFGEM.PERDIDA));
        estadoFGEM.setValue(rFGEM.produccionFGEM().signum() > 0 ? EstadoCampaniaFGEM.COSECHADA : EstadoCampaniaFGEM.PERDIDA);
        DatePicker fechaFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        TextField motivoFGEM = new TextField();
        motivoFGEM.setPromptText("Ej.: helada, granizada, sequía, plaga");
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Resultado *", estadoFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Fecha *", fechaFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Causa / observación", motivoFGEM);
        FormulariosFGEM.notaFGEM(grillaFGEM, "Si se perdió, indica la causa. Una campaña cerrada se puede reabrir desde «Más».");
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Cerrar campaña", rFGEM.campaniaFGEM().nombreFGEM(), grillaFGEM, "Cerrar",
                () -> servicioFGEM().cerrarCampaniaFGEM(rFGEM.campaniaFGEM().idFGEM(), estadoFGEM.getValue(), CamposFechaFGEM.leerFGEM(fechaFGEM, "cierre"),
                        motivoFGEM.getText()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Campaña cerrada.");
        }
    }

    @FXML
    private void reabrirCampaniaFGEM() {
        ResultadoCampaniaFGEM rFGEM = seleccionadaFGEM();
        if (rFGEM == null || !rFGEM.campaniaFGEM().estadoFGEM().cerradaFGEM()) {
            return;
        }
        try {
            servicioFGEM().reabrirCampaniaFGEM(rFGEM.campaniaFGEM().idFGEM());
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("La campaña vuelve a estar en curso.");
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    @FXML
    private void anularCampaniaFGEM() {
        ResultadoCampaniaFGEM rFGEM = seleccionadaFGEM();
        if (rFGEM == null || rFGEM.campaniaFGEM().estadoFGEM() == EstadoCampaniaFGEM.ANULADA) {
            return;
        }
        TextField motivoFGEM = new TextField();
        motivoFGEM.setPromptText("Ej.: registrada por error");
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Motivo *", motivoFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Anular campaña",
                rFGEM.campaniaFGEM().nombreFGEM() + " dejará de contar en los resultados y en el balance. Si se perdió por "
                        + "helada o granizo, no la anules: ciérrala como «Perdida».",
                grillaFGEM, "Anular", () -> servicioFGEM().anularCampaniaFGEM(rFGEM.campaniaFGEM().idFGEM(), motivoFGEM.getText()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Campaña anulada.");
        }
    }

    /** Costos y cosechas de la campaña, con la opción de corregirlos o anular los registrados por error. */
    @FXML
    private void verRegistrosFGEM() {
        ResultadoCampaniaFGEM rFGEM = seleccionadaFGEM();
        if (rFGEM == null) {
            return;
        }
        long idFGEM = rFGEM.campaniaFGEM().idFGEM();
        boolean gestionaFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.AGRO_GESTIONAR);
        TableView<CostoCampaniaFGEM> costosFGEM = new TableView<>();
        costosFGEM.setPrefHeight(200);
        costosFGEM.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        costosFGEM.getColumns().add(columnaFGEM("Fecha", 90, kFGEM -> FechasFGEM.paraMostrarFGEM(kFGEM.fechaFGEM())));
        costosFGEM.getColumns().add(columnaFGEM("Rubro", 150, kFGEM -> kFGEM.rubroFGEM().descripcionFGEM() + (kFGEM.anuladoFGEM() ? " (anulado)" : "")));
        costosFGEM.getColumns().add(columnaFGEM("Descripción", 170, CostoCampaniaFGEM::descripcionFGEM));
        costosFGEM.getColumns().add(columnaFGEM("Monto", 100, kFGEM -> MontosFGEM.formatearFGEM(kFGEM.montoFGEM(), monedaFGEM)));
        costosFGEM.setRowFactory(tFGEM -> filaArchivadaFGEM(CostoCampaniaFGEM::anuladoFGEM));
        costosFGEM.setPlaceholder(new Label("Sin costos registrados."));

        TableView<CosechaFGEM> cosechasFGEM = new TableView<>();
        cosechasFGEM.setPrefHeight(160);
        cosechasFGEM.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cosechasFGEM.getColumns().add(columnaFGEM("Fecha", 90, hFGEM -> FechasFGEM.paraMostrarFGEM(hFGEM.fechaFGEM())));
        cosechasFGEM.getColumns().add(columnaFGEM("Kilos", 90, hFGEM -> MontosFGEM.cantidadFGEM(hFGEM.cantidadFGEM())));
        cosechasFGEM.getColumns().add(columnaFGEM("Destino", 170, hFGEM -> hFGEM.destinoFGEM().descripcionFGEM() + (hFGEM.anuladoFGEM() ? " (anulada)" : "")));
        cosechasFGEM.getColumns().add(columnaFGEM("Precio/kg", 90, hFGEM -> hFGEM.precioFGEM() == null ? "" : MontosFGEM.formatearFGEM(hFGEM.precioFGEM(), monedaFGEM)));
        cosechasFGEM.setRowFactory(tFGEM -> filaArchivadaFGEM(CosechaFGEM::anuladoFGEM));
        cosechasFGEM.setPlaceholder(new Label("Sin cosechas registradas."));

        boolean[] cambiosFGEM = {false};
        Runnable recargarFGEM = () -> {
            costosFGEM.getItems().setAll(servicioFGEM().listarCostosFGEM(idFGEM));
            cosechasFGEM.getItems().setAll(servicioFGEM().listarCosechasFGEM(idFGEM));
        };
        Label tituloCostosFGEM = new Label("Costos");
        tituloCostosFGEM.getStyleClass().add("step-title");
        Label tituloCosechasFGEM = new Label("Cosechas");
        tituloCosechasFGEM.getStyleClass().add("step-title");
        VBox contenidoFGEM = new VBox(8, tituloCostosFGEM, costosFGEM);
        if (gestionaFGEM) {
            Button anularCostoFGEM = new Button("Anular costo seleccionado");
            anularCostoFGEM.getStyleClass().add("button-secondary");
            anularCostoFGEM.disableProperty().bind(costosFGEM.getSelectionModel().selectedItemProperty().isNull());
            anularCostoFGEM.setOnAction(eFGEM -> anularFGEM(costosFGEM.getSelectionModel().getSelectedItem() == null ? null
                    : () -> servicioFGEM().anularCostoFGEM(costosFGEM.getSelectionModel().getSelectedItem().idFGEM()), recargarFGEM, cambiosFGEM));
            Button editarCostoFGEM = new Button("Editar costo seleccionado");
            editarCostoFGEM.getStyleClass().add("button-secondary");
            editarCostoFGEM.disableProperty().bind(costosFGEM.getSelectionModel().selectedItemProperty().isNull());
            editarCostoFGEM.setOnAction(eFGEM -> {
                CostoCampaniaFGEM kFGEM = costosFGEM.getSelectionModel().getSelectedItem();
                if (kFGEM != null && !kFGEM.anuladoFGEM() && formularioCostoFGEM(rFGEM.campaniaFGEM().nombreFGEM(), idFGEM, kFGEM)) {
                    cambiosFGEM[0] = true;
                    recargarFGEM.run();
                }
            });
            contenidoFGEM.getChildren().add(new HBox(8, editarCostoFGEM, anularCostoFGEM));
        }
        contenidoFGEM.getChildren().addAll(tituloCosechasFGEM, cosechasFGEM);
        if (gestionaFGEM) {
            Button anularCosechaFGEM = new Button("Anular cosecha seleccionada");
            anularCosechaFGEM.getStyleClass().add("button-secondary");
            anularCosechaFGEM.disableProperty().bind(cosechasFGEM.getSelectionModel().selectedItemProperty().isNull());
            anularCosechaFGEM.setOnAction(eFGEM -> anularFGEM(cosechasFGEM.getSelectionModel().getSelectedItem() == null ? null
                    : () -> servicioFGEM().anularCosechaFGEM(cosechasFGEM.getSelectionModel().getSelectedItem().idFGEM()), recargarFGEM, cambiosFGEM));
            Button editarCosechaFGEM = new Button("Editar cosecha seleccionada");
            editarCosechaFGEM.getStyleClass().add("button-secondary");
            editarCosechaFGEM.disableProperty().bind(cosechasFGEM.getSelectionModel().selectedItemProperty().isNull());
            editarCosechaFGEM.setOnAction(eFGEM -> {
                CosechaFGEM hFGEM = cosechasFGEM.getSelectionModel().getSelectedItem();
                if (hFGEM != null && !hFGEM.anuladoFGEM() && formularioCosechaFGEM(rFGEM.campaniaFGEM().nombreFGEM(), idFGEM, hFGEM)) {
                    cambiosFGEM[0] = true;
                    recargarFGEM.run();
                }
            });
            contenidoFGEM.getChildren().add(new HBox(8, editarCosechaFGEM, anularCosechaFGEM));
        }
        try {
            recargarFGEM.run();
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        DialogosFGEM.informacionFGEM(ventanaFGEM(), "Registros de la campaña", rFGEM.campaniaFGEM().nombreFGEM(), contenidoFGEM);
        if (cambiosFGEM[0]) {
            cargarFGEM();
        }
    }

    private void anularFGEM(Runnable accionFGEM, Runnable recargarFGEM, boolean[] cambiosFGEM) {
        if (accionFGEM == null || !DialogosFGEM.confirmarFGEM(ventanaFGEM(), "Anular registro",
                "El registro seleccionado dejará de contar en el resultado de la campaña.")) {
            return;
        }
        try {
            accionFGEM.run();
            cambiosFGEM[0] = true;
            recargarFGEM.run();
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    // ------------------------------------------------------------------ parcelas

    @FXML
    private void nuevaParcelaFGEM() {
        FormularioParcelaFGEM formularioFGEM = new FormularioParcelaFGEM(null);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Nueva parcela", null, formularioFGEM.contenidoFGEM(), "Registrar",
                () -> servicioFGEM().registrarParcelaFGEM(formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Parcela registrada.");
        }
    }

    @FXML
    private void editarParcelaFGEM() {
        ParcelaFGEM pFGEM = tablaParcelasFGEM.getSelectionModel().getSelectedItem();
        if (pFGEM == null) {
            return;
        }
        FormularioParcelaFGEM formularioFGEM = new FormularioParcelaFGEM(pFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Editar parcela", null, formularioFGEM.contenidoFGEM(), "Guardar cambios",
                () -> servicioFGEM().actualizarParcelaFGEM(pFGEM.idFGEM(), formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Parcela actualizada.");
        }
    }

    @FXML
    private void cambiarEstadoParcelaFGEM() {
        ParcelaFGEM pFGEM = tablaParcelasFGEM.getSelectionModel().getSelectedItem();
        if (pFGEM == null) {
            return;
        }
        try {
            servicioFGEM().cambiarParcelaActivaFGEM(pFGEM.idFGEM(), !pFGEM.activaFGEM());
            cargarFGEM();
            NotificadorFGEM.exitoFGEM(pFGEM.activaFGEM() ? "Parcela desactivada." : "Parcela reactivada.");
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    // ------------------------------------------------------------------ tablas

    private void configurarTablaCampaniasFGEM() {
        colCamCultivoFGEM.setCellValueFactory(cFGEM -> {
            CampaniaFGEM kFGEM = cFGEM.getValue().campaniaFGEM();
            return textoFGEM(kFGEM.cultivoFGEM().descripcionFGEM() + (kFGEM.variedadFGEM() == null ? "" : " (" + kFGEM.variedadFGEM() + ")"));
        });
        colCamSiembraFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().campaniaFGEM().fechaSiembraFGEM()));
        colCamSiembraFGEM.setCellFactory(colFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fechaFGEM, boolean vacioFGEM) {
                super.updateItem(fechaFGEM, vacioFGEM);
                setText(vacioFGEM ? null : FechasFGEM.paraMostrarFGEM(fechaFGEM));
            }
        });
        colCamAreaFGEM.setCellValueFactory(cFGEM -> textoFGEM(MontosFGEM.cantidadFGEM(cFGEM.getValue().campaniaFGEM().areaFGEM())));
        colCamEstadoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().campaniaFGEM().estadoFGEM().descripcionFGEM()));
        colCamCostoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().costoTotalFGEM()));
        colCamCostoFGEM.setCellFactory(colFGEM -> celdaMontoFGEM(false));
        colCamRendimientoFGEM.setCellValueFactory(cFGEM -> {
            BigDecimal rendFGEM = cFGEM.getValue().rendimientoFGEM();
            return textoFGEM(rendFGEM == null ? "—" : MontosFGEM.cantidadFGEM(rendFGEM.setScale(0, RoundingMode.HALF_UP)));
        });
        colCamMargenFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(
                cFGEM.getValue().produccionFGEM().signum() > 0 || cFGEM.getValue().campaniaFGEM().estadoFGEM().cerradaFGEM()
                        ? cFGEM.getValue().margenFGEM() : null));
        colCamMargenFGEM.setCellFactory(colFGEM -> celdaMontoFGEM(true));
        tablaCampaniasFGEM.setItems(filtradasFGEM);
        tablaCampaniasFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> {
            mostrarDetalleFGEM();
            actualizarAccionesFGEM();
        });
        tablaCampaniasFGEM.setRowFactory(tFGEM -> {
            TableRow<ResultadoCampaniaFGEM> filaFGEM = new TableRow<>() {
                @Override
                protected void updateItem(ResultadoCampaniaFGEM rFGEM, boolean vacioFGEM) {
                    super.updateItem(rFGEM, vacioFGEM);
                    pseudoClassStateChanged(ARCHIVADO_FGEM, !vacioFGEM && rFGEM != null
                            && rFGEM.campaniaFGEM().estadoFGEM() == EstadoCampaniaFGEM.ANULADA);
                }
            };
            filaFGEM.setOnMouseClicked(eFGEM -> {
                if (eFGEM.getButton() == MouseButton.PRIMARY && eFGEM.getClickCount() == 2 && !filaFGEM.isEmpty()) {
                    verRegistrosFGEM();
                }
            });
            return filaFGEM;
        });
    }

    private void configurarTablaParcelasFGEM() {
        colParNombreFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().nombreFGEM() + (cFGEM.getValue().activaFGEM() ? "" : "  (desactivada)")));
        colParAreaFGEM.setCellValueFactory(cFGEM -> textoFGEM(hectareasFGEM(cFGEM.getValue().areaFGEM())));
        colParTenenciaFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().tenenciaFGEM().descripcionFGEM()));
        colParRiegoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().riegoFGEM() ? "Con riego" : "Secano"));
        colParUbicacionFGEM.setCellValueFactory(cFGEM -> textoFGEM(Objects.requireNonNullElse(cFGEM.getValue().ubicacionFGEM(), "")));
        tablaParcelasFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarAccionesFGEM());
        tablaParcelasFGEM.setRowFactory(tFGEM -> filaArchivadaFGEM(pFGEM -> !pFGEM.activaFGEM()));
    }

    private void actualizarAccionesFGEM() {
        ResultadoCampaniaFGEM rFGEM = seleccionadaFGEM();
        EstadoCampaniaFGEM estadoFGEM = rFGEM == null ? null : rFGEM.campaniaFGEM().estadoFGEM();
        boolean vigenteFGEM = estadoFGEM != null && estadoFGEM != EstadoCampaniaFGEM.ANULADA;
        btnCostoFGEM.setDisable(!vigenteFGEM);
        btnCosechaFGEM.setDisable(!vigenteFGEM || estadoFGEM == EstadoCampaniaFGEM.COSECHADA);
        btnCerrarFGEM.setDisable(estadoFGEM != EstadoCampaniaFGEM.EN_CURSO);
        btnRegistrosFGEM.setDisable(rFGEM == null);
        menuMasFGEM.setDisable(rFGEM == null);
        itemEditarFGEM.setDisable(!vigenteFGEM);
        itemReabrirFGEM.setDisable(estadoFGEM == null || !estadoFGEM.cerradaFGEM());
        itemAnularFGEM.setDisable(!vigenteFGEM);

        ParcelaFGEM pFGEM = tablaParcelasFGEM.getSelectionModel().getSelectedItem();
        btnEditarParcelaFGEM.setDisable(pFGEM == null);
        btnEstadoParcelaFGEM.setDisable(pFGEM == null);
        btnEstadoParcelaFGEM.setText(pFGEM != null && !pFGEM.activaFGEM() ? "Reactivar" : "Desactivar");
    }

    private <T> TableCell<T, BigDecimal> celdaMontoFGEM(boolean colorearFGEM) {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal montoFGEM, boolean vacioFGEM) {
                super.updateItem(montoFGEM, vacioFGEM);
                getStyleClass().removeAll("margen-ok", "margen-negativo");
                if (vacioFGEM) {
                    setText(null);
                    return;
                }
                setText(montoFGEM == null ? "—" : MontosFGEM.formatearFGEM(montoFGEM, monedaFGEM));
                if (colorearFGEM && montoFGEM != null && montoFGEM.signum() != 0) {
                    getStyleClass().add(montoFGEM.signum() > 0 ? "margen-ok" : "margen-negativo");
                }
            }
        };
    }

    private static <T> TableRow<T> filaArchivadaFGEM(java.util.function.Predicate<T> apagadaFGEM) {
        return new TableRow<>() {
            @Override
            protected void updateItem(T itemFGEM, boolean vacioFGEM) {
                super.updateItem(itemFGEM, vacioFGEM);
                pseudoClassStateChanged(ARCHIVADO_FGEM, !vacioFGEM && itemFGEM != null && apagadaFGEM.test(itemFGEM));
            }
        };
    }

    private static <T> TableColumn<T, String> columnaFGEM(String tituloFGEM, double anchoFGEM, Function<T, String> valorFGEM) {
        TableColumn<T, String> columnaFGEM = new TableColumn<>(tituloFGEM);
        columnaFGEM.setPrefWidth(anchoFGEM);
        columnaFGEM.setSortable(false);
        columnaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(valorFGEM.apply(cFGEM.getValue())));
        return columnaFGEM;
    }

    // ------------------------------------------------------------------ utilidades

    private ResultadoCampaniaFGEM seleccionadaFGEM() {
        return tablaCampaniasFGEM.getSelectionModel().getSelectedItem();
    }

    private void seleccionarFGEM(long campaniaIdFGEM) {
        tablaCampaniasFGEM.getItems().stream().filter(rFGEM -> rFGEM.campaniaFGEM().idFGEM() == campaniaIdFGEM).findFirst()
                .ifPresent(rFGEM -> tablaCampaniasFGEM.getSelectionModel().select(rFGEM));
    }

    private AgriculturaServiceFGEM servicioFGEM() {
        return contextoFGEM.agriculturaFGEM();
    }

    private Window ventanaFGEM() {
        return tablaCampaniasFGEM.getScene().getWindow();
    }

    private static ReadOnlyStringWrapper textoFGEM(String valorFGEM) {
        return new ReadOnlyStringWrapper(valorFGEM);
    }

    private static String hectareasFGEM(BigDecimal haFGEM) {
        return MontosFGEM.cantidadFGEM(haFGEM) + " ha";
    }

    private static Label notaFGEM() {
        Label lFGEM = new Label();
        lFGEM.getStyleClass().add("hint");
        lFGEM.setWrapText(true);
        lFGEM.setMinHeight(Region.USE_PREF_SIZE);
        return lFGEM;
    }

    private static BigDecimal opcionalFGEM(String textoFGEM, String campoFGEM) {
        return textoFGEM == null || textoFGEM.isBlank() ? null : MontosFGEM.leerFGEM(textoFGEM, campoFGEM);
    }

    /** Formulario de campaña: propone el rendimiento de referencia del cultivo. */
    private final class FormularioCampaniaFGEM {
        private final ComboBox<ParcelaFGEM> cmbParcelaFGEM;
        private final ComboBox<CultivoFGEM> cmbCultivoFGEM = new ComboBox<>(FXCollections.observableArrayList(CultivoFGEM.values()));
        private final TextField txtVariedadFGEM = new TextField();
        private final DatePicker dpSiembraFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        private final TextField txtAreaFGEM = new TextField();
        private final TextField txtReferenciaFGEM = new TextField();
        private final TextField txtPrecioFGEM = new TextField();
        private final TextField txtObservacionFGEM = new TextField();

        FormularioCampaniaFGEM(List<ParcelaFGEM> parcelasFGEM, CampaniaFGEM cFGEM) {
            cmbParcelaFGEM = new ComboBox<>(FXCollections.observableArrayList(parcelasFGEM));
            cmbParcelaFGEM.setPromptText("Selecciona la parcela");
            cmbCultivoFGEM.setPromptText("Selecciona el cultivo");
            cmbCultivoFGEM.setVisibleRowCount(15);
            txtVariedadFGEM.setPromptText("Opcional. Ej.: Yungay, Canchán");
            txtAreaFGEM.setPromptText("Hectáreas");
            txtReferenciaFGEM.setPromptText("kg/ha");
            txtPrecioFGEM.setPromptText("Opcional. S/ por kg");
            txtObservacionFGEM.setPromptText("Opcional");
            cmbParcelaFGEM.valueProperty().addListener((oFGEM, aFGEM, pFGEM) -> {
                if (pFGEM != null && (txtAreaFGEM.getText() == null || txtAreaFGEM.getText().isBlank())) {
                    txtAreaFGEM.setText(MontosFGEM.cantidadFGEM(pFGEM.areaFGEM()));
                }
            });
            cmbCultivoFGEM.valueProperty().addListener((oFGEM, aFGEM, cultivoFGEM) -> {
                BigDecimal refFGEM = cultivoFGEM == null ? null : cultivoFGEM.rendimientoReferenciaFGEM();
                txtReferenciaFGEM.setText(refFGEM == null ? "" : refFGEM.toPlainString());
            });
            if (cFGEM != null) {
                parcelasFGEM.stream().filter(pFGEM -> pFGEM.idFGEM() == cFGEM.parcelaIdFGEM()).findFirst().ifPresent(cmbParcelaFGEM::setValue);
                cmbCultivoFGEM.setValue(cFGEM.cultivoFGEM());
                txtVariedadFGEM.setText(Objects.requireNonNullElse(cFGEM.variedadFGEM(), ""));
                dpSiembraFGEM.setValue(cFGEM.fechaSiembraFGEM());
                txtAreaFGEM.setText(MontosFGEM.cantidadFGEM(cFGEM.areaFGEM()));
                txtReferenciaFGEM.setText(cFGEM.rendimientoReferenciaFGEM() == null ? ""
                        : cFGEM.rendimientoReferenciaFGEM().stripTrailingZeros().toPlainString());
                txtPrecioFGEM.setText(cFGEM.precioChacraFGEM() == null ? "" : MontosFGEM.paraEditarFGEM(cFGEM.precioChacraFGEM()));
                txtObservacionFGEM.setText(Objects.requireNonNullElse(cFGEM.observacionFGEM(), ""));
            } else if (parcelasFGEM.size() == 1) {
                cmbParcelaFGEM.setValue(parcelasFGEM.get(0));
            }
        }

        GridPane contenidoFGEM() {
            GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
            FormulariosFGEM.filaFGEM(grillaFGEM, "Parcela *", cmbParcelaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Cultivo *", cmbCultivoFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Variedad", txtVariedadFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Fecha de siembra *", dpSiembraFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Área sembrada (ha) *", txtAreaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Rendimiento de referencia", txtReferenciaFGEM);
            FormulariosFGEM.notaFGEM(grillaFGEM, "Kilos por hectárea con que se comparará la cosecha. Se propone el promedio "
                    + "nacional aproximado; ajústalo al de tu zona si lo conoces.");
            FormulariosFGEM.filaFGEM(grillaFGEM, "Precio en chacra (" + monedaFGEM.simboloFGEM() + "/kg)", txtPrecioFGEM);
            FormulariosFGEM.notaFGEM(grillaFGEM, "Sirve para valorizar lo que no se vende (consumo, semilla, forraje).");
            FormulariosFGEM.filaFGEM(grillaFGEM, "Observación", txtObservacionFGEM);
            return grillaFGEM;
        }

        DatosCampaniaFGEM datosFGEM() {
            LocalDate siembraFGEM = CamposFechaFGEM.leerFGEM(dpSiembraFGEM, "siembra");
            ParcelaFGEM parcelaFGEM = cmbParcelaFGEM.getValue();
            return new DatosCampaniaFGEM(parcelaFGEM == null ? null : parcelaFGEM.idFGEM(), cmbCultivoFGEM.getValue(), txtVariedadFGEM.getText(),
                    siembraFGEM, opcionalFGEM(txtAreaFGEM.getText(), "Área sembrada"), opcionalFGEM(txtReferenciaFGEM.getText(),
                    "Rendimiento de referencia"), opcionalFGEM(txtPrecioFGEM.getText(), "Precio en chacra"),
                    txtObservacionFGEM.getText());
        }
    }

    /** Formulario de parcela. */
    private final class FormularioParcelaFGEM {
        private final TextField txtNombreFGEM = new TextField();
        private final TextField txtAreaFGEM = new TextField();
        private final ComboBox<TenenciaFGEM> cmbTenenciaFGEM = new ComboBox<>(FXCollections.observableArrayList(TenenciaFGEM.values()));
        private final CheckBox chkRiegoFGEM = new CheckBox("Tiene riego");
        private final TextField txtUbicacionFGEM = new TextField();

        FormularioParcelaFGEM(ParcelaFGEM pFGEM) {
            txtNombreFGEM.setPromptText("Ej.: Parcela Alta");
            txtAreaFGEM.setPromptText("Hectáreas. Ej.: 0.75");
            cmbTenenciaFGEM.setValue(TenenciaFGEM.PROPIA);
            txtUbicacionFGEM.setPromptText("Opcional. Ej.: sector Chacrapampa");
            if (pFGEM != null) {
                txtNombreFGEM.setText(pFGEM.nombreFGEM());
                txtAreaFGEM.setText(MontosFGEM.cantidadFGEM(pFGEM.areaFGEM()));
                cmbTenenciaFGEM.setValue(pFGEM.tenenciaFGEM());
                chkRiegoFGEM.setSelected(pFGEM.riegoFGEM());
                txtUbicacionFGEM.setText(Objects.requireNonNullElse(pFGEM.ubicacionFGEM(), ""));
            }
        }

        GridPane contenidoFGEM() {
            GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
            FormulariosFGEM.filaFGEM(grillaFGEM, "Nombre *", txtNombreFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Superficie (ha) *", txtAreaFGEM);
            FormulariosFGEM.notaFGEM(grillaFGEM, "1 hectárea = 10 000 m². Un topo equivale a unos 0.3 ha según la zona.");
            FormulariosFGEM.filaFGEM(grillaFGEM, "Tenencia *", cmbTenenciaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Agua", chkRiegoFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Ubicación", txtUbicacionFGEM);
            return grillaFGEM;
        }

        DatosParcelaFGEM datosFGEM() {
            return new DatosParcelaFGEM(txtNombreFGEM.getText(), opcionalFGEM(txtAreaFGEM.getText(), "Superficie"), cmbTenenciaFGEM.getValue(),
                    chkRiegoFGEM.isSelected(), txtUbicacionFGEM.getText());
        }
    }
}
