package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.AnalisisFinanciero;
import com.businessanalyzer.analytics.EstadoResultados;
import com.businessanalyzer.analytics.LecturaFinanciera;
import com.businessanalyzer.analytics.Margenes;
import com.businessanalyzer.analytics.PuntoEquilibrio;
import com.businessanalyzer.analytics.RentabilidadProducto;
import com.businessanalyzer.analytics.Variacion;
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
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Módulo "Análisis financiero": estado de resultados, márgenes, punto de equilibrio y rentabilidad. */
public class AnalisisFinancieroController implements VistaModulo {

    /** Tipo de fila del estado de resultados (define el estilo). */
    private enum TipoFila { DETALLE, SUBTOTAL, RESULTADO }

    /**
     * Fila del estado de resultados.
     *
     * @param esCosto si sube, empeora el resultado (se pinta en rojo)
     */
    private record FilaEstado(String concepto, BigDecimal actual, BigDecimal anterior, BigDecimal sobreVentas,
                              TipoFila tipo, boolean esCosto) {
        Variacion variacion() {
            return new Variacion(actual, anterior);
        }

        boolean mejora() {
            int signo = variacion().absoluta().signum();
            return esCosto ? signo < 0 : signo > 0;
        }
    }

    private static final PseudoClass SUBTOTAL = PseudoClass.getPseudoClass("subtotal");
    private static final PseudoClass RESULTADO = PseudoClass.getPseudoClass("resultado");

    private final AppContext contexto;
    private Moneda moneda = Moneda.PEN;

    private final TarjetaIndicador kpiUtilidad = new TarjetaIndicador("Utilidad neta", "fth-dollar-sign");
    private final TarjetaIndicador kpiMargen = new TarjetaIndicador("Margen neto", "fth-percent");
    private final TarjetaIndicador kpiEquilibrio = new TarjetaIndicador("Punto de equilibrio", "fth-target");
    private final TarjetaIndicador kpiSeguridad = new TarjetaIndicador("Margen de seguridad", "fth-shield");

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ProgressIndicator progreso;
    @FXML private Label lblComparacion;
    @FXML private HBox indicadores;
    @FXML private TableView<FilaEstado> tablaEstado;
    @FXML private TableColumn<FilaEstado, String> colConcepto;
    @FXML private TableColumn<FilaEstado, FilaEstado> colActual;
    @FXML private TableColumn<FilaEstado, String> colPorcentaje;
    @FXML private TableColumn<FilaEstado, FilaEstado> colAnterior;
    @FXML private TableColumn<FilaEstado, FilaEstado> colVariacionPct;
    @FXML private HBox margenes;
    @FXML private StackPane contenedorEvolucion;
    @FXML private VBox lectura;
    @FXML private GridPane gridEquilibrio;
    @FXML private StackPane contenedorEquilibrio;
    @FXML private TableView<RentabilidadProducto> tablaProductos;
    @FXML private TableColumn<RentabilidadProducto, String> colProdCodigo;
    @FXML private TableColumn<RentabilidadProducto, String> colProdNombre;
    @FXML private TableColumn<RentabilidadProducto, String> colProdCantidad;
    @FXML private TableColumn<RentabilidadProducto, BigDecimal> colProdVentas;
    @FXML private TableColumn<RentabilidadProducto, BigDecimal> colProdMargen;
    @FXML private TableColumn<RentabilidadProducto, BigDecimal> colProdMargenPct;
    @FXML private TableColumn<RentabilidadProducto, BigDecimal> colProdAporte;
    private BigDecimal margenTotalProductos = BigDecimal.ZERO;
    private long solicitudActual;

    public AnalisisFinancieroController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        indicadores.getChildren().addAll(kpiUtilidad, kpiMargen, kpiEquilibrio, kpiSeguridad);
        CamposFecha.configurar(dpDesde);
        CamposFecha.configurar(dpHasta);
        // Mes en curso hasta hoy: se compara con los mismos días del mes anterior.
        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());
        dpDesde.valueProperty().addListener((o, a, n) -> cargar());
        dpHasta.valueProperty().addListener((o, a, n) -> cargar());
        configurarTablaEstado();
        configurarTablaProductos();

        ColumnConstraints conceptos = new ColumnConstraints();
        conceptos.setMinWidth(185);
        ColumnConstraints valores = new ColumnConstraints();
        valores.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        gridEquilibrio.getColumnConstraints().setAll(conceptos, valores);
    }

    @Override
    public void alMostrar() {
        cargar();
    }

    // ------------------------------------------------------------------ período

    @FXML
    private void esteMes() {
        fijarPeriodo(LocalDate.now().withDayOfMonth(1), LocalDate.now());
    }

    @FXML
    private void mesAnterior() {
        YearMonth anterior = YearMonth.now().minusMonths(1);
        fijarPeriodo(anterior.atDay(1), anterior.atEndOfMonth());
    }

    @FXML
    private void esteAnio() {
        LocalDate hoy = LocalDate.now();
        fijarPeriodo(hoy.withDayOfYear(1), hoy);
    }

    /** Cambia ambas fechas y carga una sola vez. */
    private void fijarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(dpHasta.getValue())) {
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
            limpiar("Selecciona una empresa en la barra superior para ver su análisis.");
            return;
        }
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            return;
        }
        moneda = empresa.moneda();
        progreso.setVisible(true);
        // Si el usuario cambia el período antes de que termine el cálculo, se descarta el resultado viejo.
        long numero = ++solicitudActual;
        Tareas.ejecutar(() -> contexto.analisis().analizar(desde, hasta),
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

    private void limpiar(String mensaje) {
        tablaEstado.getItems().clear();
        tablaEstado.setPlaceholder(new Label(mensaje));
        tablaProductos.getItems().clear();
        for (TarjetaIndicador kpi : List.of(kpiUtilidad, kpiMargen, kpiEquilibrio, kpiSeguridad)) {
            kpi.actualizar(TarjetaIndicador.SIN_VALOR, "");
        }
        contenedorEvolucion.getChildren().clear();
        contenedorEquilibrio.getChildren().clear();
        margenes.getChildren().clear();
        lectura.getChildren().clear();
        contexto.mostrarFrase(com.businessanalyzer.ui.Modulo.ANALISIS_FINANCIERO, null);
        gridEquilibrio.getChildren().clear();
        lblComparacion.setText("");
    }

    private void mostrar(AnalisisFinanciero a) {
        lblComparacion.setText("Comparado con " + a.periodoAnterior().descripcion());
        mostrarIndicadores(a);
        mostrarEstado(a);
        mostrarEquilibrio(a);
        mostrarProductos(a);
    }

    private void mostrarIndicadores(AnalisisFinanciero a) {
        EstadoResultados e = a.actual();
        Variacion variacion = new Variacion(e.utilidadNeta(), a.anterior().utilidadNeta());
        kpiUtilidad.actualizar(Montos.formatear(e.utilidadNeta(), moneda),
                variacion.relativa() == null ? "Sin datos del período anterior"
                        : signo(variacion.relativa()) + Montos.porcentaje(variacion.relativa()) + " vs. período anterior");
        kpiMargen.actualizar(Montos.porcentaje(e.margenNeto()),
                "Bruto " + Montos.porcentaje(e.margenBruto()) + " · operativo " + Montos.porcentaje(e.margenOperativo()));

        PuntoEquilibrio pe = a.equilibrio();
        if (pe.ventasEquilibrio() == null) {
            kpiEquilibrio.actualizar("No alcanzable", e.ventasNetas().signum() == 0
                    ? "Sin ventas en el período" : "Las ventas no cubren sus costos variables");
            kpiSeguridad.actualizar(TarjetaIndicador.SIN_VALOR, "Requiere un punto de equilibrio");
        } else {
            kpiEquilibrio.actualizar(Montos.formatear(pe.ventasEquilibrio(), moneda), pe.alcanzado()
                    ? "Alcanzado · ventas al " + Montos.porcentaje(pe.cobertura()) + " del equilibrio"
                    : "Faltan " + Montos.formatear(pe.ventasFaltantes(), moneda) + " en ventas");
            kpiSeguridad.actualizar(Montos.porcentaje(pe.margenSeguridad()), pe.alcanzado()
                    ? "Lo que pueden caer las ventas sin pérdida"
                    : "Ventas por debajo del equilibrio");
        }
    }

    // ------------------------------------------------------------------ estado de resultados

    private void mostrarEstado(AnalisisFinanciero a) {
        EstadoResultados e = a.actual();
        EstadoResultados p = a.anterior();
        List<FilaEstado> filas = new ArrayList<>();
        filas.add(fila("Ventas netas", e, p, EstadoResultados::ventasNetas, TipoFila.DETALLE, false));
        filas.add(fila("(−) Costo de ventas", e, p, EstadoResultados::costoVentas, TipoFila.DETALLE, true));
        filas.add(fila("= Utilidad bruta", e, p, EstadoResultados::utilidadBruta, TipoFila.SUBTOTAL, false));
        filas.add(fila("(−) Gastos operativos", e, p, EstadoResultados::gastosOperativos, TipoFila.DETALLE, true));
        filas.add(fila("= Utilidad operativa", e, p, EstadoResultados::utilidadOperativa, TipoFila.SUBTOTAL, false));
        filas.add(fila("(+) Ingresos financieros", e, p, EstadoResultados::ingresosFinancieros, TipoFila.DETALLE, false));
        filas.add(fila("(−) Gastos financieros", e, p, EstadoResultados::gastosFinancieros, TipoFila.DETALLE, true));
        filas.add(fila("(+) Otros ingresos", e, p, EstadoResultados::otrosIngresos, TipoFila.DETALLE, false));
        filas.add(fila("= Utilidad antes de impuestos", e, p, EstadoResultados::utilidadAntesImpuestos, TipoFila.SUBTOTAL, false));
        filas.add(fila("(−) Impuesto a la renta", e, p, EstadoResultados::impuestoRenta, TipoFila.DETALLE, true));
        filas.add(fila("= Utilidad neta", e, p, EstadoResultados::utilidadNeta, TipoFila.RESULTADO, false));
        tablaEstado.getItems().setAll(filas);

        margenes.getChildren().setAll(
                chipMargen("Bruto", e.margenBruto(), p.margenBruto()),
                chipMargen("Operativo", e.margenOperativo(), p.margenOperativo()),
                chipMargen("Neto", e.margenNeto(), p.margenNeto()));

        contenedorEvolucion.getChildren().setAll(Graficos.visor(Graficos.evolucionMensual(a.evolucion(), moneda)));

        lectura.getChildren().clear();
        List<String> frases = LecturaFinanciera.generar(a, moneda);
        contexto.mostrarFrase(com.businessanalyzer.ui.Modulo.ANALISIS_FINANCIERO, frases.isEmpty() ? null : frases.get(0));
        for (String frase : frases) {
            Label punto = new Label("•  " + frase);
            punto.setWrapText(true);
            punto.getStyleClass().add("lectura");
            lectura.getChildren().add(punto);
        }
    }

    private static FilaEstado fila(String concepto, EstadoResultados actual, EstadoResultados anterior,
                                   Function<EstadoResultados, BigDecimal> valor, TipoFila tipo, boolean esCosto) {
        BigDecimal monto = valor.apply(actual);
        return new FilaEstado(concepto, monto, valor.apply(anterior), actual.sobreVentas(monto), tipo, esCosto);
    }

    /** "Margen bruto 33.3 % (+2.1 pp)": pp = puntos porcentuales respecto al período anterior. */
    private static Label chipMargen(String nombre, BigDecimal actual, BigDecimal anterior) {
        String texto = "Margen " + nombre.toLowerCase() + "  " + Montos.porcentaje(actual);
        if (actual != null && anterior != null) {
            BigDecimal puntos = actual.subtract(anterior).movePointRight(2).setScale(1, RoundingMode.HALF_UP);
            texto += "  (" + (puntos.signum() >= 0 ? "+" : "") + puntos + " pp)";
        }
        Label chip = new Label(texto);
        chip.getStyleClass().add("chip");
        chip.setMinWidth(Region.USE_PREF_SIZE);
        chip.setTooltip(new javafx.scene.control.Tooltip("pp = puntos porcentuales de diferencia con el período anterior"));
        return chip;
    }

    private void configurarTablaEstado() {
        colConcepto.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().concepto()));
        colPorcentaje.setCellValueFactory(c -> new ReadOnlyStringWrapper(Montos.porcentaje(c.getValue().sobreVentas())));
        configurarCeldaFila(colActual, f -> Montos.formatear(f.actual(), moneda), f -> f.actual().signum() < 0 && f.tipo() != TipoFila.DETALLE ? "margen-negativo" : null);
        configurarCeldaFila(colAnterior, f -> Montos.formatear(f.anterior(), moneda), f -> null);
        configurarCeldaFila(colVariacionPct, f -> {
            BigDecimal relativa = f.variacion().relativa();
            return relativa == null ? "—" : signo(relativa) + Montos.porcentaje(relativa);
        }, this::claseVariacion);

        tablaEstado.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(FilaEstado fila, boolean vacio) {
                super.updateItem(fila, vacio);
                pseudoClassStateChanged(SUBTOTAL, !vacio && fila != null && fila.tipo() == TipoFila.SUBTOTAL);
                pseudoClassStateChanged(RESULTADO, !vacio && fila != null && fila.tipo() == TipoFila.RESULTADO);
            }
        });
        tablaEstado.setPlaceholder(new Label("Elige un período para ver el estado de resultados."));
    }

    private String claseVariacion(FilaEstado f) {
        if (f.variacion().absoluta().signum() == 0) {
            return null;
        }
        return f.mejora() ? "margen-ok" : "margen-negativo";
    }

    private static void configurarCeldaFila(TableColumn<FilaEstado, FilaEstado> columna,
                                            Function<FilaEstado, String> texto,
                                            Function<FilaEstado, String> clase) {
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(FilaEstado fila, boolean vacio) {
                super.updateItem(fila, vacio);
                getStyleClass().removeAll("margen-ok", "margen-negativo");
                if (vacio || fila == null) {
                    setText(null);
                    return;
                }
                setText(texto.apply(fila));
                String estilo = clase.apply(fila);
                if (estilo != null) {
                    getStyleClass().add(estilo);
                }
            }
        });
    }

    // ------------------------------------------------------------------ punto de equilibrio

    private void mostrarEquilibrio(AnalisisFinanciero a) {
        PuntoEquilibrio pe = a.equilibrio();
        BigDecimal costoVentas = a.actual().costoVentas();
        BigDecimal gastosVariables = pe.costosVariables().subtract(costoVentas);

        gridEquilibrio.getChildren().clear();
        int fila = 0;
        fila = filaEquilibrio(fila, "Ventas netas", Montos.formatear(pe.ventas(), moneda), false);
        fila = filaEquilibrio(fila, "(−) Costo de lo vendido", Montos.formatear(costoVentas, moneda), false);
        fila = filaEquilibrio(fila, "(−) Gastos variables", Montos.formatear(gastosVariables, moneda), false);
        fila = filaEquilibrio(fila, "= Margen de contribución", Montos.formatear(pe.margenContribucion(), moneda), true);
        fila = filaEquilibrio(fila, "Razón de contribución", Montos.porcentaje(pe.razonContribucion())
                + " de cada venta", false);
        fila = filaEquilibrio(fila, "Costos fijos", Montos.formatear(pe.costosFijos(), moneda), false);
        fila = filaEquilibrio(fila, "Punto de equilibrio", pe.ventasEquilibrio() == null
                ? "No alcanzable" : Montos.formatear(pe.ventasEquilibrio(), moneda), true);
        Label formula = new Label("= costos fijos ÷ razón de contribución");
        formula.getStyleClass().add("hint");
        formula.setWrapText(true);
        gridEquilibrio.add(formula, 1, fila++);
        filaEquilibrio(fila, "Margen de seguridad", Montos.porcentaje(pe.margenSeguridad()), false);

        contenedorEquilibrio.getChildren().setAll(Graficos.visor(Graficos.puntoEquilibrio(pe, moneda)));
    }

    private int filaEquilibrio(int fila, String concepto, String valor, boolean destacado) {
        Label etiqueta = new Label(concepto);
        etiqueta.getStyleClass().add(destacado ? "step-title" : "info-key");
        Label dato = new Label(valor);
        dato.getStyleClass().add(destacado ? "total-valor-destacado" : "info-value");
        gridEquilibrio.add(etiqueta, 0, fila);
        gridEquilibrio.add(dato, 1, fila);
        return fila + 1;
    }

    // ------------------------------------------------------------------ rentabilidad por producto

    private void mostrarProductos(AnalisisFinanciero a) {
        margenTotalProductos = a.margenTotalProductos();
        tablaProductos.setItems(FXCollections.observableArrayList(a.productos()));
        tablaProductos.setPlaceholder(new Label("No hubo ventas en el período."));
        contenedorProductos.getChildren().setAll(Graficos.visor(Graficos.rentabilidadProductos(a.productos(), 8, moneda)));
    }

    @FXML private StackPane contenedorProductos;

    private void configurarTablaProductos() {
        colProdCodigo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().codigo()));
        colProdNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colProdCantidad.setCellValueFactory(c -> new ReadOnlyStringWrapper(Montos.cantidad(c.getValue().cantidad())));
        colProdVentas.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().ventas()));
        colProdMargen.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().margen()));
        colProdMargenPct.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().margenSobreVentas()));
        colProdAporte.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(margenTotalProductos.signum() == 0 ? null
                : c.getValue().margen().divide(margenTotalProductos, Margenes.ESCALA_RATIO, RoundingMode.HALF_UP)));
        colProdVentas.setCellFactory(col -> celdaNumero(v -> Montos.formatear(v, moneda)));
        colProdMargen.setCellFactory(col -> celdaNumero(v -> Montos.formatear(v, moneda)));
        colProdMargenPct.setCellFactory(col -> celdaNumero(Montos::porcentaje));
        colProdAporte.setCellFactory(col -> celdaNumero(Montos::porcentaje));
    }

    private static TableCell<RentabilidadProducto, BigDecimal> celdaNumero(Function<BigDecimal, String> formato) {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal valor, boolean vacio) {
                super.updateItem(valor, vacio);
                getStyleClass().remove("margen-negativo");
                if (vacio) {
                    setText(null);
                    return;
                }
                setText(formato.apply(valor));
                if (valor != null && valor.signum() < 0) {
                    getStyleClass().add("margen-negativo");
                }
            }
        };
    }

    private static String signo(BigDecimal valor) {
        return valor.signum() > 0 ? "+" : "";
    }
}
