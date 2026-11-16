package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.AutenticacionExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosUsuarioFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioServiceTestFGEM {

    private static final String CLAVE_FGEM = "Temporal123";

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private UsuarioServiceFGEM servicioFGEM;
    private UsuarioFGEM adminFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = entornoFGEM.usuarioServiceFGEM;
        adminFGEM = entornoFGEM.administradorConectadoFGEM();
    }

    @Test
    void creaUnUsuarioQueDebeCambiarSuContrasenaFGEM() {
        UsuarioFGEM creadoFGEM = crearFGEM("maria.lopez", "GERENTE");

        assertEquals("Gerente", creadoFGEM.rolFGEM().nombreFGEM());
        assertTrue(creadoFGEM.activoFGEM());
        assertTrue(creadoFGEM.debeCambiarPasswordFGEM());
        assertEquals(2, servicioFGEM.listarFGEM().size());
    }

    @Test
    void validaLosDatosDelUsuarioFGEM() {
        long rolFGEM = entornoFGEM.idRolFGEM("OPERADOR");
        assertThrows(ValidacionExceptionFGEM.class,
                () -> servicioFGEM.crearFGEM(new DatosUsuarioFGEM("con espacio", "Nombre", null, rolFGEM), CLAVE_FGEM, CLAVE_FGEM));
        assertThrows(ValidacionExceptionFGEM.class,
                () -> servicioFGEM.crearFGEM(new DatosUsuarioFGEM("ab", "Nombre", null, rolFGEM), CLAVE_FGEM, CLAVE_FGEM));
        assertThrows(ValidacionExceptionFGEM.class,
                () -> servicioFGEM.crearFGEM(new DatosUsuarioFGEM("valido", "", null, rolFGEM), CLAVE_FGEM, CLAVE_FGEM));
        assertThrows(ValidacionExceptionFGEM.class,
                () -> servicioFGEM.crearFGEM(new DatosUsuarioFGEM("valido", "Nombre", "no-es-correo", rolFGEM), CLAVE_FGEM, CLAVE_FGEM));
        assertThrows(ValidacionExceptionFGEM.class,
                () -> servicioFGEM.crearFGEM(new DatosUsuarioFGEM("valido", "Nombre", null, 999), CLAVE_FGEM, CLAVE_FGEM));
        assertThrows(ValidacionExceptionFGEM.class,
                () -> servicioFGEM.crearFGEM(new DatosUsuarioFGEM("valido", "Nombre", null, rolFGEM), "corta", "corta"));
    }

    @Test
    void elNombreDeUsuarioEsUnicoSinImportarMayusculasFGEM() {
        crearFGEM("maria", "OPERADOR");
        assertThrows(ValidacionExceptionFGEM.class, () -> crearFGEM("MARIA", "OPERADOR"));
    }

    @Test
    void sinPermisoNoSePuedeAdministrarUsuariosFGEM() {
        UsuarioFGEM operadorFGEM = crearFGEM("operador", "OPERADOR");
        entornoFGEM.autenticacionFGEM.cerrarSesionFGEM();
        assertThrows(AccesoDenegadoExceptionFGEM.class, servicioFGEM::listarFGEM);

        entornoFGEM.autenticacionFGEM.iniciarSesionFGEM(operadorFGEM.nombreUsuarioFGEM(), CLAVE_FGEM);
        assertThrows(AccesoDenegadoExceptionFGEM.class, servicioFGEM::listarFGEM);
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> crearFGEM("intruso", "ADMINISTRADOR"));
    }

    @Test
    void protegeAlUltimoAdministradorFGEM() {
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.cambiarEstadoFGEM(adminFGEM.idFGEM(), false),
                "No puede desactivarse a sí mismo");
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.actualizarFGEM(adminFGEM.idFGEM(),
                        new DatosUsuarioFGEM("admin", "Administrador", null, entornoFGEM.idRolFGEM("ANALISTA"))),
                "No puede quitarse el rol de administrador");

        UsuarioFGEM segundoFGEM = crearFGEM("admin2", "ADMINISTRADOR");
        servicioFGEM.cambiarEstadoFGEM(segundoFGEM.idFGEM(), false);
        UsuarioFGEM reactivadoFGEM = servicioFGEM.cambiarEstadoFGEM(segundoFGEM.idFGEM(), true);
        assertTrue(reactivadoFGEM.activoFGEM());

        UsuarioFGEM degradadoFGEM = servicioFGEM.actualizarFGEM(segundoFGEM.idFGEM(),
                new DatosUsuarioFGEM("admin2", "Segundo admin", null, entornoFGEM.idRolFGEM("ANALISTA")));
        assertEquals("Analista", degradadoFGEM.rolFGEM().nombreFGEM());
    }

    @Test
    void editarActualizaLosDatosYLaSesionPropiaFGEM() {
        UsuarioFGEM actualizadoFGEM = servicioFGEM.actualizarFGEM(adminFGEM.idFGEM(),
                new DatosUsuarioFGEM("admin", "Nombre Corregido", "nuevo@empresa.com", entornoFGEM.idRolFGEM("ADMINISTRADOR")));

        assertEquals("Nombre Corregido", actualizadoFGEM.nombreCompletoFGEM());
        assertEquals("Nombre Corregido", entornoFGEM.sesionFGEM.usuarioFGEM().nombreCompletoFGEM());
    }

    @Test
    void restablecerContrasenaDesbloqueaYObligaACambiarlaFGEM() {
        UsuarioFGEM operadorFGEM = crearFGEM("operador", "OPERADOR");
        entornoFGEM.autenticacionFGEM.cerrarSesionFGEM();
        for (int iFGEM = 0; iFGEM < AutenticacionServiceFGEM.MAXIMO_INTENTOS_FGEM; iFGEM++) {
            assertThrows(AutenticacionExceptionFGEM.class,
                    () -> entornoFGEM.autenticacionFGEM.iniciarSesionFGEM("operador", "Incorrecta1"));
        }
        entornoFGEM.autenticacionFGEM.iniciarSesionFGEM("admin", EntornoPruebaFGEM.PASSWORD_ADMIN_FGEM);

        servicioFGEM.restablecerPasswordFGEM(operadorFGEM.idFGEM(), "NuevaTemp45", "NuevaTemp45");
        entornoFGEM.autenticacionFGEM.cerrarSesionFGEM();

        UsuarioFGEM conectadoFGEM = entornoFGEM.autenticacionFGEM.iniciarSesionFGEM("operador", "NuevaTemp45");
        assertTrue(conectadoFGEM.debeCambiarPasswordFGEM());
        assertNull(conectadoFGEM.bloqueadoHastaFGEM());
    }

    @Test
    void desbloquearPermiteVolverAIngresarFGEM() {
        UsuarioFGEM operadorFGEM = crearFGEM("operador", "OPERADOR");
        entornoFGEM.autenticacionFGEM.cerrarSesionFGEM();
        for (int iFGEM = 0; iFGEM < AutenticacionServiceFGEM.MAXIMO_INTENTOS_FGEM; iFGEM++) {
            assertThrows(AutenticacionExceptionFGEM.class,
                    () -> entornoFGEM.autenticacionFGEM.iniciarSesionFGEM("operador", "Incorrecta1"));
        }
        entornoFGEM.autenticacionFGEM.iniciarSesionFGEM("admin", EntornoPruebaFGEM.PASSWORD_ADMIN_FGEM);

        UsuarioFGEM desbloqueadoFGEM = servicioFGEM.desbloquearFGEM(operadorFGEM.idFGEM());

        assertNull(desbloqueadoFGEM.bloqueadoHastaFGEM());
        assertFalse(servicioFGEM.listarAccesosFGEM().isEmpty());
    }

    @Test
    void elAdministradorNoRestableceSuPropiaContrasenaFGEM() {
        assertThrows(ValidacionExceptionFGEM.class,
                () -> servicioFGEM.restablecerPasswordFGEM(adminFGEM.idFGEM(), "NuevaTemp45", "NuevaTemp45"));
    }

    private UsuarioFGEM crearFGEM(String nombreUsuarioFGEM, String codigoRolFGEM) {
        return servicioFGEM.crearFGEM(new DatosUsuarioFGEM(nombreUsuarioFGEM, "Usuario " + nombreUsuarioFGEM, null,
                entornoFGEM.idRolFGEM(codigoRolFGEM)), CLAVE_FGEM, CLAVE_FGEM);
    }
}
