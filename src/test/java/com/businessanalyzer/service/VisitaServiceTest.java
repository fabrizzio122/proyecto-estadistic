package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.AspectoInspeccion;
import com.businessanalyzer.model.Compromiso;
import com.businessanalyzer.model.ConclusionVisita;
import com.businessanalyzer.model.DatosEmpresa;
import com.businessanalyzer.model.DatosVisita;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.EstadoCompromiso;
import com.businessanalyzer.model.FotoVisita;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.ResultadoAspecto;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.TamanoEmpresa;
import com.businessanalyzer.model.TipoVisita;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.model.Visita;
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

class VisitaServiceTest {

    /** El reloj de pruebas está en 2026-01-15. */
    private static final LocalDate HOY = LocalDate.of(2026, 1, 15);

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private VisitaService servicio;
    private Empresa fundo;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = entorno.visitas;
        entorno.administradorConectado();
        fundo = entorno.empresaService.crear(new DatosEmpresa("Fundo Los Andes", null, null, Sector.AGROPECUARIO,
                TamanoEmpresa.MICRO, Moneda.PEN, 1, null, null, null, null));
        entorno.empresaService.seleccionarEmpresaActiva(fundo.id());
    }

    @Test
    void registraLaVisitaConSuVerificacionYCompromisos() {
        Visita v = servicio.registrar(visita(HOY.minusDays(5), Map.of(
                AspectoInspeccion.SANIDAD_ANIMAL, ResultadoAspecto.CUMPLE,
                AspectoInspeccion.VACUNACION, ResultadoAspecto.PARCIAL,
                AspectoInspeccion.RIEGO, ResultadoAspecto.NO_APLICA,
                AspectoInspeccion.INSTALACIONES, ResultadoAspecto.NO_CUMPLE),
                List.of(new DatosVisita.NuevoCompromiso("Construir el cobertizo", HOY.plusMonths(1)))));

        assertEquals(4, v.aspectos().size());
        assertEquals(0, new BigDecimal("0.5").compareTo(v.cumplimiento())); // (1 + 0.5 + 0) / 3
        assertEquals("Vacunas pendientes", v.notas().get(AspectoInspeccion.VACUNACION));
        assertEquals("-12.13520, -75.23110", v.coordenadas());
        List<Compromiso> compromisos = servicio.compromisos();
        assertEquals(1, compromisos.size());
        assertEquals(EstadoCompromiso.PENDIENTE, compromisos.get(0).estado());
        assertEquals(ConclusionVisita.CON_OBSERVACIONES, ConclusionVisita.sugerida(v.cumplimiento()));
    }

    @Test
    void validaLosDatos() {
        assertInvalido(() -> servicio.registrar(visita(HOY.plusDays(1), Map.of(), List.of())));
        assertInvalido(() -> servicio.registrar(new DatosVisita(HOY, TipoVisita.SUPERVISION, "Ing. Rosa", null,
                -12.0, null, null, null, ConclusionVisita.FAVORABLE, null, Map.of(), Map.of(), List.of()))); // solo latitud
        assertInvalido(() -> servicio.registrar(new DatosVisita(HOY, TipoVisita.SUPERVISION, "Ing. Rosa", null,
                -95.0, 10.0, null, null, ConclusionVisita.FAVORABLE, null, Map.of(), Map.of(), List.of())));
        assertInvalido(() -> servicio.registrar(new DatosVisita(HOY, TipoVisita.SUPERVISION, "Ing. Rosa", null,
                null, null, null, null, ConclusionVisita.FAVORABLE, HOY, Map.of(), Map.of(), List.of()))); // próxima = hoy
        assertInvalido(() -> servicio.registrar(visita(HOY, Map.of(), List.of(
                new DatosVisita.NuevoCompromiso("Hacer algo útil", HOY.minusDays(1))))));             // plazo vencido
        entorno.crearEmpresaActiva("Bodega");
        assertInvalido(() -> servicio.listar(false));                                            // no agropecuaria
    }

    @Test
    void losCompromisosSeVerificanEnUnaVisitaPosterior() {
        Visita primera = servicio.registrar(visita(HOY.minusMonths(2), Map.of(), List.of(
                new DatosVisita.NuevoCompromiso("Dosificar las alpacas", HOY.minusMonths(1)))));
        Compromiso k = servicio.compromisos().get(0);
        assertTrue(k.vencido(HOY));

        assertInvalido(() -> servicio.verificarCompromiso(k.id(), EstadoCompromiso.CUMPLIDO, HOY, primera.id(), null));
        assertInvalido(() -> servicio.verificarCompromiso(k.id(), EstadoCompromiso.CUMPLIDO,
                HOY.minusMonths(3), null, null));                                               // antes de acordarlo

        Visita segunda = servicio.registrar(visita(HOY, Map.of(), List.of()));
        Compromiso cumplido = servicio.verificarCompromiso(k.id(), EstadoCompromiso.CUMPLIDO, HOY, segunda.id(), "Hecho");
        assertEquals(EstadoCompromiso.CUMPLIDO, cumplido.estado());
        assertEquals(segunda.id(), cumplido.visitaVerificacionId());
        assertEquals("admin", cumplido.verificadoPor());
        assertFalse(cumplido.vencido(HOY));

        Compromiso reabierto = servicio.verificarCompromiso(k.id(), EstadoCompromiso.PENDIENTE, null, null, null);
        assertNull(reabierto.fechaVerificacion());
    }

    @Test
    void editarYAnular() {
        Visita v = servicio.registrar(visita(HOY, Map.of(AspectoInspeccion.RIEGO, ResultadoAspecto.CUMPLE), List.of()));
        Visita editada = servicio.actualizar(v.id(), visita(HOY, Map.of(AspectoInspeccion.SUELO, ResultadoAspecto.NO_CUMPLE),
                List.of(new DatosVisita.NuevoCompromiso("Abonar la parcela", null))));
        assertEquals(Set.of(AspectoInspeccion.SUELO), editada.aspectos().keySet());
        assertEquals(1, servicio.compromisos().size());

        servicio.anular(v.id(), "Registrada por error");
        assertEquals(0, servicio.listar(false).size());
        assertEquals(0, servicio.compromisos().size());                                     // se ocultan con la visita
        assertInvalido(() -> servicio.actualizar(v.id(), visita(HOY, Map.of(), List.of())));
    }

    @Test
    void agregaYQuitaFotos() throws Exception {
        Visita v = servicio.registrar(visita(HOY, Map.of(), List.of()));
        Path foto = carpeta.resolve("cobertizo.png");
        ImageIO.write(new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB), "png", foto.toFile());
        Path texto = Files.writeString(carpeta.resolve("nota.png"), "no soy una imagen");

        FotoVisita f = servicio.agregarFoto(v.id(), foto, "Cobertizo de crías");

        assertTrue(f.archivo().startsWith(VisitaService.CARPETA_FOTOS + "/" + v.id() + "/"));
        assertTrue(Files.exists(servicio.archivo(f)));
        assertEquals(1, servicio.fotos(v.id()).size());
        assertInvalido(() -> servicio.agregarFoto(v.id(), texto, null));
        assertInvalido(() -> servicio.agregarFoto(v.id(), carpeta.resolve("plano.pdf"), null));

        servicio.eliminarFoto(f.id());
        assertEquals(0, servicio.fotos(v.id()).size());
        assertFalse(Files.exists(servicio.archivo(f)));
    }

    @Test
    void elProductorVeLasVisitasPeroNoLasRegistra() {
        servicio.registrar(visita(HOY, Map.of(), List.of()));
        Usuario productor = entorno.crearUsuario("productor", "OPERADOR");
        entorno.empresaService.asignarAccesos(fundo.id(), Set.of(productor.id()));
        entorno.entrarComo("productor", "Temporal123");
        entorno.empresaService.seleccionarEmpresaActiva(fundo.id());

        assertEquals(1, servicio.listar(false).size());
        assertThrows(AccesoDenegadoException.class, () -> servicio.registrar(visita(HOY, Map.of(), List.of())));
    }

    // ------------------------------------------------------------------ utilidades

    private static DatosVisita visita(LocalDate fecha, Map<AspectoInspeccion, ResultadoAspecto> aspectos,
                                      List<DatosVisita.NuevoCompromiso> compromisos) {
        Map<AspectoInspeccion, String> notas = new EnumMap<>(AspectoInspeccion.class);
        notas.put(AspectoInspeccion.VACUNACION, "Vacunas pendientes");
        return new DatosVisita(fecha, TipoVisita.SUPERVISION, "Ing. Rosa Quispe", "Anexo Chacapampa", -12.1352,
                -75.2311, "Juan Huamán", "Observaciones de la visita", ConclusionVisita.CON_OBSERVACIONES, null,
                aspectos, notas, compromisos);
    }

    private static void assertInvalido(Runnable accion) {
        assertThrows(ValidacionException.class, accion::run);
    }
}
