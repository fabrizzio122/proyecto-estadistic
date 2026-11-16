package com.businessanalyzer.ui;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/** Logo de la aplicación: tres barras verticales (dos en color de acento y una en verde). */
public final class Logo {

    private Logo() {
    }

    public static HBox nuevo() {
        HBox logo = new HBox(3, barra(11, "logo-barra"), barra(19, "logo-barra"), barra(14, "logo-barra-exito"));
        logo.setAlignment(Pos.BOTTOM_LEFT);
        logo.setMinHeight(19);
        logo.setMaxHeight(19);
        return logo;
    }

    private static Region barra(double alto, String clase) {
        Region barra = new Region();
        barra.getStyleClass().add(clase);
        barra.setMinSize(4.5, alto);
        barra.setPrefSize(4.5, alto);
        barra.setMaxSize(4.5, alto);
        return barra;
    }
}
