package com.businessanalyzer.repository;

import com.businessanalyzer.config.DatabaseFGEM;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

/** Acceso a la tabla {@code configuracion} (pares clave/valor). */
public class ConfiguracionRepositoryFGEM {

    private final DatabaseFGEM databaseFGEM;

    public ConfiguracionRepositoryFGEM(DatabaseFGEM databaseFGEM) {
        this.databaseFGEM = databaseFGEM;
    }

    public Optional<String> buscarFGEM(String claveFGEM) {
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                    "SELECT valor FROM configuracion WHERE clave = ?")) {
                psFGEM.setString(1, claveFGEM);
                try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                    return rsFGEM.next() ? Optional.of(rsFGEM.getString(1)) : Optional.empty();
                }
            }
        });
    }

    /** Inserta la clave o actualiza su valor si ya existe. */
    public void guardarFGEM(String claveFGEM, String valorFGEM) {
        databaseFGEM.conConexionFGEM(cFGEM -> {
            try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                    INSERT INTO configuracion (clave, valor, actualizado_en)
                    VALUES (?, ?, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime'))
                    ON CONFLICT (clave) DO UPDATE
                        SET valor = excluded.valor,
                            actualizado_en = excluded.actualizado_en""")) {
                psFGEM.setString(1, claveFGEM);
                psFGEM.setString(2, valorFGEM);
                return psFGEM.executeUpdate();
            }
        });
    }
}
