package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.exception.AutenticacionException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosUsuario;
import com.businessanalyzer.model.EventoAcceso;
import com.businessanalyzer.model.Rol;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.seguridad.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static com.businessanalyzer.EntornoPrueba.PASSWORD_ADMIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutenticacionServiceTest {

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private AutenticacionService autenticacion;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        autenticacion = entorno.autenticacion;
    }

    @Test
    void elPrimerArranqueRequiereCrearElAdministrador() {
        assertTrue(autenticacion.requiereConfiguracionInicial());

        Usuario admin = autenticacion.crearAdministradorInicial(
                new DatosUsuario("  admin ", "Administrador", "", 0), PASSWORD_ADMIN, PASSWORD_ADMIN);

        assertFalse(autenticacion.requiereConfiguracionInicial());
        assertEquals("admin", admin.nombreUsuario());
        assertEquals(Rol.ADMINISTRADOR, admin.rol().codigo());
        assertFalse(admin.debeCambiarPassword());
    }

    @Test
    void noSePuedeCrearUnSegundoAdministradorInicial() {
        entorno.administradorConectado();

        assertThrows(ValidacionException.class, () -> autenticacion.crearAdministradorInicial(
                new DatosUsuario("otro", "Otro admin", null, 0), "Clave12345", "Clave12345"));
    }

    @Test
    void inicioDeSesionCorrectoAbreLaSesion() {
        entorno.administradorConectado();
        autenticacion.cerrarSesion();
        assertFalse(entorno.sesion.activa());

        Usuario usuario = autenticacion.iniciarSesion("ADMIN", PASSWORD_ADMIN);

        assertTrue(entorno.sesion.activa(), "La sesión debe quedar abierta");
        assertEquals("admin", usuario.nombreUsuario(), "El usuario no distingue mayúsculas");
        assertNotNull(usuario.ultimoAcceso());
    }

    @Test
    void usuarioInexistenteYContrasenaErroneaDanElMismoMensaje() {
        entorno.administradorConectado();

        AutenticacionException inexistente = assertThrows(AutenticacionException.class,
                () -> autenticacion.iniciarSesion("nadie", "Clave12345"));
        AutenticacionException erronea = assertThrows(AutenticacionException.class,
                () -> autenticacion.iniciarSesion("admin", "Incorrecta1"));

        assertEquals(inexistente.getMessage(), erronea.getMessage());
    }

    @Test
    void camposVaciosSeRechazan() {
        assertThrows(AutenticacionException.class, () -> autenticacion.iniciarSesion("", "x"));
        assertThrows(AutenticacionException.class, () -> autenticacion.iniciarSesion("admin", ""));
        assertThrows(AutenticacionException.class, () -> autenticacion.iniciarSesion(null, null));
    }

    @Test
    void cincoIntentosFallidosBloqueanLaCuentaQuinceMinutos() {
        entorno.administradorConectado();
        autenticacion.cerrarSesion();

        for (int i = 0; i < AutenticacionService.MAXIMO_INTENTOS; i++) {
            assertThrows(AutenticacionException.class, () -> autenticacion.iniciarSesion("admin", "Incorrecta1"));
        }

        AutenticacionException bloqueada = assertThrows(AutenticacionException.class,
                () -> autenticacion.iniciarSesion("admin", PASSWORD_ADMIN));
        assertTrue(bloqueada.getMessage().contains("bloqueada"),
                "Con la cuenta bloqueada ni la contraseña correcta permite entrar");

        entorno.reloj.avanzar(AutenticacionService.DURACION_BLOQUEO.plus(Duration.ofSeconds(1)));
        Usuario usuario = autenticacion.iniciarSesion("admin", PASSWORD_ADMIN);
        assertEquals(0, usuario.intentosFallidos());
    }

    @Test
    void unAccesoCorrectoReiniciaElContadorDeFallos() {
        entorno.administradorConectado();
        for (int i = 0; i < AutenticacionService.MAXIMO_INTENTOS - 1; i++) {
            assertThrows(AutenticacionException.class, () -> autenticacion.iniciarSesion("admin", "Incorrecta1"));
        }
        autenticacion.iniciarSesion("admin", PASSWORD_ADMIN);

        assertThrows(AutenticacionException.class, () -> autenticacion.iniciarSesion("admin", "Incorrecta1"));
        Usuario usuario = autenticacion.iniciarSesion("admin", PASSWORD_ADMIN);
        assertFalse(usuario.bloqueadoEn(entorno.reloj.instant().atZone(entorno.reloj.getZone()).toLocalDateTime()));
    }

    @Test
    void unaCuentaDesactivadaNoPuedeIngresar() {
        entorno.administradorConectado();
        Usuario operador = entorno.usuarioService.crear(
                new DatosUsuario("operador", "Operador Uno", null, entorno.idRol("OPERADOR")),
                "Temporal123", "Temporal123");
        entorno.usuarioService.cambiarEstado(operador.id(), false);

        AutenticacionException error = assertThrows(AutenticacionException.class,
                () -> autenticacion.iniciarSesion("operador", "Temporal123"));
        assertTrue(error.getMessage().contains("desactivada"));
    }

    @Test
    void cambiarLaPropiaContrasenaExigeLaActual() {
        entorno.administradorConectado();

        assertThrows(ValidacionException.class,
                () -> autenticacion.cambiarPasswordPropia("Incorrecta1", "NuevaClave9", "NuevaClave9"));
        assertThrows(ValidacionException.class,
                () -> autenticacion.cambiarPasswordPropia(PASSWORD_ADMIN, PASSWORD_ADMIN, PASSWORD_ADMIN));

        autenticacion.cambiarPasswordPropia(PASSWORD_ADMIN, "NuevaClave9", "NuevaClave9");
        autenticacion.cerrarSesion();

        assertThrows(AutenticacionException.class, () -> autenticacion.iniciarSesion("admin", PASSWORD_ADMIN));
        assertNotNull(autenticacion.iniciarSesion("admin", "NuevaClave9"));
    }

    @Test
    void losHashesAntiguosSeActualizanAlIniciarSesion() {
        entorno.administradorConectado();
        String hashAntes = hashDeAdmin();

        EntornoPrueba conMasIteraciones = new EntornoPrueba(carpeta, new PasswordHasher(2_000));
        conMasIteraciones.autenticacion.iniciarSesion("admin", PASSWORD_ADMIN);

        String hashDespues = hashDeAdmin();
        assertTrue(hashAntes.startsWith("pbkdf2_sha256$1000$"));
        assertTrue(hashDespues.startsWith("pbkdf2_sha256$2000$"));
    }

    @Test
    void losEventosQuedanEnElRegistroDeAccesos() {
        entorno.administradorConectado();
        assertThrows(AutenticacionException.class, () -> autenticacion.iniciarSesion("admin", "Incorrecta1"));
        autenticacion.cerrarSesion();

        List<EventoAcceso> eventos = entorno.database.conConexion(c -> entorno.registro.eventosDe(c, "admin"));
        assertEquals(List.of(EventoAcceso.CUENTA_CREADA, EventoAcceso.INICIO_SESION,
                EventoAcceso.CREDENCIALES_INVALIDAS, EventoAcceso.CIERRE_SESION), eventos);
    }

    private String hashDeAdmin() {
        return entorno.database.conConexion(c -> entorno.usuarios.buscarCredenciales(c, "admin"))
                .orElseThrow().passwordHash();
    }
}
