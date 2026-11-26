package com.businessanalyzer.analytics.sugerencias;

import com.businessanalyzer.analytics.MargenesFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM.VentaFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM.DatosLineaFGEM;
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

public final class SugerenciasProductoFGEM {

    public static final BigDecimal MARGEN_OBJETIVO_PREDETERMINADO_FGEM = new BigDecimal("0.20");
    public static final int DIAS_ANALISIS_FGEM = 60;
    public static final int DIAS_HISTORIAL_FGEM = 180;
    public static final int DIAS_MINIMOS_FGEM = 14;
    public static final int DIAS_COBERTURA_FGEM = 14;
    public static final int DIAS_URGENTE_FGEM = 7;
    public static final int DIAS_COBERTURA_PERECIBLE_FGEM = 3;
    public static final int DIAS_VIDA_PERECIBLE_FGEM = 15;

    private static final BigDecimal REDONDEO_PRECIO_FGEM = new BigDecimal("0.10");
    private static final BigDecimal DIAS_MES_FGEM = BigDecimal.valueOf(30);
    private static final int ESCALA_FGEM = 6;

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

    public record PrecioFGEM(ProductoFGEM productoFGEM, MotivoPrecioFGEM motivoFGEM, BigDecimal margenActualFGEM, BigDecimal precioSugeridoFGEM,
                             BigDecimal margenSugeridoFGEM, BigDecimal unidadesMesFGEM, BigDecimal gananciaExtraMesFGEM, NotaPrecioFGEM notaFGEM) {
    }

    public record ReposicionFGEM(ProductoFGEM productoFGEM, UrgenciaFGEM urgenciaFGEM, BigDecimal stockFGEM, BigDecimal vendidoFGEM, int diasBaseFGEM,
                                 BigDecimal ventaDiariaFGEM, BigDecimal diasCoberturaFGEM, BigDecimal cantidadFGEM, BigDecimal costoFGEM,
                                 boolean segunHistorialFGEM) {
    }

    public record SinMovimientoFGEM(ProductoFGEM productoFGEM, SituacionFGEM situacionFGEM, BigDecimal stockFGEM, BigDecimal valorFGEM,
                                    BigDecimal diasCoberturaFGEM) {
    }

    public record ResultadoFGEM(int diasAnalizadosFGEM, BigDecimal margenObjetivoFGEM, List<PrecioFGEM> preciosFGEM,
                                List<ReposicionFGEM> reposicionFGEM, List<SinMovimientoFGEM> sinMovimientoFGEM, BigDecimal valorInventarioFGEM,
                                BigDecimal utilidadMensualFGEM) {

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

        public BigDecimal porcentajeInmovilizadoFGEM() {
            return valorInventarioFGEM.signum() <= 0 ? null
                    : valorInmovilizadoFGEM().divide(valorInventarioFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
        }

        public boolean pocosDatosFGEM() {
            return diasAnalizadosFGEM < DIAS_MINIMOS_FGEM;
        }

        public ResultadoFGEM conUtilidadMensualFGEM(BigDecimal utilidadFGEM) {
            return new ResultadoFGEM(diasAnalizadosFGEM, margenObjetivoFGEM, preciosFGEM, reposicionFGEM, sinMovimientoFGEM, valorInventarioFGEM,
                    utilidadFGEM);
        }
    }

    public record PeriodoVentasFGEM(List<VentaFGEM> ventasFGEM, int diasFGEM, LocalDate desdeFGEM, Map<Long, LocalDate> ultimaVentaFGEM) {

        public PeriodoVentasFGEM(List<VentaFGEM> ventasFGEM, int diasFGEM) {
            this(ventasFGEM, diasFGEM, null, Map.of());
        }

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

    public static List<DatosLineaFGEM> lineasCompraFGEM(List<ReposicionFGEM> reposicionFGEM) {
        return reposicionFGEM.stream()
                .filter(rFGEM -> rFGEM.cantidadFGEM().signum() > 0)
                .sorted(Comparator.comparing((ReposicionFGEM rFGEM) -> rFGEM.urgenciaFGEM().ordinal())
                        .thenComparing(rFGEM -> rFGEM.productoFGEM().nombreFGEM()))
                .map(rFGEM -> new DatosLineaFGEM(rFGEM.productoFGEM().idFGEM(), rFGEM.cantidadFGEM(), rFGEM.productoFGEM().costoUnitarioFGEM()))
                .toList();
    }

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
            return Optional.empty();
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

    static BigDecimal precioParaMargenFGEM(BigDecimal costoFGEM, BigDecimal objetivoFGEM) {
        BigDecimal exactoFGEM = costoFGEM.divide(BigDecimal.ONE.subtract(objetivoFGEM), ESCALA_FGEM, RoundingMode.HALF_UP);
        return exactoFGEM.divide(REDONDEO_PRECIO_FGEM, 0, RoundingMode.CEILING).multiply(REDONDEO_PRECIO_FGEM).setScale(2, RoundingMode.UNNECESSARY);
    }

    private static Optional<ReposicionFGEM> reposicionFGEM(ProductoFGEM pFGEM, BigDecimal stockFGEM, BigDecimal vendidoRecienteFGEM,
                                                          int diasRecientesFGEM, BigDecimal vendidoLargoFGEM, int diasConStockFGEM) {
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
            BigDecimal coberturaFGEM = ritmoRecienteFGEM.signum() == 0 ? null : stockFGEM.divide(ritmoRecienteFGEM, 0, RoundingMode.HALF_UP);
            return coberturaFGEM == null || coberturaFGEM.compareTo(BigDecimal.valueOf(DIAS_VIDA_PERECIBLE_FGEM)) > 0
                    ? Optional.of(new SinMovimientoFGEM(pFGEM, SituacionFGEM.PERECIBLE_EN_RIESGO, stockFGEM, valorFGEM, coberturaFGEM))
                    : Optional.empty();
        }
        if (ritmoLargoFGEM.signum() == 0) {
            return Optional.of(new SinMovimientoFGEM(pFGEM, SituacionFGEM.SIN_VENTAS, stockFGEM, valorFGEM, null));
        }
        BigDecimal coberturaFGEM = stockFGEM.divide(ritmoLargoFGEM, 0, RoundingMode.HALF_UP);
        return coberturaFGEM.compareTo(BigDecimal.valueOf(diasHistorialFGEM)) > 0
                ? Optional.of(new SinMovimientoFGEM(pFGEM, SituacionFGEM.VENTA_LENTA, stockFGEM, valorFGEM, coberturaFGEM))
                : Optional.empty();
    }
}
