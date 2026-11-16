package com.businessanalyzer.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/** Ayudantes para columnas opcionales (que admiten NULL). */
final class SqlFGEM {

    private SqlFGEM() {
    }

    static Long enteroOpcionalFGEM(ResultSet rsFGEM, String columnaFGEM) throws SQLException {
        long valorFGEM = rsFGEM.getLong(columnaFGEM);
        return rsFGEM.wasNull() ? null : valorFGEM;
    }

    static void enteroOpcionalFGEM(PreparedStatement psFGEM, int indiceFGEM, Long valorFGEM) throws SQLException {
        if (valorFGEM == null) {
            psFGEM.setNull(indiceFGEM, Types.INTEGER);
        } else {
            psFGEM.setLong(indiceFGEM, valorFGEM);
        }
    }
}
