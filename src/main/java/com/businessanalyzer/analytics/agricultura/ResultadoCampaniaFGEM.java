package com.businessanalyzer.analytics.agricultura;

import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;
import com.businessanalyzer.analytics.ganaderia.IndicadorGanaderoFGEM;
import com.businessanalyzer.model.CampaniaFGEM;
import com.businessanalyzer.model.CosechaFGEM;
import com.businessanalyzer.model.CostoCampaniaFGEM;
import com.businessanalyzer.model.DestinoCosechaFGEM;
import com.businessanalyzer.model.EstadoCampaniaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.RubroCostoFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resultados técnicos y económicos de una campaña:
 * <ul>
 *   <li><b>Rendimiento</b> = kilos cosechados ÷ hectáreas sembradas.</li>
 *   <li><b>Valor de la producción</b> = ventas + lo no vendido valorizado al precio en chacra (o, si no se
 *       indicó, al precio promedio de venta). La merma poscosecha no se valoriza.</li>
 *   <li><b>Margen</b> = valor de la producción − costos.</li>
 *   <li><b>Rendimiento de equilibrio</b> = costo por hectárea ÷ precio por kilo: los kilos por hectárea
 *       que hay que cosechar para no perder.</li>
 * </ul>
 *
 * @param precioValorizacionFGEM S/ por kg con que se valoriza lo no vendido; {@code null} si no hay precio
 */
public record ResultadoCampaniaFGEM(CampaniaFGEM campaniaFGEM, BigDecimal costoTotalFGEM, Map<RubroCostoFGEM, BigDecimal> costosPorRubroFGEM,
                                BigDecimal produccionFGEM, Map<DestinoCosechaFGEM, BigDecimal> porDestinoFGEM,
                                BigDecimal ingresoVentasFGEM, BigDecimal precioPromedioVentaFGEM, BigDecimal precioValorizacionFGEM,
                                BigDecimal valorProduccionFGEM) {

    /** Proporción de la referencia desde la cual el rendimiento se considera bueno. */
    public static final BigDecimal RENDIMIENTO_VERDE_FGEM = BigDecimal.ONE;
    /** Por debajo de esta proporción de la referencia el rendimiento es bajo. */
    public static final BigDecimal RENDIMIENTO_AMARILLO_FGEM = new BigDecimal("0.70");
    /** Rentabilidad (margen ÷ costo) desde la cual la campaña se considera buena. */
    public static final BigDecimal RENTABILIDAD_VERDE_FGEM = new BigDecimal("0.20");

    private static final int ESCALA_FGEM = 6;

    public static ResultadoCampaniaFGEM deFGEM(CampaniaFGEM campaniaFGEM, Collection<CostoCampaniaFGEM> costosFGEM, Collection<CosechaFGEM> cosechasFGEM) {
        Map<RubroCostoFGEM, BigDecimal> rubrosFGEM = new EnumMap<>(RubroCostoFGEM.class);
        BigDecimal costoTotalFGEM = BigDecimal.ZERO;
        for (CostoCampaniaFGEM kFGEM : costosFGEM) {
            if (!kFGEM.anuladoFGEM() && kFGEM.campaniaIdFGEM() == campaniaFGEM.idFGEM()) {
                rubrosFGEM.merge(kFGEM.rubroFGEM(), kFGEM.montoFGEM(), BigDecimal::add);
                costoTotalFGEM = costoTotalFGEM.add(kFGEM.montoFGEM());
            }
        }
        Map<DestinoCosechaFGEM, BigDecimal> destinosFGEM = new EnumMap<>(DestinoCosechaFGEM.class);
        BigDecimal produccionFGEM = BigDecimal.ZERO;
        BigDecimal ingresosFGEM = BigDecimal.ZERO;
        BigDecimal kilosConPrecioFGEM = BigDecimal.ZERO;
        for (CosechaFGEM hFGEM : cosechasFGEM) {
            if (hFGEM.anuladoFGEM() || hFGEM.campaniaIdFGEM() != campaniaFGEM.idFGEM()) {
                continue;
            }
            destinosFGEM.merge(hFGEM.destinoFGEM(), hFGEM.cantidadFGEM(), BigDecimal::add);
            produccionFGEM = produccionFGEM.add(hFGEM.cantidadFGEM());
            if (hFGEM.destinoFGEM() == DestinoCosechaFGEM.VENTA && hFGEM.precioFGEM() != null) {
                ingresosFGEM = ingresosFGEM.add(hFGEM.cantidadFGEM().multiply(hFGEM.precioFGEM()));
                kilosConPrecioFGEM = kilosConPrecioFGEM.add(hFGEM.cantidadFGEM());
            }
        }
        BigDecimal precioPromedioFGEM = kilosConPrecioFGEM.signum() == 0 ? null
                : ingresosFGEM.divide(kilosConPrecioFGEM, 4, RoundingMode.HALF_UP);
        BigDecimal precioFGEM = campaniaFGEM.precioChacraFGEM() != null ? campaniaFGEM.precioChacraFGEM() : precioPromedioFGEM;

        BigDecimal valorFGEM = ingresosFGEM;
        if (precioFGEM != null) {
            for (Map.Entry<DestinoCosechaFGEM, BigDecimal> eFGEM : destinosFGEM.entrySet()) {
                if (eFGEM.getKey() != DestinoCosechaFGEM.VENTA && eFGEM.getKey() != DestinoCosechaFGEM.MERMA) {
                    valorFGEM = valorFGEM.add(eFGEM.getValue().multiply(precioFGEM));
                }
            }
            // Ventas registradas sin precio: se valorizan al precio de referencia.
            BigDecimal vendidosSinPrecioFGEM = destinosFGEM.getOrDefault(DestinoCosechaFGEM.VENTA, BigDecimal.ZERO).subtract(kilosConPrecioFGEM);
            valorFGEM = valorFGEM.add(vendidosSinPrecioFGEM.multiply(precioFGEM));
        }
        return new ResultadoCampaniaFGEM(campaniaFGEM, costoTotalFGEM, rubrosFGEM, produccionFGEM, destinosFGEM,
                ingresosFGEM.setScale(2, RoundingMode.HALF_UP), precioPromedioFGEM, precioFGEM, valorFGEM.setScale(2, RoundingMode.HALF_UP));
    }

    /** Kilos por hectárea; {@code null} si aún no hay cosecha (salvo campaña perdida: 0). */
    public BigDecimal rendimientoFGEM() {
        if (produccionFGEM.signum() == 0 && campaniaFGEM.estadoFGEM() != EstadoCampaniaFGEM.PERDIDA) {
            return null;
        }
        return produccionFGEM.divide(campaniaFGEM.areaFGEM(), ESCALA_FGEM, RoundingMode.HALF_UP);
    }

    /** Rendimiento ÷ referencia; {@code null} sin rendimiento o sin referencia. */
    public BigDecimal avanceReferenciaFGEM() {
        BigDecimal rFGEM = rendimientoFGEM();
        return rFGEM == null || campaniaFGEM.rendimientoReferenciaFGEM() == null ? null
                : rFGEM.divide(campaniaFGEM.rendimientoReferenciaFGEM(), ESCALA_FGEM, RoundingMode.HALF_UP);
    }

    public BigDecimal costoPorHectareaFGEM() {
        return costoTotalFGEM.divide(campaniaFGEM.areaFGEM(), 2, RoundingMode.HALF_UP);
    }

    /** Costo de producir un kilo; {@code null} sin producción. */
    public BigDecimal costoPorKiloFGEM() {
        return produccionFGEM.signum() == 0 ? null : costoTotalFGEM.divide(produccionFGEM, 4, RoundingMode.HALF_UP);
    }

    public BigDecimal margenFGEM() {
        return valorProduccionFGEM.subtract(costoTotalFGEM);
    }

    /** Margen ÷ costo; {@code null} sin costos. */
    public BigDecimal rentabilidadFGEM() {
        return costoTotalFGEM.signum() == 0 ? null : margenFGEM().divide(costoTotalFGEM, ESCALA_FGEM, RoundingMode.HALF_UP);
    }

    /** Kilos por hectárea necesarios para cubrir los costos; {@code null} sin precio o sin costos. */
    public BigDecimal rendimientoEquilibrioFGEM() {
        return precioValorizacionFGEM == null || precioValorizacionFGEM.signum() == 0 || costoTotalFGEM.signum() == 0 ? null
                : costoPorHectareaFGEM().divide(precioValorizacionFGEM, 2, RoundingMode.HALF_UP);
    }

    /** Parte de la producción que se vendió; {@code null} sin producción. */
    public BigDecimal parteVendidaFGEM() {
        return produccionFGEM.signum() == 0 ? null
                : porDestinoFGEM.getOrDefault(DestinoCosechaFGEM.VENTA, BigDecimal.ZERO).divide(produccionFGEM, ESCALA_FGEM, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ evaluación

    public List<IndicadorGanaderoFGEM> evaluarFGEM(MonedaFGEM monedaFGEM) {
        List<IndicadorGanaderoFGEM> listaFGEM = new ArrayList<>();
        BigDecimal rendimientoFGEM = rendimientoFGEM();
        BigDecimal referenciaFGEM = campaniaFGEM.rendimientoReferenciaFGEM();
        if (rendimientoFGEM == null) {
            listaFGEM.add(new IndicadorGanaderoFGEM("Rendimiento", "—", SemaforoFGEM.SIN_DATOS, null,
                    "Aún no hay cosecha registrada."));
        } else {
            BigDecimal avanceFGEM = avanceReferenciaFGEM();
            SemaforoFGEM semaforoFGEM = avanceFGEM == null ? SemaforoFGEM.SIN_DATOS
                    : avanceFGEM.compareTo(RENDIMIENTO_VERDE_FGEM) >= 0 ? SemaforoFGEM.VERDE
                    : avanceFGEM.compareTo(RENDIMIENTO_AMARILLO_FGEM) >= 0 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO;
            String lecturaFGEM = campaniaFGEM.estadoFGEM() == EstadoCampaniaFGEM.PERDIDA && produccionFGEM.signum() == 0
                    ? "Campaña perdida" + (campaniaFGEM.motivoFGEM() == null ? "." : ": " + campaniaFGEM.motivoFGEM() + ".")
                    : MontosFGEM.cantidadFGEM(produccionFGEM.setScale(0, RoundingMode.HALF_UP)) + " kg en "
                    + MontosFGEM.cantidadFGEM(campaniaFGEM.areaFGEM()) + " ha"
                    + (avanceFGEM == null ? "." : "; " + MontosFGEM.porcentajeFGEM(avanceFGEM) + " de la referencia ("
                    + kilosFGEM(referenciaFGEM) + " kg/ha).");
            listaFGEM.add(new IndicadorGanaderoFGEM("Rendimiento", kilosFGEM(rendimientoFGEM) + " kg/ha", semaforoFGEM,
                    avanceFGEM == null ? null : "Bien desde la referencia; bajo por debajo del 70 % de ella.", lecturaFGEM));
        }

        listaFGEM.add(new IndicadorGanaderoFGEM("Costo por hectárea", MontosFGEM.formatearFGEM(costoPorHectareaFGEM(), monedaFGEM),
                SemaforoFGEM.SIN_DATOS, null, "Costo total " + MontosFGEM.formatearFGEM(costoTotalFGEM, monedaFGEM) + rubroPrincipalFGEM(monedaFGEM)));

        if (costoPorKiloFGEM() != null) {
            listaFGEM.add(new IndicadorGanaderoFGEM("Costo por kilo", MontosFGEM.formatearFGEM(costoPorKiloFGEM(), monedaFGEM), SemaforoFGEM.SIN_DATOS,
                    null, precioValorizacionFGEM == null ? "Indica el precio en chacra para compararlo."
                    : "Precio por kilo: " + MontosFGEM.formatearFGEM(precioValorizacionFGEM, monedaFGEM) + "."));
        }

        BigDecimal equilibrioFGEM = rendimientoEquilibrioFGEM();
        if (equilibrioFGEM != null) {
            SemaforoFGEM semaforoFGEM = rendimientoFGEM == null ? SemaforoFGEM.SIN_DATOS
                    : rendimientoFGEM.compareTo(equilibrioFGEM) >= 0 ? SemaforoFGEM.VERDE : SemaforoFGEM.ROJO;
            listaFGEM.add(new IndicadorGanaderoFGEM("Rendimiento para no perder", kilosFGEM(equilibrioFGEM) + " kg/ha", semaforoFGEM,
                    "Bien si el rendimiento lo supera.",
                    "Con " + MontosFGEM.formatearFGEM(precioValorizacionFGEM, monedaFGEM) + " por kilo, hay que cosechar al menos "
                            + kilosFGEM(equilibrioFGEM) + " kg por hectárea para cubrir los costos."));
        }

        if (produccionFGEM.signum() > 0 || campaniaFGEM.estadoFGEM().cerradaFGEM()) {
            BigDecimal rentabilidadFGEM = rentabilidadFGEM();
            SemaforoFGEM semaforoFGEM = rentabilidadFGEM == null ? SemaforoFGEM.SIN_DATOS
                    : rentabilidadFGEM.compareTo(RENTABILIDAD_VERDE_FGEM) >= 0 ? SemaforoFGEM.VERDE
                    : rentabilidadFGEM.signum() >= 0 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO;
            listaFGEM.add(new IndicadorGanaderoFGEM("Margen", MontosFGEM.formatearFGEM(margenFGEM(), monedaFGEM), semaforoFGEM,
                    "Bien si gana al menos 20 % sobre el costo; riesgo si pierde.",
                    "Valor de la producción " + MontosFGEM.formatearFGEM(valorProduccionFGEM, monedaFGEM)
                            + (rentabilidadFGEM == null ? "." : "; " + (rentabilidadFGEM.signum() >= 0 ? "gana " : "pierde ")
                            + MontosFGEM.porcentajeFGEM(rentabilidadFGEM.abs()) + " sobre lo invertido.")
                            + (precioValorizacionFGEM == null && produccionFGEM.signum() > 0
                            ? " Sin precio de venta ni en chacra: lo no vendido no se valoriza." : "")));
        }

        if (parteVendidaFGEM() != null) {
            List<String> partesFGEM = new ArrayList<>();
            porDestinoFGEM.forEach((dFGEM, kgFGEM) -> partesFGEM.add(dFGEM.descripcionFGEM().toLowerCase() + " "
                    + MontosFGEM.porcentajeFGEM(kgFGEM.divide(produccionFGEM, ESCALA_FGEM, RoundingMode.HALF_UP))));
            listaFGEM.add(new IndicadorGanaderoFGEM("Destino de la cosecha", MontosFGEM.porcentajeFGEM(parteVendidaFGEM()) + " vendido",
                    SemaforoFGEM.SIN_DATOS, null, capitalFGEM(String.join(", ", partesFGEM)) + "."));
        }
        return listaFGEM;
    }

    private String rubroPrincipalFGEM(MonedaFGEM monedaFGEM) {
        return costosPorRubroFGEM.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(eFGEM -> "; el rubro mayor es " + eFGEM.getKey().descripcionFGEM().toLowerCase() + " ("
                        + MontosFGEM.formatearFGEM(eFGEM.getValue(), monedaFGEM) + ").")
                .orElse(".");
    }

    private static String kilosFGEM(BigDecimal valorFGEM) {
        return MontosFGEM.cantidadFGEM(valorFGEM.setScale(0, RoundingMode.HALF_UP));
    }

    private static String capitalFGEM(String textoFGEM) {
        return textoFGEM.isEmpty() ? textoFGEM : Character.toUpperCase(textoFGEM.charAt(0)) + textoFGEM.substring(1);
    }
}
