package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.AutenticacionException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosUsuario;
import com.businessanalyzer.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioServiceTest {

    private static final String CLAVE = "Temporal123";

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private UsuarioService servicio;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = entorno.usuarioService;
        admin = entorno.administradorConectado();
    }

    @Test
    void creaUnUsuarioQueDebeCambiarSuContrasena() {
        Usuario creado = crear("maria.lopez", "GERENTE");

        assertEquals("Gerente", creado.rol().nombre());
        assertTrue(creado.activo());
        assertTrue(creado.debeCambiarPassword());
        assertEquals(2, servicio.listar().size());
    }

    @Test
    void validaLosDatosDelUsuario() {
        long rol = entorno.idRol("OPERADOR");
        assertThrows(ValidacionException.class,
                () -> servicio.crear(new DatosUsuario("con espacio", "Nombre", null, rol), CLAVE, CLAVE));
        assertThrows(ValidacionException.class,
                () -> servicio.crear(new DatosUsuario("ab", "Nombre", null, rol), CLAVE, CLAVE));
        assertThrows(ValidacionException.class,
                () -> servicio.crear(new DatosUsuario("valido", "", null, rol), CLAVE, CLAVE));
        assertThrows(ValidacionException.class,
                () -> servicio.crear(new DatosUsuario("valido", "Nombre", "no-es-correo", rol), CLAVE, CLAVE));
        assertThrows(ValidacionException.class,
                () -> servicio.crear(new DatosUsuario("valido", "Nombre", null, 999), CLAVE, CLAVE));
        assertThrows(ValidacionException.class,
                () -> servicio.crear(new DatosUsuario("valido", "Nombre", null, rol), "corta", "corta"));
    }

    @Test
    void elNombreDeUsuarioEsUnicoSinImportarMayusculas() {
        crear("maria", "OPERADOR");
        assertThrows(ValidacionException.class, () -> crear("MARIA", "OPERADOR"));
    }

    @Test
    void sinPermisoNoSePuedeAdministrarUsuarios() {
        Usuario operador = crear("operador", "OPERADOR");
        entorno.autenticacion.cerrarSesion();
        assertThrows(AccesoDenegadoException.class, servicio::listar);

        entorno.autenticacion.iniciarSesion(operador.nombreUsuario(), CLAVE);
        assertThrows(AccesoDenegadoException.class, servicio::listar);
        assertThrows(AccesoDenegadoException.class, () -> crear("intruso", "ADMINISTRADOR"));
    }

    @Test
    void protegeAlUltimoAdministrador() {
        assertThrows(ValidacionException.class, () -> servicio.cambiarEstado(admin.id(), false),
                "No puede desactivarse a sí mismo");
        assertThrows(ValidacionException.class, () -> servicio.actualizar(admin.id(),
                        new DatosUsuario("admin", "Administrador", null, entorno.idRol("ANALISTA"))),
                "No puede quitarse el rol de administrador");

        Usuario segundo = crear("admin2", "ADMINISTRADOR");
        servicio.cambiarEstado(segundo.id(), false);
        Usuario reactivado = servicio.cambiarEstado(segundo.id(), true);
        assertTrue(reactivado.activo());

        Usuario degradado = servicio.actualizar(segundo.id(),
                new DatosUsuario("admin2", "Segundo admin", null, entorno.idRol("ANALISTA")));
        assertEquals("Analista", degradado.rol().nombre());
    }

    @Test
    void editarActualizaLosDatosYLaSesionPropia() {
        Usuario actualizado = servicio.actualizar(admin.id(),
                new DatosUsuario("admin", "Nombre Corregido", "nuevo@empresa.com", entorno.idRol("ADMINISTRADOR")));

        assertEquals("Nombre Corregido", actualizado.nombreCompleto());
        assertEquals("Nombre Corregido", entorno.sesion.usuario().nombreCompleto());
    }

    @Test
    void restablecerContrasenaDesbloqueaYObligaACambiarla() {
        Usuario operador = crear("operador", "OPERADOR");
        entorno.autenticacion.cerrarSesion();
        for (int i = 0; i < AutenticacionService.MAXIMO_INTENTOS; i++) {
            assertThrows(AutenticacionException.class,
                    () -> entorno.autenticacion.iniciarSesion("operador", "Incorrecta1"));
        }
        entorno.autenticacion.iniciarSesion("admin", EntornoPrueba.PASSWORD_ADMIN);

        servicio.restablecerPassword(operador.id(), "NuevaTemp45", "NuevaTemp45");
        entorno.autenticacion.cerrarSesion();

        Usuario conectado = entorno.autenticacion.iniciarSesion("operador", "NuevaTemp45");
        assertTrue(conectado.debeCambiarPassword());
        assertNull(conectado.bloqueadoHasta());
    }

    @Test
    void desbloquearPermiteVolverAIngresar() {
        Usuario operador = crear("operador", "OPERADOR");
        entorno.autenticacion.cerrarSesion();
        for (int i = 0; i < AutenticacionService.MAXIMO_INTENTOS; i++) {
            assertThrows(AutenticacionException.class,
                    () -> entorno.autenticacion.iniciarSesion("operador", "Incorrecta1"));
        }
        entorno.autenticacion.iniciarSesion("admin", EntornoPrueba.PASSWORD_ADMIN);

        Usuario desbloqueado = servicio.desbloquear(operador.id());

        assertNull(desbloqueado.bloqueadoHasta());
        assertFalse(servicio.listarAccesos().isEmpty());
    }

    @Test
    void elAdministradorNoRestableceSuPropiaContrasena() {
        assertThrows(ValidacionException.class,
                () -> servicio.restablecerPassword(admin.id(), "NuevaTemp45", "NuevaTemp45"));
    }

    private Usuario crear(String nombreUsuario, String codigoRol) {
        return servicio.crear(new DatosUsuario(nombreUsuario, "Usuario " + nombreUsuario, null,
                entorno.idRol(codigoRol)), CLAVE, CLAVE);
    }
}
