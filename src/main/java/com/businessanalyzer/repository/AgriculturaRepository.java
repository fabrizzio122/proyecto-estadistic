package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Campania;
import com.businessanalyzer.model.Cosecha;
import com.businessanalyzer.model.CostoCampania;
import com.businessanalyzer.model.Cultivo;
import com.businessanalyzer.model.DatosCampania;
import com.businessanalyzer.model.DatosParcela;
import com.businessanalyzer.model.DestinoCosecha;
import com.businessanalyzer.model.EstadoCampania;
import com.businessanalyzer.model.Parcela;
import com.businessanalyzer.model.RubroCosto;
import com.businessanalyzer.model.Tenencia;
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

/** Acceso a las tablas {@code parcela}, {@code campania}, {@code costo_campania} y {@code cosecha}. */
public class AgriculturaRepository {

    private static final String CONSULTA_PARCELAS = """
            SELECT id, empresa_id, nombre, area, tenencia, riego, ubicacion, activa FROM parcela""";

    private static final String CONSULTA_CAMPANIAS = """
            SELECT c.id, c.empresa_id, c.parcela_id, p.nombre AS parcela_nombre, c.cultivo, c.variedad, c.fecha_siembra,
                   c.area, c.rendimiento_referencia, c.precio_chacra, c.estado, c.fecha_cierre, c.motivo, c.observacion,
                   c.registrado_por
            FROM campania c JOIN parcela p ON p.id = c.parcela_id""";

    private static final String CONSULTA_COSTOS = """
            SELECT k.id, k.campania_id, k.fecha, k.rubro, k.descripcion, k.monto, k.estado, k.registrado_por
            FROM costo_campania k JOIN campania c ON c.id = k.campania_id""";

    private static final String CONSULTA_COSECHAS = """
            SELECT h.id, h.campania_id, h.fecha, h.cantidad, h.destino, h.precio, h.observacion, h.estado, h.registrado_por
            FROM cosecha h JOIN campania c ON c.id = h.campania_id""";

    // ------------------------------------------------------------------ parcelas

    public List<Parcela> listarParcelas(Connection c, long empresaId, boolean incluirInactivas) throws SQLException {
        String sql = CONSULTA_PARCELAS + " WHERE empresa_id = ?" + (incluirInactivas ? "" : " AND activa = 1")
                + " ORDER BY nombre COLLATE NOCASE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            return mapearParcelas(ps);
        }
    }

    public Optional<Parcela> buscarParcela(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA_PARCELAS + " WHERE id = ?")) {
            ps.setLong(1, id);
            List<Parcela> r = mapearParcelas(ps);
            return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
        }
    }

    public boolean existeParcela(Connection c, long empresaId, String nombre, long excluirId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM parcela WHERE empresa_id = ? AND nombre = ? COLLATE NOCASE AND id <> ?")) {
            ps.setLong(1, empresaId);
            ps.setString(2, nombre);
            ps.setLong(3, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insertarParcela(Connection c, long empresaId, DatosParcela d, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO parcela (nombre, area, tenencia, riego, ubicacion, empresa_id, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarParcela(ps, d);
            ps.setLong(6, empresaId);
            ps.setString(7, usuario);
            ps.setString(8, Fechas.aTexto(ahora));
            ps.executeUpdate();
            return clave(ps);
        }
    }

    public void actualizarParcela(Connection c, long id, DatosParcela d, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE parcela SET nombre = ?, area = ?, tenencia = ?, riego = ?, ubicacion = ?,
                                   actualizado_por = ?, actualizado_en = ?
                WHERE id = ?""")) {
            asignarParcela(ps, d);
            ps.setString(6, usuario);
            ps.setString(7, Fechas.aTexto(ahora));
            ps.setLong(8, id);
            exigirUnaFila(ps.executeUpdate(), "La parcela ya no existe.");
        }
    }

    public void cambiarParcelaActiva(Connection c, long id, boolean activa) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE parcela SET activa = ? WHERE id = ?")) {
            ps.setInt(1, activa ? 1 : 0);
            ps.setLong(2, id);
            exigirUnaFila(ps.executeUpdate(), "La parcela ya no existe.");
        }
    }

    // ------------------------------------------------------------------ campañas

    public List<Campania> listarCampanias(Connection c, long empresaId, boolean incluirAnuladas) throws SQLException {
        String sql = CONSULTA_CAMPANIAS + " WHERE c.empresa_id = ?"
                + (incluirAnuladas ? "" : " AND c.estado <> 'ANULADA'")
                + " ORDER BY c.fecha_siembra DESC, c.id DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            return mapearCampanias(ps);
        }
    }

    public Optional<Campania> buscarCampania(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CONSULTA_CAMPANIAS + " WHERE c.id = ?")) {
            ps.setLong(1, id);
            List<Campania> r = mapearCampanias(ps);
            return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
        }
    }

    public long insertarCampania(Connection c, long empresaId, DatosCampania d, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO campania (parcela_id, cultivo, variedad, fecha_siembra, area, rendimiento_referencia,
                                      precio_chacra, observacion, empresa_id, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarCampania(ps, d);
            ps.setLong(9, empresaId);
            ps.setString(10, usuario);
            ps.setString(11, Fechas.aTexto(ahora));
            ps.executeUpdate();
            return clave(ps);
        }
    }

    public void actualizarCampania(Connection c, long id, DatosCampania d, String usuario, LocalDateTime ahora)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE campania SET parcela_id = ?, cultivo = ?, variedad = ?, fecha_siembra = ?, area = ?,
                                    rendimiento_referencia = ?, precio_chacra = ?, observacion = ?,
                                    actualizado_por = ?, actualizado_en = ?
                WHERE id = ? AND estado <> 'ANULADA'""")) {
            asignarCampania(ps, d);
            ps.setString(9, usuario);
            ps.setString(10, Fechas.aTexto(ahora));
            ps.setLong(11, id);
            exigirUnaFila(ps.executeUpdate(), "La campaña ya no existe o está anulada.");
        }
    }

    public void cambiarEstado(Connection c, long id, EstadoCampania estado, LocalDate fechaCierre, String motivo,
                              String usuario, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE campania SET estado = ?, fecha_cierre = ?, motivo = ?, actualizado_por = ?, actualizado_en = ?
                WHERE id = ?""")) {
            ps.setString(1, estado.name());
            ps.setString(2, Fechas.fechaATexto(fechaCierre));
            ps.setString(3, motivo);
            ps.setString(4, usuario);
            ps.setString(5, Fechas.aTexto(ahora));
            ps.setLong(6, id);
            exigirUnaFila(ps.executeUpdate(), "La campaña ya no existe.");
        }
    }

    // ------------------------------------------------------------------ costos y cosechas

    /** Costos vigentes o todos de las campañas de la empresa (o de una sola campaña si {@code campaniaId} no es nulo). */
    public List<CostoCampania> listarCostos(Connection c, long empresaId, Long campaniaId, boolean incluirAnulados)
            throws SQLException {
        String sql = CONSULTA_COSTOS + " WHERE c.empresa_id = ?" + (campaniaId == null ? "" : " AND k.campania_id = ?")
                + (incluirAnulados ? "" : " AND k.estado = 'REGISTRADO'") + " ORDER BY k.fecha DESC, k.id DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            if (campaniaId != null) {
                ps.setLong(2, campaniaId);
            }
            List<CostoCampania> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new CostoCampania(rs.getLong("id"), rs.getLong("campania_id"),
                            Fechas.fechaDesdeTexto(rs.getString("fecha")), RubroCosto.valueOf(rs.getString("rubro")),
                            rs.getString("descripcion"), Montos.desdeEntero(rs.getLong("monto")),
                            "ANULADO".equals(rs.getString("estado")), rs.getString("registrado_por")));
                }
            }
            return lista;
        }
    }

    public List<Cosecha> listarCosechas(Connection c, long empresaId, Long campaniaId, boolean incluirAnuladas)
            throws SQLException {
        String sql = CONSULTA_COSECHAS + " WHERE c.empresa_id = ?" + (campaniaId == null ? "" : " AND h.campania_id = ?")
                + (incluirAnuladas ? "" : " AND h.estado = 'REGISTRADO'") + " ORDER BY h.fecha DESC, h.id DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            if (campaniaId != null) {
                ps.setLong(2, campaniaId);
            }
            List<Cosecha> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Cosecha(rs.getLong("id"), rs.getLong("campania_id"),
                            Fechas.fechaDesdeTexto(rs.getString("fecha")), Montos.desdeEntero(rs.getLong("cantidad")),
                            DestinoCosecha.valueOf(rs.getString("destino")), leerMonto(rs, "precio"),
                            rs.getString("observacion"), "ANULADO".equals(rs.getString("estado")),
                            rs.getString("registrado_por")));
                }
            }
            return lista;
        }
    }

    public long insertarCosto(Connection c, long campaniaId, LocalDate fecha, RubroCosto rubro, String descripcion,
                              BigDecimal monto, String usuario, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO costo_campania (campania_id, fecha, rubro, descripcion, monto, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, campaniaId);
            ps.setString(2, Fechas.fechaATexto(fecha));
            ps.setString(3, rubro.name());
            ps.setString(4, descripcion);
            ps.setLong(5, Montos.aEntero(monto));
            ps.setString(6, usuario);
            ps.setString(7, Fechas.aTexto(ahora));
            ps.executeUpdate();
            return clave(ps);
        }
    }

    public long insertarCosecha(Connection c, long campaniaId, LocalDate fecha, BigDecimal cantidad,
                                DestinoCosecha destino, BigDecimal precio, String observacion, String usuario,
                                LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO cosecha (campania_id, fecha, cantidad, destino, precio, observacion, registrado_por, registrado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, campaniaId);
            ps.setString(2, Fechas.fechaATexto(fecha));
            ps.setLong(3, Montos.aEntero(cantidad));
            ps.setString(4, destino.name());
            montoOpcional(ps, 5, precio);
            ps.setString(6, observacion);
            ps.setString(7, usuario);
            ps.setString(8, Fechas.aTexto(ahora));
            ps.executeUpdate();
            return clave(ps);
        }
    }

    /** Anula un costo o una cosecha; devuelve la campaña a la que pertenecía o vacío si no existe o ya estaba anulado. */
    public Optional<Long> anularRegistro(Connection c, String tabla, long id, long empresaId, String usuario,
                                         LocalDateTime ahora) throws SQLException {
        if (!tabla.equals("costo_campania") && !tabla.equals("cosecha")) {
            throw new IllegalArgumentException(tabla);
        }
        long campaniaId;
        try (PreparedStatement ps = c.prepareStatement("SELECT t.campania_id FROM " + tabla + " t "
                + "JOIN campania c ON c.id = t.campania_id WHERE t.id = ? AND c.empresa_id = ? AND t.estado = 'REGISTRADO'")) {
            ps.setLong(1, id);
            ps.setLong(2, empresaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                campaniaId = rs.getLong(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement("UPDATE " + tabla
                + " SET estado = 'ANULADO', anulado_por = ?, anulado_en = ? WHERE id = ?")) {
            ps.setString(1, usuario);
            ps.setString(2, Fechas.aTexto(ahora));
            ps.setLong(3, id);
            ps.executeUpdate();
        }
        return Optional.of(campaniaId);
    }

    // ------------------------------------------------------------------ utilidades

    private static void asignarParcela(PreparedStatement ps, DatosParcela d) throws SQLException {
        ps.setString(1, d.nombre());
        ps.setLong(2, Montos.aEntero(d.area()));
        ps.setString(3, d.tenencia().name());
        ps.setInt(4, d.riego() ? 1 : 0);
        ps.setString(5, d.ubicacion());
    }

    private static void asignarCampania(PreparedStatement ps, DatosCampania d) throws SQLException {
        ps.setLong(1, d.parcelaId());
        ps.setString(2, d.cultivo().name());
        ps.setString(3, d.variedad());
        ps.setString(4, Fechas.fechaATexto(d.fechaSiembra()));
        ps.setLong(5, Montos.aEntero(d.area()));
        montoOpcional(ps, 6, d.rendimientoReferencia());
        montoOpcional(ps, 7, d.precioChacra());
        ps.setString(8, d.observacion());
    }

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

    private static List<Parcela> mapearParcelas(PreparedStatement ps) throws SQLException {
        List<Parcela> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Parcela(rs.getLong("id"), rs.getLong("empresa_id"), rs.getString("nombre"),
                        Montos.desdeEntero(rs.getLong("area")), Tenencia.valueOf(rs.getString("tenencia")),
                        rs.getInt("riego") == 1, rs.getString("ubicacion"), rs.getInt("activa") == 1));
            }
        }
        return lista;
    }

    private static List<Campania> mapearCampanias(PreparedStatement ps) throws SQLException {
        List<Campania> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Campania(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("parcela_id"),
                        rs.getString("parcela_nombre"), Cultivo.valueOf(rs.getString("cultivo")),
                        rs.getString("variedad"), Fechas.fechaDesdeTexto(rs.getString("fecha_siembra")),
                        Montos.desdeEntero(rs.getLong("area")), leerMonto(rs, "rendimiento_referencia"),
                        leerMonto(rs, "precio_chacra"), EstadoCampania.valueOf(rs.getString("estado")),
                        Fechas.fechaDesdeTexto(rs.getString("fecha_cierre")), rs.getString("motivo"),
                        rs.getString("observacion"), rs.getString("registrado_por")));
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
