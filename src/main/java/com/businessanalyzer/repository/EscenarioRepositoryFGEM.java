package com.businessanalyzer.repository;

import com.businessanalyzer.analytics.simulacion.SupuestosFGEM;
import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.EscenarioFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acceso a la tabla {@code escenario}. */
public class EscenarioRepositoryFGEM {

    private static final String CONSULTA_FGEM = """
            SELECT id, empresa_id, nombre, descripcion, cambio_precio, cambio_volumen, cambio_costo_unitario,
                   cambio_gastos_variables, cambio_costos_fijos, costo_fijo_adicional, tasa_impuesto,
                   creado_por, actualizado_en
            FROM escenario""";

    public List<EscenarioFGEM> listarFGEM(Connection cFGEM, long empresaIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_FGEM + " WHERE empresa_id = ? ORDER BY nombre")) {
            psFGEM.setLong(1, empresaIdFGEM);
            return mapearFGEM(psFGEM);
        }
    }

    public Optional<EscenarioFGEM> buscarPorIdFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_FGEM + " WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<EscenarioFGEM> resultadoFGEM = mapearFGEM(psFGEM);
            return resultadoFGEM.isEmpty() ? Optional.empty() : Optional.of(resultadoFGEM.get(0));
        }
    }

    public Optional<EscenarioFGEM> buscarPorNombreFGEM(Connection cFGEM, long empresaIdFGEM, String nombreFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_FGEM + " WHERE empresa_id = ? AND nombre = ?")) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, nombreFGEM);
            List<EscenarioFGEM> resultadoFGEM = mapearFGEM(psFGEM);
            return resultadoFGEM.isEmpty() ? Optional.empty() : Optional.of(resultadoFGEM.get(0));
        }
    }

    public long insertarFGEM(Connection cFGEM, long empresaIdFGEM, String nombreFGEM, String descripcionFGEM, SupuestosFGEM sFGEM,
                         String usuarioFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO escenario (nombre, descripcion, cambio_precio, cambio_volumen, cambio_costo_unitario,
                                       cambio_gastos_variables, cambio_costos_fijos, costo_fijo_adicional,
                                       tasa_impuesto, creado_por, actualizado_en, empresa_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarFGEM(psFGEM, nombreFGEM, descripcionFGEM, sFGEM, usuarioFGEM, ahoraFGEM);
            psFGEM.setLong(12, empresaIdFGEM);
            psFGEM.executeUpdate();
            try (ResultSet clavesFGEM = psFGEM.getGeneratedKeys()) {
                if (!clavesFGEM.next()) {
                    throw new DataAccessExceptionFGEM("No se obtuvo el identificador del escenario creado");
                }
                return clavesFGEM.getLong(1);
            }
        }
    }

    public void actualizarFGEM(Connection cFGEM, long idFGEM, String nombreFGEM, String descripcionFGEM, SupuestosFGEM sFGEM, String usuarioFGEM,
                           LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE escenario
                SET nombre = ?, descripcion = ?, cambio_precio = ?, cambio_volumen = ?, cambio_costo_unitario = ?,
                    cambio_gastos_variables = ?, cambio_costos_fijos = ?, costo_fijo_adicional = ?,
                    tasa_impuesto = ?, creado_por = ?, actualizado_en = ?
                WHERE id = ?""")) {
            asignarFGEM(psFGEM, nombreFGEM, descripcionFGEM, sFGEM, usuarioFGEM, ahoraFGEM);
            psFGEM.setLong(12, idFGEM);
            if (psFGEM.executeUpdate() != 1) {
                throw new ValidacionExceptionFGEM("El escenario ya no existe.");
            }
        }
    }

    public void eliminarFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("DELETE FROM escenario WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            if (psFGEM.executeUpdate() != 1) {
                throw new ValidacionExceptionFGEM("El escenario ya no existe.");
            }
        }
    }

    private static void asignarFGEM(PreparedStatement psFGEM, String nombreFGEM, String descripcionFGEM, SupuestosFGEM sFGEM,
                                String usuarioFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        psFGEM.setString(1, nombreFGEM);
        psFGEM.setString(2, descripcionFGEM);
        psFGEM.setLong(3, MontosFGEM.aEnteroFGEM(sFGEM.precioFGEM()));
        psFGEM.setLong(4, MontosFGEM.aEnteroFGEM(sFGEM.volumenFGEM()));
        psFGEM.setLong(5, MontosFGEM.aEnteroFGEM(sFGEM.costoUnitarioFGEM()));
        psFGEM.setLong(6, MontosFGEM.aEnteroFGEM(sFGEM.gastosVariablesFGEM()));
        psFGEM.setLong(7, MontosFGEM.aEnteroFGEM(sFGEM.costosFijosFGEM()));
        psFGEM.setLong(8, MontosFGEM.aEnteroFGEM(sFGEM.costoFijoAdicionalFGEM()));
        psFGEM.setLong(9, MontosFGEM.aEnteroFGEM(sFGEM.tasaImpuestoFGEM()));
        psFGEM.setString(10, usuarioFGEM);
        psFGEM.setString(11, FechasFGEM.aTextoFGEM(ahoraFGEM));
    }

    private static List<EscenarioFGEM> mapearFGEM(PreparedStatement psFGEM) throws SQLException {
        List<EscenarioFGEM> escenariosFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                SupuestosFGEM sFGEM = new SupuestosFGEM(
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("cambio_precio")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("cambio_volumen")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("cambio_costo_unitario")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("cambio_gastos_variables")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("cambio_costos_fijos")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("costo_fijo_adicional")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("tasa_impuesto")));
                escenariosFGEM.add(new EscenarioFGEM(rsFGEM.getLong("id"), rsFGEM.getLong("empresa_id"), rsFGEM.getString("nombre"),
                        rsFGEM.getString("descripcion"), sFGEM, rsFGEM.getString("creado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("actualizado_en"))));
            }
        }
        return escenariosFGEM;
    }
}
