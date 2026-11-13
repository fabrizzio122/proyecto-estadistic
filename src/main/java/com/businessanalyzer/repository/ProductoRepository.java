package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosProducto;
import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.RegistroPrecio;
import com.businessanalyzer.model.TipoItem;
import com.businessanalyzer.model.UnidadMedida;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acceso a las tablas {@code producto} e {@code historial_precio}. */
public class ProductoRepository {

    private static final String CONSULTA = """
            SELECT p.id, p.empresa_id, p.categoria_id, c.nombre AS categoria_nombre, p.tipo, p.codigo,
                   p.nombre, p.descripcion, p.unidad, p.costo_unitario, p.precio_venta, p.activo,
                   p.creado_en, p.actualizado_en
            FROM producto p
            LEFT JOIN categoria c ON c.id = p.categoria_id""";

    public List<Producto> listar(Connection c, long empresaId, boolean incluirArchivados) throws SQLException {
        String sql = CONSULTA + " WHERE p.empresa_id = ?"
                + (incluirArchivados ? "" : " AND p.activo = 1")
                + " ORDER BY p.nombre COLLATE NOCASE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            return mapear(ps);
        }
    }

    public Optional<Producto> buscarPorId(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA + " WHERE p.id = ?")) {
            ps.setLong(1, id);
            List<Producto> resultado = mapear(ps);
            return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
        }
    }

    /** Comparación sin distinguir mayúsculas (columna COLLATE NOCASE). */
    public boolean existeCodigo(Connection c, long empresaId, String codigo, long excluirId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM producto WHERE empresa_id = ? AND codigo = ? AND id <> ?")) {
            ps.setLong(1, empresaId);
            ps.setString(2, codigo);
            ps.setLong(3, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Siguiente código automático para la empresa: P-0001, P-0002... (o S- para servicios). */
    public String siguienteCodigo(Connection c, long empresaId, String prefijo) throws SQLException {
        int mayor = 0;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT codigo FROM producto WHERE empresa_id = ? AND codigo LIKE ?")) {
            ps.setLong(1, empresaId);
            ps.setString(2, prefijo + "-%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String sufijo = rs.getString(1).substring(prefijo.length() + 1);
                    if (sufijo.matches("\\d{1,9}")) {
                        mayor = Math.max(mayor, Integer.parseInt(sufijo));
                    }
                }
            }
        }
        return String.format("%s-%04d", prefijo, mayor + 1);
    }

    public long insertar(Connection c, long empresaId, DatosProducto d, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO producto (categoria_id, tipo, codigo, nombre, descripcion, unidad, costo_unitario,
                                      precio_venta, empresa_id, creado_en, actualizado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarDatos(ps, d);
            ps.setLong(9, empresaId);
            ps.setString(10, Fechas.aTexto(ahora));
            ps.setString(11, Fechas.aTexto(ahora));
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new DataAccessException("No se obtuvo el identificador del producto creado");
                }
                return claves.getLong(1);
            }
        }
    }

    public void actualizar(Connection c, long id, DatosProducto d, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE producto
                SET categoria_id = ?, tipo = ?, codigo = ?, nombre = ?, descripcion = ?, unidad = ?,
                    costo_unitario = ?, precio_venta = ?, actualizado_en = ?
                WHERE id = ?""")) {
            asignarDatos(ps, d);
            ps.setString(9, Fechas.aTexto(ahora));
            ps.setLong(10, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    public void cambiarActivo(Connection c, long id, boolean activo, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE producto SET activo = ?, actualizado_en = ? WHERE id = ?")) {
            ps.setInt(1, activo ? 1 : 0);
            ps.setString(2, Fechas.aTexto(ahora));
            ps.setLong(3, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    // ------------------------------------------------------------------ historial de precios

    public void registrarPrecio(Connection c, long productoId, BigDecimal costo, BigDecimal precio,
                                LocalDateTime fecha, String usuario) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO historial_precio (producto_id, costo_unitario, precio_venta, fecha, usuario)
                VALUES (?, ?, ?, ?, ?)""")) {
            ps.setLong(1, productoId);
            ps.setLong(2, Montos.aEntero(costo));
            ps.setLong(3, Montos.aEntero(precio));
            ps.setString(4, Fechas.aTexto(fecha));
            ps.setString(5, usuario);
            ps.executeUpdate();
        }
    }

    /** Del cambio más reciente al más antiguo. */
    public List<RegistroPrecio> historial(Connection c, long productoId) throws SQLException {
        List<RegistroPrecio> registros = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT fecha, costo_unitario, precio_venta, usuario
                FROM historial_precio WHERE producto_id = ? ORDER BY id DESC""")) {
            ps.setLong(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    registros.add(new RegistroPrecio(
                            Fechas.desdeTexto(rs.getString("fecha")),
                            Montos.desdeEntero(rs.getLong("costo_unitario")),
                            Montos.desdeEntero(rs.getLong("precio_venta")),
                            rs.getString("usuario")));
                }
            }
        }
        return registros;
    }

    // ------------------------------------------------------------------ mapeo

    private static void asignarDatos(PreparedStatement ps, DatosProducto d) throws SQLException {
        if (d.categoriaId() == null) {
            ps.setNull(1, Types.INTEGER);
        } else {
            ps.setLong(1, d.categoriaId());
        }
        ps.setString(2, d.tipo().name());
        ps.setString(3, d.codigo());
        ps.setString(4, d.nombre());
        ps.setString(5, d.descripcion());
        ps.setString(6, d.unidad().name());
        ps.setLong(7, Montos.aEntero(d.costoUnitario()));
        ps.setLong(8, Montos.aEntero(d.precioVenta()));
    }

    private static List<Producto> mapear(PreparedStatement ps) throws SQLException {
        List<Producto> productos = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long categoriaId = rs.getLong("categoria_id");
                boolean sinCategoria = rs.wasNull();
                productos.add(new Producto(
                        rs.getLong("id"),
                        rs.getLong("empresa_id"),
                        sinCategoria ? null : categoriaId,
                        rs.getString("categoria_nombre"),
                        TipoItem.valueOf(rs.getString("tipo")),
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        UnidadMedida.valueOf(rs.getString("unidad")),
                        Montos.desdeEntero(rs.getLong("costo_unitario")),
                        Montos.desdeEntero(rs.getLong("precio_venta")),
                        rs.getInt("activo") == 1,
                        Fechas.desdeTexto(rs.getString("creado_en")),
                        Fechas.desdeTexto(rs.getString("actualizado_en"))));
            }
        }
        return productos;
    }

    private static void exigirUnaFila(int filas) {
        if (filas != 1) {
            throw new ValidacionException("El producto ya no existe. Actualiza la lista.");
        }
    }
}
