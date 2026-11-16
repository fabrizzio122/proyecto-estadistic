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
public final class BarraProporcion extends Region {

    private final Region pista = new Region();
    private final Region tramo = new Region();
    private final Region relleno = new Region();
    private final Region marca = new Region();
    private final DoubleProperty proporcion = new SimpleDoubleProperty(0);
    private double desdeTramo = -1;
    private double hastaTramo = -1;
    private double posicionMarca = -1;
    private final double alto;

    /**
     * @param alto          grosor de la barra en píxeles
     * @param claseRelleno  clase CSS del relleno ({@code relleno-acento}, {@code relleno-neutro}...)
     */
    public BarraProporcion(double alto, String claseRelleno) {
        this.alto = alto;
        getStyleClass().add("barra-proporcion");
        pista.getStyleClass().add("barra-proporcion-pista");
        tramo.getStyleClass().add("barra-proporcion-tramo");
        relleno.getStyleClass().addAll("barra-proporcion-relleno", claseRelleno);
        marca.getStyleClass().add("barra-proporcion-marca");
        tramo.setVisible(false);
        marca.setVisible(false);
        getChildren().addAll(pista, tramo, relleno, marca);
        proporcion.addListener((o, a, n) -> requestLayout());
        setMinHeight(alto + 8);
        setPrefHeight(alto + 8);
        setMaxHeight(alto + 8);
        setMinWidth(40);
    }

    /** Llena la barra hasta {@code valor} (entre 0 y 1). */
    public BarraProporcion llenar(double valor) {
        Animaciones.hacia(proporcion, limitar(valor), Duration.millis(800), Duration.millis(100));
        return this;
    }

    /** Marca vertical en la posición indicada (entre 0 y 1). */
    public BarraProporcion marcar(double posicion) {
        posicionMarca = limitar(posicion);
        marca.setVisible(true);
        requestLayout();
        return this;
    }

    /** Tramo resaltado entre dos posiciones (entre 0 y 1). */
    public BarraProporcion resaltar(double desde, double hasta) {
        desdeTramo = limitar(Math.min(desde, hasta));
        hastaTramo = limitar(Math.max(desde, hasta));
        tramo.setVisible(true);
        requestLayout();
        return this;
    }

    @Override
    protected void layoutChildren() {
        double ancho = getWidth();
        double y = (getHeight() - alto) / 2;
        pista.resizeRelocate(0, y, ancho, alto);
        if (desdeTramo >= 0) {
            tramo.resizeRelocate(ancho * desdeTramo, y, ancho * (hastaTramo - desdeTramo), alto);
        }
        relleno.resizeRelocate(0, y, ancho * proporcion.get(), alto);
        if (posicionMarca >= 0) {
            double x = Math.max(0, Math.min(ancho - 3, ancho * posicionMarca - 1.5));
            marca.resizeRelocate(x, 0, 3, getHeight());
        }
    }

    private static double limitar(double valor) {
        return Double.isNaN(valor) ? 0 : Math.max(0, Math.min(1, valor));
    }
}
