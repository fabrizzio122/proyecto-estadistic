package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosCuentaFGEM;
import com.businessanalyzer.model.DatosMovimientoTesoreriaFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

final class ValidadorTesoreriaFGEM {

    private static final LocalDate FECHA_MINIMA_FGEM = LocalDate.of(2000, 1, 1);

    private ValidadorTesoreriaFGEM() {
    }

    static DatosCuentaFGEM cuentaFGEM(DatosCuentaFGEM dFGEM) {
        if (dFGEM == null || dFGEM.tipoFGEM() == null) {
            throw new ValidacionExceptionFGEM("Indica el tipo de cuenta.");
        }
        String nombreFGEM = ValidacionesFGEM.requeridoFGEM(dFGEM.nombreFGEM(), "Nombre de la cuenta", 2, 60);
        String numeroFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.numeroFGEM(), "Número de cuenta o celular", 40);
        BigDecimal saldoFGEM = dFGEM.saldoInicialFGEM() == null ? BigDecimal.ZERO
                : ValidadorCatalogoFGEM.montoFGEM(dFGEM.saldoInicialFGEM(), "Saldo inicial");
        return new DatosCuentaFGEM(nombreFGEM, dFGEM.tipoFGEM(), numeroFGEM, saldoFGEM);
    }

    static DatosMovimientoTesoreriaFGEM movimientoFGEM(DatosMovimientoTesoreriaFGEM dFGEM, LocalDate hoyFGEM) {
        if (dFGEM == null || dFGEM.tipoFGEM() == null) {
            throw new ValidacionExceptionFGEM("Indica si es una transferencia, un aporte o un retiro.");
        }
        LocalDate fechaFGEM = fechaFGEM(dFGEM.fechaFGEM(), hoyFGEM);
        Long origenFGEM = dFGEM.tipoFGEM().usaOrigenFGEM() ? dFGEM.cuentaOrigenIdFGEM() : null;
        Long destinoFGEM = dFGEM.tipoFGEM().usaDestinoFGEM() ? dFGEM.cuentaDestinoIdFGEM() : null;
        if (dFGEM.tipoFGEM().usaOrigenFGEM() && origenFGEM == null) {
            throw new ValidacionExceptionFGEM("Indica la cuenta de la que sale el dinero.");
        }
        if (dFGEM.tipoFGEM().usaDestinoFGEM() && destinoFGEM == null) {
            throw new ValidacionExceptionFGEM("Indica la cuenta a la que entra el dinero.");
        }
        if (origenFGEM != null && Objects.equals(origenFGEM, destinoFGEM)) {
            throw new ValidacionExceptionFGEM("La cuenta de origen y la de destino deben ser distintas.");
        }
        BigDecimal montoFGEM = montoPositivoFGEM(dFGEM.montoFGEM(), "Monto");
        String descripcionFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.descripcionFGEM(), "Descripción", 150);
        return new DatosMovimientoTesoreriaFGEM(dFGEM.tipoFGEM(), fechaFGEM, origenFGEM, destinoFGEM, montoFGEM, descripcionFGEM);
    }

    static BigDecimal montoPositivoFGEM(BigDecimal montoFGEM, String campoFGEM) {
        BigDecimal limpioFGEM = ValidadorCatalogoFGEM.montoFGEM(montoFGEM, campoFGEM);
        if (limpioFGEM.signum() == 0) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" debe ser mayor que cero.");
        }
        return limpioFGEM;
    }

    static LocalDate fechaFGEM(LocalDate fechaFGEM, LocalDate hoyFGEM) {
        if (fechaFGEM == null) {
            throw new ValidacionExceptionFGEM("Indica la fecha.");
        }
        if (fechaFGEM.isBefore(FECHA_MINIMA_FGEM)) {
            throw new ValidacionExceptionFGEM("La fecha no puede ser anterior al año 2000.");
        }
        if (fechaFGEM.isAfter(hoyFGEM)) {
            throw new ValidacionExceptionFGEM("La fecha no puede ser posterior a hoy.");
        }
        return fechaFGEM;
    }
}
