package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CategoriaFGEM;
import com.businessanalyzer.model.DatosCategoriaFGEM;
import com.businessanalyzer.util.FechasFGEM;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoriaRepositoryFGEM {

    private static final String CONSULTA_FGEM = """
            SELECT c.id, c.empresa_id, c.nombre, c.descripcion,
                   (SELECT COUNT(*) FROM producto p WHERE p.categoria_id = c.id) AS cantidad
            FROM categoria c""";

    public List<CategoriaFGEM> listarFGEM(Connection cFGEM, long empresaIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_FGEM + " WHERE c.empresa_id = ? ORDER BY c.nombre")) {
            psFGEM.setLong(1, empresaIdFGEM);
            return mapearFGEM(psFGEM);
        }
    }

    public Optional<CategoriaFGEM> buscarPorIdFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_FGEM + " WHERE c.id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<CategoriaFGEM> resultadoFGEM = mapearFGEM(psFGEM);
            return resultadoFGEM.isEmpty() ? Optional.empty() : Optional.of(resultadoFGEM.get(0));
        }
    }

    public boolean existeNombreFGEM(Connection cFGEM, long empresaIdFGEM, String nombreFGEM, long excluirIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT 1 FROM categoria WHERE empresa_id = ? AND nombre = ? AND id <> ?")) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, nombreFGEM);
            psFGEM.setLong(3, excluirIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next();
            }
        }
    }

    public long insertarFGEM(Connection cFGEM, long empresaIdFGEM, DatosCategoriaFGEM dFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO categoria (empresa_id, nombre, descripcion, creado_en, actualizado_en)
                VALUES (?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, dFGEM.nombreFGEM());
            psFGEM.setString(3, dFGEM.descripcionFGEM());
            psFGEM.setString(4, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setString(5, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            try (ResultSet clavesFGEM = psFGEM.getGeneratedKeys()) {
                if (!clavesFGEM.next()) {
                    throw new DataAccessExceptionFGEM("No se obtuvo el identificador de la categoría creada");
                }
                return clavesFGEM.getLong(1);
            }
        }
    }

    public void actualizarFGEM(Connection cFGEM, long idFGEM, DatosCategoriaFGEM dFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "UPDATE categoria SET nombre = ?, descripcion = ?, actualizado_en = ? WHERE id = ?")) {
            psFGEM.setString(1, dFGEM.nombreFGEM());
            psFGEM.setString(2, dFGEM.descripcionFGEM());
            psFGEM.setString(3, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(4, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    public void eliminarFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("DELETE FROM categoria WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    private static List<CategoriaFGEM> mapearFGEM(PreparedStatement psFGEM) throws SQLException {
        List<CategoriaFGEM> categoriasFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                categoriasFGEM.add(new CategoriaFGEM(rsFGEM.getLong("id"), rsFGEM.getLong("empresa_id"), rsFGEM.getString("nombre"),
                        rsFGEM.getString("descripcion"), rsFGEM.getInt("cantidad")));
            }
        }
        return categoriasFGEM;
    }

    private static void exigirUnaFilaFGEM(int filasFGEM) {
        if (filasFGEM != 1) {
            throw new ValidacionExceptionFGEM("La categoría ya no existe. Actualiza la lista.");
        }
    }
}
