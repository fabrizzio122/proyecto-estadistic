package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DeudaFGEM(
        long idFGEM,
        long empresaIdFGEM,
        CategoriaDeudaFGEM categoriaFGEM,
        String acreedorFGEM,
        String descripcionFGEM,
        LocalDate fechaFGEM,
        BigDecimal montoFGEM,
        BigDecimal pagadoFGEM,
        BigDecimal tasaAnualFGEM,
        BigDecimal cuotaMensualFGEM,
        LocalDate vencimientoFGEM,
        boolean anuladaFGEM,
        String motivoAnulacionFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM,
        String actualizadoPorFGEM,
        LocalDateTime actualizadoEnFGEM,
        Long cuentaIdFGEM) {

    public DeudaFGEM(long idFGEM, long empresaIdFGEM, CategoriaDeudaFGEM categoriaFGEM, String acreedorFGEM, String descripcionFGEM,
                 LocalDate fechaFGEM, BigDecimal montoFGEM, BigDecimal pagadoFGEM, BigDecimal tasaAnualFGEM, BigDecimal cuotaMensualFGEM,
                 LocalDate vencimientoFGEM, boolean anuladaFGEM, String motivoAnulacionFGEM, String registradoPorFGEM,
                 LocalDateTime registradoEnFGEM, String actualizadoPorFGEM, LocalDateTime actualizadoEnFGEM) {
        this(idFGEM, empresaIdFGEM, categoriaFGEM, acreedorFGEM, descripcionFGEM, fechaFGEM, montoFGEM, pagadoFGEM, tasaAnualFGEM, cuotaMensualFGEM,
                vencimientoFGEM, anuladaFGEM, motivoAnulacionFGEM, registradoPorFGEM, registradoEnFGEM, actualizadoPorFGEM, actualizadoEnFGEM, null);
    }

    public BigDecimal saldoFGEM() {
        return montoFGEM.subtract(pagadoFGEM).max(BigDecimal.ZERO);
    }

    public boolean canceladaFGEM() {
        return !anuladaFGEM && saldoFGEM().signum() == 0;
    }

    public boolean pendienteFGEM() {
        return !anuladaFGEM && saldoFGEM().signum() > 0;
    }

    public String estadoTextoFGEM() {
        if (anuladaFGEM) {
            return "Anulada";
        }
        return canceladaFGEM() ? "Cancelada" : "Pendiente";
    }
}
