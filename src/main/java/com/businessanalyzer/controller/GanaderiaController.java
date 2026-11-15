package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.diagnostico.Semaforo;
import com.businessanalyzer.analytics.ganaderia.ExistenciaValorizada;
import com.businessanalyzer.analytics.ganaderia.IndicadorGanadero;
import com.businessanalyzer.analytics.ganaderia.IndicadoresEspecie;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CategoriaGanado;
import com.businessanalyzer.model.DatosMovimientoGanado;
import com.businessanalyzer.model.DatosProduccion;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Especie;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.MovimientoGanado;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.ProduccionGanadera;
import com.businessanalyzer.model.ProductoGanadero;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.TipoMovimientoGanado;
import com.businessanalyzer.service.GanaderiaService;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Módulo "Ganadería": hato, movimientos, producción e indicadores técnicos (empresas agropecuarias). */
public class GanaderiaController implements VistaModulo {

    private static final String TODAS = "Todas las especies";
    private static final PseudoClass SUBTOTAL = PseudoClass.getPseudoClass("subtotal");
    private static final PseudoClass RESULTADO = PseudoClass.getPseudoClass("resultado");
    private static final PseudoClass ARCHIVADO = PseudoClass.getPseudoClass("archivado");

    /** Fila de la tabla del hato: una categoría, el subtotal de una especie o el total. */
    private record FilaHato(String concepto, int cabezas, BigDecimal precio, BigDecimal valor, int nivel) {
    }

    private final AppContext contexto;
    private Moneda moneda = Moneda.PEN;
    private List<ExistenciaValorizada> hatoActual = List.of();

    private final ObservableList<MovimientoGanado> movimientos = FXCollections.observableArrayList();
    private final FilteredList<MovimientoGanado> movimientosFiltrados = new FilteredList<>(movimientos);

    private final TarjetaIndicador kpiAnimales = new TarjetaIndicador("Animales", "fth-feather");
    private final TarjetaIndicador kpiValor = new TarjetaIndicador("Valor del hato", "fth-dollar-sign");
    private final TarjetaIndicador kpiNacimientos = new TarjetaIndicador("Nacimientos", "fth-plus-circle");
    private final TarjetaIndicador kpiMortalidad = new TarjetaIndicador("Mortalidad", "fth-alert-triangle");

    @FXML private HBox indicadores;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Button btnNuevoMovimiento;
    @FXML private TabPane pestanas;

    @FXML private Label lblHato;
    @FXML private Button btnInventarioInicial;
    @FXML private Button btnPrecios;
    @FXML private TableView<FilaHato> tablaHato;
    @FXML private TableColumn<FilaHato, FilaHato> colHatoCategoria;
    @FXML private TableColumn<FilaHato, FilaHato> colHatoCabezas;
    @FXML private TableColumn<FilaHato, FilaHato> colHatoPrecio;
    @FXML private TableColumn<FilaHato, FilaHato> colHatoValor;

    @FXML private ComboBox<String> cmbEspecie;
    @FXML private CheckBox chkAnulados;
    @FXML private Button btnAnularMovimiento;
    @FXML private Button btnEditarMovimiento;
    @FXML private TableView<MovimientoGanado> tablaMovimientos;
    @FXML private TableColumn<MovimientoGanado, LocalDate> colMovFecha;
    @FXML private TableColumn<MovimientoGanado, String> colMovTipo;
    @FXML private TableColumn<MovimientoGanado, String> colMovCategoria;
    @FXML private TableColumn<MovimientoGanado, String> colMovCantidad;
    @FXML private TableColumn<MovimientoGanado, String> colMovMonto;
    @FXML private TableColumn<MovimientoGanado, String> colMovDetalle;

    @FXML private Label lblProduccion;
    @FXML private Button btnAnularProduccion;
    @FXML private Button btnEditarProduccion;
    @FXML private Button btnNuevaProduccion;
    @FXML private TableView<ProduccionGanadera> tablaProduccion;
    @FXML private TableColumn<ProduccionGanadera, LocalDate> colProdFecha;
    @FXML private TableColumn<ProduccionGanadera, String> colProdProducto;
    @FXML private TableColumn<ProduccionGanadera, String> colProdCantidad;
    @FXML private TableColumn<ProduccionGanadera, String> colProdDias;
    @FXML private TableColumn<ProduccionGanadera, String> colProdAnimales;
    @FXML private TableColumn<ProduccionGanadera, String> colProdPromedio;
    @FXML private TableColumn<ProduccionGanadera, String> colProdValor;

    @FXML private VBox contenedorIndicadores;

    @FXML private ComboBox<Integer> cmbMesesEvolucion;
    @FXML private ComboBox<ProductoGanadero> cmbProductoEvolucion;
    @FXML private javafx.scene.layout.StackPane graficoExistencias;
    @FXML private javafx.scene.layout.StackPane graficoNacimientos;
    @FXML private javafx.scene.layout.StackPane graficoProduccion;
    @FXML private VBox lecturaEvolucion;
    private com.businessanalyzer.analytics.ganaderia.EvolucionHato evolucion;

    public GanaderiaController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        indicadores.getChildren().addAll(kpiAnimales, kpiValor, kpiNacimientos, kpiMortalidad);
        CamposFecha.configurar(dpDesde);
        CamposFecha.configurar(dpHasta);
        fijarUltimos12Meses();
        dpDesde.valueProperty().addListener((o, a, n) -> cargar());
        dpHasta.valueProperty().addListener((o, a, n) -> cargar());

        List<String> especies = new ArrayList<>();
        especies.add(TODAS);
        for (Especie e : Especie.values()) {
            especies.add(e.descripcion());
        }
        cmbEspecie.getItems().setAll(especies);
        cmbEspecie.setValue(TODAS);
        cmbEspecie.valueProperty().addListener((o, a, n) -> filtrar());

        configurarTablaHato();
        configurarTablaMovimientos();
        configurarTablaProduccion();
        cmbMesesEvolucion.getItems().setAll(12, 24, 36);
        cmbMesesEvolucion.setValue(24);
        cmbMesesEvolucion.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Integer meses) {
                return meses == null ? "" : meses + " meses";
            }

            @Override
            public Integer fromString(String texto) {
                return null;
            }
        });
        cmbMesesEvolucion.valueProperty().addListener((o, a, n) -> mostrarEvolucion());
        cmbProductoEvolucion.valueProperty().addListener((o, a, n) -> dibujarProduccion());

        boolean gestiona = contexto.sesion().tienePermiso(Permiso.AGRO_GESTIONAR);
        for (Node nodo : List.of(btnNuevoMovimiento, btnInventarioInicial, btnPrecios, btnAnularMovimiento,
                btnEditarMovimiento, btnAnularProduccion, btnEditarProduccion, btnNuevaProduccion)) {
            nodo.setVisible(gestiona);
            nodo.setManaged(gestiona);
        }
    }

    @Override
    public void alMostrar() {
        cargar();
    }

    // ------------------------------------------------------------------ período

    @FXML
    private void ultimos12Meses() {
        fijarUltimos12Meses();
    }

    private void fijarUltimos12Meses() {
        LocalDate hoy = LocalDate.now();
        dpHasta.setValue(hoy);
        dpDesde.setValue(hoy.minusYears(1).plusDays(1));
    }

    @FXML
    private void esteAnio() {
        LocalDate hoy = LocalDate.now();
        dpHasta.setValue(hoy);
        dpDesde.setValue(hoy.withDayOfYear(1));
    }

    // ------------------------------------------------------------------ carga

    @FXML
    private void cargar() {
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        boolean agro = empresa != null && empresa.sector() == Sector.AGROPECUARIO;
        for (Button b : List.of(btnNuevoMovimiento, btnInventarioInicial, btnPrecios, btnNuevaProduccion)) {
            b.setDisable(!agro);
        }
        if (!agro) {
            limpiar(empresa == null ? "Selecciona una empresa en la barra superior."
                    : "La ganadería está disponible solo para empresas del sector agropecuario.");
            return;
        }
        moneda = empresa.moneda();
        if (dpDesde.getValue() == null || dpHasta.getValue() == null || dpDesde.getValue().isAfter(dpHasta.getValue())) {
            return;
        }
        try {
            GanaderiaService servicio = servicio();
            hatoActual = servicio.hato();
            movimientos.setAll(servicio.listarMovimientos(dpDesde.getValue(), dpHasta.getValue(), chkAnulados.isSelected()));
            List<ProduccionGanadera> produccion = servicio.listarProduccion(dpDesde.getValue(), dpHasta.getValue(),
                    chkAnulados.isSelected());
            tablaProduccion.getItems().setAll(produccion);
            List<IndicadoresEspecie> indicadoresPeriodo =
                    servicio.indicadores(new Periodo(dpDesde.getValue(), dpHasta.getValue()));
            mostrarHato();
            mostrarResumen(indicadoresPeriodo);
            Periodo actual = new Periodo(dpDesde.getValue(), dpHasta.getValue());
            mostrarIndicadores(indicadoresPeriodo, servicio.indicadores(actual.anterior()), actual.anterior());
            mostrarEvolucion();
            lblProduccion.setText(produccion.isEmpty() ? "Sin registros de producción en el período."
                    : produccion.size() + (produccion.size() == 1 ? " registro" : " registros") + " en el período.");
        } catch (RuntimeException e) {
            limpiar(Errores.mensaje(e));
            Notificador.error(Errores.mensaje(e));
        }
        tablaMovimientos.setPlaceholder(new Label("No hay movimientos en el período elegido."));
        tablaProduccion.setPlaceholder(new Label("No hay producción registrada en el período."));
        tablaHato.setPlaceholder(new Label("Aún no hay animales. Empieza con «Inventario inicial»."));
        filtrar();
        actualizarAcciones();
    }

    private void limpiar(String mensaje) {
        hatoActual = List.of();
        movimientos.clear();
        tablaHato.getItems().clear();
        tablaProduccion.getItems().clear();
        contenedorIndicadores.getChildren().clear();
        for (TableView<?> tabla : List.of(tablaHato, tablaMovimientos, tablaProduccion)) {
            tabla.setPlaceholder(new Label(mensaje));
        }
        for (TarjetaIndicador kpi : List.of(kpiAnimales, kpiValor, kpiNacimientos, kpiMortalidad)) {
            kpi.actualizar(TarjetaIndicador.SIN_VALOR, "");
        }
        lblHato.setText("");
        lblProduccion.setText("");
    }

    private void mostrarHato() {
        List<FilaHato> filas = new ArrayList<>();
        int totalCabezas = 0;
        BigDecimal totalValor = BigDecimal.ZERO;
        int sinPrecio = 0;
        for (Especie especie : Especie.values()) {
            List<ExistenciaValorizada> deEspecie = hatoActual.stream()
                    .filter(e -> e.categoria().especie() == especie).toList();
            if (deEspecie.isEmpty()) {
                continue;
            }
            int cabezas = deEspecie.stream().mapToInt(ExistenciaValorizada::cabezas).sum();
            BigDecimal valor = ExistenciaValorizada.total(deEspecie);
            filas.add(new FilaHato(especie.descripcion(), cabezas, null, valor, 1));
            for (ExistenciaValorizada e : deEspecie) {
                filas.add(new FilaHato("    " + e.categoria().descripcion(), e.cabezas(), e.precio(), e.valor(), 0));
                if (e.precio() == null) {
                    sinPrecio++;
                }
            }
            totalCabezas += cabezas;
            totalValor = totalValor.add(valor);
        }
        if (!filas.isEmpty()) {
            filas.add(new FilaHato("TOTAL", totalCabezas, null, totalValor, 2));
        }
        tablaHato.getItems().setAll(filas);
        lblHato.setText(sinPrecio == 0 ? (filas.isEmpty() ? "" : "Existencias al " + Fechas.paraMostrar(LocalDate.now()) + ".")
                : sinPrecio + (sinPrecio == 1 ? " categoría no tiene" : " categorías no tienen")
                + " precio por cabeza: su valor no se suma. Indícalo en «Precios por cabeza».");
        kpiAnimales.actualizar(String.valueOf(totalCabezas), resumenEspecies());
        kpiValor.actualizar(Montos.formatear(totalValor, moneda), "A precio de mercado");
    }

    private String resumenEspecies() {
        Map<Especie, Integer> porEspecie = new EnumMap<>(Especie.class);
        hatoActual.forEach(e -> porEspecie.merge(e.categoria().especie(), e.cabezas(), Integer::sum));
        if (porEspecie.isEmpty()) {
            return "Sin animales registrados";
        }
        List<String> partes = new ArrayList<>();
        porEspecie.forEach((esp, n) -> partes.add(n + " " + esp.descripcion().toLowerCase()));
        return String.join(" · ", partes);
    }

    private void mostrarResumen(List<IndicadoresEspecie> lista) {
        int nacimientos = lista.stream().mapToInt(IndicadoresEspecie::nacimientos).sum();
        int muertes = lista.stream().mapToInt(IndicadoresEspecie::muertes).sum();
        int base = lista.stream().mapToInt(i -> i.existenciaInicial() + i.nacimientos() + i.compras()).sum();
        kpiNacimientos.actualizar(String.valueOf(nacimientos), "En el período elegido");
        kpiMortalidad.actualizar(base == 0 ? TarjetaIndicador.SIN_VALOR
                        : Montos.porcentaje(BigDecimal.valueOf(muertes).divide(BigDecimal.valueOf(base), 6, RoundingMode.HALF_UP)),
                muertes + (muertes == 1 ? " animal muerto" : " animales muertos"));
    }

    private void mostrarIndicadores(List<IndicadoresEspecie> lista, List<IndicadoresEspecie> anteriores, Periodo anterior) {
        contenedorIndicadores.getChildren().clear();
        if (lista.isEmpty()) {
            Label vacio = new Label("Sin datos en el período. Registra el inventario inicial y los movimientos del hato.");
            vacio.getStyleClass().add("muted");
            contenedorIndicadores.getChildren().add(vacio);
            return;
        }
        Label periodo = new Label("Período: " + new Periodo(dpDesde.getValue(), dpHasta.getValue()).descripcion()
                + ", comparado con " + anterior.descripcion() + " (entre paréntesis). Los criterios son referencias "
                + "para la ganadería familiar y mediana del Perú.");
        periodo.getStyleClass().add("muted");
        periodo.setWrapText(true);
        contenedorIndicadores.getChildren().add(periodo);
        for (IndicadoresEspecie i : lista) {
            IndicadoresEspecie previo = anteriores.stream().filter(a -> a.especie() == i.especie() && a.historiaCompleta()).findFirst().orElse(null);
            contenedorIndicadores.getChildren().add(tarjetaEspecie(i, previo));
        }
    }

    // ------------------------------------------------------------------ evolución

    private void mostrarEvolucion() {
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        if (empresa == null || empresa.sector() != Sector.AGROPECUARIO || cmbMesesEvolucion.getValue() == null) {
            return;
        }
        YearMonth hasta = YearMonth.now();
        try {
            evolucion = servicio().evolucion(hasta.minusMonths(cmbMesesEvolucion.getValue() - 1L), hasta);
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }
        Map<String, double[]> especies = new LinkedHashMap<>();
        evolucion.existencias().forEach((especie, serie) ->
                especies.put(especie.descripcion(), java.util.Arrays.stream(serie).asDoubleStream().toArray()));
        graficoExistencias.getChildren().setAll(especies.isEmpty() ? sinDatos()
                : com.businessanalyzer.ui.Graficos.visor(com.businessanalyzer.ui.Graficos.lineasMensuales(
                evolucion.meses(), especies, "#,##0")));

        Map<String, double[]> flujo = new LinkedHashMap<>();
        flujo.put("Nacimientos", java.util.Arrays.stream(evolucion.nacimientos()).asDoubleStream().toArray());
        flujo.put("Muertes", java.util.Arrays.stream(evolucion.muertes()).asDoubleStream().toArray());
        flujo.put("Ventas y consumo", java.util.Arrays.stream(evolucion.salidas()).asDoubleStream().toArray());
        graficoNacimientos.getChildren().setAll(com.businessanalyzer.ui.Graficos.visor(
                com.businessanalyzer.ui.Graficos.barrasMensuales(evolucion.meses(), flujo, new java.awt.Color[]{
                        com.businessanalyzer.ui.Graficos.VERDE, com.businessanalyzer.ui.Graficos.ROJO,
                        com.businessanalyzer.ui.Graficos.AZUL}, null, 0, "#,##0")));

        ProductoGanadero elegido = cmbProductoEvolucion.getValue();
        cmbProductoEvolucion.getItems().setAll(evolucion.produccion().keySet());
        cmbProductoEvolucion.setValue(evolucion.produccion().containsKey(elegido) ? elegido
                : cmbProductoEvolucion.getItems().isEmpty() ? null : cmbProductoEvolucion.getItems().get(0));
        cmbProductoEvolucion.setVisible(cmbProductoEvolucion.getItems().size() > 1);
        dibujarProduccion();

        lecturaEvolucion.getChildren().clear();
        for (String frase : evolucion.lectura()) {
            Label punto = new Label("•  " + frase);
            punto.setWrapText(true);
            punto.getStyleClass().add("lectura");
            lecturaEvolucion.getChildren().add(punto);
        }
    }

    private void dibujarProduccion() {
        ProductoGanadero producto = cmbProductoEvolucion.getValue();
        if (evolucion == null || producto == null) {
            graficoProduccion.getChildren().setAll(sinDatos());
            return;
        }
        double[] serie = evolucion.produccion().get(producto);
        int inicio = 0;
        while (inicio < serie.length && serie[inicio] == 0) {
            inicio++;
        }
        Map<String, double[]> datos = new LinkedHashMap<>();
        datos.put(producto.descripcion() + " (" + producto.unidad() + ")", serie);
        graficoProduccion.getChildren().setAll(com.businessanalyzer.ui.Graficos.visor(
                com.businessanalyzer.ui.Graficos.barrasMensuales(evolucion.meses(), datos,
                        new java.awt.Color[]{com.businessanalyzer.ui.Graficos.AZUL}, evolucion.tendencia(producto),
                        inicio, "#,##0")));
    }

    private static Label sinDatos() {
        Label l = new Label("Sin datos en estos meses.");
        l.getStyleClass().add("muted");
        return l;
    }

    private Node tarjetaEspecie(IndicadoresEspecie i, IndicadoresEspecie previo) {
        Map<String, String> valoresPrevios = new java.util.HashMap<>();
        if (previo != null) {
            previo.evaluar(moneda).forEach(p -> valoresPrevios.put(p.nombre(), p.valor()));
        }
        Label titulo = new Label(i.especie().descripcion() + "  ·  " + i.existenciaFinal() + " animales");
        titulo.getStyleClass().add("card-title");
        Label movimientosTexto = new Label("Inicio: " + i.existenciaInicial() + "   Nacimientos: " + i.nacimientos()
                + "   Compras: " + i.compras() + "   Ventas: " + i.ventas() + "   Muertes: " + i.muertes()
                + "   Consumo: " + i.consumo() + "   Final: " + i.existenciaFinal());
        movimientosTexto.getStyleClass().add("hint");
        movimientosTexto.setWrapText(true);

        GridPane grilla = new GridPane();
        grilla.setHgap(16);
        grilla.setVgap(8);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(190);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setMinWidth(170);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setHgrow(Priority.ALWAYS);
        grilla.getColumnConstraints().setAll(c1, c2, c3);
        int fila = 0;
        for (IndicadorGanadero ind : i.evaluar(moneda)) {
            Label nombre = new Label(ind.nombre());
            nombre.getStyleClass().add("info-key");
            String antes = valoresPrevios.get(ind.nombre());
            Label valor = new Label(ind.valor() + (antes == null || "—".equals(antes) ? "" : "  (" + antes + ")"));
            valor.getStyleClass().add("info-value");
            if (ind.semaforo().evaluado() || "—".equals(ind.valor())) {
                valor.getStyleClass().add("semaforo-" + claseSemaforo(ind.semaforo()));
            }
            valor.setStyle("-fx-font-weight: bold;");
            Label lectura = new Label(ind.lectura());
            lectura.getStyleClass().add("muted");
            lectura.setWrapText(true);
            if (ind.criterio() != null) {
                Tooltip ayuda = new Tooltip(ind.criterio());
                nombre.setTooltip(ayuda);
                valor.setTooltip(ayuda);
                lectura.setText(ind.lectura() + "  (" + ind.criterio() + ")");
            }
            grilla.addRow(fila++, nombre, valor, lectura);
        }
        VBox tarjeta = new VBox(10, titulo, movimientosTexto, grilla);
        tarjeta.getStyleClass().add("card");
        return tarjeta;
    }

    private static String claseSemaforo(Semaforo s) {
        return switch (s) {
            case VERDE -> "verde";
            case AMARILLO -> "amarillo";
            case ROJO -> "rojo";
            default -> "sin-datos";
        };
    }

    private void filtrar() {
        String especie = cmbEspecie.getValue();
        movimientosFiltrados.setPredicate(m -> especie == null || TODAS.equals(especie)
                || m.especie().descripcion().equals(especie));
    }

    // ------------------------------------------------------------------ acciones

    @FXML
    private void nuevoMovimiento() {
        abrirFormularioMovimiento(null);
    }

    @FXML
    private void inventarioInicial() {
        abrirFormularioMovimiento(TipoMovimientoGanado.INVENTARIO_INICIAL);
    }

    private void abrirFormularioMovimiento(TipoMovimientoGanado tipo) {
        FormularioMovimiento formulario = new FormularioMovimiento(tipo);
        MovimientoGanado[] creado = new MovimientoGanado[1];
        boolean ok = Dialogos.formulario(ventana(), tipo == TipoMovimientoGanado.INVENTARIO_INICIAL
                        ? "Inventario inicial" : "Registrar movimiento del hato",
                tipo == TipoMovimientoGanado.INVENTARIO_INICIAL
                        ? "Registra cuántos animales de cada categoría tenía al empezar. Repite para cada categoría."
                        : null,
                formulario.contenido(), "Registrar", 540, () -> creado[0] = servicio().registrarMovimiento(formulario.datos()));
        if (ok) {
            cargar();
            MovimientoGanado m = creado[0];
            Notificador.exito(m.tipo().descripcion() + " registrado: " + m.cantidad() + " "
                    + m.categoria().nombreCompleto().toLowerCase() + ".");
        }
    }

    @FXML
    private void anularMovimiento() {
        MovimientoGanado m = tablaMovimientos.getSelectionModel().getSelectedItem();
        if (m == null || m.anulado()) {
            return;
        }
        TextField motivo = new TextField();
        motivo.setPromptText("Ej.: registrado dos veces");
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Motivo *", motivo);
        boolean ok = Dialogos.formulario(ventana(), "Anular movimiento",
                m.tipo().descripcion() + " de " + m.cantidad() + " " + m.categoria().nombreCompleto().toLowerCase()
                        + " del " + Fechas.paraMostrar(m.fecha()) + ". Las existencias se recalcularán.",
                grilla, "Anular", () -> servicio().anularMovimiento(m.id(), motivo.getText()));
        if (ok) {
            cargar();
            Notificador.exito("Movimiento anulado.");
        }
    }

    @FXML
    private void editarMovimiento() {
        MovimientoGanado m = tablaMovimientos.getSelectionModel().getSelectedItem();
        if (m == null || m.anulado()) {
            return;
        }
        FormularioMovimiento formulario = new FormularioMovimiento(m.tipo());
        formulario.cargarDesde(m);
        boolean ok = Dialogos.formulario(ventana(), "Editar movimiento",
                "El registro original queda anulado como historial y se guarda el corregido. "
                        + "Las existencias se recalculan solas.",
                formulario.contenido(), "Guardar cambios", 540,
                () -> servicio().corregirMovimiento(m.id(), formulario.datos()));
        if (ok) {
            cargar();
            Notificador.exito("Movimiento corregido.");
        }
    }

    @FXML
    private void editarProduccion() {
        ProduccionGanadera p = tablaProduccion.getSelectionModel().getSelectedItem();
        if (p == null || p.anulado()) {
            return;
        }
        FormularioProduccion formulario = new FormularioProduccion();
        formulario.cargarDesde(p);
        boolean ok = Dialogos.formulario(ventana(), "Editar producción", null, formulario.contenido(),
                "Guardar cambios", 520, () -> servicio().corregirProduccion(p.id(), formulario.datos()));
        if (ok) {
            cargar();
            pestanas.getSelectionModel().select(2);
            Notificador.exito("Producción corregida.");
        }
    }

    @FXML
    private void nuevaProduccion() {
        FormularioProduccion formulario = new FormularioProduccion();
        boolean ok = Dialogos.formulario(ventana(), "Registrar producción", null, formulario.contenido(),
                "Registrar", 520, () -> servicio().registrarProduccion(formulario.datos()));
        if (ok) {
            cargar();
            pestanas.getSelectionModel().select(2);
            Notificador.exito("Producción registrada.");
        }
    }

    @FXML
    private void anularProduccion() {
        ProduccionGanadera p = tablaProduccion.getSelectionModel().getSelectedItem();
        if (p == null || p.anulado()) {
            return;
        }
        if (Dialogos.confirmar(ventana(), "Anular registro de producción",
                "Se anulará el registro de " + Montos.cantidad(p.cantidad()) + " " + p.producto().unidad() + " de "
                        + p.producto().descripcion().toLowerCase() + " del " + Fechas.paraMostrar(p.fecha()) + ".")) {
            try {
                servicio().anularProduccion(p.id());
                cargar();
                Notificador.exito("Registro anulado.");
            } catch (RuntimeException e) {
                Notificador.error(Errores.mensaje(e));
            }
        }
    }

    @FXML
    private void editarPrecios() {
        Map<CategoriaGanado, BigDecimal> actuales;
        try {
            actuales = servicio().precios();
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }
        // Solo las especies que tienen animales; si aún no hay ninguno, todas.
        List<Especie> especies = hatoActual.stream().map(e -> e.categoria().especie()).distinct().toList();
        if (especies.isEmpty()) {
            especies = List.of(Especie.values());
        }
        Map<CategoriaGanado, TextField> campos = new LinkedHashMap<>();
        GridPane grilla = Formularios.grilla();
        for (Especie especie : especies) {
            Label titulo = new Label(especie.descripcion());
            titulo.getStyleClass().add("step-title");
            grilla.add(titulo, 0, grilla.getRowCount(), 2, 1);
            for (CategoriaGanado c : CategoriaGanado.de(especie)) {
                TextField campo = new TextField(actuales.containsKey(c) ? Montos.paraEditar(actuales.get(c)) : "");
                campo.setPromptText("Sin precio");
                campos.put(c, campo);
                Formularios.fila(grilla, c.descripcion() + " (" + moneda.simbolo() + ")", campo);
            }
        }
        ScrollPane desplazable = new ScrollPane(grilla);
        desplazable.setFitToWidth(true);
        desplazable.setPrefViewportHeight(360);
        desplazable.getStyleClass().add("dialog-scroll");
        boolean ok = Dialogos.formulario(ventana(), "Precios por cabeza",
                "Precio de mercado de un animal de cada categoría (lo que pagarían hoy en la feria o el camal). "
                        + "Deja vacío para no valorizar esa categoría.",
                desplazable, "Guardar precios", 480, () -> {
                    Map<CategoriaGanado, BigDecimal> precios = new EnumMap<>(CategoriaGanado.class);
                    campos.forEach((c, campo) -> precios.put(c, campo.getText() == null || campo.getText().isBlank()
                            ? null : Montos.leer(campo.getText(), "Precio de " + c.nombreCompleto().toLowerCase())));
                    servicio().guardarPrecios(precios);
                });
        if (ok) {
            cargar();
            Notificador.exito("Precios guardados.");
        }
    }

    // ------------------------------------------------------------------ tablas

    private void configurarTablaHato() {
        colHatoCategoria.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colHatoCategoria.setCellFactory(col -> celdaHato(FilaHato::concepto));
        colHatoCabezas.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colHatoCabezas.setCellFactory(col -> celdaHato(f -> String.valueOf(f.cabezas())));
        colHatoPrecio.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colHatoPrecio.setCellFactory(col -> celdaHato(f -> f.nivel() > 0 ? ""
                : f.precio() == null ? "Sin precio" : Montos.formatear(f.precio(), moneda)));
        colHatoValor.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colHatoValor.setCellFactory(col -> celdaHato(f -> f.valor() == null ? "—" : Montos.formatear(f.valor(), moneda)));
        tablaHato.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(FilaHato fila, boolean vacio) {
                super.updateItem(fila, vacio);
                pseudoClassStateChanged(SUBTOTAL, !vacio && fila != null && fila.nivel() == 1);
                pseudoClassStateChanged(RESULTADO, !vacio && fila != null && fila.nivel() == 2);
            }
        });
    }

    private static TableCell<FilaHato, FilaHato> celdaHato(Function<FilaHato, String> texto) {
        return new TableCell<>() {
            @Override
            protected void updateItem(FilaHato fila, boolean vacio) {
                super.updateItem(fila, vacio);
                setText(vacio || fila == null ? null : texto.apply(fila));
            }
        };
    }

    private void configurarTablaMovimientos() {
        colMovFecha.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().fecha()));
        colMovFecha.setCellFactory(col -> celdaFecha());
        colMovTipo.setCellValueFactory(c -> texto(c.getValue().tipo().descripcion()
                + (c.getValue().anulado() ? "  (anulado)" : "")));
        colMovCategoria.setCellValueFactory(c -> {
            MovimientoGanado m = c.getValue();
            return texto(m.categoria().nombreCompleto()
                    + (m.categoriaDestino() == null ? "" : " → " + m.categoriaDestino().descripcion()));
        });
        colMovCantidad.setCellValueFactory(c -> {
            MovimientoGanado m = c.getValue();
            String signo = m.tipo().signo() > 0 ? "+" : m.tipo().signo() < 0 ? "−" : "";
            return texto(signo + m.cantidad());
        });
        colMovMonto.setCellValueFactory(c -> texto(c.getValue().monto() == null ? ""
                : Montos.formatear(c.getValue().monto(), moneda)));
        colMovDetalle.setCellValueFactory(c -> {
            MovimientoGanado m = c.getValue();
            List<String> partes = new ArrayList<>();
            if (m.causa() != null) {
                partes.add("Causa: " + m.causa());
            }
            if (m.observacion() != null) {
                partes.add(m.observacion());
            }
            return texto(String.join(" · ", partes));
        });
        tablaMovimientos.setItems(movimientosFiltrados);
        tablaMovimientos.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> actualizarAcciones());
        tablaMovimientos.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(MovimientoGanado m, boolean vacio) {
                super.updateItem(m, vacio);
                boolean anulado = !vacio && m != null && m.anulado();
                pseudoClassStateChanged(ARCHIVADO, anulado);
                setTooltip(anulado ? new Tooltip("Anulado: " + m.motivoAnulacion()) : null);
            }
        });
    }

    private void configurarTablaProduccion() {
        colProdFecha.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().fecha()));
        colProdFecha.setCellFactory(col -> celdaFecha());
        colProdProducto.setCellValueFactory(c -> texto(c.getValue().producto().descripcion() + " ("
                + c.getValue().especie().descripcion().toLowerCase() + ")" + (c.getValue().anulado() ? "  (anulado)" : "")));
        colProdCantidad.setCellValueFactory(c -> texto(Montos.cantidad(c.getValue().cantidad()) + " "
                + c.getValue().producto().abreviatura()));
        colProdDias.setCellValueFactory(c -> texto(String.valueOf(c.getValue().dias())));
        colProdAnimales.setCellValueFactory(c -> texto(c.getValue().animales() == null ? "—"
                : String.valueOf(c.getValue().animales())));
        colProdPromedio.setCellValueFactory(c -> {
            ProduccionGanadera p = c.getValue();
            if (p.animales() == null) {
                return texto("—");
            }
            int base = p.animales() * (p.producto().porDia() ? p.dias() : 1);
            BigDecimal promedio = p.cantidad().divide(BigDecimal.valueOf(base), 2, RoundingMode.HALF_UP);
            return texto(Montos.cantidad(promedio) + " " + p.producto().abreviatura()
                    + (p.producto().porDia() ? "/vaca/día" : "/animal"));
        });
        colProdValor.setCellValueFactory(c -> texto(c.getValue().valor() == null ? ""
                : Montos.formatear(c.getValue().valor(), moneda)));
        tablaProduccion.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> actualizarAcciones());
        tablaProduccion.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(ProduccionGanadera p, boolean vacio) {
                super.updateItem(p, vacio);
                pseudoClassStateChanged(ARCHIVADO, !vacio && p != null && p.anulado());
            }
        });
    }

    private void actualizarAcciones() {
        MovimientoGanado m = tablaMovimientos.getSelectionModel().getSelectedItem();
        btnAnularMovimiento.setDisable(m == null || m.anulado());
        btnEditarMovimiento.setDisable(m == null || m.anulado());
        ProduccionGanadera p = tablaProduccion.getSelectionModel().getSelectedItem();
        btnAnularProduccion.setDisable(p == null || p.anulado());
        btnEditarProduccion.setDisable(p == null || p.anulado());
    }

    private static <T> TableCell<T, LocalDate> celdaFecha() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                setText(vacio ? null : Fechas.paraMostrar(fecha));
            }
        };
    }

    // ------------------------------------------------------------------ utilidades

    private GanaderiaService servicio() {
        return contexto.ganaderia();
    }

    private Window ventana() {
        return tablaHato.getScene().getWindow();
    }

    private static ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(valor);
    }

    private static Integer entero(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(texto.strip());
        } catch (NumberFormatException e) {
            throw new ValidacionException("El campo \"" + campo + "\" debe ser un número entero.");
        }
    }

    private int existencias(CategoriaGanado c) {
        return hatoActual.stream().filter(e -> e.categoria() == c).mapToInt(ExistenciaValorizada::cabezas).sum();
    }

    /** Formulario de movimiento: los campos se activan según el tipo (valor en compras y ventas, causa en muertes...). */
    private final class FormularioMovimiento {
        private final ComboBox<TipoMovimientoGanado> cmbTipo =
                new ComboBox<>(FXCollections.observableArrayList(TipoMovimientoGanado.values()));
        private final Label lblAyuda = new Label();
        private final DatePicker dpFecha = CamposFecha.nuevo(LocalDate.now());
        private final ComboBox<Especie> cmbEspecie = new ComboBox<>(FXCollections.observableArrayList(Especie.values()));
        private final ComboBox<CategoriaGanado> cmbCategoria = new ComboBox<>();
        private final Label lblExistencias = new Label();
        private final ComboBox<CategoriaGanado> cmbDestino = new ComboBox<>();
        private final TextField txtCantidad = new TextField();
        private final TextField txtMonto = new TextField();
        private final TextField txtCausa = new TextField();
        private final TextField txtObservacion = new TextField();

        FormularioMovimiento(TipoMovimientoGanado tipo) {
            cmbTipo.setPromptText("Selecciona el movimiento");
            cmbTipo.setVisibleRowCount(10);
            cmbEspecie.setPromptText("Especie");
            cmbCategoria.setPromptText("Categoría");
            cmbDestino.setPromptText("Pasan a…");
            txtCantidad.setPromptText("Número de animales");
            txtMonto.setPromptText("Opcional");
            txtCausa.setPromptText("Opcional. Ej.: neumonía, helada, puma");
            txtObservacion.setPromptText("Opcional");
            for (Label l : List.of(lblAyuda, lblExistencias)) {
                l.getStyleClass().add("hint");
                l.setWrapText(true);
                l.setMinHeight(Region.USE_PREF_SIZE);
            }
            cmbTipo.valueProperty().addListener((o, a, n) -> actualizar());
            cmbEspecie.valueProperty().addListener((o, a, n) -> actualizar());
            cmbCategoria.valueProperty().addListener((o, a, n) -> {
                lblExistencias.setText(n == null ? "" : "Existencias de hoy: " + existencias(n) + " "
                        + n.descripcion().toLowerCase() + ".");
                actualizarDestino();
            });
            if (cmbEspecie.getItems().size() > 0) {
                // Propone la especie con más animales.
                hatoActual.stream().max(java.util.Comparator.comparingInt(ExistenciaValorizada::cabezas))
                        .ifPresent(e -> cmbEspecie.setValue(e.categoria().especie()));
            }
            cmbTipo.setValue(tipo);
            if (tipo != null) {
                cmbTipo.setDisable(true);
            }
            actualizar();
        }

        private void actualizar() {
            TipoMovimientoGanado tipo = cmbTipo.getValue();
            Especie especie = cmbEspecie.getValue();
            lblAyuda.setText(tipo == null ? "" : tipo.ayuda());
            CategoriaGanado anterior = cmbCategoria.getValue();
            List<CategoriaGanado> categorias = especie == null ? List.of() : CategoriaGanado.de(especie).stream()
                    .filter(c -> tipo != TipoMovimientoGanado.NACIMIENTO || c.cria()).toList();
            cmbCategoria.getItems().setAll(categorias);
            cmbCategoria.setValue(categorias.contains(anterior) ? anterior : null);
            boolean monto = tipo != null && tipo.llevaMonto();
            boolean causa = tipo != null && tipo.llevaCausa();
            txtMonto.setDisable(!monto);
            txtCausa.setDisable(!causa);
            if (!monto) {
                txtMonto.clear();
            }
            if (!causa) {
                txtCausa.clear();
            }
            actualizarDestino();
        }

        private void actualizarDestino() {
            boolean cambio = cmbTipo.getValue() == TipoMovimientoGanado.CAMBIO_CATEGORIA;
            CategoriaGanado origen = cmbCategoria.getValue();
            cmbDestino.setDisable(!cambio || origen == null);
            cmbDestino.getItems().setAll(!cambio || origen == null ? List.of()
                    : CategoriaGanado.de(origen.especie()).stream().filter(c -> c != origen).toList());
        }

        void cargarDesde(MovimientoGanado m) {
            dpFecha.setValue(m.fecha());
            cmbEspecie.setValue(m.categoria().especie());
            cmbCategoria.setValue(m.categoria());
            cmbDestino.setValue(m.categoriaDestino());
            txtCantidad.setText(String.valueOf(m.cantidad()));
            txtMonto.setText(m.monto() == null ? "" : Montos.paraEditar(m.monto()));
            txtCausa.setText(m.causa() == null ? "" : m.causa());
            txtObservacion.setText(m.observacion() == null ? "" : m.observacion());
        }

        GridPane contenido() {
            GridPane grilla = Formularios.grilla();
            Formularios.fila(grilla, "Movimiento *", cmbTipo);
            Formularios.fila(grilla, "", lblAyuda);
            Formularios.fila(grilla, "Fecha *", dpFecha);
            Formularios.fila(grilla, "Especie *", cmbEspecie);
            Formularios.fila(grilla, "Categoría *", cmbCategoria);
            Formularios.fila(grilla, "", lblExistencias);
            Formularios.fila(grilla, "Pasan a", cmbDestino);
            Formularios.fila(grilla, "Cantidad *", txtCantidad);
            Formularios.fila(grilla, "Valor total (" + moneda.simbolo() + ")", txtMonto);
            Formularios.fila(grilla, "Causa", txtCausa);
            Formularios.fila(grilla, "Observación", txtObservacion);
            return grilla;
        }

        DatosMovimientoGanado datos() {
            LocalDate fecha = CamposFecha.leer(dpFecha, "el movimiento");
            BigDecimal monto = txtMonto.getText() == null || txtMonto.getText().isBlank() ? null
                    : Montos.leer(txtMonto.getText(), "Valor total");
            return new DatosMovimientoGanado(fecha, cmbTipo.getValue(), cmbCategoria.getValue(),
                    entero(txtCantidad.getText(), "Cantidad"), cmbDestino.getValue(), monto, txtCausa.getText(),
                    txtObservacion.getText());
        }
    }

    /** Formulario de producción de leche, fibra o lana. */
    private final class FormularioProduccion {
        private final ComboBox<Especie> cmbEspecie = new ComboBox<>(FXCollections.observableArrayList(Especie.values()));
        private final ComboBox<ProductoGanadero> cmbProducto = new ComboBox<>();
        private final DatePicker dpFecha = CamposFecha.nuevo(LocalDate.now());
        private final TextField txtDias = new TextField("1");
        private final TextField txtCantidad = new TextField();
        private final TextField txtAnimales = new TextField();
        private final TextField txtPrecio = new TextField();
        private final TextField txtObservacion = new TextField();
        private final Label lblCantidad = new Label("Cantidad *");
        private final Label lblAnimales = new Label("Animales");
        private final Label lblPrecio = new Label("Precio unitario");

        FormularioProduccion() {
            cmbEspecie.setPromptText("Especie");
            cmbProducto.setPromptText("Producto");
            txtDias.setPromptText("1 = un día; 30 = el mes");
            txtCantidad.setPromptText("0.0");
            txtAnimales.setPromptText("Opcional");
            txtPrecio.setPromptText("Opcional");
            txtObservacion.setPromptText("Opcional");
            cmbEspecie.valueProperty().addListener((o, a, especie) -> {
                cmbProducto.getItems().setAll(especie == null ? List.of() : ProductoGanadero.de(especie));
                cmbProducto.setValue(cmbProducto.getItems().size() == 1 ? cmbProducto.getItems().get(0) : null);
            });
            cmbProducto.valueProperty().addListener((o, a, p) -> {
                lblCantidad.setText(p == null ? "Cantidad *" : "Cantidad (" + p.unidad() + ") *");
                lblAnimales.setText(p == null ? "Animales" : capital(p.animales()));
                lblPrecio.setText(p == null ? "Precio unitario" : "Precio por " + p.abreviatura() + " (" + moneda.simbolo() + ")");
                txtDias.setDisable(p != null && !p.porDia());
            });
            hatoActual.stream().max(java.util.Comparator.comparingInt(ExistenciaValorizada::cabezas))
                    .ifPresent(e -> cmbEspecie.setValue(e.categoria().especie()));
        }

        void cargarDesde(ProduccionGanadera p) {
            cmbEspecie.setValue(p.especie());
            cmbProducto.setValue(p.producto());
            dpFecha.setValue(p.fecha());
            txtDias.setText(String.valueOf(p.dias()));
            txtCantidad.setText(Montos.paraEditar(p.cantidad()));
            txtAnimales.setText(p.animales() == null ? "" : String.valueOf(p.animales()));
            txtPrecio.setText(p.precioUnitario() == null ? "" : Montos.paraEditar(p.precioUnitario()));
            txtObservacion.setText(p.observacion() == null ? "" : p.observacion());
        }

        GridPane contenido() {
            GridPane grilla = Formularios.grilla();
            Formularios.fila(grilla, "Especie *", cmbEspecie);
            Formularios.fila(grilla, "Producto *", cmbProducto);
            Formularios.fila(grilla, "Fecha *", dpFecha);
            Formularios.fila(grilla, "Días que abarca", txtDias);
            for (Label l : List.of(lblCantidad, lblAnimales, lblPrecio)) {
                l.getStyleClass().add("form-label");
            }
            int fila = grilla.getRowCount();
            grilla.addRow(fila, lblCantidad, txtCantidad);
            grilla.addRow(fila + 1, lblAnimales, txtAnimales);
            grilla.addRow(fila + 2, lblPrecio, txtPrecio);
            Formularios.fila(grilla, "Observación", txtObservacion);
            Formularios.nota(grilla, "Para la leche, «días» indica cuántos días suma el registro. La fibra y la lana "
                    + "se registran por esquila.");
            return grilla;
        }

        DatosProduccion datos() {
            LocalDate fecha = CamposFecha.leer(dpFecha, "la producción");
            BigDecimal precio = txtPrecio.getText() == null || txtPrecio.getText().isBlank() ? null
                    : Montos.leer(txtPrecio.getText(), "Precio");
            ProductoGanadero producto = cmbProducto.getValue();
            Integer dias = producto != null && !producto.porDia() ? Integer.valueOf(1) : entero(txtDias.getText(), "Días");
            return new DatosProduccion(cmbEspecie.getValue(), producto, fecha, dias,
                    txtCantidad.getText() == null || txtCantidad.getText().isBlank() ? null
                            : Montos.leer(txtCantidad.getText(), "Cantidad"),
                    entero(txtAnimales.getText(), "Animales"), precio, txtObservacion.getText());
        }

        private String capital(String texto) {
            return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
        }
    }
}
