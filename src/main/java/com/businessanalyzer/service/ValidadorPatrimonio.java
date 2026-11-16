package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosActivo;
import com.businessanalyzer.model.DatosDeuda;
import com.businessanalyzer.util.Validaciones;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Valida y normaliza activos, deudas y pagos de deudas. */
final class ValidadorPatrimonio {

    /** Un local o un terreno pueden haberse comprado hace décadas. */
    private static final LocalDate FECHA_MINIMA = LocalDate.of(1950, 1, 1);
    private static final BigDecimal TASA_MAXIMA = new BigDecimal("300");

    private ValidadorPatrimonio() {
    }

    static DatosActivo activo(DatosActivo d, LocalDate hoy) {
        if (d == null || d.categoria() == null) {
            throw new ValidacionException("Selecciona el tipo de activo.");
        }
        String descripcion = Validaciones.requerido(d.descripcion(), "Descripción", 3, 150);
        LocalDate fecha = fecha(d.fecha(), d.categoria().corriente() ? "Fecha del saldo" : "Fecha de adquisición", hoy);
        BigDecimal valor = ValidadorCatalogo.monto(d.valor(), d.categoria().corriente() ? "Saldo" : "Valor de adquisición");
        if (!d.categoria().corriente() && valor.signum() == 0) {
            throw new ValidacionException("El valor de adquisición debe ser mayor que cero.");
        }

        Integer vida = null;
        BigDecimal residual = BigDecimal.ZERO;
        if (d.categoria().depreciable()) {
            vida = d.vidaUtilAnios();
            if (vida != null && (vida < 1 || vida > 100)) {
                throw new ValidacionException("La vida útil debe estar entre 1 y 100 años.");
            }
            if (vida != null && d.valorResidual() != null) {
                residual = ValidadorCatalogo.monto(d.valorResidual(), "Valor residual");
                if (residual.compareTo(valor) > 0) {
                    throw new ValidacionException("El valor residual no puede ser mayor que el valor de adquisición.");
                }
            }
        }
        return new DatosActivo(d.categoria(), descripcion, fecha, valor, vida, residual, d.cuentaId());
    }

    static DatosDeuda deuda(DatosDeuda d, LocalDate hoy) {
        if (d == null || d.categoria() == null) {
            throw new ValidacionException("Selecciona el tipo de deuda.");
        }
        String acreedor = Validaciones.requerido(d.acreedor(), "Acreedor", 2, 100);
        String descripcion = Validaciones.opcional(d.descripcion(), "Descripción", 150);
        LocalDate fecha = fecha(d.fecha(), "Fecha de inicio", hoy);
        BigDecimal monto = ValidadorCatalogo.monto(d.monto(), "Monto de la deuda");
        if (monto.signum() == 0) {
            throw new ValidacionException("El monto de la deuda debe ser mayor que cero.");
        }

        BigDecimal tasa = d.tasaAnual();
        if (tasa != null) {
            tasa = ValidadorCatalogo.monto(tasa, "Tasa de interés anual");
            if (tasa.compareTo(TASA_MAXIMA) > 0) {
                throw new ValidacionException("La tasa de interés anual no puede superar 300 %.");
            }
        }
        BigDecimal cuota = d.cuotaMensual();
        if (cuota != null) {
            cuota = ValidadorCatalogo.monto(cuota, "Cuota mensual");
            if (cuota.signum() == 0) {
                cuota = null;
            }
        }
        LocalDate vencimiento = d.vencimiento();
        if (vencimiento != null && !vencimiento.isAfter(fecha)) {
            throw new ValidacionException("El vencimiento debe ser posterior a la fecha de inicio.");
        }
        if (vencimiento != null && vencimiento.isAfter(fecha.plusYears(50))) {
            throw new ValidacionException("El plazo de la deuda no puede superar 50 años.");
        }
        return new DatosDeuda(d.categoria(), acreedor, descripcion, fecha, monto, tasa, cuota, vencimiento,
                d.cuentaId());
    }

    static BigDecimal montoPago(BigDecimal monto, BigDecimal saldo) {
        BigDecimal limpio = ValidadorCatalogo.monto(monto, "Monto del pago");
        if (limpio.signum() == 0) {
            throw new ValidacionException("El monto del pago debe ser mayor que cero.");
        }
        if (limpio.compareTo(saldo) > 0) {
            throw new ValidacionException("El pago no puede superar el saldo pendiente.");
        }
        return limpio;
    }

    /** Intereses de la cuota: opcionales (cero si no se indican). */
    static BigDecimal interes(BigDecimal interes) {
        return interes == null ? BigDecimal.ZERO : ValidadorCatalogo.monto(interes, "Intereses");
    }

    static LocalDate fecha(LocalDate fecha, String campo, LocalDate hoy) {
        if (fecha == null) {
            throw new ValidacionException("Indica la " + campo.toLowerCase() + ".");
        }
        if (fecha.isBefore(FECHA_MINIMA)) {
            throw new ValidacionException("La " + campo.toLowerCase() + " no puede ser anterior a 1950.");
        }
        if (fecha.isAfter(hoy)) {
            throw new ValidacionException("La " + campo.toLowerCase() + " no puede ser posterior a hoy.");
        }
        return fecha;
    }
}
