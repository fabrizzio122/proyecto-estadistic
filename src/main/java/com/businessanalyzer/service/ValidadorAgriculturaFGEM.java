package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosCampaniaFGEM;
import com.businessanalyzer.model.DatosParcelaFGEM;
import com.businessanalyzer.model.ParcelaFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;

import java.math.BigDecimal;
import java.time.LocalDate;

final class ValidadorAgriculturaFGEM {

    private static final LocalDate FECHA_MINIMA_FGEM = LocalDate.of(2000, 1, 1);
    private static final BigDecimal AREA_MAXIMA_FGEM = new BigDecimal("100000");

    private ValidadorAgriculturaFGEM() {
    }

    static DatosParcelaFGEM parcelaFGEM(DatosParcelaFGEM dFGEM) {
        if (dFGEM == null) {
            throw new ValidacionExceptionFGEM("Faltan los datos de la parcela.");
        }
        String nombreFGEM = ValidacionesFGEM.requeridoFGEM(dFGEM.nombreFGEM(), "Nombre de la parcela", 2, 60);
        BigDecimal areaFGEM = hectareasFGEM(dFGEM.areaFGEM(), "Superficie");
        if (dFGEM.tenenciaFGEM() == null) {
            throw new ValidacionExceptionFGEM("Indica la tenencia de la parcela.");
        }
        String ubicacionFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.ubicacionFGEM(), "Ubicación", 150);
        return new DatosParcelaFGEM(nombreFGEM, areaFGEM, dFGEM.tenenciaFGEM(), dFGEM.riegoFGEM(), ubicacionFGEM);
    }

    static DatosCampaniaFGEM campaniaFGEM(DatosCampaniaFGEM dFGEM, ParcelaFGEM parcelaFGEM, LocalDate hoyFGEM) {
        if (dFGEM == null || dFGEM.cultivoFGEM() == null) {
            throw new ValidacionExceptionFGEM("Selecciona el cultivo.");
        }
        String variedadFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.variedadFGEM(), "Variedad", 60);
        LocalDate siembraFGEM = fechaFGEM(dFGEM.fechaSiembraFGEM(), "La fecha de siembra", hoyFGEM);
        BigDecimal areaFGEM = hectareasFGEM(dFGEM.areaFGEM(), "Área sembrada");
        if (areaFGEM.compareTo(parcelaFGEM.areaFGEM()) > 0) {
            throw new ValidacionExceptionFGEM("El área sembrada no puede ser mayor que la parcela ("
                    + parcelaFGEM.areaFGEM().stripTrailingZeros().toPlainString() + " ha).");
        }
        BigDecimal referenciaFGEM = dFGEM.rendimientoReferenciaFGEM() != null ? dFGEM.rendimientoReferenciaFGEM()
                : dFGEM.cultivoFGEM().rendimientoReferenciaFGEM();
        if (referenciaFGEM != null) {
            referenciaFGEM = ValidadorCatalogoFGEM.montoFGEM(referenciaFGEM, "Rendimiento de referencia");
            if (referenciaFGEM.signum() == 0) {
                referenciaFGEM = null;
            }
        }
        BigDecimal precioFGEM = dFGEM.precioChacraFGEM() == null ? null : ValidadorCatalogoFGEM.montoFGEM(dFGEM.precioChacraFGEM(), "Precio en chacra");
        String observacionFGEM = ValidacionesFGEM.opcionalFGEM(dFGEM.observacionFGEM(), "Observación", 200);
        return new DatosCampaniaFGEM(parcelaFGEM.idFGEM(), dFGEM.cultivoFGEM(), variedadFGEM, siembraFGEM, areaFGEM, referenciaFGEM, precioFGEM, observacionFGEM);
    }

    static BigDecimal montoFGEM(BigDecimal montoFGEM, String campoFGEM) {
        BigDecimal limpioFGEM = ValidadorCatalogoFGEM.montoFGEM(montoFGEM, campoFGEM);
        if (limpioFGEM.signum() == 0) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" debe ser mayor que cero.");
        }
        return limpioFGEM;
    }

    static LocalDate fechaFGEM(LocalDate fechaFGEM, String campoFGEM, LocalDate hoyFGEM) {
        if (fechaFGEM == null) {
            throw new ValidacionExceptionFGEM(campoFGEM + " es obligatoria.");
        }
        if (fechaFGEM.isBefore(FECHA_MINIMA_FGEM)) {
            throw new ValidacionExceptionFGEM(campoFGEM + " no puede ser anterior al año 2000.");
        }
        if (fechaFGEM.isAfter(hoyFGEM)) {
            throw new ValidacionExceptionFGEM(campoFGEM + " no puede ser posterior a hoy.");
        }
        return fechaFGEM;
    }

    private static BigDecimal hectareasFGEM(BigDecimal valorFGEM, String campoFGEM) {
        BigDecimal areaFGEM = ValidadorCatalogoFGEM.montoFGEM(valorFGEM, campoFGEM + " (ha)");
        if (areaFGEM.signum() == 0) {
            throw new ValidacionExceptionFGEM("La superficie debe ser mayor que cero.");
        }
        if (areaFGEM.compareTo(AREA_MAXIMA_FGEM) > 0) {
            throw new ValidacionExceptionFGEM("La superficie supera el máximo permitido.");
        }
        return areaFGEM;
    }
}
