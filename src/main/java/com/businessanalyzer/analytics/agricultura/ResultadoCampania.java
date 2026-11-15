package com.businessanalyzer.analytics.agricultura;

import com.businessanalyzer.analytics.diagnostico.Semaforo;
import com.businessanalyzer.analytics.ganaderia.IndicadorGanadero;
import com.businessanalyzer.model.Campania;
import com.businessanalyzer.model.Cosecha;
import com.businessanalyzer.model.CostoCampania;
import com.businessanalyzer.model.DestinoCosecha;
import com.businessanalyzer.model.EstadoCampania;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.RubroCosto;
import com.businessanalyzer.util.Montos;

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
 * @param precioValorizacion S/ por kg con que se valoriza lo no vendido; {@code null} si no hay precio
 */
public record ResultadoCampania(Campania campania, BigDecimal costoTotal, Map<RubroCosto, BigDecimal> costosPorRubro,
                                BigDecimal produccion, Map<DestinoCosecha, BigDecimal> porDestino,
                                BigDecimal ingresoVentas, BigDecimal precioPromedioVenta, BigDecimal precioValorizacion,
                                BigDecimal valorProduccion) {

    /** Proporción de la referencia desde la cual el rendimiento se considera bueno. */
    public static final BigDecimal RENDIMIENTO_VERDE = BigDecimal.ONE;
    /** Por debajo de esta proporción de la referencia el rendimiento es bajo. */
    public static final BigDecimal RENDIMIENTO_AMARILLO = new BigDecimal("0.70");
    /** Rentabilidad (margen ÷ costo) desde la cual la campaña se considera buena. */
    public static final BigDecimal RENTABILIDAD_VERDE = new BigDecimal("0.20");

    private static final int ESCALA = 6;

    public static ResultadoCampania de(Campania campania, Collection<CostoCampania> costos, Collection<Cosecha> cosechas) {
        Map<RubroCosto, BigDecimal> rubros = new EnumMap<>(RubroCosto.class);
        BigDecimal costoTotal = BigDecimal.ZERO;
        for (CostoCampania k : costos) {
            if (!k.anulado() && k.campaniaId() == campania.id()) {
                rubros.merge(k.rubro(), k.monto(), BigDecimal::add);
                costoTotal = costoTotal.add(k.monto());
            }
        }
        Map<DestinoCosecha, BigDecimal> destinos = new EnumMap<>(DestinoCosecha.class);
        BigDecimal produccion = BigDecimal.ZERO;
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal kilosConPrecio = BigDecimal.ZERO;
        for (Cosecha h : cosechas) {
            if (h.anulado() || h.campaniaId() != campania.id()) {
                continue;
            }
            destinos.merge(h.destino(), h.cantidad(), BigDecimal::add);
            produccion = produccion.add(h.cantidad());
            if (h.destino() == DestinoCosecha.VENTA && h.precio() != null) {
                ingresos = ingresos.add(h.cantidad().multiply(h.precio()));
                kilosConPrecio = kilosConPrecio.add(h.cantidad());
            }
        }
        BigDecimal precioPromedio = kilosConPrecio.signum() == 0 ? null
                : ingresos.divide(kilosConPrecio, 4, RoundingMode.HALF_UP);
        BigDecimal precio = campania.precioChacra() != null ? campania.precioChacra() : precioPromedio;

        BigDecimal valor = ingresos;
        if (precio != null) {
            for (Map.Entry<DestinoCosecha, BigDecimal> e : destinos.entrySet()) {
                if (e.getKey() != DestinoCosecha.VENTA && e.getKey() != DestinoCosecha.MERMA) {
                    valor = valor.add(e.getValue().multiply(precio));
                }
            }
            // Ventas registradas sin precio: se valorizan al precio de referencia.
            BigDecimal vendidosSinPrecio = destinos.getOrDefault(DestinoCosecha.VENTA, BigDecimal.ZERO).subtract(kilosConPrecio);
            valor = valor.add(vendidosSinPrecio.multiply(precio));
        }
        return new ResultadoCampania(campania, costoTotal, rubros, produccion, destinos,
                ingresos.setScale(2, RoundingMode.HALF_UP), precioPromedio, precio, valor.setScale(2, RoundingMode.HALF_UP));
    }

    /** Kilos por hectárea; {@code null} si aún no hay cosecha (salvo campaña perdida: 0). */
    public BigDecimal rendimiento() {
        if (produccion.signum() == 0 && campania.estado() != EstadoCampania.PERDIDA) {
            return null;
        }
        return produccion.divide(campania.area(), ESCALA, RoundingMode.HALF_UP);
    }

    /** Rendimiento ÷ referencia; {@code null} sin rendimiento o sin referencia. */
    public BigDecimal avanceReferencia() {
        BigDecimal r = rendimiento();
        return r == null || campania.rendimientoReferencia() == null ? null
                : r.divide(campania.rendimientoReferencia(), ESCALA, RoundingMode.HALF_UP);
    }

    public BigDecimal costoPorHectarea() {
        return costoTotal.divide(campania.area(), 2, RoundingMode.HALF_UP);
    }

    /** Costo de producir un kilo; {@code null} sin producción. */
    public BigDecimal costoPorKilo() {
        return produccion.signum() == 0 ? null : costoTotal.divide(produccion, 4, RoundingMode.HALF_UP);
    }

    public BigDecimal margen() {
        return valorProduccion.subtract(costoTotal);
    }

    /** Margen ÷ costo; {@code null} sin costos. */
    public BigDecimal rentabilidad() {
        return costoTotal.signum() == 0 ? null : margen().divide(costoTotal, ESCALA, RoundingMode.HALF_UP);
    }

    /** Kilos por hectárea necesarios para cubrir los costos; {@code null} sin precio o sin costos. */
    public BigDecimal rendimientoEquilibrio() {
        return precioValorizacion == null || precioValorizacion.signum() == 0 || costoTotal.signum() == 0 ? null
                : costoPorHectarea().divide(precioValorizacion, 2, RoundingMode.HALF_UP);
    }

    /** Parte de la producción que se vendió; {@code null} sin producción. */
    public BigDecimal parteVendida() {
        return produccion.signum() == 0 ? null
                : porDestino.getOrDefault(DestinoCosecha.VENTA, BigDecimal.ZERO).divide(produccion, ESCALA, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ evaluación

    public List<IndicadorGanadero> evaluar(Moneda moneda) {
        List<IndicadorGanadero> lista = new ArrayList<>();
        BigDecimal rendimiento = rendimiento();
        BigDecimal referencia = campania.rendimientoReferencia();
        if (rendimiento == null) {
            lista.add(new IndicadorGanadero("Rendimiento", "—", Semaforo.SIN_DATOS, null,
                    "Aún no hay cosecha registrada."));
        } else {
            BigDecimal avance = avanceReferencia();
            Semaforo semaforo = avance == null ? Semaforo.SIN_DATOS
                    : avance.compareTo(RENDIMIENTO_VERDE) >= 0 ? Semaforo.VERDE
                    : avance.compareTo(RENDIMIENTO_AMARILLO) >= 0 ? Semaforo.AMARILLO : Semaforo.ROJO;
            String lectura = campania.estado() == EstadoCampania.PERDIDA && produccion.signum() == 0
                    ? "Campaña perdida" + (campania.motivo() == null ? "." : ": " + campania.motivo() + ".")
                    : Montos.cantidad(produccion.setScale(0, RoundingMode.HALF_UP)) + " kg en "
                    + Montos.cantidad(campania.area()) + " ha"
                    + (avance == null ? "." : "; " + Montos.porcentaje(avance) + " de la referencia ("
                    + kilos(referencia) + " kg/ha).");
            lista.add(new IndicadorGanadero("Rendimiento", kilos(rendimiento) + " kg/ha", semaforo,
                    avance == null ? null : "Bien desde la referencia; bajo por debajo del 70 % de ella.", lectura));
        }

        lista.add(new IndicadorGanadero("Costo por hectárea", Montos.formatear(costoPorHectarea(), moneda),
                Semaforo.SIN_DATOS, null, "Costo total " + Montos.formatear(costoTotal, moneda) + rubroPrincipal(moneda)));

        if (costoPorKilo() != null) {
            lista.add(new IndicadorGanadero("Costo por kilo", Montos.formatear(costoPorKilo(), moneda), Semaforo.SIN_DATOS,
                    null, precioValorizacion == null ? "Indica el precio en chacra para compararlo."
                    : "Precio por kilo: " + Montos.formatear(precioValorizacion, moneda) + "."));
        }

        BigDecimal equilibrio = rendimientoEquilibrio();
        if (equilibrio != null) {
            Semaforo semaforo = rendimiento == null ? Semaforo.SIN_DATOS
                    : rendimiento.compareTo(equilibrio) >= 0 ? Semaforo.VERDE : Semaforo.ROJO;
            lista.add(new IndicadorGanadero("Rendimiento para no perder", kilos(equilibrio) + " kg/ha", semaforo,
                    "Bien si el rendimiento lo supera.",
                    "Con " + Montos.formatear(precioValorizacion, moneda) + " por kilo, hay que cosechar al menos "
                            + kilos(equilibrio) + " kg por hectárea para cubrir los costos."));
        }

        if (produccion.signum() > 0 || campania.estado().cerrada()) {
            BigDecimal rentabilidad = rentabilidad();
            Semaforo semaforo = rentabilidad == null ? Semaforo.SIN_DATOS
                    : rentabilidad.compareTo(RENTABILIDAD_VERDE) >= 0 ? Semaforo.VERDE
                    : rentabilidad.signum() >= 0 ? Semaforo.AMARILLO : Semaforo.ROJO;
            lista.add(new IndicadorGanadero("Margen", Montos.formatear(margen(), moneda), semaforo,
                    "Bien si gana al menos 20 % sobre el costo; riesgo si pierde.",
                    "Valor de la producción " + Montos.formatear(valorProduccion, moneda)
                            + (rentabilidad == null ? "." : "; " + (rentabilidad.signum() >= 0 ? "gana " : "pierde ")
                            + Montos.porcentaje(rentabilidad.abs()) + " sobre lo invertido.")
                            + (precioValorizacion == null && produccion.signum() > 0
                            ? " Sin precio de venta ni en chacra: lo no vendido no se valoriza." : "")));
        }

        if (parteVendida() != null) {
            List<String> partes = new ArrayList<>();
            porDestino.forEach((d, kg) -> partes.add(d.descripcion().toLowerCase() + " "
                    + Montos.porcentaje(kg.divide(produccion, ESCALA, RoundingMode.HALF_UP))));
            lista.add(new IndicadorGanadero("Destino de la cosecha", Montos.porcentaje(parteVendida()) + " vendido",
                    Semaforo.SIN_DATOS, null, capital(String.join(", ", partes)) + "."));
        }
        return lista;
    }

    private String rubroPrincipal(Moneda moneda) {
        return costosPorRubro.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(e -> "; el rubro mayor es " + e.getKey().descripcion().toLowerCase() + " ("
                        + Montos.formatear(e.getValue(), moneda) + ").")
                .orElse(".");
    }

    private static String kilos(BigDecimal valor) {
        return Montos.cantidad(valor.setScale(0, RoundingMode.HALF_UP));
    }

    private static String capital(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
