package com.businessanalyzer.controller;

import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.config.VersionApp;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.service.PreferenciasService;
import com.businessanalyzer.service.PreferenciasService.PerfilRecordado;
import com.businessanalyzer.ui.Animaciones;
import com.businessanalyzer.ui.ColorAcento;
import com.businessanalyzer.ui.DialogoCambioPassword;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Estilos;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.PanelMarca;
import com.businessanalyzer.ui.Tareas;
import com.businessanalyzer.ui.Ventana;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de inicio de sesión. Muestra como tarjetas solo a los usuarios que pidieron ser recordados en
 * este equipo; el resto entra con «Entrar con otro usuario». El color elegido en «Color de la luz» se
 * guarda para el usuario que inicia sesión. La verificación corre en segundo plano (el hash es costoso).
 */
public class AccesoController {

    private static final Logger log = LoggerFactory.getLogger(AccesoController.class);
    private static final PseudoClass SELECCIONADA = PseudoClass.getPseudoClass("selected");

    private final AppContext contexto;
    private final Ventana ventana;
    private final List<HBox> tarjetas = new ArrayList<>();
    /** Usuario recordado elegido; {@code null} = se escribe el nombre de usuario. */
    private PerfilRecordado elegido;
    /** Color elegido en esta pantalla; se guarda para el usuario que entra. */
    private ColorAcento colorElegido;

    @FXML private PanelMarca panelMarca;
    @FXML private VBox formulario;
    @FXML private Label lblSubtitulo;
    @FXML private VBox listaUsuarios;
    @FXML private VBox campoUsuario;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private Button btnMostrar;
    @FXML private CheckBox chkRecordar;
    @FXML private Label lblError;
    @FXML private Button btnIngresar;
    @FXML private ProgressIndicator progreso;
    @FXML private Label lblVersion;

    public AccesoController(AppContext contexto, Ventana ventana) {
        this.contexto = contexto;
        this.ventana = ventana;
    }

    @FXML
    private void initialize() {
        lblError.managedProperty().bind(lblError.visibleProperty());
        campoUsuario.managedProperty().bind(campoUsuario.visibleProperty());
        listaUsuarios.managedProperty().bind(listaUsuarios.visibleProperty());
        txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());
        lblVersion.setText("Business Analyzer " + VersionApp.NUMERO);
        panelMarca.setAlElegirColor(this::alElegirColor);

        List<PerfilRecordado> perfiles = List.of();
        try {
            perfiles = preferencias().recordados();
        } catch (RuntimeException e) {
            log.warn("No se pudieron leer los usuarios recordados", e);
        }
        if (perfiles.isEmpty()) {
            listaUsuarios.setVisible(false);
            lblSubtitulo.setText("Ingresa tu usuario y contraseña para continuar.");
            elegirOtro();
        } else {
            for (PerfilRecordado perfil : perfiles) {
                listaUsuarios.getChildren().add(tarjeta(perfil));
            }
            listaUsuarios.getChildren().add(tarjetaOtro());
            elegir(perfiles.get(0), tarjetas.get(0));
        }
        Platform.runLater(() -> Animaciones.aparecer(formulario, 18, Duration.millis(450)));
    }

    // ------------------------------------------------------------------ tarjetas de usuario

    private HBox tarjeta(PerfilRecordado perfil) {
        Circle circulo = new Circle(20);
        circulo.getStyleClass().add("avatar-circulo");
        Label iniciales = new Label(perfil.iniciales());
        iniciales.getStyleClass().add("avatar-iniciales");
        StackPane avatar = new StackPane(circulo, iniciales);

        Label nombre = new Label(perfil.nombreCompleto());
        nombre.getStyleClass().add("tarjeta-usuario-nombre");
        Label detalle = new Label(perfil.empresa() != null ? perfil.empresa() : "@" + perfil.nombreUsuario());
        detalle.getStyleClass().add("tarjeta-usuario-detalle");
        HBox tarjeta = new HBox(12, avatar, new VBox(2, nombre, detalle));
        prepararTarjeta(tarjeta, () -> elegir(perfil, tarjeta));
        return tarjeta;
    }

    private HBox tarjetaOtro() {
        Circle circulo = new Circle(20);
        circulo.getStyleClass().add("avatar-otro");
        FontIcon mas = new FontIcon("fth-plus");
        mas.getStyleClass().add("avatar-otro-icono");
        Label texto = new Label("Entrar con otro usuario");
        texto.getStyleClass().add("tarjeta-usuario-otro");
        HBox tarjeta = new HBox(12, new StackPane(circulo, mas), texto);
        prepararTarjeta(tarjeta, () -> {
            marcar(tarjeta);
            elegirOtro();
        });
        return tarjeta;
    }

    private void prepararTarjeta(HBox tarjeta, Runnable accion) {
        tarjeta.getStyleClass().add("tarjeta-usuario");
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.setCursor(Cursor.HAND);
        tarjeta.setFocusTraversable(true);
        tarjeta.setOnMouseClicked(e -> accion.run());
        tarjeta.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                accion.run();
                e.consume();
            }
        });
        tarjetas.add(tarjeta);
    }

    private void elegir(PerfilRecordado perfil, HBox tarjeta) {
        elegido = perfil;
        marcar(tarjeta);
        campoUsuario.setVisible(false);
        chkRecordar.setSelected(true);
        // La pantalla toma el color de ese usuario, salvo que se haya elegido otro aquí mismo.
        if (colorElegido == null && perfil.color() != null) {
            ColorAcento.desde(perfil.color()).ifPresent(c -> {
                Estilos.cambiarAcento(c);
                panelMarca.marcarColorActual();
            });
        }
        Platform.runLater(txtPassword::requestFocus);
    }

    private void elegirOtro() {
        elegido = null;
        campoUsuario.setVisible(true);
        chkRecordar.setSelected(false);
        Platform.runLater(txtUsuario::requestFocus);
    }

    private void marcar(HBox elegida) {
        for (HBox t : tarjetas) {
            t.pseudoClassStateChanged(SELECCIONADA, t == elegida);
        }
    }

    // ------------------------------------------------------------------ acciones

    private void alElegirColor(ColorAcento color) {
        colorElegido = color;
        try {
            preferencias().guardarUltimoColor(color.name());
        } catch (RuntimeException e) {
            log.warn("No se pudo guardar el color elegido", e);
        }
    }

    @FXML
    private void enfocarPassword() {
        txtPassword.requestFocus();
    }

    @FXML
    private void alternarPassword() {
        boolean mostrar = !txtPasswordVisible.isVisible();
        txtPasswordVisible.setVisible(mostrar);
        txtPassword.setVisible(!mostrar);
        btnMostrar.setText(mostrar ? "Ocultar" : "Mostrar");
        (mostrar ? txtPasswordVisible : txtPassword).requestFocus();
        (mostrar ? txtPasswordVisible : txtPassword).end();
    }

    @FXML
    private void ingresar() {
        String usuario = elegido != null ? elegido.nombreUsuario() : txtUsuario.getText();
        String password = txtPassword.getText();
        boolean recordar = chkRecordar.isSelected();
        ocupado(true);
        lblError.setVisible(false);

        Tareas.ejecutar(
                () -> contexto.autenticacion().iniciarSesion(usuario, password),
                conectado -> {
                    ocupado(false);
                    txtPassword.clear();
                    guardarPreferencias(conectado, recordar);
                    entrar(conectado, password);
                },
                error -> {
                    ocupado(false);
                    txtPassword.clear();
                    txtPassword.requestFocus();
                    mostrarError(Errores.mensaje(error));
                });
    }

    /** Recuerda (u olvida) al usuario en el equipo y aplica su color, o le guarda el elegido aquí. */
    private void guardarPreferencias(Usuario usuario, boolean recordar) {
        try {
            PreferenciasService p = preferencias();
            if (recordar) {
                p.recordarUsuarioActual();
            } else {
                p.olvidar(usuario.nombreUsuario());
            }
            if (colorElegido != null) {
                p.guardarColorDelUsuario(colorElegido.name());
            } else {
                p.colorDelUsuario().flatMap(ColorAcento::desde).ifPresent(c -> {
                    Estilos.cambiarAcento(c);
                    p.guardarUltimoColor(c.name());
                });
            }
        } catch (RuntimeException e) {
            log.warn("No se pudieron guardar las preferencias de acceso", e);
        }
    }

    private void entrar(Usuario usuario, String passwordIngresada) {
        if (usuario.debeCambiarPassword()) {
            boolean cambiada = DialogoCambioPassword.mostrar(
                    ventana.stage(), contexto.autenticacion(), passwordIngresada, true);
            if (!cambiada) {
                contexto.autenticacion().cerrarSesion();
                mostrarError("Debes cambiar tu contraseña temporal para poder ingresar.");
                return;
            }
        }
        ventana.mostrarPrincipal();
        Notificador.exito("Bienvenido, " + usuario.nombreCompleto() + ".");
    }

    private void ocupado(boolean ocupado) {
        btnIngresar.setDisable(ocupado);
        txtUsuario.setDisable(ocupado);
        txtPassword.setDisable(ocupado);
        txtPasswordVisible.setDisable(ocupado);
        listaUsuarios.setDisable(ocupado);
        progreso.setVisible(ocupado);
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
    }

    private PreferenciasService preferencias() {
        return contexto.preferencias();
    }
}
