package com.businessanalyzer.analytics;

import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MargenesTestFGEM {

    @Test
    void calculaMargenSobrePrecioYSobreCostoFGEM() {
        MargenesFGEM.MargenFGEM margenFGEM = MargenesFGEM.calcularFGEM(new BigDecimal("75"), new BigDecimal("100"));

        assertEquals(0, new BigDecimal("25").compareTo(margenFGEM.unitarioFGEM()));
        assertEquals(new BigDecimal("0.250000"), margenFGEM.sobrePrecioFGEM());
        assertEquals(new BigDecimal("0.333333"), margenFGEM.sobreCostoFGEM());
        assertFalse(margenFGEM.negativoFGEM());
    }

    @Test
    void detectaVentasBajoElCostoFGEM() {
        MargenesFGEM.MargenFGEM margenFGEM = MargenesFGEM.calcularFGEM(new BigDecimal("120"), new BigDecimal("100"));

        assertTrue(margenFGEM.negativoFGEM());
        assertEquals(new BigDecimal("-0.200000"), margenFGEM.sobrePrecioFGEM());
    }

    @Test
    void evitaDividirEntreCeroFGEM() {
        MargenesFGEM.MargenFGEM sinCostoFGEM = MargenesFGEM.calcularFGEM(BigDecimal.ZERO, new BigDecimal("50"));
        assertEquals(new BigDecimal("1.000000"), sinCostoFGEM.sobrePrecioFGEM(), "Sin costo, todo el precio es margen");
        assertNull(sinCostoFGEM.sobreCostoFGEM());

        MargenesFGEM.MargenFGEM sinPrecioFGEM = MargenesFGEM.calcularFGEM(new BigDecimal("10"), BigDecimal.ZERO);
        assertNull(sinPrecioFGEM.sobrePrecioFGEM());
        assertTrue(sinPrecioFGEM.negativoFGEM());
    }

    @Test
    void resumeElCatalogoActivoFGEM() {
        List<ProductoFGEM> itemsFGEM = List.of(
                itemFGEM(TipoItemFGEM.PRODUCTO, "75", "100", true),
                itemFGEM(TipoItemFGEM.PRODUCTO, "60", "50", true),
                itemFGEM(TipoItemFGEM.SERVICIO, "0", "80", true),
                itemFGEM(TipoItemFGEM.SERVICIO, "1", "0", true),
                itemFGEM(TipoItemFGEM.PRODUCTO, "1", "100", false));

        ResumenCatalogoFGEM resumenFGEM = ResumenCatalogoFGEM.deFGEM(itemsFGEM);

        assertEquals(4, resumenFGEM.totalFGEM());
        assertEquals(2, resumenFGEM.productosFGEM());
        assertEquals(2, resumenFGEM.serviciosFGEM());
        assertEquals(2, resumenFGEM.conMargenNegativoFGEM());
        assertEquals(new BigDecimal("0.350000"), resumenFGEM.margenPromedioFGEM(), "(0.25 - 0.20 + 1.00) / 3");
        assertNull(ResumenCatalogoFGEM.deFGEM(List.of()).margenPromedioFGEM());
    }

    private static ProductoFGEM itemFGEM(TipoItemFGEM tipoFGEM, String costoFGEM, String precioFGEM, boolean activoFGEM) {
        return new ProductoFGEM(1, 1, null, null, tipoFGEM, "X", "Ítem", null, UnidadMedidaFGEM.UNIDAD,
                new BigDecimal(costoFGEM), new BigDecimal(precioFGEM), null, activoFGEM, null, null);
    }
}
