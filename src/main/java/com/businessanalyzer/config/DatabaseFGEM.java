package com.businessanalyzer.config;

import com.businessanalyzer.exception.DataAccessExceptionFGEM;
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
public final class DatabaseFGEM {

    /** Operacion JDBC que recibe una conexion abierta. */
    @FunctionalInterface
    public interface OperacionSqlFGEM<T> {
        T ejecutarFGEM(Connection conexionFGEM) throws SQLException;
    }

    private final Path archivoFGEM;
    private final SQLiteDataSource dataSourceFGEM;

    public DatabaseFGEM(Path archivoFGEM) {
        this.archivoFGEM = Objects.requireNonNull(archivoFGEM, "archivo").toAbsolutePath();

        SQLiteConfig configFGEM = new SQLiteConfig();
        configFGEM.enforceForeignKeys(true);
        configFGEM.setBusyTimeout(5_000);
        configFGEM.setJournalMode(SQLiteConfig.JournalMode.WAL);
        configFGEM.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);

        this.dataSourceFGEM = new SQLiteDataSource(configFGEM);
        this.dataSourceFGEM.setUrl("jdbc:sqlite:" + this.archivoFGEM);
    }

    public Path archivoFGEM() {
        return archivoFGEM;
    }

    /** Abre una conexion nueva. Quien la llama es responsable de cerrarla. */
    public Connection abrirConexionFGEM() throws SQLException {
        return dataSourceFGEM.getConnection();
    }

    /** Ejecuta una operacion de lectura o una escritura simple (autocommit). */
    public <T> T conConexionFGEM(OperacionSqlFGEM<T> operacionFGEM) {
        try (Connection conexionFGEM = abrirConexionFGEM()) {
            return operacionFGEM.ejecutarFGEM(conexionFGEM);
        } catch (SQLException eFGEM) {
            throw new DataAccessExceptionFGEM("Error al acceder a la base de datos: " + eFGEM.getMessage(), eFGEM);
        }
    }

    /**
     * Ejecuta la operacion dentro de una transaccion: si algo falla se revierten
     * todos los cambios. Usar siempre que se escriba en mas de una tabla.
     */
    public <T> T enTransaccionFGEM(OperacionSqlFGEM<T> operacionFGEM) {
        try (Connection conexionFGEM = abrirConexionFGEM()) {
            conexionFGEM.setAutoCommit(false);
            try {
                T resultadoFGEM = operacionFGEM.ejecutarFGEM(conexionFGEM);
                conexionFGEM.commit();
                return resultadoFGEM;
            } catch (SQLException | RuntimeException eFGEM) {
                revertirFGEM(conexionFGEM, eFGEM);
                throw eFGEM;
            }
        } catch (SQLException eFGEM) {
            throw new DataAccessExceptionFGEM("La operación falló y sus cambios fueron revertidos: "
                    + eFGEM.getMessage(), eFGEM);
        }
    }

    private static void revertirFGEM(Connection conexionFGEM, Exception causaFGEM) {
        try {
            conexionFGEM.rollback();
        } catch (SQLException eFGEM) {
            causaFGEM.addSuppressed(eFGEM);
        }
    }
}
