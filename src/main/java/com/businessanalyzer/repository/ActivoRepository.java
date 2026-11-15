package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Activo;
import com.businessanalyzer.model.CategoriaActivo;
import com.businessanalyzer.model.DatosActivo;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acceso a la tabla {@code activo}. */
public class ActivoRepository {

    private static final String CONSULTA = """
            SELECT id, empresa_id, categoria, descripcion, fecha, valor, vida_util_anios, valor_residual,
                   estado, fecha_baja, motivo_baja, registrado_por, registrado_en, actualizado_por, actualizado_en
            FROM activo""";

    public List<Activo> listar(Connection c, long empresaId, boolean incluirBajas) throws SQLException {
        String sql = CONSULTA + " WHERE empresa_id = ?"
                + (incluirBajas ? "" : " AND estado = 'VIGENTE'")
                + " ORDER BY fecha DESC, id DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            return mapear(ps);
        }
    }

    public Optional<Activo> buscarPorId(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA + " WHERE id = ?")) {
            ps.setLong(1, id);
            List<Activo> resultado = mapear(ps);
            return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
        }
    }

    public long insertar(Connection c, long empresaId, DatosActivo d, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO activo (categoria, descripcion, fecha, valor, vida_util_anios, valor_residual,
                                    empresa_id, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarDatos(ps, d);
            ps.setLong(7, empresaId);
            ps.setString(8, usuario);
            ps.setString(9, Fechas.aTexto(ahora));
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new DataAccessException("No se obtuvo el identificador del activo creado");
                }
                return claves.getLong(1);
            }
        }
    }

    public void actualizar(Connection c, long id, DatosActivo d, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE activo
                SET categoria = ?, descripcion = ?, fecha = ?, valor = ?, vida_util_anios = ?, valor_residual = ?,
                    actualizado_por = ?, actualizado_en = ?
                WHERE id = ? AND estado = 'VIGENTE'""")) {
            asignarDatos(ps, d);
            ps.setString(7, usuario);
            ps.setString(8, Fechas.aTexto(ahora));
            ps.setLong(9, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    public void darDeBaja(Connection c, long id, LocalDate fecha, String motivo, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE activo
                SET estado = 'BAJA', fecha_baja = ?, motivo_baja = ?, baja_por = ?, baja_en = ?
                WHERE id = ? AND estado = 'VIGENTE'""")) {
            ps.setString(1, Fechas.fechaATexto(fecha));
            ps.setString(2, motivo);
            ps.setString(3, usuario);
            ps.setString(4, Fechas.aTexto(ahora));
            ps.setLong(5, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    private static void asignarDatos(PreparedStatement ps, DatosActivo d) throws SQLException {
        ps.setString(1, d.categoria().name());
        ps.setString(2, d.descripcion());
        ps.setString(3, Fechas.fechaATexto(d.fecha()));
        ps.setLong(4, Montos.aEntero(d.valor()));
        if (d.vidaUtilAnios() == null) {
            ps.setNull(5, Types.INTEGER);
        } else {
            ps.setInt(5, d.vidaUtilAnios());
        }
        ps.setLong(6, Montos.aEntero(d.valorResidual()));
    }

    private static List<Activo> mapear(PreparedStatement ps) throws SQLException {
        List<Activo> activos = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int vida = rs.getInt("vida_util_anios");
                Integer vidaUtil = rs.wasNull() ? null : vida;
                String fechaBaja = rs.getString("fecha_baja");
                activos.add(new Activo(
                        rs.getLong("id"),
                        rs.getLong("empresa_id"),
                        CategoriaActivo.valueOf(rs.getString("categoria")),
                        rs.getString("descripcion"),
                        Fechas.fechaDesdeTexto(rs.getString("fecha")),
                        Montos.desdeEntero(rs.getLong("valor")),
                        vidaUtil,
                        Montos.desdeEntero(rs.getLong("valor_residual")),
                        "BAJA".equals(rs.getString("estado")),
                        fechaBaja == null ? null : Fechas.fechaDesdeTexto(fechaBaja),
                        rs.getString("motivo_baja"),
                        rs.getString("registrado_por"),
                        Fechas.desdeTexto(rs.getString("registrado_en")),
                        rs.getString("actualizado_por"),
                        Fechas.desdeTexto(rs.getString("actualizado_en"))));
            }
        }
        return activos;
    }

    private static void exigirUnaFila(int filas) {
        if (filas != 1) {
            throw new ValidacionException("El activo ya no existe o fue dado de baja. Actualiza la lista.");
        }
    }
}
