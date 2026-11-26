package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosProductoFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.RegistroPrecioFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;

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

public class ProductoRepositoryFGEM {

    private static final String CONSULTA_FGEM = """
            SELECT p.id, p.empresa_id, p.categoria_id, c.nombre AS categoria_nombre, p.tipo, p.codigo,
                   p.nombre, p.descripcion, p.unidad, p.costo_unitario, p.precio_venta, p.activo, p.perecible,
                   p.creado_en, p.actualizado_en,
                   (SELECT COALESCE(SUM(CASE o.tipo WHEN 'COMPRA' THEN ol.cantidad ELSE -ol.cantidad END), 0)
                    FROM operacion_linea ol
                    JOIN operacion o ON o.id = ol.operacion_id
                    WHERE ol.producto_id = p.id AND o.estado = 'REGISTRADA') AS stock
            FROM producto p
            LEFT JOIN categoria c ON c.id = p.categoria_id""";

    public List<ProductoFGEM> listarFGEM(Connection cFGEM, long empresaIdFGEM, boolean incluirArchivadosFGEM) throws SQLException {
        String sqlFGEM = CONSULTA_FGEM + " WHERE p.empresa_id = ?"
                + (incluirArchivadosFGEM ? "" : " AND p.activo = 1")
                + " ORDER BY p.nombre COLLATE NOCASE";
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(sqlFGEM)) {
            psFGEM.setLong(1, empresaIdFGEM);
            return mapearFGEM(psFGEM);
        }
    }

    public Optional<ProductoFGEM> buscarPorIdFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(CONSULTA_FGEM + " WHERE p.id = ?")) {
            psFGEM.setLong(1, idFGEM);
            List<ProductoFGEM> resultadoFGEM = mapearFGEM(psFGEM);
            return resultadoFGEM.isEmpty() ? Optional.empty() : Optional.of(resultadoFGEM.get(0));
        }
    }

    public boolean existeCodigoFGEM(Connection cFGEM, long empresaIdFGEM, String codigoFGEM, long excluirIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT 1 FROM producto WHERE empresa_id = ? AND codigo = ? AND id <> ?")) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, codigoFGEM);
            psFGEM.setLong(3, excluirIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next();
            }
        }
    }

    public String siguienteCodigoFGEM(Connection cFGEM, long empresaIdFGEM, String prefijoFGEM) throws SQLException {
        int mayorFGEM = 0;
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT codigo FROM producto WHERE empresa_id = ? AND codigo LIKE ?")) {
            psFGEM.setLong(1, empresaIdFGEM);
            psFGEM.setString(2, prefijoFGEM + "-%");
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    String sufijoFGEM = rsFGEM.getString(1).substring(prefijoFGEM.length() + 1);
                    if (sufijoFGEM.matches("\\d{1,9}")) {
                        mayorFGEM = Math.max(mayorFGEM, Integer.parseInt(sufijoFGEM));
                    }
                }
            }
        }
        return String.format("%s-%04d", prefijoFGEM, mayorFGEM + 1);
    }

    public long insertarFGEM(Connection cFGEM, long empresaIdFGEM, DatosProductoFGEM dFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO producto (categoria_id, tipo, codigo, nombre, descripcion, unidad, costo_unitario,
                                      precio_venta, perecible, empresa_id, creado_en, actualizado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarDatosFGEM(psFGEM, dFGEM);
            psFGEM.setLong(10, empresaIdFGEM);
            psFGEM.setString(11, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setString(12, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            try (ResultSet clavesFGEM = psFGEM.getGeneratedKeys()) {
                if (!clavesFGEM.next()) {
                    throw new DataAccessExceptionFGEM("No se obtuvo el identificador del producto creado");
                }
                return clavesFGEM.getLong(1);
            }
        }
    }

    public void actualizarFGEM(Connection cFGEM, long idFGEM, DatosProductoFGEM dFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE producto
                SET categoria_id = ?, tipo = ?, codigo = ?, nombre = ?, descripcion = ?, unidad = ?,
                    costo_unitario = ?, precio_venta = ?, perecible = ?, actualizado_en = ?
                WHERE id = ?""")) {
            asignarDatosFGEM(psFGEM, dFGEM);
            psFGEM.setString(10, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(11, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    public void cambiarActivoFGEM(Connection cFGEM, long idFGEM, boolean activoFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "UPDATE producto SET activo = ?, actualizado_en = ? WHERE id = ?")) {
            psFGEM.setInt(1, activoFGEM ? 1 : 0);
            psFGEM.setString(2, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(3, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    public void actualizarCostoFGEM(Connection cFGEM, long idFGEM, BigDecimal costoFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "UPDATE producto SET costo_unitario = ?, actualizado_en = ? WHERE id = ?")) {
            psFGEM.setLong(1, MontosFGEM.aEnteroFGEM(costoFGEM));
            psFGEM.setString(2, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(3, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    public void registrarPrecioFGEM(Connection cFGEM, long productoIdFGEM, BigDecimal costoFGEM, BigDecimal precioFGEM,
                                LocalDateTime fechaFGEM, String usuarioFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO historial_precio (producto_id, costo_unitario, precio_venta, fecha, usuario)
                VALUES (?, ?, ?, ?, ?)""")) {
            psFGEM.setLong(1, productoIdFGEM);
            psFGEM.setLong(2, MontosFGEM.aEnteroFGEM(costoFGEM));
            psFGEM.setLong(3, MontosFGEM.aEnteroFGEM(precioFGEM));
            psFGEM.setString(4, FechasFGEM.aTextoFGEM(fechaFGEM));
            psFGEM.setString(5, usuarioFGEM);
            psFGEM.executeUpdate();
        }
    }

    public List<RegistroPrecioFGEM> historialFGEM(Connection cFGEM, long productoIdFGEM) throws SQLException {
        List<RegistroPrecioFGEM> registrosFGEM = new ArrayList<>();
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                SELECT fecha, costo_unitario, precio_venta, usuario
                FROM historial_precio WHERE producto_id = ? ORDER BY id DESC""")) {
            psFGEM.setLong(1, productoIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                while (rsFGEM.next()) {
                    registrosFGEM.add(new RegistroPrecioFGEM(
                            FechasFGEM.desdeTextoFGEM(rsFGEM.getString("fecha")),
                            MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("costo_unitario")),
                            MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("precio_venta")),
                            rsFGEM.getString("usuario")));
                }
            }
        }
        return registrosFGEM;
    }

    private static void asignarDatosFGEM(PreparedStatement psFGEM, DatosProductoFGEM dFGEM) throws SQLException {
        if (dFGEM.categoriaIdFGEM() == null) {
            psFGEM.setNull(1, Types.INTEGER);
        } else {
            psFGEM.setLong(1, dFGEM.categoriaIdFGEM());
        }
        psFGEM.setString(2, dFGEM.tipoFGEM().name());
        psFGEM.setString(3, dFGEM.codigoFGEM());
        psFGEM.setString(4, dFGEM.nombreFGEM());
        psFGEM.setString(5, dFGEM.descripcionFGEM());
        psFGEM.setString(6, dFGEM.unidadFGEM().name());
        psFGEM.setLong(7, MontosFGEM.aEnteroFGEM(dFGEM.costoUnitarioFGEM()));
        psFGEM.setLong(8, MontosFGEM.aEnteroFGEM(dFGEM.precioVentaFGEM()));
        psFGEM.setInt(9, dFGEM.perecibleFGEM() ? 1 : 0);
    }

    private static List<ProductoFGEM> mapearFGEM(PreparedStatement psFGEM) throws SQLException {
        List<ProductoFGEM> productosFGEM = new ArrayList<>();
        try (ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                long categoriaIdFGEM = rsFGEM.getLong("categoria_id");
                boolean sinCategoriaFGEM = rsFGEM.wasNull();
                TipoItemFGEM tipoFGEM = TipoItemFGEM.valueOf(rsFGEM.getString("tipo"));
                productosFGEM.add(new ProductoFGEM(
                        rsFGEM.getLong("id"),
                        rsFGEM.getLong("empresa_id"),
                        sinCategoriaFGEM ? null : categoriaIdFGEM,
                        rsFGEM.getString("categoria_nombre"),
                        tipoFGEM,
                        rsFGEM.getString("codigo"),
                        rsFGEM.getString("nombre"),
                        rsFGEM.getString("descripcion"),
                        UnidadMedidaFGEM.valueOf(rsFGEM.getString("unidad")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("costo_unitario")),
                        MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("precio_venta")),
                        tipoFGEM == TipoItemFGEM.PRODUCTO ? MontosFGEM.desdeEnteroFGEM(rsFGEM.getLong("stock")) : null,
                        rsFGEM.getInt("activo") == 1,
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("creado_en")),
                        FechasFGEM.desdeTextoFGEM(rsFGEM.getString("actualizado_en")),
                        rsFGEM.getInt("perecible") == 1));
            }
        }
        return productosFGEM;
    }

    private static void exigirUnaFilaFGEM(int filasFGEM) {
        if (filasFGEM != 1) {
            throw new ValidacionExceptionFGEM("El producto ya no existe. Actualiza la lista.");
        }
    }
}
