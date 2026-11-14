package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.estadistica.AnalisisAbc;
import com.businessanalyzer.analytics.estadistica.AnalisisEstadistico;
import com.businessanalyzer.analytics.estadistica.EstacionalidadSemanal;
import com.businessanalyzer.analytics.estadistica.EstadisticaDescriptiva;
import com.businessanalyzer.analytics.estadistica.LecturaEstadistica;
import com.businessanalyzer.analytics.estadistica.MatrizCorrelacion;
import com.businessanalyzer.analytics.estadistica.Tendencia;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.ui.CamposFecha;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Graficos;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.Tareas;
import com.businessanalyzer.ui.TarjetaIndicador;
import com.businessanalyzer.ui.VistaModulo;
import com.businessanalyzer.util.Montos;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/** Módulo "Análisis estadístico": descriptiva, tendencia, días de la semana, ABC y correlaciones. */
public class EstadisticaController implements VistaModulo {

    private final AppContext contexto;
    private Moneda moneda = Moneda.PEN;
    private long solicitudActual;

    private final TarjetaIndicador kpiVentas = new TarjetaIndicador("Ventas registradas", "fth-shopping-bag");
    private final TarjetaIndicador kpiTicket = new TarjetaIndicador("Venta típica (mediana)", "fth-tag");
    private final TarjetaIndicador kpiTendencia = new TarjetaIndicador("Tendencia mensual", "fth-trending-up");
    private final TarjetaIndicador kpiClaseA = new TarjetaIndicador("Productos clase A", "fth-star");

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ProgressIndicator progreso;
    @FXML private HBox indicadores;
    @FXML private GridPane gridTicket;
    @FXML private GridPane gridDiarias;
    @FXML private StackPane contenedorHistograma;
    @FXML private VBox lecturaResumen;
    @FXML private StackPane contenedorTendencia;
    @FXML private GridPane gridTendencia;
    @FXML private VBox lecturaTendencia;
    @FXML private StackPane contenedorDias;
    @FXML private GridPane gridDias;
    @FXML private VBox lecturaDias;
    @FXML private javafx.scene.layout.FlowPane resumenAbc;
    @FXML private TableView<AnalisisAbc.Item> tablaAbc;
    @FXML private TableColumn<AnalisisAbc.Item, AnalisisAbc.Item> colAbcClase;
    @FXML private TableColumn<AnalisisAbc.Item, String> colAbcNombre;
    @FXML private TableColumn<AnalisisAbc.Item, String> colAbcVentas;
    @FXML private TableColumn<AnalisisAbc.Item, String> colAbcParticipacion;
    @FXML private TableColumn<AnalisisAbc.Item, String> colAbcAcumulado;
    @FXML private VBox lecturaAbc;
    @FXML private StackPane contenedorPareto;
    @FXML private GridPane gridCorrelacion;
    @FXML private VBox lecturaCorrelaciones;

    public EstadisticaController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        indicadores.getChildren().addAll(kpiVentas, kpiTicket, kpiTendencia, kpiClaseA);
        CamposFecha.configurar(dpDesde);
        CamposFecha.configurar(dpHasta);
        fijarUltimosMeses(6);
        dpDesde.valueProperty().addListener((o, a, n) -> cargar());
        dpHasta.valueProperty().addListener((o, a, n) -> cargar());
        for (GridPane grilla : List.of(gridTicket, gridDiarias, gridTendencia, gridDias)) {
            ColumnConstraints etiquetas = new ColumnConstraints();
            etiquetas.setMinWidth(125);
            ColumnConstraints valores = new ColumnConstraints();
            valores.setHgrow(Priority.ALWAYS);
            valores.setHalignment(javafx.geometry.HPos.RIGHT);
            grilla.getColumnConstraints().setAll(etiquetas, valores);
        }
        configurarTablaAbc();
    }

    @Override
    public void alMostrar() {
        cargar();
    }

    // ------------------------------------------------------------------ período

    @FXML
    private void ultimos3() {
        fijarUltimosMeses(3);
    }

    @FXML
    private void ultimos6() {
        fijarUltimosMeses(6);
    }

    @FXML
    private void ultimos12() {
        fijarUltimosMeses(12);
    }

    /** Desde el día 1 de hace (n − 1) meses hasta el fin del mes actual. */
    private void fijarUltimosMeses(int meses) {
        YearMonth actual = YearMonth.now();
        LocalDate desde = actual.minusMonths(meses - 1L).atDay(1);
        LocalDate hasta = actual.atEndOfMonth();
        if (dpHasta.getValue() != null && desde.isAfter(dpHasta.getValue())) {
            dpHasta.setValue(hasta);
            dpDesde.setValue(desde);
        } else {
            dpDesde.setValue(desde);
            dpHasta.setValue(hasta);
        }
    }

    // ------------------------------------------------------------------ carga

    private void cargar() {
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        if (empresa == null) {
            tablaAbc.getItems().clear();
            tablaAbc.setPlaceholder(new Label("Selecciona una empresa en la barra superior."));
            return;
        }
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            return;
        }
        moneda = empresa.moneda();
        progreso.setVisible(true);
        long numero = ++solicitudActual;
        Tareas.ejecutar(() -> contexto.estadistica().analizar(desde, hasta),
                analisis -> {
                    if (numero == solicitudActual) {
                        progreso.setVisible(false);
                        mostrar(analisis);
                    }
                },
                error -> {
                    if (numero == solicitudActual) {
                        progreso.setVisible(false);
                        Notificador.error(Errores.mensaje(error));
                    }
                });
    }

    private void mostrar(AnalisisEstadistico a) {
        mostrarIndicadores(a);
        mostrarResumen(a);
        mostrarTendencia(a);
        mostrarDias(a);
        mostrarAbc(a);
        mostrarCorrelaciones(a);
    }

    private void mostrarIndicadores(AnalisisEstadistico a) {
        EstadisticaDescriptiva t = a.ticket();
        kpiVentas.actualizar(String.valueOf(t.n()), t.n() == 1 ? "venta en el período" : "ventas en el período");
        kpiTicket.actualizar(dinero(t.mediana()), "Promedio " + dinero(t.media()));
        Tendencia tendencia = a.tendenciaVentas();
        kpiTendencia.actualizar(tendencia.direccion().descripcion(), switch (tendencia.direccion()) {
            case INSUFICIENTE -> "Se necesitan 3 meses completos";
            case ESTABLE -> "Variaciones atribuibles al azar";
            default -> (tendencia.pendiente() > 0 ? "+" : "") + porcentaje(tendencia.crecimientoMensual())
                    + " al mes · R² " + decimal(tendencia.r2());
        });
        AnalisisAbc abc = a.abc();
        kpiClaseA.actualizar(abc.items().isEmpty() ? TarjetaIndicador.SIN_VALOR
                        : abc.cantidad(AnalisisAbc.Clase.A) + " de " + abc.items().size(),
                abc.items().isEmpty() ? "Sin ventas" : "Generan " + Montos.porcentaje(abc.participacion(AnalisisAbc.Clase.A))
                        + " de las ventas");
    }

    // ------------------------------------------------------------------ resumen descriptivo

    private void mostrarResumen(AnalisisEstadistico a) {
        llenarDescriptiva(gridTicket, a.ticket());
        llenarDescriptiva(gridDiarias, a.ventasDiarias());
        contenedorHistograma.getChildren().setAll(Graficos.visor(Graficos.histograma(a.histogramaTickets(), moneda)));
        lectura(lecturaResumen, "Interpretación", LecturaEstadistica.resumen(a, moneda));
    }

    private void llenarDescriptiva(GridPane grilla, EstadisticaDescriptiva e) {
        grilla.getChildren().clear();
        int fila = 0;
        fila = dato(grilla, fila, "Observaciones", String.valueOf(e.n()), "Cantidad de datos analizados");
        fila = dato(grilla, fila, "Media", dinero(e.media()), "Promedio");
        fila = dato(grilla, fila, "Mediana", dinero(e.mediana()), "La mitad de los valores está por debajo");
        fila = dato(grilla, fila, "Desviación estándar", dinero(e.desviacion()), "Cuánto se alejan los valores del promedio");
        fila = dato(grilla, fila, "Coef. de variación", porcentaje(e.coeficienteVariacion()), "Dispersión relativa: menos de 30 % es estable");
        fila = dato(grilla, fila, "Mínimo", dinero(e.minimo()), null);
        fila = dato(grilla, fila, "Cuartil 1 (25 %)", dinero(e.q1()), "El 25 % de los valores está por debajo");
        fila = dato(grilla, fila, "Cuartil 3 (75 %)", dinero(e.q3()), "El 75 % de los valores está por debajo");
        fila = dato(grilla, fila, "Máximo", dinero(e.maximo()), null);
        dato(grilla, fila, "Asimetría", decimal(e.asimetria()), "Positiva: pocos valores muy altos estiran la distribución");
    }

    // ------------------------------------------------------------------ tendencia

    private void mostrarTendencia(AnalisisEstadistico a) {
        Tendencia t = a.tendenciaVentas();
        contenedorTendencia.getChildren().setAll(Graficos.visor(Graficos.tendencia(a.serieMensual(), t, moneda)));
        gridTendencia.getChildren().clear();
        int fila = 0;
        fila = dato(gridTendencia, fila, "Meses analizados", String.valueOf(t.n()), "Meses completos con ventas");
        fila = dato(gridTendencia, fila, "Dirección", t.direccion().descripcion(), null);
        fila = dato(gridTendencia, fila, "Cambio por mes", dinero(t.pendiente()), "Pendiente de la recta");
        fila = dato(gridTendencia, fila, "Crecimiento mensual", porcentaje(t.crecimientoMensual()), "Pendiente ÷ venta mensual promedio");
        fila = dato(gridTendencia, fila, "R²", decimal(t.r2()), "Qué parte de la variación explica la recta (0 a 1)");
        dato(gridTendencia, fila, "Valor p", valorP(t.significancia()), "Menor a 0.05: tendencia estadísticamente significativa");
        lectura(lecturaTendencia, null, LecturaEstadistica.tendencia(a, moneda));
    }

    // ------------------------------------------------------------------ días de la semana

    private void mostrarDias(AnalisisEstadistico a) {
        EstacionalidadSemanal e = a.estacionalidad();
        contenedorDias.getChildren().setAll(Graficos.visor(Graficos.diasDeLaSemana(e, moneda)));
        gridDias.getChildren().clear();
        int fila = 0;
        for (DayOfWeek dia : DayOfWeek.values()) {
            Double promedio = e.promedioPorDia().get(dia);
            double indice = e.indice(dia);
            String texto = promedio == null ? "—" : dinero(promedio) + "  ·  " + decimal(indice);
            String nombre = LecturaEstadistica.nombre(dia);
            fila = dato(gridDias, fila, Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1), texto,
                    "Venta promedio · índice (1.00 = día promedio)");
        }
        lectura(lecturaDias, null, LecturaEstadistica.estacionalidad(a, moneda));
    }

    // ------------------------------------------------------------------ ABC

    private void mostrarAbc(AnalisisEstadistico a) {
        AnalisisAbc abc = a.abc();
        tablaAbc.setItems(FXCollections.observableArrayList(abc.items()));
        tablaAbc.setPlaceholder(new Label("No hubo ventas en el período."));
        resumenAbc.getChildren().clear();
        for (AnalisisAbc.Clase clase : AnalisisAbc.Clase.values()) {
            Label chip = new Label("Clase " + clase + ":  " + abc.cantidad(clase) + " ítems · "
                    + Montos.porcentaje(abc.participacion(clase)) + " de las ventas");
            chip.getStyleClass().addAll("chip", "chip-clase-" + clase.name().toLowerCase());
            chip.setMinWidth(Region.USE_PREF_SIZE);
            resumenAbc.getChildren().add(chip);
        }
        contenedorPareto.getChildren().setAll(Graficos.visor(Graficos.pareto(abc, 15, moneda)));
        lectura(lecturaAbc, null, LecturaEstadistica.abc(a));
    }

    private void configurarTablaAbc() {
        colAbcClase.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue()));
        colAbcClase.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(AnalisisAbc.Item item, boolean vacio) {
                super.updateItem(item, vacio);
                setText(null);
                if (vacio || item == null) {
                    setGraphic(null);
                    return;
                }
                Label insignia = new Label(item.clase().name());
                insignia.getStyleClass().addAll("badge", "badge-clase-" + item.clase().name().toLowerCase());
                setGraphic(insignia);
            }
        });
        colAbcNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().producto().nombre()));
        colAbcVentas.setCellValueFactory(c -> new ReadOnlyStringWrapper(Montos.formatear(c.getValue().producto().ventas(), moneda)));
        colAbcParticipacion.setCellValueFactory(c -> new ReadOnlyStringWrapper(Montos.porcentaje(c.getValue().participacion())));
        colAbcAcumulado.setCellValueFactory(c -> new ReadOnlyStringWrapper(Montos.porcentaje(c.getValue().acumulado())));
    }

    // ------------------------------------------------------------------ correlaciones

    private void mostrarCorrelaciones(AnalisisEstadistico a) {
        MatrizCorrelacion m = a.correlaciones();
        gridCorrelacion.getChildren().clear();
        List<String> variables = m.variables();
        for (int j = 0; j < variables.size(); j++) {
            gridCorrelacion.add(encabezado(variables.get(j), Pos.CENTER), j + 1, 0);
        }
        for (int i = 0; i < variables.size(); i++) {
            gridCorrelacion.add(encabezado(variables.get(i), Pos.CENTER_LEFT), 0, i + 1);
            for (int j = 0; j < variables.size(); j++) {
                gridCorrelacion.add(celdaCorrelacion(m.r(i, j), m.valoresP()[i][j], i == j), j + 1, i + 1);
            }
        }
        lectura(lecturaCorrelaciones, null, LecturaEstadistica.correlaciones(a));
    }

    private static Label encabezado(String texto, Pos alineacion) {
        Label label = new Label(texto);
        label.getStyleClass().add("info-key");
        label.setWrapText(true);
        label.setAlignment(alineacion);
        label.setMinWidth(105);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    /** Celda coloreada: azul si r es positivo, rojo si es negativo; más opaca cuanto más fuerte. */
    private static Label celdaCorrelacion(double r, double p, boolean diagonal) {
        Label celda = new Label(Double.isNaN(r) ? "—" : String.format(Locale.US, "%.2f", r));
        celda.setMinSize(105, 44);
        celda.setMaxWidth(Double.MAX_VALUE);
        celda.setAlignment(Pos.CENTER);
        celda.getStyleClass().add("celda-correlacion");
        if (!Double.isNaN(r)) {
            double opacidad = diagonal ? 0.12 : 0.15 + 0.65 * Math.abs(r);
            String color = r >= 0 ? "56,189,248" : "239,68,68";
            celda.setStyle("-fx-background-color: rgba(" + color + "," + String.format(Locale.US, "%.2f", opacidad) + ");");
            if (!diagonal) {
                celda.setTooltip(new Tooltip("Relación " + MatrizCorrelacion.fuerza(r) + " (valor p " + valorP(p) + ")"));
            }
        }
        return celda;
    }

    // ------------------------------------------------------------------ utilidades

    private static int dato(GridPane grilla, int fila, String etiqueta, String valor, String ayuda) {
        Label clave = new Label(etiqueta);
        clave.getStyleClass().add("info-key");
        Label dato = new Label(valor);
        dato.getStyleClass().add("info-value");
        dato.setMinWidth(Region.USE_PREF_SIZE);
        if (ayuda != null) {
            Tooltip tooltip = new Tooltip(ayuda);
            clave.setTooltip(tooltip);
            dato.setTooltip(tooltip);
        }
        grilla.add(clave, 0, fila);
        grilla.add(dato, 1, fila);
        return fila + 1;
    }

    private static void lectura(VBox contenedor, String titulo, List<String> frases) {
        contenedor.getChildren().clear();
        if (titulo != null) {
            Label encabezado = new Label(titulo);
            encabezado.getStyleClass().add("card-title");
            contenedor.getChildren().add(encabezado);
        }
        for (String frase : frases) {
            Label punto = new Label("•  " + frase);
            punto.setWrapText(true);
            punto.setMinHeight(Region.USE_PREF_SIZE);
            punto.getStyleClass().add("lectura");
            contenedor.getChildren().add(punto);
        }
    }

    private String dinero(double valor) {
        return Double.isNaN(valor) ? "—" : Montos.formatear(BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP), moneda);
    }

    private static String porcentaje(double fraccion) {
        return Double.isNaN(fraccion) ? "—" : Montos.porcentaje(BigDecimal.valueOf(fraccion));
    }

    private static String decimal(double valor) {
        return Double.isNaN(valor) ? "—" : String.format(Locale.US, "%.2f", valor);
    }

    private static String valorP(double p) {
        return Double.isNaN(p) ? "—" : p < 0.001 ? "< 0.001" : String.format(Locale.US, "%.3f", p);
    }
}
