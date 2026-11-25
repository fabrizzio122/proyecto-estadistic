package com.businessanalyzer.analytics.sugerencias;

import com.businessanalyzer.analytics.MargenesFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM.VentaFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Sugerencias concretas para mejorar el negocio, producto por producto, con los datos que ya tiene la
 * aplicación (catálogo, stock y ventas):
 * <ul>
 *     <li><b>Precios</b>: productos que se venden con pérdida o con un margen menor al objetivo, con el precio
 *         sugerido (redondeado a 10 céntimos) y cuánto más se ganaría al mes si se vende la misma cantidad. Los
 *         que no se venden o se venden poco se marcan: subirles el precio no es la prioridad.</li>
 *     <li><b>Reposición</b>: productos que se agotan pronto según lo que se vende por día, y cuánto comprar
 *         para cubrir {@value #DIAS_COBERTURA_FGEM} días ({@value #DIAS_COBERTURA_PERECIBLE_FGEM} si es perecible).
 *         Un producto agotado que no se vendió en el período reciente usa su historial largo: no se vendió
 *         porque no había.</li>
 *     <li><b>Sin movimiento</b>: perecibles que se vencerán antes de venderse, productos sin ventas en
 *         {@value #DIAS_HISTORIAL_FGEM} días y productos que tardarían más de ese tiempo en venderse.</li>
 * </ul>
 * Es cálculo puro (sin base de datos ni interfaz).
 */
public final class SugerenciasProductoFGEM {

    /** Margen bruto sobre el precio que se busca si el usuario no indica otro. */
    public static final BigDecimal MARGEN_OBJETIVO_PREDETERMINADO_FGEM = new BigDecimal("0.20");
    /** Días de ventas recientes: el ritmo actual (precios y reposición). */
    public static final int DIAS_ANALISIS_FGEM = 60;
    /** Días de historial para la venta lenta y los productos agotados sin ventas recientes. */
    public static final int DIAS_HISTORIAL_FGEM = 180;
    /** Con menos días de ventas las cantidades sugeridas son poco confiables. */
    public static final int DIAS_MINIMOS_FGEM = 14;
    /** La reposición sugerida alcanza para este número de días. */
    public static final int DIAS_COBERTURA_FGEM = 14;
    /** Por debajo de esta cobertura, el producto se agota esta semana. */
    public static final int DIAS_URGENTE_FGEM = 7;
    /** Un perecible se repone para pocos días. */
    public static final int DIAS_COBERTURA_PERECIBLE_FGEM = 3;
    /** Un perecible que tardaría más que esto en venderse probablemente se vencerá antes. */
    public static final int DIAS_VIDA_PERECIBLE_FGEM = 15;

    private static final BigDecimal REDONDEO_PRECIO_FGEM = new BigDecimal("0.10");
    private static final BigDecimal DIAS_MES_FGEM = BigDecimal.valueOf(30);
    private static final int ESCALA_FGEM = 6;

    /** Por qué se sugiere cambiar el precio. */
    public enum MotivoPrecioFGEM {
        PERDIDA("Se vende con pérdida"),
        MARGEN_BAJO("Margen bajo");

        private final String descripcionFGEM;

        MotivoPrecioFGEM(String descripcionFGEM) {
            this.descripcionFGEM = descripcionFGEM;
        }

        public String descripcionFGEM() {
            return descripcionFGEM;
        }
    }

    /** Aviso sobre una sugerencia de precio que no es prioritaria. */
    public enum NotaPrecioFGEM {
        SIN_VENTAS("Sin ventas recientes: revisa por qué no se vende."),
        VENTA_LENTA("Tiene stock para más de 6 meses: primero véndelo; subirle el precio puede frenarlo más.");

        private final String descripcionFGEM;

        NotaPrecioFGEM(String descripcionFGEM) {
            this.descripcionFGEM = descripcionFGEM;
        }

        public String descripcionFGEM() {
            return descripcionFGEM;
        }
    }

    /** Qué tan pronto hay que reponer. */
    public enum UrgenciaFGEM {
        AGOTADO("Agotado"),
        URGENTE("Se agota esta semana"),
        PRONTO("Reponer pronto");

        private final String descripcionFGEM;

        UrgenciaFGEM(String descripcionFGEM) {
            this.descripcionFGEM = descripcionFGEM;
        }

        public String descripcionFGEM() {
            return descripcionFGEM;
        }
    }

    /** Por qué un producto tiene dinero parado, de lo más urgente a lo menos. */
    public enum SituacionFGEM {
        PERECIBLE_EN_RIESGO("Perecible: se vencerá antes de venderse", "Ofrécelo ya en promoción y compra menos la próxima vez."),
        SIN_VENTAS("Sin ventas", "No vuelvas a comprarlo; ofrécelo en promoción o en combo."),
        VENTA_LENTA("Venta lenta", "Compra menos cantidad y con menos frecuencia.");

        private final String descripcionFGEM;
        private final String sugerenciaFGEM;

        SituacionFGEM(String descripcionFGEM, String sugerenciaFGEM) {
            this.descripcionFGEM = descripcionFGEM;
            this.sugerenciaFGEM = sugerenciaFGEM;
        }

        public String descripcionFGEM() {
            return descripcionFGEM;
        }

        public String sugerenciaFGEM() {
            return sugerenciaFGEM;
        }
    }

    /**
     * @param margenActualFGEM     (precio − costo) ÷ precio; {@code null} si el precio es cero
     * @param unidadesMesFGEM      unidades vendidas por mes en el período reciente
     * @param gananciaExtraMesFGEM (precio sugerido − precio actual) × unidades por mes, si se vende la misma cantidad
     * @param notaFGEM             {@code null} si la sugerencia es prioritaria
     */
    public record PrecioFGEM(ProductoFGEM productoFGEM, MotivoPrecioFGEM motivoFGEM, BigDecimal margenActualFGEM, BigDecimal precioSugeridoFGEM,
                             BigDecimal margenSugeridoFGEM, BigDecimal unidadesMesFGEM, BigDecimal gananciaExtraMesFGEM, NotaPrecioFGEM notaFGEM) {
    }

    /**
     * @param vendidoFGEM          unidades vendidas en los {@code diasBaseFGEM} días usados para el ritmo
     * @param diasBaseFGEM         días del período usado: los recientes o, si estaba agotado, los del historial hasta su última venta
     * @param segunHistorialFGEM   se usó el historial largo porque no hubo ventas recientes (estaba agotado)
     * @param diasCoberturaFGEM    días que alcanza el stock al ritmo actual; cero si está agotado
     * @param cantidadFGEM         cantidad sugerida a comprar
     * @param costoFGEM            cantidad × costo unitario
     */
    public record ReposicionFGEM(ProductoFGEM productoFGEM, UrgenciaFGEM urgenciaFGEM, BigDecimal stockFGEM, BigDecimal vendidoFGEM, int diasBaseFGEM,
                                 BigDecimal ventaDiariaFGEM, BigDecimal diasCoberturaFGEM, BigDecimal cantidadFGEM, BigDecimal costoFGEM,
                                 boolean segunHistorialFGEM) {
    }

    /**
     * @param diasCoberturaFGEM días que tardaría en venderse el stock; {@code null} si no se vendió en el historial
     */
    public record SinMovimientoFGEM(ProductoFGEM productoFGEM, SituacionFGEM situacionFGEM, BigDecimal stockFGEM, BigDecimal valorFGEM,
                                    BigDecimal diasCoberturaFGEM) {
    }

    /**
     * @param diasAnalizadosFGEM  días de ventas recientes usados (los del período o desde la primera venta)
     * @param valorInventarioFGEM stock × costo de todos los productos con stock
     * @param utilidadMensualFGEM utilidad neta mensual del negocio (la del diagnóstico); {@code null} si no se conoce
     */
    public record ResultadoFGEM(int diasAnalizadosFGEM, BigDecimal margenObjetivoFGEM, List<PrecioFGEM> preciosFGEM,
                                List<ReposicionFGEM> reposicionFGEM, List<SinMovimientoFGEM> sinMovimientoFGEM, BigDecimal valorInventarioFGEM,
                                BigDecimal utilidadMensualFGEM) {

        /** Solo las sugerencias prioritarias (sin nota). */
        public BigDecimal gananciaExtraMesFGEM() {
            return preciosFGEM.stream().filter(pFGEM -> pFGEM.notaFGEM() == null).map(PrecioFGEM::gananciaExtraMesFGEM)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public long preciosPrioritariosFGEM() {
            return preciosFGEM.stream().filter(pFGEM -> pFGEM.notaFGEM() == null).count();
        }

        public BigDecimal costoReposicionFGEM() {
            return reposicionFGEM.stream().map(ReposicionFGEM::costoFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public BigDecimal valorInmovilizadoFGEM() {
            return sinMovimientoFGEM.stream().map(SinMovimientoFGEM::valorFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public BigDecimal valorFGEM(SituacionFGEM situacionFGEM) {
            return sinMovimientoFGEM.stream().filter(sFGEM -> sFGEM.situacionFGEM() == situacionFGEM).map(SinMovimientoFGEM::valorFGEM)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public long cantidadFGEM(SituacionFGEM situacionFGEM) {
            return sinMovimientoFGEM.stream().filter(sFGEM -> sFGEM.situacionFGEM() == situacionFGEM).count();
        }

        /** Dinero inmovilizado ÷ valor del inventario; {@code null} sin inventario. */
        public BigDecimal porcentajeInmovilizadoFGEM() {
            return valorInventarioFGEM.signum() <= 0 ? null
                    : valorInmovilizadoFGEM().divide(valorInventarioFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
        }

        /** Pocos días de ventas: las cantidades son orientativas. */
        public boolean pocosDatosFGEM() {
            return diasAnalizadosFGEM < DIAS_MINIMOS_FGEM;
        }

        public ResultadoFGEM conUtilidadMensualFGEM(BigDecimal utilidadFGEM) {
            return new ResultadoFGEM(diasAnalizadosFGEM, margenObjetivoFGEM, preciosFGEM, reposicionFGEM, sinMovimientoFGEM, valorInventarioFGEM,
                    utilidadFGEM);
        }
    }

    /**
     * Ventas de un período.
     *
     * @param diasFGEM          días que cubre el período (al menos 1)
     * @param desdeFGEM         primer día del período; {@code null} si no se conoce
     * @param ultimaVentaFGEM   fecha de la última venta de cada producto en el período
     */
    public record PeriodoVentasFGEM(List<VentaFGEM> ventasFGEM, int diasFGEM, LocalDate desdeFGEM, Map<Long, LocalDate> ultimaVentaFGEM) {

        public PeriodoVentasFGEM(List<VentaFGEM> ventasFGEM, int diasFGEM) {
            this(ventasFGEM, diasFGEM, null, Map.of());
        }

        /**
         * Días en que el producto pudo venderse: desde el inicio del período hasta su última venta. Si se agotó
         * a mitad del período, los días sin stock no cuentan (si no, el ritmo saldría más bajo que el real).
         */
        int diasHastaUltimaVentaFGEM(long productoIdFGEM) {
            LocalDate ultimaFGEM = ultimaVentaFGEM.get(productoIdFGEM);
            if (desdeFGEM == null || ultimaFGEM == null) {
                return diasFGEM;
            }
            long hastaUltimaFGEM = java.time.temporal.ChronoUnit.DAYS.between(desdeFGEM, ultimaFGEM) + 1;
            return (int) Math.max(1, Math.min(diasFGEM, hastaUltimaFGEM));
        }
    }

    private SugerenciasProductoFGEM() {
    }

    /**
     * @param productosFGEM      productos y servicios activos, con su stock
     * @param recientesFGEM      ventas de los últimos {@value #DIAS_ANALISIS_FGEM} días (el ritmo actual)
     * @param historialFGEM      ventas de los últimos {@value #DIAS_HISTORIAL_FGEM} días (venta lenta y agotados)
     * @param margenObjetivoFGEM margen bruto buscado sobre el precio (0.20 = 20 %)
     */
    public static ResultadoFGEM calcularFGEM(List<ProductoFGEM> productosFGEM, PeriodoVentasFGEM recientesFGEM, PeriodoVentasFGEM historialFGEM,
                                            BigDecimal margenObjetivoFGEM) {
        int diasRecientesFGEM = Math.max(1, recientesFGEM.diasFGEM());
        int diasHistorialFGEM = Math.max(diasRecientesFGEM, historialFGEM.diasFGEM());
        Map<Long, BigDecimal> recienteFGEM = vendidoFGEM(recientesFGEM.ventasFGEM());
        Map<Long, BigDecimal> largoFGEM = vendidoFGEM(historialFGEM.ventasFGEM());

        List<PrecioFGEM> preciosFGEM = new ArrayList<>();
        List<ReposicionFGEM> reposicionFGEM = new ArrayList<>();
        List<SinMovimientoFGEM> sinMovimientoFGEM = new ArrayList<>();
        BigDecimal inventarioFGEM = BigDecimal.ZERO;
        for (ProductoFGEM pFGEM : productosFGEM) {
            if (!pFGEM.activoFGEM()) {
                continue;
            }
            BigDecimal vendidoRecienteFGEM = recienteFGEM.getOrDefault(pFGEM.idFGEM(), BigDecimal.ZERO);
            BigDecimal vendidoLargoFGEM = largoFGEM.getOrDefault(pFGEM.idFGEM(), vendidoRecienteFGEM).max(vendidoRecienteFGEM);
            BigDecimal ritmoRecienteFGEM = porDiaFGEM(vendidoRecienteFGEM, diasRecientesFGEM);
            BigDecimal ritmoLargoFGEM = porDiaFGEM(vendidoLargoFGEM, diasHistorialFGEM);

            Optional<SinMovimientoFGEM> paradoFGEM = Optional.empty();
            if (pFGEM.tipoFGEM() == TipoItemFGEM.PRODUCTO) {
                BigDecimal stockFGEM = pFGEM.stockFGEM() == null ? BigDecimal.ZERO : pFGEM.stockFGEM();
                if (stockFGEM.signum() > 0) {
                    inventarioFGEM = inventarioFGEM.add(stockFGEM.multiply(pFGEM.costoUnitarioFGEM()));
                }
                reposicionFGEM(pFGEM, stockFGEM, vendidoRecienteFGEM, diasRecientesFGEM, vendidoLargoFGEM,
                        historialFGEM.diasHastaUltimaVentaFGEM(pFGEM.idFGEM())).ifPresent(reposicionFGEM::add);
                paradoFGEM = sinMovimientoFGEM(pFGEM, stockFGEM, ritmoRecienteFGEM, ritmoLargoFGEM, diasHistorialFGEM);
                paradoFGEM.ifPresent(sinMovimientoFGEM::add);
            }
            NotaPrecioFGEM notaFGEM = vendidoRecienteFGEM.signum() == 0 ? NotaPrecioFGEM.SIN_VENTAS
                    : paradoFGEM.isPresent() ? NotaPrecioFGEM.VENTA_LENTA : null;
            precioFGEM(pFGEM, ritmoRecienteFGEM, margenObjetivoFGEM, notaFGEM).ifPresent(preciosFGEM::add);
        }

        // Primero lo prioritario y lo que más dinero suma o libera.
        preciosFGEM.sort(Comparator.comparing((PrecioFGEM pFGEM) -> pFGEM.notaFGEM() != null)
                .thenComparing(PrecioFGEM::gananciaExtraMesFGEM, Comparator.reverseOrder())
                .thenComparing(PrecioFGEM::motivoFGEM).thenComparing(pFGEM -> pFGEM.productoFGEM().nombreFGEM()));
        reposicionFGEM.sort(Comparator.comparing(ReposicionFGEM::urgenciaFGEM).thenComparing(ReposicionFGEM::diasCoberturaFGEM)
                .thenComparing(rFGEM -> rFGEM.productoFGEM().nombreFGEM()));
        sinMovimientoFGEM.sort(Comparator.comparing(SinMovimientoFGEM::situacionFGEM)
                .thenComparing(SinMovimientoFGEM::valorFGEM, Comparator.reverseOrder())
                .thenComparing(sFGEM -> sFGEM.productoFGEM().nombreFGEM()));
        return new ResultadoFGEM(diasRecientesFGEM, margenObjetivoFGEM, List.copyOf(preciosFGEM), List.copyOf(reposicionFGEM),
                List.copyOf(sinMovimientoFGEM), inventarioFGEM.setScale(2, RoundingMode.HALF_UP), null);
    }

    private static Map<Long, BigDecimal> vendidoFGEM(List<VentaFGEM> ventasFGEM) {
        Map<Long, BigDecimal> vendidoFGEM = new HashMap<>();
        for (VentaFGEM vFGEM : ventasFGEM) {
            vendidoFGEM.merge(vFGEM.productoIdFGEM(), vFGEM.cantidadFGEM(), BigDecimal::add);
        }
        return vendidoFGEM;
    }

    private static BigDecimal porDiaFGEM(BigDecimal cantidadFGEM, int diasFGEM) {
        return cantidadFGEM.divide(BigDecimal.valueOf(diasFGEM), ESCALA_FGEM, RoundingMode.HALF_UP);
    }

    private static Optional<PrecioFGEM> precioFGEM(ProductoFGEM pFGEM, BigDecimal ventaDiariaFGEM, BigDecimal objetivoFGEM, NotaPrecioFGEM notaFGEM) {
        BigDecimal costoFGEM = pFGEM.costoUnitarioFGEM();
        BigDecimal precioFGEM = pFGEM.precioVentaFGEM();
        if (costoFGEM == null || costoFGEM.signum() <= 0 || precioFGEM == null) {
            return Optional.empty();   // sin costo registrado no se puede juzgar el margen
        }
        BigDecimal margenFGEM = precioFGEM.signum() == 0 ? null
                : precioFGEM.subtract(costoFGEM).divide(precioFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
        MotivoPrecioFGEM motivoFGEM;
        if (precioFGEM.compareTo(costoFGEM) < 0) {
            motivoFGEM = MotivoPrecioFGEM.PERDIDA;
        } else if (margenFGEM == null || margenFGEM.compareTo(objetivoFGEM) < 0) {
            motivoFGEM = MotivoPrecioFGEM.MARGEN_BAJO;
        } else {
            return Optional.empty();
        }
        BigDecimal sugeridoFGEM = precioParaMargenFGEM(costoFGEM, objetivoFGEM);
        if (sugeridoFGEM.compareTo(precioFGEM) <= 0) {
            return Optional.empty();
        }
        BigDecimal margenSugeridoFGEM = sugeridoFGEM.subtract(costoFGEM).divide(sugeridoFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
        BigDecimal unidadesMesFGEM = ventaDiariaFGEM.multiply(DIAS_MES_FGEM);
        BigDecimal extraFGEM = sugeridoFGEM.subtract(precioFGEM).multiply(unidadesMesFGEM).setScale(2, RoundingMode.HALF_UP);
        return Optional.of(new PrecioFGEM(pFGEM, motivoFGEM, margenFGEM, sugeridoFGEM, margenSugeridoFGEM, unidadesMesFGEM, extraFGEM, notaFGEM));
    }

    /** Precio que deja el margen buscado, redondeado hacia arriba a 10 céntimos (como se cobra en una tienda). */
    static BigDecimal precioParaMargenFGEM(BigDecimal costoFGEM, BigDecimal objetivoFGEM) {
        BigDecimal exactoFGEM = costoFGEM.divide(BigDecimal.ONE.subtract(objetivoFGEM), ESCALA_FGEM, RoundingMode.HALF_UP);
        return exactoFGEM.divide(REDONDEO_PRECIO_FGEM, 0, RoundingMode.CEILING).multiply(REDONDEO_PRECIO_FGEM).setScale(2, RoundingMode.UNNECESSARY);
    }

    private static Optional<ReposicionFGEM> reposicionFGEM(ProductoFGEM pFGEM, BigDecimal stockFGEM, BigDecimal vendidoRecienteFGEM,
                                                          int diasRecientesFGEM, BigDecimal vendidoLargoFGEM, int diasConStockFGEM) {
        // Agotado y sin ventas recientes: no se vendió porque no había. Se usa el ritmo del historial largo,
        // contando solo los días hasta su última venta (los días agotado no cuentan).
        boolean usarHistorialFGEM = vendidoRecienteFGEM.signum() == 0 && stockFGEM.signum() <= 0 && vendidoLargoFGEM.signum() > 0;
        BigDecimal vendidoFGEM = usarHistorialFGEM ? vendidoLargoFGEM : vendidoRecienteFGEM;
        int diasBaseFGEM = usarHistorialFGEM ? diasConStockFGEM : diasRecientesFGEM;
        BigDecimal ventaDiariaFGEM = porDiaFGEM(vendidoFGEM, diasBaseFGEM);
        if (ventaDiariaFGEM.signum() <= 0) {
            return Optional.empty();
        }
        int coberturaObjetivoFGEM = pFGEM.perecibleFGEM() ? DIAS_COBERTURA_PERECIBLE_FGEM : DIAS_COBERTURA_FGEM;
        int urgenteFGEM = pFGEM.perecibleFGEM() ? 1 : DIAS_URGENTE_FGEM;
        BigDecimal coberturaFGEM = stockFGEM.signum() <= 0 ? BigDecimal.ZERO : stockFGEM.divide(ventaDiariaFGEM, 1, RoundingMode.HALF_UP);
        UrgenciaFGEM urgenciaFGEM;
        if (stockFGEM.signum() <= 0) {
            urgenciaFGEM = UrgenciaFGEM.AGOTADO;
        } else if (coberturaFGEM.compareTo(BigDecimal.valueOf(urgenteFGEM)) < 0) {
            urgenciaFGEM = UrgenciaFGEM.URGENTE;
        } else if (coberturaFGEM.compareTo(BigDecimal.valueOf(coberturaObjetivoFGEM)) < 0) {
            urgenciaFGEM = UrgenciaFGEM.PRONTO;
        } else {
            return Optional.empty();
        }
        // Se compran unidades enteras; si el stock es negativo (faltan compras registradas) se cuenta como cero.
        BigDecimal cantidadFGEM = ventaDiariaFGEM.multiply(BigDecimal.valueOf(coberturaObjetivoFGEM)).subtract(stockFGEM.max(BigDecimal.ZERO))
                .setScale(0, RoundingMode.CEILING);
        if (cantidadFGEM.signum() <= 0) {
            return Optional.empty();
        }
        BigDecimal costoFGEM = cantidadFGEM.multiply(pFGEM.costoUnitarioFGEM()).setScale(2, RoundingMode.HALF_UP);
        return Optional.of(new ReposicionFGEM(pFGEM, urgenciaFGEM, stockFGEM, vendidoFGEM, diasBaseFGEM, ventaDiariaFGEM, coberturaFGEM,
                cantidadFGEM, costoFGEM, usarHistorialFGEM));
    }

    private static Optional<SinMovimientoFGEM> sinMovimientoFGEM(ProductoFGEM pFGEM, BigDecimal stockFGEM, BigDecimal ritmoRecienteFGEM,
                                                                BigDecimal ritmoLargoFGEM, int diasHistorialFGEM) {
        if (stockFGEM.signum() <= 0) {
            return Optional.empty();
        }
        BigDecimal valorFGEM = stockFGEM.multiply(pFGEM.costoUnitarioFGEM()).setScale(2, RoundingMode.HALF_UP);
        if (pFGEM.perecibleFGEM()) {
            // Para un perecible importa el ritmo actual: si tardará más que su vida útil, se perderá.
            BigDecimal coberturaFGEM = ritmoRecienteFGEM.signum() == 0 ? null : stockFGEM.divide(ritmoRecienteFGEM, 0, RoundingMode.HALF_UP);
            return coberturaFGEM == null || coberturaFGEM.compareTo(BigDecimal.valueOf(DIAS_VIDA_PERECIBLE_FGEM)) > 0
                    ? Optional.of(new SinMovimientoFGEM(pFGEM, SituacionFGEM.PERECIBLE_EN_RIESGO, stockFGEM, valorFGEM, coberturaFGEM))
                    : Optional.empty();
        }
        if (ritmoLargoFGEM.signum() == 0) {
            return Optional.of(new SinMovimientoFGEM(pFGEM, SituacionFGEM.SIN_VENTAS, stockFGEM, valorFGEM, null));
        }
        // Venta lenta: con el ritmo del historial largo (en una bodega muchos productos se venden pocas veces al mes).
        BigDecimal coberturaFGEM = stockFGEM.divide(ritmoLargoFGEM, 0, RoundingMode.HALF_UP);
        return coberturaFGEM.compareTo(BigDecimal.valueOf(diasHistorialFGEM)) > 0
                ? Optional.of(new SinMovimientoFGEM(pFGEM, SituacionFGEM.VENTA_LENTA, stockFGEM, valorFGEM, coberturaFGEM))
                : Optional.empty();
    }
}
