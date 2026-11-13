package com.businessanalyzer.service;

import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.AutenticacionException;
import com.businessanalyzer.exception.DataAccessException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosUsuario;
import com.businessanalyzer.model.EventoAcceso;
import com.businessanalyzer.model.Rol;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.repository.RegistroAccesoRepository;
import com.businessanalyzer.repository.RolRepository;
import com.businessanalyzer.repository.UsuarioRepository;
import com.businessanalyzer.repository.UsuarioRepository.Credenciales;
import com.businessanalyzer.seguridad.PasswordHasher;
import com.businessanalyzer.seguridad.PoliticaPassword;
import com.businessanalyzer.seguridad.Sesion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Inicio y cierre de sesión, creación del primer administrador y cambio de la propia contraseña.
 * <p>
 * Medidas de seguridad:
 * <ul>
 *     <li>Mensaje genérico ante usuario inexistente o contraseña errónea (no revela qué falló).</li>
 *     <li>Si el usuario no existe se calcula igualmente un hash, para que el tiempo de respuesta
 *         no delate qué usuarios existen.</li>
 *     <li>Bloqueo temporal tras {@value #MAXIMO_INTENTOS} intentos fallidos.</li>
 *     <li>Todos los eventos quedan en el registro de accesos.</li>
 * </ul>
 */
public class AutenticacionService {

    public static final int MAXIMO_INTENTOS = 5;
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    private static final String CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos.";
    private static final Logger log = LoggerFactory.getLogger(AutenticacionService.class);

    private final Database database;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final RegistroAccesoRepository registro;
    private final PasswordHasher hasher;
    private final Sesion sesion;
    private final Clock reloj;
    private volatile String hashFicticio;

    public AutenticacionService(Database database, UsuarioRepository usuarios, RolRepository roles,
                                RegistroAccesoRepository registro, PasswordHasher hasher, Sesion sesion,
                                Clock reloj) {
        this.database = database;
        this.usuarios = usuarios;
        this.roles = roles;
        this.registro = registro;
        this.hasher = hasher;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    /** {@code true} en el primer arranque, cuando todavía no existe ningún usuario. */
    public boolean requiereConfiguracionInicial() {
        return database.conConexion(usuarios::contar) == 0;
    }

    /** Crea la primera cuenta, siempre con rol Administrador. Solo funciona si no hay usuarios. */
    public Usuario crearAdministradorInicial(DatosUsuario datos, String password, String confirmacion) {
        DatosUsuario limpios = ValidadorUsuario.normalizar(datos);
        PoliticaPassword.validar(password, confirmacion, limpios.nombreUsuario());
        String hash = hasher.hashear(password.toCharArray());
        LocalDateTime ahora = ahora();

        return database.enTransaccion(c -> {
            if (usuarios.contar(c) > 0) {
                throw new ValidacionException("El sistema ya tiene usuarios. Inicia sesión con una cuenta existente.");
            }
            Rol administrador = roles.buscarPorCodigo(c, Rol.ADMINISTRADOR)
                    .orElseThrow(() -> new DataAccessException("No existe el rol " + Rol.ADMINISTRADOR));
            long id = usuarios.insertar(c, limpios.conRol(administrador.id()), hash, false, ahora);
            registro.registrar(c, id, limpios.nombreUsuario(), EventoAcceso.CUENTA_CREADA, null, ahora);
            return usuarios.buscarPorId(c, id).orElseThrow();
        });
    }

    /**
     * Verifica las credenciales y abre la sesión.
     *
     * @throws AutenticacionException con un mensaje apto para mostrar si no se puede ingresar
     */
    public Usuario iniciarSesion(String nombreUsuario, String password) {
        if (nombreUsuario == null || nombreUsuario.isBlank() || password == null || password.isEmpty()) {
            throw new AutenticacionException("Ingresa tu usuario y tu contraseña.");
        }
        String nombre = nombreUsuario.strip();
        LocalDateTime ahora = ahora();
        Optional<Credenciales> credenciales = database.conConexion(c -> usuarios.buscarCredenciales(c, nombre));

        if (credenciales.isEmpty()) {
            hasher.verificar(password.toCharArray(), hashFicticio());
            registrarEvento(null, nombre, EventoAcceso.CREDENCIALES_INVALIDAS, ahora);
            throw new AutenticacionException(CREDENCIALES_INVALIDAS);
        }

        Usuario usuario = credenciales.get().usuario();
        String hashGuardado = credenciales.get().passwordHash();

        if (usuario.bloqueadoEn(ahora)) {
            registrarEvento(usuario.id(), usuario.nombreUsuario(), EventoAcceso.INTENTO_CUENTA_BLOQUEADA, ahora);
            long minutos = Duration.between(ahora, usuario.bloqueadoHasta()).toMinutes() + 1;
            throw new AutenticacionException("La cuenta está bloqueada por intentos fallidos. "
                    + "Inténtalo de nuevo en " + minutos + " min o pide a un administrador que la desbloquee.");
        }

        if (!hasher.verificar(password.toCharArray(), hashGuardado)) {
            registrarFallo(usuario, ahora);
        }

        if (!usuario.activo()) {
            registrarEvento(usuario.id(), usuario.nombreUsuario(), EventoAcceso.INTENTO_CUENTA_INACTIVA, ahora);
            throw new AutenticacionException("La cuenta está desactivada. Contacta a un administrador.");
        }

        String hashNuevo = hasher.requiereActualizacion(hashGuardado) ? hasher.hashear(password.toCharArray()) : null;
        Usuario conectado = database.enTransaccion(c -> {
            usuarios.registrarAccesoExitoso(c, usuario.id(), ahora);
            if (hashNuevo != null) {
                usuarios.reemplazarHash(c, usuario.id(), hashNuevo);
            }
            registro.registrar(c, usuario.id(), usuario.nombreUsuario(), EventoAcceso.INICIO_SESION, null, ahora);
            return usuarios.buscarPorId(c, usuario.id()).orElseThrow();
        });
        sesion.iniciar(conectado);
        log.info("Sesión iniciada por {}", conectado.nombreUsuario());
        return conectado;
    }

    public void cerrarSesion() {
        sesion.usuarioActual().ifPresent(u -> {
            try {
                registrarEvento(u.id(), u.nombreUsuario(), EventoAcceso.CIERRE_SESION, ahora());
            } catch (DataAccessException e) {
                log.warn("No se pudo registrar el cierre de sesión de {}", u.nombreUsuario(), e);
            }
        });
        sesion.cerrar();
    }

    /** Cambio de contraseña del propio usuario conectado. Exige la contraseña actual. */
    public void cambiarPasswordPropia(String actual, String nueva, String confirmacion) {
        Usuario usuario = sesion.usuario();
        if (actual == null || actual.isEmpty()) {
            throw new ValidacionException("Ingresa tu contraseña actual.");
        }
        PoliticaPassword.validar(nueva, confirmacion, usuario.nombreUsuario());
        if (nueva.equals(actual)) {
            throw new ValidacionException("La nueva contraseña debe ser diferente de la actual.");
        }
        String hashActual = database.conConexion(c -> usuarios.buscarHash(c, usuario.id()))
                .orElseThrow(() -> new ValidacionException("Tu cuenta ya no existe."));
        if (!hasher.verificar(actual.toCharArray(), hashActual)) {
            throw new ValidacionException("La contraseña actual no es correcta.");
        }

        String hashNuevo = hasher.hashear(nueva.toCharArray());
        LocalDateTime ahora = ahora();
        Usuario actualizado = database.enTransaccion(c -> {
            usuarios.actualizarPassword(c, usuario.id(), hashNuevo, false, ahora);
            registro.registrar(c, usuario.id(), usuario.nombreUsuario(), EventoAcceso.CAMBIO_PASSWORD, null, ahora);
            return usuarios.buscarPorId(c, usuario.id()).orElseThrow();
        });
        sesion.actualizar(actualizado);
    }

    private void registrarFallo(Usuario usuario, LocalDateTime ahora) {
        // Si hubo un bloqueo que ya venció, el conteo empieza de nuevo.
        int previos = usuario.bloqueadoHasta() != null ? 0 : usuario.intentosFallidos();
        int intentos = previos + 1;
        boolean bloquear = intentos >= MAXIMO_INTENTOS;
        LocalDateTime bloqueadoHasta = bloquear ? ahora.plus(DURACION_BLOQUEO) : null;

        database.enTransaccion(c -> {
            usuarios.registrarFallo(c, usuario.id(), bloquear ? 0 : intentos, bloqueadoHasta);
            registro.registrar(c, usuario.id(), usuario.nombreUsuario(),
                    bloquear ? EventoAcceso.CUENTA_BLOQUEADA : EventoAcceso.CREDENCIALES_INVALIDAS, null, ahora);
            return null;
        });
        if (bloquear) {
            log.warn("Cuenta {} bloqueada por intentos fallidos", usuario.nombreUsuario());
            throw new AutenticacionException("Demasiados intentos fallidos. La cuenta quedó bloqueada por "
                    + DURACION_BLOQUEO.toMinutes() + " minutos.");
        }
        throw new AutenticacionException(CREDENCIALES_INVALIDAS);
    }

    private void registrarEvento(Long usuarioId, String nombreUsuario, EventoAcceso evento, LocalDateTime fecha) {
        database.conConexion(c -> {
            registro.registrar(c, usuarioId, nombreUsuario, evento, null, fecha);
            return null;
        });
    }

    private String hashFicticio() {
        String hash = hashFicticio;
        if (hash == null) {
            hash = hasher.hashear("contraseña-ficticia-0".toCharArray());
            hashFicticio = hash;
        }
        return hash;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
