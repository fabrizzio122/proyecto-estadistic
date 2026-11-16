package com.businessanalyzer.service;

import com.businessanalyzer.analytics.AnalisisFinancieroFGEM;
import com.businessanalyzer.analytics.AnalisisFinancieroFGEM.ResultadoMensualFGEM;
import com.businessanalyzer.analytics.EstadoResultadosFGEM;
import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.PuntoEquilibrioFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM;
import com.businessanalyzer.analytics.ResumenFinancieroFGEM;
import com.businessanalyzer.analytics.ResumenOperacionesFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Análisis financiero de la empresa activa: combina las ventas y compras (Fase 5) con los
 * ingresos y gastos (Fase 6) para obtener el estado de resultados, los márgenes, el punto de
 * equilibrio y la rentabilidad por producto. Requiere el permiso {@link PermisoFGEM#ANALISIS_VER}.
 */
public class AnalisisServiceFGEM {

    /** Meses que muestra el gráfico de evolución. */
    public static final int MESES_EVOLUCION_FGEM = 12;

    /** Estado de resultados y punto de equilibrio de un período (versión ligera para el dashboard). */
    public record ResumenPeriodoFGEM(PeriodoFGEM periodoFGEM, EstadoResultadosFGEM estadoFGEM, PuntoEquilibrioFGEM equilibrioFGEM) {
    }

    private final DatabaseFGEM databaseFGEM;
    private final OperacionRepositoryFGEM operacionesFGEM;
    private final MovimientoRepositoryFGEM movimientosFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final SesionFGEM sesionFGEM;

    public AnalisisServiceFGEM(DatabaseFGEM databaseFGEM, OperacionRepositoryFGEM operacionesFGEM, MovimientoRepositoryFGEM movimientosFGEM,
                           EmpresaServiceFGEM empresasFGEM, SesionFGEM sesionFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.operacionesFGEM = operacionesFGEM;
        this.movimientosFGEM = movimientosFGEM;
        this.empresasFGEM = empresasFGEM;
        this.sesionFGEM = sesionFGEM;
    }

    public AnalisisFinancieroFGEM analizarFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.ANALISIS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        PeriodoFGEM periodoFGEM = new PeriodoFGEM(desdeFGEM, hastaFGEM);
        PeriodoFGEM anteriorFGEM = periodoFGEM.anteriorFGEM();

        return databaseFGEM.conConexionFGEM(cFGEM -> {
            ResumenOperacionesFGEM opsActualFGEM = operacionesFGEM.resumenFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM.desdeFGEM(), periodoFGEM.hastaFGEM());
            ResumenFinancieroFGEM finActualFGEM = finanzasFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM);
            ResumenOperacionesFGEM opsAnteriorFGEM = operacionesFGEM.resumenFGEM(cFGEM, empresaFGEM.idFGEM(), anteriorFGEM.desdeFGEM(), anteriorFGEM.hastaFGEM());
            ResumenFinancieroFGEM finAnteriorFGEM = finanzasFGEM(cFGEM, empresaFGEM.idFGEM(), anteriorFGEM);

            List<RentabilidadProductoFGEM> productosFGEM = RentabilidadProductoFGEM.agruparFGEM(
                    operacionesFGEM.lineasVendidasFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM.desdeFGEM(), periodoFGEM.hastaFGEM()));

            return new AnalisisFinancieroFGEM(periodoFGEM, anteriorFGEM,
                    EstadoResultadosFGEM.deFGEM(opsActualFGEM, finActualFGEM),
                    EstadoResultadosFGEM.deFGEM(opsAnteriorFGEM, finAnteriorFGEM),
                    PuntoEquilibrioFGEM.deFGEM(opsActualFGEM, finActualFGEM),
                    productosFGEM,
                    evolucionFGEM(cFGEM, empresaFGEM.idFGEM(), YearMonth.from(hastaFGEM)));
        });
    }

    public ResumenPeriodoFGEM resumenFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.ANALISIS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        PeriodoFGEM periodoFGEM = new PeriodoFGEM(desdeFGEM, hastaFGEM);
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            ResumenOperacionesFGEM opsFGEM = operacionesFGEM.resumenFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM.desdeFGEM(), periodoFGEM.hastaFGEM());
            ResumenFinancieroFGEM finFGEM = finanzasFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM);
            return new ResumenPeriodoFGEM(periodoFGEM, EstadoResultadosFGEM.deFGEM(opsFGEM, finFGEM), PuntoEquilibrioFGEM.deFGEM(opsFGEM, finFGEM));
        });
    }

    /** Estado de resultados de cada uno de los últimos meses, terminando en {@code ultimo}. */
    private List<ResultadoMensualFGEM> evolucionFGEM(Connection cFGEM, long empresaIdFGEM, YearMonth ultimoFGEM) throws SQLException {
        YearMonth primeroFGEM = ultimoFGEM.minusMonths(MESES_EVOLUCION_FGEM - 1L);
        List<ResultadoMensualFGEM> mesesFGEM = new ArrayList<>();
        for (ConsultaMensualFGEM.MesFGEM mesFGEM : ConsultaMensualFGEM.porMesFGEM(cFGEM, operacionesFGEM, movimientosFGEM, empresaIdFGEM, primeroFGEM, ultimoFGEM)) {
            mesesFGEM.add(new ResultadoMensualFGEM(mesFGEM.mesFGEM(), EstadoResultadosFGEM.deFGEM(mesFGEM.operacionesFGEM(), mesFGEM.finanzasFGEM())));
        }
        return mesesFGEM;
    }

    private ResumenFinancieroFGEM finanzasFGEM(Connection cFGEM, long empresaIdFGEM, PeriodoFGEM periodoFGEM) throws SQLException {
        return ResumenFinancieroFGEM.deFGEM(movimientosFGEM.listarFGEM(cFGEM, empresaIdFGEM, periodoFGEM.desdeFGEM(), periodoFGEM.hastaFGEM(), false));
    }
}
