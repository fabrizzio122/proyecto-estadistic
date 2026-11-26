package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.AnalisisFinancieroFGEM;
import com.businessanalyzer.analytics.EstadoResultadosFGEM;
import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM;
import com.businessanalyzer.analytics.VariacionFGEM;
import com.businessanalyzer.analytics.diagnostico.DiagnosticoFGEM;
import com.businessanalyzer.analytics.sugerencias.AvisosSugerenciasFGEM;
import com.businessanalyzer.analytics.sugerencias.AvisosSugerenciasFGEM.AvisoFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.service.TesoreriaServiceFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.GraficosFGEM;
import com.businessanalyzer.ui.ModuloFGEM;
import com.businessanalyzer.ui.TareasFGEM;
import com.businessanalyzer.ui.TarjetaIndicadorFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class DashboardControllerFGEM implements VistaModuloFGEM {

    private static final int MAX_PRODUCTOS_FGEM = 10;
    private static final int MAX_AVISOS_FGEM = 4;

    private final AppContextFGEM contextoFGEM;
    private long solicitudFGEM;
    private MonedaFGEM monedaFGEM;

    private final TarjetaIndicadorFGEM kpiVentasFGEM = new TarjetaIndicadorFGEM("Ventas del mes", "fth-shopping-cart");
    private final TarjetaIndicadorFGEM kpiUtilidadFGEM = new TarjetaIndicadorFGEM("Utilidad neta", "fth-dollar-sign");
    private final TarjetaIndicadorFGEM kpiMargenFGEM = new TarjetaIndicadorFGEM("Margen neto", "fth-percent");
    private final TarjetaIndicadorFGEM kpiDineroFGEM = new TarjetaIndicadorFGEM("Dinero disponible", "fth-credit-card");

    private record PanelFGEM(AnalisisFinancieroFGEM analisisFGEM, TesoreriaServiceFGEM.ResumenFGEM tesoreriaFGEM,
                             SugerenciasProductoFGEM.ResultadoFGEM sugerenciasFGEM, String errorSugerenciasFGEM, DiagnosticoFGEM diagnosticoFGEM) {
    }

    @FXML private HBox bannerEmpresaFGEM;
    @FXML private FontIcon iconoBannerFGEM;
    @FXML private Label lblBannerTituloFGEM;
    @FXML private Label lblBannerDetalleFGEM;
    @FXML private VBox contenidoFGEM;
    @FXML private HBox indicadoresFGEM;
    @FXML private StackPane graficoFGEM;
    @FXML private VBox avisosFGEM;
    @FXML private TableView<RentabilidadProductoFGEM> tablaProductosFGEM;
    @FXML private TableColumn<RentabilidadProductoFGEM, String> colProductoFGEM;
    @FXML private TableColumn<RentabilidadProductoFGEM, String> colCantidadFGEM;
    @FXML private TableColumn<RentabilidadProductoFGEM, String> colVentasFGEM;
    @FXML private TableColumn<RentabilidadProductoFGEM, String> colMargenFGEM;
    @FXML private Button btnAnalisisFGEM;
    @FXML private Button btnSugerenciasFGEM;

    public DashboardControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        indicadoresFGEM.getChildren().addAll(kpiVentasFGEM, kpiUtilidadFGEM, kpiMargenFGEM, kpiDineroFGEM);
        boolean analisisFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.ANALISIS_VER);
        btnAnalisisFGEM.setVisible(analisisFGEM);
        btnSugerenciasFGEM.setVisible(analisisFGEM);
        colProductoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(cFGEM.getValue().nombreFGEM()));
        colCantidadFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(MontosFGEM.cantidadFGEM(cFGEM.getValue().cantidadFGEM())));
        colVentasFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(MontosFGEM.formatearFGEM(cFGEM.getValue().ventasFGEM(), monedaFGEM)));
        colMargenFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(MontosFGEM.formatearFGEM(cFGEM.getValue().margenFGEM(), monedaFGEM)));
        tablaProductosFGEM.setPlaceholder(new Label("Aún no hay ventas en el mes en curso."));
    }

    @Override
    public void alMostrarFGEM() {
        Optional<EmpresaFGEM> empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM();
        if (empresaFGEM.isEmpty()) {
            avisoFGEM("No hay una empresa activa", contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.EMPRESAS_GESTIONAR)
                    ? "Registra tu primera empresa en el módulo Empresas (o crea la «Bodega Demo» para probar) "
                    + "o elige una en el selector de la barra superior."
                    : "Pide a un administrador que te asigne una empresa para empezar a trabajar.");
            return;
        }
        if (!contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.ANALISIS_VER)) {
            avisoFGEM("Tu rol no incluye los análisis", "Registra ventas, compras y gastos desde el menú.");
            return;
        }
        monedaFGEM = empresaFGEM.get().monedaFGEM();
        LocalDate hoyFGEM = LocalDate.now();
        PeriodoFGEM mesFGEM = new PeriodoFGEM(hoyFGEM.withDayOfMonth(1), hoyFGEM);
        long numeroFGEM = ++solicitudFGEM;
        TareasFGEM.ejecutarFGEM(() -> cargarFGEM(mesFGEM), panelFGEM -> {
            if (numeroFGEM == solicitudFGEM) {
                mostrarFGEM(panelFGEM);
            }
        }, errorFGEM -> {
            if (numeroFGEM == solicitudFGEM) {
                avisoFGEM("No se pudo cargar el resumen", ErroresFGEM.mensajeFGEM(errorFGEM));
            }
        });
    }

    private PanelFGEM cargarFGEM(PeriodoFGEM mesFGEM) {
        AnalisisFinancieroFGEM analisisFGEM = contextoFGEM.analisisFGEM().analizarFGEM(mesFGEM.desdeFGEM(), mesFGEM.hastaFGEM());
        TesoreriaServiceFGEM.ResumenFGEM tesoreriaFGEM = null;
        try {
            tesoreriaFGEM = contextoFGEM.tesoreriaFGEM().resumenFGEM();
        } catch (RuntimeException eFGEM) {
        }
        SugerenciasProductoFGEM.ResultadoFGEM sugerenciasFGEM = null;
        String errorSugerenciasFGEM = null;
        try {
            sugerenciasFGEM = contextoFGEM.sugerenciasFGEM().sugerenciasActualesFGEM();
        } catch (RuntimeException eFGEM) {
            errorSugerenciasFGEM = ErroresFGEM.mensajeFGEM(eFGEM);
        }
        DiagnosticoFGEM diagnosticoFGEM = null;
        try {
            diagnosticoFGEM = contextoFGEM.diagnosticoFGEM().diagnosticarFGEM();
        } catch (RuntimeException eFGEM) {
        }
        return new PanelFGEM(analisisFGEM, tesoreriaFGEM, sugerenciasFGEM, errorSugerenciasFGEM, diagnosticoFGEM);
    }

    private void avisoFGEM(String tituloFGEM, String detalleFGEM) {
        bannerEmpresaFGEM.setVisible(true);
        bannerEmpresaFGEM.setManaged(true);
        lblBannerTituloFGEM.setText(tituloFGEM);
        lblBannerDetalleFGEM.setText(detalleFGEM);
        contenidoFGEM.setVisible(false);
        contenidoFGEM.setManaged(false);
    }

    private void mostrarFGEM(PanelFGEM pFGEM) {
        bannerEmpresaFGEM.setVisible(false);
        bannerEmpresaFGEM.setManaged(false);
        contenidoFGEM.setVisible(true);
        contenidoFGEM.setManaged(true);
        mostrarIndicadoresFGEM(pFGEM.analisisFGEM(), pFGEM.tesoreriaFGEM());
        graficoFGEM.getChildren().setAll(GraficosFGEM.visorFGEM(GraficosFGEM.evolucionMensualFGEM(pFGEM.analisisFGEM().evolucionFGEM(), monedaFGEM)));
        mostrarAvisosFGEM(pFGEM.sugerenciasFGEM(), pFGEM.errorSugerenciasFGEM());
        tablaProductosFGEM.getItems().setAll(pFGEM.analisisFGEM().productosFGEM().stream()
                .sorted(Comparator.comparing(RentabilidadProductoFGEM::ventasFGEM).reversed())
                .limit(MAX_PRODUCTOS_FGEM)
                .toList());
        contextoFGEM.mostrarAlertasDiagnosticoFGEM(pFGEM.diagnosticoFGEM() == null || !pFGEM.diagnosticoFGEM().evaluadoFGEM()
                ? 0 : pFGEM.diagnosticoFGEM().recomendacionesFGEM().size());
    }

    private void mostrarIndicadoresFGEM(AnalisisFinancieroFGEM aFGEM, TesoreriaServiceFGEM.ResumenFGEM tesoreriaFGEM) {
        EstadoResultadosFGEM eFGEM = aFGEM.actualFGEM();
        EstadoResultadosFGEM anteriorFGEM = aFGEM.anteriorFGEM();
        PeriodoFGEM previoFGEM = aFGEM.periodoAnteriorFGEM();
        String comparadoFGEM = " vs. " + previoFGEM.desdeFGEM().getDayOfMonth() + " – " + previoFGEM.hastaFGEM().getDayOfMonth() + " "
                + FechasFGEM.nombreMesFGEM(previoFGEM.desdeFGEM().getMonthValue()).toLowerCase().substring(0, 3);

        kpiVentasFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(eFGEM.ventasNetasFGEM(), monedaFGEM),
                variacionFGEM(eFGEM.ventasNetasFGEM(), anteriorFGEM.ventasNetasFGEM(), comparadoFGEM));
        kpiUtilidadFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(eFGEM.utilidadNetaFGEM(), monedaFGEM),
                variacionFGEM(eFGEM.utilidadNetaFGEM(), anteriorFGEM.utilidadNetaFGEM(), comparadoFGEM));
        kpiMargenFGEM.actualizarFGEM(MontosFGEM.porcentajeFGEM(eFGEM.margenNetoFGEM()),
                "Margen bruto " + MontosFGEM.porcentajeFGEM(eFGEM.margenBrutoFGEM()));
        kpiDineroFGEM.setVisible(tesoreriaFGEM != null);
        kpiDineroFGEM.setManaged(tesoreriaFGEM != null);
        if (tesoreriaFGEM != null) {
            kpiDineroFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(tesoreriaFGEM.disponibleFGEM(), monedaFGEM),
                    "Por cobrar: " + MontosFGEM.formatearFGEM(tesoreriaFGEM.porCobrarFGEM(), monedaFGEM));
        }
    }

    private static String variacionFGEM(BigDecimal actualFGEM, BigDecimal anteriorFGEM, String comparadoFGEM) {
        VariacionFGEM vFGEM = new VariacionFGEM(actualFGEM, anteriorFGEM);
        if (vFGEM.relativaFGEM() == null) {
            return "Sin comparación";
        }
        return (vFGEM.relativaFGEM().signum() >= 0 ? "+" : "−") + MontosFGEM.porcentajeFGEM(vFGEM.relativaFGEM().abs()) + comparadoFGEM;
    }

    private void mostrarAvisosFGEM(SugerenciasProductoFGEM.ResultadoFGEM rFGEM, String errorFGEM) {
        avisosFGEM.getChildren().clear();
        if (rFGEM == null) {
            avisosFGEM.getChildren().add(textoFGEM(errorFGEM != null ? errorFGEM : "Sin datos."));
            return;
        }
        List<AvisoFGEM> listaFGEM = AvisosSugerenciasFGEM.avisosFGEM(rFGEM);
        if (listaFGEM.isEmpty()) {
            avisosFGEM.getChildren().add(textoFGEM("No hay avisos."));
            return;
        }
        for (AvisoFGEM aFGEM : listaFGEM.subList(0, Math.min(MAX_AVISOS_FGEM, listaFGEM.size()))) {
            avisosFGEM.getChildren().add(textoFGEM(aFGEM.tituloFGEM() + " (" + aFGEM.cantidadFGEM() + "): " + aFGEM.detalleFGEM()));
        }
    }

    private static Label textoFGEM(String textoFGEM) {
        Label lFGEM = new Label(textoFGEM);
        lFGEM.setWrapText(true);
        lFGEM.setMinHeight(Region.USE_PREF_SIZE);
        return lFGEM;
    }

    @FXML
    private void irSugerenciasFGEM() {
        contextoFGEM.abrirModuloFGEM(ModuloFGEM.SUGERENCIAS);
    }

    @FXML
    private void irAnalisisFGEM() {
        contextoFGEM.abrirModuloFGEM(ModuloFGEM.ANALISIS_FINANCIERO);
    }
}
