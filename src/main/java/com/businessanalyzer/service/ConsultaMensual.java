package com.businessanalyzer.service;

import com.businessanalyzer.analytics.ResumenFinanciero;
import com.businessanalyzer.analytics.ResumenOperaciones;
import com.businessanalyzer.model.MovimientoFinanciero;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Totales de ventas, compras, ingresos y gastos de cada mes de un rango, con solo dos consultas.
 * La comparten el análisis financiero (evolución) y el estadístico (tendencia y correlaciones).
 */
final class ConsultaMensual {

    record Mes(YearMonth mes, ResumenOperaciones operaciones, ResumenFinanciero finanzas) {
    }

    private ConsultaMensual() {
    }

    static List<Mes> porMes(Connection c, OperacionRepository operaciones, MovimientoRepository movimientos,
                            long empresaId, YearMonth primero, YearMonth ultimo) throws SQLException {
        Map<YearMonth, ResumenOperaciones> opsPorMes = operaciones.resumenMensual(c, empresaId,
                primero.atDay(1), ultimo.atEndOfMonth());
        Map<YearMonth, List<MovimientoFinanciero>> movsPorMes = movimientos.listar(c, empresaId,
                        primero.atDay(1), ultimo.atEndOfMonth(), false)
                .stream().collect(Collectors.groupingBy(m -> YearMonth.from(m.fecha())));

        List<Mes> meses = new ArrayList<>();
        for (YearMonth mes = primero; !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
            meses.add(new Mes(mes,
                    opsPorMes.getOrDefault(mes, ResumenOperaciones.VACIO),
                    ResumenFinanciero.de(movsPorMes.getOrDefault(mes, List.of()))));
        }
        return meses;
    }
}
