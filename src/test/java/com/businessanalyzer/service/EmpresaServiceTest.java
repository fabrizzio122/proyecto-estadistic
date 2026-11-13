package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosEmpresa;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.TamanoEmpresa;
import com.businessanalyzer.model.Usuario;
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

class EmpresaServiceTest {

    private static final String CLAVE = "Temporal123";

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private EmpresaService servicio;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = entorno.empresaService;
        entorno.administradorConectado();
    }

    @Test
    void registraUnaEmpresaNormalizandoSusDatos() {
        Empresa empresa = servicio.crear(new DatosEmpresa("  Comercial Andina S.A.C. ", "", "20601234567",
                Sector.COMERCIO, TamanoEmpresa.PEQUENA, Moneda.PEN, null, LocalDate.of(2020, 3, 1),
                " ", null, "ventas@andina.pe"));

        assertEquals("Comercial Andina S.A.C.", empresa.razonSocial());
        assertNull(empresa.nombreComercial());
        assertEquals("Comercial Andina S.A.C.", empresa.nombreVisible(), "Sin nombre comercial usa la razón social");
        assertEquals(1, empresa.mesInicioEjercicio(), "El ejercicio fiscal empieza en enero por defecto");
        assertNull(empresa.direccion());
        assertTrue(empresa.activa());
    }

    @Test
    void validaLosDatosDeLaEmpresa() {
        assertInvalida(datos("ab", "20601234567"));
        assertInvalida(datos("Empresa válida", "123"));
        assertInvalida(datos("Empresa válida", "RUC CON ESPACIOS"));
        assertInvalida(new DatosEmpresa("Empresa", null, null, null, TamanoEmpresa.MICRO, Moneda.PEN,
                1, null, null, null, null));
        assertInvalida(new DatosEmpresa("Empresa", null, null, Sector.SERVICIOS, TamanoEmpresa.MICRO, Moneda.PEN,
                13, null, null, null, null));
        assertInvalida(new DatosEmpresa("Empresa", null, null, Sector.SERVICIOS, TamanoEmpresa.MICRO, Moneda.PEN,
                1, LocalDate.of(2030, 1, 1), null, null, null));
        assertInvalida(new DatosEmpresa("Empresa", null, null, Sector.SERVICIOS, TamanoEmpresa.MICRO, Moneda.PEN,
                1, null, null, "abc", null));
        assertInvalida(new DatosEmpresa("Empresa", null, null, Sector.SERVICIOS, TamanoEmpresa.MICRO, Moneda.PEN,
                1, null, null, null, "correo-invalido"));
    }

    @Test
    void elRucNoSePuedeRepetir() {
        servicio.crear(datos("Primera empresa", "20601234567"));
        Empresa segunda = servicio.crear(datos("Segunda empresa", "ab-12345"));

        assertThrows(ValidacionException.class, () -> servicio.crear(datos("Copia", "20601234567")));
        assertThrows(ValidacionException.class, () -> servicio.crear(datos("Copia", "AB-12345")),
                "La comparación no distingue mayúsculas");
        assertEquals("AB-12345", segunda.identificacionFiscal(), "Se guarda en mayúsculas");
        // Sin RUC se pueden registrar varias empresas
        servicio.crear(datos("Sin RUC uno", null));
        servicio.crear(datos("Sin RUC dos", null));
    }

    @Test
    void elAdministradorVeTodasYLosDemasSoloLasAsignadas() {
        Empresa a = servicio.crear(datos("Empresa A", null));
        Empresa b = servicio.crear(datos("Empresa B", null));
        Usuario gerente = entorno.crearUsuario("gerente", "GERENTE");
        servicio.asignarAccesos(a.id(), Set.of(gerente.id()));

        assertEquals(2, servicio.listar(false).size());

        entorno.entrarComo("gerente", CLAVE);
        List<Empresa> visibles = servicio.listar(false);
        assertEquals(List.of(a.id()), visibles.stream().map(Empresa::id).toList());
        assertThrows(AccesoDenegadoException.class, () -> servicio.seleccionarEmpresaActiva(b.id()));
        assertThrows(AccesoDenegadoException.class, () -> servicio.actualizar(b.id(), datos("Cambio", null)));
    }

    @Test
    void quienRegistraUnaEmpresaQuedaAsignadoAElla() {
        entorno.crearUsuario("gerente", "GERENTE");
        entorno.entrarComo("gerente", CLAVE);

        Empresa propia = servicio.crear(datos("Empresa del gerente", null));

        assertEquals(List.of(propia.id()), servicio.listar(false).stream().map(Empresa::id).toList());
    }

    @Test
    void losPermisosDelRolSeRespetan() {
        Empresa empresa = servicio.crear(datos("Empresa", null));
        Usuario analista = entorno.crearUsuario("analista", "ANALISTA");
        servicio.asignarAccesos(empresa.id(), Set.of(analista.id()));

        entorno.entrarComo("analista", CLAVE);
        assertEquals(1, servicio.listar(false).size(), "El analista puede ver");
        assertThrows(AccesoDenegadoException.class, () -> servicio.crear(datos("Otra", null)));
        assertThrows(AccesoDenegadoException.class, () -> servicio.cambiarEstado(empresa.id(), false));
        assertThrows(AccesoDenegadoException.class, () -> servicio.asignarAccesos(empresa.id(), Set.of()),
                "Solo un administrador asigna accesos");
    }

    @Test
    void archivarLaEmpresaActivaLaQuitaDeLaSesionYDelSelector() {
        Empresa empresa = servicio.crear(datos("Empresa", null));
        servicio.seleccionarEmpresaActiva(empresa.id());
        List<Optional<Empresa>> notificaciones = new ArrayList<>();
        entorno.sesion.alCambiarEmpresa(notificaciones::add);

        servicio.cambiarEstado(empresa.id(), false);

        assertTrue(entorno.sesion.empresaActiva().isEmpty());
        assertEquals(List.of(Optional.empty()), notificaciones, "La ventana principal debe enterarse");
        assertTrue(servicio.listarParaSelector().isEmpty());
        assertEquals(1, servicio.listar(true).size(), "Sigue existiendo como archivada");
        assertThrows(ValidacionException.class, () -> servicio.seleccionarEmpresaActiva(empresa.id()));

        servicio.cambiarEstado(empresa.id(), true);
        assertEquals(1, servicio.listarParaSelector().size());
    }

    @Test
    void editarLaEmpresaActivaActualizaLaSesion() {
        Empresa empresa = servicio.crear(datos("Nombre original", null));
        servicio.seleccionarEmpresaActiva(empresa.id());

        servicio.actualizar(empresa.id(), datos("Nombre corregido", null));

        assertEquals("Nombre corregido", entorno.sesion.empresaActivaRequerida().razonSocial());
    }

    @Test
    void recuerdaLaUltimaEmpresaDeCadaUsuario() {
        Empresa a = servicio.crear(datos("Empresa A", null));
        Empresa b = servicio.crear(datos("Empresa B", null));
        servicio.seleccionarEmpresaActiva(b.id());

        entorno.entrarComo("admin", EntornoPrueba.PASSWORD_ADMIN);
        assertTrue(entorno.sesion.empresaActiva().isEmpty(), "Al iniciar sesión no hay empresa hasta restaurarla");
        assertEquals(b.id(), servicio.restaurarEmpresaActiva().orElseThrow().id());

        servicio.cambiarEstado(b.id(), false);
        entorno.entrarComo("admin", EntornoPrueba.PASSWORD_ADMIN);
        assertEquals(a.id(), servicio.restaurarEmpresaActiva().orElseThrow().id(),
                "Si la última ya no está disponible, se usa la primera disponible");
    }

    @Test
    void sinEmpresasNoHayEmpresaActiva() {
        assertTrue(servicio.restaurarEmpresaActiva().isEmpty());
        assertThrows(ValidacionException.class, () -> entorno.sesion.empresaActivaRequerida());
    }

    @Test
    void quitarElAccesoSeReflejaEnLaLista() {
        Empresa empresa = servicio.crear(datos("Empresa", null));
        Usuario operador = entorno.crearUsuario("operador", "OPERADOR");
        servicio.asignarAccesos(empresa.id(), Set.of(operador.id()));
        assertEquals(Set.of(operador.id()), servicio.usuariosConAcceso(empresa.id()));

        servicio.asignarAccesos(empresa.id(), Set.of());

        assertTrue(servicio.usuariosConAcceso(empresa.id()).isEmpty());
        assertThrows(ValidacionException.class, () -> servicio.asignarAccesos(empresa.id(), Set.of(999L)));
    }

    private void assertInvalida(DatosEmpresa datos) {
        assertThrows(ValidacionException.class, () -> servicio.crear(datos));
    }

    private static DatosEmpresa datos(String razonSocial, String ruc) {
        return new DatosEmpresa(razonSocial, null, ruc, Sector.SERVICIOS, TamanoEmpresa.MICRO, Moneda.PEN,
                1, null, null, null, null);
    }
}
