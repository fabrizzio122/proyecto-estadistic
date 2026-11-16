package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosMovimientoGanadoFGEM;
import com.businessanalyzer.model.DatosProduccionFGEM;
import com.businessanalyzer.model.TipoMovimientoGanadoFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Valida y normaliza los movimientos del hato y los registros de producción. */
final class ValidadorGanaderiaFGEM {

    private static final LocalDate FECHA_MINIMA_FGEM = LocalDate.of(1990, 1, 1);
    private static final int MAXIMO_ANIMALES_FGEM = 100_000;

    private ValidadorGanaderiaFGEM() {
    }

    static DatosMovimientoGanadoFGEM movimientoFGEM(DatosMovimientoGanadoFGEM dFGEM, LocalDate hoyFGEM) {
        if (dFGEM == null || dFGEM.tipoFGEM() == null) {
            throw new ValidacionExceptionFGEM("Selecciona el tipo de movimiento.");
        }
        LocalDate fechaFGEM = fechaFGEM(dFGEM.fechaFGEM(), hoyFGEM);
        if (dFGEM.categoriaFGEM() == null) {
            throw new ValidacionExceptionFGEM("Selecciona la especie y la categoría de los animales.");
        }
        int cantidadFGEM = cantidadFGEM(dFGEM.cantidadFGEM(), "Cantidad de animales");

        TipoMovimientoGanadoFGEM tipoFGEM = dFGEM.tipoFGEM();
        if (tipoFGEM == TipoMovimientoGanadoFGEM.NACIMIENTO && !dFGEM.categoriaFGEM().criaFGEM()) {
            throw new ValidacionExceptionFGEM("Los nacimientos se registran en la categoría de crías ("
                    + dFGEM.categoriaFGEM().especieFGEM().descripcionFGEM().toLowerCase() + ").");
        }
        var destinoFGEM = dFGEM.categoriaDestinoFGEM();
        if (tipoFGEM == TipoMovimientoGanadoFGEM.CAMBIO_CATEGORIA) {
            if (destinoFGEM == null) {
                throw new ValidacionExceptionFGEM("Indica la categoría a la que pasan los animales.");
            }
            if (destinoFGEM.especieFGEM() != dFGEM.categoriaFGEM().especieFGEM()) {
                throw new ValidacionExceptionFGEM("Los animales solo pueden pasar a una categoría de la misma especie.");
            }
            if (destinoFGEM == dFGEM.categoriaFGEM()) {
                throw new ValidacionExceptionFGEM("La categoría de destino debe ser distinta de la de origen.");
            }
        } else {
            destinoFGEM = null;
        }

        BigDecimal montoFGEM = null;
        if (tipoFGEM.llevaMontoFGEM() && dFGEM.montoFGEM() != null) {
            montoFGEM = ValidadorCatalogoFGEM.montoFGEM(dFGEM.montoFGEM(), "Valor total");
        }
        String causaFGEM = tipoFGEM.llevaCausaFGEM() ? ValidacionesFGEM.opcionalFGEM(dFGEM.causaFGEM(), "Causa", 100) : null;
        String observacionFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.observacionFGEM(), "Observación", 200);
        return new DatosMovimientoGanadoFGEM(fechaFGEM, tipoFGEM, dFGEM.categoriaFGEM(), cantidadFGEM, destinoFGEM, montoFGEM, causaFGEM, observacionFGEM);
    }

    static DatosProduccionFGEM produccionFGEM(DatosProduccionFGEM dFGEM, LocalDate hoyFGEM) {
        if (dFGEM == null || dFGEM.especieFGEM() == null) {
            throw new ValidacionExceptionFGEM("Selecciona la especie.");
        }
        if (dFGEM.productoFGEM() == null) {
            throw new ValidacionExceptionFGEM("Selecciona el producto.");
        }
        if (!dFGEM.productoFGEM().admiteFGEM(dFGEM.especieFGEM())) {
            throw new ValidacionExceptionFGEM("La " + dFGEM.productoFGEM().descripcionFGEM().toLowerCase() + " no corresponde a "
                    + dFGEM.especieFGEM().descripcionFGEM().toLowerCase() + ".");
        }
        LocalDate fechaFGEM = fechaFGEM(dFGEM.fechaFGEM(), hoyFGEM);
        int diasFGEM = dFGEM.diasFGEM() == null ? 1 : dFGEM.diasFGEM();
        if (diasFGEM < 1 || diasFGEM > 366) {
            throw new ValidacionExceptionFGEM("Los días del registro deben estar entre 1 y 366.");
        }
        BigDecimal cantidadFGEM = ValidadorCatalogoFGEM.montoFGEM(dFGEM.cantidadFGEM(), "Cantidad producida");
        if (cantidadFGEM.signum() == 0) {
            throw new ValidacionExceptionFGEM("La cantidad producida debe ser mayor que cero.");
        }
        Integer animalesFGEM = dFGEM.animalesFGEM() == null ? null : cantidadFGEM(dFGEM.animalesFGEM(), "Animales");
        BigDecimal precioFGEM = dFGEM.precioUnitarioFGEM() == null ? null : ValidadorCatalogoFGEM.montoFGEM(dFGEM.precioUnitarioFGEM(), "Precio");
        String observacionFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.observacionFGEM(), "Observación", 200);
        return new DatosProduccionFGEM(dFGEM.especieFGEM(), dFGEM.productoFGEM(), fechaFGEM, diasFGEM, cantidadFGEM, animalesFGEM, precioFGEM, observacionFGEM);
    }

    static BigDecimal precioFGEM(BigDecimal precioFGEM) {
        return precioFGEM == null ? null : ValidadorCatalogoFGEM.montoFGEM(precioFGEM, "Precio por cabeza");
    }

    private static LocalDate fechaFGEM(LocalDate fechaFGEM, LocalDate hoyFGEM) {
        if (fechaFGEM == null) {
            throw new ValidacionExceptionFGEM("Indica la fecha.");
        }
        if (fechaFGEM.isBefore(FECHA_MINIMA_FGEM)) {
            throw new ValidacionExceptionFGEM("La fecha no puede ser anterior a 1990.");
        }
        if (fechaFGEM.isAfter(hoyFGEM)) {
            throw new ValidacionExceptionFGEM("La fecha no puede ser posterior a hoy.");
        }
        return fechaFGEM;
    }

    private static int cantidadFGEM(Integer valorFGEM, String campoFGEM) {
        if (valorFGEM == null) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" es obligatorio.");
        }
        if (valorFGEM < 1 || valorFGEM > MAXIMO_ANIMALES_FGEM) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" debe estar entre 1 y " + MAXIMO_ANIMALES_FGEM + ".");
        }
        return valorFGEM;
    }
}
