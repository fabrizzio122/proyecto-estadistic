package com.businessanalyzer.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Region;
import javafx.stage.Window;

import java.util.Objects;

/**
 * Aplica la hoja de estilos del tema, las tipografías y el color de acento. El acento se guarda
 * aquí y se pinta sobrescribiendo la variable {@code -ba-acento} en el nodo raíz de cada ventana,
 * así que cambiarlo actualiza toda la aplicación de una vez.
 */
public final class EstilosFGEM {

    public static final String TEMA_OSCURO_FGEM = "/css/tema-oscuro.css";

    private static volatile ColorAcentoFGEM acentoFGEM = ColorAcentoFGEM.PREDETERMINADO_FGEM;

    private EstilosFGEM() {
    }

    public static void aplicarFGEM(Scene escenaFGEM) {
        FuentesFGEM.cargarFGEM();
        escenaFGEM.getStylesheets().setAll(hojaActualFGEM());
        pintarFGEM(escenaFGEM.getRoot());
        if (!escenaFGEM.getProperties().containsKey(EstilosFGEM.class)) {
            escenaFGEM.getProperties().put(EstilosFGEM.class, Boolean.TRUE);
            escenaFGEM.rootProperty().addListener((oFGEM, anteriorFGEM, nuevoFGEM) -> pintarFGEM(nuevoFGEM));
        }
    }

    public static void aplicarFGEM(DialogPane dialogoFGEM) {
        FuentesFGEM.cargarFGEM();
        dialogoFGEM.getStylesheets().setAll(hojaActualFGEM());
        pintarFGEM(dialogoFGEM);
        ajustarBotonesFGEM(dialogoFGEM);
        encabezadosTablaFGEM(dialogoFGEM.getContent());
        if (dialogoFGEM.getProperties().putIfAbsent(ESPANOL_FGEM, Boolean.TRUE) == null) {
            dialogoFGEM.contentProperty().addListener((oFGEM, antesFGEM, ahoraFGEM) -> encabezadosTablaFGEM(ahoraFGEM));
        }
        dialogoFGEM.getButtonTypes().addListener((javafx.collections.ListChangeListener<ButtonType>) cFGEM -> ajustarBotonesFGEM(dialogoFGEM));
    }

    /**
     * Cada botón del diálogo toma el ancho de su propio texto: el ancho uniforme de JavaFX se calcula
     * antes de aplicar las tipografías del tema y cortaba textos como «Registrar venta».
     */
    private static void ajustarBotonesFGEM(DialogPane dialogoFGEM) {
        for (ButtonType tipoFGEM : dialogoFGEM.getButtonTypes()) {
            if (dialogoFGEM.lookupButton(tipoFGEM) instanceof Button botonFGEM) {
                ButtonBar.setButtonUniformSize(botonFGEM, false);
                botonFGEM.setMinWidth(Region.USE_PREF_SIZE);
            }
        }
    }

    private static final java.util.Locale ESPANOL_FGEM = java.util.Locale.forLanguageTag("es");

    /**
     * Encabezados de tabla en mayúsculas pequeñas («FECHA», «MONTO»): el CSS de JavaFX no tiene
     * text-transform, así que se cambia el texto de las columnas. Recorre también el contenido de las
     * pestañas y de los paneles con desplazamiento, y sigue las columnas que se agreguen o renombren después.
     * Solo cambia lo que se ve: las exportaciones usan sus propios títulos.
     */
    public static void encabezadosTablaFGEM(javafx.scene.Node raizFGEM) {
        if (raizFGEM == null) {
            return;
        }
        if (raizFGEM instanceof javafx.scene.control.TableView<?> tablaFGEM) {
            if (tablaFGEM.getProperties().putIfAbsent(ESPANOL_FGEM, Boolean.TRUE) == null) {
                tablaFGEM.getColumns().forEach(EstilosFGEM::mayusculasFGEM);
                tablaFGEM.getColumns().addListener((javafx.collections.ListChangeListener<javafx.scene.control.TableColumn<?, ?>>)
                        cFGEM -> cFGEM.getList().forEach(EstilosFGEM::mayusculasFGEM));
            }
            return;
        }
        if (raizFGEM instanceof javafx.scene.control.TabPane pestanasFGEM) {
            for (javafx.scene.control.Tab pestanaFGEM : pestanasFGEM.getTabs()) {
                // Algunas pestañas cambian su contenido al cargar los datos (Predicciones).
                if (pestanaFGEM.getProperties().putIfAbsent(ESPANOL_FGEM, Boolean.TRUE) == null) {
                    pestanaFGEM.contentProperty().addListener((oFGEM, antesFGEM, ahoraFGEM) -> encabezadosTablaFGEM(ahoraFGEM));
                }
                encabezadosTablaFGEM(pestanaFGEM.getContent());
            }
        }
        if (raizFGEM instanceof javafx.scene.control.ScrollPane desplazableFGEM) {
            encabezadosTablaFGEM(desplazableFGEM.getContent());
        }
        if (raizFGEM instanceof javafx.scene.control.SplitPane divisionFGEM) {
            divisionFGEM.getItems().forEach(EstilosFGEM::encabezadosTablaFGEM);
        }
        if (raizFGEM instanceof javafx.scene.control.TitledPane plegableFGEM) {
            encabezadosTablaFGEM(plegableFGEM.getContent());
        }
        if (raizFGEM instanceof Parent padreFGEM) {
            padreFGEM.getChildrenUnmodifiable().forEach(EstilosFGEM::encabezadosTablaFGEM);
        }
    }

    private static void mayusculasFGEM(javafx.scene.control.TableColumnBase<?, ?> columnaFGEM) {
        if (columnaFGEM.getProperties().putIfAbsent(ESPANOL_FGEM, Boolean.TRUE) == null) {
            columnaFGEM.textProperty().addListener((oFGEM, antesFGEM, ahoraFGEM) -> {
                if (ahoraFGEM != null && !ahoraFGEM.equals(ahoraFGEM.toUpperCase(ESPANOL_FGEM))) {
                    columnaFGEM.setText(ahoraFGEM.toUpperCase(ESPANOL_FGEM));
                }
            });
        }
        if (columnaFGEM.getText() != null) {
            columnaFGEM.setText(columnaFGEM.getText().toUpperCase(ESPANOL_FGEM));
        }
        columnaFGEM.getColumns().forEach(EstilosFGEM::mayusculasFGEM);
    }

    public static ColorAcentoFGEM acentoFGEM() {
        return acentoFGEM;
    }

    /** Cambia el color de acento y repinta todas las ventanas abiertas. */
    public static void cambiarAcentoFGEM(ColorAcentoFGEM nuevoFGEM) {
        acentoFGEM = Objects.requireNonNull(nuevoFGEM, "acento");
        GraficosFGEM.AZUL_FGEM = nuevoFGEM.awtFGEM();
        for (Window ventanaFGEM : Window.getWindows()) {
            if (ventanaFGEM.getScene() != null) {
                pintarFGEM(ventanaFGEM.getScene().getRoot());
            }
        }
    }

    private static void pintarFGEM(Parent raizFGEM) {
        if (raizFGEM != null) {
            raizFGEM.setStyle("-ba-acento: " + acentoFGEM.hexFGEM() + ";");
        }
    }

    private static String hojaActualFGEM() {
        return Objects.requireNonNull(EstilosFGEM.class.getResource(TEMA_OSCURO_FGEM),
                "No se encontro " + TEMA_OSCURO_FGEM).toExternalForm();
    }
}
