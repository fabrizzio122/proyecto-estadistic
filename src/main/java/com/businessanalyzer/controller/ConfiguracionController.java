package com.businessanalyzer.controller;

import com.businessanalyzer.BusinessAnalyzerApp;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.CopiaSeguridad;
import com.businessanalyzer.model.CopiaSeguridad.Contenido;
import com.businessanalyzer.service.CopiaSeguridadService;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.Tareas;
import com.businessanalyzer.ui.VistaModulo;
import com.businessanalyzer.util.Fechas;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
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
public class ConfiguracionController implements VistaModulo {

    private static final String CLAVE_CARPETA_INFORMES = "informes.carpeta";

    private final AppContext contexto;

    @FXML private Label lblUltima;
    @FXML private Label lblCarpeta;
    @FXML private Button btnCrear;
    @FXML private ProgressIndicator progreso;
    @FXML private CheckBox chkAutomatica;
    @FXML private Spinner<Integer> spnConservar;
    @FXML private Button btnRestaurar;
    @FXML private Button btnEliminar;
    @FXML private TableView<CopiaSeguridad> tablaCopias;
    @FXML private TableColumn<CopiaSeguridad, String> colFecha;
    @FXML private TableColumn<CopiaSeguridad, String> colTipo;
    @FXML private TableColumn<CopiaSeguridad, String> colTamano;
    @FXML private TableColumn<CopiaSeguridad, String> colArchivo;
    @FXML private GridPane gridSistema;
    @FXML private Label lblCarpetaInformes;

    public ConfiguracionController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        spnConservar.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, CopiaSeguridadService.CONSERVAR_MAXIMO, CopiaSeguridadService.CONSERVAR_PREDETERMINADO));
        colFecha.setCellValueFactory(c -> texto(Fechas.paraMostrar(c.getValue().fecha())));
        colTipo.setCellValueFactory(c -> texto(c.getValue().tipo().descripcion()));
        colTamano.setCellValueFactory(c -> texto(tamano(c.getValue().bytes())));
        colArchivo.setCellValueFactory(c -> texto(c.getValue().archivo().getFileName().toString()));
        tablaCopias.setPlaceholder(new Label("Todavía no hay copias en la carpeta."));
        tablaCopias.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            btnRestaurar.setDisable(n == null);
            btnEliminar.setDisable(n == null);
        });
        btnRestaurar.setDisable(true);
        btnEliminar.setDisable(true);
    }

    @Override
    public void alMostrar() {
        CopiaSeguridadService copias = contexto.copias();
        chkAutomatica.setSelected(copias.automaticaActiva());
        spnConservar.getValueFactory().setValue(copias.copiasAConservar());
        cargarCopias();
        mostrarSistema();
    }

    // ------------------------------------------------------------------ copias

    private void cargarCopias() {
        Tareas.ejecutar(() -> contexto.copias().listar(),
                lista -> {
                    tablaCopias.getItems().setAll(lista);
                    lblCarpeta.setText("Carpeta: " + contexto.copias().carpeta());
                    lblUltima.setText(lista.isEmpty()
                            ? "Aún no hay copias de seguridad."
                            : Fechas.paraMostrar(lista.get(0).fecha()) + "  ·  " + lista.get(0).tipo().descripcion()
                            + "  ·  " + tamano(lista.get(0).bytes()));
                },
                error -> Notificador.error(Errores.mensaje(error)));
    }

    @FXML
    private void crearCopia() {
        btnCrear.setDisable(true);
        progreso.setVisible(true);
        Tareas.ejecutar(() -> contexto.copias().crearManual(),
                copia -> {
                    btnCrear.setDisable(false);
                    progreso.setVisible(false);
                    Notificador.exito("Copia creada: " + copia.archivo().getFileName() + " (" + tamano(copia.bytes()) + ").");
                    cargarCopias();
                },
                error -> {
                    btnCrear.setDisable(false);
                    progreso.setVisible(false);
                    Notificador.error(Errores.mensaje(error));
                });
    }

    @FXML
    private void cambiarCarpeta() {
        DirectoryChooser selector = new DirectoryChooser();
        selector.setTitle("Carpeta para las copias de seguridad");
        Path actual = contexto.copias().carpeta();
        if (Files.isDirectory(actual)) {
            selector.setInitialDirectory(actual.toFile());
        }
        File carpeta = selector.showDialog(ventana());
        if (carpeta == null) {
            return;
        }
        try {
            contexto.copias().cambiarCarpeta(carpeta.toPath());
            Notificador.exito("Las próximas copias se guardarán en " + carpeta + ".");
            cargarCopias();
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    @FXML
    private void abrirCarpetaCopias() {
        Path carpeta = contexto.copias().carpeta();
        try {
            Files.createDirectories(carpeta);
        } catch (IOException e) {
            Notificador.error("No se pudo crear la carpeta " + carpeta + ".");
            return;
        }
        abrir(carpeta);
    }

    @FXML
    private void guardarAutomatica() {
        try {
            spnConservar.increment(0); // confirma lo escrito en el campo
            contexto.copias().configurarAutomatica(chkAutomatica.isSelected(), spnConservar.getValue());
            Notificador.exito("Preferencias de copia automática guardadas.");
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    @FXML
    private void eliminarCopia() {
        CopiaSeguridad copia = tablaCopias.getSelectionModel().getSelectedItem();
        if (copia == null || !Dialogos.confirmar(ventana(), "Eliminar copia",
                "¿Eliminar la copia del " + Fechas.paraMostrar(copia.fecha()) + "? Esta acción no se puede deshacer.")) {
            return;
        }
        try {
            contexto.copias().eliminar(copia);
            Notificador.exito("Copia eliminada.");
            cargarCopias();
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    // ------------------------------------------------------------------ restauración

    @FXML
    private void restaurarSeleccionada() {
        CopiaSeguridad copia = tablaCopias.getSelectionModel().getSelectedItem();
        if (copia != null) {
            restaurar(copia.archivo());
        }
    }

    @FXML
    private void restaurarDesdeArchivo() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Elegir copia de seguridad");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Copias de Business Analyzer (*.db)", "*.db"));
        Path carpeta = contexto.copias().carpeta();
        if (Files.isDirectory(carpeta)) {
            selector.setInitialDirectory(carpeta.toFile());
        }
        File archivo = selector.showOpenDialog(ventana());
        if (archivo != null) {
            restaurar(archivo.toPath());
        }
    }

    private void restaurar(Path origen) {
        Contenido contenido;
        try {
            contenido = contexto.copias().validar(origen);
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }
        String resumen = "La copia «" + origen.getFileName() + "» contiene " + contenido.empresas() + " empresa(s), "
                + contenido.usuarios() + " usuario(s) y " + contenido.operaciones() + " operación(es).\n\n"
                + "Todos los datos actuales se reemplazarán por los de la copia. Antes se guardará una copia del "
                + "estado actual en la carpeta de copias.\n\nLa aplicación se cerrará; al abrirla de nuevo trabajarás "
                + "con los datos restaurados. ¿Continuar?";
        if (!Dialogos.confirmar(ventana(), "Restaurar copia de seguridad", resumen)) {
            return;
        }
        progreso.setVisible(true);
        Tareas.ejecutar(() -> contexto.copias().programarRestauracion(origen),
                ok -> {
                    progreso.setVisible(false);
                    Dialogos.informacion(ventana(), "Restauración preparada",
                            "La aplicación se cerrará ahora. Ábrela de nuevo para continuar con los datos restaurados.",
                            null);
                    Platform.exit();
                },
                error -> {
                    progreso.setVisible(false);
                    Notificador.error(Errores.mensaje(error));
                });
    }

    // ------------------------------------------------------------------ sistema

    private void mostrarSistema() {
        Path datos = contexto.config().directorioDatos();
        Path base = contexto.config().archivoBaseDatos();
        long bytesBase = tamanoArchivo(base) + tamanoArchivo(base.resolveSibling(base.getFileName() + "-wal"));
        gridSistema.getChildren().clear();
        fila(0, "Aplicación", BusinessAnalyzerApp.NOMBRE + " Desktop " + BusinessAnalyzerApp.VERSION);
        fila(1, "Esquema de la base de datos", "Versión " + contexto.versionEsquema());
        fila(2, "Base de datos", base + "  (" + tamano(bytesBase) + ")");
        fila(3, "Carpeta de datos", datos.toString());
        fila(4, "Registros (logs)", datos.resolve("logs").toString());
        fila(5, "Java", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        fila(6, "JavaFX", System.getProperty("javafx.runtime.version", "—"));
        fila(7, "Sistema operativo", System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " (" + System.getProperty("os.arch") + ")");
        lblCarpetaInformes.setText(contexto.configuracion().obtener(CLAVE_CARPETA_INFORMES,
                "Documentos (se recuerda la última carpeta usada)"));
    }

    private void fila(int fila, String etiqueta, String valor) {
        Label nombre = new Label(etiqueta);
        nombre.getStyleClass().add("form-label");
        nombre.setMinWidth(Label.USE_PREF_SIZE);
        Label dato = new Label(valor);
        dato.getStyleClass().add("info-value");
        dato.setWrapText(true);
        gridSistema.addRow(fila, nombre, dato);
    }

    @FXML
    private void abrirCarpetaDatos() {
        abrir(contexto.config().directorioDatos());
    }

    @FXML
    private void abrirRegistros() {
        abrir(contexto.config().directorioDatos().resolve("logs"));
    }

    @FXML
    private void verificarIntegridad() {
        Tareas.ejecutar(() -> contexto.copias().verificarIntegridad(),
                resultado -> {
                    if (resultado.size() == 1 && "ok".equalsIgnoreCase(resultado.get(0))) {
                        Notificador.exito("La base de datos está íntegra.");
                    } else {
                        Dialogos.informacion(ventana(), "Problemas en la base de datos",
                                "La verificación encontró problemas. Restaura una copia de seguridad reciente:\n\n"
                                        + String.join("\n", resultado.subList(0, Math.min(10, resultado.size()))), null);
                    }
                },
                error -> Notificador.error(Errores.mensaje(error)));
    }

    @FXML
    private void cambiarCarpetaInformes() {
        DirectoryChooser selector = new DirectoryChooser();
        selector.setTitle("Carpeta predeterminada de los informes");
        File carpeta = selector.showDialog(ventana());
        if (carpeta != null) {
            contexto.configuracion().guardar(CLAVE_CARPETA_INFORMES, carpeta.getAbsolutePath());
            lblCarpetaInformes.setText(carpeta.getAbsolutePath());
            Notificador.exito("Carpeta de informes actualizada.");
        }
    }

    // ------------------------------------------------------------------ utilidades

    private void abrir(Path ruta) {
        if (!Files.exists(ruta)) {
            Notificador.advertencia("La carpeta todavía no existe: " + ruta);
            return;
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Notificador.advertencia("El sistema no permite abrir carpetas desde la aplicación. Ruta: " + ruta);
            return;
        }
        Tareas.ejecutar(() -> {
                    Desktop.getDesktop().open(ruta.toFile());
                    return null;
                },
                ok -> {
                },
                error -> Notificador.error("No se pudo abrir " + ruta + "."));
    }

    private Window ventana() {
        return tablaCopias.getScene().getWindow();
    }

    static String tamano(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.0f KB", bytes / 1024.0);
        }
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024));
    }

    private static long tamanoArchivo(Path archivo) {
        try {
            return Files.exists(archivo) ? Files.size(archivo) : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    private static ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(valor);
    }
}
