package com.businessanalyzer.analytics;

import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.util.Montos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Interpreta el análisis en frases cortas y claras para quien no es especialista
 * ("¿estoy ganando?", "¿cuánto me falta para cubrir mis costos?").
 * El diagnóstico completo con semáforo de riesgos llega en la Fase 11.
 */
public final class LecturaFinanciera {

    private LecturaFinanciera() {
    }

    public static List<String> generar(AnalisisFinanciero a, Moneda moneda) {
        List<String> frases = new ArrayList<>();
        EstadoResultados actual = a.actual();

        if (actual.ventasNetas().signum() == 0) {
            frases.add("No hay ventas registradas en " + a.periodo().descripcion() + ".");
        }

        BigDecimal neta = actual.utilidadNeta();
        if (neta.signum() > 0) {
            frases.add("El negocio ganó " + Montos.formatear(neta, moneda)
                    + (actual.margenNeto() == null ? "" : " (margen neto de " + Montos.porcentaje(actual.margenNeto()) + ")")
                    + ".");
        } else if (neta.signum() < 0) {
            frases.add("El negocio perdió " + Montos.formatear(neta.negate(), moneda) + " en el período.");
        } else if (actual.ventasNetas().signum() > 0) {
            frases.add("El negocio no ganó ni perdió en el período.");
        }

        Variacion ventas = new Variacion(actual.ventasNetas(), a.anterior().ventasNetas());
        if (ventas.relativa() != null) {
            String sentido = ventas.absoluta().signum() >= 0 ? "subieron" : "bajaron";
            frases.add("Las ventas " + sentido + " " + Montos.porcentaje(ventas.relativa().abs())
                    + " respecto a " + a.periodoAnterior().descripcion() + ".");
        }

        BigDecimal margenActual = actual.margenNeto();
        BigDecimal margenAnterior = a.anterior().margenNeto();
        if (margenActual != null && margenAnterior != null) {
            BigDecimal puntos = margenActual.subtract(margenAnterior).movePointRight(2).setScale(1, RoundingMode.HALF_UP);
            if (puntos.signum() != 0) {
                frases.add("El margen neto " + (puntos.signum() > 0 ? "mejoró " : "empeoró ") + puntos.abs()
                        + " puntos porcentuales.");
            }
        }

        PuntoEquilibrio pe = a.equilibrio();
        if (actual.ventasNetas().signum() > 0) {
            if (pe.ventasEquilibrio() == null) {
                frases.add("Con los márgenes actuales no se alcanza el punto de equilibrio: las ventas no cubren "
                        + "ni sus costos variables. Revisa precios y costos.");
            } else if (pe.alcanzado()) {
                frases.add("Las ventas superan el punto de equilibrio (" + Montos.formatear(pe.ventasEquilibrio(), moneda)
                        + "): podrían caer hasta " + Montos.porcentaje(pe.margenSeguridad()) + " antes de generar pérdidas.");
            } else {
                frases.add("Faltan " + Montos.formatear(pe.ventasFaltantes(), moneda)
                        + " en ventas para cubrir los costos fijos (punto de equilibrio: "
                        + Montos.formatear(pe.ventasEquilibrio(), moneda) + ").");
            }
        }

        BigDecimal margenTotal = a.margenTotalProductos();
        if (!a.productos().isEmpty() && margenTotal.signum() > 0) {
            RentabilidadProducto mejor = a.productos().get(0);
            frases.add("\"" + mejor.nombre() + "\" es el que más aporta al margen: "
                    + Montos.porcentaje(mejor.margen().divide(margenTotal, Margenes.ESCALA_RATIO, RoundingMode.HALF_UP))
                    + " del total.");
            long conPerdida = a.productos().stream().filter(p -> p.margen().signum() < 0).count();
            if (conPerdida > 0) {
                frases.add(conPerdida + (conPerdida == 1 ? " producto se vendió" : " productos se vendieron")
                        + " por debajo de su costo.");
            }
        }
        return frases;
    }
}
