package com.businessanalyzer.controller;

import com.businessanalyzer.BusinessAnalyzerApp;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.Empresa;
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
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
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

    public MainController(AppContext contexto, Ventana ventana) {
        this.contexto = contexto;
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

        String ultimo = contexto.configuracion().obtener(claveUltimoModulo(usuario), "");
        Modulo.desdeNombre(ultimo)
                .filter(botones::containsKey)
                .or(() -> botones.keySet().stream().findFirst())
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
                menu.getChildren().add(encabezado);
            }
            ToggleButton boton = new ToggleButton(modulo.titulo(), new FontIcon(modulo.icono()));
            boton.setToggleGroup(grupoMenu);
            boton.getStyleClass().add("sidebar-item");
            boton.setMaxWidth(Double.MAX_VALUE);
            boton.setAlignment(Pos.CENTER_LEFT);
            if (!modulo.disponible()) {
                boton.setTooltip(new Tooltip("Disponible en la Fase " + modulo.fase()));
            }
            boton.setOnAction(e -> abrir(modulo));
            botones.put(modulo, boton);
            menu.getChildren().add(boton);
        }
    }

    private void abrir(Modulo modulo) {
        botones.get(modulo).setSelected(true);
        tituloModulo.setText(modulo.titulo());
        descripcionModulo.setText(modulo.descripcion());
        try {
            navegador.mostrar(modulo);
            moduloActual = modulo;
        } catch (Exception e) {
            log.error("No se pudo abrir el modulo {}", modulo, e);
            Notificador.error("No se pudo abrir el módulo " + modulo.titulo());
            return;
        }
        recordarUltimoModulo(modulo);
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
        if (moduloActual != null) {
            navegador.alCambiarEmpresa(moduloActual);
        }
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
        MenuItem cerrarSesion = new MenuItem("Cerrar sesión", new FontIcon("fth-log-out"));
        cerrarSesion.setOnAction(e -> cerrarSesion());

        menuUsuario.getItems().setAll(cuenta, new SeparatorMenuItem(), cambiarPassword, cerrarSesion);
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
