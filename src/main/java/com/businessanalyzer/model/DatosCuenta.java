package com.businessanalyzer.model;

import java.math.BigDecimal;

/** Datos de una cuenta de dinero tal como llegan desde el formulario. */
public record DatosCuenta(String nombre, TipoCuenta tipo, String numero, BigDecimal saldoInicial) {
}
