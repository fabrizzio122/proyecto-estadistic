package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CondicionPagoFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM.DatosLineaFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Valida y normaliza una venta o compra antes de registrarla. */
final class ValidadorOperacionFGEM {

    static final int MAXIMO_LINEAS_FGEM = 500;
    private static final BigDecimal CANTIDAD_MAXIMA_FGEM = new BigDecimal("10000000");
    private static final BigDecimal TASA_MAXIMA_FGEM = new BigDecimal("100");
    private static final LocalDate FECHA_MINIMA_FGEM = LocalDate.of(2000, 1, 1);

    private ValidadorOperacionFGEM() {
    }

    static DatosOperacionFGEM normalizarFGEM(DatosOperacionFGEM dFGEM, LocalDate hoyFGEM) {
        if (dFGEM == null || dFGEM.tipoFGEM() == null) {
            throw new ValidacionExceptionFGEM("Faltan los datos de la operación.");
        }
        if (dFGEM.fechaFGEM() == null) {
            throw new ValidacionExceptionFGEM("Indica la fecha de la " + dFGEM.tipoFGEM().descripcionFGEM().toLowerCase() + ".");
        }
        if (dFGEM.fechaFGEM().isAfter(hoyFGEM)) {
            throw new ValidacionExceptionFGEM("La fecha no puede ser futura.");
        }
        if (dFGEM.fechaFGEM().isBefore(FECHA_MINIMA_FGEM)) {
            throw new ValidacionExceptionFGEM("La fecha no puede ser anterior al año 2000.");
        }
        String contraparteFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.contraparteFGEM(), dFGEM.tipoFGEM().contraparteFGEM(), 120);
        String documentoFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.documentoContraparteFGEM(), "Documento", 20);
        String comprobanteFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.comprobanteFGEM(), "Comprobante", 40);
        String observacionesFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.observacionesFGEM(), "Observaciones", 500);

        BigDecimal tasaFGEM = dFGEM.tasaImpuestoFGEM() == null ? BigDecimal.ZERO : dFGEM.tasaImpuestoFGEM();
        if (tasaFGEM.signum() < 0 || tasaFGEM.compareTo(TASA_MAXIMA_FGEM) > 0) {
            throw new ValidacionExceptionFGEM("El impuesto debe estar entre 0 % y 100 %.");
        }
        if (tasaFGEM.stripTrailingZeros().scale() > 2) {
            throw new ValidacionExceptionFGEM("El impuesto admite como máximo 2 decimales.");
        }

        if (dFGEM.lineasFGEM() == null || dFGEM.lineasFGEM().isEmpty()) {
            throw new ValidacionExceptionFGEM("Agrega al menos un producto o servicio.");
        }
        if (dFGEM.lineasFGEM().size() > MAXIMO_LINEAS_FGEM) {
            throw new ValidacionExceptionFGEM("Una operación admite como máximo " + MAXIMO_LINEAS_FGEM + " líneas.");
        }
        Set<Long> productosFGEM = new HashSet<>();
        List<DatosLineaFGEM> lineasFGEM = new ArrayList<>();
        for (DatosLineaFGEM lineaFGEM : dFGEM.lineasFGEM()) {
            if (!productosFGEM.add(lineaFGEM.productoIdFGEM())) {
                throw new ValidacionExceptionFGEM("Un mismo producto aparece en dos líneas. Suma las cantidades en una sola.");
            }
            BigDecimal cantidadFGEM = lineaFGEM.cantidadFGEM();
            if (cantidadFGEM == null || cantidadFGEM.signum() <= 0) {
                throw new ValidacionExceptionFGEM("Las cantidades deben ser mayores que cero.");
            }
            if (cantidadFGEM.compareTo(CANTIDAD_MAXIMA_FGEM) > 0 || cantidadFGEM.stripTrailingZeros().scale() > 4) {
                throw new ValidacionExceptionFGEM("Cantidad no válida: máximo 10 000 000 y 4 decimales.");
            }
            BigDecimal precioFGEM = ValidadorCatalogoFGEM.montoFGEM(lineaFGEM.precioUnitarioFGEM(), "Precio unitario");
            lineasFGEM.add(new DatosLineaFGEM(lineaFGEM.productoIdFGEM(), cantidadFGEM, precioFGEM));
        }

        CondicionPagoFGEM condicionFGEM = dFGEM.condicionFGEM() == null ? CondicionPagoFGEM.CONTADO : dFGEM.condicionFGEM();
        Long cuentaIdFGEM = condicionFGEM == CondicionPagoFGEM.CONTADO ? dFGEM.cuentaIdFGEM() : null;
        LocalDate vencimientoFGEM = condicionFGEM == CondicionPagoFGEM.CREDITO ? dFGEM.vencimientoFGEM() : null;
        if (vencimientoFGEM != null && vencimientoFGEM.isBefore(dFGEM.fechaFGEM())) {
            throw new ValidacionExceptionFGEM("El vencimiento no puede ser anterior a la fecha de la "
                    + dFGEM.tipoFGEM().descripcionFGEM().toLowerCase() + ".");
        }
        if (vencimientoFGEM != null && vencimientoFGEM.isAfter(dFGEM.fechaFGEM().plusYears(5))) {
            throw new ValidacionExceptionFGEM("El plazo de crédito no puede superar 5 años.");
        }
        if (condicionFGEM == CondicionPagoFGEM.CREDITO && contraparteFGEM == null) {
            throw new ValidacionExceptionFGEM("Una " + dFGEM.tipoFGEM().descripcionFGEM().toLowerCase() + " al crédito necesita el "
                    + dFGEM.tipoFGEM().contraparteFGEM().toLowerCase() + ", para saber quién debe.");
        }
        return new DatosOperacionFGEM(dFGEM.tipoFGEM(), dFGEM.fechaFGEM(), contraparteFGEM, documentoFGEM, comprobanteFGEM, tasaFGEM, observacionesFGEM,
                lineasFGEM, dFGEM.actualizarCostosFGEM(), condicionFGEM, cuentaIdFGEM, vencimientoFGEM);
    }
}
