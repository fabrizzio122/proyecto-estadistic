package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosMovimientoGanado;
import com.businessanalyzer.model.DatosProduccion;
import com.businessanalyzer.model.TipoMovimientoGanado;
import com.businessanalyzer.util.Validaciones;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Valida y normaliza los movimientos del hato y los registros de producción. */
final class ValidadorGanaderia {

    private static final LocalDate FECHA_MINIMA = LocalDate.of(1990, 1, 1);
    private static final int MAXIMO_ANIMALES = 100_000;

    private ValidadorGanaderia() {
    }

    static DatosMovimientoGanado movimiento(DatosMovimientoGanado d, LocalDate hoy) {
        if (d == null || d.tipo() == null) {
            throw new ValidacionException("Selecciona el tipo de movimiento.");
        }
        LocalDate fecha = fecha(d.fecha(), hoy);
        if (d.categoria() == null) {
            throw new ValidacionException("Selecciona la especie y la categoría de los animales.");
        }
        int cantidad = cantidad(d.cantidad(), "Cantidad de animales");

        TipoMovimientoGanado tipo = d.tipo();
        if (tipo == TipoMovimientoGanado.NACIMIENTO && !d.categoria().cria()) {
            throw new ValidacionException("Los nacimientos se registran en la categoría de crías ("
                    + d.categoria().especie().descripcion().toLowerCase() + ").");
        }
        var destino = d.categoriaDestino();
        if (tipo == TipoMovimientoGanado.CAMBIO_CATEGORIA) {
            if (destino == null) {
                throw new ValidacionException("Indica la categoría a la que pasan los animales.");
            }
            if (destino.especie() != d.categoria().especie()) {
                throw new ValidacionException("Los animales solo pueden pasar a una categoría de la misma especie.");
            }
            if (destino == d.categoria()) {
                throw new ValidacionException("La categoría de destino debe ser distinta de la de origen.");
            }
        } else {
            destino = null;
        }

        BigDecimal monto = null;
        if (tipo.llevaMonto() && d.monto() != null) {
            monto = ValidadorCatalogo.monto(d.monto(), "Valor total");
        }
        String causa = tipo.llevaCausa() ? Validaciones.opcional(d.causa(), "Causa", 100) : null;
        String observacion = Validaciones.opcional(d.observacion(), "Observación", 200);
        return new DatosMovimientoGanado(fecha, tipo, d.categoria(), cantidad, destino, monto, causa, observacion);
    }

    static DatosProduccion produccion(DatosProduccion d, LocalDate hoy) {
        if (d == null || d.especie() == null) {
            throw new ValidacionException("Selecciona la especie.");
        }
        if (d.producto() == null) {
            throw new ValidacionException("Selecciona el producto.");
        }
        if (!d.producto().admite(d.especie())) {
            throw new ValidacionException("La " + d.producto().descripcion().toLowerCase() + " no corresponde a "
                    + d.especie().descripcion().toLowerCase() + ".");
        }
        LocalDate fecha = fecha(d.fecha(), hoy);
        int dias = d.dias() == null ? 1 : d.dias();
        if (dias < 1 || dias > 366) {
            throw new ValidacionException("Los días del registro deben estar entre 1 y 366.");
        }
        BigDecimal cantidad = ValidadorCatalogo.monto(d.cantidad(), "Cantidad producida");
        if (cantidad.signum() == 0) {
            throw new ValidacionException("La cantidad producida debe ser mayor que cero.");
        }
        Integer animales = d.animales() == null ? null : cantidad(d.animales(), "Animales");
        BigDecimal precio = d.precioUnitario() == null ? null : ValidadorCatalogo.monto(d.precioUnitario(), "Precio");
        String observacion = Validaciones.opcional(d.observacion(), "Observación", 200);
        return new DatosProduccion(d.especie(), d.producto(), fecha, dias, cantidad, animales, precio, observacion);
    }

    static BigDecimal precio(BigDecimal precio) {
        return precio == null ? null : ValidadorCatalogo.monto(precio, "Precio por cabeza");
    }

    private static LocalDate fecha(LocalDate fecha, LocalDate hoy) {
        if (fecha == null) {
            throw new ValidacionException("Indica la fecha.");
        }
        if (fecha.isBefore(FECHA_MINIMA)) {
            throw new ValidacionException("La fecha no puede ser anterior a 1990.");
        }
        if (fecha.isAfter(hoy)) {
            throw new ValidacionException("La fecha no puede ser posterior a hoy.");
        }
        return fecha;
    }

    private static int cantidad(Integer valor, String campo) {
        if (valor == null) {
            throw new ValidacionException("El campo \"" + campo + "\" es obligatorio.");
        }
        if (valor < 1 || valor > MAXIMO_ANIMALES) {
            throw new ValidacionException("El campo \"" + campo + "\" debe estar entre 1 y " + MAXIMO_ANIMALES + ".");
        }
        return valor;
    }
}
