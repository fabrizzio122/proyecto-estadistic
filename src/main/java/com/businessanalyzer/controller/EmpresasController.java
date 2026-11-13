package com.businessanalyzer.controller;

import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.DatosEmpresa;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.TamanoEmpresa;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.service.EmpresaService;
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

import com.businessanalyzer.ui.CamposFecha;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Módulo "Empresas": registro, edición, archivo, acceso de usuarios y empresa activa. */
public class EmpresasController implements VistaModulo {

    private final AppContext contexto;
    private final ObservableList<Empresa> empresas = FXCollections.observableArrayList();
    private final FilteredList<Empresa> filtradas = new FilteredList<>(empresas);

    @FXML private TextField txtBuscar;
    @FXML private CheckBox chkArchivadas;
    @FXML private TableView<Empresa> tabla;
    @FXML private TableColumn<Empresa, String> colNombre;
    @FXML private TableColumn<Empresa, String> colRazon;
    @FXML private TableColumn<Empresa, String> colRuc;
    @FXML private TableColumn<Empresa, String> colSector;
    @FXML private TableColumn<Empresa, String> colMoneda;
    @FXML private TableColumn<Empresa, Empresa> colEstado;
    @FXML private Button btnUsar;
    @FXML private Button btnEditar;
    @FXML private Button btnNueva;
    @FXML private MenuButton menuAcciones;
    @FXML private MenuItem itemEstado;
    @FXML private MenuItem itemAccesos;
    @FXML private Label lblResumen;

    public EmpresasController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        boolean gestiona = contexto.sesion().tienePermiso(Permiso.EMPRESAS_GESTIONAR);
        boolean administraUsuarios = contexto.sesion().tienePermiso(Permiso.USUARIOS_GESTIONAR);
        mostrar(btnNueva, gestiona);
        mostrar(btnEditar, gestiona);
        mostrar(menuAcciones, gestiona || administraUsuarios);
        itemEstado.setVisible(gestiona);
        itemAccesos.setVisible(administraUsuarios);

        colNombre.setCellValueFactory(c -> texto(c.getValue().nombreVisible()));
        colRazon.setCellValueFactory(c -> texto(c.getValue().razonSocial()));
        colRuc.setCellValueFactory(c -> texto(valor(c.getValue().identificacionFiscal())));
        colSector.setCellValueFactory(c -> texto(c.getValue().sector().descripcion()));
        colMoneda.setCellValueFactory(c -> texto(c.getValue().moneda().name()));
        colEstado.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colEstado.setSortable(false);
        colEstado.setCellFactory(columna -> new CeldaEstado());

        SortedList<Empresa> ordenadas = new SortedList<>(filtradas);
        ordenadas.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(ordenadas);
        tabla.setPlaceholder(new Label(gestiona
                ? "No hay empresas registradas. Usa \"Nueva empresa\" para crear la primera."
                : "No tienes empresas asignadas. Pide acceso a un administrador."));

        txtBuscar.textProperty().addListener((obs, anterior, texto) -> filtrar(texto));
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, a, n) -> actualizarAcciones());
        tabla.setRowFactory(t -> {
            TableRow<Empresa> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()) {
                    if (gestiona) {
                        editar();
                    } else {
                        usarSeleccionada();
                    }
                }
            });
            return fila;
        });
        actualizarAcciones();
    }

    @Override
    public void alMostrar() {
        cargar();
    }

    @FXML
    private void cargar() {
        Long seleccion = seleccionada() == null ? null : seleccionada().id();
        try {
            empresas.setAll(servicio().listar(chkArchivadas.isSelected()));
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }
        if (seleccion != null) {
            seleccionar(seleccion);
        }
        long activas = empresas.stream().filter(Empresa::activa).count();
        long archivadas = empresas.size() - activas;
        lblResumen.setText(activas + (activas == 1 ? " empresa activa" : " empresas activas")
                + (chkArchivadas.isSelected()
                ? " y " + archivadas + (archivadas == 1 ? " archivada" : " archivadas")
                : ""));
        tabla.refresh();
        actualizarAcciones();
    }

    // ------------------------------------------------------------------ acciones

    @FXML
    private void nueva() {
        FormularioEmpresa formulario = new FormularioEmpresa(null);
        Empresa[] creada = new Empresa[1];
        boolean ok = Dialogos.formulario(ventana(), "Nueva empresa",
                "Los campos marcados con * son obligatorios.", formulario.contenido(), "Registrar empresa",
                () -> creada[0] = servicio().crear(formulario.datos()));
        if (!ok) {
            return;
        }
        String mensaje = "Empresa \"" + creada[0].nombreVisible() + "\" registrada.";
        if (contexto.sesion().empresaActiva().isEmpty()) {
            servicio().seleccionarEmpresaActiva(creada[0].id());
            mensaje += " Ya puedes trabajar con ella.";
        }
        cargar();
        seleccionar(creada[0].id());
        Notificador.exito(mensaje);
    }

    @FXML
    private void editar() {
        Empresa empresa = seleccionada();
        if (empresa == null || !contexto.sesion().tienePermiso(Permiso.EMPRESAS_GESTIONAR)) {
            return;
        }
        FormularioEmpresa formulario = new FormularioEmpresa(empresa);
        boolean ok = Dialogos.formulario(ventana(), "Editar empresa", null, formulario.contenido(),
                "Guardar cambios", () -> servicio().actualizar(empresa.id(), formulario.datos()));
        if (ok) {
            cargar();
            Notificador.exito("Datos de \"" + empresa.nombreVisible() + "\" actualizados.");
        }
    }

    @FXML
    private void usarSeleccionada() {
        Empresa empresa = seleccionada();
        if (empresa == null) {
            return;
        }
        try {
            servicio().seleccionarEmpresaActiva(empresa.id());
            tabla.refresh();
            actualizarAcciones();
            Notificador.info("Ahora trabajas con " + empresa.nombreVisible() + ".");
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    @FXML
    private void cambiarEstado() {
        Empresa empresa = seleccionada();
        if (empresa == null) {
            return;
        }
        boolean reactivar = !empresa.activa();
        String mensaje = reactivar
                ? "¿Reactivar \"" + empresa.nombreVisible() + "\"? Volverá a aparecer en el selector de empresas."
                : "¿Archivar \"" + empresa.nombreVisible() + "\"? Dejará de aparecer en el selector, "
                + "pero su información se conservará y podrás reactivarla.";
        if (!Dialogos.confirmar(ventana(), reactivar ? "Reactivar empresa" : "Archivar empresa", mensaje)) {
            return;
        }
        try {
            servicio().cambiarEstado(empresa.id(), reactivar);
            cargar();
            Notificador.exito(reactivar ? "Empresa reactivada." : "Empresa archivada.");
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    @FXML
    private void gestionarAccesos() {
        Empresa empresa = seleccionada();
        if (empresa == null) {
            return;
        }
        List<Usuario> candidatos;
        Set<Long> asignados;
        try {
            candidatos = contexto.usuarios().listar().stream()
                    .filter(u -> !u.rol().esAdministrador())
                    .toList();
            asignados = servicio().usuariosConAcceso(empresa.id());
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }

        VBox lista = new VBox(10);
        Map<Long, CheckBox> casillas = candidatos.stream().collect(Collectors.toMap(Usuario::id, u -> {
            CheckBox casilla = new CheckBox(u.nombreCompleto() + "  (" + u.nombreUsuario() + " · "
                    + u.rol().nombre() + (u.activo() ? "" : " · inactivo") + ")");
            casilla.setSelected(asignados.contains(u.id()));
            return casilla;
        }));
        candidatos.forEach(u -> lista.getChildren().add(casillas.get(u.id())));
        if (candidatos.isEmpty()) {
            Label vacio = new Label("Todavía no hay usuarios sin rol de administrador.");
            vacio.getStyleClass().add("muted");
            lista.getChildren().add(vacio);
        }
        ScrollPane desplazable = new ScrollPane(lista);
        desplazable.setFitToWidth(true);
        desplazable.setPrefViewportHeight(Math.min(260, 34.0 * Math.max(1, candidatos.size())));
        desplazable.getStyleClass().add("dialog-scroll");

        boolean ok = Dialogos.formulario(ventana(), "Usuarios con acceso",
                "Elige quién puede trabajar con \"" + empresa.nombreVisible()
                        + "\". Los administradores tienen acceso a todas las empresas.",
                desplazable, "Guardar accesos", () -> {
                    Set<Long> elegidos = new LinkedHashSet<>();
                    casillas.forEach((id, casilla) -> {
                        if (casilla.isSelected()) {
                            elegidos.add(id);
                        }
                    });
                    servicio().asignarAccesos(empresa.id(), elegidos);
                });
        if (ok) {
            Notificador.exito("Accesos de \"" + empresa.nombreVisible() + "\" actualizados.");
        }
    }

    // ------------------------------------------------------------------ utilidades

    private void actualizarAcciones() {
        Empresa empresa = seleccionada();
        boolean sinSeleccion = empresa == null;
        btnEditar.setDisable(sinSeleccion);
        menuAcciones.setDisable(sinSeleccion);
        long activa = contexto.sesion().empresaActiva().map(Empresa::id).orElse(-1L);
        btnUsar.setDisable(sinSeleccion || !empresa.activa() || empresa.id() == activa);
        if (!sinSeleccion) {
            itemEstado.setText(empresa.activa() ? "Archivar" : "Reactivar");
        }
    }

    private void filtrar(String texto) {
        String buscado = texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
        filtradas.setPredicate(e -> buscado.isEmpty()
                || e.nombreVisible().toLowerCase(Locale.ROOT).contains(buscado)
                || e.razonSocial().toLowerCase(Locale.ROOT).contains(buscado)
                || (e.identificacionFiscal() != null && e.identificacionFiscal().toLowerCase(Locale.ROOT).contains(buscado)));
    }

    private void seleccionar(long id) {
        tabla.getItems().stream().filter(e -> e.id() == id).findFirst()
                .ifPresent(e -> tabla.getSelectionModel().select(e));
    }

    private Empresa seleccionada() {
        return tabla.getSelectionModel().getSelectedItem();
    }

    private EmpresaService servicio() {
        return contexto.empresas();
    }

    private Window ventana() {
        return tabla.getScene().getWindow();
    }

    private static void mostrar(javafx.scene.Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private static ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(valor);
    }

    /** Estado: Activa / Archivada, más una marca si es la empresa con la que se trabaja. */
    private final class CeldaEstado extends TableCell<Empresa, Empresa> {
        @Override
        protected void updateItem(Empresa empresa, boolean vacio) {
            super.updateItem(empresa, vacio);
            setText(null);
            if (vacio || empresa == null) {
                setGraphic(null);
                return;
            }
            Label estado = new Label(empresa.activa() ? "Activa" : "Archivada");
            estado.getStyleClass().addAll("badge", empresa.activa() ? "badge-activo" : "badge-inactivo");
            HBox contenido = new HBox(6, estado);
            contenido.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            boolean enUso = contexto.sesion().empresaActiva().map(a -> a.id() == empresa.id()).orElse(false);
            if (enUso) {
                Label actual = new Label("En uso");
                actual.getStyleClass().addAll("badge", "badge-actual");
                contenido.getChildren().add(actual);
            }
            setGraphic(contenido);
        }
    }

    /** Campos del formulario de empresa (alta y edición). */
    private final class FormularioEmpresa {
        private final TextField txtRazon = new TextField();
        private final TextField txtNombre = new TextField();
        private final TextField txtRuc = new TextField();
        private final ComboBox<Sector> cmbSector = new ComboBox<>(FXCollections.observableArrayList(Sector.values()));
        private final ComboBox<TamanoEmpresa> cmbTamano =
                new ComboBox<>(FXCollections.observableArrayList(TamanoEmpresa.values()));
        private final ComboBox<Moneda> cmbMoneda = new ComboBox<>(FXCollections.observableArrayList(Moneda.values()));
        private final ComboBox<Integer> cmbMes =
                new ComboBox<>(FXCollections.observableArrayList(IntStream.rangeClosed(1, 12).boxed().toList()));
        private final DatePicker dpInicio = new DatePicker();
        private final TextField txtDireccion = new TextField();
        private final TextField txtTelefono = new TextField();
        private final TextField txtEmail = new TextField();

        FormularioEmpresa(Empresa e) {
            cmbSector.setPromptText("Selecciona el sector");
            cmbTamano.setPromptText("Selecciona el tamaño");
            cmbMes.setConverter(new StringConverter<>() {
                @Override
                public String toString(Integer mes) {
                    return mes == null ? "" : Fechas.nombreMes(mes);
                }

                @Override
                public Integer fromString(String texto) {
                    return null;
                }
            });
            CamposFecha.configurar(dpInicio);
            txtRuc.setPromptText("Ej.: 20601234567");

            if (e == null) {
                cmbMoneda.setValue(Moneda.PEN);
                cmbMes.setValue(1);
            } else {
                txtRazon.setText(e.razonSocial());
                txtNombre.setText(valor(e.nombreComercial()));
                txtRuc.setText(valor(e.identificacionFiscal()));
                cmbSector.setValue(e.sector());
                cmbTamano.setValue(e.tamano());
                cmbMoneda.setValue(e.moneda());
                cmbMes.setValue(e.mesInicioEjercicio());
                dpInicio.setValue(e.fechaInicioOperaciones());
                txtDireccion.setText(valor(e.direccion()));
                txtTelefono.setText(valor(e.telefono()));
                txtEmail.setText(valor(e.email()));
            }
        }

        GridPane contenido() {
            GridPane grilla = Formularios.grilla();
            Formularios.fila(grilla, "Razón social *", txtRazon);
            Formularios.fila(grilla, "Nombre comercial", txtNombre);
            Formularios.fila(grilla, "RUC / Id. fiscal", txtRuc);
            Formularios.fila(grilla, "Sector *", cmbSector);
            Formularios.fila(grilla, "Tamaño *", cmbTamano);
            Formularios.fila(grilla, "Moneda *", cmbMoneda);
            Formularios.fila(grilla, "Inicio ejercicio fiscal", cmbMes);
            Formularios.fila(grilla, "Inicio de operaciones", dpInicio);
            Formularios.fila(grilla, "Dirección", txtDireccion);
            Formularios.fila(grilla, "Teléfono", txtTelefono);
            Formularios.fila(grilla, "Correo electrónico", txtEmail);
            return grilla;
        }

        DatosEmpresa datos() {
            LocalDate inicio = CamposFecha.leer(dpInicio, "inicio de operaciones");
            return new DatosEmpresa(txtRazon.getText(), txtNombre.getText(), txtRuc.getText(),
                    cmbSector.getValue(), cmbTamano.getValue(), cmbMoneda.getValue(), cmbMes.getValue(),
                    inicio, txtDireccion.getText(), txtTelefono.getText(), txtEmail.getText());
        }
    }
}
