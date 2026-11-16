package com.businessanalyzer.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/** Tarjeta de indicador (KPI): titulo, icono, valor principal y una linea de detalle. */
public class TarjetaIndicadorFGEM extends VBox {

    public static final String SIN_VALOR_FGEM = "—";

    private final Label valorFGEM = new Label(SIN_VALOR_FGEM);
    private final Label detalleFGEM = new Label();

    public TarjetaIndicadorFGEM(String tituloFGEM, String iconoFGEM) {
        Label lblTituloFGEM = new Label(tituloFGEM);
        lblTituloFGEM.getStyleClass().add("kpi-title");
        FontIcon iconFGEM = new FontIcon(iconoFGEM);
        iconFGEM.getStyleClass().add("kpi-icon");
        Region espacioFGEM = new Region();
        HBox.setHgrow(espacioFGEM, Priority.ALWAYS);
        HBox cabeceraFGEM = new HBox(8, lblTituloFGEM, espacioFGEM, iconFGEM);
        cabeceraFGEM.setAlignment(Pos.CENTER_LEFT);

        valorFGEM.getStyleClass().add("kpi-value");
        detalleFGEM.getStyleClass().add("kpi-detail");
        detalleFGEM.setWrapText(true);
        detalleFGEM.setMinHeight(Region.USE_PREF_SIZE);

        getStyleClass().add("kpi-card");
        getChildren().addAll(cabeceraFGEM, valorFGEM, detalleFGEM);
        setSpacing(6);
        setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this, Priority.ALWAYS);
    }

    public void actualizarFGEM(String nuevoValorFGEM, String nuevoDetalleFGEM) {
        valorFGEM.setText(nuevoValorFGEM);
        detalleFGEM.setText(nuevoDetalleFGEM);
    }
}
