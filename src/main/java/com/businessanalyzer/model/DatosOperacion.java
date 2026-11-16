package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Datos de una venta o compra nueva, tal como llegan desde el formulario.
 *
 * @param tasaImpuesto     porcentaje (18 = 18 %); 0 si no aplica
 * @param actualizarCostos solo en compras: usar los precios pagados como nuevo costo del catálogo
 * @param condicion        al contado o al crédito; {@code null} = al contado
 * @param cuentaId         al contado: cuenta donde entra o de donde sale el dinero; {@code null} = la
 *                         cuenta principal de la empresa. Al crédito se ignora.
 * @param vencimiento      al crédito: fecha límite de cobro o pago (opcional)
 */
public record DatosOperacion(
        TipoOperacion tipo,
        LocalDate fecha,
        String contraparte,
        String documentoContraparte,
        String comprobante,
        BigDecimal tasaImpuesto,
        String observaciones,
        List<DatosLinea> lineas,
        boolean actualizarCostos,
        CondicionPago condicion,
        Long cuentaId,
        LocalDate vencimiento) {

    /** Venta o compra al contado en la cuenta principal. */
    public DatosOperacion(TipoOperacion tipo, LocalDate fecha, String contraparte, String documentoContraparte,
                          String comprobante, BigDecimal tasaImpuesto, String observaciones, List<DatosLinea> lineas,
                          boolean actualizarCostos) {
        this(tipo, fecha, contraparte, documentoContraparte, comprobante, tasaImpuesto, observaciones, lineas,
                actualizarCostos, CondicionPago.CONTADO, null, null);
    }

    /** Una línea: qué producto, cuánto y a qué precio unitario (sin impuestos). */
    public record DatosLinea(long productoId, BigDecimal cantidad, BigDecimal precioUnitario) {
    }
}
