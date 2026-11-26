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

public record ResultadoCampaniaFGEM(CampaniaFGEM campaniaFGEM, BigDecimal costoTotalFGEM, Map<RubroCostoFGEM, BigDecimal> costosPorRubroFGEM,
                                BigDecimal produccionFGEM, Map<DestinoCosechaFGEM, BigDecimal> porDestinoFGEM,
                                BigDecimal ingresoVentasFGEM, BigDecimal precioPromedioVentaFGEM, BigDecimal precioValorizacionFGEM,
                                BigDecimal valorProduccionFGEM) {

    public static final BigDecimal RENDIMIENTO_VERDE_FGEM = BigDecimal.ONE;
    public static final BigDecimal RENDIMIENTO_AMARILLO_FGEM = new BigDecimal("0.70");
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
            BigDecimal vendidosSinPrecioFGEM = destinosFGEM.getOrDefault(DestinoCosechaFGEM.VENTA, BigDecimal.ZERO).subtract(kilosConPrecioFGEM);
            valorFGEM = valorFGEM.add(vendidosSinPrecioFGEM.multiply(precioFGEM));
        }
        return new ResultadoCampaniaFGEM(campaniaFGEM, costoTotalFGEM, rubrosFGEM, produccionFGEM, destinosFGEM,
                ingresosFGEM.setScale(2, RoundingMode.HALF_UP), precioPromedioFGEM, precioFGEM, valorFGEM.setScale(2, RoundingMode.HALF_UP));
    }

    public BigDecimal rendimientoFGEM() {
        if (produccionFGEM.signum() == 0 && campaniaFGEM.estadoFGEM() != EstadoCampaniaFGEM.PERDIDA) {
            return null;
        }
        return produccionFGEM.divide(campaniaFGEM.areaFGEM(), ESCALA_FGEM, RoundingMode.HALF_UP);
    }

    public BigDecimal avanceReferenciaFGEM() {
        BigDecimal rFGEM = rendimientoFGEM();
        return rFGEM == null || campaniaFGEM.rendimientoReferenciaFGEM() == null ? null
                : rFGEM.divide(campaniaFGEM.rendimientoReferenciaFGEM(), ESCALA_FGEM, RoundingMode.HALF_UP);
    }

    public BigDecimal costoPorHectareaFGEM() {
        return costoTotalFGEM.divide(campaniaFGEM.areaFGEM(), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal costoPorKiloFGEM() {
        return produccionFGEM.signum() == 0 ? null : costoTotalFGEM.divide(produccionFGEM, 4, RoundingMode.HALF_UP);
    }

    public BigDecimal margenFGEM() {
        return valorProduccionFGEM.subtract(costoTotalFGEM);
    }

    public BigDecimal rentabilidadFGEM() {
        return costoTotalFGEM.signum() == 0 ? null : margenFGEM().divide(costoTotalFGEM, ESCALA_FGEM, RoundingMode.HALF_UP);
    }

    public BigDecimal rendimientoEquilibrioFGEM() {
        return precioValorizacionFGEM == null || precioValorizacionFGEM.signum() == 0 || costoTotalFGEM.signum() == 0 ? null
                : costoPorHectareaFGEM().divide(precioValorizacionFGEM, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal parteVendidaFGEM() {
        return produccionFGEM.signum() == 0 ? null
                : porDestinoFGEM.getOrDefault(DestinoCosechaFGEM.VENTA, BigDecimal.ZERO).divide(produccionFGEM, ESCALA_FGEM, RoundingMode.HALF_UP);
    }

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
