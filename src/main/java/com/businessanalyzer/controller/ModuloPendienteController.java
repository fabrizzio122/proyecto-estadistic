package com.businessanalyzer.controller;

import com.businessanalyzer.ui.Modulo;
import com.businessanalyzer.ui.VistaModulo;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.kordamp.ikonli.javafx.FontIcon;

/** Pantalla de los modulos que aun no se implementan: indica en que fase se construyen. */
public class ModuloPendienteController implements VistaModulo {

    @FXML private FontIcon icono;
    @FXML private Label titulo;
    @FXML private Label descripcion;
    @FXML private Label fase;

    @Override
    public void inicializar(Modulo modulo) {
        icono.setIconLiteral(modulo.icono());
        titulo.setText(modulo.titulo());
        descripcion.setText(modulo.descripcion() + ". Este módulo está en construcción.");
        fase.setText("Se implementa en la Fase " + modulo.fase());
    }
}
