package com.businessanalyzer.analytics;

import com.businessanalyzer.model.GrupoFinancieroFGEM;

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
public record EstadoResultadosFGEM(
        BigDecimal ventasNetasFGEM,
        BigDecimal costoVentasFGEM,
        BigDecimal utilidadBrutaFGEM,
        BigDecimal gastosOperativosFGEM,
        BigDecimal utilidadOperativaFGEM,
        BigDecimal ingresosFinancierosFGEM,
        BigDecimal gastosFinancierosFGEM,
        BigDecimal otrosIngresosFGEM,
        BigDecimal utilidadAntesImpuestosFGEM,
        BigDecimal impuestoRentaFGEM,
        BigDecimal utilidadNetaFGEM) {

    public static EstadoResultadosFGEM deFGEM(ResumenOperacionesFGEM operacionesFGEM, ResumenFinancieroFGEM finanzasFGEM) {
        BigDecimal ventasFGEM = operacionesFGEM.ventasNetasFGEM();
        BigDecimal costoFGEM = operacionesFGEM.costoVentasFGEM();
        BigDecimal brutaFGEM = ventasFGEM.subtract(costoFGEM);
        BigDecimal gastosOperativosFGEM = finanzasFGEM.porGrupoFGEM(GrupoFinancieroFGEM.GASTO_OPERATIVO);
        BigDecimal operativaFGEM = brutaFGEM.subtract(gastosOperativosFGEM);
        BigDecimal ingresosFinancierosFGEM = finanzasFGEM.porGrupoFGEM(GrupoFinancieroFGEM.INGRESO_FINANCIERO);
        BigDecimal gastosFinancierosFGEM = finanzasFGEM.porGrupoFGEM(GrupoFinancieroFGEM.GASTO_FINANCIERO);
        BigDecimal otrosFGEM = finanzasFGEM.porGrupoFGEM(GrupoFinancieroFGEM.OTRO_INGRESO);
        BigDecimal antesImpuestosFGEM = operativaFGEM.add(ingresosFinancierosFGEM).subtract(gastosFinancierosFGEM).add(otrosFGEM);
        BigDecimal impuestoFGEM = finanzasFGEM.porGrupoFGEM(GrupoFinancieroFGEM.IMPUESTO_RENTA);
        return new EstadoResultadosFGEM(ventasFGEM, costoFGEM, brutaFGEM, gastosOperativosFGEM, operativaFGEM, ingresosFinancierosFGEM,
                gastosFinancierosFGEM, otrosFGEM, antesImpuestosFGEM, impuestoFGEM, antesImpuestosFGEM.subtract(impuestoFGEM));
    }

    /** Utilidad bruta ÷ ventas netas. */
    public BigDecimal margenBrutoFGEM() {
        return sobreVentasFGEM(utilidadBrutaFGEM);
    }

    /** Utilidad operativa ÷ ventas netas. */
    public BigDecimal margenOperativoFGEM() {
        return sobreVentasFGEM(utilidadOperativaFGEM);
    }

    /** Utilidad neta ÷ ventas netas (rentabilidad sobre ventas). */
    public BigDecimal margenNetoFGEM() {
        return sobreVentasFGEM(utilidadNetaFGEM);
    }

    /** Proporción de un monto sobre las ventas netas; {@code null} si no hubo ventas. */
    public BigDecimal sobreVentasFGEM(BigDecimal montoFGEM) {
        return ventasNetasFGEM.signum() == 0
                ? null
                : montoFGEM.divide(ventasNetasFGEM, MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);
    }
}
