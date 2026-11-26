package com.businessanalyzer.informes;

import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.ReposicionFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.UrgenciaFGEM;
import com.businessanalyzer.informes.LibroExcelFGEM.CantidadFGEM;
import com.businessanalyzer.informes.LibroExcelFGEM.FormulaFGEM;
import com.businessanalyzer.informes.LibroExcelFGEM.HojaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class ListaComprasFGEM {

    public static final String SIN_CATEGORIA_FGEM = "Sin categoría";

    private static final Collator COLLATOR_FGEM = Collator.getInstance(Locale.forLanguageTag("es-PE"));

    private ListaComprasFGEM() {
    }

    public record GrupoFGEM(String categoriaFGEM, List<ReposicionFGEM> itemsFGEM) {

        public BigDecimal subtotalFGEM() {
            return itemsFGEM.stream().map(ReposicionFGEM::costoFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    public static List<GrupoFGEM> agruparFGEM(List<ReposicionFGEM> reposicionFGEM) {
        Map<String, List<ReposicionFGEM>> porCategoriaFGEM = new TreeMap<>((aFGEM, bFGEM) -> {
            if (aFGEM.equals(bFGEM)) {
                return 0;
            }
            if (aFGEM.equals(SIN_CATEGORIA_FGEM) || bFGEM.equals(SIN_CATEGORIA_FGEM)) {
                return aFGEM.equals(SIN_CATEGORIA_FGEM) ? 1 : -1;
            }
            int cFGEM = COLLATOR_FGEM.compare(aFGEM, bFGEM);
            return cFGEM != 0 ? cFGEM : aFGEM.compareTo(bFGEM);
        });
        for (ReposicionFGEM rFGEM : reposicionFGEM) {
            porCategoriaFGEM.computeIfAbsent(categoriaFGEM(rFGEM.productoFGEM()), kFGEM -> new ArrayList<>()).add(rFGEM);
        }
        Comparator<ReposicionFGEM> ordenFGEM = Comparator.comparing((ReposicionFGEM rFGEM) -> rFGEM.urgenciaFGEM().ordinal())
                .thenComparing(rFGEM -> rFGEM.productoFGEM().nombreFGEM(), COLLATOR_FGEM);
        Map<String, List<ReposicionFGEM>> ordenadoFGEM = new LinkedHashMap<>();
        porCategoriaFGEM.forEach((kFGEM, vFGEM) -> ordenadoFGEM.put(kFGEM, vFGEM.stream().sorted(ordenFGEM).toList()));
        return ordenadoFGEM.entrySet().stream().map(eFGEM -> new GrupoFGEM(eFGEM.getKey(), eFGEM.getValue())).toList();
    }

    public static ContenidoInformeFGEM contenidoFGEM(List<ReposicionFGEM> reposicionFGEM, MonedaFGEM monedaFGEM, String periodoFGEM) {
        ContenidoInformeFGEM cFGEM = new ContenidoInformeFGEM();
        BigDecimal totalFGEM = reposicionFGEM.stream().map(ReposicionFGEM::costoFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
        long agotadosFGEM = reposicionFGEM.stream().filter(rFGEM -> rFGEM.urgenciaFGEM() == UrgenciaFGEM.AGOTADO).count();
        long urgentesFGEM = reposicionFGEM.stream().filter(rFGEM -> rFGEM.urgenciaFGEM() == UrgenciaFGEM.URGENTE).count();
        cFGEM.indicadoresFGEM("Productos", String.valueOf(reposicionFGEM.size()),
                "Agotados", String.valueOf(agotadosFGEM),
                "Urgentes", String.valueOf(urgentesFGEM),
                "Costo aproximado", MontosFGEM.formatearFGEM(totalFGEM, monedaFGEM));
        cFGEM.notaFGEM("Cantidades para cubrir 14 días de venta (3 días los perecibles) " + periodoFGEM
                + ". Urgente: se agota esta semana (un perecible, mañana). El costo usa el costo unitario registrado de cada "
                + "producto; confirma los precios con tu proveedor.");
        if (reposicionFGEM.isEmpty()) {
            cFGEM.textoFGEM("No hay nada que comprar: hay stock para al menos dos semanas de todos los productos que se venden.");
            return cFGEM;
        }
        for (GrupoFGEM gFGEM : agruparFGEM(reposicionFGEM)) {
            cFGEM.seccionFGEM(gFGEM.categoriaFGEM());
            cFGEM.encabezadoFGEM("Producto", "Estado", "Stock", "Comprar", "Costo aprox.");
            for (ReposicionFGEM rFGEM : gFGEM.itemsFGEM()) {
                ProductoFGEM pFGEM = rFGEM.productoFGEM();
                cFGEM.filaFGEM(nombreFGEM(pFGEM), estadoFGEM(rFGEM.urgenciaFGEM()), MontosFGEM.cantidadFGEM(rFGEM.stockFGEM()),
                        MontosFGEM.cantidadFGEM(rFGEM.cantidadFGEM()) + " " + pFGEM.unidadFGEM().abreviaturaFGEM(),
                        MontosFGEM.formatearFGEM(rFGEM.costoFGEM(), monedaFGEM));
            }
            cFGEM.totalFGEM("Subtotal " + gFGEM.categoriaFGEM(), "", "", "", MontosFGEM.formatearFGEM(gFGEM.subtotalFGEM(), monedaFGEM));
        }
        cFGEM.espacioFGEM();
        cFGEM.totalFGEM("Total de la compra", "", "", "", MontosFGEM.formatearFGEM(totalFGEM, monedaFGEM));
        return cFGEM;
    }

    public static LibroExcelFGEM libroFGEM(List<ReposicionFGEM> reposicionFGEM, MonedaFGEM monedaFGEM, String tituloFGEM, String detalleFGEM) {
        LibroExcelFGEM libroFGEM = new LibroExcelFGEM(monedaFGEM);
        HojaFGEM hFGEM = libroFGEM.hojaFGEM("Lista de compras");
        hFGEM.tituloFGEM(tituloFGEM, detalleFGEM);
        hFGEM.encabezadosFGEM("Categoría", "Producto", "Código", "Estado", "Stock", "Unidad", "Comprar", "Costo unitario", "Costo aprox.", "Comprado");
        for (GrupoFGEM gFGEM : agruparFGEM(reposicionFGEM)) {
            for (ReposicionFGEM rFGEM : gFGEM.itemsFGEM()) {
                ProductoFGEM pFGEM = rFGEM.productoFGEM();
                int filaExcelFGEM = hFGEM.filaActualFGEM() + 1;
                hFGEM.filaFGEM(gFGEM.categoriaFGEM(), nombreFGEM(pFGEM), pFGEM.codigoFGEM() == null ? "" : pFGEM.codigoFGEM(),
                        estadoFGEM(rFGEM.urgenciaFGEM()), new CantidadFGEM(rFGEM.stockFGEM()), pFGEM.unidadFGEM().abreviaturaFGEM(),
                        new CantidadFGEM(rFGEM.cantidadFGEM()), pFGEM.costoUnitarioFGEM(),
                        new FormulaFGEM("ROUND(G" + filaExcelFGEM + "*H" + filaExcelFGEM + ",2)"), "");
            }
        }
        int finDatosFGEM = hFGEM.filaActualFGEM();
        hFGEM.totalFGEM("Total", reposicionFGEM.size() + (reposicionFGEM.size() == 1 ? " producto" : " productos"), null, null, null, null, null,
                null, hFGEM.sumaFGEM(8, null, null), null);
        hFGEM.terminarFGEM(finDatosFGEM);
        return libroFGEM;
    }

    static String estadoFGEM(UrgenciaFGEM urgenciaFGEM) {
        return switch (urgenciaFGEM) {
            case AGOTADO -> "Agotado";
            case URGENTE -> "Urgente";
            case PRONTO -> "Pronto";
        };
    }

    private static String categoriaFGEM(ProductoFGEM pFGEM) {
        String nombreFGEM = pFGEM.categoriaNombreFGEM();
        return nombreFGEM == null || nombreFGEM.isBlank() ? SIN_CATEGORIA_FGEM : nombreFGEM.strip();
    }

    private static String nombreFGEM(ProductoFGEM pFGEM) {
        return pFGEM.nombreFGEM() + (pFGEM.perecibleFGEM() ? " (perecible)" : "");
    }
}
