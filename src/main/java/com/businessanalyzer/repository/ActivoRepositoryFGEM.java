package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.ActivoFGEM;
import com.businessanalyzer.model.CategoriaActivoFGEM;
import com.businessanalyzer.model.DatosActivoFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;

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

public class ActivoRepositoryFGEM {

    private static final String CONSULTA_FGEM = """
            SELECT id, empresa_id, categoria, descripcion, fecha, valor, vida_util_anios, valor_residual,
                   estado, fecha_baja, motivo_baja, registrado_por, registrado_en, actualizado_por, actualizado_en,
                   cuenta_id
            FROM activo""";

    public List<ActivoFGEM> listarFGEM(Connection cFGEM, long empresaIdFGEM, boolean incluirBajasFGEM) throws SQLException {
        String sqlFGEM = CONSULTA_FGEM + " WHERE empresa_id = ?"
                + (incluirBajasFGEM ? "" : " AND estado = 'VIGENTE'")
                + " ORDER BY fecha DESC, id DESC";
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, empresaIdFGEM);
            return mapearFGEM(psFGEM);
        }
    }

    public Optional<ActivoFGEM> buscarPorIdFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_FGEM + " WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<ActivoFGEM> resultadoFGEM = mapearFGEM(psFGEM);
            return resultadoFGEM.isEmpty() ? Optional.empty() : Optional.of(resultadoFGEM.get(0));
        }
    }

    public long insertarFGEM(Connection cFGEM, long empresaIdFGEM, DatosActivoFGEM dFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO activo (categoria, descripcion, fecha, valor, vida_util_anios, valor_residual,
                                    cuenta_id, empresa_id, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarDatosFGEM(psFGEM, dFGEM);
            psFGEM.setLong(8, empresaIdFGEM);
            psFGEM.setString(9, usuarioFGEM);
            psFGEM.setString(10, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            try (ResultSet clavesFGEM = psFGEM.getGeneratedKeys()) {
                if (!clavesFGEM.next()) {
                    throw new DataAccessExceptionFGEM("No se obtuvo el identificador del activo creado");
                }
                return clavesFGEM.getLong(1);
            }
        }
    }

    public void actualizarFGEM(Connection cFGEM, long idFGEM, DatosActivoFGEM dFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE activo
                SET categoria = ?, descripcion = ?, fecha = ?, valor = ?, vida_util_anios = ?, valor_residual = ?,
                    cuenta_id = ?, actualizado_por = ?, actualizado_en = ?
                WHERE id = ? AND estado = 'VIGENTE'""")) {
            asignarDatosFGEM(psFGEM, dFGEM);
            psFGEM.setString(8, usuarioFGEM);
            psFGEM.setString(9, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(10, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    public void darDeBajaFGEM(Connection cFGEM, long idFGEM, LocalDate fechaFGEM, String motivoFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE activo
                SET estado = 'BAJA', fecha_baja = ?, motivo_baja = ?, baja_por = ?, baja_en = ?
                WHERE id = ? AND estado = 'VIGENTE'""")) {
            psFGEM.setString(1, FechasFGEM.fechaATextoFGEM(fechaFGEM));
            psFGEM.setString(2, motivoFGEM);
            psFGEM.setString(3, usuarioFGEM);
            psFGEM.setString(4, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(5, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    private static void asignarDatosFGEM(PreparedStatement psFGEM, DatosActivoFGEM dFGEM) throws SQLException {
        psFGEM.setString(1, dFGEM.categoriaFGEM().name());
        psFGEM.setString(2, dFGEM.descripcionFGEM());
        psFGEM.setString(3, FechasFGEM.fechaATextoFGEM(dFGEM.fechaFGEM()));
        psFGEM.setLong(4, MontosFGEM.aEnteroFGEM(dFGEM.valorFGEM()));
        if (dFGEM.vidaUtilAniosFGEM() == null) {
            psFGEM.setNull(5, Types.INTEGER);
        } else {
            psFGEM.setInt(5, dFGEM.vidaUtilAniosFGEM());
        }
        psFGEM.setLong(6, MontosFGEM.aEnteroFGEM(dFGEM.valorResidualFGEM()));
        SqlFGEM.enteroOpcionalFGEM(psFGEM, 7, dFGEM.cuentaIdFGEM());
    }

    private static List<ActivoFGEM> mapearFGEM(PreparedStatement psFGEM) throws SQLException {
        List<ActivoFGEM> activosFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                int vidaFGEM = rsFGEM.getInt("vida_util_anios");
                Integer vidaUtilFGEM = rsFGEM.wasNull() ? null : vidaFGEM;
                String fechaBajaFGEM = rsFGEM.getString("fecha_baja");
                activosFGEM.add(new ActivoFGEM(
                        rsFGEM.getLong("id"),
                        rsFGEM.getLong("empresa_id"),
                        CategoriaActivoFGEM.valueOf(rsFGEM.getString("categoria")),
                        rsFGEM.getString("descripcion"),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("valor")),
                        vidaUtilFGEM,
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("valor_residual")),
                        "BAJA".equals(rsFGEM.getString("estado")),
                        fechaBajaFGEM == null ? null : FechasFGEM.fechaDesdeTextoFGEM(fechaBajaFGEM),
                        rsFGEM.getString("motivo_baja"),
                        rsFGEM.getString("registrado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("registrado_en")),
                        rsFGEM.getString("actualizado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("actualizado_en")),
                        SqlFGEM.enteroOpcionalFGEM(rsFGEM, "cuenta_id")));
            }
        }
        return activosFGEM;
    }

    private static void exigirUnaFilaFGEM(int filasFGEM) {
        if (filasFGEM != 1) {
            throw new ValidacionExceptionFGEM("El activo ya no existe o fue dado de baja. Actualiza la lista.");
        }
    }
}
