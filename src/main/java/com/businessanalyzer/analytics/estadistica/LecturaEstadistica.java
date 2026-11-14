package com.businessanalyzer.analytics.estadistica;

import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.util.Montos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Traduce los resultados estadísticos a frases claras para quien no es especialista. */
public final class LecturaEstadistica {

    private static final Locale ESPANOL = Locale.forLanguageTag("es");

    private LecturaEstadistica() {
    }

    public static List<String> resumen(AnalisisEstadistico a, Moneda moneda) {
        List<String> frases = new ArrayList<>();
        EstadisticaDescriptiva t = a.ticket();
        if (t.vacia()) {
            frases.add("No hay ventas registradas en el período.");
            return frases;
        }
        frases.add("Se registraron " + t.n() + (t.n() == 1 ? " venta" : " ventas") + ". La venta típica (mediana) fue de "
                + dinero(t.mediana(), moneda) + " y el promedio de " + dinero(t.media(), moneda) + ".");
        if (t.n() >= 4) {
            frases.add("La mitad de las ventas estuvo entre " + dinero(t.q1(), moneda) + " y " + dinero(t.q3(), moneda) + ".");
        }
        if (!Double.isNaN(t.asimetria()) && t.asimetria() > 1) {
            frases.add("Unas pocas ventas grandes elevan el promedio por encima de la venta típica.");
        }
        double cv = a.ventasDiarias().coeficienteVariacion();
        if (!Double.isNaN(cv)) {
            String nivel = cv < 0.3 ? "estables" : cv < 0.6 ? "moderadamente variables" : "muy variables: hay días fuertes y días flojos";
            frases.add("Las ventas diarias son " + nivel + " (coeficiente de variación " + porcentaje(cv) + ").");
        }
        return frases;
    }

    public static List<String> tendencia(AnalisisEstadistico a, Moneda moneda) {
        List<String> frases = new ArrayList<>();
        Tendencia t = a.tendenciaVentas();
        switch (t.direccion()) {
            case INSUFICIENTE -> frases.add("Se necesitan al menos " + Tendencia.MINIMO_PUNTOS
                    + " meses completos con ventas para medir la tendencia (hay " + t.n() + ").");
            case CRECIENTE, DECRECIENTE -> {
                boolean crece = t.direccion() == Tendencia.Direccion.CRECIENTE;
                frases.add("Las ventas " + (crece ? "crecen" : "caen") + " en promedio " + dinero(Math.abs(t.pendiente()), moneda)
                        + " por mes (" + (crece ? "+" : "") + porcentaje(t.crecimientoMensual()) + " mensual).");
                frases.add("La tendencia es estadísticamente significativa (p = " + valorP(t.significancia())
                        + "): es poco probable que se deba al azar.");
            }
            case ESTABLE -> frases.add("No hay una tendencia clara: las subidas y bajadas mensuales podrían deberse al azar (p = "
                    + valorP(t.significancia()) + ").");
        }
        if (t.direccion() != Tendencia.Direccion.INSUFICIENTE && !Double.isNaN(t.r2())) {
            frases.add("La recta de tendencia explica el " + porcentaje(t.r2()) + " de la variación de las ventas mensuales"
                    + " (R² = " + String.format(Locale.US, "%.2f", t.r2()) + ").");
        }
        if (a.mesActualExcluido()) {
            frases.add("El mes en curso no se incluye porque aún no termina.");
        }
        return frases;
    }

    public static List<String> estacionalidad(AnalisisEstadistico a, Moneda moneda) {
        List<String> frases = new ArrayList<>();
        EstacionalidadSemanal e = a.estacionalidad();
        DayOfWeek mejor = e.mejorDia();
        DayOfWeek peor = e.peorDia();
        if (mejor == null || Double.isNaN(e.promedioGeneral()) || e.promedioGeneral() == 0) {
            frases.add("No hay ventas en el período para comparar los días de la semana.");
            return frases;
        }
        frases.add("En un día promedio se vende " + dinero(e.promedioGeneral(), moneda) + ".");
        frases.add("El mejor día es el " + nombre(mejor) + ": se vende " + diferencia(e.indice(mejor)) + " que un día promedio.");
        if (peor != mejor) {
            frases.add("El día más flojo es el " + nombre(peor) + " (" + diferencia(e.indice(peor)) + " que un día promedio).");
        }
        frases.add("Úsalo para planificar personal, compras y promociones.");
        return frases;
    }

    public static List<String> abc(AnalisisEstadistico a) {
        List<String> frases = new ArrayList<>();
        AnalisisAbc abc = a.abc();
        if (abc.items().isEmpty()) {
            frases.add("No hay ventas en el período para clasificar los productos.");
            return frases;
        }
        int total = abc.items().size();
        long cantidadA = abc.cantidad(AnalisisAbc.Clase.A);
        frases.add(cantidadA + " de " + total + " productos (" + porcentaje((double) cantidadA / total) + ") generan el "
                + Montos.porcentaje(abc.participacion(AnalisisAbc.Clase.A)) + " de las ventas: son la clase A, los que nunca deben faltar.");
        long cantidadC = abc.cantidad(AnalisisAbc.Clase.C);
        if (cantidadC > 0) {
            frases.add("La clase C reúne " + cantidadC + (cantidadC == 1 ? " producto que aporta" : " productos que aportan")
                    + " solo el " + Montos.porcentaje(abc.participacion(AnalisisAbc.Clase.C))
                    + " de las ventas; revisa si conviene mantenerlos en el catálogo.");
        }
        return frases;
    }

    public static List<String> correlaciones(AnalisisEstadistico a) {
        List<String> frases = new ArrayList<>();
        MatrizCorrelacion m = a.correlaciones();
        if (m.observaciones() < MatrizCorrelacion.MINIMO_OBSERVACIONES) {
            frases.add("Se necesitan al menos " + MatrizCorrelacion.MINIMO_OBSERVACIONES
                    + " meses completos para calcular correlaciones (hay " + m.observaciones() + ").");
            return frases;
        }
        List<MatrizCorrelacion.Par> destacados = m.paresOrdenados().stream()
                .filter(p -> Math.abs(p.r()) >= 0.4).limit(3).toList();
        if (destacados.isEmpty()) {
            frases.add("No se encontraron relaciones fuertes ni moderadas entre las variables.");
        }
        for (MatrizCorrelacion.Par par : destacados) {
            frases.add("\"" + par.variable1() + "\" y \"" + par.variable2() + "\" tienen una relación "
                    + (par.r() > 0 ? "positiva " : "inversa ") + MatrizCorrelacion.fuerza(par.r())
                    + " (r = " + String.format(Locale.US, "%.2f", par.r()) + ")"
                    + (Double.isNaN(par.p()) || par.p() >= 0.05 ? ", aunque con pocos datos podría deberse al azar." : "."));
        }
        frases.add("Calculado con " + m.observaciones() + " meses. Recuerda: que dos variables se muevan juntas no significa que una cause la otra.");
        return frases;
    }

    // ------------------------------------------------------------------ formato

    private static String dinero(double valor, Moneda moneda) {
        return Montos.formatear(BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP), moneda);
    }

    private static String porcentaje(double fraccion) {
        return Montos.porcentaje(BigDecimal.valueOf(fraccion));
    }

    private static String valorP(double p) {
        return Double.isNaN(p) ? "—" : p < 0.001 ? "< 0.001" : String.format(Locale.US, "%.3f", p);
    }

    /** 1.35 → "35 % más"; 0.62 → "38 % menos". */
    private static String diferencia(double indice) {
        double cambio = indice - 1;
        return porcentaje(Math.abs(cambio)) + (cambio >= 0 ? " más" : " menos");
    }

    public static String nombre(DayOfWeek dia) {
        return dia.getDisplayName(TextStyle.FULL, ESPANOL);
    }
}
