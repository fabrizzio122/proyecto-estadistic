package com.businessanalyzer.ui;

import com.businessanalyzer.analytics.AnalisisFinanciero.ResultadoMensual;
import com.businessanalyzer.analytics.PuntoEquilibrio;
import com.businessanalyzer.analytics.RentabilidadProducto;
import com.businessanalyzer.analytics.estadistica.AnalisisAbc;
import com.businessanalyzer.analytics.estadistica.AnalisisEstadistico;
import com.businessanalyzer.analytics.estadistica.EstacionalidadSemanal;
import com.businessanalyzer.analytics.estadistica.Histograma;
import com.businessanalyzer.analytics.estadistica.Tendencia;
import com.businessanalyzer.analytics.prediccion.Pronostico;
import com.businessanalyzer.analytics.simulacion.ResultadoEscenario;
import com.businessanalyzer.analytics.simulacion.Simulador;
import com.businessanalyzer.model.Moneda;
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

/**
 * Gráficos de JFreeChart con el tema oscuro de la aplicación. Cada método devuelve un
 * {@link JFreeChart} listo para mostrarse con {@link #visor(JFreeChart)}; el visor incluye
 * un menú contextual (clic derecho) para exportar la imagen.
 */
public final class Graficos {

    public static final Color AZUL = new Color(0x38, 0xbd, 0xf8);
    public static final Color VERDE = new Color(0x22, 0xc5, 0x5e);
    public static final Color AMBAR = new Color(0xf5, 0x9e, 0x0b);
    public static final Color ROJO = new Color(0xef, 0x44, 0x44);

    private static final Color FONDO = new Color(0x14, 0x1e, 0x34);
    private static final Color GRILLA = new Color(0x22, 0x30, 0x4d);
    private static final Color TEXTO = new Color(0x8d, 0x9b, 0xb5);
    private static final Color TEXTO_FUERTE = new Color(0xe6, 0xeb, 0xf5);
    private static final Font FUENTE = new Font("Segoe UI", Font.PLAIN, 11);
    private static final BasicStroke LINEA_GRUESA = new BasicStroke(2.5f);
    private static final BasicStroke LINEA_PUNTEADA = new BasicStroke(1.5f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 5f}, 0f);
    private static final Locale ESPANOL = Locale.forLanguageTag("es");

    private Graficos() {
    }

    public static ChartViewer visor(JFreeChart grafico) {
        ChartViewer visor = new ChartViewer(grafico);
        visor.setMinSize(200, 160);
        visor.setPrefSize(400, 240);
        return visor;
    }

    /** Barras de ventas netas y línea de utilidad neta de cada mes. */
    public static JFreeChart evolucionMensual(List<ResultadoMensual> meses, Moneda moneda) {
        DefaultCategoryDataset ventas = new DefaultCategoryDataset();
        DefaultCategoryDataset utilidad = new DefaultCategoryDataset();
        for (ResultadoMensual m : meses) {
            String etiqueta = etiquetaMes(m.mes());
            ventas.addValue(m.estado().ventasNetas(), "Ventas netas", etiqueta);
            utilidad.addValue(m.estado().utilidadNeta(), "Utilidad neta", etiqueta);
        }

        BarRenderer barras = new BarRenderer();
        barras.setBarPainter(new StandardBarPainter());
        barras.setShadowVisible(false);
        barras.setSeriesPaint(0, AZUL);
        barras.setItemMargin(0);
        barras.setMaximumBarWidth(0.05);

        CategoryAxis ejeMeses = new CategoryAxis();
        ejeMeses.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        ejeMeses.setCategoryMargin(0.1);
        CategoryPlot plot = new CategoryPlot(ventas, ejeMeses, ejeMonto(moneda), barras);

        LineAndShapeRenderer linea = new LineAndShapeRenderer(true, true);
        linea.setSeriesPaint(0, VERDE);
        linea.setSeriesStroke(0, LINEA_GRUESA);
        plot.setDataset(1, utilidad);
        plot.setRenderer(1, linea);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        plot.addRangeMarker(new ValueMarker(0, TEXTO, new BasicStroke(1f)));

        return aplicarTema(new JFreeChart(null, FUENTE, plot, true));
    }

    /**
     * Gráfico clásico de equilibrio: la recta de ingresos y la de costos totales
     * (fijos + variables) se cruzan en el punto de equilibrio.
     */
    public static JFreeChart puntoEquilibrio(PuntoEquilibrio pe, Moneda moneda) {
        double fijos = pe.costosFijos().doubleValue();
        double razonVariable = pe.razonCostoVariable() == null ? 1 : pe.razonCostoVariable().doubleValue();
        double referencia = Math.max(pe.ventas().doubleValue(),
                pe.ventasEquilibrio() == null ? 0 : pe.ventasEquilibrio().doubleValue());
        double maximo = referencia > 0 ? referencia * 1.4 : Math.max(fijos * 2, 100);

        XYSeries ingresos = new XYSeries("Ingresos por ventas");
        XYSeries costos = new XYSeries("Costos totales");
        XYSeries costosFijos = new XYSeries("Costos fijos");
        for (double x : new double[]{0, maximo}) {
            ingresos.add(x, x);
            costos.add(x, fijos + razonVariable * x);
            costosFijos.add(x, fijos);
        }
        XYSeriesCollection datos = new XYSeriesCollection();
        datos.addSeries(ingresos);
        datos.addSeries(costos);
        datos.addSeries(costosFijos);

        XYLineAndShapeRenderer lineas = new XYLineAndShapeRenderer(true, false);
        lineas.setSeriesPaint(0, AZUL);
        lineas.setSeriesStroke(0, LINEA_GRUESA);
        lineas.setSeriesPaint(1, AMBAR);
        lineas.setSeriesStroke(1, LINEA_GRUESA);
        lineas.setSeriesPaint(2, TEXTO);
        lineas.setSeriesStroke(2, LINEA_PUNTEADA);

        NumberAxis ejeVentas = ejeMonto(moneda);
        ejeVentas.setLabel("Nivel de ventas");
        XYPlot plot = new XYPlot(datos, ejeVentas, ejeMonto(moneda), lineas);
        if (pe.ventasEquilibrio() != null) {
            plot.addDomainMarker(marcador(pe.ventasEquilibrio().doubleValue(), "Equilibrio", VERDE));
        }
        if (pe.ventas().signum() > 0) {
            plot.addDomainMarker(marcador(pe.ventas().doubleValue(), "Ventas actuales", TEXTO_FUERTE));
        }
        return aplicarTema(new JFreeChart(null, FUENTE, plot, true));
    }

    /** Barras horizontales con el margen bruto de los productos que más aportan (en rojo si es negativo). */
    public static JFreeChart rentabilidadProductos(List<RentabilidadProducto> productos, int maximo, Moneda moneda) {
        DefaultCategoryDataset datos = new DefaultCategoryDataset();
        productos.stream().limit(maximo).forEach(p -> datos.addValue(p.margen(), "Margen bruto", abreviar(p.nombre(), 26)));

        BarRenderer barras = new BarRenderer() {
            @Override
            public Paint getItemPaint(int fila, int columna) {
                Number valor = datos.getValue(fila, columna);
                return valor != null && valor.doubleValue() < 0 ? ROJO : AZUL;
            }
        };
        barras.setBarPainter(new StandardBarPainter());
        barras.setShadowVisible(false);
        barras.setMaximumBarWidth(0.08);

        CategoryAxis ejeProductos = new CategoryAxis();
        ejeProductos.setMaximumCategoryLabelWidthRatio(0.45f);
        CategoryPlot plot = new CategoryPlot(datos, ejeProductos, ejeMonto(moneda), barras);
        plot.setOrientation(PlotOrientation.HORIZONTAL);
        return aplicarTema(new JFreeChart(null, FUENTE, plot, false));
    }

    // ------------------------------------------------------------------ estadística (Fase 8)

    /** Frecuencia de ventas por rango de monto. */
    public static JFreeChart histograma(Histograma histograma, Moneda moneda) {
        DefaultCategoryDataset datos = new DefaultCategoryDataset();
        DecimalFormat entero = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
        for (Histograma.Clase clase : histograma.clases()) {
            datos.addValue(clase.frecuencia(), "Ventas",
                    entero.format(clase.desde()) + "–" + entero.format(clase.hasta()));
        }
        BarRenderer barras = barrasSimples(AZUL);
        barras.setItemMargin(0);
        CategoryAxis ejeRangos = new CategoryAxis("Monto de la venta (" + moneda.simbolo() + ")");
        ejeRangos.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        ejeRangos.setCategoryMargin(0.05);
        NumberAxis ejeFrecuencia = new NumberAxis("Cantidad de ventas");
        ejeFrecuencia.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        return aplicarTema(new JFreeChart(null, FUENTE, new CategoryPlot(datos, ejeRangos, ejeFrecuencia, barras), false));
    }

    /** Ventas mensuales en barras y la recta de tendencia (regresión lineal). */
    public static JFreeChart tendencia(List<AnalisisEstadistico.PuntoMensual> serie, Tendencia tendencia, Moneda moneda) {
        DefaultCategoryDataset ventas = new DefaultCategoryDataset();
        DefaultCategoryDataset recta = new DefaultCategoryDataset();
        for (int t = 0; t < serie.size(); t++) {
            String mes = etiquetaMes(serie.get(t).mes());
            ventas.addValue(serie.get(t).ventas(), "Ventas netas", mes);
            if (!Double.isNaN(tendencia.pendiente())) {
                recta.addValue(tendencia.valorAjustado(t), "Tendencia", mes);
            }
        }
        CategoryAxis ejeMeses = new CategoryAxis();
        ejeMeses.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        CategoryPlot plot = new CategoryPlot(ventas, ejeMeses, ejeMonto(moneda), barrasSimples(AZUL));
        LineAndShapeRenderer linea = new LineAndShapeRenderer(true, false);
        linea.setSeriesPaint(0, AMBAR);
        linea.setSeriesStroke(0, LINEA_GRUESA);
        plot.setDataset(1, recta);
        plot.setRenderer(1, linea);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        return aplicarTema(new JFreeChart(null, FUENTE, plot, true));
    }

    /** Venta promedio por día de la semana; el mejor día en verde. */
    public static JFreeChart diasDeLaSemana(EstacionalidadSemanal estacionalidad, Moneda moneda) {
        DefaultCategoryDataset datos = new DefaultCategoryDataset();
        java.time.DayOfWeek mejor = estacionalidad.mejorDia();
        for (java.time.DayOfWeek dia : java.time.DayOfWeek.values()) {
            Double promedio = estacionalidad.promedioPorDia().get(dia);
            datos.addValue(promedio == null ? 0 : promedio, "Venta promedio",
                    dia.getDisplayName(TextStyle.SHORT, ESPANOL).replace(".", ""));
        }
        int indiceMejor = mejor == null ? -1 : mejor.getValue() - 1;
        BarRenderer barras = new BarRenderer() {
            @Override
            public Paint getItemPaint(int fila, int columna) {
                return columna == indiceMejor ? VERDE : AZUL;
            }
        };
        barras.setBarPainter(new StandardBarPainter());
        barras.setShadowVisible(false);
        barras.setMaximumBarWidth(0.1);
        CategoryPlot plot = new CategoryPlot(datos, new CategoryAxis(), ejeMonto(moneda), barras);
        if (!Double.isNaN(estacionalidad.promedioGeneral())) {
            ValueMarker promedio = new ValueMarker(estacionalidad.promedioGeneral(), AMBAR, LINEA_PUNTEADA);
            promedio.setLabel("Promedio diario");
            promedio.setLabelPaint(AMBAR);
            promedio.setLabelFont(FUENTE);
            promedio.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
            promedio.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);
            plot.addRangeMarker(promedio);
        }
        return aplicarTema(new JFreeChart(null, FUENTE, plot, false));
    }

    /** Diagrama de Pareto: ventas por producto (color según clase ABC) y porcentaje acumulado. */
    public static JFreeChart pareto(AnalisisAbc abc, int maximo, Moneda moneda) {
        DefaultCategoryDataset ventas = new DefaultCategoryDataset();
        DefaultCategoryDataset acumulado = new DefaultCategoryDataset();
        List<AnalisisAbc.Item> items = abc.items().stream().limit(maximo).toList();
        for (AnalisisAbc.Item item : items) {
            String nombre = abreviar(item.producto().nombre(), 14);
            ventas.addValue(item.producto().ventas(), "Ventas", nombre);
            acumulado.addValue(item.acumulado().movePointRight(2), "% acumulado", nombre);
        }
        BarRenderer barras = new BarRenderer() {
            @Override
            public Paint getItemPaint(int fila, int columna) {
                return switch (items.get(columna).clase()) {
                    case A -> AZUL;
                    case B -> AMBAR;
                    case C -> TEXTO;
                };
            }
        };
        barras.setBarPainter(new StandardBarPainter());
        barras.setShadowVisible(false);
        barras.setItemMargin(0);

        CategoryAxis ejeProductos = new CategoryAxis();
        ejeProductos.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        CategoryPlot plot = new CategoryPlot(ventas, ejeProductos, ejeMonto(moneda), barras);

        NumberAxis ejePorcentaje = new NumberAxis("% acumulado");
        ejePorcentaje.setRange(0, 105);
        ejePorcentaje.setNumberFormatOverride(new DecimalFormat("0'%'"));
        plot.setRangeAxis(1, ejePorcentaje);
        plot.setDataset(1, acumulado);
        plot.mapDatasetToRangeAxis(1, 1);
        LineAndShapeRenderer linea = new LineAndShapeRenderer(true, true);
        linea.setSeriesPaint(0, VERDE);
        linea.setSeriesStroke(0, LINEA_GRUESA);
        plot.setRenderer(1, linea);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        ValueMarker ochenta = new ValueMarker(80, VERDE, LINEA_PUNTEADA);
        plot.addRangeMarker(1, ochenta, org.jfree.chart.ui.Layer.FOREGROUND);
        JFreeChart grafico = aplicarTema(new JFreeChart(null, FUENTE, plot, false));
        estiloPlot(ejePorcentaje);
        return grafico;
    }

    // ------------------------------------------------------------------ predicciones (Fase 9)

    /**
     * Historia (línea continua), pronóstico (discontinua) y su rango al 95 % (punteado).
     * Se muestran como máximo los últimos {@code mesesHistoria} meses de historia.
     */
    public static JFreeChart pronostico(List<YearMonth> meses, double[] historia, Pronostico pronostico,
                                        int mesesHistoria, Moneda moneda) {
        DefaultCategoryDataset datos = new DefaultCategoryDataset();
        int desde = Math.max(0, meses.size() - mesesHistoria);
        for (int i = desde; i < meses.size(); i++) {
            String mes = etiquetaMesConAnio(meses.get(i));
            datos.addValue(historia[i], "Historia", mes);
            boolean ultimo = i == meses.size() - 1;
            datos.addValue(ultimo ? historia[i] : null, "Pronóstico", mes);
            datos.addValue(ultimo ? historia[i] : null, "Rango inferior", mes);
            datos.addValue(ultimo ? historia[i] : null, "Rango superior", mes);
        }
        YearMonth siguiente = meses.isEmpty() ? YearMonth.now() : meses.get(meses.size() - 1).plusMonths(1);
        for (Pronostico.Punto punto : pronostico.puntos()) {
            String mes = etiquetaMesConAnio(siguiente.plusMonths(punto.h() - 1L));
            datos.addValue(null, "Historia", mes);
            datos.addValue(punto.valor(), "Pronóstico", mes);
            datos.addValue(punto.inferior(), "Rango inferior", mes);
            datos.addValue(punto.superior(), "Rango superior", mes);
        }

        LineAndShapeRenderer lineas = new LineAndShapeRenderer(true, true);
        lineas.setSeriesPaint(0, AZUL);
        lineas.setSeriesStroke(0, LINEA_GRUESA);
        lineas.setSeriesPaint(1, AMBAR);
        lineas.setSeriesStroke(1, new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[]{8f, 6f}, 0f));
        for (int serie = 2; serie <= 3; serie++) {
            lineas.setSeriesPaint(serie, TEXTO);
            lineas.setSeriesStroke(serie, LINEA_PUNTEADA);
            lineas.setSeriesShapesVisible(serie, false);
        }
        CategoryAxis ejeMeses = new CategoryAxis();
        ejeMeses.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        CategoryPlot plot = new CategoryPlot(datos, ejeMeses, ejeMonto(moneda), lineas);
        plot.addRangeMarker(new ValueMarker(0, GRILLA, new BasicStroke(1f)));
        return aplicarTema(new JFreeChart(null, FUENTE, plot, true));
    }

    // ------------------------------------------------------------------ simulación (Fase 10)

    /** Ventas, costos totales y utilidad neta de varios escenarios, agrupados. */
    public static JFreeChart comparacionEscenarios(List<String> nombres, List<ResultadoEscenario> resultados,
                                                   Moneda moneda) {
        DefaultCategoryDataset datos = new DefaultCategoryDataset();
        for (int i = 0; i < nombres.size(); i++) {
            ResultadoEscenario r = resultados.get(i);
            String serie = abreviar(nombres.get(i), 22);
            datos.addValue(r.ventas(), serie, "Ventas");
            datos.addValue(r.costosTotales(), serie, "Costos totales");
            datos.addValue(r.utilidadNeta(), serie, "Utilidad neta");
        }
        BarRenderer barras = new BarRenderer();
        barras.setBarPainter(new StandardBarPainter());
        barras.setShadowVisible(false);
        barras.setItemMargin(0.05);
        Color[] colores = {TEXTO, AZUL, AMBAR, VERDE, new Color(0xa7, 0x8b, 0xfa), ROJO};
        for (int i = 0; i < nombres.size(); i++) {
            barras.setSeriesPaint(i, colores[i % colores.length]);
        }
        CategoryPlot plot = new CategoryPlot(datos, new CategoryAxis(), ejeMonto(moneda), barras);
        plot.addRangeMarker(new ValueMarker(0, GRILLA, new BasicStroke(1f)));
        return aplicarTema(new JFreeChart(null, FUENTE, plot, true));
    }

    /**
     * Gráfico de tornado: cuánto cambia la utilidad neta si cada variable baja o sube.
     * Las variables con barras más largas son las que más pesan.
     */
    public static JFreeChart tornado(List<Simulador.Sensibilidad> sensibilidades, String variacion, Moneda moneda) {
        DefaultCategoryDataset datos = new DefaultCategoryDataset();
        for (Simulador.Sensibilidad s : sensibilidades) {
            String variable = s.variable().descripcion();
            datos.addValue(s.utilidadSiBaja().subtract(s.utilidadEscenario()), "Si baja " + variacion, variable);
            datos.addValue(s.utilidadSiSube().subtract(s.utilidadEscenario()), "Si sube " + variacion, variable);
        }
        org.jfree.chart.renderer.category.StackedBarRenderer barras = new org.jfree.chart.renderer.category.StackedBarRenderer();
        barras.setBarPainter(new StandardBarPainter());
        barras.setShadowVisible(false);
        barras.setSeriesPaint(0, AMBAR);
        barras.setSeriesPaint(1, AZUL);
        barras.setMaximumBarWidth(0.12);
        NumberAxis eje = ejeMonto(moneda);
        eje.setLabel("Cambio en la utilidad neta mensual");
        CategoryPlot plot = new CategoryPlot(datos, new CategoryAxis(), eje, barras);
        plot.setOrientation(PlotOrientation.HORIZONTAL);
        plot.addRangeMarker(new ValueMarker(0, TEXTO, new BasicStroke(1.5f)));
        ((CategoryAxis) plot.getDomainAxis()).setMaximumCategoryLabelWidthRatio(0.35f);
        return aplicarTema(new JFreeChart(null, FUENTE, plot, true));
    }

    private static String etiquetaMesConAnio(YearMonth mes) {
        return mes.getMonth().getDisplayName(TextStyle.SHORT, ESPANOL).replace(".", "") + " "
                + String.valueOf(mes.getYear()).substring(2);
    }

    private static BarRenderer barrasSimples(Color color) {
        BarRenderer barras = new BarRenderer();
        barras.setBarPainter(new StandardBarPainter());
        barras.setShadowVisible(false);
        barras.setSeriesPaint(0, color);
        barras.setMaximumBarWidth(0.08);
        return barras;
    }

    // ------------------------------------------------------------------ estilo

    private static JFreeChart aplicarTema(JFreeChart grafico) {
        grafico.setBackgroundPaint(FONDO);
        grafico.setBorderVisible(false);
        grafico.setPadding(new RectangleInsets(6, 4, 4, 8));
        grafico.setAntiAlias(true);
        grafico.setTextAntiAlias(true);
        if (grafico.getLegend() != null) {
            grafico.getLegend().setBackgroundPaint(FONDO);
            grafico.getLegend().setItemPaint(TEXTO);
            grafico.getLegend().setItemFont(FUENTE);
            grafico.getLegend().setFrame(BlockBorder.NONE);
        }
        if (grafico.getPlot() instanceof CategoryPlot plot) {
            estiloPlot(plot.getDomainAxis(), plot.getRangeAxis());
            plot.setBackgroundPaint(FONDO);
            plot.setOutlineVisible(false);
            plot.setRangeGridlinePaint(GRILLA);
            plot.setDomainGridlinesVisible(false);
        } else if (grafico.getPlot() instanceof XYPlot plot) {
            estiloPlot(plot.getDomainAxis(), plot.getRangeAxis());
            plot.setBackgroundPaint(FONDO);
            plot.setOutlineVisible(false);
            plot.setRangeGridlinePaint(GRILLA);
            plot.setDomainGridlinePaint(GRILLA);
        }
        return grafico;
    }

    private static void estiloPlot(Axis... ejes) {
        for (Axis eje : ejes) {
            eje.setLabelPaint(TEXTO);
            eje.setLabelFont(FUENTE);
            eje.setTickLabelPaint(TEXTO);
            eje.setTickLabelFont(FUENTE);
            eje.setAxisLinePaint(GRILLA);
            eje.setTickMarkPaint(GRILLA);
        }
    }

    private static NumberAxis ejeMonto(Moneda moneda) {
        NumberAxis eje = new NumberAxis();
        eje.setNumberFormatOverride(new DecimalFormat("'" + moneda.simbolo() + "' #,##0",
                DecimalFormatSymbols.getInstance(Locale.US)));
        eje.setAutoRangeIncludesZero(true);
        return eje;
    }

    private static ValueMarker marcador(double valor, String etiqueta, Color color) {
        ValueMarker marcador = new ValueMarker(valor, color, LINEA_PUNTEADA);
        marcador.setLabel(etiqueta);
        marcador.setLabelPaint(color);
        marcador.setLabelFont(FUENTE);
        marcador.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        marcador.setLabelTextAnchor(TextAnchor.TOP_LEFT);
        return marcador;
    }

    /** "ene", "feb"... (se añade el año solo en enero para no saturar el eje). */
    private static String etiquetaMes(YearMonth mes) {
        String nombre = mes.getMonth().getDisplayName(TextStyle.SHORT, ESPANOL).replace(".", "");
        return mes.getMonthValue() == 1 ? nombre + " " + String.valueOf(mes.getYear()).substring(2) : nombre;
    }

    private static String abreviar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }
}
