package com.businessanalyzer.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Mitad izquierda de las pantallas de acceso: logo, lámpara colgante, siete barras que suben al abrir,
 * la frase del producto y el selector «Color de la luz».
 * <ul>
 *   <li>Lámpara encendida: el foco y un resplandor del color de acento iluminan el panel.</li>
 *   <li>Apagada: foco gris, sin resplandor y el contenido baja a 45 % de opacidad.</li>
 *   <li>Elegir un color cambia el acento de toda la pantalla, enciende la lámpara y avisa «Color guardado».</li>
 * </ul>
 */
public final class PanelMarcaFGEM extends StackPane {

    /** Altura relativa de las siete barras (la sexta es la más alta y va en el color de acento). */
    private static final double[] ALTURAS_FGEM = {0.36, 0.48, 0.42, 0.58, 0.7, 0.96, 0.6};
    private static final double ALTURA_MAXIMA_FGEM = 175;
    private static final Duration TRANSICION_FGEM = Duration.millis(550);

    private final Region resplandorFGEM = new Region();
    private final BorderPane contenidoFGEM = new BorderPane();
    private final LamparaFGEM lamparaFGEM = new LamparaFGEM(78);
    private final Label lblColorFGEM = new Label("Color de la luz");
    private final List<StackPane> muestrasFGEM = new ArrayList<>();
    private final List<Region> barrasFGEM = new ArrayList<>();
    private Consumer<ColorAcentoFGEM> alElegirColorFGEM = cFGEM -> {
    };
    private boolean animadoFGEM;

    public PanelMarcaFGEM() {
        getStyleClass().add("panel-marca");
        setMinWidth(420);

        resplandorFGEM.getStyleClass().add("panel-marca-resplandor");
        resplandorFGEM.setMouseTransparent(true);

        contenidoFGEM.setPadding(new Insets(44, 52, 40, 52));
        contenidoFGEM.setTop(logoFGEM());
        contenidoFGEM.setCenter(centroFGEM());
        contenidoFGEM.setBottom(pieFGEM());

        Pane capaLamparaFGEM = new Pane(lamparaFGEM);
        capaLamparaFGEM.setPickOnBounds(false);
        lamparaFGEM.layoutXProperty().bind(widthProperty().subtract(150));
        lamparaFGEM.setLayoutY(0);
        lamparaFGEM.setId("lampara");

        getChildren().addAll(resplandorFGEM, contenidoFGEM, capaLamparaFGEM);

        resplandorFGEM.setOpacity(0.6);
        lamparaFGEM.encendidaPropertyFGEM().addListener((oFGEM, antesFGEM, encendidaFGEM) -> {
            AnimacionesFGEM.opacidadFGEM(resplandorFGEM, encendidaFGEM ? 0.6 : 0, TRANSICION_FGEM);
            AnimacionesFGEM.opacidadFGEM(contenidoFGEM, encendidaFGEM ? 1 : 0.45, TRANSICION_FGEM);
        });
        // Las barras suben una tras otra la primera vez que el panel aparece en pantalla.
        sceneProperty().addListener((oFGEM, antesFGEM, escenaFGEM) -> {
            if (escenaFGEM != null && !animadoFGEM) {
                animadoFGEM = true;
                animarBarrasFGEM();
            }
        });
        marcarColorActualFGEM();
    }

    /** Se llama al elegir un color (por ejemplo, para guardarlo). */
    public void setAlElegirColorFGEM(Consumer<ColorAcentoFGEM> accionFGEM) {
        this.alElegirColorFGEM = accionFGEM == null ? cFGEM -> {
        } : accionFGEM;
    }

    /** Refleja el color de acento actual en el selector (por ejemplo, al elegir un usuario con otro color). */
    public void marcarColorActualFGEM() {
        ColorAcentoFGEM actualFGEM = EstilosFGEM.acentoFGEM();
        for (StackPane muestraFGEM : muestrasFGEM) {
            boolean elegidaFGEM = muestraFGEM.getUserData() == actualFGEM;
            muestraFGEM.getChildren().get(0).setVisible(elegidaFGEM);
        }
    }

    private HBox logoFGEM() {
        HBox marcaFGEM = new HBox(10, LogoFGEM.nuevoFGEM(), new Label("Business Analyzer"));
        marcaFGEM.getChildren().get(1).getStyleClass().add("marca-nombre");
        marcaFGEM.setAlignment(Pos.CENTER_LEFT);
        return marcaFGEM;
    }

    private VBox centroFGEM() {
        HBox filaFGEM = new HBox(14);
        filaFGEM.setAlignment(Pos.BOTTOM_LEFT);
        filaFGEM.setMinHeight(ALTURA_MAXIMA_FGEM);
        filaFGEM.setPrefHeight(ALTURA_MAXIMA_FGEM);
        filaFGEM.setMaxWidth(560);
        for (int iFGEM = 0; iFGEM < ALTURAS_FGEM.length; iFGEM++) {
            Region barraFGEM = new Region();
            barraFGEM.getStyleClass().add("barra-marca");
            if (iFGEM == 5) {
                barraFGEM.getStyleClass().add("barra-marca-acento");
            } else if (iFGEM == 6) {
                barraFGEM.getStyleClass().add("barra-marca-exito");
            } else {
                barraFGEM.getStyleClass().add("barra-marca-" + (iFGEM % 3));
            }
            barraFGEM.setPrefWidth(66);
            barraFGEM.setMaxWidth(90);
            HBox.setHgrow(barraFGEM, Priority.ALWAYS);
            barraFGEM.setMinHeight(0);
            barraFGEM.setPrefHeight(AnimacionesFGEM.reducidasFGEM() ? ALTURAS_FGEM[iFGEM] * ALTURA_MAXIMA_FGEM : 0);
            barraFGEM.setMaxHeight(Region.USE_PREF_SIZE);
            barrasFGEM.add(barraFGEM);
            filaFGEM.getChildren().add(barraFGEM);
        }
        Label fraseFGEM = new Label("Los números de tu negocio, explicados en simple.");
        fraseFGEM.getStyleClass().add("marca-frase");
        fraseFGEM.setWrapText(true);
        fraseFGEM.setMaxWidth(470);
        VBox centroFGEM = new VBox(34, filaFGEM, fraseFGEM);
        centroFGEM.setAlignment(Pos.CENTER_LEFT);
        centroFGEM.setPadding(new Insets(110, 0, 0, 0));
        return centroFGEM;
    }

    private void animarBarrasFGEM() {
        for (int iFGEM = 0; iFGEM < barrasFGEM.size(); iFGEM++) {
            AnimacionesFGEM.haciaFGEM(barrasFGEM.get(iFGEM).prefHeightProperty(), ALTURAS_FGEM[iFGEM] * ALTURA_MAXIMA_FGEM,
                    Duration.millis(700), Duration.millis(150 + 70L * iFGEM));
        }
    }

    private HBox pieFGEM() {
        FontIcon candadoFGEM = new FontIcon("fth-lock");
        candadoFGEM.getStyleClass().add("marca-candado");
        Label privadoFGEM = new Label("Tus datos se guardan solo en este equipo.");
        privadoFGEM.getStyleClass().add("marca-pie");
        Region espacioFGEM = new Region();
        HBox.setHgrow(espacioFGEM, Priority.ALWAYS);
        lblColorFGEM.getStyleClass().add("marca-pie");
        HBox coloresFGEM = new HBox(10);
        coloresFGEM.setAlignment(Pos.CENTER);
        for (ColorAcentoFGEM colorFGEM : ColorAcentoFGEM.values()) {
            coloresFGEM.getChildren().add(muestraFGEM(colorFGEM));
        }
        HBox pieFGEM = new HBox(10, candadoFGEM, privadoFGEM, espacioFGEM, lblColorFGEM, coloresFGEM);
        pieFGEM.setAlignment(Pos.CENTER_LEFT);
        pieFGEM.setPadding(new Insets(18, 0, 0, 0));
        return pieFGEM;
    }

    /** Círculo de un color; el elegido lleva un anillo alrededor. */
    private StackPane muestraFGEM(ColorAcentoFGEM colorFGEM) {
        Circle anilloFGEM = new Circle(13);
        anilloFGEM.getStyleClass().add("muestra-anillo");
        anilloFGEM.setStroke(colorFGEM.colorFGEM());
        Circle puntoFGEM = new Circle(9, colorFGEM.colorFGEM());
        StackPane muestraFGEM = new StackPane(anilloFGEM, puntoFGEM);
        muestraFGEM.setUserData(colorFGEM);
        muestraFGEM.setMinSize(28, 28);
        muestraFGEM.setCursor(Cursor.HAND);
        muestraFGEM.setFocusTraversable(true);
        muestraFGEM.getStyleClass().add("muestra-color");
        Tooltip.install(muestraFGEM, new Tooltip(colorFGEM.nombreFGEM()));
        muestraFGEM.setOnMouseClicked(eFGEM -> elegirFGEM(colorFGEM));
        muestraFGEM.setOnKeyPressed(eFGEM -> {
            if (eFGEM.getCode() == KeyCode.ENTER || eFGEM.getCode() == KeyCode.SPACE) {
                elegirFGEM(colorFGEM);
            }
        });
        muestrasFGEM.add(muestraFGEM);
        return muestraFGEM;
    }

    private void elegirFGEM(ColorAcentoFGEM colorFGEM) {
        EstilosFGEM.cambiarAcentoFGEM(colorFGEM);
        marcarColorActualFGEM();
        lamparaFGEM.encenderFGEM();
        lblColorFGEM.setText("Color guardado");
        PauseTransition volverFGEM = new PauseTransition(Duration.seconds(2.5));
        volverFGEM.setOnFinished(eFGEM -> lblColorFGEM.setText("Color de la luz"));
        volverFGEM.play();
        alElegirColorFGEM.accept(colorFGEM);
    }
}
