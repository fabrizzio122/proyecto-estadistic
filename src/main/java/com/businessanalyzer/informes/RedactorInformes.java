package com.businessanalyzer.informes;

import com.businessanalyzer.analytics.AnalisisFinanciero;
import com.businessanalyzer.analytics.AnalisisFinanciero.ResultadoMensual;
import com.businessanalyzer.analytics.EstadoResultados;
import com.businessanalyzer.analytics.LecturaFinanciera;
import com.businessanalyzer.analytics.PuntoEquilibrio;
import com.businessanalyzer.analytics.RentabilidadProducto;
import com.businessanalyzer.analytics.Variacion;
import com.businessanalyzer.analytics.diagnostico.Area;
import com.businessanalyzer.analytics.diagnostico.Diagnostico;
import com.businessanalyzer.analytics.diagnostico.Hallazgo;
import com.businessanalyzer.analytics.diagnostico.Semaforo;
import com.businessanalyzer.informes.LibroExcel.Cantidad;
import com.businessanalyzer.informes.LibroExcel.Porcentaje;
import com.businessanalyzer.model.EstadoOperacion;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.MovimientoFinanciero;
import com.businessanalyzer.model.Operacion;
import com.businessanalyzer.ui.Graficos;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

/**
 * Redacta el contenido de cada informe a partir de los datos ya calculados: qué secciones lleva,
 * en qué orden y con qué textos. No calcula indicadores (eso lo hacen los paquetes de analytics)
 * ni accede a la base de datos.
 */
public final class RedactorInformes {

    private static final int ANCHO_GRAFICO = 515;
    private static final int ALTO_GRAFICO = 222;
    private static final int PRODUCTOS_EJECUTIVO = 10;

    private final DatosInforme datos;
    private final AnalisisFinanciero a;
    private final Moneda moneda;

    public RedactorInformes(DatosInforme datos) {
        this.datos = datos;
        this.a = datos.analisis();
        this.moneda = datos.empresa().moneda();
    }

    // ------------------------------------------------------------------ PDF

    public ContenidoInforme informeEjecutivo() {
        ContenidoInforme c = new ContenidoInforme();
        EstadoResultados e = a.actual();
        Diagnostico d = datos.diagnostico();
        c.indicadores("Ventas netas", dinero(e.ventasNetas()),
                "Utilidad neta", dinero(e.utilidadNeta()),
                "Margen neto", Montos.porcentaje(e.margenNeto()),
                "Salud del negocio", d != null && d.evaluado() ? d.puntaje() + "/100 · " + d.nivel().descripcion() : "Sin datos");

        c.seccion("Resumen del período");
        LecturaFinanciera.generar(a, moneda).forEach(c::texto);

        estadoDeResultados(c);
        evolucionGrafico(c);
        puntoDeEquilibrio(c);

        c.seccion("Productos más rentables");
        if (a.productos().isEmpty()) {
            c.texto("No hubo ventas de productos en el período.");
        } else {
            tablaProductos(c, a.productos().stream().limit(PRODUCTOS_EJECUTIVO).toList(), false);
            if (a.productos().size() > PRODUCTOS_EJECUTIVO) {
                c.nota("Se muestran los " + PRODUCTOS_EJECUTIVO + " productos con mayor margen de "
                        + a.productos().size() + ". El detalle completo está en el informe «Rentabilidad por producto».");
            }
        }

        if (d != null) {
            diagnostico(c, d);
        }
        return c;
    }

    public ContenidoInforme estadoResultados() {
        ContenidoInforme c = new ContenidoInforme();
        EstadoResultados e = a.actual();
        c.indicadores("Ventas netas", dinero(e.ventasNetas()),
                "Utilidad bruta", dinero(e.utilidadBruta()),
                "Utilidad operativa", dinero(e.utilidadOperativa()),
                "Utilidad neta", dinero(e.utilidadNeta()));
        estadoDeResultados(c);

        c.seccion("Márgenes");
        c.encabezado("Margen", "Período", "Anterior", "Diferencia");
        margen(c, "Margen bruto", EstadoResultados::margenBruto);
        margen(c, "Margen operativo", EstadoResultados::margenOperativo);
        margen(c, "Margen neto", EstadoResultados::margenNeto);

        puntoDeEquilibrio(c);
        evolucionGrafico(c);
        c.encabezado("Mes", "Ventas netas", "Utilidad bruta", "Utilidad neta", "Margen neto");
        for (ResultadoMensual m : a.evolucion()) {
            EstadoResultados em = m.estado();
            c.fila(Fechas.nombreMes(m.mes().getMonthValue()) + " " + m.mes().getYear(), dinero(em.ventasNetas()),
                    dinero(em.utilidadBruta()), dinero(em.utilidadNeta()), Montos.porcentaje(em.margenNeto()));
        }
        return c;
    }

    public ContenidoInforme rentabilidadProductos() {
        ContenidoInforme c = new ContenidoInforme();
        List<RentabilidadProducto> productos = a.productos();
        BigDecimal ventas = suma(productos, RentabilidadProducto::ventas);
        BigDecimal margen = suma(productos, RentabilidadProducto::margen);
        c.indicadores("Productos vendidos", String.valueOf(productos.size()),
                "Ventas", dinero(ventas),
                "Margen bruto", dinero(margen),
                "Margen promedio", Montos.porcentaje(ventas.signum() == 0 ? null
                        : margen.divide(ventas, 6, RoundingMode.HALF_UP)));
        if (productos.isEmpty()) {
            c.seccion("Detalle por producto");
            c.texto("No hubo ventas de productos en el período.");
            return c;
        }
        c.seccion("Productos con mayor margen");
        c.grafico(Graficos.imagenParaImpresion(Graficos.rentabilidadProductos(productos, 12, moneda),
                ANCHO_GRAFICO, ALTO_GRAFICO));

        c.seccion("Detalle por producto");
        tablaProductos(c, productos, true);
        List<RentabilidadProducto> conPerdida = productos.stream().filter(p -> p.margen().signum() < 0).toList();
        if (!conPerdida.isEmpty()) {
            c.espacio();
            c.texto(conPerdida.size() + (conPerdida.size() == 1 ? " producto se vendió" : " productos se vendieron")
                    + " por debajo de su costo, con una pérdida total de "
                    + dinero(suma(conPerdida, RentabilidadProducto::margen).negate()) + ".");
        }
        c.nota("El costo es el que tenía cada producto en el momento de la venta. El margen no incluye los gastos "
                + "operativos (alquiler, sueldos, etc.).");
        return c;
    }

    private void estadoDeResultados(ContenidoInforme c) {
        EstadoResultados e = a.actual();
        EstadoResultados p = a.anterior();
        c.seccion("Estado de resultados");
        c.encabezado("Concepto", "Período", "Anterior", "Variación", "% de ventas");
        linea(c, false, "Ventas netas", e.ventasNetas(), p.ventasNetas(), e);
        linea(c, false, "(-) Costo de ventas", e.costoVentas(), p.costoVentas(), e);
        linea(c, true, "Utilidad bruta", e.utilidadBruta(), p.utilidadBruta(), e);
        linea(c, false, "(-) Gastos operativos", e.gastosOperativos(), p.gastosOperativos(), e);
        linea(c, true, "Utilidad operativa", e.utilidadOperativa(), p.utilidadOperativa(), e);
        linea(c, false, "(+) Ingresos financieros", e.ingresosFinancieros(), p.ingresosFinancieros(), e);
        linea(c, false, "(-) Gastos financieros", e.gastosFinancieros(), p.gastosFinancieros(), e);
        linea(c, false, "(+) Otros ingresos", e.otrosIngresos(), p.otrosIngresos(), e);
        linea(c, true, "Utilidad antes de impuestos", e.utilidadAntesImpuestos(), p.utilidadAntesImpuestos(), e);
        linea(c, false, "(-) Impuesto a la renta", e.impuestoRenta(), p.impuestoRenta(), e);
        linea(c, true, "Utilidad neta", e.utilidadNeta(), p.utilidadNeta(), e);
        c.nota("Período anterior: " + a.periodoAnterior().descripcion() + ". Montos sin impuestos a las ventas; "
                + "no incluye operaciones ni movimientos anulados.");
    }

    private void linea(ContenidoInforme c, boolean total, String concepto, BigDecimal actual, BigDecimal anterior,
                       EstadoResultados e) {
        String[] valores = {dinero(actual), dinero(anterior), variacion(actual, anterior),
                Montos.porcentaje(e.sobreVentas(actual))};
        if (total) {
            c.total(concepto, valores);
        } else {
            c.fila(concepto, valores);
        }
    }

    private void margen(ContenidoInforme c, String nombre, Function<EstadoResultados, BigDecimal> margen) {
        BigDecimal actual = margen.apply(a.actual());
        BigDecimal anterior = margen.apply(a.anterior());
        String diferencia = actual == null || anterior == null ? "—"
                : String.format(java.util.Locale.US, "%+.1f pp",
                actual.subtract(anterior).movePointRight(2).doubleValue());
        c.fila(nombre, Montos.porcentaje(actual), Montos.porcentaje(anterior), diferencia);
    }

    private void evolucionGrafico(ContenidoInforme c) {
        c.seccion("Evolución mensual (últimos " + a.evolucion().size() + " meses)");
        boolean hayVentas = a.evolucion().stream().anyMatch(m -> m.estado().ventasNetas().signum() != 0);
        if (hayVentas) {
            c.grafico(Graficos.imagenParaImpresion(Graficos.evolucionMensual(a.evolucion(), moneda),
                    ANCHO_GRAFICO, ALTO_GRAFICO));
        } else {
            c.texto("No hay ventas registradas en los últimos meses.");
        }
    }

    private void puntoDeEquilibrio(ContenidoInforme c) {
        PuntoEquilibrio pe = a.equilibrio();
        c.seccion("Punto de equilibrio");
        c.encabezado("Concepto", "Período");
        c.fila("Ventas del período", dinero(pe.ventas()));
        c.fila("(-) Costos variables", dinero(pe.costosVariables()));
        c.total("Margen de contribución", dinero(pe.margenContribucion()));
        c.fila("Razón de contribución", Montos.porcentaje(pe.razonContribucion()));
        c.fila("Costos fijos", dinero(pe.costosFijos()));
        c.total("Ventas de equilibrio", pe.ventasEquilibrio() == null ? "No alcanzable" : dinero(pe.ventasEquilibrio()));
        c.fila("Margen de seguridad", Montos.porcentaje(pe.margenSeguridad()));
        if (pe.ventasEquilibrio() != null) {
            c.texto(pe.alcanzado()
                    ? "Las ventas superan el punto de equilibrio: podrían caer " + Montos.porcentaje(pe.margenSeguridad())
                    + " antes de que el negocio entre en pérdida."
                    : "Faltaron " + dinero(pe.ventasFaltantes()) + " en ventas para cubrir todos los costos del período.");
        }
    }

    private void tablaProductos(ContenidoInforme c, List<RentabilidadProducto> productos, boolean conTotal) {
        c.encabezado("Producto o servicio", "Cantidad", "Ventas", "Margen", "Margen %");
        for (RentabilidadProducto p : productos) {
            c.fila(p.nombre(), Montos.cantidad(p.cantidad()), dinero(p.ventas()), dinero(p.margen()),
                    Montos.porcentaje(p.margenSobreVentas()));
        }
        if (conTotal) {
            BigDecimal ventas = suma(productos, RentabilidadProducto::ventas);
            BigDecimal margen = suma(productos, RentabilidadProducto::margen);
            c.total("Total", "", dinero(ventas), dinero(margen),
                    Montos.porcentaje(ventas.signum() == 0 ? null : margen.divide(ventas, 6, RoundingMode.HALF_UP)));
        }
    }

    private void diagnostico(ContenidoInforme c, Diagnostico d) {
        c.seccion("Diagnóstico empresarial");
        c.nota("El diagnóstico evalúa siempre los últimos meses completos (" + d.periodo().descripcion()
                + "), sin importar el período elegido para el resto del informe.");
        if (!d.evaluado()) {
            c.texto(d.resumen().get(0));
            return;
        }
        d.resumen().forEach(c::texto);
        for (Area area : Area.values()) {
            if (d.deArea(area).isEmpty()) {
                continue;
            }
            c.espacio();
            c.encabezado(area.descripcion() + (d.puntajePorArea().containsKey(area)
                    ? "  ·  " + d.puntajePorArea().get(area) + "/100" : ""));
            for (Hallazgo h : d.deArea(area)) {
                c.semaforo(h.tipo().nombre(), h.semaforo(), h.valor(), h.lectura());
            }
        }
        List<Hallazgo> recomendaciones = d.recomendaciones();
        c.seccion("Recomendaciones");
        if (recomendaciones.isEmpty()) {
            c.texto("No hay alertas: todos los indicadores evaluados están en verde.");
        }
        int n = 1;
        for (Hallazgo h : recomendaciones) {
            c.recomendacion(h.semaforo() == Semaforo.ROJO ? "URGENTE" : "MEJORAR", h.semaforo(),
                    n++ + ". " + h.tipo().nombre() + " (" + h.valor() + ")", h.recomendacion());
        }
    }

    // ------------------------------------------------------------------ Excel

    public LibroExcel libroExcel() {
        LibroExcel libro = new LibroExcel(moneda);
        String empresa = datos.empresa().nombreVisible();
        String periodo = a.periodo().descripcion();

        EstadoResultados e = a.actual();
        EstadoResultados p = a.anterior();
        LibroExcel.Hoja resumen = libro.hoja("Resumen")
                .titulo(empresa + " · Estado de resultados", periodo + " (anterior: " + a.periodoAnterior().descripcion() + ")")
                .encabezados("Concepto", "Período", "Anterior", "Variación", "% de ventas");
        filaResumen(resumen, false, "Ventas netas", e.ventasNetas(), p.ventasNetas(), e);
        filaResumen(resumen, false, "(-) Costo de ventas", e.costoVentas(), p.costoVentas(), e);
        filaResumen(resumen, true, "Utilidad bruta", e.utilidadBruta(), p.utilidadBruta(), e);
        filaResumen(resumen, false, "(-) Gastos operativos", e.gastosOperativos(), p.gastosOperativos(), e);
        filaResumen(resumen, true, "Utilidad operativa", e.utilidadOperativa(), p.utilidadOperativa(), e);
        filaResumen(resumen, false, "(+) Ingresos financieros", e.ingresosFinancieros(), p.ingresosFinancieros(), e);
        filaResumen(resumen, false, "(-) Gastos financieros", e.gastosFinancieros(), p.gastosFinancieros(), e);
        filaResumen(resumen, false, "(+) Otros ingresos", e.otrosIngresos(), p.otrosIngresos(), e);
        filaResumen(resumen, true, "Utilidad antes de impuestos", e.utilidadAntesImpuestos(), p.utilidadAntesImpuestos(), e);
        filaResumen(resumen, false, "(-) Impuesto a la renta", e.impuestoRenta(), p.impuestoRenta(), e);
        filaResumen(resumen, true, "Utilidad neta", e.utilidadNeta(), p.utilidadNeta(), e);
        int finResumen = resumen.filaActual();
        PuntoEquilibrio pe = a.equilibrio();
        resumen.espacio()
                .fila("Margen de contribución", pe.margenContribucion())
                .fila("Costos fijos", pe.costosFijos())
                .fila("Ventas de equilibrio", pe.ventasEquilibrio() == null ? "No alcanzable" : pe.ventasEquilibrio())
                .fila("Margen de seguridad", new Porcentaje(pe.margenSeguridad()));
        resumen.terminar(finResumen);

        LibroExcel.Hoja evolucion = libro.hoja("Evolución mensual")
                .titulo(empresa + " · Evolución mensual", "Últimos " + a.evolucion().size() + " meses")
                .encabezados("Mes", "Ventas netas", "Costo de ventas", "Utilidad bruta", "Gastos operativos",
                        "Utilidad neta", "Margen neto");
        for (ResultadoMensual m : a.evolucion()) {
            EstadoResultados em = m.estado();
            evolucion.fila(Fechas.nombreMes(m.mes().getMonthValue()) + " " + m.mes().getYear(), em.ventasNetas(),
                    em.costoVentas(), em.utilidadBruta(), em.gastosOperativos(), em.utilidadNeta(),
                    new Porcentaje(em.margenNeto()));
        }
        evolucion.terminar(evolucion.filaActual());

        LibroExcel.Hoja productos = libro.hoja("Productos")
                .titulo(empresa + " · Rentabilidad por producto", periodo)
                .encabezados("Código", "Producto o servicio", "Tipo", "Categoría", "Cantidad", "Ventas", "Costo",
                        "Margen", "Margen %");
        for (RentabilidadProducto r : a.productos()) {
            productos.fila(r.codigo(), r.nombre(), r.tipo().descripcion(), r.categoria(), new Cantidad(r.cantidad()),
                    r.ventas(), r.costo(), r.margen(), new Porcentaje(r.margenSobreVentas()));
        }
        int finProductos = productos.filaActual();
        productos.total("Total", null, null, null, null, productos.suma(5, null, null), productos.suma(6, null, null),
                productos.suma(7, null, null));
        productos.terminar(finProductos);

        hojaOperaciones(libro, "Ventas", "Cliente", datos.ventas(), true);
        hojaOperaciones(libro, "Compras", "Proveedor", datos.compras(), false);

        LibroExcel.Hoja movimientos = libro.hoja("Ingresos y gastos")
                .titulo(empresa + " · Ingresos y gastos", periodo + ". Los anulados se listan pero no se suman.")
                .encabezados("Fecha", "Tipo", "Categoría", "Grupo", "Comportamiento", "Descripción", "Comprobante",
                        "Monto", "Estado");
        for (MovimientoFinanciero m : datos.movimientos()) {
            movimientos.fila(m.fecha(), m.tipo().descripcion(), m.categoria().descripcion(),
                    m.categoria().grupo().descripcion(), m.comportamiento() == null ? null : m.comportamiento().descripcion(),
                    m.descripcion(), m.comprobante(), m.monto(), m.anulado() ? "Anulado" : "Vigente");
        }
        int finMovimientos = movimientos.filaActual();
        LibroExcel.Formula ingresos = movimientos.sumaSi(7, 1, "Ingreso", 8, "Vigente");
        LibroExcel.Formula gastos = movimientos.sumaSi(7, 1, "Gasto", 8, "Vigente");
        movimientos.total("Total de ingresos", null, null, null, null, null, null, ingresos);
        movimientos.total("Total de gastos", null, null, null, null, null, null, gastos);
        movimientos.terminar(finMovimientos);
        return libro;
    }

    private void hojaOperaciones(LibroExcel libro, String nombre, String contraparte, List<Operacion> operaciones,
                                 boolean conMargen) {
        String empresa = datos.empresa().nombreVisible();
        LibroExcel.Hoja hoja = libro.hoja(nombre)
                .titulo(empresa + " · " + nombre, a.periodo().descripcion() + ". Montos sin impuesto en «Subtotal»; "
                        + "las anuladas se listan pero no se suman.");
        if (conMargen) {
            hoja.encabezados("Número", "Fecha", contraparte, "Documento", "Comprobante", "Ítems", "Subtotal",
                    "Impuesto", "Total", "Costo", "Margen", "Estado");
        } else {
            hoja.encabezados("Número", "Fecha", contraparte, "Documento", "Comprobante", "Ítems", "Subtotal",
                    "Impuesto", "Total", "Estado");
        }
        for (Operacion o : operaciones) {
            String estado = o.estado() == EstadoOperacion.ANULADA ? "Anulada" : "Registrada";
            if (conMargen) {
                hoja.fila(o.numero(), o.fecha(), o.contraparte(), o.documentoContraparte(), o.comprobante(),
                        o.cantidadItems(), o.subtotal(), o.impuesto(), o.total(), o.costoTotal(),
                        o.subtotal().subtract(o.costoTotal()), estado);
            } else {
                hoja.fila(o.numero(), o.fecha(), o.contraparte(), o.documentoContraparte(), o.comprobante(),
                        o.cantidadItems(), o.subtotal(), o.impuesto(), o.total(), estado);
            }
        }
        int fin = hoja.filaActual();
        int columnaEstado = conMargen ? 11 : 9;
        if (conMargen) {
            hoja.total("Total (registradas)", null, null, null, null, null, hoja.suma(6, columnaEstado, "Registrada"),
                    hoja.suma(7, columnaEstado, "Registrada"), hoja.suma(8, columnaEstado, "Registrada"),
                    hoja.suma(9, columnaEstado, "Registrada"), hoja.suma(10, columnaEstado, "Registrada"));
        } else {
            hoja.total("Total (registradas)", null, null, null, null, null, hoja.suma(6, columnaEstado, "Registrada"),
                    hoja.suma(7, columnaEstado, "Registrada"), hoja.suma(8, columnaEstado, "Registrada"));
        }
        hoja.terminar(fin);
    }

    private void filaResumen(LibroExcel.Hoja hoja, boolean total, String concepto, BigDecimal actual,
                             BigDecimal anterior, EstadoResultados e) {
        Object[] valores = {concepto, actual, anterior, new Porcentaje(new Variacion(actual, anterior).relativa()),
                new Porcentaje(e.sobreVentas(actual))};
        if (total) {
            hoja.total(valores);
        } else {
            hoja.fila(valores);
        }
    }

    // ------------------------------------------------------------------ utilidades

    private String dinero(BigDecimal valor) {
        return valor == null ? "—" : Montos.formatear(valor.setScale(2, RoundingMode.HALF_UP), moneda);
    }

    private static String variacion(BigDecimal actual, BigDecimal anterior) {
        BigDecimal relativa = new Variacion(actual, anterior).relativa();
        if (relativa == null) {
            return "—";
        }
        return (relativa.signum() > 0 ? "+" : "") + Montos.porcentaje(relativa);
    }

    private static <T> BigDecimal suma(List<T> elementos, Function<T, BigDecimal> valor) {
        return elementos.stream().map(valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
