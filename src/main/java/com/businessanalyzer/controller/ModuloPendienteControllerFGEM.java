package com.businessanalyzer.controller;

import com.businessanalyzer.ui.ModuloFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.kordamp.ikonli.javafx.FontIcon;

/** Pantalla de los modulos que aun no se implementan: indica en que fase se construyen. */
public class ModuloPendienteControllerFGEM implements VistaModuloFGEM {

    @FXML private FontIcon iconoFGEM;
    @FXML private Label tituloFGEM;
    @FXML private Label descripcionFGEM;
    @FXML private Label faseFGEM;

    @Override
    public void inicializarFGEM(ModuloFGEM moduloFGEM) {
        iconoFGEM.setIconLiteral(moduloFGEM.iconoFGEM());
        tituloFGEM.setText(moduloFGEM.tituloFGEM());
        descripcionFGEM.setText(moduloFGEM.descripcionFGEM() + ". Este módulo está en construcción.");
        faseFGEM.setText("Se implementa en la Fase " + moduloFGEM.faseFGEM());
    }
}
