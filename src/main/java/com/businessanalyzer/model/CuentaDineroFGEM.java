package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cuenta donde la empresa guarda su dinero: la caja, una cuenta bancaria o una billetera digital.
 * Su saldo no se guarda: se calcula con el saldo inicial y los movimientos registrados.
 *
 * @param numeroFGEM       número de cuenta, CCI o celular (solo referencia); puede ser {@code null}
 * @param saldoInicialFGEM dinero que tenía la cuenta antes de cualquier movimiento registrado
 */
public record CuentaDineroFGEM(
        long idFGEM,
        long empresaIdFGEM,
        String nombreFGEM,
        TipoCuentaFGEM tipoFGEM,
        String numeroFGEM,
        BigDecimal saldoInicialFGEM,
        boolean activaFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM,
        String actualizadoPorFGEM,
        LocalDateTime actualizadoEnFGEM) {

    @Override
    public String toString() {
        return nombreFGEM;
    }
}
