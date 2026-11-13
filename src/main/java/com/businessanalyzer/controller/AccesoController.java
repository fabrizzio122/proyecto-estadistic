package com.businessanalyzer.controller;

import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.ui.DialogoCambioPassword;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.Tareas;
import com.businessanalyzer.ui.Ventana;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

/** Pantalla de inicio de sesión. La verificación corre en segundo plano (el hash es costoso). */
public class AccesoController {

    private final AppContext contexto;
    private final Ventana ventana;

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Button btnIngresar;
    @FXML private ProgressIndicator progreso;

    public AccesoController(AppContext contexto, Ventana ventana) {
        this.contexto = contexto;
        this.ventana = ventana;
    }

    @FXML
    private void initialize() {
        lblError.managedProperty().bind(lblError.visibleProperty());
        progreso.managedProperty().bind(progreso.visibleProperty());
        Platform.runLater(txtUsuario::requestFocus);
    }

    @FXML
    private void enfocarPassword() {
        txtPassword.requestFocus();
    }

    @FXML
    private void ingresar() {
        String usuario = txtUsuario.getText();
        String password = txtPassword.getText();
        ocupado(true);
        lblError.setVisible(false);

        Tareas.ejecutar(
                () -> contexto.autenticacion().iniciarSesion(usuario, password),
                conectado -> {
                    ocupado(false);
                    txtPassword.clear();
                    entrar(conectado, password);
                },
                error -> {
                    ocupado(false);
                    txtPassword.clear();
                    txtPassword.requestFocus();
                    mostrarError(Errores.mensaje(error));
                });
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
        progreso.setVisible(ocupado);
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
    }
}
