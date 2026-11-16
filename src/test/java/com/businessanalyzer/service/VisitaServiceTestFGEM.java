package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.AspectoInspeccionFGEM;
import com.businessanalyzer.model.CompromisoFGEM;
import com.businessanalyzer.model.ConclusionVisitaFGEM;
import com.businessanalyzer.model.DatosEmpresaFGEM;
import com.businessanalyzer.model.DatosVisitaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EstadoCompromisoFGEM;
import com.businessanalyzer.model.FotoVisitaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.ResultadoAspectoFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.TamanoEmpresaFGEM;
import com.businessanalyzer.model.TipoVisitaFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.model.VisitaFGEM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitaServiceTestFGEM {

    /** El reloj de pruebas está en 2026-01-15. */
    private static final LocalDate HOY_FGEM = LocalDate.of(2026, 1, 15);

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private VisitaServiceFGEM servicioFGEM;
    private EmpresaFGEM fundoFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        servicioFGEM = entornoFGEM.visitasFGEM;
        entornoFGEM.administradorConectadoFGEM();
        fundoFGEM = entornoFGEM.empresaServiceFGEM.crearFGEM(new DatosEmpresaFGEM("Fundo Los Andes", null, null, SectorFGEM.AGROPECUARIO,
                TamanoEmpresaFGEM.MICRO, MonedaFGEM.PEN, 1, null, null, null, null));
        entornoFGEM.empresaServiceFGEM.seleccionarEmpresaActivaFGEM(fundoFGEM.idFGEM());
    }

    @Test
    void registraLaVisitaConSuVerificacionYCompromisosFGEM() {
        VisitaFGEM vFGEM = servicioFGEM.registrarFGEM(visitaFGEM(HOY_FGEM.minusDays(5), Map.of(
                AspectoInspeccionFGEM.SANIDAD_ANIMAL, ResultadoAspectoFGEM.CUMPLE,
                AspectoInspeccionFGEM.VACUNACION, ResultadoAspectoFGEM.PARCIAL,
                AspectoInspeccionFGEM.RIEGO, ResultadoAspectoFGEM.NO_APLICA,
                AspectoInspeccionFGEM.INSTALACIONES, ResultadoAspectoFGEM.NO_CUMPLE),
                List.of(new DatosVisitaFGEM.NuevoCompromisoFGEM("Construir el cobertizo", HOY_FGEM.plusMonths(1)))));

        assertEquals(4, vFGEM.aspectosFGEM().size());
        assertEquals(0, new BigDecimal("0.5").compareTo(vFGEM.cumplimientoFGEM())); // (1 + 0.5 + 0) / 3
        assertEquals("Vacunas pendientes", vFGEM.notasFGEM().get(AspectoInspeccionFGEM.VACUNACION));
        assertEquals("-12.13520, -75.23110", vFGEM.coordenadasFGEM());
        List<CompromisoFGEM> compromisosFGEM = servicioFGEM.compromisosFGEM();
        assertEquals(1, compromisosFGEM.size());
        assertEquals(EstadoCompromisoFGEM.PENDIENTE, compromisosFGEM.get(0).estadoFGEM());
        assertEquals(ConclusionVisitaFGEM.CON_OBSERVACIONES, ConclusionVisitaFGEM.sugeridaFGEM(vFGEM.cumplimientoFGEM()));
    }

    @Test
    void validaLosDatosFGEM() {
        assertInvalidoFGEM(() -> servicioFGEM.registrarFGEM(visitaFGEM(HOY_FGEM.plusDays(1), Map.of(), List.of())));
        assertInvalidoFGEM(() -> servicioFGEM.registrarFGEM(new DatosVisitaFGEM(HOY_FGEM, TipoVisitaFGEM.SUPERVISION, "Ing. Rosa", null,
                -12.0, null, null, null, ConclusionVisitaFGEM.FAVORABLE, null, Map.of(), Map.of(), List.of()))); // solo latitud
        assertInvalidoFGEM(() -> servicioFGEM.registrarFGEM(new DatosVisitaFGEM(HOY_FGEM, TipoVisitaFGEM.SUPERVISION, "Ing. Rosa", null,
                -95.0, 10.0, null, null, ConclusionVisitaFGEM.FAVORABLE, null, Map.of(), Map.of(), List.of())));
        assertInvalidoFGEM(() -> servicioFGEM.registrarFGEM(new DatosVisitaFGEM(HOY_FGEM, TipoVisitaFGEM.SUPERVISION, "Ing. Rosa", null,
                null, null, null, null, ConclusionVisitaFGEM.FAVORABLE, HOY_FGEM, Map.of(), Map.of(), List.of()))); // próxima = hoy
        assertInvalidoFGEM(() -> servicioFGEM.registrarFGEM(visitaFGEM(HOY_FGEM, Map.of(), List.of(
                new DatosVisitaFGEM.NuevoCompromisoFGEM("Hacer algo útil", HOY_FGEM.minusDays(1))))));             // plazo vencido
        entornoFGEM.crearEmpresaActivaFGEM("Bodega");
        assertInvalidoFGEM(() -> servicioFGEM.listarFGEM(false));                                            // no agropecuaria
    }

    @Test
    void losCompromisosSeVerificanEnUnaVisitaPosteriorFGEM() {
        VisitaFGEM primeraFGEM = servicioFGEM.registrarFGEM(visitaFGEM(HOY_FGEM.minusMonths(2), Map.of(), List.of(
                new DatosVisitaFGEM.NuevoCompromisoFGEM("Dosificar las alpacas", HOY_FGEM.minusMonths(1)))));
        CompromisoFGEM kFGEM = servicioFGEM.compromisosFGEM().get(0);
        assertTrue(kFGEM.vencidoFGEM(HOY_FGEM));

        assertInvalidoFGEM(() -> servicioFGEM.verificarCompromisoFGEM(kFGEM.idFGEM(), EstadoCompromisoFGEM.CUMPLIDO, HOY_FGEM, primeraFGEM.idFGEM(), null));
        assertInvalidoFGEM(() -> servicioFGEM.verificarCompromisoFGEM(kFGEM.idFGEM(), EstadoCompromisoFGEM.CUMPLIDO,
                HOY_FGEM.minusMonths(3), null, null));                                               // antes de acordarlo

        VisitaFGEM segundaFGEM = servicioFGEM.registrarFGEM(visitaFGEM(HOY_FGEM, Map.of(), List.of()));
        CompromisoFGEM cumplidoFGEM = servicioFGEM.verificarCompromisoFGEM(kFGEM.idFGEM(), EstadoCompromisoFGEM.CUMPLIDO, HOY_FGEM, segundaFGEM.idFGEM(), "Hecho");
        assertEquals(EstadoCompromisoFGEM.CUMPLIDO, cumplidoFGEM.estadoFGEM());
        assertEquals(segundaFGEM.idFGEM(), cumplidoFGEM.visitaVerificacionIdFGEM());
        assertEquals("admin", cumplidoFGEM.verificadoPorFGEM());
        assertFalse(cumplidoFGEM.vencidoFGEM(HOY_FGEM));

        CompromisoFGEM reabiertoFGEM = servicioFGEM.verificarCompromisoFGEM(kFGEM.idFGEM(), EstadoCompromisoFGEM.PENDIENTE, null, null, null);
        assertNull(reabiertoFGEM.fechaVerificacionFGEM());
    }

    @Test
    void editarYAnularFGEM() {
        VisitaFGEM vFGEM = servicioFGEM.registrarFGEM(visitaFGEM(HOY_FGEM, Map.of(AspectoInspeccionFGEM.RIEGO, ResultadoAspectoFGEM.CUMPLE), List.of()));
        VisitaFGEM editadaFGEM = servicioFGEM.actualizarFGEM(vFGEM.idFGEM(), visitaFGEM(HOY_FGEM, Map.of(AspectoInspeccionFGEM.SUELO, ResultadoAspectoFGEM.NO_CUMPLE),
                List.of(new DatosVisitaFGEM.NuevoCompromisoFGEM("Abonar la parcela", null))));
        assertEquals(Set.of(AspectoInspeccionFGEM.SUELO), editadaFGEM.aspectosFGEM().keySet());
        assertEquals(1, servicioFGEM.compromisosFGEM().size());

        servicioFGEM.anularFGEM(vFGEM.idFGEM(), "Registrada por error");
        assertEquals(0, servicioFGEM.listarFGEM(false).size());
        assertEquals(0, servicioFGEM.compromisosFGEM().size());                                     // se ocultan con la visita
        assertInvalidoFGEM(() -> servicioFGEM.actualizarFGEM(vFGEM.idFGEM(), visitaFGEM(HOY_FGEM, Map.of(), List.of())));
    }

    @Test
    void agregaYQuitaFotosFGEM() throws Exception {
        VisitaFGEM vFGEM = servicioFGEM.registrarFGEM(visitaFGEM(HOY_FGEM, Map.of(), List.of()));
        Path fotoFGEM = carpetaFGEM.resolve("cobertizo.png");
        ImageIO.write(new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB), "png", fotoFGEM.toFile());
        Path textoFGEM = Files.writeString(carpetaFGEM.resolve("nota.png"), "no soy una imagen");

        FotoVisitaFGEM fFGEM = servicioFGEM.agregarFotoFGEM(vFGEM.idFGEM(), fotoFGEM, "Cobertizo de crías");

        assertTrue(fFGEM.archivoFGEM().startsWith(VisitaServiceFGEM.CARPETA_FOTOS_FGEM + "/" + vFGEM.idFGEM() + "/"));
        assertTrue(Files.exists(servicioFGEM.archivoFGEM(fFGEM)));
        assertEquals(1, servicioFGEM.fotosFGEM(vFGEM.idFGEM()).size());
        assertInvalidoFGEM(() -> servicioFGEM.agregarFotoFGEM(vFGEM.idFGEM(), textoFGEM, null));
        assertInvalidoFGEM(() -> servicioFGEM.agregarFotoFGEM(vFGEM.idFGEM(), carpetaFGEM.resolve("plano.pdf"), null));

        servicioFGEM.eliminarFotoFGEM(fFGEM.idFGEM());
        assertEquals(0, servicioFGEM.fotosFGEM(vFGEM.idFGEM()).size());
        assertFalse(Files.exists(servicioFGEM.archivoFGEM(fFGEM)));
    }

    @Test
    void elProductorVeLasVisitasPeroNoLasRegistraFGEM() {
        servicioFGEM.registrarFGEM(visitaFGEM(HOY_FGEM, Map.of(), List.of()));
        UsuarioFGEM productorFGEM = entornoFGEM.crearUsuarioFGEM("productor", "OPERADOR");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(fundoFGEM.idFGEM(), Set.of(productorFGEM.idFGEM()));
        entornoFGEM.entrarComoFGEM("productor", "Temporal123");
        entornoFGEM.empresaServiceFGEM.seleccionarEmpresaActivaFGEM(fundoFGEM.idFGEM());

        assertEquals(1, servicioFGEM.listarFGEM(false).size());
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> servicioFGEM.registrarFGEM(visitaFGEM(HOY_FGEM, Map.of(), List.of())));
    }

    // ------------------------------------------------------------------ utilidades

    private static DatosVisitaFGEM visitaFGEM(LocalDate fechaFGEM, Map<AspectoInspeccionFGEM, ResultadoAspectoFGEM> aspectosFGEM,
                                      List<DatosVisitaFGEM.NuevoCompromisoFGEM> compromisosFGEM) {
        Map<AspectoInspeccionFGEM, String> notasFGEM = new EnumMap<>(AspectoInspeccionFGEM.class);
        notasFGEM.put(AspectoInspeccionFGEM.VACUNACION, "Vacunas pendientes");
        return new DatosVisitaFGEM(fechaFGEM, TipoVisitaFGEM.SUPERVISION, "Ing. Rosa Quispe", "Anexo Chacapampa", -12.1352,
                -75.2311, "Juan Huamán", "Observaciones de la visita", ConclusionVisitaFGEM.CON_OBSERVACIONES, null,
                aspectosFGEM, notasFGEM, compromisosFGEM);
    }

    private static void assertInvalidoFGEM(Runnable accionFGEM) {
        assertThrows(ValidacionExceptionFGEM.class, accionFGEM::run);
    }
}
