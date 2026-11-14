package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.InformeGenerado;
import com.businessanalyzer.model.TipoInforme;
import com.businessanalyzer.service.InformeService;
import com.businessanalyzer.ui.CamposFecha;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.Tareas;
import com.businessanalyzer.ui.VistaModulo;
import com.businessanalyzer.util.Fechas;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

/** Módulo "Informes y reportes": genera PDF y Excel y muestra el historial. */
public class InformesController implements VistaModulo {

    private static final String CLAVE_CARPETA = "informes.carpeta";

    /** Atajos de período. Los meses "completos" no incluyen el mes en curso. */
    private enum Atajo {
        MES_ANTERIOR("Mes anterior"),
        TRIMESTRE("Últimos 3 meses completos"),
        SEMESTRE("Últimos 6 meses completos"),
        DOCE_MESES("Últimos 12 meses completos"),
        MES_ACTUAL("Mes en curso (hasta hoy)"),
        ANIO_ACTUAL("Año en curso (hasta hoy)"),
        PERSONALIZADO("Personalizado");

        private final String descripcion;

        Atajo(String descripcion) {
            this.descripcion = descripcion;
        }

        Periodo periodo(LocalDate hoy) {
            YearMonth anterior = YearMonth.from(hoy).minusMonths(1);
            return switch (this) {
                case MES_ANTERIOR -> Periodo.mes(anterior);
                case TRIMESTRE -> completos(anterior, 3);
                case SEMESTRE -> completos(anterior, 6);
                case DOCE_MESES -> completos(anterior, 12);
                case MES_ACTUAL -> new Periodo(hoy.withDayOfMonth(1), hoy);
                case ANIO_ACTUAL -> new Periodo(hoy.withDayOfYear(1), hoy);
                case PERSONALIZADO -> null;
            };
        }

        private static Periodo completos(YearMonth ultimo, int meses) {
            return new Periodo(ultimo.minusMonths(meses - 1L).atDay(1), ultimo.atEndOfMonth());
        }

        @Override
        public String toString() {
            return descripcion;
        }
    }

    private final AppContext contexto;
    private final ToggleGroup grupoTipos = new ToggleGroup();
    private boolean aplicandoAtajo;

    @FXML private HBox tarjetas;
    @FXML private ComboBox<Atajo> cmbPeriodo;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ProgressIndicator progreso;
    @FXML private Button btnGenerar;
    @FXML private Label lblResumen;
    @FXML private Button btnAbrir;
    @FXML private Button btnCarpeta;
    @FXML private TableView<InformeGenerado> tablaHistorial;
    @FXML private TableColumn<InformeGenerado, String> colFecha;
    @FXML private TableColumn<InformeGenerado, String> colInforme;
    @FXML private TableColumn<InformeGenerado, String> colPeriodo;
    @FXML private TableColumn<InformeGenerado, String> colUsuario;
    @FXML private TableColumn<InformeGenerado, String> colArchivo;

    public InformesController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        for (TipoInforme tipo : TipoInforme.values()) {
            tarjetas.getChildren().add(tarjeta(tipo));
        }
        grupoTipos.selectedToggleProperty().addListener((o, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true); // siempre hay un informe elegido
            }
            actualizarResumen();
        });
        grupoTipos.getToggles().get(0).setSelected(true);

        CamposFecha.configurar(dpDesde);
        CamposFecha.configurar(dpHasta);
        cmbPeriodo.getItems().setAll(Atajo.values());
        cmbPeriodo.valueProperty().addListener((o, a, atajo) -> aplicarAtajo(atajo));
        dpDesde.valueProperty().addListener((o, a, n) -> fechaEditada());
        dpHasta.valueProperty().addListener((o, a, n) -> fechaEditada());
        cmbPeriodo.setValue(Atajo.TRIMESTRE);

        colFecha.setCellValueFactory(c -> texto(Fechas.paraMostrar(c.getValue().generadoEn())));
        colInforme.setCellValueFactory(c -> texto(c.getValue().tipo().titulo() + " (" + c.getValue().tipo().formato().nombre() + ")"));
        colPeriodo.setCellValueFactory(c -> texto(new Periodo(c.getValue().desde(), c.getValue().hasta()).descripcion()));
        colUsuario.setCellValueFactory(c -> texto(c.getValue().generadoPor()));
        colArchivo.setCellValueFactory(c -> texto(c.getValue().archivo().toString()));
        tablaHistorial.setPlaceholder(new Label("Aún no se generaron informes para esta empresa."));
        tablaHistorial.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            btnAbrir.setDisable(n == null);
            btnCarpeta.setDisable(n == null);
        });
        tablaHistorial.setRowFactory(t -> {
            TableRow<InformeGenerado> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()) {
                    abrir();
                }
            });
            return fila;
        });
        btnAbrir.setDisable(true);
        btnCarpeta.setDisable(true);
    }

    @Override
    public void alMostrar() {
        cargarHistorial();
        actualizarResumen();
    }

    // ------------------------------------------------------------------ elección del informe y del período

    private ToggleButton tarjeta(TipoInforme tipo) {
        FontIcon icono = new FontIcon(tipo.icono());
        icono.getStyleClass().add("tarjeta-informe-icono");
        Label formato = new Label(tipo.formato().nombre());
        formato.getStyleClass().addAll("chip", tipo.formato() == TipoInforme.Formato.PDF ? "chip-pdf" : "chip-excel");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(8, icono, espacio, formato);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        Label titulo = new Label(tipo.titulo());
        titulo.getStyleClass().add("info-value");
        Label descripcion = new Label(tipo.descripcion());
        descripcion.getStyleClass().add("hint");
        descripcion.setWrapText(true);
        descripcion.setMinHeight(Region.USE_PREF_SIZE);
        VBox contenido = new VBox(8, cabecera, titulo, descripcion);
        contenido.setAlignment(Pos.TOP_LEFT);

        ToggleButton boton = new ToggleButton();
        boton.setGraphic(contenido);
        boton.setUserData(tipo);
        boton.setToggleGroup(grupoTipos);
        boton.getStyleClass().add("tarjeta-informe");
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setMinHeight(165);
        boton.setPrefHeight(165);
        boton.setMaxHeight(165);
        boton.setMinWidth(150);
        boton.setPrefWidth(220);
        contenido.prefWidthProperty().bind(boton.widthProperty().subtract(30));
        HBox.setHgrow(boton, Priority.ALWAYS);
        return boton;
    }

    private TipoInforme tipoElegido() {
        return (TipoInforme) grupoTipos.getSelectedToggle().getUserData();
    }

    private void aplicarAtajo(Atajo atajo) {
        if (atajo == null || atajo == Atajo.PERSONALIZADO) {
            return;
        }
        Periodo periodo = atajo.periodo(LocalDate.now());
        aplicandoAtajo = true;
        dpDesde.setValue(periodo.desde());
        dpHasta.setValue(periodo.hasta());
        aplicandoAtajo = false;
        actualizarResumen();
    }

    private void fechaEditada() {
        if (!aplicandoAtajo) {
            cmbPeriodo.setValue(Atajo.PERSONALIZADO);
            actualizarResumen();
        }
    }

    private void actualizarResumen() {
        if (lblResumen == null || grupoTipos.getSelectedToggle() == null) {
            return;
        }
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        if (empresa == null) {
            lblResumen.setText("Selecciona una empresa en la barra superior para generar informes.");
            btnGenerar.setDisable(true);
            return;
        }
        btnGenerar.setDisable(false);
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            lblResumen.setText("Indica un período válido (la fecha inicial no puede ser posterior a la final).");
            return;
        }
        TipoInforme tipo = tipoElegido();
        String texto = "Se generará «" + tipo.titulo() + "» (" + tipo.formato().descripcionArchivo().toLowerCase(Locale.ROOT)
                + ") de " + empresa.nombreVisible() + " para " + new Periodo(desde, hasta).descripcion() + ".";
        if (tipo == TipoInforme.INFORME_EJECUTIVO) {
            texto += " El diagnóstico incluido siempre evalúa los últimos 3 meses completos.";
        }
        lblResumen.setText(texto);
    }

    // ------------------------------------------------------------------ generación

    @FXML
    private void generar() {
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        if (empresa == null) {
            Notificador.advertencia("Selecciona una empresa en la barra superior.");
            return;
        }
        LocalDate desde;
        LocalDate hasta;
        try {
            desde = CamposFecha.leer(dpDesde, "Desde");
            hasta = CamposFecha.leer(dpHasta, "Hasta");
            new Periodo(desde, hasta);
        } catch (ValidacionException e) {
            Notificador.error(e.getMessage());
            return;
        }
        TipoInforme tipo = tipoElegido();

        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar " + tipo.titulo());
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                tipo.formato().descripcionArchivo() + " (*." + tipo.formato().extension() + ")",
                "*." + tipo.formato().extension()));
        selector.setInitialFileName(InformeService.nombreSugerido(tipo, empresa, desde, hasta));
        File carpeta = carpetaInicial();
        if (carpeta != null) {
            selector.setInitialDirectory(carpeta);
        }
        File archivo = selector.showSaveDialog(btnGenerar.getScene().getWindow());
        if (archivo == null) {
            return;
        }

        btnGenerar.setDisable(true);
        progreso.setVisible(true);
        Tareas.ejecutar(() -> contexto.informes().generar(tipo, desde, hasta, archivo.toPath()),
                generado -> {
                    btnGenerar.setDisable(false);
                    progreso.setVisible(false);
                    recordarCarpeta(generado.getParent());
                    cargarHistorial();
                    if (Dialogos.confirmar(btnGenerar.getScene().getWindow(), "Informe generado",
                            "Se guardó «" + generado.getFileName() + "».\n\n¿Quieres abrirlo ahora?")) {
                        abrirArchivo(generado);
                    } else {
                        Notificador.exito("Informe guardado en " + generado.getParent() + ".");
                    }
                },
                error -> {
                    btnGenerar.setDisable(false);
                    progreso.setVisible(false);
                    Notificador.error(Errores.mensaje(error));
                });
    }

    private File carpetaInicial() {
        String guardada = contexto.configuracion().obtener(CLAVE_CARPETA, null);
        Path[] candidatas = {
                guardada == null ? null : Path.of(guardada),
                Path.of(System.getProperty("user.home"), "Documents"),
                Path.of(System.getProperty("user.home"))};
        for (Path carpeta : candidatas) {
            if (carpeta != null && Files.isDirectory(carpeta)) {
                return carpeta.toFile();
            }
        }
        return null;
    }

    private void recordarCarpeta(Path carpeta) {
        try {
            contexto.configuracion().guardar(CLAVE_CARPETA, carpeta.toString());
        } catch (RuntimeException e) {
            // No poder recordar la carpeta no impide seguir trabajando.
        }
    }

    // ------------------------------------------------------------------ historial

    private void cargarHistorial() {
        if (contexto.sesion().empresaActiva().isEmpty()) {
            tablaHistorial.getItems().clear();
            return;
        }
        Tareas.ejecutar(() -> contexto.informes().historial(),
                lista -> tablaHistorial.getItems().setAll(lista),
                error -> Notificador.error(Errores.mensaje(error)));
    }

    @FXML
    private void abrir() {
        InformeGenerado informe = tablaHistorial.getSelectionModel().getSelectedItem();
        if (informe != null) {
            abrirArchivo(informe.archivo());
        }
    }

    @FXML
    private void abrirCarpeta() {
        InformeGenerado informe = tablaHistorial.getSelectionModel().getSelectedItem();
        if (informe == null) {
            return;
        }
        Path archivo = informe.archivo();
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        if (windows && Files.exists(archivo)) {
            try {
                new ProcessBuilder("explorer.exe", "/select,", archivo.toString()).start();
                return;
            } catch (IOException e) {
                // Se intenta abrir solo la carpeta.
            }
        }
        abrirArchivo(archivo.getParent());
    }

    /** Abre el archivo o la carpeta con el programa predeterminado del sistema. */
    private void abrirArchivo(Path ruta) {
        if (ruta == null || !Files.exists(ruta)) {
            Notificador.advertencia("El archivo ya no existe en esa ubicación (pudo moverse o eliminarse).");
            return;
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Notificador.advertencia("El sistema no permite abrir archivos desde la aplicación. Ruta: " + ruta);
            return;
        }
        Tareas.ejecutar(() -> {
                    Desktop.getDesktop().open(ruta.toFile());
                    return null;
                },
                ok -> {
                },
                error -> Notificador.error("No se pudo abrir «" + ruta.getFileName()
                        + "». ¿Hay un programa instalado para abrir este tipo de archivo?"));
    }

    private static ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(valor);
    }
}
