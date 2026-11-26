package com.businessanalyzer.ui;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class DialogosFGEM {

    private static final ButtonType ACEPTAR_FGEM = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
    private static final ButtonType CANCELAR_FGEM = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

    private DialogosFGEM() {
    }

    public static boolean confirmarFGEM(Window propietarioFGEM, String tituloFGEM, String mensajeFGEM) {
        Alert alertaFGEM = new Alert(Alert.AlertType.CONFIRMATION, mensajeFGEM, ACEPTAR_FGEM, CANCELAR_FGEM);
        prepararFGEM(alertaFGEM, propietarioFGEM, tituloFGEM);
        return alertaFGEM.showAndWait().filter(ACEPTAR_FGEM::equals).isPresent();
    }

    public static void errorFGEM(Window propietarioFGEM, String tituloFGEM, String mensajeFGEM, Throwable causaFGEM) {
        Alert alertaFGEM = new Alert(Alert.AlertType.ERROR, mensajeFGEM, ACEPTAR_FGEM);
        prepararFGEM(alertaFGEM, propietarioFGEM, tituloFGEM);
        if (causaFGEM != null) {
            TextArea detalleFGEM = new TextArea(trazaDeFGEM(causaFGEM));
            detalleFGEM.setEditable(false);
            detalleFGEM.setWrapText(false);
            detalleFGEM.setPrefRowCount(12);
            alertaFGEM.getDialogPane().setExpandableContent(
                    new VBox(8, new Label("Detalle técnico:"), detalleFGEM));
        }
        alertaFGEM.showAndWait();
    }

    public static boolean formularioFGEM(Window propietarioFGEM, String tituloFGEM, String descripcionFGEM,
                                     Node contenidoFGEM, String textoAceptarFGEM, Runnable accionFGEM) {
        return formularioFGEM(propietarioFGEM, tituloFGEM, descripcionFGEM, contenidoFGEM, textoAceptarFGEM, 480, accionFGEM);
    }

    public static boolean formularioFGEM(Window propietarioFGEM, String tituloFGEM, String descripcionFGEM,
                                     Node contenidoFGEM, String textoAceptarFGEM, double anchoFGEM, Runnable accionFGEM) {
        Dialog<ButtonType> dialogoFGEM = new Dialog<>();
        if (propietarioFGEM != null) {
            dialogoFGEM.initOwner(propietarioFGEM);
        }
        dialogoFGEM.setTitle(tituloFGEM);
        dialogoFGEM.setHeaderText(tituloFGEM);

        ButtonType aceptarFGEM = new ButtonType(textoAceptarFGEM, ButtonBar.ButtonData.OK_DONE);
        dialogoFGEM.getDialogPane().getButtonTypes().setAll(aceptarFGEM, CANCELAR_FGEM);

        Label errorFGEM = new Label();
        errorFGEM.getStyleClass().add("form-error");
        errorFGEM.setWrapText(true);
        errorFGEM.setMaxWidth(Double.MAX_VALUE);
        errorFGEM.setVisible(false);
        errorFGEM.managedProperty().bind(errorFGEM.visibleProperty());

        VBox cuerpoFGEM = new VBox(16);
        if (descripcionFGEM != null && !descripcionFGEM.isBlank()) {
            Label textoFGEM = new Label(descripcionFGEM);
            textoFGEM.getStyleClass().add("muted");
            textoFGEM.setWrapText(true);
            textoFGEM.setPrefWidth(anchoFGEM);
            textoFGEM.setMinHeight(Region.USE_PREF_SIZE);
            cuerpoFGEM.getChildren().add(textoFGEM);
        }
        errorFGEM.setMinHeight(Region.USE_PREF_SIZE);
        cuerpoFGEM.getChildren().addAll(contenidoFGEM, errorFGEM);
        cuerpoFGEM.setPrefWidth(anchoFGEM);
        dialogoFGEM.getDialogPane().setContent(cuerpoFGEM);
        EstilosFGEM.aplicarFGEM(dialogoFGEM.getDialogPane());

        Button botonAceptarFGEM = (Button) dialogoFGEM.getDialogPane().lookupButton(aceptarFGEM);
        botonAceptarFGEM.getStyleClass().add("button-primary");
        botonAceptarFGEM.addEventFilter(ActionEvent.ACTION, eventoFGEM -> {
            try {
                accionFGEM.run();
            } catch (RuntimeException eFGEM) {
                errorFGEM.setText(ErroresFGEM.mensajeFGEM(eFGEM));
                errorFGEM.setVisible(true);
                eventoFGEM.consume();
                dialogoFGEM.getDialogPane().getScene().getWindow().sizeToScene();
            }
        });
        return dialogoFGEM.showAndWait().filter(aceptarFGEM::equals).isPresent();
    }

    public static void informacionFGEM(Window propietarioFGEM, String tituloFGEM, String descripcionFGEM, Node contenidoFGEM) {
        Dialog<ButtonType> dialogoFGEM = new Dialog<>();
        if (propietarioFGEM != null) {
            dialogoFGEM.initOwner(propietarioFGEM);
        }
        dialogoFGEM.setTitle(tituloFGEM);
        dialogoFGEM.setHeaderText(tituloFGEM);
        dialogoFGEM.getDialogPane().getButtonTypes().setAll(new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE));

        VBox cuerpoFGEM = new VBox(14);
        if (descripcionFGEM != null && !descripcionFGEM.isBlank()) {
            Label textoFGEM = new Label(descripcionFGEM);
            textoFGEM.getStyleClass().add("muted");
            textoFGEM.setWrapText(true);
            textoFGEM.setPrefWidth(contenidoFGEM == null ? 460 : 640);
            textoFGEM.setMinHeight(Region.USE_PREF_SIZE);
            cuerpoFGEM.getChildren().add(textoFGEM);
        }
        if (contenidoFGEM != null) {
            cuerpoFGEM.getChildren().add(contenidoFGEM);
        }
        cuerpoFGEM.setPrefWidth(contenidoFGEM == null ? 460 : 640);
        dialogoFGEM.getDialogPane().setContent(cuerpoFGEM);
        EstilosFGEM.aplicarFGEM(dialogoFGEM.getDialogPane());
        dialogoFGEM.showAndWait();
    }

    private static void prepararFGEM(Alert alertaFGEM, Window propietarioFGEM, String tituloFGEM) {
        if (propietarioFGEM != null) {
            alertaFGEM.initOwner(propietarioFGEM);
        }
        alertaFGEM.setTitle(tituloFGEM);
        alertaFGEM.setHeaderText(tituloFGEM);
        EstilosFGEM.aplicarFGEM(alertaFGEM.getDialogPane());
    }

    private static String trazaDeFGEM(Throwable causaFGEM) {
        StringWriter textoFGEM = new StringWriter();
        causaFGEM.printStackTrace(new PrintWriter(textoFGEM));
        return textoFGEM.toString();
    }
}
