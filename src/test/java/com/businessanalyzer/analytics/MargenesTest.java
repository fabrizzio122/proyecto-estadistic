package com.businessanalyzer.analytics;

import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.TipoItem;
import com.businessanalyzer.model.UnidadMedida;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MargenesTest {

    @Test
    void calculaMargenSobrePrecioYSobreCosto() {
        Margenes.Margen margen = Margenes.calcular(new BigDecimal("75"), new BigDecimal("100"));

        assertEquals(0, new BigDecimal("25").compareTo(margen.unitario()));
        assertEquals(new BigDecimal("0.250000"), margen.sobrePrecio());
        assertEquals(new BigDecimal("0.333333"), margen.sobreCosto());
        assertFalse(margen.negativo());
    }

    @Test
    void detectaVentasBajoElCosto() {
        Margenes.Margen margen = Margenes.calcular(new BigDecimal("120"), new BigDecimal("100"));

        assertTrue(margen.negativo());
        assertEquals(new BigDecimal("-0.200000"), margen.sobrePrecio());
    }

    @Test
    void evitaDividirEntreCero() {
        Margenes.Margen sinCosto = Margenes.calcular(BigDecimal.ZERO, new BigDecimal("50"));
        assertEquals(new BigDecimal("1.000000"), sinCosto.sobrePrecio(), "Sin costo, todo el precio es margen");
        assertNull(sinCosto.sobreCosto());

        Margenes.Margen sinPrecio = Margenes.calcular(new BigDecimal("10"), BigDecimal.ZERO);
        assertNull(sinPrecio.sobrePrecio());
        assertTrue(sinPrecio.negativo());
    }

    @Test
    void resumeElCatalogoActivo() {
        List<Producto> items = List.of(
                item(TipoItem.PRODUCTO, "75", "100", true),    // 25 %
                item(TipoItem.PRODUCTO, "60", "50", true),     // -20 %
                item(TipoItem.SERVICIO, "0", "80", true),      // 100 %
                item(TipoItem.SERVICIO, "1", "0", true),       // sin precio: no entra al promedio
                item(TipoItem.PRODUCTO, "1", "100", false));   // archivado: no cuenta

        ResumenCatalogo resumen = ResumenCatalogo.de(items);

        assertEquals(4, resumen.total());
        assertEquals(2, resumen.productos());
        assertEquals(2, resumen.servicios());
        assertEquals(2, resumen.conMargenNegativo());
        assertEquals(new BigDecimal("0.350000"), resumen.margenPromedio(), "(0.25 - 0.20 + 1.00) / 3");
        assertNull(ResumenCatalogo.de(List.of()).margenPromedio());
    }

    private static Producto item(TipoItem tipo, String costo, String precio, boolean activo) {
        return new Producto(1, 1, null, null, tipo, "X", "Ítem", null, UnidadMedida.UNIDAD,
                new BigDecimal(costo), new BigDecimal(precio), activo, null, null);
    }
}
