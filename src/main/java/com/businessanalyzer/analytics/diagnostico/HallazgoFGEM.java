package com.businessanalyzer.analytics.diagnostico;

public record HallazgoFGEM(TipoIndicadorFGEM tipoFGEM, SemaforoFGEM semaforoFGEM, String valorFGEM, String criterioFGEM, String lecturaFGEM,
                       String recomendacionFGEM) {

    public static HallazgoFGEM sinDatosFGEM(TipoIndicadorFGEM tipoFGEM, String motivoFGEM) {
        return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.SIN_DATOS, "—", null, motivoFGEM, null);
    }

    public int prioridadFGEM() {
        int gravedadFGEM = switch (semaforoFGEM) {
            case ROJO -> 2;
            case AMARILLO -> 1;
            default -> 0;
        };
        return gravedadFGEM * 10 + tipoFGEM.pesoFGEM();
    }
}
