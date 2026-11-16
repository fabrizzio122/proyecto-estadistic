package com.businessanalyzer.ui;

import com.businessanalyzer.seguridad.PoliticaPasswordFGEM;
import com.businessanalyzer.service.AutenticacionServiceFGEM;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/** Diálogo para que el usuario conectado cambie su propia contraseña. */
public final class DialogoCambioPasswordFGEM {

    private DialogoCambioPasswordFGEM() {
    }

    /**
     * @param passwordActualFGEM la contraseña actual si ya se conoce (recién escrita en el inicio de
     *                       sesión); en ese caso no se vuelve a pedir. {@code null} para pedirla.
     * @param obligatorioFGEM    {@code true} cuando un administrador asignó una contraseña temporal
     * @return {@code true} si la contraseña se cambió
     */
    public static boolean mostrarFGEM(Window propietarioFGEM, AutenticacionServiceFGEM autenticacionFGEM,
                                  String passwordActualFGEM, boolean obligatorioFGEM) {
        PasswordField actualFGEM = new PasswordField();
        PasswordField nuevaFGEM = new PasswordField();
        PasswordField confirmacionFGEM = new PasswordField();

        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        if (passwordActualFGEM == null) {
            FormulariosFGEM.filaFGEM(grillaFGEM, "Contraseña actual", actualFGEM);
        }
        FormulariosFGEM.filaFGEM(grillaFGEM, "Nueva contraseña", nuevaFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Confirmar contraseña", confirmacionFGEM);
        FormulariosFGEM.notaFGEM(grillaFGEM, PoliticaPasswordFGEM.DESCRIPCION_FGEM);

        String descripcionFGEM = obligatorioFGEM
                ? "Un administrador te asignó una contraseña temporal. Elige una nueva para continuar."
                : null;
        return DialogosFGEM.formularioFGEM(propietarioFGEM,
                obligatorioFGEM ? "Debes cambiar tu contraseña" : "Cambiar contraseña",
                descripcionFGEM, grillaFGEM, "Guardar",
                () -> autenticacionFGEM.cambiarPasswordPropiaFGEM(
                        passwordActualFGEM != null ? passwordActualFGEM : actualFGEM.getText(),
                        nuevaFGEM.getText(), confirmacionFGEM.getText()));
    }
}
