package com.businessanalyzer.repository;

import com.businessanalyzer.analytics.tesoreria.FlujoDinero;
import com.businessanalyzer.analytics.tesoreria.RubroFlujo;
import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CuentaDinero;
import com.businessanalyzer.model.DatosCuenta;
import com.businessanalyzer.model.DatosMovimientoTesoreria;
import com.businessanalyzer.model.MovimientoTesoreria;
import com.businessanalyzer.model.PagoOperacion;
import com.businessanalyzer.model.TipoCuenta;
import com.businessanalyzer.model.TipoMovimientoTesoreria;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;

import java.math.BigDecimal;
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

/**
 * Acceso a las tablas de tesorería ({@code cuenta_dinero}, {@code movimiento_tesoreria},
 * {@code pago_operacion}) y a los flujos de dinero de todos los registros de la empresa.
 */
public class TesoreriaRepository {

    // ------------------------------------------------------------------ cuentas

    private static final String CONSULTA_CUENTAS = """
            SELECT id, empresa_id, nombre, tipo, numero, saldo_inicial, activa, registrado_por, registrado_en,
                   actualizado_por, actualizado_en
            FROM cuenta_dinero""";

    public List<CuentaDinero> listarCuentas(Connection c, long empresaId, boolean incluirInactivas) throws SQLException {
        String sql = CONSULTA_CUENTAS + " WHERE empresa_id = ?" + (incluirInactivas ? "" : " AND activa = 1")
                + " ORDER BY activa DESC, id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            return mapearCuentas(ps);
        }
    }

    public Optional<CuentaDinero> buscarCuenta(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA_CUENTAS + " WHERE id = ?")) {
            ps.setLong(1, id);
            List<CuentaDinero> resultado = mapearCuentas(ps);
            return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
        }
    }

    /** La primera cuenta activa (la más antigua): se usa cuando un registro no indica cuenta. */
    public Optional<CuentaDinero> cuentaPrincipal(Connection c, long empresaId) throws SQLException {
        List<CuentaDinero> activas = listarCuentas(c, empresaId, false);
        return activas.isEmpty() ? Optional.empty() : Optional.of(activas.get(0));
    }

    public boolean existeNombre(Connection c, long empresaId, String nombre, long excluirId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM cuenta_dinero WHERE empresa_id = ? AND nombre = ? AND id <> ?")) {
            ps.setLong(1, empresaId);
            ps.setString(2, nombre);
            ps.setLong(3, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insertarCuenta(Connection c, long empresaId, DatosCuenta d, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO cuenta_dinero (empresa_id, nombre, tipo, numero, saldo_inicial, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, empresaId);
            ps.setString(2, d.nombre());
            ps.setString(3, d.tipo().name());
            ps.setString(4, d.numero());
            ps.setLong(5, Montos.aEntero(d.saldoInicial()));
            ps.setString(6, usuario);
            ps.setString(7, Fechas.aTexto(ahora));
            ps.executeUpdate();
            return claveGenerada(ps, "la cuenta");
        }
    }

    public void actualizarCuenta(Connection c, long id, DatosCuenta d, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE cuenta_dinero
                SET nombre = ?, tipo = ?, numero = ?, saldo_inicial = ?, actualizado_por = ?, actualizado_en = ?
                WHERE id = ?""")) {
            ps.setString(1, d.nombre());
            ps.setString(2, d.tipo().name());
            ps.setString(3, d.numero());
            ps.setLong(4, Montos.aEntero(d.saldoInicial()));
            ps.setString(5, usuario);
            ps.setString(6, Fechas.aTexto(ahora));
            ps.setLong(7, id);
            exigirUnaFila(ps.executeUpdate(), "La cuenta ya no existe. Actualiza la lista.");
        }
    }

    public void cambiarActiva(Connection c, long id, boolean activa, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE cuenta_dinero SET activa = ?, actualizado_por = ?, actualizado_en = ? WHERE id = ?")) {
            ps.setInt(1, activa ? 1 : 0);
            ps.setString(2, usuario);
            ps.setString(3, Fechas.aTexto(ahora));
            ps.setLong(4, id);
            exigirUnaFila(ps.executeUpdate(), "La cuenta ya no existe. Actualiza la lista.");
        }
    }

    private static List<CuentaDinero> mapearCuentas(PreparedStatement ps) throws SQLException {
        List<CuentaDinero> cuentas = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cuentas.add(new CuentaDinero(
                        rs.getLong("id"),
                        rs.getLong("empresa_id"),
                        rs.getString("nombre"),
                        TipoCuenta.valueOf(rs.getString("tipo")),
                        rs.getString("numero"),
                        Montos.desdeEntero(rs.getLong("saldo_inicial")),
                        rs.getInt("activa") == 1,
                        rs.getString("registrado_por"),
                        Fechas.desdeTexto(rs.getString("registrado_en")),
                        rs.getString("actualizado_por"),
                        Fechas.desdeTexto(rs.getString("actualizado_en"))));
            }
        }
        return cuentas;
    }

    // ------------------------------------------------------------------ transferencias, aportes y retiros

    private static final String CONSULTA_MOVIMIENTOS = """
            SELECT m.id, m.empresa_id, m.tipo, m.fecha, m.cuenta_origen_id, o.nombre AS origen, m.cuenta_destino_id,
                   d.nombre AS destino, m.monto, m.descripcion, m.estado, m.motivo_anulacion, m.registrado_por,
                   m.registrado_en
            FROM movimiento_tesoreria m
            LEFT JOIN cuenta_dinero o ON o.id = m.cuenta_origen_id
            LEFT JOIN cuenta_dinero d ON d.id = m.cuenta_destino_id""";

    public List<MovimientoTesoreria> listarMovimientos(Connection c, long empresaId, LocalDate desde, LocalDate hasta,
                                                       boolean incluirAnulados) throws SQLException {
        String sql = CONSULTA_MOVIMIENTOS + " WHERE m.empresa_id = ? AND m.fecha BETWEEN ? AND ?"
                + (incluirAnulados ? "" : " AND m.estado = 'REGISTRADO'") + " ORDER BY m.fecha DESC, m.id DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setString(2, Fechas.fechaATexto(desde));
            ps.setString(3, Fechas.fechaATexto(hasta));
            return mapearMovimientos(ps);
        }
    }

    public Optional<MovimientoTesoreria> buscarMovimiento(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA_MOVIMIENTOS + " WHERE m.id = ?")) {
            ps.setLong(1, id);
            List<MovimientoTesoreria> resultado = mapearMovimientos(ps);
            return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
        }
    }

    public long insertarMovimiento(Connection c, long empresaId, DatosMovimientoTesoreria d, String usuario,
                                   LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO movimiento_tesoreria (empresa_id, tipo, fecha, cuenta_origen_id, cuenta_destino_id, monto,
                                                  descripcion, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, empresaId);
            ps.setString(2, d.tipo().name());
            ps.setString(3, Fechas.fechaATexto(d.fecha()));
            Sql.enteroOpcional(ps, 4, d.cuentaOrigenId());
            Sql.enteroOpcional(ps, 5, d.cuentaDestinoId());
            ps.setLong(6, Montos.aEntero(d.monto()));
            ps.setString(7, d.descripcion());
            ps.setString(8, usuario);
            ps.setString(9, Fechas.aTexto(ahora));
            ps.executeUpdate();
            return claveGenerada(ps, "el movimiento");
        }
    }

    public void anularMovimiento(Connection c, long id, String motivo, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE movimiento_tesoreria
                SET estado = 'ANULADO', motivo_anulacion = ?, anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'REGISTRADO'""")) {
            ps.setString(1, motivo);
            ps.setString(2, usuario);
            ps.setString(3, Fechas.aTexto(ahora));
            ps.setLong(4, id);
            exigirUnaFila(ps.executeUpdate(), "El movimiento ya no existe o está anulado. Actualiza la lista.");
        }
    }

    private static List<MovimientoTesoreria> mapearMovimientos(PreparedStatement ps) throws SQLException {
        List<MovimientoTesoreria> movimientos = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                movimientos.add(new MovimientoTesoreria(
                        rs.getLong("id"),
                        rs.getLong("empresa_id"),
                        TipoMovimientoTesoreria.valueOf(rs.getString("tipo")),
                        Fechas.fechaDesdeTexto(rs.getString("fecha")),
                        Sql.enteroOpcional(rs, "cuenta_origen_id"),
                        rs.getString("origen"),
                        Sql.enteroOpcional(rs, "cuenta_destino_id"),
                        rs.getString("destino"),
                        Montos.desdeEntero(rs.getLong("monto")),
                        rs.getString("descripcion"),
                        "ANULADO".equals(rs.getString("estado")),
                        rs.getString("motivo_anulacion"),
                        rs.getString("registrado_por"),
                        Fechas.desdeTexto(rs.getString("registrado_en"))));
            }
        }
        return movimientos;
    }

    // ------------------------------------------------------------------ cobros y pagos de operaciones al crédito

    private static final String CONSULTA_PAGOS = """
            SELECT p.id, p.operacion_id, p.cuenta_id, cd.nombre AS cuenta, p.fecha, p.monto, p.nota, p.estado,
                   p.registrado_por, p.registrado_en, p.anulado_por, p.anulado_en
            FROM pago_operacion p
            JOIN cuenta_dinero cd ON cd.id = p.cuenta_id""";

    public List<PagoOperacion> listarPagos(Connection c, long operacionId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA_PAGOS
                + " WHERE p.operacion_id = ? ORDER BY p.fecha DESC, p.id DESC")) {
            ps.setLong(1, operacionId);
            return mapearPagos(ps);
        }
    }

    public Optional<PagoOperacion> buscarPago(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA_PAGOS + " WHERE p.id = ?")) {
            ps.setLong(1, id);
            List<PagoOperacion> resultado = mapearPagos(ps);
            return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
        }
    }

    /** Cantidad de cobros o pagos vigentes de la operación. */
    public int pagosVigentes(Connection c, long operacionId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM pago_operacion WHERE operacion_id = ? AND estado = 'REGISTRADO'")) {
            ps.setLong(1, operacionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public long insertarPago(Connection c, long operacionId, long cuentaId, LocalDate fecha, BigDecimal monto,
                             String nota, String usuario, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO pago_operacion (operacion_id, cuenta_id, fecha, monto, nota, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, operacionId);
            ps.setLong(2, cuentaId);
            ps.setString(3, Fechas.fechaATexto(fecha));
            ps.setLong(4, Montos.aEntero(monto));
            ps.setString(5, nota);
            ps.setString(6, usuario);
            ps.setString(7, Fechas.aTexto(ahora));
            ps.executeUpdate();
            return claveGenerada(ps, "el cobro o pago");
        }
    }

    public void anularPago(Connection c, long id, String usuario, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE pago_operacion SET estado = 'ANULADO', anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'REGISTRADO'""")) {
            ps.setString(1, usuario);
            ps.setString(2, Fechas.aTexto(ahora));
            ps.setLong(3, id);
            exigirUnaFila(ps.executeUpdate(), "El registro ya no existe o está anulado. Actualiza la lista.");
        }
    }

    private static List<PagoOperacion> mapearPagos(PreparedStatement ps) throws SQLException {
        List<PagoOperacion> pagos = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                pagos.add(new PagoOperacion(
                        rs.getLong("id"),
                        rs.getLong("operacion_id"),
                        rs.getLong("cuenta_id"),
                        rs.getString("cuenta"),
                        Fechas.fechaDesdeTexto(rs.getString("fecha")),
                        Montos.desdeEntero(rs.getLong("monto")),
                        rs.getString("nota"),
                        "ANULADO".equals(rs.getString("estado")),
                        rs.getString("registrado_por"),
                        Fechas.desdeTexto(rs.getString("registrado_en")),
                        rs.getString("anulado_por"),
                        Fechas.desdeTexto(rs.getString("anulado_en"))));
            }
        }
        return pagos;
    }

    // ------------------------------------------------------------------ flujos de dinero

    /**
     * Todas las entradas (+) y salidas (−) de dinero de las cuentas de la empresa, ordenadas por fecha.
     * Solo cuentan los registros vigentes que indican una cuenta: los anteriores a la tesorería no mueven
     * el saldo (su efecto ya está en el saldo inicial de la cuenta «Caja»).
     */
    public List<FlujoDinero> flujos(Connection c, long empresaId) throws SQLException {
        String sql = """
                SELECT fecha, cuenta_id, monto, rubro, concepto FROM (
                    SELECT o.fecha, o.cuenta_id,
                           CASE o.tipo WHEN 'VENTA' THEN o.total ELSE -o.total END AS monto,
                           CASE o.tipo WHEN 'VENTA' THEN 'COBROS_VENTAS' ELSE 'PAGOS_COMPRAS' END AS rubro,
                           CASE o.tipo WHEN 'VENTA' THEN 'Venta ' ELSE 'Compra ' END || o.numero AS concepto,
                           o.id AS orden
                    FROM operacion o
                    WHERE o.empresa_id = ? AND o.estado = 'REGISTRADA' AND o.condicion = 'CONTADO'
                      AND o.cuenta_id IS NOT NULL
                    UNION ALL
                    SELECT p.fecha, p.cuenta_id,
                           CASE o.tipo WHEN 'VENTA' THEN p.monto ELSE -p.monto END,
                           CASE o.tipo WHEN 'VENTA' THEN 'COBROS_VENTAS' ELSE 'PAGOS_COMPRAS' END,
                           CASE o.tipo WHEN 'VENTA' THEN 'Cobro de ' ELSE 'Pago de ' END || o.numero,
                           p.id
                    FROM pago_operacion p
                    JOIN operacion o ON o.id = p.operacion_id
                    WHERE o.empresa_id = ? AND p.estado = 'REGISTRADO' AND o.estado = 'REGISTRADA'
                    UNION ALL
                    SELECT m.fecha, m.cuenta_id,
                           CASE m.tipo WHEN 'INGRESO' THEN m.monto ELSE -m.monto END,
                           CASE WHEN m.tipo = 'INGRESO' THEN 'OTROS_INGRESOS'
                                WHEN m.categoria = 'INTERESES_PRESTAMOS' THEN 'INTERESES'
                                ELSE 'GASTOS' END,
                           m.descripcion,
                           m.id
                    FROM movimiento_financiero m
                    WHERE m.empresa_id = ? AND m.estado = 'REGISTRADO' AND m.cuenta_id IS NOT NULL
                    UNION ALL
                    SELECT a.fecha, a.cuenta_id, -a.valor, 'COMPRA_ACTIVOS', 'Compra: ' || a.descripcion, a.id
                    FROM activo a
                    WHERE a.empresa_id = ? AND a.cuenta_id IS NOT NULL
                    UNION ALL
                    SELECT d.fecha, d.cuenta_id, d.monto, 'PRESTAMOS', 'Préstamo de ' || d.acreedor, d.id
                    FROM deuda d
                    WHERE d.empresa_id = ? AND d.estado = 'VIGENTE' AND d.cuenta_id IS NOT NULL
                    UNION ALL
                    SELECT p.fecha, p.cuenta_id, -p.monto, 'PAGO_DEUDAS', 'Pago a ' || d.acreedor, p.id
                    FROM pago_deuda p
                    JOIN deuda d ON d.id = p.deuda_id
                    WHERE d.empresa_id = ? AND d.estado = 'VIGENTE' AND p.estado = 'REGISTRADO'
                      AND p.cuenta_id IS NOT NULL
                    UNION ALL
                    SELECT t.fecha, t.cuenta_destino_id, t.monto,
                           CASE t.tipo WHEN 'APORTE' THEN 'APORTES' ELSE 'TRANSFERENCIAS' END,
                           t.descripcion, t.id
                    FROM movimiento_tesoreria t
                    WHERE t.empresa_id = ? AND t.estado = 'REGISTRADO' AND t.cuenta_destino_id IS NOT NULL
                    UNION ALL
                    SELECT t.fecha, t.cuenta_origen_id, -t.monto,
                           CASE t.tipo WHEN 'RETIRO' THEN 'RETIROS' ELSE 'TRANSFERENCIAS' END,
                           t.descripcion, t.id
                    FROM movimiento_tesoreria t
                    WHERE t.empresa_id = ? AND t.estado = 'REGISTRADO' AND t.cuenta_origen_id IS NOT NULL
                )
                ORDER BY fecha, orden""";
        List<FlujoDinero> flujos = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 1; i <= 8; i++) {
                ps.setLong(i, empresaId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    flujos.add(new FlujoDinero(
                            Fechas.fechaDesdeTexto(rs.getString("fecha")),
                            rs.getLong("cuenta_id"),
                            Montos.desdeEntero(rs.getLong("monto")),
                            RubroFlujo.valueOf(rs.getString("rubro")),
                            rs.getString("concepto")));
                }
            }
        }
        return flujos;
    }

    // ------------------------------------------------------------------ utilidades

    private static long claveGenerada(PreparedStatement ps, String que) throws SQLException {
        try (ResultSet claves = ps.getGeneratedKeys()) {
            if (!claves.next()) {
                throw new DataAccessException("No se obtuvo el identificador de " + que);
            }
            return claves.getLong(1);
        }
    }

    private static void exigirUnaFila(int filas, String mensaje) {
        if (filas != 1) {
            throw new ValidacionException(mensaje);
        }
    }
}
