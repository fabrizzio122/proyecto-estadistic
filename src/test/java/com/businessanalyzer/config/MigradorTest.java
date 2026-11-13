package com.businessanalyzer.config;

import com.businessanalyzer.exception.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigradorTest {

    @TempDir
    Path carpeta;

    private Database database;

    @BeforeEach
    void crearBaseDatos() {
        database = new Database(carpeta.resolve("prueba.db"));
    }

    @Test
    void aplicaElEsquemaBaseEnUnaBaseNueva() {
        int version = new Migrador(database).migrar();

        assertEquals(1, version);
        assertTrue(existeTabla("configuracion"));
        assertEquals("oscuro", valorConfiguracion("app.tema"));
    }

    @Test
    void ejecutarDosVecesNoDuplicaNiFalla() {
        new Migrador(database).migrar();
        int version = new Migrador(database).migrar();

        assertEquals(1, version);
        assertEquals(1, contarFilas("schema_version"));
        assertEquals(1, contarFilas("configuracion"));
    }

    @Test
    void unaMigracionFallidaSeRevierteCompleta() {
        new Migrador(database).migrar();
        List<Migracion> conFallo = new ArrayList<>(Migrador.MIGRACIONES);
        conFallo.add(new Migracion(2, "Script con error", "/db/test/V2__con_error.sql"));

        assertThrows(DataAccessException.class, () -> new Migrador(database, conFallo).migrar());

        assertEquals(1, new Migrador(database).versionActual());
        assertFalse(existeTabla("tabla_parcial"), "La primera sentencia del script fallido no debe quedar aplicada");
    }

    @Test
    void rechazaMigracionesDesordenadasORepetidas() {
        List<Migracion> desordenadas = List.of(
                new Migracion(2, "Dos", "/x.sql"),
                new Migracion(1, "Uno", "/y.sql"));
        List<Migracion> repetidas = List.of(
                new Migracion(1, "Uno", "/x.sql"),
                new Migracion(1, "Otra vez uno", "/y.sql"));

        assertThrows(IllegalArgumentException.class, () -> new Migrador(database, desordenadas));
        assertThrows(IllegalArgumentException.class, () -> new Migrador(database, repetidas));
    }

    @Test
    void lasClavesForaneasEstanActivadas() {
        int activadas = database.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement("PRAGMA foreign_keys");
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
        assertEquals(1, activadas);
    }

    private boolean existeTabla(String nombre) {
        return database.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?")) {
                ps.setString(1, nombre);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) == 1;
                }
            }
        });
    }

    private int contarFilas(String tabla) {
        return database.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM " + tabla);
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    private String valorConfiguracion(String clave) {
        return database.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT valor FROM configuracion WHERE clave = ?")) {
                ps.setString(1, clave);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        });
    }
}
