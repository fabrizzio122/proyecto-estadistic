package com.businessanalyzer.analytics.diagnostico;

import com.businessanalyzer.analytics.PeriodoFGEM;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record DiagnosticoFGEM(PeriodoFGEM periodoFGEM, NivelSaludFGEM nivelFGEM, int puntajeFGEM, List<HallazgoFGEM> hallazgosFGEM,
                          Map<AreaFGEM, Integer> puntajePorAreaFGEM, List<String> resumenFGEM) {

    public DiagnosticoFGEM {
        hallazgosFGEM = List.copyOf(hallazgosFGEM);
        puntajePorAreaFGEM = Map.copyOf(puntajePorAreaFGEM);
        resumenFGEM = List.copyOf(resumenFGEM);
    }

    public static DiagnosticoFGEM sinDatosFGEM(PeriodoFGEM periodoFGEM, String motivoFGEM) {
        return new DiagnosticoFGEM(periodoFGEM, NivelSaludFGEM.SIN_DATOS, -1, List.of(), new EnumMap<>(AreaFGEM.class), List.of(motivoFGEM));
    }

    public boolean evaluadoFGEM() {
        return nivelFGEM != NivelSaludFGEM.SIN_DATOS;
    }

    public List<HallazgoFGEM> recomendacionesFGEM() {
        return hallazgosFGEM.stream()
                .filter(hFGEM -> hFGEM.semaforoFGEM() == SemaforoFGEM.ROJO || hFGEM.semaforoFGEM() == SemaforoFGEM.AMARILLO)
                .sorted(Comparator.comparingInt(HallazgoFGEM::prioridadFGEM).reversed())
                .toList();
    }

    public List<HallazgoFGEM> fortalezasFGEM() {
        return hallazgosFGEM.stream()
                .filter(hFGEM -> hFGEM.semaforoFGEM() == SemaforoFGEM.VERDE)
                .sorted(Comparator.comparingInt((HallazgoFGEM hFGEM) -> hFGEM.tipoFGEM().pesoFGEM()).reversed())
                .toList();
    }

    public long contarFGEM(SemaforoFGEM semaforoFGEM) {
        return hallazgosFGEM.stream().filter(hFGEM -> hFGEM.semaforoFGEM() == semaforoFGEM).count();
    }

    public List<HallazgoFGEM> deAreaFGEM(AreaFGEM areaFGEM) {
        return hallazgosFGEM.stream().filter(hFGEM -> hFGEM.tipoFGEM().areaFGEM() == areaFGEM).toList();
    }
}
