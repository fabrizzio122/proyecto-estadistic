package com.businessanalyzer.controller;

import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.model.DatosUsuarioFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.RegistroAccesoFGEM;
import com.businessanalyzer.model.RolFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.seguridad.PoliticaPasswordFGEM;
import com.businessanalyzer.service.UsuarioServiceFGEM;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class UsuariosControllerFGEM implements VistaModuloFGEM {

    private static final DateTimeFormatter FORMATO_FECHA_ACCESO_FGEM = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AppContextFGEM contextoFGEM;
    private final ObservableList<UsuarioFGEM> usuariosFGEM = FXCollections.observableArrayList();
    private final FilteredList<UsuarioFGEM> usuariosFiltradosFGEM = new FilteredList<>(usuariosFGEM);
    private List<RolFGEM> rolesFGEM = List.of();

    @FXML private TextField txtBuscarFGEM;
    @FXML private TableView<UsuarioFGEM> tablaUsuariosFGEM;
    @FXML private TableColumn<UsuarioFGEM, String> colUsuarioFGEM;
    @FXML private TableColumn<UsuarioFGEM, String> colNombreFGEM;
    @FXML private TableColumn<UsuarioFGEM, String> colEmailFGEM;
    @FXML private TableColumn<UsuarioFGEM, String> colRolFGEM;
    @FXML private TableColumn<UsuarioFGEM, UsuarioFGEM> colEstadoFGEM;
    @FXML private TableColumn<UsuarioFGEM, String> colUltimoAccesoFGEM;
    @FXML private Button btnEditarFGEM;
    @FXML private MenuButton menuAccionesFGEM;
    @FXML private MenuItem itemEstadoFGEM;
    @FXML private MenuItem itemRestablecerFGEM;
    @FXML private MenuItem itemDesbloquearFGEM;

    @FXML private VBox listaRolesFGEM;
    @FXML private TableView<PermisoFGEM> tablaPermisosFGEM;

    @FXML private Label lblAccesosFGEM;
    @FXML private TableView<RegistroAccesoFGEM> tablaAccesosFGEM;
    @FXML private TableColumn<RegistroAccesoFGEM, String> colAccFechaFGEM;
    @FXML private TableColumn<RegistroAccesoFGEM, String> colAccUsuarioFGEM;
    @FXML private TableColumn<RegistroAccesoFGEM, String> colAccEventoFGEM;
    @FXML private TableColumn<RegistroAccesoFGEM, String> colAccResponsableFGEM;

    public UsuariosControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        configurarTablaUsuariosFGEM();
        configurarTablaAccesosFGEM();
        lblAccesosFGEM.setText("Últimos " + UsuarioServiceFGEM.LIMITE_REGISTRO_ACCESOS_FGEM
                + " eventos de seguridad, del más reciente al más antiguo.");
    }

    @Override
    public void alMostrarFGEM() {
        cargarTodoFGEM();
    }

    private void cargarTodoFGEM() {
        try {
            rolesFGEM = servicioFGEM().listarRolesFGEM();
            cargarUsuariosFGEM(null);
            construirMatrizPermisosFGEM();
            cargarAccesosFGEM();
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    private void cargarUsuariosFGEM(Long seleccionarIdFGEM) {
        usuariosFGEM.setAll(servicioFGEM().listarFGEM());
        if (seleccionarIdFGEM != null) {
            usuariosFGEM.stream().filter(uFGEM -> uFGEM.idFGEM() == seleccionarIdFGEM).findFirst()
                    .ifPresent(uFGEM -> tablaUsuariosFGEM.getSelectionModel().select(uFGEM));
        }
        actualizarAccionesFGEM();
    }

    @FXML
    private void cargarAccesosFGEM() {
        try {
            tablaAccesosFGEM.getItems().setAll(servicioFGEM().listarAccesosFGEM());
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    @FXML
    private void nuevoFGEM() {
        TextField txtUsuarioFGEM = new TextField();
        TextField txtNombreFGEM = new TextField();
        TextField txtEmailFGEM = new TextField();
        ComboBox<RolFGEM> cmbRolFGEM = comboRolesFGEM(null);
        PasswordField txtPasswordFGEM = new PasswordField();
        PasswordField txtConfirmacionFGEM = new PasswordField();

        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Usuario", txtUsuarioFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Nombre completo", txtNombreFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Correo (opcional)", txtEmailFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Rol", cmbRolFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Contraseña temporal", txtPasswordFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Confirmar contraseña", txtConfirmacionFGEM);
        FormulariosFGEM.notaFGEM(grillaFGEM, PoliticaPasswordFGEM.DESCRIPCION_FGEM
                + " El usuario deberá cambiarla en su primer inicio de sesión.");

        UsuarioFGEM[] creadoFGEM = new UsuarioFGEM[1];
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Nuevo usuario", null, grillaFGEM, "Crear usuario", () ->
                creadoFGEM[0] = servicioFGEM().crearFGEM(
                        new DatosUsuarioFGEM(txtUsuarioFGEM.getText(), txtNombreFGEM.getText(), txtEmailFGEM.getText(),
                                idRolFGEM(cmbRolFGEM)),
                        txtPasswordFGEM.getText(), txtConfirmacionFGEM.getText()));
        if (okFGEM) {
            despuesDeCambioFGEM(creadoFGEM[0].idFGEM(), "Usuario \"" + creadoFGEM[0].nombreUsuarioFGEM() + "\" creado.");
        }
    }

    @FXML
    private void editarFGEM() {
        UsuarioFGEM usuarioFGEM = seleccionadoFGEM();
        if (usuarioFGEM == null) {
            return;
        }
        TextField txtUsuarioFGEM = new TextField(usuarioFGEM.nombreUsuarioFGEM());
        TextField txtNombreFGEM = new TextField(usuarioFGEM.nombreCompletoFGEM());
        TextField txtEmailFGEM = new TextField(usuarioFGEM.emailFGEM() == null ? "" : usuarioFGEM.emailFGEM());
        ComboBox<RolFGEM> cmbRolFGEM = comboRolesFGEM(usuarioFGEM.rolFGEM());

        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Usuario", txtUsuarioFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Nombre completo", txtNombreFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Correo (opcional)", txtEmailFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Rol", cmbRolFGEM);

        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Editar usuario", null, grillaFGEM, "Guardar cambios", () ->
                servicioFGEM().actualizarFGEM(usuarioFGEM.idFGEM(),
                        new DatosUsuarioFGEM(txtUsuarioFGEM.getText(), txtNombreFGEM.getText(), txtEmailFGEM.getText(),
                                idRolFGEM(cmbRolFGEM))));
        if (okFGEM) {
            despuesDeCambioFGEM(usuarioFGEM.idFGEM(), "Datos de \"" + usuarioFGEM.nombreUsuarioFGEM() + "\" actualizados.");
        }
    }

    @FXML
    private void cambiarEstadoFGEM() {
        UsuarioFGEM usuarioFGEM = seleccionadoFGEM();
        if (usuarioFGEM == null) {
            return;
        }
        boolean activarFGEM = !usuarioFGEM.activoFGEM();
        String mensajeFGEM = activarFGEM
                ? "¿Activar la cuenta de \"" + usuarioFGEM.nombreUsuarioFGEM() + "\"? Podrá volver a iniciar sesión."
                : "¿Desactivar la cuenta de \"" + usuarioFGEM.nombreUsuarioFGEM() + "\"? No podrá iniciar sesión, "
                + "pero se conservará su historial.";
        if (!DialogosFGEM.confirmarFGEM(ventanaFGEM(), activarFGEM ? "Activar usuario" : "Desactivar usuario", mensajeFGEM)) {
            return;
        }
        ejecutarFGEM(() -> servicioFGEM().cambiarEstadoFGEM(usuarioFGEM.idFGEM(), activarFGEM),
                activarFGEM ? "Cuenta activada." : "Cuenta desactivada.");
    }

    @FXML
    private void restablecerPasswordFGEM() {
        UsuarioFGEM usuarioFGEM = seleccionadoFGEM();
        if (usuarioFGEM == null) {
            return;
        }
        PasswordField txtPasswordFGEM = new PasswordField();
        PasswordField txtConfirmacionFGEM = new PasswordField();
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Contraseña temporal", txtPasswordFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Confirmar contraseña", txtConfirmacionFGEM);
        FormulariosFGEM.notaFGEM(grillaFGEM, PoliticaPasswordFGEM.DESCRIPCION_FGEM);

        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Restablecer contraseña",
                "Asigna una contraseña temporal a \"" + usuarioFGEM.nombreUsuarioFGEM()
                        + "\". Deberá cambiarla al iniciar sesión. Si la cuenta estaba bloqueada, se desbloqueará.",
                grillaFGEM, "Restablecer",
                () -> servicioFGEM().restablecerPasswordFGEM(usuarioFGEM.idFGEM(), txtPasswordFGEM.getText(), txtConfirmacionFGEM.getText()));
        if (okFGEM) {
            despuesDeCambioFGEM(usuarioFGEM.idFGEM(), "Contraseña restablecida. Comunícasela al usuario por un medio seguro.");
        }
    }

    @FXML
    private void desbloquearFGEM() {
        UsuarioFGEM usuarioFGEM = seleccionadoFGEM();
        if (usuarioFGEM != null) {
            ejecutarFGEM(() -> servicioFGEM().desbloquearFGEM(usuarioFGEM.idFGEM()), "Cuenta desbloqueada.");
        }
    }

    private void ejecutarFGEM(Supplier<UsuarioFGEM> accionFGEM, String mensajeExitoFGEM) {
        try {
            UsuarioFGEM resultadoFGEM = accionFGEM.get();
            despuesDeCambioFGEM(resultadoFGEM.idFGEM(), mensajeExitoFGEM);
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    private void despuesDeCambioFGEM(long idSeleccionarFGEM, String mensajeFGEM) {
        cargarUsuariosFGEM(idSeleccionarFGEM);
        cargarAccesosFGEM();
        NotificadorFGEM.exitoFGEM(mensajeFGEM);
    }

    private void configurarTablaUsuariosFGEM() {
        colUsuarioFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().nombreUsuarioFGEM()));
        colNombreFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().nombreCompletoFGEM()));
        colEmailFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().emailFGEM() == null ? "" : cFGEM.getValue().emailFGEM()));
        colRolFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().rolFGEM().nombreFGEM()));
        colUltimoAccesoFGEM.setCellValueFactory(cFGEM -> textoFGEM(FechasFGEM.paraMostrarFGEM(cFGEM.getValue().ultimoAccesoFGEM())));
        colEstadoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colEstadoFGEM.setComparator((aFGEM, bFGEM) -> aFGEM.estadoFGEM(LocalDateTime.now()).compareTo(bFGEM.estadoFGEM(LocalDateTime.now())));
        colEstadoFGEM.setCellFactory(columnaFGEM -> new CeldaEstadoFGEM());

        SortedList<UsuarioFGEM> ordenadosFGEM = new SortedList<>(usuariosFiltradosFGEM);
        ordenadosFGEM.comparatorProperty().bind(tablaUsuariosFGEM.comparatorProperty());
        tablaUsuariosFGEM.setItems(ordenadosFGEM);
        tablaUsuariosFGEM.setPlaceholder(new Label("No hay usuarios que coincidan con la búsqueda."));

        txtBuscarFGEM.textProperty().addListener((obsFGEM, anteriorFGEM, textoFGEM) -> filtrarFGEM(textoFGEM));
        tablaUsuariosFGEM.getSelectionModel().selectedItemProperty().addListener((obsFGEM, aFGEM, nFGEM) -> actualizarAccionesFGEM());

        ContextMenu contextualFGEM = new ContextMenu();
        MenuItem ctxEditarFGEM = new MenuItem("Editar");
        ctxEditarFGEM.setOnAction(eFGEM -> editarFGEM());
        MenuItem ctxEstadoFGEM = new MenuItem();
        ctxEstadoFGEM.textProperty().bind(itemEstadoFGEM.textProperty());
        ctxEstadoFGEM.setOnAction(eFGEM -> cambiarEstadoFGEM());
        MenuItem ctxRestablecerFGEM = new MenuItem("Restablecer contraseña");
        ctxRestablecerFGEM.setOnAction(eFGEM -> restablecerPasswordFGEM());
        MenuItem ctxDesbloquearFGEM = new MenuItem("Desbloquear cuenta");
        ctxDesbloquearFGEM.disableProperty().bind(itemDesbloquearFGEM.disableProperty());
        ctxDesbloquearFGEM.setOnAction(eFGEM -> desbloquearFGEM());
        contextualFGEM.getItems().addAll(ctxEditarFGEM, ctxEstadoFGEM, ctxRestablecerFGEM, ctxDesbloquearFGEM);

        tablaUsuariosFGEM.setRowFactory(tablaFGEM -> {
            TableRow<UsuarioFGEM> filaFGEM = new TableRow<>();
            filaFGEM.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(filaFGEM.emptyProperty())
                            .then((ContextMenu) null).otherwise(contextualFGEM));
            filaFGEM.setOnMouseClicked(eFGEM -> {
                if (eFGEM.getButton() == MouseButton.PRIMARY && eFGEM.getClickCount() == 2 && !filaFGEM.isEmpty()) {
                    editarFGEM();
                }
            });
            return filaFGEM;
        });
    }

    private void configurarTablaAccesosFGEM() {
        colAccFechaFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().fechaFGEM().format(FORMATO_FECHA_ACCESO_FGEM)));
        colAccUsuarioFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().nombreUsuarioFGEM()));
        colAccEventoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().eventoFGEM().descripcionFGEM()));
        colAccResponsableFGEM.setCellValueFactory(cFGEM ->
                textoFGEM(cFGEM.getValue().realizadoPorFGEM() == null ? "" : cFGEM.getValue().realizadoPorFGEM()));
        tablaAccesosFGEM.setPlaceholder(new Label("Todavía no hay eventos registrados."));
    }

    private void construirMatrizPermisosFGEM() {
        listaRolesFGEM.getChildren().clear();
        for (RolFGEM rolFGEM : rolesFGEM) {
            Label nombreFGEM = new Label(rolFGEM.nombreFGEM());
            nombreFGEM.getStyleClass().add("step-title");
            Label descripcionFGEM = new Label("  —  " + rolFGEM.descripcionFGEM());
            descripcionFGEM.getStyleClass().add("muted");
            listaRolesFGEM.getChildren().add(new javafx.scene.layout.HBox(nombreFGEM, descripcionFGEM));
        }

        tablaPermisosFGEM.getColumns().clear();
        TableColumn<PermisoFGEM, String> colPermisoFGEM = new TableColumn<>("Permiso");
        colPermisoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().descripcionFGEM()));
        colPermisoFGEM.setPrefWidth(340);
        colPermisoFGEM.setSortable(false);
        tablaPermisosFGEM.getColumns().add(colPermisoFGEM);

        for (RolFGEM rolFGEM : rolesFGEM) {
            TableColumn<PermisoFGEM, String> columnaFGEM = new TableColumn<>(rolFGEM.nombreFGEM());
            columnaFGEM.setCellValueFactory(cFGEM -> textoFGEM(rolFGEM.tieneFGEM(cFGEM.getValue()) ? "✓" : "—"));
            columnaFGEM.getStyleClass().add("col-centro");
            columnaFGEM.setPrefWidth(110);
            columnaFGEM.setSortable(false);
            tablaPermisosFGEM.getColumns().add(columnaFGEM);
        }
        tablaPermisosFGEM.getItems().setAll(PermisoFGEM.values());
    }

    private void filtrarFGEM(String textoFGEM) {
        String buscadoFGEM = textoFGEM == null ? "" : textoFGEM.strip().toLowerCase(Locale.ROOT);
        usuariosFiltradosFGEM.setPredicate(uFGEM -> buscadoFGEM.isEmpty()
                || uFGEM.nombreUsuarioFGEM().toLowerCase(Locale.ROOT).contains(buscadoFGEM)
                || uFGEM.nombreCompletoFGEM().toLowerCase(Locale.ROOT).contains(buscadoFGEM)
                || (uFGEM.emailFGEM() != null && uFGEM.emailFGEM().toLowerCase(Locale.ROOT).contains(buscadoFGEM))
                || uFGEM.rolFGEM().nombreFGEM().toLowerCase(Locale.ROOT).contains(buscadoFGEM));
    }

    private void actualizarAccionesFGEM() {
        UsuarioFGEM usuarioFGEM = seleccionadoFGEM();
        boolean sinSeleccionFGEM = usuarioFGEM == null;
        btnEditarFGEM.setDisable(sinSeleccionFGEM);
        menuAccionesFGEM.setDisable(sinSeleccionFGEM);
        if (!sinSeleccionFGEM) {
            itemEstadoFGEM.setText(usuarioFGEM.activoFGEM() ? "Desactivar" : "Activar");
            itemDesbloquearFGEM.setDisable(!usuarioFGEM.bloqueadoEnFGEM(LocalDateTime.now()));
        }
    }

    private ComboBox<RolFGEM> comboRolesFGEM(RolFGEM seleccionadoFGEM) {
        ComboBox<RolFGEM> comboFGEM = new ComboBox<>(FXCollections.observableArrayList(rolesFGEM));
        comboFGEM.setPromptText("Selecciona un rol");
        if (seleccionadoFGEM != null) {
            rolesFGEM.stream().filter(rFGEM -> rFGEM.idFGEM() == seleccionadoFGEM.idFGEM()).findFirst().ifPresent(comboFGEM::setValue);
        }
        return comboFGEM;
    }

    private static long idRolFGEM(ComboBox<RolFGEM> comboFGEM) {
        return comboFGEM.getValue() == null ? 0 : comboFGEM.getValue().idFGEM();
    }

    private UsuarioFGEM seleccionadoFGEM() {
        return tablaUsuariosFGEM.getSelectionModel().getSelectedItem();
    }

    private UsuarioServiceFGEM servicioFGEM() {
        return contextoFGEM.usuariosFGEM();
    }

    private Window ventanaFGEM() {
        return tablaUsuariosFGEM.getScene().getWindow();
    }

    private static ReadOnlyStringWrapper textoFGEM(String valorFGEM) {
        return new ReadOnlyStringWrapper(valorFGEM);
    }

    private static final class CeldaEstadoFGEM extends TableCell<UsuarioFGEM, UsuarioFGEM> {
        @Override
        protected void updateItem(UsuarioFGEM usuarioFGEM, boolean vacioFGEM) {
            super.updateItem(usuarioFGEM, vacioFGEM);
            if (vacioFGEM || usuarioFGEM == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            String estadoFGEM = usuarioFGEM.estadoFGEM(LocalDateTime.now());
            Label insigniaFGEM = new Label(estadoFGEM);
            insigniaFGEM.getStyleClass().addAll("badge", "badge-" + estadoFGEM.toLowerCase(Locale.ROOT));
            setGraphic(insigniaFGEM);
            setText(null);
        }
    }
}
