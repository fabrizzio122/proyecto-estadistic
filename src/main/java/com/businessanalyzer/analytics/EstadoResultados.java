package com.businessanalyzer.analytics;

import com.businessanalyzer.model.GrupoFinanciero;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estado de resultados (estado de pérdidas y ganancias) de un período. Todos los montos sin impuestos.
 * <pre>
 *   Ventas netas
 * − Costo de ventas                 (costo de la mercadería vendida, Fase 5)
 * = Utilidad bruta
 * − Gastos operativos               (alquiler, sueldos, servicios..., Fase 6)
 * = Utilidad operativa
 * + Ingresos financieros − Gastos financieros + Otros ingresos
 * = Utilidad antes de impuestos
 * − Impuesto a la renta              (registrado por el usuario en Gestión financiera)
 * = Utilidad neta
 * </pre>
 */
public record EstadoResultados(
        BigDecimal ventasNetas,
        BigDecimal costoVentas,
        BigDecimal utilidadBruta,
        BigDecimal gastosOperativos,
        BigDecimal utilidadOperativa,
        BigDecimal ingresosFinancieros,
        BigDecimal gastosFinancieros,
        BigDecimal otrosIngresos,
        BigDecimal utilidadAntesImpuestos,
        BigDecimal impuestoRenta,
        BigDecimal utilidadNeta) {

    public static EstadoResultados de(ResumenOperaciones operaciones, ResumenFinanciero finanzas) {
        BigDecimal ventas = operaciones.ventasNetas();
        BigDecimal costo = operaciones.costoVentas();
        BigDecimal bruta = ventas.subtract(costo);
        BigDecimal gastosOperativos = finanzas.porGrupo(GrupoFinanciero.GASTO_OPERATIVO);
        BigDecimal operativa = bruta.subtract(gastosOperativos);
        BigDecimal ingresosFinancieros = finanzas.porGrupo(GrupoFinanciero.INGRESO_FINANCIERO);
        BigDecimal gastosFinancieros = finanzas.porGrupo(GrupoFinanciero.GASTO_FINANCIERO);
        BigDecimal otros = finanzas.porGrupo(GrupoFinanciero.OTRO_INGRESO);
        BigDecimal antesImpuestos = operativa.add(ingresosFinancieros).subtract(gastosFinancieros).add(otros);
        BigDecimal impuesto = finanzas.porGrupo(GrupoFinanciero.IMPUESTO_RENTA);
        return new EstadoResultados(ventas, costo, bruta, gastosOperativos, operativa, ingresosFinancieros,
                gastosFinancieros, otros, antesImpuestos, impuesto, antesImpuestos.subtract(impuesto));
    }

    /** Utilidad bruta ÷ ventas netas. */
    public BigDecimal margenBruto() {
        return sobreVentas(utilidadBruta);
    }

    /** Utilidad operativa ÷ ventas netas. */
    public BigDecimal margenOperativo() {
        return sobreVentas(utilidadOperativa);
    }

    /** Utilidad neta ÷ ventas netas (rentabilidad sobre ventas). */
    public BigDecimal margenNeto() {
        return sobreVentas(utilidadNeta);
    }

    /** Proporción de un monto sobre las ventas netas; {@code null} si no hubo ventas. */
    public BigDecimal sobreVentas(BigDecimal monto) {
        return ventasNetas.signum() == 0
                ? null
                : monto.divide(ventasNetas, Margenes.ESCALA_RATIO, RoundingMode.HALF_UP);
    }
}
