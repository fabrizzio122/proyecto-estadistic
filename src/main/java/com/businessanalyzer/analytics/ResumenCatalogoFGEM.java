package com.businessanalyzer.analytics;

import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;

/**
 * Indicadores del catálogo activo.
 *
 * @param margenPromedioFGEM promedio simple del margen sobre el precio (solo ítems con precio);
 *                       {@code null} si no hay ninguno
 */
public record ResumenCatalogoFGEM(int totalFGEM, int productosFGEM, int serviciosFGEM, BigDecimal margenPromedioFGEM,
                              int conMargenNegativoFGEM) {

    /** Considera solo los ítems activos. */
    public static ResumenCatalogoFGEM deFGEM(Collection<ProductoFGEM> itemsFGEM) {
        List<ProductoFGEM> activosFGEM = itemsFGEM.stream().filter(ProductoFGEM::activoFGEM).toList();
        int productosFGEM = (int) activosFGEM.stream().filter(pFGEM -> pFGEM.tipoFGEM() == TipoItemFGEM.PRODUCTO).count();
        int negativosFGEM = (int) activosFGEM.stream().filter(pFGEM -> pFGEM.margenFGEM().negativoFGEM()).count();

        List<BigDecimal> margenesFGEM = activosFGEM.stream()
                .map(pFGEM -> pFGEM.margenFGEM().sobrePrecioFGEM())
                .filter(mFGEM -> mFGEM != null)
                .toList();
        BigDecimal promedioFGEM = margenesFGEM.isEmpty()
                ? null
                : margenesFGEM.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(margenesFGEM.size()), MargenesFGEM.ESCALA_RATIO_FGEM, RoundingMode.HALF_UP);

        return new ResumenCatalogoFGEM(activosFGEM.size(), productosFGEM, activosFGEM.size() - productosFGEM, promedioFGEM, negativosFGEM);
    }
}
