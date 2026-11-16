package com.businessanalyzer.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * Barra horizontal con una pista y un relleno proporcional (0 a 1), opcionalmente con una marca
 * vertical (por ejemplo, dónde está el punto de equilibrio) y un tramo resaltado (un rango probable).
 * El relleno crece al mostrarse. El color del relleno se elige con una clase de estilo.
 */
public final class BarraProporcionFGEM extends Region {

    private final Region pistaFGEM = new Region();
    private final Region tramoFGEM = new Region();
    private final Region rellenoFGEM = new Region();
    private final Region marcaFGEM = new Region();
    private final DoubleProperty proporcionFGEM = new SimpleDoubleProperty(0);
    private double desdeTramoFGEM = -1;
    private double hastaTramoFGEM = -1;
    private double posicionMarcaFGEM = -1;
    private final double altoFGEM;

    /**
     * @param altoFGEM          grosor de la barra en píxeles
     * @param claseRellenoFGEM  clase CSS del relleno ({@code relleno-acento}, {@code relleno-neutro}...)
     */
    public BarraProporcionFGEM(double altoFGEM, String claseRellenoFGEM) {
        this.altoFGEM = altoFGEM;
        getStyleClass().add("barra-proporcion");
        pistaFGEM.getStyleClass().add("barra-proporcion-pista");
        tramoFGEM.getStyleClass().add("barra-proporcion-tramo");
        rellenoFGEM.getStyleClass().addAll("barra-proporcion-relleno", claseRellenoFGEM);
        marcaFGEM.getStyleClass().add("barra-proporcion-marca");
        tramoFGEM.setVisible(false);
        marcaFGEM.setVisible(false);
        getChildren().addAll(pistaFGEM, tramoFGEM, rellenoFGEM, marcaFGEM);
        proporcionFGEM.addListener((oFGEM, aFGEM, nFGEM) -> requestLayout());
        setMinHeight(altoFGEM + 8);
        setPrefHeight(altoFGEM + 8);
        setMaxHeight(altoFGEM + 8);
        setMinWidth(40);
    }

    /** Llena la barra hasta {@code valor} (entre 0 y 1). */
    public BarraProporcionFGEM llenarFGEM(double valorFGEM) {
        AnimacionesFGEM.haciaFGEM(proporcionFGEM, limitarFGEM(valorFGEM), Duration.millis(800), Duration.millis(100));
        return this;
    }

    /** Marca vertical en la posición indicada (entre 0 y 1). */
    public BarraProporcionFGEM marcarFGEM(double posicionFGEM) {
        posicionMarcaFGEM = limitarFGEM(posicionFGEM);
        marcaFGEM.setVisible(true);
        requestLayout();
        return this;
    }

    /** Tramo resaltado entre dos posiciones (entre 0 y 1). */
    public BarraProporcionFGEM resaltarFGEM(double desdeFGEM, double hastaFGEM) {
        desdeTramoFGEM = limitarFGEM(Math.min(desdeFGEM, hastaFGEM));
        hastaTramoFGEM = limitarFGEM(Math.max(desdeFGEM, hastaFGEM));
        tramoFGEM.setVisible(true);
        requestLayout();
        return this;
    }

    @Override
    protected void layoutChildren() {
        double anchoFGEM = getWidth();
        double yFGEM = (getHeight() - altoFGEM) / 2;
        pistaFGEM.resizeRelocate(0, yFGEM, anchoFGEM, altoFGEM);
        if (desdeTramoFGEM >= 0) {
            tramoFGEM.resizeRelocate(anchoFGEM * desdeTramoFGEM, yFGEM, anchoFGEM * (hastaTramoFGEM - desdeTramoFGEM), altoFGEM);
        }
        rellenoFGEM.resizeRelocate(0, yFGEM, anchoFGEM * proporcionFGEM.get(), altoFGEM);
        if (posicionMarcaFGEM >= 0) {
            double xFGEM = Math.max(0, Math.min(anchoFGEM - 3, anchoFGEM * posicionMarcaFGEM - 1.5));
            marcaFGEM.resizeRelocate(xFGEM, 0, 3, getHeight());
        }
    }

    private static double limitarFGEM(double valorFGEM) {
        return Double.isNaN(valorFGEM) ? 0 : Math.max(0, Math.min(1, valorFGEM));
    }
}
