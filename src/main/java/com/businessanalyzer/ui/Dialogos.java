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
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.PrintWriter;
import java.io.StringWriter;

/** Ventanas modales de confirmacion, error y formularios con el estilo de la aplicacion. */
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

    /** Muestra un error; si hay excepcion, su detalle técnico va en una sección desplegable. */
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

    /**
     * Muestra un formulario modal. Al pulsar el botón de aceptar se ejecuta {@code accion}:
     * si lanza una excepción, el mensaje aparece dentro del diálogo y este sigue abierto para
     * que el usuario corrija los datos.
     *
     * @param descripcion texto explicativo opcional sobre el formulario
     * @return {@code true} si la acción se completó correctamente
     */
    public static boolean formulario(Window propietario, String titulo, String descripcion,
                                     Node contenido, String textoAceptar, Runnable accion) {
        return formulario(propietario, titulo, descripcion, contenido, textoAceptar, 480, accion);
    }

    /** Igual que el anterior, indicando el ancho del formulario en píxeles. */
    public static boolean formulario(Window propietario, String titulo, String descripcion,
                                     Node contenido, String textoAceptar, double ancho, Runnable accion) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        if (propietario != null) {
            dialogo.initOwner(propietario);
        }
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(titulo);

        ButtonType aceptar = new ButtonType(textoAceptar, ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().setAll(aceptar, CANCELAR);

        Label error = new Label();
        error.getStyleClass().add("form-error");
        error.setWrapText(true);
        error.setMaxWidth(Double.MAX_VALUE);
        error.setVisible(false);
        error.managedProperty().bind(error.visibleProperty());

        VBox cuerpo = new VBox(16);
        if (descripcion != null && !descripcion.isBlank()) {
            Label texto = new Label(descripcion);
            texto.getStyleClass().add("muted");
            texto.setWrapText(true);
            cuerpo.getChildren().add(texto);
        }
        cuerpo.getChildren().addAll(contenido, error);
        cuerpo.setPrefWidth(ancho);
        dialogo.getDialogPane().setContent(cuerpo);
        Estilos.aplicar(dialogo.getDialogPane());

        Button botonAceptar = (Button) dialogo.getDialogPane().lookupButton(aceptar);
        botonAceptar.getStyleClass().add("button-primary");
        botonAceptar.addEventFilter(ActionEvent.ACTION, evento -> {
            try {
                accion.run();
            } catch (RuntimeException e) {
                error.setText(Errores.mensaje(e));
                error.setVisible(true);
                evento.consume();
                dialogo.getDialogPane().getScene().getWindow().sizeToScene();
            }
        });
        return dialogo.showAndWait().filter(aceptar::equals).isPresent();
    }

    /** Ventana de solo lectura con un contenido cualquiera (tabla, detalle...) y botón "Cerrar". */
    public static void informacion(Window propietario, String titulo, String descripcion, Node contenido) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        if (propietario != null) {
            dialogo.initOwner(propietario);
        }
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(titulo);
        dialogo.getDialogPane().getButtonTypes().setAll(new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE));

        VBox cuerpo = new VBox(14);
        if (descripcion != null && !descripcion.isBlank()) {
            Label texto = new Label(descripcion);
            texto.getStyleClass().add("muted");
            texto.setWrapText(true);
            cuerpo.getChildren().add(texto);
        }
        cuerpo.getChildren().add(contenido);
        cuerpo.setPrefWidth(640);
        dialogo.getDialogPane().setContent(cuerpo);
        Estilos.aplicar(dialogo.getDialogPane());
        dialogo.showAndWait();
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
