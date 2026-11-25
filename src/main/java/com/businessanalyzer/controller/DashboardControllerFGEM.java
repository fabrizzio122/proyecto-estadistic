package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.AnalisisFinancieroFGEM;
import com.businessanalyzer.analytics.EstadoResultadosFGEM;
import com.businessanalyzer.analytics.FraseMesFGEM;
import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.PuntoEquilibrioFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM;
import com.businessanalyzer.analytics.VariacionFGEM;
import com.businessanalyzer.analytics.diagnostico.DiagnosticoFGEM;
import com.businessanalyzer.analytics.diagnostico.HallazgoFGEM;
import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;
import com.businessanalyzer.analytics.prediccion.PronosticoFGEM;
import com.businessanalyzer.analytics.prediccion.ResultadoPrediccionFGEM;
import com.businessanalyzer.analytics.sugerencias.AvisosSugerenciasFGEM;
import com.businessanalyzer.analytics.sugerencias.AvisosSugerenciasFGEM.AvisoFGEM;
import com.businessanalyzer.analytics.sugerencias.AvisosSugerenciasFGEM.GravedadFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.service.TesoreriaServiceFGEM;
import com.businessanalyzer.ui.BarraProporcionFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.GraficoMesesFGEM;
import com.businessanalyzer.ui.MedidorSaludFGEM;
import com.businessanalyzer.ui.ModuloFGEM;
import com.businessanalyzer.ui.TareasFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Panel principal: el mes en curso resumido en una frase y cuatro indicadores (comparados con los mismos
 * días del mes anterior), la salud del negocio, las ventas y la utilidad de los últimos 12 meses con el
 * cierre esperado del mes, lo que viene, los productos que más aportan y lo que hay que atender hoy (agotados,
 * perecibles por vencer, productos con pérdida). Cada tarjeta lleva al módulo
 * donde se ve el detalle. Todo sale de los datos reales de la empresa activa.
 */
public class DashboardControllerFGEM implements VistaModuloFGEM {

    private static final int MAX_PRODUCTOS_FGEM = 6;
    private static final int MAX_ALERTAS_FGEM = 2;
    private static final int MAX_AVISOS_FGEM = 3;

    private final AppContextFGEM contextoFGEM;
    private long solicitudFGEM;

    /** Lo que muestra el panel; cada parte puede faltar (sin permiso o sin datos). */
    private record PanelFGEM(AnalisisFinancieroFGEM analisisFGEM, DiagnosticoFGEM diagnosticoFGEM, ResultadoPrediccionFGEM prediccionFGEM,
                         String errorDiagnosticoFGEM, String errorPrediccionFGEM, TesoreriaServiceFGEM.ResumenFGEM tesoreriaFGEM,
                         SugerenciasProductoFGEM.ResultadoFGEM sugerenciasFGEM, String errorSugerenciasFGEM) {
    }

    @FXML private HBox bannerEmpresaFGEM;
    @FXML private FontIcon iconoBannerFGEM;
    @FXML private Label lblBannerTituloFGEM;
    @FXML private Label lblBannerDetalleFGEM;
    @FXML private VBox contenidoFGEM;
    @FXML private VBox tarjetaMesFGEM;
    @FXML private VBox saludFGEM;
    @FXML private VBox evolucionFGEM;
    @FXML private VBox proximoFGEM;
    @FXML private VBox productosFGEM;
    @FXML private VBox atenderFGEM;
    @FXML private Label lblTituloProductosFGEM;
    @FXML private Button btnAnalisisFGEM;
    @FXML private Button btnDiagnosticoFGEM;
    @FXML private Button btnVentasFGEM;
    @FXML private Button btnPrediccionesFGEM;
    @FXML private Button btnSugerenciasFGEM;

    public DashboardControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        boolean analisisFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.ANALISIS_VER);
        for (Button bFGEM : List.of(btnAnalisisFGEM, btnDiagnosticoFGEM, btnPrediccionesFGEM, btnSugerenciasFGEM)) {
            bFGEM.setVisible(analisisFGEM);
        }
        btnVentasFGEM.setVisible(contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.OPERACIONES_VER));
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
        MonedaFGEM monedaFGEM = empresaFGEM.get().monedaFGEM();
        LocalDate hoyFGEM = LocalDate.now();
        PeriodoFGEM mesFGEM = new PeriodoFGEM(hoyFGEM.withDayOfMonth(1), hoyFGEM);
        long numeroFGEM = ++solicitudFGEM;
        TareasFGEM.ejecutarFGEM(() -> cargarFGEM(mesFGEM), panelFGEM -> {
            if (numeroFGEM == solicitudFGEM) {
                mostrarFGEM(panelFGEM, monedaFGEM, hoyFGEM);
            }
        }, errorFGEM -> {
            if (numeroFGEM == solicitudFGEM) {
                avisoFGEM("No se pudo cargar el resumen", ErroresFGEM.mensajeFGEM(errorFGEM));
            }
        });
    }

    private PanelFGEM cargarFGEM(PeriodoFGEM mesFGEM) {
        AnalisisFinancieroFGEM analisisFGEM = contextoFGEM.analisisFGEM().analizarFGEM(mesFGEM.desdeFGEM(), mesFGEM.hastaFGEM());
        DiagnosticoFGEM diagnosticoFGEM = null;
        String errorDiagnosticoFGEM = null;
        try {
            diagnosticoFGEM = contextoFGEM.diagnosticoFGEM().diagnosticarFGEM();
        } catch (RuntimeException eFGEM) {
            errorDiagnosticoFGEM = ErroresFGEM.mensajeFGEM(eFGEM);
        }
        ResultadoPrediccionFGEM prediccionFGEM = null;
        String errorPrediccionFGEM = null;
        try {
            prediccionFGEM = contextoFGEM.prediccionesFGEM().predecirFGEM(3);
        } catch (RuntimeException eFGEM) {
            errorPrediccionFGEM = ErroresFGEM.mensajeFGEM(eFGEM);
        }
        TesoreriaServiceFGEM.ResumenFGEM tesoreriaFGEM = null;
        try {
            tesoreriaFGEM = contextoFGEM.tesoreriaFGEM().resumenFGEM();
        } catch (RuntimeException eFGEM) {
            // Sin permiso de finanzas o sin cuentas: ese indicador no se muestra.
        }
        SugerenciasProductoFGEM.ResultadoFGEM sugerenciasFGEM = null;
        String errorSugerenciasFGEM = null;
        try {
            sugerenciasFGEM = contextoFGEM.sugerenciasFGEM().sugerenciasActualesFGEM();
        } catch (RuntimeException eFGEM) {
            errorSugerenciasFGEM = ErroresFGEM.mensajeFGEM(eFGEM);
        }
        return new PanelFGEM(analisisFGEM, diagnosticoFGEM, prediccionFGEM, errorDiagnosticoFGEM, errorPrediccionFGEM, tesoreriaFGEM,
                sugerenciasFGEM, errorSugerenciasFGEM);
    }

    private void avisoFGEM(String tituloFGEM, String detalleFGEM) {
        bannerEmpresaFGEM.setVisible(true);
        bannerEmpresaFGEM.setManaged(true);
        lblBannerTituloFGEM.setText(tituloFGEM);
        lblBannerDetalleFGEM.setText(detalleFGEM);
        contenidoFGEM.setVisible(false);
        contenidoFGEM.setManaged(false);
    }

    private void mostrarFGEM(PanelFGEM pFGEM, MonedaFGEM monedaFGEM, LocalDate hoyFGEM) {
        bannerEmpresaFGEM.setVisible(false);
        bannerEmpresaFGEM.setManaged(false);
        contenidoFGEM.setVisible(true);
        contenidoFGEM.setManaged(true);
        Double esperadoVentasFGEM = esperadoFGEM(pFGEM.prediccionFGEM(), true, hoyFGEM);
        Double esperadoUtilidadFGEM = esperadoFGEM(pFGEM.prediccionFGEM(), false, hoyFGEM);
        mostrarMesFGEM(pFGEM.analisisFGEM(), pFGEM.tesoreriaFGEM(), monedaFGEM);
        mostrarSaludFGEM(pFGEM.diagnosticoFGEM(), pFGEM.errorDiagnosticoFGEM());
        mostrarEvolucionFGEM(pFGEM.analisisFGEM(), esperadoVentasFGEM, esperadoUtilidadFGEM, monedaFGEM, hoyFGEM);
        mostrarProximoFGEM(pFGEM.prediccionFGEM(), pFGEM.errorPrediccionFGEM(), pFGEM.analisisFGEM().actualFGEM().ventasNetasFGEM(), monedaFGEM, hoyFGEM);
        mostrarProductosFGEM(pFGEM.analisisFGEM(), monedaFGEM);
        mostrarAtenderFGEM(pFGEM.sugerenciasFGEM(), pFGEM.errorSugerenciasFGEM(), monedaFGEM);
        contextoFGEM.mostrarAlertasDiagnosticoFGEM(pFGEM.diagnosticoFGEM() == null || !pFGEM.diagnosticoFGEM().evaluadoFGEM()
                ? 0 : pFGEM.diagnosticoFGEM().recomendacionesFGEM().size());
    }

    // ------------------------------------------------------------------ tu mes hasta hoy

    private void mostrarMesFGEM(AnalisisFinancieroFGEM aFGEM, TesoreriaServiceFGEM.ResumenFGEM tesoreriaFGEM, MonedaFGEM monedaFGEM) {
        EstadoResultadosFGEM eFGEM = aFGEM.actualFGEM();
        EstadoResultadosFGEM anteriorFGEM = aFGEM.anteriorFGEM();
        PuntoEquilibrioFGEM peFGEM = aFGEM.equilibrioFGEM();
        PeriodoFGEM previoFGEM = aFGEM.periodoAnteriorFGEM();
        String mesAnteriorFGEM = FechasFGEM.nombreMesFGEM(previoFGEM.desdeFGEM().getMonthValue()).toLowerCase();
        String comparadoFGEM = " vs. " + previoFGEM.desdeFGEM().getDayOfMonth() + " – " + previoFGEM.hastaFGEM().getDayOfMonth() + " "
                + mesAnteriorFGEM.substring(0, 3);

        Label tuFGEM = etiquetaFGEM("TU ", "etiqueta-seccion");
        Label mesFGEM = etiquetaFGEM("MES HASTA HOY", "etiqueta-seccion-acento");
        HBox kickerFGEM = new HBox(tuFGEM, mesFGEM);

        FraseMesFGEM fraseFGEM = FraseMesFGEM.deFGEM(eFGEM.ventasNetasFGEM(), anteriorFGEM.ventasNetasFGEM(), mesAnteriorFGEM, peFGEM.coberturaFGEM(),
                peFGEM.ventasFaltantesFGEM() == null ? "" : MontosFGEM.formatearFGEM(peFGEM.ventasFaltantesFGEM(), monedaFGEM));
        TextFlow textoFGEM = new TextFlow();
        textoFGEM.getStyleClass().add("frase-mes");
        for (FraseMesFGEM.ParteFGEM parteFGEM : fraseFGEM.partesFGEM()) {
            Text tFGEM = new Text(parteFGEM.textoFGEM());
            tFGEM.getStyleClass().addAll("frase-mes-texto", switch (parteFGEM.tonoFGEM()) {
                case POSITIVO -> "frase-positiva";
                case NEGATIVO -> "frase-negativa";
                case NORMAL -> "frase-normal";
            });
            textoFGEM.getChildren().add(tFGEM);
        }

        GridPane grillaFGEM = new GridPane();
        grillaFGEM.setHgap(28);
        grillaFGEM.setVgap(22);
        for (int iFGEM = 0; iFGEM < 3; iFGEM++) {
            ColumnConstraints colFGEM = new ColumnConstraints();
            colFGEM.setPercentWidth(33.3);
            grillaFGEM.getColumnConstraints().add(colFGEM);
        }
        grillaFGEM.add(indicadorFGEM("Ventas del mes", MontosFGEM.formatearFGEM(eFGEM.ventasNetasFGEM(), monedaFGEM),
                variacionFGEM(eFGEM.ventasNetasFGEM(), anteriorFGEM.ventasNetasFGEM(), comparadoFGEM)), 0, 0);
        grillaFGEM.add(indicadorFGEM("Utilidad neta", MontosFGEM.formatearFGEM(eFGEM.utilidadNetaFGEM(), monedaFGEM),
                eFGEM.utilidadNetaFGEM().signum() < 0 ? detalleFGEM("El mes va con pérdida", "texto-alerta")
                        : variacionFGEM(eFGEM.utilidadNetaFGEM(), anteriorFGEM.utilidadNetaFGEM(), comparadoFGEM)), 1, 0);
        grillaFGEM.add(indicadorFGEM("Margen neto", MontosFGEM.porcentajeFGEM(eFGEM.margenNetoFGEM()),
                detalleFGEM("Margen bruto " + MontosFGEM.porcentajeFGEM(eFGEM.margenBrutoFGEM()), "texto-tenue")), 2, 0);
        grillaFGEM.add(equilibrioFGEM(peFGEM, monedaFGEM), 0, 1);
        if (tesoreriaFGEM != null) {
            grillaFGEM.add(indicadorFGEM("Dinero disponible", MontosFGEM.formatearFGEM(tesoreriaFGEM.disponibleFGEM(), monedaFGEM),
                    tesoreriaFGEM.cuentasNegativasFGEM() > 0 ? detalleFGEM("Hay cuentas en negativo", "texto-alerta")
                            : tesoreriaFGEM.porCobrarVencidoFGEM().signum() > 0
                            ? detalleFGEM("Cobros vencidos: " + MontosFGEM.formatearFGEM(tesoreriaFGEM.porCobrarVencidoFGEM(), monedaFGEM),
                            "texto-alerta")
                            : detalleFGEM("Por cobrar: " + MontosFGEM.formatearFGEM(tesoreriaFGEM.porCobrarFGEM(), monedaFGEM), "texto-tenue")),
                    1, 1);
        }
        Region lineaFGEM = new Region();
        lineaFGEM.getStyleClass().add("separador");
        tarjetaMesFGEM.getChildren().setAll(kickerFGEM, textoFGEM, lineaFGEM, grillaFGEM);
    }

    private VBox indicadorFGEM(String tituloFGEM, String valorFGEM, Node detalleFGEM) {
        Label lblTituloFGEM = etiquetaFGEM(tituloFGEM, "indicador-titulo");
        Label lblValorFGEM = etiquetaFGEM(valorFGEM, "indicador-valor");
        lblValorFGEM.setMinWidth(Region.USE_PREF_SIZE);
        return new VBox(4, lblTituloFGEM, lblValorFGEM, detalleFGEM);
    }

    /** «215 % cubierto» con una barra que muestra las ventas y una marca en el punto de equilibrio. */
    private VBox equilibrioFGEM(PuntoEquilibrioFGEM peFGEM, MonedaFGEM monedaFGEM) {
        if (peFGEM.ventasEquilibrioFGEM() == null) {
            return indicadorFGEM("Punto de equilibrio", "No alcanzable", detalleFGEM(peFGEM.ventasFGEM().signum() == 0
                    ? "Sin ventas este mes" : "Las ventas no cubren sus costos variables", "texto-alerta"));
        }
        BigDecimal coberturaFGEM = peFGEM.coberturaFGEM();
        double ventasFGEM = peFGEM.ventasFGEM().doubleValue();
        double metaFGEM = peFGEM.ventasEquilibrioFGEM().doubleValue();
        double escalaFGEM = Math.max(ventasFGEM, metaFGEM);
        BarraProporcionFGEM barraFGEM = new BarraProporcionFGEM(6, peFGEM.alcanzadoFGEM() ? "relleno-exito" : "relleno-acento");
        barraFGEM.llenarFGEM(ventasFGEM / escalaFGEM).marcarFGEM(metaFGEM / escalaFGEM);
        barraFGEM.setMaxWidth(220);
        Tooltip.install(barraFGEM, new Tooltip("La marca es el punto de equilibrio: "
                + MontosFGEM.formatearFGEM(peFGEM.ventasEquilibrioFGEM(), monedaFGEM)));
        Label tituloFGEM = etiquetaFGEM("Punto de equilibrio · " + MontosFGEM.formatearFGEM(peFGEM.ventasEquilibrioFGEM(), monedaFGEM),
                "indicador-titulo");
        Label valorFGEM = etiquetaFGEM(coberturaFGEM.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP)
                + " % cubierto", "indicador-valor");
        valorFGEM.setMinWidth(Region.USE_PREF_SIZE);
        return new VBox(4, tituloFGEM, valorFGEM, barraFGEM);
    }

    private Node variacionFGEM(BigDecimal actualFGEM, BigDecimal anteriorFGEM, String comparadoFGEM) {
        VariacionFGEM vFGEM = new VariacionFGEM(actualFGEM, anteriorFGEM);
        if (vFGEM.relativaFGEM() == null) {
            return detalleFGEM("Sin comparación", "texto-tenue");
        }
        boolean subeFGEM = vFGEM.relativaFGEM().signum() >= 0;
        return detalleFGEM((subeFGEM ? "▲ " : "▼ ") + MontosFGEM.porcentajeFGEM(vFGEM.relativaFGEM().abs()) + comparadoFGEM,
                subeFGEM ? "texto-positivo" : "texto-alerta");
    }

    private static Label detalleFGEM(String textoFGEM, String claseFGEM) {
        Label lFGEM = etiquetaFGEM(textoFGEM, "indicador-detalle");
        lFGEM.getStyleClass().add(claseFGEM);
        return lFGEM;
    }

    // ------------------------------------------------------------------ salud del negocio

    private void mostrarSaludFGEM(DiagnosticoFGEM dFGEM, String errorFGEM) {
        saludFGEM.getChildren().clear();
        if (dFGEM == null || !dFGEM.evaluadoFGEM()) {
            Label sinDatosFGEM = etiquetaFGEM(errorFGEM != null ? errorFGEM : dFGEM == null ? "Sin datos." : String.join(" ", dFGEM.resumenFGEM()),
                    "muted");
            sinDatosFGEM.setWrapText(true);
            saludFGEM.getChildren().add(sinDatosFGEM);
            return;
        }
        MedidorSaludFGEM medidorFGEM = new MedidorSaludFGEM(40);
        medidorFGEM.mostrarFGEM(dFGEM.puntajeFGEM(), claseFGEM(dFGEM.nivelFGEM().colorFGEM()));
        Label nivelFGEM = etiquetaFGEM(dFGEM.nivelFGEM().descripcionFGEM(), "nivel-salud");
        nivelFGEM.getStyleClass().add(claseFGEM(dFGEM.nivelFGEM().colorFGEM()));
        nivelFGEM.setMinWidth(Region.USE_PREF_SIZE);
        Label evaluadoFGEM = etiquetaFGEM("Evaluado: " + dFGEM.periodoFGEM().descripcionFGEM(), "indicador-detalle");
        evaluadoFGEM.getStyleClass().add("texto-tenue");
        evaluadoFGEM.setWrapText(true);
        VBox ladoFGEM = new VBox(8, nivelFGEM, evaluadoFGEM);
        ladoFGEM.setAlignment(Pos.CENTER_LEFT);
        HBox cabeceraFGEM = new HBox(18, medidorFGEM, ladoFGEM);
        cabeceraFGEM.setAlignment(Pos.CENTER_LEFT);
        Region lineaFGEM = new Region();
        lineaFGEM.getStyleClass().add("separador");
        saludFGEM.getChildren().addAll(cabeceraFGEM, lineaFGEM);

        List<HallazgoFGEM> pendientesFGEM = dFGEM.recomendacionesFGEM();
        if (pendientesFGEM.isEmpty()) {
            Label bienFGEM = etiquetaFGEM("Todos los indicadores están en verde. ¡Buen trabajo!", "lectura");
            bienFGEM.setWrapText(true);
            saludFGEM.getChildren().add(bienFGEM);
            return;
        }
        saludFGEM.getChildren().add(etiquetaFGEM("Atender primero", "indicador-titulo"));
        for (HallazgoFGEM hFGEM : pendientesFGEM.subList(0, Math.min(MAX_ALERTAS_FGEM, pendientesFGEM.size()))) {
            Label nombreFGEM = etiquetaFGEM(hFGEM.tipoFGEM().nombreFGEM(), "fila-alerta-texto");
            nombreFGEM.setWrapText(true);
            nombreFGEM.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nombreFGEM, Priority.ALWAYS);
            Label valorFGEM = etiquetaFGEM(hFGEM.valorFGEM(), "fila-alerta-valor");
            valorFGEM.setMinWidth(Region.USE_PREF_SIZE);
            HBox filaFGEM = new HBox(10, nombreFGEM, valorFGEM);
            filaFGEM.getStyleClass().add("fila-alerta");
            if (hFGEM.semaforoFGEM() == SemaforoFGEM.AMARILLO) {
                filaFGEM.getStyleClass().add("fila-alerta-media");
            }
            filaFGEM.setAlignment(Pos.CENTER_LEFT);
            if (hFGEM.recomendacionFGEM() != null) {
                Tooltip.install(filaFGEM, new Tooltip(hFGEM.recomendacionFGEM()));
            }
            saludFGEM.getChildren().add(filaFGEM);
        }
        if (pendientesFGEM.size() > MAX_ALERTAS_FGEM) {
            saludFGEM.getChildren().add(etiquetaFGEM("y " + (pendientesFGEM.size() - MAX_ALERTAS_FGEM) + " más en el diagnóstico",
                    "hint"));
        }
    }

    // ------------------------------------------------------------------ últimos 12 meses

    private void mostrarEvolucionFGEM(AnalisisFinancieroFGEM aFGEM, Double esperadoVentasFGEM, Double esperadoUtilidadFGEM, MonedaFGEM monedaFGEM,
                                  LocalDate hoyFGEM) {
        List<AnalisisFinancieroFGEM.ResultadoMensualFGEM> mesesFGEM = aFGEM.evolucionFGEM();
        if (mesesFGEM.isEmpty()) {
            evolucionFGEM.getChildren().setAll(etiquetaFGEM("Aún no hay meses con movimientos.", "muted"));
            return;
        }
        double[] ventasFGEM = mesesFGEM.stream().mapToDouble(mFGEM -> mFGEM.estadoFGEM().ventasNetasFGEM().doubleValue()).toArray();
        double[] utilidadFGEM = mesesFGEM.stream().mapToDouble(mFGEM -> mFGEM.estadoFGEM().utilidadNetaFGEM().doubleValue()).toArray();
        evolucionFGEM.getChildren().setAll(new GraficoMesesFGEM(
                mesesFGEM.stream().map(AnalisisFinancieroFGEM.ResultadoMensualFGEM::mesFGEM).toList(), ventasFGEM, utilidadFGEM,
                esperadoVentasFGEM, esperadoUtilidadFGEM, YearMonth.from(hoyFGEM), hoyFGEM.getDayOfMonth(), monedaFGEM));
    }

    /** Cierre esperado del mes en curso según las predicciones; {@code null} si no hay pronóstico. */
    private static Double esperadoFGEM(ResultadoPrediccionFGEM rFGEM, boolean ventasFGEM, LocalDate hoyFGEM) {
        if (rFGEM == null) {
            return null;
        }
        PronosticoFGEM pFGEM = ventasFGEM ? rFGEM.pronosticoVentasFGEM() : rFGEM.pronosticoUtilidadFGEM();
        if (pFGEM == null || pFGEM.puntosFGEM().isEmpty() || !rFGEM.primerMesProyectadoFGEM(YearMonth.from(hoyFGEM)).equals(YearMonth.from(hoyFGEM))) {
            return null;
        }
        return pFGEM.puntosFGEM().get(0).valorFGEM();
    }

    // ------------------------------------------------------------------ lo que viene

    private void mostrarProximoFGEM(ResultadoPrediccionFGEM rFGEM, String errorFGEM, BigDecimal vendidoFGEM, MonedaFGEM monedaFGEM, LocalDate hoyFGEM) {
        proximoFGEM.getChildren().clear();
        PronosticoFGEM ventasFGEM = rFGEM == null ? null : rFGEM.pronosticoVentasFGEM();
        if (ventasFGEM == null || ventasFGEM.puntosFGEM().isEmpty()) {
            Label sinFGEM = etiquetaFGEM(errorFGEM != null ? errorFGEM
                    : "Se necesitan al menos unos meses completos de ventas para proyectar.", "muted");
            sinFGEM.setWrapText(true);
            proximoFGEM.getChildren().add(sinFGEM);
            return;
        }
        YearMonth mesFGEM = rFGEM.primerMesProyectadoFGEM(YearMonth.from(hoyFGEM));
        PronosticoFGEM.PuntoFGEM pFGEM = ventasFGEM.puntosFGEM().get(0);
        double inferiorFGEM = Math.max(0, pFGEM.inferiorFGEM());
        double superiorFGEM = Math.max(pFGEM.superiorFGEM(), pFGEM.valorFGEM());
        double margenFGEM = (superiorFGEM - inferiorFGEM) * 0.18 + 1;
        double desdeFGEM = Math.max(0, inferiorFGEM - margenFGEM);
        double hastaFGEM = superiorFGEM + margenFGEM;

        Label tituloFGEM = etiquetaFGEM("Ventas esperadas en " + FechasFGEM.nombreMesFGEM(mesFGEM.getMonthValue()).toLowerCase(),
                "indicador-titulo");
        Label valorFGEM = etiquetaFGEM(aproximadoFGEM(pFGEM.valorFGEM(), monedaFGEM), "cifra-proyectada");
        BarraProporcionFGEM rangoFGEM = new BarraProporcionFGEM(8, "relleno-acento");
        rangoFGEM.resaltarFGEM((inferiorFGEM - desdeFGEM) / (hastaFGEM - desdeFGEM), (superiorFGEM - desdeFGEM) / (hastaFGEM - desdeFGEM))
                .marcarFGEM((pFGEM.valorFGEM() - desdeFGEM) / (hastaFGEM - desdeFGEM));
        Label izquierdaFGEM = etiquetaFGEM(sinAproximadoFGEM(inferiorFGEM, monedaFGEM), "rango-extremo");
        Label centroFGEM = etiquetaFGEM("rango probable", "rango-centro");
        Label derechaFGEM = etiquetaFGEM(sinAproximadoFGEM(superiorFGEM, monedaFGEM), "rango-extremo");
        centroFGEM.setMaxWidth(Double.MAX_VALUE);
        centroFGEM.setAlignment(Pos.CENTER);
        HBox.setHgrow(centroFGEM, Priority.ALWAYS);
        HBox extremosFGEM = new HBox(izquierdaFGEM, centroFGEM, derechaFGEM);
        Region lineaFGEM = new Region();
        lineaFGEM.getStyleClass().add("separador");
        proximoFGEM.getChildren().addAll(tituloFGEM, valorFGEM, rangoFGEM, extremosFGEM, lineaFGEM);

        if (mesFGEM.equals(YearMonth.from(hoyFGEM)) && pFGEM.valorFGEM() > 0) {
            BigDecimal avanceFGEM = vendidoFGEM.divide(BigDecimal.valueOf(pFGEM.valorFGEM()), 4, RoundingMode.HALF_UP);
            proximoFGEM.getChildren().add(filaDatoFGEM("Llevas vendido",
                    avanceFGEM.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP) + " % de lo esperado",
                    "dato-valor"));
        }
        PronosticoFGEM utilidadFGEM = rFGEM.pronosticoUtilidadFGEM();
        if (utilidadFGEM != null && !utilidadFGEM.puntosFGEM().isEmpty()) {
            double uFGEM = utilidadFGEM.puntosFGEM().get(0).valorFGEM();
            proximoFGEM.getChildren().add(filaDatoFGEM(uFGEM >= 0 ? "Utilidad esperada" : "Pérdida esperada",
                    aproximadoFGEM(Math.abs(uFGEM), monedaFGEM), uFGEM >= 0 ? "texto-positivo" : "texto-alerta"));
        }
        Label confiabilidadFGEM = etiquetaFGEM("Confiabilidad " + ventasFGEM.confiabilidadFGEM().descripcionFGEM().toLowerCase() + " · "
                + ventasFGEM.elegidaFGEM().metodoFGEM().nombreFGEM().toLowerCase(), "hint");
        confiabilidadFGEM.setWrapText(true);
        proximoFGEM.getChildren().add(confiabilidadFGEM);
    }

    private static HBox filaDatoFGEM(String claveFGEM, String valorFGEM, String claseValorFGEM) {
        Label lblClaveFGEM = etiquetaFGEM(claveFGEM, "dato-clave");
        Region espacioFGEM = new Region();
        HBox.setHgrow(espacioFGEM, Priority.ALWAYS);
        Label lblValorFGEM = etiquetaFGEM(valorFGEM, "dato-valor");
        lblValorFGEM.getStyleClass().add(claseValorFGEM);
        lblValorFGEM.setMinWidth(Region.USE_PREF_SIZE);
        HBox filaFGEM = new HBox(8, lblClaveFGEM, espacioFGEM, lblValorFGEM);
        filaFGEM.setAlignment(Pos.CENTER_LEFT);
        return filaFGEM;
    }

    // ------------------------------------------------------------------ productos

    private void mostrarProductosFGEM(AnalisisFinancieroFGEM aFGEM, MonedaFGEM monedaFGEM) {
        List<RentabilidadProductoFGEM> listaFGEM = aFGEM.productosFGEM().stream()
                .sorted(Comparator.comparing(RentabilidadProductoFGEM::margenFGEM).reversed())
                .limit(MAX_PRODUCTOS_FGEM)
                .toList();
        if (listaFGEM.isEmpty()) {
            productosFGEM.getChildren().setAll(etiquetaFGEM("Aún no hay ventas en el mes en curso.", "muted"));
            return;
        }
        double maximoFGEM = Math.max(1, listaFGEM.get(0).margenFGEM().doubleValue());
        GridPane grillaFGEM = new GridPane();
        grillaFGEM.setHgap(18);
        grillaFGEM.setVgap(12);
        ColumnConstraints nombresFGEM = new ColumnConstraints();
        nombresFGEM.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints barrasFGEM = new ColumnConstraints();
        barrasFGEM.setHgrow(Priority.ALWAYS);
        ColumnConstraints valoresFGEM = new ColumnConstraints();
        valoresFGEM.setMinWidth(Region.USE_PREF_SIZE);
        valoresFGEM.setHalignment(javafx.geometry.HPos.RIGHT);
        grillaFGEM.getColumnConstraints().addAll(nombresFGEM, barrasFGEM, valoresFGEM);
        for (int iFGEM = 0; iFGEM < listaFGEM.size(); iFGEM++) {
            RentabilidadProductoFGEM rpFGEM = listaFGEM.get(iFGEM);
            double margenFGEM = rpFGEM.margenFGEM().doubleValue();
            Label nombreFGEM = etiquetaFGEM(rpFGEM.nombreFGEM(), "producto-nombre");
            nombreFGEM.setMinWidth(Region.USE_PREF_SIZE);
            BarraProporcionFGEM barraFGEM = new BarraProporcionFGEM(10, iFGEM == 0 ? "relleno-acento" : "relleno-neutro");
            barraFGEM.llenarFGEM(Math.max(0, margenFGEM) / maximoFGEM);
            Label valorFGEM = etiquetaFGEM(MontosFGEM.formatearFGEM(rpFGEM.margenFGEM().setScale(0, RoundingMode.HALF_UP), monedaFGEM)
                    .replaceAll("\\.00$", ""), "producto-valor");
            if (margenFGEM < 0) {
                valorFGEM.getStyleClass().add("texto-alerta");
            }
            grillaFGEM.addRow(iFGEM, nombreFGEM, barraFGEM, valorFGEM);
        }
        productosFGEM.getChildren().setAll(grillaFGEM);
    }

    // ------------------------------------------------------------------ atender hoy

    /** Lo urgente de las sugerencias: qué reponer, qué ofrecer antes de que se venza y qué precios no cubren el costo. */
    private void mostrarAtenderFGEM(SugerenciasProductoFGEM.ResultadoFGEM rFGEM, String errorFGEM, MonedaFGEM monedaFGEM) {
        atenderFGEM.getChildren().clear();
        if (rFGEM == null) {
            Label sinFGEM = etiquetaFGEM(errorFGEM != null ? errorFGEM : "Sin datos.", "muted");
            sinFGEM.setWrapText(true);
            atenderFGEM.getChildren().add(sinFGEM);
            return;
        }
        List<AvisoFGEM> avisosFGEM = AvisosSugerenciasFGEM.avisosFGEM(rFGEM);
        if (avisosFGEM.isEmpty()) {
            Label bienFGEM = etiquetaFGEM("Nada urgente: no hay agotados, perecibles por vencer ni productos que se vendan con pérdida.",
                    "lectura");
            bienFGEM.setWrapText(true);
            atenderFGEM.getChildren().add(bienFGEM);
        }
        for (AvisoFGEM aFGEM : avisosFGEM.subList(0, Math.min(MAX_AVISOS_FGEM, avisosFGEM.size()))) {
            Label tituloFGEM = etiquetaFGEM(aFGEM.tituloFGEM(), "fila-alerta-texto");
            tituloFGEM.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(tituloFGEM, Priority.ALWAYS);
            Label cantidadFGEM = etiquetaFGEM(String.valueOf(aFGEM.cantidadFGEM()), "fila-alerta-valor");
            cantidadFGEM.setMinWidth(Region.USE_PREF_SIZE);
            HBox cabeceraFGEM = new HBox(10, tituloFGEM, cantidadFGEM);
            cabeceraFGEM.setAlignment(Pos.CENTER_LEFT);
            Label detalleFGEM = etiquetaFGEM(aFGEM.detalleFGEM(), "fila-alerta-detalle");
            detalleFGEM.setWrapText(true);
            detalleFGEM.setMinHeight(Region.USE_PREF_SIZE);
            VBox filaFGEM = new VBox(2, cabeceraFGEM, detalleFGEM);
            filaFGEM.getStyleClass().add("fila-alerta");
            if (aFGEM.gravedadFGEM() == GravedadFGEM.MEDIA) {
                filaFGEM.getStyleClass().add("fila-alerta-media");
            }
            atenderFGEM.getChildren().add(filaFGEM);
        }
        if (avisosFGEM.size() > MAX_AVISOS_FGEM) {
            atenderFGEM.getChildren().add(etiquetaFGEM("y " + (avisosFGEM.size() - MAX_AVISOS_FGEM) + " aviso(s) más en Sugerencias", "hint"));
        }
        Region lineaFGEM = new Region();
        lineaFGEM.getStyleClass().add("separador");
        atenderFGEM.getChildren().add(lineaFGEM);
        atenderFGEM.getChildren().add(filaDatoFGEM("Compra sugerida", rFGEM.reposicionFGEM().isEmpty() ? "Nada por ahora"
                : MontosFGEM.formatearFGEM(rFGEM.costoReposicionFGEM(), monedaFGEM), "dato-valor"));
        if (rFGEM.gananciaExtraMesFGEM().signum() > 0) {
            atenderFGEM.getChildren().add(filaDatoFGEM("Ganancia extra con precios", aproximadoFGEM(rFGEM.gananciaExtraMesFGEM().doubleValue(), monedaFGEM)
                    + " / mes", "texto-positivo"));
        }
    }

    // ------------------------------------------------------------------ navegación

    @FXML
    private void irSugerenciasFGEM() {
        contextoFGEM.abrirModuloFGEM(ModuloFGEM.SUGERENCIAS);
    }

    @FXML
    private void irAnalisisFGEM() {
        contextoFGEM.abrirModuloFGEM(ModuloFGEM.ANALISIS_FINANCIERO);
    }

    @FXML
    private void irDiagnosticoFGEM() {
        contextoFGEM.abrirModuloFGEM(ModuloFGEM.DIAGNOSTICO);
    }

    @FXML
    private void irVentasFGEM() {
        contextoFGEM.abrirModuloFGEM(ModuloFGEM.VENTAS_COMPRAS);
    }

    @FXML
    private void irPrediccionesFGEM() {
        contextoFGEM.abrirModuloFGEM(ModuloFGEM.PREDICCIONES);
    }

    // ------------------------------------------------------------------ utilidades

    /** Las proyecciones son aproximadas: se muestran en soles enteros. */
    private static String aproximadoFGEM(double valorFGEM, MonedaFGEM monedaFGEM) {
        return "≈ " + sinAproximadoFGEM(valorFGEM, monedaFGEM);
    }

    private static String sinAproximadoFGEM(double valorFGEM, MonedaFGEM monedaFGEM) {
        return MontosFGEM.formatearFGEM(BigDecimal.valueOf(valorFGEM).setScale(0, RoundingMode.HALF_UP), monedaFGEM)
                .replaceAll("\\.00$", "");
    }

    private static Label etiquetaFGEM(String textoFGEM, String claseFGEM) {
        Label lFGEM = new Label(textoFGEM);
        lFGEM.getStyleClass().add(claseFGEM);
        return lFGEM;
    }

    private static String claseFGEM(SemaforoFGEM semaforoFGEM) {
        return switch (semaforoFGEM) {
            case VERDE -> "semaforo-verde";
            case AMARILLO -> "semaforo-amarillo";
            case ROJO -> "semaforo-rojo";
            case SIN_DATOS -> "semaforo-sin-datos";
        };
    }
}
