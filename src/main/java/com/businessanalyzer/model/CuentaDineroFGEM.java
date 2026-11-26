package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CuentaDineroFGEM(
        long idFGEM,
        long empresaIdFGEM,
        String nombreFGEM,
        TipoCuentaFGEM tipoFGEM,
        String numeroFGEM,
        BigDecimal saldoInicialFGEM,
        boolean activaFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM,
        String actualizadoPorFGEM,
        LocalDateTime actualizadoEnFGEM) {

    @Override
    public String toString() {
        return nombreFGEM;
    }
}
