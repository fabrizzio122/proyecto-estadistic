package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CategoriaGanado;
import com.businessanalyzer.model.DatosMovimientoGanado;
import com.businessanalyzer.model.DatosProduccion;
import com.businessanalyzer.model.Especie;
import com.businessanalyzer.model.MovimientoGanado;
import com.businessanalyzer.model.ProduccionGanadera;
import com.businessanalyzer.model.ProductoGanadero;
import com.businessanalyzer.model.TipoMovimientoGanado;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Acceso a las tablas {@code movimiento_ganado}, {@code produccion_ganadera} y {@code precio_ganado}. */
public class GanaderiaRepository {

    private static final String CONSULTA_MOVIMIENTOS = """
            SELECT id, empresa_id, fecha, tipo, categoria, cantidad, categoria_destino, monto, causa, observacion,
                   estado, motivo_anulacion, registrado_por, registrado_en
            FROM movimiento_ganado""";

    private static final String CONSULTA_PRODUCCION = """
            SELECT id, empresa_id, especie, producto, fecha, dias, cantidad, animales, precio_unitario, observacion,
                   estado, registrado_por, registrado_en
            FROM produccion_ganadera""";

    // ------------------------------------------------------------------ movimientos

    /** Todos los movimientos hasta la fecha indicada (incluida), del más antiguo al más reciente. */
    public List<MovimientoGanado> listarMovimientos(Connection c, long empresaId, LocalDate hasta,
                                                    boolean incluirAnulados) throws SQLException {
        String sql = CONSULTA_MOVIMIENTOS + " WHERE empresa_id = ? AND fecha <= ?"
                + (incluirAnulados ? "" : " AND estado = 'REGISTRADO'")
                + " ORDER BY fecha, id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setString(2, Fechas.fechaATexto(hasta));
            return mapearMovimientos(ps);
        }
    }

    public Optional<MovimientoGanado> buscarMovimiento(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA_MOVIMIENTOS + " WHERE id = ?")) {
            ps.setLong(1, id);
            List<MovimientoGanado> r = mapearMovimientos(ps);
            return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
        }
    }

    public long insertarMovimiento(Connection c, long empresaId, DatosMovimientoGanado d, String usuario,
                                   LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO movimiento_ganado (empresa_id, fecha, tipo, categoria, cantidad, categoria_destino, monto,
                                               causa, observacion, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, empresaId);
            ps.setString(2, Fechas.fechaATexto(d.fecha()));
            ps.setString(3, d.tipo().name());
            ps.setString(4, d.categoria().name());
            ps.setInt(5, d.cantidad());
            ps.setString(6, d.categoriaDestino() == null ? null : d.categoriaDestino().name());
            montoOpcional(ps, 7, d.monto());
            ps.setString(8, d.causa());
            ps.setString(9, d.observacion());
            ps.setString(10, usuario);
            ps.setString(11, Fechas.aTexto(ahora));
            ps.executeUpdate();
            return clave(ps);
        }
    }

    public void anularMovimiento(Connection c, long id, String motivo, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE movimiento_ganado SET estado = 'ANULADO', motivo_anulacion = ?, anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'REGISTRADO'""")) {
            ps.setString(1, motivo);
            ps.setString(2, usuario);
            ps.setString(3, Fechas.aTexto(ahora));
            ps.setLong(4, id);
            exigirUnaFila(ps.executeUpdate(), "El movimiento ya no existe o está anulado. Actualiza la lista.");
        }
    }

    // ------------------------------------------------------------------ producción

    public List<ProduccionGanadera> listarProduccion(Connection c, long empresaId, LocalDate desde, LocalDate hasta,
                                                     boolean incluirAnulados) throws SQLException {
        String sql = CONSULTA_PRODUCCION + " WHERE empresa_id = ? AND fecha BETWEEN ? AND ?"
                + (incluirAnulados ? "" : " AND estado = 'REGISTRADO'")
                + " ORDER BY fecha DESC, id DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setString(2, Fechas.fechaATexto(desde));
            ps.setString(3, Fechas.fechaATexto(hasta));
            return mapearProduccion(ps);
        }
    }

    public Optional<ProduccionGanadera> buscarProduccion(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA_PRODUCCION + " WHERE id = ?")) {
            ps.setLong(1, id);
            List<ProduccionGanadera> r = mapearProduccion(ps);
            return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
        }
    }

    public long insertarProduccion(Connection c, long empresaId, DatosProduccion d, String usuario,
                                   LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO produccion_ganadera (empresa_id, especie, producto, fecha, dias, cantidad, animales,
                                                 precio_unitario, observacion, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, empresaId);
            ps.setString(2, d.especie().name());
            ps.setString(3, d.producto().name());
            ps.setString(4, Fechas.fechaATexto(d.fecha()));
            ps.setInt(5, d.dias());
            ps.setLong(6, Montos.aEntero(d.cantidad()));
            if (d.animales() == null) {
                ps.setNull(7, Types.INTEGER);
            } else {
                ps.setInt(7, d.animales());
            }
            montoOpcional(ps, 8, d.precioUnitario());
            ps.setString(9, d.observacion());
            ps.setString(10, usuario);
            ps.setString(11, Fechas.aTexto(ahora));
            ps.executeUpdate();
            return clave(ps);
        }
    }

    public void anularProduccion(Connection c, long id, String usuario, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE produccion_ganadera SET estado = 'ANULADO', anulado_por = ?, anulado_en = ?
                WHERE id = ? AND estado = 'REGISTRADO'""")) {
            ps.setString(1, usuario);
            ps.setString(2, Fechas.aTexto(ahora));
            ps.setLong(3, id);
            exigirUnaFila(ps.executeUpdate(), "El registro ya no existe o está anulado. Actualiza la lista.");
        }
    }

    // ------------------------------------------------------------------ precios

    public Map<CategoriaGanado, BigDecimal> precios(Connection c, long empresaId) throws SQLException {
        Map<CategoriaGanado, BigDecimal> precios = new EnumMap<>(CategoriaGanado.class);
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT categoria, precio FROM precio_ganado WHERE empresa_id = ?")) {
            ps.setLong(1, empresaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    precios.put(CategoriaGanado.valueOf(rs.getString(1)), Montos.desdeEntero(rs.getLong(2)));
                }
            }
        }
        return precios;
    }

    /** Guarda el precio por cabeza; {@code null} lo elimina. */
    public void guardarPrecio(Connection c, long empresaId, CategoriaGanado categoria, BigDecimal precio,
                              String usuario, LocalDateTime ahora) throws SQLException {
        if (precio == null) {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM precio_ganado WHERE empresa_id = ? AND categoria = ?")) {
                ps.setLong(1, empresaId);
                ps.setString(2, categoria.name());
                ps.executeUpdate();
            }
            return;
        }
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO precio_ganado (empresa_id, categoria, precio, actualizado_por, actualizado_en)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (empresa_id, categoria) DO UPDATE
                SET precio = excluded.precio, actualizado_por = excluded.actualizado_por,
                    actualizado_en = excluded.actualizado_en""")) {
            ps.setLong(1, empresaId);
            ps.setString(2, categoria.name());
            ps.setLong(3, Montos.aEntero(precio));
            ps.setString(4, usuario);
            ps.setString(5, Fechas.aTexto(ahora));
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static void montoOpcional(PreparedStatement ps, int indice, BigDecimal valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setLong(indice, Montos.aEntero(valor));
        }
    }

    private static BigDecimal leerMonto(ResultSet rs, String columna) throws SQLException {
        long valor = rs.getLong(columna);
        return rs.wasNull() ? null : Montos.desdeEntero(valor);
    }

    private static long clave(PreparedStatement ps) throws SQLException {
        try (ResultSet claves = ps.getGeneratedKeys()) {
            if (!claves.next()) {
                throw new DataAccessException("No se obtuvo el identificador del registro creado");
            }
            return claves.getLong(1);
        }
    }

    private static List<MovimientoGanado> mapearMovimientos(PreparedStatement ps) throws SQLException {
        List<MovimientoGanado> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String destino = rs.getString("categoria_destino");
                lista.add(new MovimientoGanado(
                        rs.getLong("id"),
                        rs.getLong("empresa_id"),
                        Fechas.fechaDesdeTexto(rs.getString("fecha")),
                        TipoMovimientoGanado.valueOf(rs.getString("tipo")),
                        CategoriaGanado.valueOf(rs.getString("categoria")),
                        rs.getInt("cantidad"),
                        destino == null ? null : CategoriaGanado.valueOf(destino),
                        leerMonto(rs, "monto"),
                        rs.getString("causa"),
                        rs.getString("observacion"),
                        "ANULADO".equals(rs.getString("estado")),
                        rs.getString("motivo_anulacion"),
                        rs.getString("registrado_por"),
                        Fechas.desdeTexto(rs.getString("registrado_en"))));
            }
        }
        return lista;
    }

    private static List<ProduccionGanadera> mapearProduccion(PreparedStatement ps) throws SQLException {
        List<ProduccionGanadera> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int animales = rs.getInt("animales");
                Integer animalesOpcional = rs.wasNull() ? null : animales;
                lista.add(new ProduccionGanadera(
                        rs.getLong("id"),
                        rs.getLong("empresa_id"),
                        Especie.valueOf(rs.getString("especie")),
                        ProductoGanadero.valueOf(rs.getString("producto")),
                        Fechas.fechaDesdeTexto(rs.getString("fecha")),
                        rs.getInt("dias"),
                        Montos.desdeEntero(rs.getLong("cantidad")),
                        animalesOpcional,
                        leerMonto(rs, "precio_unitario"),
                        rs.getString("observacion"),
                        "ANULADO".equals(rs.getString("estado")),
                        rs.getString("registrado_por"),
                        Fechas.desdeTexto(rs.getString("registrado_en"))));
            }
        }
        return lista;
    }

    private static void exigirUnaFila(int filas, String mensaje) {
        if (filas != 1) {
            throw new ValidacionException(mensaje);
        }
    }
}
