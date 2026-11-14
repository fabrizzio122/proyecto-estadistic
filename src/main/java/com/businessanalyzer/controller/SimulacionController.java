package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.PuntoEquilibrio;
import com.businessanalyzer.analytics.simulacion.BaseSimulacion;
import com.businessanalyzer.analytics.simulacion.EscenarioPredefinido;
import com.businessanalyzer.analytics.simulacion.ResultadoEscenario;
import com.businessanalyzer.analytics.simulacion.Simulador;
import com.businessanalyzer.analytics.simulacion.Supuestos;
import com.businessanalyzer.analytics.simulacion.Variable;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Escenario;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.service.SimulacionService;
import com.businessanalyzer.service.SimulacionService.TipoBase;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Formularios;
import com.businessanalyzer.ui.Graficos;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.Tareas;
import com.businessanalyzer.ui.TarjetaIndicador;
import com.businessanalyzer.ui.VistaModulo;
import com.businessanalyzer.util.Montos;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Módulo "Simulación de escenarios": ¿qué pasaría si cambian precios, volumen o costos? */
public class SimulacionController implements VistaModulo {

    /** Fila de la tabla base frente a escenario. */
    private record FilaResultado(String concepto, String base, String escenario, String diferencia,
                                 String claseDiferencia, boolean destacada) {
    }

    /** Fila de la comparación de escenarios: un valor por columna. */
    private record FilaMetrica(String concepto, List<String> valores) {
    }

    private static final PseudoClass SUBTOTAL = PseudoClass.getPseudoClass("subtotal");

    private final AppContext contexto;
    private final Map<Variable, ControlCambio> controles = new EnumMap<>(Variable.class);
    private final TextField txtCostoAdicional = new TextField("0");
    private final TextField txtTasa = new TextField("0");
    private final Label lblErrorSupuestos = new Label();
    private BaseSimulacion base;
    private List<Escenario> guardados = List.of();
    private Moneda moneda = Moneda.PEN;
    private boolean cargando;
    private long solicitudActual;

    private final TarjetaIndicador kpiUtilidad = new TarjetaIndicador("Utilidad neta del escenario", "fth-dollar-sign");
    private final TarjetaIndicador kpiMargen = new TarjetaIndicador("Margen neto", "fth-percent");
    private final TarjetaIndicador kpiEquilibrio = new TarjetaIndicador("Punto de equilibrio", "fth-target");
    private final TarjetaIndicador kpiSeguridad = new TarjetaIndicador("Margen de seguridad", "fth-shield");

    @FXML private ComboBox<TipoBase> cmbBase;
    @FXML private Label lblBase;
    @FXML private ProgressIndicator progreso;
    @FXML private HBox indicadores;
    @FXML private VBox panelSupuestos;
    @FXML private TableView<FilaResultado> tablaResultados;
    @FXML private TableColumn<FilaResultado, String> colConcepto;
    @FXML private TableColumn<FilaResultado, String> colBase;
    @FXML private TableColumn<FilaResultado, String> colEscenario;
    @FXML private TableColumn<FilaResultado, FilaResultado> colVariacion;
    @FXML private StackPane contenedorTornado;
    @FXML private VBox lecturaSensibilidad;
    @FXML private TextField txtMeta;
    @FXML private VBox resultadoMeta;
    @FXML private ListView<Escenario> listaEscenarios;
    @FXML private Button btnCargar;
    @FXML private Button btnEliminar;
    @FXML private TableView<FilaMetrica> tablaComparacion;
    @FXML private StackPane contenedorComparacion;

    public SimulacionController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        indicadores.getChildren().addAll(kpiUtilidad, kpiMargen, kpiEquilibrio, kpiSeguridad);
        cmbBase.getItems().setAll(TipoBase.values());
        cmbBase.setValue(TipoBase.PROMEDIO_3_MESES);
        cmbBase.valueProperty().addListener((o, a, n) -> cargarBase());
        construirPanelSupuestos();
        configurarTablaResultados();
        listaEscenarios.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            btnCargar.setDisable(n == null);
            btnEliminar.setDisable(n == null);
        });
        btnCargar.setDisable(true);
        btnEliminar.setDisable(true);
        listaEscenarios.setPlaceholder(new Label("Guarda un escenario para compararlo."));
    }

    @Override
    public void alMostrar() {
        cargarBase();
    }

    // ------------------------------------------------------------------ base

    private void cargarBase() {
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        if (empresa == null) {
            lblBase.setText("Selecciona una empresa en la barra superior.");
            base = null;
            return;
        }
        moneda = empresa.moneda();
        TipoBase tipo = cmbBase.getValue();
        progreso.setVisible(true);
        long numero = ++solicitudActual;
        Tareas.ejecutar(() -> new Object[]{contexto.simulacion().base(tipo), contexto.simulacion().listarEscenarios()},
                datos -> {
                    if (numero != solicitudActual) {
                        return;
                    }
                    progreso.setVisible(false);
                    boolean primeraVez = base == null;
                    base = (BaseSimulacion) datos[0];
                    @SuppressWarnings("unchecked")
                    List<Escenario> escenarios = (List<Escenario>) datos[1];
                    guardados = escenarios;
                    listaEscenarios.getItems().setAll(escenarios);
                    lblBase.setText(base.periodo().descripcion() + (base.meses() > 1 ? " (promedio mensual)" : "")
                            + (base.sinVentas() ? "  ·  sin ventas en ese período" : ""));
                    if (primeraVez) {
                        aplicar(Supuestos.neutros(base.tasaImpuestoEfectiva()));
                    } else {
                        recalcular();
                    }
                },
                error -> {
                    if (numero == solicitudActual) {
                        progreso.setVisible(false);
                        Notificador.error(Errores.mensaje(error));
                    }
                });
    }

    // ------------------------------------------------------------------ supuestos

    private void construirPanelSupuestos() {
        Label titulo = new Label("Supuestos del escenario");
        titulo.getStyleClass().add("card-title");
        Label ayuda = new Label("Mueve las barras o escribe el porcentaje. Los resultados se recalculan al instante.");
        ayuda.getStyleClass().add("hint");
        ayuda.setWrapText(true);
        ayuda.setMinHeight(Region.USE_PREF_SIZE);

        FlowPane predefinidos = new FlowPane(8, 8);
        for (EscenarioPredefinido p : EscenarioPredefinido.values()) {
            Button boton = new Button(p.nombre());
            boton.getStyleClass().add("button-secondary");
            boton.setTooltip(new Tooltip(p.descripcion()));
            boton.setOnAction(e -> aplicar(p.aplicar(tasaActual())));
            predefinidos.getChildren().add(boton);
        }

        panelSupuestos.getChildren().addAll(titulo, ayuda, predefinidos);
        for (Variable variable : Variable.values()) {
            ControlCambio control = new ControlCambio(variable);
            controles.put(variable, control);
            panelSupuestos.getChildren().add(control.nodo());
        }

        txtCostoAdicional.setPrefWidth(110);
        txtTasa.setPrefWidth(70);
        confirmarAlSalir(txtCostoAdicional);
        confirmarAlSalir(txtTasa);
        GridPane extras = Formularios.grilla();
        Formularios.fila(extras, "Costo fijo adicional (por mes)", txtCostoAdicional);
        Formularios.nota(extras, "Ej.: 1500 si contratas a alguien; negativo si es un ahorro.");
        Formularios.fila(extras, "Impuesto a la renta (%)", txtTasa);
        extras.getColumnConstraints().get(0).setMinWidth(170);
        extras.getColumnConstraints().get(1).setMinWidth(100);

        lblErrorSupuestos.getStyleClass().add("form-error");
        lblErrorSupuestos.setWrapText(true);
        lblErrorSupuestos.setVisible(false);
        lblErrorSupuestos.managedProperty().bind(lblErrorSupuestos.visibleProperty());
        panelSupuestos.getChildren().addAll(extras, lblErrorSupuestos);
    }

    private void confirmarAlSalir(TextField campo) {
        campo.setOnAction(e -> recalcular());
        campo.focusedProperty().addListener((o, a, enfocado) -> {
            if (!enfocado) {
                recalcular();
            }
        });
    }

    /** Pone los controles en los valores indicados y recalcula una sola vez. */
    private void aplicar(Supuestos s) {
        cargando = true;
        for (Variable variable : Variable.values()) {
            controles.get(variable).establecer(s.valor(variable));
        }
        txtCostoAdicional.setText(Montos.paraEditar(s.costoFijoAdicional()).replace(".00", ""));
        txtTasa.setText(textoPorcentaje(s.tasaImpuesto().movePointRight(2).doubleValue()));
        cargando = false;
        recalcular();
    }

    @FXML
    private void restablecer() {
        if (base != null) {
            aplicar(Supuestos.neutros(base.tasaImpuestoEfectiva()));
        }
    }

    private BigDecimal tasaActual() {
        try {
            return Montos.leer(txtTasa.getText().replace("%", ""), "Impuesto").movePointLeft(2);
        } catch (ValidacionException e) {
            return base == null ? BigDecimal.ZERO : base.tasaImpuestoEfectiva();
        }
    }

    /** Supuestos de los controles; lanza ValidacionException si alguno no es válido. */
    private Supuestos supuestosActuales() {
        Supuestos s = new Supuestos(
                controles.get(Variable.PRECIO).fraccion(),
                controles.get(Variable.VOLUMEN).fraccion(),
                controles.get(Variable.COSTO_UNITARIO).fraccion(),
                controles.get(Variable.GASTOS_VARIABLES).fraccion(),
                controles.get(Variable.COSTOS_FIJOS).fraccion(),
                txtCostoAdicional.getText().isBlank() ? BigDecimal.ZERO
                        : Montos.leer(txtCostoAdicional.getText(), "Costo fijo adicional"),
                txtTasa.getText().isBlank() ? BigDecimal.ZERO
                        : Montos.leer(txtTasa.getText().replace("%", ""), "Impuesto a la renta").movePointLeft(2));
        SimulacionService.validar(s);
        return s;
    }

    // ------------------------------------------------------------------ cálculo y presentación

    private void recalcular() {
        if (cargando || base == null) {
            return;
        }
        Supuestos s;
        try {
            s = supuestosActuales();
            lblErrorSupuestos.setVisible(false);
        } catch (ValidacionException e) {
            lblErrorSupuestos.setText(e.getMessage());
            lblErrorSupuestos.setVisible(true);
            return;
        }
        ResultadoEscenario rBase = Simulador.simular(base, Supuestos.neutros(base.tasaImpuestoEfectiva()));
        ResultadoEscenario rEscenario = Simulador.simular(base, s);
        mostrarIndicadores(rBase, rEscenario);
        mostrarTabla(rBase, rEscenario);
        mostrarSensibilidad(s);
        mostrarComparacion(rBase, rEscenario);
        resultadoMeta.getChildren().clear();
    }

    private void mostrarIndicadores(ResultadoEscenario b, ResultadoEscenario e) {
        BigDecimal diferencia = e.utilidadNeta().subtract(b.utilidadNeta());
        kpiUtilidad.actualizar(dinero(e.utilidadNeta()), "Base " + dinero(b.utilidadNeta()) + "  ("
                + (diferencia.signum() >= 0 ? "+" : "") + dinero(diferencia) + ")");
        kpiMargen.actualizar(Montos.porcentaje(e.margenNeto()), "Base " + Montos.porcentaje(b.margenNeto()));
        PuntoEquilibrio peEscenario = e.equilibrio();
        PuntoEquilibrio peBase = b.equilibrio();
        kpiEquilibrio.actualizar(peEscenario.ventasEquilibrio() == null ? "No alcanzable" : dinero(peEscenario.ventasEquilibrio()),
                "Base " + (peBase.ventasEquilibrio() == null ? "no alcanzable" : dinero(peBase.ventasEquilibrio())));
        kpiSeguridad.actualizar(Montos.porcentaje(peEscenario.margenSeguridad()),
                peEscenario.alcanzado() ? "Por encima del equilibrio" : "Por debajo del equilibrio");
    }

    private void mostrarTabla(ResultadoEscenario b, ResultadoEscenario e) {
        List<FilaResultado> filas = new ArrayList<>();
        filas.add(monto("Ventas", b, e, ResultadoEscenario::ventas, false, false));
        filas.add(monto("(−) Costo de ventas", b, e, ResultadoEscenario::costoVentas, true, false));
        filas.add(monto("(−) Gastos variables", b, e, ResultadoEscenario::gastosVariables, true, false));
        filas.add(monto("= Margen de contribución", b, e, ResultadoEscenario::margenContribucion, false, true));
        filas.add(monto("(−) Costos fijos", b, e, ResultadoEscenario::costosFijos, true, false));
        filas.add(monto("(+) Otros ingresos", b, e, ResultadoEscenario::otrosIngresos, false, false));
        filas.add(monto("= Utilidad antes de impuestos", b, e, ResultadoEscenario::utilidadAntesImpuestos, false, true));
        filas.add(monto("(−) Impuesto a la renta", b, e, ResultadoEscenario::impuesto, true, false));
        filas.add(monto("= Utilidad neta", b, e, ResultadoEscenario::utilidadNeta, false, true));
        filas.add(porcentajeFila("Margen neto", b.margenNeto(), e.margenNeto()));
        BigDecimal peBase = b.equilibrio().ventasEquilibrio();
        BigDecimal peEsc = e.equilibrio().ventasEquilibrio();
        String difPe = peBase == null || peEsc == null ? "—" : signo(peEsc.subtract(peBase)) + dinero(peEsc.subtract(peBase));
        String clasePe = peBase == null || peEsc == null || peEsc.compareTo(peBase) == 0 ? null
                : peEsc.compareTo(peBase) < 0 ? "margen-ok" : "margen-negativo";
        filas.add(new FilaResultado("Punto de equilibrio", peBase == null ? "No alcanzable" : dinero(peBase),
                peEsc == null ? "No alcanzable" : dinero(peEsc), difPe, clasePe, false));
        filas.add(porcentajeFila("Margen de seguridad", b.equilibrio().margenSeguridad(), e.equilibrio().margenSeguridad()));
        tablaResultados.getItems().setAll(filas);
    }

    private FilaResultado monto(String concepto, ResultadoEscenario b, ResultadoEscenario e,
                                Function<ResultadoEscenario, BigDecimal> valor, boolean esCosto, boolean destacada) {
        BigDecimal vb = valor.apply(b);
        BigDecimal ve = valor.apply(e);
        BigDecimal diferencia = ve.subtract(vb);
        String clase = diferencia.signum() == 0 ? null
                : (diferencia.signum() > 0) != esCosto ? "margen-ok" : "margen-negativo";
        return new FilaResultado(concepto, dinero(vb), dinero(ve),
                diferencia.signum() == 0 ? "—" : signo(diferencia) + dinero(diferencia), clase, destacada);
    }

    private static FilaResultado porcentajeFila(String concepto, BigDecimal base, BigDecimal escenario) {
        String diferencia = "—";
        String clase = null;
        if (base != null && escenario != null && base.compareTo(escenario) != 0) {
            BigDecimal puntos = escenario.subtract(base).movePointRight(2).setScale(1, RoundingMode.HALF_UP);
            diferencia = signo(puntos) + puntos + " pp";
            clase = puntos.signum() > 0 ? "margen-ok" : "margen-negativo";
        }
        return new FilaResultado(concepto, Montos.porcentaje(base), Montos.porcentaje(escenario), diferencia, clase, false);
    }

    private void configurarTablaResultados() {
        colConcepto.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().concepto()));
        colBase.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().base()));
        colEscenario.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().escenario()));
        colVariacion.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue()));
        colVariacion.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(FilaResultado fila, boolean vacio) {
                super.updateItem(fila, vacio);
                getStyleClass().removeAll("margen-ok", "margen-negativo");
                if (vacio || fila == null) {
                    setText(null);
                    return;
                }
                setText(fila.diferencia());
                if (fila.claseDiferencia() != null) {
                    getStyleClass().add(fila.claseDiferencia());
                }
            }
        });
        tablaResultados.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(FilaResultado fila, boolean vacio) {
                super.updateItem(fila, vacio);
                pseudoClassStateChanged(SUBTOTAL, !vacio && fila != null && fila.destacada());
            }
        });
        tablaResultados.setPlaceholder(new Label("Cargando la base..."));
    }

    // ------------------------------------------------------------------ sensibilidad y meta

    private void mostrarSensibilidad(Supuestos s) {
        List<Simulador.Sensibilidad> sensibilidad = Simulador.sensibilidad(base, s);
        contenedorTornado.getChildren().setAll(Graficos.visor(
                Graficos.tornado(sensibilidad, Simulador.VARIACION_SENSIBILIDAD_TEXTO, moneda)));
        lecturaSensibilidad.getChildren().clear();
        if (base.sinVentas()) {
            lecturaSensibilidad.getChildren().add(frase("Sin ventas en la base, la sensibilidad no es representativa."));
            return;
        }
        Simulador.Sensibilidad mayor = sensibilidad.get(0);
        Simulador.Sensibilidad menor = sensibilidad.get(sensibilidad.size() - 1);
        lecturaSensibilidad.getChildren().addAll(
                frase("Lo que más pesa es \"" + mayor.variable().descripcion() + "\": si sube "
                        + Simulador.VARIACION_SENSIBILIDAD_TEXTO + ", la utilidad neta mensual queda en "
                        + dinero(mayor.utilidadSiSube()) + "; si baja, en " + dinero(mayor.utilidadSiBaja()) + "."),
                frase("Lo que menos pesa es \"" + menor.variable().descripcion() + "\". Enfoca tus esfuerzos en las barras más largas."));
    }

    @FXML
    private void calcularMeta() {
        resultadoMeta.getChildren().clear();
        if (base == null) {
            return;
        }
        BigDecimal meta;
        Supuestos s;
        try {
            meta = Montos.leer(txtMeta.getText(), "Utilidad neta deseada");
            s = supuestosActuales();
        } catch (ValidacionException e) {
            resultadoMeta.getChildren().add(frase(e.getMessage()));
            return;
        }
        Optional<BigDecimal> precio = Simulador.cambioPrecioParaMeta(base, s, meta);
        Optional<BigDecimal> volumen = Simulador.cambioVolumenParaMeta(base, s, meta);
        resultadoMeta.getChildren().add(resultadoMetaFila("Solo con precios", precio, s.precio(),
                "No es posible: no hay ventas en la base."));
        resultadoMeta.getChildren().add(resultadoMetaFila("Solo con volumen", volumen, s.volumen(),
                "No es posible: con estos precios y costos cada venta no deja margen, o la meta ya se supera aun sin vender."));
        resultadoMeta.getChildren().add(frase("Los porcentajes son frente a la base. Puedes llevarlos al simulador para ver el detalle."));
    }

    private VBox resultadoMetaFila(String titulo, Optional<BigDecimal> necesario, BigDecimal actual, String imposible) {
        Label encabezado = new Label(titulo);
        encabezado.getStyleClass().add("step-title");
        VBox caja = new VBox(2, encabezado);
        if (necesario.isEmpty()) {
            caja.getChildren().add(frase(imposible));
            return caja;
        }
        BigDecimal valor = necesario.get();
        Label cifra = new Label((valor.signum() >= 0 ? "+" : "") + Montos.porcentaje(valor));
        cifra.getStyleClass().add("total-valor-destacado");
        BigDecimal adicional = valor.subtract(actual);
        caja.getChildren().addAll(cifra, frase(adicional.signum() <= 0
                ? "El escenario actual ya alcanza la meta por este lado."
                : "Hace falta " + Montos.porcentaje(adicional) + " más que en el escenario actual."));
        return caja;
    }

    // ------------------------------------------------------------------ comparación de escenarios

    private void mostrarComparacion(ResultadoEscenario rBase, ResultadoEscenario rActual) {
        List<String> nombres = new ArrayList<>(List.of("Base", "Escenario actual"));
        List<ResultadoEscenario> resultados = new ArrayList<>(List.of(rBase, rActual));
        for (Escenario e : guardados) {
            nombres.add(e.nombre());
            resultados.add(Simulador.simular(base, e.supuestos()));
        }
        // Columnas dinámicas (una por escenario): ancho fijo y desplazamiento horizontal si no entran.
        tablaComparacion.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tablaComparacion.getColumns().clear();
        TableColumn<FilaMetrica, String> concepto = new TableColumn<>("Concepto");
        concepto.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().concepto()));
        concepto.setPrefWidth(140);
        concepto.setMinWidth(130);
        concepto.setSortable(false);
        tablaComparacion.getColumns().add(concepto);
        for (int i = 0; i < nombres.size(); i++) {
            int indice = i;
            TableColumn<FilaMetrica, String> columna = new TableColumn<>(nombres.get(i));
            columna.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().valores().get(indice)));
            columna.getStyleClass().add("col-numero");
            columna.setPrefWidth(118);
            columna.setMinWidth(110);
            columna.setSortable(false);
            tablaComparacion.getColumns().add(columna);
        }
        tablaComparacion.getItems().setAll(
                metrica("Ventas", resultados, r -> dinero(r.ventas())),
                metrica("Costos totales", resultados, r -> dinero(r.costosTotales())),
                metrica("Utilidad neta", resultados, r -> dinero(r.utilidadNeta())),
                metrica("Margen neto", resultados, r -> Montos.porcentaje(r.margenNeto())),
                metrica("Punto de equilibrio", resultados, r -> r.equilibrio().ventasEquilibrio() == null
                        ? "No alcanzable" : dinero(r.equilibrio().ventasEquilibrio())));
        org.jfree.chart.fx.ChartViewer visor = Graficos.visor(Graficos.comparacionEscenarios(nombres, resultados, moneda));
        visor.setMinSize(200, 100);
        contenedorComparacion.getChildren().setAll(visor);
    }

    private static FilaMetrica metrica(String concepto, List<ResultadoEscenario> resultados,
                                       Function<ResultadoEscenario, String> valor) {
        return new FilaMetrica(concepto, resultados.stream().map(valor).toList());
    }

    @FXML
    private void guardarEscenario() {
        Supuestos s;
        try {
            s = supuestosActuales();
        } catch (ValidacionException e) {
            Notificador.error(e.getMessage());
            return;
        }
        TextField nombre = new TextField();
        TextField descripcion = new TextField();
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Nombre *", nombre);
        Formularios.fila(grilla, "Descripción", descripcion);
        Escenario[] guardado = new Escenario[1];
        boolean ok = Dialogos.formulario(tablaResultados.getScene().getWindow(), "Guardar escenario",
                "Si ya existe un escenario con ese nombre, se reemplazará.", grilla, "Guardar",
                () -> guardado[0] = contexto.simulacion().guardar(nombre.getText(), descripcion.getText(), s));
        if (ok) {
            Notificador.exito("Escenario \"" + guardado[0].nombre() + "\" guardado.");
            cargarBase();
        }
    }

    @FXML
    private void cargarEscenario() {
        Escenario e = listaEscenarios.getSelectionModel().getSelectedItem();
        if (e != null) {
            aplicar(e.supuestos());
            Notificador.info("Escenario \"" + e.nombre() + "\" cargado en el simulador.");
        }
    }

    @FXML
    private void eliminarEscenario() {
        Escenario e = listaEscenarios.getSelectionModel().getSelectedItem();
        if (e == null || !Dialogos.confirmar(tablaResultados.getScene().getWindow(), "Eliminar escenario",
                "¿Eliminar el escenario \"" + e.nombre() + "\"?")) {
            return;
        }
        try {
            contexto.simulacion().eliminar(e.id());
            Notificador.exito("Escenario eliminado.");
            cargarBase();
        } catch (RuntimeException ex) {
            Notificador.error(Errores.mensaje(ex));
        }
    }

    // ------------------------------------------------------------------ utilidades

    private String dinero(BigDecimal valor) {
        return Montos.formatear(valor.setScale(2, RoundingMode.HALF_UP), moneda);
    }

    private static String signo(BigDecimal valor) {
        return valor.signum() > 0 ? "+" : "";
    }

    private static Label frase(String texto) {
        Label label = new Label("•  " + texto);
        label.getStyleClass().add("lectura");
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    private static String textoPorcentaje(double valor) {
        String texto = String.format(Locale.US, "%.2f", valor);
        return texto.replaceAll("\\.?0+$", "");
    }

    /** Barra deslizable (−50 % a +100 %) con campo de texto para escribir valores exactos (−100 % a +500 %). */
    private final class ControlCambio {
        private final Variable variable;
        private final Slider barra = new Slider(-50, 100, 0);
        private final TextField texto = new TextField("0");
        private double valor;
        private boolean moviendo;

        ControlCambio(Variable variable) {
            this.variable = variable;
            barra.setBlockIncrement(1);
            barra.setMajorTickUnit(25);
            barra.valueProperty().addListener((o, a, n) -> {
                if (!moviendo) {
                    fijar(Math.round(n.doubleValue()), false);
                }
            });
            texto.setPrefWidth(64);
            texto.setAlignment(Pos.CENTER_RIGHT);
            texto.setOnAction(e -> confirmarTexto());
            texto.focusedProperty().addListener((o, a, enfocado) -> {
                if (!enfocado) {
                    confirmarTexto();
                }
            });
        }

        VBox nodo() {
            Label nombre = new Label(variable.descripcion());
            nombre.getStyleClass().add("info-value");
            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);
            Label signoPorcentaje = new Label("%");
            signoPorcentaje.getStyleClass().add("muted");
            HBox cabecera = new HBox(6, nombre, espacio, texto, signoPorcentaje);
            cabecera.setAlignment(Pos.CENTER_LEFT);
            Tooltip ayuda = new Tooltip(variable.ayuda());
            nombre.setTooltip(ayuda);
            barra.setTooltip(ayuda);
            return new VBox(4, cabecera, barra);
        }

        BigDecimal fraccion() {
            return BigDecimal.valueOf(valor).movePointLeft(2);
        }

        void establecer(BigDecimal fraccion) {
            fijar(fraccion.movePointRight(2).doubleValue(), true);
        }

        private void confirmarTexto() {
            try {
                fijar(Montos.leer(texto.getText().replace("%", "").replace("+", ""), variable.descripcion()).doubleValue(), true);
            } catch (ValidacionException e) {
                texto.setText(textoPorcentaje(valor));
            }
        }

        private void fijar(double nuevo, boolean moverBarra) {
            valor = nuevo;
            moviendo = true;
            if (moverBarra) {
                barra.setValue(Math.max(barra.getMin(), Math.min(barra.getMax(), nuevo)));
            }
            texto.setText(textoPorcentaje(nuevo));
            moviendo = false;
            recalcular();
        }
    }
}
