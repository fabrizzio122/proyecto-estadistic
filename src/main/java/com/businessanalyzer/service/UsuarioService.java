package com.businessanalyzer.service;

import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosUsuario;
import com.businessanalyzer.model.EventoAcceso;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.RegistroAcceso;
import com.businessanalyzer.model.Rol;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.repository.RegistroAccesoRepository;
import com.businessanalyzer.repository.RolRepository;
import com.businessanalyzer.repository.UsuarioRepository;
import com.businessanalyzer.seguridad.PasswordHasher;
import com.businessanalyzer.seguridad.PoliticaPassword;
import com.businessanalyzer.seguridad.Sesion;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Administración de cuentas de usuario. Todas las operaciones exigen el permiso
 * {@link Permiso#USUARIOS_GESTIONAR}.
 * <p>
 * Reglas que protegen el acceso al sistema:
 * <ul>
 *     <li>Siempre debe quedar al menos un administrador activo.</li>
 *     <li>Un administrador no puede desactivarse ni quitarse el rol a sí mismo.</li>
 *     <li>Las cuentas nuevas y las contraseñas restablecidas obligan a cambiar la contraseña
 *         en el siguiente inicio de sesión.</li>
 * </ul>
 * Los usuarios no se eliminan, se desactivan, para conservar el historial.
 */
public class UsuarioService {

    public static final int LIMITE_REGISTRO_ACCESOS = 200;

    private final Database database;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final RegistroAccesoRepository registro;
    private final PasswordHasher hasher;
    private final Sesion sesion;
    private final Clock reloj;

    public UsuarioService(Database database, UsuarioRepository usuarios, RolRepository roles,
                          RegistroAccesoRepository registro, PasswordHasher hasher, Sesion sesion, Clock reloj) {
        this.database = database;
        this.usuarios = usuarios;
        this.roles = roles;
        this.registro = registro;
        this.hasher = hasher;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    public List<Usuario> listar() {
        sesion.requerir(Permiso.USUARIOS_GESTIONAR);
        return database.conConexion(usuarios::listar);
    }

    public List<Rol> listarRoles() {
        sesion.requerir(Permiso.USUARIOS_GESTIONAR);
        return database.conConexion(roles::listar);
    }

    public List<RegistroAcceso> listarAccesos() {
        sesion.requerir(Permiso.USUARIOS_GESTIONAR);
        return database.conConexion(c -> registro.listarRecientes(c, LIMITE_REGISTRO_ACCESOS));
    }

    public Usuario crear(DatosUsuario datos, String password, String confirmacion) {
        sesion.requerir(Permiso.USUARIOS_GESTIONAR);
        DatosUsuario limpios = ValidadorUsuario.normalizar(datos);
        PoliticaPassword.validar(password, confirmacion, limpios.nombreUsuario());
        String hash = hasher.hashear(password.toCharArray());
        LocalDateTime ahora = ahora();
        String responsable = sesion.usuario().nombreUsuario();

        return database.enTransaccion(c -> {
            exigirRol(c, limpios.rolId());
            exigirNombreDisponible(c, limpios.nombreUsuario(), 0);
            long id = usuarios.insertar(c, limpios, hash, true, ahora);
            registro.registrar(c, id, limpios.nombreUsuario(), EventoAcceso.CUENTA_CREADA, responsable, ahora);
            return usuarios.buscarPorId(c, id).orElseThrow();
        });
    }

    public Usuario actualizar(long id, DatosUsuario datos) {
        sesion.requerir(Permiso.USUARIOS_GESTIONAR);
        DatosUsuario limpios = ValidadorUsuario.normalizar(datos);
        Usuario responsable = sesion.usuario();
        LocalDateTime ahora = ahora();

        Usuario actualizado = database.enTransaccion(c -> {
            Usuario actual = exigirUsuario(c, id);
            Rol nuevoRol = exigirRol(c, limpios.rolId());
            exigirNombreDisponible(c, limpios.nombreUsuario(), id);
            if (actual.rol().esAdministrador() && !nuevoRol.esAdministrador()) {
                if (id == responsable.id()) {
                    throw new ValidacionException("No puedes quitarte a ti mismo el rol de administrador.");
                }
                exigirOtroAdministrador(c, actual);
            }
            usuarios.actualizarDatos(c, id, limpios, ahora);
            registro.registrar(c, id, limpios.nombreUsuario(), EventoAcceso.CUENTA_ACTUALIZADA,
                    responsable.nombreUsuario(), ahora);
            return usuarios.buscarPorId(c, id).orElseThrow();
        });
        sesion.actualizar(actualizado);
        return actualizado;
    }

    public Usuario cambiarEstado(long id, boolean activo) {
        sesion.requerir(Permiso.USUARIOS_GESTIONAR);
        Usuario responsable = sesion.usuario();
        if (!activo && id == responsable.id()) {
            throw new ValidacionException("No puedes desactivar tu propia cuenta.");
        }
        LocalDateTime ahora = ahora();

        return database.enTransaccion(c -> {
            Usuario usuario = exigirUsuario(c, id);
            if (!activo && usuario.rol().esAdministrador()) {
                exigirOtroAdministrador(c, usuario);
            }
            usuarios.cambiarActivo(c, id, activo, ahora);
            registro.registrar(c, id, usuario.nombreUsuario(),
                    activo ? EventoAcceso.CUENTA_ACTIVADA : EventoAcceso.CUENTA_DESACTIVADA,
                    responsable.nombreUsuario(), ahora);
            return usuarios.buscarPorId(c, id).orElseThrow();
        });
    }

    /** Asigna una contraseña temporal; el usuario deberá cambiarla al ingresar. También lo desbloquea. */
    public void restablecerPassword(long id, String nueva, String confirmacion) {
        sesion.requerir(Permiso.USUARIOS_GESTIONAR);
        Usuario responsable = sesion.usuario();
        if (id == responsable.id()) {
            throw new ValidacionException("Para tu propia cuenta usa la opción \"Cambiar contraseña\" del menú de usuario.");
        }
        Usuario usuario = database.conConexion(c -> exigirUsuario(c, id));
        PoliticaPassword.validar(nueva, confirmacion, usuario.nombreUsuario());
        String hash = hasher.hashear(nueva.toCharArray());
        LocalDateTime ahora = ahora();

        database.enTransaccion(c -> {
            usuarios.actualizarPassword(c, id, hash, true, ahora);
            registro.registrar(c, id, usuario.nombreUsuario(), EventoAcceso.PASSWORD_RESTABLECIDA,
                    responsable.nombreUsuario(), ahora);
            return null;
        });
    }

    public Usuario desbloquear(long id) {
        sesion.requerir(Permiso.USUARIOS_GESTIONAR);
        String responsable = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = ahora();

        return database.enTransaccion(c -> {
            Usuario usuario = exigirUsuario(c, id);
            usuarios.desbloquear(c, id, ahora);
            registro.registrar(c, id, usuario.nombreUsuario(), EventoAcceso.CUENTA_DESBLOQUEADA, responsable, ahora);
            return usuarios.buscarPorId(c, id).orElseThrow();
        });
    }

    private Usuario exigirUsuario(Connection c, long id) throws SQLException {
        return usuarios.buscarPorId(c, id)
                .orElseThrow(() -> new ValidacionException("El usuario ya no existe. Actualiza la lista."));
    }

    private Rol exigirRol(Connection c, long rolId) throws SQLException {
        return roles.buscarPorId(c, rolId)
                .orElseThrow(() -> new ValidacionException("Selecciona un rol válido."));
    }

    private void exigirNombreDisponible(Connection c, String nombreUsuario, long excluirId) throws SQLException {
        if (usuarios.existeNombreUsuario(c, nombreUsuario, excluirId)) {
            throw new ValidacionException("Ya existe un usuario llamado \"" + nombreUsuario + "\".");
        }
    }

    /** Impide dejar el sistema sin administradores activos. */
    private void exigirOtroAdministrador(Connection c, Usuario afectado) throws SQLException {
        if (afectado.activo() && usuarios.contarAdministradoresActivos(c) <= 1) {
            throw new ValidacionException("Debe existir al menos un administrador activo. "
                    + "Asigna el rol de administrador a otra cuenta antes de continuar.");
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
