package com.businessanalyzer.controller;

import com.businessanalyzer.BusinessAnalyzerApp;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.service.ConfiguracionService;
import com.businessanalyzer.ui.DialogoCambioPassword;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Modulo;
import com.businessanalyzer.ui.Navegador;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.Ventana;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ventana principal: menu lateral (solo con los modulos permitidos al usuario), barra
 * superior con el selector de empresa activa y el menu de usuario, area de contenido y
 * barra de estado.
 */
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);
    private static final String SIN_EMPRESA = "Sin empresa seleccionada";

    private final AppContext contexto;
    private final Ventana ventana;
    private final ToggleGroup grupoMenu = new ToggleGroup();
    private final Map<Modulo, ToggleButton> botones = new EnumMap<>(Modulo.class);
    private final Map<Modulo.Seccion, Label> encabezados = new EnumMap<>(Modulo.Seccion.class);
    private Navegador navegador;
    private Modulo moduloActual;

    @FXML private VBox menu;
    @FXML private StackPane contenido;
    @FXML private Label tituloModulo;
    @FXML private Label descripcionModulo;
    @FXML private Label estadoBaseDatos;
    @FXML private Label estadoVersion;
    @FXML private MenuButton menuUsuario;
    @FXML private MenuButton selectorEmpresa;
    @FXML private Label kickerModulo;
    @FXML private Label chipPeriodo;
    @FXML private Button btnAccionPrincipal;
    @FXML private HBox franjaFrase;
    @FXML private Label lblFrase;

    /** Contador de alertas pendientes junto a «Diagnóstico empresarial». */
    private final Label contadorAlertas = new Label();

    public MainController(AppContext contexto, Ventana ventana) {
        this.contexto = contexto;
        contexto.alAbrirModulo(m -> {
            if (botones.containsKey(m) && botones.get(m).isVisible() && !botones.get(m).isDisabled()) {
                abrir(m);
            }
        });
        contexto.alActualizarAlertas(this::mostrarAlertas);
        contexto.alMostrarFrase(this::mostrarFrase);
        this.ventana = ventana;
    }

    @FXML
    private void initialize() {
        Usuario usuario = contexto.sesion().usuario();
        navegador = new Navegador(contenido, contexto);
        construirMenu();
        construirMenuUsuario(usuario);
        prepararSelectorEmpresa();

        estadoBaseDatos.setText("Base de datos: " + contexto.config().archivoBaseDatos()
                + "   ·   Esquema v" + contexto.versionEsquema());
        estadoVersion.setText(BusinessAnalyzerApp.NOMBRE + " " + BusinessAnalyzerApp.VERSION);

        // Evita que el menu quede sin opcion marcada al pulsar la opcion activa.
        grupoMenu.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true);
            }
        });

        aplicarSector(contexto.sesion().empresaActiva());
        String ultimo = contexto.configuracion().obtener(claveUltimoModulo(usuario), "");
        Modulo.desdeNombre(ultimo)
                .filter(botones::containsKey)
                .filter(m -> botones.get(m).isVisible())
                .or(() -> botones.keySet().stream().filter(m -> botones.get(m).isVisible()).findFirst())
                .ifPresent(this::abrir);
    }

    // ------------------------------------------------------------------ menu lateral

    private void construirMenu() {
        Modulo.Seccion seccionActual = null;
        for (Modulo modulo : Modulo.values()) {
            if (!contexto.sesion().tienePermiso(modulo.permiso())) {
                continue;
            }
            if (modulo.seccion() != seccionActual) {
                seccionActual = modulo.seccion();
                Label encabezado = new Label(seccionActual.titulo().toUpperCase());
                encabezado.getStyleClass().add("sidebar-section");
                encabezados.put(seccionActual, encabezado);
                menu.getChildren().add(encabezado);
            }
            ToggleButton boton = new ToggleButton(modulo.titulo(), new FontIcon(modulo.icono()));
            boton.setToggleGroup(grupoMenu);
            boton.getStyleClass().add("sidebar-item");
            boton.setMaxWidth(Double.MAX_VALUE);
            boton.setAlignment(Pos.CENTER_LEFT);
            boton.setWrapText(true);
            if (!modulo.disponible()) {
                boton.setTooltip(new Tooltip("Disponible en la Fase " + modulo.fase()));
            }
            boton.setOnAction(e -> abrir(modulo));
            botones.put(modulo, boton);
            if (modulo == Modulo.DIAGNOSTICO) {
                // El contador va encima del botón, a la derecha, y no intercepta los clics.
                contadorAlertas.getStyleClass().add("contador-alertas");
                contadorAlertas.setVisible(false);
                contadorAlertas.setMouseTransparent(true);
                StackPane conContador = new StackPane(boton, contadorAlertas);
                conContador.getStyleClass().add("con-contador");
                StackPane.setAlignment(contadorAlertas, Pos.CENTER_RIGHT);
                StackPane.setMargin(contadorAlertas, new javafx.geometry.Insets(0, 10, 0, 0));
                boton.managedProperty().addListener((o, a, gestionado) -> conContador.setManaged(gestionado));
                boton.visibleProperty().addListener((o, a, visible) -> conContador.setVisible(visible));
                menu.getChildren().add(conContador);
            } else {
                menu.getChildren().add(boton);
            }
        }
    }

    private void mostrarFrase(Modulo modulo, String frase) {
        boolean mostrar = frase != null && !frase.isBlank() && modulo != null && modulo == moduloActual;
        if (modulo != null && modulo != moduloActual) {
            return;
        }
        lblFrase.setText(mostrar ? frase : "");
        visible(franjaFrase, mostrar);
        if (mostrar) {
            com.businessanalyzer.ui.Animaciones.aparecer(franjaFrase, 6, javafx.util.Duration.millis(350));
        }
    }

    private void mostrarAlertas(int cantidad) {
        contadorAlertas.setText(String.valueOf(cantidad));
        contadorAlertas.setVisible(cantidad > 0);
        Tooltip.install(contadorAlertas, new Tooltip(cantidad == 1 ? "1 indicador por atender"
                : cantidad + " indicadores por atender"));
    }

    private void abrir(Modulo modulo) {
        botones.get(modulo).setSelected(true);
        mostrarFrase(null, null);
        mostrarEncabezado(modulo);
        try {
            navegador.mostrar(modulo);
            moduloActual = modulo;
            com.businessanalyzer.ui.Estilos.encabezadosTabla(contenido);
        } catch (Exception e) {
            log.error("No se pudo abrir el modulo {}", modulo, e);
            Notificador.error("No se pudo abrir el módulo " + modulo.titulo());
            return;
        }
        recordarUltimoModulo(modulo);
    }

    /**
     * Encabezado de la pantalla. En el Dashboard: la empresa en una línea tenue, un saludo según la hora,
     * el período del mes en curso y el botón «Registrar venta». En los demás módulos: título y descripción.
     */
    private void mostrarEncabezado(Modulo modulo) {
        boolean inicio = modulo == Modulo.DASHBOARD;
        Optional<Empresa> empresa = contexto.sesion().empresaActiva();
        visible(kickerModulo, inicio && empresa.isPresent());
        visible(chipPeriodo, inicio && empresa.isPresent());
        visible(descripcionModulo, !inicio);
        boolean puedeVender = inicio && empresa.isPresent()
                && contexto.sesion().tienePermiso(com.businessanalyzer.model.Permiso.OPERACIONES_GESTIONAR)
                && botones.containsKey(Modulo.VENTAS_COMPRAS);
        visible(btnAccionPrincipal, puedeVender);
        if (!inicio) {
            tituloModulo.setText(modulo.titulo());
            descripcionModulo.setText(modulo.descripcion());
            return;
        }
        String nombre = contexto.sesion().usuario().nombreCompleto().strip().split("\\s+")[0];
        int hora = java.time.LocalTime.now().getHour();
        tituloModulo.setText((hora < 12 ? "Buenos días" : hora < 19 ? "Buenas tardes" : "Buenas noches") + ", " + nombre);
        empresa.ifPresent(e -> kickerModulo.setText(e.nombreVisible() + "  ·  " + e.tamano().descripcion()
                + "  ·  " + e.moneda().nombre() + " (" + e.moneda().simbolo() + ")"));
        java.time.LocalDate hoy = java.time.LocalDate.now();
        chipPeriodo.setText("1 – " + hoy.getDayOfMonth() + " de "
                + com.businessanalyzer.util.Fechas.nombreMes(hoy.getMonthValue()).toLowerCase() + " " + hoy.getYear());
        btnAccionPrincipal.setText("Registrar venta");
        btnAccionPrincipal.setOnAction(e -> registrarVenta());
    }

    /** Abre «Ventas y compras» con el formulario de nueva venta. */
    private void registrarVenta() {
        abrir(Modulo.VENTAS_COMPRAS);
        javafx.application.Platform.runLater(() -> {
            if (contenido.lookup("#btnNueva") instanceof javafx.scene.control.Button nueva && !nueva.isDisabled()) {
                nueva.fire();
            }
        });
    }

    private static void visible(javafx.scene.Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }

    // ------------------------------------------------------------------ empresa activa

    private void prepararSelectorEmpresa() {
        try {
            contexto.empresas().restaurarEmpresaActiva();
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
        mostrarEmpresa(contexto.sesion().empresaActiva());
        contexto.sesion().alCambiarEmpresa(this::empresaCambiada);
        // La lista se arma cada vez que se abre, así siempre refleja los últimos cambios.
        selectorEmpresa.setOnShowing(e -> construirSelectorEmpresa());
        construirSelectorEmpresa();
    }

    private void construirSelectorEmpresa() {
        selectorEmpresa.getItems().clear();
        List<Empresa> disponibles;
        try {
            disponibles = contexto.empresas().listarParaSelector();
        } catch (RuntimeException e) {
            disponibles = List.of();
        }

        ToggleGroup grupo = new ToggleGroup();
        long activa = contexto.sesion().empresaActiva().map(Empresa::id).orElse(-1L);
        for (Empresa empresa : disponibles) {
            RadioMenuItem item = new RadioMenuItem(empresa.nombreVisible());
            item.setToggleGroup(grupo);
            item.setSelected(empresa.id() == activa);
            item.setOnAction(e -> seleccionarEmpresa(empresa));
            selectorEmpresa.getItems().add(item);
        }
        if (disponibles.isEmpty()) {
            MenuItem vacio = new MenuItem("No tienes empresas disponibles");
            vacio.setDisable(true);
            selectorEmpresa.getItems().add(vacio);
        }
        if (botones.containsKey(Modulo.EMPRESAS)) {
            MenuItem administrar = new MenuItem("Administrar empresas", new FontIcon("fth-settings"));
            administrar.setOnAction(e -> abrir(Modulo.EMPRESAS));
            selectorEmpresa.getItems().addAll(new SeparatorMenuItem(), administrar);
        }
    }

    private void seleccionarEmpresa(Empresa empresa) {
        try {
            Empresa activa = contexto.empresas().seleccionarEmpresaActiva(empresa.id());
            Notificador.info("Ahora trabajas con " + activa.nombreVisible() + ".");
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    private void empresaCambiada(Optional<Empresa> empresa) {
        mostrarEmpresa(empresa);
        mostrarAlertas(0);
        mostrarFrase(null, null);
        if (moduloActual != null) {
            mostrarEncabezado(moduloActual);
        }
        if (aplicarSector(empresa)) {
            return;
        }
        if (moduloActual != null) {
            navegador.alCambiarEmpresa(moduloActual);
        }
    }

    /**
     * Muestra la sección «Agropecuario» del menú solo si la empresa activa es de ese sector.
     *
     * @return {@code true} si el módulo abierto dejó de estar disponible y se abrió otro
     */
    private boolean aplicarSector(Optional<Empresa> empresa) {
        boolean agro = empresa.map(e -> e.sector() == Sector.AGROPECUARIO).orElse(false);
        botones.forEach((modulo, boton) -> {
            if (modulo.soloAgropecuario()) {
                boton.setVisible(agro);
                boton.setManaged(agro);
            }
        });
        Label encabezado = encabezados.get(Modulo.Seccion.AGRO);
        if (encabezado != null) {
            encabezado.setVisible(agro);
            encabezado.setManaged(agro);
        }
        if (!agro && moduloActual != null && moduloActual.soloAgropecuario()) {
            botones.keySet().stream().filter(m -> !m.soloAgropecuario()).findFirst().ifPresent(this::abrir);
            return true;
        }
        return false;
    }

    private void mostrarEmpresa(Optional<Empresa> empresa) {
        selectorEmpresa.setText(empresa.map(Empresa::nombreVisible).orElse(SIN_EMPRESA));
        selectorEmpresa.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("vacio"), empresa.isEmpty());
    }

    // ------------------------------------------------------------------ menu de usuario

    private void construirMenuUsuario(Usuario usuario) {
        menuUsuario.setText(usuario.nombreCompleto());

        MenuItem cuenta = new MenuItem(usuario.nombreUsuario() + "  ·  " + usuario.rol().nombre());
        cuenta.setDisable(true);
        MenuItem cambiarPassword = new MenuItem("Cambiar contraseña", new FontIcon("fth-key"));
        cambiarPassword.setOnAction(e -> cambiarPassword());
        MenuItem apariencia = new MenuItem("Apariencia…", new FontIcon("fth-droplet"));
        apariencia.setOnAction(e -> mostrarApariencia());
        MenuItem cerrarSesion = new MenuItem("Cerrar sesión", new FontIcon("fth-log-out"));
        cerrarSesion.setOnAction(e -> cerrarSesion());

        menuUsuario.getItems().setAll(cuenta, new SeparatorMenuItem(), apariencia, cambiarPassword, cerrarSesion);
    }

    /** Color de acento y «Reducir animaciones», disponible para cualquier rol. */
    private void mostrarApariencia() {
        javafx.scene.control.Dialog<Void> dialogo = new javafx.scene.control.Dialog<>();
        dialogo.initOwner(ventana.stage());
        dialogo.setTitle("Apariencia");
        dialogo.setHeaderText("Elige cómo se ve Business Analyzer para ti");
        com.businessanalyzer.ui.PanelApariencia panel = new com.businessanalyzer.ui.PanelApariencia(contexto.preferencias());
        panel.setPrefWidth(400);
        dialogo.getDialogPane().setContent(panel);
        dialogo.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        com.businessanalyzer.ui.Estilos.aplicar(dialogo.getDialogPane());
        dialogo.showAndWait();
    }

    private void cambiarPassword() {
        if (DialogoCambioPassword.mostrar(ventana.stage(), contexto.autenticacion(), null, false)) {
            Notificador.exito("Tu contraseña se actualizó correctamente.");
        }
    }

    private void cerrarSesion() {
        if (Dialogos.confirmar(ventana.stage(), "Cerrar sesión", "¿Deseas cerrar la sesión actual?")) {
            contexto.autenticacion().cerrarSesion();
            ventana.mostrarAcceso();
            Notificador.info("Sesión cerrada.");
        }
    }

    private void recordarUltimoModulo(Modulo modulo) {
        try {
            contexto.configuracion().guardar(claveUltimoModulo(contexto.sesion().usuario()), modulo.name());
        } catch (RuntimeException e) {
            // No es critico: solo afecta a que modulo se abre al iniciar.
            log.warn("No se pudo guardar el ultimo modulo abierto", e);
        }
    }

    /** Cada usuario recuerda su propio ultimo modulo. */
    private static String claveUltimoModulo(Usuario usuario) {
        return ConfiguracionService.CLAVE_ULTIMO_MODULO + "." + usuario.id();
    }
}
