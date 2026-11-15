package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosCampania;
import com.businessanalyzer.model.DatosParcela;
import com.businessanalyzer.model.Parcela;
import com.businessanalyzer.util.Validaciones;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Valida y normaliza parcelas, campañas, costos y cosechas. */
final class ValidadorAgricultura {

    private static final LocalDate FECHA_MINIMA = LocalDate.of(2000, 1, 1);
    private static final BigDecimal AREA_MAXIMA = new BigDecimal("100000");

    private ValidadorAgricultura() {
    }

    static DatosParcela parcela(DatosParcela d) {
        if (d == null) {
            throw new ValidacionException("Faltan los datos de la parcela.");
        }
        String nombre = Validaciones.requerido(d.nombre(), "Nombre de la parcela", 2, 60);
        BigDecimal area = hectareas(d.area(), "Superficie");
        if (d.tenencia() == null) {
            throw new ValidacionException("Indica la tenencia de la parcela.");
        }
        String ubicacion = Validaciones.opcional(d.ubicacion(), "Ubicación", 150);
        return new DatosParcela(nombre, area, d.tenencia(), d.riego(), ubicacion);
    }

    static DatosCampania campania(DatosCampania d, Parcela parcela, LocalDate hoy) {
        if (d == null || d.cultivo() == null) {
            throw new ValidacionException("Selecciona el cultivo.");
        }
        String variedad = Validaciones.opcional(d.variedad(), "Variedad", 60);
        LocalDate siembra = fecha(d.fechaSiembra(), "La fecha de siembra", hoy);
        BigDecimal area = hectareas(d.area(), "Área sembrada");
        if (area.compareTo(parcela.area()) > 0) {
            throw new ValidacionException("El área sembrada no puede ser mayor que la parcela ("
                    + parcela.area().stripTrailingZeros().toPlainString() + " ha).");
        }
        BigDecimal referencia = d.rendimientoReferencia() != null ? d.rendimientoReferencia()
                : d.cultivo().rendimientoReferencia();
        if (referencia != null) {
            referencia = ValidadorCatalogo.monto(referencia, "Rendimiento de referencia");
            if (referencia.signum() == 0) {
                referencia = null;
            }
        }
        BigDecimal precio = d.precioChacra() == null ? null : ValidadorCatalogo.monto(d.precioChacra(), "Precio en chacra");
        String observacion = Validaciones.opcional(d.observacion(), "Observación", 200);
        return new DatosCampania(parcela.id(), d.cultivo(), variedad, siembra, area, referencia, precio, observacion);
    }

    static BigDecimal monto(BigDecimal monto, String campo) {
        BigDecimal limpio = ValidadorCatalogo.monto(monto, campo);
        if (limpio.signum() == 0) {
            throw new ValidacionException("El campo \"" + campo + "\" debe ser mayor que cero.");
        }
        return limpio;
    }

    static LocalDate fecha(LocalDate fecha, String campo, LocalDate hoy) {
        if (fecha == null) {
            throw new ValidacionException(campo + " es obligatoria.");
        }
        if (fecha.isBefore(FECHA_MINIMA)) {
            throw new ValidacionException(campo + " no puede ser anterior al año 2000.");
        }
        if (fecha.isAfter(hoy)) {
            throw new ValidacionException(campo + " no puede ser posterior a hoy.");
        }
        return fecha;
    }

    private static BigDecimal hectareas(BigDecimal valor, String campo) {
        BigDecimal area = ValidadorCatalogo.monto(valor, campo + " (ha)");
        if (area.signum() == 0) {
            throw new ValidacionException("La superficie debe ser mayor que cero.");
        }
        if (area.compareTo(AREA_MAXIMA) > 0) {
            throw new ValidacionException("La superficie supera el máximo permitido.");
        }
        return area;
    }
}
