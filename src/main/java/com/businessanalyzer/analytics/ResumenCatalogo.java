package com.businessanalyzer.analytics;

import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.TipoItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;

/**
 * Indicadores del catálogo activo.
 *
 * @param margenPromedio promedio simple del margen sobre el precio (solo ítems con precio);
 *                       {@code null} si no hay ninguno
 */
public record ResumenCatalogo(int total, int productos, int servicios, BigDecimal margenPromedio,
                              int conMargenNegativo) {

    /** Considera solo los ítems activos. */
    public static ResumenCatalogo de(Collection<Producto> items) {
        List<Producto> activos = items.stream().filter(Producto::activo).toList();
        int productos = (int) activos.stream().filter(p -> p.tipo() == TipoItem.PRODUCTO).count();
        int negativos = (int) activos.stream().filter(p -> p.margen().negativo()).count();

        List<BigDecimal> margenes = activos.stream()
                .map(p -> p.margen().sobrePrecio())
                .filter(m -> m != null)
                .toList();
        BigDecimal promedio = margenes.isEmpty()
                ? null
                : margenes.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(margenes.size()), Margenes.ESCALA_RATIO, RoundingMode.HALF_UP);

        return new ResumenCatalogo(activos.size(), productos, activos.size() - productos, promedio, negativos);
    }
}
