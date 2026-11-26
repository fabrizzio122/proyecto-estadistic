package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MovimientoFinancieroFGEM(
        long idFGEM,
        long empresaIdFGEM,
        TipoMovimientoFGEM tipoFGEM,
        CategoriaFinancieraFGEM categoriaFGEM,
        ComportamientoFGEM comportamientoFGEM,
        LocalDate fechaFGEM,
        String descripcionFGEM,
        BigDecimal montoFGEM,
        String comprobanteFGEM,
        boolean anuladoFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM,
        String actualizadoPorFGEM,
        LocalDateTime actualizadoEnFGEM,
        String motivoAnulacionFGEM,
        String anuladoPorFGEM,
        LocalDateTime anuladoEnFGEM,
        Long cuentaIdFGEM,
        String cuentaFGEM,
        boolean dePagoDeudaFGEM) {
}
