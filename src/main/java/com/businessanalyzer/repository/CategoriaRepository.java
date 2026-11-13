package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Categoria;
import com.businessanalyzer.model.DatosCategoria;
import com.businessanalyzer.util.Fechas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acceso a la tabla {@code categoria}. */
public class CategoriaRepository {

    private static final String CONSULTA = """
            SELECT c.id, c.empresa_id, c.nombre, c.descripcion,
                   (SELECT COUNT(*) FROM producto p WHERE p.categoria_id = c.id) AS cantidad
            FROM categoria c""";

    public List<Categoria> listar(Connection c, long empresaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA + " WHERE c.empresa_id = ? ORDER BY c.nombre")) {
            ps.setLong(1, empresaId);
            return mapear(ps);
        }
    }

    public Optional<Categoria> buscarPorId(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA + " WHERE c.id = ?")) {
            ps.setLong(1, id);
            List<Categoria> resultado = mapear(ps);
            return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
        }
    }

    public boolean existeNombre(Connection c, long empresaId, String nombre, long excluirId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM categoria WHERE empresa_id = ? AND nombre = ? AND id <> ?")) {
            ps.setLong(1, empresaId);
            ps.setString(2, nombre);
            ps.setLong(3, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insertar(Connection c, long empresaId, DatosCategoria d, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO categoria (empresa_id, nombre, descripcion, creado_en, actualizado_en)
                VALUES (?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, empresaId);
            ps.setString(2, d.nombre());
            ps.setString(3, d.descripcion());
            ps.setString(4, Fechas.aTexto(ahora));
            ps.setString(5, Fechas.aTexto(ahora));
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new DataAccessException("No se obtuvo el identificador de la categoría creada");
                }
                return claves.getLong(1);
            }
        }
    }

    public void actualizar(Connection c, long id, DatosCategoria d, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE categoria SET nombre = ?, descripcion = ?, actualizado_en = ? WHERE id = ?")) {
            ps.setString(1, d.nombre());
            ps.setString(2, d.descripcion());
            ps.setString(3, Fechas.aTexto(ahora));
            ps.setLong(4, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    public void eliminar(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM categoria WHERE id = ?")) {
            ps.setLong(1, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    private static List<Categoria> mapear(PreparedStatement ps) throws SQLException {
        List<Categoria> categorias = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categorias.add(new Categoria(rs.getLong("id"), rs.getLong("empresa_id"), rs.getString("nombre"),
                        rs.getString("descripcion"), rs.getInt("cantidad")));
            }
        }
        return categorias;
    }

    private static void exigirUnaFila(int filas) {
        if (filas != 1) {
            throw new ValidacionException("La categoría ya no existe. Actualiza la lista.");
        }
    }
}
