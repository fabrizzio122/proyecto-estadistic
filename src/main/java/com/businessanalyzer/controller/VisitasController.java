package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.diagnostico.Semaforo;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.AspectoInspeccion;
import com.businessanalyzer.model.Compromiso;
import com.businessanalyzer.model.ConclusionVisita;
import com.businessanalyzer.model.DatosVisita;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.EstadoCompromiso;
import com.businessanalyzer.model.FotoVisita;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.ResultadoAspecto;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.TipoVisita;
import com.businessanalyzer.model.Visita;
import com.businessanalyzer.service.InformeTecnicoService;
import com.businessanalyzer.service.VisitaService;
import com.businessanalyzer.ui.CamposFecha;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Formularios;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.Tareas;
import com.businessanalyzer.ui.TarjetaIndicador;
import com.businessanalyzer.ui.VistaModulo;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;
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
public class VisitasController implements VistaModulo {

    private static final PseudoClass ARCHIVADO = PseudoClass.getPseudoClass("archivado");
    private static final String CLAVE_CARPETA = "informes.carpeta";

    private final AppContext contexto;
    private final ObservableList<Compromiso> compromisos = FXCollections.observableArrayList();
    private final FilteredList<Compromiso> compromisosFiltrados = new FilteredList<>(compromisos);

    private final TarjetaIndicador kpiVisitas = new TarjetaIndicador("Visitas", "fth-clipboard");
    private final TarjetaIndicador kpiCumplimiento = new TarjetaIndicador("Cumplimiento (última visita)", "fth-check-circle");
    private final TarjetaIndicador kpiCompromisos = new TarjetaIndicador("Compromisos pendientes", "fth-flag");
    private final TarjetaIndicador kpiProxima = new TarjetaIndicador("Próxima visita", "fth-calendar");

    @FXML private HBox indicadores;
    @FXML private TabPane pestanas;
    @FXML private CheckBox chkAnuladas;
    @FXML private ProgressIndicator progreso;
    @FXML private Button btnInforme;
    @FXML private Button btnFotos;
    @FXML private Button btnEditar;
    @FXML private Button btnAnular;
    @FXML private Button btnNueva;
    @FXML private TableView<Visita> tablaVisitas;
    @FXML private TableColumn<Visita, LocalDate> colFecha;
    @FXML private TableColumn<Visita, String> colTipo;
    @FXML private TableColumn<Visita, String> colInspector;
    @FXML private TableColumn<Visita, String> colCumplimiento;
    @FXML private TableColumn<Visita, Visita> colConclusion;
    @FXML private Label lblDetalleTitulo;
    @FXML private VBox detalle;

    @FXML private CheckBox chkSoloPendientes;
    @FXML private Button btnPendiente;
    @FXML private Button btnIncumplido;
    @FXML private Button btnCumplido;
    @FXML private TableView<Compromiso> tablaCompromisos;
    @FXML private TableColumn<Compromiso, String> colComDescripcion;
    @FXML private TableColumn<Compromiso, String> colComAcordado;
    @FXML private TableColumn<Compromiso, String> colComPlazo;
    @FXML private TableColumn<Compromiso, Compromiso> colComEstado;
    @FXML private TableColumn<Compromiso, String> colComVerificacion;

    public VisitasController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        indicadores.getChildren().addAll(kpiVisitas, kpiCumplimiento, kpiCompromisos, kpiProxima);
        configurarTablaVisitas();
        configurarTablaCompromisos();
        boolean registra = contexto.sesion().tienePermiso(Permiso.VISITAS_REGISTRAR);
        for (Node nodo : List.of(btnNueva, btnEditar, btnAnular, btnPendiente, btnIncumplido, btnCumplido)) {
            nodo.setVisible(registra);
            nodo.setManaged(registra);
        }
        boolean informes = contexto.sesion().tienePermiso(Permiso.INFORMES_GENERAR);
        btnInforme.setVisible(informes);
        btnInforme.setManaged(informes);
    }

    @Override
    public void alMostrar() {
        cargar();
    }

    // ------------------------------------------------------------------ carga

    @FXML
    private void cargar() {
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        boolean agro = empresa != null && empresa.sector() == Sector.AGROPECUARIO;
        btnNueva.setDisable(!agro);
        if (!agro) {
            String mensaje = empresa == null ? "Selecciona una empresa en la barra superior."
                    : "Las visitas de inspección son para empresas del sector agropecuario.";
            tablaVisitas.getItems().clear();
            compromisos.clear();
            tablaVisitas.setPlaceholder(new Label(mensaje));
            tablaCompromisos.setPlaceholder(new Label(mensaje));
            detalle.getChildren().clear();
            for (TarjetaIndicador kpi : List.of(kpiVisitas, kpiCumplimiento, kpiCompromisos, kpiProxima)) {
                kpi.actualizar(TarjetaIndicador.SIN_VALOR, "");
            }
            return;
        }
        Long seleccionada = seleccionada() == null ? null : seleccionada().id();
        try {
            tablaVisitas.getItems().setAll(servicio().listar(chkAnuladas.isSelected()));
            compromisos.setAll(servicio().compromisos());
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }
        tablaVisitas.setPlaceholder(new Label("Aún no hay visitas registradas."));
        tablaCompromisos.setPlaceholder(new Label("No hay compromisos."));
        mostrarResumen();
        filtrarCompromisos();
        if (seleccionada != null) {
            tablaVisitas.getItems().stream().filter(v -> v.id() == seleccionada).findFirst()
                    .ifPresent(v -> tablaVisitas.getSelectionModel().select(v));
        }
        if (tablaVisitas.getSelectionModel().isEmpty() && !tablaVisitas.getItems().isEmpty()) {
            tablaVisitas.getSelectionModel().selectFirst();
        }
        mostrarDetalle();
        actualizarAcciones();
    }

    private void mostrarResumen() {
        List<Visita> vigentes = tablaVisitas.getItems().stream().filter(v -> !v.anulada()).toList();
        LocalDate hoy = LocalDate.now();
        kpiVisitas.actualizar(String.valueOf(vigentes.size()), vigentes.isEmpty() ? "Sin visitas"
                : "Última: " + Fechas.paraMostrar(vigentes.get(0).fecha()));
        BigDecimal cumplimiento = vigentes.isEmpty() ? null : vigentes.get(0).cumplimiento();
        kpiCumplimiento.actualizar(Montos.porcentaje(cumplimiento),
                vigentes.isEmpty() ? "" : vigentes.get(0).conclusion().descripcion());
        long pendientes = compromisos.stream().filter(k -> k.estado() == EstadoCompromiso.PENDIENTE).count();
        long vencidos = compromisos.stream().filter(k -> k.vencido(hoy)).count();
        kpiCompromisos.actualizar(String.valueOf(pendientes), vencidos == 0 ? "Ninguno vencido"
                : vencidos + (vencidos == 1 ? " con el plazo vencido" : " con el plazo vencido"));
        LocalDate proxima = vigentes.isEmpty() ? null : vigentes.get(0).proximaVisita();
        kpiProxima.actualizar(proxima == null ? TarjetaIndicador.SIN_VALOR : Fechas.paraMostrar(proxima),
                proxima == null ? "No programada" : proxima.isBefore(hoy) ? "Ya pasó la fecha" : "Programada");
    }

    private void mostrarDetalle() {
        Visita v = seleccionada();
        detalle.getChildren().clear();
        if (v == null) {
            lblDetalleTitulo.setText("Detalle de la visita");
            detalle.getChildren().add(texto("Selecciona una visita para ver su detalle.", "muted"));
            return;
        }
        lblDetalleTitulo.setText(v.tipo().descripcion() + " · " + Fechas.paraMostrar(v.fecha()));
        List<String> datos = new ArrayList<>();
        datos.add("Inspector: " + v.inspector());
        if (v.atendidoPor() != null) {
            datos.add("Atendió: " + v.atendidoPor());
        }
        if (v.lugar() != null) {
            datos.add("Lugar: " + v.lugar());
        }
        if (v.coordenadas() != null) {
            datos.add("Coordenadas: " + v.coordenadas());
        }
        detalle.getChildren().add(texto(String.join("\n", datos), "hint"));
        if (v.anulada()) {
            detalle.getChildren().add(texto("Anulada: " + v.motivoAnulacion(), "margen-negativo"));
        }
        Label conclusion = new Label(v.conclusion().descripcion() + (v.cumplimiento() == null ? ""
                : "  ·  cumple el " + Montos.porcentaje(v.cumplimiento())));
        conclusion.getStyleClass().addAll("info-value", claseSemaforo(v.conclusion().semaforo()));
        conclusion.setStyle("-fx-font-weight: bold;");
        conclusion.setWrapText(true);
        detalle.getChildren().add(conclusion);
        for (AspectoInspeccion a : AspectoInspeccion.values()) {
            ResultadoAspecto r = v.aspectos().get(a);
            if (r == null) {
                continue;
            }
            Label nombre = new Label(a.descripcion());
            nombre.getStyleClass().add("info-key");
            nombre.setWrapText(true);
            Label valor = new Label(r.descripcion());
            valor.getStyleClass().addAll("info-value", claseSemaforo(r.semaforo()));
            valor.setMinWidth(Region.USE_PREF_SIZE);
            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);
            VBox bloque = new VBox(2, new HBox(8, nombre, espacio, valor));
            if (v.notas().get(a) != null) {
                bloque.getChildren().add(texto(v.notas().get(a), "muted"));
            }
            detalle.getChildren().add(bloque);
        }
        if (v.observaciones() != null) {
            Label titulo = texto("Observaciones", "step-title");
            detalle.getChildren().addAll(titulo, texto(v.observaciones(), "muted"));
        }
    }

    // ------------------------------------------------------------------ visitas

    @FXML
    private void nuevaVisita() {
        abrirFormulario(null);
    }

    @FXML
    private void editarVisita() {
        Visita v = seleccionada();
        if (v != null && !v.anulada()) {
            abrirFormulario(v);
        }
    }

    private void abrirFormulario(Visita v) {
        List<Compromiso> pendientes = compromisos.stream()
                .filter(k -> k.estado() == EstadoCompromiso.PENDIENTE && (v == null || k.visitaId() != v.id()))
                .toList();
        FormularioVisita formulario = new FormularioVisita(v, pendientes);
        Visita[] guardada = new Visita[1];
        boolean ok = Dialogos.formulario(ventana(), v == null ? "Nueva visita de inspección" : "Editar visita",
                null, formulario.contenido(), v == null ? "Registrar visita" : "Guardar cambios", 760, () -> {
                    DatosVisita datos = formulario.datos();
                    guardada[0] = v == null ? servicio().registrar(datos) : servicio().actualizar(v.id(), datos);
                });
        if (!ok) {
            return;
        }
        int verificados = 0;
        for (Map.Entry<Compromiso, EstadoCompromiso> e : formulario.verificaciones().entrySet()) {
            try {
                servicio().verificarCompromiso(e.getKey().id(), e.getValue(), guardada[0].fecha(), guardada[0].id(), null);
                verificados++;
            } catch (RuntimeException ex) {
                Notificador.error("Compromiso «" + e.getKey().descripcion() + "»: " + Errores.mensaje(ex));
            }
        }
        cargar();
        tablaVisitas.getItems().stream().filter(x -> x.id() == guardada[0].id()).findFirst()
                .ifPresent(x -> tablaVisitas.getSelectionModel().select(x));
        Notificador.exito((v == null ? "Visita registrada." : "Visita actualizada.")
                + (verificados == 0 ? "" : " " + verificados + (verificados == 1 ? " compromiso verificado." : " compromisos verificados.")));
    }

    @FXML
    private void anularVisita() {
        Visita v = seleccionada();
        if (v == null || v.anulada()) {
            return;
        }
        TextField motivo = new TextField();
        motivo.setPromptText("Ej.: registrada en el productor equivocado");
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Motivo *", motivo);
        boolean ok = Dialogos.formulario(ventana(), "Anular visita",
                "La visita del " + Fechas.paraMostrar(v.fecha()) + " y sus compromisos dejarán de mostrarse.",
                grilla, "Anular", () -> servicio().anular(v.id(), motivo.getText()));
        if (ok) {
            cargar();
            Notificador.exito("Visita anulada.");
        }
    }

    // ------------------------------------------------------------------ fotos

    @FXML
    private void verFotos() {
        Visita v = seleccionada();
        if (v == null) {
            return;
        }
        boolean registra = contexto.sesion().tienePermiso(Permiso.VISITAS_REGISTRAR) && !v.anulada();
        FlowPane galeria = new FlowPane(12, 12);
        galeria.setPrefWrapLength(600);
        Runnable[] recargar = new Runnable[1];
        recargar[0] = () -> {
            galeria.getChildren().clear();
            List<FotoVisita> fotos;
            try {
                fotos = servicio().fotos(v.id());
            } catch (RuntimeException e) {
                Notificador.error(Errores.mensaje(e));
                return;
            }
            if (fotos.isEmpty()) {
                galeria.getChildren().add(texto("Aún no hay fotos de esta visita.", "muted"));
            }
            for (FotoVisita f : fotos) {
                Path ruta = servicio().archivo(f);
                ImageView vista = new ImageView();
                if (Files.exists(ruta)) {
                    vista.setImage(new Image(ruta.toUri().toString(), 180, 135, true, true, true));
                }
                vista.setFitWidth(180);
                vista.setFitHeight(135);
                vista.setPreserveRatio(true);
                VBox tarjeta = new VBox(6, vista, texto(Objects.requireNonNullElse(f.descripcion(), "Sin descripción"), "hint"));
                tarjeta.setPrefWidth(190);
                tarjeta.setAlignment(Pos.TOP_CENTER);
                tarjeta.getStyleClass().add("card");
                if (registra) {
                    Button quitar = new Button("Quitar");
                    quitar.getStyleClass().add("button-secondary");
                    quitar.setOnAction(e -> {
                        if (Dialogos.confirmar(galeria.getScene().getWindow(), "Quitar foto",
                                "La foto se eliminará de la visita y de la carpeta de datos.")) {
                            try {
                                servicio().eliminarFoto(f.id());
                                recargar[0].run();
                            } catch (RuntimeException ex) {
                                Notificador.error(Errores.mensaje(ex));
                            }
                        }
                    });
                    tarjeta.getChildren().add(quitar);
                }
                galeria.getChildren().add(tarjeta);
            }
        };
        recargar[0].run();
        ScrollPane desplazable = new ScrollPane(galeria);
        desplazable.setFitToWidth(true);
        desplazable.setPrefViewportHeight(380);
        desplazable.getStyleClass().add("dialog-scroll");
        VBox contenido = new VBox(10, desplazable);
        if (registra) {
            TextField descripcion = new TextField();
            descripcion.setPromptText("Descripción (opcional). Ej.: cobertizo de alpacas");
            Button agregar = new Button("Agregar fotos…");
            agregar.getStyleClass().add("button-primary");
            agregar.setOnAction(e -> {
                FileChooser selector = new FileChooser();
                selector.setTitle("Elegir fotos de la visita");
                selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fotos (JPG, PNG)",
                        "*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG"));
                List<File> archivos = selector.showOpenMultipleDialog(galeria.getScene().getWindow());
                if (archivos == null) {
                    return;
                }
                int agregadas = 0;
                for (File archivo : archivos) {
                    try {
                        servicio().agregarFoto(v.id(), archivo.toPath(), descripcion.getText());
                        agregadas++;
                    } catch (RuntimeException ex) {
                        Notificador.error(archivo.getName() + ": " + Errores.mensaje(ex));
                    }
                }
                descripcion.clear();
                recargar[0].run();
                if (agregadas > 0) {
                    Notificador.exito(agregadas + (agregadas == 1 ? " foto agregada." : " fotos agregadas."));
                }
            });
            HBox fila = new HBox(10, descripcion, agregar);
            HBox.setHgrow(descripcion, Priority.ALWAYS);
            contenido.getChildren().add(fila);
        }
        Dialogos.informacion(ventana(), "Fotos de la visita del " + Fechas.paraMostrar(v.fecha()),
                "Las fotos se copian a la carpeta de datos de la aplicación. En el informe técnico se incluyen las "
                        + "6 primeras.", contenido);
    }

    // ------------------------------------------------------------------ informe técnico

    @FXML
    private void generarInforme() {
        Visita v = seleccionada();
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        if (v == null || v.anulada() || empresa == null) {
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar informe técnico");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
        selector.setInitialFileName(InformeTecnicoService.nombreSugerido(empresa, v));
        String guardada = contexto.configuracion().obtener(CLAVE_CARPETA, null);
        for (Path carpeta : new Path[]{guardada == null ? null : Path.of(guardada),
                Path.of(System.getProperty("user.home"), "Documents"), Path.of(System.getProperty("user.home"))}) {
            if (carpeta != null && Files.isDirectory(carpeta)) {
                selector.setInitialDirectory(carpeta.toFile());
                break;
            }
        }
        File archivo = selector.showSaveDialog(ventana());
        if (archivo == null) {
            return;
        }
        btnInforme.setDisable(true);
        progreso.setVisible(true);
        Tareas.ejecutar(() -> contexto.informeTecnico().generar(v.id(), archivo.toPath()),
                generado -> {
                    btnInforme.setDisable(false);
                    progreso.setVisible(false);
                    try {
                        contexto.configuracion().guardar(CLAVE_CARPETA, generado.getParent().toString());
                    } catch (RuntimeException e) {
                        // No poder recordar la carpeta no impide seguir.
                    }
                    if (Dialogos.confirmar(ventana(), "Informe generado",
                            "Se guardó «" + generado.getFileName() + "».\n\n¿Quieres abrirlo ahora?")) {
                        abrir(generado);
                    }
                },
                error -> {
                    btnInforme.setDisable(false);
                    progreso.setVisible(false);
                    Notificador.error(Errores.mensaje(error));
                });
    }

    private void abrir(Path ruta) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Notificador.advertencia("El sistema no permite abrir archivos desde la aplicación. Ruta: " + ruta);
            return;
        }
        Tareas.ejecutar(() -> {
            Desktop.getDesktop().open(ruta.toFile());
            return null;
        }, ok -> {
        }, error -> Notificador.error("No se pudo abrir el archivo: " + Errores.mensaje(error)));
    }

    // ------------------------------------------------------------------ compromisos

    @FXML
    private void filtrarCompromisos() {
        boolean solo = chkSoloPendientes.isSelected();
        compromisosFiltrados.setPredicate(k -> !solo || k.estado() == EstadoCompromiso.PENDIENTE);
        actualizarAcciones();
    }

    @FXML
    private void marcarCumplido() {
        verificar(EstadoCompromiso.CUMPLIDO);
    }

    @FXML
    private void marcarIncumplido() {
        verificar(EstadoCompromiso.INCUMPLIDO);
    }

    @FXML
    private void marcarPendiente() {
        verificar(EstadoCompromiso.PENDIENTE);
    }

    private void verificar(EstadoCompromiso estado) {
        Compromiso k = tablaCompromisos.getSelectionModel().getSelectedItem();
        if (k == null) {
            return;
        }
        if (estado == EstadoCompromiso.PENDIENTE) {
            try {
                servicio().verificarCompromiso(k.id(), estado, null, null, null);
                cargar();
            } catch (RuntimeException e) {
                Notificador.error(Errores.mensaje(e));
            }
            return;
        }
        DatePicker fecha = CamposFecha.nuevo(LocalDate.now());
        TextField observacion = new TextField();
        observacion.setPromptText("Opcional. Ej.: construyó el cobertizo");
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Fecha *", fecha);
        Formularios.fila(grilla, "Observación", observacion);
        boolean ok = Dialogos.formulario(ventana(), "Marcar como «" + estado.descripcion().toLowerCase() + "»",
                k.descripcion(), grilla, "Guardar",
                () -> servicio().verificarCompromiso(k.id(), estado, CamposFecha.leer(fecha, "verificación"), null,
                        observacion.getText()));
        if (ok) {
            cargar();
            Notificador.exito("Compromiso actualizado.");
        }
    }

    // ------------------------------------------------------------------ tablas

    private void configurarTablaVisitas() {
        colFecha.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().fecha()));
        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                setText(vacio ? null : Fechas.paraMostrar(fecha));
            }
        });
        colTipo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().tipo().descripcion()
                + (c.getValue().anulada() ? "  (anulada)" : "")));
        colInspector.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().inspector()));
        colCumplimiento.setCellValueFactory(c -> new ReadOnlyStringWrapper(Montos.porcentaje(c.getValue().cumplimiento())));
        colConclusion.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colConclusion.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Visita v, boolean vacio) {
                super.updateItem(v, vacio);
                getStyleClass().removeAll("margen-ok", "margen-bajo", "margen-negativo");
                if (vacio || v == null) {
                    setText(null);
                    return;
                }
                setText(v.conclusion().descripcion());
                getStyleClass().add(switch (v.conclusion()) {
                    case FAVORABLE -> "margen-ok";
                    case CON_OBSERVACIONES -> "margen-bajo";
                    case DESFAVORABLE -> "margen-negativo";
                });
            }
        });
        tablaVisitas.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            mostrarDetalle();
            actualizarAcciones();
        });
        tablaVisitas.setRowFactory(t -> {
            TableRow<Visita> fila = new TableRow<>() {
                @Override
                protected void updateItem(Visita v, boolean vacio) {
                    super.updateItem(v, vacio);
                    pseudoClassStateChanged(ARCHIVADO, !vacio && v != null && v.anulada());
                }
            };
            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()
                        && contexto.sesion().tienePermiso(Permiso.VISITAS_REGISTRAR)) {
                    editarVisita();
                }
            });
            return fila;
        });
    }

    private void configurarTablaCompromisos() {
        LocalDate hoy = LocalDate.now();
        colComDescripcion.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().descripcion()));
        colComAcordado.setCellValueFactory(c -> new ReadOnlyStringWrapper(Fechas.paraMostrar(c.getValue().fechaVisita())));
        colComPlazo.setCellValueFactory(c -> new ReadOnlyStringWrapper(Fechas.paraMostrar(c.getValue().plazo())));
        colComEstado.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colComEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Compromiso k, boolean vacio) {
                super.updateItem(k, vacio);
                getStyleClass().removeAll("margen-ok", "margen-bajo", "margen-negativo");
                if (vacio || k == null) {
                    setText(null);
                    return;
                }
                boolean vencido = k.vencido(hoy);
                setText(k.estado().descripcion() + (vencido ? " (vencido)" : ""));
                getStyleClass().add(switch (k.estado()) {
                    case CUMPLIDO -> "margen-ok";
                    case INCUMPLIDO -> "margen-negativo";
                    case PENDIENTE -> vencido ? "margen-negativo" : "margen-bajo";
                });
            }
        });
        colComVerificacion.setCellValueFactory(c -> {
            Compromiso k = c.getValue();
            if (k.estado() == EstadoCompromiso.PENDIENTE) {
                return new ReadOnlyStringWrapper("");
            }
            return new ReadOnlyStringWrapper(Fechas.paraMostrar(k.fechaVerificacion())
                    + (k.verificadoPor() == null ? "" : " · " + k.verificadoPor())
                    + (k.observacion() == null ? "" : " · " + k.observacion()));
        });
        tablaCompromisos.setItems(compromisosFiltrados);
        tablaCompromisos.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> actualizarAcciones());
    }

    private void actualizarAcciones() {
        Visita v = seleccionada();
        boolean vigente = v != null && !v.anulada();
        btnInforme.setDisable(!vigente);
        btnFotos.setDisable(v == null);
        btnEditar.setDisable(!vigente);
        btnAnular.setDisable(!vigente);
        Compromiso k = tablaCompromisos.getSelectionModel().getSelectedItem();
        btnCumplido.setDisable(k == null || k.estado() == EstadoCompromiso.CUMPLIDO);
        btnIncumplido.setDisable(k == null || k.estado() == EstadoCompromiso.INCUMPLIDO);
        btnPendiente.setDisable(k == null || k.estado() == EstadoCompromiso.PENDIENTE);
    }

    // ------------------------------------------------------------------ utilidades

    private Visita seleccionada() {
        return tablaVisitas.getSelectionModel().getSelectedItem();
    }

    private VisitaService servicio() {
        return contexto.visitas();
    }

    private Window ventana() {
        return tablaVisitas.getScene().getWindow();
    }

    private static Label texto(String texto, String clase) {
        Label l = new Label(texto);
        l.getStyleClass().add(clase);
        l.setWrapText(true);
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }

    private static String claseSemaforo(Semaforo s) {
        return switch (s) {
            case VERDE -> "semaforo-verde";
            case AMARILLO -> "semaforo-amarillo";
            case ROJO -> "semaforo-rojo";
            default -> "semaforo-sin-datos";
        };
    }

    private static Double decimal(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(texto.strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValidacionException("El campo \"" + campo + "\" debe ser un número en grados decimales, "
                    + "por ejemplo -12.0651.");
        }
    }

    /**
     * Formulario de la visita: datos generales, verificación de cada aspecto, compromisos pendientes
     * de visitas anteriores y nuevos compromisos. Propone la conclusión según el cumplimiento.
     */
    private final class FormularioVisita {
        private final DatePicker dpFecha = CamposFecha.nuevo(LocalDate.now());
        private final ComboBox<TipoVisita> cmbTipo = new ComboBox<>(FXCollections.observableArrayList(TipoVisita.values()));
        private final TextField txtInspector = new TextField();
        private final TextField txtAtendido = new TextField();
        private final TextField txtLugar = new TextField();
        private final TextField txtLatitud = new TextField();
        private final TextField txtLongitud = new TextField();
        private final Map<AspectoInspeccion, ComboBox<ResultadoAspecto>> resultados = new EnumMap<>(AspectoInspeccion.class);
        private final Map<AspectoInspeccion, TextField> notas = new EnumMap<>(AspectoInspeccion.class);
        private final Label lblCumplimiento = new Label();
        private final ComboBox<ConclusionVisita> cmbConclusion =
                new ComboBox<>(FXCollections.observableArrayList(ConclusionVisita.values()));
        private boolean conclusionElegida;
        private final TextArea txtObservaciones = new TextArea();
        private final DatePicker dpProxima = CamposFecha.nuevo(null);
        private final Map<Compromiso, ComboBox<EstadoCompromiso>> pendientes = new LinkedHashMap<>();
        private final VBox nuevos = new VBox(8);

        FormularioVisita(Visita v, List<Compromiso> pendientesPrevios) {
            cmbTipo.setValue(TipoVisita.SUPERVISION);
            txtInspector.setText(contexto.sesion().usuario().nombreCompleto());
            txtAtendido.setPromptText("Productor o familiar que atendió");
            txtLugar.setPromptText("Ej.: Anexo Chacapampa, distrito de Huayucachi");
            txtLatitud.setPromptText("Latitud. Ej.: -12.1352");
            txtLongitud.setPromptText("Longitud. Ej.: -75.2311");
            txtObservaciones.setPromptText("Situación general, problemas encontrados, recomendaciones…");
            txtObservaciones.setPrefRowCount(4);
            txtObservaciones.setWrapText(true);
            for (AspectoInspeccion a : AspectoInspeccion.values()) {
                ComboBox<ResultadoAspecto> combo = new ComboBox<>(FXCollections.observableArrayList(ResultadoAspecto.values()));
                combo.setPromptText("Sin evaluar");
                combo.setPrefWidth(160);
                combo.valueProperty().addListener((o, x, y) -> actualizarCumplimiento());
                resultados.put(a, combo);
                TextField nota = new TextField();
                nota.setPromptText("Observación (opcional)");
                notas.put(a, nota);
            }
            cmbConclusion.setValue(ConclusionVisita.FAVORABLE);
            cmbConclusion.setOnAction(e -> conclusionElegida = true);
            for (Compromiso k : pendientesPrevios) {
                ComboBox<EstadoCompromiso> estado = new ComboBox<>(FXCollections.observableArrayList(EstadoCompromiso.values()));
                estado.setValue(EstadoCompromiso.PENDIENTE);
                pendientes.put(k, estado);
            }
            if (v != null) {
                dpFecha.setValue(v.fecha());
                cmbTipo.setValue(v.tipo());
                txtInspector.setText(v.inspector());
                txtAtendido.setText(Objects.requireNonNullElse(v.atendidoPor(), ""));
                txtLugar.setText(Objects.requireNonNullElse(v.lugar(), ""));
                txtLatitud.setText(v.latitud() == null ? "" : String.valueOf(v.latitud()));
                txtLongitud.setText(v.longitud() == null ? "" : String.valueOf(v.longitud()));
                v.aspectos().forEach((a, r) -> resultados.get(a).setValue(r));
                v.notas().forEach((a, n) -> notas.get(a).setText(n));
                txtObservaciones.setText(Objects.requireNonNullElse(v.observaciones(), ""));
                dpProxima.setValue(v.proximaVisita());
                cmbConclusion.setValue(v.conclusion());
                conclusionElegida = true;
            }
            actualizarCumplimiento();
        }

        private void actualizarCumplimiento() {
            List<ResultadoAspecto> elegidos = resultados.values().stream().map(ComboBox::getValue)
                    .filter(Objects::nonNull).toList();
            BigDecimal cumplimiento = Visita.cumplimiento(elegidos);
            lblCumplimiento.setText(cumplimiento == null ? "Marca el resultado de cada aspecto que verifiques."
                    : "Cumplimiento: " + Montos.porcentaje(cumplimiento) + " de " + elegidos.stream()
                    .filter(r -> r != ResultadoAspecto.NO_APLICA).count() + " aspectos aplicables. Conclusión sugerida: "
                    + ConclusionVisita.sugerida(cumplimiento).descripcion().toLowerCase() + ".");
            if (!conclusionElegida) {
                cmbConclusion.setValue(ConclusionVisita.sugerida(cumplimiento));
            }
        }

        Node contenido() {
            GridPane datos = Formularios.grilla();
            Formularios.fila(datos, "Fecha *", dpFecha);
            Formularios.fila(datos, "Tipo de visita *", cmbTipo);
            Formularios.fila(datos, "Inspector *", txtInspector);
            Formularios.fila(datos, "Atendido por", txtAtendido);
            Formularios.fila(datos, "Lugar", txtLugar);
            HBox coordenadas = new HBox(8, txtLatitud, txtLongitud);
            HBox.setHgrow(txtLatitud, Priority.ALWAYS);
            HBox.setHgrow(txtLongitud, Priority.ALWAYS);
            Formularios.fila(datos, "Coordenadas GPS", coordenadas);
            Formularios.nota(datos, "En grados decimales, como las muestra el celular (Google Maps: mantener presionado el punto).");

            GridPane verificacion = new GridPane();
            verificacion.setHgap(10);
            verificacion.setVgap(6);
            ColumnConstraints c1 = new ColumnConstraints();
            c1.setMinWidth(230);
            c1.setPrefWidth(230);
            ColumnConstraints c2 = new ColumnConstraints();
            c2.setMinWidth(160);
            ColumnConstraints c3 = new ColumnConstraints();
            c3.setHgrow(Priority.ALWAYS);
            verificacion.getColumnConstraints().setAll(c1, c2, c3);
            int fila = 0;
            for (AspectoInspeccion.Area area : AspectoInspeccion.Area.values()) {
                Label titulo = texto(area.descripcion(), "step-title");
                verificacion.add(titulo, 0, fila++, 3, 1);
                for (AspectoInspeccion a : AspectoInspeccion.values()) {
                    if (a.area() != area) {
                        continue;
                    }
                    Label nombre = new Label(a.descripcion());
                    nombre.getStyleClass().add("form-label");
                    nombre.setTooltip(new Tooltip(a.guia()));
                    nombre.setWrapText(true);
                    verificacion.addRow(fila++, nombre, resultados.get(a), notas.get(a));
                }
            }
            lblCumplimiento.getStyleClass().add("hint");
            lblCumplimiento.setWrapText(true);

            VBox compromisosBox = new VBox(8);
            if (!pendientes.isEmpty()) {
                compromisosBox.getChildren().add(texto("Compromisos pendientes de visitas anteriores: indica si se cumplieron.",
                        "hint"));
                pendientes.forEach((k, combo) -> {
                    Label desc = new Label(k.descripcion() + "  (acordado el " + Fechas.paraMostrar(k.fechaVisita())
                            + (k.plazo() == null ? "" : ", plazo " + Fechas.paraMostrar(k.plazo())) + ")");
                    desc.setWrapText(true);
                    desc.getStyleClass().add("info-value");
                    HBox.setHgrow(desc, Priority.ALWAYS);
                    desc.setMaxWidth(Double.MAX_VALUE);
                    compromisosBox.getChildren().add(new HBox(10, desc, combo));
                });
            }
            Button agregar = new Button("Agregar compromiso");
            agregar.getStyleClass().add("button-secondary");
            agregar.setOnAction(e -> agregarFilaCompromiso());
            compromisosBox.getChildren().addAll(texto("Nuevos compromisos o recomendaciones acordados en esta visita:", "hint"),
                    nuevos, agregar);

            GridPane cierre = Formularios.grilla();
            Formularios.fila(cierre, "Observaciones", txtObservaciones);
            Formularios.fila(cierre, "Conclusión *", cmbConclusion);
            Formularios.fila(cierre, "Próxima visita", dpProxima);

            VBox todo = new VBox(14,
                    texto("Datos de la visita", "card-title"), datos,
                    texto("Verificación en campo", "card-title"), lblCumplimiento, verificacion,
                    texto("Compromisos", "card-title"), compromisosBox,
                    texto("Conclusión", "card-title"), cierre);
            todo.setStyle("-fx-padding: 0 12 0 0;");
            ScrollPane desplazable = new ScrollPane(todo);
            desplazable.setFitToWidth(true);
            desplazable.setPrefViewportHeight(520);
            desplazable.getStyleClass().add("dialog-scroll");
            return desplazable;
        }

        private void agregarFilaCompromiso() {
            TextField descripcion = new TextField();
            descripcion.setPromptText("Ej.: Construir cobertizo para crías antes de las heladas");
            DatePicker plazo = CamposFecha.nuevo(null);
            plazo.setPromptText("Plazo");
            plazo.setPrefWidth(140);
            Button quitar = new Button("Quitar");
            quitar.getStyleClass().add("button-secondary");
            HBox fila = new HBox(8, descripcion, plazo, quitar);
            HBox.setHgrow(descripcion, Priority.ALWAYS);
            fila.setUserData(new Object[]{descripcion, plazo});
            quitar.setOnAction(e -> nuevos.getChildren().remove(fila));
            nuevos.getChildren().add(fila);
            descripcion.requestFocus();
        }

        DatosVisita datos() {
            LocalDate fecha = CamposFecha.leer(dpFecha, "la visita");
            LocalDate proxima = CamposFecha.leer(dpProxima, "la próxima visita");
            Map<AspectoInspeccion, ResultadoAspecto> aspectos = new EnumMap<>(AspectoInspeccion.class);
            Map<AspectoInspeccion, String> textos = new EnumMap<>(AspectoInspeccion.class);
            resultados.forEach((a, combo) -> {
                if (combo.getValue() != null) {
                    aspectos.put(a, combo.getValue());
                    textos.put(a, notas.get(a).getText());
                }
            });
            List<DatosVisita.NuevoCompromiso> compromisosNuevos = new ArrayList<>();
            for (Node n : nuevos.getChildren()) {
                Object[] campos = (Object[]) n.getUserData();
                TextField descripcion = (TextField) campos[0];
                if (descripcion.getText() == null || descripcion.getText().isBlank()) {
                    continue;
                }
                compromisosNuevos.add(new DatosVisita.NuevoCompromiso(descripcion.getText(),
                        CamposFecha.leer((DatePicker) campos[1], "plazo del compromiso")));
            }
            return new DatosVisita(fecha, cmbTipo.getValue(), txtInspector.getText(), txtLugar.getText(),
                    decimal(txtLatitud.getText(), "Latitud"), decimal(txtLongitud.getText(), "Longitud"),
                    txtAtendido.getText(), txtObservaciones.getText(), cmbConclusion.getValue(), proxima, aspectos, textos,
                    compromisosNuevos);
        }

        /** Compromisos previos a los que se les cambió el estado. */
        Map<Compromiso, EstadoCompromiso> verificaciones() {
            Map<Compromiso, EstadoCompromiso> cambios = new LinkedHashMap<>();
            pendientes.forEach((k, combo) -> {
                if (combo.getValue() != null && combo.getValue() != EstadoCompromiso.PENDIENTE) {
                    cambios.put(k, combo.getValue());
                }
            });
            return cambios;
        }
    }
}
