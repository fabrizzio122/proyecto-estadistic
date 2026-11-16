package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Deuda de la empresa (pasivo).
 *
 * @param monto         capital recibido o adeudado al inicio
 * @param pagado        suma de los pagos de capital registrados (no anulados)
 * @param tasaAnual     tasa de interés anual en porcentaje (24.5 = 24.5 %); {@code null} si no se indicó
 * @param cuotaMensual  cuota que se paga cada mes (capital + interés); {@code null} si no hay cuotas fijas
 * @param vencimiento   fecha del último pago; {@code null} si no tiene plazo definido
 * @param cuentaId      cuenta a la que ingresó el dinero del préstamo; {@code null} si no ingresó dinero
 */
public record Deuda(
        long id,
        long empresaId,
        CategoriaDeuda categoria,
        String acreedor,
        String descripcion,
        LocalDate fecha,
        BigDecimal monto,
        BigDecimal pagado,
        BigDecimal tasaAnual,
        BigDecimal cuotaMensual,
        LocalDate vencimiento,
        boolean anulada,
        String motivoAnulacion,
        String registradoPor,
        LocalDateTime registradoEn,
        String actualizadoPor,
        LocalDateTime actualizadoEn,
        Long cuentaId) {

    /** Deuda por la que no ingresó dinero a las cuentas de tesorería. */
    public Deuda(long id, long empresaId, CategoriaDeuda categoria, String acreedor, String descripcion,
                 LocalDate fecha, BigDecimal monto, BigDecimal pagado, BigDecimal tasaAnual, BigDecimal cuotaMensual,
                 LocalDate vencimiento, boolean anulada, String motivoAnulacion, String registradoPor,
                 LocalDateTime registradoEn, String actualizadoPor, LocalDateTime actualizadoEn) {
        this(id, empresaId, categoria, acreedor, descripcion, fecha, monto, pagado, tasaAnual, cuotaMensual,
                vencimiento, anulada, motivoAnulacion, registradoPor, registradoEn, actualizadoPor, actualizadoEn, null);
    }

    public BigDecimal saldo() {
        return monto.subtract(pagado).max(BigDecimal.ZERO);
    }

    /** Se pagó por completo. */
    public boolean cancelada() {
        return !anulada && saldo().signum() == 0;
    }

    /** Cuenta en el balance: no anulada y con saldo pendiente. */
    public boolean pendiente() {
        return !anulada && saldo().signum() > 0;
    }

    public String estadoTexto() {
        if (anulada) {
            return "Anulada";
        }
        return cancelada() ? "Cancelada" : "Pendiente";
    }
}
