package com.businessanalyzer.service;

import com.businessanalyzer.analytics.AnalisisFinanciero;
import com.businessanalyzer.analytics.AnalisisFinanciero.ResultadoMensual;
import com.businessanalyzer.analytics.EstadoResultados;
import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.PuntoEquilibrio;
import com.businessanalyzer.analytics.RentabilidadProducto;
import com.businessanalyzer.analytics.ResumenFinanciero;
import com.businessanalyzer.analytics.ResumenOperaciones;
import com.businessanalyzer.config.Database;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.MovimientoFinanciero;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
import com.businessanalyzer.seguridad.Sesion;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Análisis financiero de la empresa activa: combina las ventas y compras (Fase 5) con los
 * ingresos y gastos (Fase 6) para obtener el estado de resultados, los márgenes, el punto de
 * equilibrio y la rentabilidad por producto. Requiere el permiso {@link Permiso#ANALISIS_VER}.
 */
public class AnalisisService {

    /** Meses que muestra el gráfico de evolución. */
    public static final int MESES_EVOLUCION = 12;

    /** Estado de resultados y punto de equilibrio de un período (versión ligera para el dashboard). */
    public record ResumenPeriodo(Periodo periodo, EstadoResultados estado, PuntoEquilibrio equilibrio) {
    }

    private final Database database;
    private final OperacionRepository operaciones;
    private final MovimientoRepository movimientos;
    private final EmpresaService empresas;
    private final Sesion sesion;

    public AnalisisService(Database database, OperacionRepository operaciones, MovimientoRepository movimientos,
                           EmpresaService empresas, Sesion sesion) {
        this.database = database;
        this.operaciones = operaciones;
        this.movimientos = movimientos;
        this.empresas = empresas;
        this.sesion = sesion;
    }

    public AnalisisFinanciero analizar(LocalDate desde, LocalDate hasta) {
        sesion.requerir(Permiso.ANALISIS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        Periodo periodo = new Periodo(desde, hasta);
        Periodo anterior = periodo.anterior();

        return database.conConexion(c -> {
            ResumenOperaciones opsActual = operaciones.resumen(c, empresa.id(), periodo.desde(), periodo.hasta());
            ResumenFinanciero finActual = finanzas(c, empresa.id(), periodo);
            ResumenOperaciones opsAnterior = operaciones.resumen(c, empresa.id(), anterior.desde(), anterior.hasta());
            ResumenFinanciero finAnterior = finanzas(c, empresa.id(), anterior);

            List<RentabilidadProducto> productos = RentabilidadProducto.agrupar(
                    operaciones.lineasVendidas(c, empresa.id(), periodo.desde(), periodo.hasta()));

            return new AnalisisFinanciero(periodo, anterior,
                    EstadoResultados.de(opsActual, finActual),
                    EstadoResultados.de(opsAnterior, finAnterior),
                    PuntoEquilibrio.de(opsActual, finActual),
                    productos,
                    evolucion(c, empresa.id(), YearMonth.from(hasta)));
        });
    }

    public ResumenPeriodo resumen(LocalDate desde, LocalDate hasta) {
        sesion.requerir(Permiso.ANALISIS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        Periodo periodo = new Periodo(desde, hasta);
        return database.conConexion(c -> {
            ResumenOperaciones ops = operaciones.resumen(c, empresa.id(), periodo.desde(), periodo.hasta());
            ResumenFinanciero fin = finanzas(c, empresa.id(), periodo);
            return new ResumenPeriodo(periodo, EstadoResultados.de(ops, fin), PuntoEquilibrio.de(ops, fin));
        });
    }

    /** Estado de resultados de cada uno de los últimos meses, terminando en {@code ultimo}. */
    private List<ResultadoMensual> evolucion(Connection c, long empresaId, YearMonth ultimo) throws SQLException {
        YearMonth primero = ultimo.minusMonths(MESES_EVOLUCION - 1L);
        LocalDate desde = primero.atDay(1);
        LocalDate hasta = ultimo.atEndOfMonth();

        Map<YearMonth, ResumenOperaciones> opsPorMes = operaciones.resumenMensual(c, empresaId, desde, hasta);
        Map<YearMonth, List<MovimientoFinanciero>> movsPorMes = movimientos.listar(c, empresaId, desde, hasta, false)
                .stream().collect(Collectors.groupingBy(m -> YearMonth.from(m.fecha())));

        List<ResultadoMensual> meses = new ArrayList<>();
        for (YearMonth mes = primero; !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
            EstadoResultados estado = EstadoResultados.de(
                    opsPorMes.getOrDefault(mes, ResumenOperaciones.VACIO),
                    ResumenFinanciero.de(movsPorMes.getOrDefault(mes, List.of())));
            meses.add(new ResultadoMensual(mes, estado));
        }
        return meses;
    }

    private ResumenFinanciero finanzas(Connection c, long empresaId, Periodo periodo) throws SQLException {
        return ResumenFinanciero.de(movimientos.listar(c, empresaId, periodo.desde(), periodo.hasta(), false));
    }
}
