package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.tesoreria.ActividadFGEM;
import com.businessanalyzer.analytics.tesoreria.FlujoEfectivoFGEM;
import com.businessanalyzer.analytics.tesoreria.FlujoMensualFGEM;
import com.businessanalyzer.analytics.tesoreria.ProyeccionCajaFGEM;
import com.businessanalyzer.analytics.tesoreria.RubroFlujoFGEM;
import com.businessanalyzer.analytics.tesoreria.SaldoCuentaFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.model.CuentaDineroFGEM;
import com.businessanalyzer.model.DatosCuentaFGEM;
import com.businessanalyzer.model.DatosMovimientoTesoreriaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.MovimientoTesoreriaFGEM;
import com.businessanalyzer.model.OperacionFGEM;
import com.businessanalyzer.model.PagoOperacionFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.TipoCuentaFGEM;
import com.businessanalyzer.model.TipoMovimientoTesoreriaFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.service.TesoreriaServiceFGEM;
import com.businessanalyzer.ui.CamposFechaFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.FormulariosFGEM;
import com.businessanalyzer.ui.GraficosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.SelectorCuentaFGEM;
import com.businessanalyzer.ui.TarjetaIndicadorFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Módulo "Tesorería": cuentas de dinero con su saldo, transferencias, aportes y retiros, cuentas por
 * cobrar y por pagar, estado de flujo de efectivo y proyección de caja.
 */
public class TesoreriaControllerFGEM implements VistaModuloFGEM {

    private enum TipoFilaFGEM { TITULO, DETALLE, SUBTOTAL, RESULTADO }

    private record FilaFlujoFGEM(String conceptoFGEM, BigDecimal montoFGEM, TipoFilaFGEM tipoFGEM) {
    }

    private static final PseudoClass SUBTOTAL_FGEM = PseudoClass.getPseudoClass("subtotal");
    private static final PseudoClass RESULTADO_FGEM = PseudoClass.getPseudoClass("resultado");
    private static final PseudoClass ARCHIVADO_FGEM = PseudoClass.getPseudoClass("archivado");
    private static final int MESES_EVOLUCION_FGEM = 12;

    private final AppContextFGEM contextoFGEM;
    private final ToggleGroup grupoPendientesFGEM = new ToggleGroup();
    private MonedaFGEM monedaFGEM = MonedaFGEM.PEN;

    private final TarjetaIndicadorFGEM kpiDisponibleFGEM = new TarjetaIndicadorFGEM("Dinero disponible", "fth-dollar-sign");
    private final TarjetaIndicadorFGEM kpiCobrarFGEM = new TarjetaIndicadorFGEM("Por cobrar", "fth-arrow-down-circle");
    private final TarjetaIndicadorFGEM kpiPagarFGEM = new TarjetaIndicadorFGEM("Por pagar", "fth-arrow-up-circle");
    private final TarjetaIndicadorFGEM kpiProyeccionFGEM = new TarjetaIndicadorFGEM("Dinero en 6 meses", "fth-trending-up");

    @FXML private HBox indicadoresFGEM;

    @FXML private CheckBox chkInactivasFGEM;
    @FXML private Button btnEditarCuentaFGEM;
    @FXML private Button btnActivarCuentaFGEM;
    @FXML private Button btnNuevaCuentaFGEM;
    @FXML private TableView<SaldoCuentaFGEM> tablaCuentasFGEM;
    @FXML private TableColumn<SaldoCuentaFGEM, String> colCtaNombreFGEM;
    @FXML private TableColumn<SaldoCuentaFGEM, String> colCtaTipoFGEM;
    @FXML private TableColumn<SaldoCuentaFGEM, String> colCtaNumeroFGEM;
    @FXML private TableColumn<SaldoCuentaFGEM, BigDecimal> colCtaInicialFGEM;
    @FXML private TableColumn<SaldoCuentaFGEM, BigDecimal> colCtaEntradasFGEM;
    @FXML private TableColumn<SaldoCuentaFGEM, BigDecimal> colCtaSalidasFGEM;
    @FXML private TableColumn<SaldoCuentaFGEM, BigDecimal> colCtaSaldoFGEM;
    @FXML private TableColumn<SaldoCuentaFGEM, LocalDate> colCtaUltimoFGEM;

    @FXML private DatePicker dpMovDesdeFGEM;
    @FXML private DatePicker dpMovHastaFGEM;
    @FXML private CheckBox chkMovAnuladosFGEM;
    @FXML private Button btnAnularMovimientoFGEM;
    @FXML private Button btnNuevoMovimientoFGEM;
    @FXML private TableView<MovimientoTesoreriaFGEM> tablaMovimientosFGEM;
    @FXML private TableColumn<MovimientoTesoreriaFGEM, LocalDate> colMovFechaFGEM;
    @FXML private TableColumn<MovimientoTesoreriaFGEM, String> colMovTipoFGEM;
    @FXML private TableColumn<MovimientoTesoreriaFGEM, String> colMovOrigenFGEM;
    @FXML private TableColumn<MovimientoTesoreriaFGEM, String> colMovDestinoFGEM;
    @FXML private TableColumn<MovimientoTesoreriaFGEM, BigDecimal> colMovMontoFGEM;
    @FXML private TableColumn<MovimientoTesoreriaFGEM, String> colMovDescripcionFGEM;

    @FXML private ToggleButton tglCobrarFGEM;
    @FXML private ToggleButton tglPagarFGEM;
    @FXML private Button btnVerPagosFGEM;
    @FXML private Button btnRegistrarPagoFGEM;
    @FXML private TableView<OperacionFGEM> tablaPendientesFGEM;
    @FXML private TableColumn<OperacionFGEM, String> colPenNumeroFGEM;
    @FXML private TableColumn<OperacionFGEM, LocalDate> colPenFechaFGEM;
    @FXML private TableColumn<OperacionFGEM, String> colPenContraparteFGEM;
    @FXML private TableColumn<OperacionFGEM, BigDecimal> colPenTotalFGEM;
    @FXML private TableColumn<OperacionFGEM, BigDecimal> colPenPagadoFGEM;
    @FXML private TableColumn<OperacionFGEM, BigDecimal> colPenPendienteFGEM;
    @FXML private TableColumn<OperacionFGEM, LocalDate> colPenVenceFGEM;
    @FXML private TableColumn<OperacionFGEM, OperacionFGEM> colPenEstadoFGEM;
    @FXML private Label lblPendientesFGEM;

    @FXML private DatePicker dpFlujoDesdeFGEM;
    @FXML private DatePicker dpFlujoHastaFGEM;
    @FXML private TableView<FilaFlujoFGEM> tablaFlujoFGEM;
    @FXML private TableColumn<FilaFlujoFGEM, FilaFlujoFGEM> colFluConceptoFGEM;
    @FXML private TableColumn<FilaFlujoFGEM, FilaFlujoFGEM> colFluMontoFGEM;
    @FXML private Label lblFlujoFGEM;
    @FXML private StackPane graficoEvolucionFGEM;

    @FXML private HBox avisoProyeccionFGEM;
    @FXML private FontIcon iconoProyeccionFGEM;
    @FXML private Label lblProyeccionFGEM;
    @FXML private StackPane graficoProyeccionFGEM;
    @FXML private TableView<ProyeccionCajaFGEM.MesProyectadoFGEM> tablaProyeccionFGEM;
    @FXML private TableColumn<ProyeccionCajaFGEM.MesProyectadoFGEM, String> colProMesFGEM;
    @FXML private TableColumn<ProyeccionCajaFGEM.MesProyectadoFGEM, BigDecimal> colProOperacionFGEM;
    @FXML private TableColumn<ProyeccionCajaFGEM.MesProyectadoFGEM, BigDecimal> colProDeudasFGEM;
    @FXML private TableColumn<ProyeccionCajaFGEM.MesProyectadoFGEM, BigDecimal> colProSaldoFGEM;
    @FXML private TableColumn<ProyeccionCajaFGEM.MesProyectadoFGEM, BigDecimal> colProPesimistaFGEM;
    @FXML private Label lblMetodoFGEM;

    public TesoreriaControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        indicadoresFGEM.getChildren().addAll(kpiDisponibleFGEM, kpiCobrarFGEM, kpiPagarFGEM, kpiProyeccionFGEM);
        tglCobrarFGEM.setToggleGroup(grupoPendientesFGEM);
        tglPagarFGEM.setToggleGroup(grupoPendientesFGEM);
        grupoPendientesFGEM.selectedToggleProperty().addListener((oFGEM, anteriorFGEM, nuevoFGEM) -> {
            if (nuevoFGEM == null) {
                anteriorFGEM.setSelected(true);
            } else {
                cargarPendientesFGEM();
            }
        });

        LocalDate hoyFGEM = LocalDate.now();
        for (DatePicker selectorFGEM : List.of(dpMovDesdeFGEM, dpMovHastaFGEM, dpFlujoDesdeFGEM, dpFlujoHastaFGEM)) {
            CamposFechaFGEM.configurarFGEM(selectorFGEM);
        }
        dpMovDesdeFGEM.setValue(hoyFGEM.withDayOfMonth(1).minusMonths(2));
        dpMovHastaFGEM.setValue(hoyFGEM);
        dpFlujoDesdeFGEM.setValue(hoyFGEM.withDayOfMonth(1).minusMonths(3));
        dpFlujoHastaFGEM.setValue(hoyFGEM.withDayOfMonth(1).minusDays(1));
        dpMovDesdeFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarMovimientosFGEM());
        dpMovHastaFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarMovimientosFGEM());
        dpFlujoDesdeFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarFlujoFGEM());
        dpFlujoHastaFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarFlujoFGEM());

        configurarTablaCuentasFGEM();
        configurarTablaMovimientosFGEM();
        configurarTablaPendientesFGEM();
        configurarTablaFlujoFGEM();
        configurarTablaProyeccionFGEM();

        boolean gestionaFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        for (Node nodoFGEM : List.of(btnEditarCuentaFGEM, btnActivarCuentaFGEM, btnNuevaCuentaFGEM, btnAnularMovimientoFGEM,
                btnNuevoMovimientoFGEM)) {
            nodoFGEM.setVisible(gestionaFGEM);
            nodoFGEM.setManaged(gestionaFGEM);
        }
        boolean cobraFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.OPERACIONES_GESTIONAR);
        btnRegistrarPagoFGEM.setVisible(cobraFGEM);
        btnRegistrarPagoFGEM.setManaged(cobraFGEM);
    }

    @Override
    public void alMostrarFGEM() {
        cargarFGEM();
    }

    // ------------------------------------------------------------------ carga

    @FXML
    private void cargarFGEM() {
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        btnNuevaCuentaFGEM.setDisable(empresaFGEM == null);
        btnNuevoMovimientoFGEM.setDisable(empresaFGEM == null);
        if (empresaFGEM == null) {
            String mensajeFGEM = "Selecciona una empresa en la barra superior para ver su tesorería.";
            for (TableView<?> tablaFGEM : List.of(tablaCuentasFGEM, tablaMovimientosFGEM, tablaPendientesFGEM, tablaFlujoFGEM, tablaProyeccionFGEM)) {
                tablaFGEM.getItems().clear();
                tablaFGEM.setPlaceholder(new Label(mensajeFGEM));
            }
            for (TarjetaIndicadorFGEM kpiFGEM : List.of(kpiDisponibleFGEM, kpiCobrarFGEM, kpiPagarFGEM, kpiProyeccionFGEM)) {
                kpiFGEM.actualizarFGEM(TarjetaIndicadorFGEM.SIN_VALOR_FGEM, "");
            }
            graficoEvolucionFGEM.getChildren().clear();
            graficoProyeccionFGEM.getChildren().clear();
            lblProyeccionFGEM.setText(mensajeFGEM);
            return;
        }
        monedaFGEM = empresaFGEM.monedaFGEM();
        try {
            tablaCuentasFGEM.getItems().setAll(servicioFGEM().listarCuentasFGEM(chkInactivasFGEM.isSelected()));
            mostrarResumenFGEM(servicioFGEM().resumenFGEM());
        } catch (RuntimeException eFGEM) {
            tablaCuentasFGEM.getItems().clear();
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
        cargarMovimientosFGEM();
        cargarPendientesFGEM();
        cargarFlujoFGEM();
        cargarProyeccionFGEM();
        actualizarAccionesFGEM();
    }

    private void mostrarResumenFGEM(TesoreriaServiceFGEM.ResumenFGEM rFGEM) {
        int cuentasFGEM = (int) tablaCuentasFGEM.getItems().stream().filter(sFGEM -> sFGEM.cuentaFGEM().activaFGEM()).count();
        kpiDisponibleFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(rFGEM.disponibleFGEM(), monedaFGEM), rFGEM.cuentasNegativasFGEM() > 0
                ? rFGEM.cuentasNegativasFGEM() + (rFGEM.cuentasNegativasFGEM() == 1 ? " cuenta en negativo" : " cuentas en negativo")
                : "En " + cuentasFGEM + (cuentasFGEM == 1 ? " cuenta" : " cuentas"));
        kpiCobrarFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(rFGEM.porCobrarFGEM(), monedaFGEM), rFGEM.porCobrarVencidoFGEM().signum() > 0
                ? "Vencido: " + MontosFGEM.formatearFGEM(rFGEM.porCobrarVencidoFGEM(), monedaFGEM) : "Nada vencido");
        kpiPagarFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(rFGEM.porPagarFGEM(), monedaFGEM), rFGEM.porPagarVencidoFGEM().signum() > 0
                ? "Vencido: " + MontosFGEM.formatearFGEM(rFGEM.porPagarVencidoFGEM(), monedaFGEM) : "Nada vencido");
    }

    @FXML
    private void cargarMovimientosFGEM() {
        if (contextoFGEM.sesionFGEM().empresaActivaFGEM().isEmpty() || dpMovDesdeFGEM.getValue() == null || dpMovHastaFGEM.getValue() == null) {
            return;
        }
        tablaMovimientosFGEM.setPlaceholder(new Label("No hay transferencias, aportes ni retiros en el período."));
        try {
            tablaMovimientosFGEM.getItems().setAll(servicioFGEM().listarMovimientosFGEM(dpMovDesdeFGEM.getValue(), dpMovHastaFGEM.getValue(),
                    chkMovAnuladosFGEM.isSelected()));
        } catch (RuntimeException eFGEM) {
            tablaMovimientosFGEM.getItems().clear();
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
        actualizarAccionesFGEM();
    }

    private TipoOperacionFGEM tipoPendienteFGEM() {
        return tglPagarFGEM.isSelected() ? TipoOperacionFGEM.COMPRA : TipoOperacionFGEM.VENTA;
    }

    private void cargarPendientesFGEM() {
        TipoOperacionFGEM tipoFGEM = tipoPendienteFGEM();
        boolean ventaFGEM = tipoFGEM == TipoOperacionFGEM.VENTA;
        colPenContraparteFGEM.setText(tipoFGEM.contraparteFGEM());
        colPenPagadoFGEM.setText(ventaFGEM ? "Cobrado" : "Pagado");
        btnRegistrarPagoFGEM.setText(ventaFGEM ? "Registrar cobro" : "Registrar pago");
        btnVerPagosFGEM.setText(ventaFGEM ? "Ver cobros" : "Ver pagos");
        if (contextoFGEM.sesionFGEM().empresaActivaFGEM().isEmpty()) {
            return;
        }
        tablaPendientesFGEM.setPlaceholder(new Label(ventaFGEM ? "No hay ventas al crédito pendientes de cobro."
                : "No hay compras al crédito pendientes de pago."));
        try {
            List<OperacionFGEM> pendientesFGEM = servicioFGEM().pendientesFGEM(tipoFGEM);
            tablaPendientesFGEM.getItems().setAll(pendientesFGEM);
            LocalDate hoyFGEM = LocalDate.now();
            BigDecimal totalFGEM = pendientesFGEM.stream().map(OperacionFGEM::pendienteFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal vencidoFGEM = pendientesFGEM.stream().filter(oFGEM -> oFGEM.vencidaEnFGEM(hoyFGEM)).map(OperacionFGEM::pendienteFGEM)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            lblPendientesFGEM.setText(pendientesFGEM.size() + (pendientesFGEM.size() == 1 ? " operación" : " operaciones")
                    + " · pendiente " + MontosFGEM.formatearFGEM(totalFGEM, monedaFGEM) + " · vencido " + MontosFGEM.formatearFGEM(vencidoFGEM, monedaFGEM));
        } catch (RuntimeException eFGEM) {
            tablaPendientesFGEM.getItems().clear();
            lblPendientesFGEM.setText("");
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
        actualizarAccionesFGEM();
    }

    private void cargarFlujoFGEM() {
        if (contextoFGEM.sesionFGEM().empresaActivaFGEM().isEmpty() || dpFlujoDesdeFGEM.getValue() == null || dpFlujoHastaFGEM.getValue() == null) {
            return;
        }
        try {
            FlujoEfectivoFGEM flujoFGEM = servicioFGEM().flujoEfectivoFGEM(dpFlujoDesdeFGEM.getValue(), dpFlujoHastaFGEM.getValue());
            tablaFlujoFGEM.getItems().setAll(filasFlujoFGEM(flujoFGEM));
            tablaFlujoFGEM.setPlaceholder(new Label("Sin datos."));
            lblFlujoFGEM.setText(flujoFGEM.sinMovimientosFGEM()
                    ? "No hubo movimientos de dinero registrados en las cuentas durante el período."
                    : "Método directo: lo que realmente entró y salió de las cuentas (con impuestos), aunque la venta o "
                    + "el gasto sean de otro mes. Las transferencias entre cuentas no cambian el total y no se muestran.");
            List<FlujoMensualFGEM> evolucionFGEM = servicioFGEM().evolucionFGEM(MESES_EVOLUCION_FGEM);
            List<YearMonth> mesesFGEM = evolucionFGEM.stream().map(FlujoMensualFGEM::mesFGEM).toList();
            graficoEvolucionFGEM.getChildren().setAll(GraficosFGEM.visorFGEM(GraficosFGEM.flujoCajaFGEM(mesesFGEM,
                    serieFGEM(evolucionFGEM, FlujoMensualFGEM::entradasFGEM), serieFGEM(evolucionFGEM, FlujoMensualFGEM::salidasFGEM),
                    serieFGEM(evolucionFGEM, FlujoMensualFGEM::saldoCierreFGEM), monedaFGEM)));
        } catch (RuntimeException eFGEM) {
            tablaFlujoFGEM.getItems().clear();
            graficoEvolucionFGEM.getChildren().clear();
            lblFlujoFGEM.setText(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    private void cargarProyeccionFGEM() {
        avisoProyeccionFGEM.getStyleClass().remove("banner-aviso");
        try {
            ProyeccionCajaFGEM pFGEM = servicioFGEM().proyeccionFGEM();
            tablaProyeccionFGEM.getItems().setAll(pFGEM.mesesFGEM());
            if (!pFGEM.suficienteFGEM()) {
                kpiProyeccionFGEM.actualizarFGEM(TarjetaIndicadorFGEM.SIN_VALOR_FGEM, "Faltan meses de historia");
                iconoProyeccionFGEM.setIconLiteral("fth-info");
                lblProyeccionFGEM.setText("Para proyectar el dinero disponible se necesitan al menos "
                        + ProyeccionCajaFGEM.MINIMO_MESES_FGEM + " meses completos con movimientos en las cuentas (hay "
                        + pFGEM.mesesHistoriaFGEM() + "). Registra tus ventas, compras y gastos indicando su cuenta.");
                tablaProyeccionFGEM.setPlaceholder(new Label("Sin proyección todavía."));
                graficoProyeccionFGEM.getChildren().clear();
                lblMetodoFGEM.setText("");
                return;
            }
            BigDecimal alFinalFGEM = pFGEM.mesesFGEM().get(pFGEM.mesesFGEM().size() - 1).saldoFGEM();
            kpiProyeccionFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(alFinalFGEM, monedaFGEM), pFGEM.primerNegativoFGEM() != null
                    ? "Falta dinero en " + mesTextoFGEM(pFGEM.primerNegativoFGEM())
                    : pFGEM.primerNegativoPesimistaFGEM() != null ? "Riesgo en " + mesTextoFGEM(pFGEM.primerNegativoPesimistaFGEM())
                    : "Alcanza incluso en el peor caso");
            if (pFGEM.primerNegativoFGEM() != null) {
                avisoProyeccionFGEM.getStyleClass().add("banner-aviso");
                iconoProyeccionFGEM.setIconLiteral("fth-alert-triangle");
                lblProyeccionFGEM.setText("Con el ritmo actual, el dinero no alcanza: en " + mesTextoFGEM(pFGEM.primerNegativoFGEM())
                        + " las cuentas quedarían en negativo. Acelera los cobros, posterga compras no urgentes "
                        + "o negocia las cuotas antes de llegar a ese mes.");
            } else if (pFGEM.primerNegativoPesimistaFGEM() != null) {
                avisoProyeccionFGEM.getStyleClass().add("banner-aviso");
                iconoProyeccionFGEM.setIconLiteral("fth-alert-circle");
                lblProyeccionFGEM.setText("El dinero alcanza con el pronóstico base, pero en un escenario pesimista "
                        + "faltaría en " + mesTextoFGEM(pFGEM.primerNegativoPesimistaFGEM()) + ". Conviene armar una reserva.");
            } else {
                iconoProyeccionFGEM.setIconLiteral("fth-check-circle");
                lblProyeccionFGEM.setText("El dinero alcanza los próximos " + ProyeccionCajaFGEM.HORIZONTE_FGEM + " meses incluso "
                        + "en el escenario pesimista. Al final habría unos " + MontosFGEM.formatearFGEM(alFinalFGEM, monedaFGEM) + ".");
            }
            List<YearMonth> mesesFGEM = pFGEM.mesesFGEM().stream().map(ProyeccionCajaFGEM.MesProyectadoFGEM::mesFGEM).toList();
            double[] baseFGEM = pFGEM.mesesFGEM().stream().mapToDouble(mFGEM -> mFGEM.saldoFGEM().doubleValue()).toArray();
            double[] pesimistaFGEM = pFGEM.mesesFGEM().stream().mapToDouble(mFGEM -> mFGEM.saldoPesimistaFGEM().doubleValue()).toArray();
            graficoProyeccionFGEM.getChildren().setAll(GraficosFGEM.visorFGEM(GraficosFGEM.proyeccionCajaFGEM(mesesFGEM, baseFGEM, pesimistaFGEM, monedaFGEM)));
            lblMetodoFGEM.setText("Hoy hay " + MontosFGEM.formatearFGEM(pFGEM.saldoActualFGEM(), monedaFGEM) + ". Flujo de operación "
                    + "pronosticado con «" + pFGEM.pronosticoFGEM().elegidaFGEM().metodoFGEM().nombreFGEM() + "» (el de menor error en "
                    + pFGEM.mesesHistoriaFGEM() + " meses de historia; confiabilidad " + pFGEM.pronosticoFGEM().confiabilidadFGEM()
                    .descripcionFGEM().toLowerCase() + "). Del mes en curso solo cuenta lo que falta del mes; a las deudas "
                    + "se les descuenta el capital de cada cuota (los intereses ya están en el flujo de operación).");
        } catch (RuntimeException eFGEM) {
            tablaProyeccionFGEM.getItems().clear();
            graficoProyeccionFGEM.getChildren().clear();
            lblProyeccionFGEM.setText(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    private static double[] serieFGEM(List<FlujoMensualFGEM> mesesFGEM, Function<FlujoMensualFGEM, BigDecimal> valorFGEM) {
        return mesesFGEM.stream().mapToDouble(mFGEM -> valorFGEM.apply(mFGEM).doubleValue()).toArray();
    }

    private static List<FilaFlujoFGEM> filasFlujoFGEM(FlujoEfectivoFGEM fFGEM) {
        List<FilaFlujoFGEM> filasFGEM = new ArrayList<>();
        filasFGEM.add(new FilaFlujoFGEM("Dinero al inicio (" + FechasFGEM.paraMostrarFGEM(fFGEM.periodoFGEM().desdeFGEM()) + ")", fFGEM.saldoInicialFGEM(),
                TipoFilaFGEM.RESULTADO));
        for (ActividadFGEM actividadFGEM : List.of(ActividadFGEM.OPERACION, ActividadFGEM.INVERSION, ActividadFGEM.FINANCIAMIENTO)) {
            filasFGEM.add(new FilaFlujoFGEM(actividadFGEM.descripcionFGEM().toUpperCase(), null, TipoFilaFGEM.TITULO));
            boolean algunoFGEM = false;
            for (RubroFlujoFGEM rubroFGEM : RubroFlujoFGEM.values()) {
                if (rubroFGEM.actividadFGEM() == actividadFGEM && fFGEM.porRubroFGEM().containsKey(rubroFGEM)) {
                    filasFGEM.add(new FilaFlujoFGEM("    " + rubroFGEM.descripcionFGEM(), fFGEM.rubroFGEM(rubroFGEM), TipoFilaFGEM.DETALLE));
                    algunoFGEM = true;
                }
            }
            if (!algunoFGEM) {
                filasFGEM.add(new FilaFlujoFGEM("    Sin movimientos", BigDecimal.ZERO, TipoFilaFGEM.DETALLE));
            }
            filasFGEM.add(new FilaFlujoFGEM("Flujo neto de " + nombreCortoFGEM(actividadFGEM), fFGEM.netoFGEM(actividadFGEM), TipoFilaFGEM.SUBTOTAL));
        }
        filasFGEM.add(new FilaFlujoFGEM(fFGEM.variacionFGEM().signum() >= 0 ? "AUMENTO DEL DINERO" : "DISMINUCIÓN DEL DINERO",
                fFGEM.variacionFGEM(), TipoFilaFGEM.SUBTOTAL));
        filasFGEM.add(new FilaFlujoFGEM("Dinero al final (" + FechasFGEM.paraMostrarFGEM(fFGEM.periodoFGEM().hastaFGEM()) + ")", fFGEM.saldoFinalFGEM(),
                TipoFilaFGEM.RESULTADO));
        return filasFGEM;
    }

    private static String nombreCortoFGEM(ActividadFGEM actividadFGEM) {
        return switch (actividadFGEM) {
            case OPERACION -> "operación";
            case INVERSION -> "inversión";
            case FINANCIAMIENTO -> "financiamiento";
            case INTERNA -> "transferencias";
        };
    }

    @FXML
    private void ultimosTresMesesFGEM() {
        // Meses completos: con el mes en curso a medias, las compras del mes ya pagadas pesan más que las ventas.
        LocalDate hoyFGEM = LocalDate.now();
        dpFlujoHastaFGEM.setValue(hoyFGEM.withDayOfMonth(1).minusDays(1));
        dpFlujoDesdeFGEM.setValue(hoyFGEM.withDayOfMonth(1).minusMonths(3));
    }

    @FXML
    private void esteAnioFGEM() {
        LocalDate hoyFGEM = LocalDate.now();
        dpFlujoHastaFGEM.setValue(hoyFGEM);
        dpFlujoDesdeFGEM.setValue(hoyFGEM.withDayOfYear(1));
    }

    // ------------------------------------------------------------------ cuentas

    @FXML
    private void nuevaCuentaFGEM() {
        FormularioCuentaFGEM formularioFGEM = new FormularioCuentaFGEM(null);
        CuentaDineroFGEM[] creadaFGEM = new CuentaDineroFGEM[1];
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Nueva cuenta",
                "Una cuenta es un lugar donde la empresa guarda dinero: la caja, una cuenta bancaria o una billetera "
                        + "digital.", formularioFGEM.contenidoFGEM(), "Registrar",
                () -> creadaFGEM[0] = servicioFGEM().registrarCuentaFGEM(formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Cuenta registrada: " + creadaFGEM[0].nombreFGEM() + ".");
        }
    }

    @FXML
    private void editarCuentaFGEM() {
        SaldoCuentaFGEM sFGEM = tablaCuentasFGEM.getSelectionModel().getSelectedItem();
        if (sFGEM == null || !contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.FINANZAS_GESTIONAR)) {
            return;
        }
        FormularioCuentaFGEM formularioFGEM = new FormularioCuentaFGEM(sFGEM.cuentaFGEM());
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Editar cuenta", null, formularioFGEM.contenidoFGEM(), "Guardar cambios",
                () -> servicioFGEM().actualizarCuentaFGEM(sFGEM.cuentaFGEM().idFGEM(), formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Cuenta actualizada.");
        }
    }

    @FXML
    private void cambiarActivaFGEM() {
        SaldoCuentaFGEM sFGEM = tablaCuentasFGEM.getSelectionModel().getSelectedItem();
        if (sFGEM == null) {
            return;
        }
        boolean activarFGEM = !sFGEM.cuentaFGEM().activaFGEM();
        if (!DialogosFGEM.confirmarFGEM(ventanaFGEM(), activarFGEM ? "Activar cuenta" : "Desactivar cuenta", activarFGEM
                ? "«" + sFGEM.cuentaFGEM().nombreFGEM() + "» volverá a aparecer al registrar ventas, gastos y pagos."
                : "«" + sFGEM.cuentaFGEM().nombreFGEM() + "» dejará de aparecer al registrar. Su historial se conserva. "
                + "Solo se puede desactivar una cuenta con saldo cero.")) {
            return;
        }
        try {
            servicioFGEM().cambiarCuentaActivaFGEM(sFGEM.cuentaFGEM().idFGEM(), activarFGEM);
            cargarFGEM();
            NotificadorFGEM.exitoFGEM(activarFGEM ? "Cuenta activada." : "Cuenta desactivada.");
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    // ------------------------------------------------------------------ transferencias, aportes y retiros

    @FXML
    private void nuevoMovimientoFGEM() {
        List<CuentaDineroFGEM> cuentasFGEM;
        try {
            cuentasFGEM = servicioFGEM().cuentasActivasFGEM();
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        ComboBox<TipoMovimientoTesoreriaFGEM> tipoFGEM = new ComboBox<>(
                FXCollections.observableArrayList(TipoMovimientoTesoreriaFGEM.values()));
        tipoFGEM.setMaxWidth(Double.MAX_VALUE);
        DatePicker fechaFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        SelectorCuentaFGEM origenFGEM = new SelectorCuentaFGEM(cuentasFGEM, null, null);
        SelectorCuentaFGEM destinoFGEM = new SelectorCuentaFGEM(cuentasFGEM, null, cuentasFGEM.size() > 1 ? cuentasFGEM.get(1).idFGEM() : null);
        TextField montoFGEM = new TextField();
        montoFGEM.setPromptText("0.00");
        TextField descripcionFGEM = new TextField();
        descripcionFGEM.setPromptText("Opcional. Ej.: depósito de la venta de la semana");
        Label ayudaFGEM = new Label();
        ayudaFGEM.getStyleClass().add("hint");
        ayudaFGEM.setWrapText(true);
        tipoFGEM.valueProperty().addListener((oFGEM, aFGEM, tFGEM) -> {
            origenFGEM.setDisable(tFGEM == null || !tFGEM.usaOrigenFGEM());
            destinoFGEM.setDisable(tFGEM == null || !tFGEM.usaDestinoFGEM());
            ayudaFGEM.setText(tFGEM == null ? "" : switch (tFGEM) {
                case TRANSFERENCIA -> "El dinero cambia de lugar (por ejemplo, depositar el efectivo de la caja en el "
                        + "banco). El total no cambia.";
                case APORTE -> "El dueño pone dinero en el negocio. Aumenta el dinero, pero no es una venta ni un "
                        + "ingreso: no cambia la utilidad.";
                case RETIRO -> "El dueño saca dinero para uso personal. Disminuye el dinero, pero no es un gasto del "
                        + "negocio: no cambia la utilidad.";
            });
        });
        tipoFGEM.setValue(TipoMovimientoTesoreriaFGEM.TRANSFERENCIA);
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Tipo *", tipoFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "", ayudaFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Fecha *", fechaFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Sale de", origenFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Entra a", destinoFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Monto (" + monedaFGEM.simboloFGEM() + ") *", montoFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Descripción", descripcionFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Mover dinero", null, grillaFGEM, "Registrar",
                () -> servicioFGEM().registrarMovimientoFGEM(new DatosMovimientoTesoreriaFGEM(tipoFGEM.getValue(),
                        CamposFechaFGEM.leerFGEM(fechaFGEM, "el movimiento"), origenFGEM.cuentaIdFGEM(), destinoFGEM.cuentaIdFGEM(),
                        MontosFGEM.leerFGEM(montoFGEM.getText(), "Monto"), descripcionFGEM.getText())));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM(tipoFGEM.getValue().descripcionFGEM() + " registrado.");
        }
    }

    @FXML
    private void anularMovimientoFGEM() {
        MovimientoTesoreriaFGEM mFGEM = tablaMovimientosFGEM.getSelectionModel().getSelectedItem();
        if (mFGEM == null || mFGEM.anuladoFGEM()) {
            return;
        }
        TextField motivoFGEM = new TextField();
        motivoFGEM.setPromptText("Ej.: registrado dos veces");
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Motivo *", motivoFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Anular movimiento",
                mFGEM.tipoFGEM().descripcionFGEM() + " de " + MontosFGEM.formatearFGEM(mFGEM.montoFGEM(), monedaFGEM) + " del "
                        + FechasFGEM.paraMostrarFGEM(mFGEM.fechaFGEM()) + ": dejará de mover las cuentas, pero se conservará como anulado.",
                grillaFGEM, "Anular", () -> servicioFGEM().anularMovimientoFGEM(mFGEM.idFGEM(), motivoFGEM.getText()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Movimiento anulado.");
        }
    }

    // ------------------------------------------------------------------ cobros y pagos

    @FXML
    private void registrarPagoFGEM() {
        OperacionFGEM oFGEM = tablaPendientesFGEM.getSelectionModel().getSelectedItem();
        if (oFGEM == null || oFGEM.pendienteFGEM().signum() == 0) {
            return;
        }
        boolean ventaFGEM = oFGEM.tipoFGEM() == TipoOperacionFGEM.VENTA;
        DatePicker fechaFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        TextField montoFGEM = new TextField(MontosFGEM.paraEditarFGEM(oFGEM.pendienteFGEM()));
        SelectorCuentaFGEM cuentaFGEM = new SelectorCuentaFGEM(servicioFGEM().cuentasActivasFGEM(), null, null);
        TextField notaFGEM = new TextField();
        notaFGEM.setPromptText("Opcional. Ej.: adelanto, transferencia");
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Fecha *", fechaFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Monto (" + monedaFGEM.simboloFGEM() + ") *", montoFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, ventaFGEM ? "Entra a *" : "Sale de *", cuentaFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Nota", notaFGEM);
        FormulariosFGEM.notaFGEM(grillaFGEM, "Pendiente: " + MontosFGEM.formatearFGEM(oFGEM.pendienteFGEM(), monedaFGEM) + " de "
                + MontosFGEM.formatearFGEM(oFGEM.totalFGEM(), monedaFGEM) + ". Puedes registrar pagos parciales.");
        OperacionFGEM[] actualizadaFGEM = new OperacionFGEM[1];
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), (ventaFGEM ? "Cobrar " : "Pagar ") + oFGEM.numeroFGEM() + " · "
                        + Objects.requireNonNullElse(oFGEM.contraparteFGEM(), ""), null, grillaFGEM,
                ventaFGEM ? "Registrar cobro" : "Registrar pago",
                () -> {
                    if (cuentaFGEM.cuentaIdFGEM() == null) {
                        throw new com.businessanalyzer.exception.ValidacionExceptionFGEM("Elige la cuenta.");
                    }
                    actualizadaFGEM[0] = servicioFGEM().registrarPagoFGEM(oFGEM.idFGEM(), cuentaFGEM.cuentaIdFGEM(),
                            CamposFechaFGEM.leerFGEM(fechaFGEM, ventaFGEM ? "el cobro" : "el pago"), MontosFGEM.leerFGEM(montoFGEM.getText(), "Monto"),
                            notaFGEM.getText());
                });
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM(actualizadaFGEM[0].pendienteFGEM().signum() == 0
                    ? oFGEM.numeroFGEM() + (ventaFGEM ? " quedó cobrada por completo." : " quedó pagada por completo.")
                    : "Registrado. Falta " + MontosFGEM.formatearFGEM(actualizadaFGEM[0].pendienteFGEM(), monedaFGEM) + ".");
        }
    }

    @FXML
    private void verPagosFGEM() {
        OperacionFGEM oFGEM = tablaPendientesFGEM.getSelectionModel().getSelectedItem();
        if (oFGEM == null) {
            return;
        }
        boolean ventaFGEM = oFGEM.tipoFGEM() == TipoOperacionFGEM.VENTA;
        TableView<PagoOperacionFGEM> tablaFGEM = new TableView<>();
        tablaFGEM.setPrefHeight(260);
        tablaFGEM.setPlaceholder(new Label(ventaFGEM ? "Aún no hay cobros." : "Aún no hay pagos."));
        tablaFGEM.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaFGEM.getColumns().add(columnaFGEM("Fecha", 95, pFGEM -> FechasFGEM.paraMostrarFGEM(pFGEM.fechaFGEM()), false));
        tablaFGEM.getColumns().add(columnaFGEM("Monto", 110, pFGEM -> MontosFGEM.formatearFGEM(pFGEM.montoFGEM(), monedaFGEM), true));
        tablaFGEM.getColumns().add(columnaFGEM("Cuenta", 120, PagoOperacionFGEM::cuentaFGEM, false));
        tablaFGEM.getColumns().add(columnaFGEM("Nota", 150, pFGEM -> Objects.requireNonNullElse(pFGEM.notaFGEM(), ""), false));
        tablaFGEM.getColumns().add(columnaFGEM("Registró", 120, pFGEM -> pFGEM.anuladoFGEM() ? "Anulado por " + pFGEM.anuladoPorFGEM()
                : pFGEM.registradoPorFGEM(), false));
        tablaFGEM.setRowFactory(tFGEM -> new TableRow<>() {
            @Override
            protected void updateItem(PagoOperacionFGEM pFGEM, boolean vacioFGEM) {
                super.updateItem(pFGEM, vacioFGEM);
                pseudoClassStateChanged(ARCHIVADO_FGEM, !vacioFGEM && pFGEM != null && pFGEM.anuladoFGEM());
            }
        });
        Label resumenFGEM = new Label();
        resumenFGEM.getStyleClass().add("info-value");
        Runnable recargarFGEM = () -> {
            List<PagoOperacionFGEM> pagosFGEM = servicioFGEM().listarPagosFGEM(oFGEM.idFGEM());
            tablaFGEM.getItems().setAll(pagosFGEM);
            BigDecimal pagadoFGEM = pagosFGEM.stream().filter(pFGEM -> !pFGEM.anuladoFGEM()).map(PagoOperacionFGEM::montoFGEM)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            resumenFGEM.setText("Total: " + MontosFGEM.formatearFGEM(oFGEM.totalFGEM(), monedaFGEM) + "   ·   " + (ventaFGEM ? "Cobrado: " : "Pagado: ")
                    + MontosFGEM.formatearFGEM(pagadoFGEM, monedaFGEM) + "   ·   Pendiente: "
                    + MontosFGEM.formatearFGEM(oFGEM.totalFGEM().subtract(pagadoFGEM).max(BigDecimal.ZERO), monedaFGEM));
        };
        boolean[] cambiosFGEM = {false};
        VBox contenidoFGEM = new VBox(10, resumenFGEM, tablaFGEM);
        if (contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.OPERACIONES_GESTIONAR)) {
            Button anularFGEM = new Button(ventaFGEM ? "Anular cobro seleccionado" : "Anular pago seleccionado");
            anularFGEM.getStyleClass().add("button-secondary");
            anularFGEM.disableProperty().bind(tablaFGEM.getSelectionModel().selectedItemProperty().isNull());
            anularFGEM.setOnAction(eFGEM -> {
                PagoOperacionFGEM pFGEM = tablaFGEM.getSelectionModel().getSelectedItem();
                if (pFGEM == null || pFGEM.anuladoFGEM()) {
                    return;
                }
                if (DialogosFGEM.confirmarFGEM(tablaFGEM.getScene().getWindow(), ventaFGEM ? "Anular cobro" : "Anular pago",
                        "El registro de " + MontosFGEM.formatearFGEM(pFGEM.montoFGEM(), monedaFGEM) + " del " + FechasFGEM.paraMostrarFGEM(pFGEM.fechaFGEM())
                                + " se anulará: el dinero sale de «" + pFGEM.cuentaFGEM() + "» y lo pendiente vuelve a subir.")) {
                    try {
                        servicioFGEM().anularPagoFGEM(pFGEM.idFGEM());
                        cambiosFGEM[0] = true;
                        recargarFGEM.run();
                    } catch (RuntimeException exFGEM) {
                        NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(exFGEM));
                    }
                }
            });
            contenidoFGEM.getChildren().add(anularFGEM);
        }
        try {
            recargarFGEM.run();
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        DialogosFGEM.informacionFGEM(ventanaFGEM(), (ventaFGEM ? "Cobros de " : "Pagos de ") + oFGEM.numeroFGEM(),
                Objects.requireNonNullElse(oFGEM.contraparteFGEM(), ""), contenidoFGEM);
        if (cambiosFGEM[0]) {
            cargarFGEM();
        }
    }

    // ------------------------------------------------------------------ tablas

    private void configurarTablaCuentasFGEM() {
        colCtaNombreFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().cuentaFGEM().nombreFGEM()
                + (cFGEM.getValue().cuentaFGEM().activaFGEM() ? "" : "  (desactivada)")));
        colCtaTipoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().cuentaFGEM().tipoFGEM().descripcionFGEM()));
        colCtaNumeroFGEM.setCellValueFactory(cFGEM -> textoFGEM(Objects.requireNonNullElse(cFGEM.getValue().cuentaFGEM().numeroFGEM(), "—")));
        colCtaInicialFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().cuentaFGEM().saldoInicialFGEM()));
        colCtaInicialFGEM.setCellFactory(colFGEM -> celdaMontoFGEM(false));
        colCtaEntradasFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().entradasFGEM()));
        colCtaEntradasFGEM.setCellFactory(colFGEM -> celdaMontoFGEM(false));
        colCtaSalidasFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().salidasFGEM()));
        colCtaSalidasFGEM.setCellFactory(colFGEM -> celdaMontoFGEM(false));
        colCtaSaldoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().saldoFGEM()));
        colCtaSaldoFGEM.setCellFactory(colFGEM -> celdaMontoFGEM(true));
        colCtaUltimoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().ultimoMovimientoFGEM()));
        colCtaUltimoFGEM.setCellFactory(colFGEM -> celdaFechaFGEM());
        tablaCuentasFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarAccionesFGEM());
        tablaCuentasFGEM.setRowFactory(tFGEM -> {
            TableRow<SaldoCuentaFGEM> filaFGEM = new TableRow<>() {
                @Override
                protected void updateItem(SaldoCuentaFGEM sFGEM, boolean vacioFGEM) {
                    super.updateItem(sFGEM, vacioFGEM);
                    pseudoClassStateChanged(ARCHIVADO_FGEM, !vacioFGEM && sFGEM != null && !sFGEM.cuentaFGEM().activaFGEM());
                }
            };
            filaFGEM.setOnMouseClicked(eFGEM -> {
                if (eFGEM.getButton() == MouseButton.PRIMARY && eFGEM.getClickCount() == 2 && !filaFGEM.isEmpty()) {
                    editarCuentaFGEM();
                }
            });
            return filaFGEM;
        });
    }

    private void configurarTablaMovimientosFGEM() {
        colMovFechaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().fechaFGEM()));
        colMovFechaFGEM.setCellFactory(colFGEM -> celdaFechaFGEM());
        colMovTipoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().tipoFGEM().descripcionFGEM()
                + (cFGEM.getValue().anuladoFGEM() ? "  (anulado)" : "")));
        colMovOrigenFGEM.setCellValueFactory(cFGEM -> textoFGEM(Objects.requireNonNullElse(cFGEM.getValue().cuentaOrigenFGEM(), "—")));
        colMovDestinoFGEM.setCellValueFactory(cFGEM -> textoFGEM(Objects.requireNonNullElse(cFGEM.getValue().cuentaDestinoFGEM(), "—")));
        colMovMontoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().montoFGEM()));
        colMovMontoFGEM.setCellFactory(colFGEM -> celdaMontoFGEM(false));
        colMovDescripcionFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().descripcionFGEM()));
        tablaMovimientosFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarAccionesFGEM());
        tablaMovimientosFGEM.setRowFactory(tFGEM -> new TableRow<>() {
            @Override
            protected void updateItem(MovimientoTesoreriaFGEM mFGEM, boolean vacioFGEM) {
                super.updateItem(mFGEM, vacioFGEM);
                boolean anuladoFGEM = !vacioFGEM && mFGEM != null && mFGEM.anuladoFGEM();
                pseudoClassStateChanged(ARCHIVADO_FGEM, anuladoFGEM);
                setTooltip(anuladoFGEM ? new Tooltip("Anulado: " + mFGEM.motivoAnulacionFGEM()) : null);
            }
        });
    }

    private void configurarTablaPendientesFGEM() {
        colPenNumeroFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().numeroFGEM()));
        colPenFechaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().fechaFGEM()));
        colPenFechaFGEM.setCellFactory(colFGEM -> celdaFechaFGEM());
        colPenContraparteFGEM.setCellValueFactory(cFGEM -> textoFGEM(Objects.requireNonNullElse(cFGEM.getValue().contraparteFGEM(), "—")));
        colPenTotalFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().totalFGEM()));
        colPenTotalFGEM.setCellFactory(colFGEM -> celdaMontoFGEM(false));
        colPenPagadoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().pagadoFGEM()));
        colPenPagadoFGEM.setCellFactory(colFGEM -> celdaMontoFGEM(false));
        colPenPendienteFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().pendienteFGEM()));
        colPenPendienteFGEM.setCellFactory(colFGEM -> celdaMontoFGEM(false));
        colPenVenceFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().vencimientoFGEM()));
        colPenVenceFGEM.setCellFactory(colFGEM -> celdaFechaFGEM());
        colPenEstadoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colPenEstadoFGEM.setSortable(false);
        colPenEstadoFGEM.setCellFactory(colFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(OperacionFGEM oFGEM, boolean vacioFGEM) {
                super.updateItem(oFGEM, vacioFGEM);
                getStyleClass().removeAll("margen-negativo", "margen-bajo");
                if (vacioFGEM || oFGEM == null) {
                    setText(null);
                    return;
                }
                LocalDate hoyFGEM = LocalDate.now();
                if (oFGEM.vencimientoFGEM() == null) {
                    setText("Sin fecha límite");
                } else if (oFGEM.vencidaEnFGEM(hoyFGEM)) {
                    long diasFGEM = ChronoUnit.DAYS.between(oFGEM.vencimientoFGEM(), hoyFGEM);
                    setText("Vencida hace " + diasFGEM + (diasFGEM == 1 ? " día" : " días"));
                    getStyleClass().add("margen-negativo");
                } else {
                    long diasFGEM = ChronoUnit.DAYS.between(hoyFGEM, oFGEM.vencimientoFGEM());
                    setText(diasFGEM == 0 ? "Vence hoy" : "Vence en " + diasFGEM + (diasFGEM == 1 ? " día" : " días"));
                    if (diasFGEM <= 7) {
                        getStyleClass().add("margen-bajo");
                    }
                }
            }
        });
        tablaPendientesFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarAccionesFGEM());
        tablaPendientesFGEM.setRowFactory(tFGEM -> {
            TableRow<OperacionFGEM> filaFGEM = new TableRow<>();
            filaFGEM.setOnMouseClicked(eFGEM -> {
                if (eFGEM.getButton() == MouseButton.PRIMARY && eFGEM.getClickCount() == 2 && !filaFGEM.isEmpty()) {
                    verPagosFGEM();
                }
            });
            return filaFGEM;
        });
    }

    private void configurarTablaFlujoFGEM() {
        colFluConceptoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colFluConceptoFGEM.setCellFactory(colFGEM -> celdaFlujoFGEM(FilaFlujoFGEM::conceptoFGEM, false));
        colFluMontoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colFluMontoFGEM.setCellFactory(colFGEM -> celdaFlujoFGEM(fFGEM -> fFGEM.montoFGEM() == null ? "" : MontosFGEM.formatearFGEM(fFGEM.montoFGEM(), monedaFGEM), true));
        tablaFlujoFGEM.setRowFactory(tFGEM -> new TableRow<>() {
            @Override
            protected void updateItem(FilaFlujoFGEM filaFGEM, boolean vacioFGEM) {
                super.updateItem(filaFGEM, vacioFGEM);
                pseudoClassStateChanged(SUBTOTAL_FGEM, !vacioFGEM && filaFGEM != null
                        && (filaFGEM.tipoFGEM() == TipoFilaFGEM.SUBTOTAL || filaFGEM.tipoFGEM() == TipoFilaFGEM.TITULO));
                pseudoClassStateChanged(RESULTADO_FGEM, !vacioFGEM && filaFGEM != null && filaFGEM.tipoFGEM() == TipoFilaFGEM.RESULTADO);
            }
        });
    }

    private TableCell<FilaFlujoFGEM, FilaFlujoFGEM> celdaFlujoFGEM(Function<FilaFlujoFGEM, String> textoFGEM, boolean colorearFGEM) {
        return new TableCell<>() {
            @Override
            protected void updateItem(FilaFlujoFGEM filaFGEM, boolean vacioFGEM) {
                super.updateItem(filaFGEM, vacioFGEM);
                getStyleClass().remove("margen-negativo");
                if (vacioFGEM || filaFGEM == null) {
                    setText(null);
                    return;
                }
                setText(textoFGEM.apply(filaFGEM));
                if (colorearFGEM && filaFGEM.montoFGEM() != null && filaFGEM.montoFGEM().signum() < 0) {
                    getStyleClass().add("margen-negativo");
                }
            }
        };
    }

    private void configurarTablaProyeccionFGEM() {
        colProMesFGEM.setCellValueFactory(cFGEM -> textoFGEM(mesTextoFGEM(cFGEM.getValue().mesFGEM())
                + (cFGEM.getValue().mesFGEM().equals(YearMonth.now()) ? " (resto)" : "")));
        colProOperacionFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().operacionFGEM()));
        colProOperacionFGEM.setCellFactory(colFGEM -> celdaMontoFGEM(true));
        colProDeudasFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().deudasFGEM()));
        colProDeudasFGEM.setCellFactory(colFGEM -> celdaMontoFGEM(true));
        colProSaldoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().saldoFGEM()));
        colProSaldoFGEM.setCellFactory(colFGEM -> celdaMontoFGEM(true));
        colProPesimistaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().saldoPesimistaFGEM()));
        colProPesimistaFGEM.setCellFactory(colFGEM -> celdaMontoFGEM(true));
    }

    private void actualizarAccionesFGEM() {
        SaldoCuentaFGEM sFGEM = tablaCuentasFGEM.getSelectionModel().getSelectedItem();
        btnEditarCuentaFGEM.setDisable(sFGEM == null);
        btnActivarCuentaFGEM.setDisable(sFGEM == null);
        btnActivarCuentaFGEM.setText(sFGEM != null && !sFGEM.cuentaFGEM().activaFGEM() ? "Activar" : "Desactivar");
        MovimientoTesoreriaFGEM mFGEM = tablaMovimientosFGEM.getSelectionModel().getSelectedItem();
        btnAnularMovimientoFGEM.setDisable(mFGEM == null || mFGEM.anuladoFGEM());
        OperacionFGEM oFGEM = tablaPendientesFGEM.getSelectionModel().getSelectedItem();
        btnVerPagosFGEM.setDisable(oFGEM == null);
        btnRegistrarPagoFGEM.setDisable(oFGEM == null || oFGEM.pendienteFGEM().signum() == 0);
    }

    // ------------------------------------------------------------------ utilidades

    /** Celda de monto; con {@code colorear}, los negativos en rojo. */
    private <T> TableCell<T, BigDecimal> celdaMontoFGEM(boolean colorearFGEM) {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal montoFGEM, boolean vacioFGEM) {
                super.updateItem(montoFGEM, vacioFGEM);
                getStyleClass().remove("margen-negativo");
                setText(vacioFGEM ? null : montoFGEM == null ? "—" : MontosFGEM.formatearFGEM(montoFGEM, monedaFGEM));
                if (colorearFGEM && !vacioFGEM && montoFGEM != null && montoFGEM.signum() < 0) {
                    getStyleClass().add("margen-negativo");
                }
            }
        };
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

    private static TableColumn<PagoOperacionFGEM, String> columnaFGEM(String tituloFGEM, double anchoFGEM,
                                                              Function<PagoOperacionFGEM, String> valorFGEM, boolean numeroFGEM) {
        TableColumn<PagoOperacionFGEM, String> columnaFGEM = new TableColumn<>(tituloFGEM);
        columnaFGEM.setPrefWidth(anchoFGEM);
        columnaFGEM.setSortable(false);
        columnaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(valorFGEM.apply(cFGEM.getValue())));
        if (numeroFGEM) {
            columnaFGEM.getStyleClass().add("col-numero");
        }
        return columnaFGEM;
    }

    private static String mesTextoFGEM(YearMonth mesFGEM) {
        return FechasFGEM.nombreMesFGEM(mesFGEM.getMonthValue()) + " " + mesFGEM.getYear();
    }

    private static ReadOnlyStringWrapper textoFGEM(String valorFGEM) {
        return new ReadOnlyStringWrapper(valorFGEM);
    }

    private TesoreriaServiceFGEM servicioFGEM() {
        return contextoFGEM.tesoreriaFGEM();
    }

    private Window ventanaFGEM() {
        return tablaCuentasFGEM.getScene().getWindow();
    }

    /** Formulario de cuenta de dinero. */
    private final class FormularioCuentaFGEM {
        private final TextField txtNombreFGEM = new TextField();
        private final ComboBox<TipoCuentaFGEM> cmbTipoFGEM = new ComboBox<>(FXCollections.observableArrayList(TipoCuentaFGEM.values()));
        private final TextField txtNumeroFGEM = new TextField();
        private final TextField txtSaldoFGEM = new TextField();

        FormularioCuentaFGEM(CuentaDineroFGEM cFGEM) {
            txtNombreFGEM.setPromptText("Ej.: BCP Cuenta corriente, Yape, Caja chica");
            txtNumeroFGEM.setPromptText("Opcional: número de cuenta, CCI o celular");
            txtSaldoFGEM.setPromptText("0.00");
            cmbTipoFGEM.setMaxWidth(Double.MAX_VALUE);
            cmbTipoFGEM.setValue(TipoCuentaFGEM.BANCO);
            if (cFGEM != null) {
                txtNombreFGEM.setText(cFGEM.nombreFGEM());
                cmbTipoFGEM.setValue(cFGEM.tipoFGEM());
                txtNumeroFGEM.setText(Objects.requireNonNullElse(cFGEM.numeroFGEM(), ""));
                txtSaldoFGEM.setText(MontosFGEM.paraEditarFGEM(cFGEM.saldoInicialFGEM()));
            }
        }

        GridPane contenidoFGEM() {
            GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
            FormulariosFGEM.filaFGEM(grillaFGEM, "Nombre *", txtNombreFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Tipo *", cmbTipoFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Número", txtNumeroFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Saldo inicial (" + monedaFGEM.simboloFGEM() + ")", txtSaldoFGEM);
            FormulariosFGEM.notaFGEM(grillaFGEM, "Saldo inicial: el dinero que tenía la cuenta antes del primer movimiento que "
                    + "registres en el sistema. Desde ahí el saldo se calcula solo.");
            return grillaFGEM;
        }

        DatosCuentaFGEM datosFGEM() {
            return new DatosCuentaFGEM(txtNombreFGEM.getText(), cmbTipoFGEM.getValue(), txtNumeroFGEM.getText(),
                    txtSaldoFGEM.getText() == null || txtSaldoFGEM.getText().isBlank() ? BigDecimal.ZERO
                            : MontosFGEM.leerFGEM(txtSaldoFGEM.getText(), "Saldo inicial"));
        }
    }
}
