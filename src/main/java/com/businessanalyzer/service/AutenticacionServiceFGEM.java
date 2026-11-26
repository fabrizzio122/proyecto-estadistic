package com.businessanalyzer.service;

import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.AutenticacionExceptionFGEM;
import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosUsuarioFGEM;
import com.businessanalyzer.model.EventoAccesoFGEM;
import com.businessanalyzer.model.RolFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.repository.RegistroAccesoRepositoryFGEM;
import com.businessanalyzer.repository.RolRepositoryFGEM;
import com.businessanalyzer.repository.UsuarioRepositoryFGEM;
import com.businessanalyzer.repository.UsuarioRepositoryFGEM.CredencialesFGEM;
import com.businessanalyzer.seguridad.PasswordHasherFGEM;
import com.businessanalyzer.seguridad.PoliticaPasswordFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class AutenticacionServiceFGEM {

    public static final int MAXIMO_INTENTOS_FGEM = 5;
    public static final Duration DURACION_BLOQUEO_FGEM = Duration.ofMinutes(15);

    private static final String CREDENCIALES_INVALIDAS_FGEM = "Usuario o contraseña incorrectos.";
    private static final Logger logFGEM = LoggerFactory.getLogger(AutenticacionServiceFGEM.class);

    private final DatabaseFGEM databaseFGEM;
    private final UsuarioRepositoryFGEM usuariosFGEM;
    private final RolRepositoryFGEM rolesFGEM;
    private final RegistroAccesoRepositoryFGEM registroFGEM;
    private final PasswordHasherFGEM hasherFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;
    private volatile String hashFicticioFGEM;

    public AutenticacionServiceFGEM(DatabaseFGEM databaseFGEM, UsuarioRepositoryFGEM usuariosFGEM, RolRepositoryFGEM rolesFGEM,
                                RegistroAccesoRepositoryFGEM registroFGEM, PasswordHasherFGEM hasherFGEM, SesionFGEM sesionFGEM,
                                Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.usuariosFGEM = usuariosFGEM;
        this.rolesFGEM = rolesFGEM;
        this.registroFGEM = registroFGEM;
        this.hasherFGEM = hasherFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    public boolean requiereConfiguracionInicialFGEM() {
        return databaseFGEM.conConexionFGEM(usuariosFGEM::contarFGEM) == 0;
    }

    public UsuarioFGEM crearAdministradorInicialFGEM(DatosUsuarioFGEM datosFGEM, String passwordFGEM, String confirmacionFGEM) {
        DatosUsuarioFGEM limpiosFGEM = ValidadorUsuarioFGEM.normalizarFGEM(datosFGEM);
        PoliticaPasswordFGEM.validarFGEM(passwordFGEM, confirmacionFGEM, limpiosFGEM.nombreUsuarioFGEM());
        String hashFGEM = hasherFGEM.hashearFGEM(passwordFGEM.toCharArray());
        LocalDateTime ahoraFGEM = ahoraFGEM();

        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            if (usuariosFGEM.contarFGEM(cFGEM) > 0) {
                throw new ValidacionExceptionFGEM("El sistema ya tiene usuarios. Inicia sesión con una cuenta existente.");
            }
            RolFGEM administradorFGEM = rolesFGEM.buscarPorCodigoFGEM(cFGEM, RolFGEM.ADMINISTRADOR_FGEM)
                    .orElseThrow(() -> new DataAccessExceptionFGEM("No existe el rol " + RolFGEM.ADMINISTRADOR_FGEM));
            long idFGEM = usuariosFGEM.insertarFGEM(cFGEM, limpiosFGEM.conRolFGEM(administradorFGEM.idFGEM()), hashFGEM, false, ahoraFGEM);
            registroFGEM.registrarFGEM(cFGEM, idFGEM, limpiosFGEM.nombreUsuarioFGEM(), EventoAccesoFGEM.CUENTA_CREADA, null, ahoraFGEM);
            return usuariosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public UsuarioFGEM iniciarSesionFGEM(String nombreUsuarioFGEM, String passwordFGEM) {
        if (nombreUsuarioFGEM == null || nombreUsuarioFGEM.isBlank() || passwordFGEM == null || passwordFGEM.isEmpty()) {
            throw new AutenticacionExceptionFGEM("Ingresa tu usuario y tu contraseña.");
        }
        String nombreFGEM = nombreUsuarioFGEM.strip();
        LocalDateTime ahoraFGEM = ahoraFGEM();
        Optional<CredencialesFGEM> credencialesFGEM = databaseFGEM.conConexionFGEM(cFGEM -> usuariosFGEM.buscarCredencialesFGEM(cFGEM, nombreFGEM));

        if (credencialesFGEM.isEmpty()) {
            hasherFGEM.verificarFGEM(passwordFGEM.toCharArray(), hashFicticioFGEM());
            registrarEventoFGEM(null, nombreFGEM, EventoAccesoFGEM.CREDENCIALES_INVALIDAS, ahoraFGEM);
            throw new AutenticacionExceptionFGEM(CREDENCIALES_INVALIDAS_FGEM);
        }

        UsuarioFGEM usuarioFGEM = credencialesFGEM.get().usuarioFGEM();
        String hashGuardadoFGEM = credencialesFGEM.get().passwordHashFGEM();

        if (usuarioFGEM.bloqueadoEnFGEM(ahoraFGEM)) {
            registrarEventoFGEM(usuarioFGEM.idFGEM(), usuarioFGEM.nombreUsuarioFGEM(), EventoAccesoFGEM.INTENTO_CUENTA_BLOQUEADA, ahoraFGEM);
            long minutosFGEM = Duration.between(ahoraFGEM, usuarioFGEM.bloqueadoHastaFGEM()).toMinutes() + 1;
            throw new AutenticacionExceptionFGEM("La cuenta está bloqueada por intentos fallidos. "
                    + "Inténtalo de nuevo en " + minutosFGEM + " min o pide a un administrador que la desbloquee.");
        }

        if (!hasherFGEM.verificarFGEM(passwordFGEM.toCharArray(), hashGuardadoFGEM)) {
            registrarFalloFGEM(usuarioFGEM, ahoraFGEM);
        }

        if (!usuarioFGEM.activoFGEM()) {
            registrarEventoFGEM(usuarioFGEM.idFGEM(), usuarioFGEM.nombreUsuarioFGEM(), EventoAccesoFGEM.INTENTO_CUENTA_INACTIVA, ahoraFGEM);
            throw new AutenticacionExceptionFGEM("La cuenta está desactivada. Contacta a un administrador.");
        }

        String hashNuevoFGEM = hasherFGEM.requiereActualizacionFGEM(hashGuardadoFGEM) ? hasherFGEM.hashearFGEM(passwordFGEM.toCharArray()) : null;
        UsuarioFGEM conectadoFGEM = databaseFGEM.enTransaccionFGEM(cFGEM -> {
            usuariosFGEM.registrarAccesoExitosoFGEM(cFGEM, usuarioFGEM.idFGEM(), ahoraFGEM);
            if (hashNuevoFGEM != null) {
                usuariosFGEM.reemplazarHashFGEM(cFGEM, usuarioFGEM.idFGEM(), hashNuevoFGEM);
            }
            registroFGEM.registrarFGEM(cFGEM, usuarioFGEM.idFGEM(), usuarioFGEM.nombreUsuarioFGEM(), EventoAccesoFGEM.INICIO_SESION, null, ahoraFGEM);
            return usuariosFGEM.buscarPorIdFGEM(cFGEM, usuarioFGEM.idFGEM()).orElseThrow();
        });
        sesionFGEM.iniciarFGEM(conectadoFGEM);
        logFGEM.info("Sesión iniciada por {}", conectadoFGEM.nombreUsuarioFGEM());
        return conectadoFGEM;
    }

    public void cerrarSesionFGEM() {
        sesionFGEM.usuarioActualFGEM().ifPresent(uFGEM -> {
            try {
                registrarEventoFGEM(uFGEM.idFGEM(), uFGEM.nombreUsuarioFGEM(), EventoAccesoFGEM.CIERRE_SESION, ahoraFGEM());
            } catch (DataAccessExceptionFGEM eFGEM) {
                logFGEM.warn("No se pudo registrar el cierre de sesión de {}", uFGEM.nombreUsuarioFGEM(), eFGEM);
            }
        });
        sesionFGEM.cerrarFGEM();
    }

    public void cambiarPasswordPropiaFGEM(String actualFGEM, String nuevaFGEM, String confirmacionFGEM) {
        UsuarioFGEM usuarioFGEM = sesionFGEM.usuarioFGEM();
        if (actualFGEM == null || actualFGEM.isEmpty()) {
            throw new ValidacionExceptionFGEM("Ingresa tu contraseña actual.");
        }
        PoliticaPasswordFGEM.validarFGEM(nuevaFGEM, confirmacionFGEM, usuarioFGEM.nombreUsuarioFGEM());
        if (nuevaFGEM.equals(actualFGEM)) {
            throw new ValidacionExceptionFGEM("La nueva contraseña debe ser diferente de la actual.");
        }
        String hashActualFGEM = databaseFGEM.conConexionFGEM(cFGEM -> usuariosFGEM.buscarHashFGEM(cFGEM, usuarioFGEM.idFGEM()))
                .orElseThrow(() -> new ValidacionExceptionFGEM("Tu cuenta ya no existe."));
        if (!hasherFGEM.verificarFGEM(actualFGEM.toCharArray(), hashActualFGEM)) {
            throw new ValidacionExceptionFGEM("La contraseña actual no es correcta.");
        }

        String hashNuevoFGEM = hasherFGEM.hashearFGEM(nuevaFGEM.toCharArray());
        LocalDateTime ahoraFGEM = ahoraFGEM();
        UsuarioFGEM actualizadoFGEM = databaseFGEM.enTransaccionFGEM(cFGEM -> {
            usuariosFGEM.actualizarPasswordFGEM(cFGEM, usuarioFGEM.idFGEM(), hashNuevoFGEM, false, ahoraFGEM);
            registroFGEM.registrarFGEM(cFGEM, usuarioFGEM.idFGEM(), usuarioFGEM.nombreUsuarioFGEM(), EventoAccesoFGEM.CAMBIO_PASSWORD, null, ahoraFGEM);
            return usuariosFGEM.buscarPorIdFGEM(cFGEM, usuarioFGEM.idFGEM()).orElseThrow();
        });
        sesionFGEM.actualizarFGEM(actualizadoFGEM);
    }

    private void registrarFalloFGEM(UsuarioFGEM usuarioFGEM, LocalDateTime ahoraFGEM) {
        int previosFGEM = usuarioFGEM.bloqueadoHastaFGEM() != null ? 0 : usuarioFGEM.intentosFallidosFGEM();
        int intentosFGEM = previosFGEM + 1;
        boolean bloquearFGEM = intentosFGEM >= MAXIMO_INTENTOS_FGEM;
        LocalDateTime bloqueadoHastaFGEM = bloquearFGEM ? ahoraFGEM.plus(DURACION_BLOQUEO_FGEM) : null;

        databaseFGEM.enTransaccionFGEM(cFGEM -> {
            usuariosFGEM.registrarFalloFGEM(cFGEM, usuarioFGEM.idFGEM(), bloquearFGEM ? 0 : intentosFGEM, bloqueadoHastaFGEM);
            registroFGEM.registrarFGEM(cFGEM, usuarioFGEM.idFGEM(), usuarioFGEM.nombreUsuarioFGEM(),
                    bloquearFGEM ? EventoAccesoFGEM.CUENTA_BLOQUEADA : EventoAccesoFGEM.CREDENCIALES_INVALIDAS, null, ahoraFGEM);
            return null;
        });
        if (bloquearFGEM) {
            logFGEM.warn("Cuenta {} bloqueada por intentos fallidos", usuarioFGEM.nombreUsuarioFGEM());
            throw new AutenticacionExceptionFGEM("Demasiados intentos fallidos. La cuenta quedó bloqueada por "
                    + DURACION_BLOQUEO_FGEM.toMinutes() + " minutos.");
        }
        throw new AutenticacionExceptionFGEM(CREDENCIALES_INVALIDAS_FGEM);
    }

    private void registrarEventoFGEM(Long usuarioIdFGEM, String nombreUsuarioFGEM, EventoAccesoFGEM eventoFGEM, LocalDateTime fechaFGEM) {
        databaseFGEM.conConexionFGEM(cFGEM -> {
            registroFGEM.registrarFGEM(cFGEM, usuarioIdFGEM, nombreUsuarioFGEM, eventoFGEM, null, fechaFGEM);
            return null;
        });
    }

    private String hashFicticioFGEM() {
        String hashFGEM = hashFicticioFGEM;
        if (hashFGEM == null) {
            hashFGEM = hasherFGEM.hashearFGEM("contraseña-ficticia-0".toCharArray());
            hashFicticioFGEM = hashFGEM;
        }
        return hashFGEM;
    }

    private LocalDateTime ahoraFGEM() {
        return LocalDateTime.now(relojFGEM);
    }
}
