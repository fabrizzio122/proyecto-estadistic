package com.businessanalyzer.informes;

import com.businessanalyzer.analytics.AnalisisFinancieroFGEM;
import com.businessanalyzer.analytics.AnalisisFinancieroFGEM.ResultadoMensualFGEM;
import com.businessanalyzer.analytics.EstadoResultadosFGEM;
import com.businessanalyzer.analytics.LecturaFinancieraFGEM;
import com.businessanalyzer.analytics.PuntoEquilibrioFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM;
import com.businessanalyzer.analytics.VariacionFGEM;
import com.businessanalyzer.analytics.diagnostico.AreaFGEM;
import com.businessanalyzer.analytics.diagnostico.DiagnosticoFGEM;
import com.businessanalyzer.analytics.diagnostico.HallazgoFGEM;
import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;
import com.businessanalyzer.informes.LibroExcelFGEM.CantidadFGEM;
import com.businessanalyzer.informes.LibroExcelFGEM.PorcentajeFGEM;
import com.businessanalyzer.model.EstadoOperacionFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.MovimientoFinancieroFGEM;
import com.businessanalyzer.model.OperacionFGEM;
import com.businessanalyzer.ui.GraficosFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

/**
 * Redacta el contenido de cada informe a partir de los datos ya calculados: qué secciones lleva,
 * en qué orden y con qué textos. No calcula indicadores (eso lo hacen los paquetes de analytics)
 * ni accede a la base de datos.
 */
public final class RedactorInformesFGEM {

    private static final int ANCHO_GRAFICO_FGEM = 515;
    private static final int ALTO_GRAFICO_FGEM = 222;
    private static final int PRODUCTOS_EJECUTIVO_FGEM = 10;

    private final DatosInformeFGEM datosFGEM;
    private final AnalisisFinancieroFGEM aFGEM;
    private final MonedaFGEM monedaFGEM;

    public RedactorInformesFGEM(DatosInformeFGEM datosFGEM) {
        this.datosFGEM = datosFGEM;
        this.aFGEM = datosFGEM.analisisFGEM();
        this.monedaFGEM = datosFGEM.empresaFGEM().monedaFGEM();
    }

    // ------------------------------------------------------------------ PDF

    public ContenidoInformeFGEM informeEjecutivoFGEM() {
        ContenidoInformeFGEM cFGEM = new ContenidoInformeFGEM();
        EstadoResultadosFGEM eFGEM = aFGEM.actualFGEM();
        DiagnosticoFGEM dFGEM = datosFGEM.diagnosticoFGEM();
        cFGEM.indicadoresFGEM("Ventas netas", dineroFGEM(eFGEM.ventasNetasFGEM()),
                "Utilidad neta", dineroFGEM(eFGEM.utilidadNetaFGEM()),
                "Margen neto", MontosFGEM.porcentajeFGEM(eFGEM.margenNetoFGEM()),
                "Salud del negocio", dFGEM != null && dFGEM.evaluadoFGEM() ? dFGEM.puntajeFGEM() + "/100 · " + dFGEM.nivelFGEM().descripcionFGEM() : "Sin datos");

        cFGEM.seccionFGEM("Resumen del período");
        LecturaFinancieraFGEM.generarFGEM(aFGEM, monedaFGEM).forEach(cFGEM::textoFGEM);

        estadoDeResultadosFGEM(cFGEM);
        evolucionGraficoFGEM(cFGEM);
        puntoDeEquilibrioFGEM(cFGEM);

        cFGEM.seccionFGEM("Productos más rentables");
        if (aFGEM.productosFGEM().isEmpty()) {
            cFGEM.textoFGEM("No hubo ventas de productos en el período.");
        } else {
            tablaProductosFGEM(cFGEM, aFGEM.productosFGEM().stream().limit(PRODUCTOS_EJECUTIVO_FGEM).toList(), false);
            if (aFGEM.productosFGEM().size() > PRODUCTOS_EJECUTIVO_FGEM) {
                cFGEM.notaFGEM("Se muestran los " + PRODUCTOS_EJECUTIVO_FGEM + " productos con mayor margen de "
                        + aFGEM.productosFGEM().size() + ". El detalle completo está en el informe «Rentabilidad por producto».");
            }
        }

        if (dFGEM != null) {
            diagnosticoFGEM(cFGEM, dFGEM);
        }
        return cFGEM;
    }

    public ContenidoInformeFGEM estadoResultadosFGEM() {
        ContenidoInformeFGEM cFGEM = new ContenidoInformeFGEM();
        EstadoResultadosFGEM eFGEM = aFGEM.actualFGEM();
        cFGEM.indicadoresFGEM("Ventas netas", dineroFGEM(eFGEM.ventasNetasFGEM()),
                "Utilidad bruta", dineroFGEM(eFGEM.utilidadBrutaFGEM()),
                "Utilidad operativa", dineroFGEM(eFGEM.utilidadOperativaFGEM()),
                "Utilidad neta", dineroFGEM(eFGEM.utilidadNetaFGEM()));
        estadoDeResultadosFGEM(cFGEM);

        cFGEM.seccionFGEM("Márgenes");
        cFGEM.encabezadoFGEM("Margen", "Período", "Anterior", "Diferencia");
        margenFGEM(cFGEM, "Margen bruto", EstadoResultadosFGEM::margenBrutoFGEM);
        margenFGEM(cFGEM, "Margen operativo", EstadoResultadosFGEM::margenOperativoFGEM);
        margenFGEM(cFGEM, "Margen neto", EstadoResultadosFGEM::margenNetoFGEM);

        puntoDeEquilibrioFGEM(cFGEM);
        evolucionGraficoFGEM(cFGEM);
        cFGEM.encabezadoFGEM("Mes", "Ventas netas", "Utilidad bruta", "Utilidad neta", "Margen neto");
        for (ResultadoMensualFGEM mFGEM : aFGEM.evolucionFGEM()) {
            EstadoResultadosFGEM emFGEM = mFGEM.estadoFGEM();
            cFGEM.filaFGEM(FechasFGEM.nombreMesFGEM(mFGEM.mesFGEM().getMonthValue()) + " " + mFGEM.mesFGEM().getYear(), dineroFGEM(emFGEM.ventasNetasFGEM()),
                    dineroFGEM(emFGEM.utilidadBrutaFGEM()), dineroFGEM(emFGEM.utilidadNetaFGEM()), MontosFGEM.porcentajeFGEM(emFGEM.margenNetoFGEM()));
        }
        return cFGEM;
    }

    public ContenidoInformeFGEM rentabilidadProductosFGEM() {
        ContenidoInformeFGEM cFGEM = new ContenidoInformeFGEM();
        List<RentabilidadProductoFGEM> productosFGEM = aFGEM.productosFGEM();
        BigDecimal ventasFGEM = sumaFGEM(productosFGEM, RentabilidadProductoFGEM::ventasFGEM);
        BigDecimal margenFGEM = sumaFGEM(productosFGEM, RentabilidadProductoFGEM::margenFGEM);
        cFGEM.indicadoresFGEM("Productos vendidos", String.valueOf(productosFGEM.size()),
                "Ventas", dineroFGEM(ventasFGEM),
                "Margen bruto", dineroFGEM(margenFGEM),
                "Margen promedio", MontosFGEM.porcentajeFGEM(ventasFGEM.signum() == 0 ? null
                        : margenFGEM.divide(ventasFGEM, 6, RoundingMode.HALF_UP)));
        if (productosFGEM.isEmpty()) {
            cFGEM.seccionFGEM("Detalle por producto");
            cFGEM.textoFGEM("No hubo ventas de productos en el período.");
            return cFGEM;
        }
        cFGEM.seccionFGEM("Productos con mayor margen");
        cFGEM.graficoFGEM(GraficosFGEM.imagenParaImpresionFGEM(GraficosFGEM.rentabilidadProductosFGEM(productosFGEM, 12, monedaFGEM),
                ANCHO_GRAFICO_FGEM, ALTO_GRAFICO_FGEM));

        cFGEM.seccionFGEM("Detalle por producto");
        tablaProductosFGEM(cFGEM, productosFGEM, true);
        List<RentabilidadProductoFGEM> conPerdidaFGEM = productosFGEM.stream().filter(pFGEM -> pFGEM.margenFGEM().signum() < 0).toList();
        if (!conPerdidaFGEM.isEmpty()) {
            cFGEM.espacioFGEM();
            cFGEM.textoFGEM(conPerdidaFGEM.size() + (conPerdidaFGEM.size() == 1 ? " producto se vendió" : " productos se vendieron")
                    + " por debajo de su costo, con una pérdida total de "
                    + dineroFGEM(sumaFGEM(conPerdidaFGEM, RentabilidadProductoFGEM::margenFGEM).negate()) + ".");
        }
        cFGEM.notaFGEM("El costo es el que tenía cada producto en el momento de la venta. El margen no incluye los gastos "
                + "operativos (alquiler, sueldos, etc.).");
        return cFGEM;
    }

    private void estadoDeResultadosFGEM(ContenidoInformeFGEM cFGEM) {
        EstadoResultadosFGEM eFGEM = aFGEM.actualFGEM();
        EstadoResultadosFGEM pFGEM = aFGEM.anteriorFGEM();
        cFGEM.seccionFGEM("Estado de resultados");
        cFGEM.encabezadoFGEM("Concepto", "Período", "Anterior", "Variación", "% de ventas");
        lineaFGEM(cFGEM, false, "Ventas netas", eFGEM.ventasNetasFGEM(), pFGEM.ventasNetasFGEM(), eFGEM);
        lineaFGEM(cFGEM, false, "(-) Costo de ventas", eFGEM.costoVentasFGEM(), pFGEM.costoVentasFGEM(), eFGEM);
        lineaFGEM(cFGEM, true, "Utilidad bruta", eFGEM.utilidadBrutaFGEM(), pFGEM.utilidadBrutaFGEM(), eFGEM);
        lineaFGEM(cFGEM, false, "(-) Gastos operativos", eFGEM.gastosOperativosFGEM(), pFGEM.gastosOperativosFGEM(), eFGEM);
        lineaFGEM(cFGEM, true, "Utilidad operativa", eFGEM.utilidadOperativaFGEM(), pFGEM.utilidadOperativaFGEM(), eFGEM);
        lineaFGEM(cFGEM, false, "(+) Ingresos financieros", eFGEM.ingresosFinancierosFGEM(), pFGEM.ingresosFinancierosFGEM(), eFGEM);
        lineaFGEM(cFGEM, false, "(-) Gastos financieros", eFGEM.gastosFinancierosFGEM(), pFGEM.gastosFinancierosFGEM(), eFGEM);
        lineaFGEM(cFGEM, false, "(+) Otros ingresos", eFGEM.otrosIngresosFGEM(), pFGEM.otrosIngresosFGEM(), eFGEM);
        lineaFGEM(cFGEM, true, "Utilidad antes de impuestos", eFGEM.utilidadAntesImpuestosFGEM(), pFGEM.utilidadAntesImpuestosFGEM(), eFGEM);
        lineaFGEM(cFGEM, false, "(-) Impuesto a la renta", eFGEM.impuestoRentaFGEM(), pFGEM.impuestoRentaFGEM(), eFGEM);
        lineaFGEM(cFGEM, true, "Utilidad neta", eFGEM.utilidadNetaFGEM(), pFGEM.utilidadNetaFGEM(), eFGEM);
        cFGEM.notaFGEM("Período anterior: " + aFGEM.periodoAnteriorFGEM().descripcionFGEM() + ". Montos sin impuestos a las ventas; "
                + "no incluye operaciones ni movimientos anulados.");
    }

    private void lineaFGEM(ContenidoInformeFGEM cFGEM, boolean totalFGEM, String conceptoFGEM, BigDecimal actualFGEM, BigDecimal anteriorFGEM,
                       EstadoResultadosFGEM eFGEM) {
        String[] valoresFGEM = {dineroFGEM(actualFGEM), dineroFGEM(anteriorFGEM), variacionFGEM(actualFGEM, anteriorFGEM),
                MontosFGEM.porcentajeFGEM(eFGEM.sobreVentasFGEM(actualFGEM))};
        if (totalFGEM) {
            cFGEM.totalFGEM(conceptoFGEM, valoresFGEM);
        } else {
            cFGEM.filaFGEM(conceptoFGEM, valoresFGEM);
        }
    }

    private void margenFGEM(ContenidoInformeFGEM cFGEM, String nombreFGEM, Function<EstadoResultadosFGEM, BigDecimal> margenFGEM) {
        BigDecimal actualFGEM = margenFGEM.apply(aFGEM.actualFGEM());
        BigDecimal anteriorFGEM = margenFGEM.apply(aFGEM.anteriorFGEM());
        String diferenciaFGEM = actualFGEM == null || anteriorFGEM == null ? "—"
                : String.format(java.util.Locale.US, "%+.1f pp",
                actualFGEM.subtract(anteriorFGEM).movePointRight(2).doubleValue());
        cFGEM.filaFGEM(nombreFGEM, MontosFGEM.porcentajeFGEM(actualFGEM), MontosFGEM.porcentajeFGEM(anteriorFGEM), diferenciaFGEM);
    }

    private void evolucionGraficoFGEM(ContenidoInformeFGEM cFGEM) {
        cFGEM.seccionFGEM("Evolución mensual (últimos " + aFGEM.evolucionFGEM().size() + " meses)");
        boolean hayVentasFGEM = aFGEM.evolucionFGEM().stream().anyMatch(mFGEM -> mFGEM.estadoFGEM().ventasNetasFGEM().signum() != 0);
        if (hayVentasFGEM) {
            cFGEM.graficoFGEM(GraficosFGEM.imagenParaImpresionFGEM(GraficosFGEM.evolucionMensualFGEM(aFGEM.evolucionFGEM(), monedaFGEM),
                    ANCHO_GRAFICO_FGEM, ALTO_GRAFICO_FGEM));
        } else {
            cFGEM.textoFGEM("No hay ventas registradas en los últimos meses.");
        }
    }

    private void puntoDeEquilibrioFGEM(ContenidoInformeFGEM cFGEM) {
        PuntoEquilibrioFGEM peFGEM = aFGEM.equilibrioFGEM();
        cFGEM.seccionFGEM("Punto de equilibrio");
        cFGEM.encabezadoFGEM("Concepto", "Período");
        cFGEM.filaFGEM("Ventas del período", dineroFGEM(peFGEM.ventasFGEM()));
        cFGEM.filaFGEM("(-) Costos variables", dineroFGEM(peFGEM.costosVariablesFGEM()));
        cFGEM.totalFGEM("Margen de contribución", dineroFGEM(peFGEM.margenContribucionFGEM()));
        cFGEM.filaFGEM("Razón de contribución", MontosFGEM.porcentajeFGEM(peFGEM.razonContribucionFGEM()));
        cFGEM.filaFGEM("Costos fijos", dineroFGEM(peFGEM.costosFijosFGEM()));
        cFGEM.totalFGEM("Ventas de equilibrio", peFGEM.ventasEquilibrioFGEM() == null ? "No alcanzable" : dineroFGEM(peFGEM.ventasEquilibrioFGEM()));
        cFGEM.filaFGEM("Margen de seguridad", MontosFGEM.porcentajeFGEM(peFGEM.margenSeguridadFGEM()));
        if (peFGEM.ventasEquilibrioFGEM() != null) {
            cFGEM.textoFGEM(peFGEM.alcanzadoFGEM()
                    ? "Las ventas superan el punto de equilibrio: podrían caer " + MontosFGEM.porcentajeFGEM(peFGEM.margenSeguridadFGEM())
                    + " antes de que el negocio entre en pérdida."
                    : "Faltaron " + dineroFGEM(peFGEM.ventasFaltantesFGEM()) + " en ventas para cubrir todos los costos del período.");
        }
    }

    private void tablaProductosFGEM(ContenidoInformeFGEM cFGEM, List<RentabilidadProductoFGEM> productosFGEM, boolean conTotalFGEM) {
        cFGEM.encabezadoFGEM("Producto o servicio", "Cantidad", "Ventas", "Margen", "Margen %");
        for (RentabilidadProductoFGEM pFGEM : productosFGEM) {
            cFGEM.filaFGEM(pFGEM.nombreFGEM(), MontosFGEM.cantidadFGEM(pFGEM.cantidadFGEM()), dineroFGEM(pFGEM.ventasFGEM()), dineroFGEM(pFGEM.margenFGEM()),
                    MontosFGEM.porcentajeFGEM(pFGEM.margenSobreVentasFGEM()));
        }
        if (conTotalFGEM) {
            BigDecimal ventasFGEM = sumaFGEM(productosFGEM, RentabilidadProductoFGEM::ventasFGEM);
            BigDecimal margenFGEM = sumaFGEM(productosFGEM, RentabilidadProductoFGEM::margenFGEM);
            cFGEM.totalFGEM("Total", "", dineroFGEM(ventasFGEM), dineroFGEM(margenFGEM),
                    MontosFGEM.porcentajeFGEM(ventasFGEM.signum() == 0 ? null : margenFGEM.divide(ventasFGEM, 6, RoundingMode.HALF_UP)));
        }
    }

    private void diagnosticoFGEM(ContenidoInformeFGEM cFGEM, DiagnosticoFGEM dFGEM) {
        cFGEM.seccionFGEM("Diagnóstico empresarial");
        cFGEM.notaFGEM("El diagnóstico evalúa siempre los últimos meses completos (" + dFGEM.periodoFGEM().descripcionFGEM()
                + "), sin importar el período elegido para el resto del informe.");
        if (!dFGEM.evaluadoFGEM()) {
            cFGEM.textoFGEM(dFGEM.resumenFGEM().get(0));
            return;
        }
        dFGEM.resumenFGEM().forEach(cFGEM::textoFGEM);
        for (AreaFGEM areaFGEM : AreaFGEM.values()) {
            if (dFGEM.deAreaFGEM(areaFGEM).isEmpty()) {
                continue;
            }
            cFGEM.espacioFGEM();
            cFGEM.encabezadoFGEM(areaFGEM.descripcionFGEM() + (dFGEM.puntajePorAreaFGEM().containsKey(areaFGEM)
                    ? "  ·  " + dFGEM.puntajePorAreaFGEM().get(areaFGEM) + "/100" : ""));
            for (HallazgoFGEM hFGEM : dFGEM.deAreaFGEM(areaFGEM)) {
                cFGEM.semaforoFGEM(hFGEM.tipoFGEM().nombreFGEM(), hFGEM.semaforoFGEM(), hFGEM.valorFGEM(), hFGEM.lecturaFGEM());
            }
        }
        List<HallazgoFGEM> recomendacionesFGEM = dFGEM.recomendacionesFGEM();
        cFGEM.seccionFGEM("Recomendaciones");
        if (recomendacionesFGEM.isEmpty()) {
            cFGEM.textoFGEM("No hay alertas: todos los indicadores evaluados están en verde.");
        }
        int nFGEM = 1;
        for (HallazgoFGEM hFGEM : recomendacionesFGEM) {
            cFGEM.recomendacionFGEM(hFGEM.semaforoFGEM() == SemaforoFGEM.ROJO ? "URGENTE" : "MEJORAR", hFGEM.semaforoFGEM(),
                    nFGEM++ + ". " + hFGEM.tipoFGEM().nombreFGEM() + " (" + hFGEM.valorFGEM() + ")", hFGEM.recomendacionFGEM());
        }
    }

    // ------------------------------------------------------------------ Excel

    public LibroExcelFGEM libroExcelFGEM() {
        LibroExcelFGEM libroFGEM = new LibroExcelFGEM(monedaFGEM);
        String empresaFGEM = datosFGEM.empresaFGEM().nombreVisibleFGEM();
        String periodoFGEM = aFGEM.periodoFGEM().descripcionFGEM();

        EstadoResultadosFGEM eFGEM = aFGEM.actualFGEM();
        EstadoResultadosFGEM pFGEM = aFGEM.anteriorFGEM();
        LibroExcelFGEM.HojaFGEM resumenFGEM = libroFGEM.hojaFGEM("Resumen")
                .tituloFGEM(empresaFGEM + " · Estado de resultados", periodoFGEM + " (anterior: " + aFGEM.periodoAnteriorFGEM().descripcionFGEM() + ")")
                .encabezadosFGEM("Concepto", "Período", "Anterior", "Variación", "% de ventas");
        filaResumenFGEM(resumenFGEM, false, "Ventas netas", eFGEM.ventasNetasFGEM(), pFGEM.ventasNetasFGEM(), eFGEM);
        filaResumenFGEM(resumenFGEM, false, "(-) Costo de ventas", eFGEM.costoVentasFGEM(), pFGEM.costoVentasFGEM(), eFGEM);
        filaResumenFGEM(resumenFGEM, true, "Utilidad bruta", eFGEM.utilidadBrutaFGEM(), pFGEM.utilidadBrutaFGEM(), eFGEM);
        filaResumenFGEM(resumenFGEM, false, "(-) Gastos operativos", eFGEM.gastosOperativosFGEM(), pFGEM.gastosOperativosFGEM(), eFGEM);
        filaResumenFGEM(resumenFGEM, true, "Utilidad operativa", eFGEM.utilidadOperativaFGEM(), pFGEM.utilidadOperativaFGEM(), eFGEM);
        filaResumenFGEM(resumenFGEM, false, "(+) Ingresos financieros", eFGEM.ingresosFinancierosFGEM(), pFGEM.ingresosFinancierosFGEM(), eFGEM);
        filaResumenFGEM(resumenFGEM, false, "(-) Gastos financieros", eFGEM.gastosFinancierosFGEM(), pFGEM.gastosFinancierosFGEM(), eFGEM);
        filaResumenFGEM(resumenFGEM, false, "(+) Otros ingresos", eFGEM.otrosIngresosFGEM(), pFGEM.otrosIngresosFGEM(), eFGEM);
        filaResumenFGEM(resumenFGEM, true, "Utilidad antes de impuestos", eFGEM.utilidadAntesImpuestosFGEM(), pFGEM.utilidadAntesImpuestosFGEM(), eFGEM);
        filaResumenFGEM(resumenFGEM, false, "(-) Impuesto a la renta", eFGEM.impuestoRentaFGEM(), pFGEM.impuestoRentaFGEM(), eFGEM);
        filaResumenFGEM(resumenFGEM, true, "Utilidad neta", eFGEM.utilidadNetaFGEM(), pFGEM.utilidadNetaFGEM(), eFGEM);
        int finResumenFGEM = resumenFGEM.filaActualFGEM();
        PuntoEquilibrioFGEM peFGEM = aFGEM.equilibrioFGEM();
        resumenFGEM.espacioFGEM()
                .filaFGEM("Margen de contribución", peFGEM.margenContribucionFGEM())
                .filaFGEM("Costos fijos", peFGEM.costosFijosFGEM())
                .filaFGEM("Ventas de equilibrio", peFGEM.ventasEquilibrioFGEM() == null ? "No alcanzable" : peFGEM.ventasEquilibrioFGEM())
                .filaFGEM("Margen de seguridad", new PorcentajeFGEM(peFGEM.margenSeguridadFGEM()));
        resumenFGEM.terminarFGEM(finResumenFGEM);

        LibroExcelFGEM.HojaFGEM evolucionFGEM = libroFGEM.hojaFGEM("Evolución mensual")
                .tituloFGEM(empresaFGEM + " · Evolución mensual", "Últimos " + aFGEM.evolucionFGEM().size() + " meses")
                .encabezadosFGEM("Mes", "Ventas netas", "Costo de ventas", "Utilidad bruta", "Gastos operativos",
                        "Utilidad neta", "Margen neto");
        for (ResultadoMensualFGEM mFGEM : aFGEM.evolucionFGEM()) {
            EstadoResultadosFGEM emFGEM = mFGEM.estadoFGEM();
            evolucionFGEM.filaFGEM(FechasFGEM.nombreMesFGEM(mFGEM.mesFGEM().getMonthValue()) + " " + mFGEM.mesFGEM().getYear(), emFGEM.ventasNetasFGEM(),
                    emFGEM.costoVentasFGEM(), emFGEM.utilidadBrutaFGEM(), emFGEM.gastosOperativosFGEM(), emFGEM.utilidadNetaFGEM(),
                    new PorcentajeFGEM(emFGEM.margenNetoFGEM()));
        }
        evolucionFGEM.terminarFGEM(evolucionFGEM.filaActualFGEM());

        LibroExcelFGEM.HojaFGEM productosFGEM = libroFGEM.hojaFGEM("Productos")
                .tituloFGEM(empresaFGEM + " · Rentabilidad por producto", periodoFGEM)
                .encabezadosFGEM("Código", "Producto o servicio", "Tipo", "Categoría", "Cantidad", "Ventas", "Costo",
                        "Margen", "Margen %");
        for (RentabilidadProductoFGEM rFGEM : aFGEM.productosFGEM()) {
            productosFGEM.filaFGEM(rFGEM.codigoFGEM(), rFGEM.nombreFGEM(), rFGEM.tipoFGEM().descripcionFGEM(), rFGEM.categoriaFGEM(), new CantidadFGEM(rFGEM.cantidadFGEM()),
                    rFGEM.ventasFGEM(), rFGEM.costoFGEM(), rFGEM.margenFGEM(), new PorcentajeFGEM(rFGEM.margenSobreVentasFGEM()));
        }
        int finProductosFGEM = productosFGEM.filaActualFGEM();
        productosFGEM.totalFGEM("Total", null, null, null, null, productosFGEM.sumaFGEM(5, null, null), productosFGEM.sumaFGEM(6, null, null),
                productosFGEM.sumaFGEM(7, null, null));
        productosFGEM.terminarFGEM(finProductosFGEM);

        hojaOperacionesFGEM(libroFGEM, "Ventas", "Cliente", datosFGEM.ventasFGEM(), true);
        hojaOperacionesFGEM(libroFGEM, "Compras", "Proveedor", datosFGEM.comprasFGEM(), false);

        LibroExcelFGEM.HojaFGEM movimientosFGEM = libroFGEM.hojaFGEM("Ingresos y gastos")
                .tituloFGEM(empresaFGEM + " · Ingresos y gastos", periodoFGEM + ". Los anulados se listan pero no se suman.")
                .encabezadosFGEM("Fecha", "Tipo", "Categoría", "Grupo", "Comportamiento", "Descripción", "Comprobante",
                        "Monto", "Estado");
        for (MovimientoFinancieroFGEM mFGEM : datosFGEM.movimientosFGEM()) {
            movimientosFGEM.filaFGEM(mFGEM.fechaFGEM(), mFGEM.tipoFGEM().descripcionFGEM(), mFGEM.categoriaFGEM().descripcionFGEM(),
                    mFGEM.categoriaFGEM().grupoFGEM().descripcionFGEM(), mFGEM.comportamientoFGEM() == null ? null : mFGEM.comportamientoFGEM().descripcionFGEM(),
                    mFGEM.descripcionFGEM(), mFGEM.comprobanteFGEM(), mFGEM.montoFGEM(), mFGEM.anuladoFGEM() ? "Anulado" : "Vigente");
        }
        int finMovimientosFGEM = movimientosFGEM.filaActualFGEM();
        LibroExcelFGEM.FormulaFGEM ingresosFGEM = movimientosFGEM.sumaSiFGEM(7, 1, "Ingreso", 8, "Vigente");
        LibroExcelFGEM.FormulaFGEM gastosFGEM = movimientosFGEM.sumaSiFGEM(7, 1, "Gasto", 8, "Vigente");
        movimientosFGEM.totalFGEM("Total de ingresos", null, null, null, null, null, null, ingresosFGEM);
        movimientosFGEM.totalFGEM("Total de gastos", null, null, null, null, null, null, gastosFGEM);
        movimientosFGEM.terminarFGEM(finMovimientosFGEM);
        return libroFGEM;
    }

    private void hojaOperacionesFGEM(LibroExcelFGEM libroFGEM, String nombreFGEM, String contraparteFGEM, List<OperacionFGEM> operacionesFGEM,
                                 boolean conMargenFGEM) {
        String empresaFGEM = datosFGEM.empresaFGEM().nombreVisibleFGEM();
        LibroExcelFGEM.HojaFGEM hojaFGEM = libroFGEM.hojaFGEM(nombreFGEM)
                .tituloFGEM(empresaFGEM + " · " + nombreFGEM, aFGEM.periodoFGEM().descripcionFGEM() + ". Montos sin impuesto en «Subtotal»; "
                        + "las anuladas se listan pero no se suman.");
        if (conMargenFGEM) {
            hojaFGEM.encabezadosFGEM("Número", "Fecha", contraparteFGEM, "Documento", "Comprobante", "Ítems", "Subtotal",
                    "Impuesto", "Total", "Costo", "Margen", "Estado");
        } else {
            hojaFGEM.encabezadosFGEM("Número", "Fecha", contraparteFGEM, "Documento", "Comprobante", "Ítems", "Subtotal",
                    "Impuesto", "Total", "Estado");
        }
        for (OperacionFGEM oFGEM : operacionesFGEM) {
            String estadoFGEM = oFGEM.estadoFGEM() == EstadoOperacionFGEM.ANULADA ? "Anulada" : "Registrada";
            if (conMargenFGEM) {
                hojaFGEM.filaFGEM(oFGEM.numeroFGEM(), oFGEM.fechaFGEM(), oFGEM.contraparteFGEM(), oFGEM.documentoContraparteFGEM(), oFGEM.comprobanteFGEM(),
                        oFGEM.cantidadItemsFGEM(), oFGEM.subtotalFGEM(), oFGEM.impuestoFGEM(), oFGEM.totalFGEM(), oFGEM.costoTotalFGEM(),
                        oFGEM.subtotalFGEM().subtract(oFGEM.costoTotalFGEM()), estadoFGEM);
            } else {
                hojaFGEM.filaFGEM(oFGEM.numeroFGEM(), oFGEM.fechaFGEM(), oFGEM.contraparteFGEM(), oFGEM.documentoContraparteFGEM(), oFGEM.comprobanteFGEM(),
                        oFGEM.cantidadItemsFGEM(), oFGEM.subtotalFGEM(), oFGEM.impuestoFGEM(), oFGEM.totalFGEM(), estadoFGEM);
            }
        }
        int finFGEM = hojaFGEM.filaActualFGEM();
        int columnaEstadoFGEM = conMargenFGEM ? 11 : 9;
        if (conMargenFGEM) {
            hojaFGEM.totalFGEM("Total (registradas)", null, null, null, null, null, hojaFGEM.sumaFGEM(6, columnaEstadoFGEM, "Registrada"),
                    hojaFGEM.sumaFGEM(7, columnaEstadoFGEM, "Registrada"), hojaFGEM.sumaFGEM(8, columnaEstadoFGEM, "Registrada"),
                    hojaFGEM.sumaFGEM(9, columnaEstadoFGEM, "Registrada"), hojaFGEM.sumaFGEM(10, columnaEstadoFGEM, "Registrada"));
        } else {
            hojaFGEM.totalFGEM("Total (registradas)", null, null, null, null, null, hojaFGEM.sumaFGEM(6, columnaEstadoFGEM, "Registrada"),
                    hojaFGEM.sumaFGEM(7, columnaEstadoFGEM, "Registrada"), hojaFGEM.sumaFGEM(8, columnaEstadoFGEM, "Registrada"));
        }
        hojaFGEM.terminarFGEM(finFGEM);
    }

    private void filaResumenFGEM(LibroExcelFGEM.HojaFGEM hojaFGEM, boolean totalFGEM, String conceptoFGEM, BigDecimal actualFGEM,
                             BigDecimal anteriorFGEM, EstadoResultadosFGEM eFGEM) {
        Object[] valoresFGEM = {conceptoFGEM, actualFGEM, anteriorFGEM, new PorcentajeFGEM(new VariacionFGEM(actualFGEM, anteriorFGEM).relativaFGEM()),
                new PorcentajeFGEM(eFGEM.sobreVentasFGEM(actualFGEM))};
        if (totalFGEM) {
            hojaFGEM.totalFGEM(valoresFGEM);
        } else {
            hojaFGEM.filaFGEM(valoresFGEM);
        }
    }

    // ------------------------------------------------------------------ utilidades

    private String dineroFGEM(BigDecimal valorFGEM) {
        return valorFGEM == null ? "—" : MontosFGEM.formatearFGEM(valorFGEM.setScale(2, RoundingMode.HALF_UP), monedaFGEM);
    }

    private static String variacionFGEM(BigDecimal actualFGEM, BigDecimal anteriorFGEM) {
        BigDecimal relativaFGEM = new VariacionFGEM(actualFGEM, anteriorFGEM).relativaFGEM();
        if (relativaFGEM == null) {
            return "—";
        }
        return (relativaFGEM.signum() > 0 ? "+" : "") + MontosFGEM.porcentajeFGEM(relativaFGEM);
    }

    private static <T> BigDecimal sumaFGEM(List<T> elementosFGEM, Function<T, BigDecimal> valorFGEM) {
        return elementosFGEM.stream().map(valorFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
