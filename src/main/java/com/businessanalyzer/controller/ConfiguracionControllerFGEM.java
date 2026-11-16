package com.businessanalyzer.controller;

import com.businessanalyzer.BusinessAnalyzerAppFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.model.CopiaSeguridadFGEM;
import com.businessanalyzer.model.CopiaSeguridadFGEM.ContenidoFGEM;
import com.businessanalyzer.service.CopiaSeguridadServiceFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.TareasFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.FechasFGEM;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Módulo "Configuración": copias de seguridad, restauración e información del sistema. */
public class ConfiguracionControllerFGEM implements VistaModuloFGEM {

    private static final String CLAVE_CARPETA_INFORMES_FGEM = "informes.carpeta";

    private final AppContextFGEM contextoFGEM;

    @FXML private Label lblUltimaFGEM;
    @FXML private Label lblCarpetaFGEM;
    @FXML private Button btnCrearFGEM;
    @FXML private ProgressIndicator progresoFGEM;
    @FXML private CheckBox chkAutomaticaFGEM;
    @FXML private Spinner<Integer> spnConservarFGEM;
    @FXML private Button btnRestaurarFGEM;
    @FXML private Button btnEliminarFGEM;
    @FXML private TableView<CopiaSeguridadFGEM> tablaCopiasFGEM;
    @FXML private TableColumn<CopiaSeguridadFGEM, String> colFechaFGEM;
    @FXML private TableColumn<CopiaSeguridadFGEM, String> colTipoFGEM;
    @FXML private TableColumn<CopiaSeguridadFGEM, String> colTamanoFGEM;
    @FXML private TableColumn<CopiaSeguridadFGEM, String> colArchivoFGEM;
    @FXML private GridPane gridSistemaFGEM;
    @FXML private Label lblCarpetaInformesFGEM;
    @FXML private VBox cajaAparienciaFGEM;

    public ConfiguracionControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        cajaAparienciaFGEM.getChildren().add(new com.businessanalyzer.ui.PanelAparienciaFGEM(contextoFGEM.preferenciasFGEM()));
        spnConservarFGEM.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, CopiaSeguridadServiceFGEM.CONSERVAR_MAXIMO_FGEM, CopiaSeguridadServiceFGEM.CONSERVAR_PREDETERMINADO_FGEM));
        colFechaFGEM.setCellValueFactory(cFGEM -> textoFGEM(FechasFGEM.paraMostrarFGEM(cFGEM.getValue().fechaFGEM())));
        colTipoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().tipoFGEM().descripcionFGEM()));
        colTamanoFGEM.setCellValueFactory(cFGEM -> textoFGEM(tamanoFGEM(cFGEM.getValue().bytesFGEM())));
        colArchivoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().archivoFGEM().getFileName().toString()));
        tablaCopiasFGEM.setPlaceholder(new Label("Todavía no hay copias en la carpeta."));
        tablaCopiasFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> {
            btnRestaurarFGEM.setDisable(nFGEM == null);
            btnEliminarFGEM.setDisable(nFGEM == null);
        });
        btnRestaurarFGEM.setDisable(true);
        btnEliminarFGEM.setDisable(true);
    }

    @Override
    public void alMostrarFGEM() {
        CopiaSeguridadServiceFGEM copiasFGEM = contextoFGEM.copiasFGEM();
        chkAutomaticaFGEM.setSelected(copiasFGEM.automaticaActivaFGEM());
        spnConservarFGEM.getValueFactory().setValue(copiasFGEM.copiasAConservarFGEM());
        cargarCopiasFGEM();
        mostrarSistemaFGEM();
    }

    // ------------------------------------------------------------------ copias

    private void cargarCopiasFGEM() {
        TareasFGEM.ejecutarFGEM(() -> contextoFGEM.copiasFGEM().listarFGEM(),
                listaFGEM -> {
                    tablaCopiasFGEM.getItems().setAll(listaFGEM);
                    lblCarpetaFGEM.setText("Carpeta: " + contextoFGEM.copiasFGEM().carpetaFGEM());
                    lblUltimaFGEM.setText(listaFGEM.isEmpty()
                            ? "Aún no hay copias de seguridad."
                            : FechasFGEM.paraMostrarFGEM(listaFGEM.get(0).fechaFGEM()) + "  ·  " + listaFGEM.get(0).tipoFGEM().descripcionFGEM()
                            + "  ·  " + tamanoFGEM(listaFGEM.get(0).bytesFGEM()));
                },
                errorFGEM -> NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(errorFGEM)));
    }

    @FXML
    private void crearCopiaFGEM() {
        btnCrearFGEM.setDisable(true);
        progresoFGEM.setVisible(true);
        TareasFGEM.ejecutarFGEM(() -> contextoFGEM.copiasFGEM().crearManualFGEM(),
                copiaFGEM -> {
                    btnCrearFGEM.setDisable(false);
                    progresoFGEM.setVisible(false);
                    NotificadorFGEM.exitoFGEM("Copia creada: " + copiaFGEM.archivoFGEM().getFileName() + " (" + tamanoFGEM(copiaFGEM.bytesFGEM()) + ").");
                    cargarCopiasFGEM();
                },
                errorFGEM -> {
                    btnCrearFGEM.setDisable(false);
                    progresoFGEM.setVisible(false);
                    NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(errorFGEM));
                });
    }

    @FXML
    private void cambiarCarpetaFGEM() {
        DirectoryChooser selectorFGEM = new DirectoryChooser();
        selectorFGEM.setTitle("Carpeta para las copias de seguridad");
        Path actualFGEM = contextoFGEM.copiasFGEM().carpetaFGEM();
        if (Files.isDirectory(actualFGEM)) {
            selectorFGEM.setInitialDirectory(actualFGEM.toFile());
        }
        File carpetaFGEM = selectorFGEM.showDialog(ventanaFGEM());
        if (carpetaFGEM == null) {
            return;
        }
        try {
            contextoFGEM.copiasFGEM().cambiarCarpetaFGEM(carpetaFGEM.toPath());
            NotificadorFGEM.exitoFGEM("Las próximas copias se guardarán en " + carpetaFGEM + ".");
            cargarCopiasFGEM();
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    @FXML
    private void abrirCarpetaCopiasFGEM() {
        Path carpetaFGEM = contextoFGEM.copiasFGEM().carpetaFGEM();
        try {
            Files.createDirectories(carpetaFGEM);
        } catch (IOException eFGEM) {
            NotificadorFGEM.errorFGEM("No se pudo crear la carpeta " + carpetaFGEM + ".");
            return;
        }
        abrirFGEM(carpetaFGEM);
    }

    @FXML
    private void guardarAutomaticaFGEM() {
        try {
            spnConservarFGEM.increment(0); // confirma lo escrito en el campo
            contextoFGEM.copiasFGEM().configurarAutomaticaFGEM(chkAutomaticaFGEM.isSelected(), spnConservarFGEM.getValue());
            NotificadorFGEM.exitoFGEM("Preferencias de copia automática guardadas.");
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    @FXML
    private void eliminarCopiaFGEM() {
        CopiaSeguridadFGEM copiaFGEM = tablaCopiasFGEM.getSelectionModel().getSelectedItem();
        if (copiaFGEM == null || !DialogosFGEM.confirmarFGEM(ventanaFGEM(), "Eliminar copia",
                "¿Eliminar la copia del " + FechasFGEM.paraMostrarFGEM(copiaFGEM.fechaFGEM()) + "? Esta acción no se puede deshacer.")) {
            return;
        }
        try {
            contextoFGEM.copiasFGEM().eliminarFGEM(copiaFGEM);
            NotificadorFGEM.exitoFGEM("Copia eliminada.");
            cargarCopiasFGEM();
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    // ------------------------------------------------------------------ restauración

    @FXML
    private void restaurarSeleccionadaFGEM() {
        CopiaSeguridadFGEM copiaFGEM = tablaCopiasFGEM.getSelectionModel().getSelectedItem();
        if (copiaFGEM != null) {
            restaurarFGEM(copiaFGEM.archivoFGEM());
        }
    }

    @FXML
    private void restaurarDesdeArchivoFGEM() {
        FileChooser selectorFGEM = new FileChooser();
        selectorFGEM.setTitle("Elegir copia de seguridad");
        selectorFGEM.getExtensionFilters().add(new FileChooser.ExtensionFilter("Copias de Business Analyzer (*.zip, *.db)", "*.zip", "*.db"));
        Path carpetaFGEM = contextoFGEM.copiasFGEM().carpetaFGEM();
        if (Files.isDirectory(carpetaFGEM)) {
            selectorFGEM.setInitialDirectory(carpetaFGEM.toFile());
        }
        File archivoFGEM = selectorFGEM.showOpenDialog(ventanaFGEM());
        if (archivoFGEM != null) {
            restaurarFGEM(archivoFGEM.toPath());
        }
    }

    private void restaurarFGEM(Path origenFGEM) {
        ContenidoFGEM contenidoFGEM;
        try {
            contenidoFGEM = contextoFGEM.copiasFGEM().validarFGEM(origenFGEM);
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        String resumenFGEM = "La copia «" + origenFGEM.getFileName() + "» contiene " + contenidoFGEM.empresasFGEM() + " empresa(s), "
                + contenidoFGEM.usuariosFGEM() + " usuario(s), " + contenidoFGEM.operacionesFGEM() + " operación(es) y "
                + (contenidoFGEM.fotosFGEM() == 0 ? "ninguna foto de visitas" : contenidoFGEM.fotosFGEM() + " foto(s) de visitas") + ".\n\n"
                + "Todos los datos actuales se reemplazarán por los de la copia. Antes se guardará una copia del "
                + "estado actual en la carpeta de copias.\n\nLa aplicación se cerrará; al abrirla de nuevo trabajarás "
                + "con los datos restaurados. ¿Continuar?";
        if (!DialogosFGEM.confirmarFGEM(ventanaFGEM(), "Restaurar copia de seguridad", resumenFGEM)) {
            return;
        }
        progresoFGEM.setVisible(true);
        TareasFGEM.ejecutarFGEM(() -> contextoFGEM.copiasFGEM().programarRestauracionFGEM(origenFGEM),
                okFGEM -> {
                    progresoFGEM.setVisible(false);
                    DialogosFGEM.informacionFGEM(ventanaFGEM(), "Restauración preparada",
                            "La aplicación se cerrará ahora. Ábrela de nuevo para continuar con los datos restaurados.",
                            null);
                    Platform.exit();
                },
                errorFGEM -> {
                    progresoFGEM.setVisible(false);
                    NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(errorFGEM));
                });
    }

    // ------------------------------------------------------------------ sistema

    private void mostrarSistemaFGEM() {
        Path datosFGEM = contextoFGEM.configFGEM().directorioDatosFGEM();
        Path baseFGEM = contextoFGEM.configFGEM().archivoBaseDatosFGEM();
        long bytesBaseFGEM = tamanoArchivoFGEM(baseFGEM) + tamanoArchivoFGEM(baseFGEM.resolveSibling(baseFGEM.getFileName() + "-wal"));
        gridSistemaFGEM.getChildren().clear();
        filaFGEM(0, "Aplicación", BusinessAnalyzerAppFGEM.NOMBRE_FGEM + " Desktop " + BusinessAnalyzerAppFGEM.VERSION_FGEM);
        filaFGEM(1, "Esquema de la base de datos", "Versión " + contextoFGEM.versionEsquemaFGEM());
        filaFGEM(2, "Base de datos", baseFGEM + "  (" + tamanoFGEM(bytesBaseFGEM) + ")");
        filaFGEM(3, "Carpeta de datos", datosFGEM.toString());
        filaFGEM(4, "Registros (logs)", datosFGEM.resolve("logs").toString());
        filaFGEM(5, "Java", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        filaFGEM(6, "JavaFX", System.getProperty("javafx.runtime.version", "—"));
        filaFGEM(7, "Sistema operativo", System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " (" + System.getProperty("os.arch") + ")");
        lblCarpetaInformesFGEM.setText(contextoFGEM.configuracionFGEM().obtenerFGEM(CLAVE_CARPETA_INFORMES_FGEM,
                "Documentos (se recuerda la última carpeta usada)"));
    }

    private void filaFGEM(int filaFGEM, String etiquetaFGEM, String valorFGEM) {
        Label nombreFGEM = new Label(etiquetaFGEM);
        nombreFGEM.getStyleClass().add("form-label");
        nombreFGEM.setMinWidth(Label.USE_PREF_SIZE);
        Label datoFGEM = new Label(valorFGEM);
        datoFGEM.getStyleClass().add("info-value");
        datoFGEM.setWrapText(true);
        gridSistemaFGEM.addRow(filaFGEM, nombreFGEM, datoFGEM);
    }

    @FXML
    private void abrirCarpetaDatosFGEM() {
        abrirFGEM(contextoFGEM.configFGEM().directorioDatosFGEM());
    }

    @FXML
    private void abrirRegistrosFGEM() {
        abrirFGEM(contextoFGEM.configFGEM().directorioDatosFGEM().resolve("logs"));
    }

    @FXML
    private void verificarIntegridadFGEM() {
        TareasFGEM.ejecutarFGEM(() -> contextoFGEM.copiasFGEM().verificarIntegridadFGEM(),
                resultadoFGEM -> {
                    if (resultadoFGEM.size() == 1 && "ok".equalsIgnoreCase(resultadoFGEM.get(0))) {
                        NotificadorFGEM.exitoFGEM("La base de datos está íntegra.");
                    } else {
                        DialogosFGEM.informacionFGEM(ventanaFGEM(), "Problemas en la base de datos",
                                "La verificación encontró problemas. Restaura una copia de seguridad reciente:\n\n"
                                        + String.join("\n", resultadoFGEM.subList(0, Math.min(10, resultadoFGEM.size()))), null);
                    }
                },
                errorFGEM -> NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(errorFGEM)));
    }

    @FXML
    private void cambiarCarpetaInformesFGEM() {
        DirectoryChooser selectorFGEM = new DirectoryChooser();
        selectorFGEM.setTitle("Carpeta predeterminada de los informes");
        File carpetaFGEM = selectorFGEM.showDialog(ventanaFGEM());
        if (carpetaFGEM != null) {
            contextoFGEM.configuracionFGEM().guardarFGEM(CLAVE_CARPETA_INFORMES_FGEM, carpetaFGEM.getAbsolutePath());
            lblCarpetaInformesFGEM.setText(carpetaFGEM.getAbsolutePath());
            NotificadorFGEM.exitoFGEM("Carpeta de informes actualizada.");
        }
    }

    // ------------------------------------------------------------------ utilidades

    private void abrirFGEM(Path rutaFGEM) {
        if (!Files.exists(rutaFGEM)) {
            NotificadorFGEM.advertenciaFGEM("La carpeta todavía no existe: " + rutaFGEM);
            return;
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            NotificadorFGEM.advertenciaFGEM("El sistema no permite abrir carpetas desde la aplicación. Ruta: " + rutaFGEM);
            return;
        }
        TareasFGEM.ejecutarFGEM(() -> {
                    Desktop.getDesktop().open(rutaFGEM.toFile());
                    return null;
                },
                okFGEM -> {
                },
                errorFGEM -> NotificadorFGEM.errorFGEM("No se pudo abrir " + rutaFGEM + "."));
    }

    private Window ventanaFGEM() {
        return tablaCopiasFGEM.getScene().getWindow();
    }

    static String tamanoFGEM(long bytesFGEM) {
        if (bytesFGEM < 1024) {
            return bytesFGEM + " B";
        }
        if (bytesFGEM < 1024 * 1024) {
            return String.format(Locale.US, "%.0f KB", bytesFGEM / 1024.0);
        }
        return String.format(Locale.US, "%.1f MB", bytesFGEM / (1024.0 * 1024));
    }

    private static long tamanoArchivoFGEM(Path archivoFGEM) {
        try {
            return Files.exists(archivoFGEM) ? Files.size(archivoFGEM) : 0;
        } catch (IOException eFGEM) {
            return 0;
        }
    }

    private static ReadOnlyStringWrapper textoFGEM(String valorFGEM) {
        return new ReadOnlyStringWrapper(valorFGEM);
    }
}
