package com.businessanalyzer.controller;

import com.businessanalyzer.BusinessAnalyzerApp;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.service.ConfiguracionService;
import com.businessanalyzer.ui.Modulo;
import com.businessanalyzer.ui.Navegador;
import com.businessanalyzer.ui.Notificador;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

/** Ventana principal: menu lateral, barra superior, area de contenido y barra de estado. */
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    private final AppContext contexto;
    private final ToggleGroup grupoMenu = new ToggleGroup();
    private final Map<Modulo, ToggleButton> botones = new EnumMap<>(Modulo.class);
    private Navegador navegador;

    @FXML private VBox menu;
    @FXML private StackPane contenido;
    @FXML private VBox notificaciones;
    @FXML private Label tituloModulo;
    @FXML private Label descripcionModulo;
    @FXML private Label estadoBaseDatos;
    @FXML private Label estadoVersion;

    public MainController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        Notificador.instalar(notificaciones);
        navegador = new Navegador(contenido, contexto);
        construirMenu();

        estadoBaseDatos.setText("Base de datos: " + contexto.config().archivoBaseDatos()
                + "   ·   Esquema v" + contexto.versionEsquema());
        estadoVersion.setText(BusinessAnalyzerApp.NOMBRE + " " + BusinessAnalyzerApp.VERSION);

        // Evita que el menu quede sin opcion marcada al pulsar la opcion activa.
        grupoMenu.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true);
            }
        });

        String ultimo = contexto.configuracion().obtener(ConfiguracionService.CLAVE_ULTIMO_MODULO, "");
        abrir(Modulo.desdeNombre(ultimo).orElse(Modulo.DASHBOARD));
    }

    private void construirMenu() {
        Modulo.Seccion seccionActual = null;
        for (Modulo modulo : Modulo.values()) {
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
        } catch (Exception e) {
            log.error("No se pudo abrir el modulo {}", modulo, e);
            Notificador.error("No se pudo abrir el módulo " + modulo.titulo());
            return;
        }
        recordarUltimoModulo(modulo);
    }

    private void recordarUltimoModulo(Modulo modulo) {
        try {
            contexto.configuracion().guardar(ConfiguracionService.CLAVE_ULTIMO_MODULO, modulo.name());
        } catch (RuntimeException e) {
            // No es critico: solo afecta a que modulo se abre al iniciar.
            log.warn("No se pudo guardar el ultimo modulo abierto", e);
        }
    }
}
