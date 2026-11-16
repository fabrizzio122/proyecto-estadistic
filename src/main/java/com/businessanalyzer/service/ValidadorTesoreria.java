package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosCuenta;
import com.businessanalyzer.model.DatosMovimientoTesoreria;
import com.businessanalyzer.util.Validaciones;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Valida y normaliza cuentas de dinero, transferencias, aportes, retiros y cobros o pagos. */
final class ValidadorTesoreria {

    private static final LocalDate FECHA_MINIMA = LocalDate.of(2000, 1, 1);

    private ValidadorTesoreria() {
    }

    static DatosCuenta cuenta(DatosCuenta d) {
        if (d == null || d.tipo() == null) {
            throw new ValidacionException("Indica el tipo de cuenta.");
        }
        String nombre = Validaciones.requerido(d.nombre(), "Nombre de la cuenta", 2, 60);
        String numero = Validaciones.opcional(d.numero(), "Número de cuenta o celular", 40);
        BigDecimal saldo = d.saldoInicial() == null ? BigDecimal.ZERO
                : ValidadorCatalogo.monto(d.saldoInicial(), "Saldo inicial");
        return new DatosCuenta(nombre, d.tipo(), numero, saldo);
    }

    static DatosMovimientoTesoreria movimiento(DatosMovimientoTesoreria d, LocalDate hoy) {
        if (d == null || d.tipo() == null) {
            throw new ValidacionException("Indica si es una transferencia, un aporte o un retiro.");
        }
        LocalDate fecha = fecha(d.fecha(), hoy);
        Long origen = d.tipo().usaOrigen() ? d.cuentaOrigenId() : null;
        Long destino = d.tipo().usaDestino() ? d.cuentaDestinoId() : null;
        if (d.tipo().usaOrigen() && origen == null) {
            throw new ValidacionException("Indica la cuenta de la que sale el dinero.");
        }
        if (d.tipo().usaDestino() && destino == null) {
            throw new ValidacionException("Indica la cuenta a la que entra el dinero.");
        }
        if (origen != null && Objects.equals(origen, destino)) {
            throw new ValidacionException("La cuenta de origen y la de destino deben ser distintas.");
        }
        BigDecimal monto = montoPositivo(d.monto(), "Monto");
        String descripcion = Validaciones.opcional(d.descripcion(), "Descripción", 150);
        return new DatosMovimientoTesoreria(d.tipo(), fecha, origen, destino, monto, descripcion);
    }

    static BigDecimal montoPositivo(BigDecimal monto, String campo) {
        BigDecimal limpio = ValidadorCatalogo.monto(monto, campo);
        if (limpio.signum() == 0) {
            throw new ValidacionException("El campo \"" + campo + "\" debe ser mayor que cero.");
        }
        return limpio;
    }

    static LocalDate fecha(LocalDate fecha, LocalDate hoy) {
        if (fecha == null) {
            throw new ValidacionException("Indica la fecha.");
        }
        if (fecha.isBefore(FECHA_MINIMA)) {
            throw new ValidacionException("La fecha no puede ser anterior al año 2000.");
        }
        if (fecha.isAfter(hoy)) {
            throw new ValidacionException("La fecha no puede ser posterior a hoy.");
        }
        return fecha;
    }
}
