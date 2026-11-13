package com.businessanalyzer.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Construcción uniforme de formularios: etiqueta a la izquierda, campo a la derecha. */
public final class Formularios {

    private Formularios() {
    }

    public static GridPane grilla() {
        GridPane grilla = new GridPane();
        grilla.setHgap(14);
        grilla.setVgap(12);
        ColumnConstraints etiquetas = new ColumnConstraints();
        etiquetas.setMinWidth(150);
        ColumnConstraints campos = new ColumnConstraints();
        campos.setHgrow(Priority.ALWAYS);
        campos.setMinWidth(260);
        grilla.getColumnConstraints().addAll(etiquetas, campos);
        return grilla;
    }

    public static void fila(GridPane grilla, String etiqueta, Node campo) {
        int fila = grilla.getRowCount();
        Label label = new Label(etiqueta);
        label.getStyleClass().add("form-label");
        grilla.add(label, 0, fila);
        grilla.add(campo, 1, fila);
        if (campo instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
    }

    /** Texto de ayuda bajo el último campo. */
    public static void nota(GridPane grilla, String texto) {
        Label nota = new Label(texto);
        nota.getStyleClass().add("hint");
        nota.setWrapText(true);
        grilla.add(nota, 1, grilla.getRowCount());
    }
}
