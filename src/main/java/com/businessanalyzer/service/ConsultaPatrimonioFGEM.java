package com.businessanalyzer.service;

import com.businessanalyzer.analytics.EstadoResultadosFGEM;
import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.ResumenFinancieroFGEM;
import com.businessanalyzer.analytics.ganaderia.ExistenciaValorizadaFGEM;
import com.businessanalyzer.analytics.ganaderia.HatoFGEM;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneralFGEM;
import com.businessanalyzer.analytics.patrimonio.IndicadoresPatrimonioFGEM;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonialFGEM;
import com.businessanalyzer.analytics.tesoreria.SaldoCuentaFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.repository.TesoreriaRepositoryFGEM;
import com.businessanalyzer.repository.AgriculturaRepositoryFGEM;
import com.businessanalyzer.repository.ActivoRepositoryFGEM;
import com.businessanalyzer.repository.DeudaRepositoryFGEM;
import com.businessanalyzer.repository.GanaderiaRepositoryFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.businessanalyzer.repository.ProductoRepositoryFGEM;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

final class ConsultaPatrimonioFGEM {

    static final int MESES_RESULTADOS_FGEM = 12;

    private final ActivoRepositoryFGEM activosFGEM;
    private final DeudaRepositoryFGEM deudasFGEM;
    private final ProductoRepositoryFGEM productosFGEM;
    private final OperacionRepositoryFGEM operacionesFGEM;
    private final MovimientoRepositoryFGEM movimientosFGEM;
    private final GanaderiaRepositoryFGEM ganaderiaFGEM = new GanaderiaRepositoryFGEM();
    private final AgriculturaRepositoryFGEM agriculturaFGEM = new AgriculturaRepositoryFGEM();
    private final TesoreriaRepositoryFGEM tesoreriaFGEM = new TesoreriaRepositoryFGEM();

    ConsultaPatrimonioFGEM(ActivoRepositoryFGEM activosFGEM, DeudaRepositoryFGEM deudasFGEM, ProductoRepositoryFGEM productosFGEM,
                       OperacionRepositoryFGEM operacionesFGEM, MovimientoRepositoryFGEM movimientosFGEM) {
        this.activosFGEM = activosFGEM;
        this.deudasFGEM = deudasFGEM;
        this.productosFGEM = productosFGEM;
        this.operacionesFGEM = operacionesFGEM;
        this.movimientosFGEM = movimientosFGEM;
    }

    SituacionPatrimonialFGEM situacionFGEM(Connection cFGEM, long empresaIdFGEM, LocalDate hoyFGEM) throws SQLException {
        BigDecimal ganadoFGEM = ExistenciaValorizadaFGEM.totalFGEM(ExistenciaValorizadaFGEM.deFGEM(
                HatoFGEM.existenciasFGEM(ganaderiaFGEM.listarMovimientosFGEM(cFGEM, empresaIdFGEM, hoyFGEM, false), hoyFGEM),
                ganaderiaFGEM.preciosFGEM(cFGEM, empresaIdFGEM)));
        BigDecimal cultivosFGEM = cultivosEnProcesoFGEM(cFGEM, empresaIdFGEM);
        BigDecimal efectivoFGEM = SaldoCuentaFGEM.totalFGEM(SaldoCuentaFGEM.deFGEM(tesoreriaFGEM.listarCuentasFGEM(cFGEM, empresaIdFGEM, true),
                tesoreriaFGEM.flujosFGEM(cFGEM, empresaIdFGEM), hoyFGEM));
        BigDecimal porCobrarFGEM = TesoreriaServiceFGEM.pendienteFGEM(
                operacionesFGEM.listarPendientesFGEM(cFGEM, empresaIdFGEM, TipoOperacionFGEM.VENTA), null);
        BigDecimal porPagarFGEM = TesoreriaServiceFGEM.pendienteFGEM(
                operacionesFGEM.listarPendientesFGEM(cFGEM, empresaIdFGEM, TipoOperacionFGEM.COMPRA), null);
        BalanceGeneralFGEM balanceFGEM = BalanceGeneralFGEM.deFGEM(hoyFGEM, activosFGEM.listarFGEM(cFGEM, empresaIdFGEM, false),
                valorInventarioFGEM(productosFGEM.listarFGEM(cFGEM, empresaIdFGEM, true)), ganadoFGEM, cultivosFGEM, deudasFGEM.listarFGEM(cFGEM, empresaIdFGEM, false),
                efectivoFGEM, porCobrarFGEM, porPagarFGEM);

        YearMonth ultimoFGEM = YearMonth.from(hoyFGEM).minusMonths(1);
        List<ConsultaMensualFGEM.MesFGEM> historiaFGEM = ConsultaMensualFGEM.porMesFGEM(cFGEM, operacionesFGEM, movimientosFGEM, empresaIdFGEM,
                ultimoFGEM.minusMonths(MESES_RESULTADOS_FGEM - 1L), ultimoFGEM);
        YearMonth primeroFGEM = historiaFGEM.stream()
                .filter(mFGEM -> mFGEM.operacionesFGEM().ventasNetasFGEM().signum() > 0)
                .map(ConsultaMensualFGEM.MesFGEM::mesFGEM)
                .findFirst().orElse(null);
        if (primeroFGEM == null) {
            return new SituacionPatrimonialFGEM(balanceFGEM, IndicadoresPatrimonioFGEM.deFGEM(balanceFGEM, null, 0), null);
        }
        PeriodoFGEM periodoFGEM = new PeriodoFGEM(primeroFGEM.atDay(1), ultimoFGEM.atEndOfMonth());
        EstadoResultadosFGEM resultadosFGEM = EstadoResultadosFGEM.deFGEM(
                operacionesFGEM.resumenFGEM(cFGEM, empresaIdFGEM, periodoFGEM.desdeFGEM(), periodoFGEM.hastaFGEM()),
                ResumenFinancieroFGEM.deFGEM(movimientosFGEM.listarFGEM(cFGEM, empresaIdFGEM, periodoFGEM.desdeFGEM(), periodoFGEM.hastaFGEM(), false)));
        int mesesFGEM = (int) (ultimoFGEM.getYear() * 12L + ultimoFGEM.getMonthValue() - primeroFGEM.getYear() * 12L
                - primeroFGEM.getMonthValue() + 1);
        return new SituacionPatrimonialFGEM(balanceFGEM, IndicadoresPatrimonioFGEM.deFGEM(balanceFGEM, resultadosFGEM, mesesFGEM), periodoFGEM);
    }

    private BigDecimal cultivosEnProcesoFGEM(Connection cFGEM, long empresaIdFGEM) throws SQLException {
        java.util.Set<Long> enCursoFGEM = agriculturaFGEM.listarCampaniasFGEM(cFGEM, empresaIdFGEM, false).stream()
                .filter(kFGEM -> kFGEM.estadoFGEM() == com.businessanalyzer.model.EstadoCampaniaFGEM.EN_CURSO)
                .map(com.businessanalyzer.model.CampaniaFGEM::idFGEM)
                .collect(java.util.stream.Collectors.toSet());
        if (enCursoFGEM.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return agriculturaFGEM.listarCostosFGEM(cFGEM, empresaIdFGEM, null, false).stream()
                .filter(kFGEM -> enCursoFGEM.contains(kFGEM.campaniaIdFGEM()))
                .map(com.businessanalyzer.model.CostoCampaniaFGEM::montoFGEM)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal valorInventarioFGEM(List<ProductoFGEM> itemsFGEM) {
        return itemsFGEM.stream()
                .filter(pFGEM -> pFGEM.tipoFGEM() == TipoItemFGEM.PRODUCTO && pFGEM.stockFGEM() != null && pFGEM.stockFGEM().signum() > 0)
                .map(pFGEM -> pFGEM.stockFGEM().multiply(pFGEM.costoUnitarioFGEM()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
