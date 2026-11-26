package com.businessanalyzer.service;

import com.businessanalyzer.analytics.ResumenFinancieroFGEM;
import com.businessanalyzer.analytics.ResumenOperacionesFGEM;
import com.businessanalyzer.model.MovimientoFinancieroFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class ConsultaMensualFGEM {

    record MesFGEM(YearMonth mesFGEM, ResumenOperacionesFGEM operacionesFGEM, ResumenFinancieroFGEM finanzasFGEM) {
    }

    private ConsultaMensualFGEM() {
    }

    static List<MesFGEM> porMesFGEM(Connection cFGEM, OperacionRepositoryFGEM operacionesFGEM, MovimientoRepositoryFGEM movimientosFGEM,
                            long empresaIdFGEM, YearMonth primeroFGEM, YearMonth ultimoFGEM) throws SQLException {
        Map<YearMonth, ResumenOperacionesFGEM> opsPorMesFGEM = operacionesFGEM.resumenMensualFGEM(cFGEM, empresaIdFGEM,
                primeroFGEM.atDay(1), ultimoFGEM.atEndOfMonth());
        Map<YearMonth, List<MovimientoFinancieroFGEM>> movsPorMesFGEM = movimientosFGEM.listarFGEM(cFGEM, empresaIdFGEM,
                        primeroFGEM.atDay(1), ultimoFGEM.atEndOfMonth(), false)
                .stream().collect(Collectors.groupingBy(mFGEM -> YearMonth.from(mFGEM.fechaFGEM())));

        List<MesFGEM> mesesFGEM = new ArrayList<>();
        for (YearMonth mesFGEM = primeroFGEM; !mesFGEM.isAfter(ultimoFGEM); mesFGEM = mesFGEM.plusMonths(1)) {
            mesesFGEM.add(new MesFGEM(mesFGEM,
                    opsPorMesFGEM.getOrDefault(mesFGEM, ResumenOperacionesFGEM.VACIO_FGEM),
                    ResumenFinancieroFGEM.deFGEM(movsPorMesFGEM.getOrDefault(mesFGEM, List.of()))));
        }
        return mesesFGEM;
    }
}
