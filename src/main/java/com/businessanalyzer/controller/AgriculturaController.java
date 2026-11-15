package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.agricultura.ResultadoCampania;
import com.businessanalyzer.analytics.diagnostico.Semaforo;
import com.businessanalyzer.analytics.ganaderia.IndicadorGanadero;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.Campania;
import com.businessanalyzer.model.Cosecha;
import com.businessanalyzer.model.CostoCampania;
import com.businessanalyzer.model.Cultivo;
import com.businessanalyzer.model.DatosCampania;
import com.businessanalyzer.model.DatosParcela;
import com.businessanalyzer.model.DestinoCosecha;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.EstadoCampania;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Parcela;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.RubroCosto;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.Tenencia;
import com.businessanalyzer.service.AgriculturaService;
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
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Módulo "Agricultura": parcelas, campañas, costos, cosechas y resultado de cada campaña. */
public class AgriculturaController implements VistaModulo {

    private static final String TODAS = "Todas";
    private static final String EN_CURSO = "En curso";
    private static final String CERRADAS = "Cerradas";
    private static final PseudoClass ARCHIVADO = PseudoClass.getPseudoClass("archivado");

    private final AppContext contexto;
    private Moneda moneda = Moneda.PEN;

    private final ObservableList<ResultadoCampania> campanias = FXCollections.observableArrayList();
    private final FilteredList<ResultadoCampania> filtradas = new FilteredList<>(campanias);

    private final TarjetaIndicador kpiArea = new TarjetaIndicador("Área sembrada", "fth-map");
    private final TarjetaIndicador kpiInvertido = new TarjetaIndicador("Invertido en curso", "fth-trending-down");
    private final TarjetaIndicador kpiCosechado = new TarjetaIndicador("Cosechado", "fth-package");
    private final TarjetaIndicador kpiMargen = new TarjetaIndicador("Margen de campañas cerradas", "fth-dollar-sign");

    @FXML private HBox indicadores;
    @FXML private TabPane pestanas;
    @FXML private ComboBox<String> cmbFiltro;
    @FXML private CheckBox chkAnuladas;
    @FXML private Button btnCosto;
    @FXML private Button btnCosecha;
    @FXML private Button btnCerrar;
    @FXML private Button btnRegistros;
    @FXML private MenuButton menuMas;
    @FXML private MenuItem itemEditar;
    @FXML private MenuItem itemReabrir;
    @FXML private MenuItem itemAnular;
    @FXML private Button btnNuevaCampania;
    @FXML private TableView<ResultadoCampania> tablaCampanias;
    @FXML private TableColumn<ResultadoCampania, String> colCamCultivo;
    @FXML private TableColumn<ResultadoCampania, LocalDate> colCamSiembra;
    @FXML private TableColumn<ResultadoCampania, String> colCamArea;
    @FXML private TableColumn<ResultadoCampania, String> colCamEstado;
    @FXML private TableColumn<ResultadoCampania, BigDecimal> colCamCosto;
    @FXML private TableColumn<ResultadoCampania, String> colCamRendimiento;
    @FXML private TableColumn<ResultadoCampania, BigDecimal> colCamMargen;
    @FXML private Label lblDetalleTitulo;
    @FXML private VBox detalle;

    @FXML private CheckBox chkInactivas;
    @FXML private Button btnEditarParcela;
    @FXML private Button btnEstadoParcela;
    @FXML private Button btnNuevaParcela;
    @FXML private TableView<Parcela> tablaParcelas;
    @FXML private TableColumn<Parcela, String> colParNombre;
    @FXML private TableColumn<Parcela, String> colParArea;
    @FXML private TableColumn<Parcela, String> colParTenencia;
    @FXML private TableColumn<Parcela, String> colParRiego;
    @FXML private TableColumn<Parcela, String> colParUbicacion;

    public AgriculturaController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        indicadores.getChildren().addAll(kpiArea, kpiInvertido, kpiCosechado, kpiMargen);
        cmbFiltro.getItems().setAll(TODAS, EN_CURSO, CERRADAS);
        cmbFiltro.setValue(TODAS);
        cmbFiltro.valueProperty().addListener((o, a, n) -> filtrar());
        configurarTablaCampanias();
        configurarTablaParcelas();

        boolean gestiona = contexto.sesion().tienePermiso(Permiso.AGRO_GESTIONAR);
        for (Node nodo : List.of(btnCosto, btnCosecha, btnCerrar, menuMas, btnNuevaCampania, btnEditarParcela,
                btnEstadoParcela, btnNuevaParcela)) {
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
    private void cargar() {
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        boolean agro = empresa != null && empresa.sector() == Sector.AGROPECUARIO;
        btnNuevaCampania.setDisable(!agro);
        btnNuevaParcela.setDisable(!agro);
        if (!agro) {
            String mensaje = empresa == null ? "Selecciona una empresa en la barra superior."
                    : "La agricultura está disponible solo para empresas del sector agropecuario.";
            limpiar(mensaje);
            return;
        }
        moneda = empresa.moneda();
        Long seleccionada = seleccionada() == null ? null : seleccionada().campania().id();
        try {
            AgriculturaService servicio = servicio();
            tablaParcelas.getItems().setAll(servicio.listarParcelas(chkInactivas.isSelected()));
            campanias.setAll(servicio.listarCampanias(chkAnuladas.isSelected()));
        } catch (RuntimeException e) {
            limpiar(Errores.mensaje(e));
            Notificador.error(Errores.mensaje(e));
            return;
        }
        tablaParcelas.setPlaceholder(new Label("Aún no hay parcelas. Registra la primera con «Nueva parcela»."));
        tablaCampanias.setPlaceholder(new Label(tablaParcelas.getItems().isEmpty()
                ? "Primero registra tus parcelas en la pestaña «Parcelas»." : "Aún no hay campañas."));
        mostrarResumen();
        filtrar();
        if (seleccionada != null) {
            tablaCampanias.getItems().stream().filter(r -> r.campania().id() == seleccionada).findFirst()
                    .ifPresent(r -> tablaCampanias.getSelectionModel().select(r));
        }
        if (tablaCampanias.getSelectionModel().isEmpty() && !tablaCampanias.getItems().isEmpty()) {
            tablaCampanias.getSelectionModel().selectFirst();
        }
        mostrarDetalle();
        actualizarAcciones();
    }

    private void limpiar(String mensaje) {
        campanias.clear();
        tablaParcelas.getItems().clear();
        tablaCampanias.setPlaceholder(new Label(mensaje));
        tablaParcelas.setPlaceholder(new Label(mensaje));
        detalle.getChildren().clear();
        for (TarjetaIndicador kpi : List.of(kpiArea, kpiInvertido, kpiCosechado, kpiMargen)) {
            kpi.actualizar(TarjetaIndicador.SIN_VALOR, "");
        }
    }

    private void mostrarResumen() {
        List<ResultadoCampania> enCurso = campanias.stream()
                .filter(r -> r.campania().estado() == EstadoCampania.EN_CURSO).toList();
        List<ResultadoCampania> cerradas = campanias.stream().filter(r -> r.campania().estado().cerrada()).toList();
        BigDecimal area = enCurso.stream().map(r -> r.campania().area()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal invertido = enCurso.stream().map(ResultadoCampania::costoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cosechado = campanias.stream().map(ResultadoCampania::produccion).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal margen = cerradas.stream().map(ResultadoCampania::margen).reduce(BigDecimal.ZERO, BigDecimal::add);
        kpiArea.actualizar(hectareas(area), enCurso.size() + (enCurso.size() == 1 ? " campaña en curso" : " campañas en curso"));
        kpiInvertido.actualizar(Montos.formatear(invertido, moneda), "Costos de las campañas en curso");
        kpiCosechado.actualizar(Montos.cantidad(cosechado.setScale(0, RoundingMode.HALF_UP)) + " kg", "En todas las campañas");
        kpiMargen.actualizar(Montos.formatear(margen, moneda),
                cerradas.size() + (cerradas.size() == 1 ? " campaña cerrada" : " campañas cerradas"));
    }

    private void filtrar() {
        String filtro = cmbFiltro.getValue();
        filtradas.setPredicate(r -> filtro == null || TODAS.equals(filtro)
                || (EN_CURSO.equals(filtro) && r.campania().estado() == EstadoCampania.EN_CURSO)
                || (CERRADAS.equals(filtro) && r.campania().estado().cerrada()));
    }

    private void mostrarDetalle() {
        ResultadoCampania r = seleccionada();
        detalle.getChildren().clear();
        if (r == null) {
            lblDetalleTitulo.setText("Resultado de la campaña");
            Label vacio = new Label("Selecciona una campaña para ver su resultado.");
            vacio.getStyleClass().add("muted");
            vacio.setWrapText(true);
            detalle.getChildren().add(vacio);
            return;
        }
        Campania c = r.campania();
        lblDetalleTitulo.setText(c.cultivo().descripcion() + (c.variedad() == null ? "" : " (" + c.variedad() + ")"));
        Label sub = new Label(c.parcelaNombre() + " · " + hectareas(c.area()) + " · sembrada el "
                + Fechas.paraMostrar(c.fechaSiembra()) + " · " + c.estado().descripcion().toLowerCase()
                + (c.fechaCierre() == null ? "" : " el " + Fechas.paraMostrar(c.fechaCierre()))
                + (c.motivo() == null ? "" : " (" + c.motivo() + ")"));
        sub.getStyleClass().add("hint");
        sub.setWrapText(true);
        detalle.getChildren().add(sub);
        for (IndicadorGanadero ind : r.evaluar(moneda)) {
            Label nombre = new Label(ind.nombre());
            nombre.getStyleClass().add("info-key");
            nombre.setWrapText(true);
            Label valor = new Label(ind.valor());
            valor.getStyleClass().add("info-value");
            valor.setMinWidth(Region.USE_PREF_SIZE);
            valor.setStyle("-fx-font-weight: bold;");
            if (ind.semaforo().evaluado() || "—".equals(ind.valor())) {
                valor.getStyleClass().add("semaforo-" + switch (ind.semaforo()) {
                    case VERDE -> "verde";
                    case AMARILLO -> "amarillo";
                    case ROJO -> "rojo";
                    default -> "sin-datos";
                });
            }
            Region espacio = new Region();
            HBox.setHgrow(espacio, javafx.scene.layout.Priority.ALWAYS);
            HBox cabecera = new HBox(8, nombre, espacio, valor);
            Label lectura = new Label(ind.lectura());
            lectura.getStyleClass().add("muted");
            lectura.setWrapText(true);
            VBox bloque = new VBox(2, cabecera, lectura);
            if (ind.criterio() != null) {
                Tooltip ayuda = new Tooltip(ind.criterio());
                Tooltip.install(bloque, ayuda);
            }
            detalle.getChildren().add(bloque);
        }
    }

    // ------------------------------------------------------------------ campañas

    @FXML
    private void nuevaCampania() {
        List<Parcela> activas = tablaParcelas.getItems().stream().filter(Parcela::activa).toList();
        if (activas.isEmpty()) {
            Notificador.info("Primero registra una parcela en la pestaña «Parcelas».");
            pestanas.getSelectionModel().select(1);
            return;
        }
        FormularioCampania formulario = new FormularioCampania(activas, null);
        Campania[] creada = new Campania[1];
        boolean ok = Dialogos.formulario(ventana(), "Nueva campaña", null, formulario.contenido(), "Registrar", 520,
                () -> creada[0] = servicio().registrarCampania(formulario.datos()));
        if (ok) {
            cargar();
            seleccionar(creada[0].id());
            Notificador.exito("Campaña registrada. Ahora registra sus costos y, al final, la cosecha.");
        }
    }

    @FXML
    private void editarCampania() {
        ResultadoCampania r = seleccionada();
        if (r == null || r.campania().estado() == EstadoCampania.ANULADA) {
            return;
        }
        FormularioCampania formulario = new FormularioCampania(tablaParcelas.getItems(), r.campania());
        boolean ok = Dialogos.formulario(ventana(), "Editar campaña", null, formulario.contenido(), "Guardar cambios",
                520, () -> servicio().actualizarCampania(r.campania().id(), formulario.datos()));
        if (ok) {
            cargar();
            Notificador.exito("Campaña actualizada.");
        }
    }

    @FXML
    private void registrarCosto() {
        ResultadoCampania r = seleccionada();
        if (r == null) {
            return;
        }
        ComboBox<RubroCosto> rubro = new ComboBox<>(FXCollections.observableArrayList(RubroCosto.values()));
        rubro.setPromptText("Selecciona el rubro");
        rubro.setVisibleRowCount(11);
        Label ayuda = nota();
        rubro.valueProperty().addListener((o, a, n) -> ayuda.setText(n == null ? "" : n.ayuda()));
        DatePicker fecha = CamposFecha.nuevo(LocalDate.now());
        TextField descripcion = new TextField();
        descripcion.setPromptText("Ej.: 4 sacos de urea");
        TextField monto = new TextField();
        monto.setPromptText("0.00");
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Rubro *", rubro);
        Formularios.fila(grilla, "", ayuda);
        Formularios.fila(grilla, "Fecha *", fecha);
        Formularios.fila(grilla, "Descripción *", descripcion);
        Formularios.fila(grilla, "Monto (" + moneda.simbolo() + ") *", monto);
        boolean ok = Dialogos.formulario(ventana(), "Registrar costo", r.campania().nombre(), grilla, "Registrar",
                () -> servicio().registrarCosto(r.campania().id(), CamposFecha.leer(fecha, "el costo"), rubro.getValue(),
                        descripcion.getText(), Montos.leer(monto.getText(), "Monto")));
        if (ok) {
            cargar();
            Notificador.exito("Costo registrado.");
        }
    }

    @FXML
    private void registrarCosecha() {
        ResultadoCampania r = seleccionada();
        if (r == null) {
            return;
        }
        DatePicker fecha = CamposFecha.nuevo(LocalDate.now());
        TextField kilos = new TextField();
        kilos.setPromptText("Kilos");
        ComboBox<DestinoCosecha> destino = new ComboBox<>(FXCollections.observableArrayList(DestinoCosecha.values()));
        destino.setValue(DestinoCosecha.VENTA);
        TextField precio = new TextField();
        precio.setPromptText("Opcional");
        destino.valueProperty().addListener((o, a, n) -> {
            precio.setDisable(n != DestinoCosecha.VENTA);
            if (n != DestinoCosecha.VENTA) {
                precio.clear();
            }
        });
        TextField observacion = new TextField();
        observacion.setPromptText("Opcional. Ej.: primera calidad");
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Fecha *", fecha);
        Formularios.fila(grilla, "Cantidad (kg) *", kilos);
        Formularios.fila(grilla, "Destino *", destino);
        Formularios.fila(grilla, "Precio por kg (" + moneda.simbolo() + ")", precio);
        Formularios.fila(grilla, "Observación", observacion);
        Formularios.nota(grilla, "Registra por separado lo que vendes, lo que guardas para consumo o semilla y lo que "
                + "das a los animales. Una arroba son 11.5 kg; un quintal, 46 kg.");
        boolean ok = Dialogos.formulario(ventana(), "Registrar cosecha", r.campania().nombre(), grilla, "Registrar",
                () -> servicio().registrarCosecha(r.campania().id(), CamposFecha.leer(fecha, "la cosecha"),
                        Montos.leer(kilos.getText(), "Cantidad"), destino.getValue(),
                        precio.getText() == null || precio.getText().isBlank() ? null : Montos.leer(precio.getText(), "Precio"),
                        observacion.getText()));
        if (ok) {
            cargar();
            Notificador.exito("Cosecha registrada.");
        }
    }

    @FXML
    private void cerrarCampania() {
        ResultadoCampania r = seleccionada();
        if (r == null || r.campania().estado() != EstadoCampania.EN_CURSO) {
            return;
        }
        ComboBox<EstadoCampania> estado = new ComboBox<>(FXCollections.observableArrayList(
                EstadoCampania.COSECHADA, EstadoCampania.PERDIDA));
        estado.setValue(r.produccion().signum() > 0 ? EstadoCampania.COSECHADA : EstadoCampania.PERDIDA);
        DatePicker fecha = CamposFecha.nuevo(LocalDate.now());
        TextField motivo = new TextField();
        motivo.setPromptText("Ej.: helada, granizada, sequía, plaga");
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Resultado *", estado);
        Formularios.fila(grilla, "Fecha *", fecha);
        Formularios.fila(grilla, "Causa / observación", motivo);
        Formularios.nota(grilla, "Si se perdió, indica la causa. Una campaña cerrada se puede reabrir desde «Más».");
        boolean ok = Dialogos.formulario(ventana(), "Cerrar campaña", r.campania().nombre(), grilla, "Cerrar",
                () -> servicio().cerrarCampania(r.campania().id(), estado.getValue(), CamposFecha.leer(fecha, "cierre"),
                        motivo.getText()));
        if (ok) {
            cargar();
            Notificador.exito("Campaña cerrada.");
        }
    }

    @FXML
    private void reabrirCampania() {
        ResultadoCampania r = seleccionada();
        if (r == null || !r.campania().estado().cerrada()) {
            return;
        }
        try {
            servicio().reabrirCampania(r.campania().id());
            cargar();
            Notificador.exito("La campaña vuelve a estar en curso.");
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    @FXML
    private void anularCampania() {
        ResultadoCampania r = seleccionada();
        if (r == null || r.campania().estado() == EstadoCampania.ANULADA) {
            return;
        }
        TextField motivo = new TextField();
        motivo.setPromptText("Ej.: registrada por error");
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Motivo *", motivo);
        boolean ok = Dialogos.formulario(ventana(), "Anular campaña",
                r.campania().nombre() + " dejará de contar en los resultados y en el balance. Si se perdió por "
                        + "helada o granizo, no la anules: ciérrala como «Perdida».",
                grilla, "Anular", () -> servicio().anularCampania(r.campania().id(), motivo.getText()));
        if (ok) {
            cargar();
            Notificador.exito("Campaña anulada.");
        }
    }

    /** Costos y cosechas de la campaña, con la opción de anular los registrados por error. */
    @FXML
    private void verRegistros() {
        ResultadoCampania r = seleccionada();
        if (r == null) {
            return;
        }
        long id = r.campania().id();
        boolean gestiona = contexto.sesion().tienePermiso(Permiso.AGRO_GESTIONAR);
        TableView<CostoCampania> costos = new TableView<>();
        costos.setPrefHeight(200);
        costos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        costos.getColumns().add(columna("Fecha", 90, k -> Fechas.paraMostrar(k.fecha())));
        costos.getColumns().add(columna("Rubro", 150, k -> k.rubro().descripcion() + (k.anulado() ? " (anulado)" : "")));
        costos.getColumns().add(columna("Descripción", 170, CostoCampania::descripcion));
        costos.getColumns().add(columna("Monto", 100, k -> Montos.formatear(k.monto(), moneda)));
        costos.setRowFactory(t -> filaArchivada(CostoCampania::anulado));
        costos.setPlaceholder(new Label("Sin costos registrados."));

        TableView<Cosecha> cosechas = new TableView<>();
        cosechas.setPrefHeight(160);
        cosechas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cosechas.getColumns().add(columna("Fecha", 90, h -> Fechas.paraMostrar(h.fecha())));
        cosechas.getColumns().add(columna("Kilos", 90, h -> Montos.cantidad(h.cantidad())));
        cosechas.getColumns().add(columna("Destino", 170, h -> h.destino().descripcion() + (h.anulado() ? " (anulada)" : "")));
        cosechas.getColumns().add(columna("Precio/kg", 90, h -> h.precio() == null ? "" : Montos.formatear(h.precio(), moneda)));
        cosechas.setRowFactory(t -> filaArchivada(Cosecha::anulado));
        cosechas.setPlaceholder(new Label("Sin cosechas registradas."));

        boolean[] cambios = {false};
        Runnable recargar = () -> {
            costos.getItems().setAll(servicio().listarCostos(id));
            cosechas.getItems().setAll(servicio().listarCosechas(id));
        };
        Label tituloCostos = new Label("Costos");
        tituloCostos.getStyleClass().add("step-title");
        Label tituloCosechas = new Label("Cosechas");
        tituloCosechas.getStyleClass().add("step-title");
        VBox contenido = new VBox(8, tituloCostos, costos);
        if (gestiona) {
            Button anularCosto = new Button("Anular costo seleccionado");
            anularCosto.getStyleClass().add("button-secondary");
            anularCosto.disableProperty().bind(costos.getSelectionModel().selectedItemProperty().isNull());
            anularCosto.setOnAction(e -> anular(costos.getSelectionModel().getSelectedItem() == null ? null
                    : () -> servicio().anularCosto(costos.getSelectionModel().getSelectedItem().id()), recargar, cambios));
            contenido.getChildren().add(anularCosto);
        }
        contenido.getChildren().addAll(tituloCosechas, cosechas);
        if (gestiona) {
            Button anularCosecha = new Button("Anular cosecha seleccionada");
            anularCosecha.getStyleClass().add("button-secondary");
            anularCosecha.disableProperty().bind(cosechas.getSelectionModel().selectedItemProperty().isNull());
            anularCosecha.setOnAction(e -> anular(cosechas.getSelectionModel().getSelectedItem() == null ? null
                    : () -> servicio().anularCosecha(cosechas.getSelectionModel().getSelectedItem().id()), recargar, cambios));
            contenido.getChildren().add(anularCosecha);
        }
        try {
            recargar.run();
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }
        Dialogos.informacion(ventana(), "Registros de la campaña", r.campania().nombre(), contenido);
        if (cambios[0]) {
            cargar();
        }
    }

    private void anular(Runnable accion, Runnable recargar, boolean[] cambios) {
        if (accion == null || !Dialogos.confirmar(ventana(), "Anular registro",
                "El registro seleccionado dejará de contar en el resultado de la campaña.")) {
            return;
        }
        try {
            accion.run();
            cambios[0] = true;
            recargar.run();
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    // ------------------------------------------------------------------ parcelas

    @FXML
    private void nuevaParcela() {
        FormularioParcela formulario = new FormularioParcela(null);
        boolean ok = Dialogos.formulario(ventana(), "Nueva parcela", null, formulario.contenido(), "Registrar",
                () -> servicio().registrarParcela(formulario.datos()));
        if (ok) {
            cargar();
            Notificador.exito("Parcela registrada.");
        }
    }

    @FXML
    private void editarParcela() {
        Parcela p = tablaParcelas.getSelectionModel().getSelectedItem();
        if (p == null) {
            return;
        }
        FormularioParcela formulario = new FormularioParcela(p);
        boolean ok = Dialogos.formulario(ventana(), "Editar parcela", null, formulario.contenido(), "Guardar cambios",
                () -> servicio().actualizarParcela(p.id(), formulario.datos()));
        if (ok) {
            cargar();
            Notificador.exito("Parcela actualizada.");
        }
    }

    @FXML
    private void cambiarEstadoParcela() {
        Parcela p = tablaParcelas.getSelectionModel().getSelectedItem();
        if (p == null) {
            return;
        }
        try {
            servicio().cambiarParcelaActiva(p.id(), !p.activa());
            cargar();
            Notificador.exito(p.activa() ? "Parcela desactivada." : "Parcela reactivada.");
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    // ------------------------------------------------------------------ tablas

    private void configurarTablaCampanias() {
        colCamCultivo.setCellValueFactory(c -> {
            Campania k = c.getValue().campania();
            return texto(k.cultivo().descripcion() + (k.variedad() == null ? "" : " (" + k.variedad() + ")"));
        });
        colCamSiembra.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().campania().fechaSiembra()));
        colCamSiembra.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                setText(vacio ? null : Fechas.paraMostrar(fecha));
            }
        });
        colCamArea.setCellValueFactory(c -> texto(Montos.cantidad(c.getValue().campania().area())));
        colCamEstado.setCellValueFactory(c -> texto(c.getValue().campania().estado().descripcion()));
        colCamCosto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().costoTotal()));
        colCamCosto.setCellFactory(col -> celdaMonto(false));
        colCamRendimiento.setCellValueFactory(c -> {
            BigDecimal rend = c.getValue().rendimiento();
            return texto(rend == null ? "—" : Montos.cantidad(rend.setScale(0, RoundingMode.HALF_UP)));
        });
        colCamMargen.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().produccion().signum() > 0 || c.getValue().campania().estado().cerrada()
                        ? c.getValue().margen() : null));
        colCamMargen.setCellFactory(col -> celdaMonto(true));
        tablaCampanias.setItems(filtradas);
        tablaCampanias.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            mostrarDetalle();
            actualizarAcciones();
        });
        tablaCampanias.setRowFactory(t -> {
            TableRow<ResultadoCampania> fila = new TableRow<>() {
                @Override
                protected void updateItem(ResultadoCampania r, boolean vacio) {
                    super.updateItem(r, vacio);
                    pseudoClassStateChanged(ARCHIVADO, !vacio && r != null
                            && r.campania().estado() == EstadoCampania.ANULADA);
                }
            };
            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()) {
                    verRegistros();
                }
            });
            return fila;
        });
    }

    private void configurarTablaParcelas() {
        colParNombre.setCellValueFactory(c -> texto(c.getValue().nombre() + (c.getValue().activa() ? "" : "  (desactivada)")));
        colParArea.setCellValueFactory(c -> texto(hectareas(c.getValue().area())));
        colParTenencia.setCellValueFactory(c -> texto(c.getValue().tenencia().descripcion()));
        colParRiego.setCellValueFactory(c -> texto(c.getValue().riego() ? "Con riego" : "Secano"));
        colParUbicacion.setCellValueFactory(c -> texto(Objects.requireNonNullElse(c.getValue().ubicacion(), "")));
        tablaParcelas.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> actualizarAcciones());
        tablaParcelas.setRowFactory(t -> filaArchivada(p -> !p.activa()));
    }

    private void actualizarAcciones() {
        ResultadoCampania r = seleccionada();
        EstadoCampania estado = r == null ? null : r.campania().estado();
        boolean vigente = estado != null && estado != EstadoCampania.ANULADA;
        btnCosto.setDisable(!vigente);
        btnCosecha.setDisable(!vigente || estado == EstadoCampania.COSECHADA);
        btnCerrar.setDisable(estado != EstadoCampania.EN_CURSO);
        btnRegistros.setDisable(r == null);
        menuMas.setDisable(r == null);
        itemEditar.setDisable(!vigente);
        itemReabrir.setDisable(estado == null || !estado.cerrada());
        itemAnular.setDisable(!vigente);

        Parcela p = tablaParcelas.getSelectionModel().getSelectedItem();
        btnEditarParcela.setDisable(p == null);
        btnEstadoParcela.setDisable(p == null);
        btnEstadoParcela.setText(p != null && !p.activa() ? "Reactivar" : "Desactivar");
    }

    private <T> TableCell<T, BigDecimal> celdaMonto(boolean colorear) {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal monto, boolean vacio) {
                super.updateItem(monto, vacio);
                getStyleClass().removeAll("margen-ok", "margen-negativo");
                if (vacio) {
                    setText(null);
                    return;
                }
                setText(monto == null ? "—" : Montos.formatear(monto, moneda));
                if (colorear && monto != null && monto.signum() != 0) {
                    getStyleClass().add(monto.signum() > 0 ? "margen-ok" : "margen-negativo");
                }
            }
        };
    }

    private static <T> TableRow<T> filaArchivada(java.util.function.Predicate<T> apagada) {
        return new TableRow<>() {
            @Override
            protected void updateItem(T item, boolean vacio) {
                super.updateItem(item, vacio);
                pseudoClassStateChanged(ARCHIVADO, !vacio && item != null && apagada.test(item));
            }
        };
    }

    private static <T> TableColumn<T, String> columna(String titulo, double ancho, Function<T, String> valor) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setCellValueFactory(c -> new ReadOnlyStringWrapper(valor.apply(c.getValue())));
        return columna;
    }

    // ------------------------------------------------------------------ utilidades

    private ResultadoCampania seleccionada() {
        return tablaCampanias.getSelectionModel().getSelectedItem();
    }

    private void seleccionar(long campaniaId) {
        tablaCampanias.getItems().stream().filter(r -> r.campania().id() == campaniaId).findFirst()
                .ifPresent(r -> tablaCampanias.getSelectionModel().select(r));
    }

    private AgriculturaService servicio() {
        return contexto.agricultura();
    }

    private Window ventana() {
        return tablaCampanias.getScene().getWindow();
    }

    private static ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(valor);
    }

    private static String hectareas(BigDecimal ha) {
        return Montos.cantidad(ha) + " ha";
    }

    private static Label nota() {
        Label l = new Label();
        l.getStyleClass().add("hint");
        l.setWrapText(true);
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }

    private static BigDecimal opcional(String texto, String campo) {
        return texto == null || texto.isBlank() ? null : Montos.leer(texto, campo);
    }

    /** Formulario de campaña: propone el rendimiento de referencia del cultivo. */
    private final class FormularioCampania {
        private final ComboBox<Parcela> cmbParcela;
        private final ComboBox<Cultivo> cmbCultivo = new ComboBox<>(FXCollections.observableArrayList(Cultivo.values()));
        private final TextField txtVariedad = new TextField();
        private final DatePicker dpSiembra = CamposFecha.nuevo(LocalDate.now());
        private final TextField txtArea = new TextField();
        private final TextField txtReferencia = new TextField();
        private final TextField txtPrecio = new TextField();
        private final TextField txtObservacion = new TextField();

        FormularioCampania(List<Parcela> parcelas, Campania c) {
            cmbParcela = new ComboBox<>(FXCollections.observableArrayList(parcelas));
            cmbParcela.setPromptText("Selecciona la parcela");
            cmbCultivo.setPromptText("Selecciona el cultivo");
            cmbCultivo.setVisibleRowCount(15);
            txtVariedad.setPromptText("Opcional. Ej.: Yungay, Canchán");
            txtArea.setPromptText("Hectáreas");
            txtReferencia.setPromptText("kg/ha");
            txtPrecio.setPromptText("Opcional. S/ por kg");
            txtObservacion.setPromptText("Opcional");
            cmbParcela.valueProperty().addListener((o, a, p) -> {
                if (p != null && (txtArea.getText() == null || txtArea.getText().isBlank())) {
                    txtArea.setText(Montos.cantidad(p.area()));
                }
            });
            cmbCultivo.valueProperty().addListener((o, a, cultivo) -> {
                BigDecimal ref = cultivo == null ? null : cultivo.rendimientoReferencia();
                txtReferencia.setText(ref == null ? "" : ref.toPlainString());
            });
            if (c != null) {
                parcelas.stream().filter(p -> p.id() == c.parcelaId()).findFirst().ifPresent(cmbParcela::setValue);
                cmbCultivo.setValue(c.cultivo());
                txtVariedad.setText(Objects.requireNonNullElse(c.variedad(), ""));
                dpSiembra.setValue(c.fechaSiembra());
                txtArea.setText(Montos.cantidad(c.area()));
                txtReferencia.setText(c.rendimientoReferencia() == null ? ""
                        : c.rendimientoReferencia().stripTrailingZeros().toPlainString());
                txtPrecio.setText(c.precioChacra() == null ? "" : Montos.paraEditar(c.precioChacra()));
                txtObservacion.setText(Objects.requireNonNullElse(c.observacion(), ""));
            } else if (parcelas.size() == 1) {
                cmbParcela.setValue(parcelas.get(0));
            }
        }

        GridPane contenido() {
            GridPane grilla = Formularios.grilla();
            Formularios.fila(grilla, "Parcela *", cmbParcela);
            Formularios.fila(grilla, "Cultivo *", cmbCultivo);
            Formularios.fila(grilla, "Variedad", txtVariedad);
            Formularios.fila(grilla, "Fecha de siembra *", dpSiembra);
            Formularios.fila(grilla, "Área sembrada (ha) *", txtArea);
            Formularios.fila(grilla, "Rendimiento de referencia", txtReferencia);
            Formularios.nota(grilla, "Kilos por hectárea con que se comparará la cosecha. Se propone el promedio "
                    + "nacional aproximado; ajústalo al de tu zona si lo conoces.");
            Formularios.fila(grilla, "Precio en chacra (" + moneda.simbolo() + "/kg)", txtPrecio);
            Formularios.nota(grilla, "Sirve para valorizar lo que no se vende (consumo, semilla, forraje).");
            Formularios.fila(grilla, "Observación", txtObservacion);
            return grilla;
        }

        DatosCampania datos() {
            LocalDate siembra = CamposFecha.leer(dpSiembra, "siembra");
            Parcela parcela = cmbParcela.getValue();
            return new DatosCampania(parcela == null ? null : parcela.id(), cmbCultivo.getValue(), txtVariedad.getText(),
                    siembra, opcional(txtArea.getText(), "Área sembrada"), opcional(txtReferencia.getText(),
                    "Rendimiento de referencia"), opcional(txtPrecio.getText(), "Precio en chacra"),
                    txtObservacion.getText());
        }
    }

    /** Formulario de parcela. */
    private final class FormularioParcela {
        private final TextField txtNombre = new TextField();
        private final TextField txtArea = new TextField();
        private final ComboBox<Tenencia> cmbTenencia = new ComboBox<>(FXCollections.observableArrayList(Tenencia.values()));
        private final CheckBox chkRiego = new CheckBox("Tiene riego");
        private final TextField txtUbicacion = new TextField();

        FormularioParcela(Parcela p) {
            txtNombre.setPromptText("Ej.: Parcela Alta");
            txtArea.setPromptText("Hectáreas. Ej.: 0.75");
            cmbTenencia.setValue(Tenencia.PROPIA);
            txtUbicacion.setPromptText("Opcional. Ej.: sector Chacrapampa");
            if (p != null) {
                txtNombre.setText(p.nombre());
                txtArea.setText(Montos.cantidad(p.area()));
                cmbTenencia.setValue(p.tenencia());
                chkRiego.setSelected(p.riego());
                txtUbicacion.setText(Objects.requireNonNullElse(p.ubicacion(), ""));
            }
        }

        GridPane contenido() {
            GridPane grilla = Formularios.grilla();
            Formularios.fila(grilla, "Nombre *", txtNombre);
            Formularios.fila(grilla, "Superficie (ha) *", txtArea);
            Formularios.nota(grilla, "1 hectárea = 10 000 m². Un topo equivale a unos 0.3 ha según la zona.");
            Formularios.fila(grilla, "Tenencia *", cmbTenencia);
            Formularios.fila(grilla, "Agua", chkRiego);
            Formularios.fila(grilla, "Ubicación", txtUbicacion);
            return grilla;
        }

        DatosParcela datos() {
            return new DatosParcela(txtNombre.getText(), opcional(txtArea.getText(), "Superficie"), cmbTenencia.getValue(),
                    chkRiego.isSelected(), txtUbicacion.getText());
        }
    }
}
