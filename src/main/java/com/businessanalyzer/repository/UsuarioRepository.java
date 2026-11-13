package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosUsuario;
import com.businessanalyzer.model.Rol;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.util.Fechas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Acceso a la tabla {@code usuario}. Todas las consultas son parametrizadas. */
public class UsuarioRepository {

    /** Usuario junto con su hash; solo lo usan los servicios para verificar contraseñas. */
    public record Credenciales(Usuario usuario, String passwordHash) {
    }

    private static final String COLUMNAS = """
            id, nombre_usuario, nombre_completo, email, rol_id, activo, debe_cambiar_password,
            intentos_fallidos, bloqueado_hasta, ultimo_acceso, creado_en""";

    private final RolRepository roles;

    public UsuarioRepository(RolRepository roles) {
        this.roles = roles;
    }

    public int contar(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM usuario");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int contarAdministradoresActivos(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT COUNT(*) FROM usuario u
                JOIN rol r ON r.id = u.rol_id
                WHERE u.activo = 1 AND r.codigo = ?""")) {
            ps.setString(1, Rol.ADMINISTRADOR);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<Usuario> listar(Connection c) throws SQLException {
        Map<Long, Rol> mapaRoles = mapaRoles(c);
        List<Usuario> usuarios = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + COLUMNAS + " FROM usuario ORDER BY nombre_usuario");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                usuarios.add(mapear(rs, mapaRoles));
            }
        }
        return usuarios;
    }

    public Optional<Usuario> buscarPorId(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNAS + " FROM usuario WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs, mapaRoles(c))) : Optional.empty();
            }
        }
    }

    /** Búsqueda sin distinguir mayúsculas y minúsculas (columna COLLATE NOCASE). */
    public Optional<Credenciales> buscarCredenciales(Connection c, String nombreUsuario) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + COLUMNAS + ", password_hash FROM usuario WHERE nombre_usuario = ?")) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(new Credenciales(mapear(rs, mapaRoles(c)), rs.getString("password_hash")))
                        : Optional.empty();
            }
        }
    }

    public Optional<String> buscarHash(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM usuario WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        }
    }

    public boolean existeNombreUsuario(Connection c, String nombreUsuario, long excluirId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM usuario WHERE nombre_usuario = ? AND id <> ?")) {
            ps.setString(1, nombreUsuario);
            ps.setLong(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insertar(Connection c, DatosUsuario datos, String passwordHash, boolean debeCambiarPassword,
                         LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO usuario (nombre_usuario, nombre_completo, email, password_hash, rol_id,
                                     debe_cambiar_password, creado_en, actualizado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, datos.nombreUsuario());
            ps.setString(2, datos.nombreCompleto());
            ps.setString(3, datos.email());
            ps.setString(4, passwordHash);
            ps.setLong(5, datos.rolId());
            ps.setInt(6, debeCambiarPassword ? 1 : 0);
            ps.setString(7, Fechas.aTexto(ahora));
            ps.setString(8, Fechas.aTexto(ahora));
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new DataAccessException("No se obtuvo el identificador del usuario creado");
                }
                return claves.getLong(1);
            }
        }
    }

    public void actualizarDatos(Connection c, long id, DatosUsuario datos, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE usuario
                SET nombre_usuario = ?, nombre_completo = ?, email = ?, rol_id = ?, actualizado_en = ?
                WHERE id = ?""")) {
            ps.setString(1, datos.nombreUsuario());
            ps.setString(2, datos.nombreCompleto());
            ps.setString(3, datos.email());
            ps.setLong(4, datos.rolId());
            ps.setString(5, Fechas.aTexto(ahora));
            ps.setLong(6, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    /** Cambia la contraseña y, de paso, reinicia los intentos fallidos y el bloqueo. */
    public void actualizarPassword(Connection c, long id, String passwordHash, boolean debeCambiarPassword,
                                   LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE usuario
                SET password_hash = ?, debe_cambiar_password = ?, intentos_fallidos = 0,
                    bloqueado_hasta = NULL, actualizado_en = ?
                WHERE id = ?""")) {
            ps.setString(1, passwordHash);
            ps.setInt(2, debeCambiarPassword ? 1 : 0);
            ps.setString(3, Fechas.aTexto(ahora));
            ps.setLong(4, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    /** Reemplaza el hash sin tocar nada más (actualización transparente del algoritmo). */
    public void reemplazarHash(Connection c, long id, String passwordHash) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE usuario SET password_hash = ? WHERE id = ?")) {
            ps.setString(1, passwordHash);
            ps.setLong(2, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    public void cambiarActivo(Connection c, long id, boolean activo, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE usuario SET activo = ?, actualizado_en = ? WHERE id = ?")) {
            ps.setInt(1, activo ? 1 : 0);
            ps.setString(2, Fechas.aTexto(ahora));
            ps.setLong(3, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    public void registrarFallo(Connection c, long id, int intentosFallidos, LocalDateTime bloqueadoHasta)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE usuario SET intentos_fallidos = ?, bloqueado_hasta = ? WHERE id = ?")) {
            ps.setInt(1, intentosFallidos);
            ps.setString(2, Fechas.aTexto(bloqueadoHasta));
            ps.setLong(3, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    public void registrarAccesoExitoso(Connection c, long id, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE usuario
                SET intentos_fallidos = 0, bloqueado_hasta = NULL, ultimo_acceso = ?
                WHERE id = ?""")) {
            ps.setString(1, Fechas.aTexto(ahora));
            ps.setLong(2, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    public void desbloquear(Connection c, long id, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE usuario
                SET intentos_fallidos = 0, bloqueado_hasta = NULL, actualizado_en = ?
                WHERE id = ?""")) {
            ps.setString(1, Fechas.aTexto(ahora));
            ps.setLong(2, id);
            exigirUnaFila(ps.executeUpdate());
        }
    }

    private Map<Long, Rol> mapaRoles(Connection c) throws SQLException {
        return roles.listar(c).stream().collect(Collectors.toMap(Rol::id, Function.identity()));
    }

    private static Usuario mapear(ResultSet rs, Map<Long, Rol> mapaRoles) throws SQLException {
        long rolId = rs.getLong("rol_id");
        Rol rol = mapaRoles.get(rolId);
        if (rol == null) {
            throw new DataAccessException("El usuario " + rs.getString("nombre_usuario")
                    + " tiene un rol inexistente (" + rolId + ")");
        }
        return new Usuario(
                rs.getLong("id"),
                rs.getString("nombre_usuario"),
                rs.getString("nombre_completo"),
                rs.getString("email"),
                rol,
                rs.getInt("activo") == 1,
                rs.getInt("debe_cambiar_password") == 1,
                rs.getInt("intentos_fallidos"),
                Fechas.desdeTexto(rs.getString("bloqueado_hasta")),
                Fechas.desdeTexto(rs.getString("ultimo_acceso")),
                Fechas.desdeTexto(rs.getString("creado_en")));
    }

    private static void exigirUnaFila(int filasAfectadas) {
        if (filasAfectadas != 1) {
            throw new ValidacionException("El usuario ya no existe. Actualiza la lista e inténtalo de nuevo.");
        }
    }
}
