package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.config.MigradorFGEM;
import com.businessanalyzer.config.RestauracionPendienteFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CopiaSeguridadFGEM;
import com.businessanalyzer.model.CopiaSeguridadFGEM.ContenidoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.repository.EmpresaRepositoryFGEM;
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

class CopiaSeguridadServiceTestFGEM {

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private CopiaSeguridadServiceFGEM servicioFGEM;
    private Path carpetaCopiasFGEM;

    @BeforeEach
    void prepararFGEM() throws Exception {
        entornoFGEM = new EntornoPruebaFGEM(Files.createDirectories(carpetaFGEM.resolve("datos")));
        carpetaCopiasFGEM = carpetaFGEM.resolve("datos").resolve("copias");
        servicioFGEM = new CopiaSeguridadServiceFGEM(entornoFGEM.databaseFGEM, carpetaCopiasFGEM, entornoFGEM.configuracionFGEM,
                entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM);
        entornoFGEM.administradorConectadoFGEM();
        entornoFGEM.crearEmpresaActivaFGEM("Comercial Andina");
    }

    @Test
    void creaUnaCopiaIntegraConLosDatosFGEM() {
        CopiaSeguridadFGEM copiaFGEM = servicioFGEM.crearManualFGEM();

        assertTrue(Files.isRegularFile(copiaFGEM.archivoFGEM()));
        assertEquals(carpetaCopiasFGEM.toAbsolutePath(), copiaFGEM.archivoFGEM().getParent());
        assertEquals(CopiaSeguridadFGEM.TipoFGEM.MANUAL, copiaFGEM.tipoFGEM());
        assertTrue(copiaFGEM.bytesFGEM() > 0);
        ContenidoFGEM contenidoFGEM = servicioFGEM.validarFGEM(copiaFGEM.archivoFGEM());
        assertEquals(MigradorFGEM.ultimaVersionFGEM(), contenidoFGEM.versionEsquemaFGEM());
        assertEquals(1, contenidoFGEM.empresasFGEM());
        assertEquals(1, contenidoFGEM.usuariosFGEM());
        assertEquals(List.of(copiaFGEM), servicioFGEM.listarFGEM());
    }

    @Test
    void dosCopiasEnElMismoSegundoNoSeSobrescribenFGEM() {
        CopiaSeguridadFGEM aFGEM = servicioFGEM.crearManualFGEM();
        CopiaSeguridadFGEM bFGEM = servicioFGEM.crearManualFGEM();

        assertFalse(aFGEM.archivoFGEM().equals(bFGEM.archivoFGEM()));
        assertEquals(2, servicioFGEM.listarFGEM().size());
    }

    @Test
    void copiaAutomaticaUnaVezAlDiaYConRetencionFGEM() {
        servicioFGEM.configurarAutomaticaFGEM(true, 2);

        assertTrue(servicioFGEM.crearAutomaticaSiCorrespondeFGEM().isPresent());
        assertTrue(servicioFGEM.crearAutomaticaSiCorrespondeFGEM().isEmpty(), "No se repite el mismo día");
        for (int diaFGEM = 1; diaFGEM <= 3; diaFGEM++) {
            entornoFGEM.relojFGEM.avanzarFGEM(Duration.ofDays(1));
            assertTrue(servicioFGEM.crearAutomaticaSiCorrespondeFGEM().isPresent());
        }
        CopiaSeguridadFGEM manualFGEM = servicioFGEM.crearManualFGEM();

        List<CopiaSeguridadFGEM> copiasFGEM = servicioFGEM.listarFGEM();
        assertEquals(2, copiasFGEM.stream().filter(cFGEM -> cFGEM.tipoFGEM() == CopiaSeguridadFGEM.TipoFGEM.AUTOMATICA).count(),
                "Solo se conservan las 2 automáticas más recientes");
        assertTrue(copiasFGEM.contains(manualFGEM), "Las manuales no se borran");
        assertEquals(manualFGEM, copiasFGEM.get(0), "La más reciente primero");
    }

    @Test
    void restaurarUnaCopiaAutomaticaNoGeneraOtraElMismoDiaFGEM() throws Exception {
        CopiaSeguridadFGEM automaticaFGEM = servicioFGEM.crearAutomaticaSiCorrespondeFGEM().orElseThrow();
        servicioFGEM.programarRestauracionFGEM(automaticaFGEM.archivoFGEM());

        // Reinicio: se aplica la copia y la aplicación arranca de nuevo con los datos restaurados.
        RestauracionPendienteFGEM.aplicarFGEM(entornoFGEM.databaseFGEM.archivoFGEM());
        DatabaseFGEM reabiertaFGEM = new DatabaseFGEM(entornoFGEM.databaseFGEM.archivoFGEM());
        new MigradorFGEM(reabiertaFGEM).migrarFGEM();
        ConfiguracionServiceFGEM configuracionRestauradaFGEM = new ConfiguracionServiceFGEM(
                new com.businessanalyzer.repository.ConfiguracionRepositoryFGEM(reabiertaFGEM));
        CopiaSeguridadServiceFGEM despuesDeRestaurarFGEM = new CopiaSeguridadServiceFGEM(reabiertaFGEM, carpetaCopiasFGEM,
                configuracionRestauradaFGEM, entornoFGEM.sesionFGEM, entornoFGEM.relojFGEM);

        assertTrue(despuesDeRestaurarFGEM.crearAutomaticaSiCorrespondeFGEM().isEmpty(),
                "La copia ya registra la fecha de la última automática");
        entornoFGEM.relojFGEM.avanzarFGEM(Duration.ofDays(1));
        assertTrue(despuesDeRestaurarFGEM.crearAutomaticaSiCorrespondeFGEM().isPresent(), "Al día siguiente sí se crea");
    }

    @Test
    void laCopiaAutomaticaSePuedeDesactivarFGEM() {
        servicioFGEM.configurarAutomaticaFGEM(false, 5);

        assertTrue(servicioFGEM.crearAutomaticaSiCorrespondeFGEM().isEmpty());
        assertTrue(servicioFGEM.listarFGEM().isEmpty());
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.configurarAutomaticaFGEM(true, 0));
    }

    @Test
    void laCopiaIncluyeLasFotosYLasRestauraFGEM() throws Exception {
        Path fotosFGEM = entornoFGEM.databaseFGEM.archivoFGEM().resolveSibling("fotos-visitas");
        Path fotoFGEM = Files.createDirectories(fotosFGEM.resolve("7")).resolve("cobertizo.jpg");
        Files.write(fotoFGEM, new byte[]{1, 2, 3, 4, 5});
        CopiaSeguridadFGEM copiaFGEM = servicioFGEM.crearManualFGEM();

        assertTrue(copiaFGEM.archivoFGEM().getFileName().toString().endsWith(".zip"));
        assertEquals(1, servicioFGEM.validarFGEM(copiaFGEM.archivoFGEM()).fotosFGEM());

        // Después de la copia se borra esa foto y se agrega otra.
        Files.delete(fotoFGEM);
        Files.write(fotosFGEM.resolve("7").resolve("nueva.jpg"), new byte[]{9});

        servicioFGEM.programarRestauracionFGEM(copiaFGEM.archivoFGEM());
        assertTrue(Files.isRegularFile(RestauracionPendienteFGEM.carpetaFotosPendienteFGEM(entornoFGEM.databaseFGEM.archivoFGEM())
                .resolve("7").resolve("cobertizo.jpg")));
        // La copia previa a la restauración guarda las fotos del estado actual.
        CopiaSeguridadFGEM previaFGEM = servicioFGEM.listarFGEM().stream()
                .filter(cFGEM -> cFGEM.tipoFGEM() == CopiaSeguridadFGEM.TipoFGEM.PREVIA_RESTAURACION).findFirst().orElseThrow();
        assertEquals(1, servicioFGEM.validarFGEM(previaFGEM.archivoFGEM()).fotosFGEM());

        assertTrue(RestauracionPendienteFGEM.aplicarFGEM(entornoFGEM.databaseFGEM.archivoFGEM()));
        assertTrue(Files.isRegularFile(fotoFGEM), "La foto de la copia vuelve");
        assertFalse(Files.exists(fotosFGEM.resolve("7").resolve("nueva.jpg")), "Las fotos quedan como en la copia");
        assertFalse(Files.exists(RestauracionPendienteFGEM.carpetaFotosPendienteFGEM(entornoFGEM.databaseFGEM.archivoFGEM())));
    }

    @Test
    void unaCopiaAntiguaSoloConLaBaseSeRestauraSinTocarLasFotosFGEM() throws Exception {
        Path antiguaFGEM = copiaAntiguaFGEM("business-analyzer_2025-12-01_080000_manual.db");
        Path fotoFGEM = Files.createDirectories(entornoFGEM.databaseFGEM.archivoFGEM().resolveSibling("fotos-visitas").resolve("3"))
                .resolve("a.jpg");
        Files.write(fotoFGEM, new byte[]{1});

        ContenidoFGEM contenidoFGEM = servicioFGEM.programarRestauracionFGEM(antiguaFGEM);

        assertEquals(0, contenidoFGEM.fotosFGEM());
        assertFalse(Files.exists(RestauracionPendienteFGEM.carpetaFotosPendienteFGEM(entornoFGEM.databaseFGEM.archivoFGEM())));
        assertTrue(RestauracionPendienteFGEM.aplicarFGEM(entornoFGEM.databaseFGEM.archivoFGEM()));
        assertTrue(Files.isRegularFile(fotoFGEM), "Las fotos actuales se conservan");
    }

    @Test
    void rechazaUnZipQueNoEsUnaCopiaFGEM() throws Exception {
        Path zipFGEM = carpetaFGEM.resolve("otro.zip");
        try (java.util.zip.ZipOutputStream salidaFGEM = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipFGEM))) {
            salidaFGEM.putNextEntry(new java.util.zip.ZipEntry("documento.txt"));
            salidaFGEM.write("hola".getBytes());
            salidaFGEM.closeEntry();
        }
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.validarFGEM(zipFGEM));
    }

    /** Copia en el formato anterior a las fotos: solo el archivo de la base de datos. */
    private Path copiaAntiguaFGEM(String nombreFGEM) {
        Path destinoFGEM = carpetaFGEM.resolve(nombreFGEM);
        entornoFGEM.databaseFGEM.conConexionFGEM(cFGEM -> {
            try (PreparedStatement psFGEM = cFGEM.prepareStatement("VACUUM INTO ?")) {
                psFGEM.setString(1, destinoFGEM.toString());
                psFGEM.execute();
            }
            return null;
        });
        return destinoFGEM;
    }

    @Test
    void restaurarReemplazaLaBaseAlReiniciarFGEM() throws Exception {
        CopiaSeguridadFGEM copiaFGEM = servicioFGEM.crearManualFGEM();
        entornoFGEM.crearEmpresaActivaFGEM("Empresa creada después de la copia");
        assertEquals(2, contarEmpresasFGEM(entornoFGEM.databaseFGEM));

        ContenidoFGEM contenidoFGEM = servicioFGEM.programarRestauracionFGEM(copiaFGEM.archivoFGEM());

        assertEquals(1, contenidoFGEM.empresasFGEM());
        assertTrue(RestauracionPendienteFGEM.hayPendienteFGEM(entornoFGEM.databaseFGEM.archivoFGEM()));
        assertTrue(servicioFGEM.listarFGEM().stream().anyMatch(cFGEM -> cFGEM.tipoFGEM() == CopiaSeguridadFGEM.TipoFGEM.PREVIA_RESTAURACION),
                "Se guarda el estado anterior");

        // Simula el reinicio: se aplica la restauración antes de abrir la base de datos.
        assertTrue(RestauracionPendienteFGEM.aplicarFGEM(entornoFGEM.databaseFGEM.archivoFGEM()));
        assertFalse(RestauracionPendienteFGEM.hayPendienteFGEM(entornoFGEM.databaseFGEM.archivoFGEM()));
        DatabaseFGEM reabiertaFGEM = new DatabaseFGEM(entornoFGEM.databaseFGEM.archivoFGEM());
        new MigradorFGEM(reabiertaFGEM).migrarFGEM();
        assertEquals(1, contarEmpresasFGEM(reabiertaFGEM));
        assertFalse(RestauracionPendienteFGEM.aplicarFGEM(entornoFGEM.databaseFGEM.archivoFGEM()), "Solo se aplica una vez");
    }

    @Test
    void rechazaArchivosQueNoSonCopiasValidasFGEM() throws Exception {
        Path textoFGEM = Files.writeString(carpetaFGEM.resolve("notas.db"), "esto no es una base de datos");
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.validarFGEM(textoFGEM));
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.validarFGEM(carpetaFGEM.resolve("no-existe.db")));

        // Base SQLite de otra aplicación
        DatabaseFGEM otraFGEM = new DatabaseFGEM(carpetaFGEM.resolve("otra.db"));
        otraFGEM.conConexionFGEM(cFGEM -> {
            try (Statement stFGEM = cFGEM.createStatement()) {
                stFGEM.execute("CREATE TABLE cosas (id INTEGER)");
            }
            return null;
        });
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.validarFGEM(otraFGEM.archivoFGEM()));

        // Copia (en el formato antiguo, solo la base) de una versión más nueva de la aplicación
        Path viejaFGEM = copiaAntiguaFGEM("futura.db");
        DatabaseFGEM futuraFGEM = new DatabaseFGEM(viejaFGEM);
        futuraFGEM.conConexionFGEM(cFGEM -> {
            try (PreparedStatement psFGEM = cFGEM.prepareStatement(
                    "INSERT INTO schema_version (version, descripcion, aplicada_en) VALUES (?, 'futura', '2030-01-01')")) {
                psFGEM.setInt(1, MigradorFGEM.ultimaVersionFGEM() + 1);
                psFGEM.executeUpdate();
            }
            return null;
        });
        ValidacionExceptionFGEM errorFGEM = assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.validarFGEM(viejaFGEM));
        assertTrue(errorFGEM.getMessage().contains("más nueva"));
        assertFalse(RestauracionPendienteFGEM.hayPendienteFGEM(entornoFGEM.databaseFGEM.archivoFGEM()));
    }

    @Test
    void cambiarCarpetaYVerificarIntegridadFGEM() throws Exception {
        Path otraCarpetaFGEM = Files.createDirectories(carpetaFGEM.resolve("usb"));
        servicioFGEM.cambiarCarpetaFGEM(otraCarpetaFGEM);

        CopiaSeguridadFGEM copiaFGEM = servicioFGEM.crearManualFGEM();
        assertEquals(otraCarpetaFGEM.toAbsolutePath(), copiaFGEM.archivoFGEM().getParent());
        assertThrows(ValidacionExceptionFGEM.class, () -> servicioFGEM.cambiarCarpetaFGEM(carpetaFGEM.resolve("no-existe")));
        assertEquals(List.of("ok"), servicioFGEM.verificarIntegridadFGEM());

        servicioFGEM.eliminarFGEM(copiaFGEM);
        assertTrue(servicioFGEM.listarFGEM().isEmpty());
    }

    @Test
    void soloElAdministradorGestionaLasCopiasFGEM() {
        EmpresaFGEM empresaFGEM = entornoFGEM.sesionFGEM.empresaActivaFGEM().orElseThrow();
        var gerenteFGEM = entornoFGEM.crearUsuarioFGEM("gerente", "GERENTE");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of(gerenteFGEM.idFGEM()));
        entornoFGEM.entrarComoFGEM("gerente", "Temporal123");

        assertThrows(AccesoDenegadoExceptionFGEM.class, servicioFGEM::crearManualFGEM);
        assertThrows(AccesoDenegadoExceptionFGEM.class, servicioFGEM::listarFGEM);
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.configurarAutomaticaFGEM(true, 3));
        assertTrue(servicioFGEM.crearAutomaticaSiCorrespondeFGEM().isPresent(), "La copia automática no depende del usuario");
    }

    private static int contarEmpresasFGEM(DatabaseFGEM databaseFGEM) {
        return databaseFGEM.conConexionFGEM(cFGEM -> new EmpresaRepositoryFGEM().listarFGEM(cFGEM, true).size());
    }
}
