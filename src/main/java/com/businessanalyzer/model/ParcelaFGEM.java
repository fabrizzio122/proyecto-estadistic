package com.businessanalyzer.model;

import java.math.BigDecimal;

public record ParcelaFGEM(long idFGEM, long empresaIdFGEM, String nombreFGEM, BigDecimal areaFGEM, TenenciaFGEM tenenciaFGEM, boolean riegoFGEM,
                      String ubicacionFGEM, boolean activaFGEM) {

    @Override
    public String toString() {
        return nombreFGEM;
    }
}
