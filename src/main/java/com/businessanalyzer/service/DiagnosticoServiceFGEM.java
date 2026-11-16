package com.businessanalyzer.service;

import com.businessanalyzer.analytics.EstadoResultadosFGEM;
import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM;
import com.businessanalyzer.analytics.ResumenFinancieroFGEM;
import com.businessanalyzer.analytics.ResumenOperacionesFGEM;
import com.businessanalyzer.analytics.agricultura.ResultadoCampaniaFGEM;
import com.businessanalyzer.analytics.diagnostico.DatosAgroFGEM;
import com.businessanalyzer.analytics.diagnostico.DatosDiagnosticoFGEM;
import com.businessanalyzer.analytics.ganaderia.IndicadoresEspecieFGEM;
import com.businessanalyzer.model.CompromisoFGEM;
import com.businessanalyzer.model.CosechaFGEM;
import com.businessanalyzer.model.CostoCampaniaFGEM;
import com.businessanalyzer.model.EspecieFGEM;
import com.businessanalyzer.model.EstadoCompromisoFGEM;
import com.businessanalyzer.model.MovimientoGanadoFGEM;
import com.businessanalyzer.model.ProduccionGanaderaFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.repository.AgriculturaRepositoryFGEM;
import com.businessanalyzer.repository.GanaderiaRepositoryFGEM;
import com.businessanalyzer.repository.VisitaRepositoryFGEM;
import java.sql.Connection;
import java.sql.SQLException;
import com.businessanalyzer.analytics.diagnostico.DiagnosticoFGEM;
import com.businessanalyzer.analytics.diagnostico.MotorDiagnosticoFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.repository.ActivoRepositoryFGEM;
import com.businessanalyzer.repository.DeudaRepositoryFGEM;
import com.businessanalyzer.repository.TesoreriaRepositoryFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.businessanalyzer.repository.ProductoRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Diagnóstico empresarial de la empresa activa: reúne los datos de los últimos meses completos y
 * los entrega a {@link MotorDiagnosticoFGEM}. Requiere el permiso {@link PermisoFGEM#ANALISIS_VER}.
 */
public class DiagnosticoServiceFGEM {

    /** Meses completos que se evalúan (y que se comparan con los mismos meses previos). */
    public static final int MESES_EVALUADOS_FGEM = 3;
    /** Historia mensual usada para la tendencia, la regularidad y la proyección. */
    public static final int MESES_HISTORIA_FGEM = 24;

    private final DatabaseFGEM databaseFGEM;
    private final OperacionRepositoryFGEM operacionesFGEM;
    private final MovimientoRepositoryFGEM movimientosFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;
    private final ConsultaPatrimonioFGEM patrimonioFGEM;
    private final GanaderiaRepositoryFGEM ganaderiaFGEM = new GanaderiaRepositoryFGEM();
    private final AgriculturaRepositoryFGEM agriculturaFGEM = new AgriculturaRepositoryFGEM();
    private final VisitaRepositoryFGEM visitasFGEM = new VisitaRepositoryFGEM();

    public DiagnosticoServiceFGEM(DatabaseFGEM databaseFGEM, OperacionRepositoryFGEM operacionesFGEM, MovimientoRepositoryFGEM movimientosFGEM,
                              EmpresaServiceFGEM empresasFGEM, SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.operacionesFGEM = operacionesFGEM;
        this.movimientosFGEM = movimientosFGEM;
        this.empresasFGEM = empresasFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
        this.patrimonioFGEM = new ConsultaPatrimonioFGEM(new ActivoRepositoryFGEM(), new DeudaRepositoryFGEM(),
                new ProductoRepositoryFGEM(), operacionesFGEM, movimientosFGEM);
    }

    public DiagnosticoFGEM diagnosticarFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.ANALISIS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        YearMonth ultimoFGEM = YearMonth.from(LocalDate.now(relojFGEM)).minusMonths(1);
        PeriodoFGEM periodoFGEM = new PeriodoFGEM(ultimoFGEM.minusMonths(MESES_EVALUADOS_FGEM - 1L).atDay(1), ultimoFGEM.atEndOfMonth());
        PeriodoFGEM anteriorFGEM = DatosDiagnosticoFGEM.comparadoConFGEM(periodoFGEM, empresaFGEM.sectorFGEM() == SectorFGEM.AGROPECUARIO);

        DatosDiagnosticoFGEM datosFGEM = databaseFGEM.conConexionFGEM(cFGEM -> {
            ResumenOperacionesFGEM opsFGEM = operacionesFGEM.resumenFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM.desdeFGEM(), periodoFGEM.hastaFGEM());
            ResumenFinancieroFGEM finFGEM = ResumenFinancieroFGEM.deFGEM(
                    movimientosFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM.desdeFGEM(), periodoFGEM.hastaFGEM(), false));
            ResumenOperacionesFGEM opsAnteriorFGEM = operacionesFGEM.resumenFGEM(cFGEM, empresaFGEM.idFGEM(), anteriorFGEM.desdeFGEM(), anteriorFGEM.hastaFGEM());
            List<RentabilidadProductoFGEM> productosFGEM = RentabilidadProductoFGEM.agruparFGEM(
                    operacionesFGEM.lineasVendidasFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM.desdeFGEM(), periodoFGEM.hastaFGEM()));
            List<ConsultaMensualFGEM.MesFGEM> historiaFGEM = ConsultaMensualFGEM.porMesFGEM(cFGEM, operacionesFGEM, movimientosFGEM, empresaFGEM.idFGEM(),
                    ultimoFGEM.minusMonths(MESES_HISTORIA_FGEM - 1L), ultimoFGEM);

            // Se omiten los meses iniciales sin ventas (el negocio aún no operaba en el sistema).
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
            return new DatosDiagnosticoFGEM(empresaFGEM.sectorFGEM(), empresaFGEM.monedaFGEM(), periodoFGEM, MESES_EVALUADOS_FGEM, opsFGEM, finFGEM,
                    opsAnteriorFGEM, mesesFGEM, ventasFGEM, utilidadFGEM, productosFGEM,
                    patrimonioFGEM.situacionFGEM(cFGEM, empresaFGEM.idFGEM(), LocalDate.now(relojFGEM)),
                    empresaFGEM.sectorFGEM() == SectorFGEM.AGROPECUARIO ? datosAgroFGEM(cFGEM, empresaFGEM.idFGEM()) : null,
                    TesoreriaServiceFGEM.proyeccionFGEM(new TesoreriaRepositoryFGEM(), new DeudaRepositoryFGEM(), cFGEM, empresaFGEM.idFGEM(),
                            LocalDate.now(relojFGEM)));
        });
        return MotorDiagnosticoFGEM.evaluarFGEM(datosFGEM);
    }

    /** Hato, campañas cerradas y compromisos de inspección de los últimos 12 meses. */
    private DatosAgroFGEM datosAgroFGEM(Connection cFGEM, long empresaIdFGEM) throws SQLException {
        LocalDate hoyFGEM = LocalDate.now(relojFGEM);
        PeriodoFGEM anioFGEM = new PeriodoFGEM(hoyFGEM.minusYears(1).plusDays(1), hoyFGEM);
        List<MovimientoGanadoFGEM> movimientosFGEM = ganaderiaFGEM.listarMovimientosFGEM(cFGEM, empresaIdFGEM, hoyFGEM, false);
        List<ProduccionGanaderaFGEM> produccionFGEM = ganaderiaFGEM.listarProduccionFGEM(cFGEM, empresaIdFGEM, anioFGEM.desdeFGEM(), hoyFGEM, false);
        List<IndicadoresEspecieFGEM> especiesFGEM = new ArrayList<>();
        for (EspecieFGEM especieFGEM : EspecieFGEM.values()) {
            IndicadoresEspecieFGEM iFGEM = IndicadoresEspecieFGEM.calcularFGEM(especieFGEM, anioFGEM, movimientosFGEM, produccionFGEM);
            if (iFGEM.tieneDatosFGEM()) {
                especiesFGEM.add(iFGEM);
            }
        }
        List<CostoCampaniaFGEM> costosFGEM = agriculturaFGEM.listarCostosFGEM(cFGEM, empresaIdFGEM, null, false);
        List<CosechaFGEM> cosechasFGEM = agriculturaFGEM.listarCosechasFGEM(cFGEM, empresaIdFGEM, null, false);
        List<ResultadoCampaniaFGEM> campaniasFGEM = agriculturaFGEM.listarCampaniasFGEM(cFGEM, empresaIdFGEM, false).stream()
                .filter(kFGEM -> kFGEM.estadoFGEM().cerradaFGEM() && kFGEM.fechaCierreFGEM() != null && !kFGEM.fechaCierreFGEM().isBefore(anioFGEM.desdeFGEM()))
                .map(kFGEM -> ResultadoCampaniaFGEM.deFGEM(kFGEM, costosFGEM, cosechasFGEM))
                .toList();
        int cumplidosFGEM = 0;
        int fallidosFGEM = 0;
        for (CompromisoFGEM kFGEM : visitasFGEM.listarCompromisosFGEM(cFGEM, empresaIdFGEM)) {
            if (kFGEM.estadoFGEM() == EstadoCompromisoFGEM.CUMPLIDO) {
                cumplidosFGEM++;
            } else if (kFGEM.estadoFGEM() == EstadoCompromisoFGEM.INCUMPLIDO || kFGEM.vencidoFGEM(hoyFGEM)) {
                fallidosFGEM++;
            }
        }
        return new DatosAgroFGEM(especiesFGEM, campaniasFGEM, cumplidosFGEM, fallidosFGEM);
    }
}
