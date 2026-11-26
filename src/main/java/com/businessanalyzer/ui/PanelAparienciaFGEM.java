package com.businessanalyzer.ui;

import com.businessanalyzer.service.PreferenciasServiceFGEM;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class PanelAparienciaFGEM extends VBox {

    public PanelAparienciaFGEM(PreferenciasServiceFGEM preferenciasFGEM) {
        super(10);
        CheckBox reducirFGEM = new CheckBox("Reducir animaciones");
        reducirFGEM.setSelected(preferenciasFGEM.animacionesReducidasFGEM());
        reducirFGEM.selectedProperty().addListener((oFGEM, antesFGEM, ahoraFGEM) -> {
            preferenciasFGEM.reducirAnimacionesFGEM(ahoraFGEM);
            AnimacionesFGEM.reducirFGEM(ahoraFGEM);
            NotificadorFGEM.infoFGEM(ahoraFGEM ? "Los gráficos aparecerán sin moverse." : "Animaciones activadas.");
        });
        Label ayudaFGEM = new Label("Útil en equipos lentos. Se guarda en este equipo.");
        ayudaFGEM.getStyleClass().add("hint");
        ayudaFGEM.setWrapText(true);
        ayudaFGEM.setMinHeight(Region.USE_PREF_SIZE);
        getChildren().addAll(reducirFGEM, ayudaFGEM);
    }
}
