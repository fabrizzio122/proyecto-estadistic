package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Comportamiento;
import com.businessanalyzer.model.DatosMovimiento;
import com.businessanalyzer.model.TipoMovimiento;
import com.businessanalyzer.util.Validaciones;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/** Valida y normaliza un ingreso o gasto. */
final class ValidadorMovimiento {

    private static final LocalDate FECHA_MINIMA = LocalDate.of(2000, 1, 1);

    private ValidadorMovimiento() {
    }

    /**
     * @param hoy se aceptan fechas hasta el último día del mes en curso, para poder registrar
     *            por adelantado los gastos fijos del mes (alquiler, sueldos).
     */
    static DatosMovimiento normalizar(DatosMovimiento d, LocalDate hoy) {
        if (d == null || d.tipo() == null) {
            throw new ValidacionException("Indica si es un ingreso o un gasto.");
        }
        String nombreTipo = d.tipo().descripcion().toLowerCase();
        if (d.categoria() == null) {
            throw new ValidacionException("Selecciona la categoría del " + nombreTipo + ".");
        }
        if (d.categoria().tipo() != d.tipo()) {
            throw new ValidacionException("La categoría \"" + d.categoria().descripcion()
                    + "\" no corresponde a un " + nombreTipo + ".");
        }

        Comportamiento comportamiento = null;
        if (d.tipo() == TipoMovimiento.GASTO) {
            comportamiento = d.comportamiento() != null ? d.comportamiento() : d.categoria().comportamientoSugerido();
        }

        if (d.fecha() == null) {
            throw new ValidacionException("Indica la fecha del " + nombreTipo + ".");
        }
        if (d.fecha().isBefore(FECHA_MINIMA)) {
            throw new ValidacionException("La fecha no puede ser anterior al año 2000.");
        }
        if (d.fecha().isAfter(YearMonth.from(hoy).atEndOfMonth())) {
            throw new ValidacionException("La fecha no puede ser posterior al mes en curso.");
        }

        String descripcion = Validaciones.requerido(d.descripcion(), "Descripción", 3, 150);
        BigDecimal monto = ValidadorCatalogo.monto(d.monto(), "Monto");
        if (monto.signum() == 0) {
            throw new ValidacionException("El monto debe ser mayor que cero.");
        }
        String comprobante = Validaciones.opcional(d.comprobante(), "Comprobante", 40);
        return new DatosMovimiento(d.tipo(), d.categoria(), comportamiento, d.fecha(), descripcion, monto, comprobante);
    }
}
