package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.prediccion.LecturaPrediccionFGEM;
import com.businessanalyzer.analytics.prediccion.MetodoPronosticoFGEM;
import com.businessanalyzer.analytics.prediccion.PronosticadorFGEM;
import com.businessanalyzer.analytics.prediccion.PronosticoFGEM;
import com.businessanalyzer.analytics.prediccion.ResultadoPrediccionFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.service.PrediccionServiceFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.GraficosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.TareasFGEM;
import com.businessanalyzer.ui.TarjetaIndicadorFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Function;

public class PrediccionesControllerFGEM implements VistaModuloFGEM {

    private static final int MESES_HISTORIA_GRAFICO_FGEM = 12;

    private final AppContextFGEM contextoFGEM;
    private MonedaFGEM monedaFGEM = MonedaFGEM.PEN;
    private long solicitudActualFGEM;

    private final TarjetaIndicadorFGEM kpiVentasFGEM = new TarjetaIndicadorFGEM("Ventas proyectadas", "fth-trending-up");
    private final TarjetaIndicadorFGEM kpiCambioFGEM = new TarjetaIndicadorFGEM("Cambio esperado", "fth-repeat");
    private final TarjetaIndicadorFGEM kpiUtilidadFGEM = new TarjetaIndicadorFGEM("Utilidad proyectada", "fth-dollar-sign");
    private final TarjetaIndicadorFGEM kpiConfiabilidadFGEM = new TarjetaIndicadorFGEM("Confiabilidad", "fth-check-circle");

    @FXML private ComboBox<Integer> cmbHorizonteFGEM;
    @FXML private ProgressIndicator progresoFGEM;
    @FXML private Label lblHistoriaFGEM;
    @FXML private HBox indicadoresFGEM;
    @FXML private Tab tabVentasFGEM;
    @FXML private Tab tabUtilidadFGEM;
    @FXML private Tab tabMetodosFGEM;

    public PrediccionesControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        indicadoresFGEM.getChildren().addAll(kpiVentasFGEM, kpiCambioFGEM, kpiUtilidadFGEM, kpiConfiabilidadFGEM);
        cmbHorizonteFGEM.getItems().setAll(3, 6, 12);
        cmbHorizonteFGEM.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer mesesFGEM) {
                return mesesFGEM == null ? "" : "Próximos " + mesesFGEM + " meses";
            }

            @Override
            public Integer fromString(String textoFGEM) {
                return null;
            }
        });
        cmbHorizonteFGEM.setValue(6);
        cmbHorizonteFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> cargarFGEM());
        tabMetodosFGEM.setContent(explicacionMetodosFGEM());
    }

    @Override
    public void alMostrarFGEM() {
        cargarFGEM();
    }

    private void cargarFGEM() {
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        if (empresaFGEM == null || cmbHorizonteFGEM.getValue() == null) {
            Label avisoFGEM = new Label("Selecciona una empresa en la barra superior para ver sus proyecciones.");
            avisoFGEM.getStyleClass().add("muted");
            tabVentasFGEM.setContent(new StackPane(avisoFGEM));
            tabUtilidadFGEM.setContent(new StackPane());
            return;
        }
        monedaFGEM = empresaFGEM.monedaFGEM();
        int horizonteFGEM = cmbHorizonteFGEM.getValue();
        progresoFGEM.setVisible(true);
        long numeroFGEM = ++solicitudActualFGEM;
        TareasFGEM.ejecutarFGEM(() -> contextoFGEM.prediccionesFGEM().predecirFGEM(horizonteFGEM),
                resultadoFGEM -> {
                    if (numeroFGEM == solicitudActualFGEM) {
                        progresoFGEM.setVisible(false);
                        mostrarFGEM(resultadoFGEM);
                    }
                },
                errorFGEM -> {
                    if (numeroFGEM == solicitudActualFGEM) {
                        progresoFGEM.setVisible(false);
                        NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(errorFGEM));
                    }
                });
    }

    private void mostrarFGEM(ResultadoPrediccionFGEM rFGEM) {
        lblHistoriaFGEM.setText(rFGEM.mesesFGEM().isEmpty() ? "Sin meses completos con ventas"
                : "Basado en " + rFGEM.mesesFGEM().size() + (rFGEM.mesesFGEM().size() == 1 ? " mes completo" : " meses completos")
                + " de historia (" + nombreMesFGEM(rFGEM.mesesFGEM().get(0)) + " a " + nombreMesFGEM(rFGEM.mesesFGEM().get(rFGEM.mesesFGEM().size() - 1)) + ")");
        mostrarIndicadoresFGEM(rFGEM);
        List<String> lecturaFGEM = LecturaPrediccionFGEM.generarFGEM(rFGEM, monedaFGEM);
        contextoFGEM.mostrarFraseFGEM(com.businessanalyzer.ui.ModuloFGEM.PREDICCIONES, rFGEM.pronosticoVentasFGEM() == null || lecturaFGEM.isEmpty() ? null : lecturaFGEM.get(0));
        tabVentasFGEM.setContent(contenidoSerieFGEM(rFGEM, rFGEM.pronosticoVentasFGEM(), rFGEM.ventasFGEM(), lecturaFGEM));
        tabUtilidadFGEM.setContent(contenidoSerieFGEM(rFGEM, rFGEM.pronosticoUtilidadFGEM(), rFGEM.utilidadFGEM(), null));
    }

    private void mostrarIndicadoresFGEM(ResultadoPrediccionFGEM rFGEM) {
        PronosticoFGEM ventasFGEM = rFGEM.pronosticoVentasFGEM();
        if (!ventasFGEM.suficienteFGEM()) {
            for (TarjetaIndicadorFGEM kpiFGEM : List.of(kpiVentasFGEM, kpiCambioFGEM, kpiUtilidadFGEM, kpiConfiabilidadFGEM)) {
                kpiFGEM.actualizarFGEM(TarjetaIndicadorFGEM.SIN_VALOR_FGEM, "Se necesitan " + PronosticadorFGEM.MINIMO_DATOS_FGEM + " meses completos");
            }
            return;
        }
        YearMonth primeroFGEM = rFGEM.primerMesProyectadoFGEM(YearMonth.now());
        kpiVentasFGEM.actualizarFGEM(redondeadoFGEM(ventasFGEM.totalFGEM()), "Próximos " + rFGEM.horizonteFGEM() + " meses, desde " + nombreMesFGEM(primeroFGEM));
        double anterioresFGEM = rFGEM.ventasUltimosMesesFGEM();
        if (anterioresFGEM > 0 && rFGEM.mesesComparadosFGEM() == rFGEM.horizonteFGEM()) {
            double cambioFGEM = ventasFGEM.totalFGEM() / anterioresFGEM - 1;
            kpiCambioFGEM.actualizarFGEM((cambioFGEM >= 0 ? "+" : "") + porcentajeFGEM(cambioFGEM),
                    "Últimos " + rFGEM.horizonteFGEM() + " meses: " + dineroFGEM(anterioresFGEM));
        } else {
            kpiCambioFGEM.actualizarFGEM(TarjetaIndicadorFGEM.SIN_VALOR_FGEM, "Historia más corta que el horizonte");
        }
        PronosticoFGEM utilidadFGEM = rFGEM.pronosticoUtilidadFGEM();
        kpiUtilidadFGEM.actualizarFGEM(redondeadoFGEM(utilidadFGEM.totalFGEM()), utilidadFGEM.totalFGEM() >= 0 ? "Ganancia estimada del período"
                : "Pérdida estimada del período");
        kpiConfiabilidadFGEM.actualizarFGEM(ventasFGEM.confiabilidadFGEM().descripcionFGEM(),
                "Error típico " + porcentajeFGEM(ventasFGEM.errorRelativoFGEM()) + " · " + ventasFGEM.elegidaFGEM().metodoFGEM().nombreFGEM());
    }

    private Node contenidoSerieFGEM(ResultadoPrediccionFGEM rFGEM, PronosticoFGEM pFGEM, double[] historiaFGEM, List<String> lecturaFGEM) {
        if (!pFGEM.suficienteFGEM()) {
            Label avisoFGEM = new Label("Se necesitan al menos " + PronosticadorFGEM.MINIMO_DATOS_FGEM + " meses completos con ventas "
                    + "para proyectar (hay " + pFGEM.datosFGEM() + "). Sigue registrando tus operaciones y vuelve más adelante.");
            avisoFGEM.getStyleClass().add("muted");
            avisoFGEM.setWrapText(true);
            VBox cajaFGEM = new VBox(avisoFGEM);
            cajaFGEM.getStyleClass().addAll("card", "tab-contenido");
            return cajaFGEM;
        }

        VBox tarjetaGraficoFGEM = tarjetaFGEM(pFGEM.serieFGEM() + ": historia y proyección");
        StackPane graficoFGEM = new StackPane(GraficosFGEM.visorFGEM(GraficosFGEM.pronosticoFGEM(rFGEM.mesesFGEM(), historiaFGEM, pFGEM,
                MESES_HISTORIA_GRAFICO_FGEM, monedaFGEM)));
        VBox.setVgrow(graficoFGEM, Priority.ALWAYS);
        graficoFGEM.setMinHeight(200);
        tarjetaGraficoFGEM.getChildren().add(graficoFGEM);
        VBox.setVgrow(tarjetaGraficoFGEM, Priority.ALWAYS);

        VBox izquierdaFGEM = new VBox(12, tarjetaGraficoFGEM);
        if (lecturaFGEM != null) {
            VBox tarjetaLecturaFGEM = tarjetaFGEM(null);
            for (String fraseFGEM : lecturaFGEM) {
                Label puntoFGEM = new Label("•  " + fraseFGEM);
                puntoFGEM.setWrapText(true);
                puntoFGEM.setMinHeight(Region.USE_PREF_SIZE);
                puntoFGEM.getStyleClass().add("lectura");
                tarjetaLecturaFGEM.getChildren().add(puntoFGEM);
            }
            ScrollPane desplazableFGEM = new ScrollPane(tarjetaLecturaFGEM);
            desplazableFGEM.setFitToWidth(true);
            desplazableFGEM.getStyleClass().add("dialog-scroll");
            desplazableFGEM.setPrefHeight(150);
            desplazableFGEM.setMinHeight(110);
            izquierdaFGEM.getChildren().add(desplazableFGEM);
        }
        HBox.setHgrow(izquierdaFGEM, Priority.ALWAYS);

        VBox derechaFGEM = tarjetaFGEM("Método elegido");
        Label metodoFGEM = new Label(pFGEM.elegidaFGEM().metodoFGEM().nombreFGEM());
        metodoFGEM.getStyleClass().add("total-valor-destacado");
        Label explicacionFGEM = new Label(pFGEM.elegidaFGEM().metodoFGEM().explicacionFGEM());
        explicacionFGEM.getStyleClass().add("muted");
        explicacionFGEM.setWrapText(true);
        explicacionFGEM.setMinHeight(Region.USE_PREF_SIZE);
        derechaFGEM.getChildren().addAll(metodoFGEM, explicacionFGEM, subtituloFGEM("Proyección mensual"), tablaPuntosFGEM(rFGEM, pFGEM),
                subtituloFGEM("Error medio de cada método con tus datos"), tablaMetodosFGEM(pFGEM));
        ScrollPane panelDerechoFGEM = new ScrollPane(derechaFGEM);
        panelDerechoFGEM.setFitToWidth(true);
        panelDerechoFGEM.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        panelDerechoFGEM.getStyleClass().add("dialog-scroll");
        panelDerechoFGEM.setPrefWidth(450);
        panelDerechoFGEM.setMinWidth(400);

        HBox contenidoFGEM = new HBox(16, izquierdaFGEM, panelDerechoFGEM);
        contenidoFGEM.getStyleClass().add("tab-contenido");
        return contenidoFGEM;
    }

    private TableView<PronosticoFGEM.PuntoFGEM> tablaPuntosFGEM(ResultadoPrediccionFGEM rFGEM, PronosticoFGEM pFGEM) {
        YearMonth primeroFGEM = rFGEM.primerMesProyectadoFGEM(YearMonth.now());
        TableView<PronosticoFGEM.PuntoFGEM> tablaFGEM = new TableView<>(FXCollections.observableArrayList(pFGEM.puntosFGEM()));
        TableColumn<PronosticoFGEM.PuntoFGEM, String> mesFGEM = columnaFGEM("Mes", puntoFGEM -> mesCortoFGEM(primeroFGEM.plusMonths(puntoFGEM.hFGEM() - 1L)), 80, false);
        mesFGEM.setMinWidth(70);
        tablaFGEM.getColumns().add(mesFGEM);
        tablaFGEM.getColumns().add(columnaFGEM("Proyección", puntoFGEM -> enteroFGEM(puntoFGEM.valorFGEM()), 95, true));
        tablaFGEM.getColumns().add(columnaFGEM("Rango 95 %", puntoFGEM -> enteroFGEM(puntoFGEM.inferiorFGEM()) + " – "
                + enteroFGEM(puntoFGEM.superiorFGEM()).replace(monedaFGEM.simboloFGEM() + " ", ""), 150, true));
        tablaFGEM.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaFGEM.getStyleClass().add("tabla-compacta");
        tablaFGEM.setFixedCellSize(32);
        tablaFGEM.setPrefHeight(Math.min(32 * pFGEM.puntosFGEM().size() + 42, 32 * 6 + 42));
        tablaFGEM.setMinHeight(Region.USE_PREF_SIZE);
        return tablaFGEM;
    }

    private TableView<PronosticoFGEM.EvaluacionFGEM> tablaMetodosFGEM(PronosticoFGEM pFGEM) {
        TableView<PronosticoFGEM.EvaluacionFGEM> tablaFGEM = new TableView<>(FXCollections.observableArrayList(pFGEM.evaluacionesFGEM()));
        tablaFGEM.getColumns().add(columnaFGEM("Método", eFGEM -> (eFGEM == pFGEM.elegidaFGEM() ? "✓ " : "   ") + eFGEM.metodoFGEM().nombreFGEM(), 230, false));
        tablaFGEM.getColumns().add(columnaFGEM("Error medio", eFGEM -> dineroFGEM(eFGEM.maeFGEM()), 100, true));
        tablaFGEM.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaFGEM.getStyleClass().add("tabla-compacta");
        tablaFGEM.setFixedCellSize(32);
        tablaFGEM.setPrefHeight(32 * pFGEM.evaluacionesFGEM().size() + 42);
        tablaFGEM.setMinHeight(Region.USE_PREF_SIZE);
        tablaFGEM.setTooltip(new javafx.scene.control.Tooltip("Error medio al pronosticar cada uno de los "
                + pFGEM.elegidaFGEM().mesesEvaluadosFGEM() + " meses validados con los datos anteriores a él"));
        return tablaFGEM;
    }

    private String enteroFGEM(double valorFGEM) {
        return monedaFGEM.simboloFGEM() + " " + new java.text.DecimalFormat("#,##0",
                java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US)).format(Math.round(valorFGEM));
    }

    private static String mesCortoFGEM(YearMonth mesFGEM) {
        return nombreMesFGEM(mesFGEM).substring(0, 3) + " " + String.valueOf(mesFGEM.getYear()).substring(2);
    }

    private Node explicacionMetodosFGEM() {
        VBox cajaFGEM = tarjetaFGEM("Cómo se proyecta");
        cajaFGEM.getChildren().add(parrafoFGEM("Se usan solo meses completos (el mes en curso es el primero que se proyecta) y "
                + "hasta " + PrediccionServiceFGEM.MESES_HISTORIA_FGEM + " meses de historia."));
        for (MetodoPronosticoFGEM metodoFGEM : PronosticadorFGEM.METODOS_FGEM) {
            Label nombreFGEM = new Label(metodoFGEM.nombreFGEM());
            nombreFGEM.getStyleClass().add("step-title");
            cajaFGEM.getChildren().addAll(nombreFGEM, parrafoFGEM(metodoFGEM.explicacionFGEM()));
        }
        cajaFGEM.getChildren().addAll(
                subtituloFGEM("Cómo se elige el método"),
                parrafoFGEM("Cada método \"predice\" cada mes pasado usando solo los meses anteriores a él, y se compara con lo "
                        + "que realmente se vendió. Se elige el que tuvo el menor error medio. Así el sistema usa el método "
                        + "que mejor funciona con los datos de tu negocio, no uno fijo."),
                subtituloFGEM("Rango probable"),
                parrafoFGEM("Es un intervalo aproximado al 95 %: pronóstico ± 1.96 × error histórico × √(meses de distancia). "
                        + "Se ensancha cuanto más lejos está el mes y cuanto más se equivocó el método en el pasado."),
                subtituloFGEM("Confiabilidad"),
                parrafoFGEM("Alta: error típico menor al 10 % de una venta mensual. Media: hasta 25 %. Baja: mayor, o menos de "
                        + "3 meses para validar."),
                subtituloFGEM("Limitaciones"),
                parrafoFGEM("El método de Holt-Winters, que capta la estacionalidad anual (por ejemplo, campañas de diciembre), "
                        + "necesita al menos 24 meses de historia y se incorporará cuando haya datos suficientes. Ningún "
                        + "pronóstico anticipa cambios bruscos del mercado."));
        ScrollPane desplazableFGEM = new ScrollPane(cajaFGEM);
        desplazableFGEM.setFitToWidth(true);
        desplazableFGEM.getStyleClass().addAll("dialog-scroll", "tab-contenido");
        return desplazableFGEM;
    }

    private static VBox tarjetaFGEM(String tituloFGEM) {
        VBox cajaFGEM = new VBox(8);
        cajaFGEM.getStyleClass().add("card");
        if (tituloFGEM != null) {
            Label labelFGEM = new Label(tituloFGEM);
            labelFGEM.getStyleClass().add("card-title");
            cajaFGEM.getChildren().add(labelFGEM);
        }
        return cajaFGEM;
    }

    private static Label subtituloFGEM(String textoFGEM) {
        Label labelFGEM = new Label(textoFGEM);
        labelFGEM.getStyleClass().add("step-title");
        labelFGEM.setPadding(new javafx.geometry.Insets(6, 0, 0, 0));
        return labelFGEM;
    }

    private static Label parrafoFGEM(String textoFGEM) {
        Label labelFGEM = new Label(textoFGEM);
        labelFGEM.getStyleClass().add("muted");
        labelFGEM.setWrapText(true);
        labelFGEM.setMinHeight(Region.USE_PREF_SIZE);
        return labelFGEM;
    }

    private static <T> TableColumn<T, String> columnaFGEM(String tituloFGEM, Function<T, String> valorFGEM, double anchoFGEM,
                                                      boolean numeroFGEM) {
        TableColumn<T, String> columnaFGEM = new TableColumn<>(tituloFGEM);
        columnaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(valorFGEM.apply(cFGEM.getValue())));
        columnaFGEM.setPrefWidth(anchoFGEM);
        columnaFGEM.setSortable(false);
        if (numeroFGEM) {
            columnaFGEM.getStyleClass().add("col-numero");
        }
        return columnaFGEM;
    }

    private String redondeadoFGEM(double valorFGEM) {
        return Double.isNaN(valorFGEM) ? "—" : MontosFGEM.formatearFGEM(BigDecimal.valueOf(valorFGEM).setScale(0, RoundingMode.HALF_UP), monedaFGEM)
                .replaceAll("\\.00$", "");
    }

    private String dineroFGEM(double valorFGEM) {
        return Double.isNaN(valorFGEM) ? "—" : MontosFGEM.formatearFGEM(BigDecimal.valueOf(valorFGEM).setScale(2, RoundingMode.HALF_UP), monedaFGEM);
    }

    private static String porcentajeFGEM(double fraccionFGEM) {
        return Double.isNaN(fraccionFGEM) ? "—" : MontosFGEM.porcentajeFGEM(BigDecimal.valueOf(fraccionFGEM));
    }

    private static String nombreMesFGEM(YearMonth mesFGEM) {
        return FechasFGEM.nombreMesFGEM(mesFGEM.getMonthValue()).toLowerCase() + " " + mesFGEM.getYear();
    }
}
