package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosEmpresa;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.TamanoEmpresa;
import com.businessanalyzer.util.Fechas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Acceso a las tablas {@code empresa} y {@code usuario_empresa}. */
public class EmpresaRepository {

    private static final String COLUMNAS = """
            e.id, e.razon_social, e.nombre_comercial, e.identificacion_fiscal, e.sector, e.tamano,
            e.moneda, e.mes_inicio_ejercicio, e.fecha_inicio_operaciones, e.direccion, e.telefono,
            e.email, e.activa, e.creado_en, e.actualizado_en""";

    private static final String ORDEN = " ORDER BY COALESCE(e.nombre_comercial, e.razon_social) COLLATE NOCASE";

    /** Todas las empresas (para administradores). */
    public List<Empresa> listar(Connection c, boolean incluirArchivadas) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM empresa e"
                + (incluirArchivadas ? "" : " WHERE e.activa = 1") + ORDEN;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            return mapearLista(ps);
        }
    }

    /** Empresas asignadas a un usuario. */
    public List<Empresa> listarAsignadas(Connection c, long usuarioId, boolean incluirArchivadas) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM empresa e"
                + " JOIN usuario_empresa ue ON ue.empresa_id = e.id"
                + " WHERE ue.usuario_id = ?"
                + (incluirArchivadas ? "" : " AND e.activa = 1") + ORDEN;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            return mapearLista(ps);
        }
    }

    public Optional<Empresa> buscarPorId(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNAS + " FROM empresa e WHERE e.id = ?")) {
            ps.setLong(1, id);
            List<Empresa> resultado = mapearLista(ps);
            return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
        }
    }

    /** Comparación sin distinguir mayúsculas (columna COLLATE NOCASE). */
    public boolean existeIdentificacion(Connection c, String identificacion, long excluirId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM empresa WHERE identificacion_fiscal = ? AND id <> ?")) {
            ps.setString(1, identificacion);
            ps.setLong(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insertar(Connection c, DatosEmpresa d, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO empresa (razon_social, nombre_comercial, identificacion_fiscal, sector, tamano, moneda,
                                     mes_inicio_ejercicio, fecha_inicio_operaciones, direccion, telefono, email,
                                     creado_en, actualizado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            asignarDatos(ps, d);
            ps.setString(12, Fechas.aTexto(ahora));
            ps.setString(13, Fechas.aTexto(ahora));
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new DataAccessException("No se obtuvo el identificador de la empresa creada");
                }
                return claves.getLong(1);
            }
        }
    }

    public void actualizar(Connection c, long id, DatosEmpresa d, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE empresa
                SET razon_social = ?, nombre_comercial = ?, identificacion_fiscal = ?, sector = ?, tamano = ?,
                    moneda = ?, mes_inicio_ejercicio = ?, fecha_inicio_operaciones = ?, direccion = ?,
                    telefono = ?, email = ?, actualizado_en = ?
                WHERE id = ?""")) {
            asignarDatos(ps, d);
            ps.setString(12, Fechas.aTexto(ahora));
            ps.setLong(13, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    public void cambiarActiva(Connection c, long id, boolean activa, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE empresa SET activa = ?, actualizado_en = ? WHERE id = ?")) {
            ps.setInt(1, activa ? 1 : 0);
            ps.setString(2, Fechas.aTexto(ahora));
            ps.setLong(3, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    // ------------------------------------------------------------------ acceso de usuarios

    public boolean estaAsignada(Connection c, long usuarioId, long empresaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM usuario_empresa WHERE usuario_id = ? AND empresa_id = ?")) {
            ps.setLong(1, usuarioId);
            ps.setLong(2, empresaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Set<Long> usuariosAsignados(Connection c, long empresaId) throws SQLException {
        Set<Long> ids = new LinkedHashSet<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT usuario_id FROM usuario_empresa WHERE empresa_id = ?")) {
            ps.setLong(1, empresaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong(1));
                }
            }
        }
        return ids;
    }

    public void asignar(Connection c, long usuarioId, long empresaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO usuario_empresa (usuario_id, empresa_id) VALUES (?, ?)")) {
            ps.setLong(1, usuarioId);
            ps.setLong(2, empresaId);
            ps.executeUpdate();
        }
    }

    /** Deja exactamente esos usuarios asignados a la empresa. Usar dentro de una transacción. */
    public void reemplazarAsignaciones(Connection c, long empresaId, Set<Long> usuarioIds) throws SQLException {
        try (PreparedStatement borrar = c.prepareStatement("DELETE FROM usuario_empresa WHERE empresa_id = ?")) {
            borrar.setLong(1, empresaId);
            borrar.executeUpdate();
        }
        for (long usuarioId : usuarioIds) {
            asignar(c, usuarioId, empresaId);
        }
    }

    // ------------------------------------------------------------------ mapeo

    private static void asignarDatos(PreparedStatement ps, DatosEmpresa d) throws SQLException {
        ps.setString(1, d.razonSocial());
        ps.setString(2, d.nombreComercial());
        ps.setString(3, d.identificacionFiscal());
        ps.setString(4, d.sector().name());
        ps.setString(5, d.tamano().name());
        ps.setString(6, d.moneda().name());
        ps.setInt(7, d.mesInicioEjercicio());
        ps.setString(8, Fechas.fechaATexto(d.fechaInicioOperaciones()));
        ps.setString(9, d.direccion());
        ps.setString(10, d.telefono());
        ps.setString(11, d.email());
    }

    private static List<Empresa> mapearLista(PreparedStatement ps) throws SQLException {
        List<Empresa> empresas = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                empresas.add(new Empresa(
                        rs.getLong("id"),
                        rs.getString("razon_social"),
                        rs.getString("nombre_comercial"),
                        rs.getString("identificacion_fiscal"),
                        Sector.valueOf(rs.getString("sector")),
                        TamanoEmpresa.valueOf(rs.getString("tamano")),
                        Moneda.valueOf(rs.getString("moneda")),
                        rs.getInt("mes_inicio_ejercicio"),
                        Fechas.fechaDesdeTexto(rs.getString("fecha_inicio_operaciones")),
                        rs.getString("direccion"),
                        rs.getString("telefono"),
                        rs.getString("email"),
                        rs.getInt("activa") == 1,
                        Fechas.desdeTexto(rs.getString("creado_en")),
                        Fechas.desdeTexto(rs.getString("actualizado_en"))));
            }
        }
        return empresas;
    }

    private static void exigirUnaFila(int filasAfectadas) {
        if (filasAfectadas != 1) {
            throw new ValidacionException("La empresa ya no existe. Actualiza la lista e inténtalo de nuevo.");
        }
    }
}
