package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.CalculadoraOperacion;
import com.businessanalyzer.analytics.ResumenOperaciones;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.exception.StockInsuficienteException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CondicionPago;
import com.businessanalyzer.model.DatosOperacion;
import com.businessanalyzer.model.DatosOperacion.DatosLinea;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.LineaOperacion;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Operacion;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.TipoItem;
import com.businessanalyzer.model.TipoOperacion;
import com.businessanalyzer.service.OperacionService;
import com.businessanalyzer.ui.CamposFecha;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Formularios;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.SelectorCuenta;
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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/** Módulo "Ventas y compras": registro, consulta y anulación de operaciones comerciales. */
public class OperacionesController implements VistaModulo {

    private final AppContext contexto;
    private final ObservableList<Operacion> operaciones = FXCollections.observableArrayList();
    private final FilteredList<Operacion> filtradas = new FilteredList<>(operaciones);
    private final ToggleGroup grupoTipo = new ToggleGroup();
    private Moneda moneda = Moneda.PEN;

    private final TarjetaIndicador kpiVentas = new TarjetaIndicador("Ventas netas", "fth-trending-up");
    private final TarjetaIndicador kpiMargen = new TarjetaIndicador("Margen bruto", "fth-percent");
    private final TarjetaIndicador kpiCompras = new TarjetaIndicador("Compras netas", "fth-truck");
    private final TarjetaIndicador kpiDiferencia = new TarjetaIndicador("Ventas − compras", "fth-repeat");

    @FXML private HBox indicadores;
    @FXML private ToggleButton tglVentas;
    @FXML private ToggleButton tglCompras;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private TextField txtBuscar;
    @FXML private CheckBox chkAnuladas;
    @FXML private TableView<Operacion> tabla;
    @FXML private TableColumn<Operacion, String> colNumero;
    @FXML private TableColumn<Operacion, LocalDate> colFecha;
    @FXML private TableColumn<Operacion, String> colContraparte;
    @FXML private TableColumn<Operacion, String> colComprobante;
    @FXML private TableColumn<Operacion, Number> colItems;
    @FXML private TableColumn<Operacion, BigDecimal> colSubtotal;
    @FXML private TableColumn<Operacion, BigDecimal> colImpuesto;
    @FXML private TableColumn<Operacion, BigDecimal> colTotal;
    @FXML private TableColumn<Operacion, BigDecimal> colMargen;
    @FXML private TableColumn<Operacion, Operacion> colPago;
    @FXML private TableColumn<Operacion, Operacion> colEstado;
    @FXML private Button btnDetalle;
    @FXML private Button btnAnular;
    @FXML private Button btnEditar;
    @FXML private Button btnNueva;
    @FXML private Label lblPie;

    public OperacionesController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        indicadores.getChildren().addAll(kpiVentas, kpiMargen, kpiCompras, kpiDiferencia);

        tglVentas.setToggleGroup(grupoTipo);
        tglCompras.setToggleGroup(grupoTipo);
        grupoTipo.selectedToggleProperty().addListener((o, anterior, nuevo) -> {
            if (nuevo == null) {
                anterior.setSelected(true);
            } else {
                cargar();
            }
        });

        CamposFecha.configurar(dpDesde);
        CamposFecha.configurar(dpHasta);
        LocalDate hoy = LocalDate.now();
        dpDesde.setValue(hoy.withDayOfMonth(1));
        dpHasta.setValue(hoy);
        dpDesde.valueProperty().addListener((o, a, n) -> cargar());
        dpHasta.valueProperty().addListener((o, a, n) -> cargar());
        txtBuscar.textProperty().addListener((o, a, n) -> filtrar());

        configurarTabla();
        boolean gestiona = contexto.sesion().tienePermiso(Permiso.OPERACIONES_GESTIONAR);
        for (Node nodo : List.of(btnNueva, btnEditar, btnAnular)) {
            nodo.setVisible(gestiona);
            nodo.setManaged(gestiona);
        }
    }

    @Override
    public void alMostrar() {
        cargar();
    }

    private TipoOperacion tipo() {
        return tglCompras.isSelected() ? TipoOperacion.COMPRA : TipoOperacion.VENTA;
    }

    // ------------------------------------------------------------------ carga

    @FXML
    private void esteMes() {
        LocalDate hoy = LocalDate.now();
        dpHasta.setValue(hoy);
        dpDesde.setValue(hoy.withDayOfMonth(1));
    }

    @FXML
    private void cargar() {
        TipoOperacion tipo = tipo();
        boolean esVenta = tipo == TipoOperacion.VENTA;
        btnNueva.setText("Nueva " + tipo.descripcion().toLowerCase());
        colContraparte.setText(tipo.contraparte());
        colMargen.setVisible(esVenta);

        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        btnNueva.setDisable(empresa == null);
        if (empresa == null) {
            operaciones.clear();
            tabla.setPlaceholder(new Label("Selecciona una empresa en la barra superior para ver sus operaciones."));
            actualizarIndicadores(ResumenOperaciones.VACIO);
            lblPie.setText("");
            return;
        }
        moneda = empresa.moneda();
        tabla.setPlaceholder(new Label("No hay " + tipo.plural().toLowerCase() + " en el período elegido."));

        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        if (desde == null || hasta == null) {
            return;
        }
        try {
            operaciones.setAll(servicio().listar(tipo, desde, hasta, chkAnuladas.isSelected()));
            actualizarIndicadores(servicio().resumen(desde, hasta));
        } catch (RuntimeException e) {
            operaciones.clear();
            Notificador.error(Errores.mensaje(e));
            return;
        }
        filtrar();
        actualizarAcciones();
    }

    private void actualizarIndicadores(ResumenOperaciones r) {
        kpiVentas.actualizar(Montos.formatear(r.ventasNetas(), moneda),
                plural(r.cantidadVentas(), "venta", "ventas") + " · ticket promedio "
                        + Montos.formatear(r.ticketPromedio(), moneda));
        kpiMargen.actualizar(Montos.formatear(r.margenBruto(), moneda),
                Montos.porcentaje(r.margenBrutoSobreVentas()) + " de las ventas netas");
        kpiCompras.actualizar(Montos.formatear(r.comprasNetas(), moneda),
                plural(r.cantidadCompras(), "compra", "compras") + " en el período");
        kpiDiferencia.actualizar(Montos.formatear(r.ventasNetas().subtract(r.comprasNetas()), moneda),
                "Montos sin impuestos y sin anuladas");
    }

    private void filtrar() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().strip().toLowerCase(Locale.ROOT);
        filtradas.setPredicate(o -> texto.isEmpty()
                || o.numero().toLowerCase(Locale.ROOT).contains(texto)
                || contiene(o.contraparte(), texto)
                || contiene(o.documentoContraparte(), texto)
                || contiene(o.comprobante(), texto));
        BigDecimal total = filtradas.stream().filter(o -> !o.anulada())
                .map(Operacion::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblPie.setText(filtradas.size() + " operaciones en la lista · total con impuestos (sin anuladas): "
                + Montos.formatear(total, moneda));
    }

    // ------------------------------------------------------------------ acciones

    @FXML
    private void nueva() {
        TipoOperacion tipo = tipo();
        FormularioOperacion formulario;
        try {
            formulario = new FormularioOperacion(tipo, servicio().productosDisponibles(), servicio().tasaImpuestoSugerida());
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }
        Operacion[] registrada = new Operacion[1];
        boolean ok = Dialogos.formulario(ventana(), "Nueva " + tipo.descripcion().toLowerCase(),
                "Precios unitarios sin impuestos. El número se asigna al registrar.",
                formulario.contenido(), "Registrar " + tipo.descripcion().toLowerCase(), 860, () -> {
                    DatosOperacion datos = formulario.datos();
                    try {
                        registrada[0] = servicio().registrar(datos, false);
                    } catch (StockInsuficienteException e) {
                        boolean confirmar = Dialogos.confirmar(formulario.ventana(), "Stock insuficiente",
                                e.getMessage() + "\n\n¿Registrar la venta de todas formas? El stock quedará en negativo.");
                        if (!confirmar) {
                            throw new ValidacionException("Venta no registrada. Ajusta las cantidades o registra "
                                    + "primero la compra de la mercadería.");
                        }
                        registrada[0] = servicio().registrar(datos, true);
                    }
                });
        if (ok) {
            cargar();
            seleccionar(registrada[0].id());
            Notificador.exito(tipo.descripcion() + " " + registrada[0].numero() + " registrada por "
                    + Montos.formatear(registrada[0].total(), moneda) + ".");
        }
    }

    /** Corrige la operación seleccionada: la original queda anulada y se registra la versión corregida. */
    @FXML
    private void editar() {
        Operacion seleccionada = seleccionada();
        if (seleccionada == null || seleccionada.anulada()) {
            return;
        }
        FormularioOperacion formulario;
        try {
            Operacion original = servicio().obtener(seleccionada.id());
            formulario = new FormularioOperacion(original.tipo(), servicio().productosDisponibles(),
                    original.tasaImpuesto());
            formulario.cargarDesde(original);
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }
        Operacion[] corregida = new Operacion[1];
        boolean ok = Dialogos.formulario(ventana(), "Editar " + seleccionada.numero(),
                "Al guardar, " + seleccionada.numero() + " queda anulada como historial y los datos corregidos se "
                        + "registran con un número nuevo. El stock y los totales se recalculan solos.",
                formulario.contenido(), "Guardar cambios", 860, () -> {
                    DatosOperacion datos = formulario.datos();
                    try {
                        corregida[0] = servicio().corregir(seleccionada.id(), datos, false);
                    } catch (StockInsuficienteException e) {
                        boolean confirmar = Dialogos.confirmar(formulario.ventana(), "Stock insuficiente",
                                e.getMessage() + "\n\n¿Guardar la venta de todas formas? El stock quedará en negativo.");
                        if (!confirmar) {
                            throw new ValidacionException("Cambios no guardados. Ajusta las cantidades.");
                        }
                        corregida[0] = servicio().corregir(seleccionada.id(), datos, true);
                    }
                });
        if (ok) {
            cargar();
            seleccionar(corregida[0].id());
            Notificador.exito(seleccionada.numero() + " corregida: ahora es " + corregida[0].numero() + ".");
        }
    }

    @FXML
    private void verDetalle() {
        Operacion seleccionada = seleccionada();
        if (seleccionada == null) {
            return;
        }
        Operacion operacion;
        try {
            operacion = servicio().obtener(seleccionada.id());
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }
        Dialogos.informacion(ventana(), operacion.tipo().descripcion() + " " + operacion.numero(), null,
                detalle(operacion));
    }

    @FXML
    private void anular() {
        Operacion operacion = seleccionada();
        if (operacion == null || operacion.anulada()) {
            return;
        }
        TextField motivo = new TextField();
        motivo.setPromptText("Ej.: devolución del cliente, error de digitación…");
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Motivo *", motivo);
        boolean ok = Dialogos.formulario(ventana(), "Anular " + operacion.numero(),
                "La operación dejará de contar en el stock y en los totales, pero se conservará como anulada. "
                        + "Esta acción no se puede deshacer.",
                grilla, "Anular operación", () -> servicio().anular(operacion.id(), motivo.getText()));
        if (ok) {
            cargar();
            Notificador.exito(operacion.numero() + " anulada.");
        }
    }

    // ------------------------------------------------------------------ tabla

    private void configurarTabla() {
        colNumero.setCellValueFactory(c -> texto(c.getValue().numero()));
        colFecha.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().fecha()));
        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                setText(vacio || fecha == null ? null : Fechas.paraMostrar(fecha));
            }
        });
        colContraparte.setCellValueFactory(c -> texto(Objects.requireNonNullElse(c.getValue().contraparte(), "")));
        colComprobante.setCellValueFactory(c -> texto(Objects.requireNonNullElse(c.getValue().comprobante(), "")));
        colItems.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().cantidadItems()));
        configurarMonto(colSubtotal, Operacion::subtotal);
        configurarMonto(colImpuesto, Operacion::impuesto);
        configurarMonto(colTotal, Operacion::total);
        configurarMonto(colMargen, Operacion::margenBruto);
        colPago.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colPago.setSortable(false);
        colPago.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Operacion o, boolean vacio) {
                super.updateItem(o, vacio);
                getStyleClass().remove("margen-negativo");
                if (vacio || o == null) {
                    setText(null);
                    return;
                }
                setText(o.anulada() ? "—" : textoPago(o).replace("Al contado · ", ""));
                if (!o.anulada() && o.vencidaEn(LocalDate.now())) {
                    getStyleClass().add("margen-negativo");
                }
            }
        });
        colEstado.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colEstado.setSortable(false);
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Operacion o, boolean vacio) {
                super.updateItem(o, vacio);
                setText(null);
                if (vacio || o == null) {
                    setGraphic(null);
                    return;
                }
                Label estado = new Label(o.estado().descripcion());
                estado.getStyleClass().addAll("badge", o.anulada() ? "badge-bloqueado" : "badge-activo");
                setGraphic(estado);
            }
        });

        SortedList<Operacion> ordenadas = new SortedList<>(filtradas);
        ordenadas.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(ordenadas);
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> actualizarAcciones());
        tabla.setRowFactory(t -> {
            TableRow<Operacion> fila = new TableRow<>() {
                @Override
                protected void updateItem(Operacion operacion, boolean vacio) {
                    super.updateItem(operacion, vacio);
                    pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("archivado"),
                            !vacio && operacion != null && operacion.anulada());
                }
            };
            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()) {
                    verDetalle();
                }
            });
            return fila;
        });
        actualizarAcciones();
    }

    private void configurarMonto(TableColumn<Operacion, BigDecimal> columna, Function<Operacion, BigDecimal> valor) {
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor.apply(c.getValue())));
        columna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal monto, boolean vacio) {
                super.updateItem(monto, vacio);
                getStyleClass().remove("margen-negativo");
                setText(vacio || monto == null ? null : Montos.formatear(monto, moneda));
                if (!vacio && monto != null && monto.signum() < 0) {
                    getStyleClass().add("margen-negativo");
                }
            }
        });
    }

    private void actualizarAcciones() {
        Operacion operacion = seleccionada();
        btnDetalle.setDisable(operacion == null);
        btnAnular.setDisable(operacion == null || operacion.anulada());
        btnEditar.setDisable(operacion == null || operacion.anulada());
    }

    // ------------------------------------------------------------------ detalle

    private Node detalle(Operacion o) {
        GridPane datos = new GridPane();
        datos.setHgap(14);
        datos.setVgap(8);
        datos.getColumnConstraints().addAll(new ColumnConstraints(110), new ColumnConstraints(200),
                new ColumnConstraints(110), new ColumnConstraints(180));
        agregarDato(datos, 0, 0, "Fecha", Fechas.paraMostrar(o.fecha()));
        agregarDato(datos, 2, 0, "Estado", o.estado().descripcion());
        agregarDato(datos, 0, 1, o.tipo().contraparte(), Objects.requireNonNullElse(o.contraparte(), "—"));
        agregarDato(datos, 2, 1, "Documento", Objects.requireNonNullElse(o.documentoContraparte(), "—"));
        agregarDato(datos, 0, 2, "Comprobante", Objects.requireNonNullElse(o.comprobante(), "—"));
        agregarDato(datos, 2, 2, "Registrado por", o.registradoPor() + " · " + Fechas.paraMostrar(o.registradoEn()));
        agregarDato(datos, 0, 3, "Pago", textoPago(o));
        agregarDato(datos, 2, 3, "Vence", o.alCredito() ? Fechas.paraMostrar(o.vencimiento()) : "—");
        int fila = 4;
        if (o.observaciones() != null) {
            agregarDato(datos, 0, fila++, "Observaciones", o.observaciones());
        }
        if (o.anulada()) {
            agregarDato(datos, 0, fila, "Anulada", o.motivoAnulacion() + " (" + o.anuladoPor() + ", "
                    + Fechas.paraMostrar(o.anuladoEn()) + ")");
        }

        TableView<LineaOperacion> lineas = new TableView<>(FXCollections.observableArrayList(o.lineas()));
        lineas.getColumns().add(columna("Código", LineaOperacion::codigo, 80, false));
        lineas.getColumns().add(columna("Producto o servicio", LineaOperacion::nombre, 220, false));
        lineas.getColumns().add(columna("Cantidad", l -> Montos.cantidad(l.cantidad()) + " " + l.unidad().abreviatura(), 90, true));
        lineas.getColumns().add(columna("Precio unit.", l -> Montos.formatear(l.precioUnitario(), moneda), 100, true));
        lineas.getColumns().add(columna("Subtotal", l -> Montos.formatear(l.subtotal(), moneda), 100, true));
        lineas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        lineas.setPrefHeight(Math.min(320, 48 + 40.0 * o.lineas().size()));

        VBox totales = bloqueTotales(o.subtotal(), o.tasaImpuesto(), o.impuesto(), o.total(),
                o.tipo() == TipoOperacion.VENTA ? o.margenBruto() : null);
        return new VBox(14, datos, lineas, totales);
    }

    /** «Al contado · Caja», «Debe S/ 118.00 (vencida)», «Al crédito · pagada»… */
    private String textoPago(Operacion o) {
        if (!o.alCredito()) {
            return o.cuenta() == null ? "Al contado" : "Al contado · " + o.cuenta();
        }
        if (o.anulada()) {
            return "Al crédito";
        }
        if (o.pendiente().signum() == 0) {
            return "Al crédito · pagada";
        }
        return (o.tipo() == TipoOperacion.VENTA ? "Por cobrar " : "Por pagar ") + Montos.formatear(o.pendiente(), moneda)
                + (o.vencidaEn(LocalDate.now()) ? " (vencida)" : "");
    }

    private static void agregarDato(GridPane grilla, int columna, int fila, String etiqueta, String valor) {
        Label clave = new Label(etiqueta);
        clave.getStyleClass().add("info-key");
        Label dato = new Label(valor);
        dato.getStyleClass().add("info-value");
        dato.setWrapText(true);
        grilla.add(clave, columna, fila);
        grilla.add(dato, columna + 1, fila);
        if (columna == 0 && (etiqueta.equals("Observaciones") || etiqueta.equals("Anulada"))) {
            GridPane.setColumnSpan(dato, 3);
        }
    }

    /** Subtotal, impuesto, total y (en ventas) margen, alineados a la derecha. */
    private VBox bloqueTotales(BigDecimal subtotal, BigDecimal tasa, BigDecimal impuesto, BigDecimal total,
                               BigDecimal margen) {
        VBox caja = new VBox(4);
        caja.setAlignment(Pos.CENTER_RIGHT);
        caja.getChildren().add(lineaTotal("Subtotal", Montos.formatear(subtotal, moneda), false));
        caja.getChildren().add(lineaTotal("Impuesto (" + (tasa == null ? "—" : tasa.stripTrailingZeros().toPlainString())
                + " %)", Montos.formatear(impuesto, moneda), false));
        caja.getChildren().add(lineaTotal("Total", Montos.formatear(total, moneda), true));
        if (margen != null) {
            HBox fila = lineaTotal("Margen bruto", Montos.formatear(margen, moneda), false);
            if (margen.signum() < 0) {
                fila.getChildren().get(1).getStyleClass().add("margen-negativo");
            }
            caja.getChildren().add(fila);
        }
        return caja;
    }

    private static HBox lineaTotal(String etiqueta, String valor, boolean destacado) {
        Label clave = new Label(etiqueta);
        clave.getStyleClass().add(destacado ? "total-clave-destacado" : "info-key");
        Label dato = new Label(valor);
        dato.getStyleClass().add(destacado ? "total-valor-destacado" : "info-value");
        dato.setMinWidth(120);
        dato.setAlignment(Pos.CENTER_RIGHT);
        HBox fila = new HBox(16, clave, dato);
        fila.setAlignment(Pos.CENTER_RIGHT);
        return fila;
    }

    // ------------------------------------------------------------------ utilidades

    private void seleccionar(long id) {
        tabla.getItems().stream().filter(o -> o.id() == id).findFirst().ifPresent(o -> {
            tabla.getSelectionModel().select(o);
            tabla.scrollTo(o);
        });
    }

    private Operacion seleccionada() {
        return tabla.getSelectionModel().getSelectedItem();
    }

    private OperacionService servicio() {
        return contexto.operaciones();
    }

    private Window ventana() {
        return tabla.getScene().getWindow();
    }

    private static boolean contiene(String valor, String buscado) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(buscado);
    }

    private static String plural(int cantidad, String singular, String plural) {
        return cantidad + " " + (cantidad == 1 ? singular : plural);
    }

    private static ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(valor);
    }

    private static <T> TableColumn<T, String> columna(String titulo, Function<T, String> valor, double ancho,
                                                      boolean numero) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new ReadOnlyStringWrapper(valor.apply(c.getValue())));
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        if (numero) {
            columna.getStyleClass().add("col-numero");
        }
        return columna;
    }

    // ------------------------------------------------------------------ formulario de registro

    /** Línea editable del formulario (aún no registrada). */
    private static final class LineaFormulario {
        private final Producto producto;
        private BigDecimal cantidad;
        private BigDecimal precio;

        LineaFormulario(Producto producto, BigDecimal cantidad, BigDecimal precio) {
            this.producto = producto;
            this.cantidad = cantidad;
            this.precio = precio;
        }
    }

    /** Formulario de venta o compra: cabecera, buscador de productos, líneas y totales en vivo. */
    private final class FormularioOperacion {
        private final TipoOperacion tipo;
        private final List<Producto> catalogo;
        private final DatePicker dpFecha = CamposFecha.nuevo(LocalDate.now());
        private final TextField txtTasa = new TextField();
        private final TextField txtContraparte = new TextField();
        private final TextField txtDocumento = new TextField();
        private final TextField txtComprobante = new TextField();
        private final TextField txtObservaciones = new TextField();
        private final ComboBox<CondicionPago> cmbCondicion =
                new ComboBox<>(FXCollections.observableArrayList(CondicionPago.values()));
        private final SelectorCuenta cmbCuenta = new SelectorCuenta(contexto.tesoreria().cuentasActivas(), null, null);
        private final DatePicker dpVencimiento = CamposFecha.nuevo(null);
        private final CheckBox chkActualizarCostos = new CheckBox("Usar estos precios como nuevo costo del catálogo");
        private final TextField txtFiltro = new TextField();
        private final ComboBox<Producto> cmbProducto = new ComboBox<>();
        private final TextField txtCantidad = new TextField("1");
        private final TextField txtPrecio = new TextField();
        private final Label lblErrorLinea = new Label();
        private final ObservableList<LineaFormulario> lineas = FXCollections.observableArrayList();
        private final TableView<LineaFormulario> tablaLineas = new TableView<>(lineas);
        private final VBox totales = new VBox();
        private VBox raiz;

        FormularioOperacion(TipoOperacion tipo, List<Producto> catalogo, BigDecimal tasaSugerida) {
            this.tipo = tipo;
            this.catalogo = catalogo;
            txtTasa.setText(tasaSugerida.stripTrailingZeros().toPlainString());
            txtContraparte.setPromptText(tipo == TipoOperacion.VENTA ? "Opcional" : "Nombre del proveedor");
            txtDocumento.setPromptText("RUC o DNI (opcional)");
            txtComprobante.setPromptText("Ej.: F001-000123");
            txtObservaciones.setPromptText("Opcional");
            dpVencimiento.setPromptText("Fecha límite (opcional)");
            // Al contado el dinero entra o sale de una cuenta; al crédito queda pendiente hasta su vencimiento.
            cmbCondicion.valueProperty().addListener((o, a, condicion) -> {
                boolean credito = condicion == CondicionPago.CREDITO;
                cmbCuenta.setDisable(credito);
                dpVencimiento.setDisable(!credito);
                if (credito && dpVencimiento.getValue() == null && dpFecha.getValue() != null) {
                    dpVencimiento.setValue(dpFecha.getValue().plusDays(30));
                }
                if (!credito) {
                    dpVencimiento.setValue(null);
                }
            });
            cmbCondicion.setValue(CondicionPago.CONTADO);
            chkActualizarCostos.setSelected(true);
            txtFiltro.setPromptText("Filtrar productos…");
            txtCantidad.setPrefWidth(80);
            txtPrecio.setPrefWidth(100);
            txtPrecio.setPromptText("Precio");
            lblErrorLinea.getStyleClass().add("margen-negativo");
            lblErrorLinea.setManaged(false);

            cmbProducto.setPrefWidth(330);
            cmbProducto.setPromptText(catalogo.isEmpty()
                    ? "No hay productos activos: regístralos en Productos y servicios"
                    : "Elige un producto o servicio");
            cmbProducto.setCellFactory(lista -> new CeldaProducto());
            cmbProducto.setButtonCell(new CeldaProducto());
            cmbProducto.getItems().setAll(catalogo);
            cmbProducto.valueProperty().addListener((o, a, producto) -> {
                if (producto != null) {
                    txtPrecio.setText(Montos.paraEditar(tipo == TipoOperacion.VENTA
                            ? producto.precioVenta() : producto.costoUnitario()));
                }
            });
            txtFiltro.textProperty().addListener((o, a, texto) -> filtrarCatalogo(texto));

            configurarTablaLineas();
            txtTasa.textProperty().addListener((o, a, n) -> recalcular());
            recalcular();
        }

        Window ventana() {
            return raiz.getScene().getWindow();
        }

        /** Rellena el formulario con una operación ya registrada (para corregirla). */
        void cargarDesde(Operacion o) {
            dpFecha.setValue(o.fecha());
            txtContraparte.setText(o.contraparte() == null ? "" : o.contraparte());
            txtDocumento.setText(o.documentoContraparte() == null ? "" : o.documentoContraparte());
            txtComprobante.setText(o.comprobante() == null ? "" : o.comprobante());
            txtObservaciones.setText(o.observaciones() == null ? "" : o.observaciones());
            cmbCondicion.setValue(o.condicion());
            if (o.cuentaId() != null) {
                cmbCuenta.getItems().stream().filter(x -> o.cuentaId().equals(x.id())).findFirst()
                        .ifPresent(cmbCuenta::setValue);
            }
            dpVencimiento.setValue(o.vencimiento());
            chkActualizarCostos.setSelected(false);
            for (LineaOperacion linea : o.lineas()) {
                Producto producto = catalogo.stream().filter(p -> p.id() == linea.productoId()).findFirst()
                        .orElseThrow(() -> new ValidacionException("\"" + linea.nombre() + "\" está archivado. "
                                + "Reactívalo en Productos y servicios para poder editar " + o.numero() + "."));
                lineas.add(new LineaFormulario(producto, linea.cantidad(), linea.precioUnitario()));
            }
            recalcular();
        }

        VBox contenido() {
            GridPane cabecera = new GridPane();
            cabecera.setHgap(12);
            cabecera.setVgap(10);
            ColumnConstraints etiqueta = new ColumnConstraints(110);
            ColumnConstraints campo = new ColumnConstraints();
            campo.setHgrow(Priority.ALWAYS);
            cabecera.getColumnConstraints().addAll(etiqueta, campo, new ColumnConstraints(110), campo);
            campoCabecera(cabecera, 0, 0, "Fecha *", dpFecha);
            campoCabecera(cabecera, 2, 0, "Impuesto (%)", txtTasa);
            campoCabecera(cabecera, 0, 1, tipo.contraparte(), txtContraparte);
            campoCabecera(cabecera, 2, 1, "Documento", txtDocumento);
            campoCabecera(cabecera, 0, 2, "Comprobante", txtComprobante);
            campoCabecera(cabecera, 2, 2, "Observaciones", txtObservaciones);
            campoCabecera(cabecera, 0, 3, "Pago *", cmbCondicion);
            campoCabecera(cabecera, 2, 3, tipo == TipoOperacion.VENTA ? "Entra a" : "Sale de", cmbCuenta);
            campoCabecera(cabecera, 0, 4, "Vence", dpVencimiento);
            Label ayudaPago = new Label(tipo == TipoOperacion.VENTA
                    ? "Al crédito, la venta queda por cobrar en «Tesorería» hasta que registres los cobros."
                    : "Al crédito, la compra queda por pagar en «Tesorería» hasta que registres los pagos.");
            ayudaPago.getStyleClass().add("hint");
            ayudaPago.setWrapText(true);
            cabecera.add(ayudaPago, 2, 4, 2, 1);

            Button agregar = new Button("Agregar", new org.kordamp.ikonli.javafx.FontIcon("fth-plus"));
            agregar.getStyleClass().add("button-secondary");
            agregar.setMinWidth(Region.USE_PREF_SIZE);
            agregar.setOnAction(e -> agregarLinea());
            txtPrecio.setOnAction(e -> agregarLinea());
            txtCantidad.setOnAction(e -> agregarLinea());
            HBox.setHgrow(cmbProducto, Priority.ALWAYS);
            txtFiltro.setPrefWidth(150);
            HBox selector = new HBox(8, txtFiltro, cmbProducto, etiquetaCorta("Cant."), txtCantidad,
                    etiquetaCorta("Precio"), txtPrecio, agregar);
            selector.setAlignment(Pos.CENTER_LEFT);

            Button quitar = new Button("Quitar línea", new org.kordamp.ikonli.javafx.FontIcon("fth-trash-2"));
            quitar.getStyleClass().add("button-secondary");
            quitar.disableProperty().bind(tablaLineas.getSelectionModel().selectedItemProperty().isNull());
            quitar.setOnAction(e -> {
                lineas.remove(tablaLineas.getSelectionModel().getSelectedItem());
                recalcular();
            });
            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);
            HBox pie = new HBox(12, quitar, espacio, totales);
            pie.setAlignment(Pos.TOP_LEFT);

            raiz = new VBox(12, cabecera, separador("Detalle"), selector, lblErrorLinea, tablaLineas, pie);
            if (tipo == TipoOperacion.COMPRA) {
                raiz.getChildren().add(chkActualizarCostos);
            }
            return raiz;
        }

        private void configurarTablaLineas() {
            tablaLineas.getColumns().add(columna("Código", (LineaFormulario l) -> l.producto.codigo(), 80, false));
            tablaLineas.getColumns().add(columna("Producto o servicio", (LineaFormulario l) -> l.producto.nombre(), 250, false));
            tablaLineas.getColumns().add(columna("Cantidad", (LineaFormulario l) ->
                    Montos.cantidad(l.cantidad) + " " + l.producto.unidad().abreviatura(), 90, true));
            if (tipo == TipoOperacion.VENTA) {
                tablaLineas.getColumns().add(columna("Stock", (LineaFormulario l) ->
                        l.producto.stock() == null ? "—" : Montos.cantidad(l.producto.stock()), 70, true));
            }
            tablaLineas.getColumns().add(columna("Precio unit.", (LineaFormulario l) -> Montos.formatear(l.precio, moneda), 100, true));
            tablaLineas.getColumns().add(columna("Subtotal", (LineaFormulario l) ->
                    Montos.formatear(CalculadoraOperacion.subtotalLinea(l.cantidad, l.precio), moneda), 100, true));
            tablaLineas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            tablaLineas.setPrefHeight(220);
            tablaLineas.setPlaceholder(new Label("Agrega productos o servicios con el selector de arriba."));
        }

        private void filtrarCatalogo(String texto) {
            String buscado = texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
            List<Producto> coincidentes = catalogo.stream()
                    .filter(p -> buscado.isEmpty()
                            || p.codigo().toLowerCase(Locale.ROOT).contains(buscado)
                            || p.nombre().toLowerCase(Locale.ROOT).contains(buscado))
                    .toList();
            cmbProducto.getItems().setAll(coincidentes);
            if (coincidentes.size() == 1) {
                cmbProducto.setValue(coincidentes.get(0));
            }
        }

        private void agregarLinea() {
            mostrarErrorLinea(null);
            Producto producto = cmbProducto.getValue();
            if (producto == null) {
                mostrarErrorLinea("Elige un producto o servicio.");
                return;
            }
            BigDecimal cantidad;
            BigDecimal precio;
            try {
                cantidad = Montos.leer(txtCantidad.getText(), "Cantidad");
                precio = Montos.leer(txtPrecio.getText(), "Precio");
            } catch (ValidacionException e) {
                mostrarErrorLinea(e.getMessage());
                return;
            }
            if (cantidad.signum() <= 0 || precio.signum() < 0) {
                mostrarErrorLinea("La cantidad debe ser mayor que cero y el precio no puede ser negativo.");
                return;
            }
            LineaFormulario existente = lineas.stream().filter(l -> l.producto.id() == producto.id()).findFirst().orElse(null);
            if (existente != null) {
                existente.cantidad = existente.cantidad.add(cantidad);
                existente.precio = precio;
                tablaLineas.refresh();
            } else {
                lineas.add(new LineaFormulario(producto, cantidad, precio));
            }
            txtCantidad.setText("1");
            cmbProducto.setValue(null);
            txtPrecio.clear();
            txtFiltro.clear();
            txtFiltro.requestFocus();
            recalcular();
        }

        private void recalcular() {
            BigDecimal tasa;
            try {
                tasa = Montos.leer(txtTasa.getText(), "Impuesto");
            } catch (ValidacionException e) {
                tasa = BigDecimal.ZERO;
            }
            List<CalculadoraOperacion.Linea> calculo = lineas.stream()
                    .map(l -> new CalculadoraOperacion.Linea(l.cantidad, l.precio,
                            tipo == TipoOperacion.VENTA ? l.producto.costoUnitario() : l.precio))
                    .toList();
            CalculadoraOperacion.Totales t = CalculadoraOperacion.calcular(calculo, tasa);
            VBox nuevo = bloqueTotales(t.subtotal(), tasa, t.impuesto(), t.total(),
                    tipo == TipoOperacion.VENTA ? t.margenBruto() : null);
            totales.getChildren().setAll(nuevo.getChildren());
            totales.setSpacing(nuevo.getSpacing());
        }

        DatosOperacion datos() {
            LocalDate fecha = CamposFecha.leer(dpFecha, "la operación");
            BigDecimal tasa = txtTasa.getText() == null || txtTasa.getText().isBlank()
                    ? BigDecimal.ZERO : Montos.leer(txtTasa.getText(), "Impuesto (%)");
            List<DatosLinea> datosLineas = new ArrayList<>();
            for (LineaFormulario l : lineas) {
                datosLineas.add(new DatosLinea(l.producto.id(), l.cantidad, l.precio));
            }
            CondicionPago condicion = cmbCondicion.getValue();
            return new DatosOperacion(tipo, fecha, txtContraparte.getText(), txtDocumento.getText(),
                    txtComprobante.getText(), tasa, txtObservaciones.getText(), datosLineas,
                    tipo == TipoOperacion.COMPRA && chkActualizarCostos.isSelected(), condicion,
                    condicion == CondicionPago.CONTADO ? cmbCuenta.cuentaId() : null,
                    CamposFecha.leer(dpVencimiento, "vencimiento"));
        }

        private void mostrarErrorLinea(String mensaje) {
            lblErrorLinea.setText(mensaje == null ? "" : mensaje);
            lblErrorLinea.setManaged(mensaje != null);
            lblErrorLinea.setVisible(mensaje != null);
        }

        private void campoCabecera(GridPane grilla, int columna, int fila, String etiqueta, Region campo) {
            Label label = new Label(etiqueta);
            label.getStyleClass().add("form-label");
            campo.setMaxWidth(Double.MAX_VALUE);
            grilla.add(label, columna, fila);
            grilla.add(campo, columna + 1, fila);
        }

        private Label etiquetaCorta(String texto) {
            Label label = new Label(texto);
            label.getStyleClass().add("form-label");
            label.setMinWidth(Region.USE_PREF_SIZE);
            return label;
        }

        private Node separador(String titulo) {
            Label label = new Label(titulo);
            label.getStyleClass().add("card-title");
            return label;
        }
    }

    /** Muestra código, nombre, precio de referencia y (en productos) el stock disponible. */
    private final class CeldaProducto extends ListCell<Producto> {
        @Override
        protected void updateItem(Producto p, boolean vacio) {
            super.updateItem(p, vacio);
            if (vacio || p == null) {
                setText(null);
                return;
            }
            String stock = p.tipo() == TipoItem.PRODUCTO ? " · stock " + Montos.cantidad(p.stock()) : "";
            setText(p.codigo() + " — " + p.nombre() + stock);
        }
    }
}
