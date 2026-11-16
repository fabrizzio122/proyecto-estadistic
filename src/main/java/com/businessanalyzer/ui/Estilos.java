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
public final class Estilos {

    public static final String TEMA_OSCURO = "/css/tema-oscuro.css";

    private static volatile ColorAcento acento = ColorAcento.PREDETERMINADO;

    private Estilos() {
    }

    public static void aplicar(Scene escena) {
        Fuentes.cargar();
        escena.getStylesheets().setAll(hojaActual());
        pintar(escena.getRoot());
        if (!escena.getProperties().containsKey(Estilos.class)) {
            escena.getProperties().put(Estilos.class, Boolean.TRUE);
            escena.rootProperty().addListener((o, anterior, nuevo) -> pintar(nuevo));
        }
    }

    public static void aplicar(DialogPane dialogo) {
        Fuentes.cargar();
        dialogo.getStylesheets().setAll(hojaActual());
        pintar(dialogo);
        ajustarBotones(dialogo);
        encabezadosTabla(dialogo.getContent());
        if (dialogo.getProperties().putIfAbsent(ESPANOL, Boolean.TRUE) == null) {
            dialogo.contentProperty().addListener((o, antes, ahora) -> encabezadosTabla(ahora));
        }
        dialogo.getButtonTypes().addListener((javafx.collections.ListChangeListener<ButtonType>) c -> ajustarBotones(dialogo));
    }

    /**
     * Cada botón del diálogo toma el ancho de su propio texto: el ancho uniforme de JavaFX se calcula
     * antes de aplicar las tipografías del tema y cortaba textos como «Registrar venta».
     */
    private static void ajustarBotones(DialogPane dialogo) {
        for (ButtonType tipo : dialogo.getButtonTypes()) {
            if (dialogo.lookupButton(tipo) instanceof Button boton) {
                ButtonBar.setButtonUniformSize(boton, false);
                boton.setMinWidth(Region.USE_PREF_SIZE);
            }
        }
    }

    private static final java.util.Locale ESPANOL = java.util.Locale.forLanguageTag("es");

    /**
     * Encabezados de tabla en mayúsculas pequeñas («FECHA», «MONTO»): el CSS de JavaFX no tiene
     * text-transform, así que se cambia el texto de las columnas. Recorre también el contenido de las
     * pestañas y de los paneles con desplazamiento, y sigue las columnas que se agreguen o renombren después.
     * Solo cambia lo que se ve: las exportaciones usan sus propios títulos.
     */
    public static void encabezadosTabla(javafx.scene.Node raiz) {
        if (raiz == null) {
            return;
        }
        if (raiz instanceof javafx.scene.control.TableView<?> tabla) {
            if (tabla.getProperties().putIfAbsent(ESPANOL, Boolean.TRUE) == null) {
                tabla.getColumns().forEach(Estilos::mayusculas);
                tabla.getColumns().addListener((javafx.collections.ListChangeListener<javafx.scene.control.TableColumn<?, ?>>)
                        c -> c.getList().forEach(Estilos::mayusculas));
            }
            return;
        }
        if (raiz instanceof javafx.scene.control.TabPane pestanas) {
            for (javafx.scene.control.Tab pestana : pestanas.getTabs()) {
                // Algunas pestañas cambian su contenido al cargar los datos (Predicciones).
                if (pestana.getProperties().putIfAbsent(ESPANOL, Boolean.TRUE) == null) {
                    pestana.contentProperty().addListener((o, antes, ahora) -> encabezadosTabla(ahora));
                }
                encabezadosTabla(pestana.getContent());
            }
        }
        if (raiz instanceof javafx.scene.control.ScrollPane desplazable) {
            encabezadosTabla(desplazable.getContent());
        }
        if (raiz instanceof javafx.scene.control.SplitPane division) {
            division.getItems().forEach(Estilos::encabezadosTabla);
        }
        if (raiz instanceof javafx.scene.control.TitledPane plegable) {
            encabezadosTabla(plegable.getContent());
        }
        if (raiz instanceof Parent padre) {
            padre.getChildrenUnmodifiable().forEach(Estilos::encabezadosTabla);
        }
    }

    private static void mayusculas(javafx.scene.control.TableColumnBase<?, ?> columna) {
        if (columna.getProperties().putIfAbsent(ESPANOL, Boolean.TRUE) == null) {
            columna.textProperty().addListener((o, antes, ahora) -> {
                if (ahora != null && !ahora.equals(ahora.toUpperCase(ESPANOL))) {
                    columna.setText(ahora.toUpperCase(ESPANOL));
                }
            });
        }
        if (columna.getText() != null) {
            columna.setText(columna.getText().toUpperCase(ESPANOL));
        }
        columna.getColumns().forEach(Estilos::mayusculas);
    }

    public static ColorAcento acento() {
        return acento;
    }

    /** Cambia el color de acento y repinta todas las ventanas abiertas. */
    public static void cambiarAcento(ColorAcento nuevo) {
        acento = Objects.requireNonNull(nuevo, "acento");
        Graficos.AZUL = nuevo.awt();
        for (Window ventana : Window.getWindows()) {
            if (ventana.getScene() != null) {
                pintar(ventana.getScene().getRoot());
            }
        }
    }

    private static void pintar(Parent raiz) {
        if (raiz != null) {
            raiz.setStyle("-ba-acento: " + acento.hex() + ";");
        }
    }

    private static String hojaActual() {
        return Objects.requireNonNull(Estilos.class.getResource(TEMA_OSCURO),
                "No se encontro " + TEMA_OSCURO).toExternalForm();
    }
}
