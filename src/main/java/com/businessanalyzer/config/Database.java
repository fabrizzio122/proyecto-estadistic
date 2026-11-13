package com.businessanalyzer.config;

import com.businessanalyzer.exception.DataAccessException;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Punto unico de acceso a la base de datos SQLite mediante JDBC.
 * <p>
 * Cada operacion abre su propia conexion y la cierra al terminar (en SQLite abrir una
 * conexion es barato). Todas las conexiones tienen activadas las claves foraneas.
 * <p>
 * Uso tipico desde un repositorio:
 * <pre>{@code
 * database.conConexion(c -> { ... consulta parametrizada ... });
 * database.enTransaccion(c -> { ... varias escrituras: todas o ninguna ... });
 * }</pre>
 */
public final class Database {

    /** Operacion JDBC que recibe una conexion abierta. */
    @FunctionalInterface
    public interface OperacionSql<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }

    private final Path archivo;
    private final SQLiteDataSource dataSource;

    public Database(Path archivo) {
        this.archivo = Objects.requireNonNull(archivo, "archivo").toAbsolutePath();

        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setBusyTimeout(5_000);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);

        this.dataSource = new SQLiteDataSource(config);
        this.dataSource.setUrl("jdbc:sqlite:" + this.archivo);
    }

    public Path archivo() {
        return archivo;
    }

    /** Abre una conexion nueva. Quien la llama es responsable de cerrarla. */
    public Connection abrirConexion() throws SQLException {
        return dataSource.getConnection();
    }

    /** Ejecuta una operacion de lectura o una escritura simple (autocommit). */
    public <T> T conConexion(OperacionSql<T> operacion) {
        try (Connection conexion = abrirConexion()) {
            return operacion.ejecutar(conexion);
        } catch (SQLException e) {
            throw new DataAccessException("Error al acceder a la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Ejecuta la operacion dentro de una transaccion: si algo falla se revierten
     * todos los cambios. Usar siempre que se escriba en mas de una tabla.
     */
    public <T> T enTransaccion(OperacionSql<T> operacion) {
        try (Connection conexion = abrirConexion()) {
            conexion.setAutoCommit(false);
            try {
                T resultado = operacion.ejecutar(conexion);
                conexion.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                revertir(conexion, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("La operación falló y sus cambios fueron revertidos: "
                    + e.getMessage(), e);
        }
    }

    private static void revertir(Connection conexion, Exception causa) {
        try {
            conexion.rollback();
        } catch (SQLException e) {
            causa.addSuppressed(e);
        }
    }
}
