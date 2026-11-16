package com.businessanalyzer.config;

import com.businessanalyzer.exception.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigradorTest {

    private static final int ULTIMA_VERSION =
            Migrador.MIGRACIONES.get(Migrador.MIGRACIONES.size() - 1).version();

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

        assertEquals(ULTIMA_VERSION, version);
        assertTrue(existeTabla("configuracion"));
        assertTrue(existeTabla("usuario"));
        assertEquals("oscuro", valorConfiguracion("app.tema"));
    }

    @Test
    void ejecutarDosVecesNoDuplicaNiFalla() {
        new Migrador(database).migrar();
        int version = new Migrador(database).migrar();

        assertEquals(ULTIMA_VERSION, version);
        assertEquals(Migrador.MIGRACIONES.size(), contarFilas("schema_version"));
        assertEquals(1, contarFilas("configuracion"));
    }

    @Test
    void unaMigracionFallidaSeRevierteCompleta() {
        new Migrador(database).migrar();
        List<Migracion> conFallo = new ArrayList<>(Migrador.MIGRACIONES);
        conFallo.add(new Migracion(ULTIMA_VERSION + 1, "Script con error", "/db/test/V99__con_error.sql"));

        assertThrows(DataAccessException.class, () -> new Migrador(database, conFallo).migrar());

        assertEquals(ULTIMA_VERSION, new Migrador(database).versionActual());
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
    void laTesoreriaConvierteCajaYBancosEnLaCuentaCaja() {
        // Base de la versión 1.1 (esquema V12) con una empresa que tenía su efectivo como activo.
        new Migrador(database, Migrador.MIGRACIONES.subList(0, 12)).migrar();
        database.enTransaccion(c -> {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("""
                        INSERT INTO empresa (id, razon_social, sector, tamano, moneda, creado_en, actualizado_en)
                        VALUES (1, 'Bodega Antigua', 'COMERCIO', 'MICRO', 'PEN', '2026-01-01T00:00:00',
                                '2026-01-01T00:00:00')""");
                st.executeUpdate("""
                        INSERT INTO activo (empresa_id, categoria, descripcion, fecha, valor, registrado_por, registrado_en)
                        VALUES (1, 'CAJA_BANCOS', 'Caja', '2026-01-01', 52500000, 'admin', '2026-01-01T00:00:00'),
                               (1, 'CAJA_BANCOS', 'Banco', '2026-01-01', 10000000, 'admin', '2026-01-01T00:00:00'),
                               (1, 'MOBILIARIO', 'Vitrina', '2026-01-01', 20000000, 'admin', '2026-01-01T00:00:00')""");
            }
            return null;
        });

        assertEquals(ULTIMA_VERSION, new Migrador(database).migrar());

        assertEquals(1, contarFilas("cuenta_dinero"));
        long saldoInicial = database.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT saldo_inicial FROM cuenta_dinero WHERE empresa_id = 1 AND nombre = 'Caja'");
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        });
        assertEquals(62_500_000L, saldoInicial, "S/ 6 250 = 5 250 de caja + 1 000 de banco");
        int vigentesCaja = database.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM activo WHERE categoria = 'CAJA_BANCOS' AND estado = 'VIGENTE'");
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        });
        assertEquals(0, vigentesCaja, "«Caja y bancos» pasa a la cuenta Caja");
        assertEquals(3, contarFilas("activo"), "Los activos se conservan como historial");
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
