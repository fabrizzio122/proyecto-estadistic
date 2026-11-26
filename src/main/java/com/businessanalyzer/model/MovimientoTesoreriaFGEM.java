package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MovimientoTesoreriaFGEM(
        long idFGEM,
        long empresaIdFGEM,
        TipoMovimientoTesoreriaFGEM tipoFGEM,
        LocalDate fechaFGEM,
        Long cuentaOrigenIdFGEM,
        String cuentaOrigenFGEM,
        Long cuentaDestinoIdFGEM,
        String cuentaDestinoFGEM,
        BigDecimal montoFGEM,
        String descripcionFGEM,
        boolean anuladoFGEM,
        String motivoAnulacionFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM) {
}
