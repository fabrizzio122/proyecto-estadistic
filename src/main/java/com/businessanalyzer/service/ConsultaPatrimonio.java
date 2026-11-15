package com.businessanalyzer.service;

import com.businessanalyzer.analytics.EstadoResultados;
import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.ResumenFinanciero;
import com.businessanalyzer.analytics.ganaderia.ExistenciaValorizada;
import com.businessanalyzer.analytics.ganaderia.Hato;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneral;
import com.businessanalyzer.analytics.patrimonio.IndicadoresPatrimonio;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonial;
import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.TipoItem;
import com.businessanalyzer.repository.AgriculturaRepository;
import com.businessanalyzer.repository.ActivoRepository;
import com.businessanalyzer.repository.DeudaRepository;
import com.businessanalyzer.repository.GanaderiaRepository;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
import com.businessanalyzer.repository.ProductoRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Arma la situación patrimonial de una empresa (balance general + indicadores). La comparten el
 * módulo de Patrimonio y el diagnóstico empresarial.
 */
final class ConsultaPatrimonio {

    /** Meses completos de resultados usados para la rentabilidad y la capacidad de pago. */
    static final int MESES_RESULTADOS = 12;

    private final ActivoRepository activos;
    private final DeudaRepository deudas;
    private final ProductoRepository productos;
    private final OperacionRepository operaciones;
    private final MovimientoRepository movimientos;
    private final GanaderiaRepository ganaderia = new GanaderiaRepository();
    private final AgriculturaRepository agricultura = new AgriculturaRepository();

    ConsultaPatrimonio(ActivoRepository activos, DeudaRepository deudas, ProductoRepository productos,
                       OperacionRepository operaciones, MovimientoRepository movimientos) {
        this.activos = activos;
        this.deudas = deudas;
        this.productos = productos;
        this.operaciones = operaciones;
        this.movimientos = movimientos;
    }

    SituacionPatrimonial situacion(Connection c, long empresaId, LocalDate hoy) throws SQLException {
        BigDecimal ganado = ExistenciaValorizada.total(ExistenciaValorizada.de(
                Hato.existencias(ganaderia.listarMovimientos(c, empresaId, hoy, false), hoy),
                ganaderia.precios(c, empresaId)));
        BigDecimal cultivos = cultivosEnProceso(c, empresaId);
        BalanceGeneral balance = BalanceGeneral.de(hoy, activos.listar(c, empresaId, false),
                valorInventario(productos.listar(c, empresaId, true)), ganado, cultivos, deudas.listar(c, empresaId, false));

        // Resultados de los últimos 12 meses completos, desde el primero con ventas.
        YearMonth ultimo = YearMonth.from(hoy).minusMonths(1);
        List<ConsultaMensual.Mes> historia = ConsultaMensual.porMes(c, operaciones, movimientos, empresaId,
                ultimo.minusMonths(MESES_RESULTADOS - 1L), ultimo);
        YearMonth primero = historia.stream()
                .filter(m -> m.operaciones().ventasNetas().signum() > 0)
                .map(ConsultaMensual.Mes::mes)
                .findFirst().orElse(null);
        if (primero == null) {
            return new SituacionPatrimonial(balance, IndicadoresPatrimonio.de(balance, null, 0), null);
        }
        Periodo periodo = new Periodo(primero.atDay(1), ultimo.atEndOfMonth());
        EstadoResultados resultados = EstadoResultados.de(
                operaciones.resumen(c, empresaId, periodo.desde(), periodo.hasta()),
                ResumenFinanciero.de(movimientos.listar(c, empresaId, periodo.desde(), periodo.hasta(), false)));
        int meses = (int) (ultimo.getYear() * 12L + ultimo.getMonthValue() - primero.getYear() * 12L
                - primero.getMonthValue() + 1);
        return new SituacionPatrimonial(balance, IndicadoresPatrimonio.de(balance, resultados, meses), periodo);
    }

    /** Costos acumulados de las campañas agrícolas en curso (aún no cosechadas). */
    private BigDecimal cultivosEnProceso(Connection c, long empresaId) throws SQLException {
        java.util.Set<Long> enCurso = agricultura.listarCampanias(c, empresaId, false).stream()
                .filter(k -> k.estado() == com.businessanalyzer.model.EstadoCampania.EN_CURSO)
                .map(com.businessanalyzer.model.Campania::id)
                .collect(java.util.stream.Collectors.toSet());
        if (enCurso.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return agricultura.listarCostos(c, empresaId, null, false).stream()
                .filter(k -> enCurso.contains(k.campaniaId()))
                .map(com.businessanalyzer.model.CostoCampania::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Stock positivo de cada producto (incluso archivado) por su costo unitario. */
    static BigDecimal valorInventario(List<Producto> items) {
        return items.stream()
                .filter(p -> p.tipo() == TipoItem.PRODUCTO && p.stock() != null && p.stock().signum() > 0)
                .map(p -> p.stock().multiply(p.costoUnitario()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
