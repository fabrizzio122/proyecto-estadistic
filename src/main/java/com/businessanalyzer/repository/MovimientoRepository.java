package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CategoriaFinanciera;
import com.businessanalyzer.model.Comportamiento;
import com.businessanalyzer.model.DatosMovimiento;
import com.businessanalyzer.model.MovimientoFinanciero;
import com.businessanalyzer.model.TipoMovimiento;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;

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

/** Acceso a la tabla {@code movimiento_financiero}. */
public class MovimientoRepository {

    private static final String CONSULTA = """
            SELECT id, empresa_id, tipo, categoria, comportamiento, fecha, descripcion, monto, comprobante,
                   estado, registrado_por, registrado_en, actualizado_por, actualizado_en, motivo_anulacion,
                   anulado_por, anulado_en, cuenta_id,
                   (SELECT cd.nombre FROM cuenta_dinero cd WHERE cd.id = movimiento_financiero.cuenta_id) AS cuenta,
                   EXISTS (SELECT 1 FROM pago_deuda p WHERE p.movimiento_id = movimiento_financiero.id) AS de_pago
            FROM movimiento_financiero""";

    public List<MovimientoFinanciero> listar(Connection c, long empresaId, LocalDate desde, LocalDate hasta,
                                             boolean incluirAnulados) throws SQLException {
        String sql = CONSULTA + " WHERE empresa_id = ? AND fecha BETWEEN ? AND ?"
                + (incluirAnulados ? "" : " AND estado = 'REGISTRADO'")
                + " ORDER BY fecha DESC, id DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setString(2, Fechas.fechaATexto(desde));
            ps.setString(3, Fechas.fechaATexto(hasta));
            return mapear(ps);
        }
    }

    public Optional<MovimientoFinanciero> buscarPorId(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA + " WHERE id = ?")) {
            ps.setLong(1, id);
            List<MovimientoFinanciero> resultado = mapear(ps);
            return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
        }
    }

    public long insertar(Connection c, long empresaId, DatosMovimiento d, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO movimiento_financiero (tipo, categoria, comportamiento, fecha, descripcion, monto,
                                                  comprobante, cuenta_id, empresa_id, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarDatos(ps, d);
            ps.setLong(9, empresaId);
            ps.setString(10, usuario);
            ps.setString(11, Fechas.aTexto(ahora));
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new DataAccessException("No se obtuvo el identificador del movimiento creado");
                }
                return claves.getLong(1);
            }
        }
    }

    public void actualizar(Connection c, long id, DatosMovimiento d, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE movimiento_financiero
                SET tipo = ?, categoria = ?, comportamiento = ?, fecha = ?, descripcion = ?, monto = ?,
                    comprobante = ?, cuenta_id = ?, actualizado_por = ?, actualizado_en = ?
                WHERE id = ? AND estado = 'REGISTRADO'""")) {
            asignarDatos(ps, d);
            ps.setString(9, usuario);
            ps.setString(10, Fechas.aTexto(ahora));
            ps.setLong(11, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    public void anular(Connection c, long id, String motivo, String usuario, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE movimiento_financiero
                SET estado = 'ANULADO', motivo_anulacion = ?, anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'REGISTRADO'""")) {
            ps.setString(1, motivo);
            ps.setString(2, usuario);
            ps.setString(3, Fechas.aTexto(ahora));
            ps.setLong(4, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    private static void asignarDatos(PreparedStatement ps, DatosMovimiento d) throws SQLException {
        ps.setString(1, d.tipo().name());
        ps.setString(2, d.categoria().name());
        ps.setString(3, d.comportamiento() == null ? null : d.comportamiento().name());
        ps.setString(4, Fechas.fechaATexto(d.fecha()));
        ps.setString(5, d.descripcion());
        ps.setLong(6, Montos.aEntero(d.monto()));
        ps.setString(7, d.comprobante());
        Sql.enteroOpcional(ps, 8, d.cuentaId());
    }

    private static List<MovimientoFinanciero> mapear(PreparedStatement ps) throws SQLException {
        List<MovimientoFinanciero> movimientos = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String comportamiento = rs.getString("comportamiento");
                movimientos.add(new MovimientoFinanciero(
                        rs.getLong("id"),
                        rs.getLong("empresa_id"),
                        TipoMovimiento.valueOf(rs.getString("tipo")),
                        CategoriaFinanciera.valueOf(rs.getString("categoria")),
                        comportamiento == null ? null : Comportamiento.valueOf(comportamiento),
                        Fechas.fechaDesdeTexto(rs.getString("fecha")),
                        rs.getString("descripcion"),
                        Montos.desdeEntero(rs.getLong("monto")),
                        rs.getString("comprobante"),
                        "ANULADO".equals(rs.getString("estado")),
                        rs.getString("registrado_por"),
                        Fechas.desdeTexto(rs.getString("registrado_en")),
                        rs.getString("actualizado_por"),
                        Fechas.desdeTexto(rs.getString("actualizado_en")),
                        rs.getString("motivo_anulacion"),
                        rs.getString("anulado_por"),
                        Fechas.desdeTexto(rs.getString("anulado_en")),
                        Sql.enteroOpcional(rs, "cuenta_id"),
                        rs.getString("cuenta"),
                        rs.getBoolean("de_pago")));
            }
        }
        return movimientos;
    }

    private static void exigirUnaFila(int filas) {
        if (filas != 1) {
            throw new ValidacionException("El movimiento ya no existe o está anulado. Actualiza la lista.");
        }
    }
}
