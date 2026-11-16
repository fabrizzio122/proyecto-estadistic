package com.businessanalyzer.analytics.patrimonio;

import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Explica en lenguaje sencillo la situación patrimonial de la empresa. */
public final class LecturaPatrimonioFGEM {

    private LecturaPatrimonioFGEM() {
    }

    public static List<String> generarFGEM(SituacionPatrimonialFGEM sFGEM, MonedaFGEM monedaFGEM) {
        BalanceGeneralFGEM bFGEM = sFGEM.balanceFGEM();
        IndicadoresPatrimonioFGEM iFGEM = sFGEM.indicadoresFGEM();
        List<String> frasesFGEM = new ArrayList<>();
        if (bFGEM.sinRegistrosFGEM()) {
            frasesFGEM.add("Aún no hay activos ni deudas registrados. Empieza por el saldo de caja y bancos, "
                    + "los equipos y vehículos del negocio y los préstamos que tenga.");
            if (bFGEM.inventarioFGEM().signum() > 0) {
                frasesFGEM.add("Por ahora solo figura el inventario de mercadería ("
                        + MontosFGEM.formatearFGEM(bFGEM.inventarioFGEM(), monedaFGEM) + "), calculado con el stock del catálogo.");
            }
            return frasesFGEM;
        }

        BigDecimal patrimonioFGEM = bFGEM.patrimonioFGEM();
        if (patrimonioFGEM.signum() > 0) {
            frasesFGEM.add("El negocio tiene " + MontosFGEM.formatearFGEM(bFGEM.totalActivoFGEM(), monedaFGEM) + " en bienes y derechos y debe "
                    + MontosFGEM.formatearFGEM(bFGEM.totalPasivoFGEM(), monedaFGEM) + ": lo que realmente pertenece a los dueños es "
                    + MontosFGEM.formatearFGEM(patrimonioFGEM, monedaFGEM) + ".");
        } else {
            frasesFGEM.add("Las deudas (" + MontosFGEM.formatearFGEM(bFGEM.totalPasivoFGEM(), monedaFGEM) + ") superan a los bienes del negocio ("
                    + MontosFGEM.formatearFGEM(bFGEM.totalActivoFGEM(), monedaFGEM) + "): el patrimonio es negativo. Es una señal de alerta "
                    + "que conviene revisar con un asesor.");
        }

        if (iFGEM.liquidezCorrienteFGEM() == null) {
            frasesFGEM.add("No hay deudas que venzan en los próximos 12 meses.");
        } else {
            BigDecimal porSolFGEM = iFGEM.liquidezCorrienteFGEM().setScale(2, RoundingMode.HALF_UP);
            String baseFGEM = "Por cada " + monedaFGEM.simboloFGEM() + " 1 de deuda de corto plazo hay " + monedaFGEM.simboloFGEM() + " "
                    + porSolFGEM.toPlainString() + " en caja, cobros pendientes e inventario";
            if (iFGEM.liquidezCorrienteFGEM().compareTo(IndicadoresPatrimonioFGEM.LIQUIDEZ_VERDE_FGEM) >= 0) {
                frasesFGEM.add(baseFGEM + ": las deudas del año están bien cubiertas.");
            } else if (iFGEM.liquidezCorrienteFGEM().compareTo(IndicadoresPatrimonioFGEM.LIQUIDEZ_AMARILLO_FGEM) >= 0) {
                frasesFGEM.add(baseFGEM + ": alcanza, pero con poco margen ante un imprevisto.");
            } else {
                frasesFGEM.add(baseFGEM + ": no alcanza para cubrir lo que vence en el año. Faltan "
                        + MontosFGEM.formatearFGEM(iFGEM.capitalTrabajoFGEM().negate(), monedaFGEM) + ".");
            }
            if (iFGEM.pruebaAcidaFGEM() != null && bFGEM.inventarioFGEM().signum() > 0
                    && iFGEM.pruebaAcidaFGEM().compareTo(BigDecimal.ONE) < 0
                    && iFGEM.liquidezCorrienteFGEM().compareTo(BigDecimal.ONE) >= 0) {
                frasesFGEM.add("Sin contar el inventario, el dinero disponible no cubre esas deudas: pagarlas depende "
                        + "de vender la mercadería a tiempo.");
            }
        }

        if (iFGEM.endeudamientoFGEM() != null) {
            String pctFGEM = MontosFGEM.porcentajeFGEM(iFGEM.endeudamientoFGEM());
            if (iFGEM.endeudamientoFGEM().compareTo(IndicadoresPatrimonioFGEM.ENDEUDAMIENTO_VERDE_FGEM) <= 0) {
                frasesFGEM.add("Las deudas financian el " + pctFGEM + " de los bienes del negocio: un nivel de endeudamiento sano.");
            } else if (iFGEM.endeudamientoFGEM().compareTo(IndicadoresPatrimonioFGEM.ENDEUDAMIENTO_AMARILLO_FGEM) <= 0) {
                frasesFGEM.add("Las deudas financian el " + pctFGEM + " de los bienes: conviene no endeudarse más por ahora.");
            } else {
                frasesFGEM.add("Las deudas financian el " + pctFGEM + " de los bienes: el negocio depende mucho de sus acreedores.");
            }
        }

        if (iFGEM.rentabilidadPatrimonioFGEM() != null) {
            frasesFGEM.add("Con las utilidades de los últimos " + iFGEM.mesesResultadosFGEM() + " meses, el patrimonio rinde "
                    + MontosFGEM.porcentajeFGEM(iFGEM.rentabilidadPatrimonioFGEM()) + " al año (rentabilidad del patrimonio).");
        }

        if (iFGEM.coberturaCuotasFGEM() != null) {
            String cuotaFGEM = MontosFGEM.formatearFGEM(bFGEM.cuotasMensualesFGEM(), monedaFGEM);
            if (iFGEM.coberturaCuotasFGEM().compareTo(IndicadoresPatrimonioFGEM.COBERTURA_VERDE_FGEM) >= 0) {
                frasesFGEM.add("La utilidad operativa de un mes cubre " + IndicadoresPatrimonioFGEM.vecesFGEM(iFGEM.coberturaCuotasFGEM())
                        + " veces las cuotas (" + cuotaFGEM + " al mes): hay capacidad de pago.");
            } else if (iFGEM.coberturaCuotasFGEM().compareTo(BigDecimal.ONE) >= 0) {
                frasesFGEM.add("La utilidad operativa apenas cubre las cuotas (" + cuotaFGEM + " al mes): un mes malo "
                        + "podría complicar los pagos.");
            } else {
                frasesFGEM.add("La utilidad operativa no alcanza para pagar las cuotas (" + cuotaFGEM + " al mes): "
                        + "el negocio necesita otros fondos para cumplir. Evalúa refinanciar a un plazo mayor.");
            }
        }

        if (bFGEM.depreciacionMensualFGEM().signum() > 0) {
            frasesFGEM.add("Los equipos y bienes del negocio pierden unos " + MontosFGEM.formatearFGEM(bFGEM.depreciacionMensualFGEM(), monedaFGEM)
                    + " de valor al mes por desgaste (depreciación). Conviene ahorrar al menos ese monto para reponerlos.");
        }
        return frasesFGEM;
    }
}
