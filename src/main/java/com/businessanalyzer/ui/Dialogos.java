package com.businessanalyzer.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.PrintWriter;
import java.io.StringWriter;

/** Ventanas modales de confirmacion y error con el estilo de la aplicacion. */
public final class Dialogos {

    private static final ButtonType ACEPTAR = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
    private static final ButtonType CANCELAR = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

    private Dialogos() {
    }

    /** @return {@code true} si el usuario pulso "Aceptar". */
    public static boolean confirmar(Window propietario, String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ACEPTAR, CANCELAR);
        preparar(alerta, propietario, titulo);
        return alerta.showAndWait().filter(ACEPTAR::equals).isPresent();
    }

    /** Muestra un error; si hay excepcion, su detalle tecnico va en una seccion desplegable. */
    public static void error(Window propietario, String titulo, String mensaje, Throwable causa) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje, ACEPTAR);
        preparar(alerta, propietario, titulo);
        if (causa != null) {
            TextArea detalle = new TextArea(trazaDe(causa));
            detalle.setEditable(false);
            detalle.setWrapText(false);
            detalle.setPrefRowCount(12);
            alerta.getDialogPane().setExpandableContent(
                    new VBox(8, new Label("Detalle técnico:"), detalle));
        }
        alerta.showAndWait();
    }

    private static void preparar(Alert alerta, Window propietario, String titulo) {
        if (propietario != null) {
            alerta.initOwner(propietario);
        }
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        Estilos.aplicar(alerta.getDialogPane());
    }

    private static String trazaDe(Throwable causa) {
        StringWriter texto = new StringWriter();
        causa.printStackTrace(new PrintWriter(texto));
        return texto.toString();
    }
}
