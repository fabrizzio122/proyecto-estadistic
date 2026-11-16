package com.businessanalyzer.analytics.diagnostico;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM;
import com.businessanalyzer.analytics.ResumenFinancieroFGEM;
import com.businessanalyzer.analytics.ResumenOperacionesFGEM;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonialFGEM;
import com.businessanalyzer.analytics.tesoreria.ProyeccionCajaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.SectorFGEM;

import java.time.YearMonth;
import java.util.List;

/**
 * Información que necesita el diagnóstico (la reúne {@code DiagnosticoService}).
 *
 * @param periodoFGEM          meses completos que se evalúan (normalmente los tres últimos)
 * @param mesesFGEM            meses de la serie histórica, desde el primero con ventas
 * @param ventasMensualesFGEM  ventas netas de cada mes de {@code meses}
 * @param utilidadMensualFGEM  utilidad neta de cada mes de {@code meses}
 * @param productosFGEM        rentabilidad por producto en {@code periodo}
 * @param patrimonioFGEM       balance general a hoy; {@code null} si no se dispone (liquidez y
 *                         endeudamiento quedan sin evaluar)
 * @param agroFGEM             datos del hato, las campañas y las inspecciones; {@code null} si la empresa no
 *                         es agropecuaria (sus indicadores no se incluyen)
 * @param cajaFGEM             proyección del dinero disponible (tesorería); {@code null} si no se dispone
 */
public record DatosDiagnosticoFGEM(SectorFGEM sectorFGEM, MonedaFGEM monedaFGEM, PeriodoFGEM periodoFGEM, int mesesPeriodoFGEM,
                               ResumenOperacionesFGEM operacionesFGEM, ResumenFinancieroFGEM finanzasFGEM,
                               ResumenOperacionesFGEM operacionesAnterioresFGEM,
                               List<YearMonth> mesesFGEM, double[] ventasMensualesFGEM, double[] utilidadMensualFGEM,
                               List<RentabilidadProductoFGEM> productosFGEM, SituacionPatrimonialFGEM patrimonioFGEM,
                               DatosAgroFGEM agroFGEM, ProyeccionCajaFGEM cajaFGEM) {

    /** Sin proyección de caja. */
    public DatosDiagnosticoFGEM(SectorFGEM sectorFGEM, MonedaFGEM monedaFGEM, PeriodoFGEM periodoFGEM, int mesesPeriodoFGEM,
                            ResumenOperacionesFGEM operacionesFGEM, ResumenFinancieroFGEM finanzasFGEM,
                            ResumenOperacionesFGEM operacionesAnterioresFGEM,
                            List<YearMonth> mesesFGEM, double[] ventasMensualesFGEM, double[] utilidadMensualFGEM,
                            List<RentabilidadProductoFGEM> productosFGEM, SituacionPatrimonialFGEM patrimonioFGEM, DatosAgroFGEM agroFGEM) {
        this(sectorFGEM, monedaFGEM, periodoFGEM, mesesPeriodoFGEM, operacionesFGEM, finanzasFGEM, operacionesAnterioresFGEM, mesesFGEM,
                ventasMensualesFGEM, utilidadMensualFGEM, productosFGEM, patrimonioFGEM, agroFGEM, null);
    }

    /**
     * Período con el que se comparan las ventas: el trimestre anterior o, en un productor agropecuario,
     * el mismo período del año anterior (las cosechas y ventas de animales son estacionales).
     */
    public PeriodoFGEM periodoComparadoFGEM() {
        return comparadoConFGEM(periodoFGEM, agroFGEM != null);
    }

    public static PeriodoFGEM comparadoConFGEM(PeriodoFGEM periodoFGEM, boolean agropecuarioFGEM) {
        return agropecuarioFGEM
                ? new PeriodoFGEM(periodoFGEM.desdeFGEM().minusYears(1), java.time.YearMonth.from(periodoFGEM.hastaFGEM().minusYears(1)).atEndOfMonth())
                : periodoFGEM.anteriorFGEM();
    }

    /** Sin datos agropecuarios. */
    public DatosDiagnosticoFGEM(SectorFGEM sectorFGEM, MonedaFGEM monedaFGEM, PeriodoFGEM periodoFGEM, int mesesPeriodoFGEM,
                            ResumenOperacionesFGEM operacionesFGEM, ResumenFinancieroFGEM finanzasFGEM,
                            ResumenOperacionesFGEM operacionesAnterioresFGEM,
                            List<YearMonth> mesesFGEM, double[] ventasMensualesFGEM, double[] utilidadMensualFGEM,
                            List<RentabilidadProductoFGEM> productosFGEM, SituacionPatrimonialFGEM patrimonioFGEM) {
        this(sectorFGEM, monedaFGEM, periodoFGEM, mesesPeriodoFGEM, operacionesFGEM, finanzasFGEM, operacionesAnterioresFGEM, mesesFGEM,
                ventasMensualesFGEM, utilidadMensualFGEM, productosFGEM, patrimonioFGEM, null);
    }

    public DatosDiagnosticoFGEM {
        mesesFGEM = List.copyOf(mesesFGEM);
        productosFGEM = List.copyOf(productosFGEM);
        if (ventasMensualesFGEM.length != mesesFGEM.size() || utilidadMensualFGEM.length != mesesFGEM.size()) {
            throw new IllegalArgumentException("La serie mensual no coincide con los meses.");
        }
    }

    /** Sin datos de balance general. */
    public DatosDiagnosticoFGEM(SectorFGEM sectorFGEM, MonedaFGEM monedaFGEM, PeriodoFGEM periodoFGEM, int mesesPeriodoFGEM,
                            ResumenOperacionesFGEM operacionesFGEM, ResumenFinancieroFGEM finanzasFGEM,
                            ResumenOperacionesFGEM operacionesAnterioresFGEM,
                            List<YearMonth> mesesFGEM, double[] ventasMensualesFGEM, double[] utilidadMensualFGEM,
                            List<RentabilidadProductoFGEM> productosFGEM) {
        this(sectorFGEM, monedaFGEM, periodoFGEM, mesesPeriodoFGEM, operacionesFGEM, finanzasFGEM, operacionesAnterioresFGEM, mesesFGEM,
                ventasMensualesFGEM, utilidadMensualFGEM, productosFGEM, null, null);
    }
}
