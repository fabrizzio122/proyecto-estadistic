package com.businessanalyzer.analytics.prediccion;

import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.util.Montos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Explica la proyección en frases claras. */
public final class LecturaPrediccion {

    private LecturaPrediccion() {
    }

    public static List<String> generar(ResultadoPrediccion r, Moneda moneda) {
        List<String> frases = new ArrayList<>();
        Pronostico ventas = r.pronosticoVentas();
        if (!ventas.suficiente()) {
            frases.add("Se necesitan al menos " + Pronosticador.MINIMO_DATOS + " meses completos con ventas para proyectar "
                    + "(hay " + ventas.datos() + "). Sigue registrando tus ventas y vuelve más adelante.");
            return frases;
        }
        String meses = r.horizonte() == 1 ? "el próximo mes" : "los próximos " + r.horizonte() + " meses";
        frases.add("Si se mantiene el comportamiento reciente, las ventas de " + meses + " sumarían alrededor de "
                + dinero(ventas.total(), moneda) + " (entre " + dinero(ventas.totalInferior(), moneda) + " y "
                + dinero(ventas.totalSuperior(), moneda) + ").");

        double anteriores = r.ventasUltimosMeses();
        if (anteriores > 0 && r.mesesComparados() == r.horizonte()) {
            double cambio = ventas.total() / anteriores - 1;
            frases.add("Eso es " + porcentaje(Math.abs(cambio)) + (cambio >= 0 ? " más" : " menos")
                    + " que los últimos " + r.horizonte() + " meses (" + dinero(anteriores, moneda) + ").");
        }

        Pronostico.Evaluacion elegida = ventas.elegida();
        frases.add("El método más preciso con tus datos fue \"" + elegida.metodo().nombre() + "\": al probarlo con los meses "
                + "pasados se equivocó en promedio " + dinero(elegida.mae(), moneda) + " por mes ("
                + porcentaje(ventas.errorRelativo()) + " de una venta mensual típica).");
        if (ventas.confiabilidad() == Pronostico.Confiabilidad.BAJA) {
            frases.add("La confiabilidad es baja" + (elegida.mesesEvaluados() < 3 ? " porque aún hay pocos meses de historia" : "")
                    + ": tómalo como referencia y vuelve a calcularlo cada mes.");
        }

        Pronostico utilidad = r.pronosticoUtilidad();
        if (utilidad.suficiente()) {
            double total = utilidad.total();
            frases.add(total >= 0
                    ? "La utilidad neta proyectada es de " + dinero(total, moneda) + " en el mismo período."
                    : "Se proyectan pérdidas por " + dinero(-total, moneda) + ": conviene revisar precios y costos.");
        }
        frases.add("Los pronósticos suponen que las condiciones actuales se mantienen; no anticipan cambios bruscos "
                + "como nuevos competidores, crisis o campañas especiales.");
        return frases;
    }

    private static String dinero(double valor, Moneda moneda) {
        return Montos.formatear(BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP), moneda);
    }

    private static String porcentaje(double fraccion) {
        return Double.isNaN(fraccion) ? "—" : Montos.porcentaje(BigDecimal.valueOf(fraccion));
    }
}
