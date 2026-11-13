package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.Margenes;
import com.businessanalyzer.analytics.ResumenFinanciero;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.CategoriaFinanciera;
import com.businessanalyzer.model.Comportamiento;
import com.businessanalyzer.model.DatosMovimiento;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.MovimientoFinanciero;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.TipoMovimiento;
import com.businessanalyzer.service.FinanzasService;
import com.businessanalyzer.ui.CamposFecha;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Formularios;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.TarjetaIndicador;
import com.businessanalyzer.ui.VistaModulo;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Módulo "Gestión financiera": ingresos y gastos no comerciales, fijos y variables. */
public class FinanzasController implements VistaModulo {

    private static final String TODOS = "Todos";

    private final AppContext contexto;
    private final ObservableList<MovimientoFinanciero> movimientos = FXCollections.observableArrayList();
    private final FilteredList<MovimientoFinanciero> filtrados = new FilteredList<>(movimientos);
    private Moneda moneda = Moneda.PEN;

    private final TarjetaIndicador kpiGastos = new TarjetaIndicador("Gastos del período", "fth-credit-card");
    private final TarjetaIndicador kpiFijos = new TarjetaIndicador("Costos fijos", "fth-lock");
    private final TarjetaIndicador kpiVariables = new TarjetaIndicador("Gastos variables", "fth-activity");
    private final TarjetaIndicador kpiIngresos = new TarjetaIndicador("Otros ingresos", "fth-plus-circle");

    @FXML private HBox indicadores;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ComboBox<String> cmbTipo;
    @FXML private TextField txtBuscar;
    @FXML private CheckBox chkAnulados;
    @FXML private Button btnCopiar;
    @FXML private Button btnEditar;
    @FXML private Button btnAnular;
    @FXML private Button btnNuevoIngreso;
    @FXML private Button btnNuevoGasto;
    @FXML private TableView<MovimientoFinanciero> tabla;
    @FXML private TableColumn<MovimientoFinanciero, LocalDate> colFecha;
    @FXML private TableColumn<MovimientoFinanciero, String> colCategoria;
    @FXML private TableColumn<MovimientoFinanciero, String> colDescripcion;
    @FXML private TableColumn<MovimientoFinanciero, String> colComportamiento;
    @FXML private TableColumn<MovimientoFinanciero, MovimientoFinanciero> colMonto;
    @FXML private VBox desglose;

    public FinanzasController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        indicadores.getChildren().addAll(kpiGastos, kpiFijos, kpiVariables, kpiIngresos);

        CamposFecha.configurar(dpDesde);
        CamposFecha.configurar(dpHasta);
        LocalDate hoy = LocalDate.now();
        dpDesde.setValue(hoy.withDayOfMonth(1));
        dpHasta.setValue(YearMonth.from(hoy).atEndOfMonth());
        dpDesde.valueProperty().addListener((o, a, n) -> cargar());
        dpHasta.valueProperty().addListener((o, a, n) -> cargar());

        cmbTipo.getItems().setAll(TODOS, TipoMovimiento.GASTO.plural(), TipoMovimiento.INGRESO.plural());
        cmbTipo.setValue(TODOS);
        cmbTipo.valueProperty().addListener((o, a, n) -> filtrar());
        txtBuscar.textProperty().addListener((o, a, n) -> filtrar());

        configurarTabla();
        boolean gestiona = contexto.sesion().tienePermiso(Permiso.FINANZAS_GESTIONAR);
        for (Node nodo : List.of(btnCopiar, btnEditar, btnAnular, btnNuevoIngreso, btnNuevoGasto)) {
            nodo.setVisible(gestiona);
            nodo.setManaged(gestiona);
        }
    }

    @Override
    public void alMostrar() {
        cargar();
    }

    // ------------------------------------------------------------------ carga

    @FXML
    private void esteMes() {
        LocalDate hoy = LocalDate.now();
        dpHasta.setValue(YearMonth.from(hoy).atEndOfMonth());
        dpDesde.setValue(hoy.withDayOfMonth(1));
    }

    @FXML
    private void cargar() {
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        for (Button boton : List.of(btnCopiar, btnNuevoIngreso, btnNuevoGasto)) {
            boton.setDisable(empresa == null);
        }
        if (empresa == null) {
            movimientos.clear();
            tabla.setPlaceholder(new Label("Selecciona una empresa en la barra superior para ver sus finanzas."));
            mostrarResumen(ResumenFinanciero.de(List.of()));
            return;
        }
        moneda = empresa.moneda();
        tabla.setPlaceholder(new Label("No hay ingresos ni gastos en el período elegido."));
        if (dpDesde.getValue() == null || dpHasta.getValue() == null) {
            return;
        }
        try {
            movimientos.setAll(servicio().listar(dpDesde.getValue(), dpHasta.getValue(), chkAnulados.isSelected()));
        } catch (RuntimeException e) {
            movimientos.clear();
            Notificador.error(Errores.mensaje(e));
        }
        mostrarResumen(ResumenFinanciero.de(movimientos));
        filtrar();
        actualizarAcciones();
    }

    private void mostrarResumen(ResumenFinanciero r) {
        kpiGastos.actualizar(Montos.formatear(r.gastos(), moneda),
                r.cantidadMovimientos() + (r.cantidadMovimientos() == 1 ? " movimiento" : " movimientos"));
        kpiFijos.actualizar(Montos.formatear(r.gastosFijos(), moneda),
                Montos.porcentaje(r.participacionFijos()) + " del total de gastos");
        kpiVariables.actualizar(Montos.formatear(r.gastosVariables(), moneda), "Sin costo de mercadería");
        kpiIngresos.actualizar(Montos.formatear(r.ingresos(), moneda), "No provienen de ventas");
        construirDesglose(r);
    }

    /** Barras con el peso de cada categoría sobre el total de gastos. */
    private void construirDesglose(ResumenFinanciero r) {
        desglose.getChildren().clear();
        List<Map.Entry<CategoriaFinanciera, BigDecimal>> gastos = r.porCategoria().entrySet().stream()
                .filter(e -> e.getKey().tipo() == TipoMovimiento.GASTO)
                .sorted(Map.Entry.<CategoriaFinanciera, BigDecimal>comparingByValue().reversed())
                .toList();
        if (gastos.isEmpty()) {
            Label vacio = new Label("Sin gastos en el período.");
            vacio.getStyleClass().add("muted");
            desglose.getChildren().add(vacio);
            return;
        }
        for (Map.Entry<CategoriaFinanciera, BigDecimal> e : gastos) {
            BigDecimal participacion = e.getValue().divide(r.gastos(), Margenes.ESCALA_RATIO, RoundingMode.HALF_UP);
            Label nombre = new Label(e.getKey().descripcion());
            nombre.getStyleClass().add("info-value");
            nombre.setWrapText(true);
            Label monto = new Label(Montos.formatear(e.getValue(), moneda));
            monto.getStyleClass().add("info-value");
            monto.setMinWidth(Region.USE_PREF_SIZE);
            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);
            HBox cabecera = new HBox(8, nombre, espacio, monto);

            ProgressBar barra = new ProgressBar(participacion.doubleValue());
            barra.setMaxWidth(Double.MAX_VALUE);
            barra.getStyleClass().add("barra-categoria");
            Label porcentaje = new Label(Montos.porcentaje(participacion) + " de los gastos · "
                    + e.getKey().grupo().descripcion().toLowerCase());
            porcentaje.getStyleClass().add("hint");
            desglose.getChildren().add(new VBox(4, cabecera, barra, porcentaje));
        }
    }

    private void filtrar() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().strip().toLowerCase(Locale.ROOT);
        String tipo = cmbTipo.getValue();
        filtrados.setPredicate(m -> {
            boolean coincideTipo = tipo == null || TODOS.equals(tipo) || m.tipo().plural().equals(tipo);
            boolean coincideTexto = texto.isEmpty()
                    || m.descripcion().toLowerCase(Locale.ROOT).contains(texto)
                    || m.categoria().descripcion().toLowerCase(Locale.ROOT).contains(texto)
                    || (m.comprobante() != null && m.comprobante().toLowerCase(Locale.ROOT).contains(texto));
            return coincideTipo && coincideTexto;
        });
    }

    // ------------------------------------------------------------------ acciones

    @FXML
    private void nuevoGasto() {
        nuevo(TipoMovimiento.GASTO);
    }

    @FXML
    private void nuevoIngreso() {
        nuevo(TipoMovimiento.INGRESO);
    }

    private void nuevo(TipoMovimiento tipo) {
        FormularioMovimiento formulario = new FormularioMovimiento(tipo, null);
        MovimientoFinanciero[] creado = new MovimientoFinanciero[1];
        boolean ok = Dialogos.formulario(ventana(), "Nuevo " + tipo.descripcion().toLowerCase(),
                "Monto sin impuestos.", formulario.contenido(), "Registrar",
                () -> creado[0] = servicio().registrar(formulario.datos()));
        if (ok) {
            cargar();
            seleccionar(creado[0].id());
            Notificador.exito(tipo.descripcion() + " registrado: " + Montos.formatear(creado[0].monto(), moneda) + ".");
        }
    }

    @FXML
    private void editar() {
        MovimientoFinanciero m = seleccionado();
        if (m == null || m.anulado() || !contexto.sesion().tienePermiso(Permiso.FINANZAS_GESTIONAR)) {
            return;
        }
        FormularioMovimiento formulario = new FormularioMovimiento(m.tipo(), m);
        boolean ok = Dialogos.formulario(ventana(), "Editar " + m.tipo().descripcion().toLowerCase(), null,
                formulario.contenido(), "Guardar cambios", () -> servicio().actualizar(m.id(), formulario.datos()));
        if (ok) {
            cargar();
            Notificador.exito("Movimiento actualizado.");
        }
    }

    @FXML
    private void anular() {
        MovimientoFinanciero m = seleccionado();
        if (m == null || m.anulado()) {
            return;
        }
        TextField motivo = new TextField();
        motivo.setPromptText("Ej.: registrado dos veces");
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Motivo *", motivo);
        boolean ok = Dialogos.formulario(ventana(), "Anular movimiento",
                "\"" + m.descripcion() + "\" (" + Montos.formatear(m.monto(), moneda) + ") dejará de contar en los "
                        + "totales y análisis, pero se conservará como anulado.",
                grilla, "Anular", () -> servicio().anular(m.id(), motivo.getText()));
        if (ok) {
            cargar();
            Notificador.exito("Movimiento anulado.");
        }
    }

    @FXML
    private void copiarFijos() {
        YearMonth destino = YearMonth.now();
        YearMonth origen = destino.minusMonths(1);
        String nombreOrigen = Fechas.nombreMes(origen.getMonthValue()).toLowerCase() + " " + origen.getYear();
        String nombreDestino = Fechas.nombreMes(destino.getMonthValue()).toLowerCase() + " " + destino.getYear();
        if (!Dialogos.confirmar(ventana(), "Copiar gastos fijos",
                "Se copiarán a " + nombreDestino + " los gastos fijos registrados en " + nombreOrigen
                        + " (alquiler, sueldos, servicios...). Los que ya existan con la misma categoría, "
                        + "descripción y monto no se duplicarán. Luego puedes editar los montos que cambien.")) {
            return;
        }
        try {
            int copiados = servicio().copiarGastosFijos(origen, destino);
            esteMes();
            cargar();
            if (copiados == 0) {
                Notificador.info("No había gastos fijos nuevos que copiar desde " + nombreOrigen + ".");
            } else {
                Notificador.exito(copiados + (copiados == 1 ? " gasto fijo copiado" : " gastos fijos copiados")
                        + " a " + nombreDestino + ".");
            }
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    // ------------------------------------------------------------------ tabla

    private void configurarTabla() {
        colFecha.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().fecha()));
        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                setText(vacio || fecha == null ? null : Fechas.paraMostrar(fecha));
            }
        });
        colCategoria.setCellValueFactory(c -> texto(c.getValue().categoria().descripcion()));
        colDescripcion.setCellValueFactory(c -> texto(c.getValue().descripcion()
                + (c.getValue().anulado() ? "  (anulado)" : "")));
        colComportamiento.setCellValueFactory(c -> texto(c.getValue().comportamiento() == null
                ? "Ingreso" : c.getValue().comportamiento().descripcion()));

        colMonto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colMonto.setComparator(Comparator.comparing(MovimientoFinanciero::monto));
        colMonto.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(MovimientoFinanciero m, boolean vacio) {
                super.updateItem(m, vacio);
                getStyleClass().remove("margen-ok");
                if (vacio || m == null) {
                    setText(null);
                    return;
                }
                boolean ingreso = m.tipo() == TipoMovimiento.INGRESO;
                setText((ingreso ? "+" : "") + Montos.formatear(m.monto(), moneda));
                if (ingreso) {
                    getStyleClass().add("margen-ok");
                }
            }
        });

        SortedList<MovimientoFinanciero> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(ordenados);
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> actualizarAcciones());
        tabla.setRowFactory(t -> {
            TableRow<MovimientoFinanciero> fila = new TableRow<>() {
                @Override
                protected void updateItem(MovimientoFinanciero m, boolean vacio) {
                    super.updateItem(m, vacio);
                    boolean anulado = !vacio && m != null && m.anulado();
                    pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("archivado"), anulado);
                    setTooltip(anulado ? new Tooltip("Anulado por " + m.anuladoPor() + ": " + m.motivoAnulacion()) : null);
                }
            };
            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()) {
                    editar();
                }
            });
            return fila;
        });
        actualizarAcciones();
    }

    private void actualizarAcciones() {
        MovimientoFinanciero m = seleccionado();
        boolean deshabilitar = m == null || m.anulado();
        btnEditar.setDisable(deshabilitar);
        btnAnular.setDisable(deshabilitar);
    }

    // ------------------------------------------------------------------ utilidades

    private void seleccionar(long id) {
        tabla.getItems().stream().filter(m -> m.id() == id).findFirst().ifPresent(m -> {
            tabla.getSelectionModel().select(m);
            tabla.scrollTo(m);
        });
    }

    private MovimientoFinanciero seleccionado() {
        return tabla.getSelectionModel().getSelectedItem();
    }

    private FinanzasService servicio() {
        return contexto.finanzas();
    }

    private Window ventana() {
        return tabla.getScene().getWindow();
    }

    private static ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(valor);
    }

    /** Formulario de ingreso o gasto. En gastos sugiere el tipo de costo según la categoría. */
    private final class FormularioMovimiento {
        private final TipoMovimiento tipo;
        private final ComboBox<CategoriaFinanciera> cmbCategoria;
        private final ComboBox<Comportamiento> cmbComportamiento =
                new ComboBox<>(FXCollections.observableArrayList(Comportamiento.values()));
        private final Label lblComportamiento = new Label();
        private final DatePicker dpFecha = CamposFecha.nuevo(LocalDate.now());
        private final TextField txtDescripcion = new TextField();
        private final TextField txtMonto = new TextField();
        private final TextField txtComprobante = new TextField();

        FormularioMovimiento(TipoMovimiento tipo, MovimientoFinanciero m) {
            this.tipo = tipo;
            cmbCategoria = new ComboBox<>(FXCollections.observableArrayList(CategoriaFinanciera.de(tipo)));
            cmbCategoria.setPromptText("Selecciona la categoría");
            cmbCategoria.setVisibleRowCount(12);
            txtDescripcion.setPromptText(tipo == TipoMovimiento.GASTO ? "Ej.: Alquiler de noviembre" : "Ej.: Intereses de la cuenta");
            txtMonto.setPromptText("0.00");
            txtComprobante.setPromptText("Opcional");
            lblComportamiento.getStyleClass().add("hint");
            lblComportamiento.setWrapText(true);
            lblComportamiento.setMinHeight(Region.USE_PREF_SIZE);

            cmbCategoria.valueProperty().addListener((o, anterior, categoria) -> {
                if (categoria != null && categoria.comportamientoSugerido() != null) {
                    cmbComportamiento.setValue(categoria.comportamientoSugerido());
                }
            });
            cmbComportamiento.valueProperty().addListener((o, a, c) ->
                    lblComportamiento.setText(c == null ? "" : c.explicacion()));

            if (m != null) {
                cmbCategoria.setValue(m.categoria());
                cmbComportamiento.setValue(m.comportamiento());
                dpFecha.setValue(m.fecha());
                txtDescripcion.setText(m.descripcion());
                txtMonto.setText(Montos.paraEditar(m.monto()));
                txtComprobante.setText(Objects.requireNonNullElse(m.comprobante(), ""));
            }
        }

        GridPane contenido() {
            GridPane grilla = Formularios.grilla();
            Formularios.fila(grilla, "Categoría *", cmbCategoria);
            if (tipo == TipoMovimiento.GASTO) {
                Formularios.fila(grilla, "Tipo de costo *", cmbComportamiento);
                Formularios.fila(grilla, "", lblComportamiento);
            }
            Formularios.fila(grilla, "Fecha *", dpFecha);
            Formularios.fila(grilla, "Descripción *", txtDescripcion);
            Formularios.fila(grilla, "Monto (" + moneda.simbolo() + ") *", txtMonto);
            Formularios.fila(grilla, "Comprobante", txtComprobante);
            return grilla;
        }

        DatosMovimiento datos() {
            LocalDate fecha = CamposFecha.leer(dpFecha, "el movimiento");
            return new DatosMovimiento(tipo, cmbCategoria.getValue(),
                    tipo == TipoMovimiento.GASTO ? cmbComportamiento.getValue() : null,
                    fecha, txtDescripcion.getText(), Montos.leer(txtMonto.getText(), "Monto"),
                    txtComprobante.getText());
        }
    }
}
