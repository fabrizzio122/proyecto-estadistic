package com.businessanalyzer.controller;

import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.config.VersionAppFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.service.PreferenciasServiceFGEM;
import com.businessanalyzer.service.PreferenciasServiceFGEM.PerfilRecordadoFGEM;
import com.businessanalyzer.ui.DialogoCambioPasswordFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.TareasFGEM;
import com.businessanalyzer.ui.VentanaFGEM;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AccesoControllerFGEM {

    private static final Logger logFGEM = LoggerFactory.getLogger(AccesoControllerFGEM.class);

    private final AppContextFGEM contextoFGEM;
    private final VentanaFGEM ventanaFGEM;

    @FXML private TextField txtUsuarioFGEM;
    @FXML private PasswordField txtPasswordFGEM;
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
        lblVersionFGEM.setText("Versión " + VersionAppFGEM.NUMERO_FGEM);

        List<PerfilRecordadoFGEM> perfilesFGEM = List.of();
        try {
            perfilesFGEM = preferenciasFGEM().recordadosFGEM();
        } catch (RuntimeException eFGEM) {
            logFGEM.warn("No se pudieron leer los usuarios recordados", eFGEM);
        }
        if (perfilesFGEM.isEmpty()) {
            Platform.runLater(txtUsuarioFGEM::requestFocus);
        } else {
            txtUsuarioFGEM.setText(perfilesFGEM.get(0).nombreUsuarioFGEM());
            chkRecordarFGEM.setSelected(true);
            Platform.runLater(txtPasswordFGEM::requestFocus);
        }
    }

    @FXML
    private void enfocarPasswordFGEM() {
        txtPasswordFGEM.requestFocus();
    }

    @FXML
    private void ingresarFGEM() {
        String usuarioFGEM = txtUsuarioFGEM.getText();
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

    private void guardarPreferenciasFGEM(UsuarioFGEM usuarioFGEM, boolean recordarFGEM) {
        try {
            PreferenciasServiceFGEM pFGEM = preferenciasFGEM();
            if (recordarFGEM) {
                pFGEM.recordarUsuarioActualFGEM();
            } else {
                pFGEM.olvidarFGEM(usuarioFGEM.nombreUsuarioFGEM());
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
