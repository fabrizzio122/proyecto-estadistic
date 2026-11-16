package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CategoriaDeuda;
import com.businessanalyzer.model.DatosDeuda;
import com.businessanalyzer.model.Deuda;
import com.businessanalyzer.model.PagoDeuda;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;

import java.math.BigDecimal;
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

/** Acceso a las tablas {@code deuda} y {@code pago_deuda}. El monto pagado se calcula con los pagos vigentes. */
public class DeudaRepository {

    private static final String CONSULTA = """
            SELECT d.id, d.empresa_id, d.categoria, d.acreedor, d.descripcion, d.fecha, d.monto, d.tasa_anual,
                   d.cuota_mensual, d.vencimiento, d.estado, d.motivo_anulacion, d.registrado_por, d.registrado_en,
                   d.actualizado_por, d.actualizado_en, d.cuenta_id,
                   (SELECT COALESCE(SUM(p.monto), 0) FROM pago_deuda p
                    WHERE p.deuda_id = d.id AND p.estado = 'REGISTRADO') AS pagado
            FROM deuda d""";

    private static final String CONSULTA_PAGOS = """
            SELECT id, deuda_id, fecha, monto, nota, estado, registrado_por, registrado_en, anulado_por, anulado_en,
                   interes, cuenta_id,
                   (SELECT cd.nombre FROM cuenta_dinero cd WHERE cd.id = pago_deuda.cuenta_id) AS cuenta
            FROM pago_deuda""";

    public List<Deuda> listar(Connection c, long empresaId, boolean incluirAnuladas) throws SQLException {
        String sql = CONSULTA + " WHERE d.empresa_id = ?"
                + (incluirAnuladas ? "" : " AND d.estado = 'VIGENTE'")
                + " ORDER BY d.fecha DESC, d.id DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            return mapear(ps);
        }
    }

    public Optional<Deuda> buscarPorId(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA + " WHERE d.id = ?")) {
            ps.setLong(1, id);
            List<Deuda> resultado = mapear(ps);
            return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
        }
    }

    public long insertar(Connection c, long empresaId, DatosDeuda d, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO deuda (categoria, acreedor, descripcion, fecha, monto, tasa_anual, cuota_mensual,
                                   vencimiento, cuenta_id, empresa_id, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarDatos(ps, d);
            ps.setLong(10, empresaId);
            ps.setString(11, usuario);
            ps.setString(12, Fechas.aTexto(ahora));
            ps.executeUpdate();
            return claveGenerada(ps, "la deuda");
        }
    }

    public void actualizar(Connection c, long id, DatosDeuda d, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE deuda
                SET categoria = ?, acreedor = ?, descripcion = ?, fecha = ?, monto = ?, tasa_anual = ?,
                    cuota_mensual = ?, vencimiento = ?, cuenta_id = ?, actualizado_por = ?, actualizado_en = ?
                WHERE id = ? AND estado = 'VIGENTE'""")) {
            asignarDatos(ps, d);
            ps.setString(10, usuario);
            ps.setString(11, Fechas.aTexto(ahora));
            ps.setLong(12, id);
            exigirUnaFila(ps.executeUpdate(), "La deuda ya no existe o está anulada. Actualiza la lista.");
        }
    }

    public void anular(Connection c, long id, String motivo, String usuario, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE deuda
                SET estado = 'ANULADA', motivo_anulacion = ?, anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'VIGENTE'""")) {
            ps.setString(1, motivo);
            ps.setString(2, usuario);
            ps.setString(3, Fechas.aTexto(ahora));
            ps.setLong(4, id);
            exigirUnaFila(ps.executeUpdate(), "La deuda ya no existe o está anulada. Actualiza la lista.");
        }
    }

    // ------------------------------------------------------------------ pagos

    public List<PagoDeuda> listarPagos(Connection c, long deudaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA_PAGOS
                + " WHERE deuda_id = ? ORDER BY fecha DESC, id DESC")) {
            ps.setLong(1, deudaId);
            return mapearPagos(ps);
        }
    }

    public Optional<PagoDeuda> buscarPago(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA_PAGOS + " WHERE id = ?")) {
            ps.setLong(1, id);
            List<PagoDeuda> resultado = mapearPagos(ps);
            return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
        }
    }

    /** Fecha del primer pago vigente; vacío si no hay pagos. */
    public Optional<LocalDate> primerPago(Connection c, long deudaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT MIN(fecha) FROM pago_deuda WHERE deuda_id = ? AND estado = 'REGISTRADO'")) {
            ps.setLong(1, deudaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(Fechas.fechaDesdeTexto(rs.getString(1))) : Optional.empty();
            }
        }
    }

    /**
     * @param capital      lo que baja el saldo de la deuda
     * @param interes      intereses de la cuota (cero si no hay)
     * @param cuentaId     cuenta de la que salió el pago
     * @param movimientoId gasto financiero con el que se registraron los intereses; {@code null} si no hay
     */
    public long insertarPago(Connection c, long deudaId, LocalDate fecha, BigDecimal capital, BigDecimal interes,
                             Long cuentaId, Long movimientoId, String nota, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO pago_deuda (deuda_id, fecha, monto, interes, cuenta_id, movimiento_id, nota,
                                        registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, deudaId);
            ps.setString(2, Fechas.fechaATexto(fecha));
            ps.setLong(3, Montos.aEntero(capital));
            ps.setLong(4, Montos.aEntero(interes));
            Sql.enteroOpcional(ps, 5, cuentaId);
            Sql.enteroOpcional(ps, 6, movimientoId);
            ps.setString(7, nota);
            ps.setString(8, usuario);
            ps.setString(9, Fechas.aTexto(ahora));
            ps.executeUpdate();
            return claveGenerada(ps, "el pago");
        }
    }

    /** Gasto financiero (intereses) que se generó con el pago; vacío si el pago no tuvo intereses. */
    public Optional<Long> movimientoDelPago(Connection c, long pagoId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT movimiento_id FROM pago_deuda WHERE id = ?")) {
            ps.setLong(1, pagoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(Sql.enteroOpcional(rs, "movimiento_id")) : Optional.empty();
            }
        }
    }

    /** Gastos financieros (intereses) de los pagos vigentes de la deuda. */
    public List<Long> movimientosDeLaDeuda(Connection c, long deudaId) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT movimiento_id FROM pago_deuda
                WHERE deuda_id = ? AND estado = 'REGISTRADO' AND movimiento_id IS NOT NULL""")) {
            ps.setLong(1, deudaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong(1));
                }
            }
        }
        return ids;
    }

    public void anularPago(Connection c, long id, String usuario, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE pago_deuda SET estado = 'ANULADO', anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'REGISTRADO'""")) {
            ps.setString(1, usuario);
            ps.setString(2, Fechas.aTexto(ahora));
            ps.setLong(3, id);
            exigirUnaFila(ps.executeUpdate(), "El pago ya no existe o está anulado. Actualiza la lista.");
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static void asignarDatos(PreparedStatement ps, DatosDeuda d) throws SQLException {
        ps.setString(1, d.categoria().name());
        ps.setString(2, d.acreedor());
        ps.setString(3, d.descripcion());
        ps.setString(4, Fechas.fechaATexto(d.fecha()));
        ps.setLong(5, Montos.aEntero(d.monto()));
        montoOpcional(ps, 6, d.tasaAnual());
        montoOpcional(ps, 7, d.cuotaMensual());
        ps.setString(8, Fechas.fechaATexto(d.vencimiento()));
        Sql.enteroOpcional(ps, 9, d.cuentaId());
    }

    private static void montoOpcional(PreparedStatement ps, int indice, BigDecimal valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setLong(indice, Montos.aEntero(valor));
        }
    }

    private static BigDecimal leerOpcional(ResultSet rs, String columna) throws SQLException {
        long valor = rs.getLong(columna);
        return rs.wasNull() ? null : Montos.desdeEntero(valor);
    }

    private static long claveGenerada(PreparedStatement ps, String que) throws SQLException {
        try (ResultSet claves = ps.getGeneratedKeys()) {
            if (!claves.next()) {
                throw new DataAccessException("No se obtuvo el identificador de " + que);
            }
            return claves.getLong(1);
        }
    }

    private static List<Deuda> mapear(PreparedStatement ps) throws SQLException {
        List<Deuda> deudas = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                deudas.add(new Deuda(
                        rs.getLong("id"),
                        rs.getLong("empresa_id"),
                        CategoriaDeuda.valueOf(rs.getString("categoria")),
                        rs.getString("acreedor"),
                        rs.getString("descripcion"),
                        Fechas.fechaDesdeTexto(rs.getString("fecha")),
                        Montos.desdeEntero(rs.getLong("monto")),
                        Montos.desdeEntero(rs.getLong("pagado")),
                        leerOpcional(rs, "tasa_anual"),
                        leerOpcional(rs, "cuota_mensual"),
                        Fechas.fechaDesdeTexto(rs.getString("vencimiento")),
                        "ANULADA".equals(rs.getString("estado")),
                        rs.getString("motivo_anulacion"),
                        rs.getString("registrado_por"),
                        Fechas.desdeTexto(rs.getString("registrado_en")),
                        rs.getString("actualizado_por"),
                        Fechas.desdeTexto(rs.getString("actualizado_en")),
                        Sql.enteroOpcional(rs, "cuenta_id")));
            }
        }
        return deudas;
    }

    private static List<PagoDeuda> mapearPagos(PreparedStatement ps) throws SQLException {
        List<PagoDeuda> pagos = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                pagos.add(new PagoDeuda(
                        rs.getLong("id"),
                        rs.getLong("deuda_id"),
                        Fechas.fechaDesdeTexto(rs.getString("fecha")),
                        Montos.desdeEntero(rs.getLong("monto")),
                        rs.getString("nota"),
                        "ANULADO".equals(rs.getString("estado")),
                        rs.getString("registrado_por"),
                        Fechas.desdeTexto(rs.getString("registrado_en")),
                        rs.getString("anulado_por"),
                        Fechas.desdeTexto(rs.getString("anulado_en")),
                        Montos.desdeEntero(rs.getLong("interes")),
                        Sql.enteroOpcional(rs, "cuenta_id"),
                        rs.getString("cuenta")));
            }
        }
        return pagos;
    }

    private static void exigirUnaFila(int filas, String mensaje) {
        if (filas != 1) {
            throw new ValidacionException(mensaje);
        }
    }
}
