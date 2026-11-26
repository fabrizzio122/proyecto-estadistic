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

public record DatosDiagnosticoFGEM(SectorFGEM sectorFGEM, MonedaFGEM monedaFGEM, PeriodoFGEM periodoFGEM, int mesesPeriodoFGEM,
                               ResumenOperacionesFGEM operacionesFGEM, ResumenFinancieroFGEM finanzasFGEM,
                               ResumenOperacionesFGEM operacionesAnterioresFGEM,
                               List<YearMonth> mesesFGEM, double[] ventasMensualesFGEM, double[] utilidadMensualFGEM,
                               List<RentabilidadProductoFGEM> productosFGEM, SituacionPatrimonialFGEM patrimonioFGEM,
                               DatosAgroFGEM agroFGEM, ProyeccionCajaFGEM cajaFGEM) {

    public DatosDiagnosticoFGEM(SectorFGEM sectorFGEM, MonedaFGEM monedaFGEM, PeriodoFGEM periodoFGEM, int mesesPeriodoFGEM,
                            ResumenOperacionesFGEM operacionesFGEM, ResumenFinancieroFGEM finanzasFGEM,
                            ResumenOperacionesFGEM operacionesAnterioresFGEM,
                            List<YearMonth> mesesFGEM, double[] ventasMensualesFGEM, double[] utilidadMensualFGEM,
                            List<RentabilidadProductoFGEM> productosFGEM, SituacionPatrimonialFGEM patrimonioFGEM, DatosAgroFGEM agroFGEM) {
        this(sectorFGEM, monedaFGEM, periodoFGEM, mesesPeriodoFGEM, operacionesFGEM, finanzasFGEM, operacionesAnterioresFGEM, mesesFGEM,
                ventasMensualesFGEM, utilidadMensualFGEM, productosFGEM, patrimonioFGEM, agroFGEM, null);
    }

    public PeriodoFGEM periodoComparadoFGEM() {
        return comparadoConFGEM(periodoFGEM, agroFGEM != null);
    }

    public static PeriodoFGEM comparadoConFGEM(PeriodoFGEM periodoFGEM, boolean agropecuarioFGEM) {
        return agropecuarioFGEM
                ? new PeriodoFGEM(periodoFGEM.desdeFGEM().minusYears(1), java.time.YearMonth.from(periodoFGEM.hastaFGEM().minusYears(1)).atEndOfMonth())
                : periodoFGEM.anteriorFGEM();
    }

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

    public DatosDiagnosticoFGEM(SectorFGEM sectorFGEM, MonedaFGEM monedaFGEM, PeriodoFGEM periodoFGEM, int mesesPeriodoFGEM,
                            ResumenOperacionesFGEM operacionesFGEM, ResumenFinancieroFGEM finanzasFGEM,
                            ResumenOperacionesFGEM operacionesAnterioresFGEM,
                            List<YearMonth> mesesFGEM, double[] ventasMensualesFGEM, double[] utilidadMensualFGEM,
                            List<RentabilidadProductoFGEM> productosFGEM) {
        this(sectorFGEM, monedaFGEM, periodoFGEM, mesesPeriodoFGEM, operacionesFGEM, finanzasFGEM, operacionesAnterioresFGEM, mesesFGEM,
                ventasMensualesFGEM, utilidadMensualFGEM, productosFGEM, null, null);
    }
}
