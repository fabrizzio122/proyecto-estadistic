package com.businessanalyzer.config;

import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public final class DatabaseFGEM {

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

    public Connection abrirConexionFGEM() throws SQLException {
        return dataSourceFGEM.getConnection();
    }

    private final ThreadLocal<Connection> transaccionActualFGEM = new ThreadLocal<>();

    public <T> T conConexionFGEM(OperacionSqlFGEM<T> operacionFGEM) {
        Connection compartidaFGEM = transaccionActualFGEM.get();
        if (compartidaFGEM != null) {
            return enCompartidaFGEM(compartidaFGEM, operacionFGEM, "Error al acceder a la base de datos: ");
        }
        try (Connection conexionFGEM = abrirConexionFGEM()) {
            return operacionFGEM.ejecutarFGEM(conexionFGEM);
        } catch (SQLException eFGEM) {
            throw new DataAccessExceptionFGEM("Error al acceder a la base de datos: " + eFGEM.getMessage(), eFGEM);
        }
    }

    public <T> T enTransaccionFGEM(OperacionSqlFGEM<T> operacionFGEM) {
        Connection compartidaFGEM = transaccionActualFGEM.get();
        if (compartidaFGEM != null) {
            return enCompartidaFGEM(compartidaFGEM, operacionFGEM, "La operación falló: ");
        }
        try (Connection conexionFGEM = abrirConexionFGEM()) {
            conexionFGEM.setAutoCommit(false);
            transaccionActualFGEM.set(conexionFGEM);
            try {
                T resultadoFGEM = operacionFGEM.ejecutarFGEM(conexionFGEM);
                conexionFGEM.commit();
                return resultadoFGEM;
            } catch (SQLException | RuntimeException eFGEM) {
                revertirFGEM(conexionFGEM, eFGEM);
                throw eFGEM;
            } finally {
                transaccionActualFGEM.remove();
            }
        } catch (SQLException eFGEM) {
            throw new DataAccessExceptionFGEM("La operación falló y sus cambios fueron revertidos: "
                    + eFGEM.getMessage(), eFGEM);
        }
    }

    private static <T> T enCompartidaFGEM(Connection conexionFGEM, OperacionSqlFGEM<T> operacionFGEM, String prefijoErrorFGEM) {
        try {
            return operacionFGEM.ejecutarFGEM(conexionFGEM);
        } catch (SQLException eFGEM) {
            throw new DataAccessExceptionFGEM(prefijoErrorFGEM + eFGEM.getMessage(), eFGEM);
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
