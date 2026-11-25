package com.businessanalyzer.analytics.sugerencias;

import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.MotivoPrecioFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.PrecioFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.ReposicionFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.ResultadoFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.SinMovimientoFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.SituacionFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.UrgenciaFGEM;
import com.businessanalyzer.model.ProductoFGEM;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Lo más urgente de las sugerencias, en pocas líneas, para el Dashboard: productos agotados, perecibles que se
 * vencerán, productos que se venden con pérdida y productos que se agotan esta semana. Solo cuenta lo que pide
 * actuar ya: los precios bajos de productos que casi no se venden y la venta lenta quedan para el módulo.
 * Es cálculo puro.
 */
public final class AvisosSugerenciasFGEM {

    /** Nombres que se muestran en el detalle de cada aviso. */
    public static final int MAX_NOMBRES_FGEM = 3;

    private AvisosSugerenciasFGEM() {
    }

    /** {@code GRAVE}: hay que actuar hoy; {@code MEDIA}: esta semana. */
    public enum GravedadFGEM { GRAVE, MEDIA }

    /**
     * @param tituloFGEM   qué pasa («Agotados»)
     * @param cantidadFGEM cuántos productos
     * @param detalleFGEM  los primeros nombres y qué hacer
     */
    public record AvisoFGEM(GravedadFGEM gravedadFGEM, String tituloFGEM, int cantidadFGEM, String detalleFGEM) {
    }

    /** Avisos de lo más a lo menos urgente; lista vacía si no hay nada urgente. */
    public static List<AvisoFGEM> avisosFGEM(ResultadoFGEM rFGEM) {
        List<AvisoFGEM> avisosFGEM = new ArrayList<>();
        agregarFGEM(avisosFGEM, GravedadFGEM.GRAVE, "Agotados", "Repón: ", rFGEM.reposicionFGEM(),
                rpFGEM -> rpFGEM.urgenciaFGEM() == UrgenciaFGEM.AGOTADO, ReposicionFGEM::productoFGEM);
        agregarFGEM(avisosFGEM, GravedadFGEM.GRAVE, "Perecibles por vencer", "Ofrécelos en promoción: ", rFGEM.sinMovimientoFGEM(),
                sFGEM -> sFGEM.situacionFGEM() == SituacionFGEM.PERECIBLE_EN_RIESGO, SinMovimientoFGEM::productoFGEM);
        agregarFGEM(avisosFGEM, GravedadFGEM.GRAVE, "Se venden con pérdida", "Sube el precio: ", rFGEM.preciosFGEM(),
                pFGEM -> pFGEM.motivoFGEM() == MotivoPrecioFGEM.PERDIDA && pFGEM.notaFGEM() == null, PrecioFGEM::productoFGEM);
        agregarFGEM(avisosFGEM, GravedadFGEM.MEDIA, "Se agotan esta semana", "Compra pronto: ", rFGEM.reposicionFGEM(),
                rpFGEM -> rpFGEM.urgenciaFGEM() == UrgenciaFGEM.URGENTE, ReposicionFGEM::productoFGEM);
        return avisosFGEM;
    }

    private static <T> void agregarFGEM(List<AvisoFGEM> avisosFGEM, GravedadFGEM gravedadFGEM, String tituloFGEM, String accionFGEM, List<T> itemsFGEM,
                                        Predicate<T> filtroFGEM, Function<T, ProductoFGEM> productoFGEM) {
        List<String> nombresFGEM = itemsFGEM.stream().filter(filtroFGEM).map(iFGEM -> productoFGEM.apply(iFGEM).nombreFGEM()).toList();
        if (nombresFGEM.isEmpty()) {
            return;
        }
        String detalleFGEM = accionFGEM + String.join(", ", nombresFGEM.subList(0, Math.min(MAX_NOMBRES_FGEM, nombresFGEM.size())))
                + (nombresFGEM.size() > MAX_NOMBRES_FGEM ? " y " + (nombresFGEM.size() - MAX_NOMBRES_FGEM) + " más" : "") + ".";
        avisosFGEM.add(new AvisoFGEM(gravedadFGEM, tituloFGEM, nombresFGEM.size(), detalleFGEM));
    }
}
