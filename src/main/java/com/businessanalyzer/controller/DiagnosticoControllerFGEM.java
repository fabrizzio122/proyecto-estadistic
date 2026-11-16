package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.diagnostico.AreaFGEM;
import com.businessanalyzer.analytics.diagnostico.DiagnosticoFGEM;
import com.businessanalyzer.analytics.diagnostico.HallazgoFGEM;
import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.service.DiagnosticoServiceFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.TareasFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/** Módulo "Diagnóstico empresarial": puntaje de salud, semáforo de indicadores y recomendaciones. */
public class DiagnosticoControllerFGEM implements VistaModuloFGEM {

    private static final List<String> CLASES_SEMAFORO_FGEM =
            List.of("semaforo-verde", "semaforo-amarillo", "semaforo-rojo", "semaforo-sin-datos");

    private final AppContextFGEM contextoFGEM;
    private long solicitudActualFGEM;

    @FXML private Label lblPeriodoFGEM;
    @FXML private ProgressIndicator progresoFGEM;
    @FXML private Label lblPuntajeFGEM;
    @FXML private Label lblNivelFGEM;
    @FXML private VBox resumenFGEM;
    @FXML private HBox areasFGEM;
    @FXML private VBox indicadoresFGEM;
    @FXML private VBox recomendacionesFGEM;

    public DiagnosticoControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        lblPeriodoFGEM.setText("Evalúa los últimos " + DiagnosticoServiceFGEM.MESES_EVALUADOS_FGEM
                + " meses completos frente a los " + DiagnosticoServiceFGEM.MESES_EVALUADOS_FGEM + " anteriores.");
    }

    @Override
    public void alMostrarFGEM() {
        actualizarFGEM();
    }

    @FXML
    private void actualizarFGEM() {
        if (contextoFGEM.sesionFGEM().empresaActivaFGEM().isEmpty()) {
            mostrarVacioFGEM("Selecciona una empresa en la barra superior.");
            return;
        }
        progresoFGEM.setVisible(true);
        long numeroFGEM = ++solicitudActualFGEM;
        TareasFGEM.ejecutarFGEM(() -> contextoFGEM.diagnosticoFGEM().diagnosticarFGEM(),
                diagnosticoFGEM -> {
                    if (numeroFGEM == solicitudActualFGEM) {
                        progresoFGEM.setVisible(false);
                        mostrarFGEM(diagnosticoFGEM);
                    }
                },
                errorFGEM -> {
                    if (numeroFGEM == solicitudActualFGEM) {
                        progresoFGEM.setVisible(false);
                        NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(errorFGEM));
                    }
                });
    }

    private void mostrarVacioFGEM(String mensajeFGEM) {
        lblPuntajeFGEM.setText("—");
        lblNivelFGEM.setText("");
        resumenFGEM.getChildren().setAll(fraseFGEM(mensajeFGEM));
        contextoFGEM.mostrarFraseFGEM(com.businessanalyzer.ui.ModuloFGEM.DIAGNOSTICO, null);
        areasFGEM.getChildren().clear();
        indicadoresFGEM.getChildren().clear();
        recomendacionesFGEM.getChildren().clear();
    }

    private void mostrarFGEM(DiagnosticoFGEM dFGEM) {
        boolean agroFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM()
                .map(eFGEM -> eFGEM.sectorFGEM() == com.businessanalyzer.model.SectorFGEM.AGROPECUARIO).orElse(false);
        lblPeriodoFGEM.setText("Período evaluado: " + dFGEM.periodoFGEM().descripcionFGEM() + ", comparado con "
                + com.businessanalyzer.analytics.diagnostico.DatosDiagnosticoFGEM.comparadoConFGEM(dFGEM.periodoFGEM(), agroFGEM).descripcionFGEM()
                + (agroFGEM ? " (mismo período del año anterior, por la estacionalidad agropecuaria)." : "."));
        if (!dFGEM.evaluadoFGEM()) {
            mostrarVacioFGEM(dFGEM.resumenFGEM().get(0));
            colorearFGEM(lblNivelFGEM, SemaforoFGEM.SIN_DATOS);
            lblNivelFGEM.setText(dFGEM.nivelFGEM().descripcionFGEM());
            return;
        }
        lblPuntajeFGEM.setText(String.valueOf(dFGEM.puntajeFGEM()));
        colorearFGEM(lblPuntajeFGEM, dFGEM.nivelFGEM().colorFGEM());
        lblNivelFGEM.setText(dFGEM.nivelFGEM().descripcionFGEM());
        colorearFGEM(lblNivelFGEM, dFGEM.nivelFGEM().colorFGEM());
        resumenFGEM.getChildren().setAll(dFGEM.resumenFGEM().stream().skip(1).map(DiagnosticoControllerFGEM::fraseFGEM).toList());
        contextoFGEM.mostrarFraseFGEM(com.businessanalyzer.ui.ModuloFGEM.DIAGNOSTICO, dFGEM.resumenFGEM().get(0));
        contextoFGEM.mostrarAlertasDiagnosticoFGEM(dFGEM.recomendacionesFGEM().size());

        areasFGEM.getChildren().clear();
        for (AreaFGEM areaFGEM : AreaFGEM.values()) {
            if (dFGEM.deAreaFGEM(areaFGEM).isEmpty()) {
                continue;
            }
            areasFGEM.getChildren().add(tarjetaAreaFGEM(areaFGEM, dFGEM));
        }

        indicadoresFGEM.getChildren().clear();
        for (AreaFGEM areaFGEM : AreaFGEM.values()) {
            if (dFGEM.deAreaFGEM(areaFGEM).isEmpty()) {
                continue;
            }
            Label tituloFGEM = new Label(areaFGEM.descripcionFGEM().toUpperCase());
            tituloFGEM.getStyleClass().add("diagnostico-seccion");
            indicadoresFGEM.getChildren().add(tituloFGEM);
            dFGEM.deAreaFGEM(areaFGEM).forEach(hFGEM -> indicadoresFGEM.getChildren().add(filaIndicadorFGEM(hFGEM)));
        }

        recomendacionesFGEM.getChildren().clear();
        List<HallazgoFGEM> pendientesFGEM = dFGEM.recomendacionesFGEM();
        if (pendientesFGEM.isEmpty()) {
            recomendacionesFGEM.getChildren().add(fraseFGEM("No hay alertas: todos los indicadores evaluados están en verde."));
        }
        for (int iFGEM = 0; iFGEM < pendientesFGEM.size(); iFGEM++) {
            recomendacionesFGEM.getChildren().add(recomendacionFGEM(iFGEM + 1, pendientesFGEM.get(iFGEM)));
        }
        List<HallazgoFGEM> fortalezasFGEM = dFGEM.fortalezasFGEM();
        if (!fortalezasFGEM.isEmpty()) {
            Label tituloFGEM = new Label("FORTALEZAS");
            tituloFGEM.getStyleClass().add("diagnostico-seccion");
            VBox listaFGEM = new VBox(6, tituloFGEM);
            for (HallazgoFGEM hFGEM : fortalezasFGEM) {
                FontIcon iconoFGEM = new FontIcon("fth-check-circle");
                iconoFGEM.getStyleClass().add("icono-fortaleza");
                Label textoFGEM = new Label(hFGEM.tipoFGEM().nombreFGEM() + ": " + hFGEM.valorFGEM());
                textoFGEM.setWrapText(true);
                textoFGEM.setMinHeight(Region.USE_PREF_SIZE);
                HBox filaFGEM = new HBox(8, iconoFGEM, textoFGEM);
                filaFGEM.setAlignment(Pos.CENTER_LEFT);
                listaFGEM.getChildren().add(filaFGEM);
            }
            recomendacionesFGEM.getChildren().add(listaFGEM);
        }
    }

    private VBox tarjetaAreaFGEM(AreaFGEM areaFGEM, DiagnosticoFGEM dFGEM) {
        Label nombreFGEM = new Label(areaFGEM.descripcionFGEM());
        nombreFGEM.getStyleClass().add("kpi-title");
        nombreFGEM.setWrapText(true);
        nombreFGEM.setMinHeight(Region.USE_PREF_SIZE);
        Integer puntajeFGEM = dFGEM.puntajePorAreaFGEM().get(areaFGEM);
        Label valorFGEM = new Label(puntajeFGEM == null ? "—" : puntajeFGEM + "/100");
        valorFGEM.getStyleClass().add("kpi-value");
        valorFGEM.setMinWidth(Region.USE_PREF_SIZE);
        SemaforoFGEM colorFGEM = puntajeFGEM == null ? SemaforoFGEM.SIN_DATOS
                : puntajeFGEM >= 75 ? SemaforoFGEM.VERDE : puntajeFGEM >= 45 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO;
        colorearFGEM(valorFGEM, colorFGEM);
        ProgressBar barraFGEM = new ProgressBar(puntajeFGEM == null ? 0 : puntajeFGEM / 100.0);
        barraFGEM.setMaxWidth(Double.MAX_VALUE);
        barraFGEM.getStyleClass().add("barra-salud");
        colorearFGEM(barraFGEM, colorFGEM);
        HBox puntosFGEM = new HBox(5);
        puntosFGEM.setAlignment(Pos.CENTER_LEFT);
        for (HallazgoFGEM hFGEM : dFGEM.deAreaFGEM(areaFGEM)) {
            Circle puntoFGEM = puntoFGEM(hFGEM.semaforoFGEM(), 5);
            Tooltip.install(puntoFGEM, new Tooltip(hFGEM.tipoFGEM().nombreFGEM() + ": " + hFGEM.semaforoFGEM().descripcionFGEM()));
            puntosFGEM.getChildren().add(puntoFGEM);
        }
        Label preguntaFGEM = new Label(areaFGEM.preguntaFGEM());
        preguntaFGEM.getStyleClass().add("kpi-detail");
        preguntaFGEM.setWrapText(true);
        preguntaFGEM.setMinHeight(Region.USE_PREF_SIZE);
        VBox tarjetaFGEM = new VBox(8, nombreFGEM, valorFGEM, barraFGEM, puntosFGEM, preguntaFGEM);
        tarjetaFGEM.getStyleClass().add("kpi-card");
        tarjetaFGEM.setStyle("-fx-min-width: 115;");
        tarjetaFGEM.setPrefWidth(150);
        HBox.setHgrow(tarjetaFGEM, Priority.ALWAYS);
        return tarjetaFGEM;
    }

    private HBox filaIndicadorFGEM(HallazgoFGEM hFGEM) {
        Label nombreFGEM = new Label(hFGEM.tipoFGEM().nombreFGEM());
        nombreFGEM.getStyleClass().add("info-value");
        Label lecturaFGEM = new Label(hFGEM.lecturaFGEM());
        lecturaFGEM.getStyleClass().add("muted");
        lecturaFGEM.setWrapText(true);
        lecturaFGEM.setMinHeight(Region.USE_PREF_SIZE);
        VBox textosFGEM = new VBox(2, nombreFGEM, lecturaFGEM);
        HBox.setHgrow(textosFGEM, Priority.ALWAYS);
        textosFGEM.setMinWidth(0);

        Label valorFGEM = new Label(hFGEM.valorFGEM());
        valorFGEM.getStyleClass().add("diagnostico-valor");
        valorFGEM.setMinWidth(Region.USE_PREF_SIZE);
        colorearFGEM(valorFGEM, hFGEM.semaforoFGEM());
        Label estadoFGEM = new Label(hFGEM.semaforoFGEM().descripcionFGEM());
        estadoFGEM.getStyleClass().add("diagnostico-estado");
        estadoFGEM.setMinWidth(Region.USE_PREF_SIZE);
        colorearFGEM(estadoFGEM, hFGEM.semaforoFGEM());
        VBox derechaFGEM = new VBox(2, valorFGEM, estadoFGEM);
        derechaFGEM.setAlignment(Pos.TOP_RIGHT);
        derechaFGEM.setMinWidth(Region.USE_PREF_SIZE);

        Circle puntoFGEM = puntoFGEM(hFGEM.semaforoFGEM(), 7);
        HBox filaFGEM = new HBox(12, puntoFGEM, textosFGEM, derechaFGEM);
        filaFGEM.setAlignment(Pos.TOP_LEFT);
        filaFGEM.getStyleClass().add("diagnostico-fila");
        Tooltip ayudaFGEM = new Tooltip(hFGEM.tipoFGEM().queMideFGEM() + (hFGEM.criterioFGEM() == null ? "" : "\n" + hFGEM.criterioFGEM()));
        ayudaFGEM.setWrapText(true);
        ayudaFGEM.setMaxWidth(380);
        ayudaFGEM.setShowDelay(Duration.millis(300));
        Tooltip.install(filaFGEM, ayudaFGEM);
        return filaFGEM;
    }

    private static VBox recomendacionFGEM(int numeroFGEM, HallazgoFGEM hFGEM) {
        Label prioridadFGEM = new Label(hFGEM.semaforoFGEM() == SemaforoFGEM.ROJO ? "Urgente" : "Mejorar");
        prioridadFGEM.getStyleClass().add("diagnostico-prioridad");
        prioridadFGEM.setMinWidth(Region.USE_PREF_SIZE);
        colorearFGEM(prioridadFGEM, hFGEM.semaforoFGEM());
        Label tituloFGEM = new Label(numeroFGEM + ". " + hFGEM.tipoFGEM().nombreFGEM() + " (" + hFGEM.valorFGEM() + ")");
        tituloFGEM.getStyleClass().add("step-title");
        tituloFGEM.setWrapText(true);
        tituloFGEM.setMinHeight(Region.USE_PREF_SIZE);
        HBox cabeceraFGEM = new HBox(8, prioridadFGEM, tituloFGEM);
        cabeceraFGEM.setAlignment(Pos.CENTER_LEFT);
        Label textoFGEM = new Label(hFGEM.recomendacionFGEM());
        textoFGEM.getStyleClass().add("lectura");
        textoFGEM.setWrapText(true);
        textoFGEM.setMinHeight(Region.USE_PREF_SIZE);
        return new VBox(4, cabeceraFGEM, textoFGEM);
    }

    private static Circle puntoFGEM(SemaforoFGEM semaforoFGEM, double radioFGEM) {
        Circle puntoFGEM = new Circle(radioFGEM);
        puntoFGEM.getStyleClass().add("punto-semaforo");
        colorearFGEM(puntoFGEM, semaforoFGEM);
        if (radioFGEM > 6) {
            HBox.setMargin(puntoFGEM, new javafx.geometry.Insets(4, 0, 0, 0));
        }
        return puntoFGEM;
    }

    private static void colorearFGEM(Node nodoFGEM, SemaforoFGEM semaforoFGEM) {
        nodoFGEM.getStyleClass().removeAll(CLASES_SEMAFORO_FGEM);
        nodoFGEM.getStyleClass().add(switch (semaforoFGEM) {
            case VERDE -> "semaforo-verde";
            case AMARILLO -> "semaforo-amarillo";
            case ROJO -> "semaforo-rojo";
            case SIN_DATOS -> "semaforo-sin-datos";
        });
    }

    private static Label fraseFGEM(String textoFGEM) {
        Label labelFGEM = new Label(textoFGEM);
        labelFGEM.getStyleClass().add("lectura");
        labelFGEM.setWrapText(true);
        labelFGEM.setMinHeight(Region.USE_PREF_SIZE);
        return labelFGEM;
    }
}
