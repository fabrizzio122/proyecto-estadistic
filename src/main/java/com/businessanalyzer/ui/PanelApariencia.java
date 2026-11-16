package com.businessanalyzer.ui;

import com.businessanalyzer.service.PreferenciasService;
import javafx.scene.Cursor;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;

/**
 * Preferencias de apariencia del usuario: el color de acento (se guarda en su cuenta y se aplica al
 * instante en todas las ventanas) y «Reducir animaciones» (se guarda en este equipo). Se usa en
 * Configuración y en el menú del usuario, para que cualquier rol pueda elegir su color.
 */
public final class PanelApariencia extends VBox {

    private final PreferenciasService preferencias;
    private final List<StackPane> muestras = new ArrayList<>();
    private final Label lblColor = new Label();

    public PanelApariencia(PreferenciasService preferencias) {
        super(10);
        this.preferencias = preferencias;

        Label titulo = new Label("Color de acento");
        titulo.getStyleClass().add("form-label");
        HBox colores = new HBox(10);
        for (ColorAcento color : ColorAcento.values()) {
            colores.getChildren().add(muestra(color));
        }
        lblColor.getStyleClass().add("info-value");
        HBox filaColores = new HBox(14, colores, lblColor);
        filaColores.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label ayudaColor = new Label("Se guarda en tu usuario: el botón principal, el elemento activo del menú y lo "
                + "destacado en los gráficos usan este color.");
        ayudaColor.getStyleClass().add("hint");
        ayudaColor.setWrapText(true);
        ayudaColor.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        CheckBox reducir = new CheckBox("Reducir animaciones");
        reducir.setSelected(preferencias.animacionesReducidas());
        reducir.selectedProperty().addListener((o, antes, ahora) -> {
            preferencias.reducirAnimaciones(ahora);
            Animaciones.reducir(ahora);
            Notificador.info(ahora ? "Las barras y los gráficos aparecerán sin moverse."
                    : "Animaciones activadas.");
        });
        Label ayudaAnimaciones = new Label("Las barras, los medidores y la lámpara del acceso aparecen quietos. "
                + "Útil en equipos lentos o si el movimiento te incomoda. Se guarda en este equipo.");
        ayudaAnimaciones.getStyleClass().add("hint");
        ayudaAnimaciones.setWrapText(true);
        ayudaAnimaciones.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        javafx.scene.layout.Region espacio = new javafx.scene.layout.Region();
        espacio.setMinHeight(6);
        getChildren().addAll(titulo, filaColores, ayudaColor, espacio, reducir, ayudaAnimaciones);
        marcarActual();
    }

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
        try {
            preferencias.guardarColorDelUsuario(color.name());
            preferencias.guardarUltimoColor(color.name());
        } catch (RuntimeException e) {
            Notificador.error("No se pudo guardar el color: " + Errores.mensaje(e));
        }
        marcarActual();
    }

    private void marcarActual() {
        ColorAcento actual = Estilos.acento();
        for (StackPane muestra : muestras) {
            muestra.getChildren().get(0).setVisible(muestra.getUserData() == actual);
        }
        lblColor.setText(actual.nombre());
    }
}
