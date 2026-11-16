package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CategoriaDeudaFGEM;
import com.businessanalyzer.model.DatosDeudaFGEM;
import com.businessanalyzer.model.DeudaFGEM;
import com.businessanalyzer.model.PagoDeudaFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;

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
public class DeudaRepositoryFGEM {

    private static final String CONSULTA_FGEM = """
            SELECT d.id, d.empresa_id, d.categoria, d.acreedor, d.descripcion, d.fecha, d.monto, d.tasa_anual,
                   d.cuota_mensual, d.vencimiento, d.estado, d.motivo_anulacion, d.registrado_por, d.registrado_en,
                   d.actualizado_por, d.actualizado_en, d.cuenta_id,
                   (SELECT COALESCE(SUM(p.monto), 0) FROM pago_deuda p
                    WHERE p.deuda_id = d.id AND p.estado = 'REGISTRADO') AS pagado
            FROM deuda d""";

    private static final String CONSULTA_PAGOS_FGEM = """
            SELECT id, deuda_id, fecha, monto, nota, estado, registrado_por, registrado_en, anulado_por, anulado_en,
                   interes, cuenta_id,
                   (SELECT cd.nombre FROM cuenta_dinero cd WHERE cd.id = pago_deuda.cuenta_id) AS cuenta
            FROM pago_deuda""";

    public List<DeudaFGEM> listarFGEM(Connection cFGEM, long empresaIdFGEM, boolean incluirAnuladasFGEM) throws SQLException {
        String sqlFGEM = CONSULTA_FGEM + " WHERE d.empresa_id = ?"
                + (incluirAnuladasFGEM ? "" : " AND d.estado = 'VIGENTE'")
                + " ORDER BY d.fecha DESC, d.id DESC";
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, empresaIdFGEM);
            return mapearFGEM(psFGEM);
        }
    }

    public Optional<DeudaFGEM> buscarPorIdFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_FGEM + " WHERE d.id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<DeudaFGEM> resultadoFGEM = mapearFGEM(psFGEM);
            return resultadoFGEM.isEmpty() ? Optional.empty() : Optional.of(resultadoFGEM.get(0));
        }
    }

    public long insertarFGEM(Connection cFGEM, long empresaIdFGEM, DatosDeudaFGEM dFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO deuda (categoria, acreedor, descripcion, fecha, monto, tasa_anual, cuota_mensual,
                                   vencimiento, cuenta_id, empresa_id, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarDatosFGEM(psFGEM, dFGEM);
            psFGEM.setLong(10, empresaIdFGEM);
            psFGEM.setString(11, usuarioFGEM);
            psFGEM.setString(12, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            return claveGeneradaFGEM(psFGEM, "la deuda");
        }
    }

    public void actualizarFGEM(Connection cFGEM, long idFGEM, DatosDeudaFGEM dFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE deuda
                SET categoria = ?, acreedor = ?, descripcion = ?, fecha = ?, monto = ?, tasa_anual = ?,
                    cuota_mensual = ?, vencimiento = ?, cuenta_id = ?, actualizado_por = ?, actualizado_en = ?
                WHERE id = ? AND estado = 'VIGENTE'""")) {
            asignarDatosFGEM(psFGEM, dFGEM);
            psFGEM.setString(10, usuarioFGEM);
            psFGEM.setString(11, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(12, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate(), "La deuda ya no existe o está anulada. Actualiza la lista.");
        }
    }

    public void anularFGEM(Connection cFGEM, long idFGEM, String motivoFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE deuda
                SET estado = 'ANULADA', motivo_anulacion = ?, anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'VIGENTE'""")) {
            psFGEM.setString(1, motivoFGEM);
            psFGEM.setString(2, usuarioFGEM);
            psFGEM.setString(3, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(4, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate(), "La deuda ya no existe o está anulada. Actualiza la lista.");
        }
    }

    // ------------------------------------------------------------------ pagos

    public List<PagoDeudaFGEM> listarPagosFGEM(Connection cFGEM, long deudaIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_PAGOS_FGEM
                + " WHERE deuda_id = ? ORDER BY fecha DESC, id DESC")) {
            psFGEM.setLong(1, deudaIdFGEM);
            return mapearPagosFGEM(psFGEM);
        }
    }

    public Optional<PagoDeudaFGEM> buscarPagoFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_PAGOS_FGEM + " WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<PagoDeudaFGEM> resultadoFGEM = mapearPagosFGEM(psFGEM);
            return resultadoFGEM.isEmpty() ? Optional.empty() : Optional.of(resultadoFGEM.get(0));
        }
    }

    /** Fecha del primer pago vigente; vacío si no hay pagos. */
    public Optional<LocalDate> primerPagoFGEM(Connection cFGEM, long deudaIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT MIN(fecha) FROM pago_deuda WHERE deuda_id = ? AND estado = 'REGISTRADO'")) {
            psFGEM.setLong(1, deudaIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next() ? Optional.ofNullable(FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString(1))) : Optional.empty();
            }
        }
    }

    /**
     * @param capitalFGEM      lo que baja el saldo de la deuda
     * @param interesFGEM      intereses de la cuota (cero si no hay)
     * @param cuentaIdFGEM     cuenta de la que salió el pago
     * @param movimientoIdFGEM gasto financiero con el que se registraron los intereses; {@code null} si no hay
     */
    public long insertarPagoFGEM(Connection cFGEM, long deudaIdFGEM, LocalDate fechaFGEM, BigDecimal capitalFGEM, BigDecimal interesFGEM,
                             Long cuentaIdFGEM, Long movimientoIdFGEM, String notaFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO pago_deuda (deuda_id, fecha, monto, interes, cuenta_id, movimiento_id, nota,
                                        registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            psFGEM.setLong(1, deudaIdFGEM);
            psFGEM.setString(2, FechasFGEM.fechaATextoFGEM(fechaFGEM));
            psFGEM.setLong(3, MontosFGEM.aEnteroFGEM(capitalFGEM));
            psFGEM.setLong(4, MontosFGEM.aEnteroFGEM(interesFGEM));
            SqlFGEM.enteroOpcionalFGEM(psFGEM, 5, cuentaIdFGEM);
            SqlFGEM.enteroOpcionalFGEM(psFGEM, 6, movimientoIdFGEM);
            psFGEM.setString(7, notaFGEM);
            psFGEM.setString(8, usuarioFGEM);
            psFGEM.setString(9, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            return claveGeneradaFGEM(psFGEM, "el pago");
        }
    }

    /** Gasto financiero (intereses) que se generó con el pago; vacío si el pago no tuvo intereses. */
    public Optional<Long> movimientoDelPagoFGEM(Connection cFGEM, long pagoIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("SELECT movimiento_id FROM pago_deuda WHERE id = ?")) {
            psFGEM.setLong(1, pagoIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next() ? Optional.ofNullable(SqlFGEM.enteroOpcionalFGEM(rsFGEM, "movimiento_id")) : Optional.empty();
            }
        }
    }

    /** Gastos financieros (intereses) de los pagos vigentes de la deuda. */
    public List<Long> movimientosDeLaDeudaFGEM(Connection cFGEM, long deudaIdFGEM) throws SQLException {
        List<Long> idsFGEM = new ArrayList<>();
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                SELECT movimiento_id FROM pago_deuda
                WHERE deuda_id = ? AND estado = 'REGISTRADO' AND movimiento_id IS NOT NULL""")) {
            psFGEM.setLong(1, deudaIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    idsFGEM.add(rsFGEM.getLong(1));
                }
            }
        }
        return idsFGEM;
    }

    public void anularPagoFGEM(Connection cFGEM, long idFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE pago_deuda SET estado = 'ANULADO', anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'REGISTRADO'""")) {
            psFGEM.setString(1, usuarioFGEM);
            psFGEM.setString(2, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(3, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate(), "El pago ya no existe o está anulado. Actualiza la lista.");
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static void asignarDatosFGEM(PreparedStatement psFGEM, DatosDeudaFGEM dFGEM) throws SQLException {
        psFGEM.setString(1, dFGEM.categoriaFGEM().name());
        psFGEM.setString(2, dFGEM.acreedorFGEM());
        psFGEM.setString(3, dFGEM.descripcionFGEM());
        psFGEM.setString(4, FechasFGEM.fechaATextoFGEM(dFGEM.fechaFGEM()));
        psFGEM.setLong(5, MontosFGEM.aEnteroFGEM(dFGEM.montoFGEM()));
        montoOpcionalFGEM(psFGEM, 6, dFGEM.tasaAnualFGEM());
        montoOpcionalFGEM(psFGEM, 7, dFGEM.cuotaMensualFGEM());
        psFGEM.setString(8, FechasFGEM.fechaATextoFGEM(dFGEM.vencimientoFGEM()));
        SqlFGEM.enteroOpcionalFGEM(psFGEM, 9, dFGEM.cuentaIdFGEM());
    }

    private static void montoOpcionalFGEM(PreparedStatement psFGEM, int indiceFGEM, BigDecimal valorFGEM) throws SQLException {
        if (valorFGEM == null) {
            psFGEM.setNull(indiceFGEM, Types.INTEGER);
        } else {
            psFGEM.setLong(indiceFGEM, MontosFGEM.aEnteroFGEM(valorFGEM));
        }
    }

    private static BigDecimal leerOpcionalFGEM(ResultSet rsFGEM, String columnaFGEM) throws SQLException {
        long valorFGEM = rsFGEM.getLong(columnaFGEM);
        return rsFGEM.wasNull() ? null : MontosFGEM.desdeEnteroFGEM(valorFGEM);
    }

    private static long claveGeneradaFGEM(PreparedStatement psFGEM, String queFGEM) throws SQLException {
        try (ResultSet clavesFGEM = psFGEM.getGeneratedKeys()) {
            if (!clavesFGEM.next()) {
                throw new DataAccessExceptionFGEM("No se obtuvo el identificador de " + queFGEM);
            }
            return clavesFGEM.getLong(1);
        }
    }

    private static List<DeudaFGEM> mapearFGEM(PreparedStatement psFGEM) throws SQLException {
        List<DeudaFGEM> deudasFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                deudasFGEM.add(new DeudaFGEM(
                        rsFGEM.getLong("id"),
                        rsFGEM.getLong("empresa_id"),
                        CategoriaDeudaFGEM.valueOf(rsFGEM.getString("categoria")),
                        rsFGEM.getString("acreedor"),
                        rsFGEM.getString("descripcion"),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("monto")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("pagado")),
                        leerOpcionalFGEM(rsFGEM, "tasa_anual"),
                        leerOpcionalFGEM(rsFGEM, "cuota_mensual"),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("vencimiento")),
                        "ANULADA".equals(rsFGEM.getString("estado")),
                        rsFGEM.getString("motivo_anulacion"),
                        rsFGEM.getString("registrado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("registrado_en")),
                        rsFGEM.getString("actualizado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("actualizado_en")),
                        SqlFGEM.enteroOpcionalFGEM(rsFGEM, "cuenta_id")));
            }
        }
        return deudasFGEM;
    }

    private static List<PagoDeudaFGEM> mapearPagosFGEM(PreparedStatement psFGEM) throws SQLException {
        List<PagoDeudaFGEM> pagosFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                pagosFGEM.add(new PagoDeudaFGEM(
                        rsFGEM.getLong("id"),
                        rsFGEM.getLong("deuda_id"),
                        FechasFGEM.fechaDesdeTextoFGEM(rsFGEM.getString("fecha")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("monto")),
                        rsFGEM.getString("nota"),
                        "ANULADO".equals(rsFGEM.getString("estado")),
                        rsFGEM.getString("registrado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("registrado_en")),
                        rsFGEM.getString("anulado_por"),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("anulado_en")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("interes")),
                        SqlFGEM.enteroOpcionalFGEM(rsFGEM, "cuenta_id"),
                        rsFGEM.getString("cuenta")));
            }
        }
        return pagosFGEM;
    }

    private static void exigirUnaFilaFGEM(int filasFGEM, String mensajeFGEM) {
        if (filasFGEM != 1) {
            throw new ValidacionExceptionFGEM(mensajeFGEM);
        }
    }
}
