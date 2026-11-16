package com.businessanalyzer.analytics;

import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Interpreta el análisis en frases cortas y claras para quien no es especialista
 * ("¿estoy ganando?", "¿cuánto me falta para cubrir mis costos?").
 * El diagnóstico completo con semáforo de riesgos llega en la Fase 11.
 */
public final class LecturaFinancieraFGEM {

    private LecturaFinancieraFGEM() {
    }

    public static List<String> generarFGEM(AnalisisFinancieroFGEM aFGEM, MonedaFGEM monedaFGEM) {
        List<String> frasesFGEM = new ArrayList<>();
        EstadoResultadosFGEM actualFGEM = aFGEM.actualFGEM();

        if (actualFGEM.ventasNetasFGEM().signum() == 0) {
            frasesFGEM.add("No hay ventas registradas en " + aFGEM.periodoFGEM().descripcionFGEM() + ".");
        }

        BigDecimal netaFGEM = actualFGEM.utilidadNetaFGEM();
        if (netaFGEM.signum() > 0) {
            frasesFGEM.add("El negocio ganó " + MontosFGEM.formatearFGEM(netaFGEM, monedaFGEM)
                    + (actualFGEM.margenNetoFGEM() == null ? "" : " (margen neto de " + MontosFGEM.porcentajeFGEM(actualFGEM.margenNetoFGEM()) + ")")
                    + ".");
        } else if (netaFGEM.signum() < 0) {
            frasesFGEM.add("El negocio perdió " + MontosFGEM.formatearFGEM(netaFGEM.negate(), monedaFGEM) + " en el período.");
        } else if (actualFGEM.ventasNetasFGEM().signum() > 0) {
            frasesFGEM.add("El negocio no ganó ni perdió en el período.");
        }

        VariacionFGEM ventasFGEM = new VariacionFGEM(actualFGEM.ventasNetasFGEM(), aFGEM.anteriorFGEM().ventasNetasFGEM());
        if (ventasFGEM.relativaFGEM() != null) {
            String sentidoFGEM = ventasFGEM.absolutaFGEM().signum() >= 0 ? "subieron" : "bajaron";
            frasesFGEM.add("Las ventas " + sentidoFGEM + " " + MontosFGEM.porcentajeFGEM(ventasFGEM.relativaFGEM().abs())
                    + " respecto a " + aFGEM.periodoAnteriorFGEM().descripcionFGEM() + ".");
        }

        BigDecimal margenActualFGEM = actualFGEM.margenNetoFGEM();
        BigDecimal margenAnteriorFGEM = aFGEM.anteriorFGEM().margenNetoFGEM();
        if (margenActualFGEM != null && margenAnteriorFGEM != null) {
            BigDecimal puntosFGEM = margenActualFGEM.subtract(margenAnteriorFGEM).movePointRight(2).setScale(1, RoundingMode.HALF_UP);
            if (puntosFGEM.signum() != 0) {
                frasesFGEM.add("El margen neto " + (puntosFGEM.signum() > 0 ? "mejoró " : "empeoró ") + puntosFGEM.abs()
                        + " puntos porcentuales.");
            }
        }

        PuntoEquilibrioFGEM peFGEM = aFGEM.equilibrioFGEM();
        if (actualFGEM.ventasNetasFGEM().signum() > 0) {
            if (peFGEM.ventasEquilibrioFGEM() == null) {
                frasesFGEM.add("Con los márgenes actuales no se alcanza el punto de equilibrio: las ventas no cubren "
                        + "ni sus costos variables. Revisa precios y costos.");
            } else if (peFGEM.alcanzadoFGEM()) {
                frasesFGEM.add("Las ventas superan el punto de equilibrio (" + MontosFGEM.formatearFGEM(peFGEM.ventasEquilibrioFGEM(), monedaFGEM)
                        + "): podrían caer hasta " + MontosFGEM.porcentajeFGEM(peFGEM.margenSeguridadFGEM()) + " antes de generar pérdidas.");
            } else {
                frasesFGEM.add("Faltan " + MontosFGEM.formatearFGEM(peFGEM.ventasFaltantesFGEM(), monedaFGEM)
                        + " en ventas para cubrir los costos fijos (punto de equilibrio: "
                        + MontosFGEM.formatearFGEM(peFGEM.ventasEquilibrioFGEM(), monedaFGEM) + ").");
            }
        }

        BigDecimal margenTotalFGEM = aFGEM.margenTotalProductosFGEM();
        if (!aFGEM.productosFGEM().isEmpty() && margenTotalFGEM.signum() > 0) {
            RentabilidadProductoFGEM mejorFGEM = aFGEM.productosFGEM().get(0);
            frasesFGEM.add("\"" + mejorFGEM.nombreFGEM() + "\" es el que más aporta al margen: "
                    + MontosFGEM.porcentajeFGEM(mejorFGEM.margenFGEM().divide(margenTotalFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP))
                    + " del total.");
            long conPerdidaFGEM = aFGEM.productosFGEM().stream().filter(pFGEM -> pFGEM.margenFGEM().signum() < 0).count();
            if (conPerdidaFGEM > 0) {
                frasesFGEM.add(conPerdidaFGEM + (conPerdidaFGEM == 1 ? " producto se vendió" : " productos se vendieron")
                        + " por debajo de su costo.");
            }
        }
        return frasesFGEM;
    }
}
