package com.businessanalyzer.analytics.patrimonio;

import com.businessanalyzer.model.Activo;
import com.businessanalyzer.model.CategoriaActivo;
import com.businessanalyzer.model.CategoriaDeuda;
import com.businessanalyzer.model.Deuda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Balance general simplificado a una fecha de corte:
 * <pre>
 *   Activo corriente      (caja y bancos, cuentas por cobrar, inventario, otros)
 * + Activo no corriente   (bienes a su valor en libros, ya descontada la depreciación)
 * = Total activo
 *   Pasivo corriente      (lo que vence en los próximos 12 meses)
 * + Pasivo no corriente   (el resto de las deudas)
 * = Total pasivo
 *   Patrimonio = Total activo − Total pasivo
 * </pre>
 * El inventario se valoriza con el stock actual de los productos por su costo unitario.
 *
 * @param depreciacionAcumulada depreciación de todos los activos fijos hasta la fecha de corte
 * @param depreciacionMensual   depreciación que generan los activos fijos cada mes
 * @param cuotasMensuales       suma de las cuotas mensuales de las deudas pendientes
 * @param ganado                valor de mercado del hato (Fase 15); cero si no hay ganado
 * @param activosRegistrados    cantidad de activos registrados (sin contar el inventario)
 * @param deudasPendientes      cantidad de deudas con saldo
 */
public record BalanceGeneral(LocalDate corte,
                             List<Partida> activoCorriente,
                             List<Partida> activoNoCorriente,
                             List<Partida> pasivoCorriente,
                             List<Partida> pasivoNoCorriente,
                             BigDecimal inventario,
                             BigDecimal ganado,
                             BigDecimal depreciacionAcumulada,
                             BigDecimal depreciacionMensual,
                             BigDecimal cuotasMensuales,
                             int activosRegistrados,
                             int deudasPendientes) {

    public static final String INVENTARIO = "Inventario de mercadería";
    public static final String GANADO = "Ganado (valor de mercado)";

    /** Rubro del balance con su monto. */
    public record Partida(String concepto, BigDecimal monto) {
    }

    public BalanceGeneral {
        activoCorriente = List.copyOf(activoCorriente);
        activoNoCorriente = List.copyOf(activoNoCorriente);
        pasivoCorriente = List.copyOf(pasivoCorriente);
        pasivoNoCorriente = List.copyOf(pasivoNoCorriente);
    }

    /**
     * @param activos    activos de la empresa (se consideran los vigentes a la fecha de corte)
     * @param inventario valor del stock de mercadería
     * @param deudas     deudas de la empresa (se consideran las pendientes iniciadas hasta la fecha de corte)
     */
    public static BalanceGeneral de(LocalDate corte, Collection<Activo> activos, BigDecimal inventario,
                                    Collection<Deuda> deudas) {
        return de(corte, activos, inventario, BigDecimal.ZERO, deudas);
    }

    /** Igual que el anterior, sumando el valor del ganado al activo no corriente. */
    public static BalanceGeneral de(LocalDate corte, Collection<Activo> activos, BigDecimal inventario,
                                    BigDecimal ganado, Collection<Deuda> deudas) {
        Map<CategoriaActivo, BigDecimal> porCategoria = new EnumMap<>(CategoriaActivo.class);
        BigDecimal depAcumulada = BigDecimal.ZERO;
        BigDecimal depMensual = BigDecimal.ZERO;
        int registrados = 0;
        for (Activo a : activos) {
            if (!a.vigenteEn(corte)) {
                continue;
            }
            registrados++;
            Depreciacion d = Depreciacion.de(a, corte);
            depAcumulada = depAcumulada.add(d.acumulada());
            depMensual = depMensual.add(d.mensual());
            porCategoria.merge(a.categoria(), d.valorEnLibros(), BigDecimal::add);
        }

        List<Partida> corriente = new ArrayList<>();
        List<Partida> noCorriente = new ArrayList<>();
        BigDecimal stock = inventario == null ? BigDecimal.ZERO : inventario.max(BigDecimal.ZERO);
        for (CategoriaActivo categoria : CategoriaActivo.values()) {
            if (categoria == CategoriaActivo.OTROS_CORRIENTES && stock.signum() > 0) {
                corriente.add(new Partida(INVENTARIO, stock));
            }
            BigDecimal monto = porCategoria.get(categoria);
            if (monto != null) {
                (categoria.corriente() ? corriente : noCorriente).add(new Partida(categoria.descripcion(), monto));
            }
        }
        BigDecimal hato = ganado == null ? BigDecimal.ZERO : ganado.max(BigDecimal.ZERO);
        if (hato.signum() > 0) {
            noCorriente.add(new Partida(GANADO, hato));
        }

        Map<CategoriaDeuda, BigDecimal> cortoPlazo = new EnumMap<>(CategoriaDeuda.class);
        Map<CategoriaDeuda, BigDecimal> largoPlazo = new EnumMap<>(CategoriaDeuda.class);
        BigDecimal cuotas = BigDecimal.ZERO;
        int pendientes = 0;
        for (Deuda d : deudas) {
            if (!d.pendiente() || d.fecha().isAfter(corte)) {
                continue;
            }
            pendientes++;
            BigDecimal corto = porcionCorriente(d, corte);
            cortoPlazo.merge(d.categoria(), corto, BigDecimal::add);
            BigDecimal largo = d.saldo().subtract(corto);
            if (largo.signum() > 0) {
                largoPlazo.merge(d.categoria(), largo, BigDecimal::add);
            }
            if (d.cuotaMensual() != null) {
                cuotas = cuotas.add(d.cuotaMensual());
            }
        }
        return new BalanceGeneral(corte, corriente, noCorriente, partidas(cortoPlazo), partidas(largoPlazo),
                stock, hato, depAcumulada, depMensual, cuotas, registrados, pendientes);
    }

    /**
     * Parte del saldo que se pagará en los próximos 12 meses. Sin vencimiento, o si vence dentro
     * de un año, es todo el saldo; si vence después, se supone que el capital se paga en partes
     * iguales cada mes hasta el vencimiento.
     */
    public static BigDecimal porcionCorriente(Deuda d, LocalDate corte) {
        BigDecimal saldo = d.saldo();
        if (d.vencimiento() == null) {
            return saldo;
        }
        long meses = mesesHasta(corte, d.vencimiento());
        if (meses <= 12) {
            return saldo;
        }
        return saldo.multiply(BigDecimal.valueOf(12)).divide(BigDecimal.valueOf(meses), 2, RoundingMode.HALF_UP);
    }

    /** Meses que faltan hasta la fecha (redondeado hacia arriba); 0 si ya pasó. */
    static long mesesHasta(LocalDate desde, LocalDate hasta) {
        if (!hasta.isAfter(desde)) {
            return 0;
        }
        long meses = ChronoUnit.MONTHS.between(desde, hasta);
        return desde.plusMonths(meses).isBefore(hasta) ? meses + 1 : meses;
    }

    private static List<Partida> partidas(Map<CategoriaDeuda, BigDecimal> montos) {
        return montos.entrySet().stream()
                .filter(e -> e.getValue().signum() > 0)
                .map(e -> new Partida(e.getKey().descripcion(), e.getValue()))
                .toList();
    }

    public BigDecimal totalActivoCorriente() {
        return suma(activoCorriente);
    }

    public BigDecimal totalActivoNoCorriente() {
        return suma(activoNoCorriente);
    }

    public BigDecimal totalActivo() {
        return totalActivoCorriente().add(totalActivoNoCorriente());
    }

    public BigDecimal totalPasivoCorriente() {
        return suma(pasivoCorriente);
    }

    public BigDecimal totalPasivoNoCorriente() {
        return suma(pasivoNoCorriente);
    }

    public BigDecimal totalPasivo() {
        return totalPasivoCorriente().add(totalPasivoNoCorriente());
    }

    /** Lo que realmente pertenece a los dueños: activos − deudas. Puede ser negativo. */
    public BigDecimal patrimonio() {
        return totalActivo().subtract(totalPasivo());
    }

    /** No hay activos ni deudas registrados: no se puede evaluar la situación patrimonial. */
    public boolean sinRegistros() {
        return !tieneActivos() && deudasPendientes == 0;
    }

    /** Hay activos registrados o ganado valorizado (sin contar el inventario de mercadería). */
    public boolean tieneActivos() {
        return activosRegistrados > 0 || ganado.signum() > 0;
    }

    private static BigDecimal suma(List<Partida> partidas) {
        return partidas.stream().map(Partida::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
