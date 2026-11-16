package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosActivoFGEM;
import com.businessanalyzer.model.DatosDeudaFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Valida y normaliza activos, deudas y pagos de deudas. */
final class ValidadorPatrimonioFGEM {

    /** Un local o un terreno pueden haberse comprado hace décadas. */
    private static final LocalDate FECHA_MINIMA_FGEM = LocalDate.of(1950, 1, 1);
    private static final BigDecimal TASA_MAXIMA_FGEM = new BigDecimal("300");

    private ValidadorPatrimonioFGEM() {
    }

    static DatosActivoFGEM activoFGEM(DatosActivoFGEM dFGEM, LocalDate hoyFGEM) {
        if (dFGEM == null || dFGEM.categoriaFGEM() == null) {
            throw new ValidacionExceptionFGEM("Selecciona el tipo de activo.");
        }
        String descripcionFGEM = ValidacionesFGEM.requeridoFGEM(dFGEM.descripcionFGEM(), "Descripción", 3, 150);
        LocalDate fechaFGEM = fechaFGEM(dFGEM.fechaFGEM(), dFGEM.categoriaFGEM().corrienteFGEM() ? "Fecha del saldo" : "Fecha de adquisición", hoyFGEM);
        BigDecimal valorFGEM = ValidadorCatalogoFGEM.montoFGEM(dFGEM.valorFGEM(), dFGEM.categoriaFGEM().corrienteFGEM() ? "Saldo" : "Valor de adquisición");
        if (!dFGEM.categoriaFGEM().corrienteFGEM() && valorFGEM.signum() == 0) {
            throw new ValidacionExceptionFGEM("El valor de adquisición debe ser mayor que cero.");
        }

        Integer vidaFGEM = null;
        BigDecimal residualFGEM = BigDecimal.ZERO;
        if (dFGEM.categoriaFGEM().depreciableFGEM()) {
            vidaFGEM = dFGEM.vidaUtilAniosFGEM();
            if (vidaFGEM != null && (vidaFGEM < 1 || vidaFGEM > 100)) {
                throw new ValidacionExceptionFGEM("La vida útil debe estar entre 1 y 100 años.");
            }
            if (vidaFGEM != null && dFGEM.valorResidualFGEM() != null) {
                residualFGEM = ValidadorCatalogoFGEM.montoFGEM(dFGEM.valorResidualFGEM(), "Valor residual");
                if (residualFGEM.compareTo(valorFGEM) > 0) {
                    throw new ValidacionExceptionFGEM("El valor residual no puede ser mayor que el valor de adquisición.");
                }
            }
        }
        return new DatosActivoFGEM(dFGEM.categoriaFGEM(), descripcionFGEM, fechaFGEM, valorFGEM, vidaFGEM, residualFGEM, dFGEM.cuentaIdFGEM());
    }

    static DatosDeudaFGEM deudaFGEM(DatosDeudaFGEM dFGEM, LocalDate hoyFGEM) {
        if (dFGEM == null || dFGEM.categoriaFGEM() == null) {
            throw new ValidacionExceptionFGEM("Selecciona el tipo de deuda.");
        }
        String acreedorFGEM = ValidacionesFGEM.requeridoFGEM(dFGEM.acreedorFGEM(), "Acreedor", 2, 100);
        String descripcionFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.descripcionFGEM(), "Descripción", 150);
        LocalDate fechaFGEM = fechaFGEM(dFGEM.fechaFGEM(), "Fecha de inicio", hoyFGEM);
        BigDecimal montoFGEM = ValidadorCatalogoFGEM.montoFGEM(dFGEM.montoFGEM(), "Monto de la deuda");
        if (montoFGEM.signum() == 0) {
            throw new ValidacionExceptionFGEM("El monto de la deuda debe ser mayor que cero.");
        }

        BigDecimal tasaFGEM = dFGEM.tasaAnualFGEM();
        if (tasaFGEM != null) {
            tasaFGEM = ValidadorCatalogoFGEM.montoFGEM(tasaFGEM, "Tasa de interés anual");
            if (tasaFGEM.compareTo(TASA_MAXIMA_FGEM) > 0) {
                throw new ValidacionExceptionFGEM("La tasa de interés anual no puede superar 300 %.");
            }
        }
        BigDecimal cuotaFGEM = dFGEM.cuotaMensualFGEM();
        if (cuotaFGEM != null) {
            cuotaFGEM = ValidadorCatalogoFGEM.montoFGEM(cuotaFGEM, "Cuota mensual");
            if (cuotaFGEM.signum() == 0) {
                cuotaFGEM = null;
            }
        }
        LocalDate vencimientoFGEM = dFGEM.vencimientoFGEM();
        if (vencimientoFGEM != null && !vencimientoFGEM.isAfter(fechaFGEM)) {
            throw new ValidacionExceptionFGEM("El vencimiento debe ser posterior a la fecha de inicio.");
        }
        if (vencimientoFGEM != null && vencimientoFGEM.isAfter(fechaFGEM.plusYears(50))) {
            throw new ValidacionExceptionFGEM("El plazo de la deuda no puede superar 50 años.");
        }
        return new DatosDeudaFGEM(dFGEM.categoriaFGEM(), acreedorFGEM, descripcionFGEM, fechaFGEM, montoFGEM, tasaFGEM, cuotaFGEM, vencimientoFGEM,
                dFGEM.cuentaIdFGEM());
    }

    static BigDecimal montoPagoFGEM(BigDecimal montoFGEM, BigDecimal saldoFGEM) {
        BigDecimal limpioFGEM = ValidadorCatalogoFGEM.montoFGEM(montoFGEM, "Monto del pago");
        if (limpioFGEM.signum() == 0) {
            throw new ValidacionExceptionFGEM("El monto del pago debe ser mayor que cero.");
        }
        if (limpioFGEM.compareTo(saldoFGEM) > 0) {
            throw new ValidacionExceptionFGEM("El pago no puede superar el saldo pendiente.");
        }
        return limpioFGEM;
    }

    /** Intereses de la cuota: opcionales (cero si no se indican). */
    static BigDecimal interesFGEM(BigDecimal interesFGEM) {
        return interesFGEM == null ? BigDecimal.ZERO : ValidadorCatalogoFGEM.montoFGEM(interesFGEM, "Intereses");
    }

    static LocalDate fechaFGEM(LocalDate fechaFGEM, String campoFGEM, LocalDate hoyFGEM) {
        if (fechaFGEM == null) {
            throw new ValidacionExceptionFGEM("Indica la " + campoFGEM.toLowerCase() + ".");
        }
        if (fechaFGEM.isBefore(FECHA_MINIMA_FGEM)) {
            throw new ValidacionExceptionFGEM("La " + campoFGEM.toLowerCase() + " no puede ser anterior a 1950.");
        }
        if (fechaFGEM.isAfter(hoyFGEM)) {
            throw new ValidacionExceptionFGEM("La " + campoFGEM.toLowerCase() + " no puede ser posterior a hoy.");
        }
        return fechaFGEM;
    }
}
