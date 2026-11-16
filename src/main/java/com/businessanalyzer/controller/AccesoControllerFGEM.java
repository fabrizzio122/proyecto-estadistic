package com.businessanalyzer.controller;

import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.config.VersionAppFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.service.PreferenciasServiceFGEM;
import com.businessanalyzer.service.PreferenciasServiceFGEM.PerfilRecordadoFGEM;
import com.businessanalyzer.ui.AnimacionesFGEM;
import com.businessanalyzer.ui.ColorAcentoFGEM;
import com.businessanalyzer.ui.DialogoCambioPasswordFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.EstilosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.PanelMarcaFGEM;
import com.businessanalyzer.ui.TareasFGEM;
import com.businessanalyzer.ui.VentanaFGEM;
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
public class AccesoControllerFGEM {

    private static final Logger logFGEM = LoggerFactory.getLogger(AccesoControllerFGEM.class);
    private static final PseudoClass SELECCIONADA_FGEM = PseudoClass.getPseudoClass("selected");

    private final AppContextFGEM contextoFGEM;
    private final VentanaFGEM ventanaFGEM;
    private final List<HBox> tarjetasFGEM = new ArrayList<>();
    /** Usuario recordado elegido; {@code null} = se escribe el nombre de usuario. */
    private PerfilRecordadoFGEM elegidoFGEM;
    /** Color elegido en esta pantalla; se guarda para el usuario que entra. */
    private ColorAcentoFGEM colorElegidoFGEM;

    @FXML private PanelMarcaFGEM panelMarcaFGEM;
    @FXML private VBox formularioFGEM;
    @FXML private Label lblSubtituloFGEM;
    @FXML private VBox listaUsuariosFGEM;
    @FXML private VBox campoUsuarioFGEM;
    @FXML private TextField txtUsuarioFGEM;
    @FXML private PasswordField txtPasswordFGEM;
    @FXML private TextField txtPasswordVisibleFGEM;
    @FXML private Button btnMostrarFGEM;
    @FXML private CheckBox chkRecordarFGEM;
    @FXML private Label lblErrorFGEM;
    @FXML private Button btnIngresarFGEM;
    @FXML private ProgressIndicator progresoFGEM;
    @FXML private Label lblVersionFGEM;

    public AccesoControllerFGEM(AppContextFGEM contextoFGEM, VentanaFGEM ventanaFGEM) {
        this.contextoFGEM = contextoFGEM;
        this.ventanaFGEM = ventanaFGEM;
    }

    @FXML
    private void initialize() {
        lblErrorFGEM.managedProperty().bind(lblErrorFGEM.visibleProperty());
        campoUsuarioFGEM.managedProperty().bind(campoUsuarioFGEM.visibleProperty());
        listaUsuariosFGEM.managedProperty().bind(listaUsuariosFGEM.visibleProperty());
        txtPasswordVisibleFGEM.textProperty().bindBidirectional(txtPasswordFGEM.textProperty());
        lblVersionFGEM.setText("Business Analyzer " + VersionAppFGEM.NUMERO_FGEM);
        panelMarcaFGEM.setAlElegirColorFGEM(this::alElegirColorFGEM);

        List<PerfilRecordadoFGEM> perfilesFGEM = List.of();
        try {
            perfilesFGEM = preferenciasFGEM().recordadosFGEM();
        } catch (RuntimeException eFGEM) {
            logFGEM.warn("No se pudieron leer los usuarios recordados", eFGEM);
        }
        if (perfilesFGEM.isEmpty()) {
            listaUsuariosFGEM.setVisible(false);
            lblSubtituloFGEM.setText("Ingresa tu usuario y contraseña para continuar.");
            elegirOtroFGEM();
        } else {
            for (PerfilRecordadoFGEM perfilFGEM : perfilesFGEM) {
                listaUsuariosFGEM.getChildren().add(tarjetaFGEM(perfilFGEM));
            }
            listaUsuariosFGEM.getChildren().add(tarjetaOtroFGEM());
            elegirFGEM(perfilesFGEM.get(0), tarjetasFGEM.get(0));
        }
        Platform.runLater(() -> AnimacionesFGEM.aparecerFGEM(formularioFGEM, 18, Duration.millis(450)));
    }

    // ------------------------------------------------------------------ tarjetas de usuario

    private HBox tarjetaFGEM(PerfilRecordadoFGEM perfilFGEM) {
        Circle circuloFGEM = new Circle(20);
        circuloFGEM.getStyleClass().add("avatar-circulo");
        Label inicialesFGEM = new Label(perfilFGEM.inicialesFGEM());
        inicialesFGEM.getStyleClass().add("avatar-iniciales");
        StackPane avatarFGEM = new StackPane(circuloFGEM, inicialesFGEM);

        Label nombreFGEM = new Label(perfilFGEM.nombreCompletoFGEM());
        nombreFGEM.getStyleClass().add("tarjeta-usuario-nombre");
        Label detalleFGEM = new Label(perfilFGEM.empresaFGEM() != null ? perfilFGEM.empresaFGEM() : "@" + perfilFGEM.nombreUsuarioFGEM());
        detalleFGEM.getStyleClass().add("tarjeta-usuario-detalle");
        HBox tarjetaFGEM = new HBox(12, avatarFGEM, new VBox(2, nombreFGEM, detalleFGEM));
        prepararTarjetaFGEM(tarjetaFGEM, () -> elegirFGEM(perfilFGEM, tarjetaFGEM));
        return tarjetaFGEM;
    }

    private HBox tarjetaOtroFGEM() {
        Circle circuloFGEM = new Circle(20);
        circuloFGEM.getStyleClass().add("avatar-otro");
        FontIcon masFGEM = new FontIcon("fth-plus");
        masFGEM.getStyleClass().add("avatar-otro-icono");
        Label textoFGEM = new Label("Entrar con otro usuario");
        textoFGEM.getStyleClass().add("tarjeta-usuario-otro");
        HBox tarjetaFGEM = new HBox(12, new StackPane(circuloFGEM, masFGEM), textoFGEM);
        prepararTarjetaFGEM(tarjetaFGEM, () -> {
            marcarFGEM(tarjetaFGEM);
            elegirOtroFGEM();
        });
        return tarjetaFGEM;
    }

    private void prepararTarjetaFGEM(HBox tarjetaFGEM, Runnable accionFGEM) {
        tarjetaFGEM.getStyleClass().add("tarjeta-usuario");
        tarjetaFGEM.setAlignment(Pos.CENTER_LEFT);
        tarjetaFGEM.setCursor(Cursor.HAND);
        tarjetaFGEM.setFocusTraversable(true);
        tarjetaFGEM.setOnMouseClicked(eFGEM -> accionFGEM.run());
        tarjetaFGEM.setOnKeyPressed(eFGEM -> {
            if (eFGEM.getCode() == KeyCode.ENTER || eFGEM.getCode() == KeyCode.SPACE) {
                accionFGEM.run();
                eFGEM.consume();
            }
        });
        tarjetasFGEM.add(tarjetaFGEM);
    }

    private void elegirFGEM(PerfilRecordadoFGEM perfilFGEM, HBox tarjetaFGEM) {
        elegidoFGEM = perfilFGEM;
        marcarFGEM(tarjetaFGEM);
        campoUsuarioFGEM.setVisible(false);
        chkRecordarFGEM.setSelected(true);
        // La pantalla toma el color de ese usuario, salvo que se haya elegido otro aquí mismo.
        if (colorElegidoFGEM == null && perfilFGEM.colorFGEM() != null) {
            ColorAcentoFGEM.desdeFGEM(perfilFGEM.colorFGEM()).ifPresent(cFGEM -> {
                EstilosFGEM.cambiarAcentoFGEM(cFGEM);
                panelMarcaFGEM.marcarColorActualFGEM();
            });
        }
        Platform.runLater(txtPasswordFGEM::requestFocus);
    }

    private void elegirOtroFGEM() {
        elegidoFGEM = null;
        campoUsuarioFGEM.setVisible(true);
        chkRecordarFGEM.setSelected(false);
        Platform.runLater(txtUsuarioFGEM::requestFocus);
    }

    private void marcarFGEM(HBox elegidaFGEM) {
        for (HBox tFGEM : tarjetasFGEM) {
            tFGEM.pseudoClassStateChanged(SELECCIONADA_FGEM, tFGEM == elegidaFGEM);
        }
    }

    // ------------------------------------------------------------------ acciones

    private void alElegirColorFGEM(ColorAcentoFGEM colorFGEM) {
        colorElegidoFGEM = colorFGEM;
        try {
            preferenciasFGEM().guardarUltimoColorFGEM(colorFGEM.name());
        } catch (RuntimeException eFGEM) {
            logFGEM.warn("No se pudo guardar el color elegido", eFGEM);
        }
    }

    @FXML
    private void enfocarPasswordFGEM() {
        txtPasswordFGEM.requestFocus();
    }

    @FXML
    private void alternarPasswordFGEM() {
        boolean mostrarFGEM = !txtPasswordVisibleFGEM.isVisible();
        txtPasswordVisibleFGEM.setVisible(mostrarFGEM);
        txtPasswordFGEM.setVisible(!mostrarFGEM);
        btnMostrarFGEM.setText(mostrarFGEM ? "Ocultar" : "Mostrar");
        (mostrarFGEM ? txtPasswordVisibleFGEM : txtPasswordFGEM).requestFocus();
        (mostrarFGEM ? txtPasswordVisibleFGEM : txtPasswordFGEM).end();
    }

    @FXML
    private void ingresarFGEM() {
        String usuarioFGEM = elegidoFGEM != null ? elegidoFGEM.nombreUsuarioFGEM() : txtUsuarioFGEM.getText();
        String passwordFGEM = txtPasswordFGEM.getText();
        boolean recordarFGEM = chkRecordarFGEM.isSelected();
        ocupadoFGEM(true);
        lblErrorFGEM.setVisible(false);

        TareasFGEM.ejecutarFGEM(
                () -> contextoFGEM.autenticacionFGEM().iniciarSesionFGEM(usuarioFGEM, passwordFGEM),
                conectadoFGEM -> {
                    ocupadoFGEM(false);
                    txtPasswordFGEM.clear();
                    guardarPreferenciasFGEM(conectadoFGEM, recordarFGEM);
                    entrarFGEM(conectadoFGEM, passwordFGEM);
                },
                errorFGEM -> {
                    ocupadoFGEM(false);
                    txtPasswordFGEM.clear();
                    txtPasswordFGEM.requestFocus();
                    mostrarErrorFGEM(ErroresFGEM.mensajeFGEM(errorFGEM));
                });
    }

    /** Recuerda (u olvida) al usuario en el equipo y aplica su color, o le guarda el elegido aquí. */
    private void guardarPreferenciasFGEM(UsuarioFGEM usuarioFGEM, boolean recordarFGEM) {
        try {
            PreferenciasServiceFGEM pFGEM = preferenciasFGEM();
            if (recordarFGEM) {
                pFGEM.recordarUsuarioActualFGEM();
            } else {
                pFGEM.olvidarFGEM(usuarioFGEM.nombreUsuarioFGEM());
            }
            if (colorElegidoFGEM != null) {
                pFGEM.guardarColorDelUsuarioFGEM(colorElegidoFGEM.name());
            } else {
                pFGEM.colorDelUsuarioFGEM().flatMap(ColorAcentoFGEM::desdeFGEM).ifPresent(cFGEM -> {
                    EstilosFGEM.cambiarAcentoFGEM(cFGEM);
                    pFGEM.guardarUltimoColorFGEM(cFGEM.name());
                });
            }
        } catch (RuntimeException eFGEM) {
            logFGEM.warn("No se pudieron guardar las preferencias de acceso", eFGEM);
        }
    }

    private void entrarFGEM(UsuarioFGEM usuarioFGEM, String passwordIngresadaFGEM) {
        if (usuarioFGEM.debeCambiarPasswordFGEM()) {
            boolean cambiadaFGEM = DialogoCambioPasswordFGEM.mostrarFGEM(
                    ventanaFGEM.stageFGEM(), contextoFGEM.autenticacionFGEM(), passwordIngresadaFGEM, true);
            if (!cambiadaFGEM) {
                contextoFGEM.autenticacionFGEM().cerrarSesionFGEM();
                mostrarErrorFGEM("Debes cambiar tu contraseña temporal para poder ingresar.");
                return;
            }
        }
        ventanaFGEM.mostrarPrincipalFGEM();
        NotificadorFGEM.exitoFGEM("Bienvenido, " + usuarioFGEM.nombreCompletoFGEM() + ".");
    }

    private void ocupadoFGEM(boolean ocupadoFGEM) {
        btnIngresarFGEM.setDisable(ocupadoFGEM);
        txtUsuarioFGEM.setDisable(ocupadoFGEM);
        txtPasswordFGEM.setDisable(ocupadoFGEM);
        txtPasswordVisibleFGEM.setDisable(ocupadoFGEM);
        listaUsuariosFGEM.setDisable(ocupadoFGEM);
        progresoFGEM.setVisible(ocupadoFGEM);
    }

    private void mostrarErrorFGEM(String mensajeFGEM) {
        lblErrorFGEM.setText(mensajeFGEM);
        lblErrorFGEM.setVisible(true);
    }

    private PreferenciasServiceFGEM preferenciasFGEM() {
        return contextoFGEM.preferenciasFGEM();
    }
}
