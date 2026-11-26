package com.businessanalyzer.ui;

import com.businessanalyzer.analytics.AnalisisFinancieroFGEM.ResultadoMensualFGEM;
import com.businessanalyzer.analytics.PuntoEquilibrioFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM;
import com.businessanalyzer.analytics.estadistica.AnalisisAbcFGEM;
import com.businessanalyzer.analytics.estadistica.AnalisisEstadisticoFGEM;
import com.businessanalyzer.analytics.estadistica.EstacionalidadSemanalFGEM;
import com.businessanalyzer.analytics.estadistica.HistogramaFGEM;
import com.businessanalyzer.analytics.estadistica.TendenciaFGEM;
import com.businessanalyzer.analytics.prediccion.PronosticoFGEM;
import com.businessanalyzer.analytics.simulacion.ResultadoEscenarioFGEM;
import com.businessanalyzer.analytics.simulacion.SimuladorFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public final class GraficosFGEM {

    public static final Color AZUL_FGEM = new Color(0x44, 0x72, 0xc4);
    public static final Color VERDE_FGEM = new Color(0x70, 0xad, 0x47);
    public static final Color AMBAR_FGEM = new Color(0xa5, 0xa5, 0xa5);
    public static final Color ROJO_FGEM = new Color(0xc0, 0x00, 0x00);
    public static final Color NEUTRO_FGEM = new Color(0xbf, 0xbf, 0xbf);

    private static final Color FONDO_FGEM = Color.WHITE;
    private static final Color GRILLA_FGEM = new Color(0xd9, 0xd9, 0xd9);
    private static final Color TEXTO_FGEM = new Color(0x40, 0x40, 0x40);
    private static final Color TEXTO_FUERTE_FGEM = new Color(0x1f, 0x1f, 0x1f);
    private static final Font FUENTE_FGEM = new Font("Segoe UI", Font.PLAIN, 11);
    private static final BasicStroke LINEA_GRUESA_FGEM = new BasicStroke(2f);
    private static final BasicStroke LINEA_PUNTEADA_FGEM = new BasicStroke(1.5f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 5f}, 0f);
    private static final Locale ESPANOL_FGEM = Locale.forLanguageTag("es");

    private GraficosFGEM() {
    }

    public static ChartViewer visorFGEM(JFreeChart graficoFGEM) {
        ChartViewer visorFGEM = new ChartViewer(graficoFGEM);
        visorFGEM.setMinSize(200, 160);
        visorFGEM.setPrefSize(400, 240);
        return visorFGEM;
    }

    public static JFreeChart evolucionMensualFGEM(List<ResultadoMensualFGEM> mesesFGEM, MonedaFGEM monedaFGEM) {
        DefaultCategoryDataset ventasFGEM = new DefaultCategoryDataset();
        DefaultCategoryDataset utilidadFGEM = new DefaultCategoryDataset();
        for (ResultadoMensualFGEM mFGEM : mesesFGEM) {
            String etiquetaFGEM = etiquetaMesFGEM(mFGEM.mesFGEM());
            ventasFGEM.addValue(mFGEM.estadoFGEM().ventasNetasFGEM(), "Ventas netas", etiquetaFGEM);
            utilidadFGEM.addValue(mFGEM.estadoFGEM().utilidadNetaFGEM(), "Utilidad neta", etiquetaFGEM);
        }

        BarRenderer barrasFGEM = new BarRenderer();
        barrasFGEM.setBarPainter(new StandardBarPainter());
        barrasFGEM.setShadowVisible(false);
        barrasFGEM.setSeriesPaint(0, AZUL_FGEM);
        barrasFGEM.setItemMargin(0);
        barrasFGEM.setMaximumBarWidth(0.05);

        CategoryAxis ejeMesesFGEM = nuevoEjeMesesFGEM();
        ejeMesesFGEM.setCategoryMargin(0.1);
        CategoryPlot plotFGEM = new CategoryPlot(ventasFGEM, ejeMesesFGEM, ejeMontoFGEM(monedaFGEM), barrasFGEM);

        LineAndShapeRenderer lineaFGEM = new LineAndShapeRenderer(true, true);
        lineaFGEM.setSeriesPaint(0, VERDE_FGEM);
        lineaFGEM.setSeriesStroke(0, LINEA_GRUESA_FGEM);
        plotFGEM.setDataset(1, utilidadFGEM);
        plotFGEM.setRenderer(1, lineaFGEM);
        plotFGEM.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        plotFGEM.addRangeMarker(new ValueMarker(0, TEXTO_FGEM, new BasicStroke(1f)));

        return aplicarTemaFGEM(new JFreeChart(null, FUENTE_FGEM, plotFGEM, true));
    }

    public static JFreeChart flujoCajaFGEM(List<YearMonth> mesesFGEM, double[] entradasFGEM, double[] salidasFGEM, double[] saldoFGEM,
                                       MonedaFGEM monedaFGEM) {
        DefaultCategoryDataset barrasDatosFGEM = new DefaultCategoryDataset();
        DefaultCategoryDataset saldoDatosFGEM = new DefaultCategoryDataset();
        for (int iFGEM = 0; iFGEM < mesesFGEM.size(); iFGEM++) {
            String etiquetaFGEM = etiquetaFGEM(mesesFGEM, iFGEM);
            barrasDatosFGEM.addValue(entradasFGEM[iFGEM], "Entradas", etiquetaFGEM);
            barrasDatosFGEM.addValue(salidasFGEM[iFGEM], "Salidas", etiquetaFGEM);
            saldoDatosFGEM.addValue(saldoFGEM[iFGEM], "Dinero al cierre", etiquetaFGEM);
        }
        BarRenderer barrasFGEM = new BarRenderer();
        barrasFGEM.setBarPainter(new StandardBarPainter());
        barrasFGEM.setShadowVisible(false);
        barrasFGEM.setSeriesPaint(0, VERDE_FGEM);
        barrasFGEM.setSeriesPaint(1, ROJO_FGEM);
        barrasFGEM.setItemMargin(0.02);
        barrasFGEM.setMaximumBarWidth(0.05);

        CategoryAxis ejeMesesFGEM = nuevoEjeMesesFGEM();
        CategoryPlot plotFGEM = new CategoryPlot(barrasDatosFGEM, ejeMesesFGEM, ejeMontoFGEM(monedaFGEM), barrasFGEM);
        LineAndShapeRenderer lineaFGEM = new LineAndShapeRenderer(true, true);
        lineaFGEM.setSeriesPaint(0, AZUL_FGEM);
        lineaFGEM.setSeriesStroke(0, LINEA_GRUESA_FGEM);
        plotFGEM.setDataset(1, saldoDatosFGEM);
        plotFGEM.setRenderer(1, lineaFGEM);
        plotFGEM.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        plotFGEM.addRangeMarker(new ValueMarker(0, TEXTO_FGEM, new BasicStroke(1f)));
        return aplicarTemaFGEM(new JFreeChart(null, FUENTE_FGEM, plotFGEM, true));
    }

    public static JFreeChart proyeccionCajaFGEM(List<YearMonth> mesesFGEM, double[] baseFGEM, double[] pesimistaFGEM, MonedaFGEM monedaFGEM) {
        DefaultCategoryDataset datosFGEM = new DefaultCategoryDataset();
        for (int iFGEM = 0; iFGEM < mesesFGEM.size(); iFGEM++) {
            String etiquetaFGEM = etiquetaFGEM(mesesFGEM, iFGEM);
            datosFGEM.addValue(baseFGEM[iFGEM], "Escenario base", etiquetaFGEM);
            datosFGEM.addValue(pesimistaFGEM[iFGEM], "Escenario pesimista", etiquetaFGEM);
        }
        LineAndShapeRenderer lineasFGEM = new LineAndShapeRenderer(true, true);
        lineasFGEM.setSeriesPaint(0, AZUL_FGEM);
        lineasFGEM.setSeriesPaint(1, AMBAR_FGEM);
        lineasFGEM.setSeriesStroke(0, LINEA_GRUESA_FGEM);
        lineasFGEM.setSeriesStroke(1, new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[]{6f, 5f}, 0f));
        CategoryAxis ejeMesesFGEM = nuevoEjeMesesFGEM();
        CategoryPlot plotFGEM = new CategoryPlot(datosFGEM, ejeMesesFGEM, ejeMontoFGEM(monedaFGEM), lineasFGEM);
        plotFGEM.addRangeMarker(new ValueMarker(0, ROJO_FGEM, new BasicStroke(1.5f)));
        return aplicarTemaFGEM(new JFreeChart(null, FUENTE_FGEM, plotFGEM, true));
    }

    public static JFreeChart puntoEquilibrioFGEM(PuntoEquilibrioFGEM peFGEM, MonedaFGEM monedaFGEM) {
        double fijosFGEM = peFGEM.costosFijosFGEM().doubleValue();
        double razonVariableFGEM = peFGEM.razonCostoVariableFGEM() == null ? 1 : peFGEM.razonCostoVariableFGEM().doubleValue();
        double referenciaFGEM = Math.max(peFGEM.ventasFGEM().doubleValue(),
                peFGEM.ventasEquilibrioFGEM() == null ? 0 : peFGEM.ventasEquilibrioFGEM().doubleValue());
        double maximoFGEM = referenciaFGEM > 0 ? referenciaFGEM * 1.4 : Math.max(fijosFGEM * 2, 100);

        XYSeries ingresosFGEM = new XYSeries("Ingresos por ventas");
        XYSeries costosFGEM = new XYSeries("Costos totales");
        XYSeries costosFijosFGEM = new XYSeries("Costos fijos");
        for (double xFGEM : new double[]{0, maximoFGEM}) {
            ingresosFGEM.add(xFGEM, xFGEM);
            costosFGEM.add(xFGEM, fijosFGEM + razonVariableFGEM * xFGEM);
            costosFijosFGEM.add(xFGEM, fijosFGEM);
        }
        XYSeriesCollection datosFGEM = new XYSeriesCollection();
        datosFGEM.addSeries(ingresosFGEM);
        datosFGEM.addSeries(costosFGEM);
        datosFGEM.addSeries(costosFijosFGEM);

        XYLineAndShapeRenderer lineasFGEM = new XYLineAndShapeRenderer(true, false);
        lineasFGEM.setSeriesPaint(0, AZUL_FGEM);
        lineasFGEM.setSeriesStroke(0, LINEA_GRUESA_FGEM);
        lineasFGEM.setSeriesPaint(1, AMBAR_FGEM);
        lineasFGEM.setSeriesStroke(1, LINEA_GRUESA_FGEM);
        lineasFGEM.setSeriesPaint(2, TEXTO_FGEM);
        lineasFGEM.setSeriesStroke(2, LINEA_PUNTEADA_FGEM);

        NumberAxis ejeVentasFGEM = ejeMontoFGEM(monedaFGEM);
        ejeVentasFGEM.setLabel("Nivel de ventas");
        XYPlot plotFGEM = new XYPlot(datosFGEM, ejeVentasFGEM, ejeMontoFGEM(monedaFGEM), lineasFGEM);
        if (peFGEM.ventasEquilibrioFGEM() != null) {
            plotFGEM.addDomainMarker(marcadorFGEM(peFGEM.ventasEquilibrioFGEM().doubleValue(), "Equilibrio", VERDE_FGEM));
        }
        if (peFGEM.ventasFGEM().signum() > 0) {
            plotFGEM.addDomainMarker(marcadorFGEM(peFGEM.ventasFGEM().doubleValue(), "Ventas actuales", TEXTO_FUERTE_FGEM));
        }
        return aplicarTemaFGEM(new JFreeChart(null, FUENTE_FGEM, plotFGEM, true));
    }

    public static JFreeChart rentabilidadProductosFGEM(List<RentabilidadProductoFGEM> productosFGEM, int maximoFGEM, MonedaFGEM monedaFGEM) {
        DefaultCategoryDataset datosFGEM = new DefaultCategoryDataset();
        productosFGEM.stream().limit(maximoFGEM).forEach(pFGEM -> datosFGEM.addValue(pFGEM.margenFGEM(), "Margen bruto", abreviarFGEM(pFGEM.nombreFGEM(), 26)));

        BarRenderer barrasFGEM = new BarRenderer() {
            @Override
            public Paint getItemPaint(int filaFGEM, int columnaFGEM) {
                Number valorFGEM = datosFGEM.getValue(filaFGEM, columnaFGEM);
                return valorFGEM != null && valorFGEM.doubleValue() < 0 ? ROJO_FGEM : AZUL_FGEM;
            }
        };
        barrasFGEM.setBarPainter(new StandardBarPainter());
        barrasFGEM.setShadowVisible(false);
        barrasFGEM.setMaximumBarWidth(0.08);

        CategoryAxis ejeProductosFGEM = new CategoryAxis();
        ejeProductosFGEM.setMaximumCategoryLabelWidthRatio(0.45f);
        CategoryPlot plotFGEM = new CategoryPlot(datosFGEM, ejeProductosFGEM, ejeMontoFGEM(monedaFGEM), barrasFGEM);
        plotFGEM.setOrientation(PlotOrientation.HORIZONTAL);
        return aplicarTemaFGEM(new JFreeChart(null, FUENTE_FGEM, plotFGEM, false));
    }

    public static JFreeChart histogramaFGEM(HistogramaFGEM histogramaFGEM, MonedaFGEM monedaFGEM) {
        DefaultCategoryDataset datosFGEM = new DefaultCategoryDataset();
        DecimalFormat enteroFGEM = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
        for (HistogramaFGEM.ClaseFGEM claseFGEM : histogramaFGEM.clasesFGEM()) {
            datosFGEM.addValue(claseFGEM.frecuenciaFGEM(), "Ventas",
                    enteroFGEM.format(claseFGEM.desdeFGEM()) + "–" + enteroFGEM.format(claseFGEM.hastaFGEM()));
        }
        BarRenderer barrasFGEM = barrasSimplesFGEM(AZUL_FGEM);
        barrasFGEM.setItemMargin(0);
        CategoryAxis ejeRangosFGEM = new CategoryAxis("Monto de la venta (" + monedaFGEM.simboloFGEM() + ")");
        ejeRangosFGEM.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        ejeRangosFGEM.setCategoryMargin(0.05);
        NumberAxis ejeFrecuenciaFGEM = new NumberAxis("Cantidad de ventas");
        ejeFrecuenciaFGEM.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        return aplicarTemaFGEM(new JFreeChart(null, FUENTE_FGEM, new CategoryPlot(datosFGEM, ejeRangosFGEM, ejeFrecuenciaFGEM, barrasFGEM), false));
    }

    public static JFreeChart tendenciaFGEM(List<AnalisisEstadisticoFGEM.PuntoMensualFGEM> serieFGEM, TendenciaFGEM tendenciaFGEM, MonedaFGEM monedaFGEM) {
        DefaultCategoryDataset ventasFGEM = new DefaultCategoryDataset();
        DefaultCategoryDataset rectaFGEM = new DefaultCategoryDataset();
        for (int tFGEM = 0; tFGEM < serieFGEM.size(); tFGEM++) {
            String mesFGEM = etiquetaMesFGEM(serieFGEM.get(tFGEM).mesFGEM());
            ventasFGEM.addValue(serieFGEM.get(tFGEM).ventasFGEM(), "Ventas netas", mesFGEM);
            if (!Double.isNaN(tendenciaFGEM.pendienteFGEM())) {
                rectaFGEM.addValue(tendenciaFGEM.valorAjustadoFGEM(tFGEM), "Tendencia", mesFGEM);
            }
        }
        CategoryAxis ejeMesesFGEM = nuevoEjeMesesFGEM();
        CategoryPlot plotFGEM = new CategoryPlot(ventasFGEM, ejeMesesFGEM, ejeMontoFGEM(monedaFGEM), barrasSimplesFGEM(AZUL_FGEM));
        LineAndShapeRenderer lineaFGEM = new LineAndShapeRenderer(true, false);
        lineaFGEM.setSeriesPaint(0, AMBAR_FGEM);
        lineaFGEM.setSeriesStroke(0, LINEA_GRUESA_FGEM);
        plotFGEM.setDataset(1, rectaFGEM);
        plotFGEM.setRenderer(1, lineaFGEM);
        plotFGEM.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        return aplicarTemaFGEM(new JFreeChart(null, FUENTE_FGEM, plotFGEM, true));
    }

    public static JFreeChart diasDeLaSemanaFGEM(EstacionalidadSemanalFGEM estacionalidadFGEM, MonedaFGEM monedaFGEM) {
        DefaultCategoryDataset datosFGEM = new DefaultCategoryDataset();
        java.time.DayOfWeek mejorFGEM = estacionalidadFGEM.mejorDiaFGEM();
        for (java.time.DayOfWeek diaFGEM : java.time.DayOfWeek.values()) {
            Double promedioFGEM = estacionalidadFGEM.promedioPorDiaFGEM().get(diaFGEM);
            datosFGEM.addValue(promedioFGEM == null ? 0 : promedioFGEM, "Venta promedio",
                    diaFGEM.getDisplayName(TextStyle.SHORT, ESPANOL_FGEM).replace(".", ""));
        }
        int indiceMejorFGEM = mejorFGEM == null ? -1 : mejorFGEM.getValue() - 1;
        BarRenderer barrasFGEM = new BarRenderer() {
            @Override
            public Paint getItemPaint(int filaFGEM, int columnaFGEM) {
                return columnaFGEM == indiceMejorFGEM ? VERDE_FGEM : AZUL_FGEM;
            }
        };
        barrasFGEM.setBarPainter(new StandardBarPainter());
        barrasFGEM.setShadowVisible(false);
        barrasFGEM.setMaximumBarWidth(0.1);
        CategoryPlot plotFGEM = new CategoryPlot(datosFGEM, new CategoryAxis(), ejeMontoFGEM(monedaFGEM), barrasFGEM);
        if (!Double.isNaN(estacionalidadFGEM.promedioGeneralFGEM())) {
            ValueMarker promedioFGEM = new ValueMarker(estacionalidadFGEM.promedioGeneralFGEM(), AMBAR_FGEM, LINEA_PUNTEADA_FGEM);
            promedioFGEM.setLabel("Promedio diario");
            promedioFGEM.setLabelPaint(AMBAR_FGEM);
            promedioFGEM.setLabelFont(FUENTE_FGEM);
            promedioFGEM.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
            promedioFGEM.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);
            plotFGEM.addRangeMarker(promedioFGEM);
        }
        return aplicarTemaFGEM(new JFreeChart(null, FUENTE_FGEM, plotFGEM, false));
    }

    public static JFreeChart paretoFGEM(AnalisisAbcFGEM abcFGEM, int maximoFGEM, MonedaFGEM monedaFGEM) {
        DefaultCategoryDataset ventasFGEM = new DefaultCategoryDataset();
        DefaultCategoryDataset acumuladoFGEM = new DefaultCategoryDataset();
        List<AnalisisAbcFGEM.ItemFGEM> itemsFGEM = abcFGEM.itemsFGEM().stream().limit(maximoFGEM).toList();
        for (AnalisisAbcFGEM.ItemFGEM itemFGEM : itemsFGEM) {
            String nombreFGEM = abreviarFGEM(itemFGEM.productoFGEM().nombreFGEM(), 14);
            ventasFGEM.addValue(itemFGEM.productoFGEM().ventasFGEM(), "Ventas", nombreFGEM);
            acumuladoFGEM.addValue(itemFGEM.acumuladoFGEM().movePointRight(2), "% acumulado", nombreFGEM);
        }
        BarRenderer barrasFGEM = new BarRenderer() {
            @Override
            public Paint getItemPaint(int filaFGEM, int columnaFGEM) {
                return switch (itemsFGEM.get(columnaFGEM).claseFGEM()) {
                    case A -> AZUL_FGEM;
                    case B -> AMBAR_FGEM;
                    case C -> TEXTO_FGEM;
                };
            }
        };
        barrasFGEM.setBarPainter(new StandardBarPainter());
        barrasFGEM.setShadowVisible(false);
        barrasFGEM.setItemMargin(0);

        CategoryAxis ejeProductosFGEM = new CategoryAxis();
        ejeProductosFGEM.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        CategoryPlot plotFGEM = new CategoryPlot(ventasFGEM, ejeProductosFGEM, ejeMontoFGEM(monedaFGEM), barrasFGEM);

        NumberAxis ejePorcentajeFGEM = new NumberAxis("% acumulado");
        ejePorcentajeFGEM.setRange(0, 105);
        ejePorcentajeFGEM.setNumberFormatOverride(new DecimalFormat("0'%'"));
        plotFGEM.setRangeAxis(1, ejePorcentajeFGEM);
        plotFGEM.setDataset(1, acumuladoFGEM);
        plotFGEM.mapDatasetToRangeAxis(1, 1);
        LineAndShapeRenderer lineaFGEM = new LineAndShapeRenderer(true, true);
        lineaFGEM.setSeriesPaint(0, VERDE_FGEM);
        lineaFGEM.setSeriesStroke(0, LINEA_GRUESA_FGEM);
        plotFGEM.setRenderer(1, lineaFGEM);
        plotFGEM.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        ValueMarker ochentaFGEM = new ValueMarker(80, VERDE_FGEM, LINEA_PUNTEADA_FGEM);
        plotFGEM.addRangeMarker(1, ochentaFGEM, org.jfree.chart.ui.Layer.FOREGROUND);
        JFreeChart graficoFGEM = aplicarTemaFGEM(new JFreeChart(null, FUENTE_FGEM, plotFGEM, false));
        estiloPlotFGEM(ejePorcentajeFGEM);
        return graficoFGEM;
    }

    public static JFreeChart pronosticoFGEM(List<YearMonth> mesesFGEM, double[] historiaFGEM, PronosticoFGEM pronosticoFGEM,
                                        int mesesHistoriaFGEM, MonedaFGEM monedaFGEM) {
        DefaultCategoryDataset datosFGEM = new DefaultCategoryDataset();
        int desdeFGEM = Math.max(0, mesesFGEM.size() - mesesHistoriaFGEM);
        for (int iFGEM = desdeFGEM; iFGEM < mesesFGEM.size(); iFGEM++) {
            String mesFGEM = etiquetaMesConAnioFGEM(mesesFGEM.get(iFGEM));
            datosFGEM.addValue(historiaFGEM[iFGEM], "Historia", mesFGEM);
            boolean ultimoFGEM = iFGEM == mesesFGEM.size() - 1;
            datosFGEM.addValue(ultimoFGEM ? historiaFGEM[iFGEM] : null, "Pronóstico", mesFGEM);
            datosFGEM.addValue(ultimoFGEM ? historiaFGEM[iFGEM] : null, "Rango inferior", mesFGEM);
            datosFGEM.addValue(ultimoFGEM ? historiaFGEM[iFGEM] : null, "Rango superior", mesFGEM);
        }
        YearMonth siguienteFGEM = mesesFGEM.isEmpty() ? YearMonth.now() : mesesFGEM.get(mesesFGEM.size() - 1).plusMonths(1);
        for (PronosticoFGEM.PuntoFGEM puntoFGEM : pronosticoFGEM.puntosFGEM()) {
            String mesFGEM = etiquetaMesConAnioFGEM(siguienteFGEM.plusMonths(puntoFGEM.hFGEM() - 1L));
            datosFGEM.addValue(null, "Historia", mesFGEM);
            datosFGEM.addValue(puntoFGEM.valorFGEM(), "Pronóstico", mesFGEM);
            datosFGEM.addValue(puntoFGEM.inferiorFGEM(), "Rango inferior", mesFGEM);
            datosFGEM.addValue(puntoFGEM.superiorFGEM(), "Rango superior", mesFGEM);
        }

        LineAndShapeRenderer lineasFGEM = new LineAndShapeRenderer(true, true);
        lineasFGEM.setSeriesPaint(0, AZUL_FGEM);
        lineasFGEM.setSeriesStroke(0, LINEA_GRUESA_FGEM);
        lineasFGEM.setSeriesPaint(1, AMBAR_FGEM);
        lineasFGEM.setSeriesStroke(1, new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[]{8f, 6f}, 0f));
        for (int serieFGEM = 2; serieFGEM <= 3; serieFGEM++) {
            lineasFGEM.setSeriesPaint(serieFGEM, TEXTO_FGEM);
            lineasFGEM.setSeriesStroke(serieFGEM, LINEA_PUNTEADA_FGEM);
            lineasFGEM.setSeriesShapesVisible(serieFGEM, false);
        }
        CategoryAxis ejeMesesFGEM = nuevoEjeMesesFGEM();
        CategoryPlot plotFGEM = new CategoryPlot(datosFGEM, ejeMesesFGEM, ejeMontoFGEM(monedaFGEM), lineasFGEM);
        plotFGEM.addRangeMarker(new ValueMarker(0, GRILLA_FGEM, new BasicStroke(1f)));
        return aplicarTemaFGEM(new JFreeChart(null, FUENTE_FGEM, plotFGEM, true));
    }

    private static Color[] paletaFGEM() {
        return new Color[]{AZUL_FGEM, VERDE_FGEM, AMBAR_FGEM, new Color(0xff, 0xc0, 0x00), ROJO_FGEM, new Color(0x5b, 0x9b, 0xd5)};
    }

    public static JFreeChart lineasMensualesFGEM(List<YearMonth> mesesFGEM, java.util.Map<String, double[]> seriesFGEM, String formatoFGEM) {
        DefaultCategoryDataset datosFGEM = new DefaultCategoryDataset();
        seriesFGEM.forEach((nombreFGEM, valoresFGEM) -> {
            for (int iFGEM = 0; iFGEM < mesesFGEM.size(); iFGEM++) {
                datosFGEM.addValue(valoresFGEM[iFGEM], nombreFGEM, etiquetaFGEM(mesesFGEM, iFGEM));
            }
        });
        LineAndShapeRenderer lineasFGEM = new LineAndShapeRenderer(true, false);
        for (int iFGEM = 0; iFGEM < seriesFGEM.size(); iFGEM++) {
            lineasFGEM.setSeriesPaint(iFGEM, paletaFGEM()[iFGEM % paletaFGEM().length]);
            lineasFGEM.setSeriesStroke(iFGEM, LINEA_GRUESA_FGEM);
        }
        CategoryAxis ejeMesesFGEM = nuevoEjeMesesFGEM();
        CategoryPlot plotFGEM = new CategoryPlot(datosFGEM, ejeMesesFGEM, ejeNumeroFGEM(formatoFGEM), lineasFGEM);
        return aplicarTemaFGEM(new JFreeChart(null, FUENTE_FGEM, plotFGEM, seriesFGEM.size() > 1));
    }

    public static JFreeChart barrasMensualesFGEM(List<YearMonth> mesesFGEM, java.util.Map<String, double[]> seriesFGEM, Color[] coloresFGEM,
                                             TendenciaFGEM tendenciaFGEM, int inicioTendenciaFGEM, String formatoFGEM) {
        DefaultCategoryDataset datosFGEM = new DefaultCategoryDataset();
        seriesFGEM.forEach((nombreFGEM, valoresFGEM) -> {
            for (int iFGEM = 0; iFGEM < mesesFGEM.size(); iFGEM++) {
                datosFGEM.addValue(valoresFGEM[iFGEM], nombreFGEM, etiquetaFGEM(mesesFGEM, iFGEM));
            }
        });
        BarRenderer barrasFGEM = new BarRenderer();
        barrasFGEM.setBarPainter(new StandardBarPainter());
        barrasFGEM.setShadowVisible(false);
        barrasFGEM.setItemMargin(0.02);
        barrasFGEM.setMaximumBarWidth(0.06);
        for (int iFGEM = 0; iFGEM < seriesFGEM.size(); iFGEM++) {
            barrasFGEM.setSeriesPaint(iFGEM, coloresFGEM[iFGEM % coloresFGEM.length]);
        }
        CategoryAxis ejeMesesFGEM = nuevoEjeMesesFGEM();
        CategoryPlot plotFGEM = new CategoryPlot(datosFGEM, ejeMesesFGEM, ejeNumeroFGEM(formatoFGEM), barrasFGEM);
        if (tendenciaFGEM != null && !Double.isNaN(tendenciaFGEM.pendienteFGEM())) {
            DefaultCategoryDataset rectaFGEM = new DefaultCategoryDataset();
            for (int iFGEM = inicioTendenciaFGEM; iFGEM < mesesFGEM.size(); iFGEM++) {
                rectaFGEM.addValue(tendenciaFGEM.valorAjustadoFGEM(iFGEM - inicioTendenciaFGEM), "Tendencia", etiquetaFGEM(mesesFGEM, iFGEM));
            }
            LineAndShapeRenderer lineaFGEM = new LineAndShapeRenderer(true, false);
            lineaFGEM.setSeriesPaint(0, AMBAR_FGEM);
            lineaFGEM.setSeriesStroke(0, LINEA_GRUESA_FGEM);
            plotFGEM.setDataset(1, rectaFGEM);
            plotFGEM.setRenderer(1, lineaFGEM);
            plotFGEM.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        }
        return aplicarTemaFGEM(new JFreeChart(null, FUENTE_FGEM, plotFGEM, seriesFGEM.size() > 1 || tendenciaFGEM != null));
    }

    public static JFreeChart barrasCategoriasFGEM(List<String> categoriasFGEM, java.util.Map<String, double[]> seriesFGEM, String formatoFGEM) {
        DefaultCategoryDataset datosFGEM = new DefaultCategoryDataset();
        seriesFGEM.forEach((nombreFGEM, valoresFGEM) -> {
            for (int iFGEM = 0; iFGEM < categoriasFGEM.size(); iFGEM++) {
                datosFGEM.addValue(valoresFGEM[iFGEM], nombreFGEM, abreviarFGEM(categoriasFGEM.get(iFGEM), 18));
            }
        });
        BarRenderer barrasFGEM = new BarRenderer();
        barrasFGEM.setBarPainter(new StandardBarPainter());
        barrasFGEM.setShadowVisible(false);
        barrasFGEM.setItemMargin(0.05);
        barrasFGEM.setMaximumBarWidth(0.12);
        Color[] coloresFGEM = {AZUL_FGEM, TEXTO_FGEM, VERDE_FGEM, AMBAR_FGEM};
        for (int iFGEM = 0; iFGEM < seriesFGEM.size(); iFGEM++) {
            barrasFGEM.setSeriesPaint(iFGEM, coloresFGEM[iFGEM % coloresFGEM.length]);
        }
        CategoryAxis ejeFGEM = new CategoryAxis();
        ejeFGEM.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        CategoryPlot plotFGEM = new CategoryPlot(datosFGEM, ejeFGEM, ejeNumeroFGEM(formatoFGEM), barrasFGEM);
        return aplicarTemaFGEM(new JFreeChart(null, FUENTE_FGEM, plotFGEM, seriesFGEM.size() > 1));
    }

    private static String etiquetaFGEM(List<YearMonth> mesesFGEM, int iFGEM) {
        YearMonth mesFGEM = mesesFGEM.get(iFGEM);
        if (mesesFGEM.size() <= 12) {
            return etiquetaMesFGEM(mesFGEM);
        }
        return mesFGEM.getMonth().getDisplayName(TextStyle.SHORT, ESPANOL_FGEM).replace(".", "") + " "
                + String.valueOf(mesFGEM.getYear()).substring(2);
    }

    private static NumberAxis ejeNumeroFGEM(String formatoFGEM) {
        NumberAxis ejeFGEM = new NumberAxis();
        ejeFGEM.setNumberFormatOverride(new DecimalFormat(formatoFGEM, DecimalFormatSymbols.getInstance(Locale.US)));
        ejeFGEM.setAutoRangeIncludesZero(true);
        return ejeFGEM;
    }

    public static java.awt.image.BufferedImage imagenParaImpresionFGEM(JFreeChart graficoFGEM, int anchoFGEM, int altoFGEM) {
        Color fondoFGEM = Color.WHITE;
        Color textoFGEM = new Color(0x47, 0x55, 0x69);
        Color grillaFGEM = new Color(0xe2, 0xe8, 0xf0);
        Color azulImpresionFGEM = new Color(0x02, 0x84, 0xc7);
        graficoFGEM.setBackgroundPaint(fondoFGEM);
        if (graficoFGEM.getLegend() != null) {
            graficoFGEM.getLegend().setBackgroundPaint(fondoFGEM);
            graficoFGEM.getLegend().setItemPaint(textoFGEM);
        }
        if (graficoFGEM.getPlot() instanceof CategoryPlot plotFGEM) {
            plotFGEM.setBackgroundPaint(fondoFGEM);
            plotFGEM.setRangeGridlinePaint(grillaFGEM);
            colorEjesFGEM(textoFGEM, grillaFGEM, plotFGEM.getDomainAxis(), plotFGEM.getRangeAxis());
            for (int rFGEM = 0; rFGEM < plotFGEM.getRendererCount(); rFGEM++) {
                if (plotFGEM.getRenderer(rFGEM) != null && plotFGEM.getDataset(rFGEM) != null) {
                    for (int sFGEM = 0; sFGEM < plotFGEM.getDataset(rFGEM).getRowCount(); sFGEM++) {
                        if (AZUL_FGEM.equals(plotFGEM.getRenderer(rFGEM).getSeriesPaint(sFGEM))) {
                            plotFGEM.getRenderer(rFGEM).setSeriesPaint(sFGEM, azulImpresionFGEM);
                        }
                    }
                }
            }
        } else if (graficoFGEM.getPlot() instanceof XYPlot plotFGEM) {
            plotFGEM.setBackgroundPaint(fondoFGEM);
            plotFGEM.setRangeGridlinePaint(grillaFGEM);
            plotFGEM.setDomainGridlinePaint(grillaFGEM);
            colorEjesFGEM(textoFGEM, grillaFGEM, plotFGEM.getDomainAxis(), plotFGEM.getRangeAxis());
        }
        return graficoFGEM.createBufferedImage(anchoFGEM * 2, altoFGEM * 2, anchoFGEM, altoFGEM, null);
    }

    private static void colorEjesFGEM(Color textoFGEM, Color lineaFGEM, Axis... ejesFGEM) {
        for (Axis ejeFGEM : ejesFGEM) {
            ejeFGEM.setLabelPaint(textoFGEM);
            ejeFGEM.setTickLabelPaint(textoFGEM);
            ejeFGEM.setAxisLinePaint(lineaFGEM);
            ejeFGEM.setTickMarkPaint(lineaFGEM);
        }
    }

    public static JFreeChart comparacionEscenariosFGEM(List<String> nombresFGEM, List<ResultadoEscenarioFGEM> resultadosFGEM,
                                                   MonedaFGEM monedaFGEM) {
        DefaultCategoryDataset datosFGEM = new DefaultCategoryDataset();
        for (int iFGEM = 0; iFGEM < nombresFGEM.size(); iFGEM++) {
            ResultadoEscenarioFGEM rFGEM = resultadosFGEM.get(iFGEM);
            String serieFGEM = abreviarFGEM(nombresFGEM.get(iFGEM), 22);
            datosFGEM.addValue(rFGEM.ventasFGEM(), serieFGEM, "Ventas");
            datosFGEM.addValue(rFGEM.costosTotalesFGEM(), serieFGEM, "Costos totales");
            datosFGEM.addValue(rFGEM.utilidadNetaFGEM(), serieFGEM, "Utilidad neta");
        }
        BarRenderer barrasFGEM = new BarRenderer();
        barrasFGEM.setBarPainter(new StandardBarPainter());
        barrasFGEM.setShadowVisible(false);
        barrasFGEM.setItemMargin(0.05);
        Color[] coloresFGEM = {TEXTO_FGEM, AZUL_FGEM, AMBAR_FGEM, VERDE_FGEM, new Color(0xff, 0xc0, 0x00), ROJO_FGEM};
        for (int iFGEM = 0; iFGEM < nombresFGEM.size(); iFGEM++) {
            barrasFGEM.setSeriesPaint(iFGEM, coloresFGEM[iFGEM % coloresFGEM.length]);
        }
        CategoryPlot plotFGEM = new CategoryPlot(datosFGEM, new CategoryAxis(), ejeMontoFGEM(monedaFGEM), barrasFGEM);
        plotFGEM.addRangeMarker(new ValueMarker(0, GRILLA_FGEM, new BasicStroke(1f)));
        return aplicarTemaFGEM(new JFreeChart(null, FUENTE_FGEM, plotFGEM, true));
    }

    public static JFreeChart tornadoFGEM(List<SimuladorFGEM.SensibilidadFGEM> sensibilidadesFGEM, String variacionFGEM, MonedaFGEM monedaFGEM) {
        DefaultCategoryDataset datosFGEM = new DefaultCategoryDataset();
        for (SimuladorFGEM.SensibilidadFGEM sFGEM : sensibilidadesFGEM) {
            String variableFGEM = sFGEM.variableFGEM().descripcionFGEM();
            datosFGEM.addValue(sFGEM.utilidadSiBajaFGEM().subtract(sFGEM.utilidadEscenarioFGEM()), "Si baja " + variacionFGEM, variableFGEM);
            datosFGEM.addValue(sFGEM.utilidadSiSubeFGEM().subtract(sFGEM.utilidadEscenarioFGEM()), "Si sube " + variacionFGEM, variableFGEM);
        }
        org.jfree.chart.renderer.category.StackedBarRenderer barrasFGEM = new org.jfree.chart.renderer.category.StackedBarRenderer();
        barrasFGEM.setBarPainter(new StandardBarPainter());
        barrasFGEM.setShadowVisible(false);
        barrasFGEM.setSeriesPaint(0, AMBAR_FGEM);
        barrasFGEM.setSeriesPaint(1, AZUL_FGEM);
        barrasFGEM.setMaximumBarWidth(0.12);
        NumberAxis ejeFGEM = ejeMontoFGEM(monedaFGEM);
        ejeFGEM.setLabel("Cambio en la utilidad neta mensual");
        CategoryPlot plotFGEM = new CategoryPlot(datosFGEM, new CategoryAxis(), ejeFGEM, barrasFGEM);
        plotFGEM.setOrientation(PlotOrientation.HORIZONTAL);
        plotFGEM.addRangeMarker(new ValueMarker(0, TEXTO_FGEM, new BasicStroke(1.5f)));
        ((CategoryAxis) plotFGEM.getDomainAxis()).setMaximumCategoryLabelWidthRatio(0.35f);
        return aplicarTemaFGEM(new JFreeChart(null, FUENTE_FGEM, plotFGEM, true));
    }

    private static String etiquetaMesConAnioFGEM(YearMonth mesFGEM) {
        return mesFGEM.getMonth().getDisplayName(TextStyle.SHORT, ESPANOL_FGEM).replace(".", "") + " "
                + String.valueOf(mesFGEM.getYear()).substring(2);
    }

    private static BarRenderer barrasSimplesFGEM(Color colorFGEM) {
        BarRenderer barrasFGEM = new BarRenderer();
        barrasFGEM.setBarPainter(new StandardBarPainter());
        barrasFGEM.setShadowVisible(false);
        barrasFGEM.setSeriesPaint(0, colorFGEM);
        barrasFGEM.setMaximumBarWidth(0.08);
        return barrasFGEM;
    }

    private static JFreeChart aplicarTemaFGEM(JFreeChart graficoFGEM) {
        graficoFGEM.setBackgroundPaint(FONDO_FGEM);
        graficoFGEM.setBorderVisible(false);
        graficoFGEM.setPadding(new RectangleInsets(6, 4, 4, 8));
        graficoFGEM.setAntiAlias(true);
        graficoFGEM.setTextAntiAlias(true);
        if (graficoFGEM.getLegend() != null) {
            graficoFGEM.getLegend().setBackgroundPaint(FONDO_FGEM);
            graficoFGEM.getLegend().setItemPaint(TEXTO_FGEM);
            graficoFGEM.getLegend().setItemFont(FUENTE_FGEM);
            graficoFGEM.getLegend().setFrame(BlockBorder.NONE);
        }
        if (graficoFGEM.getPlot() instanceof CategoryPlot plotFGEM) {
            estiloPlotFGEM(plotFGEM.getDomainAxis(), plotFGEM.getRangeAxis());
            plotFGEM.setBackgroundPaint(FONDO_FGEM);
            plotFGEM.setOutlineVisible(false);
            plotFGEM.setRangeGridlinePaint(GRILLA_FGEM);
            plotFGEM.setDomainGridlinesVisible(false);
        } else if (graficoFGEM.getPlot() instanceof XYPlot plotFGEM) {
            estiloPlotFGEM(plotFGEM.getDomainAxis(), plotFGEM.getRangeAxis());
            plotFGEM.setBackgroundPaint(FONDO_FGEM);
            plotFGEM.setOutlineVisible(false);
            plotFGEM.setRangeGridlinePaint(GRILLA_FGEM);
            plotFGEM.setDomainGridlinePaint(GRILLA_FGEM);
        }
        return graficoFGEM;
    }

    private static void estiloPlotFGEM(Axis... ejesFGEM) {
        for (Axis ejeFGEM : ejesFGEM) {
            ejeFGEM.setLabelPaint(TEXTO_FGEM);
            ejeFGEM.setLabelFont(FUENTE_FGEM);
            ejeFGEM.setTickLabelPaint(TEXTO_FGEM);
            ejeFGEM.setTickLabelFont(FUENTE_FGEM);
            ejeFGEM.setAxisLinePaint(GRILLA_FGEM);
            ejeFGEM.setTickMarkPaint(GRILLA_FGEM);
        }
    }

    private static NumberAxis ejeMontoFGEM(MonedaFGEM monedaFGEM) {
        NumberAxis ejeFGEM = new NumberAxis();
        ejeFGEM.setNumberFormatOverride(new DecimalFormat("'" + monedaFGEM.simboloFGEM() + "' #,##0",
                DecimalFormatSymbols.getInstance(Locale.US)));
        ejeFGEM.setAutoRangeIncludesZero(true);
        return ejeFGEM;
    }

    private static ValueMarker marcadorFGEM(double valorFGEM, String etiquetaFGEM, Color colorFGEM) {
        ValueMarker marcadorFGEM = new ValueMarker(valorFGEM, colorFGEM, LINEA_PUNTEADA_FGEM);
        marcadorFGEM.setLabel(etiquetaFGEM);
        marcadorFGEM.setLabelPaint(colorFGEM);
        marcadorFGEM.setLabelFont(FUENTE_FGEM);
        marcadorFGEM.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        marcadorFGEM.setLabelTextAnchor(TextAnchor.TOP_LEFT);
        return marcadorFGEM;
    }

    private static CategoryAxis nuevoEjeMesesFGEM() {
        CategoryAxis ejeFGEM = new CategoryAxis();
        ejeFGEM.setCategoryLabelPositions(CategoryLabelPositions.STANDARD);
        ejeFGEM.setMaximumCategoryLabelLines(2);
        ejeFGEM.setMaximumCategoryLabelWidthRatio(1.0f);
        return ejeFGEM;
    }

    private static String etiquetaMesFGEM(YearMonth mesFGEM) {
        String nombreFGEM = mesFGEM.getMonth().getDisplayName(TextStyle.SHORT, ESPANOL_FGEM).replace(".", "");
        return mesFGEM.getMonthValue() == 1 ? nombreFGEM + " " + String.valueOf(mesFGEM.getYear()).substring(2) : nombreFGEM;
    }

    private static String abreviarFGEM(String textoFGEM, int maximoFGEM) {
        return textoFGEM.length() <= maximoFGEM ? textoFGEM : textoFGEM.substring(0, maximoFGEM - 1) + "…";
    }
}
