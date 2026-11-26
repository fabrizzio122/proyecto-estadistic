package com.businessanalyzer.service;

import com.businessanalyzer.analytics.EstadoResultadosFGEM;
import com.businessanalyzer.analytics.prediccion.PronosticadorFGEM;
import com.businessanalyzer.analytics.prediccion.ResultadoPrediccionFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class PrediccionServiceFGEM {

    public static final int MESES_HISTORIA_FGEM = 24;
    public static final int HORIZONTE_MAXIMO_FGEM = 12;

    private final DatabaseFGEM databaseFGEM;
    private final OperacionRepositoryFGEM operacionesFGEM;
    private final MovimientoRepositoryFGEM movimientosFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public PrediccionServiceFGEM(DatabaseFGEM databaseFGEM, OperacionRepositoryFGEM operacionesFGEM, MovimientoRepositoryFGEM movimientosFGEM,
                             EmpresaServiceFGEM empresasFGEM, SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.operacionesFGEM = operacionesFGEM;
        this.movimientosFGEM = movimientosFGEM;
        this.empresasFGEM = empresasFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    public ResultadoPrediccionFGEM predecirFGEM(int horizonteFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.ANALISIS_VER);
        if (horizonteFGEM < 1 || horizonteFGEM > HORIZONTE_MAXIMO_FGEM) {
            throw new ValidacionExceptionFGEM("El horizonte debe estar entre 1 y " + HORIZONTE_MAXIMO_FGEM + " meses.");
        }
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        YearMonth ultimoCompletoFGEM = YearMonth.from(LocalDate.now(relojFGEM)).minusMonths(1);
        YearMonth primeroFGEM = ultimoCompletoFGEM.minusMonths(MESES_HISTORIA_FGEM - 1L);

        List<ConsultaMensualFGEM.MesFGEM> historiaFGEM = databaseFGEM.conConexionFGEM(cFGEM ->
                ConsultaMensualFGEM.porMesFGEM(cFGEM, operacionesFGEM, movimientosFGEM, empresaFGEM.idFGEM(), primeroFGEM, ultimoCompletoFGEM));

        int inicioFGEM = 0;
        while (inicioFGEM < historiaFGEM.size() && historiaFGEM.get(inicioFGEM).operacionesFGEM().ventasNetasFGEM().signum() == 0) {
            inicioFGEM++;
        }
        List<YearMonth> mesesFGEM = new ArrayList<>();
        double[] ventasFGEM = new double[historiaFGEM.size() - inicioFGEM];
        double[] utilidadFGEM = new double[historiaFGEM.size() - inicioFGEM];
        for (int iFGEM = inicioFGEM; iFGEM < historiaFGEM.size(); iFGEM++) {
            ConsultaMensualFGEM.MesFGEM mesFGEM = historiaFGEM.get(iFGEM);
            EstadoResultadosFGEM estadoFGEM = EstadoResultadosFGEM.deFGEM(mesFGEM.operacionesFGEM(), mesFGEM.finanzasFGEM());
            mesesFGEM.add(mesFGEM.mesFGEM());
            ventasFGEM[iFGEM - inicioFGEM] = estadoFGEM.ventasNetasFGEM().doubleValue();
            utilidadFGEM[iFGEM - inicioFGEM] = estadoFGEM.utilidadNetaFGEM().doubleValue();
        }

        return new ResultadoPrediccionFGEM(mesesFGEM, ventasFGEM, utilidadFGEM, horizonteFGEM,
                PronosticadorFGEM.pronosticarFGEM("Ventas netas", ventasFGEM, horizonteFGEM, true),
                PronosticadorFGEM.pronosticarFGEM("Utilidad neta", utilidadFGEM, horizonteFGEM, false),
                true);
    }
}
