package com.businessanalyzer.service;

import com.businessanalyzer.analytics.EstadoResultadosFGEM;
import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM;
import com.businessanalyzer.analytics.estadistica.AnalisisAbcFGEM;
import com.businessanalyzer.analytics.estadistica.AnalisisEstadisticoFGEM;
import com.businessanalyzer.analytics.estadistica.AnalisisEstadisticoFGEM.PuntoMensualFGEM;
import com.businessanalyzer.analytics.estadistica.EstacionalidadSemanalFGEM;
import com.businessanalyzer.analytics.estadistica.EstadisticaDescriptivaFGEM;
import com.businessanalyzer.analytics.estadistica.HistogramaFGEM;
import com.businessanalyzer.analytics.estadistica.MatrizCorrelacionFGEM;
import com.businessanalyzer.analytics.estadistica.TendenciaFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.OperacionFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Análisis estadístico de las ventas de la empresa activa: descriptiva, distribución,
 * tendencia, estacionalidad semanal, clasificación ABC y correlaciones.
 * Requiere el permiso {@link PermisoFGEM#ANALISIS_VER}.
 */
public class EstadisticaServiceFGEM {

    /** Meses completos que se usan para la tendencia y las correlaciones. */
    public static final int MESES_SERIE_FGEM = 12;
    private static final int MAXIMO_CLASES_HISTOGRAMA_FGEM = 10;

    private final DatabaseFGEM databaseFGEM;
    private final OperacionRepositoryFGEM operacionesFGEM;
    private final MovimientoRepositoryFGEM movimientosFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public EstadisticaServiceFGEM(DatabaseFGEM databaseFGEM, OperacionRepositoryFGEM operacionesFGEM, MovimientoRepositoryFGEM movimientosFGEM,
                              EmpresaServiceFGEM empresasFGEM, SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.operacionesFGEM = operacionesFGEM;
        this.movimientosFGEM = movimientosFGEM;
        this.empresasFGEM = empresasFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    public AnalisisEstadisticoFGEM analizarFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.ANALISIS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        PeriodoFGEM periodoFGEM = new PeriodoFGEM(desdeFGEM, hastaFGEM);
        LocalDate hoyFGEM = LocalDate.now(relojFGEM);

        return databaseFGEM.conConexionFGEM(cFGEM -> {
            List<OperacionFGEM> ventasFGEM = operacionesFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), TipoOperacionFGEM.VENTA, desdeFGEM, hastaFGEM, false);
            double[] ticketsFGEM = ventasFGEM.stream().mapToDouble(oFGEM -> oFGEM.subtotalFGEM().doubleValue()).toArray();
            Map<LocalDate, Double> porDiaFGEM = ventasPorDiaFGEM(ventasFGEM, desdeFGEM, hastaFGEM.isAfter(hoyFGEM) ? hoyFGEM : hastaFGEM);

            YearMonth mesActualFGEM = YearMonth.from(hoyFGEM);
            YearMonth ultimoPedidoFGEM = YearMonth.from(hastaFGEM);
            boolean excluirMesActualFGEM = !ultimoPedidoFGEM.isBefore(mesActualFGEM);
            YearMonth ultimoCompletoFGEM = excluirMesActualFGEM ? mesActualFGEM.minusMonths(1) : ultimoPedidoFGEM;
            List<PuntoMensualFGEM> serieFGEM = serieMensualFGEM(cFGEM, empresaFGEM.idFGEM(), ultimoCompletoFGEM);

            double[] ventasMensualesFGEM = serieFGEM.stream().mapToDouble(PuntoMensualFGEM::ventasFGEM).toArray();
            List<RentabilidadProductoFGEM> productosFGEM = RentabilidadProductoFGEM.agruparFGEM(
                    operacionesFGEM.lineasVendidasFGEM(cFGEM, empresaFGEM.idFGEM(), desdeFGEM, hastaFGEM));

            return new AnalisisEstadisticoFGEM(
                    periodoFGEM,
                    EstadisticaDescriptivaFGEM.deFGEM(ticketsFGEM),
                    EstadisticaDescriptivaFGEM.deFGEM(porDiaFGEM.values().stream().mapToDouble(Double::doubleValue).toArray()),
                    HistogramaFGEM.deFGEM(ticketsFGEM, MAXIMO_CLASES_HISTOGRAMA_FGEM),
                    EstacionalidadSemanalFGEM.deFGEM(porDiaFGEM),
                    serieFGEM,
                    TendenciaFGEM.deFGEM(ventasMensualesFGEM),
                    MatrizCorrelacionFGEM.deFGEM(variablesFGEM(serieFGEM)),
                    AnalisisAbcFGEM.deFGEM(productosFGEM),
                    excluirMesActualFGEM);
        });
    }

    /** Total vendido cada día del rango, incluidos los días sin ventas (con cero). */
    private static Map<LocalDate, Double> ventasPorDiaFGEM(List<OperacionFGEM> ventasFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM) {
        Map<LocalDate, Double> porDiaFGEM = new TreeMap<>();
        for (LocalDate diaFGEM = desdeFGEM; !diaFGEM.isAfter(hastaFGEM); diaFGEM = diaFGEM.plusDays(1)) {
            porDiaFGEM.put(diaFGEM, 0.0);
        }
        for (OperacionFGEM ventaFGEM : ventasFGEM) {
            porDiaFGEM.computeIfPresent(ventaFGEM.fechaFGEM(), (diaFGEM, totalFGEM) -> totalFGEM + ventaFGEM.subtotalFGEM().doubleValue());
        }
        return porDiaFGEM;
    }

    /**
     * Últimos {@value #MESES_SERIE} meses completos hasta {@code ultimo}, sin los meses iniciales
     * en que todavía no había ventas (el negocio aún no operaba en el sistema).
     */
    private List<PuntoMensualFGEM> serieMensualFGEM(Connection cFGEM, long empresaIdFGEM, YearMonth ultimoFGEM) throws SQLException {
        YearMonth primeroFGEM = ultimoFGEM.minusMonths(MESES_SERIE_FGEM - 1L);
        List<PuntoMensualFGEM> puntosFGEM = new ArrayList<>();
        for (ConsultaMensualFGEM.MesFGEM mesFGEM : ConsultaMensualFGEM.porMesFGEM(cFGEM, operacionesFGEM, movimientosFGEM, empresaIdFGEM, primeroFGEM, ultimoFGEM)) {
            EstadoResultadosFGEM estadoFGEM = EstadoResultadosFGEM.deFGEM(mesFGEM.operacionesFGEM(), mesFGEM.finanzasFGEM());
            BigDecimal margenFGEM = estadoFGEM.margenBrutoFGEM();
            puntosFGEM.add(new PuntoMensualFGEM(mesFGEM.mesFGEM(),
                    estadoFGEM.ventasNetasFGEM().doubleValue(),
                    margenFGEM == null ? 0 : margenFGEM.doubleValue() * 100,
                    estadoFGEM.gastosOperativosFGEM().doubleValue(),
                    mesFGEM.finanzasFGEM().porCategoriaFGEM().getOrDefault(CategoriaFinancieraFGEM.MARKETING, BigDecimal.ZERO).doubleValue(),
                    estadoFGEM.utilidadNetaFGEM().doubleValue()));
        }
        int inicioFGEM = 0;
        while (inicioFGEM < puntosFGEM.size() && puntosFGEM.get(inicioFGEM).ventasFGEM() == 0) {
            inicioFGEM++;
        }
        return puntosFGEM.subList(inicioFGEM, puntosFGEM.size());
    }

    private static Map<String, double[]> variablesFGEM(List<PuntoMensualFGEM> serieFGEM) {
        Map<String, double[]> variablesFGEM = new LinkedHashMap<>();
        variablesFGEM.put("Ventas netas", serieFGEM.stream().mapToDouble(PuntoMensualFGEM::ventasFGEM).toArray());
        variablesFGEM.put("Margen bruto %", serieFGEM.stream().mapToDouble(PuntoMensualFGEM::margenBrutoPorcentajeFGEM).toArray());
        variablesFGEM.put("Gastos operativos", serieFGEM.stream().mapToDouble(PuntoMensualFGEM::gastosOperativosFGEM).toArray());
        variablesFGEM.put("Publicidad", serieFGEM.stream().mapToDouble(PuntoMensualFGEM::publicidadFGEM).toArray());
        variablesFGEM.put("Utilidad neta", serieFGEM.stream().mapToDouble(PuntoMensualFGEM::utilidadNetaFGEM).toArray());
        return variablesFGEM;
    }
}
