package com.businessanalyzer.analytics.valoracion;

import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.util.Montos;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuánto vale el negocio, por tres métodos:
 * <ol>
 *   <li><b>Patrimonial:</b> activos − deudas del balance general (el ganado a precio de mercado y los
 *       cultivos en proceso al costo).</li>
 *   <li><b>Capitalización de utilidades:</b> utilidad neta anual ÷ tasa de descuento (un negocio que gana
 *       lo mismo cada año, para siempre).</li>
 *   <li><b>Flujos descontados:</b> flujo anual para el dueño (utilidad neta + depreciación, que no es
 *       salida de dinero) proyectado con el crecimiento elegido, más un valor terminal con crecimiento
 *       de largo plazo, todo traído a valor de hoy con la tasa de descuento.</li>
 * </ol>
 * <b>Valor de referencia:</b> el promedio de los dos métodos por rentabilidad si supera al patrimonial
 * (el negocio vale más por lo que gana que por lo que tiene: la diferencia es el fondo de comercio);
 * si no, el valor patrimonial.
 *
 * @param flujoAnual       utilidad neta anual + depreciación anual
 * @param capitalizacion   {@code null} si la utilidad no es positiva
 * @param flujosDescontados {@code null} si el flujo no es positivo
 * @param valorRentabilidad promedio de los métodos por rentabilidad disponibles; {@code null} si ninguno
 * @param fondoComercio    valor por rentabilidad − patrimonial; {@code null} sin valor por rentabilidad
 * @param mesesResultados  meses con ventas usados para calcular la utilidad anual
 */
public record ValoracionNegocio(SupuestosValoracion supuestos, BigDecimal patrimonial, BigDecimal utilidadAnual,
                                BigDecimal depreciacionAnual, BigDecimal flujoAnual, BigDecimal capitalizacion,
                                BigDecimal flujosDescontados, List<Proyeccion> proyeccion, BigDecimal valorTerminal,
                                BigDecimal valorTerminalPresente, BigDecimal valorRentabilidad, BigDecimal fondoComercio,
                                BigDecimal valorReferencia, Base base, BigDecimal minimo, BigDecimal maximo,
                                int mesesResultados) {

    /** Meses de resultados por debajo de los cuales la valoración por rentabilidad es poco confiable. */
    public static final int MESES_CONFIABLES = 6;
    private static final MathContext PRECISION = new MathContext(20, RoundingMode.HALF_UP);

    /** Qué método define el valor de referencia. */
    public enum Base { RENTABILIDAD, PATRIMONIO }

    /** Un año proyectado. */
    public record Proyeccion(int anio, BigDecimal flujo, BigDecimal factor, BigDecimal valorPresente) {
    }

    /** Fila de la tabla de sensibilidad: valor por flujos descontados para cada combinación. */
    public record Sensibilidad(List<BigDecimal> tasas, List<BigDecimal> crecimientos, BigDecimal[][] valores) {
    }

    /**
     * @param utilidadAnual   utilidad neta llevada a 12 meses; {@code null} si aún no hay ventas
     * @param depreciacionMensual depreciación mensual de los activos fijos
     */
    public static ValoracionNegocio calcular(SupuestosValoracion s, BigDecimal patrimonial, BigDecimal utilidadAnual,
                                             BigDecimal depreciacionMensual, int mesesResultados) {
        BigDecimal utilidad = utilidadAnual == null ? BigDecimal.ZERO : utilidadAnual;
        BigDecimal depreciacion = depreciacionMensual.multiply(BigDecimal.valueOf(12));
        BigDecimal flujo = utilidad.add(depreciacion);

        BigDecimal capitalizacion = utilidad.signum() > 0 ? dinero(utilidad.divide(s.tasaDescuento(), PRECISION)) : null;

        List<Proyeccion> proyeccion = new ArrayList<>();
        BigDecimal valorTerminal = null;
        BigDecimal terminalPresente = null;
        BigDecimal descontados = null;
        if (flujo.signum() > 0 && utilidadAnual != null) {
            BigDecimal suma = BigDecimal.ZERO;
            BigDecimal ultimoFlujo = flujo;
            for (int t = 1; t <= s.anios(); t++) {
                BigDecimal flujoT = flujo.multiply(BigDecimal.ONE.add(s.crecimiento()).pow(t), PRECISION);
                BigDecimal factor = BigDecimal.ONE.divide(BigDecimal.ONE.add(s.tasaDescuento()).pow(t), PRECISION);
                BigDecimal presente = flujoT.multiply(factor, PRECISION);
                proyeccion.add(new Proyeccion(t, dinero(flujoT), factor.setScale(4, RoundingMode.HALF_UP), dinero(presente)));
                suma = suma.add(presente);
                ultimoFlujo = flujoT;
            }
            BigDecimal vt = ultimoFlujo.multiply(BigDecimal.ONE.add(s.crecimientoLargoPlazo()), PRECISION)
                    .divide(s.tasaDescuento().subtract(s.crecimientoLargoPlazo()), PRECISION);
            BigDecimal vtPresente = vt.divide(BigDecimal.ONE.add(s.tasaDescuento()).pow(s.anios()), PRECISION);
            valorTerminal = dinero(vt);
            terminalPresente = dinero(vtPresente);
            descontados = dinero(suma.add(vtPresente));
        }

        List<BigDecimal> rentabilidad = new ArrayList<>();
        if (capitalizacion != null) {
            rentabilidad.add(capitalizacion);
        }
        if (descontados != null) {
            rentabilidad.add(descontados);
        }
        BigDecimal valorRentabilidad = rentabilidad.isEmpty() ? null
                : dinero(rentabilidad.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(rentabilidad.size()), PRECISION));
        BigDecimal fondo = valorRentabilidad == null ? null : valorRentabilidad.subtract(patrimonial);
        boolean porRentabilidad = valorRentabilidad != null && valorRentabilidad.compareTo(patrimonial) > 0;
        BigDecimal referencia = porRentabilidad ? valorRentabilidad : patrimonial;

        List<BigDecimal> todos = new ArrayList<>(rentabilidad);
        todos.add(patrimonial);
        BigDecimal minimo = todos.stream().min(BigDecimal::compareTo).orElse(patrimonial);
        BigDecimal maximo = todos.stream().max(BigDecimal::compareTo).orElse(patrimonial);
        return new ValoracionNegocio(s, dinero(patrimonial), dinero(utilidad), dinero(depreciacion), dinero(flujo),
                capitalizacion, descontados, proyeccion, valorTerminal, terminalPresente, valorRentabilidad, fondo,
                dinero(referencia), porRentabilidad ? Base.RENTABILIDAD : Base.PATRIMONIO, dinero(minimo), dinero(maximo),
                mesesResultados);
    }

    /**
     * Valor por flujos descontados con la tasa y el crecimiento movidos 5 y 2 puntos hacia cada lado.
     * Las combinaciones no válidas (tasa fuera de rango) quedan en {@code null}.
     */
    public Sensibilidad sensibilidad() {
        List<BigDecimal> tasas = List.of(supuestos.tasaDescuento().subtract(new BigDecimal("0.05")),
                supuestos.tasaDescuento(), supuestos.tasaDescuento().add(new BigDecimal("0.05")));
        List<BigDecimal> crecimientos = List.of(supuestos.crecimiento().subtract(new BigDecimal("0.02")),
                supuestos.crecimiento(), supuestos.crecimiento().add(new BigDecimal("0.02")));
        BigDecimal[][] valores = new BigDecimal[tasas.size()][crecimientos.size()];
        for (int i = 0; i < tasas.size(); i++) {
            for (int j = 0; j < crecimientos.size(); j++) {
                try {
                    SupuestosValoracion s = new SupuestosValoracion(tasas.get(i), crecimientos.get(j),
                            supuestos.crecimientoLargoPlazo().min(tasas.get(i).subtract(new BigDecimal("0.01"))),
                            supuestos.anios());
                    valores[i][j] = calcular(s, patrimonial, utilidadAnual, depreciacionAnual
                            .divide(BigDecimal.valueOf(12), PRECISION), mesesResultados).flujosDescontados();
                } catch (RuntimeException e) {
                    valores[i][j] = null;
                }
            }
        }
        return new Sensibilidad(tasas, crecimientos, valores);
    }

    /** La utilidad se calculó con pocos meses: la valoración por rentabilidad es solo orientativa. */
    public boolean pocosDatos() {
        return mesesResultados < MESES_CONFIABLES;
    }

    /** Frases en lenguaje sencillo. */
    public List<String> lectura(Moneda moneda) {
        List<String> frases = new ArrayList<>();
        frases.add("Con lo que tiene y lo que debe, el negocio vale " + Montos.formatear(patrimonial, moneda)
                + " (valor patrimonial).");
        if (valorRentabilidad == null) {
            frases.add("Como en los últimos 12 meses no tuvo utilidad, por ahora solo puede valorarse por lo que tiene: "
                    + "un comprador pagaría como mucho su valor patrimonial.");
        } else {
            frases.add("Por lo que gana (" + Montos.formatear(utilidadAnual, moneda) + " al año), y exigiendo una "
                    + "rentabilidad de " + Montos.porcentaje(supuestos.tasaDescuento()) + " anual, vale alrededor de "
                    + Montos.formatear(valorRentabilidad, moneda) + ".");
            if (base == Base.RENTABILIDAD) {
                frases.add("El negocio vale más por lo que gana que por lo que tiene: el fondo de comercio (clientela, "
                        + "ubicación, prestigio) se estima en " + Montos.formatear(fondoComercio, moneda) + ".");
            } else {
                frases.add("Lo que gana no alcanza a justificar el valor de sus bienes: rinde menos de lo que exigiría "
                        + "un inversionista. Mejorar la utilidad es la forma de que el negocio valga más.");
            }
        }
        frases.add("Valor de referencia: " + Montos.formatear(valorReferencia, moneda) + " (rango de "
                + Montos.formatear(minimo, moneda) + " a " + Montos.formatear(maximo, moneda) + ").");
        if (pocosDatos() && valorRentabilidad != null) {
            frases.add("Atención: la utilidad se calculó con solo " + mesesResultados + " meses de datos; "
                    + "la valoración por rentabilidad es solo orientativa.");
        }
        return frases;
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
