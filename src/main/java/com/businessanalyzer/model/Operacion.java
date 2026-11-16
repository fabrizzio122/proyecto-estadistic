package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Venta o compra registrada.
 *
 * @param tasaImpuesto  porcentaje aplicado (18 = 18 %)
 * @param subtotal      suma de las líneas, sin impuestos
 * @param costoTotal    costo de lo vendido (en compras coincide con el subtotal)
 * @param cantidadItems número de líneas
 * @param lineas        detalle; vacío cuando se obtiene desde un listado
 * @param condicion     al contado o al crédito
 * @param cuentaId      cuenta donde entró o de donde salió el dinero (al contado); {@code null} al crédito
 *                      o en operaciones registradas antes de la tesorería
 * @param cuenta        nombre de esa cuenta
 * @param vencimiento   fecha límite de cobro o pago (al crédito); puede ser {@code null}
 * @param pagado        suma de los cobros o pagos vigentes registrados (al crédito)
 */
public record Operacion(
        long id,
        long empresaId,
        TipoOperacion tipo,
        String numero,
        LocalDate fecha,
        String contraparte,
        String documentoContraparte,
        String comprobante,
        BigDecimal tasaImpuesto,
        BigDecimal subtotal,
        BigDecimal impuesto,
        BigDecimal total,
        BigDecimal costoTotal,
        EstadoOperacion estado,
        String observaciones,
        String registradoPor,
        LocalDateTime registradoEn,
        String motivoAnulacion,
        String anuladoPor,
        LocalDateTime anuladoEn,
        int cantidadItems,
        List<LineaOperacion> lineas,
        CondicionPago condicion,
        Long cuentaId,
        String cuenta,
        LocalDate vencimiento,
        BigDecimal pagado) {

    public Operacion {
        lineas = lineas == null ? List.of() : List.copyOf(lineas);
        condicion = condicion == null ? CondicionPago.CONTADO : condicion;
        pagado = pagado == null ? BigDecimal.ZERO : pagado;
    }

    public boolean anulada() {
        return estado == EstadoOperacion.ANULADA;
    }

    public boolean alCredito() {
        return condicion == CondicionPago.CREDITO;
    }

    /** Lo que falta cobrar (venta) o pagar (compra); cero al contado o si está anulada. */
    public BigDecimal pendiente() {
        if (!alCredito() || anulada()) {
            return BigDecimal.ZERO;
        }
        return total.subtract(pagado).max(BigDecimal.ZERO);
    }

    /** Al crédito, con saldo pendiente y con el vencimiento ya pasado. */
    public boolean vencidaEn(LocalDate hoy) {
        return pendiente().signum() > 0 && vencimiento != null && vencimiento.isBefore(hoy);
    }

    /** Ganancia bruta de una venta: lo vendido sin impuestos menos su costo. */
    public BigDecimal margenBruto() {
        return subtotal.subtract(costoTotal);
    }

    /** Copia con el detalle de líneas. */
    public Operacion conLineas(List<LineaOperacion> detalle) {
        return new Operacion(id, empresaId, tipo, numero, fecha, contraparte, documentoContraparte, comprobante,
                tasaImpuesto, subtotal, impuesto, total, costoTotal, estado, observaciones, registradoPor,
                registradoEn, motivoAnulacion, anuladoPor, anuladoEn, cantidadItems, detalle, condicion, cuentaId,
                cuenta, vencimiento, pagado);
    }
}
