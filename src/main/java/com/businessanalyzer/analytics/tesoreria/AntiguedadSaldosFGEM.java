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

public final class AntiguedadSaldosFGEM {

    public static final String SIN_NOMBRE_FGEM = "Sin nombre";

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

        public static TramoFGEM deFGEM(long diasAtrasoFGEM) {
            if (diasAtrasoFGEM <= 0) {
                return AL_DIA;
            }
            return diasAtrasoFGEM <= 30 ? VENCIDO_30 : diasAtrasoFGEM <= 60 ? VENCIDO_60 : VENCIDO_MAS_60;
        }
    }

    public record SaldoTramoFGEM(TramoFGEM tramoFGEM, int operacionesFGEM, BigDecimal montoFGEM, BigDecimal porcentajeFGEM) {
    }

    public record SaldoContraparteFGEM(String contraparteFGEM, int operacionesFGEM, BigDecimal pendienteFGEM, BigDecimal vencidoFGEM,
                                       long mayorAtrasoFGEM) {

        public TramoFGEM tramoFGEM() {
            return TramoFGEM.deFGEM(mayorAtrasoFGEM);
        }
    }

    public record ResultadoFGEM(BigDecimal totalFGEM, BigDecimal vencidoFGEM, List<SaldoTramoFGEM> tramosFGEM,
                                List<SaldoContraparteFGEM> contrapartesFGEM) {

        public BigDecimal porcentajeVencidoFGEM() {
            return totalFGEM.signum() == 0 ? BigDecimal.ZERO : vencidoFGEM.divide(totalFGEM, 4, RoundingMode.HALF_UP);
        }
    }

    private AntiguedadSaldosFGEM() {
    }

    public static long diasAtrasoFGEM(OperacionFGEM oFGEM, LocalDate hoyFGEM) {
        return oFGEM.vencidaEnFGEM(hoyFGEM) ? ChronoUnit.DAYS.between(oFGEM.vencimientoFGEM(), hoyFGEM) : 0;
    }

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
