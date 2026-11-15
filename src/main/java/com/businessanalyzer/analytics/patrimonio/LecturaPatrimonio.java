package com.businessanalyzer.analytics.patrimonio;

import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.util.Montos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Explica en lenguaje sencillo la situación patrimonial de la empresa. */
public final class LecturaPatrimonio {

    private LecturaPatrimonio() {
    }

    public static List<String> generar(SituacionPatrimonial s, Moneda moneda) {
        BalanceGeneral b = s.balance();
        IndicadoresPatrimonio i = s.indicadores();
        List<String> frases = new ArrayList<>();
        if (b.sinRegistros()) {
            frases.add("Aún no hay activos ni deudas registrados. Empieza por el saldo de caja y bancos, "
                    + "los equipos y vehículos del negocio y los préstamos que tenga.");
            if (b.inventario().signum() > 0) {
                frases.add("Por ahora solo figura el inventario de mercadería ("
                        + Montos.formatear(b.inventario(), moneda) + "), calculado con el stock del catálogo.");
            }
            return frases;
        }

        BigDecimal patrimonio = b.patrimonio();
        if (patrimonio.signum() > 0) {
            frases.add("El negocio tiene " + Montos.formatear(b.totalActivo(), moneda) + " en bienes y derechos y debe "
                    + Montos.formatear(b.totalPasivo(), moneda) + ": lo que realmente pertenece a los dueños es "
                    + Montos.formatear(patrimonio, moneda) + ".");
        } else {
            frases.add("Las deudas (" + Montos.formatear(b.totalPasivo(), moneda) + ") superan a los bienes del negocio ("
                    + Montos.formatear(b.totalActivo(), moneda) + "): el patrimonio es negativo. Es una señal de alerta "
                    + "que conviene revisar con un asesor.");
        }

        if (i.liquidezCorriente() == null) {
            frases.add("No hay deudas que venzan en los próximos 12 meses.");
        } else {
            BigDecimal porSol = i.liquidezCorriente().setScale(2, RoundingMode.HALF_UP);
            String base = "Por cada " + moneda.simbolo() + " 1 de deuda de corto plazo hay " + moneda.simbolo() + " "
                    + porSol.toPlainString() + " en caja, cobros pendientes e inventario";
            if (i.liquidezCorriente().compareTo(IndicadoresPatrimonio.LIQUIDEZ_VERDE) >= 0) {
                frases.add(base + ": las deudas del año están bien cubiertas.");
            } else if (i.liquidezCorriente().compareTo(IndicadoresPatrimonio.LIQUIDEZ_AMARILLO) >= 0) {
                frases.add(base + ": alcanza, pero con poco margen ante un imprevisto.");
            } else {
                frases.add(base + ": no alcanza para cubrir lo que vence en el año. Faltan "
                        + Montos.formatear(i.capitalTrabajo().negate(), moneda) + ".");
            }
            if (i.pruebaAcida() != null && b.inventario().signum() > 0
                    && i.pruebaAcida().compareTo(BigDecimal.ONE) < 0
                    && i.liquidezCorriente().compareTo(BigDecimal.ONE) >= 0) {
                frases.add("Sin contar el inventario, el dinero disponible no cubre esas deudas: pagarlas depende "
                        + "de vender la mercadería a tiempo.");
            }
        }

        if (i.endeudamiento() != null) {
            String pct = Montos.porcentaje(i.endeudamiento());
            if (i.endeudamiento().compareTo(IndicadoresPatrimonio.ENDEUDAMIENTO_VERDE) <= 0) {
                frases.add("Las deudas financian el " + pct + " de los bienes del negocio: un nivel de endeudamiento sano.");
            } else if (i.endeudamiento().compareTo(IndicadoresPatrimonio.ENDEUDAMIENTO_AMARILLO) <= 0) {
                frases.add("Las deudas financian el " + pct + " de los bienes: conviene no endeudarse más por ahora.");
            } else {
                frases.add("Las deudas financian el " + pct + " de los bienes: el negocio depende mucho de sus acreedores.");
            }
        }

        if (i.rentabilidadPatrimonio() != null) {
            frases.add("Con las utilidades de los últimos " + i.mesesResultados() + " meses, el patrimonio rinde "
                    + Montos.porcentaje(i.rentabilidadPatrimonio()) + " al año (rentabilidad del patrimonio).");
        }

        if (i.coberturaCuotas() != null) {
            String cuota = Montos.formatear(b.cuotasMensuales(), moneda);
            if (i.coberturaCuotas().compareTo(IndicadoresPatrimonio.COBERTURA_VERDE) >= 0) {
                frases.add("La utilidad operativa de un mes cubre " + IndicadoresPatrimonio.veces(i.coberturaCuotas())
                        + " veces las cuotas (" + cuota + " al mes): hay capacidad de pago.");
            } else if (i.coberturaCuotas().compareTo(BigDecimal.ONE) >= 0) {
                frases.add("La utilidad operativa apenas cubre las cuotas (" + cuota + " al mes): un mes malo "
                        + "podría complicar los pagos.");
            } else {
                frases.add("La utilidad operativa no alcanza para pagar las cuotas (" + cuota + " al mes): "
                        + "el negocio necesita otros fondos para cumplir. Evalúa refinanciar a un plazo mayor.");
            }
        }

        if (b.depreciacionMensual().signum() > 0) {
            frases.add("Los equipos y bienes del negocio pierden unos " + Montos.formatear(b.depreciacionMensual(), moneda)
                    + " de valor al mes por desgaste (depreciación). Conviene ahorrar al menos ese monto para reponerlos.");
        }
        return frases;
    }
}
