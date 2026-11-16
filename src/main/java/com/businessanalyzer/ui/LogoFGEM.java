package com.businessanalyzer.ui;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/** Logo de la aplicación: tres barras verticales (dos en color de acento y una en verde). */
public final class LogoFGEM {

    private LogoFGEM() {
    }

    public static HBox nuevoFGEM() {
        HBox logoFGEM = new HBox(3, barraFGEM(11, "logo-barra"), barraFGEM(19, "logo-barra"), barraFGEM(14, "logo-barra-exito"));
        logoFGEM.setAlignment(Pos.BOTTOM_LEFT);
        logoFGEM.setMinHeight(19);
        logoFGEM.setMaxHeight(19);
        return logoFGEM;
    }

    private static Region barraFGEM(double altoFGEM, String claseFGEM) {
        Region barraFGEM = new Region();
        barraFGEM.getStyleClass().add(claseFGEM);
        barraFGEM.setMinSize(4.5, altoFGEM);
        barraFGEM.setPrefSize(4.5, altoFGEM);
        barraFGEM.setMaxSize(4.5, altoFGEM);
        return barraFGEM;
    }
}
