package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosCategoria;
import com.businessanalyzer.model.DatosProducto;
import com.businessanalyzer.util.Montos;
import com.businessanalyzer.util.Validaciones;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

/** Valida y normaliza productos, servicios y categorías. */
final class ValidadorCatalogo {

    static final BigDecimal MONTO_MAXIMO = new BigDecimal("999999999");
    private static final Pattern CODIGO = Pattern.compile("[A-Z0-9._-]+");

    private ValidadorCatalogo() {
    }

    static DatosProducto producto(DatosProducto d) {
        if (d == null) {
            throw new ValidacionException("Faltan los datos del producto.");
        }
        if (d.tipo() == null) {
            throw new ValidacionException("Indica si es un producto o un servicio.");
        }
        String codigo = Validaciones.opcional(d.codigo(), "Código", 30);
        if (codigo != null) {
            codigo = codigo.toUpperCase(Locale.ROOT);
            if (!CODIGO.matcher(codigo).matches()) {
                throw new ValidacionException(
                        "El código solo puede tener letras, números, punto, guion y guion bajo (sin espacios).");
            }
        }
        String nombre = Validaciones.requerido(d.nombre(), "Nombre", 2, 120);
        String descripcion = Validaciones.opcional(d.descripcion(), "Descripción", 500);
        if (d.unidad() == null) {
            throw new ValidacionException("Selecciona la unidad de medida.");
        }
        BigDecimal costo = monto(d.costoUnitario(), "Costo unitario");
        BigDecimal precio = monto(d.precioVenta(), "Precio de venta");
        return new DatosProducto(d.tipo(), codigo, nombre, descripcion, d.categoriaId(), d.unidad(), costo, precio);
    }

    static DatosCategoria categoria(DatosCategoria d) {
        if (d == null) {
            throw new ValidacionException("Faltan los datos de la categoría.");
        }
        return new DatosCategoria(
                Validaciones.requerido(d.nombre(), "Nombre", 2, 60),
                Validaciones.opcional(d.descripcion(), "Descripción", 200));
    }

    /** Monto obligatorio, no negativo, con un máximo de {@value Montos#ESCALA} decimales. */
    static BigDecimal monto(BigDecimal valor, String campo) {
        if (valor == null) {
            throw new ValidacionException("El campo \"" + campo + "\" es obligatorio.");
        }
        if (valor.signum() < 0) {
            throw new ValidacionException("El campo \"" + campo + "\" no puede ser negativo.");
        }
        if (valor.compareTo(MONTO_MAXIMO) > 0) {
            throw new ValidacionException("El campo \"" + campo + "\" supera el máximo permitido.");
        }
        if (valor.stripTrailingZeros().scale() > Montos.ESCALA) {
            throw new ValidacionException("El campo \"" + campo + "\" admite como máximo "
                    + Montos.ESCALA + " decimales.");
        }
        return valor;
    }
}
