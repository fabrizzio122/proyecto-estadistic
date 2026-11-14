package com.businessanalyzer.informes;

import com.businessanalyzer.analytics.AnalisisFinanciero;
import com.businessanalyzer.analytics.diagnostico.Diagnostico;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.MovimientoFinanciero;
import com.businessanalyzer.model.Operacion;

import java.util.List;

/**
 * Información con la que se redacta un informe (la reúne {@code InformeService}).
 * Las listas solo se cargan para el libro de Excel y el diagnóstico solo para el informe ejecutivo;
 * en los demás casos llegan vacías o en {@code null}.
 */
public record DatosInforme(Empresa empresa, AnalisisFinanciero analisis, Diagnostico diagnostico,
                           List<Operacion> ventas, List<Operacion> compras, List<MovimientoFinanciero> movimientos) {

    public DatosInforme {
        ventas = ventas == null ? List.of() : List.copyOf(ventas);
        compras = compras == null ? List.of() : List.copyOf(compras);
        movimientos = movimientos == null ? List.of() : List.copyOf(movimientos);
    }
}
