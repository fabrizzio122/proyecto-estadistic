package com.businessanalyzer.analytics.prediccion;

import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class LecturaPrediccionFGEM {

    private LecturaPrediccionFGEM() {
    }

    public static List<String> generarFGEM(ResultadoPrediccionFGEM rFGEM, MonedaFGEM monedaFGEM) {
        List<String> frasesFGEM = new ArrayList<>();
        PronosticoFGEM ventasFGEM = rFGEM.pronosticoVentasFGEM();
        if (!ventasFGEM.suficienteFGEM()) {
            frasesFGEM.add("Se necesitan al menos " + PronosticadorFGEM.MINIMO_DATOS_FGEM + " meses completos con ventas para proyectar "
                    + "(hay " + ventasFGEM.datosFGEM() + "). Sigue registrando tus ventas y vuelve más adelante.");
            return frasesFGEM;
        }
        String mesesFGEM = rFGEM.horizonteFGEM() == 1 ? "el próximo mes" : "los próximos " + rFGEM.horizonteFGEM() + " meses";
        frasesFGEM.add("Si se mantiene el comportamiento reciente, las ventas de " + mesesFGEM + " sumarían alrededor de "
                + dineroFGEM(ventasFGEM.totalFGEM(), monedaFGEM) + " (entre " + dineroFGEM(ventasFGEM.totalInferiorFGEM(), monedaFGEM) + " y "
                + dineroFGEM(ventasFGEM.totalSuperiorFGEM(), monedaFGEM) + ").");

        double anterioresFGEM = rFGEM.ventasUltimosMesesFGEM();
        if (anterioresFGEM > 0 && rFGEM.mesesComparadosFGEM() == rFGEM.horizonteFGEM()) {
            double cambioFGEM = ventasFGEM.totalFGEM() / anterioresFGEM - 1;
            frasesFGEM.add("Eso es " + porcentajeFGEM(Math.abs(cambioFGEM)) + (cambioFGEM >= 0 ? " más" : " menos")
                    + " que los últimos " + rFGEM.horizonteFGEM() + " meses (" + dineroFGEM(anterioresFGEM, monedaFGEM) + ").");
        }

        PronosticoFGEM.EvaluacionFGEM elegidaFGEM = ventasFGEM.elegidaFGEM();
        frasesFGEM.add("El método más preciso con tus datos fue \"" + elegidaFGEM.metodoFGEM().nombreFGEM() + "\": al probarlo con los meses "
                + "pasados se equivocó en promedio " + dineroFGEM(elegidaFGEM.maeFGEM(), monedaFGEM) + " por mes ("
                + porcentajeFGEM(ventasFGEM.errorRelativoFGEM()) + " de una venta mensual típica).");
        if (ventasFGEM.confiabilidadFGEM() == PronosticoFGEM.ConfiabilidadFGEM.BAJA) {
            frasesFGEM.add("La confiabilidad es baja" + (elegidaFGEM.mesesEvaluadosFGEM() < 3 ? " porque aún hay pocos meses de historia" : "")
                    + ": tómalo como referencia y vuelve a calcularlo cada mes.");
        }

        PronosticoFGEM utilidadFGEM = rFGEM.pronosticoUtilidadFGEM();
        if (utilidadFGEM.suficienteFGEM()) {
            double totalFGEM = utilidadFGEM.totalFGEM();
            frasesFGEM.add(totalFGEM >= 0
                    ? "La utilidad neta proyectada es de " + dineroFGEM(totalFGEM, monedaFGEM) + " en el mismo período."
                    : "Se proyectan pérdidas por " + dineroFGEM(-totalFGEM, monedaFGEM) + ": conviene revisar precios y costos.");
        }
        frasesFGEM.add("Los pronósticos suponen que las condiciones actuales se mantienen; no anticipan cambios bruscos "
                + "como nuevos competidores, crisis o campañas especiales.");
        return frasesFGEM;
    }

    private static String dineroFGEM(double valorFGEM, MonedaFGEM monedaFGEM) {
        return MontosFGEM.formatearFGEM(BigDecimal.valueOf(valorFGEM).setScale(2, RoundingMode.HALF_UP), monedaFGEM);
    }

    private static String porcentajeFGEM(double fraccionFGEM) {
        return Double.isNaN(fraccionFGEM) ? "—" : MontosFGEM.porcentajeFGEM(BigDecimal.valueOf(fraccionFGEM));
    }
}
