package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Venta o compra registrada.
 *
 * @param tasaImpuestoFGEM  porcentaje aplicado (18 = 18 %)
 * @param subtotalFGEM      suma de las líneas, sin impuestos
 * @param costoTotalFGEM    costo de lo vendido (en compras coincide con el subtotal)
 * @param cantidadItemsFGEM número de líneas
 * @param lineasFGEM        detalle; vacío cuando se obtiene desde un listado
 * @param condicionFGEM     al contado o al crédito
 * @param cuentaIdFGEM      cuenta donde entró o de donde salió el dinero (al contado); {@code null} al crédito
 *                      o en operaciones registradas antes de la tesorería
 * @param cuentaFGEM        nombre de esa cuenta
 * @param vencimientoFGEM   fecha límite de cobro o pago (al crédito); puede ser {@code null}
 * @param pagadoFGEM        suma de los cobros o pagos vigentes registrados (al crédito)
 */
public record OperacionFGEM(
        long idFGEM,
        long empresaIdFGEM,
        TipoOperacionFGEM tipoFGEM,
        String numeroFGEM,
        LocalDate fechaFGEM,
        String contraparteFGEM,
        String documentoContraparteFGEM,
        String comprobanteFGEM,
        BigDecimal tasaImpuestoFGEM,
        BigDecimal subtotalFGEM,
        BigDecimal impuestoFGEM,
        BigDecimal totalFGEM,
        BigDecimal costoTotalFGEM,
        EstadoOperacionFGEM estadoFGEM,
        String observacionesFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM,
        String motivoAnulacionFGEM,
        String anuladoPorFGEM,
        LocalDateTime anuladoEnFGEM,
        int cantidadItemsFGEM,
        List<LineaOperacionFGEM> lineasFGEM,
        CondicionPagoFGEM condicionFGEM,
        Long cuentaIdFGEM,
        String cuentaFGEM,
        LocalDate vencimientoFGEM,
        BigDecimal pagadoFGEM) {

    public OperacionFGEM {
        lineasFGEM = lineasFGEM == null ? List.of() : List.copyOf(lineasFGEM);
        condicionFGEM = condicionFGEM == null ? CondicionPagoFGEM.CONTADO : condicionFGEM;
        pagadoFGEM = pagadoFGEM == null ? BigDecimal.ZERO : pagadoFGEM;
    }

    public boolean anuladaFGEM() {
        return estadoFGEM == EstadoOperacionFGEM.ANULADA;
    }

    public boolean alCreditoFGEM() {
        return condicionFGEM == CondicionPagoFGEM.CREDITO;
    }

    /** Lo que falta cobrar (venta) o pagar (compra); cero al contado o si está anulada. */
    public BigDecimal pendienteFGEM() {
        if (!alCreditoFGEM() || anuladaFGEM()) {
            return BigDecimal.ZERO;
        }
        return totalFGEM.subtract(pagadoFGEM).max(BigDecimal.ZERO);
    }

    /** Al crédito, con saldo pendiente y con el vencimiento ya pasado. */
    public boolean vencidaEnFGEM(LocalDate hoyFGEM) {
        return pendienteFGEM().signum() > 0 && vencimientoFGEM != null && vencimientoFGEM.isBefore(hoyFGEM);
    }

    /** Ganancia bruta de una venta: lo vendido sin impuestos menos su costo. */
    public BigDecimal margenBrutoFGEM() {
        return subtotalFGEM.subtract(costoTotalFGEM);
    }

    /** Copia con el detalle de líneas. */
    public OperacionFGEM conLineasFGEM(List<LineaOperacionFGEM> detalleFGEM) {
        return new OperacionFGEM(idFGEM, empresaIdFGEM, tipoFGEM, numeroFGEM, fechaFGEM, contraparteFGEM, documentoContraparteFGEM, comprobanteFGEM,
                tasaImpuestoFGEM, subtotalFGEM, impuestoFGEM, totalFGEM, costoTotalFGEM, estadoFGEM, observacionesFGEM, registradoPorFGEM,
                registradoEnFGEM, motivoAnulacionFGEM, anuladoPorFGEM, anuladoEnFGEM, cantidadItemsFGEM, detalleFGEM, condicionFGEM, cuentaIdFGEM,
                cuentaFGEM, vencimientoFGEM, pagadoFGEM);
    }
}
