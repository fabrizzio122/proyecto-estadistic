package com.businessanalyzer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ingreso o gasto que no proviene de vender o comprar mercadería.
 *
 * @param comportamientoFGEM fijo o variable en gastos; {@code null} en ingresos
 * @param montoFGEM          importe sin impuestos, siempre positivo
 * @param cuentaIdFGEM       cuenta donde entró o de donde salió el dinero; {@code null} en los registrados
 *                       antes de la tesorería
 * @param cuentaFGEM         nombre de esa cuenta
 * @param dePagoDeudaFGEM    lo generó el pago de una deuda (intereses): se corrige desde ese pago
 */
public record MovimientoFinancieroFGEM(
        long idFGEM,
        long empresaIdFGEM,
        TipoMovimientoFGEM tipoFGEM,
        CategoriaFinancieraFGEM categoriaFGEM,
        ComportamientoFGEM comportamientoFGEM,
        LocalDate fechaFGEM,
        String descripcionFGEM,
        BigDecimal montoFGEM,
        String comprobanteFGEM,
        boolean anuladoFGEM,
        String registradoPorFGEM,
        LocalDateTime registradoEnFGEM,
        String actualizadoPorFGEM,
        LocalDateTime actualizadoEnFGEM,
        String motivoAnulacionFGEM,
        String anuladoPorFGEM,
        LocalDateTime anuladoEnFGEM,
        Long cuentaIdFGEM,
        String cuentaFGEM,
        boolean dePagoDeudaFGEM) {
}
