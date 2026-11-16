package com.businessanalyzer.analytics.ganaderia;

import com.businessanalyzer.model.CategoriaGanadoFGEM;
import com.businessanalyzer.model.MovimientoGanadoFGEM;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Existencias del hato calculadas a partir de sus movimientos. */
public final class HatoFGEM {

    private HatoFGEM() {
    }

    /** Cabezas por categoría al final del día indicado (solo categorías con animales). */
    public static Map<CategoriaGanadoFGEM, Integer> existenciasFGEM(Collection<MovimientoGanadoFGEM> movimientosFGEM, LocalDate fechaFGEM) {
        Map<CategoriaGanadoFGEM, Integer> resultadoFGEM = new EnumMap<>(CategoriaGanadoFGEM.class);
        for (MovimientoGanadoFGEM mFGEM : movimientosFGEM) {
            if (mFGEM.anuladoFGEM() || mFGEM.fechaFGEM().isAfter(fechaFGEM)) {
                continue;
            }
            for (CategoriaGanadoFGEM cFGEM : afectadasFGEM(mFGEM)) {
                resultadoFGEM.merge(cFGEM, mFGEM.efectoEnFGEM(cFGEM), Integer::sum);
            }
        }
        resultadoFGEM.values().removeIf(vFGEM -> vFGEM == 0);
        return resultadoFGEM;
    }

    /**
     * Revisa que ninguna categoría quede con existencias negativas en ningún momento, recorriendo
     * los movimientos en orden de fecha.
     *
     * @return la primera categoría y fecha en que faltarían animales, si las hay
     */
    public static Optional<FaltanteFGEM> primerFaltanteFGEM(Collection<MovimientoGanadoFGEM> movimientosFGEM) {
        List<MovimientoGanadoFGEM> ordenadosFGEM = movimientosFGEM.stream()
                .filter(mFGEM -> !mFGEM.anuladoFGEM())
                .sorted(Comparator.comparing(MovimientoGanadoFGEM::fechaFGEM)
                        // El mismo día, primero las entradas y después las salidas.
                        .thenComparing(mFGEM -> -mFGEM.tipoFGEM().signoFGEM())
                        .thenComparingLong(MovimientoGanadoFGEM::idFGEM))
                .toList();
        Map<CategoriaGanadoFGEM, Integer> saldoFGEM = new EnumMap<>(CategoriaGanadoFGEM.class);
        for (MovimientoGanadoFGEM mFGEM : ordenadosFGEM) {
            for (CategoriaGanadoFGEM cFGEM : afectadasFGEM(mFGEM)) {
                int nuevoFGEM = saldoFGEM.merge(cFGEM, mFGEM.efectoEnFGEM(cFGEM), Integer::sum);
                if (nuevoFGEM < 0) {
                    return Optional.of(new FaltanteFGEM(cFGEM, mFGEM.fechaFGEM(), -nuevoFGEM));
                }
            }
        }
        return Optional.empty();
    }

    /** Faltan {@code cantidad} animales de la categoría en la fecha indicada. */
    public record FaltanteFGEM(CategoriaGanadoFGEM categoriaFGEM, LocalDate fechaFGEM, int cantidadFGEM) {
    }

    private static List<CategoriaGanadoFGEM> afectadasFGEM(MovimientoGanadoFGEM mFGEM) {
        return mFGEM.categoriaDestinoFGEM() == null ? List.of(mFGEM.categoriaFGEM()) : List.of(mFGEM.categoriaFGEM(), mFGEM.categoriaDestinoFGEM());
    }
}
