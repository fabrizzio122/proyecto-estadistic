package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.service.PreferenciasServiceFGEM.PerfilRecordadoFGEM;
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

class PreferenciasServiceTestFGEM {

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private PreferenciasServiceFGEM servicioFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = entornoFGEM.preferenciasFGEM;
        entornoFGEM.administradorConectadoFGEM();
    }

    @Test
    void cadaUsuarioGuardaSuColorYElEquipoRecuerdaElUltimoFGEM() {
        assertEquals(Optional.empty(), servicioFGEM.colorDelUsuarioFGEM());
        assertEquals(Optional.empty(), servicioFGEM.ultimoColorFGEM());

        servicioFGEM.guardarColorDelUsuarioFGEM("lima");
        assertEquals(Optional.of("LIMA"), servicioFGEM.colorDelUsuarioFGEM());
        assertEquals(Optional.of("LIMA"), servicioFGEM.ultimoColorFGEM());

        // Otro usuario tiene su propio color.
        entornoFGEM.crearUsuarioFGEM("ana", "ANALISTA");
        entornoFGEM.entrarComoFGEM("ana", "Temporal123");
        assertEquals(Optional.empty(), servicioFGEM.colorDelUsuarioFGEM());
        servicioFGEM.guardarColorDelUsuarioFGEM("CORAL");
        entornoFGEM.entrarComoFGEM("admin", EntornoPruebaFGEM.PASSWORD_ADMIN_FGEM);
        assertEquals(Optional.of("LIMA"), servicioFGEM.colorDelUsuarioFGEM());
        assertEquals(Optional.of("CORAL"), servicioFGEM.ultimoColorFGEM(), "El último color usado en el equipo");
    }

    @Test
    void soloAceptaLosCuatroColoresFGEM() {
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.guardarColorDelUsuarioFGEM("ROSADO"));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.guardarUltimoColorFGEM(null));
        servicioFGEM.guardarUltimoColorFGEM("celeste");
        assertEquals(Optional.of("CELESTE"), servicioFGEM.ultimoColorFGEM());
    }

    @Test
    void soloSeMuestranLosUsuariosRecordadosConSuEmpresaFGEM() {
        assertTrue(servicioFGEM.recordadosFGEM().isEmpty(), "Nadie aparece si nadie pidió ser recordado");

        EmpresaFGEM bodegaFGEM = entornoFGEM.crearEmpresaActivaFGEM("Bodega San Juan");
        servicioFGEM.guardarColorDelUsuarioFGEM("LIMA");
        servicioFGEM.recordarUsuarioActualFGEM();

        entornoFGEM.crearUsuarioFGEM("ana", "ANALISTA");
        entornoFGEM.entrarComoFGEM("ana", "Temporal123");
        servicioFGEM.recordarUsuarioActualFGEM();

        List<PerfilRecordadoFGEM> perfilesFGEM = servicioFGEM.recordadosFGEM();
        assertEquals(List.of("ana", "admin"), perfilesFGEM.stream().map(PerfilRecordadoFGEM::nombreUsuarioFGEM).toList(),
                "El último que entró va primero");
        PerfilRecordadoFGEM adminFGEM = perfilesFGEM.get(1);
        assertEquals("Administrador General", adminFGEM.nombreCompletoFGEM());
        assertEquals("AG", adminFGEM.inicialesFGEM());
        assertEquals(bodegaFGEM.nombreVisibleFGEM(), adminFGEM.empresaFGEM());
        assertEquals("LIMA", adminFGEM.colorFGEM());
        assertNull(perfilesFGEM.get(0).empresaFGEM());

        servicioFGEM.olvidarFGEM("ANA");
        assertEquals(List.of("admin"), servicioFGEM.recordadosFGEM().stream().map(PerfilRecordadoFGEM::nombreUsuarioFGEM).toList());
    }

    @Test
    void unaCuentaDesactivadaDejaDeAparecerFGEM() {
        var anaFGEM = entornoFGEM.crearUsuarioFGEM("ana", "ANALISTA");
        entornoFGEM.entrarComoFGEM("ana", "Temporal123");
        servicioFGEM.recordarUsuarioActualFGEM();
        entornoFGEM.entrarComoFGEM("admin", EntornoPruebaFGEM.PASSWORD_ADMIN_FGEM);
        entornoFGEM.usuarioServiceFGEM.cambiarEstadoFGEM(anaFGEM.idFGEM(), false);

        assertTrue(servicioFGEM.recordadosFGEM().isEmpty());
    }

    @Test
    void recuerdaComoMaximoCuatroUsuariosFGEM() {
        for (String nombreFGEM : List.of("usr1", "usr2", "usr3", "usr4", "usr5")) {
            entornoFGEM.crearUsuarioFGEM(nombreFGEM, "OPERADOR");
        }
        for (String nombreFGEM : List.of("usr1", "usr2", "usr3", "usr4", "usr5")) {
            entornoFGEM.entrarComoFGEM(nombreFGEM, "Temporal123");
            servicioFGEM.recordarUsuarioActualFGEM();
        }
        assertEquals(List.of("usr5", "usr4", "usr3", "usr2"),
                servicioFGEM.recordadosFGEM().stream().map(PerfilRecordadoFGEM::nombreUsuarioFGEM).toList());
    }

    @Test
    void laOpcionDeReducirAnimacionesSeGuardaFGEM() {
        assertFalse(servicioFGEM.animacionesReducidasFGEM());
        servicioFGEM.reducirAnimacionesFGEM(true);
        assertTrue(servicioFGEM.animacionesReducidasFGEM());
    }
}
