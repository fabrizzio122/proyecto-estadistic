package com.businessanalyzer.analytics.sugerencias;

import com.businessanalyzer.analytics.sugerencias.AvisosSugerenciasFGEM.AvisoFGEM;
import com.businessanalyzer.analytics.sugerencias.AvisosSugerenciasFGEM.GravedadFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.MotivoPrecioFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.NotaPrecioFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.PrecioFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.ReposicionFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.ResultadoFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.SinMovimientoFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.SituacionFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.UrgenciaFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvisosSugerenciasTestFGEM {

    @Test
    void ordenaDeLoMasALoMenosUrgenteYSoloCuentaLoPrioritarioFGEM() {
        List<ReposicionFGEM> reposicionFGEM = List.of(
                reponerFGEM(1, "Arroz", UrgenciaFGEM.AGOTADO), reponerFGEM(2, "Azúcar", UrgenciaFGEM.AGOTADO),
                reponerFGEM(3, "Fideos", UrgenciaFGEM.AGOTADO), reponerFGEM(4, "Aceite", UrgenciaFGEM.AGOTADO),
                reponerFGEM(5, "Leche", UrgenciaFGEM.URGENTE), reponerFGEM(6, "Atún", UrgenciaFGEM.PRONTO));
        List<PrecioFGEM> preciosFGEM = List.of(
                precioFGEM(7, "Agua", MotivoPrecioFGEM.PERDIDA, null),
                precioFGEM(8, "Galletas", MotivoPrecioFGEM.PERDIDA, NotaPrecioFGEM.SIN_VENTAS),   // casi no se vende: no es urgente
                precioFGEM(9, "Gaseosa", MotivoPrecioFGEM.MARGEN_BAJO, null));                    // margen bajo: no es urgente
        List<SinMovimientoFGEM> paradosFGEM = List.of(
                new SinMovimientoFGEM(productoFGEM(10, "Yogur"), SituacionFGEM.PERECIBLE_EN_RIESGO, BigDecimal.TEN, BigDecimal.TEN, null),
                new SinMovimientoFGEM(productoFGEM(11, "Velas"), SituacionFGEM.SIN_VENTAS, BigDecimal.TEN, BigDecimal.TEN, null));
        ResultadoFGEM rFGEM = new ResultadoFGEM(60, new BigDecimal("0.20"), preciosFGEM, reposicionFGEM, paradosFGEM, BigDecimal.TEN, null);

        List<AvisoFGEM> avisosFGEM = AvisosSugerenciasFGEM.avisosFGEM(rFGEM);

        assertEquals(List.of("Agotados", "Perecibles por vencer", "Se venden con pérdida", "Se agotan esta semana"),
                avisosFGEM.stream().map(AvisoFGEM::tituloFGEM).toList());
        assertEquals(List.of(4, 1, 1, 1), avisosFGEM.stream().map(AvisoFGEM::cantidadFGEM).toList());
        assertEquals("Repón: Arroz, Azúcar, Fideos y 1 más.", avisosFGEM.get(0).detalleFGEM());
        assertEquals("Sube el precio: Agua.", avisosFGEM.get(2).detalleFGEM());
        assertEquals(GravedadFGEM.MEDIA, avisosFGEM.get(3).gravedadFGEM());
    }

    @Test
    void sinNadaUrgenteNoHayAvisosFGEM() {
        ResultadoFGEM rFGEM = new ResultadoFGEM(60, new BigDecimal("0.20"),
                List.of(precioFGEM(1, "Gaseosa", MotivoPrecioFGEM.MARGEN_BAJO, null)),
                List.of(reponerFGEM(2, "Atún", UrgenciaFGEM.PRONTO)), List.of(), BigDecimal.TEN, null);
        assertTrue(AvisosSugerenciasFGEM.avisosFGEM(rFGEM).isEmpty());
    }

    private static ProductoFGEM productoFGEM(long idFGEM, String nombreFGEM) {
        return new ProductoFGEM(idFGEM, 1, null, null, TipoItemFGEM.PRODUCTO, "P-" + idFGEM, nombreFGEM, null, UnidadMedidaFGEM.UNIDAD,
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, true, null, null);
    }

    private static ReposicionFGEM reponerFGEM(long idFGEM, String nombreFGEM, UrgenciaFGEM urgenciaFGEM) {
        return new ReposicionFGEM(productoFGEM(idFGEM, nombreFGEM), urgenciaFGEM, BigDecimal.ONE, BigDecimal.TEN, 60, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, false);
    }

    private static PrecioFGEM precioFGEM(long idFGEM, String nombreFGEM, MotivoPrecioFGEM motivoFGEM, NotaPrecioFGEM notaFGEM) {
        return new PrecioFGEM(productoFGEM(idFGEM, nombreFGEM), motivoFGEM, BigDecimal.ZERO, BigDecimal.TEN, new BigDecimal("0.20"),
                BigDecimal.TEN, BigDecimal.ONE, notaFGEM);
    }
}
