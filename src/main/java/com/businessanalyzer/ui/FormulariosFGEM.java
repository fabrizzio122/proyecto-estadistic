package com.businessanalyzer.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Construcción uniforme de formularios: etiqueta a la izquierda, campo a la derecha. */
public final class FormulariosFGEM {

    private FormulariosFGEM() {
    }

    public static GridPane grillaFGEM() {
        GridPane grillaFGEM = new GridPane();
        grillaFGEM.setHgap(14);
        grillaFGEM.setVgap(12);
        ColumnConstraints etiquetasFGEM = new ColumnConstraints();
        etiquetasFGEM.setMinWidth(150);
        ColumnConstraints camposFGEM = new ColumnConstraints();
        camposFGEM.setHgrow(Priority.ALWAYS);
        camposFGEM.setMinWidth(260);
        grillaFGEM.getColumnConstraints().addAll(etiquetasFGEM, camposFGEM);
        return grillaFGEM;
    }

    public static void filaFGEM(GridPane grillaFGEM, String etiquetaFGEM, Node campoFGEM) {
        int filaFGEM = grillaFGEM.getRowCount();
        Label labelFGEM = new Label(etiquetaFGEM);
        labelFGEM.getStyleClass().add("form-label");
        labelFGEM.setMinWidth(Region.USE_PREF_SIZE); // la etiqueta nunca se corta con «…»
        grillaFGEM.add(labelFGEM, 0, filaFGEM);
        grillaFGEM.add(campoFGEM, 1, filaFGEM);
        if (campoFGEM instanceof Region regionFGEM) {
            regionFGEM.setMaxWidth(Double.MAX_VALUE);
        }
    }

    /** Texto de ayuda bajo el último campo. */
    public static void notaFGEM(GridPane grillaFGEM, String textoFGEM) {
        Label notaFGEM = new Label(textoFGEM);
        notaFGEM.getStyleClass().add("hint");
        notaFGEM.setWrapText(true);
        // Con un ancho de referencia y alto mínimo = preferido, el texto baja de línea en vez de cortarse.
        notaFGEM.setPrefWidth(280);
        notaFGEM.setMaxWidth(Double.MAX_VALUE);
        notaFGEM.setMinHeight(Region.USE_PREF_SIZE);
        grillaFGEM.add(notaFGEM, 1, grillaFGEM.getRowCount());
    }
}
