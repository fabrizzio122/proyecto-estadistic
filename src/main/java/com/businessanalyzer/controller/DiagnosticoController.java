package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.diagnostico.Area;
import com.businessanalyzer.analytics.diagnostico.Diagnostico;
import com.businessanalyzer.analytics.diagnostico.Hallazgo;
import com.businessanalyzer.analytics.diagnostico.Semaforo;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.service.DiagnosticoService;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.Tareas;
import com.businessanalyzer.ui.VistaModulo;
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
public class DiagnosticoController implements VistaModulo {

    private static final List<String> CLASES_SEMAFORO =
            List.of("semaforo-verde", "semaforo-amarillo", "semaforo-rojo", "semaforo-sin-datos");

    private final AppContext contexto;
    private long solicitudActual;

    @FXML private Label lblPeriodo;
    @FXML private ProgressIndicator progreso;
    @FXML private Label lblPuntaje;
    @FXML private Label lblNivel;
    @FXML private VBox resumen;
    @FXML private HBox areas;
    @FXML private VBox indicadores;
    @FXML private VBox recomendaciones;

    public DiagnosticoController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        lblPeriodo.setText("Evalúa los últimos " + DiagnosticoService.MESES_EVALUADOS
                + " meses completos frente a los " + DiagnosticoService.MESES_EVALUADOS + " anteriores.");
    }

    @Override
    public void alMostrar() {
        actualizar();
    }

    @FXML
    private void actualizar() {
        if (contexto.sesion().empresaActiva().isEmpty()) {
            mostrarVacio("Selecciona una empresa en la barra superior.");
            return;
        }
        progreso.setVisible(true);
        long numero = ++solicitudActual;
        Tareas.ejecutar(() -> contexto.diagnostico().diagnosticar(),
                diagnostico -> {
                    if (numero == solicitudActual) {
                        progreso.setVisible(false);
                        mostrar(diagnostico);
                    }
                },
                error -> {
                    if (numero == solicitudActual) {
                        progreso.setVisible(false);
                        Notificador.error(Errores.mensaje(error));
                    }
                });
    }

    private void mostrarVacio(String mensaje) {
        lblPuntaje.setText("—");
        lblNivel.setText("");
        resumen.getChildren().setAll(frase(mensaje));
        areas.getChildren().clear();
        indicadores.getChildren().clear();
        recomendaciones.getChildren().clear();
    }

    private void mostrar(Diagnostico d) {
        boolean agro = contexto.sesion().empresaActiva()
                .map(e -> e.sector() == com.businessanalyzer.model.Sector.AGROPECUARIO).orElse(false);
        lblPeriodo.setText("Período evaluado: " + d.periodo().descripcion() + ", comparado con "
                + com.businessanalyzer.analytics.diagnostico.DatosDiagnostico.comparadoCon(d.periodo(), agro).descripcion()
                + (agro ? " (mismo período del año anterior, por la estacionalidad agropecuaria)." : "."));
        if (!d.evaluado()) {
            mostrarVacio(d.resumen().get(0));
            colorear(lblNivel, Semaforo.SIN_DATOS);
            lblNivel.setText(d.nivel().descripcion());
            return;
        }
        lblPuntaje.setText(String.valueOf(d.puntaje()));
        colorear(lblPuntaje, d.nivel().color());
        lblNivel.setText(d.nivel().descripcion());
        colorear(lblNivel, d.nivel().color());
        resumen.getChildren().setAll(d.resumen().stream().skip(1).map(DiagnosticoController::frase).toList());

        areas.getChildren().clear();
        for (Area area : Area.values()) {
            if (d.deArea(area).isEmpty()) {
                continue;
            }
            areas.getChildren().add(tarjetaArea(area, d));
        }

        indicadores.getChildren().clear();
        for (Area area : Area.values()) {
            if (d.deArea(area).isEmpty()) {
                continue;
            }
            Label titulo = new Label(area.descripcion().toUpperCase());
            titulo.getStyleClass().add("diagnostico-seccion");
            indicadores.getChildren().add(titulo);
            d.deArea(area).forEach(h -> indicadores.getChildren().add(filaIndicador(h)));
        }

        recomendaciones.getChildren().clear();
        List<Hallazgo> pendientes = d.recomendaciones();
        if (pendientes.isEmpty()) {
            recomendaciones.getChildren().add(frase("No hay alertas: todos los indicadores evaluados están en verde."));
        }
        for (int i = 0; i < pendientes.size(); i++) {
            recomendaciones.getChildren().add(recomendacion(i + 1, pendientes.get(i)));
        }
        List<Hallazgo> fortalezas = d.fortalezas();
        if (!fortalezas.isEmpty()) {
            Label titulo = new Label("FORTALEZAS");
            titulo.getStyleClass().add("diagnostico-seccion");
            VBox lista = new VBox(6, titulo);
            for (Hallazgo h : fortalezas) {
                FontIcon icono = new FontIcon("fth-check-circle");
                icono.getStyleClass().add("icono-fortaleza");
                Label texto = new Label(h.tipo().nombre() + ": " + h.valor());
                texto.setWrapText(true);
                texto.setMinHeight(Region.USE_PREF_SIZE);
                HBox fila = new HBox(8, icono, texto);
                fila.setAlignment(Pos.CENTER_LEFT);
                lista.getChildren().add(fila);
            }
            recomendaciones.getChildren().add(lista);
        }
    }

    private VBox tarjetaArea(Area area, Diagnostico d) {
        Label nombre = new Label(area.descripcion());
        nombre.getStyleClass().add("kpi-title");
        nombre.setWrapText(true);
        nombre.setMinHeight(Region.USE_PREF_SIZE);
        Integer puntaje = d.puntajePorArea().get(area);
        Label valor = new Label(puntaje == null ? "—" : puntaje + "/100");
        valor.getStyleClass().add("kpi-value");
        valor.setMinWidth(Region.USE_PREF_SIZE);
        Semaforo color = puntaje == null ? Semaforo.SIN_DATOS
                : puntaje >= 75 ? Semaforo.VERDE : puntaje >= 45 ? Semaforo.AMARILLO : Semaforo.ROJO;
        colorear(valor, color);
        ProgressBar barra = new ProgressBar(puntaje == null ? 0 : puntaje / 100.0);
        barra.setMaxWidth(Double.MAX_VALUE);
        barra.getStyleClass().add("barra-salud");
        colorear(barra, color);
        HBox puntos = new HBox(5);
        puntos.setAlignment(Pos.CENTER_LEFT);
        for (Hallazgo h : d.deArea(area)) {
            Circle punto = punto(h.semaforo(), 5);
            Tooltip.install(punto, new Tooltip(h.tipo().nombre() + ": " + h.semaforo().descripcion()));
            puntos.getChildren().add(punto);
        }
        Label pregunta = new Label(area.pregunta());
        pregunta.getStyleClass().add("kpi-detail");
        pregunta.setWrapText(true);
        pregunta.setMinHeight(Region.USE_PREF_SIZE);
        VBox tarjeta = new VBox(8, nombre, valor, barra, puntos, pregunta);
        tarjeta.getStyleClass().add("kpi-card");
        tarjeta.setStyle("-fx-min-width: 115;");
        tarjeta.setPrefWidth(150);
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        return tarjeta;
    }

    private HBox filaIndicador(Hallazgo h) {
        Label nombre = new Label(h.tipo().nombre());
        nombre.getStyleClass().add("info-value");
        Label lectura = new Label(h.lectura());
        lectura.getStyleClass().add("muted");
        lectura.setWrapText(true);
        lectura.setMinHeight(Region.USE_PREF_SIZE);
        VBox textos = new VBox(2, nombre, lectura);
        HBox.setHgrow(textos, Priority.ALWAYS);
        textos.setMinWidth(0);

        Label valor = new Label(h.valor());
        valor.getStyleClass().add("diagnostico-valor");
        valor.setMinWidth(Region.USE_PREF_SIZE);
        colorear(valor, h.semaforo());
        Label estado = new Label(h.semaforo().descripcion());
        estado.getStyleClass().add("diagnostico-estado");
        estado.setMinWidth(Region.USE_PREF_SIZE);
        colorear(estado, h.semaforo());
        VBox derecha = new VBox(2, valor, estado);
        derecha.setAlignment(Pos.TOP_RIGHT);
        derecha.setMinWidth(Region.USE_PREF_SIZE);

        Circle punto = punto(h.semaforo(), 7);
        HBox fila = new HBox(12, punto, textos, derecha);
        fila.setAlignment(Pos.TOP_LEFT);
        fila.getStyleClass().add("diagnostico-fila");
        Tooltip ayuda = new Tooltip(h.tipo().queMide() + (h.criterio() == null ? "" : "\n" + h.criterio()));
        ayuda.setWrapText(true);
        ayuda.setMaxWidth(380);
        ayuda.setShowDelay(Duration.millis(300));
        Tooltip.install(fila, ayuda);
        return fila;
    }

    private static VBox recomendacion(int numero, Hallazgo h) {
        Label prioridad = new Label(h.semaforo() == Semaforo.ROJO ? "Urgente" : "Mejorar");
        prioridad.getStyleClass().add("diagnostico-prioridad");
        prioridad.setMinWidth(Region.USE_PREF_SIZE);
        colorear(prioridad, h.semaforo());
        Label titulo = new Label(numero + ". " + h.tipo().nombre() + " (" + h.valor() + ")");
        titulo.getStyleClass().add("step-title");
        titulo.setWrapText(true);
        titulo.setMinHeight(Region.USE_PREF_SIZE);
        HBox cabecera = new HBox(8, prioridad, titulo);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        Label texto = new Label(h.recomendacion());
        texto.getStyleClass().add("lectura");
        texto.setWrapText(true);
        texto.setMinHeight(Region.USE_PREF_SIZE);
        return new VBox(4, cabecera, texto);
    }

    private static Circle punto(Semaforo semaforo, double radio) {
        Circle punto = new Circle(radio);
        punto.getStyleClass().add("punto-semaforo");
        colorear(punto, semaforo);
        if (radio > 6) {
            HBox.setMargin(punto, new javafx.geometry.Insets(4, 0, 0, 0));
        }
        return punto;
    }

    private static void colorear(Node nodo, Semaforo semaforo) {
        nodo.getStyleClass().removeAll(CLASES_SEMAFORO);
        nodo.getStyleClass().add(switch (semaforo) {
            case VERDE -> "semaforo-verde";
            case AMARILLO -> "semaforo-amarillo";
            case ROJO -> "semaforo-rojo";
            case SIN_DATOS -> "semaforo-sin-datos";
        });
    }

    private static Label frase(String texto) {
        Label label = new Label(texto);
        label.getStyleClass().add("lectura");
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }
}
