package com.businessanalyzer.service;

import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosUsuarioFGEM;
import com.businessanalyzer.model.EventoAccesoFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.RegistroAccesoFGEM;
import com.businessanalyzer.model.RolFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.repository.RegistroAccesoRepositoryFGEM;
import com.businessanalyzer.repository.RolRepositoryFGEM;
import com.businessanalyzer.repository.UsuarioRepositoryFGEM;
import com.businessanalyzer.seguridad.PasswordHasherFGEM;
import com.businessanalyzer.seguridad.PoliticaPasswordFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Administración de cuentas de usuario. Todas las operaciones exigen el permiso
 * {@link PermisoFGEM#USUARIOS_GESTIONAR}.
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
public class UsuarioServiceFGEM {

    public static final int LIMITE_REGISTRO_ACCESOS_FGEM = 200;

    private final DatabaseFGEM databaseFGEM;
    private final UsuarioRepositoryFGEM usuariosFGEM;
    private final RolRepositoryFGEM rolesFGEM;
    private final RegistroAccesoRepositoryFGEM registroFGEM;
    private final PasswordHasherFGEM hasherFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public UsuarioServiceFGEM(DatabaseFGEM databaseFGEM, UsuarioRepositoryFGEM usuariosFGEM, RolRepositoryFGEM rolesFGEM,
                          RegistroAccesoRepositoryFGEM registroFGEM, PasswordHasherFGEM hasherFGEM, SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.usuariosFGEM = usuariosFGEM;
        this.rolesFGEM = rolesFGEM;
        this.registroFGEM = registroFGEM;
        this.hasherFGEM = hasherFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    public List<UsuarioFGEM> listarFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.USUARIOS_GESTIONAR);
        return databaseFGEM.conConexionFGEM(usuariosFGEM::listarFGEM);
    }

    public List<RolFGEM> listarRolesFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.USUARIOS_GESTIONAR);
        return databaseFGEM.conConexionFGEM(rolesFGEM::listarFGEM);
    }

    public List<RegistroAccesoFGEM> listarAccesosFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.USUARIOS_GESTIONAR);
        return databaseFGEM.conConexionFGEM(cFGEM -> registroFGEM.listarRecientesFGEM(cFGEM, LIMITE_REGISTRO_ACCESOS_FGEM));
    }

    public UsuarioFGEM crearFGEM(DatosUsuarioFGEM datosFGEM, String passwordFGEM, String confirmacionFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.USUARIOS_GESTIONAR);
        DatosUsuarioFGEM limpiosFGEM = ValidadorUsuarioFGEM.normalizarFGEM(datosFGEM);
        PoliticaPasswordFGEM.validarFGEM(passwordFGEM, confirmacionFGEM, limpiosFGEM.nombreUsuarioFGEM());
        String hashFGEM = hasherFGEM.hashearFGEM(passwordFGEM.toCharArray());
        LocalDateTime ahoraFGEM = ahoraFGEM();
        String responsableFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();

        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            exigirRolFGEM(cFGEM, limpiosFGEM.rolIdFGEM());
            exigirNombreDisponibleFGEM(cFGEM, limpiosFGEM.nombreUsuarioFGEM(), 0);
            long idFGEM = usuariosFGEM.insertarFGEM(cFGEM, limpiosFGEM, hashFGEM, true, ahoraFGEM);
            registroFGEM.registrarFGEM(cFGEM, idFGEM, limpiosFGEM.nombreUsuarioFGEM(), EventoAccesoFGEM.CUENTA_CREADA, responsableFGEM, ahoraFGEM);
            return usuariosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public UsuarioFGEM actualizarFGEM(long idFGEM, DatosUsuarioFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.USUARIOS_GESTIONAR);
        DatosUsuarioFGEM limpiosFGEM = ValidadorUsuarioFGEM.normalizarFGEM(datosFGEM);
        UsuarioFGEM responsableFGEM = sesionFGEM.usuarioFGEM();
        LocalDateTime ahoraFGEM = ahoraFGEM();

        UsuarioFGEM actualizadoFGEM = databaseFGEM.enTransaccionFGEM(cFGEM -> {
            UsuarioFGEM actualFGEM = exigirUsuarioFGEM(cFGEM, idFGEM);
            RolFGEM nuevoRolFGEM = exigirRolFGEM(cFGEM, limpiosFGEM.rolIdFGEM());
            exigirNombreDisponibleFGEM(cFGEM, limpiosFGEM.nombreUsuarioFGEM(), idFGEM);
            if (actualFGEM.rolFGEM().esAdministradorFGEM() && !nuevoRolFGEM.esAdministradorFGEM()) {
                if (idFGEM == responsableFGEM.idFGEM()) {
                    throw new ValidacionExceptionFGEM("No puedes quitarte a ti mismo el rol de administrador.");
                }
                exigirOtroAdministradorFGEM(cFGEM, actualFGEM);
            }
            usuariosFGEM.actualizarDatosFGEM(cFGEM, idFGEM, limpiosFGEM, ahoraFGEM);
            registroFGEM.registrarFGEM(cFGEM, idFGEM, limpiosFGEM.nombreUsuarioFGEM(), EventoAccesoFGEM.CUENTA_ACTUALIZADA,
                    responsableFGEM.nombreUsuarioFGEM(), ahoraFGEM);
            return usuariosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
        sesionFGEM.actualizarFGEM(actualizadoFGEM);
        return actualizadoFGEM;
    }

    public UsuarioFGEM cambiarEstadoFGEM(long idFGEM, boolean activoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.USUARIOS_GESTIONAR);
        UsuarioFGEM responsableFGEM = sesionFGEM.usuarioFGEM();
        if (!activoFGEM && idFGEM == responsableFGEM.idFGEM()) {
            throw new ValidacionExceptionFGEM("No puedes desactivar tu propia cuenta.");
        }
        LocalDateTime ahoraFGEM = ahoraFGEM();

        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            UsuarioFGEM usuarioFGEM = exigirUsuarioFGEM(cFGEM, idFGEM);
            if (!activoFGEM && usuarioFGEM.rolFGEM().esAdministradorFGEM()) {
                exigirOtroAdministradorFGEM(cFGEM, usuarioFGEM);
            }
            usuariosFGEM.cambiarActivoFGEM(cFGEM, idFGEM, activoFGEM, ahoraFGEM);
            registroFGEM.registrarFGEM(cFGEM, idFGEM, usuarioFGEM.nombreUsuarioFGEM(),
                    activoFGEM ? EventoAccesoFGEM.CUENTA_ACTIVADA : EventoAccesoFGEM.CUENTA_DESACTIVADA,
                    responsableFGEM.nombreUsuarioFGEM(), ahoraFGEM);
            return usuariosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    /** Asigna una contraseña temporal; el usuario deberá cambiarla al ingresar. También lo desbloquea. */
    public void restablecerPasswordFGEM(long idFGEM, String nuevaFGEM, String confirmacionFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.USUARIOS_GESTIONAR);
        UsuarioFGEM responsableFGEM = sesionFGEM.usuarioFGEM();
        if (idFGEM == responsableFGEM.idFGEM()) {
            throw new ValidacionExceptionFGEM("Para tu propia cuenta usa la opción \"Cambiar contraseña\" del menú de usuario.");
        }
        UsuarioFGEM usuarioFGEM = databaseFGEM.conConexionFGEM(cFGEM -> exigirUsuarioFGEM(cFGEM, idFGEM));
        PoliticaPasswordFGEM.validarFGEM(nuevaFGEM, confirmacionFGEM, usuarioFGEM.nombreUsuarioFGEM());
        String hashFGEM = hasherFGEM.hashearFGEM(nuevaFGEM.toCharArray());
        LocalDateTime ahoraFGEM = ahoraFGEM();

        databaseFGEM.enTransaccionFGEM(cFGEM -> {
            usuariosFGEM.actualizarPasswordFGEM(cFGEM, idFGEM, hashFGEM, true, ahoraFGEM);
            registroFGEM.registrarFGEM(cFGEM, idFGEM, usuarioFGEM.nombreUsuarioFGEM(), EventoAccesoFGEM.PASSWORD_RESTABLECIDA,
                    responsableFGEM.nombreUsuarioFGEM(), ahoraFGEM);
            return null;
        });
    }

    public UsuarioFGEM desbloquearFGEM(long idFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.USUARIOS_GESTIONAR);
        String responsableFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = ahoraFGEM();

        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            UsuarioFGEM usuarioFGEM = exigirUsuarioFGEM(cFGEM, idFGEM);
            usuariosFGEM.desbloquearFGEM(cFGEM, idFGEM, ahoraFGEM);
            registroFGEM.registrarFGEM(cFGEM, idFGEM, usuarioFGEM.nombreUsuarioFGEM(), EventoAccesoFGEM.CUENTA_DESBLOQUEADA, responsableFGEM, ahoraFGEM);
            return usuariosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    private UsuarioFGEM exigirUsuarioFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        return usuariosFGEM.buscarPorIdFGEM(cFGEM, idFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM("El usuario ya no existe. Actualiza la lista."));
    }

    private RolFGEM exigirRolFGEM(Connection cFGEM, long rolIdFGEM) throws SQLException {
        return rolesFGEM.buscarPorIdFGEM(cFGEM, rolIdFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM("Selecciona un rol válido."));
    }

    private void exigirNombreDisponibleFGEM(Connection cFGEM, String nombreUsuarioFGEM, long excluirIdFGEM) throws SQLException {
        if (usuariosFGEM.existeNombreUsuarioFGEM(cFGEM, nombreUsuarioFGEM, excluirIdFGEM)) {
            throw new ValidacionExceptionFGEM("Ya existe un usuario llamado \"" + nombreUsuarioFGEM + "\".");
        }
    }

    /** Impide dejar el sistema sin administradores activos. */
    private void exigirOtroAdministradorFGEM(Connection cFGEM, UsuarioFGEM afectadoFGEM) throws SQLException {
        if (afectadoFGEM.activoFGEM() && usuariosFGEM.contarAdministradoresActivosFGEM(cFGEM) <= 1) {
            throw new ValidacionExceptionFGEM("Debe existir al menos un administrador activo. "
                    + "Asigna el rol de administrador a otra cuenta antes de continuar.");
        }
    }

    private LocalDateTime ahoraFGEM() {
        return LocalDateTime.now(relojFGEM);
    }
}
