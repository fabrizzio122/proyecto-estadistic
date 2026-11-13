package com.businessanalyzer;

import com.businessanalyzer.config.Database;
import com.businessanalyzer.config.Migrador;
import com.businessanalyzer.model.DatosUsuario;
import com.businessanalyzer.model.Rol;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.repository.RegistroAccesoRepository;
import com.businessanalyzer.repository.RolRepository;
import com.businessanalyzer.repository.UsuarioRepository;
import com.businessanalyzer.seguridad.PasswordHasher;
import com.businessanalyzer.seguridad.Sesion;
import com.businessanalyzer.service.AutenticacionService;
import com.businessanalyzer.service.UsuarioService;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Base de datos temporal ya migrada con todos los servicios de seguridad.
 * Usa un hash con pocas iteraciones (las pruebas serían muy lentas con 600.000)
 * y un reloj que se puede adelantar para probar los bloqueos.
 */
public final class EntornoPrueba {

    public static final String PASSWORD_ADMIN = "Admin2026";

    public final Database database;
    public final RelojAjustable reloj = new RelojAjustable(LocalDateTime.of(2026, 1, 15, 9, 0));
    public final Sesion sesion = new Sesion();
    public final RolRepository roles = new RolRepository();
    public final UsuarioRepository usuarios = new UsuarioRepository(roles);
    public final RegistroAccesoRepository registro = new RegistroAccesoRepository();
    public final PasswordHasher hasher;
    public final AutenticacionService autenticacion;
    public final UsuarioService usuarioService;

    public EntornoPrueba(Path carpeta) {
        this(carpeta, new PasswordHasher(1_000));
    }

    public EntornoPrueba(Path carpeta, PasswordHasher hasher) {
        this.database = new Database(carpeta.resolve("prueba.db"));
        new Migrador(database).migrar();
        this.hasher = hasher;
        this.autenticacion = new AutenticacionService(database, usuarios, roles, registro, hasher, sesion, reloj);
        this.usuarioService = new UsuarioService(database, usuarios, roles, registro, hasher, sesion, reloj);
    }

    /** Crea el administrador inicial "admin" e inicia sesión con él. */
    public Usuario administradorConectado() {
        autenticacion.crearAdministradorInicial(
                new DatosUsuario("admin", "Administrador General", "admin@empresa.com", 0),
                PASSWORD_ADMIN, PASSWORD_ADMIN);
        return autenticacion.iniciarSesion("admin", PASSWORD_ADMIN);
    }

    public long idRol(String codigo) {
        return database.conConexion(c -> roles.buscarPorCodigo(c, codigo)).map(Rol::id).orElseThrow();
    }

    /** Reloj fijo que solo avanza cuando la prueba lo pide. */
    public static final class RelojAjustable extends Clock {
        private Instant instante;

        public RelojAjustable(LocalDateTime inicio) {
            this.instante = inicio.toInstant(ZoneOffset.UTC);
        }

        public void avanzar(Duration duracion) {
            instante = instante.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}
