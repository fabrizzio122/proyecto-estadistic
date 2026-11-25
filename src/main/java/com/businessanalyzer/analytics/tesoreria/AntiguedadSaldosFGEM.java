package com.businessanalyzer.analytics.tesoreria;

import com.businessanalyzer.model.OperacionFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Antigüedad de las cuentas por cobrar (ventas al crédito) o por pagar (compras al crédito): cuánto de lo pendiente
 * está al día y cuánto lleva 1 a 30, 31 a 60 o más de 60 días vencido, y el resumen por cliente o proveedor.
 * Mientras más antiguo el atraso, más difícil es cobrarlo. Es cálculo puro.
 */
public final class AntiguedadSaldosFGEM {

    /** Nombre del grupo de las operaciones sin cliente o proveedor. */
    public static final String SIN_NOMBRE_FGEM = "Sin nombre";

    /** Tramos de antigüedad según los días pasados desde el vencimiento. */
    public enum TramoFGEM {
        AL_DIA("Al día", "Sin vencer o sin fecha límite"),
        VENCIDO_30("1 a 30 días", "Vencido hace 1 a 30 días"),
        VENCIDO_60("31 a 60 días", "Vencido hace 31 a 60 días"),
        VENCIDO_MAS_60("Más de 60 días", "Vencido hace más de 60 días: difícil de cobrar");

        private final String nombreFGEM;
        private final String descripcionFGEM;

        TramoFGEM(String nombreFGEM, String descripcionFGEM) {
            this.nombreFGEM = nombreFGEM;
            this.descripcionFGEM = descripcionFGEM;
        }

        public String nombreFGEM() {
            return nombreFGEM;
        }

        public String descripcionFGEM() {
            return descripcionFGEM;
        }

        /** Tramo para una cantidad de días de atraso (cero o negativo: al día). */
        public static TramoFGEM deFGEM(long diasAtrasoFGEM) {
            if (diasAtrasoFGEM <= 0) {
                return AL_DIA;
            }
            return diasAtrasoFGEM <= 30 ? VENCIDO_30 : diasAtrasoFGEM <= 60 ? VENCIDO_60 : VENCIDO_MAS_60;
        }
    }

    /**
     * @param montoFGEM       lo pendiente en el tramo
     * @param porcentajeFGEM  monto ÷ total pendiente (fracción); cero si no hay nada pendiente
     */
    public record SaldoTramoFGEM(TramoFGEM tramoFGEM, int operacionesFGEM, BigDecimal montoFGEM, BigDecimal porcentajeFGEM) {
    }

    /**
     * @param vencidoFGEM        lo pendiente con el vencimiento ya pasado
     * @param mayorAtrasoFGEM    días de atraso de su operación más antigua vencida; cero si nada está vencido
     */
    public record SaldoContraparteFGEM(String contraparteFGEM, int operacionesFGEM, BigDecimal pendienteFGEM, BigDecimal vencidoFGEM,
                                       long mayorAtrasoFGEM) {

        public TramoFGEM tramoFGEM() {
            return TramoFGEM.deFGEM(mayorAtrasoFGEM);
        }
    }

    /**
     * @param tramosFGEM        los cuatro tramos, en orden, aunque estén vacíos
     * @param contrapartesFGEM  primero quien debe (o a quien se debe) más dinero vencido, luego más pendiente
     */
    public record ResultadoFGEM(BigDecimal totalFGEM, BigDecimal vencidoFGEM, List<SaldoTramoFGEM> tramosFGEM,
                                List<SaldoContraparteFGEM> contrapartesFGEM) {

        /** Parte de lo pendiente que ya venció (fracción); cero si no hay nada pendiente. */
        public BigDecimal porcentajeVencidoFGEM() {
            return totalFGEM.signum() == 0 ? BigDecimal.ZERO : vencidoFGEM.divide(totalFGEM, 4, RoundingMode.HALF_UP);
        }
    }

    private AntiguedadSaldosFGEM() {
    }

    /** Días que lleva vencida una operación a la fecha {@code hoy}; cero si no está vencida. */
    public static long diasAtrasoFGEM(OperacionFGEM oFGEM, LocalDate hoyFGEM) {
        return oFGEM.vencidaEnFGEM(hoyFGEM) ? ChronoUnit.DAYS.between(oFGEM.vencimientoFGEM(), hoyFGEM) : 0;
    }

    /** Calcula la antigüedad con lo pendiente de cada operación (las pagadas o anuladas no cuentan). */
    public static ResultadoFGEM calcularFGEM(List<OperacionFGEM> operacionesFGEM, LocalDate hoyFGEM) {
        List<OperacionFGEM> pendientesFGEM = operacionesFGEM.stream().filter(oFGEM -> oFGEM.pendienteFGEM().signum() > 0).toList();
        BigDecimal totalFGEM = sumaFGEM(pendientesFGEM);

        Map<TramoFGEM, List<OperacionFGEM>> porTramoFGEM = new EnumMap<>(TramoFGEM.class);
        for (TramoFGEM tFGEM : TramoFGEM.values()) {
            porTramoFGEM.put(tFGEM, new ArrayList<>());
        }
        for (OperacionFGEM oFGEM : pendientesFGEM) {
            porTramoFGEM.get(TramoFGEM.deFGEM(diasAtrasoFGEM(oFGEM, hoyFGEM))).add(oFGEM);
        }
        List<SaldoTramoFGEM> tramosFGEM = new ArrayList<>();
        porTramoFGEM.forEach((tFGEM, listaFGEM) -> {
            BigDecimal montoFGEM = sumaFGEM(listaFGEM);
            tramosFGEM.add(new SaldoTramoFGEM(tFGEM, listaFGEM.size(), montoFGEM,
                    totalFGEM.signum() == 0 ? BigDecimal.ZERO : montoFGEM.divide(totalFGEM, 4, RoundingMode.HALF_UP)));
        });

        // Se agrupa sin distinguir mayúsculas ni espacios; se muestra el primer nombre tal como se escribió.
        Map<String, List<OperacionFGEM>> porNombreFGEM = new LinkedHashMap<>();
        Map<String, String> nombreVisibleFGEM = new LinkedHashMap<>();
        for (OperacionFGEM oFGEM : pendientesFGEM) {
            String nombreFGEM = oFGEM.contraparteFGEM() == null || oFGEM.contraparteFGEM().isBlank() ? SIN_NOMBRE_FGEM
                    : oFGEM.contraparteFGEM().strip().replaceAll("\\s+", " ");
            String claveFGEM = nombreFGEM.toLowerCase(Locale.ROOT);
            nombreVisibleFGEM.putIfAbsent(claveFGEM, nombreFGEM);
            porNombreFGEM.computeIfAbsent(claveFGEM, kFGEM -> new ArrayList<>()).add(oFGEM);
        }
        List<SaldoContraparteFGEM> contrapartesFGEM = porNombreFGEM.entrySet().stream()
                .map(eFGEM -> new SaldoContraparteFGEM(nombreVisibleFGEM.get(eFGEM.getKey()), eFGEM.getValue().size(), sumaFGEM(eFGEM.getValue()),
                        sumaFGEM(eFGEM.getValue().stream().filter(oFGEM -> oFGEM.vencidaEnFGEM(hoyFGEM)).toList()),
                        eFGEM.getValue().stream().mapToLong(oFGEM -> diasAtrasoFGEM(oFGEM, hoyFGEM)).max().orElse(0)))
                .sorted(Comparator.comparing(SaldoContraparteFGEM::vencidoFGEM).reversed()
                        .thenComparing(Comparator.comparing(SaldoContraparteFGEM::pendienteFGEM).reversed())
                        .thenComparing(SaldoContraparteFGEM::contraparteFGEM))
                .toList();

        BigDecimal vencidoFGEM = sumaFGEM(pendientesFGEM.stream().filter(oFGEM -> oFGEM.vencidaEnFGEM(hoyFGEM)).toList());
        return new ResultadoFGEM(totalFGEM, vencidoFGEM, List.copyOf(tramosFGEM), contrapartesFGEM);
    }

    private static BigDecimal sumaFGEM(List<OperacionFGEM> operacionesFGEM) {
        return operacionesFGEM.stream().map(OperacionFGEM::pendienteFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
