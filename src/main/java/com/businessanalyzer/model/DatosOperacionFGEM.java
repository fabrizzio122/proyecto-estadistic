package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Datos de una venta o compra nueva, tal como llegan desde el formulario.
 *
 * @param tasaImpuestoFGEM     porcentaje (18 = 18 %); 0 si no aplica
 * @param actualizarCostosFGEM solo en compras: usar los precios pagados como nuevo costo del catálogo
 * @param condicionFGEM        al contado o al crédito; {@code null} = al contado
 * @param cuentaIdFGEM         al contado: cuenta donde entra o de donde sale el dinero; {@code null} = la
 *                         cuenta principal de la empresa. Al crédito se ignora.
 * @param vencimientoFGEM      al crédito: fecha límite de cobro o pago (opcional)
 */
public record DatosOperacionFGEM(
        TipoOperacionFGEM tipoFGEM,
        LocalDate fechaFGEM,
        String contraparteFGEM,
        String documentoContraparteFGEM,
        String comprobanteFGEM,
        BigDecimal tasaImpuestoFGEM,
        String observacionesFGEM,
        List<DatosLineaFGEM> lineasFGEM,
        boolean actualizarCostosFGEM,
        CondicionPagoFGEM condicionFGEM,
        Long cuentaIdFGEM,
        LocalDate vencimientoFGEM) {

    /** Venta o compra al contado en la cuenta principal. */
    public DatosOperacionFGEM(TipoOperacionFGEM tipoFGEM, LocalDate fechaFGEM, String contraparteFGEM, String documentoContraparteFGEM,
                          String comprobanteFGEM, BigDecimal tasaImpuestoFGEM, String observacionesFGEM, List<DatosLineaFGEM> lineasFGEM,
                          boolean actualizarCostosFGEM) {
        this(tipoFGEM, fechaFGEM, contraparteFGEM, documentoContraparteFGEM, comprobanteFGEM, tasaImpuestoFGEM, observacionesFGEM, lineasFGEM,
                actualizarCostosFGEM, CondicionPagoFGEM.CONTADO, null, null);
    }

    /** Una línea: qué producto, cuánto y a qué precio unitario (sin impuestos). */
    public record DatosLineaFGEM(long productoIdFGEM, BigDecimal cantidadFGEM, BigDecimal precioUnitarioFGEM) {
    }
}
