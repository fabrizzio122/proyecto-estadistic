package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.AspectoInspeccionFGEM;
import com.businessanalyzer.model.CompromisoFGEM;
import com.businessanalyzer.model.ConclusionVisitaFGEM;
import com.businessanalyzer.model.DatosVisitaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EstadoCompromisoFGEM;
import com.businessanalyzer.model.FotoVisitaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.ResultadoAspectoFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.TipoVisitaFGEM;
import com.businessanalyzer.model.VisitaFGEM;
import com.businessanalyzer.service.InformeTecnicoServiceFGEM;
import com.businessanalyzer.service.VisitaServiceFGEM;
import com.businessanalyzer.ui.CamposFechaFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.FormulariosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.TareasFGEM;
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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Módulo "Visitas de inspección": verificación en campo, compromisos, fotos e informe técnico. */
public class VisitasControllerFGEM implements VistaModuloFGEM {

    private static final PseudoClass ARCHIVADO_FGEM = PseudoClass.getPseudoClass("archivado");
    private static final String CLAVE_CARPETA_FGEM = "informes.carpeta";

    private final AppContextFGEM contextoFGEM;
    private final ObservableList<CompromisoFGEM> compromisosFGEM = FXCollections.observableArrayList();
    private final FilteredList<CompromisoFGEM> compromisosFiltradosFGEM = new FilteredList<>(compromisosFGEM);

    private final TarjetaIndicadorFGEM kpiVisitasFGEM = new TarjetaIndicadorFGEM("Visitas", "fth-clipboard");
    private final TarjetaIndicadorFGEM kpiCumplimientoFGEM = new TarjetaIndicadorFGEM("Cumplimiento (última visita)", "fth-check-circle");
    private final TarjetaIndicadorFGEM kpiCompromisosFGEM = new TarjetaIndicadorFGEM("Compromisos pendientes", "fth-flag");
    private final TarjetaIndicadorFGEM kpiProximaFGEM = new TarjetaIndicadorFGEM("Próxima visita", "fth-calendar");

    @FXML private HBox indicadoresFGEM;
    @FXML private TabPane pestanasFGEM;
    @FXML private CheckBox chkAnuladasFGEM;
    @FXML private ProgressIndicator progresoFGEM;
    @FXML private Button btnInformeFGEM;
    @FXML private Button btnFotosFGEM;
    @FXML private Button btnEditarFGEM;
    @FXML private Button btnAnularFGEM;
    @FXML private Button btnNuevaFGEM;
    @FXML private TableView<VisitaFGEM> tablaVisitasFGEM;
    @FXML private TableColumn<VisitaFGEM, LocalDate> colFechaFGEM;
    @FXML private TableColumn<VisitaFGEM, String> colTipoFGEM;
    @FXML private TableColumn<VisitaFGEM, String> colInspectorFGEM;
    @FXML private TableColumn<VisitaFGEM, String> colCumplimientoFGEM;
    @FXML private TableColumn<VisitaFGEM, VisitaFGEM> colConclusionFGEM;
    @FXML private Label lblDetalleTituloFGEM;
    @FXML private VBox detalleFGEM;

    @FXML private CheckBox chkSoloPendientesFGEM;
    @FXML private Button btnPendienteFGEM;
    @FXML private Button btnIncumplidoFGEM;
    @FXML private Button btnCumplidoFGEM;
    @FXML private TableView<CompromisoFGEM> tablaCompromisosFGEM;
    @FXML private TableColumn<CompromisoFGEM, String> colComDescripcionFGEM;
    @FXML private TableColumn<CompromisoFGEM, String> colComAcordadoFGEM;
    @FXML private TableColumn<CompromisoFGEM, String> colComPlazoFGEM;
    @FXML private TableColumn<CompromisoFGEM, CompromisoFGEM> colComEstadoFGEM;
    @FXML private TableColumn<CompromisoFGEM, String> colComVerificacionFGEM;

    public VisitasControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        indicadoresFGEM.getChildren().addAll(kpiVisitasFGEM, kpiCumplimientoFGEM, kpiCompromisosFGEM, kpiProximaFGEM);
        configurarTablaVisitasFGEM();
        configurarTablaCompromisosFGEM();
        boolean registraFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.VISITAS_REGISTRAR);
        for (Node nodoFGEM : List.of(btnNuevaFGEM, btnEditarFGEM, btnAnularFGEM, btnPendienteFGEM, btnIncumplidoFGEM, btnCumplidoFGEM)) {
            nodoFGEM.setVisible(registraFGEM);
            nodoFGEM.setManaged(registraFGEM);
        }
        boolean informesFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.INFORMES_GENERAR);
        btnInformeFGEM.setVisible(informesFGEM);
        btnInformeFGEM.setManaged(informesFGEM);
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
        btnNuevaFGEM.setDisable(!agroFGEM);
        if (!agroFGEM) {
            String mensajeFGEM = empresaFGEM == null ? "Selecciona una empresa en la barra superior."
                    : "Las visitas de inspección son para empresas del sector agropecuario.";
            tablaVisitasFGEM.getItems().clear();
            compromisosFGEM.clear();
            tablaVisitasFGEM.setPlaceholder(new Label(mensajeFGEM));
            tablaCompromisosFGEM.setPlaceholder(new Label(mensajeFGEM));
            detalleFGEM.getChildren().clear();
            for (TarjetaIndicadorFGEM kpiFGEM : List.of(kpiVisitasFGEM, kpiCumplimientoFGEM, kpiCompromisosFGEM, kpiProximaFGEM)) {
                kpiFGEM.actualizarFGEM(TarjetaIndicadorFGEM.SIN_VALOR_FGEM, "");
            }
            return;
        }
        Long seleccionadaFGEM = seleccionadaFGEM() == null ? null : seleccionadaFGEM().idFGEM();
        try {
            tablaVisitasFGEM.getItems().setAll(servicioFGEM().listarFGEM(chkAnuladasFGEM.isSelected()));
            compromisosFGEM.setAll(servicioFGEM().compromisosFGEM());
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        tablaVisitasFGEM.setPlaceholder(new Label("Aún no hay visitas registradas."));
        tablaCompromisosFGEM.setPlaceholder(new Label("No hay compromisos."));
        mostrarResumenFGEM();
        filtrarCompromisosFGEM();
        if (seleccionadaFGEM != null) {
            tablaVisitasFGEM.getItems().stream().filter(vFGEM -> vFGEM.idFGEM() == seleccionadaFGEM).findFirst()
                    .ifPresent(vFGEM -> tablaVisitasFGEM.getSelectionModel().select(vFGEM));
        }
        if (tablaVisitasFGEM.getSelectionModel().isEmpty() && !tablaVisitasFGEM.getItems().isEmpty()) {
            tablaVisitasFGEM.getSelectionModel().selectFirst();
        }
        mostrarDetalleFGEM();
        actualizarAccionesFGEM();
    }

    private void mostrarResumenFGEM() {
        List<VisitaFGEM> vigentesFGEM = tablaVisitasFGEM.getItems().stream().filter(vFGEM -> !vFGEM.anuladaFGEM()).toList();
        LocalDate hoyFGEM = LocalDate.now();
        kpiVisitasFGEM.actualizarFGEM(String.valueOf(vigentesFGEM.size()), vigentesFGEM.isEmpty() ? "Sin visitas"
                : "Última: " + FechasFGEM.paraMostrarFGEM(vigentesFGEM.get(0).fechaFGEM()));
        BigDecimal cumplimientoFGEM = vigentesFGEM.isEmpty() ? null : vigentesFGEM.get(0).cumplimientoFGEM();
        kpiCumplimientoFGEM.actualizarFGEM(MontosFGEM.porcentajeFGEM(cumplimientoFGEM),
                vigentesFGEM.isEmpty() ? "" : vigentesFGEM.get(0).conclusionFGEM().descripcionFGEM());
        long pendientesFGEM = compromisosFGEM.stream().filter(kFGEM -> kFGEM.estadoFGEM() == EstadoCompromisoFGEM.PENDIENTE).count();
        long vencidosFGEM = compromisosFGEM.stream().filter(kFGEM -> kFGEM.vencidoFGEM(hoyFGEM)).count();
        kpiCompromisosFGEM.actualizarFGEM(String.valueOf(pendientesFGEM), vencidosFGEM == 0 ? "Ninguno vencido"
                : vencidosFGEM + (vencidosFGEM == 1 ? " con el plazo vencido" : " con el plazo vencido"));
        LocalDate proximaFGEM = vigentesFGEM.isEmpty() ? null : vigentesFGEM.get(0).proximaVisitaFGEM();
        kpiProximaFGEM.actualizarFGEM(proximaFGEM == null ? TarjetaIndicadorFGEM.SIN_VALOR_FGEM : FechasFGEM.paraMostrarFGEM(proximaFGEM),
                proximaFGEM == null ? "No programada" : proximaFGEM.isBefore(hoyFGEM) ? "Ya pasó la fecha" : "Programada");
    }

    private void mostrarDetalleFGEM() {
        VisitaFGEM vFGEM = seleccionadaFGEM();
        detalleFGEM.getChildren().clear();
        if (vFGEM == null) {
            lblDetalleTituloFGEM.setText("Detalle de la visita");
            detalleFGEM.getChildren().add(textoFGEM("Selecciona una visita para ver su detalle.", "muted"));
            return;
        }
        lblDetalleTituloFGEM.setText(vFGEM.tipoFGEM().descripcionFGEM() + " · " + FechasFGEM.paraMostrarFGEM(vFGEM.fechaFGEM()));
        List<String> datosFGEM = new ArrayList<>();
        datosFGEM.add("Inspector: " + vFGEM.inspectorFGEM());
        if (vFGEM.atendidoPorFGEM() != null) {
            datosFGEM.add("Atendió: " + vFGEM.atendidoPorFGEM());
        }
        if (vFGEM.lugarFGEM() != null) {
            datosFGEM.add("Lugar: " + vFGEM.lugarFGEM());
        }
        if (vFGEM.coordenadasFGEM() != null) {
            datosFGEM.add("Coordenadas: " + vFGEM.coordenadasFGEM());
        }
        detalleFGEM.getChildren().add(textoFGEM(String.join("\n", datosFGEM), "hint"));
        if (vFGEM.anuladaFGEM()) {
            detalleFGEM.getChildren().add(textoFGEM("Anulada: " + vFGEM.motivoAnulacionFGEM(), "margen-negativo"));
        }
        Label conclusionFGEM = new Label(vFGEM.conclusionFGEM().descripcionFGEM() + (vFGEM.cumplimientoFGEM() == null ? ""
                : "  ·  cumple el " + MontosFGEM.porcentajeFGEM(vFGEM.cumplimientoFGEM())));
        conclusionFGEM.getStyleClass().addAll("info-value", claseSemaforoFGEM(vFGEM.conclusionFGEM().semaforoFGEM()));
        conclusionFGEM.setStyle("-fx-font-weight: bold;");
        conclusionFGEM.setWrapText(true);
        detalleFGEM.getChildren().add(conclusionFGEM);
        for (AspectoInspeccionFGEM aFGEM : AspectoInspeccionFGEM.values()) {
            ResultadoAspectoFGEM rFGEM = vFGEM.aspectosFGEM().get(aFGEM);
            if (rFGEM == null) {
                continue;
            }
            Label nombreFGEM = new Label(aFGEM.descripcionFGEM());
            nombreFGEM.getStyleClass().add("info-key");
            nombreFGEM.setWrapText(true);
            Label valorFGEM = new Label(rFGEM.descripcionFGEM());
            valorFGEM.getStyleClass().addAll("info-value", claseSemaforoFGEM(rFGEM.semaforoFGEM()));
            valorFGEM.setMinWidth(Region.USE_PREF_SIZE);
            Region espacioFGEM = new Region();
            HBox.setHgrow(espacioFGEM, Priority.ALWAYS);
            VBox bloqueFGEM = new VBox(2, new HBox(8, nombreFGEM, espacioFGEM, valorFGEM));
            if (vFGEM.notasFGEM().get(aFGEM) != null) {
                bloqueFGEM.getChildren().add(textoFGEM(vFGEM.notasFGEM().get(aFGEM), "muted"));
            }
            detalleFGEM.getChildren().add(bloqueFGEM);
        }
        if (vFGEM.observacionesFGEM() != null) {
            Label tituloFGEM = textoFGEM("Observaciones", "step-title");
            detalleFGEM.getChildren().addAll(tituloFGEM, textoFGEM(vFGEM.observacionesFGEM(), "muted"));
        }
    }

    // ------------------------------------------------------------------ visitas

    @FXML
    private void nuevaVisitaFGEM() {
        abrirFormularioFGEM(null);
    }

    @FXML
    private void editarVisitaFGEM() {
        VisitaFGEM vFGEM = seleccionadaFGEM();
        if (vFGEM != null && !vFGEM.anuladaFGEM()) {
            abrirFormularioFGEM(vFGEM);
        }
    }

    private void abrirFormularioFGEM(VisitaFGEM vFGEM) {
        List<CompromisoFGEM> pendientesFGEM = compromisosFGEM.stream()
                .filter(kFGEM -> kFGEM.estadoFGEM() == EstadoCompromisoFGEM.PENDIENTE && (vFGEM == null || kFGEM.visitaIdFGEM() != vFGEM.idFGEM()))
                .toList();
        FormularioVisitaFGEM formularioFGEM = new FormularioVisitaFGEM(vFGEM, pendientesFGEM);
        VisitaFGEM[] guardadaFGEM = new VisitaFGEM[1];
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), vFGEM == null ? "Nueva visita de inspección" : "Editar visita",
                null, formularioFGEM.contenidoFGEM(), vFGEM == null ? "Registrar visita" : "Guardar cambios", 760, () -> {
                    DatosVisitaFGEM datosFGEM = formularioFGEM.datosFGEM();
                    guardadaFGEM[0] = vFGEM == null ? servicioFGEM().registrarFGEM(datosFGEM) : servicioFGEM().actualizarFGEM(vFGEM.idFGEM(), datosFGEM);
                });
        if (!okFGEM) {
            return;
        }
        int verificadosFGEM = 0;
        for (Map.Entry<CompromisoFGEM, EstadoCompromisoFGEM> eFGEM : formularioFGEM.verificacionesFGEM().entrySet()) {
            try {
                servicioFGEM().verificarCompromisoFGEM(eFGEM.getKey().idFGEM(), eFGEM.getValue(), guardadaFGEM[0].fechaFGEM(), guardadaFGEM[0].idFGEM(), null);
                verificadosFGEM++;
            } catch (RuntimeException exFGEM) {
                NotificadorFGEM.errorFGEM("Compromiso «" + eFGEM.getKey().descripcionFGEM() + "»: " + ErroresFGEM.mensajeFGEM(exFGEM));
            }
        }
        cargarFGEM();
        tablaVisitasFGEM.getItems().stream().filter(xFGEM -> xFGEM.idFGEM() == guardadaFGEM[0].idFGEM()).findFirst()
                .ifPresent(xFGEM -> tablaVisitasFGEM.getSelectionModel().select(xFGEM));
        NotificadorFGEM.exitoFGEM((vFGEM == null ? "Visita registrada." : "Visita actualizada.")
                + (verificadosFGEM == 0 ? "" : " " + verificadosFGEM + (verificadosFGEM == 1 ? " compromiso verificado." : " compromisos verificados.")));
    }

    @FXML
    private void anularVisitaFGEM() {
        VisitaFGEM vFGEM = seleccionadaFGEM();
        if (vFGEM == null || vFGEM.anuladaFGEM()) {
            return;
        }
        TextField motivoFGEM = new TextField();
        motivoFGEM.setPromptText("Ej.: registrada en el productor equivocado");
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Motivo *", motivoFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Anular visita",
                "La visita del " + FechasFGEM.paraMostrarFGEM(vFGEM.fechaFGEM()) + " y sus compromisos dejarán de mostrarse.",
                grillaFGEM, "Anular", () -> servicioFGEM().anularFGEM(vFGEM.idFGEM(), motivoFGEM.getText()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Visita anulada.");
        }
    }

    // ------------------------------------------------------------------ fotos

    @FXML
    private void verFotosFGEM() {
        VisitaFGEM vFGEM = seleccionadaFGEM();
        if (vFGEM == null) {
            return;
        }
        boolean registraFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.VISITAS_REGISTRAR) && !vFGEM.anuladaFGEM();
        FlowPane galeriaFGEM = new FlowPane(12, 12);
        galeriaFGEM.setPrefWrapLength(600);
        Runnable[] recargarFGEM = new Runnable[1];
        recargarFGEM[0] = () -> {
            galeriaFGEM.getChildren().clear();
            List<FotoVisitaFGEM> fotosFGEM;
            try {
                fotosFGEM = servicioFGEM().fotosFGEM(vFGEM.idFGEM());
            } catch (RuntimeException eFGEM) {
                NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
                return;
            }
            if (fotosFGEM.isEmpty()) {
                galeriaFGEM.getChildren().add(textoFGEM("Aún no hay fotos de esta visita.", "muted"));
            }
            for (FotoVisitaFGEM fFGEM : fotosFGEM) {
                Path rutaFGEM = servicioFGEM().archivoFGEM(fFGEM);
                ImageView vistaFGEM = new ImageView();
                if (Files.exists(rutaFGEM)) {
                    vistaFGEM.setImage(new Image(rutaFGEM.toUri().toString(), 180, 135, true, true, true));
                }
                vistaFGEM.setFitWidth(180);
                vistaFGEM.setFitHeight(135);
                vistaFGEM.setPreserveRatio(true);
                VBox tarjetaFGEM = new VBox(6, vistaFGEM, textoFGEM(Objects.requireNonNullElse(fFGEM.descripcionFGEM(), "Sin descripción"), "hint"));
                tarjetaFGEM.setPrefWidth(190);
                tarjetaFGEM.setAlignment(Pos.TOP_CENTER);
                tarjetaFGEM.getStyleClass().add("card");
                if (registraFGEM) {
                    Button quitarFGEM = new Button("Quitar");
                    quitarFGEM.getStyleClass().add("button-secondary");
                    quitarFGEM.setOnAction(eFGEM -> {
                        if (DialogosFGEM.confirmarFGEM(galeriaFGEM.getScene().getWindow(), "Quitar foto",
                                "La foto se eliminará de la visita y de la carpeta de datos.")) {
                            try {
                                servicioFGEM().eliminarFotoFGEM(fFGEM.idFGEM());
                                recargarFGEM[0].run();
                            } catch (RuntimeException exFGEM) {
                                NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(exFGEM));
                            }
                        }
                    });
                    tarjetaFGEM.getChildren().add(quitarFGEM);
                }
                galeriaFGEM.getChildren().add(tarjetaFGEM);
            }
        };
        recargarFGEM[0].run();
        ScrollPane desplazableFGEM = new ScrollPane(galeriaFGEM);
        desplazableFGEM.setFitToWidth(true);
        desplazableFGEM.setPrefViewportHeight(380);
        desplazableFGEM.getStyleClass().add("dialog-scroll");
        VBox contenidoFGEM = new VBox(10, desplazableFGEM);
        if (registraFGEM) {
            TextField descripcionFGEM = new TextField();
            descripcionFGEM.setPromptText("Descripción (opcional). Ej.: cobertizo de alpacas");
            Button agregarFGEM = new Button("Agregar fotos…");
            agregarFGEM.getStyleClass().add("button-primary");
            agregarFGEM.setOnAction(eFGEM -> {
                FileChooser selectorFGEM = new FileChooser();
                selectorFGEM.setTitle("Elegir fotos de la visita");
                selectorFGEM.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fotos (JPG, PNG)",
                        "*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG"));
                List<File> archivosFGEM = selectorFGEM.showOpenMultipleDialog(galeriaFGEM.getScene().getWindow());
                if (archivosFGEM == null) {
                    return;
                }
                int agregadasFGEM = 0;
                for (File archivoFGEM : archivosFGEM) {
                    try {
                        servicioFGEM().agregarFotoFGEM(vFGEM.idFGEM(), archivoFGEM.toPath(), descripcionFGEM.getText());
                        agregadasFGEM++;
                    } catch (RuntimeException exFGEM) {
                        NotificadorFGEM.errorFGEM(archivoFGEM.getName() + ": " + ErroresFGEM.mensajeFGEM(exFGEM));
                    }
                }
                descripcionFGEM.clear();
                recargarFGEM[0].run();
                if (agregadasFGEM > 0) {
                    NotificadorFGEM.exitoFGEM(agregadasFGEM + (agregadasFGEM == 1 ? " foto agregada." : " fotos agregadas."));
                }
            });
            HBox filaFGEM = new HBox(10, descripcionFGEM, agregarFGEM);
            HBox.setHgrow(descripcionFGEM, Priority.ALWAYS);
            contenidoFGEM.getChildren().add(filaFGEM);
        }
        DialogosFGEM.informacionFGEM(ventanaFGEM(), "Fotos de la visita del " + FechasFGEM.paraMostrarFGEM(vFGEM.fechaFGEM()),
                "Las fotos se copian a la carpeta de datos de la aplicación. En el informe técnico se incluyen las "
                        + "6 primeras.", contenidoFGEM);
    }

    // ------------------------------------------------------------------ informe técnico

    @FXML
    private void generarInformeFGEM() {
        VisitaFGEM vFGEM = seleccionadaFGEM();
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        if (vFGEM == null || vFGEM.anuladaFGEM() || empresaFGEM == null) {
            return;
        }
        FileChooser selectorFGEM = new FileChooser();
        selectorFGEM.setTitle("Guardar informe técnico");
        selectorFGEM.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
        selectorFGEM.setInitialFileName(InformeTecnicoServiceFGEM.nombreSugeridoFGEM(empresaFGEM, vFGEM));
        String guardadaFGEM = contextoFGEM.configuracionFGEM().obtenerFGEM(CLAVE_CARPETA_FGEM, null);
        for (Path carpetaFGEM : new Path[]{guardadaFGEM == null ? null : Path.of(guardadaFGEM),
                Path.of(System.getProperty("user.home"), "Documents"), Path.of(System.getProperty("user.home"))}) {
            if (carpetaFGEM != null && Files.isDirectory(carpetaFGEM)) {
                selectorFGEM.setInitialDirectory(carpetaFGEM.toFile());
                break;
            }
        }
        File archivoFGEM = selectorFGEM.showSaveDialog(ventanaFGEM());
        if (archivoFGEM == null) {
            return;
        }
        btnInformeFGEM.setDisable(true);
        progresoFGEM.setVisible(true);
        TareasFGEM.ejecutarFGEM(() -> contextoFGEM.informeTecnicoFGEM().generarFGEM(vFGEM.idFGEM(), archivoFGEM.toPath()),
                generadoFGEM -> {
                    btnInformeFGEM.setDisable(false);
                    progresoFGEM.setVisible(false);
                    try {
                        contextoFGEM.configuracionFGEM().guardarFGEM(CLAVE_CARPETA_FGEM, generadoFGEM.getParent().toString());
                    } catch (RuntimeException eFGEM) {
                        // No poder recordar la carpeta no impide seguir.
                    }
                    if (DialogosFGEM.confirmarFGEM(ventanaFGEM(), "Informe generado",
                            "Se guardó «" + generadoFGEM.getFileName() + "».\n\n¿Quieres abrirlo ahora?")) {
                        abrirFGEM(generadoFGEM);
                    }
                },
                errorFGEM -> {
                    btnInformeFGEM.setDisable(false);
                    progresoFGEM.setVisible(false);
                    NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(errorFGEM));
                });
    }

    private void abrirFGEM(Path rutaFGEM) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            NotificadorFGEM.advertenciaFGEM("El sistema no permite abrir archivos desde la aplicación. Ruta: " + rutaFGEM);
            return;
        }
        TareasFGEM.ejecutarFGEM(() -> {
            Desktop.getDesktop().open(rutaFGEM.toFile());
            return null;
        }, okFGEM -> {
        }, errorFGEM -> NotificadorFGEM.errorFGEM("No se pudo abrir el archivo: " + ErroresFGEM.mensajeFGEM(errorFGEM)));
    }

    // ------------------------------------------------------------------ compromisos

    @FXML
    private void filtrarCompromisosFGEM() {
        boolean soloFGEM = chkSoloPendientesFGEM.isSelected();
        compromisosFiltradosFGEM.setPredicate(kFGEM -> !soloFGEM || kFGEM.estadoFGEM() == EstadoCompromisoFGEM.PENDIENTE);
        actualizarAccionesFGEM();
    }

    @FXML
    private void marcarCumplidoFGEM() {
        verificarFGEM(EstadoCompromisoFGEM.CUMPLIDO);
    }

    @FXML
    private void marcarIncumplidoFGEM() {
        verificarFGEM(EstadoCompromisoFGEM.INCUMPLIDO);
    }

    @FXML
    private void marcarPendienteFGEM() {
        verificarFGEM(EstadoCompromisoFGEM.PENDIENTE);
    }

    private void verificarFGEM(EstadoCompromisoFGEM estadoFGEM) {
        CompromisoFGEM kFGEM = tablaCompromisosFGEM.getSelectionModel().getSelectedItem();
        if (kFGEM == null) {
            return;
        }
        if (estadoFGEM == EstadoCompromisoFGEM.PENDIENTE) {
            try {
                servicioFGEM().verificarCompromisoFGEM(kFGEM.idFGEM(), estadoFGEM, null, null, null);
                cargarFGEM();
            } catch (RuntimeException eFGEM) {
                NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            }
            return;
        }
        DatePicker fechaFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        TextField observacionFGEM = new TextField();
        observacionFGEM.setPromptText("Opcional. Ej.: construyó el cobertizo");
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Fecha *", fechaFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Observación", observacionFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Marcar como «" + estadoFGEM.descripcionFGEM().toLowerCase() + "»",
                kFGEM.descripcionFGEM(), grillaFGEM, "Guardar",
                () -> servicioFGEM().verificarCompromisoFGEM(kFGEM.idFGEM(), estadoFGEM, CamposFechaFGEM.leerFGEM(fechaFGEM, "verificación"), null,
                        observacionFGEM.getText()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Compromiso actualizado.");
        }
    }

    // ------------------------------------------------------------------ tablas

    private void configurarTablaVisitasFGEM() {
        colFechaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().fechaFGEM()));
        colFechaFGEM.setCellFactory(colFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fechaFGEM, boolean vacioFGEM) {
                super.updateItem(fechaFGEM, vacioFGEM);
                setText(vacioFGEM ? null : FechasFGEM.paraMostrarFGEM(fechaFGEM));
            }
        });
        colTipoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().tipoFGEM().descripcionFGEM()
                + (cFGEM.getValue().anuladaFGEM() ? "  (anulada)" : "")));
        colInspectorFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().inspectorFGEM()));
        colCumplimientoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(MontosFGEM.porcentajeFGEM(cFGEM.getValue().cumplimientoFGEM())));
        colConclusionFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colConclusionFGEM.setCellFactory(colFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(VisitaFGEM vFGEM, boolean vacioFGEM) {
                super.updateItem(vFGEM, vacioFGEM);
                getStyleClass().removeAll("margen-ok", "margen-bajo", "margen-negativo");
                if (vacioFGEM || vFGEM == null) {
                    setText(null);
                    return;
                }
                setText(vFGEM.conclusionFGEM().descripcionFGEM());
                getStyleClass().add(switch (vFGEM.conclusionFGEM()) {
                    case FAVORABLE -> "margen-ok";
                    case CON_OBSERVACIONES -> "margen-bajo";
                    case DESFAVORABLE -> "margen-negativo";
                });
            }
        });
        tablaVisitasFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> {
            mostrarDetalleFGEM();
            actualizarAccionesFGEM();
        });
        tablaVisitasFGEM.setRowFactory(tFGEM -> {
            TableRow<VisitaFGEM> filaFGEM = new TableRow<>() {
                @Override
                protected void updateItem(VisitaFGEM vFGEM, boolean vacioFGEM) {
                    super.updateItem(vFGEM, vacioFGEM);
                    pseudoClassStateChanged(ARCHIVADO_FGEM, !vacioFGEM && vFGEM != null && vFGEM.anuladaFGEM());
                }
            };
            filaFGEM.setOnMouseClicked(eFGEM -> {
                if (eFGEM.getButton() == MouseButton.PRIMARY && eFGEM.getClickCount() == 2 && !filaFGEM.isEmpty()
                        && contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.VISITAS_REGISTRAR)) {
                    editarVisitaFGEM();
                }
            });
            return filaFGEM;
        });
    }

    private void configurarTablaCompromisosFGEM() {
        LocalDate hoyFGEM = LocalDate.now();
        colComDescripcionFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().descripcionFGEM()));
        colComAcordadoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(FechasFGEM.paraMostrarFGEM(cFGEM.getValue().fechaVisitaFGEM())));
        colComPlazoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(FechasFGEM.paraMostrarFGEM(cFGEM.getValue().plazoFGEM())));
        colComEstadoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colComEstadoFGEM.setCellFactory(colFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(CompromisoFGEM kFGEM, boolean vacioFGEM) {
                super.updateItem(kFGEM, vacioFGEM);
                getStyleClass().removeAll("margen-ok", "margen-bajo", "margen-negativo");
                if (vacioFGEM || kFGEM == null) {
                    setText(null);
                    return;
                }
                boolean vencidoFGEM = kFGEM.vencidoFGEM(hoyFGEM);
                setText(kFGEM.estadoFGEM().descripcionFGEM() + (vencidoFGEM ? " (vencido)" : ""));
                getStyleClass().add(switch (kFGEM.estadoFGEM()) {
                    case CUMPLIDO -> "margen-ok";
                    case INCUMPLIDO -> "margen-negativo";
                    case PENDIENTE -> vencidoFGEM ? "margen-negativo" : "margen-bajo";
                });
            }
        });
        colComVerificacionFGEM.setCellValueFactory(cFGEM -> {
            CompromisoFGEM kFGEM = cFGEM.getValue();
            if (kFGEM.estadoFGEM() == EstadoCompromisoFGEM.PENDIENTE) {
                return new ReadOnlyStringWrapper("");
            }
            return new ReadOnlyStringWrapper(FechasFGEM.paraMostrarFGEM(kFGEM.fechaVerificacionFGEM())
                    + (kFGEM.verificadoPorFGEM() == null ? "" : " · " + kFGEM.verificadoPorFGEM())
                    + (kFGEM.observacionFGEM() == null ? "" : " · " + kFGEM.observacionFGEM()));
        });
        tablaCompromisosFGEM.setItems(compromisosFiltradosFGEM);
        tablaCompromisosFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarAccionesFGEM());
    }

    private void actualizarAccionesFGEM() {
        VisitaFGEM vFGEM = seleccionadaFGEM();
        boolean vigenteFGEM = vFGEM != null && !vFGEM.anuladaFGEM();
        btnInformeFGEM.setDisable(!vigenteFGEM);
        btnFotosFGEM.setDisable(vFGEM == null);
        btnEditarFGEM.setDisable(!vigenteFGEM);
        btnAnularFGEM.setDisable(!vigenteFGEM);
        CompromisoFGEM kFGEM = tablaCompromisosFGEM.getSelectionModel().getSelectedItem();
        btnCumplidoFGEM.setDisable(kFGEM == null || kFGEM.estadoFGEM() == EstadoCompromisoFGEM.CUMPLIDO);
        btnIncumplidoFGEM.setDisable(kFGEM == null || kFGEM.estadoFGEM() == EstadoCompromisoFGEM.INCUMPLIDO);
        btnPendienteFGEM.setDisable(kFGEM == null || kFGEM.estadoFGEM() == EstadoCompromisoFGEM.PENDIENTE);
    }

    // ------------------------------------------------------------------ utilidades

    private VisitaFGEM seleccionadaFGEM() {
        return tablaVisitasFGEM.getSelectionModel().getSelectedItem();
    }

    private VisitaServiceFGEM servicioFGEM() {
        return contextoFGEM.visitasFGEM();
    }

    private Window ventanaFGEM() {
        return tablaVisitasFGEM.getScene().getWindow();
    }

    private static Label textoFGEM(String textoFGEM, String claseFGEM) {
        Label lFGEM = new Label(textoFGEM);
        lFGEM.getStyleClass().add(claseFGEM);
        lFGEM.setWrapText(true);
        lFGEM.setMinHeight(Region.USE_PREF_SIZE);
        return lFGEM;
    }

    private static String claseSemaforoFGEM(SemaforoFGEM sFGEM) {
        return switch (sFGEM) {
            case VERDE -> "semaforo-verde";
            case AMARILLO -> "semaforo-amarillo";
            case ROJO -> "semaforo-rojo";
            default -> "semaforo-sin-datos";
        };
    }

    private static Double decimalFGEM(String textoFGEM, String campoFGEM) {
        if (textoFGEM == null || textoFGEM.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(textoFGEM.strip().replace(',', '.'));
        } catch (NumberFormatException eFGEM) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" debe ser un número en grados decimales, "
                    + "por ejemplo -12.0651.");
        }
    }

    /**
     * Formulario de la visita: datos generales, verificación de cada aspecto, compromisos pendientes
     * de visitas anteriores y nuevos compromisos. Propone la conclusión según el cumplimiento.
     */
    private final class FormularioVisitaFGEM {
        private final DatePicker dpFechaFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        private final ComboBox<TipoVisitaFGEM> cmbTipoFGEM = new ComboBox<>(FXCollections.observableArrayList(TipoVisitaFGEM.values()));
        private final TextField txtInspectorFGEM = new TextField();
        private final TextField txtAtendidoFGEM = new TextField();
        private final TextField txtLugarFGEM = new TextField();
        private final TextField txtLatitudFGEM = new TextField();
        private final TextField txtLongitudFGEM = new TextField();
        private final Map<AspectoInspeccionFGEM, ComboBox<ResultadoAspectoFGEM>> resultadosFGEM = new EnumMap<>(AspectoInspeccionFGEM.class);
        private final Map<AspectoInspeccionFGEM, TextField> notasFGEM = new EnumMap<>(AspectoInspeccionFGEM.class);
        private final Label lblCumplimientoFGEM = new Label();
        private final ComboBox<ConclusionVisitaFGEM> cmbConclusionFGEM =
                new ComboBox<>(FXCollections.observableArrayList(ConclusionVisitaFGEM.values()));
        private boolean conclusionElegidaFGEM;
        private final TextArea txtObservacionesFGEM = new TextArea();
        private final DatePicker dpProximaFGEM = CamposFechaFGEM.nuevoFGEM(null);
        private final Map<CompromisoFGEM, ComboBox<EstadoCompromisoFGEM>> pendientesFGEM = new LinkedHashMap<>();
        private final VBox nuevosFGEM = new VBox(8);

        FormularioVisitaFGEM(VisitaFGEM vFGEM, List<CompromisoFGEM> pendientesPreviosFGEM) {
            cmbTipoFGEM.setValue(TipoVisitaFGEM.SUPERVISION);
            txtInspectorFGEM.setText(contextoFGEM.sesionFGEM().usuarioFGEM().nombreCompletoFGEM());
            txtAtendidoFGEM.setPromptText("Productor o familiar que atendió");
            txtLugarFGEM.setPromptText("Ej.: Anexo Chacapampa, distrito de Huayucachi");
            txtLatitudFGEM.setPromptText("Latitud. Ej.: -12.1352");
            txtLongitudFGEM.setPromptText("Longitud. Ej.: -75.2311");
            txtObservacionesFGEM.setPromptText("Situación general, problemas encontrados, recomendaciones…");
            txtObservacionesFGEM.setPrefRowCount(4);
            txtObservacionesFGEM.setWrapText(true);
            for (AspectoInspeccionFGEM aFGEM : AspectoInspeccionFGEM.values()) {
                ComboBox<ResultadoAspectoFGEM> comboFGEM = new ComboBox<>(FXCollections.observableArrayList(ResultadoAspectoFGEM.values()));
                comboFGEM.setPromptText("Sin evaluar");
                comboFGEM.setPrefWidth(160);
                comboFGEM.valueProperty().addListener((oFGEM, xFGEM, yFGEM) -> actualizarCumplimientoFGEM());
                resultadosFGEM.put(aFGEM, comboFGEM);
                TextField notaFGEM = new TextField();
                notaFGEM.setPromptText("Observación (opcional)");
                notasFGEM.put(aFGEM, notaFGEM);
            }
            cmbConclusionFGEM.setValue(ConclusionVisitaFGEM.FAVORABLE);
            cmbConclusionFGEM.setOnAction(eFGEM -> conclusionElegidaFGEM = true);
            for (CompromisoFGEM kFGEM : pendientesPreviosFGEM) {
                ComboBox<EstadoCompromisoFGEM> estadoFGEM = new ComboBox<>(FXCollections.observableArrayList(EstadoCompromisoFGEM.values()));
                estadoFGEM.setValue(EstadoCompromisoFGEM.PENDIENTE);
                pendientesFGEM.put(kFGEM, estadoFGEM);
            }
            if (vFGEM != null) {
                dpFechaFGEM.setValue(vFGEM.fechaFGEM());
                cmbTipoFGEM.setValue(vFGEM.tipoFGEM());
                txtInspectorFGEM.setText(vFGEM.inspectorFGEM());
                txtAtendidoFGEM.setText(Objects.requireNonNullElse(vFGEM.atendidoPorFGEM(), ""));
                txtLugarFGEM.setText(Objects.requireNonNullElse(vFGEM.lugarFGEM(), ""));
                txtLatitudFGEM.setText(vFGEM.latitudFGEM() == null ? "" : String.valueOf(vFGEM.latitudFGEM()));
                txtLongitudFGEM.setText(vFGEM.longitudFGEM() == null ? "" : String.valueOf(vFGEM.longitudFGEM()));
                vFGEM.aspectosFGEM().forEach((aFGEM, rFGEM) -> resultadosFGEM.get(aFGEM).setValue(rFGEM));
                vFGEM.notasFGEM().forEach((aFGEM, nFGEM) -> notasFGEM.get(aFGEM).setText(nFGEM));
                txtObservacionesFGEM.setText(Objects.requireNonNullElse(vFGEM.observacionesFGEM(), ""));
                dpProximaFGEM.setValue(vFGEM.proximaVisitaFGEM());
                cmbConclusionFGEM.setValue(vFGEM.conclusionFGEM());
                conclusionElegidaFGEM = true;
            }
            actualizarCumplimientoFGEM();
        }

        private void actualizarCumplimientoFGEM() {
            List<ResultadoAspectoFGEM> elegidosFGEM = resultadosFGEM.values().stream().map(ComboBox::getValue)
                    .filter(Objects::nonNull).toList();
            BigDecimal cumplimientoFGEM = VisitaFGEM.cumplimientoFGEM(elegidosFGEM);
            lblCumplimientoFGEM.setText(cumplimientoFGEM == null ? "Marca el resultado de cada aspecto que verifiques."
                    : "Cumplimiento: " + MontosFGEM.porcentajeFGEM(cumplimientoFGEM) + " de " + elegidosFGEM.stream()
                    .filter(rFGEM -> rFGEM != ResultadoAspectoFGEM.NO_APLICA).count() + " aspectos aplicables. Conclusión sugerida: "
                    + ConclusionVisitaFGEM.sugeridaFGEM(cumplimientoFGEM).descripcionFGEM().toLowerCase() + ".");
            if (!conclusionElegidaFGEM) {
                cmbConclusionFGEM.setValue(ConclusionVisitaFGEM.sugeridaFGEM(cumplimientoFGEM));
            }
        }

        Node contenidoFGEM() {
            GridPane datosFGEM = FormulariosFGEM.grillaFGEM();
            FormulariosFGEM.filaFGEM(datosFGEM, "Fecha *", dpFechaFGEM);
            FormulariosFGEM.filaFGEM(datosFGEM, "Tipo de visita *", cmbTipoFGEM);
            FormulariosFGEM.filaFGEM(datosFGEM, "Inspector *", txtInspectorFGEM);
            FormulariosFGEM.filaFGEM(datosFGEM, "Atendido por", txtAtendidoFGEM);
            FormulariosFGEM.filaFGEM(datosFGEM, "Lugar", txtLugarFGEM);
            HBox coordenadasFGEM = new HBox(8, txtLatitudFGEM, txtLongitudFGEM);
            HBox.setHgrow(txtLatitudFGEM, Priority.ALWAYS);
            HBox.setHgrow(txtLongitudFGEM, Priority.ALWAYS);
            FormulariosFGEM.filaFGEM(datosFGEM, "Coordenadas GPS", coordenadasFGEM);
            FormulariosFGEM.notaFGEM(datosFGEM, "En grados decimales, como las muestra el celular (Google Maps: mantener presionado el punto).");

            GridPane verificacionFGEM = new GridPane();
            verificacionFGEM.setHgap(10);
            verificacionFGEM.setVgap(6);
            ColumnConstraints c1FGEM = new ColumnConstraints();
            c1FGEM.setMinWidth(230);
            c1FGEM.setPrefWidth(230);
            ColumnConstraints c2FGEM = new ColumnConstraints();
            c2FGEM.setMinWidth(160);
            ColumnConstraints c3FGEM = new ColumnConstraints();
            c3FGEM.setHgrow(Priority.ALWAYS);
            verificacionFGEM.getColumnConstraints().setAll(c1FGEM, c2FGEM, c3FGEM);
            int filaFGEM = 0;
            for (AspectoInspeccionFGEM.AreaFGEM areaFGEM : AspectoInspeccionFGEM.AreaFGEM.values()) {
                Label tituloFGEM = textoFGEM(areaFGEM.descripcionFGEM(), "step-title");
                verificacionFGEM.add(tituloFGEM, 0, filaFGEM++, 3, 1);
                for (AspectoInspeccionFGEM aFGEM : AspectoInspeccionFGEM.values()) {
                    if (aFGEM.areaFGEM() != areaFGEM) {
                        continue;
                    }
                    Label nombreFGEM = new Label(aFGEM.descripcionFGEM());
                    nombreFGEM.getStyleClass().add("form-label");
                    nombreFGEM.setTooltip(new Tooltip(aFGEM.guiaFGEM()));
                    nombreFGEM.setWrapText(true);
                    verificacionFGEM.addRow(filaFGEM++, nombreFGEM, resultadosFGEM.get(aFGEM), notasFGEM.get(aFGEM));
                }
            }
            lblCumplimientoFGEM.getStyleClass().add("hint");
            lblCumplimientoFGEM.setWrapText(true);

            VBox compromisosBoxFGEM = new VBox(8);
            if (!pendientesFGEM.isEmpty()) {
                compromisosBoxFGEM.getChildren().add(textoFGEM("Compromisos pendientes de visitas anteriores: indica si se cumplieron.",
                        "hint"));
                pendientesFGEM.forEach((kFGEM, comboFGEM) -> {
                    Label descFGEM = new Label(kFGEM.descripcionFGEM() + "  (acordado el " + FechasFGEM.paraMostrarFGEM(kFGEM.fechaVisitaFGEM())
                            + (kFGEM.plazoFGEM() == null ? "" : ", plazo " + FechasFGEM.paraMostrarFGEM(kFGEM.plazoFGEM())) + ")");
                    descFGEM.setWrapText(true);
                    descFGEM.getStyleClass().add("info-value");
                    HBox.setHgrow(descFGEM, Priority.ALWAYS);
                    descFGEM.setMaxWidth(Double.MAX_VALUE);
                    compromisosBoxFGEM.getChildren().add(new HBox(10, descFGEM, comboFGEM));
                });
            }
            Button agregarFGEM = new Button("Agregar compromiso");
            agregarFGEM.getStyleClass().add("button-secondary");
            agregarFGEM.setOnAction(eFGEM -> agregarFilaCompromisoFGEM());
            compromisosBoxFGEM.getChildren().addAll(textoFGEM("Nuevos compromisos o recomendaciones acordados en esta visita:", "hint"),
                    nuevosFGEM, agregarFGEM);

            GridPane cierreFGEM = FormulariosFGEM.grillaFGEM();
            FormulariosFGEM.filaFGEM(cierreFGEM, "Observaciones", txtObservacionesFGEM);
            FormulariosFGEM.filaFGEM(cierreFGEM, "Conclusión *", cmbConclusionFGEM);
            FormulariosFGEM.filaFGEM(cierreFGEM, "Próxima visita", dpProximaFGEM);

            VBox todoFGEM = new VBox(14,
                    textoFGEM("Datos de la visita", "card-title"), datosFGEM,
                    textoFGEM("Verificación en campo", "card-title"), lblCumplimientoFGEM, verificacionFGEM,
                    textoFGEM("Compromisos", "card-title"), compromisosBoxFGEM,
                    textoFGEM("Conclusión", "card-title"), cierreFGEM);
            todoFGEM.setStyle("-fx-padding: 0 12 0 0;");
            ScrollPane desplazableFGEM = new ScrollPane(todoFGEM);
            desplazableFGEM.setFitToWidth(true);
            desplazableFGEM.setPrefViewportHeight(520);
            desplazableFGEM.getStyleClass().add("dialog-scroll");
            return desplazableFGEM;
        }

        private void agregarFilaCompromisoFGEM() {
            TextField descripcionFGEM = new TextField();
            descripcionFGEM.setPromptText("Ej.: Construir cobertizo para crías antes de las heladas");
            DatePicker plazoFGEM = CamposFechaFGEM.nuevoFGEM(null);
            plazoFGEM.setPromptText("Plazo");
            plazoFGEM.setPrefWidth(140);
            Button quitarFGEM = new Button("Quitar");
            quitarFGEM.getStyleClass().add("button-secondary");
            HBox filaFGEM = new HBox(8, descripcionFGEM, plazoFGEM, quitarFGEM);
            HBox.setHgrow(descripcionFGEM, Priority.ALWAYS);
            filaFGEM.setUserData(new Object[]{descripcionFGEM, plazoFGEM});
            quitarFGEM.setOnAction(eFGEM -> nuevosFGEM.getChildren().remove(filaFGEM));
            nuevosFGEM.getChildren().add(filaFGEM);
            descripcionFGEM.requestFocus();
        }

        DatosVisitaFGEM datosFGEM() {
            LocalDate fechaFGEM = CamposFechaFGEM.leerFGEM(dpFechaFGEM, "la visita");
            LocalDate proximaFGEM = CamposFechaFGEM.leerFGEM(dpProximaFGEM, "la próxima visita");
            Map<AspectoInspeccionFGEM, ResultadoAspectoFGEM> aspectosFGEM = new EnumMap<>(AspectoInspeccionFGEM.class);
            Map<AspectoInspeccionFGEM, String> textosFGEM = new EnumMap<>(AspectoInspeccionFGEM.class);
            resultadosFGEM.forEach((aFGEM, comboFGEM) -> {
                if (comboFGEM.getValue() != null) {
                    aspectosFGEM.put(aFGEM, comboFGEM.getValue());
                    textosFGEM.put(aFGEM, notasFGEM.get(aFGEM).getText());
                }
            });
            List<DatosVisitaFGEM.NuevoCompromisoFGEM> compromisosNuevosFGEM = new ArrayList<>();
            for (Node nFGEM : nuevosFGEM.getChildren()) {
                Object[] camposFGEM = (Object[]) nFGEM.getUserData();
                TextField descripcionFGEM = (TextField) camposFGEM[0];
                if (descripcionFGEM.getText() == null || descripcionFGEM.getText().isBlank()) {
                    continue;
                }
                compromisosNuevosFGEM.add(new DatosVisitaFGEM.NuevoCompromisoFGEM(descripcionFGEM.getText(),
                        CamposFechaFGEM.leerFGEM((DatePicker) camposFGEM[1], "plazo del compromiso")));
            }
            return new DatosVisitaFGEM(fechaFGEM, cmbTipoFGEM.getValue(), txtInspectorFGEM.getText(), txtLugarFGEM.getText(),
                    decimalFGEM(txtLatitudFGEM.getText(), "Latitud"), decimalFGEM(txtLongitudFGEM.getText(), "Longitud"),
                    txtAtendidoFGEM.getText(), txtObservacionesFGEM.getText(), cmbConclusionFGEM.getValue(), proximaFGEM, aspectosFGEM, textosFGEM,
                    compromisosNuevosFGEM);
        }

        /** Compromisos previos a los que se les cambió el estado. */
        Map<CompromisoFGEM, EstadoCompromisoFGEM> verificacionesFGEM() {
            Map<CompromisoFGEM, EstadoCompromisoFGEM> cambiosFGEM = new LinkedHashMap<>();
            pendientesFGEM.forEach((kFGEM, comboFGEM) -> {
                if (comboFGEM.getValue() != null && comboFGEM.getValue() != EstadoCompromisoFGEM.PENDIENTE) {
                    cambiosFGEM.put(kFGEM, comboFGEM.getValue());
                }
            });
            return cambiosFGEM;
        }
    }
}
