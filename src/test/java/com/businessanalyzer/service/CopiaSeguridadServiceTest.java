package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.config.Database;
import com.businessanalyzer.config.Migrador;
import com.businessanalyzer.config.RestauracionPendiente;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CopiaSeguridad;
import com.businessanalyzer.model.CopiaSeguridad.Contenido;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.repository.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopiaSeguridadServiceTest {

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private CopiaSeguridadService servicio;
    private Path carpetaCopias;

    @BeforeEach
    void preparar() throws Exception {
        entorno = new EntornoPrueba(Files.createDirectories(carpeta.resolve("datos")));
        carpetaCopias = carpeta.resolve("datos").resolve("copias");
        servicio = new CopiaSeguridadService(entorno.database, carpetaCopias, entorno.configuracion,
                entorno.sesion, entorno.reloj);
        entorno.administradorConectado();
        entorno.crearEmpresaActiva("Comercial Andina");
    }

    @Test
    void creaUnaCopiaIntegraConLosDatos() {
        CopiaSeguridad copia = servicio.crearManual();

        assertTrue(Files.isRegularFile(copia.archivo()));
        assertEquals(carpetaCopias.toAbsolutePath(), copia.archivo().getParent());
        assertEquals(CopiaSeguridad.Tipo.MANUAL, copia.tipo());
        assertTrue(copia.bytes() > 0);
        Contenido contenido = servicio.validar(copia.archivo());
        assertEquals(Migrador.ultimaVersion(), contenido.versionEsquema());
        assertEquals(1, contenido.empresas());
        assertEquals(1, contenido.usuarios());
        assertEquals(List.of(copia), servicio.listar());
    }

    @Test
    void dosCopiasEnElMismoSegundoNoSeSobrescriben() {
        CopiaSeguridad a = servicio.crearManual();
        CopiaSeguridad b = servicio.crearManual();

        assertFalse(a.archivo().equals(b.archivo()));
        assertEquals(2, servicio.listar().size());
    }

    @Test
    void copiaAutomaticaUnaVezAlDiaYConRetencion() {
        servicio.configurarAutomatica(true, 2);

        assertTrue(servicio.crearAutomaticaSiCorresponde().isPresent());
        assertTrue(servicio.crearAutomaticaSiCorresponde().isEmpty(), "No se repite el mismo día");
        for (int dia = 1; dia <= 3; dia++) {
            entorno.reloj.avanzar(Duration.ofDays(1));
            assertTrue(servicio.crearAutomaticaSiCorresponde().isPresent());
        }
        CopiaSeguridad manual = servicio.crearManual();

        List<CopiaSeguridad> copias = servicio.listar();
        assertEquals(2, copias.stream().filter(c -> c.tipo() == CopiaSeguridad.Tipo.AUTOMATICA).count(),
                "Solo se conservan las 2 automáticas más recientes");
        assertTrue(copias.contains(manual), "Las manuales no se borran");
        assertEquals(manual, copias.get(0), "La más reciente primero");
    }

    @Test
    void restaurarUnaCopiaAutomaticaNoGeneraOtraElMismoDia() throws Exception {
        CopiaSeguridad automatica = servicio.crearAutomaticaSiCorresponde().orElseThrow();
        servicio.programarRestauracion(automatica.archivo());

        // Reinicio: se aplica la copia y la aplicación arranca de nuevo con los datos restaurados.
        RestauracionPendiente.aplicar(entorno.database.archivo());
        Database reabierta = new Database(entorno.database.archivo());
        new Migrador(reabierta).migrar();
        ConfiguracionService configuracionRestaurada = new ConfiguracionService(
                new com.businessanalyzer.repository.ConfiguracionRepository(reabierta));
        CopiaSeguridadService despuesDeRestaurar = new CopiaSeguridadService(reabierta, carpetaCopias,
                configuracionRestaurada, entorno.sesion, entorno.reloj);

        assertTrue(despuesDeRestaurar.crearAutomaticaSiCorresponde().isEmpty(),
                "La copia ya registra la fecha de la última automática");
        entorno.reloj.avanzar(Duration.ofDays(1));
        assertTrue(despuesDeRestaurar.crearAutomaticaSiCorresponde().isPresent(), "Al día siguiente sí se crea");
    }

    @Test
    void laCopiaAutomaticaSePuedeDesactivar() {
        servicio.configurarAutomatica(false, 5);

        assertTrue(servicio.crearAutomaticaSiCorresponde().isEmpty());
        assertTrue(servicio.listar().isEmpty());
        assertThrows(ValidacionException.class, () -> servicio.configurarAutomatica(true, 0));
    }

    @Test
    void laCopiaIncluyeLasFotosYLasRestaura() throws Exception {
        Path fotos = entorno.database.archivo().resolveSibling("fotos-visitas");
        Path foto = Files.createDirectories(fotos.resolve("7")).resolve("cobertizo.jpg");
        Files.write(foto, new byte[]{1, 2, 3, 4, 5});
        CopiaSeguridad copia = servicio.crearManual();

        assertTrue(copia.archivo().getFileName().toString().endsWith(".zip"));
        assertEquals(1, servicio.validar(copia.archivo()).fotos());

        // Después de la copia se borra esa foto y se agrega otra.
        Files.delete(foto);
        Files.write(fotos.resolve("7").resolve("nueva.jpg"), new byte[]{9});

        servicio.programarRestauracion(copia.archivo());
        assertTrue(Files.isRegularFile(RestauracionPendiente.carpetaFotosPendiente(entorno.database.archivo())
                .resolve("7").resolve("cobertizo.jpg")));
        // La copia previa a la restauración guarda las fotos del estado actual.
        CopiaSeguridad previa = servicio.listar().stream()
                .filter(c -> c.tipo() == CopiaSeguridad.Tipo.PREVIA_RESTAURACION).findFirst().orElseThrow();
        assertEquals(1, servicio.validar(previa.archivo()).fotos());

        assertTrue(RestauracionPendiente.aplicar(entorno.database.archivo()));
        assertTrue(Files.isRegularFile(foto), "La foto de la copia vuelve");
        assertFalse(Files.exists(fotos.resolve("7").resolve("nueva.jpg")), "Las fotos quedan como en la copia");
        assertFalse(Files.exists(RestauracionPendiente.carpetaFotosPendiente(entorno.database.archivo())));
    }

    @Test
    void unaCopiaAntiguaSoloConLaBaseSeRestauraSinTocarLasFotos() throws Exception {
        Path antigua = copiaAntigua("business-analyzer_2025-12-01_080000_manual.db");
        Path foto = Files.createDirectories(entorno.database.archivo().resolveSibling("fotos-visitas").resolve("3"))
                .resolve("a.jpg");
        Files.write(foto, new byte[]{1});

        Contenido contenido = servicio.programarRestauracion(antigua);

        assertEquals(0, contenido.fotos());
        assertFalse(Files.exists(RestauracionPendiente.carpetaFotosPendiente(entorno.database.archivo())));
        assertTrue(RestauracionPendiente.aplicar(entorno.database.archivo()));
        assertTrue(Files.isRegularFile(foto), "Las fotos actuales se conservan");
    }

    @Test
    void rechazaUnZipQueNoEsUnaCopia() throws Exception {
        Path zip = carpeta.resolve("otro.zip");
        try (java.util.zip.ZipOutputStream salida = new java.util.zip.ZipOutputStream(Files.newOutputStream(zip))) {
            salida.putNextEntry(new java.util.zip.ZipEntry("documento.txt"));
            salida.write("hola".getBytes());
            salida.closeEntry();
        }
        assertThrows(ValidacionException.class, () -> servicio.validar(zip));
    }

    /** Copia en el formato anterior a las fotos: solo el archivo de la base de datos. */
    private Path copiaAntigua(String nombre) {
        Path destino = carpeta.resolve(nombre);
        entorno.database.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement("VACUUM INTO ?")) {
                ps.setString(1, destino.toString());
                ps.execute();
            }
            return null;
        });
        return destino;
    }

    @Test
    void restaurarReemplazaLaBaseAlReiniciar() throws Exception {
        CopiaSeguridad copia = servicio.crearManual();
        entorno.crearEmpresaActiva("Empresa creada después de la copia");
        assertEquals(2, contarEmpresas(entorno.database));

        Contenido contenido = servicio.programarRestauracion(copia.archivo());

        assertEquals(1, contenido.empresas());
        assertTrue(RestauracionPendiente.hayPendiente(entorno.database.archivo()));
        assertTrue(servicio.listar().stream().anyMatch(c -> c.tipo() == CopiaSeguridad.Tipo.PREVIA_RESTAURACION),
                "Se guarda el estado anterior");

        // Simula el reinicio: se aplica la restauración antes de abrir la base de datos.
        assertTrue(RestauracionPendiente.aplicar(entorno.database.archivo()));
        assertFalse(RestauracionPendiente.hayPendiente(entorno.database.archivo()));
        Database reabierta = new Database(entorno.database.archivo());
        new Migrador(reabierta).migrar();
        assertEquals(1, contarEmpresas(reabierta));
        assertFalse(RestauracionPendiente.aplicar(entorno.database.archivo()), "Solo se aplica una vez");
    }

    @Test
    void rechazaArchivosQueNoSonCopiasValidas() throws Exception {
        Path texto = Files.writeString(carpeta.resolve("notas.db"), "esto no es una base de datos");
        assertThrows(ValidacionException.class, () -> servicio.validar(texto));
        assertThrows(ValidacionException.class, () -> servicio.validar(carpeta.resolve("no-existe.db")));

        // Base SQLite de otra aplicación
        Database otra = new Database(carpeta.resolve("otra.db"));
        otra.conConexion(c -> {
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TABLE cosas (id INTEGER)");
            }
            return null;
        });
        assertThrows(ValidacionException.class, () -> servicio.validar(otra.archivo()));

        // Copia (en el formato antiguo, solo la base) de una versión más nueva de la aplicación
        Path vieja = copiaAntigua("futura.db");
        Database futura = new Database(vieja);
        futura.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO schema_version (version, descripcion, aplicada_en) VALUES (?, 'futura', '2030-01-01')")) {
                ps.setInt(1, Migrador.ultimaVersion() + 1);
                ps.executeUpdate();
            }
            return null;
        });
        ValidacionException error = assertThrows(ValidacionException.class, () -> servicio.validar(vieja));
        assertTrue(error.getMessage().contains("más nueva"));
        assertFalse(RestauracionPendiente.hayPendiente(entorno.database.archivo()));
    }

    @Test
    void cambiarCarpetaYVerificarIntegridad() throws Exception {
        Path otraCarpeta = Files.createDirectories(carpeta.resolve("usb"));
        servicio.cambiarCarpeta(otraCarpeta);

        CopiaSeguridad copia = servicio.crearManual();
        assertEquals(otraCarpeta.toAbsolutePath(), copia.archivo().getParent());
        assertThrows(ValidacionException.class, () -> servicio.cambiarCarpeta(carpeta.resolve("no-existe")));
        assertEquals(List.of("ok"), servicio.verificarIntegridad());

        servicio.eliminar(copia);
        assertTrue(servicio.listar().isEmpty());
    }

    @Test
    void soloElAdministradorGestionaLasCopias() {
        Empresa empresa = entorno.sesion.empresaActiva().orElseThrow();
        var gerente = entorno.crearUsuario("gerente", "GERENTE");
        entorno.empresaService.asignarAccesos(empresa.id(), Set.of(gerente.id()));
        entorno.entrarComo("gerente", "Temporal123");

        assertThrows(AccesoDenegadoException.class, servicio::crearManual);
        assertThrows(AccesoDenegadoException.class, servicio::listar);
        assertThrows(AccesoDenegadoException.class, () -> servicio.configurarAutomatica(true, 3));
        assertTrue(servicio.crearAutomaticaSiCorresponde().isPresent(), "La copia automática no depende del usuario");
    }

    private static int contarEmpresas(Database database) {
        return database.conConexion(c -> new EmpresaRepository().listar(c, true).size());
    }
}
