package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosEmpresaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.TamanoEmpresaFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmpresaServiceTestFGEM {

    private static final String CLAVE_FGEM = "Temporal123";

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private EmpresaServiceFGEM servicioFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = entornoFGEM.empresaServiceFGEM;
        entornoFGEM.administradorConectadoFGEM();
    }

    @Test
    void registraUnaEmpresaNormalizandoSusDatosFGEM() {
        EmpresaFGEM empresaFGEM = servicioFGEM.crearFGEM(new DatosEmpresaFGEM("  Comercial Andina S.A.C. ", "", "20601234567",
                SectorFGEM.COMERCIO, TamanoEmpresaFGEM.PEQUENA, MonedaFGEM.PEN, null, LocalDate.of(2020, 3, 1),
                " ", null, "ventas@andina.pe"));

        assertEquals("Comercial Andina S.A.C.", empresaFGEM.razonSocialFGEM());
        assertNull(empresaFGEM.nombreComercialFGEM());
        assertEquals("Comercial Andina S.A.C.", empresaFGEM.nombreVisibleFGEM(), "Sin nombre comercial usa la razón social");
        assertEquals(1, empresaFGEM.mesInicioEjercicioFGEM(), "El ejercicio fiscal empieza en enero por defecto");
        assertNull(empresaFGEM.direccionFGEM());
        assertTrue(empresaFGEM.activaFGEM());
    }

    @Test
    void validaLosDatosDeLaEmpresaFGEM() {
        assertInvalidaFGEM(datosFGEM("ab", "20601234567"));
        assertInvalidaFGEM(datosFGEM("Empresa válida", "123"));
        assertInvalidaFGEM(datosFGEM("Empresa válida", "RUC CON ESPACIOS"));
        assertInvalidaFGEM(new DatosEmpresaFGEM("Empresa", null, null, null, TamanoEmpresaFGEM.MICRO, MonedaFGEM.PEN,
                1, null, null, null, null));
        assertInvalidaFGEM(new DatosEmpresaFGEM("Empresa", null, null, SectorFGEM.SERVICIOS, TamanoEmpresaFGEM.MICRO, MonedaFGEM.PEN,
                13, null, null, null, null));
        assertInvalidaFGEM(new DatosEmpresaFGEM("Empresa", null, null, SectorFGEM.SERVICIOS, TamanoEmpresaFGEM.MICRO, MonedaFGEM.PEN,
                1, LocalDate.of(2030, 1, 1), null, null, null));
        assertInvalidaFGEM(new DatosEmpresaFGEM("Empresa", null, null, SectorFGEM.SERVICIOS, TamanoEmpresaFGEM.MICRO, MonedaFGEM.PEN,
                1, null, null, "abc", null));
        assertInvalidaFGEM(new DatosEmpresaFGEM("Empresa", null, null, SectorFGEM.SERVICIOS, TamanoEmpresaFGEM.MICRO, MonedaFGEM.PEN,
                1, null, null, null, "correo-invalido"));
    }

    @Test
    void elRucNoSePuedeRepetirFGEM() {
        servicioFGEM.crearFGEM(datosFGEM("Primera empresa", "20601234567"));
        EmpresaFGEM segundaFGEM = servicioFGEM.crearFGEM(datosFGEM("Segunda empresa", "ab-12345"));

        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.crearFGEM(datosFGEM("Copia", "20601234567")));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.crearFGEM(datosFGEM("Copia", "AB-12345")),
                "La comparación no distingue mayúsculas");
        assertEquals("AB-12345", segundaFGEM.identificacionFiscalFGEM(), "Se guarda en mayúsculas");
        // Sin RUC se pueden registrar varias empresas
        servicioFGEM.crearFGEM(datosFGEM("Sin RUC uno", null));
        servicioFGEM.crearFGEM(datosFGEM("Sin RUC dos", null));
    }

    @Test
    void elAdministradorVeTodasYLosDemasSoloLasAsignadasFGEM() {
        EmpresaFGEM aFGEM = servicioFGEM.crearFGEM(datosFGEM("Empresa A", null));
        EmpresaFGEM bFGEM = servicioFGEM.crearFGEM(datosFGEM("Empresa B", null));
        UsuarioFGEM gerenteFGEM = entornoFGEM.crearUsuarioFGEM("gerente", "GERENTE");
        servicioFGEM.asignarAccesosFGEM(aFGEM.idFGEM(), Set.of(gerenteFGEM.idFGEM()));

        assertEquals(2, servicioFGEM.listarFGEM(false).size());

        entornoFGEM.entrarComoFGEM("gerente", CLAVE_FGEM);
        List<EmpresaFGEM> visiblesFGEM = servicioFGEM.listarFGEM(false);
        assertEquals(List.of(aFGEM.idFGEM()), visiblesFGEM.stream().map(EmpresaFGEM::idFGEM).toList());
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.seleccionarEmpresaActivaFGEM(bFGEM.idFGEM()));
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.actualizarFGEM(bFGEM.idFGEM(), datosFGEM("Cambio", null)));
    }

    @Test
    void quienRegistraUnaEmpresaQuedaAsignadoAEllaFGEM() {
        entornoFGEM.crearUsuarioFGEM("gerente", "GERENTE");
        entornoFGEM.entrarComoFGEM("gerente", CLAVE_FGEM);

        EmpresaFGEM propiaFGEM = servicioFGEM.crearFGEM(datosFGEM("Empresa del gerente", null));

        assertEquals(List.of(propiaFGEM.idFGEM()), servicioFGEM.listarFGEM(false).stream().map(EmpresaFGEM::idFGEM).toList());
    }

    @Test
    void losPermisosDelRolSeRespetanFGEM() {
        EmpresaFGEM empresaFGEM = servicioFGEM.crearFGEM(datosFGEM("Empresa", null));
        UsuarioFGEM analistaFGEM = entornoFGEM.crearUsuarioFGEM("analista", "ANALISTA");
        servicioFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of(analistaFGEM.idFGEM()));

        entornoFGEM.entrarComoFGEM("analista", CLAVE_FGEM);
        assertEquals(1, servicioFGEM.listarFGEM(false).size(), "El analista puede ver");
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.crearFGEM(datosFGEM("Otra", null)));
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.cambiarEstadoFGEM(empresaFGEM.idFGEM(), false));
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of()),
                "Solo un administrador asigna accesos");
    }

    @Test
    void archivarLaEmpresaActivaLaQuitaDeLaSesionYDelSelectorFGEM() {
        EmpresaFGEM empresaFGEM = servicioFGEM.crearFGEM(datosFGEM("Empresa", null));
        servicioFGEM.seleccionarEmpresaActivaFGEM(empresaFGEM.idFGEM());
        List<Optional<EmpresaFGEM>> notificacionesFGEM = new ArrayList<>();
        entornoFGEM.sesionFGEM.alCambiarEmpresaFGEM(notificacionesFGEM::add);

        servicioFGEM.cambiarEstadoFGEM(empresaFGEM.idFGEM(), false);

        assertTrue(entornoFGEM.sesionFGEM.empresaActivaFGEM().isEmpty());
        assertEquals(List.of(Optional.empty()), notificacionesFGEM, "La ventana principal debe enterarse");
        assertTrue(servicioFGEM.listarParaSelectorFGEM().isEmpty());
        assertEquals(1, servicioFGEM.listarFGEM(true).size(), "Sigue existiendo como archivada");
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.seleccionarEmpresaActivaFGEM(empresaFGEM.idFGEM()));

        servicioFGEM.cambiarEstadoFGEM(empresaFGEM.idFGEM(), true);
        assertEquals(1, servicioFGEM.listarParaSelectorFGEM().size());
    }

    @Test
    void editarLaEmpresaActivaActualizaLaSesionFGEM() {
        EmpresaFGEM empresaFGEM = servicioFGEM.crearFGEM(datosFGEM("Nombre original", null));
        servicioFGEM.seleccionarEmpresaActivaFGEM(empresaFGEM.idFGEM());

        servicioFGEM.actualizarFGEM(empresaFGEM.idFGEM(), datosFGEM("Nombre corregido", null));

        assertEquals("Nombre corregido", entornoFGEM.sesionFGEM.empresaActivaRequeridaFGEM().razonSocialFGEM());
    }

    @Test
    void recuerdaLaUltimaEmpresaDeCadaUsuarioFGEM() {
        EmpresaFGEM aFGEM = servicioFGEM.crearFGEM(datosFGEM("Empresa A", null));
        EmpresaFGEM bFGEM = servicioFGEM.crearFGEM(datosFGEM("Empresa B", null));
        servicioFGEM.seleccionarEmpresaActivaFGEM(bFGEM.idFGEM());

        entornoFGEM.entrarComoFGEM("admin", EntornoPruebaFGEM.PASSWORD_ADMIN_FGEM);
        assertTrue(entornoFGEM.sesionFGEM.empresaActivaFGEM().isEmpty(), "Al iniciar sesión no hay empresa hasta restaurarla");
        assertEquals(bFGEM.idFGEM(), servicioFGEM.restaurarEmpresaActivaFGEM().orElseThrow().idFGEM());

        servicioFGEM.cambiarEstadoFGEM(bFGEM.idFGEM(), false);
        entornoFGEM.entrarComoFGEM("admin", EntornoPruebaFGEM.PASSWORD_ADMIN_FGEM);
        assertEquals(aFGEM.idFGEM(), servicioFGEM.restaurarEmpresaActivaFGEM().orElseThrow().idFGEM(),
                "Si la última ya no está disponible, se usa la primera disponible");
    }

    @Test
    void sinEmpresasNoHayEmpresaActivaFGEM() {
        assertTrue(servicioFGEM.restaurarEmpresaActivaFGEM().isEmpty());
        assertThrows(ValidacionExceptionFGEM.class, () -> entornoFGEM.sesionFGEM.empresaActivaRequeridaFGEM());
    }

    @Test
    void quitarElAccesoSeReflejaEnLaListaFGEM() {
        EmpresaFGEM empresaFGEM = servicioFGEM.crearFGEM(datosFGEM("Empresa", null));
        UsuarioFGEM operadorFGEM = entornoFGEM.crearUsuarioFGEM("operador", "OPERADOR");
        servicioFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of(operadorFGEM.idFGEM()));
        assertEquals(Set.of(operadorFGEM.idFGEM()), servicioFGEM.usuariosConAccesoFGEM(empresaFGEM.idFGEM()));

        servicioFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of());

        assertTrue(servicioFGEM.usuariosConAccesoFGEM(empresaFGEM.idFGEM()).isEmpty());
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of(999L)));
    }

    private void assertInvalidaFGEM(DatosEmpresaFGEM datosFGEM) {
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.crearFGEM(datosFGEM));
    }

    private static DatosEmpresaFGEM datosFGEM(String razonSocialFGEM, String rucFGEM) {
        return new DatosEmpresaFGEM(razonSocialFGEM, null, rucFGEM, SectorFGEM.SERVICIOS, TamanoEmpresaFGEM.MICRO, MonedaFGEM.PEN,
                1, null, null, null, null);
    }
}
