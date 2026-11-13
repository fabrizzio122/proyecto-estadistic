package com.businessanalyzer.controller;

import com.businessanalyzer.BusinessAnalyzerApp;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Modulo;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.TarjetaIndicador;
import com.businessanalyzer.ui.VistaModulo;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Panel principal. En la Fase 1 muestra la estructura de indicadores (sin datos todavia),
 * los primeros pasos recomendados y el estado del sistema.
 * Los indicadores reales se conectan en la Fase 7 (analisis financiero).
 */
public class DashboardController implements VistaModulo {

    private final AppContext contexto;

    @FXML private HBox indicadores;
    @FXML private VBox pasos;
    @FXML private Label lblVersion;
    @FXML private Label lblEsquema;
    @FXML private Label lblBaseDatos;
    @FXML private Label lblJava;
    @FXML private Label lblJavaFx;

    public DashboardController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        String detalle = "Disponible desde la Fase " + Modulo.ANALISIS_FINANCIERO.fase();
        Stream.of(
                new TarjetaIndicador("Ventas del mes", "fth-shopping-bag"),
                new TarjetaIndicador("Utilidad neta", "fth-dollar-sign"),
                new TarjetaIndicador("Margen neto", "fth-percent"),
                new TarjetaIndicador("Punto de equilibrio", "fth-target")
        ).forEach(tarjeta -> {
            tarjeta.actualizar(TarjetaIndicador.SIN_VALOR, detalle);
            indicadores.getChildren().add(tarjeta);
        });

        construirPrimerosPasos();

        lblVersion.setText(BusinessAnalyzerApp.VERSION);
        lblEsquema.setText("v" + contexto.versionEsquema());
        lblBaseDatos.setText(contexto.config().archivoBaseDatos().toString());
        lblJava.setText(System.getProperty("java.version"));
        lblJavaFx.setText(System.getProperty("javafx.runtime.version", "desconocida"));
    }

    private void construirPrimerosPasos() {
        int numero = 1;
        for (Modulo modulo : Stream.of(Modulo.USUARIOS, Modulo.EMPRESAS, Modulo.PRODUCTOS,
                        Modulo.VENTAS_COMPRAS, Modulo.FINANZAS)
                .sorted(Comparator.comparingInt(Modulo::fase)).toList()) {
            pasos.getChildren().add(filaPaso(numero++, modulo));
        }
    }

    private static Node filaPaso(int numero, Modulo modulo) {
        Label circulo = new Label(String.valueOf(numero));
        circulo.getStyleClass().add("step-number");
        Label titulo = new Label(modulo.titulo());
        titulo.getStyleClass().add("step-title");
        Label descripcion = new Label(modulo.descripcion());
        descripcion.getStyleClass().add("muted");
        Label fase = new Label("Fase " + modulo.fase());
        fase.getStyleClass().add("chip");

        VBox textos = new VBox(2, titulo, descripcion);
        HBox.setHgrow(textos, javafx.scene.layout.Priority.ALWAYS);
        HBox fila = new HBox(12, circulo, textos, fase);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    @FXML
    private void probarNotificaciones() {
        Notificador.exito("Operación completada correctamente.");
        Notificador.info("Esta es una notificación informativa.");
        Notificador.advertencia("Revisa los datos antes de continuar.");
        Notificador.error("Ejemplo de mensaje de error.");
    }

    @FXML
    private void probarConfirmacion(ActionEvent evento) {
        boolean aceptado = Dialogos.confirmar(
                ((Node) evento.getSource()).getScene().getWindow(),
                "Confirmar acción",
                "Esta es una prueba del cuadro de confirmación. ¿Deseas continuar?");
        Notificador.info(aceptado ? "Confirmaste la acción." : "Cancelaste la acción.");
    }
}
