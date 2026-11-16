package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.patrimonio.BalanceGeneralFGEM;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneralFGEM.PartidaFGEM;
import com.businessanalyzer.analytics.patrimonio.DepreciacionFGEM;
import com.businessanalyzer.analytics.patrimonio.IndicadoresPatrimonioFGEM;
import com.businessanalyzer.analytics.patrimonio.LecturaPatrimonioFGEM;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonialFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.model.ActivoFGEM;
import com.businessanalyzer.model.CategoriaActivoFGEM;
import com.businessanalyzer.model.CategoriaDeudaFGEM;
import com.businessanalyzer.model.DatosActivoFGEM;
import com.businessanalyzer.model.DatosDeudaFGEM;
import com.businessanalyzer.model.DeudaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.PagoDeudaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.service.PatrimonioServiceFGEM;
import com.businessanalyzer.ui.CamposFechaFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.FormulariosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.SelectorCuentaFGEM;
import com.businessanalyzer.ui.TarjetaIndicadorFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/** Módulo "Activos y deudas": registro de bienes y deudas, balance general e indicadores patrimoniales. */
public class PatrimonioControllerFGEM implements VistaModuloFGEM {

    private enum TipoFilaFGEM { TITULO, DETALLE, SUBTOTAL, RESULTADO }

    private record FilaBalanceFGEM(String conceptoFGEM, BigDecimal montoFGEM, TipoFilaFGEM tipoFGEM) {
    }

    private static final PseudoClass SUBTOTAL_FGEM = PseudoClass.getPseudoClass("subtotal");
    private static final PseudoClass RESULTADO_FGEM = PseudoClass.getPseudoClass("resultado");
    private static final PseudoClass ARCHIVADO_FGEM = PseudoClass.getPseudoClass("archivado");

    private final AppContextFGEM contextoFGEM;
    private MonedaFGEM monedaFGEM = MonedaFGEM.PEN;
    private BigDecimal totalActivoFGEM = BigDecimal.ZERO;

    private final ObservableList<ActivoFGEM> activosFGEM = FXCollections.observableArrayList();
    private final FilteredList<ActivoFGEM> activosFiltradosFGEM = new FilteredList<>(activosFGEM);
    private final ObservableList<DeudaFGEM> deudasFGEM = FXCollections.observableArrayList();
    private final FilteredList<DeudaFGEM> deudasFiltradasFGEM = new FilteredList<>(deudasFGEM);

    private final TarjetaIndicadorFGEM kpiActivosFGEM = new TarjetaIndicadorFGEM("Total activos", "fth-box");
    private final TarjetaIndicadorFGEM kpiDeudasFGEM = new TarjetaIndicadorFGEM("Total deudas", "fth-credit-card");
    private final TarjetaIndicadorFGEM kpiPatrimonioFGEM = new TarjetaIndicadorFGEM("Patrimonio", "fth-shield");
    private final TarjetaIndicadorFGEM kpiLiquidezFGEM = new TarjetaIndicadorFGEM("Liquidez corriente", "fth-droplet");

    @FXML private HBox indicadoresFGEM;
    @FXML private TableView<FilaBalanceFGEM> tablaBalanceFGEM;
    @FXML private TableColumn<FilaBalanceFGEM, FilaBalanceFGEM> colBalConceptoFGEM;
    @FXML private TableColumn<FilaBalanceFGEM, FilaBalanceFGEM> colBalMontoFGEM;
    @FXML private TableColumn<FilaBalanceFGEM, FilaBalanceFGEM> colBalPorcentajeFGEM;
    @FXML private Label lblCorteFGEM;
    @FXML private GridPane gridIndicadoresFGEM;
    @FXML private VBox lecturaFGEM;

    @FXML private TextField txtBuscarActivoFGEM;
    @FXML private CheckBox chkBajasFGEM;
    @FXML private Button btnEditarActivoFGEM;
    @FXML private Button btnBajaActivoFGEM;
    @FXML private Button btnNuevoActivoFGEM;
    @FXML private TableView<ActivoFGEM> tablaActivosFGEM;
    @FXML private TableColumn<ActivoFGEM, String> colActTipoFGEM;
    @FXML private TableColumn<ActivoFGEM, String> colActDescripcionFGEM;
    @FXML private TableColumn<ActivoFGEM, LocalDate> colActFechaFGEM;
    @FXML private TableColumn<ActivoFGEM, BigDecimal> colActValorFGEM;
    @FXML private TableColumn<ActivoFGEM, String> colActVidaFGEM;
    @FXML private TableColumn<ActivoFGEM, BigDecimal> colActMensualFGEM;
    @FXML private TableColumn<ActivoFGEM, BigDecimal> colActDepreciacionFGEM;
    @FXML private TableColumn<ActivoFGEM, BigDecimal> colActLibrosFGEM;

    @FXML private TextField txtBuscarDeudaFGEM;
    @FXML private CheckBox chkAnuladasFGEM;
    @FXML private Button btnVerPagosFGEM;
    @FXML private Button btnEditarDeudaFGEM;
    @FXML private Button btnAnularDeudaFGEM;
    @FXML private Button btnPagarFGEM;
    @FXML private Button btnNuevaDeudaFGEM;
    @FXML private TableView<DeudaFGEM> tablaDeudasFGEM;
    @FXML private TableColumn<DeudaFGEM, String> colDeuAcreedorFGEM;
    @FXML private TableColumn<DeudaFGEM, String> colDeuTipoFGEM;
    @FXML private TableColumn<DeudaFGEM, LocalDate> colDeuFechaFGEM;
    @FXML private TableColumn<DeudaFGEM, BigDecimal> colDeuMontoFGEM;
    @FXML private TableColumn<DeudaFGEM, BigDecimal> colDeuSaldoFGEM;
    @FXML private TableColumn<DeudaFGEM, BigDecimal> colDeuCuotaFGEM;
    @FXML private TableColumn<DeudaFGEM, String> colDeuTasaFGEM;
    @FXML private TableColumn<DeudaFGEM, LocalDate> colDeuVenceFGEM;
    @FXML private TableColumn<DeudaFGEM, String> colDeuEstadoFGEM;

    public PatrimonioControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        indicadoresFGEM.getChildren().addAll(kpiActivosFGEM, kpiDeudasFGEM, kpiPatrimonioFGEM, kpiLiquidezFGEM);

        ColumnConstraints conceptosFGEM = new ColumnConstraints();
        conceptosFGEM.setMinWidth(190);
        ColumnConstraints valoresFGEM = new ColumnConstraints();
        valoresFGEM.setHgrow(Priority.ALWAYS);
        gridIndicadoresFGEM.getColumnConstraints().setAll(conceptosFGEM, valoresFGEM);

        configurarTablaBalanceFGEM();
        configurarTablaActivosFGEM();
        configurarTablaDeudasFGEM();
        txtBuscarActivoFGEM.textProperty().addListener((oFGEM, aFGEM, nFGEM) -> filtrarFGEM());
        txtBuscarDeudaFGEM.textProperty().addListener((oFGEM, aFGEM, nFGEM) -> filtrarFGEM());

        boolean gestionaFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        for (Node nodoFGEM : List.of(btnEditarActivoFGEM, btnBajaActivoFGEM, btnNuevoActivoFGEM, btnEditarDeudaFGEM, btnAnularDeudaFGEM,
                btnPagarFGEM, btnNuevaDeudaFGEM)) {
            nodoFGEM.setVisible(gestionaFGEM);
            nodoFGEM.setManaged(gestionaFGEM);
        }
    }

    @Override
    public void alMostrarFGEM() {
        cargarFGEM();
    }

    // ------------------------------------------------------------------ carga

    @FXML
    private void cargarFGEM() {
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        btnNuevoActivoFGEM.setDisable(empresaFGEM == null);
        btnNuevaDeudaFGEM.setDisable(empresaFGEM == null);
        if (empresaFGEM == null) {
            activosFGEM.clear();
            deudasFGEM.clear();
            tablaBalanceFGEM.getItems().clear();
            String mensajeFGEM = "Selecciona una empresa en la barra superior para ver sus activos y deudas.";
            tablaBalanceFGEM.setPlaceholder(new Label(mensajeFGEM));
            tablaActivosFGEM.setPlaceholder(new Label(mensajeFGEM));
            tablaDeudasFGEM.setPlaceholder(new Label(mensajeFGEM));
            limpiarIndicadoresFGEM();
            return;
        }
        monedaFGEM = empresaFGEM.monedaFGEM();
        tablaActivosFGEM.setPlaceholder(new Label("No hay activos registrados. Empieza por los equipos y muebles del negocio; "
                + "el dinero se registra en «Tesorería»."));
        tablaDeudasFGEM.setPlaceholder(new Label("No hay deudas registradas."));
        try {
            PatrimonioServiceFGEM servicioFGEM = servicioFGEM();
            activosFGEM.setAll(servicioFGEM.listarActivosFGEM(chkBajasFGEM.isSelected()));
            deudasFGEM.setAll(servicioFGEM.listarDeudasFGEM(chkAnuladasFGEM.isSelected()));
            mostrarSituacionFGEM(servicioFGEM.situacionFGEM());
        } catch (RuntimeException eFGEM) {
            activosFGEM.clear();
            deudasFGEM.clear();
            limpiarIndicadoresFGEM();
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
        filtrarFGEM();
        actualizarAccionesFGEM();
    }

    private void limpiarIndicadoresFGEM() {
        for (TarjetaIndicadorFGEM kpiFGEM : List.of(kpiActivosFGEM, kpiDeudasFGEM, kpiPatrimonioFGEM, kpiLiquidezFGEM)) {
            kpiFGEM.actualizarFGEM(TarjetaIndicadorFGEM.SIN_VALOR_FGEM, "");
        }
        gridIndicadoresFGEM.getChildren().clear();
        lecturaFGEM.getChildren().clear();
        lblCorteFGEM.setText("");
    }

    private void mostrarSituacionFGEM(SituacionPatrimonialFGEM sFGEM) {
        BalanceGeneralFGEM bFGEM = sFGEM.balanceFGEM();
        IndicadoresPatrimonioFGEM iFGEM = sFGEM.indicadoresFGEM();
        totalActivoFGEM = bFGEM.totalActivoFGEM();

        kpiActivosFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(bFGEM.totalActivoFGEM(), monedaFGEM),
                "Corriente: " + MontosFGEM.formatearFGEM(bFGEM.totalActivoCorrienteFGEM(), monedaFGEM));
        kpiDeudasFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(bFGEM.totalPasivoFGEM(), monedaFGEM),
                bFGEM.deudasPendientesFGEM() + (bFGEM.deudasPendientesFGEM() == 1 ? " deuda pendiente" : " deudas pendientes"));
        kpiPatrimonioFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(bFGEM.patrimonioFGEM(), monedaFGEM),
                iFGEM.endeudamientoFGEM() == null ? "Lo que pertenece a los dueños"
                        : "Deudas: " + MontosFGEM.porcentajeFGEM(iFGEM.endeudamientoFGEM()) + " de los activos");
        kpiLiquidezFGEM.actualizarFGEM(iFGEM.liquidezCorrienteFGEM() == null ? TarjetaIndicadorFGEM.SIN_VALOR_FGEM
                        : IndicadoresPatrimonioFGEM.vecesFGEM(iFGEM.liquidezCorrienteFGEM()),
                iFGEM.liquidezCorrienteFGEM() == null ? "Sin deudas de corto plazo" : "Activo ÷ pasivo corriente");

        tablaBalanceFGEM.getItems().setAll(filasBalanceFGEM(bFGEM));
        tablaBalanceFGEM.setPlaceholder(new Label("Sin datos."));
        lblCorteFGEM.setText("Balance al " + FechasFGEM.paraMostrarFGEM(bFGEM.corteFGEM()) + ". Inventario según el stock y el costo actuales; "
                + "activos fijos a su valor actual (ya descontada la depreciación).");
        mostrarIndicadoresFGEM(sFGEM);

        lecturaFGEM.getChildren().clear();
        for (String fraseFGEM : LecturaPatrimonioFGEM.generarFGEM(sFGEM, monedaFGEM)) {
            Label puntoFGEM = new Label("•  " + fraseFGEM);
            puntoFGEM.setWrapText(true);
            puntoFGEM.getStyleClass().add("lectura");
            lecturaFGEM.getChildren().add(puntoFGEM);
        }
    }

    private static List<FilaBalanceFGEM> filasBalanceFGEM(BalanceGeneralFGEM bFGEM) {
        List<FilaBalanceFGEM> filasFGEM = new ArrayList<>();
        filasFGEM.add(new FilaBalanceFGEM("ACTIVO", null, TipoFilaFGEM.TITULO));
        detalleFGEM(filasFGEM, bFGEM.activoCorrienteFGEM());
        filasFGEM.add(new FilaBalanceFGEM("Activo corriente", bFGEM.totalActivoCorrienteFGEM(), TipoFilaFGEM.SUBTOTAL));
        detalleFGEM(filasFGEM, bFGEM.activoNoCorrienteFGEM());
        filasFGEM.add(new FilaBalanceFGEM("Activo no corriente", bFGEM.totalActivoNoCorrienteFGEM(), TipoFilaFGEM.SUBTOTAL));
        filasFGEM.add(new FilaBalanceFGEM("TOTAL ACTIVO", bFGEM.totalActivoFGEM(), TipoFilaFGEM.RESULTADO));
        filasFGEM.add(new FilaBalanceFGEM("PASIVO", null, TipoFilaFGEM.TITULO));
        detalleFGEM(filasFGEM, bFGEM.pasivoCorrienteFGEM());
        filasFGEM.add(new FilaBalanceFGEM("Pasivo corriente (vence en 12 meses)", bFGEM.totalPasivoCorrienteFGEM(), TipoFilaFGEM.SUBTOTAL));
        detalleFGEM(filasFGEM, bFGEM.pasivoNoCorrienteFGEM());
        filasFGEM.add(new FilaBalanceFGEM("Pasivo no corriente", bFGEM.totalPasivoNoCorrienteFGEM(), TipoFilaFGEM.SUBTOTAL));
        filasFGEM.add(new FilaBalanceFGEM("TOTAL PASIVO", bFGEM.totalPasivoFGEM(), TipoFilaFGEM.SUBTOTAL));
        filasFGEM.add(new FilaBalanceFGEM("PATRIMONIO (activo − pasivo)", bFGEM.patrimonioFGEM(), TipoFilaFGEM.RESULTADO));
        filasFGEM.add(new FilaBalanceFGEM("TOTAL PASIVO + PATRIMONIO", bFGEM.totalPasivoFGEM().add(bFGEM.patrimonioFGEM()), TipoFilaFGEM.RESULTADO));
        return filasFGEM;
    }

    private static void detalleFGEM(List<FilaBalanceFGEM> filasFGEM, List<PartidaFGEM> partidasFGEM) {
        for (PartidaFGEM pFGEM : partidasFGEM) {
            filasFGEM.add(new FilaBalanceFGEM("    " + pFGEM.conceptoFGEM(), pFGEM.montoFGEM(), TipoFilaFGEM.DETALLE));
        }
    }

    private void mostrarIndicadoresFGEM(SituacionPatrimonialFGEM sFGEM) {
        BalanceGeneralFGEM bFGEM = sFGEM.balanceFGEM();
        IndicadoresPatrimonioFGEM iFGEM = sFGEM.indicadoresFGEM();
        gridIndicadoresFGEM.getChildren().clear();
        int filaFGEM = 0;
        filaFGEM = indicadorFGEM(filaFGEM, "Liquidez corriente", iFGEM.liquidezCorrienteFGEM() == null ? "Sin deudas de corto plazo"
                        : IndicadoresPatrimonioFGEM.vecesFGEM(iFGEM.liquidezCorrienteFGEM()),
                semaforoMayorFGEM(iFGEM.liquidezCorrienteFGEM(), IndicadoresPatrimonioFGEM.LIQUIDEZ_VERDE_FGEM, IndicadoresPatrimonioFGEM.LIQUIDEZ_AMARILLO_FGEM),
                "Activo corriente ÷ pasivo corriente. Bien desde 1.50; riesgo por debajo de 1.00.");
        filaFGEM = indicadorFGEM(filaFGEM, "Prueba ácida", iFGEM.pruebaAcidaFGEM() == null ? "—" : IndicadoresPatrimonioFGEM.vecesFGEM(iFGEM.pruebaAcidaFGEM()),
                semaforoMayorFGEM(iFGEM.pruebaAcidaFGEM(), BigDecimal.ONE, new BigDecimal("0.5")),
                "Igual que la liquidez, pero sin contar el inventario. Bien desde 1.00.");
        filaFGEM = indicadorFGEM(filaFGEM, "Capital de trabajo", MontosFGEM.formatearFGEM(iFGEM.capitalTrabajoFGEM(), monedaFGEM),
                iFGEM.capitalTrabajoFGEM().signum() < 0 ? "margen-negativo" : null,
                "Activo corriente − pasivo corriente: el dinero que queda para operar después de pagar lo del año.");
        filaFGEM = indicadorFGEM(filaFGEM, "Endeudamiento", MontosFGEM.porcentajeFGEM(iFGEM.endeudamientoFGEM()),
                semaforoMenorFGEM(iFGEM.endeudamientoFGEM(), IndicadoresPatrimonioFGEM.ENDEUDAMIENTO_VERDE_FGEM, IndicadoresPatrimonioFGEM.ENDEUDAMIENTO_AMARILLO_FGEM),
                "Deudas ÷ activos. Bien hasta 50 %; riesgo por encima de 70 %.");
        filaFGEM = indicadorFGEM(filaFGEM, "Deuda por cada 1 de patrimonio",
                iFGEM.apalancamientoFGEM() == null ? (bFGEM.patrimonioFGEM().signum() <= 0 && bFGEM.totalPasivoFGEM().signum() > 0
                        ? "Patrimonio negativo" : "—") : IndicadoresPatrimonioFGEM.vecesFGEM(iFGEM.apalancamientoFGEM()),
                iFGEM.apalancamientoFGEM() == null && bFGEM.patrimonioFGEM().signum() <= 0 && bFGEM.totalPasivoFGEM().signum() > 0
                        ? "margen-negativo" : semaforoMenorFGEM(iFGEM.apalancamientoFGEM(), BigDecimal.ONE, new BigDecimal("2.33")),
                "Deudas ÷ patrimonio (apalancamiento).");
        String mesesFGEM = iFGEM.mesesResultadosFGEM() == 0 ? "" : " (últimos " + iFGEM.mesesResultadosFGEM() + " meses)";
        filaFGEM = indicadorFGEM(filaFGEM, "Rentabilidad del patrimonio", MontosFGEM.porcentajeFGEM(iFGEM.rentabilidadPatrimonioFGEM()),
                signoFGEM(iFGEM.rentabilidadPatrimonioFGEM()),
                "Utilidad neta anual ÷ patrimonio" + mesesFGEM + ". Cuánto rinde al año lo invertido por los dueños.");
        filaFGEM = indicadorFGEM(filaFGEM, "Rentabilidad de los activos", MontosFGEM.porcentajeFGEM(iFGEM.rentabilidadActivosFGEM()),
                signoFGEM(iFGEM.rentabilidadActivosFGEM()), "Utilidad neta anual ÷ activos" + mesesFGEM + ".");
        filaFGEM = indicadorFGEM(filaFGEM, "Cuotas mensuales", bFGEM.cuotasMensualesFGEM().signum() == 0 ? "Sin cuotas"
                : MontosFGEM.formatearFGEM(bFGEM.cuotasMensualesFGEM(), monedaFGEM), null, "Suma de las cuotas de las deudas pendientes.");
        filaFGEM = indicadorFGEM(filaFGEM, "Cobertura de cuotas", iFGEM.coberturaCuotasFGEM() == null ? "—"
                        : IndicadoresPatrimonioFGEM.vecesFGEM(iFGEM.coberturaCuotasFGEM()) + " veces",
                semaforoMayorFGEM(iFGEM.coberturaCuotasFGEM(), IndicadoresPatrimonioFGEM.COBERTURA_VERDE_FGEM, BigDecimal.ONE),
                "Utilidad operativa mensual ÷ cuotas mensuales. Bien desde 1.30: la capacidad de pago que miran los bancos.");
        indicadorFGEM(filaFGEM, "Depreciación mensual", MontosFGEM.formatearFGEM(bFGEM.depreciacionMensualFGEM(), monedaFGEM), null,
                "Desgaste mensual de los activos fijos. Acumulada: " + MontosFGEM.formatearFGEM(bFGEM.depreciacionAcumuladaFGEM(), monedaFGEM) + ".");
    }

    private int indicadorFGEM(int filaFGEM, String nombreFGEM, String valorFGEM, String claseFGEM, String ayudaFGEM) {
        Label etiquetaFGEM = new Label(nombreFGEM);
        etiquetaFGEM.getStyleClass().add("info-key");
        etiquetaFGEM.setTooltip(new Tooltip(ayudaFGEM));
        Label datoFGEM = new Label(valorFGEM);
        datoFGEM.getStyleClass().add("info-value");
        if (claseFGEM != null) {
            datoFGEM.getStyleClass().add(claseFGEM);
        }
        datoFGEM.setTooltip(new Tooltip(ayudaFGEM));
        gridIndicadoresFGEM.add(etiquetaFGEM, 0, filaFGEM);
        gridIndicadoresFGEM.add(datoFGEM, 1, filaFGEM);
        return filaFGEM + 1;
    }

    /** Clase de color cuando más alto es mejor. */
    private static String semaforoMayorFGEM(BigDecimal valorFGEM, BigDecimal verdeFGEM, BigDecimal amarilloFGEM) {
        if (valorFGEM == null) {
            return null;
        }
        if (valorFGEM.compareTo(verdeFGEM) >= 0) {
            return "margen-ok";
        }
        return valorFGEM.compareTo(amarilloFGEM) >= 0 ? "margen-bajo" : "margen-negativo";
    }

    /** Clase de color cuando más bajo es mejor. */
    private static String semaforoMenorFGEM(BigDecimal valorFGEM, BigDecimal verdeFGEM, BigDecimal amarilloFGEM) {
        if (valorFGEM == null) {
            return null;
        }
        if (valorFGEM.compareTo(verdeFGEM) <= 0) {
            return "margen-ok";
        }
        return valorFGEM.compareTo(amarilloFGEM) <= 0 ? "margen-bajo" : "margen-negativo";
    }

    private static String signoFGEM(BigDecimal valorFGEM) {
        return valorFGEM == null ? null : valorFGEM.signum() < 0 ? "margen-negativo" : null;
    }

    private void filtrarFGEM() {
        String activoFGEM = normalizarFGEM(txtBuscarActivoFGEM.getText());
        activosFiltradosFGEM.setPredicate(aFGEM -> activoFGEM.isEmpty()
                || aFGEM.descripcionFGEM().toLowerCase(Locale.ROOT).contains(activoFGEM)
                || aFGEM.categoriaFGEM().descripcionFGEM().toLowerCase(Locale.ROOT).contains(activoFGEM));
        String deudaFGEM = normalizarFGEM(txtBuscarDeudaFGEM.getText());
        deudasFiltradasFGEM.setPredicate(dFGEM -> deudaFGEM.isEmpty()
                || dFGEM.acreedorFGEM().toLowerCase(Locale.ROOT).contains(deudaFGEM)
                || (dFGEM.descripcionFGEM() != null && dFGEM.descripcionFGEM().toLowerCase(Locale.ROOT).contains(deudaFGEM))
                || dFGEM.categoriaFGEM().descripcionFGEM().toLowerCase(Locale.ROOT).contains(deudaFGEM));
    }

    private static String normalizarFGEM(String textoFGEM) {
        return textoFGEM == null ? "" : textoFGEM.strip().toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ activos

    @FXML
    private void nuevoActivoFGEM() {
        FormularioActivoFGEM formularioFGEM = new FormularioActivoFGEM(null);
        ActivoFGEM[] creadoFGEM = new ActivoFGEM[1];
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Nuevo activo", "Montos sin impuestos.",
                formularioFGEM.contenidoFGEM(), "Registrar", 520, () -> creadoFGEM[0] = servicioFGEM().registrarActivoFGEM(formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            seleccionarFGEM(tablaActivosFGEM, aFGEM -> aFGEM.idFGEM() == creadoFGEM[0].idFGEM());
            NotificadorFGEM.exitoFGEM("Activo registrado: " + creadoFGEM[0].descripcionFGEM() + ".");
        }
    }

    @FXML
    private void editarActivoFGEM() {
        ActivoFGEM aFGEM = tablaActivosFGEM.getSelectionModel().getSelectedItem();
        if (aFGEM == null || aFGEM.dadoDeBajaFGEM() || !contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.FINANZAS_GESTIONAR)) {
            return;
        }
        FormularioActivoFGEM formularioFGEM = new FormularioActivoFGEM(aFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Editar activo",
                aFGEM.categoriaFGEM().corrienteFGEM() ? "Actualiza el saldo y su fecha cada vez que cambie." : null,
                formularioFGEM.contenidoFGEM(), "Guardar cambios", 520, () -> servicioFGEM().actualizarActivoFGEM(aFGEM.idFGEM(), formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Activo actualizado.");
        }
    }

    @FXML
    private void darDeBajaFGEM() {
        ActivoFGEM aFGEM = tablaActivosFGEM.getSelectionModel().getSelectedItem();
        if (aFGEM == null || aFGEM.dadoDeBajaFGEM()) {
            return;
        }
        DatePicker fechaFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        TextField motivoFGEM = new TextField();
        motivoFGEM.setPromptText("Ej.: vendido, malogrado, registrado por error");
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Fecha de baja *", fechaFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Motivo *", motivoFGEM);
        FormulariosFGEM.notaFGEM(grillaFGEM, "Si lo vendiste, registra el dinero recibido como ingreso en Gestión financiera "
                + "(categoría «Venta de activos»).");
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Dar de baja un activo",
                "\"" + aFGEM.descripcionFGEM() + "\" dejará de contar en el balance desde la fecha indicada, pero se conservará "
                        + "en el historial.", grillaFGEM, "Dar de baja",
                () -> servicioFGEM().darDeBajaFGEM(aFGEM.idFGEM(), CamposFechaFGEM.leerFGEM(fechaFGEM, "baja"), motivoFGEM.getText()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Activo dado de baja.");
        }
    }

    // ------------------------------------------------------------------ deudas

    @FXML
    private void nuevaDeudaFGEM() {
        FormularioDeudaFGEM formularioFGEM = new FormularioDeudaFGEM(null);
        DeudaFGEM[] creadaFGEM = new DeudaFGEM[1];
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Nueva deuda",
                "Registra el capital adeudado. Si ya pagaste una parte, indica el monto original y registra los pagos, "
                        + "o registra directamente el saldo actual con la fecha de hoy.",
                formularioFGEM.contenidoFGEM(), "Registrar", 520, () -> creadaFGEM[0] = servicioFGEM().registrarDeudaFGEM(formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            seleccionarFGEM(tablaDeudasFGEM, dFGEM -> dFGEM.idFGEM() == creadaFGEM[0].idFGEM());
            NotificadorFGEM.exitoFGEM("Deuda registrada: " + MontosFGEM.formatearFGEM(creadaFGEM[0].montoFGEM(), monedaFGEM) + ".");
        }
    }

    @FXML
    private void editarDeudaFGEM() {
        DeudaFGEM dFGEM = tablaDeudasFGEM.getSelectionModel().getSelectedItem();
        if (dFGEM == null || dFGEM.anuladaFGEM() || !contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.FINANZAS_GESTIONAR)) {
            return;
        }
        FormularioDeudaFGEM formularioFGEM = new FormularioDeudaFGEM(dFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Editar deuda", null, formularioFGEM.contenidoFGEM(), "Guardar cambios",
                520, () -> servicioFGEM().actualizarDeudaFGEM(dFGEM.idFGEM(), formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Deuda actualizada.");
        }
    }

    @FXML
    private void anularDeudaFGEM() {
        DeudaFGEM dFGEM = tablaDeudasFGEM.getSelectionModel().getSelectedItem();
        if (dFGEM == null || dFGEM.anuladaFGEM()) {
            return;
        }
        TextField motivoFGEM = new TextField();
        motivoFGEM.setPromptText("Ej.: registrada dos veces");
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Motivo *", motivoFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Anular deuda",
                "La deuda con " + dFGEM.acreedorFGEM() + " (" + MontosFGEM.formatearFGEM(dFGEM.montoFGEM(), monedaFGEM) + ") dejará de contar en el "
                        + "balance, pero se conservará como anulada. Para una deuda ya pagada no hace falta anularla.",
                grillaFGEM, "Anular", () -> servicioFGEM().anularDeudaFGEM(dFGEM.idFGEM(), motivoFGEM.getText()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Deuda anulada.");
        }
    }

    @FXML
    private void registrarPagoFGEM() {
        DeudaFGEM dFGEM = tablaDeudasFGEM.getSelectionModel().getSelectedItem();
        if (dFGEM == null || !dFGEM.pendienteFGEM()) {
            return;
        }
        DatePicker fechaFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        BigDecimal interesSugeridoFGEM = PatrimonioServiceFGEM.interesSugeridoFGEM(dFGEM);
        TextField interesFGEM = new TextField(interesSugeridoFGEM.signum() == 0 ? "" : MontosFGEM.paraEditarFGEM(interesSugeridoFGEM));
        interesFGEM.setPromptText("0.00");
        TextField montoFGEM = new TextField();
        montoFGEM.setPromptText("0.00");
        if (dFGEM.cuotaMensualFGEM() != null) {
            montoFGEM.setText(MontosFGEM.paraEditarFGEM(dFGEM.cuotaMensualFGEM().subtract(interesSugeridoFGEM).max(BigDecimal.ZERO).min(dFGEM.saldoFGEM())));
        }
        Label totalFGEM = new Label();
        totalFGEM.getStyleClass().add("info-value");
        Runnable sumarFGEM = () -> {
            try {
                BigDecimal capitalFGEM = montoFGEM.getText().isBlank() ? BigDecimal.ZERO : MontosFGEM.leerFGEM(montoFGEM.getText(), "Capital");
                BigDecimal interesesFGEM = interesFGEM.getText().isBlank() ? BigDecimal.ZERO : MontosFGEM.leerFGEM(interesFGEM.getText(), "Intereses");
                totalFGEM.setText("Sale de la cuenta: " + MontosFGEM.formatearFGEM(capitalFGEM.add(interesesFGEM), monedaFGEM));
            } catch (RuntimeException eFGEM) {
                totalFGEM.setText("");
            }
        };
        montoFGEM.textProperty().addListener((oFGEM, aFGEM, nFGEM) -> sumarFGEM.run());
        interesFGEM.textProperty().addListener((oFGEM, aFGEM, nFGEM) -> sumarFGEM.run());
        sumarFGEM.run();
        SelectorCuentaFGEM cuentaFGEM = new SelectorCuentaFGEM(contextoFGEM.tesoreriaFGEM().cuentasActivasFGEM(), null, dFGEM.cuentaIdFGEM());
        TextField notaFGEM = new TextField();
        notaFGEM.setPromptText("Opcional. Ej.: cuota 3 de 12");
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Fecha *", fechaFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Capital (" + monedaFGEM.simboloFGEM() + ") *", montoFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Intereses (" + monedaFGEM.simboloFGEM() + ")", interesFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Pagado desde *", cuentaFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "", totalFGEM);
        FormulariosFGEM.notaFGEM(grillaFGEM, "Saldo pendiente: " + MontosFGEM.formatearFGEM(dFGEM.saldoFGEM(), monedaFGEM) + ". El capital baja la "
                + "deuda; los intereses se registran solos como gasto financiero en Gestión financiera"
                + (interesSugeridoFGEM.signum() > 0 ? " (sugerido: saldo × tasa anual ÷ 12)." : "."));
        FormulariosFGEM.filaFGEM(grillaFGEM, "Nota", notaFGEM);
        DeudaFGEM[] actualizadaFGEM = new DeudaFGEM[1];
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Registrar pago a " + dFGEM.acreedorFGEM(), null, grillaFGEM, "Registrar pago",
                () -> actualizadaFGEM[0] = servicioFGEM().registrarPagoFGEM(dFGEM.idFGEM(), CamposFechaFGEM.leerFGEM(fechaFGEM, "pago"),
                        MontosFGEM.leerFGEM(montoFGEM.getText(), "Capital"), montoOpcionalFGEM(interesFGEM.getText(), "Intereses"),
                        cuentaFGEM.cuentaIdFGEM(), notaFGEM.getText()));
        if (okFGEM) {
            cargarFGEM();
            seleccionarFGEM(tablaDeudasFGEM, xFGEM -> xFGEM.idFGEM() == dFGEM.idFGEM());
            NotificadorFGEM.exitoFGEM(actualizadaFGEM[0].canceladaFGEM() ? "Pago registrado: la deuda quedó cancelada."
                    : "Pago registrado. Saldo: " + MontosFGEM.formatearFGEM(actualizadaFGEM[0].saldoFGEM(), monedaFGEM) + ".");
        }
    }

    @FXML
    private void verPagosFGEM() {
        DeudaFGEM dFGEM = tablaDeudasFGEM.getSelectionModel().getSelectedItem();
        if (dFGEM == null) {
            return;
        }
        TableView<PagoDeudaFGEM> tablaFGEM = new TableView<>();
        tablaFGEM.setPrefHeight(280);
        tablaFGEM.setPlaceholder(new Label("Aún no hay pagos registrados."));
        tablaFGEM.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaFGEM.getColumns().add(columnaFGEM("Fecha", 95, pFGEM -> FechasFGEM.paraMostrarFGEM(pFGEM.fechaFGEM()), false));
        tablaFGEM.getColumns().add(columnaFGEM("Capital", 100, pFGEM -> MontosFGEM.formatearFGEM(pFGEM.montoFGEM(), monedaFGEM), true));
        tablaFGEM.getColumns().add(columnaFGEM("Intereses", 90, pFGEM -> pFGEM.interesFGEM().signum() == 0 ? "—"
                : MontosFGEM.formatearFGEM(pFGEM.interesFGEM(), monedaFGEM), true));
        tablaFGEM.getColumns().add(columnaFGEM("Cuenta", 110, pFGEM -> Objects.requireNonNullElse(pFGEM.cuentaFGEM(), "—"), false));
        tablaFGEM.getColumns().add(columnaFGEM("Nota", 150, pFGEM -> Objects.requireNonNullElse(pFGEM.notaFGEM(), ""), false));
        tablaFGEM.getColumns().add(columnaFGEM("Registró", 120, pFGEM -> pFGEM.anuladoFGEM()
                ? "Anulado por " + pFGEM.anuladoPorFGEM() : pFGEM.registradoPorFGEM(), false));
        tablaFGEM.setRowFactory(tFGEM -> new TableRow<>() {
            @Override
            protected void updateItem(PagoDeudaFGEM pFGEM, boolean vacioFGEM) {
                super.updateItem(pFGEM, vacioFGEM);
                pseudoClassStateChanged(ARCHIVADO_FGEM, !vacioFGEM && pFGEM != null && pFGEM.anuladoFGEM());
            }
        });
        Label resumenFGEM = new Label();
        resumenFGEM.getStyleClass().add("info-value");
        Runnable recargarFGEM = () -> {
            DeudaFGEM actualFGEM = servicioFGEM().listarDeudasFGEM(true).stream().filter(xFGEM -> xFGEM.idFGEM() == dFGEM.idFGEM()).findFirst().orElse(dFGEM);
            tablaFGEM.getItems().setAll(servicioFGEM().listarPagosFGEM(dFGEM.idFGEM()));
            resumenFGEM.setText("Monto: " + MontosFGEM.formatearFGEM(actualFGEM.montoFGEM(), monedaFGEM) + "   ·   Pagado: "
                    + MontosFGEM.formatearFGEM(actualFGEM.pagadoFGEM(), monedaFGEM) + "   ·   Saldo: " + MontosFGEM.formatearFGEM(actualFGEM.saldoFGEM(), monedaFGEM));
        };
        boolean[] cambiosFGEM = {false};
        VBox contenidoFGEM = new VBox(10, resumenFGEM, tablaFGEM);
        if (contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.FINANZAS_GESTIONAR) && !dFGEM.anuladaFGEM()) {
            Button anularFGEM = new Button("Anular pago seleccionado");
            anularFGEM.getStyleClass().add("button-secondary");
            anularFGEM.disableProperty().bind(tablaFGEM.getSelectionModel().selectedItemProperty().isNull());
            anularFGEM.setOnAction(eFGEM -> {
                PagoDeudaFGEM pFGEM = tablaFGEM.getSelectionModel().getSelectedItem();
                if (pFGEM == null || pFGEM.anuladoFGEM()) {
                    return;
                }
                if (DialogosFGEM.confirmarFGEM(tablaFGEM.getScene().getWindow(), "Anular pago",
                        "El pago de " + MontosFGEM.formatearFGEM(pFGEM.montoFGEM(), monedaFGEM) + " del " + FechasFGEM.paraMostrarFGEM(pFGEM.fechaFGEM())
                                + " se anulará: el saldo de la deuda vuelve a subir, el dinero vuelve a la cuenta "
                                + "y sus intereses se anulan.")) {
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
        DialogosFGEM.informacionFGEM(ventanaFGEM(), "Pagos a " + dFGEM.acreedorFGEM(), dFGEM.categoriaFGEM().descripcionFGEM()
                + (dFGEM.descripcionFGEM() == null ? "" : " · " + dFGEM.descripcionFGEM()), contenidoFGEM);
        if (cambiosFGEM[0]) {
            cargarFGEM();
        }
    }

    private static TableColumn<PagoDeudaFGEM, String> columnaFGEM(String tituloFGEM, double anchoFGEM,
                                                          Function<PagoDeudaFGEM, String> valorFGEM, boolean numeroFGEM) {
        TableColumn<PagoDeudaFGEM, String> columnaFGEM = new TableColumn<>(tituloFGEM);
        columnaFGEM.setPrefWidth(anchoFGEM);
        columnaFGEM.setSortable(false);
        columnaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(valorFGEM.apply(cFGEM.getValue())));
        if (numeroFGEM) {
            columnaFGEM.getStyleClass().add("col-numero");
        }
        return columnaFGEM;
    }

    // ------------------------------------------------------------------ tablas

    private void configurarTablaBalanceFGEM() {
        colBalConceptoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colBalConceptoFGEM.setCellFactory(colFGEM -> celdaBalanceFGEM(fFGEM -> fFGEM.conceptoFGEM(), fFGEM -> null));
        colBalMontoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colBalMontoFGEM.setCellFactory(colFGEM -> celdaBalanceFGEM(fFGEM -> fFGEM.montoFGEM() == null ? "" : MontosFGEM.formatearFGEM(fFGEM.montoFGEM(), monedaFGEM),
                fFGEM -> fFGEM.montoFGEM() != null && fFGEM.montoFGEM().signum() < 0 ? "margen-negativo" : null));
        colBalPorcentajeFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colBalPorcentajeFGEM.setCellFactory(colFGEM -> celdaBalanceFGEM(fFGEM -> fFGEM.montoFGEM() == null || totalActivoFGEM.signum() == 0 ? ""
                : MontosFGEM.porcentajeFGEM(fFGEM.montoFGEM().divide(totalActivoFGEM, 6, RoundingMode.HALF_UP)), fFGEM -> null));
        tablaBalanceFGEM.setRowFactory(tFGEM -> new TableRow<>() {
            @Override
            protected void updateItem(FilaBalanceFGEM filaFGEM, boolean vacioFGEM) {
                super.updateItem(filaFGEM, vacioFGEM);
                boolean subtotalFGEM = !vacioFGEM && filaFGEM != null
                        && (filaFGEM.tipoFGEM() == TipoFilaFGEM.SUBTOTAL || filaFGEM.tipoFGEM() == TipoFilaFGEM.TITULO);
                pseudoClassStateChanged(SUBTOTAL_FGEM, subtotalFGEM);
                pseudoClassStateChanged(RESULTADO_FGEM, !vacioFGEM && filaFGEM != null && filaFGEM.tipoFGEM() == TipoFilaFGEM.RESULTADO);
            }
        });
    }

    private static TableCell<FilaBalanceFGEM, FilaBalanceFGEM> celdaBalanceFGEM(Function<FilaBalanceFGEM, String> textoFGEM,
                                                                    Function<FilaBalanceFGEM, String> claseFGEM) {
        return new TableCell<>() {
            @Override
            protected void updateItem(FilaBalanceFGEM filaFGEM, boolean vacioFGEM) {
                super.updateItem(filaFGEM, vacioFGEM);
                getStyleClass().removeAll("margen-negativo");
                if (vacioFGEM || filaFGEM == null) {
                    setText(null);
                    return;
                }
                setText(textoFGEM.apply(filaFGEM));
                String estiloFGEM = claseFGEM.apply(filaFGEM);
                if (estiloFGEM != null) {
                    getStyleClass().add(estiloFGEM);
                }
            }
        };
    }

    private void configurarTablaActivosFGEM() {
        LocalDate hoyFGEM = LocalDate.now();
        colActTipoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().categoriaFGEM().descripcionFGEM()));
        colActDescripcionFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().descripcionFGEM()
                + (cFGEM.getValue().dadoDeBajaFGEM() ? "  (baja)" : "")));
        colActFechaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().fechaFGEM()));
        colActFechaFGEM.setCellFactory(colFGEM -> celdaFechaFGEM());
        colActValorFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().valorFGEM()));
        colActValorFGEM.setCellFactory(colFGEM -> celdaMontoFGEM());
        colActVidaFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().seDepreciaFGEM()
                ? cFGEM.getValue().vidaUtilAniosFGEM() + (cFGEM.getValue().vidaUtilAniosFGEM() == 1 ? " año" : " años") : "—"));
        colActMensualFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().seDepreciaFGEM()
                ? DepreciacionFGEM.deFGEM(cFGEM.getValue(), hoyFGEM).mensualFGEM() : null));
        colActMensualFGEM.setCellFactory(colFGEM -> celdaMontoFGEM());
        colActDepreciacionFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(
                DepreciacionFGEM.deFGEM(cFGEM.getValue(), hoyFGEM).acumuladaFGEM()));
        colActDepreciacionFGEM.setCellFactory(colFGEM -> celdaMontoFGEM());
        colActLibrosFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(
                DepreciacionFGEM.deFGEM(cFGEM.getValue(), hoyFGEM).valorEnLibrosFGEM()));
        colActLibrosFGEM.setCellFactory(colFGEM -> celdaMontoFGEM());

        SortedList<ActivoFGEM> ordenadosFGEM = new SortedList<>(activosFiltradosFGEM);
        ordenadosFGEM.comparatorProperty().bind(tablaActivosFGEM.comparatorProperty());
        tablaActivosFGEM.setItems(ordenadosFGEM);
        tablaActivosFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarAccionesFGEM());
        tablaActivosFGEM.setRowFactory(tFGEM -> {
            TableRow<ActivoFGEM> filaFGEM = new TableRow<>() {
                @Override
                protected void updateItem(ActivoFGEM aFGEM, boolean vacioFGEM) {
                    super.updateItem(aFGEM, vacioFGEM);
                    boolean bajaFGEM = !vacioFGEM && aFGEM != null && aFGEM.dadoDeBajaFGEM();
                    pseudoClassStateChanged(ARCHIVADO_FGEM, bajaFGEM);
                    if (bajaFGEM) {
                        setTooltip(new Tooltip("Dado de baja el " + FechasFGEM.paraMostrarFGEM(aFGEM.fechaBajaFGEM()) + ": "
                                + aFGEM.motivoBajaFGEM()));
                    } else if (!vacioFGEM && aFGEM != null && aFGEM.seDepreciaFGEM()) {
                        DepreciacionFGEM dFGEM = DepreciacionFGEM.deFGEM(aFGEM, hoyFGEM);
                        setTooltip(new Tooltip(dFGEM.mesesTranscurridosFGEM() == 0
                                ? "Aún no cumple un mes: empieza a depreciarse el " + FechasFGEM.paraMostrarFGEM(aFGEM.fechaFGEM().plusMonths(1))
                                + " (" + MontosFGEM.formatearFGEM(dFGEM.mensualFGEM(), monedaFGEM) + " al mes)."
                                : dFGEM.mesesTranscurridosFGEM() + " meses de uso de " + aFGEM.vidaUtilAniosFGEM() * 12 + " de vida útil."));
                    } else {
                        setTooltip(null);
                    }
                }
            };
            filaFGEM.setOnMouseClicked(eFGEM -> {
                if (eFGEM.getButton() == MouseButton.PRIMARY && eFGEM.getClickCount() == 2 && !filaFGEM.isEmpty()) {
                    editarActivoFGEM();
                }
            });
            return filaFGEM;
        });
    }

    private void configurarTablaDeudasFGEM() {
        colDeuAcreedorFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().acreedorFGEM()));
        colDeuTipoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().categoriaFGEM().descripcionFGEM()));
        colDeuFechaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().fechaFGEM()));
        colDeuFechaFGEM.setCellFactory(colFGEM -> celdaFechaFGEM());
        colDeuMontoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().montoFGEM()));
        colDeuMontoFGEM.setCellFactory(colFGEM -> celdaMontoFGEM());
        colDeuSaldoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().saldoFGEM()));
        colDeuSaldoFGEM.setCellFactory(colFGEM -> celdaMontoFGEM());
        colDeuCuotaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().cuotaMensualFGEM()));
        colDeuCuotaFGEM.setCellFactory(colFGEM -> celdaMontoFGEM());
        colDeuTasaFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().tasaAnualFGEM() == null ? "—"
                : MontosFGEM.porcentajeFGEM(cFGEM.getValue().tasaAnualFGEM().movePointLeft(2))));
        colDeuVenceFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().vencimientoFGEM()));
        colDeuVenceFGEM.setCellFactory(colFGEM -> celdaFechaFGEM());
        colDeuEstadoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().estadoTextoFGEM()));

        SortedList<DeudaFGEM> ordenadasFGEM = new SortedList<>(deudasFiltradasFGEM);
        ordenadasFGEM.comparatorProperty().bind(tablaDeudasFGEM.comparatorProperty());
        tablaDeudasFGEM.setItems(ordenadasFGEM);
        tablaDeudasFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarAccionesFGEM());
        tablaDeudasFGEM.setRowFactory(tFGEM -> {
            TableRow<DeudaFGEM> filaFGEM = new TableRow<>() {
                @Override
                protected void updateItem(DeudaFGEM dFGEM, boolean vacioFGEM) {
                    super.updateItem(dFGEM, vacioFGEM);
                    boolean apagadaFGEM = !vacioFGEM && dFGEM != null && !dFGEM.pendienteFGEM();
                    pseudoClassStateChanged(ARCHIVADO_FGEM, apagadaFGEM);
                    setTooltip(!vacioFGEM && dFGEM != null && dFGEM.anuladaFGEM()
                            ? new Tooltip("Anulada: " + dFGEM.motivoAnulacionFGEM()) : null);
                }
            };
            filaFGEM.setOnMouseClicked(eFGEM -> {
                if (eFGEM.getButton() == MouseButton.PRIMARY && eFGEM.getClickCount() == 2 && !filaFGEM.isEmpty()) {
                    verPagosFGEM();
                }
            });
            return filaFGEM;
        });
    }

    private void actualizarAccionesFGEM() {
        ActivoFGEM aFGEM = tablaActivosFGEM.getSelectionModel().getSelectedItem();
        boolean sinActivoFGEM = aFGEM == null || aFGEM.dadoDeBajaFGEM();
        btnEditarActivoFGEM.setDisable(sinActivoFGEM);
        btnBajaActivoFGEM.setDisable(sinActivoFGEM);

        DeudaFGEM dFGEM = tablaDeudasFGEM.getSelectionModel().getSelectedItem();
        btnVerPagosFGEM.setDisable(dFGEM == null);
        btnEditarDeudaFGEM.setDisable(dFGEM == null || dFGEM.anuladaFGEM());
        btnAnularDeudaFGEM.setDisable(dFGEM == null || dFGEM.anuladaFGEM());
        btnPagarFGEM.setDisable(dFGEM == null || !dFGEM.pendienteFGEM());
    }

    private <T> TableCell<T, BigDecimal> celdaMontoFGEM() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal montoFGEM, boolean vacioFGEM) {
                super.updateItem(montoFGEM, vacioFGEM);
                setText(vacioFGEM || montoFGEM == null ? (vacioFGEM ? null : "—") : MontosFGEM.formatearFGEM(montoFGEM, monedaFGEM));
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

    // ------------------------------------------------------------------ utilidades

    private static <T> void seleccionarFGEM(TableView<T> tablaFGEM, java.util.function.Predicate<T> criterioFGEM) {
        tablaFGEM.getItems().stream().filter(criterioFGEM).findFirst().ifPresent(xFGEM -> {
            tablaFGEM.getSelectionModel().select(xFGEM);
            tablaFGEM.scrollTo(xFGEM);
        });
    }

    private PatrimonioServiceFGEM servicioFGEM() {
        return contextoFGEM.patrimonioFGEM();
    }

    private Window ventanaFGEM() {
        return tablaActivosFGEM.getScene().getWindow();
    }

    private static ReadOnlyStringWrapper textoFGEM(String valorFGEM) {
        return new ReadOnlyStringWrapper(valorFGEM);
    }

    private static BigDecimal montoOpcionalFGEM(String textoFGEM, String campoFGEM) {
        return textoFGEM == null || textoFGEM.isBlank() ? null : MontosFGEM.leerFGEM(textoFGEM, campoFGEM);
    }

    /** Formulario de activo. La vida útil y el valor residual solo se piden en bienes que se deprecian. */
    private final class FormularioActivoFGEM {
        private final ComboBox<CategoriaActivoFGEM> cmbCategoriaFGEM = new ComboBox<>();
        private final Label lblAyudaFGEM = new Label();
        private final TextField txtDescripcionFGEM = new TextField();
        private final DatePicker dpFechaFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        private final TextField txtValorFGEM = new TextField();
        private final TextField txtVidaFGEM = new TextField();
        private final TextField txtResidualFGEM = new TextField();
        private final Label lblFechaFGEM = new Label();
        private final Label lblValorFGEM = new Label();
        private final SelectorCuentaFGEM cmbCuentaFGEM;

        FormularioActivoFGEM(ActivoFGEM aFGEM) {
            // El efectivo se lleva en «Tesorería»; «Caja y bancos» solo aparece al editar un registro antiguo.
            cmbCategoriaFGEM.setItems(FXCollections.observableArrayList(java.util.Arrays.stream(CategoriaActivoFGEM.values())
                    .filter(cFGEM -> cFGEM != CategoriaActivoFGEM.CAJA_BANCOS
                            || (aFGEM != null && aFGEM.categoriaFGEM() == CategoriaActivoFGEM.CAJA_BANCOS))
                    .toList()));
            cmbCuentaFGEM = new SelectorCuentaFGEM(contextoFGEM.tesoreriaFGEM().cuentasActivasFGEM(),
                    "No salió dinero (ya lo tenía, aporte o compra financiada)", aFGEM == null ? null : aFGEM.cuentaIdFGEM());
            cmbCategoriaFGEM.setPromptText("Selecciona el tipo de activo");
            cmbCategoriaFGEM.setVisibleRowCount(10);
            txtDescripcionFGEM.setPromptText("Ej.: Refrigeradora exhibidora, Motocicleta");
            txtValorFGEM.setPromptText("0.00");
            txtVidaFGEM.setPromptText("Años");
            txtResidualFGEM.setPromptText("0.00 (opcional)");
            lblAyudaFGEM.getStyleClass().add("hint");
            lblAyudaFGEM.setWrapText(true);
            lblAyudaFGEM.setMinHeight(Region.USE_PREF_SIZE);

            cmbCategoriaFGEM.valueProperty().addListener((oFGEM, anteriorFGEM, cFGEM) -> {
                actualizarCamposFGEM(cFGEM);
                if (cFGEM != null && aFGEM == null) {
                    txtVidaFGEM.setText(cFGEM.vidaUtilSugeridaFGEM() == null ? "" : String.valueOf(cFGEM.vidaUtilSugeridaFGEM()));
                }
            });
            if (aFGEM != null) {
                cmbCategoriaFGEM.setValue(aFGEM.categoriaFGEM());
                txtDescripcionFGEM.setText(aFGEM.descripcionFGEM());
                dpFechaFGEM.setValue(aFGEM.fechaFGEM());
                txtValorFGEM.setText(MontosFGEM.paraEditarFGEM(aFGEM.valorFGEM()));
                txtVidaFGEM.setText(aFGEM.vidaUtilAniosFGEM() == null ? "" : String.valueOf(aFGEM.vidaUtilAniosFGEM()));
                txtResidualFGEM.setText(aFGEM.valorResidualFGEM().signum() == 0 ? "" : MontosFGEM.paraEditarFGEM(aFGEM.valorResidualFGEM()));
            } else {
                actualizarCamposFGEM(null);
            }
        }

        private void actualizarCamposFGEM(CategoriaActivoFGEM cFGEM) {
            boolean corrienteFGEM = cFGEM != null && cFGEM.corrienteFGEM();
            boolean depreciaFGEM = cFGEM != null && cFGEM.depreciableFGEM();
            lblAyudaFGEM.setText(cFGEM == null ? "" : cFGEM.ayudaFGEM());
            lblFechaFGEM.setText(corrienteFGEM ? "Fecha del saldo *" : "Fecha de adquisición *");
            lblValorFGEM.setText((corrienteFGEM ? "Saldo (" : "Valor de adquisición (") + monedaFGEM.simboloFGEM() + ") *");
            txtVidaFGEM.setDisable(!depreciaFGEM);
            txtResidualFGEM.setDisable(!depreciaFGEM);
            if (!depreciaFGEM) {
                txtVidaFGEM.clear();
                txtResidualFGEM.clear();
            }
        }

        GridPane contenidoFGEM() {
            GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
            FormulariosFGEM.filaFGEM(grillaFGEM, "Tipo *", cmbCategoriaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "", lblAyudaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Descripción *", txtDescripcionFGEM);
            lblFechaFGEM.getStyleClass().add("form-label");
            lblValorFGEM.getStyleClass().add("form-label");
            int filaFGEM = grillaFGEM.getRowCount();
            grillaFGEM.add(lblFechaFGEM, 0, filaFGEM);
            grillaFGEM.add(dpFechaFGEM, 1, filaFGEM);
            dpFechaFGEM.setMaxWidth(Double.MAX_VALUE);
            grillaFGEM.add(lblValorFGEM, 0, filaFGEM + 1);
            grillaFGEM.add(txtValorFGEM, 1, filaFGEM + 1);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Vida útil (años)", txtVidaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Valor residual (" + monedaFGEM.simboloFGEM() + ")", txtResidualFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Pagado desde", cmbCuentaFGEM);
            FormulariosFGEM.notaFGEM(grillaFGEM, "La depreciación se acumula por cada mes completo desde la fecha de adquisición: "
                    + "un bien comprado hoy empieza a perder valor en un mes. Vida útil vacía = no se deprecia. "
                    + "Valor residual: lo que valdría al final de su vida útil. Si lo compraste ahora, elige la cuenta "
                    + "con la que pagaste: el valor saldrá de su saldo en «Tesorería».");
            return grillaFGEM;
        }

        DatosActivoFGEM datosFGEM() {
            LocalDate fechaFGEM = CamposFechaFGEM.leerFGEM(dpFechaFGEM, "el activo");
            Integer vidaFGEM = null;
            String textoVidaFGEM = txtVidaFGEM.getText() == null ? "" : txtVidaFGEM.getText().strip();
            if (!textoVidaFGEM.isEmpty()) {
                try {
                    vidaFGEM = Integer.parseInt(textoVidaFGEM);
                } catch (NumberFormatException eFGEM) {
                    throw new com.businessanalyzer.exception.ValidacionExceptionFGEM(
                            "La vida útil debe ser un número entero de años.");
                }
            }
            return new DatosActivoFGEM(cmbCategoriaFGEM.getValue(), txtDescripcionFGEM.getText(), fechaFGEM,
                    MontosFGEM.leerFGEM(txtValorFGEM.getText(), "Valor"), vidaFGEM, montoOpcionalFGEM(txtResidualFGEM.getText(), "Valor residual"),
                    cmbCuentaFGEM.cuentaIdFGEM());
        }
    }

    /** Formulario de deuda. Tasa, cuota y vencimiento son opcionales, pero mejoran el análisis. */
    private final class FormularioDeudaFGEM {
        private final ComboBox<CategoriaDeudaFGEM> cmbCategoriaFGEM =
                new ComboBox<>(FXCollections.observableArrayList(CategoriaDeudaFGEM.values()));
        private final TextField txtAcreedorFGEM = new TextField();
        private final TextField txtDescripcionFGEM = new TextField();
        private final DatePicker dpFechaFGEM = CamposFechaFGEM.nuevoFGEM(LocalDate.now());
        private final TextField txtMontoFGEM = new TextField();
        private final TextField txtTasaFGEM = new TextField();
        private final TextField txtCuotaFGEM = new TextField();
        private final DatePicker dpVencimientoFGEM = CamposFechaFGEM.nuevoFGEM(null);
        private final SelectorCuentaFGEM cmbCuentaFGEM;

        FormularioDeudaFGEM(DeudaFGEM dFGEM) {
            cmbCuentaFGEM = new SelectorCuentaFGEM(contextoFGEM.tesoreriaFGEM().cuentasActivasFGEM(),
                    "No ingresó dinero (proveedor, impuestos, compra financiada)", dFGEM == null ? null : dFGEM.cuentaIdFGEM());
            cmbCategoriaFGEM.setPromptText("Selecciona el tipo de deuda");
            txtAcreedorFGEM.setPromptText("Ej.: Caja Arequipa, Distribuidora Lima");
            txtDescripcionFGEM.setPromptText("Opcional. Ej.: préstamo para la refrigeradora");
            txtMontoFGEM.setPromptText("0.00");
            txtTasaFGEM.setPromptText("Ej.: 24.5 (opcional)");
            txtCuotaFGEM.setPromptText("Opcional");
            if (dFGEM != null) {
                cmbCategoriaFGEM.setValue(dFGEM.categoriaFGEM());
                txtAcreedorFGEM.setText(dFGEM.acreedorFGEM());
                txtDescripcionFGEM.setText(Objects.requireNonNullElse(dFGEM.descripcionFGEM(), ""));
                dpFechaFGEM.setValue(dFGEM.fechaFGEM());
                txtMontoFGEM.setText(MontosFGEM.paraEditarFGEM(dFGEM.montoFGEM()));
                txtTasaFGEM.setText(dFGEM.tasaAnualFGEM() == null ? "" : dFGEM.tasaAnualFGEM().stripTrailingZeros().toPlainString());
                txtCuotaFGEM.setText(dFGEM.cuotaMensualFGEM() == null ? "" : MontosFGEM.paraEditarFGEM(dFGEM.cuotaMensualFGEM()));
                dpVencimientoFGEM.setValue(dFGEM.vencimientoFGEM());
            }
        }

        GridPane contenidoFGEM() {
            GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
            FormulariosFGEM.filaFGEM(grillaFGEM, "Tipo *", cmbCategoriaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Acreedor *", txtAcreedorFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Descripción", txtDescripcionFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Fecha de inicio *", dpFechaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Monto (" + monedaFGEM.simboloFGEM() + ") *", txtMontoFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Tasa anual (%)", txtTasaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Cuota mensual (" + monedaFGEM.simboloFGEM() + ")", txtCuotaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Vencimiento", dpVencimientoFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "El dinero ingresó a", cmbCuentaFGEM);
            FormulariosFGEM.notaFGEM(grillaFGEM, "Con la cuota se calcula la capacidad de pago y la proyección de caja; con el "
                    + "vencimiento, qué parte de la deuda es de corto plazo (sin vencimiento se considera toda de corto "
                    + "plazo). Si es un préstamo en efectivo, elige la cuenta donde se depositó.");
            return grillaFGEM;
        }

        DatosDeudaFGEM datosFGEM() {
            LocalDate fechaFGEM = CamposFechaFGEM.leerFGEM(dpFechaFGEM, "inicio");
            LocalDate vencimientoFGEM = CamposFechaFGEM.leerFGEM(dpVencimientoFGEM, "vencimiento");
            return new DatosDeudaFGEM(cmbCategoriaFGEM.getValue(), txtAcreedorFGEM.getText(), txtDescripcionFGEM.getText(), fechaFGEM,
                    MontosFGEM.leerFGEM(txtMontoFGEM.getText(), "Monto"), montoOpcionalFGEM(txtTasaFGEM.getText(), "Tasa anual"),
                    montoOpcionalFGEM(txtCuotaFGEM.getText(), "Cuota mensual"), vencimientoFGEM, cmbCuentaFGEM.cuentaIdFGEM());
        }
    }
}
