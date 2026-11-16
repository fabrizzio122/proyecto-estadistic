package com.businessanalyzer.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/** Ayudantes para columnas opcionales (que admiten NULL). */
final class Sql {

    private Sql() {
    }

    static Long enteroOpcional(ResultSet rs, String columna) throws SQLException {
        long valor = rs.getLong(columna);
        return rs.wasNull() ? null : valor;
    }

    static void enteroOpcional(PreparedStatement ps, int indice, Long valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setLong(indice, valor);
        }
    }
}
