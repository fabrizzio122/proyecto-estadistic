package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosCategoriaFGEM;
import com.businessanalyzer.model.DatosProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.util.MontosFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

/** Valida y normaliza productos, servicios y categorías. */
final class ValidadorCatalogoFGEM {

    static final BigDecimal MONTO_MAXIMO_FGEM = new BigDecimal("999999999");
    private static final Pattern CODIGO_FGEM = Pattern.compile("[A-Z0-9._-]+");

    private ValidadorCatalogoFGEM() {
    }

    static DatosProductoFGEM productoFGEM(DatosProductoFGEM dFGEM) {
        if (dFGEM == null) {
            throw new ValidacionExceptionFGEM("Faltan los datos del producto.");
        }
        if (dFGEM.tipoFGEM() == null) {
            throw new ValidacionExceptionFGEM("Indica si es un producto o un servicio.");
        }
        String codigoFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.codigoFGEM(), "Código", 30);
        if (codigoFGEM != null) {
            codigoFGEM = codigoFGEM.toUpperCase(Locale.ROOT);
            if (!CODIGO_FGEM.matcher(codigoFGEM).matches()) {
                throw new ValidacionExceptionFGEM(
                        "El código solo puede tener letras, números, punto, guion y guion bajo (sin espacios).");
            }
        }
        String nombreFGEM = ValidacionesFGEM.requeridoFGEM(dFGEM.nombreFGEM(), "Nombre", 2, 120);
        String descripcionFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.descripcionFGEM(), "Descripción", 500);
        if (dFGEM.unidadFGEM() == null) {
            throw new ValidacionExceptionFGEM("Selecciona la unidad de medida.");
        }
        BigDecimal costoFGEM = montoFGEM(dFGEM.costoUnitarioFGEM(), "Costo unitario");
        BigDecimal precioFGEM = montoFGEM(dFGEM.precioVentaFGEM(), "Precio de venta");
        return new DatosProductoFGEM(dFGEM.tipoFGEM(), codigoFGEM, nombreFGEM, descripcionFGEM, dFGEM.categoriaIdFGEM(), dFGEM.unidadFGEM(), costoFGEM, precioFGEM,
                dFGEM.tipoFGEM() == TipoItemFGEM.PRODUCTO && dFGEM.perecibleFGEM());
    }

    static DatosCategoriaFGEM categoriaFGEM(DatosCategoriaFGEM dFGEM) {
        if (dFGEM == null) {
            throw new ValidacionExceptionFGEM("Faltan los datos de la categoría.");
        }
        return new DatosCategoriaFGEM(
                ValidacionesFGEM.requeridoFGEM(dFGEM.nombreFGEM(), "Nombre", 2, 60),
                ValidacionesFGEM.opcionalFGEM(dFGEM.descripcionFGEM(), "Descripción", 200));
    }

    /** Monto obligatorio, no negativo, con un máximo de {@value Montos#ESCALA} decimales. */
    static BigDecimal montoFGEM(BigDecimal valorFGEM, String campoFGEM) {
        if (valorFGEM == null) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" es obligatorio.");
        }
        if (valorFGEM.signum() < 0) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" no puede ser negativo.");
        }
        if (valorFGEM.compareTo(MONTO_MAXIMO_FGEM) > 0) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" supera el máximo permitido.");
        }
        if (valorFGEM.stripTrailingZeros().scale() > MontosFGEM.ESCALA_FGEM) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" admite como máximo "
                    + MontosFGEM.ESCALA_FGEM + " decimales.");
        }
        return valorFGEM;
    }
}
