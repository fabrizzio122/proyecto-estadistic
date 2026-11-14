package com.businessanalyzer.analytics.diagnostico;

/**
 * Resultado de evaluar un indicador.
 *
 * @param valor          valor del indicador ya formateado ("12.4 %", "2 de 6")
 * @param criterio       umbrales con los que se evaluó ("Bien desde 5 %")
 * @param lectura        qué significa el valor para este negocio
 * @param recomendacion  qué hacer; {@code null} si el indicador está en verde o sin datos
 */
public record Hallazgo(TipoIndicador tipo, Semaforo semaforo, String valor, String criterio, String lectura,
                       String recomendacion) {

    public static Hallazgo sinDatos(TipoIndicador tipo, String motivo) {
        return new Hallazgo(tipo, Semaforo.SIN_DATOS, "—", null, motivo, null);
    }

    /** Prioridad para ordenar las recomendaciones: primero los rojos y, dentro de ellos, los de más peso. */
    public int prioridad() {
        int gravedad = switch (semaforo) {
            case ROJO -> 2;
            case AMARILLO -> 1;
            default -> 0;
        };
        return gravedad * 10 + tipo.peso();
    }
}
