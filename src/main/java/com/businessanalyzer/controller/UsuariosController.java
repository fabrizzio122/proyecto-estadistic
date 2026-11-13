package com.businessanalyzer.controller;

import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.DatosUsuario;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.RegistroAcceso;
import com.businessanalyzer.model.Rol;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.seguridad.PoliticaPassword;
import com.businessanalyzer.service.UsuarioService;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Formularios;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.VistaModulo;
import com.businessanalyzer.util.Fechas;
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

/** Módulo "Usuarios y permisos": cuentas, matriz de roles y registro de accesos. */
public class UsuariosController implements VistaModulo {

    private static final DateTimeFormatter FORMATO_FECHA_ACCESO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AppContext contexto;
    private final ObservableList<Usuario> usuarios = FXCollections.observableArrayList();
    private final FilteredList<Usuario> usuariosFiltrados = new FilteredList<>(usuarios);
    private List<Rol> roles = List.of();

    @FXML private TextField txtBuscar;
    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colUsuario;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colEmail;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, Usuario> colEstado;
    @FXML private TableColumn<Usuario, String> colUltimoAcceso;
    @FXML private Button btnEditar;
    @FXML private MenuButton menuAcciones;
    @FXML private MenuItem itemEstado;
    @FXML private MenuItem itemRestablecer;
    @FXML private MenuItem itemDesbloquear;

    @FXML private VBox listaRoles;
    @FXML private TableView<Permiso> tablaPermisos;

    @FXML private Label lblAccesos;
    @FXML private TableView<RegistroAcceso> tablaAccesos;
    @FXML private TableColumn<RegistroAcceso, String> colAccFecha;
    @FXML private TableColumn<RegistroAcceso, String> colAccUsuario;
    @FXML private TableColumn<RegistroAcceso, String> colAccEvento;
    @FXML private TableColumn<RegistroAcceso, String> colAccResponsable;

    public UsuariosController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        configurarTablaUsuarios();
        configurarTablaAccesos();
        lblAccesos.setText("Últimos " + UsuarioService.LIMITE_REGISTRO_ACCESOS
                + " eventos de seguridad, del más reciente al más antiguo.");
    }

    @Override
    public void alMostrar() {
        cargarTodo();
    }

    // ------------------------------------------------------------------ carga de datos

    private void cargarTodo() {
        try {
            roles = servicio().listarRoles();
            cargarUsuarios(null);
            construirMatrizPermisos();
            cargarAccesos();
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    private void cargarUsuarios(Long seleccionarId) {
        usuarios.setAll(servicio().listar());
        if (seleccionarId != null) {
            usuarios.stream().filter(u -> u.id() == seleccionarId).findFirst()
                    .ifPresent(u -> tablaUsuarios.getSelectionModel().select(u));
        }
        actualizarAcciones();
    }

    @FXML
    private void cargarAccesos() {
        try {
            tablaAccesos.getItems().setAll(servicio().listarAccesos());
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    // ------------------------------------------------------------------ acciones

    @FXML
    private void nuevo() {
        TextField txtUsuario = new TextField();
        TextField txtNombre = new TextField();
        TextField txtEmail = new TextField();
        ComboBox<Rol> cmbRol = comboRoles(null);
        PasswordField txtPassword = new PasswordField();
        PasswordField txtConfirmacion = new PasswordField();

        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Usuario", txtUsuario);
        Formularios.fila(grilla, "Nombre completo", txtNombre);
        Formularios.fila(grilla, "Correo (opcional)", txtEmail);
        Formularios.fila(grilla, "Rol", cmbRol);
        Formularios.fila(grilla, "Contraseña temporal", txtPassword);
        Formularios.fila(grilla, "Confirmar contraseña", txtConfirmacion);
        Formularios.nota(grilla, PoliticaPassword.DESCRIPCION
                + " El usuario deberá cambiarla en su primer inicio de sesión.");

        Usuario[] creado = new Usuario[1];
        boolean ok = Dialogos.formulario(ventana(), "Nuevo usuario", null, grilla, "Crear usuario", () ->
                creado[0] = servicio().crear(
                        new DatosUsuario(txtUsuario.getText(), txtNombre.getText(), txtEmail.getText(),
                                idRol(cmbRol)),
                        txtPassword.getText(), txtConfirmacion.getText()));
        if (ok) {
            despuesDeCambio(creado[0].id(), "Usuario \"" + creado[0].nombreUsuario() + "\" creado.");
        }
    }

    @FXML
    private void editar() {
        Usuario usuario = seleccionado();
        if (usuario == null) {
            return;
        }
        TextField txtUsuario = new TextField(usuario.nombreUsuario());
        TextField txtNombre = new TextField(usuario.nombreCompleto());
        TextField txtEmail = new TextField(usuario.email() == null ? "" : usuario.email());
        ComboBox<Rol> cmbRol = comboRoles(usuario.rol());

        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Usuario", txtUsuario);
        Formularios.fila(grilla, "Nombre completo", txtNombre);
        Formularios.fila(grilla, "Correo (opcional)", txtEmail);
        Formularios.fila(grilla, "Rol", cmbRol);

        boolean ok = Dialogos.formulario(ventana(), "Editar usuario", null, grilla, "Guardar cambios", () ->
                servicio().actualizar(usuario.id(),
                        new DatosUsuario(txtUsuario.getText(), txtNombre.getText(), txtEmail.getText(),
                                idRol(cmbRol))));
        if (ok) {
            despuesDeCambio(usuario.id(), "Datos de \"" + usuario.nombreUsuario() + "\" actualizados.");
        }
    }

    @FXML
    private void cambiarEstado() {
        Usuario usuario = seleccionado();
        if (usuario == null) {
            return;
        }
        boolean activar = !usuario.activo();
        String mensaje = activar
                ? "¿Activar la cuenta de \"" + usuario.nombreUsuario() + "\"? Podrá volver a iniciar sesión."
                : "¿Desactivar la cuenta de \"" + usuario.nombreUsuario() + "\"? No podrá iniciar sesión, "
                + "pero se conservará su historial.";
        if (!Dialogos.confirmar(ventana(), activar ? "Activar usuario" : "Desactivar usuario", mensaje)) {
            return;
        }
        ejecutar(() -> servicio().cambiarEstado(usuario.id(), activar),
                activar ? "Cuenta activada." : "Cuenta desactivada.");
    }

    @FXML
    private void restablecerPassword() {
        Usuario usuario = seleccionado();
        if (usuario == null) {
            return;
        }
        PasswordField txtPassword = new PasswordField();
        PasswordField txtConfirmacion = new PasswordField();
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Contraseña temporal", txtPassword);
        Formularios.fila(grilla, "Confirmar contraseña", txtConfirmacion);
        Formularios.nota(grilla, PoliticaPassword.DESCRIPCION);

        boolean ok = Dialogos.formulario(ventana(), "Restablecer contraseña",
                "Asigna una contraseña temporal a \"" + usuario.nombreUsuario()
                        + "\". Deberá cambiarla al iniciar sesión. Si la cuenta estaba bloqueada, se desbloqueará.",
                grilla, "Restablecer",
                () -> servicio().restablecerPassword(usuario.id(), txtPassword.getText(), txtConfirmacion.getText()));
        if (ok) {
            despuesDeCambio(usuario.id(), "Contraseña restablecida. Comunícasela al usuario por un medio seguro.");
        }
    }

    @FXML
    private void desbloquear() {
        Usuario usuario = seleccionado();
        if (usuario != null) {
            ejecutar(() -> servicio().desbloquear(usuario.id()), "Cuenta desbloqueada.");
        }
    }

    private void ejecutar(Supplier<Usuario> accion, String mensajeExito) {
        try {
            Usuario resultado = accion.get();
            despuesDeCambio(resultado.id(), mensajeExito);
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    private void despuesDeCambio(long idSeleccionar, String mensaje) {
        cargarUsuarios(idSeleccionar);
        cargarAccesos();
        Notificador.exito(mensaje);
    }

    // ------------------------------------------------------------------ tablas

    private void configurarTablaUsuarios() {
        colUsuario.setCellValueFactory(c -> texto(c.getValue().nombreUsuario()));
        colNombre.setCellValueFactory(c -> texto(c.getValue().nombreCompleto()));
        colEmail.setCellValueFactory(c -> texto(c.getValue().email() == null ? "" : c.getValue().email()));
        colRol.setCellValueFactory(c -> texto(c.getValue().rol().nombre()));
        colUltimoAcceso.setCellValueFactory(c -> texto(Fechas.paraMostrar(c.getValue().ultimoAcceso())));
        colEstado.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colEstado.setComparator((a, b) -> a.estado(LocalDateTime.now()).compareTo(b.estado(LocalDateTime.now())));
        colEstado.setCellFactory(columna -> new CeldaEstado());

        SortedList<Usuario> ordenados = new SortedList<>(usuariosFiltrados);
        ordenados.comparatorProperty().bind(tablaUsuarios.comparatorProperty());
        tablaUsuarios.setItems(ordenados);
        tablaUsuarios.setPlaceholder(new Label("No hay usuarios que coincidan con la búsqueda."));

        txtBuscar.textProperty().addListener((obs, anterior, texto) -> filtrar(texto));
        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, a, n) -> actualizarAcciones());

        ContextMenu contextual = new ContextMenu();
        MenuItem ctxEditar = new MenuItem("Editar");
        ctxEditar.setOnAction(e -> editar());
        MenuItem ctxEstado = new MenuItem();
        ctxEstado.textProperty().bind(itemEstado.textProperty());
        ctxEstado.setOnAction(e -> cambiarEstado());
        MenuItem ctxRestablecer = new MenuItem("Restablecer contraseña");
        ctxRestablecer.setOnAction(e -> restablecerPassword());
        MenuItem ctxDesbloquear = new MenuItem("Desbloquear cuenta");
        ctxDesbloquear.disableProperty().bind(itemDesbloquear.disableProperty());
        ctxDesbloquear.setOnAction(e -> desbloquear());
        contextual.getItems().addAll(ctxEditar, ctxEstado, ctxRestablecer, ctxDesbloquear);

        tablaUsuarios.setRowFactory(tabla -> {
            TableRow<Usuario> fila = new TableRow<>();
            fila.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(fila.emptyProperty())
                            .then((ContextMenu) null).otherwise(contextual));
            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()) {
                    editar();
                }
            });
            return fila;
        });
    }

    private void configurarTablaAccesos() {
        colAccFecha.setCellValueFactory(c -> texto(c.getValue().fecha().format(FORMATO_FECHA_ACCESO)));
        colAccUsuario.setCellValueFactory(c -> texto(c.getValue().nombreUsuario()));
        colAccEvento.setCellValueFactory(c -> texto(c.getValue().evento().descripcion()));
        colAccResponsable.setCellValueFactory(c ->
                texto(c.getValue().realizadoPor() == null ? "" : c.getValue().realizadoPor()));
        tablaAccesos.setPlaceholder(new Label("Todavía no hay eventos registrados."));
    }

    private void construirMatrizPermisos() {
        listaRoles.getChildren().clear();
        for (Rol rol : roles) {
            Label nombre = new Label(rol.nombre());
            nombre.getStyleClass().add("step-title");
            Label descripcion = new Label("  —  " + rol.descripcion());
            descripcion.getStyleClass().add("muted");
            listaRoles.getChildren().add(new javafx.scene.layout.HBox(nombre, descripcion));
        }

        tablaPermisos.getColumns().clear();
        TableColumn<Permiso, String> colPermiso = new TableColumn<>("Permiso");
        colPermiso.setCellValueFactory(c -> texto(c.getValue().descripcion()));
        colPermiso.setPrefWidth(340);
        colPermiso.setSortable(false);
        tablaPermisos.getColumns().add(colPermiso);

        for (Rol rol : roles) {
            TableColumn<Permiso, String> columna = new TableColumn<>(rol.nombre());
            columna.setCellValueFactory(c -> texto(rol.tiene(c.getValue()) ? "✓" : "—"));
            columna.getStyleClass().add("col-centro");
            columna.setPrefWidth(110);
            columna.setSortable(false);
            tablaPermisos.getColumns().add(columna);
        }
        tablaPermisos.getItems().setAll(Permiso.values());
    }

    // ------------------------------------------------------------------ utilidades

    private void filtrar(String texto) {
        String buscado = texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
        usuariosFiltrados.setPredicate(u -> buscado.isEmpty()
                || u.nombreUsuario().toLowerCase(Locale.ROOT).contains(buscado)
                || u.nombreCompleto().toLowerCase(Locale.ROOT).contains(buscado)
                || (u.email() != null && u.email().toLowerCase(Locale.ROOT).contains(buscado))
                || u.rol().nombre().toLowerCase(Locale.ROOT).contains(buscado));
    }

    private void actualizarAcciones() {
        Usuario usuario = seleccionado();
        boolean sinSeleccion = usuario == null;
        btnEditar.setDisable(sinSeleccion);
        menuAcciones.setDisable(sinSeleccion);
        if (!sinSeleccion) {
            itemEstado.setText(usuario.activo() ? "Desactivar" : "Activar");
            itemDesbloquear.setDisable(!usuario.bloqueadoEn(LocalDateTime.now()));
        }
    }

    private ComboBox<Rol> comboRoles(Rol seleccionado) {
        ComboBox<Rol> combo = new ComboBox<>(FXCollections.observableArrayList(roles));
        combo.setPromptText("Selecciona un rol");
        if (seleccionado != null) {
            roles.stream().filter(r -> r.id() == seleccionado.id()).findFirst().ifPresent(combo::setValue);
        }
        return combo;
    }

    private static long idRol(ComboBox<Rol> combo) {
        return combo.getValue() == null ? 0 : combo.getValue().id();
    }

    private Usuario seleccionado() {
        return tablaUsuarios.getSelectionModel().getSelectedItem();
    }

    private UsuarioService servicio() {
        return contexto.usuarios();
    }

    private Window ventana() {
        return tablaUsuarios.getScene().getWindow();
    }

    private static ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(valor);
    }

    /** Muestra el estado como una insignia de color. */
    private static final class CeldaEstado extends TableCell<Usuario, Usuario> {
        @Override
        protected void updateItem(Usuario usuario, boolean vacio) {
            super.updateItem(usuario, vacio);
            if (vacio || usuario == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            String estado = usuario.estado(LocalDateTime.now());
            Label insignia = new Label(estado);
            insignia.getStyleClass().addAll("badge", "badge-" + estado.toLowerCase(Locale.ROOT));
            setGraphic(insignia);
            setText(null);
        }
    }
}
