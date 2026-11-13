package com.businessanalyzer.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/** Tarjeta de indicador (KPI): titulo, icono, valor principal y una linea de detalle. */
public class TarjetaIndicador extends VBox {

    public static final String SIN_VALOR = "—";

    private final Label valor = new Label(SIN_VALOR);
    private final Label detalle = new Label();

    public TarjetaIndicador(String titulo, String icono) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("kpi-title");
        FontIcon icon = new FontIcon(icono);
        icon.getStyleClass().add("kpi-icon");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(8, lblTitulo, espacio, icon);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        valor.getStyleClass().add("kpi-value");
        detalle.getStyleClass().add("kpi-detail");
        detalle.setWrapText(true);

        getStyleClass().add("kpi-card");
        getChildren().addAll(cabecera, valor, detalle);
        setSpacing(6);
        setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this, Priority.ALWAYS);
    }

    public void actualizar(String nuevoValor, String nuevoDetalle) {
        valor.setText(nuevoValor);
        detalle.setText(nuevoDetalle);
    }
}
