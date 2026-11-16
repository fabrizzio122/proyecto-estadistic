package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.service.PreferenciasService.PerfilRecordado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreferenciasServiceTest {

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private PreferenciasService servicio;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = entorno.preferencias;
        entorno.administradorConectado();
    }

    @Test
    void cadaUsuarioGuardaSuColorYElEquipoRecuerdaElUltimo() {
        assertEquals(Optional.empty(), servicio.colorDelUsuario());
        assertEquals(Optional.empty(), servicio.ultimoColor());

        servicio.guardarColorDelUsuario("lima");
        assertEquals(Optional.of("LIMA"), servicio.colorDelUsuario());
        assertEquals(Optional.of("LIMA"), servicio.ultimoColor());

        // Otro usuario tiene su propio color.
        entorno.crearUsuario("ana", "ANALISTA");
        entorno.entrarComo("ana", "Temporal123");
        assertEquals(Optional.empty(), servicio.colorDelUsuario());
        servicio.guardarColorDelUsuario("CORAL");
        entorno.entrarComo("admin", EntornoPrueba.PASSWORD_ADMIN);
        assertEquals(Optional.of("LIMA"), servicio.colorDelUsuario());
        assertEquals(Optional.of("CORAL"), servicio.ultimoColor(), "El último color usado en el equipo");
    }

    @Test
    void soloAceptaLosCuatroColores() {
        assertThrows(ValidacionException.class, () -> servicio.guardarColorDelUsuario("ROSADO"));
        assertThrows(ValidacionException.class, () -> servicio.guardarUltimoColor(null));
        servicio.guardarUltimoColor("celeste");
        assertEquals(Optional.of("CELESTE"), servicio.ultimoColor());
    }

    @Test
    void soloSeMuestranLosUsuariosRecordadosConSuEmpresa() {
        assertTrue(servicio.recordados().isEmpty(), "Nadie aparece si nadie pidió ser recordado");

        Empresa bodega = entorno.crearEmpresaActiva("Bodega San Juan");
        servicio.guardarColorDelUsuario("LIMA");
        servicio.recordarUsuarioActual();

        entorno.crearUsuario("ana", "ANALISTA");
        entorno.entrarComo("ana", "Temporal123");
        servicio.recordarUsuarioActual();

        List<PerfilRecordado> perfiles = servicio.recordados();
        assertEquals(List.of("ana", "admin"), perfiles.stream().map(PerfilRecordado::nombreUsuario).toList(),
                "El último que entró va primero");
        PerfilRecordado admin = perfiles.get(1);
        assertEquals("Administrador General", admin.nombreCompleto());
        assertEquals("AG", admin.iniciales());
        assertEquals(bodega.nombreVisible(), admin.empresa());
        assertEquals("LIMA", admin.color());
        assertNull(perfiles.get(0).empresa());

        servicio.olvidar("ANA");
        assertEquals(List.of("admin"), servicio.recordados().stream().map(PerfilRecordado::nombreUsuario).toList());
    }

    @Test
    void unaCuentaDesactivadaDejaDeAparecer() {
        var ana = entorno.crearUsuario("ana", "ANALISTA");
        entorno.entrarComo("ana", "Temporal123");
        servicio.recordarUsuarioActual();
        entorno.entrarComo("admin", EntornoPrueba.PASSWORD_ADMIN);
        entorno.usuarioService.cambiarEstado(ana.id(), false);

        assertTrue(servicio.recordados().isEmpty());
    }

    @Test
    void recuerdaComoMaximoCuatroUsuarios() {
        for (String nombre : List.of("usr1", "usr2", "usr3", "usr4", "usr5")) {
            entorno.crearUsuario(nombre, "OPERADOR");
        }
        for (String nombre : List.of("usr1", "usr2", "usr3", "usr4", "usr5")) {
            entorno.entrarComo(nombre, "Temporal123");
            servicio.recordarUsuarioActual();
        }
        assertEquals(List.of("usr5", "usr4", "usr3", "usr2"),
                servicio.recordados().stream().map(PerfilRecordado::nombreUsuario).toList());
    }

    @Test
    void laOpcionDeReducirAnimacionesSeGuarda() {
        assertFalse(servicio.animacionesReducidas());
        servicio.reducirAnimaciones(true);
        assertTrue(servicio.animacionesReducidas());
    }
}
