package com.businessanalyzer.ui;

import com.businessanalyzer.service.PreferenciasServiceFGEM;
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
public final class PanelAparienciaFGEM extends VBox {

    private final PreferenciasServiceFGEM preferenciasFGEM;
    private final List<StackPane> muestrasFGEM = new ArrayList<>();
    private final Label lblColorFGEM = new Label();

    public PanelAparienciaFGEM(PreferenciasServiceFGEM preferenciasFGEM) {
        super(10);
        this.preferenciasFGEM = preferenciasFGEM;

        Label tituloFGEM = new Label("Color de acento");
        tituloFGEM.getStyleClass().add("form-label");
        HBox coloresFGEM = new HBox(10);
        for (ColorAcentoFGEM colorFGEM : ColorAcentoFGEM.values()) {
            coloresFGEM.getChildren().add(muestraFGEM(colorFGEM));
        }
        lblColorFGEM.getStyleClass().add("info-value");
        HBox filaColoresFGEM = new HBox(14, coloresFGEM, lblColorFGEM);
        filaColoresFGEM.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label ayudaColorFGEM = new Label("Se guarda en tu usuario: el botón principal, el elemento activo del menú y lo "
                + "destacado en los gráficos usan este color.");
        ayudaColorFGEM.getStyleClass().add("hint");
        ayudaColorFGEM.setWrapText(true);
        ayudaColorFGEM.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        CheckBox reducirFGEM = new CheckBox("Reducir animaciones");
        reducirFGEM.setSelected(preferenciasFGEM.animacionesReducidasFGEM());
        reducirFGEM.selectedProperty().addListener((oFGEM, antesFGEM, ahoraFGEM) -> {
            preferenciasFGEM.reducirAnimacionesFGEM(ahoraFGEM);
            AnimacionesFGEM.reducirFGEM(ahoraFGEM);
            NotificadorFGEM.infoFGEM(ahoraFGEM ? "Las barras y los gráficos aparecerán sin moverse."
                    : "Animaciones activadas.");
        });
        Label ayudaAnimacionesFGEM = new Label("Las barras, los medidores y la lámpara del acceso aparecen quietos. "
                + "Útil en equipos lentos o si el movimiento te incomoda. Se guarda en este equipo.");
        ayudaAnimacionesFGEM.getStyleClass().add("hint");
        ayudaAnimacionesFGEM.setWrapText(true);
        ayudaAnimacionesFGEM.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        javafx.scene.layout.Region espacioFGEM = new javafx.scene.layout.Region();
        espacioFGEM.setMinHeight(6);
        getChildren().addAll(tituloFGEM, filaColoresFGEM, ayudaColorFGEM, espacioFGEM, reducirFGEM, ayudaAnimacionesFGEM);
        marcarActualFGEM();
    }

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
        try {
            preferenciasFGEM.guardarColorDelUsuarioFGEM(colorFGEM.name());
            preferenciasFGEM.guardarUltimoColorFGEM(colorFGEM.name());
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM("No se pudo guardar el color: " + ErroresFGEM.mensajeFGEM(eFGEM));
        }
        marcarActualFGEM();
    }

    private void marcarActualFGEM() {
        ColorAcentoFGEM actualFGEM = EstilosFGEM.acentoFGEM();
        for (StackPane muestraFGEM : muestrasFGEM) {
            muestraFGEM.getChildren().get(0).setVisible(muestraFGEM.getUserData() == actualFGEM);
        }
        lblColorFGEM.setText(actualFGEM.nombreFGEM());
    }
}
