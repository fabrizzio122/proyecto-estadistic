package com.businessanalyzer.repository;

import com.businessanalyzer.analytics.simulacion.Supuestos;
import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Escenario;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;

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
public class EscenarioRepository {

    private static final String CONSULTA = """
            SELECT id, empresa_id, nombre, descripcion, cambio_precio, cambio_volumen, cambio_costo_unitario,
                   cambio_gastos_variables, cambio_costos_fijos, costo_fijo_adicional, tasa_impuesto,
                   creado_por, actualizado_en
            FROM escenario""";

    public List<Escenario> listar(Connection c, long empresaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA + " WHERE empresa_id = ? ORDER BY nombre")) {
            ps.setLong(1, empresaId);
            return mapear(ps);
        }
    }

    public Optional<Escenario> buscarPorId(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA + " WHERE id = ?")) {
            ps.setLong(1, id);
            List<Escenario> resultado = mapear(ps);
            return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
        }
    }

    public Optional<Escenario> buscarPorNombre(Connection c, long empresaId, String nombre) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA + " WHERE empresa_id = ? AND nombre = ?")) {
            ps.setLong(1, empresaId);
            ps.setString(2, nombre);
            List<Escenario> resultado = mapear(ps);
            return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
        }
    }

    public long insertar(Connection c, long empresaId, String nombre, String descripcion, Supuestos s,
                         String usuario, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO escenario (nombre, descripcion, cambio_precio, cambio_volumen, cambio_costo_unitario,
                                       cambio_gastos_variables, cambio_costos_fijos, costo_fijo_adicional,
                                       tasa_impuesto, creado_por, actualizado_en, empresa_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignar(ps, nombre, descripcion, s, usuario, ahora);
            ps.setLong(12, empresaId);
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new DataAccessException("No se obtuvo el identificador del escenario creado");
                }
                return claves.getLong(1);
            }
        }
    }

    public void actualizar(Connection c, long id, String nombre, String descripcion, Supuestos s, String usuario,
                           LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE escenario
                SET nombre = ?, descripcion = ?, cambio_precio = ?, cambio_volumen = ?, cambio_costo_unitario = ?,
                    cambio_gastos_variables = ?, cambio_costos_fijos = ?, costo_fijo_adicional = ?,
                    tasa_impuesto = ?, creado_por = ?, actualizado_en = ?
                WHERE id = ?""")) {
            asignar(ps, nombre, descripcion, s, usuario, ahora);
            ps.setLong(12, id);
            if (ps.executeUpdate() != 1) {
                throw new ValidacionException("El escenario ya no existe.");
            }
        }
    }

    public void eliminar(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM escenario WHERE id = ?")) {
            ps.setLong(1, id);
            if (ps.executeUpdate() != 1) {
                throw new ValidacionException("El escenario ya no existe.");
            }
        }
    }

    private static void asignar(PreparedStatement ps, String nombre, String descripcion, Supuestos s,
                                String usuario, LocalDateTime ahora) throws SQLException {
        ps.setString(1, nombre);
        ps.setString(2, descripcion);
        ps.setLong(3, Montos.aEntero(s.precio()));
        ps.setLong(4, Montos.aEntero(s.volumen()));
        ps.setLong(5, Montos.aEntero(s.costoUnitario()));
        ps.setLong(6, Montos.aEntero(s.gastosVariables()));
        ps.setLong(7, Montos.aEntero(s.costosFijos()));
        ps.setLong(8, Montos.aEntero(s.costoFijoAdicional()));
        ps.setLong(9, Montos.aEntero(s.tasaImpuesto()));
        ps.setString(10, usuario);
        ps.setString(11, Fechas.aTexto(ahora));
    }

    private static List<Escenario> mapear(PreparedStatement ps) throws SQLException {
        List<Escenario> escenarios = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Supuestos s = new Supuestos(
                        Montos.desdeEntero(rs.getLong("cambio_precio")),
                        Montos.desdeEntero(rs.getLong("cambio_volumen")),
                        Montos.desdeEntero(rs.getLong("cambio_costo_unitario")),
                        Montos.desdeEntero(rs.getLong("cambio_gastos_variables")),
                        Montos.desdeEntero(rs.getLong("cambio_costos_fijos")),
                        Montos.desdeEntero(rs.getLong("costo_fijo_adicional")),
                        Montos.desdeEntero(rs.getLong("tasa_impuesto")));
                escenarios.add(new Escenario(rs.getLong("id"), rs.getLong("empresa_id"), rs.getString("nombre"),
                        rs.getString("descripcion"), s, rs.getString("creado_por"),
                        Fechas.desdeTexto(rs.getString("actualizado_en"))));
            }
        }
        return escenarios;
    }
}
