package com.businessanalyzer.repository;

import com.businessanalyzer.analytics.tesoreria.FlujoDineroFGEM;
import com.businessanalyzer.analytics.tesoreria.RubroFlujoFGEM;
import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CuentaDineroFGEM;
import com.businessanalyzer.model.DatosCuentaFGEM;
import com.businessanalyzer.model.DatosMovimientoTesoreriaFGEM;
import com.businessanalyzer.model.MovimientoTesoreriaFGEM;
import com.businessanalyzer.model.PagoOperacionFGEM;
import com.businessanalyzer.model.TipoCuentaFGEM;
import com.businessanalyzer.model.TipoMovimientoTesoreriaFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;

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
public class TesoreriaRepositoryFGEM {

    // ------------------------------------------------------------------ cuentas

    private static final String CONSULTA_CUENTAS_FGEM = """
            SELECT id, empresa_id, nombre, tipo, numero, saldo_inicial, activa, registrado_por, registrado_en,
                   actualizado_por, actualizado_en
            FROM cuenta_dinero""";

    public List<CuentaDineroFGEM> listarCuentasFGEM(Connection cFGEM, long empresaIdFGEM, boolean incluirInactivasFGEM) throws SQLException {
        String sqlFGEM = CONSULTA_CUENTAS_FGEM + " WHERE empresa_id = ?" + (incluirInactivasFGEM ? "" : " AND activa = 1")
                + " ORDER BY activa DESC, id";
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, empresaIdFGEM);
            return mapearCuentasFGEM(psFGEM);
        }
    }

    public Optional<CuentaDineroFGEM> buscarCuentaFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_CUENTAS_FGEM + " WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<CuentaDineroFGEM> resultadoFGEM = mapearCuentasFGEM(psFGEM);
            return resultadoFGEM.isEmpty() ? Optional.empty() : Optional.of(resultadoFGEM.get(0));
        }
    }

    /** La primera cuenta activa (la más antigua): se usa cuando un registro no indica cuenta. */
    public Optional<CuentaDineroFGEM> cuentaPrincipalFGEM(Connection cFGEM, long empresaIdFGEM) throws SQLException {
        List<CuentaDineroFGEM> activasFGEM = listarCuentasFGEM(cFGEM, empresaIdFGEM, false);
        return activasFGEM.isEmpty() ? Optional.empty() : Optional.of(activasFGEM.get(0));
    }

    public boolean existeNombreFGEM(Connection cFGEM, long empresaIdFGEM, String nombreFGEM, long excluirIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT 1 FROM cuenta_dinero WHERE empresa_id = ? AND nombre = ? AND id <> ?")) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, nombreFGEM);
            psFGEM.setLong(3, excluirIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next();
            }
        }
    }

    public long insertarCuentaFGEM(Connection cFGEM, long empresaIdFGEM, DatosCuentaFGEM dFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO cuenta_dinero (empresa_id, nombre, tipo, numero, saldo_inicial, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, dFGEM.nombreFGEM());
            psFGEM.setString(3, dFGEM.tipoFGEM().name());
            psFGEM.setString(4, dFGEM.numeroFGEM());
            psFGEM.setLong(5, MontosFGEM.aEnteroFGEM(dFGEM.saldoInicialFGEM()));
            psFGEM.setString(6, usuarioFGEM);
            psFGEM.setString(7, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            return claveGeneradaFGEM(psFGEM, "la cuenta");
        }
    }

    public void actualizarCuentaFGEM(Connection cFGEM, long idFGEM, DatosCuentaFGEM dFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE cuenta_dinero
                SET nombre = ?, tipo = ?, numero = ?, saldo_inicial = ?, actualizado_por = ?, actualizado_en = ?
                WHERE id = ?""")) {
            psFGEM.setString(1, dFGEM.nombreFGEM());
            psFGEM.setString(2, dFGEM.tipoFGEM().name());
            psFGEM.setString(3, dFGEM.numeroFGEM());
            psFGEM.setLong(4, MontosFGEM.aEnteroFGEM(dFGEM.saldoInicialFGEM()));
            psFGEM.setString(5, usuarioFGEM);
            psFGEM.setString(6, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(7, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate(), "La cuenta ya no existe. Actualiza la lista.");
        }
    }

    public void cambiarActivaFGEM(Connection cFGEM, long idFGEM, boolean activaFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "UPDATE cuenta_dinero SET activa = ?, actualizado_por = ?, actualizado_en = ? WHERE id = ?")) {
            psFGEM.setInt(1, activaFGEM ? 1 : 0);
            psFGEM.setString(2, usuarioFGEM);
            psFGEM.setString(3, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(4, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate(), "La cuenta ya no existe. Actualiza la lista.");
        }
    }

    private static List<CuentaDineroFGEM> mapearCuentasFGEM(PreparedStatement psFGEM) throws SQLException {
        List<CuentaDineroFGEM> cuentasFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                cuentasFGEM.add(new CuentaDineroFGEM(
                        rsFGEM.getLong("id"),
                        rsFGEM.getLong("empresa_id"),
                        rsFGEM.getString("nombre"),
                        TipoCuentaFGEM.valueOf(rsFGEM.getString("tipo")),
                        rsFGEM.getString("numero"),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("saldo_inicial")),
                        rsFGEM.getInt("activa") == 1,
                        rsFGEM.getString("registrado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("registrado_en")),
                        rsFGEM.getString("actualizado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("actualizado_en"))));
            }
        }
        return cuentasFGEM;
    }

    // ------------------------------------------------------------------ transferencias, aportes y retiros

    private static final String CONSULTA_MOVIMIENTOS_FGEM = """
            SELECT m.id, m.empresa_id, m.tipo, m.fecha, m.cuenta_origen_id, o.nombre AS origen, m.cuenta_destino_id,
                   d.nombre AS destino, m.monto, m.descripcion, m.estado, m.motivo_anulacion, m.registrado_por,
                   m.registrado_en
            FROM movimiento_tesoreria m
            LEFT JOIN cuenta_dinero o ON o.id = m.cuenta_origen_id
            LEFT JOIN cuenta_dinero d ON d.id = m.cuenta_destino_id""";

    public List<MovimientoTesoreriaFGEM> listarMovimientosFGEM(Connection cFGEM, long empresaIdFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM,
                                                       boolean incluirAnuladosFGEM) throws SQLException {
        String sqlFGEM = CONSULTA_MOVIMIENTOS_FGEM + " WHERE m.empresa_id = ? AND m.fecha BETWEEN ? AND ?"
                + (incluirAnuladosFGEM ? "" : " AND m.estado = 'REGISTRADO'") + " ORDER BY m.fecha DESC, m.id DESC";
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, FechasFGEM.fechaATextoFGEM(desdeFGEM));
            psFGEM.setString(3, FechasFGEM.fechaATextoFGEM(hastaFGEM));
            return mapearMovimientosFGEM(psFGEM);
        }
    }

    public Optional<MovimientoTesoreriaFGEM> buscarMovimientoFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_MOVIMIENTOS_FGEM + " WHERE m.id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<MovimientoTesoreriaFGEM> resultadoFGEM = mapearMovimientosFGEM(psFGEM);
            return resultadoFGEM.isEmpty() ? Optional.empty() : Optional.of(resultadoFGEM.get(0));
        }
    }

    public long insertarMovimientoFGEM(Connection cFGEM, long empresaIdFGEM, DatosMovimientoTesoreriaFGEM dFGEM, String usuarioFGEM,
                                   LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO movimiento_tesoreria (empresa_id, tipo, fecha, cuenta_origen_id, cuenta_destino_id, monto,
                                                  descripcion, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, dFGEM.tipoFGEM().name());
            psFGEM.setString(3, FechasFGEM.fechaATextoFGEM(dFGEM.fechaFGEM()));
            SqlFGEM.enteroOpcionalFGEM(psFGEM, 4, dFGEM.cuentaOrigenIdFGEM());
            SqlFGEM.enteroOpcionalFGEM(psFGEM, 5, dFGEM.cuentaDestinoIdFGEM());
            psFGEM.setLong(6, MontosFGEM.aEnteroFGEM(dFGEM.montoFGEM()));
            psFGEM.setString(7, dFGEM.descripcionFGEM());
            psFGEM.setString(8, usuarioFGEM);
            psFGEM.setString(9, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            return claveGeneradaFGEM(psFGEM, "el movimiento");
        }
    }

    public void anularMovimientoFGEM(Connection cFGEM, long idFGEM, String motivoFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE movimiento_tesoreria
                SET estado = 'ANULADO', motivo_anulacion = ?, anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'REGISTRADO'""")) {
            psFGEM.setString(1, motivoFGEM);
            psFGEM.setString(2, usuarioFGEM);
            psFGEM.setString(3, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(4, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate(), "El movimiento ya no existe o está anulado. Actualiza la lista.");
        }
    }

    private static List<MovimientoTesoreriaFGEM> mapearMovimientosFGEM(PreparedStatement psFGEM) throws SQLException {
        List<MovimientoTesoreriaFGEM> movimientosFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                movimientosFGEM.add(new MovimientoTesoreriaFGEM(
                        rsFGEM.getLong("id"),
                        rsFGEM.getLong("empresa_id"),
                        TipoMovimientoTesoreriaFGEM.valueOf(rsFGEM.getString("tipo")),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha")),
                        SqlFGEM.enteroOpcionalFGEM(rsFGEM, "cuenta_origen_id"),
                        rsFGEM.getString("origen"),
                        SqlFGEM.enteroOpcionalFGEM(rsFGEM, "cuenta_destino_id"),
                        rsFGEM.getString("destino"),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("monto")),
                        rsFGEM.getString("descripcion"),
                        "ANULADO".equals(rsFGEM.getString("estado")),
                        rsFGEM.getString("motivo_anulacion"),
                        rsFGEM.getString("registrado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("registrado_en"))));
            }
        }
        return movimientosFGEM;
    }

    // ------------------------------------------------------------------ cobros y pagos de operaciones al crédito

    private static final String CONSULTA_PAGOS_FGEM = """
            SELECT p.id, p.operacion_id, p.cuenta_id, cd.nombre AS cuenta, p.fecha, p.monto, p.nota, p.estado,
                   p.registrado_por, p.registrado_en, p.anulado_por, p.anulado_en
            FROM pago_operacion p
            JOIN cuenta_dinero cd ON cd.id = p.cuenta_id""";

    public List<PagoOperacionFGEM> listarPagosFGEM(Connection cFGEM, long operacionIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_PAGOS_FGEM
                + " WHERE p.operacion_id = ? ORDER BY p.fecha DESC, p.id DESC")) {
            psFGEM.setLong(1, operacionIdFGEM);
            return mapearPagosFGEM(psFGEM);
        }
    }

    public Optional<PagoOperacionFGEM> buscarPagoFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_PAGOS_FGEM + " WHERE p.id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<PagoOperacionFGEM> resultadoFGEM = mapearPagosFGEM(psFGEM);
            return resultadoFGEM.isEmpty() ? Optional.empty() : Optional.of(resultadoFGEM.get(0));
        }
    }

    /** Cantidad de cobros o pagos vigentes de la operación. */
    public int pagosVigentesFGEM(Connection cFGEM, long operacionIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT COUNT(*) FROM pago_operacion WHERE operacion_id = ? AND estado = 'REGISTRADO'")) {
            psFGEM.setLong(1, operacionIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next() ? rsFGEM.getInt(1) : 0;
            }
        }
    }

    public long insertarPagoFGEM(Connection cFGEM, long operacionIdFGEM, long cuentaIdFGEM, LocalDate fechaFGEM, BigDecimal montoFGEM,
                             String notaFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO pago_operacion (operacion_id, cuenta_id, fecha, monto, nota, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            psFGEM.setLong(1, operacionIdFGEM);
            psFGEM.setLong(2, cuentaIdFGEM);
            psFGEM.setString(3, FechasFGEM.fechaATextoFGEM(fechaFGEM));
            psFGEM.setLong(4, MontosFGEM.aEnteroFGEM(montoFGEM));
            psFGEM.setString(5, notaFGEM);
            psFGEM.setString(6, usuarioFGEM);
            psFGEM.setString(7, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            return claveGeneradaFGEM(psFGEM, "el cobro o pago");
        }
    }

    public void anularPagoFGEM(Connection cFGEM, long idFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE pago_operacion SET estado = 'ANULADO', anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'REGISTRADO'""")) {
            psFGEM.setString(1, usuarioFGEM);
            psFGEM.setString(2, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(3, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate(), "El registro ya no existe o está anulado. Actualiza la lista.");
        }
    }

    private static List<PagoOperacionFGEM> mapearPagosFGEM(PreparedStatement psFGEM) throws SQLException {
        List<PagoOperacionFGEM> pagosFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                pagosFGEM.add(new PagoOperacionFGEM(
                        rsFGEM.getLong("id"),
                        rsFGEM.getLong("operacion_id"),
                        rsFGEM.getLong("cuenta_id"),
                        rsFGEM.getString("cuenta"),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("monto")),
                        rsFGEM.getString("nota"),
                        "ANULADO".equals(rsFGEM.getString("estado")),
                        rsFGEM.getString("registrado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("registrado_en")),
                        rsFGEM.getString("anulado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("anulado_en"))));
            }
        }
        return pagosFGEM;
    }

    // ------------------------------------------------------------------ flujos de dinero

    /**
     * Todas las entradas (+) y salidas (−) de dinero de las cuentas de la empresa, ordenadas por fecha.
     * Solo cuentan los registros vigentes que indican una cuenta: los anteriores a la tesorería no mueven
     * el saldo (su efecto ya está en el saldo inicial de la cuenta «Caja»).
     */
    public List<FlujoDineroFGEM> flujosFGEM(Connection cFGEM, long empresaIdFGEM) throws SQLException {
        String sqlFGEM = """
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
        List<FlujoDineroFGEM> flujosFGEM = new ArrayList<>();
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            for (int iFGEM = 1; iFGEM <= 8; iFGEM++) {
                psFGEM.setLong(iFGEM, empresaIdFGEM);
            }
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    flujosFGEM.add(new FlujoDineroFGEM(
                            FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha")),
                            rsFGEM.getLong("cuenta_id"),
                            MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("monto")),
                            RubroFlujoFGEM.valueOf(rsFGEM.getString("rubro")),
                            rsFGEM.getString("concepto")));
                }
            }
        }
        return flujosFGEM;
    }

    // ------------------------------------------------------------------ utilidades

    private static long claveGeneradaFGEM(PreparedStatement psFGEM, String queFGEM) throws SQLException {
        try (ResultSet clavesFGEM = psFGEM.getGeneratedKeys()) {
            if (!clavesFGEM.next()) {
                throw new DataAccessExceptionFGEM("No se obtuvo el identificador de " + queFGEM);
            }
            return clavesFGEM.getLong(1);
        }
    }

    private static void exigirUnaFilaFGEM(int filasFGEM, String mensajeFGEM) {
        if (filasFGEM != 1) {
            throw new ValidacionExceptionFGEM(mensajeFGEM);
        }
    }
}
