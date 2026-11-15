package com.businessanalyzer.analytics.ganaderia;

import com.businessanalyzer.model.CategoriaGanado;
import com.businessanalyzer.model.MovimientoGanado;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Existencias del hato calculadas a partir de sus movimientos. */
public final class Hato {

    private Hato() {
    }

    /** Cabezas por categoría al final del día indicado (solo categorías con animales). */
    public static Map<CategoriaGanado, Integer> existencias(Collection<MovimientoGanado> movimientos, LocalDate fecha) {
        Map<CategoriaGanado, Integer> resultado = new EnumMap<>(CategoriaGanado.class);
        for (MovimientoGanado m : movimientos) {
            if (m.anulado() || m.fecha().isAfter(fecha)) {
                continue;
            }
            for (CategoriaGanado c : afectadas(m)) {
                resultado.merge(c, m.efectoEn(c), Integer::sum);
            }
        }
        resultado.values().removeIf(v -> v == 0);
        return resultado;
    }

    /**
     * Revisa que ninguna categoría quede con existencias negativas en ningún momento, recorriendo
     * los movimientos en orden de fecha.
     *
     * @return la primera categoría y fecha en que faltarían animales, si las hay
     */
    public static Optional<Faltante> primerFaltante(Collection<MovimientoGanado> movimientos) {
        List<MovimientoGanado> ordenados = movimientos.stream()
                .filter(m -> !m.anulado())
                .sorted(Comparator.comparing(MovimientoGanado::fecha)
                        // El mismo día, primero las entradas y después las salidas.
                        .thenComparing(m -> -m.tipo().signo())
                        .thenComparingLong(MovimientoGanado::id))
                .toList();
        Map<CategoriaGanado, Integer> saldo = new EnumMap<>(CategoriaGanado.class);
        for (MovimientoGanado m : ordenados) {
            for (CategoriaGanado c : afectadas(m)) {
                int nuevo = saldo.merge(c, m.efectoEn(c), Integer::sum);
                if (nuevo < 0) {
                    return Optional.of(new Faltante(c, m.fecha(), -nuevo));
                }
            }
        }
        return Optional.empty();
    }

    /** Faltan {@code cantidad} animales de la categoría en la fecha indicada. */
    public record Faltante(CategoriaGanado categoria, LocalDate fecha, int cantidad) {
    }

    private static List<CategoriaGanado> afectadas(MovimientoGanado m) {
        return m.categoriaDestino() == null ? List.of(m.categoria()) : List.of(m.categoria(), m.categoriaDestino());
    }
}
