package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;
import com.businessanalyzer.analytics.ganaderia.ExistenciaValorizadaFGEM;
import com.businessanalyzer.analytics.ganaderia.IndicadorGanaderoFGEM;
import com.businessanalyzer.analytics.ganaderia.IndicadoresEspecieFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CategoriaGanadoFGEM;
import com.businessanalyzer.model.DatosMovimientoGanadoFGEM;
import com.businessanalyzer.model.DatosProduccionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EspecieFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.MovimientoGanadoFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.ProduccionGanaderaFGEM;
import com.businessanalyzer.model.ProductoGanaderoFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.TipoMovimientoGanadoFGEM;
import com.businessanalyzer.service.GanaderiaServiceFGEM;
import com.businessanalyzer.ui.CamposFechaFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.FormulariosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.TarjetaIndicadorFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;
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
public class GanaderiaControllerFGEM implements VistaModuloFGEM {

    private static final String TODAS_FGEM = "Todas las especies";
    private static final PseudoClass SUBTOTAL_FGEM = PseudoClass.getPseudoClass("subtotal");
    private static final PseudoClass RESULTADO_FGEM = PseudoClass.getPseudoClass("resultado");
    private static final PseudoClass ARCHIVADO_FGEM = PseudoClass.getPseudoClass("archivado");

    /** Fila de la tabla del hato: una categoría, el subtotal de una especie o el total. */
    private record FilaHatoFGEM(String conceptoFGEM, int cabezasFGEM, BigDecimal precioFGEM, BigDecimal valorFGEM, int nivelFGEM) {
    }

    private final AppContextFGEM contextoFGEM;
    private MonedaFGEM monedaFGEM = MonedaFGEM.PEN;
    private List<ExistenciaValorizadaFGEM> hatoActualFGEM = List.of();

    private final ObservableList<MovimientoGanadoFGEM> movimientosFGEM = FXCollections.observableArrayList();
    private final FilteredList<MovimientoGanadoFGEM> movimientosFiltradosFGEM = new FilteredList<>(movimientosFGEM);

    private final TarjetaIndicadorFGEM kpiAnimalesFGEM = new TarjetaIndicadorFGEM("Animales", "fth-feather");
    private final TarjetaIndicadorFGEM kpiValorFGEM = new TarjetaIndicadorFGEM("Valor del hato", "fth-dollar-sign");
    private final TarjetaIndicadorFGEM kpiNacimientosFGEM = new TarjetaIndicadorFGEM("Nacimientos", "fth-plus-circle");
    private final TarjetaIndicadorFGEM kpiMortalidadFGEM = new TarjetaIndicadorFGEM("Mortalidad", "fth-alert-triangle");

    @FXML private HBox indicadoresFGEM;
    @FXML private DatePicker dpDesdeFGEM;
    @FXML private DatePicker dpHastaFGEM;
    @FXML private Button btnNuevoMovimientoFGEM;
    @FXML private TabPane pestanasFGEM;

    @FXML private Label lblHatoFGEM;
    @FXML private Button btnInventarioInicialFGEM;
    @FXML private Button btnPreciosFGEM;
    @FXML private TableView<FilaHatoFGEM> tablaHatoFGEM;
    @FXML private TableColumn<FilaHatoFGEM, FilaHatoFGEM> colHatoCategoriaFGEM;
    @FXML private TableColumn<FilaHatoFGEM, FilaHatoFGEM> colHatoCabezasFGEM;
    @FXML private TableColumn<FilaHatoFGEM, FilaHatoFGEM> colHatoPrecioFGEM;
    @FXML private TableColumn<FilaHatoFGEM, FilaHatoFGEM> colHatoValorFGEM;

    @FXML private ComboBox<String> cmbEspecieFGEM;
    @FXML private CheckBox chkAnuladosFGEM;
    @FXML private Button btnAnularMovimientoFGEM;
    @FXML private Button btnEditarMovimientoFGEM;
    @FXML private TableView<MovimientoGanadoFGEM> tablaMovimientosFGEM;
    @FXML private TableColumn<MovimientoGanadoFGEM, LocalDate> colMovFechaFGEM;
    @FXML private TableColumn<MovimientoGanadoFGEM, String> colMovTipoFGEM;
    @FXML private TableColumn<MovimientoGanadoFGEM, String> colMovCategoriaFGEM;
    @FXML private TableColumn<MovimientoGanadoFGEM, String> colMovCantidadFGEM;
    @FXML private TableColumn<MovimientoGanadoFGEM, String> colMovMontoFGEM;
    @FXML private TableColumn<MovimientoGanadoFGEM, String> colMovDetalleFGEM;

    @FXML private Label lblProduccionFGEM;
    @FXML private Button btnAnularProduccionFGEM;
    @FXML private Button btnEditarProduccionFGEM;
    @FXML private Button btnNuevaProduccionFGEM;
    @FXML private TableView<ProduccionGanaderaFGEM> tablaProduccionFGEM;
    @FXML private TableColumn<ProduccionGanaderaFGEM, LocalDate> colProdFechaFGEM;
    @FXML private TableColumn<ProduccionGanaderaFGEM, String> colProdProductoFGEM;
    @FXML private TableColumn<ProduccionGanaderaFGEM, String> colProdCantidadFGEM;
    @FXML private TableColumn<ProduccionGanaderaFGEM, String> colProdDiasFGEM;
    @FXML private TableColumn<ProduccionGanaderaFGEM, String> colProdAnimalesFGEM;
    @FXML private TableColumn<ProduccionGanaderaFGEM, String> colProdPromedioFGEM;
    @FXML private TableColumn<ProduccionGanaderaFGEM, String> colProdValorFGEM;

    @FXML private VBox contenedorIndicadoresFGEM;

    @FXML private ComboBox<Integer> cmbMesesEvolucionFGEM;
    @FXML private ComboBox<ProductoGanaderoFGEM> cmbProductoEvolucionFGEM;
    @FXML private javafx.scene.layout.StackPane graficoExistenciasFGEM;
    @FXML private javafx.scene.layout.StackPane graficoNacimientosFGEM;
    @FXML private javafx.scene.layout.StackPane graficoProduccionFGEM;
    @FXML private VBox lecturaEvolucionFGEM;
    private com.businessanalyzer.analytics.ganaderia.EvolucionHatoFGEM evolucionFGEM;

    public GanaderiaControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        indicadoresFGEM.getChildren().addAll(kpiAnimalesFGEM, kpiValorFGEM, kpiNacimientosFGEM, kpiMortalidadFGEM);
        CamposFechaFGEM.configurarFGEM(dpDesdeFGEM);
        CamposFechaFGEM.configurarFGEM(dpHastaFGEM);
        fijarUltimos12MesesFGEM();
        dpDesdeFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarFGEM());
        dpHastaFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarFGEM());

        List<String> especiesFGEM = new ArrayList<>();
        especiesFGEM.add(TODAS_FGEM);
        for (EspecieFGEM eFGEM : EspecieFGEM.values()) {
            especiesFGEM.add(eFGEM.descripcionFGEM());
        }
        cmbEspecieFGEM.getItems().setAll(especiesFGEM);
        cmbEspecieFGEM.setValue(TODAS_FGEM);
        cmbEspecieFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> filtrarFGEM());

        configurarTablaHatoFGEM();
        configurarTablaMovimientosFGEM();
        configurarTablaProduccionFGEM();
        cmbMesesEvolucionFGEM.getItems().setAll(12, 24, 36);
        cmbMesesEvolucionFGEM.setValue(24);
        cmbMesesEvolucionFGEM.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Integer mesesFGEM) {
                return mesesFGEM == null ? "" : mesesFGEM + " meses";
            }

            @Override
            public Integer fromString(String textoFGEM) {
                return null;
            }
        });
        cmbMesesEvolucionFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> mostrarEvolucionFGEM());
        cmbProductoEvolucionFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> dibujarProduccionFGEM());

        boolean gestionaFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.AGRO_GESTIONAR);
        for (Node nodoFGEM : List.of(btnNuevoMovimientoFGEM, btnInventarioInicialFGEM, btnPreciosFGEM, btnAnularMovimientoFGEM,
                btnEditarMovimientoFGEM, btnAnularProduccionFGEM, btnEditarProduccionFGEM, btnNuevaProduccionFGEM)) {
            nodoFGEM.setVisible(gestionaFGEM);
            nodoFGEM.setManaged(gestionaFGEM);
        }
    }

    @Override
    public void alMostrarFGEM() {
        cargarFGEM();
    }

    // ------------------------------------------------------------------ período

    @FXML
    private void ultimos12MesesFGEM() {
        fijarUltimos12MesesFGEM();
    }

    private void fijarUltimos12MesesFGEM() {
        LocalDate hoyFGEM = LocalDate.now();
        dpHastaFGEM.setValue(hoyFGEM);
        dpDesdeFGEM.setValue(hoyFGEM.minusYears(1).plusDays(1));
    }

    @FXML
    private void esteAnioFGEM() {
        LocalDate hoyFGEM = LocalDate.now();
        dpHastaFGEM.setValue(hoyFGEM);
        dpDesdeFGEM.setValue(hoyFGEM.withDayOfYear(1));
    }

    // ------------------------------------------------------------------ carga

    @FXML
    private void cargarFGEM() {
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        boolean agroFGEM = empresaFGEM != null && empresaFGEM.sectorFGEM() == SectorFGEM.AGROPECUARIO;
        for (Button bFGEM : List.of(btnNuevoMovimientoFGEM, btnInventarioInicialFGEM, btnPreciosFGEM, btnNuevaProduccionFGEM)) {
            bFGEM.setDisable(!agroFGEM);
        }
        if (!agroFGEM) {
            limpiarFGEM(empresaFGEM == null ? "Selecciona una empresa en la barra superior."
                    : "La ganadería está disponible solo para empresas del sector agropecuario.");
            return;
        }
        monedaFGEM = empresaFGEM.monedaFGEM();
        if (dpDesdeFGEM.getValue() == null || dpHastaFGEM.getValue() == null || dpDesdeFGEM.getValue().isAfter(dpHastaFGEM.getValue())) {
            return;
        }
        try {
            GanaderiaServiceFGEM servicioFGEM = servicioFGEM();
            hatoActualFGEM = servicioFGEM.hatoFGEM();
            movimientosFGEM.setAll(servicioFGEM.listarMovimientosFGEM(dpDesdeFGEM.getValue(), dpHastaFGEM.getValue(), chkAnuladosFGEM.isSelected()));
            List<ProduccionGanaderaFGEM> produccionFGEM = servicioFGEM.listarProduccionFGEM(dpDesdeFGEM.getValue(), dpHastaFGEM.getValue(),
                    chkAnuladosFGEM.isSelected());
            tablaProduccionFGEM.getItems().setAll(produccionFGEM);
            List<IndicadoresEspecieFGEM> indicadoresPeriodoFGEM =
                    servicioFGEM.indicadoresFGEM(new PeriodoFGEM(dpDesdeFGEM.getValue(), dpHastaFGEM.getValue()));
            mostrarHatoFGEM();
            mostrarResumenFGEM(indicadoresPeriodoFGEM);
            PeriodoFGEM actualFGEM = new PeriodoFGEM(dpDesdeFGEM.getValue(), dpHastaFGEM.getValue());
            mostrarIndicadoresFGEM(indicadoresPeriodoFGEM, servicioFGEM.indicadoresFGEM(actualFGEM.anteriorFGEM()), actualFGEM.anteriorFGEM());
            mostrarEvolucionFGEM();
            lblProduccionFGEM.setText(produccionFGEM.isEmpty() ? "Sin registros de producción en el período."
                    : produccionFGEM.size() + (produccionFGEM.size() == 1 ? " registro" : " registros") + " en el período.");
        } catch (RuntimeException eFGEM) {
            limpiarFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
        tablaMovimientosFGEM.setPlaceholder(new Label("No hay movimientos en el período elegido."));
        tablaProduccionFGEM.setPlaceholder(new Label("No hay producción registrada en el período."));
        tablaHatoFGEM.setPlaceholder(new Label("Aún no hay animales. Empieza con «Inventario inicial»."));
        filtrarFGEM();
        actualizarAccionesFGEM();
    }

    private void limpiarFGEM(String mensajeFGEM) {
        hatoActualFGEM = List.of();
        movimientosFGEM.clear();
        tablaHatoFGEM.getItems().clear();
        tablaProduccionFGEM.getItems().clear();
        contenedorIndicadoresFGEM.getChildren().clear();
        for (TableView<?> tablaFGEM : List.of(tablaHatoFGEM, tablaMovimientosFGEM, tablaProduccionFGEM)) {
            tablaFGEM.setPlaceholder(new Label(mensajeFGEM));
        }
        for (TarjetaIndicadorFGEM kpiFGEM : List.of(kpiAnimalesFGEM, kpiValorFGEM, kpiNacimientosFGEM, kpiMortalidadFGEM)) {
            kpiFGEM.actualizarFGEM(TarjetaIndicadorFGEM.SIN_VALOR_FGEM, "");
        }
        lblHatoFGEM.setText("");
        lblProduccionFGEM.setText("");
    }

    private void mostrarHatoFGEM() {
        List<FilaHatoFGEM> filasFGEM = new ArrayList<>();
        int totalCabezasFGEM = 0;
        BigDecimal totalValorFGEM = BigDecimal.ZERO;
        int sinPrecioFGEM = 0;
        for (EspecieFGEM especieFGEM : EspecieFGEM.values()) {
            List<ExistenciaValorizadaFGEM> deEspecieFGEM = hatoActualFGEM.stream()
                    .filter(eFGEM -> eFGEM.categoriaFGEM().especieFGEM() == especieFGEM).toList();
            if (deEspecieFGEM.isEmpty()) {
                continue;
            }
            int cabezasFGEM = deEspecieFGEM.stream().mapToInt(ExistenciaValorizadaFGEM::cabezasFGEM).sum();
            BigDecimal valorFGEM = ExistenciaValorizadaFGEM.totalFGEM(deEspecieFGEM);
            filasFGEM.add(new FilaHatoFGEM(especieFGEM.descripcionFGEM(), cabezasFGEM, null, valorFGEM, 1));
            for (ExistenciaValorizadaFGEM eFGEM : deEspecieFGEM) {
                filasFGEM.add(new FilaHatoFGEM("    " + eFGEM.categoriaFGEM().descripcionFGEM(), eFGEM.cabezasFGEM(), eFGEM.precioFGEM(), eFGEM.valorFGEM(), 0));
                if (eFGEM.precioFGEM() == null) {
                    sinPrecioFGEM++;
                }
            }
            totalCabezasFGEM += cabezasFGEM;
            totalValorFGEM = totalValorFGEM.add(valorFGEM);
        }
        if (!filasFGEM.isEmpty()) {
            filasFGEM.add(new FilaHatoFGEM("TOTAL", totalCabezasFGEM, null, totalValorFGEM, 2));
        }
        tablaHatoFGEM.getItems().setAll(filasFGEM);
        lblHatoFGEM.setText(sinPrecioFGEM == 0 ? (filasFGEM.isEmpty() ? "" : "Existencias al " + FechasFGEM.paraMostrarFGEM(LocalDate.now()) + ".")
                : sinPrecioFGEM + (sinPrecioFGEM == 1 ? " categoría no tiene" : " categorías no tienen")
                + " precio por cabeza: su valor no se suma. Indícalo en «Precios por cabeza».");
        kpiAnimalesFGEM.actualizarFGEM(String.valueOf(totalCabezasFGEM), resumenEspeciesFGEM());
        kpiValorFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(totalValorFGEM, monedaFGEM), "A precio de mercado");
    }

    private String resumenEspeciesFGEM() {
        Map<EspecieFGEM, Integer> porEspecieFGEM = new EnumMap<>(EspecieFGEM.class);
        hatoActualFGEM.forEach(eFGEM -> porEspecieFGEM.merge(eFGEM.categoriaFGEM().especieFGEM(), eFGEM.cabezasFGEM(), Integer::sum));
        if (porEspecieFGEM.isEmpty()) {
            return "Sin animales registrados";
        }
        List<String> partesFGEM = new ArrayList<>();
        porEspecieFGEM.forEach((espFGEM, nFGEM) -> partesFGEM.add(nFGEM + " " + espFGEM.descripcionFGEM().toLowerCase()));
        return String.join(" · ", partesFGEM);
    }

    private void mostrarResumenFGEM(List<IndicadoresEspecieFGEM> listaFGEM) {
        int nacimientosFGEM = listaFGEM.stream().mapToInt(IndicadoresEspecieFGEM::nacimientosFGEM).sum();
        int muertesFGEM = listaFGEM.stream().mapToInt(IndicadoresEspecieFGEM::muertesFGEM).sum();
        int baseFGEM = listaFGEM.stream().mapToInt(iFGEM -> iFGEM.existenciaInicialFGEM() + iFGEM.nacimientosFGEM() + iFGEM.comprasFGEM()).sum();
        kpiNacimientosFGEM.actualizarFGEM(String.valueOf(nacimientosFGEM), "En el período elegido");
        kpiMortalidadFGEM.actualizarFGEM(baseFGEM == 0 ? TarjetaIndicadorFGEM.SIN_VALOR_FGEM
                        : MontosFGEM.porcentajeFGEM(BigDecimal.valueOf(muertesFGEM).divide(BigDecimal.valueOf(baseFGEM), 6, RoundingMode.HALF_UP)),
                muertesFGEM + (muertesFGEM == 1 ? " animal muerto" : " animales muertos"));
    }

    private void mostrarIndicadoresFGEM(List<IndicadoresEspecieFGEM> listaFGEM, List<IndicadoresEspecieFGEM> anterioresFGEM, PeriodoFGEM anteriorFGEM) {
        contenedorIndicadoresFGEM.getChildren().clear();
        if (listaFGEM.isEmpty()) {
            Label vacioFGEM = new Label("Sin datos en el período. Registra el inventario inicial y los movimientos del hato.");
            vacioFGEM.getStyleClass().add("muted");
            contenedorIndicadoresFGEM.getChildren().add(vacioFGEM);
            return;
        }
        Label periodoFGEM = new Label("Período: " + new PeriodoFGEM(dpDesdeFGEM.getValue(), dpHastaFGEM.getValue()).descripcionFGEM()
                + ", comparado con " + anteriorFGEM.descripcionFGEM() + " (entre paréntesis). Los criterios son referencias "
                + "para la ganadería familiar y mediana del Perú.");
        periodoFGEM.getStyleClass().add("muted");
        periodoFGEM.setWrapText(true);
        contenedorIndicadoresFGEM.getChildren().add(periodoFGEM);
        for (IndicadoresEspecieFGEM iFGEM : listaFGEM) {
            IndicadoresEspecieFGEM previoFGEM = anterioresFGEM.stream().filter(aFGEM -> aFGEM.especieFGEM() == iFGEM.especieFGEM() && aFGEM.historiaCompletaFGEM()).findFirst().orElse(null);
            contenedorIndicadoresFGEM.getChildren().add(tarjetaEspecieFGEM(iFGEM, previoFGEM));
        }
    }

    // ------------------------------------------------------------------ evolución

    private void mostrarEvolucionFGEM() {
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        if (empresaFGEM == null || empresaFGEM.sectorFGEM() != SectorFGEM.AGROPECUARIO || cmbMesesEvolucionFGEM.getValue() == null) {
            return;
        }
        YearMonth hastaFGEM = YearMonth.now();
        try {
            evolucionFGEM = servicioFGEM().evolucionFGEM(hastaFGEM.minusMonths(cmbMesesEvolucionFGEM.getValue() - 1L), hastaFGEM);
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        Map<String, double[]> especiesFGEM = new LinkedHashMap<>();
        evolucionFGEM.existenciasFGEM().forEach((especieFGEM, serieFGEM) ->
                especiesFGEM.put(especieFGEM.descripcionFGEM(), java.util.Arrays.stream(serieFGEM).asDoubleStream().toArray()));
        graficoExistenciasFGEM.getChildren().setAll(especiesFGEM.isEmpty() ? sinDatosFGEM()
                : com.businessanalyzer.ui.GraficosFGEM.visorFGEM(com.businessanalyzer.ui.GraficosFGEM.lineasMensualesFGEM(
                evolucionFGEM.mesesFGEM(), especiesFGEM, "#,##0")));

        Map<String, double[]> flujoFGEM = new LinkedHashMap<>();
        flujoFGEM.put("Nacimientos", java.util.Arrays.stream(evolucionFGEM.nacimientosFGEM()).asDoubleStream().toArray());
        flujoFGEM.put("Muertes", java.util.Arrays.stream(evolucionFGEM.muertesFGEM()).asDoubleStream().toArray());
        flujoFGEM.put("Ventas y consumo", java.util.Arrays.stream(evolucionFGEM.salidasFGEM()).asDoubleStream().toArray());
        graficoNacimientosFGEM.getChildren().setAll(com.businessanalyzer.ui.GraficosFGEM.visorFGEM(
                com.businessanalyzer.ui.GraficosFGEM.barrasMensualesFGEM(evolucionFGEM.mesesFGEM(), flujoFGEM, new java.awt.Color[]{
                        com.businessanalyzer.ui.GraficosFGEM.VERDE_FGEM, com.businessanalyzer.ui.GraficosFGEM.ROJO_FGEM,
                        com.businessanalyzer.ui.GraficosFGEM.AZUL_FGEM}, null, 0, "#,##0")));

        ProductoGanaderoFGEM elegidoFGEM = cmbProductoEvolucionFGEM.getValue();
        cmbProductoEvolucionFGEM.getItems().setAll(evolucionFGEM.produccionFGEM().keySet());
        cmbProductoEvolucionFGEM.setValue(evolucionFGEM.produccionFGEM().containsKey(elegidoFGEM) ? elegidoFGEM
                : cmbProductoEvolucionFGEM.getItems().isEmpty() ? null : cmbProductoEvolucionFGEM.getItems().get(0));
        cmbProductoEvolucionFGEM.setVisible(cmbProductoEvolucionFGEM.getItems().size() > 1);
        dibujarProduccionFGEM();

        lecturaEvolucionFGEM.getChildren().clear();
        for (String fraseFGEM : evolucionFGEM.lecturaFGEM()) {
            Label puntoFGEM = new Label("•  " + fraseFGEM);
            puntoFGEM.setWrapText(true);
            puntoFGEM.getStyleClass().add("lectura");
            lecturaEvolucionFGEM.getChildren().add(puntoFGEM);
        }
    }

    private void dibujarProduccionFGEM() {
        ProductoGanaderoFGEM productoFGEM = cmbProductoEvolucionFGEM.getValue();
        if (evolucionFGEM == null || productoFGEM == null) {
            graficoProduccionFGEM.getChildren().setAll(sinDatosFGEM());
            return;
        }
        double[] serieFGEM = evolucionFGEM.produccionFGEM().get(productoFGEM);
        int inicioFGEM = 0;
        while (inicioFGEM < serieFGEM.length && serieFGEM[inicioFGEM] == 0) {
            inicioFGEM++;
        }
        Map<String, double[]> datosFGEM = new LinkedHashMap<>();
        datosFGEM.put(productoFGEM.descripcionFGEM() + " (" + productoFGEM.unidadFGEM() + ")", serieFGEM);
        graficoProduccionFGEM.getChildren().setAll(com.businessanalyzer.ui.GraficosFGEM.visorFGEM(
                com.businessanalyzer.ui.GraficosFGEM.barrasMensualesFGEM(evolucionFGEM.mesesFGEM(), datosFGEM,
                        new java.awt.Color[]{com.businessanalyzer.ui.GraficosFGEM.AZUL_FGEM}, evolucionFGEM.tendenciaFGEM(productoFGEM),
                        inicioFGEM, "#,##0")));
    }

    private static Label sinDatosFGEM() {
        Label lFGEM = new Label("Sin datos en estos meses.");
        lFGEM.getStyleClass().add("muted");
        return lFGEM;
    }

    private Node tarjetaEspecieFGEM(IndicadoresEspecieFGEM iFGEM, IndicadoresEspecieFGEM previoFGEM) {
        Map<String, String> valoresPreviosFGEM = new java.util.HashMap<>();
        if (previoFGEM != null) {
            previoFGEM.evaluarFGEM(monedaFGEM).forEach(pFGEM -> valoresPreviosFGEM.put(pFGEM.nombreFGEM(), pFGEM.valorFGEM()));
        }
        Label tituloFGEM = new Label(iFGEM.especieFGEM().descripcionFGEM() + "  ·  " + iFGEM.existenciaFinalFGEM() + " animales");
        tituloFGEM.getStyleClass().add("card-title");
        Label movimientosTextoFGEM = new Label("Inicio: " + iFGEM.existenciaInicialFGEM() + "   Nacimientos: " + iFGEM.nacimientosFGEM()
                + "   Compras: " + iFGEM.comprasFGEM() + "   Ventas: " + iFGEM.ventasFGEM() + "   Muertes: " + iFGEM.muertesFGEM()
                + "   Consumo: " + iFGEM.consumoFGEM() + "   Final: " + iFGEM.existenciaFinalFGEM());
        movimientosTextoFGEM.getStyleClass().add("hint");
        movimientosTextoFGEM.setWrapText(true);

        GridPane grillaFGEM = new GridPane();
        grillaFGEM.setHgap(16);
        grillaFGEM.setVgap(8);
        ColumnConstraints c1FGEM = new ColumnConstraints();
        c1FGEM.setMinWidth(190);
        ColumnConstraints c2FGEM = new ColumnConstraints();
        c2FGEM.setMinWidth(170);
        ColumnConstraints c3FGEM = new ColumnConstraints();
        c3FGEM.setHgrow(Priority.ALWAYS);
        grillaFGEM.getColumnConstraints().setAll(c1FGEM, c2FGEM, c3FGEM);
        int filaFGEM = 0;
        for (IndicadorGanaderoFGEM indFGEM : iFGEM.evaluarFGEM(monedaFGEM)) {
            Label nombreFGEM = new Label(indFGEM.nombreFGEM());
            nombreFGEM.getStyleClass().add("info-key");
            String antesFGEM = valoresPreviosFGEM.get(indFGEM.nombreFGEM());
            Label valorFGEM = new Label(indFGEM.valorFGEM() + (antesFGEM == null || "—".equals(antesFGEM) ? "" : "  (" + antesFGEM + ")"));
            valorFGEM.getStyleClass().add("info-value");
            if (indFGEM.semaforoFGEM().evaluadoFGEM() || "—".equals(indFGEM.valorFGEM())) {
                valorFGEM.getStyleClass().add("semaforo-" + claseSemaforoFGEM(indFGEM.semaforoFGEM()));
            }
            valorFGEM.setStyle("-fx-font-weight: bold;");
            Label lecturaFGEM = new Label(indFGEM.lecturaFGEM());
            lecturaFGEM.getStyleClass().add("muted");
            lecturaFGEM.setWrapText(true);
            if (indFGEM.criterioFGEM() != null) {
                Tooltip ayudaFGEM = new Tooltip(indFGEM.criterioFGEM());
                nombreFGEM.setTooltip(ayudaFGEM);
                valorFGEM.setTooltip(ayudaFGEM);
                lecturaFGEM.setText(indFGEM.lecturaFGEM() + "  (" + indFGEM.criterioFGEM() + ")");
            }
            grillaFGEM.addRow(filaFGEM++, nombreFGEM, valorFGEM, lecturaFGEM);
        }
        VBox tarjetaFGEM = new VBox(10, tituloFGEM, movimientosTextoFGEM, grillaFGEM);
        tarjetaFGEM.getStyleClass().add("card");
        return tarjetaFGEM;
    }

    private static String claseSemaforoFGEM(SemaforoFGEM sFGEM) {
        return switch (sFGEM) {
            case VERDE -> "verde";
            case AMARILLO -> "amarillo";
            case ROJO -> "rojo";
            default -> "sin-datos";
        };
    }

    private void filtrarFGEM() {
        String especieFGEM = cmbEspecieFGEM.getValue();
        movimientosFiltradosFGEM.setPredicate(mFGEM -> especieFGEM == null || TODAS_FGEM.equals(especieFGEM)
                || mFGEM.especieFGEM().descripcionFGEM().equals(especieFGEM));
    }

    // ------------------------------------------------------------------ acciones

    @FXML
    private void nuevoMovimientoFGEM() {
        abrirFormularioMovimientoFGEM(null);
    }

    @FXML
    private void inventarioInicialFGEM() {
        abrirFormularioMovimientoFGEM(TipoMovimientoGanadoFGEM.INVENTARIO_INICIAL);
    }

    private void abrirFormularioMovimientoFGEM(TipoMovimientoGanadoFGEM tipoFGEM) {
        FormularioMovimientoFGEM formularioFGEM = new FormularioMovimientoFGEM(tipoFGEM);
        MovimientoGanadoFGEM[] creadoFGEM = new MovimientoGanadoFGEM[1];
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), tipoFGEM == TipoMovimientoGanadoFGEM.INVENTARIO_INICIAL
                        ? "Inventario inicial" : "Registrar movimiento del hato",
                tipoFGEM == TipoMovimientoGanadoFGEM.INVENTARIO_INICIAL
                        ? "Registra cuántos animales de cada categoría tenía al empezar. Repite para cada categoría."
                        : null,
                formularioFGEM.contenidoFGEM(), "Registrar", 540, () -> creadoFGEM[0] = servicioFGEM().registrarMovimientoFGEM(formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            MovimientoGanadoFGEM mFGEM = creadoFGEM[0];
            NotificadorFGEM.exitoFGEM(mFGEM.tipoFGEM().descripcionFGEM() + " registrado: " + mFGEM.cantidadFGEM() + " "
                    + mFGEM.categoriaFGEM().nombreCompletoFGEM().toLowerCase() + ".");
        }
    }

    @FXML
    private void anularMovimientoFGEM() {
        MovimientoGanadoFGEM mFGEM = tablaMovimientosFGEM.getSelectionModel().getSelectedItem();
        if (mFGEM == null || mFGEM.anuladoFGEM()) {
            return;
        }
        TextField motivoFGEM = new TextField();
        motivoFGEM.setPromptText("Ej.: registrado dos veces");
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Motivo *", motivoFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Anular movimiento",
                mFGEM.tipoFGEM().descripcionFGEM() + " de " + mFGEM.cantidadFGEM() + " " + mFGEM.categoriaFGEM().nombreCompletoFGEM().toLowerCase()
                        + " del " + FechasFGEM.paraMostrarFGEM(mFGEM.fechaFGEM()) + ". Las existencias se recalcularán.",
                grillaFGEM, "Anular", () -> servicioFGEM().anularMovimientoFGEM(mFGEM.idFGEM(), motivoFGEM.getText()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Movimiento anulado.");
        }
    }

    @FXML
    private void editarMovimientoFGEM() {
        MovimientoGanadoFGEM mFGEM = tablaMovimientosFGEM.getSelectionModel().getSelectedItem();
        if (mFGEM == null || mFGEM.anuladoFGEM()) {
            return;
        }
        FormularioMovimientoFGEM formularioFGEM = new FormularioMovimientoFGEM(mFGEM.tipoFGEM());
        formularioFGEM.cargarDesdeFGEM(mFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Editar movimiento",
                "El registro original queda anulado como historial y se guarda el corregido. "
                        + "Las existencias se recalculan solas.",
                formularioFGEM.contenidoFGEM(), "Guardar cambios", 540,
                () -> servicioFGEM().corregirMovimientoFGEM(mFGEM.idFGEM(), formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Movimiento corregido.");
        }
    }

    @FXML
    private void editarProduccionFGEM() {
        ProduccionGanaderaFGEM pFGEM = tablaProduccionFGEM.getSelectionModel().getSelectedItem();
        if (pFGEM == null || pFGEM.anuladoFGEM()) {
            return;
        }
        FormularioProduccionFGEM formularioFGEM = new FormularioProduccionFGEM();
        formularioFGEM.cargarDesdeFGEM(pFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Editar producción", null, formularioFGEM.contenidoFGEM(),
                "Guardar cambios", 520, () -> servicioFGEM().corregirProduccionFGEM(pFGEM.idFGEM(), formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            pestanasFGEM.getSelectionModel().select(2);
            NotificadorFGEM.exitoFGEM("Producción corregida.");
        }
    }

    @FXML
    private void nuevaProduccionFGEM() {
        FormularioProduccionFGEM formularioFGEM = new FormularioProduccionFGEM();
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Registrar producción", null, formularioFGEM.contenidoFGEM(),
                "Registrar", 520, () -> servicioFGEM().registrarProduccionFGEM(formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            pestanasFGEM.getSelectionModel().select(2);
            NotificadorFGEM.exitoFGEM("Producción registrada.");
        }
    }

    @FXML
    private void anularProduccionFGEM() {
        ProduccionGanaderaFGEM pFGEM = tablaProduccionFGEM.getSelectionModel().getSelectedItem();
        if (pFGEM == null || pFGEM.anuladoFGEM()) {
            return;
        }
        if (DialogosFGEM.confirmarFGEM(ventanaFGEM(), "Anular registro de producción",
                "Se anulará el registro de " + MontosFGEM.cantidadFGEM(pFGEM.cantidadFGEM()) + " " + pFGEM.productoFGEM().unidadFGEM() + " de "
                        + pFGEM.productoFGEM().descripcionFGEM().toLowerCase() + " del " + FechasFGEM.paraMostrarFGEM(pFGEM.fechaFGEM()) + ".")) {
            try {
                servicioFGEM().anularProduccionFGEM(pFGEM.idFGEM());
                cargarFGEM();
                NotificadorFGEM.exitoFGEM("Registro anulado.");
            } catch (RuntimeException eFGEM) {
                NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            }
        }
    }

    @FXML
    private void editarPreciosFGEM() {
        Map<CategoriaGanadoFGEM, BigDecimal> actualesFGEM;
        try {
            actualesFGEM = servicioFGEM().preciosFGEM();
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        // Solo las especies que tienen animales; si aún no hay ninguno, todas.
        List<EspecieFGEM> especiesFGEM = hatoActualFGEM.stream().map(eFGEM -> eFGEM.categoriaFGEM().especieFGEM()).distinct().toList();
        if (especiesFGEM.isEmpty()) {
            especiesFGEM = List.of(EspecieFGEM.values());
        }
        Map<CategoriaGanadoFGEM, TextField> camposFGEM = new LinkedHashMap<>();
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        for (EspecieFGEM especieFGEM : especiesFGEM) {
            Label tituloFGEM = new Label(especieFGEM.descripcionFGEM());
            tituloFGEM.getStyleClass().add("step-title");
            grillaFGEM.add(tituloFGEM, 0, grillaFGEM.getRowCount(), 2, 1);
            for (CategoriaGanadoFGEM cFGEM : CategoriaGanadoFGEM.deFGEM(especieFGEM)) {
                TextField campoFGEM = new TextField(actualesFGEM.containsKey(cFGEM) ? MontosFGEM.paraEditarFGEM(actualesFGEM.get(cFGEM)) : "");
                campoFGEM.setPromptText("Sin precio");
                camposFGEM.put(cFGEM, campoFGEM);
                FormulariosFGEM.filaFGEM(grillaFGEM, cFGEM.descripcionFGEM() + " (" + monedaFGEM.simboloFGEM() + ")", campoFGEM);
            }
        }
        ScrollPane desplazableFGEM = new ScrollPane(grillaFGEM);
        desplazableFGEM.setFitToWidth(true);
        desplazableFGEM.setPrefViewportHeight(360);
        desplazableFGEM.getStyleClass().add("dialog-scroll");
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Precios por cabeza",
                "Precio de mercado de un animal de cada categoría (lo que pagarían hoy en la feria o el camal). "
                        + "Deja vacío para no valorizar esa categoría.",
                desplazableFGEM, "Guardar precios", 480, () -> {
                    Map<CategoriaGanadoFGEM, BigDecimal> preciosFGEM = new EnumMap<>(CategoriaGanadoFGEM.class);
                    camposFGEM.forEach((cFGEM, campoFGEM) -> preciosFGEM.put(cFGEM, campoFGEM.getText() == null || campoFGEM.getText().isBlank()
                            ? null : MontosFGEM.leerFGEM(campoFGEM.getText(), "Precio de " + cFGEM.nombreCompletoFGEM().toLowerCase())));
                    servicioFGEM().guardarPreciosFGEM(preciosFGEM);
                });
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Precios guardados.");
        }
    }

    // ------------------------------------------------------------------ tablas

    private void configurarTablaHatoFGEM() {
        colHatoCategoriaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colHatoCategoriaFGEM.setCellFactory(colFGEM -> celdaHatoFGEM(FilaHatoFGEM::conceptoFGEM));
        colHatoCabezasFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colHatoCabezasFGEM.setCellFactory(colFGEM -> celdaHatoFGEM(fFGEM -> String.valueOf(fFGEM.cabezasFGEM())));
        colHatoPrecioFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colHatoPrecioFGEM.setCellFactory(colFGEM -> celdaHatoFGEM(fFGEM -> fFGEM.nivelFGEM() > 0 ? ""
                : fFGEM.precioFGEM() == null ? "Sin precio" : MontosFGEM.formatearFGEM(fFGEM.precioFGEM(), monedaFGEM)));
        colHatoValorFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colHatoValorFGEM.setCellFactory(colFGEM -> celdaHatoFGEM(fFGEM -> fFGEM.valorFGEM() == null ? "—" : MontosFGEM.formatearFGEM(fFGEM.valorFGEM(), monedaFGEM)));
        tablaHatoFGEM.setRowFactory(tFGEM -> new TableRow<>() {
            @Override
            protected void updateItem(FilaHatoFGEM filaFGEM, boolean vacioFGEM) {
                super.updateItem(filaFGEM, vacioFGEM);
                pseudoClassStateChanged(SUBTOTAL_FGEM, !vacioFGEM && filaFGEM != null && filaFGEM.nivelFGEM() == 1);
                pseudoClassStateChanged(RESULTADO_FGEM, !vacioFGEM && filaFGEM != null && filaFGEM.nivelFGEM() == 2);
            }
        });
    }

    private static TableCell<FilaHatoFGEM, FilaHatoFGEM> celdaHatoFGEM(Function<FilaHatoFGEM, String> textoFGEM) {
        return new TableCell<>() {
            @Override
            protected void updateItem(FilaHatoFGEM filaFGEM, boolean vacioFGEM) {
                super.updateItem(filaFGEM, vacioFGEM);
                setText(vacioFGEM || filaFGEM == null ? null : textoFGEM.apply(filaFGEM));
            }
        };
    }

    private void configurarTablaMovimientosFGEM() {
        colMovFechaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().fechaFGEM()));
        colMovFechaFGEM.setCellFactory(colFGEM -> celdaFechaFGEM());
        colMovTipoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().tipoFGEM().descripcionFGEM()
                + (cFGEM.getValue().anuladoFGEM() ? "  (anulado)" : "")));
        colMovCategoriaFGEM.setCellValueFactory(cFGEM -> {
            MovimientoGanadoFGEM mFGEM = cFGEM.getValue();
            return textoFGEM(mFGEM.categoriaFGEM().nombreCompletoFGEM()
                    + (mFGEM.categoriaDestinoFGEM() == null ? "" : " → " + mFGEM.categoriaDestinoFGEM().descripcionFGEM()));
        });
        colMovCantidadFGEM.setCellValueFactory(cFGEM -> {
            MovimientoGanadoFGEM mFGEM = cFGEM.getValue();
            String signoFGEM = mFGEM.tipoFGEM().signoFGEM() > 0 ? "+" : mFGEM.tipoFGEM().signoFGEM() < 0 ? "−" : "";
            return textoFGEM(signoFGEM + mFGEM.cantidadFGEM());
        });
        colMovMontoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().montoFGEM() == null ? ""
                : MontosFGEM.formatearFGEM(cFGEM.getValue().montoFGEM(), monedaFGEM)));
        colMovDetalleFGEM.setCellValueFactory(cFGEM -> {
            MovimientoGanadoFGEM mFGEM = cFGEM.getValue();
            List<String> partesFGEM = new ArrayList<>();
            if (mFGEM.causaFGEM() != null) {
                partesFGEM.add("Causa: " + mFGEM.causaFGEM());
            }
            if (mFGEM.observacionFGEM() != null) {
                partesFGEM.add(mFGEM.observacionFGEM());
            }
            return textoFGEM(String.join(" · ", partesFGEM));
        });
        tablaMovimientosFGEM.setItems(movimientosFiltradosFGEM);
        tablaMovimientosFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarAccionesFGEM());
        tablaMovimientosFGEM.setRowFactory(tFGEM -> new TableRow<>() {
            @Override
            protected void updateItem(MovimientoGanadoFGEM mFGEM, boolean vacioFGEM) {
                super.updateItem(mFGEM, vacioFGEM);
                boolean anuladoFGEM = !vacioFGEM && mFGEM != null && mFGEM.anuladoFGEM();
                pseudoClassStateChanged(ARCHIVADO_FGEM, anuladoFGEM);
                setTooltip(anuladoFGEM ? new Tooltip("Anulado: " + mFGEM.motivoAnulacionFGEM()) : null);
            }
        });
    }

    private void configurarTablaProduccionFGEM() {
        colProdFechaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().fechaFGEM()));
        colProdFechaFGEM.setCellFactory(colFGEM -> celdaFechaFGEM());
        colProdProductoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().productoFGEM().descripcionFGEM() + " ("
                + cFGEM.getValue().especieFGEM().descripcionFGEM().toLowerCase() + ")" + (cFGEM.getValue().anuladoFGEM() ? "  (anulado)" : "")));
        colProdCantidadFGEM.setCellValueFactory(cFGEM -> textoFGEM(MontosFGEM.cantidadFGEM(cFGEM.getValue().cantidadFGEM()) + " "
                + cFGEM.getValue().productoFGEM().abreviaturaFGEM()));
        colProdDiasFGEM.setCellValueFactory(cFGEM -> textoFGEM(String.valueOf(cFGEM.getValue().diasFGEM())));
        colProdAnimalesFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().animalesFGEM() == null ? "—"
                : String.valueOf(cFGEM.getValue().animalesFGEM())));
        colProdPromedioFGEM.setCellValueFactory(cFGEM -> {
            ProduccionGanaderaFGEM pFGEM = cFGEM.getValue();
            if (pFGEM.animalesFGEM() == null) {
                return textoFGEM("—");
            }
            int baseFGEM = pFGEM.animalesFGEM() * (pFGEM.productoFGEM().porDiaFGEM() ? pFGEM.diasFGEM() : 1);
            BigDecimal promedioFGEM = pFGEM.cantidadFGEM().divide(BigDecimal.valueOf(baseFGEM), 2, RoundingMode.HALF_UP);
            return textoFGEM(MontosFGEM.cantidadFGEM(promedioFGEM) + " " + pFGEM.productoFGEM().abreviaturaFGEM()
                    + (pFGEM.productoFGEM().porDiaFGEM() ? "/vaca/día" : "/animal"));
        });
        colProdValorFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().valorFGEM() == null ? ""
                : MontosFGEM.formatearFGEM(cFGEM.getValue().valorFGEM(), monedaFGEM)));
        tablaProduccionFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarAccionesFGEM());
        tablaProduccionFGEM.setRowFactory(tFGEM -> new TableRow<>() {
            @Override
            protected void updateItem(ProduccionGanaderaFGEM pFGEM, boolean vacioFGEM) {
                super.updateItem(pFGEM, vacioFGEM);
                pseudoClassStateChanged(ARCHIVADO_FGEM, !vacioFGEM && pFGEM != null && pFGEM.anuladoFGEM());
            }
        });
    }

    private void actualizarAccionesFGEM() {
        MovimientoGanadoFGEM mFGEM = tablaMovimientosFGEM.getSelectionModel().getSelectedItem();
        btnAnularMovimientoFGEM.setDisable(mFGEM == null || mFGEM.anuladoFGEM());
        btnEditarMovimientoFGEM.setDisable(mFGEM == null || mFGEM.anuladoFGEM());
        ProduccionGanaderaFGEM pFGEM = tablaProduccionFGEM.getSelectionModel().getSelectedItem();
        btnAnularProduccionFGEM.setDisable(pFGEM == null || pFGEM.anuladoFGEM());
        btnEditarProduccionFGEM.setDisable(pFGEM == null || pFGEM.anuladoFGEM());
    }

    private static <T> TableCell<T, LocalDate> celdaFechaFGEM() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fechaFGEM, boolean vacioFGEM) {
                super.updateItem(fechaFGEM, vacioFGEM);
                setText(vacioFGEM ? null : FechasFGEM.paraMostrarFGEM(fechaFGEM));
            }
        };
    }

    // ------------------------------------------------------------------ utilidades

    private GanaderiaServiceFGEM servicioFGEM() {
        return contextoFGEM.ganaderiaFGEM();
    }

    private Window ventanaFGEM() {
        return tablaHatoFGEM.getScene().getWindow();
    }

    private static ReadOnlyStringWrapper textoFGEM(String valorFGEM) {
        return new ReadOnlyStringWrapper(valorFGEM);
    }

    private static Integer enteroFGEM(String textoFGEM, String campoFGEM) {
        if (textoFGEM == null || textoFGEM.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(textoFGEM.strip());
        } catch (NumberFormatException eFGEM) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" debe ser un número entero.");
        }
    }

    private int existenciasFGEM(CategoriaGanadoFGEM cFGEM) {
        return hatoActualFGEM.stream().filter(eFGEM -> eFGEM.categoriaFGEM() == cFGEM).mapToInt(ExistenciaValorizadaFGEM::cabezasFGEM).sum();
    }

    /** Formulario de movimiento: los campos se activan según el tipo (valor en compras y ventas, causa en muertes...). */
    private final class FormularioMovimientoFGEM {
        private final ComboBox<TipoMovimientoGanadoFGEM> cmbTipoFGEM =
                new ComboBox<>(FXCollections.observableArrayList(TipoMovimientoGanadoFGEM.values()));
        private final Label lblAyudaFGEM = new Label();
        private final DatePicker dpFechaFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        private final ComboBox<EspecieFGEM> cmbEspecieFGEM = new ComboBox<>(FXCollections.observableArrayList(EspecieFGEM.values()));
        private final ComboBox<CategoriaGanadoFGEM> cmbCategoriaFGEM = new ComboBox<>();
        private final Label lblExistenciasFGEM = new Label();
        private final ComboBox<CategoriaGanadoFGEM> cmbDestinoFGEM = new ComboBox<>();
        private final TextField txtCantidadFGEM = new TextField();
        private final TextField txtMontoFGEM = new TextField();
        private final TextField txtCausaFGEM = new TextField();
        private final TextField txtObservacionFGEM = new TextField();

        FormularioMovimientoFGEM(TipoMovimientoGanadoFGEM tipoFGEM) {
            cmbTipoFGEM.setPromptText("Selecciona el movimiento");
            cmbTipoFGEM.setVisibleRowCount(10);
            cmbEspecieFGEM.setPromptText("Especie");
            cmbCategoriaFGEM.setPromptText("Categoría");
            cmbDestinoFGEM.setPromptText("Pasan a…");
            txtCantidadFGEM.setPromptText("Número de animales");
            txtMontoFGEM.setPromptText("Opcional");
            txtCausaFGEM.setPromptText("Opcional. Ej.: neumonía, helada, puma");
            txtObservacionFGEM.setPromptText("Opcional");
            for (Label lFGEM : List.of(lblAyudaFGEM, lblExistenciasFGEM)) {
                lFGEM.getStyleClass().add("hint");
                lFGEM.setWrapText(true);
                lFGEM.setMinHeight(Region.USE_PREF_SIZE);
            }
            cmbTipoFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarFGEM());
            cmbEspecieFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarFGEM());
            cmbCategoriaFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> {
                lblExistenciasFGEM.setText(nFGEM == null ? "" : "Existencias de hoy: " + existenciasFGEM(nFGEM) + " "
                        + nFGEM.descripcionFGEM().toLowerCase() + ".");
                actualizarDestinoFGEM();
            });
            if (cmbEspecieFGEM.getItems().size() > 0) {
                // Propone la especie con más animales.
                hatoActualFGEM.stream().max(java.util.Comparator.comparingInt(ExistenciaValorizadaFGEM::cabezasFGEM))
                        .ifPresent(eFGEM -> cmbEspecieFGEM.setValue(eFGEM.categoriaFGEM().especieFGEM()));
            }
            cmbTipoFGEM.setValue(tipoFGEM);
            if (tipoFGEM != null) {
                cmbTipoFGEM.setDisable(true);
            }
            actualizarFGEM();
        }

        private void actualizarFGEM() {
            TipoMovimientoGanadoFGEM tipoFGEM = cmbTipoFGEM.getValue();
            EspecieFGEM especieFGEM = cmbEspecieFGEM.getValue();
            lblAyudaFGEM.setText(tipoFGEM == null ? "" : tipoFGEM.ayudaFGEM());
            CategoriaGanadoFGEM anteriorFGEM = cmbCategoriaFGEM.getValue();
            List<CategoriaGanadoFGEM> categoriasFGEM = especieFGEM == null ? List.of() : CategoriaGanadoFGEM.deFGEM(especieFGEM).stream()
                    .filter(cFGEM -> tipoFGEM != TipoMovimientoGanadoFGEM.NACIMIENTO || cFGEM.criaFGEM()).toList();
            cmbCategoriaFGEM.getItems().setAll(categoriasFGEM);
            cmbCategoriaFGEM.setValue(categoriasFGEM.contains(anteriorFGEM) ? anteriorFGEM : null);
            boolean montoFGEM = tipoFGEM != null && tipoFGEM.llevaMontoFGEM();
            boolean causaFGEM = tipoFGEM != null && tipoFGEM.llevaCausaFGEM();
            txtMontoFGEM.setDisable(!montoFGEM);
            txtCausaFGEM.setDisable(!causaFGEM);
            if (!montoFGEM) {
                txtMontoFGEM.clear();
            }
            if (!causaFGEM) {
                txtCausaFGEM.clear();
            }
            actualizarDestinoFGEM();
        }

        private void actualizarDestinoFGEM() {
            boolean cambioFGEM = cmbTipoFGEM.getValue() == TipoMovimientoGanadoFGEM.CAMBIO_CATEGORIA;
            CategoriaGanadoFGEM origenFGEM = cmbCategoriaFGEM.getValue();
            cmbDestinoFGEM.setDisable(!cambioFGEM || origenFGEM == null);
            cmbDestinoFGEM.getItems().setAll(!cambioFGEM || origenFGEM == null ? List.of()
                    : CategoriaGanadoFGEM.deFGEM(origenFGEM.especieFGEM()).stream().filter(cFGEM -> cFGEM != origenFGEM).toList());
        }

        void cargarDesdeFGEM(MovimientoGanadoFGEM mFGEM) {
            dpFechaFGEM.setValue(mFGEM.fechaFGEM());
            cmbEspecieFGEM.setValue(mFGEM.categoriaFGEM().especieFGEM());
            cmbCategoriaFGEM.setValue(mFGEM.categoriaFGEM());
            cmbDestinoFGEM.setValue(mFGEM.categoriaDestinoFGEM());
            txtCantidadFGEM.setText(String.valueOf(mFGEM.cantidadFGEM()));
            txtMontoFGEM.setText(mFGEM.montoFGEM() == null ? "" : MontosFGEM.paraEditarFGEM(mFGEM.montoFGEM()));
            txtCausaFGEM.setText(mFGEM.causaFGEM() == null ? "" : mFGEM.causaFGEM());
            txtObservacionFGEM.setText(mFGEM.observacionFGEM() == null ? "" : mFGEM.observacionFGEM());
        }

        GridPane contenidoFGEM() {
            GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
            FormulariosFGEM.filaFGEM(grillaFGEM, "Movimiento *", cmbTipoFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "", lblAyudaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Fecha *", dpFechaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Especie *", cmbEspecieFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Categoría *", cmbCategoriaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "", lblExistenciasFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Pasan a", cmbDestinoFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Cantidad *", txtCantidadFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Valor total (" + monedaFGEM.simboloFGEM() + ")", txtMontoFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Causa", txtCausaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Observación", txtObservacionFGEM);
            return grillaFGEM;
        }

        DatosMovimientoGanadoFGEM datosFGEM() {
            LocalDate fechaFGEM = CamposFechaFGEM.leerFGEM(dpFechaFGEM, "el movimiento");
            BigDecimal montoFGEM = txtMontoFGEM.getText() == null || txtMontoFGEM.getText().isBlank() ? null
                    : MontosFGEM.leerFGEM(txtMontoFGEM.getText(), "Valor total");
            return new DatosMovimientoGanadoFGEM(fechaFGEM, cmbTipoFGEM.getValue(), cmbCategoriaFGEM.getValue(),
                    enteroFGEM(txtCantidadFGEM.getText(), "Cantidad"), cmbDestinoFGEM.getValue(), montoFGEM, txtCausaFGEM.getText(),
                    txtObservacionFGEM.getText());
        }
    }

    /** Formulario de producción de leche, fibra o lana. */
    private final class FormularioProduccionFGEM {
        private final ComboBox<EspecieFGEM> cmbEspecieFGEM = new ComboBox<>(FXCollections.observableArrayList(EspecieFGEM.values()));
        private final ComboBox<ProductoGanaderoFGEM> cmbProductoFGEM = new ComboBox<>();
        private final DatePicker dpFechaFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        private final TextField txtDiasFGEM = new TextField("1");
        private final TextField txtCantidadFGEM = new TextField();
        private final TextField txtAnimalesFGEM = new TextField();
        private final TextField txtPrecioFGEM = new TextField();
        private final TextField txtObservacionFGEM = new TextField();
        private final Label lblCantidadFGEM = new Label("Cantidad *");
        private final Label lblAnimalesFGEM = new Label("Animales");
        private final Label lblPrecioFGEM = new Label("Precio unitario");

        FormularioProduccionFGEM() {
            cmbEspecieFGEM.setPromptText("Especie");
            cmbProductoFGEM.setPromptText("Producto");
            txtDiasFGEM.setPromptText("1 = un día; 30 = el mes");
            txtCantidadFGEM.setPromptText("0.0");
            txtAnimalesFGEM.setPromptText("Opcional");
            txtPrecioFGEM.setPromptText("Opcional");
            txtObservacionFGEM.setPromptText("Opcional");
            cmbEspecieFGEM.valueProperty().addListener((oFGEM, aFGEM, especieFGEM) -> {
                cmbProductoFGEM.getItems().setAll(especieFGEM == null ? List.of() : ProductoGanaderoFGEM.deFGEM(especieFGEM));
                cmbProductoFGEM.setValue(cmbProductoFGEM.getItems().size() == 1 ? cmbProductoFGEM.getItems().get(0) : null);
            });
            cmbProductoFGEM.valueProperty().addListener((oFGEM, aFGEM, pFGEM) -> {
                lblCantidadFGEM.setText(pFGEM == null ? "Cantidad *" : "Cantidad (" + pFGEM.unidadFGEM() + ") *");
                lblAnimalesFGEM.setText(pFGEM == null ? "Animales" : capitalFGEM(pFGEM.animalesFGEM()));
                lblPrecioFGEM.setText(pFGEM == null ? "Precio unitario" : "Precio por " + pFGEM.abreviaturaFGEM() + " (" + monedaFGEM.simboloFGEM() + ")");
                txtDiasFGEM.setDisable(pFGEM != null && !pFGEM.porDiaFGEM());
            });
            hatoActualFGEM.stream().max(java.util.Comparator.comparingInt(ExistenciaValorizadaFGEM::cabezasFGEM))
                    .ifPresent(eFGEM -> cmbEspecieFGEM.setValue(eFGEM.categoriaFGEM().especieFGEM()));
        }

        void cargarDesdeFGEM(ProduccionGanaderaFGEM pFGEM) {
            cmbEspecieFGEM.setValue(pFGEM.especieFGEM());
            cmbProductoFGEM.setValue(pFGEM.productoFGEM());
            dpFechaFGEM.setValue(pFGEM.fechaFGEM());
            txtDiasFGEM.setText(String.valueOf(pFGEM.diasFGEM()));
            txtCantidadFGEM.setText(MontosFGEM.paraEditarFGEM(pFGEM.cantidadFGEM()));
            txtAnimalesFGEM.setText(pFGEM.animalesFGEM() == null ? "" : String.valueOf(pFGEM.animalesFGEM()));
            txtPrecioFGEM.setText(pFGEM.precioUnitarioFGEM() == null ? "" : MontosFGEM.paraEditarFGEM(pFGEM.precioUnitarioFGEM()));
            txtObservacionFGEM.setText(pFGEM.observacionFGEM() == null ? "" : pFGEM.observacionFGEM());
        }

        GridPane contenidoFGEM() {
            GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
            FormulariosFGEM.filaFGEM(grillaFGEM, "Especie *", cmbEspecieFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Producto *", cmbProductoFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Fecha *", dpFechaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Días que abarca", txtDiasFGEM);
            for (Label lFGEM : List.of(lblCantidadFGEM, lblAnimalesFGEM, lblPrecioFGEM)) {
                lFGEM.getStyleClass().add("form-label");
            }
            int filaFGEM = grillaFGEM.getRowCount();
            grillaFGEM.addRow(filaFGEM, lblCantidadFGEM, txtCantidadFGEM);
            grillaFGEM.addRow(filaFGEM + 1, lblAnimalesFGEM, txtAnimalesFGEM);
            grillaFGEM.addRow(filaFGEM + 2, lblPrecioFGEM, txtPrecioFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Observación", txtObservacionFGEM);
            FormulariosFGEM.notaFGEM(grillaFGEM, "Para la leche, «días» indica cuántos días suma el registro. La fibra y la lana "
                    + "se registran por esquila.");
            return grillaFGEM;
        }

        DatosProduccionFGEM datosFGEM() {
            LocalDate fechaFGEM = CamposFechaFGEM.leerFGEM(dpFechaFGEM, "la producción");
            BigDecimal precioFGEM = txtPrecioFGEM.getText() == null || txtPrecioFGEM.getText().isBlank() ? null
                    : MontosFGEM.leerFGEM(txtPrecioFGEM.getText(), "Precio");
            ProductoGanaderoFGEM productoFGEM = cmbProductoFGEM.getValue();
            Integer diasFGEM = productoFGEM != null && !productoFGEM.porDiaFGEM() ? Integer.valueOf(1) : enteroFGEM(txtDiasFGEM.getText(), "Días");
            return new DatosProduccionFGEM(cmbEspecieFGEM.getValue(), productoFGEM, fechaFGEM, diasFGEM,
                    txtCantidadFGEM.getText() == null || txtCantidadFGEM.getText().isBlank() ? null
                            : MontosFGEM.leerFGEM(txtCantidadFGEM.getText(), "Cantidad"),
                    enteroFGEM(txtAnimalesFGEM.getText(), "Animales"), precioFGEM, txtObservacionFGEM.getText());
        }

        private String capitalFGEM(String textoFGEM) {
            return Character.toUpperCase(textoFGEM.charAt(0)) + textoFGEM.substring(1);
        }
    }
}
