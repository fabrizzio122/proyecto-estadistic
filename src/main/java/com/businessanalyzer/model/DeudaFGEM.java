package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Deuda de la empresa (pasivo).
 *
 * @param montoFGEM         capital recibido o adeudado al inicio
 * @param pagadoFGEM        suma de los pagos de capital registrados (no anulados)
 * @param tasaAnualFGEM     tasa de interés anual en porcentaje (24.5 = 24.5 %); {@code null} si no se indicó
 * @param cuotaMensualFGEM  cuota que se paga cada mes (capital + interés); {@code null} si no hay cuotas fijas
 * @param vencimientoFGEM   fecha del último pago; {@code null} si no tiene plazo definido
 * @param cuentaIdFGEM      cuenta a la que ingresó el dinero del préstamo; {@code null} si no ingresó dinero
 */
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

    /** Deuda por la que no ingresó dinero a las cuentas de tesorería. */
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

    /** Se pagó por completo. */
    public boolean canceladaFGEM() {
        return !anuladaFGEM && saldoFGEM().signum() == 0;
    }

    /** Cuenta en el balance: no anulada y con saldo pendiente. */
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
