package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.exception.AutenticacionExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosUsuarioFGEM;
import com.businessanalyzer.model.EventoAccesoFGEM;
import com.businessanalyzer.model.RolFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.seguridad.PasswordHasherFGEM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static com.businessanalyzer.EntornoPruebaFGEM.PASSWORD_ADMIN_FGEM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutenticacionServiceTestFGEM {

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private AutenticacionServiceFGEM autenticacionFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        autenticacionFGEM = entornoFGEM.autenticacionFGEM;
    }

    @Test
    void elPrimerArranqueRequiereCrearElAdministradorFGEM() {
        assertTrue(autenticacionFGEM.requiereConfiguracionInicialFGEM());

        UsuarioFGEM adminFGEM = autenticacionFGEM.crearAdministradorInicialFGEM(
                new DatosUsuarioFGEM("  admin ", "Administrador", "", 0), PASSWORD_ADMIN_FGEM, PASSWORD_ADMIN_FGEM);

        assertFalse(autenticacionFGEM.requiereConfiguracionInicialFGEM());
        assertEquals("admin", adminFGEM.nombreUsuarioFGEM());
        assertEquals(RolFGEM.ADMINISTRADOR_FGEM, adminFGEM.rolFGEM().codigoFGEM());
        assertFalse(adminFGEM.debeCambiarPasswordFGEM());
    }

    @Test
    void noSePuedeCrearUnSegundoAdministradorInicialFGEM() {
        entornoFGEM.administradorConectadoFGEM();

        assertThrows(ValidacionExceptionFGEM.class, () -> autenticacionFGEM.crearAdministradorInicialFGEM(
                new DatosUsuarioFGEM("otro", "Otro admin", null, 0), "Clave12345", "Clave12345"));
    }

    @Test
    void inicioDeSesionCorrectoAbreLaSesionFGEM() {
        entornoFGEM.administradorConectadoFGEM();
        autenticacionFGEM.cerrarSesionFGEM();
        assertFalse(entornoFGEM.sesionFGEM.activaFGEM());

        UsuarioFGEM usuarioFGEM = autenticacionFGEM.iniciarSesionFGEM("ADMIN", PASSWORD_ADMIN_FGEM);

        assertTrue(entornoFGEM.sesionFGEM.activaFGEM(), "La sesión debe quedar abierta");
        assertEquals("admin", usuarioFGEM.nombreUsuarioFGEM(), "El usuario no distingue mayúsculas");
        assertNotNull(usuarioFGEM.ultimoAccesoFGEM());
    }

    @Test
    void usuarioInexistenteYContrasenaErroneaDanElMismoMensajeFGEM() {
        entornoFGEM.administradorConectadoFGEM();

        AutenticacionExceptionFGEM inexistenteFGEM = assertThrows(AutenticacionExceptionFGEM.class,
                () -> autenticacionFGEM.iniciarSesionFGEM("nadie", "Clave12345"));
        AutenticacionExceptionFGEM erroneaFGEM = assertThrows(AutenticacionExceptionFGEM.class,
                () -> autenticacionFGEM.iniciarSesionFGEM("admin", "Incorrecta1"));

        assertEquals(inexistenteFGEM.getMessage(), erroneaFGEM.getMessage());
    }

    @Test
    void camposVaciosSeRechazanFGEM() {
        assertThrows(AutenticacionExceptionFGEM.class, () -> autenticacionFGEM.iniciarSesionFGEM("", "x"));
        assertThrows(AutenticacionExceptionFGEM.class, () -> autenticacionFGEM.iniciarSesionFGEM("admin", ""));
        assertThrows(AutenticacionExceptionFGEM.class, () -> autenticacionFGEM.iniciarSesionFGEM(null, null));
    }

    @Test
    void cincoIntentosFallidosBloqueanLaCuentaQuinceMinutosFGEM() {
        entornoFGEM.administradorConectadoFGEM();
        autenticacionFGEM.cerrarSesionFGEM();

        for (int iFGEM = 0; iFGEM < AutenticacionServiceFGEM.MAXIMO_INTENTOS_FGEM; iFGEM++) {
            assertThrows(AutenticacionExceptionFGEM.class, () -> autenticacionFGEM.iniciarSesionFGEM("admin", "Incorrecta1"));
        }

        AutenticacionExceptionFGEM bloqueadaFGEM = assertThrows(AutenticacionExceptionFGEM.class,
                () -> autenticacionFGEM.iniciarSesionFGEM("admin", PASSWORD_ADMIN_FGEM));
        assertTrue(bloqueadaFGEM.getMessage().contains("bloqueada"),
                "Con la cuenta bloqueada ni la contraseña correcta permite entrar");

        entornoFGEM.relojFGEM.avanzarFGEM(AutenticacionServiceFGEM.DURACION_BLOQUEO_FGEM.plus(Duration.ofSeconds(1)));
        UsuarioFGEM usuarioFGEM = autenticacionFGEM.iniciarSesionFGEM("admin", PASSWORD_ADMIN_FGEM);
        assertEquals(0, usuarioFGEM.intentosFallidosFGEM());
    }

    @Test
    void unAccesoCorrectoReiniciaElContadorDeFallosFGEM() {
        entornoFGEM.administradorConectadoFGEM();
        for (int iFGEM = 0; iFGEM < AutenticacionServiceFGEM.MAXIMO_INTENTOS_FGEM - 1; iFGEM++) {
            assertThrows(AutenticacionExceptionFGEM.class, () -> autenticacionFGEM.iniciarSesionFGEM("admin", "Incorrecta1"));
        }
        autenticacionFGEM.iniciarSesionFGEM("admin", PASSWORD_ADMIN_FGEM);

        assertThrows(AutenticacionExceptionFGEM.class, () -> autenticacionFGEM.iniciarSesionFGEM("admin", "Incorrecta1"));
        UsuarioFGEM usuarioFGEM = autenticacionFGEM.iniciarSesionFGEM("admin", PASSWORD_ADMIN_FGEM);
        assertFalse(usuarioFGEM.bloqueadoEnFGEM(entornoFGEM.relojFGEM.instant().atZone(entornoFGEM.relojFGEM.getZone()).toLocalDateTime()));
    }

    @Test
    void unaCuentaDesactivadaNoPuedeIngresarFGEM() {
        entornoFGEM.administradorConectadoFGEM();
        UsuarioFGEM operadorFGEM = entornoFGEM.usuarioServiceFGEM.crearFGEM(
                new DatosUsuarioFGEM("operador", "Operador Uno", null, entornoFGEM.idRolFGEM("OPERADOR")),
                "Temporal123", "Temporal123");
        entornoFGEM.usuarioServiceFGEM.cambiarEstadoFGEM(operadorFGEM.idFGEM(), false);

        AutenticacionExceptionFGEM errorFGEM = assertThrows(AutenticacionExceptionFGEM.class,
                () -> autenticacionFGEM.iniciarSesionFGEM("operador", "Temporal123"));
        assertTrue(errorFGEM.getMessage().contains("desactivada"));
    }

    @Test
    void cambiarLaPropiaContrasenaExigeLaActualFGEM() {
        entornoFGEM.administradorConectadoFGEM();

        assertThrows(ValidacionExceptionFGEM.class,
                () -> autenticacionFGEM.cambiarPasswordPropiaFGEM("Incorrecta1", "NuevaClave9", "NuevaClave9"));
        assertThrows(ValidacionExceptionFGEM.class,
                () -> autenticacionFGEM.cambiarPasswordPropiaFGEM(PASSWORD_ADMIN_FGEM, PASSWORD_ADMIN_FGEM, PASSWORD_ADMIN_FGEM));

        autenticacionFGEM.cambiarPasswordPropiaFGEM(PASSWORD_ADMIN_FGEM, "NuevaClave9", "NuevaClave9");
        autenticacionFGEM.cerrarSesionFGEM();

        assertThrows(AutenticacionExceptionFGEM.class, () -> autenticacionFGEM.iniciarSesionFGEM("admin", PASSWORD_ADMIN_FGEM));
        assertNotNull(autenticacionFGEM.iniciarSesionFGEM("admin", "NuevaClave9"));
    }

    @Test
    void losHashesAntiguosSeActualizanAlIniciarSesionFGEM() {
        entornoFGEM.administradorConectadoFGEM();
        String hashAntesFGEM = hashDeAdminFGEM();

        EntornoPruebaFGEM conMasIteracionesFGEM = new EntornoPruebaFGEM(carpetaFGEM, new PasswordHasherFGEM(2_000));
        conMasIteracionesFGEM.autenticacionFGEM.iniciarSesionFGEM("admin", PASSWORD_ADMIN_FGEM);

        String hashDespuesFGEM = hashDeAdminFGEM();
        assertTrue(hashAntesFGEM.startsWith("pbkdf2_sha256$1000$"));
        assertTrue(hashDespuesFGEM.startsWith("pbkdf2_sha256$2000$"));
    }

    @Test
    void losEventosQuedanEnElRegistroDeAccesosFGEM() {
        entornoFGEM.administradorConectadoFGEM();
        assertThrows(AutenticacionExceptionFGEM.class, () -> autenticacionFGEM.iniciarSesionFGEM("admin", "Incorrecta1"));
        autenticacionFGEM.cerrarSesionFGEM();

        List<EventoAccesoFGEM> eventosFGEM = entornoFGEM.databaseFGEM.conConexionFGEM(cFGEM -> entornoFGEM.registroFGEM.eventosDeFGEM(cFGEM, "admin"));
        assertEquals(List.of(EventoAccesoFGEM.CUENTA_CREADA, EventoAccesoFGEM.INICIO_SESION,
                EventoAccesoFGEM.CREDENCIALES_INVALIDAS, EventoAccesoFGEM.CIERRE_SESION), eventosFGEM);
    }

    private String hashDeAdminFGEM() {
        return entornoFGEM.databaseFGEM.conConexionFGEM(cFGEM -> entornoFGEM.usuariosFGEM.buscarCredencialesFGEM(cFGEM, "admin"))
                .orElseThrow().passwordHashFGEM();
    }
}
