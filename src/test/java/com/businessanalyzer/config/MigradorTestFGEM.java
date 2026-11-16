package com.businessanalyzer.config;

import com.businessanalyzer.exception.DataAccessExceptionFGEM;
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

class MigradorTestFGEM {

    private static final int ULTIMA_VERSION_FGEM =
            MigradorFGEM.MIGRACIONES_FGEM.get(MigradorFGEM.MIGRACIONES_FGEM.size() - 1).versionFGEM();

    @TempDir
    Path carpetaFGEM;

    private DatabaseFGEM databaseFGEM;

    @BeforeEach
    void crearBaseDatosFGEM() {
        databaseFGEM = new DatabaseFGEM(carpetaFGEM.resolve("prueba.db"));
    }

    @Test
    void aplicaElEsquemaBaseEnUnaBaseNuevaFGEM() {
        int versionFGEM = new MigradorFGEM(databaseFGEM).migrarFGEM();

        assertEquals(ULTIMA_VERSION_FGEM, versionFGEM);
        assertTrue(existeTablaFGEM("configuracion"));
        assertTrue(existeTablaFGEM("usuario"));
        assertEquals("oscuro", valorConfiguracionFGEM("app.tema"));
    }

    @Test
    void ejecutarDosVecesNoDuplicaNiFallaFGEM() {
        new MigradorFGEM(databaseFGEM).migrarFGEM();
        int versionFGEM = new MigradorFGEM(databaseFGEM).migrarFGEM();

        assertEquals(ULTIMA_VERSION_FGEM, versionFGEM);
        assertEquals(MigradorFGEM.MIGRACIONES_FGEM.size(), contarFilasFGEM("schema_version"));
        assertEquals(1, contarFilasFGEM("configuracion"));
    }

    @Test
    void unaMigracionFallidaSeRevierteCompletaFGEM() {
        new MigradorFGEM(databaseFGEM).migrarFGEM();
        List<MigracionFGEM> conFalloFGEM = new ArrayList<>(MigradorFGEM.MIGRACIONES_FGEM);
        conFalloFGEM.add(new MigracionFGEM(ULTIMA_VERSION_FGEM + 1, "Script con error", "/db/test/V99__con_error.sql"));

        assertThrows(DataAccessExceptionFGEM.class, () -> new MigradorFGEM(databaseFGEM, conFalloFGEM).migrarFGEM());

        assertEquals(ULTIMA_VERSION_FGEM, new MigradorFGEM(databaseFGEM).versionActualFGEM());
        assertFalse(existeTablaFGEM("tabla_parcial"), "La primera sentencia del script fallido no debe quedar aplicada");
    }

    @Test
    void rechazaMigracionesDesordenadasORepetidasFGEM() {
        List<MigracionFGEM> desordenadasFGEM = List.of(
                new MigracionFGEM(2, "Dos", "/x.sql"),
                new MigracionFGEM(1, "Uno", "/y.sql"));
        List<MigracionFGEM> repetidasFGEM = List.of(
                new MigracionFGEM(1, "Uno", "/x.sql"),
                new MigracionFGEM(1, "Otra vez uno", "/y.sql"));

        assertThrows(IllegalArgumentException.class, () -> new MigradorFGEM(databaseFGEM, desordenadasFGEM));
        assertThrows(IllegalArgumentException.class, () -> new MigradorFGEM(databaseFGEM, repetidasFGEM));
    }

    @Test
    void laTesoreriaConvierteCajaYBancosEnLaCuentaCajaFGEM() {
        // Base de la versión 1.1 (esquema V12) con una empresa que tenía su efectivo como activo.
        new MigradorFGEM(databaseFGEM, MigradorFGEM.MIGRACIONES_FGEM.subList(0, 12)).migrarFGEM();
        databaseFGEM.enTransaccionFGEM(cFGEM -> {
            try (Statement stFGEM = cFGEM.createStatement()) {
                stFGEM.executeUpdate("""
                        INSERT INTO empresa (id, razon_social, sector, tamano, moneda, creado_en, actualizado_en)
                        VALUES (1, 'Bodega Antigua', 'COMERCIO', 'MICRO', 'PEN', '2026-01-01T00:00:00',
                                '2026-01-01T00:00:00')""");
                stFGEM.executeUpdate("""
                        INSERT INTO activo (empresa_id, categoria, descripcion, fecha, valor, registrado_por, registrado_en)
                        VALUES (1, 'CAJA_BANCOS', 'Caja', '2026-01-01', 52500000, 'admin', '2026-01-01T00:00:00'),
                               (1, 'CAJA_BANCOS', 'Banco', '2026-01-01', 10000000, 'admin', '2026-01-01T00:00:00'),
                               (1, 'MOBILIARIO', 'Vitrina', '2026-01-01', 20000000, 'admin', '2026-01-01T00:00:00')""");
            }
            return null;
        });

        assertEquals(ULTIMA_VERSION_FGEM, new MigradorFGEM(databaseFGEM).migrarFGEM());

        assertEquals(1, contarFilasFGEM("cuenta_dinero"));
        long saldoInicialFGEM = databaseFGEM.conConexionFGEM(cFGEM -> {
            try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                    "SELECT saldo_inicial FROM cuenta_dinero WHERE empresa_id = 1 AND nombre = 'Caja'");
                 ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next() ? rsFGEM.getLong(1) : -1L;
            }
        });
        assertEquals(62_500_000L, saldoInicialFGEM, "S/ 6 250 = 5 250 de caja + 1 000 de banco");
        int vigentesCajaFGEM = databaseFGEM.conConexionFGEM(cFGEM -> {
            try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                    "SELECT COUNT(*) FROM activo WHERE categoria = 'CAJA_BANCOS' AND estado = 'VIGENTE'");
                 ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next() ? rsFGEM.getInt(1) : -1;
            }
        });
        assertEquals(0, vigentesCajaFGEM, "«Caja y bancos» pasa a la cuenta Caja");
        assertEquals(3, contarFilasFGEM("activo"), "Los activos se conservan como historial");
    }

    @Test
    void lasClavesForaneasEstanActivadasFGEM() {
        int activadasFGEM = databaseFGEM.conConexionFGEM(cFGEM -> {
            try (PreparedStatement psFGEM = cFGEM.prepareStatement("PRAGMA foreign_keys");
                 ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next() ? rsFGEM.getInt(1) : 0;
            }
        });
        assertEquals(1, activadasFGEM);
    }

    private boolean existeTablaFGEM(String nombreFGEM) {
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?")) {
                psFGEM.setString(1, nombreFGEM);
                try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                    return rsFGEM.next() && rsFGEM.getInt(1) == 1;
                }
            }
        });
    }

    private int contarFilasFGEM(String tablaFGEM) {
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            try (PreparedStatement psFGEM = cFGEM.prepareStatement("SELECT COUNT(*) FROM " + tablaFGEM);
                 ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next() ? rsFGEM.getInt(1) : 0;
            }
        });
    }

    private String valorConfiguracionFGEM(String claveFGEM) {
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            try (PreparedStatement psFGEM = cFGEM.prepareStatement("SELECT valor FROM configuracion WHERE clave = ?")) {
                psFGEM.setString(1, claveFGEM);
                try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                    return rsFGEM.next() ? rsFGEM.getString(1) : null;
                }
            }
        });
    }
}
