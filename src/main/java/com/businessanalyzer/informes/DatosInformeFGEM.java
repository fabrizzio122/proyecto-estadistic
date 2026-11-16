package com.businessanalyzer.informes;

import com.businessanalyzer.analytics.AnalisisFinancieroFGEM;
import com.businessanalyzer.analytics.diagnostico.DiagnosticoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MovimientoFinancieroFGEM;
import com.businessanalyzer.model.OperacionFGEM;

import java.util.List;

/**
 * Información con la que se redacta un informe (la reúne {@code InformeService}).
 * Las listas solo se cargan para el libro de Excel y el diagnóstico solo para el informe ejecutivo;
 * en los demás casos llegan vacías o en {@code null}.
 */
public record DatosInformeFGEM(EmpresaFGEM empresaFGEM, AnalisisFinancieroFGEM analisisFGEM, DiagnosticoFGEM diagnosticoFGEM,
                           List<OperacionFGEM> ventasFGEM, List<OperacionFGEM> comprasFGEM, List<MovimientoFinancieroFGEM> movimientosFGEM) {

    public DatosInformeFGEM {
        ventasFGEM = ventasFGEM == null ? List.of() : List.copyOf(ventasFGEM);
        comprasFGEM = comprasFGEM == null ? List.of() : List.copyOf(comprasFGEM);
        movimientosFGEM = movimientosFGEM == null ? List.of() : List.copyOf(movimientosFGEM);
    }
}
