package com.businessanalyzer.analytics.estadistica;

import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Traduce los resultados estadísticos a frases claras para quien no es especialista. */
public final class LecturaEstadisticaFGEM {

    private static final Locale ESPANOL_FGEM = Locale.forLanguageTag("es");

    private LecturaEstadisticaFGEM() {
    }

    public static List<String> resumenFGEM(AnalisisEstadisticoFGEM aFGEM, MonedaFGEM monedaFGEM) {
        List<String> frasesFGEM = new ArrayList<>();
        EstadisticaDescriptivaFGEM tFGEM = aFGEM.ticketFGEM();
        if (tFGEM.vaciaFGEM()) {
            frasesFGEM.add("No hay ventas registradas en el período.");
            return frasesFGEM;
        }
        frasesFGEM.add("Se registraron " + tFGEM.nFGEM() + (tFGEM.nFGEM() == 1 ? " venta" : " ventas") + ". La venta típica (mediana) fue de "
                + dineroFGEM(tFGEM.medianaFGEM(), monedaFGEM) + " y el promedio de " + dineroFGEM(tFGEM.mediaFGEM(), monedaFGEM) + ".");
        if (tFGEM.nFGEM() >= 4) {
            frasesFGEM.add("La mitad de las ventas estuvo entre " + dineroFGEM(tFGEM.q1FGEM(), monedaFGEM) + " y " + dineroFGEM(tFGEM.q3FGEM(), monedaFGEM) + ".");
        }
        if (!Double.isNaN(tFGEM.asimetriaFGEM()) && tFGEM.asimetriaFGEM() > 1) {
            frasesFGEM.add("Unas pocas ventas grandes elevan el promedio por encima de la venta típica.");
        }
        double cvFGEM = aFGEM.ventasDiariasFGEM().coeficienteVariacionFGEM();
        if (!Double.isNaN(cvFGEM)) {
            String nivelFGEM = cvFGEM < 0.3 ? "estables" : cvFGEM < 0.6 ? "moderadamente variables" : "muy variables: hay días fuertes y días flojos";
            frasesFGEM.add("Las ventas diarias son " + nivelFGEM + " (coeficiente de variación " + porcentajeFGEM(cvFGEM) + ").");
        }
        return frasesFGEM;
    }

    public static List<String> tendenciaFGEM(AnalisisEstadisticoFGEM aFGEM, MonedaFGEM monedaFGEM) {
        List<String> frasesFGEM = new ArrayList<>();
        TendenciaFGEM tFGEM = aFGEM.tendenciaVentasFGEM();
        switch (tFGEM.direccionFGEM()) {
            case INSUFICIENTE -> frasesFGEM.add("Se necesitan al menos " + TendenciaFGEM.MINIMO_PUNTOS_FGEM
                    + " meses completos con ventas para medir la tendencia (hay " + tFGEM.nFGEM() + ").");
            case CRECIENTE, DECRECIENTE -> {
                boolean creceFGEM = tFGEM.direccionFGEM() == TendenciaFGEM.DireccionFGEM.CRECIENTE;
                frasesFGEM.add("Las ventas " + (creceFGEM ? "crecen" : "caen") + " en promedio " + dineroFGEM(Math.abs(tFGEM.pendienteFGEM()), monedaFGEM)
                        + " por mes (" + (creceFGEM ? "+" : "") + porcentajeFGEM(tFGEM.crecimientoMensualFGEM()) + " mensual).");
                frasesFGEM.add("La tendencia es estadísticamente significativa (p = " + valorPFGEM(tFGEM.significanciaFGEM())
                        + "): es poco probable que se deba al azar.");
            }
            case ESTABLE -> frasesFGEM.add("No hay una tendencia clara: las subidas y bajadas mensuales podrían deberse al azar (p = "
                    + valorPFGEM(tFGEM.significanciaFGEM()) + ").");
        }
        if (tFGEM.direccionFGEM() != TendenciaFGEM.DireccionFGEM.INSUFICIENTE && !Double.isNaN(tFGEM.r2FGEM())) {
            frasesFGEM.add("La recta de tendencia explica el " + porcentajeFGEM(tFGEM.r2FGEM()) + " de la variación de las ventas mensuales"
                    + " (R² = " + String.format(Locale.US, "%.2f", tFGEM.r2FGEM()) + ").");
        }
        if (aFGEM.mesActualExcluidoFGEM()) {
            frasesFGEM.add("El mes en curso no se incluye porque aún no termina.");
        }
        return frasesFGEM;
    }

    public static List<String> estacionalidadFGEM(AnalisisEstadisticoFGEM aFGEM, MonedaFGEM monedaFGEM) {
        List<String> frasesFGEM = new ArrayList<>();
        EstacionalidadSemanalFGEM eFGEM = aFGEM.estacionalidadFGEM();
        DayOfWeek mejorFGEM = eFGEM.mejorDiaFGEM();
        DayOfWeek peorFGEM = eFGEM.peorDiaFGEM();
        if (mejorFGEM == null || Double.isNaN(eFGEM.promedioGeneralFGEM()) || eFGEM.promedioGeneralFGEM() == 0) {
            frasesFGEM.add("No hay ventas en el período para comparar los días de la semana.");
            return frasesFGEM;
        }
        frasesFGEM.add("En un día promedio se vende " + dineroFGEM(eFGEM.promedioGeneralFGEM(), monedaFGEM) + ".");
        frasesFGEM.add("El mejor día es el " + nombreFGEM(mejorFGEM) + ": se vende " + diferenciaFGEM(eFGEM.indiceFGEM(mejorFGEM)) + " que un día promedio.");
        if (peorFGEM != mejorFGEM) {
            frasesFGEM.add("El día más flojo es el " + nombreFGEM(peorFGEM) + " (" + diferenciaFGEM(eFGEM.indiceFGEM(peorFGEM)) + " que un día promedio).");
        }
        frasesFGEM.add("Úsalo para planificar personal, compras y promociones.");
        return frasesFGEM;
    }

    /** Intervalo de confianza de la venta diaria promedio. */
    public static List<String> intervaloFGEM(AnalisisEstadisticoFGEM aFGEM, MonedaFGEM monedaFGEM) {
        InferenciaFGEM.IntervaloConfianzaFGEM iFGEM = aFGEM.intervaloDiarioFGEM();
        if (!iFGEM.calculadoFGEM() || aFGEM.ticketFGEM().vaciaFGEM()) {
            return List.of();
        }
        return List.of("Con 95 % de confianza, la venta diaria promedio del negocio está entre "
                + dineroFGEM(Math.max(0, iFGEM.inferiorFGEM()), monedaFGEM) + " y " + dineroFGEM(iFGEM.superiorFGEM(), monedaFGEM)
                + " (intervalo t de Student con " + iFGEM.nFGEM() + " días). Mientras más días, más angosto el intervalo.");
    }

    /** ANOVA por día de la semana: confirma (o no) que la diferencia entre días es real. */
    public static List<String> anovaFGEM(AnalisisEstadisticoFGEM aFGEM) {
        InferenciaFGEM.AnovaFGEM anFGEM = aFGEM.anovaDiasFGEM();
        if (!anFGEM.calculadoFGEM()) {
            return List.of("Para la prueba ANOVA se necesitan al menos dos semanas de datos.");
        }
        String pruebaFGEM = "ANOVA de un factor: F(" + anFGEM.glEntreFGEM() + ", " + anFGEM.glDentroFGEM() + ") = "
                + String.format(Locale.US, "%.2f", anFGEM.fFGEM()) + ", p = " + valorPFGEM(anFGEM.pFGEM()) + ".";
        if (anFGEM.significativoFGEM()) {
            return List.of(pruebaFGEM + " La diferencia entre los días es estadísticamente significativa: se rechaza que todos los días "
                    + "vendan lo mismo, así que el patrón semanal es real.",
                    "El día de la semana explica el " + porcentajeFGEM(anFGEM.etaCuadradoFGEM()) + " de la variación de las ventas diarias (η²).");
        }
        return List.of(pruebaFGEM + " La diferencia entre los días no es significativa: no se puede descartar que se deba al azar, "
                + "así que no conviene planificar solo con ella.");
    }

    /** Prueba t: si el cambio frente al período anterior es real. */
    public static List<String> comparacionFGEM(AnalisisEstadisticoFGEM aFGEM, MonedaFGEM monedaFGEM) {
        InferenciaFGEM.ComparacionFGEM cFGEM = aFGEM.comparacionFGEM();
        if (!cFGEM.calculadoFGEM()) {
            return List.of("No hay ventas suficientes en el período anterior (" + aFGEM.periodoAnteriorFGEM().descripcionFGEM()
                    + ") para compararlo con una prueba t.");
        }
        boolean subeFGEM = cFGEM.mediaActualFGEM() >= cFGEM.mediaAnteriorFGEM();
        String cambioFGEM = "La venta diaria promedio " + (subeFGEM ? "subió" : "bajó") + " de " + dineroFGEM(cFGEM.mediaAnteriorFGEM(), monedaFGEM)
                + " a " + dineroFGEM(cFGEM.mediaActualFGEM(), monedaFGEM)
                + (Double.isNaN(cFGEM.variacionFGEM()) ? "" : " (" + (subeFGEM ? "+" : "−") + porcentajeFGEM(Math.abs(cFGEM.variacionFGEM())) + ")")
                + " frente a " + aFGEM.periodoAnteriorFGEM().descripcionFGEM() + ".";
        String pruebaFGEM = " Prueba t de Welch: t = " + String.format(Locale.US, "%.2f", cFGEM.tFGEM()) + ", p = " + valorPFGEM(cFGEM.pFGEM()) + ".";
        return List.of(cambioFGEM + pruebaFGEM, cFGEM.significativoFGEM()
                ? "El cambio es estadísticamente significativo: es poco probable que se deba al azar."
                : "El cambio no es significativo: podría deberse a la variación normal de las ventas.");
    }

    public static List<String> abcFGEM(AnalisisEstadisticoFGEM aFGEM) {
        List<String> frasesFGEM = new ArrayList<>();
        AnalisisAbcFGEM abcFGEM = aFGEM.abcFGEM();
        if (abcFGEM.itemsFGEM().isEmpty()) {
            frasesFGEM.add("No hay ventas en el período para clasificar los productos.");
            return frasesFGEM;
        }
        int totalFGEM = abcFGEM.itemsFGEM().size();
        long cantidadAFGEM = abcFGEM.cantidadFGEM(AnalisisAbcFGEM.ClaseFGEM.A);
        frasesFGEM.add(cantidadAFGEM + " de " + totalFGEM + " productos (" + porcentajeFGEM((double) cantidadAFGEM / totalFGEM) + ") generan el "
                + MontosFGEM.porcentajeFGEM(abcFGEM.participacionFGEM(AnalisisAbcFGEM.ClaseFGEM.A)) + " de las ventas: son la clase A, los que nunca deben faltar.");
        long cantidadCFGEM = abcFGEM.cantidadFGEM(AnalisisAbcFGEM.ClaseFGEM.C);
        if (cantidadCFGEM > 0) {
            frasesFGEM.add("La clase C reúne " + cantidadCFGEM + (cantidadCFGEM == 1 ? " producto que aporta" : " productos que aportan")
                    + " solo el " + MontosFGEM.porcentajeFGEM(abcFGEM.participacionFGEM(AnalisisAbcFGEM.ClaseFGEM.C))
                    + " de las ventas; revisa si conviene mantenerlos en el catálogo.");
        }
        return frasesFGEM;
    }

    public static List<String> correlacionesFGEM(AnalisisEstadisticoFGEM aFGEM) {
        List<String> frasesFGEM = new ArrayList<>();
        MatrizCorrelacionFGEM mFGEM = aFGEM.correlacionesFGEM();
        if (mFGEM.observacionesFGEM() < MatrizCorrelacionFGEM.MINIMO_OBSERVACIONES_FGEM) {
            frasesFGEM.add("Se necesitan al menos " + MatrizCorrelacionFGEM.MINIMO_OBSERVACIONES_FGEM
                    + " meses completos para calcular correlaciones (hay " + mFGEM.observacionesFGEM() + ").");
            return frasesFGEM;
        }
        List<MatrizCorrelacionFGEM.ParFGEM> destacadosFGEM = mFGEM.paresOrdenadosFGEM().stream()
                .filter(pFGEM -> Math.abs(pFGEM.rFGEM()) >= 0.4).limit(3).toList();
        if (destacadosFGEM.isEmpty()) {
            frasesFGEM.add("No se encontraron relaciones fuertes ni moderadas entre las variables.");
        }
        for (MatrizCorrelacionFGEM.ParFGEM parFGEM : destacadosFGEM) {
            frasesFGEM.add("\"" + parFGEM.variable1FGEM() + "\" y \"" + parFGEM.variable2FGEM() + "\" tienen una relación "
                    + (parFGEM.rFGEM() > 0 ? "positiva " : "inversa ") + MatrizCorrelacionFGEM.fuerzaFGEM(parFGEM.rFGEM())
                    + " (r = " + String.format(Locale.US, "%.2f", parFGEM.rFGEM()) + ")"
                    + (Double.isNaN(parFGEM.pFGEM()) || parFGEM.pFGEM() >= 0.05 ? ", aunque con pocos datos podría deberse al azar." : "."));
        }
        frasesFGEM.add("Calculado con " + mFGEM.observacionesFGEM() + " meses. Recuerda: que dos variables se muevan juntas no significa que una cause la otra.");
        return frasesFGEM;
    }

    // ------------------------------------------------------------------ formato

    private static String dineroFGEM(double valorFGEM, MonedaFGEM monedaFGEM) {
        return MontosFGEM.formatearFGEM(BigDecimal.valueOf(valorFGEM).setScale(2, RoundingMode.HALF_UP), monedaFGEM);
    }

    private static String porcentajeFGEM(double fraccionFGEM) {
        return MontosFGEM.porcentajeFGEM(BigDecimal.valueOf(fraccionFGEM));
    }

    private static String valorPFGEM(double pFGEM) {
        return Double.isNaN(pFGEM) ? "—" : pFGEM < 0.001 ? "< 0.001" : String.format(Locale.US, "%.3f", pFGEM);
    }

    /** 1.35 → "35 % más"; 0.62 → "38 % menos". */
    private static String diferenciaFGEM(double indiceFGEM) {
        double cambioFGEM = indiceFGEM - 1;
        return porcentajeFGEM(Math.abs(cambioFGEM)) + (cambioFGEM >= 0 ? " más" : " menos");
    }

    public static String nombreFGEM(DayOfWeek diaFGEM) {
        return diaFGEM.getDisplayName(TextStyle.FULL, ESPANOL_FGEM);
    }
}
