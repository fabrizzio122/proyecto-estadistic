package com.businessanalyzer.config;

import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class MigradorFGEM {

    static final List<MigracionFGEM> MIGRACIONES_FGEM = List.of(
            new MigracionFGEM(1, "Esquema base", "/db/migration/V1__esquema_base.sql"),
            new MigracionFGEM(2, "Usuarios, roles y permisos", "/db/migration/V2__usuarios_roles_permisos.sql"),
            new MigracionFGEM(3, "Empresas", "/db/migration/V3__empresas.sql"),
            new MigracionFGEM(4, "Productos y servicios", "/db/migration/V4__productos_servicios.sql"),
            new MigracionFGEM(5, "Ventas y compras", "/db/migration/V5__ventas_compras.sql"),
            new MigracionFGEM(6, "Gestión financiera", "/db/migration/V6__gestion_financiera.sql"),
            new MigracionFGEM(7, "Escenarios de simulación", "/db/migration/V7__escenarios.sql"),
            new MigracionFGEM(8, "Historial de informes", "/db/migration/V8__informes.sql"),
            new MigracionFGEM(9, "Activos, deudas y patrimonio", "/db/migration/V9__patrimonio.sql"),
            new MigracionFGEM(10, "Ganadería", "/db/migration/V10__ganaderia.sql"),
            new MigracionFGEM(11, "Agricultura", "/db/migration/V11__agricultura.sql"),
            new MigracionFGEM(12, "Visitas de inspección", "/db/migration/V12__visitas_inspeccion.sql"),
            new MigracionFGEM(13, "Tesorería y flujo de caja", "/db/migration/V13__tesoreria.sql"),
            new MigracionFGEM(14, "Preferencias de usuario", "/db/migration/V14__preferencias_usuario.sql"),
            new MigracionFGEM(15, "Productos perecibles", "/db/migration/V15__producto_perecible.sql")
    );

    private static final Logger logFGEM = LoggerFactory.getLogger(MigradorFGEM.class);

    public static int ultimaVersionFGEM() {
        return MIGRACIONES_FGEM.get(MIGRACIONES_FGEM.size() - 1).versionFGEM();
    }

    private final DatabaseFGEM databaseFGEM;
    private final List<MigracionFGEM> migracionesFGEM;

    public MigradorFGEM(DatabaseFGEM databaseFGEM) {
        this(databaseFGEM, MIGRACIONES_FGEM);
    }

    MigradorFGEM(DatabaseFGEM databaseFGEM, List<MigracionFGEM> migracionesFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.migracionesFGEM = List.copyOf(migracionesFGEM);
        validarOrdenFGEM(this.migracionesFGEM);
    }

    public int migrarFGEM() {
        crearTablaVersionesFGEM();
        int actualFGEM = versionActualFGEM();
        for (MigracionFGEM migracionFGEM : migracionesFGEM) {
            if (migracionFGEM.versionFGEM() > actualFGEM) {
                aplicarFGEM(migracionFGEM);
            }
        }
        return versionActualFGEM();
    }

    public int versionActualFGEM() {
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            try (Statement stFGEM = cFGEM.createStatement();
                 ResultSet rsFGEM = stFGEM.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version")) {
                return rsFGEM.next() ? rsFGEM.getInt(1) : 0;
            }
        });
    }

    private void crearTablaVersionesFGEM() {
        databaseFGEM.conConexionFGEM(cFGEM -> {
            try (Statement stFGEM = cFGEM.createStatement()) {
                stFGEM.execute("""
                        CREATE TABLE IF NOT EXISTS schema_version (
                            version     INTEGER PRIMARY KEY,
                            descripcion TEXT    NOT NULL,
                            aplicada_en TEXT    NOT NULL
                        )""");
            }
            return null;
        });
    }

    private void aplicarFGEM(MigracionFGEM migracionFGEM) {
        List<String> sentenciasFGEM = SqlScriptFGEM.dividirFGEM(leerRecursoFGEM(migracionFGEM.recursoFGEM()));
        databaseFGEM.enTransaccionFGEM(cFGEM -> {
            try (Statement stFGEM = cFGEM.createStatement()) {
                for (String sentenciaFGEM : sentenciasFGEM) {
                    stFGEM.execute(sentenciaFGEM);
                }
            }
            try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                    "INSERT INTO schema_version (version, descripcion, aplicada_en) VALUES (?, ?, ?)")) {
                psFGEM.setInt(1, migracionFGEM.versionFGEM());
                psFGEM.setString(2, migracionFGEM.descripcionFGEM());
                psFGEM.setString(3, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                psFGEM.executeUpdate();
            }
            return null;
        });
        logFGEM.info("Migracion V{} aplicada: {}", migracionFGEM.versionFGEM(), migracionFGEM.descripcionFGEM());
    }

    private static String leerRecursoFGEM(String rutaFGEM) {
        try (InputStream inFGEM = MigradorFGEM.class.getResourceAsStream(rutaFGEM)) {
            if (inFGEM == null) {
                throw new DataAccessExceptionFGEM("No se encontró el script de migración: " + rutaFGEM);
            }
            return new String(inFGEM.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException eFGEM) {
            throw new DataAccessExceptionFGEM("No se pudo leer el script de migración: " + rutaFGEM, eFGEM);
        }
    }

    private static void validarOrdenFGEM(List<MigracionFGEM> migracionesFGEM) {
        int anteriorFGEM = 0;
        for (MigracionFGEM mFGEM : migracionesFGEM) {
            if (mFGEM.versionFGEM() <= anteriorFGEM) {
                throw new IllegalArgumentException(
                        "Las migraciones deben tener versiones crecientes y sin repetir (revisar V" + mFGEM.versionFGEM() + ")");
            }
            anteriorFGEM = mFGEM.versionFGEM();
        }
    }
}
