package com.businessanalyzer.analytics.diagnostico;

import com.businessanalyzer.analytics.Periodo;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resultado del diagnóstico empresarial.
 *
 * @param puntaje          0 a 100 (promedio ponderado de los indicadores evaluados); −1 sin datos
 * @param puntajePorArea   puntaje de cada área con al menos un indicador evaluado
 * @param resumen          frases que explican el resultado en lenguaje sencillo
 */
public record Diagnostico(Periodo periodo, NivelSalud nivel, int puntaje, List<Hallazgo> hallazgos,
                          Map<Area, Integer> puntajePorArea, List<String> resumen) {

    public Diagnostico {
        hallazgos = List.copyOf(hallazgos);
        puntajePorArea = Map.copyOf(puntajePorArea);
        resumen = List.copyOf(resumen);
    }

    public static Diagnostico sinDatos(Periodo periodo, String motivo) {
        return new Diagnostico(periodo, NivelSalud.SIN_DATOS, -1, List.of(), new EnumMap<>(Area.class), List.of(motivo));
    }

    public boolean evaluado() {
        return nivel != NivelSalud.SIN_DATOS;
    }

    /** Indicadores en amarillo o rojo, del más urgente al menos urgente. */
    public List<Hallazgo> recomendaciones() {
        return hallazgos.stream()
                .filter(h -> h.semaforo() == Semaforo.ROJO || h.semaforo() == Semaforo.AMARILLO)
                .sorted(Comparator.comparingInt(Hallazgo::prioridad).reversed())
                .toList();
    }

    /** Indicadores en verde, empezando por los de más peso. */
    public List<Hallazgo> fortalezas() {
        return hallazgos.stream()
                .filter(h -> h.semaforo() == Semaforo.VERDE)
                .sorted(Comparator.comparingInt((Hallazgo h) -> h.tipo().peso()).reversed())
                .toList();
    }

    public long contar(Semaforo semaforo) {
        return hallazgos.stream().filter(h -> h.semaforo() == semaforo).count();
    }

    public List<Hallazgo> deArea(Area area) {
        return hallazgos.stream().filter(h -> h.tipo().area() == area).toList();
    }
}
