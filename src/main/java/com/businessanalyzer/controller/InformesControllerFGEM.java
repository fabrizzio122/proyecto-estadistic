package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.InformeGeneradoFGEM;
import com.businessanalyzer.model.TipoInformeFGEM;
import com.businessanalyzer.service.InformeServiceFGEM;
import com.businessanalyzer.ui.CamposFechaFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.TareasFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.FechasFGEM;
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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

public class InformesControllerFGEM implements VistaModuloFGEM {

    private static final String CLAVE_CARPETA_FGEM = "informes.carpeta";

    private enum AtajoFGEM {
        MES_ANTERIOR("Mes anterior"),
        TRIMESTRE("Últimos 3 meses completos"),
        SEMESTRE("Últimos 6 meses completos"),
        DOCE_MESES("Últimos 12 meses completos"),
        MES_ACTUAL("Mes en curso (hasta hoy)"),
        ANIO_ACTUAL("Año en curso (hasta hoy)"),
        PERSONALIZADO("Personalizado");

        private final String descripcionFGEM;

        AtajoFGEM(String descripcionFGEM) {
            this.descripcionFGEM = descripcionFGEM;
        }

        PeriodoFGEM periodoFGEM(LocalDate hoyFGEM) {
            YearMonth anteriorFGEM = YearMonth.from(hoyFGEM).minusMonths(1);
            return switch (this) {
                case MES_ANTERIOR -> PeriodoFGEM.mesFGEM(anteriorFGEM);
                case TRIMESTRE -> completosFGEM(anteriorFGEM, 3);
                case SEMESTRE -> completosFGEM(anteriorFGEM, 6);
                case DOCE_MESES -> completosFGEM(anteriorFGEM, 12);
                case MES_ACTUAL -> new PeriodoFGEM(hoyFGEM.withDayOfMonth(1), hoyFGEM);
                case ANIO_ACTUAL -> new PeriodoFGEM(hoyFGEM.withDayOfYear(1), hoyFGEM);
                case PERSONALIZADO -> null;
            };
        }

        private static PeriodoFGEM completosFGEM(YearMonth ultimoFGEM, int mesesFGEM) {
            return new PeriodoFGEM(ultimoFGEM.minusMonths(mesesFGEM - 1L).atDay(1), ultimoFGEM.atEndOfMonth());
        }

        @Override
        public String toString() {
            return descripcionFGEM;
        }
    }

    private final AppContextFGEM contextoFGEM;
    private final ToggleGroup grupoTiposFGEM = new ToggleGroup();
    private boolean aplicandoAtajoFGEM;

    @FXML private HBox tarjetasFGEM;
    @FXML private ComboBox<AtajoFGEM> cmbPeriodoFGEM;
    @FXML private DatePicker dpDesdeFGEM;
    @FXML private DatePicker dpHastaFGEM;
    @FXML private ProgressIndicator progresoFGEM;
    @FXML private Button btnGenerarFGEM;
    @FXML private Label lblResumenFGEM;
    @FXML private Button btnAbrirFGEM;
    @FXML private Button btnCarpetaFGEM;
    @FXML private TableView<InformeGeneradoFGEM> tablaHistorialFGEM;
    @FXML private TableColumn<InformeGeneradoFGEM, String> colFechaFGEM;
    @FXML private TableColumn<InformeGeneradoFGEM, String> colInformeFGEM;
    @FXML private TableColumn<InformeGeneradoFGEM, String> colPeriodoFGEM;
    @FXML private TableColumn<InformeGeneradoFGEM, String> colUsuarioFGEM;
    @FXML private TableColumn<InformeGeneradoFGEM, String> colArchivoFGEM;

    public InformesControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        for (TipoInformeFGEM tipoFGEM : TipoInformeFGEM.values()) {
            if (tipoFGEM.desdeOtroModuloFGEM()) {
                continue;
            }
            tarjetasFGEM.getChildren().add(tarjetaFGEM(tipoFGEM));
        }
        grupoTiposFGEM.selectedToggleProperty().addListener((oFGEM, anteriorFGEM, nuevoFGEM) -> {
            if (nuevoFGEM == null && anteriorFGEM != null) {
                anteriorFGEM.setSelected(true);
            }
            actualizarResumenFGEM();
        });
        grupoTiposFGEM.getToggles().get(0).setSelected(true);

        CamposFechaFGEM.configurarFGEM(dpDesdeFGEM);
        CamposFechaFGEM.configurarFGEM(dpHastaFGEM);
        cmbPeriodoFGEM.getItems().setAll(AtajoFGEM.values());
        cmbPeriodoFGEM.valueProperty().addListener((oFGEM, aFGEM, atajoFGEM) -> aplicarAtajoFGEM(atajoFGEM));
        dpDesdeFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> fechaEditadaFGEM());
        dpHastaFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> fechaEditadaFGEM());
        cmbPeriodoFGEM.setValue(AtajoFGEM.TRIMESTRE);

        colFechaFGEM.setCellValueFactory(cFGEM -> textoFGEM(FechasFGEM.paraMostrarFGEM(cFGEM.getValue().generadoEnFGEM())));
        colInformeFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().tipoFGEM().tituloFGEM() + " (" + cFGEM.getValue().tipoFGEM().formatoFGEM().nombreFGEM() + ")"));
        colPeriodoFGEM.setCellValueFactory(cFGEM -> textoFGEM(new PeriodoFGEM(cFGEM.getValue().desdeFGEM(), cFGEM.getValue().hastaFGEM()).descripcionFGEM()));
        colUsuarioFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().generadoPorFGEM()));
        colArchivoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().archivoFGEM().toString()));
        tablaHistorialFGEM.setPlaceholder(new Label("Aún no se generaron informes para esta empresa."));
        tablaHistorialFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> {
            btnAbrirFGEM.setDisable(nFGEM == null);
            btnCarpetaFGEM.setDisable(nFGEM == null);
        });
        tablaHistorialFGEM.setRowFactory(tFGEM -> {
            TableRow<InformeGeneradoFGEM> filaFGEM = new TableRow<>();
            filaFGEM.setOnMouseClicked(eFGEM -> {
                if (eFGEM.getButton() == MouseButton.PRIMARY && eFGEM.getClickCount() == 2 && !filaFGEM.isEmpty()) {
                    abrirFGEM();
                }
            });
            return filaFGEM;
        });
        btnAbrirFGEM.setDisable(true);
        btnCarpetaFGEM.setDisable(true);
    }

    @Override
    public void alMostrarFGEM() {
        cargarHistorialFGEM();
        actualizarResumenFGEM();
    }

    private ToggleButton tarjetaFGEM(TipoInformeFGEM tipoFGEM) {
        Label formatoFGEM = new Label(tipoFGEM.formatoFGEM().nombreFGEM());
        formatoFGEM.getStyleClass().addAll("chip", tipoFGEM.formatoFGEM() == TipoInformeFGEM.FormatoFGEM.PDF ? "chip-pdf" : "chip-excel");
        Region espacioFGEM = new Region();
        HBox.setHgrow(espacioFGEM, Priority.ALWAYS);
        HBox cabeceraFGEM = new HBox(8, espacioFGEM, formatoFGEM);
        cabeceraFGEM.setAlignment(Pos.CENTER_LEFT);
        Label tituloFGEM = new Label(tipoFGEM.tituloFGEM());
        tituloFGEM.getStyleClass().add("info-value");
        Label descripcionFGEM = new Label(tipoFGEM.descripcionFGEM());
        descripcionFGEM.getStyleClass().add("hint");
        descripcionFGEM.setWrapText(true);
        descripcionFGEM.setMinHeight(Region.USE_PREF_SIZE);
        VBox contenidoFGEM = new VBox(8, cabeceraFGEM, tituloFGEM, descripcionFGEM);
        contenidoFGEM.setAlignment(Pos.TOP_LEFT);

        ToggleButton botonFGEM = new ToggleButton();
        botonFGEM.setGraphic(contenidoFGEM);
        botonFGEM.setUserData(tipoFGEM);
        botonFGEM.setToggleGroup(grupoTiposFGEM);
        botonFGEM.getStyleClass().add("tarjeta-informe");
        botonFGEM.setMaxWidth(Double.MAX_VALUE);
        botonFGEM.setMinHeight(165);
        botonFGEM.setPrefHeight(165);
        botonFGEM.setMaxHeight(165);
        botonFGEM.setMinWidth(150);
        botonFGEM.setPrefWidth(220);
        contenidoFGEM.prefWidthProperty().bind(botonFGEM.widthProperty().subtract(30));
        HBox.setHgrow(botonFGEM, Priority.ALWAYS);
        return botonFGEM;
    }

    private TipoInformeFGEM tipoElegidoFGEM() {
        return (TipoInformeFGEM) grupoTiposFGEM.getSelectedToggle().getUserData();
    }

    private void aplicarAtajoFGEM(AtajoFGEM atajoFGEM) {
        if (atajoFGEM == null || atajoFGEM == AtajoFGEM.PERSONALIZADO) {
            return;
        }
        PeriodoFGEM periodoFGEM = atajoFGEM.periodoFGEM(LocalDate.now());
        aplicandoAtajoFGEM = true;
        dpDesdeFGEM.setValue(periodoFGEM.desdeFGEM());
        dpHastaFGEM.setValue(periodoFGEM.hastaFGEM());
        aplicandoAtajoFGEM = false;
        actualizarResumenFGEM();
    }

    private void fechaEditadaFGEM() {
        if (!aplicandoAtajoFGEM) {
            cmbPeriodoFGEM.setValue(AtajoFGEM.PERSONALIZADO);
            actualizarResumenFGEM();
        }
    }

    private void actualizarResumenFGEM() {
        if (lblResumenFGEM == null || grupoTiposFGEM.getSelectedToggle() == null) {
            return;
        }
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        LocalDate desdeFGEM = dpDesdeFGEM.getValue();
        LocalDate hastaFGEM = dpHastaFGEM.getValue();
        if (empresaFGEM == null) {
            lblResumenFGEM.setText("Selecciona una empresa en la barra superior para generar informes.");
            btnGenerarFGEM.setDisable(true);
            return;
        }
        btnGenerarFGEM.setDisable(false);
        if (desdeFGEM == null || hastaFGEM == null || desdeFGEM.isAfter(hastaFGEM)) {
            lblResumenFGEM.setText("Indica un período válido (la fecha inicial no puede ser posterior a la final).");
            return;
        }
        TipoInformeFGEM tipoFGEM = tipoElegidoFGEM();
        String textoFGEM = "Se generará «" + tipoFGEM.tituloFGEM() + "» (" + tipoFGEM.formatoFGEM().descripcionArchivoFGEM().toLowerCase(Locale.ROOT)
                + ") de " + empresaFGEM.nombreVisibleFGEM() + " para " + new PeriodoFGEM(desdeFGEM, hastaFGEM).descripcionFGEM() + ".";
        if (tipoFGEM == TipoInformeFGEM.INFORME_EJECUTIVO) {
            textoFGEM += " El diagnóstico incluido siempre evalúa los últimos 3 meses completos.";
        }
        lblResumenFGEM.setText(textoFGEM);
    }

    @FXML
    private void generarFGEM() {
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        if (empresaFGEM == null) {
            NotificadorFGEM.advertenciaFGEM("Selecciona una empresa en la barra superior.");
            return;
        }
        LocalDate desdeFGEM;
        LocalDate hastaFGEM;
        try {
            desdeFGEM = CamposFechaFGEM.leerFGEM(dpDesdeFGEM, "Desde");
            hastaFGEM = CamposFechaFGEM.leerFGEM(dpHastaFGEM, "Hasta");
            new PeriodoFGEM(desdeFGEM, hastaFGEM);
        } catch (ValidacionExceptionFGEM eFGEM) {
            NotificadorFGEM.errorFGEM(eFGEM.getMessage());
            return;
        }
        TipoInformeFGEM tipoFGEM = tipoElegidoFGEM();

        FileChooser selectorFGEM = new FileChooser();
        selectorFGEM.setTitle("Guardar " + tipoFGEM.tituloFGEM());
        selectorFGEM.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                tipoFGEM.formatoFGEM().descripcionArchivoFGEM() + " (*." + tipoFGEM.formatoFGEM().extensionFGEM() + ")",
                "*." + tipoFGEM.formatoFGEM().extensionFGEM()));
        selectorFGEM.setInitialFileName(InformeServiceFGEM.nombreSugeridoFGEM(tipoFGEM, empresaFGEM, desdeFGEM, hastaFGEM));
        File carpetaFGEM = carpetaInicialFGEM();
        if (carpetaFGEM != null) {
            selectorFGEM.setInitialDirectory(carpetaFGEM);
        }
        File archivoFGEM = selectorFGEM.showSaveDialog(btnGenerarFGEM.getScene().getWindow());
        if (archivoFGEM == null) {
            return;
        }

        btnGenerarFGEM.setDisable(true);
        progresoFGEM.setVisible(true);
        TareasFGEM.ejecutarFGEM(() -> contextoFGEM.informesFGEM().generarFGEM(tipoFGEM, desdeFGEM, hastaFGEM, archivoFGEM.toPath()),
                generadoFGEM -> {
                    btnGenerarFGEM.setDisable(false);
                    progresoFGEM.setVisible(false);
                    recordarCarpetaFGEM(generadoFGEM.getParent());
                    cargarHistorialFGEM();
                    if (DialogosFGEM.confirmarFGEM(btnGenerarFGEM.getScene().getWindow(), "Informe generado",
                            "Se guardó «" + generadoFGEM.getFileName() + "».\n\n¿Quieres abrirlo ahora?")) {
                        abrirArchivoFGEM(generadoFGEM);
                    } else {
                        NotificadorFGEM.exitoFGEM("Informe guardado en " + generadoFGEM.getParent() + ".");
                    }
                },
                errorFGEM -> {
                    btnGenerarFGEM.setDisable(false);
                    progresoFGEM.setVisible(false);
                    NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(errorFGEM));
                });
    }

    private File carpetaInicialFGEM() {
        String guardadaFGEM = contextoFGEM.configuracionFGEM().obtenerFGEM(CLAVE_CARPETA_FGEM, null);
        Path[] candidatasFGEM = {
                guardadaFGEM == null ? null : Path.of(guardadaFGEM),
                Path.of(System.getProperty("user.home"), "Documents"),
                Path.of(System.getProperty("user.home"))};
        for (Path carpetaFGEM : candidatasFGEM) {
            if (carpetaFGEM != null && Files.isDirectory(carpetaFGEM)) {
                return carpetaFGEM.toFile();
            }
        }
        return null;
    }

    private void recordarCarpetaFGEM(Path carpetaFGEM) {
        try {
            contextoFGEM.configuracionFGEM().guardarFGEM(CLAVE_CARPETA_FGEM, carpetaFGEM.toString());
        } catch (RuntimeException eFGEM) {
        }
    }

    private void cargarHistorialFGEM() {
        if (contextoFGEM.sesionFGEM().empresaActivaFGEM().isEmpty()) {
            tablaHistorialFGEM.getItems().clear();
            return;
        }
        TareasFGEM.ejecutarFGEM(() -> contextoFGEM.informesFGEM().historialFGEM(),
                listaFGEM -> tablaHistorialFGEM.getItems().setAll(listaFGEM),
                errorFGEM -> NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(errorFGEM)));
    }

    @FXML
    private void abrirFGEM() {
        InformeGeneradoFGEM informeFGEM = tablaHistorialFGEM.getSelectionModel().getSelectedItem();
        if (informeFGEM != null) {
            abrirArchivoFGEM(informeFGEM.archivoFGEM());
        }
    }

    @FXML
    private void abrirCarpetaFGEM() {
        InformeGeneradoFGEM informeFGEM = tablaHistorialFGEM.getSelectionModel().getSelectedItem();
        if (informeFGEM == null) {
            return;
        }
        Path archivoFGEM = informeFGEM.archivoFGEM();
        boolean windowsFGEM = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        if (windowsFGEM && Files.exists(archivoFGEM)) {
            try {
                new ProcessBuilder("explorer.exe", "/select,", archivoFGEM.toString()).start();
                return;
            } catch (IOException eFGEM) {
            }
        }
        abrirArchivoFGEM(archivoFGEM.getParent());
    }

    private void abrirArchivoFGEM(Path rutaFGEM) {
        if (rutaFGEM == null || !Files.exists(rutaFGEM)) {
            NotificadorFGEM.advertenciaFGEM("El archivo ya no existe en esa ubicación (pudo moverse o eliminarse).");
            return;
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            NotificadorFGEM.advertenciaFGEM("El sistema no permite abrir archivos desde la aplicación. Ruta: " + rutaFGEM);
            return;
        }
        TareasFGEM.ejecutarFGEM(() -> {
                    Desktop.getDesktop().open(rutaFGEM.toFile());
                    return null;
                },
                okFGEM -> {
                },
                errorFGEM -> NotificadorFGEM.errorFGEM("No se pudo abrir «" + rutaFGEM.getFileName()
                        + "». ¿Hay un programa instalado para abrir este tipo de archivo?"));
    }

    private static ReadOnlyStringWrapper textoFGEM(String valorFGEM) {
        return new ReadOnlyStringWrapper(valorFGEM);
    }
}
