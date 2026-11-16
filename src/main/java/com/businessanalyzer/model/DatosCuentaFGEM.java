package com.businessanalyzer.model;

import java.math.BigDecimal;

/** Datos de una cuenta de dinero tal como llegan desde el formulario. */
public record DatosCuentaFGEM(String nombreFGEM, TipoCuentaFGEM tipoFGEM, String numeroFGEM, BigDecimal saldoInicialFGEM) {
}
