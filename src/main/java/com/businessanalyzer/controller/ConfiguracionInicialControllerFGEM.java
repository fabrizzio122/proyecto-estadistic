package com.businessanalyzer.controller;

import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.model.DatosUsuarioFGEM;
import com.businessanalyzer.seguridad.PoliticaPasswordFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.EstilosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.TareasFGEM;
import com.businessanalyzer.ui.VentanaFGEM;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.util.List;

/** Primer arranque: crea la cuenta de administrador y abre la sesión con ella. */
public class ConfiguracionInicialControllerFGEM {

    private final AppContextFGEM contextoFGEM;
    private final VentanaFGEM ventanaFGEM;

    @FXML private TextField txtUsuarioFGEM;
    @FXML private TextField txtNombreFGEM;
    @FXML private TextField txtEmailFGEM;
    @FXML private PasswordField txtPasswordFGEM;
    @FXML private PasswordField txtConfirmacionFGEM;
    @FXML private Label lblPoliticaFGEM;
    @FXML private Label lblErrorFGEM;
    @FXML private Button btnCrearFGEM;
    @FXML private ProgressIndicator progresoFGEM;

    public ConfiguracionInicialControllerFGEM(AppContextFGEM contextoFGEM, VentanaFGEM ventanaFGEM) {
        this.contextoFGEM = contextoFGEM;
        this.ventanaFGEM = ventanaFGEM;
    }

    @FXML
    private void initialize() {
        lblPoliticaFGEM.setText("Contraseña: " + PoliticaPasswordFGEM.DESCRIPCION_FGEM.toLowerCase());
        lblErrorFGEM.managedProperty().bind(lblErrorFGEM.visibleProperty());
        progresoFGEM.managedProperty().bind(progresoFGEM.visibleProperty());
        Platform.runLater(txtUsuarioFGEM::requestFocus);
    }

    @FXML
    private void crearFGEM() {
        DatosUsuarioFGEM datosFGEM = new DatosUsuarioFGEM(txtUsuarioFGEM.getText(), txtNombreFGEM.getText(), txtEmailFGEM.getText(), 0);
        String passwordFGEM = txtPasswordFGEM.getText();
        String confirmacionFGEM = txtConfirmacionFGEM.getText();
        ocupadoFGEM(true);
        lblErrorFGEM.setVisible(false);

        TareasFGEM.ejecutarFGEM(
                () -> {
                    contextoFGEM.autenticacionFGEM().crearAdministradorInicialFGEM(datosFGEM, passwordFGEM, confirmacionFGEM);
                    return contextoFGEM.autenticacionFGEM().iniciarSesionFGEM(datosFGEM.nombreUsuarioFGEM(), passwordFGEM);
                },
                usuarioFGEM -> {
                    // El administrador conserva el color elegido en «Color de la luz» y queda recordado en el equipo.
                    try {
                        contextoFGEM.preferenciasFGEM().guardarColorDelUsuarioFGEM(EstilosFGEM.acentoFGEM().name());
                        contextoFGEM.preferenciasFGEM().recordarUsuarioActualFGEM();
                    } catch (RuntimeException eFGEM) {
                        org.slf4j.LoggerFactory.getLogger(ConfiguracionInicialControllerFGEM.class)
                                .warn("No se pudieron guardar las preferencias del administrador", eFGEM);
                    }
                    ventanaFGEM.mostrarPrincipalFGEM();
                    NotificadorFGEM.exitoFGEM("Cuenta de administrador creada. Bienvenido, " + usuarioFGEM.nombreCompletoFGEM() + ".");
                },
                errorFGEM -> {
                    ocupadoFGEM(false);
                    lblErrorFGEM.setText(ErroresFGEM.mensajeFGEM(errorFGEM));
                    lblErrorFGEM.setVisible(true);
                });
    }

    private void ocupadoFGEM(boolean ocupadoFGEM) {
        List.of(txtUsuarioFGEM, txtNombreFGEM, txtEmailFGEM, txtPasswordFGEM, txtConfirmacionFGEM, btnCrearFGEM)
                .forEach(controlFGEM -> controlFGEM.setDisable(ocupadoFGEM));
        progresoFGEM.setVisible(ocupadoFGEM);
    }
}
