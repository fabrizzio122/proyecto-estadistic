package com.businessanalyzer.controller;

import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.importacion.DatosImportacionFGEM.ErrorFilaFGEM;
import com.businessanalyzer.importacion.DatosImportacionFGEM.ResultadoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.TareasFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Módulo «Importar desde Excel»: descarga de la plantilla, revisión del archivo (sin guardar nada) e
 * importación a la empresa activa. Las reglas están en {@link com.businessanalyzer.service.ImportacionServiceFGEM}.
 */
public class ImportacionControllerFGEM implements VistaModuloFGEM {

    private static final String CLAVE_CARPETA_FGEM = "importacion.carpeta";
    private static final DateTimeFormatter FECHA_FGEM = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> ESTILOS_AVISO_FGEM = List.of("aviso-importacion-ok", "aviso-importacion-error", "aviso-importacion-advertencia");

    private final AppContextFGEM contextoFGEM;

    @FXML private Label lblEmpresaFGEM;
    @FXML private Button btnElegirFGEM;
    @FXML private Button btnRevisarFGEM;
    @FXML private ProgressIndicator progresoFGEM;
    @FXML private Label lblArchivoFGEM;
    @FXML private Button btnImportarFGEM;
    @FXML private Label lblEstadoFGEM;
    @FXML private FlowPane panelConteosFGEM;
    @FXML private VBox cajaAdvertenciasFGEM;
    @FXML private Label lblTituloErroresFGEM;
    @FXML private TableView<ErrorFilaFGEM> tablaErroresFGEM;
    @FXML private TableColumn<ErrorFilaFGEM, String> colHojaFGEM;
    @FXML private TableColumn<ErrorFilaFGEM, String> colFilaFGEM;
    @FXML private TableColumn<ErrorFilaFGEM, String> colProblemaFGEM;

    /** Archivo elegido y la empresa sobre la que se revisó; la importación exige que no hayan cambiado. */
    private Path archivoFGEM;
    private Long empresaRevisadaFGEM;
    private boolean listoParaImportarFGEM;

    public ImportacionControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        colHojaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().hojaFGEM()));
        colFilaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().filaFGEM() > 0
                ? String.valueOf(cFGEM.getValue().filaFGEM()) : "—"));
        colProblemaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().mensajeFGEM()));
        colProblemaFGEM.setCellFactory(cFGEM -> new CeldaAjustadaFGEM());
        tablaErroresFGEM.setPlaceholder(new Label("Sin errores."));
        mostrarErroresFGEM(List.of());
        lblEstadoFGEM.getStyleClass().add("aviso-importacion");
        actualizarBotonesFGEM(false);
    }

    @Override
    public void alMostrarFGEM() {
        Optional<EmpresaFGEM> empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM();
        lblEmpresaFGEM.setText(empresaFGEM.map(eFGEM -> "Los datos se cargarán en: " + eFGEM.nombreVisibleFGEM())
                .orElse("Elige primero una empresa activa en la barra superior."));
        btnElegirFGEM.setDisable(empresaFGEM.isEmpty());
        // Si se cambió de empresa después de revisar, hay que revisar de nuevo.
        if (empresaRevisadaFGEM != null && empresaFGEM.map(EmpresaFGEM::idFGEM).filter(empresaRevisadaFGEM::equals).isEmpty()) {
            listoParaImportarFGEM = false;
            mostrarEstadoFGEM("Cambiaste de empresa activa. Pulsa «Revisar otra vez» para revisar el archivo con la empresa nueva.",
                    "aviso-importacion-advertencia");
        }
        actualizarBotonesFGEM(false);
    }

    // ------------------------------------------------------------------ plantilla

    @FXML
    private void guardarPlantillaVaciaFGEM() {
        guardarPlantillaFGEM(false, "plantilla-importacion.xlsx");
    }

    @FXML
    private void guardarPlantillaEjemploFGEM() {
        guardarPlantillaFGEM(true, "ejemplo-bodega.xlsx");
    }

    private void guardarPlantillaFGEM(boolean conEjemploFGEM, String nombreFGEM) {
        FileChooser selectorFGEM = selectorFGEM(conEjemploFGEM ? "Guardar plantilla con ejemplo" : "Guardar plantilla");
        selectorFGEM.setInitialFileName(nombreFGEM);
        File destinoFGEM = selectorFGEM.showSaveDialog(ventanaFGEM());
        if (destinoFGEM == null) {
            return;
        }
        Path rutaFGEM = conExtensionFGEM(destinoFGEM.toPath());
        recordarCarpetaFGEM(rutaFGEM);
        ejecutarFGEM(() -> {
            contextoFGEM.importacionFGEM().crearPlantillaFGEM(rutaFGEM, conEjemploFGEM);
            return rutaFGEM;
        }, guardadaFGEM -> {
            NotificadorFGEM.exitoFGEM("Plantilla guardada: " + guardadaFGEM.getFileName() + ". Se abrirá en Excel.");
            abrirFGEM(guardadaFGEM);
        });
    }

    // ------------------------------------------------------------------ revisión

    @FXML
    private void elegirArchivoFGEM() {
        FileChooser selectorFGEM = selectorFGEM("Elegir el archivo a importar");
        File elegidoFGEM = selectorFGEM.showOpenDialog(ventanaFGEM());
        if (elegidoFGEM == null) {
            return;
        }
        archivoFGEM = elegidoFGEM.toPath();
        recordarCarpetaFGEM(archivoFGEM);
        lblArchivoFGEM.setText("Archivo: " + archivoFGEM.getFileName());
        revisarFGEM();
    }

    @FXML
    private void revisarOtraVezFGEM() {
        if (archivoFGEM != null) {
            revisarFGEM();
        }
    }

    private void revisarFGEM() {
        Path aRevisarFGEM = archivoFGEM;
        Long empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().map(EmpresaFGEM::idFGEM).orElse(null);
        listoParaImportarFGEM = false;
        mostrarEstadoFGEM("Revisando " + aRevisarFGEM.getFileName() + "…", null);
        limpiarResultadoFGEM();
        ejecutarFGEM(() -> contextoFGEM.importacionFGEM().revisarFGEM(aRevisarFGEM), resultadoFGEM -> {
            empresaRevisadaFGEM = empresaFGEM;
            mostrarResultadoFGEM(resultadoFGEM);
            listoParaImportarFGEM = resultadoFGEM.sinErroresFGEM();
            actualizarBotonesFGEM(false);
        });
    }

    // ------------------------------------------------------------------ importación

    @FXML
    private void importarFGEM() {
        if (!listoParaImportarFGEM || archivoFGEM == null) {
            return;
        }
        String empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().map(EmpresaFGEM::nombreVisibleFGEM).orElse("la empresa activa");
        if (!DialogosFGEM.confirmarFGEM(ventanaFGEM(), "Importar datos",
                "Se importarán los datos de «" + archivoFGEM.getFileName() + "» en " + empresaFGEM + ".\n\n"
                        + "Antes se guardará una copia de seguridad. Los registros importados no se borran de golpe: si te "
                        + "equivocas de archivo, restaura esa copia desde Configuración.\n\n¿Continuar?")) {
            return;
        }
        Path aImportarFGEM = archivoFGEM;
        mostrarEstadoFGEM("Importando… (puede tardar unos segundos si hay muchas filas)", null);
        ejecutarFGEM(() -> {
            contextoFGEM.copiasFGEM().crearAntesDeImportarFGEM();
            return contextoFGEM.importacionFGEM().importarFGEM(aImportarFGEM);
        }, resultadoFGEM -> {
            mostrarResultadoFGEM(resultadoFGEM);
            if (resultadoFGEM.aplicadoFGEM()) {
                // Para no importar dos veces el mismo archivo por accidente.
                archivoFGEM = null;
                empresaRevisadaFGEM = null;
                listoParaImportarFGEM = false;
                lblArchivoFGEM.setText("");
                NotificadorFGEM.exitoFGEM("Importación completada: " + resumenFGEM(resultadoFGEM) + ".");
            } else {
                listoParaImportarFGEM = false;
            }
            actualizarBotonesFGEM(false);
        });
    }

    // ------------------------------------------------------------------ presentación

    private void mostrarResultadoFGEM(ResultadoFGEM rFGEM) {
        limpiarResultadoFGEM();
        if (rFGEM.aplicadoFGEM()) {
            mostrarEstadoFGEM("Importación completada. Ya puedes ver los datos en Ventas y compras, Gestión financiera y los análisis.",
                    "aviso-importacion-ok");
        } else if (rFGEM.sinErroresFGEM()) {
            mostrarEstadoFGEM("Todo correcto: no se encontraron errores. Revisa el resumen y pulsa «Importar a la empresa activa». "
                    + "Todavía no se ha guardado nada.", "aviso-importacion-ok");
        } else {
            mostrarEstadoFGEM("Hay " + rFGEM.erroresFGEM().size() + " fila(s) por corregir. No se ha guardado nada: corrige el archivo en "
                    + "Excel, guárdalo y pulsa «Revisar otra vez».", "aviso-importacion-error");
        }

        String verboFGEM = rFGEM.aplicadoFGEM() ? "" : "a importar";
        agregarConteoFGEM(rFGEM.productosFGEM(), "producto nuevo", "productos nuevos", verboFGEM);
        agregarConteoFGEM(rFGEM.categoriasFGEM(), "categoría nueva", "categorías nuevas", verboFGEM);
        agregarConteoFGEM(rFGEM.ventasFGEM(), "venta", "ventas", verboFGEM);
        agregarConteoFGEM(rFGEM.comprasFGEM(), "compra", "compras", verboFGEM);
        agregarConteoFGEM(rFGEM.gastosFGEM(), "gasto o ingreso", "gastos e ingresos", verboFGEM);
        if (rFGEM.desdeFGEM() != null) {
            Label periodoFGEM = new Label("Del " + FECHA_FGEM.format(rFGEM.desdeFGEM()) + " al " + FECHA_FGEM.format(rFGEM.hastaFGEM()));
            periodoFGEM.getStyleClass().addAll("chip", "chip-periodo");
            panelConteosFGEM.getChildren().add(periodoFGEM);
        }

        for (String avisoFGEM : rFGEM.advertenciasFGEM()) {
            Label lblFGEM = new Label("⚠  " + avisoFGEM);
            lblFGEM.setWrapText(true);
            lblFGEM.setMinHeight(Region.USE_PREF_SIZE);
            lblFGEM.setMaxWidth(Double.MAX_VALUE);
            lblFGEM.getStyleClass().addAll("aviso-importacion", "aviso-importacion-advertencia");
            cajaAdvertenciasFGEM.getChildren().add(lblFGEM);
        }
        mostrarErroresFGEM(rFGEM.erroresFGEM());
    }

    private void agregarConteoFGEM(int cantidadFGEM, String singularFGEM, String pluralFGEM, String sufijoFGEM) {
        if (cantidadFGEM == 0) {
            return;
        }
        Label chipFGEM = new Label(cantidadFGEM + " " + (cantidadFGEM == 1 ? singularFGEM : pluralFGEM) + (sufijoFGEM.isEmpty() ? "" : " " + sufijoFGEM));
        chipFGEM.getStyleClass().addAll("chip", "chip-ok");
        panelConteosFGEM.getChildren().add(chipFGEM);
    }

    private static String resumenFGEM(ResultadoFGEM rFGEM) {
        List<String> partesFGEM = new ArrayList<>();
        if (rFGEM.productosFGEM() > 0) {
            partesFGEM.add(rFGEM.productosFGEM() + " productos");
        }
        if (rFGEM.ventasFGEM() > 0) {
            partesFGEM.add(rFGEM.ventasFGEM() + " ventas");
        }
        if (rFGEM.comprasFGEM() > 0) {
            partesFGEM.add(rFGEM.comprasFGEM() + " compras");
        }
        if (rFGEM.gastosFGEM() > 0) {
            partesFGEM.add(rFGEM.gastosFGEM() + " gastos");
        }
        return partesFGEM.isEmpty() ? "sin registros nuevos" : String.join(", ", partesFGEM);
    }

    private void mostrarErroresFGEM(List<ErrorFilaFGEM> erroresFGEM) {
        tablaErroresFGEM.getItems().setAll(erroresFGEM);
        boolean hayFGEM = !erroresFGEM.isEmpty();
        tablaErroresFGEM.setVisible(hayFGEM);
        tablaErroresFGEM.setManaged(hayFGEM);
        lblTituloErroresFGEM.setVisible(hayFGEM);
        lblTituloErroresFGEM.setManaged(hayFGEM);
    }

    private void limpiarResultadoFGEM() {
        panelConteosFGEM.getChildren().clear();
        cajaAdvertenciasFGEM.getChildren().clear();
        mostrarErroresFGEM(List.of());
    }

    /** @param estiloFGEM una de {@link #ESTILOS_AVISO_FGEM}, o {@code null} para un mensaje neutro */
    private void mostrarEstadoFGEM(String textoFGEM, String estiloFGEM) {
        lblEstadoFGEM.setText(textoFGEM);
        lblEstadoFGEM.getStyleClass().removeAll(ESTILOS_AVISO_FGEM);
        if (estiloFGEM != null) {
            lblEstadoFGEM.getStyleClass().add(estiloFGEM);
        }
    }

    private void actualizarBotonesFGEM(boolean ocupadoFGEM) {
        progresoFGEM.setVisible(ocupadoFGEM);
        boolean sinEmpresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().isEmpty();
        btnElegirFGEM.setDisable(ocupadoFGEM || sinEmpresaFGEM);
        btnRevisarFGEM.setVisible(archivoFGEM != null);
        btnRevisarFGEM.setManaged(archivoFGEM != null);
        btnRevisarFGEM.setDisable(ocupadoFGEM || sinEmpresaFGEM);
        btnImportarFGEM.setDisable(ocupadoFGEM || !listoParaImportarFGEM);
    }

    // ------------------------------------------------------------------ utilidades

    private <T> void ejecutarFGEM(Callable<T> trabajoFGEM, java.util.function.Consumer<T> alTerminarFGEM) {
        actualizarBotonesFGEM(true);
        TareasFGEM.ejecutarFGEM(trabajoFGEM, resultadoFGEM -> {
            actualizarBotonesFGEM(false);
            alTerminarFGEM.accept(resultadoFGEM);
        }, errorFGEM -> {
            listoParaImportarFGEM = false;
            actualizarBotonesFGEM(false);
            String mensajeFGEM = ErroresFGEM.mensajeFGEM(errorFGEM);
            mostrarEstadoFGEM(mensajeFGEM, "aviso-importacion-error");
            NotificadorFGEM.errorFGEM(mensajeFGEM);
        });
    }

    private FileChooser selectorFGEM(String tituloFGEM) {
        FileChooser selectorFGEM = new FileChooser();
        selectorFGEM.setTitle(tituloFGEM);
        selectorFGEM.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libros de Excel (*.xlsx, *.xls)", "*.xlsx", "*.xls"));
        String carpetaFGEM = contextoFGEM.configuracionFGEM().obtenerFGEM(CLAVE_CARPETA_FGEM, "");
        if (!carpetaFGEM.isBlank() && Files.isDirectory(Path.of(carpetaFGEM))) {
            selectorFGEM.setInitialDirectory(new File(carpetaFGEM));
        }
        return selectorFGEM;
    }

    private void recordarCarpetaFGEM(Path archivoFGEM) {
        Path carpetaFGEM = archivoFGEM.toAbsolutePath().getParent();
        if (carpetaFGEM != null) {
            try {
                contextoFGEM.configuracionFGEM().guardarFGEM(CLAVE_CARPETA_FGEM, carpetaFGEM.toString());
            } catch (RuntimeException eFGEM) {
                // Recordar la carpeta es solo una comodidad.
            }
        }
    }

    private static Path conExtensionFGEM(Path rutaFGEM) {
        String nombreFGEM = rutaFGEM.getFileName().toString();
        return nombreFGEM.toLowerCase().endsWith(".xlsx") ? rutaFGEM : rutaFGEM.resolveSibling(nombreFGEM + ".xlsx");
    }

    private static void abrirFGEM(Path archivoFGEM) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            return;
        }
        TareasFGEM.ejecutarFGEM(() -> {
            Desktop.getDesktop().open(archivoFGEM.toFile());
            return null;
        }, okFGEM -> {
        }, errorFGEM -> NotificadorFGEM.advertenciaFGEM("No se pudo abrir el archivo. Está en " + archivoFGEM + "."));
    }

    private Window ventanaFGEM() {
        return tablaErroresFGEM.getScene().getWindow();
    }

    /** Celda que muestra el texto completo en varias líneas. */
    private final class CeldaAjustadaFGEM extends TableCell<ErrorFilaFGEM, String> {
        private final Label textoFGEM = new Label();

        CeldaAjustadaFGEM() {
            textoFGEM.setWrapText(true);
            textoFGEM.setMinHeight(Region.USE_PREF_SIZE);
            textoFGEM.maxWidthProperty().bind(colProblemaFGEM.widthProperty().subtract(14));
            textoFGEM.prefWidthProperty().bind(colProblemaFGEM.widthProperty().subtract(14));
        }

        @Override
        protected void updateItem(String itemFGEM, boolean vacioFGEM) {
            super.updateItem(itemFGEM, vacioFGEM);
            setText(null);
            setGraphic(vacioFGEM || itemFGEM == null ? null : textoFGEM);
            if (!vacioFGEM && itemFGEM != null) {
                textoFGEM.setText(itemFGEM);
            }
        }
    }
}
