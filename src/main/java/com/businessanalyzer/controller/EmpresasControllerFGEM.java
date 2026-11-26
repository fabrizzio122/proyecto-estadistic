package com.businessanalyzer.controller;

import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.model.DatosEmpresaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.TamanoEmpresaFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.service.EmpresaServiceFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.FormulariosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.FechasFGEM;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import com.businessanalyzer.ui.CamposFechaFGEM;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EmpresasControllerFGEM implements VistaModuloFGEM {

    private final AppContextFGEM contextoFGEM;
    private final ObservableList<EmpresaFGEM> empresasFGEM = FXCollections.observableArrayList();
    private final FilteredList<EmpresaFGEM> filtradasFGEM = new FilteredList<>(empresasFGEM);

    @FXML private TextField txtBuscarFGEM;
    @FXML private CheckBox chkArchivadasFGEM;
    @FXML private TableView<EmpresaFGEM> tablaFGEM;
    @FXML private TableColumn<EmpresaFGEM, String> colNombreFGEM;
    @FXML private TableColumn<EmpresaFGEM, String> colRazonFGEM;
    @FXML private TableColumn<EmpresaFGEM, String> colRucFGEM;
    @FXML private TableColumn<EmpresaFGEM, String> colSectorFGEM;
    @FXML private TableColumn<EmpresaFGEM, String> colMonedaFGEM;
    @FXML private TableColumn<EmpresaFGEM, EmpresaFGEM> colEstadoFGEM;
    @FXML private Button btnUsarFGEM;
    @FXML private Button btnEditarFGEM;
    @FXML private Button btnNuevaFGEM;
    @FXML private javafx.scene.control.MenuButton btnDemoFGEM;
    @FXML private MenuButton menuAccionesFGEM;
    @FXML private MenuItem itemEstadoFGEM;
    @FXML private MenuItem itemAccesosFGEM;
    @FXML private Label lblResumenFGEM;

    public EmpresasControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        boolean gestionaFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.EMPRESAS_GESTIONAR);
        boolean administraUsuariosFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.USUARIOS_GESTIONAR);
        mostrarFGEM(btnNuevaFGEM, gestionaFGEM);
        mostrarFGEM(btnDemoFGEM, gestionaFGEM && contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.OPERACIONES_GESTIONAR)
                && contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.FINANZAS_GESTIONAR)
                && contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.PRODUCTOS_GESTIONAR));
        mostrarFGEM(btnEditarFGEM, gestionaFGEM);
        mostrarFGEM(menuAccionesFGEM, gestionaFGEM || administraUsuariosFGEM);
        itemEstadoFGEM.setVisible(gestionaFGEM);
        itemAccesosFGEM.setVisible(administraUsuariosFGEM);

        colNombreFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().nombreVisibleFGEM()));
        colRazonFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().razonSocialFGEM()));
        colRucFGEM.setCellValueFactory(cFGEM -> textoFGEM(valorFGEM(cFGEM.getValue().identificacionFiscalFGEM())));
        colSectorFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().sectorFGEM().descripcionFGEM()));
        colMonedaFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().monedaFGEM().name()));
        colEstadoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colEstadoFGEM.setSortable(false);
        colEstadoFGEM.setCellFactory(columnaFGEM -> new CeldaEstadoFGEM());

        SortedList<EmpresaFGEM> ordenadasFGEM = new SortedList<>(filtradasFGEM);
        ordenadasFGEM.comparatorProperty().bind(tablaFGEM.comparatorProperty());
        tablaFGEM.setItems(ordenadasFGEM);
        tablaFGEM.setPlaceholder(new Label(gestionaFGEM
                ? "No hay empresas registradas. Usa \"Nueva empresa\" para crear la primera."
                : "No tienes empresas asignadas. Pide acceso a un administrador."));

        txtBuscarFGEM.textProperty().addListener((obsFGEM, anteriorFGEM, textoFGEM) -> filtrarFGEM(textoFGEM));
        tablaFGEM.getSelectionModel().selectedItemProperty().addListener((obsFGEM, aFGEM, nFGEM) -> actualizarAccionesFGEM());
        tablaFGEM.setRowFactory(tFGEM -> {
            TableRow<EmpresaFGEM> filaFGEM = new TableRow<>();
            filaFGEM.setOnMouseClicked(eFGEM -> {
                if (eFGEM.getButton() == MouseButton.PRIMARY && eFGEM.getClickCount() == 2 && !filaFGEM.isEmpty()) {
                    if (gestionaFGEM) {
                        editarFGEM();
                    } else {
                        usarSeleccionadaFGEM();
                    }
                }
            });
            return filaFGEM;
        });
        actualizarAccionesFGEM();
    }

    @Override
    public void alMostrarFGEM() {
        cargarFGEM();
    }

    @FXML
    private void cargarFGEM() {
        Long seleccionFGEM = seleccionadaFGEM() == null ? null : seleccionadaFGEM().idFGEM();
        try {
            empresasFGEM.setAll(servicioFGEM().listarFGEM(chkArchivadasFGEM.isSelected()));
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        if (seleccionFGEM != null) {
            seleccionarFGEM(seleccionFGEM);
        }
        long activasFGEM = empresasFGEM.stream().filter(EmpresaFGEM::activaFGEM).count();
        long archivadasFGEM = empresasFGEM.size() - activasFGEM;
        lblResumenFGEM.setText(activasFGEM + (activasFGEM == 1 ? " empresa activa" : " empresas activas")
                + (chkArchivadasFGEM.isSelected()
                ? " y " + archivadasFGEM + (archivadasFGEM == 1 ? " archivada" : " archivadas")
                : ""));
        tablaFGEM.refresh();
        actualizarAccionesFGEM();
    }

    @FXML
    private void nuevaFGEM() {
        FormularioEmpresaFGEM formularioFGEM = new FormularioEmpresaFGEM(null);
        EmpresaFGEM[] creadaFGEM = new EmpresaFGEM[1];
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Nueva empresa",
                "Los campos marcados con * son obligatorios.", formularioFGEM.contenidoFGEM(), "Registrar empresa",
                () -> creadaFGEM[0] = servicioFGEM().crearFGEM(formularioFGEM.datosFGEM()));
        if (!okFGEM) {
            return;
        }
        String mensajeFGEM = "Empresa \"" + creadaFGEM[0].nombreVisibleFGEM() + "\" registrada.";
        if (contextoFGEM.sesionFGEM().empresaActivaFGEM().isEmpty()) {
            servicioFGEM().seleccionarEmpresaActivaFGEM(creadaFGEM[0].idFGEM());
            mensajeFGEM += " Ya puedes trabajar con ella.";
        }
        cargarFGEM();
        seleccionarFGEM(creadaFGEM[0].idFGEM());
        NotificadorFGEM.exitoFGEM(mensajeFGEM);
    }

    @FXML
    private void crearFundoDemoFGEM() {
        if (!DialogosFGEM.confirmarFGEM(ventanaFGEM(), "Productor agropecuario de demostración",
                "Se creará \"" + com.businessanalyzer.service.DatosDemoAgroServiceFGEM.NOMBRE_FUNDO_FGEM + "\", un productor con "
                        + "vacunos, alpacas y ovinos, con un año de nacimientos, muertes, ventas y producción de leche y "
                        + "fibra, y quedará como empresa activa. Tus otras empresas no se modifican.\n\n¿Continuar?")) {
            return;
        }
        try {
            EmpresaFGEM demoFGEM = contextoFGEM.datosDemoAgroFGEM().crearFundoDemoFGEM();
            cargarFGEM();
            seleccionarFGEM(demoFGEM.idFGEM());
            NotificadorFGEM.exitoFGEM("Empresa \"" + demoFGEM.nombreVisibleFGEM() + "\" creada. Abre «Ganadería» en el menú.");
        } catch (RuntimeException eFGEM) {
            cargarFGEM();
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    @FXML
    private void crearDemoFGEM() {
        if (!DialogosFGEM.confirmarFGEM(ventanaFGEM(), "Empresa de demostración",
                "Se creará \"" + com.businessanalyzer.service.DatosDemoServiceFGEM.NOMBRE_DEMO_FGEM + "\" con productos, "
                        + "compras, ventas, gastos e ingresos ficticios de los últimos 12 meses, y quedará como "
                        + "empresa activa. Tus otras empresas no se modifican.\n\n¿Continuar?")) {
            return;
        }
        javafx.scene.Cursor cursorFGEM = tablaFGEM.getScene().getCursor();
        tablaFGEM.getScene().setCursor(javafx.scene.Cursor.WAIT);
        try {
            EmpresaFGEM demoFGEM = contextoFGEM.datosDemoFGEM().crearEmpresaDemoFGEM();
            cargarFGEM();
            seleccionarFGEM(demoFGEM.idFGEM());
            NotificadorFGEM.exitoFGEM("Empresa \"" + demoFGEM.nombreVisibleFGEM() + "\" creada con 12 meses de datos. "
                    + "Ya puedes recorrer los módulos de análisis.");
        } catch (RuntimeException eFGEM) {
            cargarFGEM();
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        } finally {
            tablaFGEM.getScene().setCursor(cursorFGEM);
        }
    }

    @FXML
    private void editarFGEM() {
        EmpresaFGEM empresaFGEM = seleccionadaFGEM();
        if (empresaFGEM == null || !contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.EMPRESAS_GESTIONAR)) {
            return;
        }
        FormularioEmpresaFGEM formularioFGEM = new FormularioEmpresaFGEM(empresaFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Editar empresa", null, formularioFGEM.contenidoFGEM(),
                "Guardar cambios", () -> servicioFGEM().actualizarFGEM(empresaFGEM.idFGEM(), formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Datos de \"" + empresaFGEM.nombreVisibleFGEM() + "\" actualizados.");
        }
    }

    @FXML
    private void usarSeleccionadaFGEM() {
        EmpresaFGEM empresaFGEM = seleccionadaFGEM();
        if (empresaFGEM == null) {
            return;
        }
        try {
            servicioFGEM().seleccionarEmpresaActivaFGEM(empresaFGEM.idFGEM());
            tablaFGEM.refresh();
            actualizarAccionesFGEM();
            NotificadorFGEM.infoFGEM("Ahora trabajas con " + empresaFGEM.nombreVisibleFGEM() + ".");
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    @FXML
    private void cambiarEstadoFGEM() {
        EmpresaFGEM empresaFGEM = seleccionadaFGEM();
        if (empresaFGEM == null) {
            return;
        }
        boolean reactivarFGEM = !empresaFGEM.activaFGEM();
        String mensajeFGEM = reactivarFGEM
                ? "¿Reactivar \"" + empresaFGEM.nombreVisibleFGEM() + "\"? Volverá a aparecer en el selector de empresas."
                : "¿Archivar \"" + empresaFGEM.nombreVisibleFGEM() + "\"? Dejará de aparecer en el selector, "
                + "pero su información se conservará y podrás reactivarla.";
        if (!DialogosFGEM.confirmarFGEM(ventanaFGEM(), reactivarFGEM ? "Reactivar empresa" : "Archivar empresa", mensajeFGEM)) {
            return;
        }
        try {
            servicioFGEM().cambiarEstadoFGEM(empresaFGEM.idFGEM(), reactivarFGEM);
            cargarFGEM();
            NotificadorFGEM.exitoFGEM(reactivarFGEM ? "Empresa reactivada." : "Empresa archivada.");
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    @FXML
    private void gestionarAccesosFGEM() {
        EmpresaFGEM empresaFGEM = seleccionadaFGEM();
        if (empresaFGEM == null) {
            return;
        }
        List<UsuarioFGEM> candidatosFGEM;
        Set<Long> asignadosFGEM;
        try {
            candidatosFGEM = contextoFGEM.usuariosFGEM().listarFGEM().stream()
                    .filter(uFGEM -> !uFGEM.rolFGEM().esAdministradorFGEM())
                    .toList();
            asignadosFGEM = servicioFGEM().usuariosConAccesoFGEM(empresaFGEM.idFGEM());
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }

        VBox listaFGEM = new VBox(10);
        Map<Long, CheckBox> casillasFGEM = candidatosFGEM.stream().collect(Collectors.toMap(UsuarioFGEM::idFGEM, uFGEM -> {
            CheckBox casillaFGEM = new CheckBox(uFGEM.nombreCompletoFGEM() + "  (" + uFGEM.nombreUsuarioFGEM() + " · "
                    + uFGEM.rolFGEM().nombreFGEM() + (uFGEM.activoFGEM() ? "" : " · inactivo") + ")");
            casillaFGEM.setSelected(asignadosFGEM.contains(uFGEM.idFGEM()));
            return casillaFGEM;
        }));
        candidatosFGEM.forEach(uFGEM -> listaFGEM.getChildren().add(casillasFGEM.get(uFGEM.idFGEM())));
        if (candidatosFGEM.isEmpty()) {
            Label vacioFGEM = new Label("Todavía no hay usuarios sin rol de administrador.");
            vacioFGEM.getStyleClass().add("muted");
            listaFGEM.getChildren().add(vacioFGEM);
        }
        ScrollPane desplazableFGEM = new ScrollPane(listaFGEM);
        desplazableFGEM.setFitToWidth(true);
        desplazableFGEM.setPrefViewportHeight(Math.min(260, 34.0 * Math.max(1, candidatosFGEM.size())));
        desplazableFGEM.getStyleClass().add("dialog-scroll");

        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Usuarios con acceso",
                "Elige quién puede trabajar con \"" + empresaFGEM.nombreVisibleFGEM()
                        + "\". Los administradores tienen acceso a todas las empresas.",
                desplazableFGEM, "Guardar accesos", () -> {
                    Set<Long> elegidosFGEM = new LinkedHashSet<>();
                    casillasFGEM.forEach((idFGEM, casillaFGEM) -> {
                        if (casillaFGEM.isSelected()) {
                            elegidosFGEM.add(idFGEM);
                        }
                    });
                    servicioFGEM().asignarAccesosFGEM(empresaFGEM.idFGEM(), elegidosFGEM);
                });
        if (okFGEM) {
            NotificadorFGEM.exitoFGEM("Accesos de \"" + empresaFGEM.nombreVisibleFGEM() + "\" actualizados.");
        }
    }

    private void actualizarAccionesFGEM() {
        EmpresaFGEM empresaFGEM = seleccionadaFGEM();
        boolean sinSeleccionFGEM = empresaFGEM == null;
        btnEditarFGEM.setDisable(sinSeleccionFGEM);
        menuAccionesFGEM.setDisable(sinSeleccionFGEM);
        long activaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().map(EmpresaFGEM::idFGEM).orElse(-1L);
        btnUsarFGEM.setDisable(sinSeleccionFGEM || !empresaFGEM.activaFGEM() || empresaFGEM.idFGEM() == activaFGEM);
        if (!sinSeleccionFGEM) {
            itemEstadoFGEM.setText(empresaFGEM.activaFGEM() ? "Archivar" : "Reactivar");
        }
    }

    private void filtrarFGEM(String textoFGEM) {
        String buscadoFGEM = textoFGEM == null ? "" : textoFGEM.strip().toLowerCase(Locale.ROOT);
        filtradasFGEM.setPredicate(eFGEM -> buscadoFGEM.isEmpty()
                || eFGEM.nombreVisibleFGEM().toLowerCase(Locale.ROOT).contains(buscadoFGEM)
                || eFGEM.razonSocialFGEM().toLowerCase(Locale.ROOT).contains(buscadoFGEM)
                || (eFGEM.identificacionFiscalFGEM() != null && eFGEM.identificacionFiscalFGEM().toLowerCase(Locale.ROOT).contains(buscadoFGEM)));
    }

    private void seleccionarFGEM(long idFGEM) {
        tablaFGEM.getItems().stream().filter(eFGEM -> eFGEM.idFGEM() == idFGEM).findFirst()
                .ifPresent(eFGEM -> tablaFGEM.getSelectionModel().select(eFGEM));
    }

    private EmpresaFGEM seleccionadaFGEM() {
        return tablaFGEM.getSelectionModel().getSelectedItem();
    }

    private EmpresaServiceFGEM servicioFGEM() {
        return contextoFGEM.empresasFGEM();
    }

    private Window ventanaFGEM() {
        return tablaFGEM.getScene().getWindow();
    }

    private static void mostrarFGEM(javafx.scene.Node nodoFGEM, boolean visibleFGEM) {
        nodoFGEM.setVisible(visibleFGEM);
        nodoFGEM.setManaged(visibleFGEM);
    }

    private static String valorFGEM(String textoFGEM) {
        return textoFGEM == null ? "" : textoFGEM;
    }

    private static ReadOnlyStringWrapper textoFGEM(String valorFGEM) {
        return new ReadOnlyStringWrapper(valorFGEM);
    }

    private final class CeldaEstadoFGEM extends TableCell<EmpresaFGEM, EmpresaFGEM> {
        @Override
        protected void updateItem(EmpresaFGEM empresaFGEM, boolean vacioFGEM) {
            super.updateItem(empresaFGEM, vacioFGEM);
            setText(null);
            if (vacioFGEM || empresaFGEM == null) {
                setGraphic(null);
                return;
            }
            Label estadoFGEM = new Label(empresaFGEM.activaFGEM() ? "Activa" : "Archivada");
            estadoFGEM.getStyleClass().addAll("badge", empresaFGEM.activaFGEM() ? "badge-activo" : "badge-inactivo");
            HBox contenidoFGEM = new HBox(6, estadoFGEM);
            contenidoFGEM.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            boolean enUsoFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().map(aFGEM -> aFGEM.idFGEM() == empresaFGEM.idFGEM()).orElse(false);
            if (enUsoFGEM) {
                Label actualFGEM = new Label("En uso");
                actualFGEM.getStyleClass().addAll("badge", "badge-actual");
                contenidoFGEM.getChildren().add(actualFGEM);
            }
            setGraphic(contenidoFGEM);
        }
    }

    private final class FormularioEmpresaFGEM {
        private final TextField txtRazonFGEM = new TextField();
        private final TextField txtNombreFGEM = new TextField();
        private final TextField txtRucFGEM = new TextField();
        private final ComboBox<SectorFGEM> cmbSectorFGEM = new ComboBox<>(FXCollections.observableArrayList(SectorFGEM.values()));
        private final ComboBox<TamanoEmpresaFGEM> cmbTamanoFGEM =
                new ComboBox<>(FXCollections.observableArrayList(TamanoEmpresaFGEM.values()));
        private final ComboBox<MonedaFGEM> cmbMonedaFGEM = new ComboBox<>(FXCollections.observableArrayList(MonedaFGEM.values()));
        private final ComboBox<Integer> cmbMesFGEM =
                new ComboBox<>(FXCollections.observableArrayList(IntStream.rangeClosed(1, 12).boxed().toList()));
        private final DatePicker dpInicioFGEM = new DatePicker();
        private final TextField txtDireccionFGEM = new TextField();
        private final TextField txtTelefonoFGEM = new TextField();
        private final TextField txtEmailFGEM = new TextField();

        FormularioEmpresaFGEM(EmpresaFGEM eFGEM) {
            cmbSectorFGEM.setPromptText("Selecciona el sector");
            cmbTamanoFGEM.setPromptText("Selecciona el tamaño");
            cmbMesFGEM.setConverter(new StringConverter<>() {
                @Override
                public String toString(Integer mesFGEM) {
                    return mesFGEM == null ? "" : FechasFGEM.nombreMesFGEM(mesFGEM);
                }

                @Override
                public Integer fromString(String textoFGEM) {
                    return null;
                }
            });
            CamposFechaFGEM.configurarFGEM(dpInicioFGEM);
            txtRucFGEM.setPromptText("Ej.: 20601234567");

            if (eFGEM == null) {
                cmbMonedaFGEM.setValue(MonedaFGEM.PEN);
                cmbMesFGEM.setValue(1);
            } else {
                txtRazonFGEM.setText(eFGEM.razonSocialFGEM());
                txtNombreFGEM.setText(valorFGEM(eFGEM.nombreComercialFGEM()));
                txtRucFGEM.setText(valorFGEM(eFGEM.identificacionFiscalFGEM()));
                cmbSectorFGEM.setValue(eFGEM.sectorFGEM());
                cmbTamanoFGEM.setValue(eFGEM.tamanoFGEM());
                cmbMonedaFGEM.setValue(eFGEM.monedaFGEM());
                cmbMesFGEM.setValue(eFGEM.mesInicioEjercicioFGEM());
                dpInicioFGEM.setValue(eFGEM.fechaInicioOperacionesFGEM());
                txtDireccionFGEM.setText(valorFGEM(eFGEM.direccionFGEM()));
                txtTelefonoFGEM.setText(valorFGEM(eFGEM.telefonoFGEM()));
                txtEmailFGEM.setText(valorFGEM(eFGEM.emailFGEM()));
            }
        }

        GridPane contenidoFGEM() {
            GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
            FormulariosFGEM.filaFGEM(grillaFGEM, "Razón social *", txtRazonFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Nombre comercial", txtNombreFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "RUC / Id. fiscal", txtRucFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Sector *", cmbSectorFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Tamaño *", cmbTamanoFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Moneda *", cmbMonedaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Inicio ejercicio fiscal", cmbMesFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Inicio de operaciones", dpInicioFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Dirección", txtDireccionFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Teléfono", txtTelefonoFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Correo electrónico", txtEmailFGEM);
            return grillaFGEM;
        }

        DatosEmpresaFGEM datosFGEM() {
            LocalDate inicioFGEM = CamposFechaFGEM.leerFGEM(dpInicioFGEM, "inicio de operaciones");
            return new DatosEmpresaFGEM(txtRazonFGEM.getText(), txtNombreFGEM.getText(), txtRucFGEM.getText(),
                    cmbSectorFGEM.getValue(), cmbTamanoFGEM.getValue(), cmbMonedaFGEM.getValue(), cmbMesFGEM.getValue(),
                    inicioFGEM, txtDireccionFGEM.getText(), txtTelefonoFGEM.getText(), txtEmailFGEM.getText());
        }
    }
}
