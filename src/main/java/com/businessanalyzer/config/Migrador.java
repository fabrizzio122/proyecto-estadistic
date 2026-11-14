package com.businessanalyzer.config;

import com.businessanalyzer.exception.DataAccessException;
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

/**
 * Aplica los scripts de {@code src/main/resources/db/migration} en orden.
 * <p>
 * Cada migracion se ejecuta una sola vez y dentro de una transaccion: si un script falla,
 * la base de datos queda exactamente como estaba. La version aplicada se guarda en la
 * tabla {@code schema_version}.
 * <p>
 * <b>Para cambiar el esquema en fases futuras:</b> crear un nuevo archivo
 * {@code V<n>__descripcion.sql} y agregarlo al final de {@link #MIGRACIONES}.
 * Nunca modificar un script que ya fue aplicado.
 */
public final class Migrador {

    static final List<Migracion> MIGRACIONES = List.of(
            new Migracion(1, "Esquema base", "/db/migration/V1__esquema_base.sql"),
            new Migracion(2, "Usuarios, roles y permisos", "/db/migration/V2__usuarios_roles_permisos.sql"),
            new Migracion(3, "Empresas", "/db/migration/V3__empresas.sql"),
            new Migracion(4, "Productos y servicios", "/db/migration/V4__productos_servicios.sql"),
            new Migracion(5, "Ventas y compras", "/db/migration/V5__ventas_compras.sql"),
            new Migracion(6, "Gestión financiera", "/db/migration/V6__gestion_financiera.sql"),
            new Migracion(7, "Escenarios de simulación", "/db/migration/V7__escenarios.sql"),
            new Migracion(8, "Historial de informes", "/db/migration/V8__informes.sql")
    );

    private static final Logger log = LoggerFactory.getLogger(Migrador.class);

    private final Database database;
    private final List<Migracion> migraciones;

    public Migrador(Database database) {
        this(database, MIGRACIONES);
    }

    Migrador(Database database, List<Migracion> migraciones) {
        this.database = database;
        this.migraciones = List.copyOf(migraciones);
        validarOrden(this.migraciones);
    }

    /** Aplica las migraciones pendientes y devuelve la version final del esquema. */
    public int migrar() {
        crearTablaVersiones();
        int actual = versionActual();
        for (Migracion migracion : migraciones) {
            if (migracion.version() > actual) {
                aplicar(migracion);
            }
        }
        return versionActual();
    }

    public int versionActual() {
        return database.conConexion(c -> {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    private void crearTablaVersiones() {
        database.conConexion(c -> {
            try (Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE IF NOT EXISTS schema_version (
                            version     INTEGER PRIMARY KEY,
                            descripcion TEXT    NOT NULL,
                            aplicada_en TEXT    NOT NULL
                        )""");
            }
            return null;
        });
    }

    private void aplicar(Migracion migracion) {
        List<String> sentencias = SqlScript.dividir(leerRecurso(migracion.recurso()));
        database.enTransaccion(c -> {
            try (Statement st = c.createStatement()) {
                for (String sentencia : sentencias) {
                    st.execute(sentencia);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO schema_version (version, descripcion, aplicada_en) VALUES (?, ?, ?)")) {
                ps.setInt(1, migracion.version());
                ps.setString(2, migracion.descripcion());
                ps.setString(3, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                ps.executeUpdate();
            }
            return null;
        });
        log.info("Migracion V{} aplicada: {}", migracion.version(), migracion.descripcion());
    }

    private static String leerRecurso(String ruta) {
        try (InputStream in = Migrador.class.getResourceAsStream(ruta)) {
            if (in == null) {
                throw new DataAccessException("No se encontró el script de migración: " + ruta);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataAccessException("No se pudo leer el script de migración: " + ruta, e);
        }
    }

    private static void validarOrden(List<Migracion> migraciones) {
        int anterior = 0;
        for (Migracion m : migraciones) {
            if (m.version() <= anterior) {
                throw new IllegalArgumentException(
                        "Las migraciones deben tener versiones crecientes y sin repetir (revisar V" + m.version() + ")");
            }
            anterior = m.version();
        }
    }
}
