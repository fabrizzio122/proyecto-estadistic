package com.businessanalyzer.controller;

import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.DatosUsuario;
import com.businessanalyzer.seguridad.PoliticaPassword;
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

import java.util.List;

/** Primer arranque: crea la cuenta de administrador y abre la sesión con ella. */
public class ConfiguracionInicialController {

    private final AppContext contexto;
    private final Ventana ventana;

    @FXML private TextField txtUsuario;
    @FXML private TextField txtNombre;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmacion;
    @FXML private Label lblPolitica;
    @FXML private Label lblError;
    @FXML private Button btnCrear;
    @FXML private ProgressIndicator progreso;

    public ConfiguracionInicialController(AppContext contexto, Ventana ventana) {
        this.contexto = contexto;
        this.ventana = ventana;
    }

    @FXML
    private void initialize() {
        lblPolitica.setText("Contraseña: " + PoliticaPassword.DESCRIPCION.toLowerCase());
        lblError.managedProperty().bind(lblError.visibleProperty());
        progreso.managedProperty().bind(progreso.visibleProperty());
        Platform.runLater(txtUsuario::requestFocus);
    }

    @FXML
    private void crear() {
        DatosUsuario datos = new DatosUsuario(txtUsuario.getText(), txtNombre.getText(), txtEmail.getText(), 0);
        String password = txtPassword.getText();
        String confirmacion = txtConfirmacion.getText();
        ocupado(true);
        lblError.setVisible(false);

        Tareas.ejecutar(
                () -> {
                    contexto.autenticacion().crearAdministradorInicial(datos, password, confirmacion);
                    return contexto.autenticacion().iniciarSesion(datos.nombreUsuario(), password);
                },
                usuario -> {
                    ventana.mostrarPrincipal();
                    Notificador.exito("Cuenta de administrador creada. Bienvenido, " + usuario.nombreCompleto() + ".");
                },
                error -> {
                    ocupado(false);
                    lblError.setText(Errores.mensaje(error));
                    lblError.setVisible(true);
                });
    }

    private void ocupado(boolean ocupado) {
        List.of(txtUsuario, txtNombre, txtEmail, txtPassword, txtConfirmacion, btnCrear)
                .forEach(control -> control.setDisable(ocupado));
        progreso.setVisible(ocupado);
    }
}
