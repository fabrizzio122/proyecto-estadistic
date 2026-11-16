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
public final class PanelMarca extends StackPane {

    /** Altura relativa de las siete barras (la sexta es la más alta y va en el color de acento). */
    private static final double[] ALTURAS = {0.36, 0.48, 0.42, 0.58, 0.7, 0.96, 0.6};
    private static final double ALTURA_MAXIMA = 175;
    private static final Duration TRANSICION = Duration.millis(550);

    private final Region resplandor = new Region();
    private final BorderPane contenido = new BorderPane();
    private final Lampara lampara = new Lampara(78);
    private final Label lblColor = new Label("Color de la luz");
    private final List<StackPane> muestras = new ArrayList<>();
    private final List<Region> barras = new ArrayList<>();
    private Consumer<ColorAcento> alElegirColor = c -> {
    };
    private boolean animado;

    public PanelMarca() {
        getStyleClass().add("panel-marca");
        setMinWidth(420);

        resplandor.getStyleClass().add("panel-marca-resplandor");
        resplandor.setMouseTransparent(true);

        contenido.setPadding(new Insets(44, 52, 40, 52));
        contenido.setTop(logo());
        contenido.setCenter(centro());
        contenido.setBottom(pie());

        Pane capaLampara = new Pane(lampara);
        capaLampara.setPickOnBounds(false);
        lampara.layoutXProperty().bind(widthProperty().subtract(150));
        lampara.setLayoutY(0);
        lampara.setId("lampara");

        getChildren().addAll(resplandor, contenido, capaLampara);

        resplandor.setOpacity(0.6);
        lampara.encendidaProperty().addListener((o, antes, encendida) -> {
            Animaciones.opacidad(resplandor, encendida ? 0.6 : 0, TRANSICION);
            Animaciones.opacidad(contenido, encendida ? 1 : 0.45, TRANSICION);
        });
        // Las barras suben una tras otra la primera vez que el panel aparece en pantalla.
        sceneProperty().addListener((o, antes, escena) -> {
            if (escena != null && !animado) {
                animado = true;
                animarBarras();
            }
        });
        marcarColorActual();
    }

    /** Se llama al elegir un color (por ejemplo, para guardarlo). */
    public void setAlElegirColor(Consumer<ColorAcento> accion) {
        this.alElegirColor = accion == null ? c -> {
        } : accion;
    }

    /** Refleja el color de acento actual en el selector (por ejemplo, al elegir un usuario con otro color). */
    public void marcarColorActual() {
        ColorAcento actual = Estilos.acento();
        for (StackPane muestra : muestras) {
            boolean elegida = muestra.getUserData() == actual;
            muestra.getChildren().get(0).setVisible(elegida);
        }
    }

    private HBox logo() {
        HBox marca = new HBox(10, Logo.nuevo(), new Label("Business Analyzer"));
        marca.getChildren().get(1).getStyleClass().add("marca-nombre");
        marca.setAlignment(Pos.CENTER_LEFT);
        return marca;
    }

    private VBox centro() {
        HBox fila = new HBox(14);
        fila.setAlignment(Pos.BOTTOM_LEFT);
        fila.setMinHeight(ALTURA_MAXIMA);
        fila.setPrefHeight(ALTURA_MAXIMA);
        fila.setMaxWidth(560);
        for (int i = 0; i < ALTURAS.length; i++) {
            Region barra = new Region();
            barra.getStyleClass().add("barra-marca");
            if (i == 5) {
                barra.getStyleClass().add("barra-marca-acento");
            } else if (i == 6) {
                barra.getStyleClass().add("barra-marca-exito");
            } else {
                barra.getStyleClass().add("barra-marca-" + (i % 3));
            }
            barra.setPrefWidth(66);
            barra.setMaxWidth(90);
            HBox.setHgrow(barra, Priority.ALWAYS);
            barra.setMinHeight(0);
            barra.setPrefHeight(Animaciones.reducidas() ? ALTURAS[i] * ALTURA_MAXIMA : 0);
            barra.setMaxHeight(Region.USE_PREF_SIZE);
            barras.add(barra);
            fila.getChildren().add(barra);
        }
        Label frase = new Label("Los números de tu negocio, explicados en simple.");
        frase.getStyleClass().add("marca-frase");
        frase.setWrapText(true);
        frase.setMaxWidth(470);
        VBox centro = new VBox(34, fila, frase);
        centro.setAlignment(Pos.CENTER_LEFT);
        centro.setPadding(new Insets(110, 0, 0, 0));
        return centro;
    }

    private void animarBarras() {
        for (int i = 0; i < barras.size(); i++) {
            Animaciones.hacia(barras.get(i).prefHeightProperty(), ALTURAS[i] * ALTURA_MAXIMA,
                    Duration.millis(700), Duration.millis(150 + 70L * i));
        }
    }

    private HBox pie() {
        FontIcon candado = new FontIcon("fth-lock");
        candado.getStyleClass().add("marca-candado");
        Label privado = new Label("Tus datos se guardan solo en este equipo.");
        privado.getStyleClass().add("marca-pie");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        lblColor.getStyleClass().add("marca-pie");
        HBox colores = new HBox(10);
        colores.setAlignment(Pos.CENTER);
        for (ColorAcento color : ColorAcento.values()) {
            colores.getChildren().add(muestra(color));
        }
        HBox pie = new HBox(10, candado, privado, espacio, lblColor, colores);
        pie.setAlignment(Pos.CENTER_LEFT);
        pie.setPadding(new Insets(18, 0, 0, 0));
        return pie;
    }

    /** Círculo de un color; el elegido lleva un anillo alrededor. */
    private StackPane muestra(ColorAcento color) {
        Circle anillo = new Circle(13);
        anillo.getStyleClass().add("muestra-anillo");
        anillo.setStroke(color.color());
        Circle punto = new Circle(9, color.color());
        StackPane muestra = new StackPane(anillo, punto);
        muestra.setUserData(color);
        muestra.setMinSize(28, 28);
        muestra.setCursor(Cursor.HAND);
        muestra.setFocusTraversable(true);
        muestra.getStyleClass().add("muestra-color");
        Tooltip.install(muestra, new Tooltip(color.nombre()));
        muestra.setOnMouseClicked(e -> elegir(color));
        muestra.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                elegir(color);
            }
        });
        muestras.add(muestra);
        return muestra;
    }

    private void elegir(ColorAcento color) {
        Estilos.cambiarAcento(color);
        marcarColorActual();
        lampara.encender();
        lblColor.setText("Color guardado");
        PauseTransition volver = new PauseTransition(Duration.seconds(2.5));
        volver.setOnFinished(e -> lblColor.setText("Color de la luz"));
        volver.play();
        alElegirColor.accept(color);
    }
}
