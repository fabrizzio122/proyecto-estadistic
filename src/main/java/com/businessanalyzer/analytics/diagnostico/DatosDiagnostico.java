package com.businessanalyzer.analytics.diagnostico;

import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.RentabilidadProducto;
import com.businessanalyzer.analytics.ResumenFinanciero;
import com.businessanalyzer.analytics.ResumenOperaciones;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonial;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Sector;

import java.time.YearMonth;
import java.util.List;

/**
 * Información que necesita el diagnóstico (la reúne {@code DiagnosticoService}).
 *
 * @param periodo          meses completos que se evalúan (normalmente los tres últimos)
 * @param meses            meses de la serie histórica, desde el primero con ventas
 * @param ventasMensuales  ventas netas de cada mes de {@code meses}
 * @param utilidadMensual  utilidad neta de cada mes de {@code meses}
 * @param productos        rentabilidad por producto en {@code periodo}
 * @param patrimonio       balance general a hoy; {@code null} si no se dispone (liquidez y
 *                         endeudamiento quedan sin evaluar)
 * @param agro             datos del hato, las campañas y las inspecciones; {@code null} si la empresa no
 *                         es agropecuaria (sus indicadores no se incluyen)
 */
public record DatosDiagnostico(Sector sector, Moneda moneda, Periodo periodo, int mesesPeriodo,
                               ResumenOperaciones operaciones, ResumenFinanciero finanzas,
                               ResumenOperaciones operacionesAnteriores,
                               List<YearMonth> meses, double[] ventasMensuales, double[] utilidadMensual,
                               List<RentabilidadProducto> productos, SituacionPatrimonial patrimonio,
                               DatosAgro agro) {

    /**
     * Período con el que se comparan las ventas: el trimestre anterior o, en un productor agropecuario,
     * el mismo período del año anterior (las cosechas y ventas de animales son estacionales).
     */
    public Periodo periodoComparado() {
        return comparadoCon(periodo, agro != null);
    }

    public static Periodo comparadoCon(Periodo periodo, boolean agropecuario) {
        return agropecuario
                ? new Periodo(periodo.desde().minusYears(1), java.time.YearMonth.from(periodo.hasta().minusYears(1)).atEndOfMonth())
                : periodo.anterior();
    }

    /** Sin datos agropecuarios. */
    public DatosDiagnostico(Sector sector, Moneda moneda, Periodo periodo, int mesesPeriodo,
                            ResumenOperaciones operaciones, ResumenFinanciero finanzas,
                            ResumenOperaciones operacionesAnteriores,
                            List<YearMonth> meses, double[] ventasMensuales, double[] utilidadMensual,
                            List<RentabilidadProducto> productos, SituacionPatrimonial patrimonio) {
        this(sector, moneda, periodo, mesesPeriodo, operaciones, finanzas, operacionesAnteriores, meses,
                ventasMensuales, utilidadMensual, productos, patrimonio, null);
    }

    public DatosDiagnostico {
        meses = List.copyOf(meses);
        productos = List.copyOf(productos);
        if (ventasMensuales.length != meses.size() || utilidadMensual.length != meses.size()) {
            throw new IllegalArgumentException("La serie mensual no coincide con los meses.");
        }
    }

    /** Sin datos de balance general. */
    public DatosDiagnostico(Sector sector, Moneda moneda, Periodo periodo, int mesesPeriodo,
                            ResumenOperaciones operaciones, ResumenFinanciero finanzas,
                            ResumenOperaciones operacionesAnteriores,
                            List<YearMonth> meses, double[] ventasMensuales, double[] utilidadMensual,
                            List<RentabilidadProducto> productos) {
        this(sector, moneda, periodo, mesesPeriodo, operaciones, finanzas, operacionesAnteriores, meses,
                ventasMensuales, utilidadMensual, productos, null, null);
    }
}
