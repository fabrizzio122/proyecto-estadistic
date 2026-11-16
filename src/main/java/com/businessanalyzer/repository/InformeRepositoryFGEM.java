package com.businessanalyzer.repository;

import com.businessanalyzer.model.InformeGeneradoFGEM;
import com.businessanalyzer.model.TipoInformeFGEM;
import com.businessanalyzer.util.FechasFGEM;

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
public class InformeRepositoryFGEM {

    public void registrarFGEM(Connection cFGEM, long empresaIdFGEM, TipoInformeFGEM tipoFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM,
                          Path archivoFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO informe_generado (empresa_id, tipo, desde, hasta, archivo, generado_por, generado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?)""")) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, tipoFGEM.name());
            psFGEM.setString(3, FechasFGEM.fechaATextoFGEM(desdeFGEM));
            psFGEM.setString(4, FechasFGEM.fechaATextoFGEM(hastaFGEM));
            psFGEM.setString(5, archivoFGEM.toAbsolutePath().toString());
            psFGEM.setString(6, usuarioFGEM);
            psFGEM.setString(7, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
        }
    }

    /** Últimos informes de la empresa, del más reciente al más antiguo. */
    public List<InformeGeneradoFGEM> listarFGEM(Connection cFGEM, long empresaIdFGEM, int limiteFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                SELECT id, empresa_id, tipo, desde, hasta, archivo, generado_por, generado_en
                FROM informe_generado
                WHERE empresa_id = ?
                ORDER BY generado_en DESC, id DESC
                LIMIT ?""")) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setInt(2, limiteFGEM);
            List<InformeGeneradoFGEM> informesFGEM = new ArrayList<>();
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    TipoInformeFGEM tipoFGEM;
                    try {
                        tipoFGEM = TipoInformeFGEM.valueOf(rsFGEM.getString("tipo"));
                    } catch (IllegalArgumentException eFGEM) {
                        continue; // tipo de una versión futura o eliminado: no se muestra
                    }
                    informesFGEM.add(new InformeGeneradoFGEM(rsFGEM.getLong("id"), rsFGEM.getLong("empresa_id"), tipoFGEM,
                            FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("desde")), FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("hasta")),
                            Path.of(rsFGEM.getString("archivo")), rsFGEM.getString("generado_por"),
                            FechasFGEM.desdeTextoFGEM(rsFGEM.getString("generado_en"))));
                }
            }
            return informesFGEM;
        }
    }
}
