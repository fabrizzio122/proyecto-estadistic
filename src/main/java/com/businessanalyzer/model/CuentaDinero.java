package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cuenta donde la empresa guarda su dinero: la caja, una cuenta bancaria o una billetera digital.
 * Su saldo no se guarda: se calcula con el saldo inicial y los movimientos registrados.
 *
 * @param numero       número de cuenta, CCI o celular (solo referencia); puede ser {@code null}
 * @param saldoInicial dinero que tenía la cuenta antes de cualquier movimiento registrado
 */
public record CuentaDinero(
        long id,
        long empresaId,
        String nombre,
        TipoCuenta tipo,
        String numero,
        BigDecimal saldoInicial,
        boolean activa,
        String registradoPor,
        LocalDateTime registradoEn,
        String actualizadoPor,
        LocalDateTime actualizadoEn) {

    @Override
    public String toString() {
        return nombre;
    }
}
