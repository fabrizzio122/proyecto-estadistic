package com.businessanalyzer.analytics.ganaderia;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;
import com.businessanalyzer.model.CategoriaGanadoFGEM;
import com.businessanalyzer.model.EspecieFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.MovimientoGanadoFGEM;
import com.businessanalyzer.model.ProduccionGanaderaFGEM;
import com.businessanalyzer.model.ProductoGanaderoFGEM;
import com.businessanalyzer.model.TipoMovimientoGanadoFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Indicadores técnicos de una especie en un período:
 * <ul>
 *   <li><b>Natalidad</b> = nacimientos ÷ hembras reproductoras al inicio.</li>
 *   <li><b>Mortalidad</b> = muertes ÷ (existencia inicial + nacimientos + compras).</li>
 *   <li><b>Mortalidad de crías</b> = muertes de crías ÷ nacimientos.</li>
 *   <li><b>Extracción</b> = (ventas + consumo familiar) ÷ existencia inicial.</li>
 *   <li><b>Crecimiento del hato</b> = (existencia final − inicial) ÷ inicial.</li>
 * </ul>
 * Un inventario inicial registrado dentro del período cuenta como existencia inicial, no como compra.
 *
 * @param hembrasPorMachoFGEM hembras reproductoras por cada macho reproductor al final; {@code null} sin machos
 * @param historiaCompletaFGEM  ya había animales registrados antes de empezar el período (sus cifras son comparables)
 */
public record IndicadoresEspecieFGEM(EspecieFGEM especieFGEM, PeriodoFGEM periodoFGEM,
                                 int existenciaInicialFGEM, int existenciaFinalFGEM, int hembrasInicioFGEM, int hembrasFinalFGEM,
                                 int machosFinalFGEM, int nacimientosFGEM, int comprasFGEM, int ventasFGEM, int muertesFGEM,
                                 int muertesCriasFGEM, int perdidasFGEM, int consumoFGEM, BigDecimal ingresoVentasFGEM,
                                 BigDecimal natalidadFGEM, BigDecimal mortalidadFGEM, BigDecimal mortalidadCriasFGEM,
                                 BigDecimal extraccionFGEM, BigDecimal crecimientoFGEM, BigDecimal hembrasPorMachoFGEM,
                                 List<ResumenProduccionFGEM> produccionFGEM, boolean historiaCompletaFGEM) {

    private static final int ESCALA_FGEM = 6;

    /** Producción de leche, fibra o lana de la especie en el período. */
    public record ResumenProduccionFGEM(ProductoGanaderoFGEM productoFGEM, BigDecimal cantidadFGEM, BigDecimal promedioFGEM,
                                    BigDecimal valorFGEM) {

        /** "6.4 l por vaca al día" o "2.3 kg por animal esquilado". */
        public String promedioTextoFGEM() {
            if (promedioFGEM == null) {
                return "—";
            }
            return MontosFGEM.cantidadFGEM(promedioFGEM.setScale(2, RoundingMode.HALF_UP)) + " " + productoFGEM.abreviaturaFGEM()
                    + (productoFGEM.porDiaFGEM() ? " por vaca al día" : " por animal esquilado");
        }
    }

    public static IndicadoresEspecieFGEM calcularFGEM(EspecieFGEM especieFGEM, PeriodoFGEM periodoFGEM, Collection<MovimientoGanadoFGEM> movimientosFGEM,
                                              Collection<ProduccionGanaderaFGEM> registrosProduccionFGEM) {
        List<MovimientoGanadoFGEM> deEspecieFGEM = movimientosFGEM.stream()
                .filter(mFGEM -> !mFGEM.anuladoFGEM() && mFGEM.especieFGEM() == especieFGEM && !mFGEM.fechaFGEM().isAfter(periodoFGEM.hastaFGEM()))
                .toList();
        List<MovimientoGanadoFGEM> antesFGEM = deEspecieFGEM.stream().filter(mFGEM -> mFGEM.fechaFGEM().isBefore(periodoFGEM.desdeFGEM())).toList();
        List<MovimientoGanadoFGEM> duranteFGEM = deEspecieFGEM.stream().filter(mFGEM -> !mFGEM.fechaFGEM().isBefore(periodoFGEM.desdeFGEM())).toList();
        List<MovimientoGanadoFGEM> inicialDuranteFGEM = duranteFGEM.stream()
                .filter(mFGEM -> mFGEM.tipoFGEM() == TipoMovimientoGanadoFGEM.INVENTARIO_INICIAL).toList();

        List<MovimientoGanadoFGEM> baseInicialFGEM = new ArrayList<>(antesFGEM);
        baseInicialFGEM.addAll(inicialDuranteFGEM);
        Map<CategoriaGanadoFGEM, Integer> inicioFGEM = HatoFGEM.existenciasFGEM(baseInicialFGEM, periodoFGEM.hastaFGEM());
        Map<CategoriaGanadoFGEM, Integer> finFGEM = HatoFGEM.existenciasFGEM(deEspecieFGEM, periodoFGEM.hastaFGEM());

        int inicialFGEM = totalFGEM(inicioFGEM);
        int finalHatoFGEM = totalFGEM(finFGEM);
        int hembrasInicioFGEM = porRolFGEM(inicioFGEM, CategoriaGanadoFGEM.RolFGEM.HEMBRA_REPRODUCTORA);
        int hembrasFinalFGEM = porRolFGEM(finFGEM, CategoriaGanadoFGEM.RolFGEM.HEMBRA_REPRODUCTORA);
        int machosFinalFGEM = porRolFGEM(finFGEM, CategoriaGanadoFGEM.RolFGEM.MACHO_REPRODUCTOR);

        int nacimientosFGEM = sumaFGEM(duranteFGEM, TipoMovimientoGanadoFGEM.NACIMIENTO);
        int comprasFGEM = sumaFGEM(duranteFGEM, TipoMovimientoGanadoFGEM.COMPRA);
        int ventasFGEM = sumaFGEM(duranteFGEM, TipoMovimientoGanadoFGEM.VENTA);
        int muertesFGEM = sumaFGEM(duranteFGEM, TipoMovimientoGanadoFGEM.MUERTE);
        int perdidasFGEM = sumaFGEM(duranteFGEM, TipoMovimientoGanadoFGEM.PERDIDA);
        int consumoFGEM = sumaFGEM(duranteFGEM, TipoMovimientoGanadoFGEM.CONSUMO);
        int muertesCriasFGEM = duranteFGEM.stream()
                .filter(mFGEM -> mFGEM.tipoFGEM() == TipoMovimientoGanadoFGEM.MUERTE && mFGEM.categoriaFGEM().criaFGEM())
                .mapToInt(MovimientoGanadoFGEM::cantidadFGEM).sum();
        BigDecimal ingresoVentasFGEM = duranteFGEM.stream()
                .filter(mFGEM -> mFGEM.tipoFGEM() == TipoMovimientoGanadoFGEM.VENTA && mFGEM.montoFGEM() != null)
                .map(MovimientoGanadoFGEM::montoFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ResumenProduccionFGEM> produccionFGEM = new ArrayList<>();
        for (ProductoGanaderoFGEM productoFGEM : ProductoGanaderoFGEM.deFGEM(especieFGEM)) {
            List<ProduccionGanaderaFGEM> registrosFGEM = registrosProduccionFGEM.stream()
                    .filter(pFGEM -> !pFGEM.anuladoFGEM() && pFGEM.especieFGEM() == especieFGEM && pFGEM.productoFGEM() == productoFGEM
                            && !pFGEM.fechaFGEM().isBefore(periodoFGEM.desdeFGEM()) && !pFGEM.fechaFGEM().isAfter(periodoFGEM.hastaFGEM()))
                    .toList();
            if (!registrosFGEM.isEmpty()) {
                produccionFGEM.add(resumirFGEM(productoFGEM, registrosFGEM));
            }
        }

        return new IndicadoresEspecieFGEM(especieFGEM, periodoFGEM, inicialFGEM, finalHatoFGEM, hembrasInicioFGEM, hembrasFinalFGEM, machosFinalFGEM,
                nacimientosFGEM, comprasFGEM, ventasFGEM, muertesFGEM, muertesCriasFGEM, perdidasFGEM, consumoFGEM, ingresoVentasFGEM,
                dividirFGEM(nacimientosFGEM, hembrasInicioFGEM),
                dividirFGEM(muertesFGEM, inicialFGEM + nacimientosFGEM + comprasFGEM),
                dividirFGEM(muertesCriasFGEM, nacimientosFGEM),
                dividirFGEM(ventasFGEM + consumoFGEM, inicialFGEM),
                inicialFGEM == 0 ? null : dividirFGEM(finalHatoFGEM - inicialFGEM, inicialFGEM),
                dividirFGEM(hembrasFinalFGEM, machosFinalFGEM),
                produccionFGEM, !HatoFGEM.existenciasFGEM(antesFGEM, periodoFGEM.desdeFGEM().minusDays(1)).isEmpty());
    }

    private static ResumenProduccionFGEM resumirFGEM(ProductoGanaderoFGEM productoFGEM, List<ProduccionGanaderaFGEM> registrosFGEM) {
        BigDecimal cantidadFGEM = registrosFGEM.stream().map(ProduccionGanaderaFGEM::cantidadFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
        // El promedio usa solo los registros que indican cuántos animales produjeron.
        BigDecimal conAnimalesFGEM = BigDecimal.ZERO;
        long baseAnimalesFGEM = 0;
        for (ProduccionGanaderaFGEM pFGEM : registrosFGEM) {
            if (pFGEM.animalesFGEM() != null) {
                conAnimalesFGEM = conAnimalesFGEM.add(pFGEM.cantidadFGEM());
                baseAnimalesFGEM += (long) pFGEM.animalesFGEM() * (productoFGEM.porDiaFGEM() ? pFGEM.diasFGEM() : 1);
            }
        }
        BigDecimal promedioFGEM = baseAnimalesFGEM == 0 ? null
                : conAnimalesFGEM.divide(BigDecimal.valueOf(baseAnimalesFGEM), ESCALA_FGEM, RoundingMode.HALF_UP);
        BigDecimal valorFGEM = registrosFGEM.stream().map(ProduccionGanaderaFGEM::valorFGEM).filter(vFGEM -> vFGEM != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResumenProduccionFGEM(productoFGEM, cantidadFGEM, promedioFGEM, valorFGEM);
    }

    /** Hay datos de esta especie en el período (existencias o movimientos). */
    public boolean tieneDatosFGEM() {
        return existenciaInicialFGEM > 0 || existenciaFinalFGEM > 0 || nacimientosFGEM > 0 || comprasFGEM > 0;
    }

    // ------------------------------------------------------------------ evaluación

    /** Indicadores con semáforo y lectura, en el orden en que se muestran. */
    public List<IndicadorGanaderoFGEM> evaluarFGEM(MonedaFGEM monedaFGEM) {
        List<IndicadorGanaderoFGEM> listaFGEM = new ArrayList<>();
        String hembrasFGEM = especieFGEM.hembrasFGEM();

        if (natalidadFGEM == null) {
            listaFGEM.add(new IndicadorGanaderoFGEM("Natalidad", "—", SemaforoFGEM.SIN_DATOS, null,
                    "No había " + hembrasFGEM + " al inicio del período para calcularla."));
        } else {
            listaFGEM.add(new IndicadorGanaderoFGEM("Natalidad", MontosFGEM.porcentajeFGEM(natalidadFGEM),
                    mayorEsMejorFGEM(natalidadFGEM, especieFGEM.natalidadVerdeFGEM(), especieFGEM.natalidadAmarilloFGEM()),
                    "Bien desde " + MontosFGEM.porcentajeFGEM(especieFGEM.natalidadVerdeFGEM()) + "; riesgo por debajo de "
                            + MontosFGEM.porcentajeFGEM(especieFGEM.natalidadAmarilloFGEM()) + ".",
                    nacimientosFGEM + " crías de " + hembrasInicioFGEM + " " + hembrasFGEM + "."));
        }

        if (mortalidadFGEM == null) {
            listaFGEM.add(new IndicadorGanaderoFGEM("Mortalidad", "—", SemaforoFGEM.SIN_DATOS, null, "Sin animales en el período."));
        } else {
            listaFGEM.add(new IndicadorGanaderoFGEM("Mortalidad", MontosFGEM.porcentajeFGEM(mortalidadFGEM),
                    menorEsMejorFGEM(mortalidadFGEM, especieFGEM.mortalidadVerdeFGEM(), especieFGEM.mortalidadAmarilloFGEM()),
                    "Bien hasta " + MontosFGEM.porcentajeFGEM(especieFGEM.mortalidadVerdeFGEM()) + "; riesgo sobre "
                            + MontosFGEM.porcentajeFGEM(especieFGEM.mortalidadAmarilloFGEM()) + ".",
                    muertesFGEM == 0 ? "No murió ningún animal." : muertesFGEM + (muertesFGEM == 1 ? " animal murió" : " animales murieron")
                            + " de " + (existenciaInicialFGEM + nacimientosFGEM + comprasFGEM) + " que hubo en el período."));
        }

        if (mortalidadCriasFGEM != null) {
            listaFGEM.add(new IndicadorGanaderoFGEM("Mortalidad de crías", MontosFGEM.porcentajeFGEM(mortalidadCriasFGEM),
                    menorEsMejorFGEM(mortalidadCriasFGEM, especieFGEM.mortalidadCriasVerdeFGEM(), especieFGEM.mortalidadCriasAmarilloFGEM()),
                    "Bien hasta " + MontosFGEM.porcentajeFGEM(especieFGEM.mortalidadCriasVerdeFGEM()) + "; riesgo sobre "
                            + MontosFGEM.porcentajeFGEM(especieFGEM.mortalidadCriasAmarilloFGEM()) + ".",
                    muertesCriasFGEM + " de " + nacimientosFGEM + " crías nacidas murieron."));
        }

        if (extraccionFGEM != null) {
            listaFGEM.add(new IndicadorGanaderoFGEM("Extracción", MontosFGEM.porcentajeFGEM(extraccionFGEM), SemaforoFGEM.SIN_DATOS, null,
                    (ventasFGEM + consumoFGEM) + " animales vendidos o consumidos"
                            + (ingresoVentasFGEM.signum() > 0 ? "; ventas por " + MontosFGEM.formatearFGEM(ingresoVentasFGEM, monedaFGEM) : "")
                            + "."));
        }

        if (crecimientoFGEM != null) {
            listaFGEM.add(new IndicadorGanaderoFGEM("Crecimiento del hato",
                    (crecimientoFGEM.signum() > 0 ? "+" : "") + MontosFGEM.porcentajeFGEM(crecimientoFGEM),
                    crecimientoFGEM.signum() >= 0 ? SemaforoFGEM.VERDE
                            : crecimientoFGEM.compareTo(new BigDecimal("-0.10")) >= 0 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO,
                    "Bien si se mantiene o crece; riesgo si cae más de 10 %.",
                    "De " + existenciaInicialFGEM + " a " + existenciaFinalFGEM + " animales."));
        }

        if (perdidasFGEM > 0) {
            listaFGEM.add(new IndicadorGanaderoFGEM("Pérdidas y robos", String.valueOf(perdidasFGEM), SemaforoFGEM.AMARILLO, null,
                    perdidasFGEM + (perdidasFGEM == 1 ? " animal perdido o robado" : " animales perdidos o robados")
                            + " en el período."));
        }

        if (hembrasFinalFGEM > 0) {
            listaFGEM.add(new IndicadorGanaderoFGEM(capitalFGEM(hembrasFGEM) + " por reproductor",
                    hembrasPorMachoFGEM == null ? "Sin reproductor" : MontosFGEM.cantidadFGEM(hembrasPorMachoFGEM.setScale(1, RoundingMode.HALF_UP)),
                    SemaforoFGEM.SIN_DATOS, null,
                    hembrasPorMachoFGEM == null
                            ? "No hay machos reproductores registrados (puede usarse monta prestada o inseminación)."
                            : hembrasFinalFGEM + " " + hembrasFGEM + " y " + machosFinalFGEM + (machosFinalFGEM == 1 ? " reproductor." : " reproductores.")));
        }

        for (ResumenProduccionFGEM pFGEM : produccionFGEM) {
            listaFGEM.add(new IndicadorGanaderoFGEM("Producción de " + pFGEM.productoFGEM().descripcionFGEM().toLowerCase(),
                    pFGEM.promedioTextoFGEM(), SemaforoFGEM.SIN_DATOS, null,
                    "Total: " + MontosFGEM.cantidadFGEM(pFGEM.cantidadFGEM().setScale(1, RoundingMode.HALF_UP)) + " " + pFGEM.productoFGEM().unidadFGEM()
                            + (pFGEM.valorFGEM().signum() > 0 ? ", valorizado en " + MontosFGEM.formatearFGEM(pFGEM.valorFGEM(), monedaFGEM) : "") + "."));
        }
        return listaFGEM;
    }

    private static String capitalFGEM(String textoFGEM) {
        return Character.toUpperCase(textoFGEM.charAt(0)) + textoFGEM.substring(1);
    }

    private static SemaforoFGEM mayorEsMejorFGEM(BigDecimal valorFGEM, BigDecimal verdeFGEM, BigDecimal amarilloFGEM) {
        valorFGEM = mostradoFGEM(valorFGEM);
        if (valorFGEM.compareTo(verdeFGEM) >= 0) {
            return SemaforoFGEM.VERDE;
        }
        return valorFGEM.compareTo(amarilloFGEM) >= 0 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO;
    }

    private static SemaforoFGEM menorEsMejorFGEM(BigDecimal valorFGEM, BigDecimal verdeFGEM, BigDecimal amarilloFGEM) {
        valorFGEM = mostradoFGEM(valorFGEM);
        if (valorFGEM.compareTo(verdeFGEM) <= 0) {
            return SemaforoFGEM.VERDE;
        }
        return valorFGEM.compareTo(amarilloFGEM) <= 0 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO;
    }

    /** El semáforo compara el porcentaje tal como se muestra (con un decimal), para que coincida con el criterio. */
    private static BigDecimal mostradoFGEM(BigDecimal fraccionFGEM) {
        return fraccionFGEM.setScale(3, RoundingMode.HALF_UP);
    }

    private static int totalFGEM(Map<CategoriaGanadoFGEM, Integer> existenciasFGEM) {
        return existenciasFGEM.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static int porRolFGEM(Map<CategoriaGanadoFGEM, Integer> existenciasFGEM, CategoriaGanadoFGEM.RolFGEM rolFGEM) {
        return existenciasFGEM.entrySet().stream().filter(eFGEM -> eFGEM.getKey().rolFGEM() == rolFGEM)
                .mapToInt(Map.Entry::getValue).sum();
    }

    private static int sumaFGEM(List<MovimientoGanadoFGEM> movimientosFGEM, TipoMovimientoGanadoFGEM tipoFGEM) {
        return movimientosFGEM.stream().filter(mFGEM -> mFGEM.tipoFGEM() == tipoFGEM).mapToInt(MovimientoGanadoFGEM::cantidadFGEM).sum();
    }

    private static BigDecimal dividirFGEM(int aFGEM, int bFGEM) {
        return bFGEM == 0 ? null : BigDecimal.valueOf(aFGEM).divide(BigDecimal.valueOf(bFGEM), ESCALA_FGEM, RoundingMode.HALF_UP);
    }
}
