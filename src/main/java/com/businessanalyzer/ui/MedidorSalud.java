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
public final class MedidorSalud extends StackPane {

    private final Arc progreso;
    private final Label nota = new Label("—");

    public MedidorSalud(double radio) {
        getStyleClass().add("medidor-salud");
        Arc pista = arco(radio, -360);
        pista.getStyleClass().add("medidor-pista");
        progreso = arco(radio, 0);
        progreso.getStyleClass().add("medidor-progreso");
        nota.getStyleClass().add("medidor-nota");
        Label sobre = new Label("de 100");
        sobre.getStyleClass().add("medidor-sobre");
        VBox centro = new VBox(-2, nota, sobre);
        centro.setAlignment(javafx.geometry.Pos.CENTER);
        // Un círculo transparente fija el tamaño: un arco corto tiene límites más chicos y se correría.
        javafx.scene.shape.Circle base = new javafx.scene.shape.Circle(0, 0, radio + 7, javafx.scene.paint.Color.TRANSPARENT);
        getChildren().addAll(new javafx.scene.Group(base, pista, progreso), centro);
        double lado = radio * 2 + 14;
        setMinSize(lado, lado);
        setMaxSize(lado, lado);
    }

    /**
     * @param puntaje nota de 0 a 100
     * @param clase   clase de color del semáforo ({@code semaforo-verde}, {@code semaforo-amarillo}...)
     */
    public void mostrar(int puntaje, String clase) {
        nota.setText(String.valueOf(puntaje));
        progreso.getStyleClass().removeIf(c -> c.startsWith("semaforo-"));
        progreso.getStyleClass().add(clase);
        progreso.setLength(0);
        Animaciones.hacia(progreso.lengthProperty(), -360.0 * Math.max(0, Math.min(100, puntaje)) / 100,
                Duration.millis(1200), Duration.millis(150));
    }

    private static Arc arco(double radio, double largo) {
        Arc arc = new Arc(0, 0, radio, radio, 90, largo);
        arc.setType(ArcType.OPEN);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.setFill(null);
        arc.setManaged(true);
        return arc;
    }
}
