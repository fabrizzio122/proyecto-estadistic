package com.businessanalyzer.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

/**
 * Medidor circular de la salud del negocio: un anillo que se llena según el puntaje (sobre 100) y la
 * nota en el centro. El anillo se dibuja al cargar (1,2 s). Su color lo da la clase de semáforo.
 */
public final class MedidorSaludFGEM extends StackPane {

    private final Arc progresoFGEM;
    private final Label notaFGEM = new Label("—");

    public MedidorSaludFGEM(double radioFGEM) {
        getStyleClass().add("medidor-salud");
        Arc pistaFGEM = arcoFGEM(radioFGEM, -360);
        pistaFGEM.getStyleClass().add("medidor-pista");
        progresoFGEM = arcoFGEM(radioFGEM, 0);
        progresoFGEM.getStyleClass().add("medidor-progreso");
        notaFGEM.getStyleClass().add("medidor-nota");
        Label sobreFGEM = new Label("de 100");
        sobreFGEM.getStyleClass().add("medidor-sobre");
        VBox centroFGEM = new VBox(-2, notaFGEM, sobreFGEM);
        centroFGEM.setAlignment(javafx.geometry.Pos.CENTER);
        // Un círculo transparente fija el tamaño: un arco corto tiene límites más chicos y se correría.
        javafx.scene.shape.Circle baseFGEM = new javafx.scene.shape.Circle(0, 0, radioFGEM + 7, javafx.scene.paint.Color.TRANSPARENT);
        getChildren().addAll(new javafx.scene.Group(baseFGEM, pistaFGEM, progresoFGEM), centroFGEM);
        double ladoFGEM = radioFGEM * 2 + 14;
        setMinSize(ladoFGEM, ladoFGEM);
        setMaxSize(ladoFGEM, ladoFGEM);
    }

    /**
     * @param puntajeFGEM nota de 0 a 100
     * @param claseFGEM   clase de color del semáforo ({@code semaforo-verde}, {@code semaforo-amarillo}...)
     */
    public void mostrarFGEM(int puntajeFGEM, String claseFGEM) {
        notaFGEM.setText(String.valueOf(puntajeFGEM));
        progresoFGEM.getStyleClass().removeIf(cFGEM -> cFGEM.startsWith("semaforo-"));
        progresoFGEM.getStyleClass().add(claseFGEM);
        progresoFGEM.setLength(0);
        AnimacionesFGEM.haciaFGEM(progresoFGEM.lengthProperty(), -360.0 * Math.max(0, Math.min(100, puntajeFGEM)) / 100,
                Duration.millis(1200), Duration.millis(150));
    }

    private static Arc arcoFGEM(double radioFGEM, double largoFGEM) {
        Arc arcFGEM = new Arc(0, 0, radioFGEM, radioFGEM, 90, largoFGEM);
        arcFGEM.setType(ArcType.OPEN);
        arcFGEM.setStrokeLineCap(StrokeLineCap.ROUND);
        arcFGEM.setFill(null);
        arcFGEM.setManaged(true);
        return arcFGEM;
    }
}
