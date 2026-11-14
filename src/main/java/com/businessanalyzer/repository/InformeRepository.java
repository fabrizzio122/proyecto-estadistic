package com.businessanalyzer.repository;

import com.businessanalyzer.model.InformeGenerado;
import com.businessanalyzer.model.TipoInforme;
import com.businessanalyzer.util.Fechas;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla {@code informe_generado}. */
public class InformeRepository {

    public void registrar(Connection c, long empresaId, TipoInforme tipo, LocalDate desde, LocalDate hasta,
                          Path archivo, String usuario, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO informe_generado (empresa_id, tipo, desde, hasta, archivo, generado_por, generado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?)""")) {
            ps.setLong(1, empresaId);
            ps.setString(2, tipo.name());
            ps.setString(3, Fechas.fechaATexto(desde));
            ps.setString(4, Fechas.fechaATexto(hasta));
            ps.setString(5, archivo.toAbsolutePath().toString());
            ps.setString(6, usuario);
            ps.setString(7, Fechas.aTexto(ahora));
            ps.executeUpdate();
        }
    }

    /** Últimos informes de la empresa, del más reciente al más antiguo. */
    public List<InformeGenerado> listar(Connection c, long empresaId, int limite) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT id, empresa_id, tipo, desde, hasta, archivo, generado_por, generado_en
                FROM informe_generado
                WHERE empresa_id = ?
                ORDER BY generado_en DESC, id DESC
                LIMIT ?""")) {
            ps.setLong(1, empresaId);
            ps.setInt(2, limite);
            List<InformeGenerado> informes = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TipoInforme tipo;
                    try {
                        tipo = TipoInforme.valueOf(rs.getString("tipo"));
                    } catch (IllegalArgumentException e) {
                        continue; // tipo de una versión futura o eliminado: no se muestra
                    }
                    informes.add(new InformeGenerado(rs.getLong("id"), rs.getLong("empresa_id"), tipo,
                            Fechas.fechaDesdeTexto(rs.getString("desde")), Fechas.fechaDesdeTexto(rs.getString("hasta")),
                            Path.of(rs.getString("archivo")), rs.getString("generado_por"),
                            Fechas.desdeTexto(rs.getString("generado_en"))));
                }
            }
            return informes;
        }
    }
}
