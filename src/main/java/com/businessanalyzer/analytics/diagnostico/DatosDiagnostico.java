package com.businessanalyzer.analytics.diagnostico;

import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.RentabilidadProducto;
import com.businessanalyzer.analytics.ResumenFinanciero;
import com.businessanalyzer.analytics.ResumenOperaciones;
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
 */
public record DatosDiagnostico(Sector sector, Moneda moneda, Periodo periodo, int mesesPeriodo,
                               ResumenOperaciones operaciones, ResumenFinanciero finanzas,
                               ResumenOperaciones operacionesAnteriores,
                               List<YearMonth> meses, double[] ventasMensuales, double[] utilidadMensual,
                               List<RentabilidadProducto> productos) {

    public DatosDiagnostico {
        meses = List.copyOf(meses);
        productos = List.copyOf(productos);
        if (ventasMensuales.length != meses.size() || utilidadMensual.length != meses.size()) {
            throw new IllegalArgumentException("La serie mensual no coincide con los meses.");
        }
    }
}
