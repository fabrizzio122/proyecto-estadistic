package com.businessanalyzer.repository;

import com.businessanalyzer.config.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

/** Acceso a la tabla {@code configuracion} (pares clave/valor). */
public class ConfiguracionRepository {

    private final Database database;

    public ConfiguracionRepository(Database database) {
        this.database = database;
    }

    public Optional<String> buscar(String clave) {
        return database.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT valor FROM configuracion WHERE clave = ?")) {
                ps.setString(1, clave);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
                }
            }
        });
    }

    /** Inserta la clave o actualiza su valor si ya existe. */
    public void guardar(String clave, String valor) {
        database.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO configuracion (clave, valor, actualizado_en)
                    VALUES (?, ?, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime'))
                    ON CONFLICT (clave) DO UPDATE
                        SET valor = excluded.valor,
                            actualizado_en = excluded.actualizado_en""")) {
                ps.setString(1, clave);
                ps.setString(2, valor);
                return ps.executeUpdate();
            }
        });
    }
}
