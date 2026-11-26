package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.ComportamientoFGEM;
import com.businessanalyzer.model.DatosMovimientoFGEM;
import com.businessanalyzer.model.TipoMovimientoFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

final class ValidadorMovimientoFGEM {

    private static final LocalDate FECHA_MINIMA_FGEM = LocalDate.of(2000, 1, 1);

    private ValidadorMovimientoFGEM() {
    }

    static DatosMovimientoFGEM normalizarFGEM(DatosMovimientoFGEM dFGEM, LocalDate hoyFGEM) {
        if (dFGEM == null || dFGEM.tipoFGEM() == null) {
            throw new ValidacionExceptionFGEM("Indica si es un ingreso o un gasto.");
        }
        String nombreTipoFGEM = dFGEM.tipoFGEM().descripcionFGEM().toLowerCase();
        if (dFGEM.categoriaFGEM() == null) {
            throw new ValidacionExceptionFGEM("Selecciona la categoría del " + nombreTipoFGEM + ".");
        }
        if (dFGEM.categoriaFGEM().tipoFGEM() != dFGEM.tipoFGEM()) {
            throw new ValidacionExceptionFGEM("La categoría \"" + dFGEM.categoriaFGEM().descripcionFGEM()
                    + "\" no corresponde a un " + nombreTipoFGEM + ".");
        }

        ComportamientoFGEM comportamientoFGEM = null;
        if (dFGEM.tipoFGEM() == TipoMovimientoFGEM.GASTO) {
            comportamientoFGEM = dFGEM.comportamientoFGEM() != null ? dFGEM.comportamientoFGEM() : dFGEM.categoriaFGEM().comportamientoSugeridoFGEM();
        }

        if (dFGEM.fechaFGEM() == null) {
            throw new ValidacionExceptionFGEM("Indica la fecha del " + nombreTipoFGEM + ".");
        }
        if (dFGEM.fechaFGEM().isBefore(FECHA_MINIMA_FGEM)) {
            throw new ValidacionExceptionFGEM("La fecha no puede ser anterior al año 2000.");
        }
        if (dFGEM.fechaFGEM().isAfter(YearMonth.from(hoyFGEM).atEndOfMonth())) {
            throw new ValidacionExceptionFGEM("La fecha no puede ser posterior al mes en curso.");
        }

        String descripcionFGEM = ValidacionesFGEM.requeridoFGEM(dFGEM.descripcionFGEM(), "Descripción", 3, 150);
        BigDecimal montoFGEM = ValidadorCatalogoFGEM.montoFGEM(dFGEM.montoFGEM(), "Monto");
        if (montoFGEM.signum() == 0) {
            throw new ValidacionExceptionFGEM("El monto debe ser mayor que cero.");
        }
        String comprobanteFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.comprobanteFGEM(), "Comprobante", 40);
        return new DatosMovimientoFGEM(dFGEM.tipoFGEM(), dFGEM.categoriaFGEM(), comportamientoFGEM, dFGEM.fechaFGEM(), descripcionFGEM, montoFGEM, comprobanteFGEM,
                dFGEM.cuentaIdFGEM());
    }
}
