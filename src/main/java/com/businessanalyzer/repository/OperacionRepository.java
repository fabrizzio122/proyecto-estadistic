package com.businessanalyzer.repository;

import com.businessanalyzer.analytics.CalculadoraOperacion;
import com.businessanalyzer.analytics.ResumenOperaciones;
import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.EstadoOperacion;
import com.businessanalyzer.model.LineaOperacion;
import com.businessanalyzer.model.Operacion;
import com.businessanalyzer.model.TipoOperacion;
import com.businessanalyzer.model.UnidadMedida;
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

/** Acceso a las tablas {@code operacion} y {@code operacion_linea}. */
public class OperacionRepository {

    /** Cabecera de una operación nueva, con los totales ya calculados. */
    public record NuevaOperacion(TipoOperacion tipo, String numero, LocalDate fecha, String contraparte,
                                 String documentoContraparte, String comprobante, BigDecimal tasaImpuesto,
                                 CalculadoraOperacion.Totales totales, String observaciones,
                                 String registradoPor, LocalDateTime registradoEn) {
    }

    private static final String CABECERA = """
            SELECT o.id, o.empresa_id, o.tipo, o.numero, o.fecha, o.contraparte, o.documento_contraparte,
                   o.comprobante, o.tasa_impuesto, o.subtotal, o.impuesto, o.total, o.costo_total, o.estado,
                   o.observaciones, o.registrado_por, o.registrado_en, o.motivo_anulacion, o.anulado_por,
                   o.anulado_en,
                   (SELECT COUNT(*) FROM operacion_linea ol WHERE ol.operacion_id = o.id) AS items
            FROM operacion o""";

    public List<Operacion> listar(Connection c, long empresaId, TipoOperacion tipo, LocalDate desde,
                                  LocalDate hasta, boolean incluirAnuladas) throws SQLException {
        String sql = CABECERA + " WHERE o.empresa_id = ? AND o.tipo = ? AND o.fecha BETWEEN ? AND ?"
                + (incluirAnuladas ? "" : " AND o.estado = 'REGISTRADA'")
                + " ORDER BY o.fecha DESC, o.id DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setString(2, tipo.name());
            ps.setString(3, Fechas.fechaATexto(desde));
            ps.setString(4, Fechas.fechaATexto(hasta));
            return mapearCabeceras(ps);
        }
    }

    /** Cabecera y líneas. */
    public Optional<Operacion> buscarPorId(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CABECERA + " WHERE o.id = ?")) {
            ps.setLong(1, id);
            List<Operacion> resultado = mapearCabeceras(ps);
            if (resultado.isEmpty()) {
                return Optional.empty();
            }
            Operacion o = resultado.get(0);
            return Optional.of(new Operacion(o.id(), o.empresaId(), o.tipo(), o.numero(), o.fecha(),
                    o.contraparte(), o.documentoContraparte(), o.comprobante(), o.tasaImpuesto(), o.subtotal(),
                    o.impuesto(), o.total(), o.costoTotal(), o.estado(), o.observaciones(), o.registradoPor(),
                    o.registradoEn(), o.motivoAnulacion(), o.anuladoPor(), o.anuladoEn(), o.cantidadItems(),
                    lineas(c, id)));
        }
    }

    public List<LineaOperacion> lineas(Connection c, long operacionId) throws SQLException {
        List<LineaOperacion> lineas = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT ol.id, ol.producto_id, p.codigo, p.nombre, p.unidad, ol.cantidad, ol.precio_unitario,
                       ol.costo_unitario, ol.subtotal
                FROM operacion_linea ol
                JOIN producto p ON p.id = ol.producto_id
                WHERE ol.operacion_id = ?
                ORDER BY ol.id""")) {
            ps.setLong(1, operacionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lineas.add(new LineaOperacion(
                            rs.getLong("id"),
                            rs.getLong("producto_id"),
                            rs.getString("codigo"),
                            rs.getString("nombre"),
                            UnidadMedida.valueOf(rs.getString("unidad")),
                            Montos.desdeEntero(rs.getLong("cantidad")),
                            Montos.desdeEntero(rs.getLong("precio_unitario")),
                            Montos.desdeEntero(rs.getLong("costo_unitario")),
                            Montos.desdeEntero(rs.getLong("subtotal"))));
                }
            }
        }
        return lineas;
    }

    /** Siguiente número para la empresa y el tipo: V-000001, C-000001... */
    public String siguienteNumero(Connection c, long empresaId, TipoOperacion tipo) throws SQLException {
        String prefijo = tipo.prefijoNumero();
        int mayor = 0;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT numero FROM operacion WHERE empresa_id = ? AND tipo = ?")) {
            ps.setLong(1, empresaId);
            ps.setString(2, tipo.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String numero = rs.getString(1);
                    if (numero.startsWith(prefijo + "-") && numero.substring(prefijo.length() + 1).matches("\\d{1,9}")) {
                        mayor = Math.max(mayor, Integer.parseInt(numero.substring(prefijo.length() + 1)));
                    }
                }
            }
        }
        return String.format("%s-%06d", prefijo, mayor + 1);
    }

    public long insertar(Connection c, long empresaId, NuevaOperacion n) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO operacion (empresa_id, tipo, numero, fecha, contraparte, documento_contraparte,
                                       comprobante, tasa_impuesto, subtotal, impuesto, total, costo_total,
                                       observaciones, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, empresaId);
            ps.setString(2, n.tipo().name());
            ps.setString(3, n.numero());
            ps.setString(4, Fechas.fechaATexto(n.fecha()));
            ps.setString(5, n.contraparte());
            ps.setString(6, n.documentoContraparte());
            ps.setString(7, n.comprobante());
            ps.setLong(8, Montos.aEntero(n.tasaImpuesto()));
            ps.setLong(9, Montos.aEntero(n.totales().subtotal()));
            ps.setLong(10, Montos.aEntero(n.totales().impuesto()));
            ps.setLong(11, Montos.aEntero(n.totales().total()));
            ps.setLong(12, Montos.aEntero(n.totales().costoTotal()));
            ps.setString(13, n.observaciones());
            ps.setString(14, n.registradoPor());
            ps.setString(15, Fechas.aTexto(n.registradoEn()));
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new DataAccessException("No se obtuvo el identificador de la operación creada");
                }
                return claves.getLong(1);
            }
        }
    }

    public void insertarLinea(Connection c, long operacionId, long productoId, BigDecimal cantidad,
                              BigDecimal precioUnitario, BigDecimal costoUnitario, BigDecimal subtotal)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO operacion_linea (operacion_id, producto_id, cantidad, precio_unitario,
                                             costo_unitario, subtotal)
                VALUES (?, ?, ?, ?, ?, ?)""")) {
            ps.setLong(1, operacionId);
            ps.setLong(2, productoId);
            ps.setLong(3, Montos.aEntero(cantidad));
            ps.setLong(4, Montos.aEntero(precioUnitario));
            ps.setLong(5, Montos.aEntero(costoUnitario));
            ps.setLong(6, Montos.aEntero(subtotal));
            ps.executeUpdate();
        }
    }

    public void anular(Connection c, long id, String motivo, String usuario, LocalDateTime fecha) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE operacion
                SET estado = 'ANULADA', motivo_anulacion = ?, anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'REGISTRADA'""")) {
            ps.setString(1, motivo);
            ps.setString(2, usuario);
            ps.setString(3, Fechas.aTexto(fecha));
            ps.setLong(4, id);
            if (ps.executeUpdate() != 1) {
                throw new ValidacionException("La operación ya no existe o ya estaba anulada. Actualiza la lista.");
            }
        }
    }

    /** Totales del período, solo con operaciones registradas (no anuladas). */
    public ResumenOperaciones resumen(Connection c, long empresaId, LocalDate desde, LocalDate hasta)
            throws SQLException {
        BigDecimal ventas = BigDecimal.ZERO;
        BigDecimal costoVentas = BigDecimal.ZERO;
        BigDecimal compras = BigDecimal.ZERO;
        int cantidadVentas = 0;
        int cantidadCompras = 0;
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT tipo, COUNT(*) AS cantidad, COALESCE(SUM(subtotal), 0) AS subtotal,
                       COALESCE(SUM(costo_total), 0) AS costo
                FROM operacion
                WHERE empresa_id = ? AND estado = 'REGISTRADA' AND fecha BETWEEN ? AND ?
                GROUP BY tipo""")) {
            ps.setLong(1, empresaId);
            ps.setString(2, Fechas.fechaATexto(desde));
            ps.setString(3, Fechas.fechaATexto(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (TipoOperacion.VENTA.name().equals(rs.getString("tipo"))) {
                        cantidadVentas = rs.getInt("cantidad");
                        ventas = Montos.desdeEntero(rs.getLong("subtotal"));
                        costoVentas = Montos.desdeEntero(rs.getLong("costo"));
                    } else {
                        cantidadCompras = rs.getInt("cantidad");
                        compras = Montos.desdeEntero(rs.getLong("subtotal"));
                    }
                }
            }
        }
        return new ResumenOperaciones(ventas, costoVentas, compras, cantidadVentas, cantidadCompras);
    }

    private static List<Operacion> mapearCabeceras(PreparedStatement ps) throws SQLException {
        List<Operacion> operaciones = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                operaciones.add(new Operacion(
                        rs.getLong("id"),
                        rs.getLong("empresa_id"),
                        TipoOperacion.valueOf(rs.getString("tipo")),
                        rs.getString("numero"),
                        Fechas.fechaDesdeTexto(rs.getString("fecha")),
                        rs.getString("contraparte"),
                        rs.getString("documento_contraparte"),
                        rs.getString("comprobante"),
                        Montos.desdeEntero(rs.getLong("tasa_impuesto")),
                        Montos.desdeEntero(rs.getLong("subtotal")),
                        Montos.desdeEntero(rs.getLong("impuesto")),
                        Montos.desdeEntero(rs.getLong("total")),
                        Montos.desdeEntero(rs.getLong("costo_total")),
                        EstadoOperacion.valueOf(rs.getString("estado")),
                        rs.getString("observaciones"),
                        rs.getString("registrado_por"),
                        Fechas.desdeTexto(rs.getString("registrado_en")),
                        rs.getString("motivo_anulacion"),
                        rs.getString("anulado_por"),
                        Fechas.desdeTexto(rs.getString("anulado_en")),
                        rs.getInt("items"),
                        List.of()));
            }
        }
        return operaciones;
    }
}
