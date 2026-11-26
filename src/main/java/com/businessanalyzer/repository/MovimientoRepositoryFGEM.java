package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.ComportamientoFGEM;
import com.businessanalyzer.model.DatosMovimientoFGEM;
import com.businessanalyzer.model.MovimientoFinancieroFGEM;
import com.businessanalyzer.model.TipoMovimientoFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MovimientoRepositoryFGEM {

    private static final String CONSULTA_FGEM = """
            SELECT id, empresa_id, tipo, categoria, comportamiento, fecha, descripcion, monto, comprobante,
                   estado, registrado_por, registrado_en, actualizado_por, actualizado_en, motivo_anulacion,
                   anulado_por, anulado_en, cuenta_id,
                   (SELECT cd.nombre FROM cuenta_dinero cd WHERE cd.id = movimiento_financiero.cuenta_id) AS cuenta,
                   EXISTS (SELECT 1 FROM pago_deuda p WHERE p.movimiento_id = movimiento_financiero.id) AS de_pago
            FROM movimiento_financiero""";

    public List<MovimientoFinancieroFGEM> listarFGEM(Connection cFGEM, long empresaIdFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM,
                                             boolean incluirAnuladosFGEM) throws SQLException {
        String sqlFGEM = CONSULTA_FGEM + " WHERE empresa_id = ? AND fecha BETWEEN ? AND ?"
                + (incluirAnuladosFGEM ? "" : " AND estado = 'REGISTRADO'")
                + " ORDER BY fecha DESC, id DESC";
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, FechasFGEM.fechaATextoFGEM(desdeFGEM));
            psFGEM.setString(3, FechasFGEM.fechaATextoFGEM(hastaFGEM));
            return mapearFGEM(psFGEM);
        }
    }

    public Optional<MovimientoFinancieroFGEM> buscarPorIdFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_FGEM + " WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<MovimientoFinancieroFGEM> resultadoFGEM = mapearFGEM(psFGEM);
            return resultadoFGEM.isEmpty() ? Optional.empty() : Optional.of(resultadoFGEM.get(0));
        }
    }

    public long insertarFGEM(Connection cFGEM, long empresaIdFGEM, DatosMovimientoFGEM dFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO movimiento_financiero (tipo, categoria, comportamiento, fecha, descripcion, monto,
                                                  comprobante, cuenta_id, empresa_id, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarDatosFGEM(psFGEM, dFGEM);
            psFGEM.setLong(9, empresaIdFGEM);
            psFGEM.setString(10, usuarioFGEM);
            psFGEM.setString(11, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            try (ResultSet clavesFGEM = psFGEM.getGeneratedKeys()) {
                if (!clavesFGEM.next()) {
                    throw new DataAccessExceptionFGEM("No se obtuvo el identificador del movimiento creado");
                }
                return clavesFGEM.getLong(1);
            }
        }
    }

    public void actualizarFGEM(Connection cFGEM, long idFGEM, DatosMovimientoFGEM dFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE movimiento_financiero
                SET tipo = ?, categoria = ?, comportamiento = ?, fecha = ?, descripcion = ?, monto = ?,
                    comprobante = ?, cuenta_id = ?, actualizado_por = ?, actualizado_en = ?
                WHERE id = ? AND estado = 'REGISTRADO'""")) {
            asignarDatosFGEM(psFGEM, dFGEM);
            psFGEM.setString(9, usuarioFGEM);
            psFGEM.setString(10, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(11, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    public void anularFGEM(Connection cFGEM, long idFGEM, String motivoFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE movimiento_financiero
                SET estado = 'ANULADO', motivo_anulacion = ?, anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'REGISTRADO'""")) {
            psFGEM.setString(1, motivoFGEM);
            psFGEM.setString(2, usuarioFGEM);
            psFGEM.setString(3, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(4, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    private static void asignarDatosFGEM(PreparedStatement psFGEM, DatosMovimientoFGEM dFGEM) throws SQLException {
        psFGEM.setString(1, dFGEM.tipoFGEM().name());
        psFGEM.setString(2, dFGEM.categoriaFGEM().name());
        psFGEM.setString(3, dFGEM.comportamientoFGEM() == null ? null : dFGEM.comportamientoFGEM().name());
        psFGEM.setString(4, FechasFGEM.fechaATextoFGEM(dFGEM.fechaFGEM()));
        psFGEM.setString(5, dFGEM.descripcionFGEM());
        psFGEM.setLong(6, MontosFGEM.aEnteroFGEM(dFGEM.montoFGEM()));
        psFGEM.setString(7, dFGEM.comprobanteFGEM());
        SqlFGEM.enteroOpcionalFGEM(psFGEM, 8, dFGEM.cuentaIdFGEM());
    }

    private static List<MovimientoFinancieroFGEM> mapearFGEM(PreparedStatement psFGEM) throws SQLException {
        List<MovimientoFinancieroFGEM> movimientosFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                String comportamientoFGEM = rsFGEM.getString("comportamiento");
                movimientosFGEM.add(new MovimientoFinancieroFGEM(
                        rsFGEM.getLong("id"),
                        rsFGEM.getLong("empresa_id"),
                        TipoMovimientoFGEM.valueOf(rsFGEM.getString("tipo")),
                        CategoriaFinancieraFGEM.valueOf(rsFGEM.getString("categoria")),
                        comportamientoFGEM == null ? null : ComportamientoFGEM.valueOf(comportamientoFGEM),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha")),
                        rsFGEM.getString("descripcion"),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("monto")),
                        rsFGEM.getString("comprobante"),
                        "ANULADO".equals(rsFGEM.getString("estado")),
                        rsFGEM.getString("registrado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("registrado_en")),
                        rsFGEM.getString("actualizado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("actualizado_en")),
                        rsFGEM.getString("motivo_anulacion"),
                        rsFGEM.getString("anulado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("anulado_en")),
                        SqlFGEM.enteroOpcionalFGEM(rsFGEM, "cuenta_id"),
                        rsFGEM.getString("cuenta"),
                        rsFGEM.getBoolean("de_pago")));
            }
        }
        return movimientosFGEM;
    }

    private static void exigirUnaFilaFGEM(int filasFGEM) {
        if (filasFGEM != 1) {
            throw new ValidacionExceptionFGEM("El movimiento ya no existe o está anulado. Actualiza la lista.");
        }
    }
}
