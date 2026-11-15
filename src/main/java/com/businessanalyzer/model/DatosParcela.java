package com.businessanalyzer.model;

import java.math.BigDecimal;

/** Datos de una parcela tal como llegan desde el formulario. */
public record DatosParcela(String nombre, BigDecimal area, Tenencia tenencia, boolean riego, String ubicacion) {
}
