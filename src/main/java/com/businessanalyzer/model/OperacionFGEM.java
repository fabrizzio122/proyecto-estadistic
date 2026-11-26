package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OperacionFGEM(
        long idFGEM,
        long empresaIdFGEM,
        TipoOperacionFGEM tipoFGEM,
        String numeroFGEM,
        LocalDate fechaFGEM,
        String contraparteFGEM,
        String documentoContraparteFGEM,
        String comprobanteFGEM,
        BigDecimal tasaImpuestoFGEM,
        BigDecimal subtotalFGEM,
        BigDecimal impuestoFGEM,
        BigDecimal totalFGEM,
        BigDecimal costoTotalFGEM,
        EstadoOperacionFGEM estadoFGEM,
        String observacionesFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM,
        String motivoAnulacionFGEM,
        String anuladoPorFGEM,
        LocalDateTime anuladoEnFGEM,
        int cantidadItemsFGEM,
        List<LineaOperacionFGEM> lineasFGEM,
        CondicionPagoFGEM condicionFGEM,
        Long cuentaIdFGEM,
        String cuentaFGEM,
        LocalDate vencimientoFGEM,
        BigDecimal pagadoFGEM) {

    public OperacionFGEM {
        lineasFGEM = lineasFGEM == null ? List.of() : List.copyOf(lineasFGEM);
        condicionFGEM = condicionFGEM == null ? CondicionPagoFGEM.CONTADO : condicionFGEM;
        pagadoFGEM = pagadoFGEM == null ? BigDecimal.ZERO : pagadoFGEM;
    }

    public boolean anuladaFGEM() {
        return estadoFGEM == EstadoOperacionFGEM.ANULADA;
    }

    public boolean alCreditoFGEM() {
        return condicionFGEM == CondicionPagoFGEM.CREDITO;
    }

    public BigDecimal pendienteFGEM() {
        if (!alCreditoFGEM() || anuladaFGEM()) {
            return BigDecimal.ZERO;
        }
        return totalFGEM.subtract(pagadoFGEM).max(BigDecimal.ZERO);
    }

    public boolean vencidaEnFGEM(LocalDate hoyFGEM) {
        return pendienteFGEM().signum() > 0 && vencimientoFGEM != null && vencimientoFGEM.isBefore(hoyFGEM);
    }

    public BigDecimal margenBrutoFGEM() {
        return subtotalFGEM.subtract(costoTotalFGEM);
    }

    public OperacionFGEM conLineasFGEM(List<LineaOperacionFGEM> detalleFGEM) {
        return new OperacionFGEM(idFGEM, empresaIdFGEM, tipoFGEM, numeroFGEM, fechaFGEM, contraparteFGEM, documentoContraparteFGEM, comprobanteFGEM,
                tasaImpuestoFGEM, subtotalFGEM, impuestoFGEM, totalFGEM, costoTotalFGEM, estadoFGEM, observacionesFGEM, registradoPorFGEM,
                registradoEnFGEM, motivoAnulacionFGEM, anuladoPorFGEM, anuladoEnFGEM, cantidadItemsFGEM, detalleFGEM, condicionFGEM, cuentaIdFGEM,
                cuentaFGEM, vencimientoFGEM, pagadoFGEM);
    }
}
