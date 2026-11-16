package com.businessanalyzer.controller;

import com.businessanalyzer.BusinessAnalyzerAppFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.service.ConfiguracionServiceFGEM;
import com.businessanalyzer.ui.DialogoCambioPasswordFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.ModuloFGEM;
import com.businessanalyzer.ui.NavegadorFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.VentanaFGEM;
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
public class MainControllerFGEM {

    private static final Logger logFGEM = LoggerFactory.getLogger(MainControllerFGEM.class);
    private static final String SIN_EMPRESA_FGEM = "Sin empresa seleccionada";

    private final AppContextFGEM contextoFGEM;
    private final VentanaFGEM ventanaFGEM;
    private final ToggleGroup grupoMenuFGEM = new ToggleGroup();
    private final Map<ModuloFGEM, ToggleButton> botonesFGEM = new EnumMap<>(ModuloFGEM.class);
    private final Map<ModuloFGEM.SeccionFGEM, Label> encabezadosFGEM = new EnumMap<>(ModuloFGEM.SeccionFGEM.class);
    private NavegadorFGEM navegadorFGEM;
    private ModuloFGEM moduloActualFGEM;

    @FXML private VBox menuFGEM;
    @FXML private StackPane contenidoFGEM;
    @FXML private Label tituloModuloFGEM;
    @FXML private Label descripcionModuloFGEM;
    @FXML private Label estadoBaseDatosFGEM;
    @FXML private Label estadoVersionFGEM;
    @FXML private MenuButton menuUsuarioFGEM;
    @FXML private MenuButton selectorEmpresaFGEM;
    @FXML private Label kickerModuloFGEM;
    @FXML private Label chipPeriodoFGEM;
    @FXML private Button btnAccionPrincipalFGEM;
    @FXML private HBox franjaFraseFGEM;
    @FXML private Label lblFraseFGEM;

    /** Contador de alertas pendientes junto a «Diagnóstico empresarial». */
    private final Label contadorAlertasFGEM = new Label();

    public MainControllerFGEM(AppContextFGEM contextoFGEM, VentanaFGEM ventanaFGEM) {
        this.contextoFGEM = contextoFGEM;
        contextoFGEM.alAbrirModuloFGEM(mFGEM -> {
            if (botonesFGEM.containsKey(mFGEM) && botonesFGEM.get(mFGEM).isVisible() && !botonesFGEM.get(mFGEM).isDisabled()) {
                abrirFGEM(mFGEM);
            }
        });
        contextoFGEM.alActualizarAlertasFGEM(this::mostrarAlertasFGEM);
        contextoFGEM.alMostrarFraseFGEM(this::mostrarFraseFGEM);
        this.ventanaFGEM = ventanaFGEM;
    }

    @FXML
    private void initialize() {
        UsuarioFGEM usuarioFGEM = contextoFGEM.sesionFGEM().usuarioFGEM();
        navegadorFGEM = new NavegadorFGEM(contenidoFGEM, contextoFGEM);
        construirMenuFGEM();
        construirMenuUsuarioFGEM(usuarioFGEM);
        prepararSelectorEmpresaFGEM();

        estadoBaseDatosFGEM.setText("Base de datos: " + contextoFGEM.configFGEM().archivoBaseDatosFGEM()
                + "   ·   Esquema v" + contextoFGEM.versionEsquemaFGEM());
        estadoVersionFGEM.setText(BusinessAnalyzerAppFGEM.NOMBRE_FGEM + " " + BusinessAnalyzerAppFGEM.VERSION_FGEM);

        // Evita que el menu quede sin opcion marcada al pulsar la opcion activa.
        grupoMenuFGEM.selectedToggleProperty().addListener((obsFGEM, anteriorFGEM, nuevoFGEM) -> {
            if (nuevoFGEM == null && anteriorFGEM != null) {
                anteriorFGEM.setSelected(true);
            }
        });

        aplicarSectorFGEM(contextoFGEM.sesionFGEM().empresaActivaFGEM());
        String ultimoFGEM = contextoFGEM.configuracionFGEM().obtenerFGEM(claveUltimoModuloFGEM(usuarioFGEM), "");
        ModuloFGEM.desdeNombreFGEM(ultimoFGEM)
                .filter(botonesFGEM::containsKey)
                .filter(mFGEM -> botonesFGEM.get(mFGEM).isVisible())
                .or(() -> botonesFGEM.keySet().stream().filter(mFGEM -> botonesFGEM.get(mFGEM).isVisible()).findFirst())
                .ifPresent(this::abrirFGEM);
    }

    // ------------------------------------------------------------------ menu lateral

    private void construirMenuFGEM() {
        ModuloFGEM.SeccionFGEM seccionActualFGEM = null;
        for (ModuloFGEM moduloFGEM : ModuloFGEM.values()) {
            if (!contextoFGEM.sesionFGEM().tienePermisoFGEM(moduloFGEM.permisoFGEM())) {
                continue;
            }
            if (moduloFGEM.seccionFGEM() != seccionActualFGEM) {
                seccionActualFGEM = moduloFGEM.seccionFGEM();
                Label encabezadoFGEM = new Label(seccionActualFGEM.tituloFGEM().toUpperCase());
                encabezadoFGEM.getStyleClass().add("sidebar-section");
                encabezadosFGEM.put(seccionActualFGEM, encabezadoFGEM);
                menuFGEM.getChildren().add(encabezadoFGEM);
            }
            ToggleButton botonFGEM = new ToggleButton(moduloFGEM.tituloFGEM(), new FontIcon(moduloFGEM.iconoFGEM()));
            botonFGEM.setToggleGroup(grupoMenuFGEM);
            botonFGEM.getStyleClass().add("sidebar-item");
            botonFGEM.setMaxWidth(Double.MAX_VALUE);
            botonFGEM.setAlignment(Pos.CENTER_LEFT);
            botonFGEM.setWrapText(true);
            if (!moduloFGEM.disponibleFGEM()) {
                botonFGEM.setTooltip(new Tooltip("Disponible en la Fase " + moduloFGEM.faseFGEM()));
            }
            botonFGEM.setOnAction(eFGEM -> abrirFGEM(moduloFGEM));
            botonesFGEM.put(moduloFGEM, botonFGEM);
            if (moduloFGEM == ModuloFGEM.DIAGNOSTICO) {
                // El contador va encima del botón, a la derecha, y no intercepta los clics.
                contadorAlertasFGEM.getStyleClass().add("contador-alertas");
                contadorAlertasFGEM.setVisible(false);
                contadorAlertasFGEM.setMouseTransparent(true);
                StackPane conContadorFGEM = new StackPane(botonFGEM, contadorAlertasFGEM);
                conContadorFGEM.getStyleClass().add("con-contador");
                StackPane.setAlignment(contadorAlertasFGEM, Pos.CENTER_RIGHT);
                StackPane.setMargin(contadorAlertasFGEM, new javafx.geometry.Insets(0, 10, 0, 0));
                botonFGEM.managedProperty().addListener((oFGEM, aFGEM, gestionadoFGEM) -> conContadorFGEM.setManaged(gestionadoFGEM));
                botonFGEM.visibleProperty().addListener((oFGEM, aFGEM, visibleFGEM) -> conContadorFGEM.setVisible(visibleFGEM));
                menuFGEM.getChildren().add(conContadorFGEM);
            } else {
                menuFGEM.getChildren().add(botonFGEM);
            }
        }
    }

    private void mostrarFraseFGEM(ModuloFGEM moduloFGEM, String fraseFGEM) {
        boolean mostrarFGEM = fraseFGEM != null && !fraseFGEM.isBlank() && moduloFGEM != null && moduloFGEM == moduloActualFGEM;
        if (moduloFGEM != null && moduloFGEM != moduloActualFGEM) {
            return;
        }
        lblFraseFGEM.setText(mostrarFGEM ? fraseFGEM : "");
        visibleFGEM(franjaFraseFGEM, mostrarFGEM);
        if (mostrarFGEM) {
            com.businessanalyzer.ui.AnimacionesFGEM.aparecerFGEM(franjaFraseFGEM, 6, javafx.util.Duration.millis(350));
        }
    }

    private void mostrarAlertasFGEM(int cantidadFGEM) {
        contadorAlertasFGEM.setText(String.valueOf(cantidadFGEM));
        contadorAlertasFGEM.setVisible(cantidadFGEM > 0);
        Tooltip.install(contadorAlertasFGEM, new Tooltip(cantidadFGEM == 1 ? "1 indicador por atender"
                : cantidadFGEM + " indicadores por atender"));
    }

    private void abrirFGEM(ModuloFGEM moduloFGEM) {
        botonesFGEM.get(moduloFGEM).setSelected(true);
        mostrarFraseFGEM(null, null);
        mostrarEncabezadoFGEM(moduloFGEM);
        try {
            navegadorFGEM.mostrarFGEM(moduloFGEM);
            moduloActualFGEM = moduloFGEM;
            com.businessanalyzer.ui.EstilosFGEM.encabezadosTablaFGEM(contenidoFGEM);
        } catch (Exception eFGEM) {
            logFGEM.error("No se pudo abrir el modulo {}", moduloFGEM, eFGEM);
            NotificadorFGEM.errorFGEM("No se pudo abrir el módulo " + moduloFGEM.tituloFGEM());
            return;
        }
        recordarUltimoModuloFGEM(moduloFGEM);
    }

    /**
     * Encabezado de la pantalla. En el Dashboard: la empresa en una línea tenue, un saludo según la hora,
     * el período del mes en curso y el botón «Registrar venta». En los demás módulos: título y descripción.
     */
    private void mostrarEncabezadoFGEM(ModuloFGEM moduloFGEM) {
        boolean inicioFGEM = moduloFGEM == ModuloFGEM.DASHBOARD;
        Optional<EmpresaFGEM> empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM();
        visibleFGEM(kickerModuloFGEM, inicioFGEM && empresaFGEM.isPresent());
        visibleFGEM(chipPeriodoFGEM, inicioFGEM && empresaFGEM.isPresent());
        visibleFGEM(descripcionModuloFGEM, !inicioFGEM);
        boolean puedeVenderFGEM = inicioFGEM && empresaFGEM.isPresent()
                && contextoFGEM.sesionFGEM().tienePermisoFGEM(com.businessanalyzer.model.PermisoFGEM.OPERACIONES_GESTIONAR)
                && botonesFGEM.containsKey(ModuloFGEM.VENTAS_COMPRAS);
        visibleFGEM(btnAccionPrincipalFGEM, puedeVenderFGEM);
        if (!inicioFGEM) {
            tituloModuloFGEM.setText(moduloFGEM.tituloFGEM());
            descripcionModuloFGEM.setText(moduloFGEM.descripcionFGEM());
            return;
        }
        String nombreFGEM = contextoFGEM.sesionFGEM().usuarioFGEM().nombreCompletoFGEM().strip().split("\\s+")[0];
        int horaFGEM = java.time.LocalTime.now().getHour();
        tituloModuloFGEM.setText((horaFGEM < 12 ? "Buenos días" : horaFGEM < 19 ? "Buenas tardes" : "Buenas noches") + ", " + nombreFGEM);
        empresaFGEM.ifPresent(eFGEM -> kickerModuloFGEM.setText(eFGEM.nombreVisibleFGEM() + "  ·  " + eFGEM.tamanoFGEM().descripcionFGEM()
                + "  ·  " + eFGEM.monedaFGEM().nombreFGEM() + " (" + eFGEM.monedaFGEM().simboloFGEM() + ")"));
        java.time.LocalDate hoyFGEM = java.time.LocalDate.now();
        chipPeriodoFGEM.setText("1 – " + hoyFGEM.getDayOfMonth() + " de "
                + com.businessanalyzer.util.FechasFGEM.nombreMesFGEM(hoyFGEM.getMonthValue()).toLowerCase() + " " + hoyFGEM.getYear());
        btnAccionPrincipalFGEM.setText("Registrar venta");
        btnAccionPrincipalFGEM.setOnAction(eFGEM -> registrarVentaFGEM());
    }

    /** Abre «Ventas y compras» con el formulario de nueva venta. */
    private void registrarVentaFGEM() {
        abrirFGEM(ModuloFGEM.VENTAS_COMPRAS);
        javafx.application.Platform.runLater(() -> {
            if (contenidoFGEM.lookup("#btnNuevaFGEM") instanceof javafx.scene.control.Button nuevaFGEM && !nuevaFGEM.isDisabled()) {
                nuevaFGEM.fire();
            }
        });
    }

    private static void visibleFGEM(javafx.scene.Node nodoFGEM, boolean visibleFGEM) {
        nodoFGEM.setVisible(visibleFGEM);
        nodoFGEM.setManaged(visibleFGEM);
    }

    // ------------------------------------------------------------------ empresa activa

    private void prepararSelectorEmpresaFGEM() {
        try {
            contextoFGEM.empresasFGEM().restaurarEmpresaActivaFGEM();
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
        mostrarEmpresaFGEM(contextoFGEM.sesionFGEM().empresaActivaFGEM());
        contextoFGEM.sesionFGEM().alCambiarEmpresaFGEM(this::empresaCambiadaFGEM);
        // La lista se arma cada vez que se abre, así siempre refleja los últimos cambios.
        selectorEmpresaFGEM.setOnShowing(eFGEM -> construirSelectorEmpresaFGEM());
        construirSelectorEmpresaFGEM();
    }

    private void construirSelectorEmpresaFGEM() {
        selectorEmpresaFGEM.getItems().clear();
        List<EmpresaFGEM> disponiblesFGEM;
        try {
            disponiblesFGEM = contextoFGEM.empresasFGEM().listarParaSelectorFGEM();
        } catch (RuntimeException eFGEM) {
            disponiblesFGEM = List.of();
        }

        ToggleGroup grupoFGEM = new ToggleGroup();
        long activaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().map(EmpresaFGEM::idFGEM).orElse(-1L);
        for (EmpresaFGEM empresaFGEM : disponiblesFGEM) {
            RadioMenuItem itemFGEM = new RadioMenuItem(empresaFGEM.nombreVisibleFGEM());
            itemFGEM.setToggleGroup(grupoFGEM);
            itemFGEM.setSelected(empresaFGEM.idFGEM() == activaFGEM);
            itemFGEM.setOnAction(eFGEM -> seleccionarEmpresaFGEM(empresaFGEM));
            selectorEmpresaFGEM.getItems().add(itemFGEM);
        }
        if (disponiblesFGEM.isEmpty()) {
            MenuItem vacioFGEM = new MenuItem("No tienes empresas disponibles");
            vacioFGEM.setDisable(true);
            selectorEmpresaFGEM.getItems().add(vacioFGEM);
        }
        if (botonesFGEM.containsKey(ModuloFGEM.EMPRESAS)) {
            MenuItem administrarFGEM = new MenuItem("Administrar empresas", new FontIcon("fth-settings"));
            administrarFGEM.setOnAction(eFGEM -> abrirFGEM(ModuloFGEM.EMPRESAS));
            selectorEmpresaFGEM.getItems().addAll(new SeparatorMenuItem(), administrarFGEM);
        }
    }

    private void seleccionarEmpresaFGEM(EmpresaFGEM empresaFGEM) {
        try {
            EmpresaFGEM activaFGEM = contextoFGEM.empresasFGEM().seleccionarEmpresaActivaFGEM(empresaFGEM.idFGEM());
            NotificadorFGEM.infoFGEM("Ahora trabajas con " + activaFGEM.nombreVisibleFGEM() + ".");
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    private void empresaCambiadaFGEM(Optional<EmpresaFGEM> empresaFGEM) {
        mostrarEmpresaFGEM(empresaFGEM);
        mostrarAlertasFGEM(0);
        mostrarFraseFGEM(null, null);
        if (moduloActualFGEM != null) {
            mostrarEncabezadoFGEM(moduloActualFGEM);
        }
        if (aplicarSectorFGEM(empresaFGEM)) {
            return;
        }
        if (moduloActualFGEM != null) {
            navegadorFGEM.alCambiarEmpresaFGEM(moduloActualFGEM);
        }
    }

    /**
     * Muestra la sección «Agropecuario» del menú solo si la empresa activa es de ese sector.
     *
     * @return {@code true} si el módulo abierto dejó de estar disponible y se abrió otro
     */
    private boolean aplicarSectorFGEM(Optional<EmpresaFGEM> empresaFGEM) {
        boolean agroFGEM = empresaFGEM.map(eFGEM -> eFGEM.sectorFGEM() == SectorFGEM.AGROPECUARIO).orElse(false);
        botonesFGEM.forEach((moduloFGEM, botonFGEM) -> {
            if (moduloFGEM.soloAgropecuarioFGEM()) {
                botonFGEM.setVisible(agroFGEM);
                botonFGEM.setManaged(agroFGEM);
            }
        });
        Label encabezadoFGEM = encabezadosFGEM.get(ModuloFGEM.SeccionFGEM.AGRO);
        if (encabezadoFGEM != null) {
            encabezadoFGEM.setVisible(agroFGEM);
            encabezadoFGEM.setManaged(agroFGEM);
        }
        if (!agroFGEM && moduloActualFGEM != null && moduloActualFGEM.soloAgropecuarioFGEM()) {
            botonesFGEM.keySet().stream().filter(mFGEM -> !mFGEM.soloAgropecuarioFGEM()).findFirst().ifPresent(this::abrirFGEM);
            return true;
        }
        return false;
    }

    private void mostrarEmpresaFGEM(Optional<EmpresaFGEM> empresaFGEM) {
        selectorEmpresaFGEM.setText(empresaFGEM.map(EmpresaFGEM::nombreVisibleFGEM).orElse(SIN_EMPRESA_FGEM));
        selectorEmpresaFGEM.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("vacio"), empresaFGEM.isEmpty());
    }

    // ------------------------------------------------------------------ menu de usuario

    private void construirMenuUsuarioFGEM(UsuarioFGEM usuarioFGEM) {
        menuUsuarioFGEM.setText(usuarioFGEM.nombreCompletoFGEM());

        MenuItem cuentaFGEM = new MenuItem(usuarioFGEM.nombreUsuarioFGEM() + "  ·  " + usuarioFGEM.rolFGEM().nombreFGEM());
        cuentaFGEM.setDisable(true);
        MenuItem cambiarPasswordFGEM = new MenuItem("Cambiar contraseña", new FontIcon("fth-key"));
        cambiarPasswordFGEM.setOnAction(eFGEM -> cambiarPasswordFGEM());
        MenuItem aparienciaFGEM = new MenuItem("Apariencia…", new FontIcon("fth-droplet"));
        aparienciaFGEM.setOnAction(eFGEM -> mostrarAparienciaFGEM());
        MenuItem cerrarSesionFGEM = new MenuItem("Cerrar sesión", new FontIcon("fth-log-out"));
        cerrarSesionFGEM.setOnAction(eFGEM -> cerrarSesionFGEM());

        menuUsuarioFGEM.getItems().setAll(cuentaFGEM, new SeparatorMenuItem(), aparienciaFGEM, cambiarPasswordFGEM, cerrarSesionFGEM);
    }

    /** Color de acento y «Reducir animaciones», disponible para cualquier rol. */
    private void mostrarAparienciaFGEM() {
        javafx.scene.control.Dialog<Void> dialogoFGEM = new javafx.scene.control.Dialog<>();
        dialogoFGEM.initOwner(ventanaFGEM.stageFGEM());
        dialogoFGEM.setTitle("Apariencia");
        dialogoFGEM.setHeaderText("Elige cómo se ve Business Analyzer para ti");
        com.businessanalyzer.ui.PanelAparienciaFGEM panelFGEM = new com.businessanalyzer.ui.PanelAparienciaFGEM(contextoFGEM.preferenciasFGEM());
        panelFGEM.setPrefWidth(400);
        dialogoFGEM.getDialogPane().setContent(panelFGEM);
        dialogoFGEM.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        com.businessanalyzer.ui.EstilosFGEM.aplicarFGEM(dialogoFGEM.getDialogPane());
        dialogoFGEM.showAndWait();
    }

    private void cambiarPasswordFGEM() {
        if (DialogoCambioPasswordFGEM.mostrarFGEM(ventanaFGEM.stageFGEM(), contextoFGEM.autenticacionFGEM(), null, false)) {
            NotificadorFGEM.exitoFGEM("Tu contraseña se actualizó correctamente.");
        }
    }

    private void cerrarSesionFGEM() {
        if (DialogosFGEM.confirmarFGEM(ventanaFGEM.stageFGEM(), "Cerrar sesión", "¿Deseas cerrar la sesión actual?")) {
            contextoFGEM.autenticacionFGEM().cerrarSesionFGEM();
            ventanaFGEM.mostrarAccesoFGEM();
            NotificadorFGEM.infoFGEM("Sesión cerrada.");
        }
    }

    private void recordarUltimoModuloFGEM(ModuloFGEM moduloFGEM) {
        try {
            contextoFGEM.configuracionFGEM().guardarFGEM(claveUltimoModuloFGEM(contextoFGEM.sesionFGEM().usuarioFGEM()), moduloFGEM.name());
        } catch (RuntimeException eFGEM) {
            // No es critico: solo afecta a que modulo se abre al iniciar.
            logFGEM.warn("No se pudo guardar el ultimo modulo abierto", eFGEM);
        }
    }

    /** Cada usuario recuerda su propio ultimo modulo. */
    private static String claveUltimoModuloFGEM(UsuarioFGEM usuarioFGEM) {
        return ConfiguracionServiceFGEM.CLAVE_ULTIMO_MODULO_FGEM + "." + usuarioFGEM.idFGEM();
    }
}
