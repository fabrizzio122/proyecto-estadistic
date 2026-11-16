package com.businessanalyzer.model;

import java.math.BigDecimal;

/** Datos de una parcela tal como llegan desde el formulario. */
public record DatosParcelaFGEM(String nombreFGEM, BigDecimal areaFGEM, TenenciaFGEM tenenciaFGEM, boolean riegoFGEM, String ubicacionFGEM) {
}
