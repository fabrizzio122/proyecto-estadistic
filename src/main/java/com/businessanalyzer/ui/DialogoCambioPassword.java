package com.businessanalyzer.ui;

import com.businessanalyzer.seguridad.PoliticaPassword;
import com.businessanalyzer.service.AutenticacionService;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/** Diálogo para que el usuario conectado cambie su propia contraseña. */
public final class DialogoCambioPassword {

    private DialogoCambioPassword() {
    }

    /**
     * @param passwordActual la contraseña actual si ya se conoce (recién escrita en el inicio de
     *                       sesión); en ese caso no se vuelve a pedir. {@code null} para pedirla.
     * @param obligatorio    {@code true} cuando un administrador asignó una contraseña temporal
     * @return {@code true} si la contraseña se cambió
     */
    public static boolean mostrar(Window propietario, AutenticacionService autenticacion,
                                  String passwordActual, boolean obligatorio) {
        PasswordField actual = new PasswordField();
        PasswordField nueva = new PasswordField();
        PasswordField confirmacion = new PasswordField();

        GridPane grilla = Formularios.grilla();
        if (passwordActual == null) {
            Formularios.fila(grilla, "Contraseña actual", actual);
        }
        Formularios.fila(grilla, "Nueva contraseña", nueva);
        Formularios.fila(grilla, "Confirmar contraseña", confirmacion);
        Formularios.nota(grilla, PoliticaPassword.DESCRIPCION);

        String descripcion = obligatorio
                ? "Un administrador te asignó una contraseña temporal. Elige una nueva para continuar."
                : null;
        return Dialogos.formulario(propietario,
                obligatorio ? "Debes cambiar tu contraseña" : "Cambiar contraseña",
                descripcion, grilla, "Guardar",
                () -> autenticacion.cambiarPasswordPropia(
                        passwordActual != null ? passwordActual : actual.getText(),
                        nueva.getText(), confirmacion.getText()));
    }
}
