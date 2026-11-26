package com.businessanalyzer.repository;

import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosUsuarioFGEM;
import com.businessanalyzer.model.RolFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.util.FechasFGEM;

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

public class UsuarioRepositoryFGEM {

    public record CredencialesFGEM(UsuarioFGEM usuarioFGEM, String passwordHashFGEM) {
    }

    private static final String COLUMNAS_FGEM = """
            id, nombre_usuario, nombre_completo, email, rol_id, activo, debe_cambiar_password,
            intentos_fallidos, bloqueado_hasta, ultimo_acceso, creado_en""";

    private final RolRepositoryFGEM rolesFGEM;

    public UsuarioRepositoryFGEM(RolRepositoryFGEM rolesFGEM) {
        this.rolesFGEM = rolesFGEM;
    }

    public int contarFGEM(Connection cFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("SELECT COUNT(*) FROM usuario");
             ResultSet rsFGEM = psFGEM.executeQuery()) {
            return rsFGEM.next() ? rsFGEM.getInt(1) : 0;
        }
    }

    public int contarAdministradoresActivosFGEM(Connection cFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                SELECT COUNT(*) FROM usuario u
                JOIN rol r ON r.id = u.rol_id
                WHERE u.activo = 1 AND r.codigo = ?""")) {
            psFGEM.setString(1, RolFGEM.ADMINISTRADOR_FGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next() ? rsFGEM.getInt(1) : 0;
            }
        }
    }

    public List<UsuarioFGEM> listarFGEM(Connection cFGEM) throws SQLException {
        Map<Long, RolFGEM> mapaRolesFGEM = mapaRolesFGEM(cFGEM);
        List<UsuarioFGEM> usuariosFGEM = new ArrayList<>();
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT " + COLUMNAS_FGEM + " FROM usuario ORDER BY nombre_usuario");
             ResultSet rsFGEM = psFGEM.executeQuery()) {
            while (rsFGEM.next()) {
                usuariosFGEM.add(mapearFGEM(rsFGEM, mapaRolesFGEM));
            }
        }
        return usuariosFGEM;
    }

    public Optional<UsuarioFGEM> buscarPorIdFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("SELECT " + COLUMNAS_FGEM + " FROM usuario WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next() ? Optional.of(mapearFGEM(rsFGEM, mapaRolesFGEM(cFGEM))) : Optional.empty();
            }
        }
    }

    public Optional<CredencialesFGEM> buscarCredencialesFGEM(Connection cFGEM, String nombreUsuarioFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT " + COLUMNAS_FGEM + ", password_hash FROM usuario WHERE nombre_usuario = ?")) {
            psFGEM.setString(1, nombreUsuarioFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next()
                        ? Optional.of(new CredencialesFGEM(mapearFGEM(rsFGEM, mapaRolesFGEM(cFGEM)), rsFGEM.getString("password_hash")))
                        : Optional.empty();
            }
        }
    }

    public Optional<String> buscarHashFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("SELECT password_hash FROM usuario WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next() ? Optional.of(rsFGEM.getString(1)) : Optional.empty();
            }
        }
    }

    public boolean existeNombreUsuarioFGEM(Connection cFGEM, String nombreUsuarioFGEM, long excluirIdFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "SELECT 1 FROM usuario WHERE nombre_usuario = ? AND id <> ?")) {
            psFGEM.setString(1, nombreUsuarioFGEM);
            psFGEM.setLong(2, excluirIdFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next();
            }
        }
    }

    public long insertarFGEM(Connection cFGEM, DatosUsuarioFGEM datosFGEM, String passwordHashFGEM, boolean debeCambiarPasswordFGEM,
                         LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                INSERT INTO usuario (nombre_usuario, nombre_completo, email, password_hash, rol_id,
                                     debe_cambiar_password, creado_en, actualizado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            psFGEM.setString(1, datosFGEM.nombreUsuarioFGEM());
            psFGEM.setString(2, datosFGEM.nombreCompletoFGEM());
            psFGEM.setString(3, datosFGEM.emailFGEM());
            psFGEM.setString(4, passwordHashFGEM);
            psFGEM.setLong(5, datosFGEM.rolIdFGEM());
            psFGEM.setInt(6, debeCambiarPasswordFGEM ? 1 : 0);
            psFGEM.setString(7, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setString(8, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.executeUpdate();
            try (ResultSet clavesFGEM = psFGEM.getGeneratedKeys()) {
                if (!clavesFGEM.next()) {
                    throw new DataAccessExceptionFGEM("No se obtuvo el identificador del usuario creado");
                }
                return clavesFGEM.getLong(1);
            }
        }
    }

    public void actualizarDatosFGEM(Connection cFGEM, long idFGEM, DatosUsuarioFGEM datosFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE usuario
                SET nombre_usuario = ?, nombre_completo = ?, email = ?, rol_id = ?, actualizado_en = ?
                WHERE id = ?""")) {
            psFGEM.setString(1, datosFGEM.nombreUsuarioFGEM());
            psFGEM.setString(2, datosFGEM.nombreCompletoFGEM());
            psFGEM.setString(3, datosFGEM.emailFGEM());
            psFGEM.setLong(4, datosFGEM.rolIdFGEM());
            psFGEM.setString(5, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(6, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    public void actualizarPasswordFGEM(Connection cFGEM, long idFGEM, String passwordHashFGEM, boolean debeCambiarPasswordFGEM,
                                   LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE usuario
                SET password_hash = ?, debe_cambiar_password = ?, intentos_fallidos = 0,
                    bloqueado_hasta = NULL, actualizado_en = ?
                WHERE id = ?""")) {
            psFGEM.setString(1, passwordHashFGEM);
            psFGEM.setInt(2, debeCambiarPasswordFGEM ? 1 : 0);
            psFGEM.setString(3, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(4, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    public void reemplazarHashFGEM(Connection cFGEM, long idFGEM, String passwordHashFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("UPDATE usuario SET password_hash = ? WHERE id = ?")) {
            psFGEM.setString(1, passwordHashFGEM);
            psFGEM.setLong(2, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    public void cambiarActivoFGEM(Connection cFGEM, long idFGEM, boolean activoFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "UPDATE usuario SET activo = ?, actualizado_en = ? WHERE id = ?")) {
            psFGEM.setInt(1, activoFGEM ? 1 : 0);
            psFGEM.setString(2, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(3, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    public void registrarFalloFGEM(Connection cFGEM, long idFGEM, int intentosFallidosFGEM, LocalDateTime bloqueadoHastaFGEM)
            throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                "UPDATE usuario SET intentos_fallidos = ?, bloqueado_hasta = ? WHERE id = ?")) {
            psFGEM.setInt(1, intentosFallidosFGEM);
            psFGEM.setString(2, FechasFGEM.aTextoFGEM(bloqueadoHastaFGEM));
            psFGEM.setLong(3, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    public Optional<String> colorAcentoFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("SELECT color_acento FROM usuario WHERE id = ?")) {
            psFGEM.setLong(1, idFGEM);
            try (ResultSet rsFGEM = psFGEM.executeQuery()) {
                return rsFGEM.next() ? Optional.ofNullable(rsFGEM.getString(1)) : Optional.empty();
            }
        }
    }

    public void guardarColorAcentoFGEM(Connection cFGEM, long idFGEM, String colorFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("UPDATE usuario SET color_acento = ? WHERE id = ?")) {
            psFGEM.setString(1, colorFGEM);
            psFGEM.setLong(2, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    public void registrarAccesoExitosoFGEM(Connection cFGEM, long idFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE usuario
                SET intentos_fallidos = 0, bloqueado_hasta = NULL, ultimo_acceso = ?
                WHERE id = ?""")) {
            psFGEM.setString(1, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(2, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    public void desbloquearFGEM(Connection cFGEM, long idFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        try (PreparedStatement psFGEM = cFGEM.prepareStatement("""
                UPDATE usuario
                SET intentos_fallidos = 0, bloqueado_hasta = NULL, actualizado_en = ?
                WHERE id = ?""")) {
            psFGEM.setString(1, FechasFGEM.aTextoFGEM(ahoraFGEM));
            psFGEM.setLong(2, idFGEM);
            exigirUnaFilaFGEM(psFGEM.executeUpdate());
        }
    }

    private Map<Long, RolFGEM> mapaRolesFGEM(Connection cFGEM) throws SQLException {
        return rolesFGEM.listarFGEM(cFGEM).stream().collect(Collectors.toMap(RolFGEM::idFGEM, Function.identity()));
    }

    private static UsuarioFGEM mapearFGEM(ResultSet rsFGEM, Map<Long, RolFGEM> mapaRolesFGEM) throws SQLException {
        long rolIdFGEM = rsFGEM.getLong("rol_id");
        RolFGEM rolFGEM = mapaRolesFGEM.get(rolIdFGEM);
        if (rolFGEM == null) {
            throw new DataAccessExceptionFGEM("El usuario " + rsFGEM.getString("nombre_usuario")
                    + " tiene un rol inexistente (" + rolIdFGEM + ")");
        }
        return new UsuarioFGEM(
                rsFGEM.getLong("id"),
                rsFGEM.getString("nombre_usuario"),
                rsFGEM.getString("nombre_completo"),
                rsFGEM.getString("email"),
                rolFGEM,
                rsFGEM.getInt("activo") == 1,
                rsFGEM.getInt("debe_cambiar_password") == 1,
                rsFGEM.getInt("intentos_fallidos"),
                FechasFGEM.desdeTextoFGEM(rsFGEM.getString("bloqueado_hasta")),
                FechasFGEM.desdeTextoFGEM(rsFGEM.getString("ultimo_acceso")),
                FechasFGEM.desdeTextoFGEM(rsFGEM.getString("creado_en")));
    }

    private static void exigirUnaFilaFGEM(int filasAfectadasFGEM) {
        if (filasAfectadasFGEM != 1) {
            throw new ValidacionExceptionFGEM("El usuario ya no existe. Actualiza la lista e inténtalo de nuevo.");
        }
    }
}
