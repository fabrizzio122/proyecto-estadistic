package com.businessanalyzer.service;

import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.agricultura.ResultadoCampania;
import com.businessanalyzer.analytics.ganaderia.ExistenciaValorizada;
import com.businessanalyzer.analytics.ganaderia.IndicadorGanadero;
import com.businessanalyzer.analytics.ganaderia.IndicadoresEspecie;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneral;
import com.businessanalyzer.analytics.patrimonio.IndicadoresPatrimonio;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonial;
import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.informes.ContenidoInforme;
import com.businessanalyzer.informes.GeneradorPdf;
import com.businessanalyzer.model.AspectoInspeccion;
import com.businessanalyzer.model.Compromiso;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.EstadoCampania;
import com.businessanalyzer.model.EstadoCompromiso;
import com.businessanalyzer.model.FotoVisita;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.ResultadoAspecto;
import com.businessanalyzer.model.TipoInforme;
import com.businessanalyzer.model.Visita;
import com.businessanalyzer.repository.InformeRepository;
import com.businessanalyzer.seguridad.Sesion;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Informe técnico en PDF de una visita de inspección: datos de la visita, verificación en campo,
 * compromisos, indicadores del hato y de las campañas de los 12 meses previos a la visita, situación
 * patrimonial, observaciones, conclusión, fotos y espacio para las firmas.
 */
public class InformeTecnicoService {

    /** Fotos que se incluyen como máximo en el informe. */
    static final int MAXIMO_FOTOS = 6;

    private final Database database;
    private final InformeRepository informes;
    private final VisitaService visitas;
    private final GanaderiaService ganaderia;
    private final AgriculturaService agricultura;
    private final PatrimonioService patrimonio;
    private final EmpresaService empresas;
    private final Sesion sesion;
    private final Clock reloj;

    public InformeTecnicoService(Database database, InformeRepository informes, VisitaService visitas,
                                 GanaderiaService ganaderia, AgriculturaService agricultura, PatrimonioService patrimonio,
                                 EmpresaService empresas, Sesion sesion, Clock reloj) {
        this.database = database;
        this.informes = informes;
        this.visitas = visitas;
        this.ganaderia = ganaderia;
        this.agricultura = agricultura;
        this.patrimonio = patrimonio;
        this.empresas = empresas;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    /** Nombre de archivo propuesto: "Informe-tecnico-Fundo-Demo-2026-11-14.pdf". */
    public static String nombreSugerido(Empresa empresa, Visita visita) {
        String limpio = java.text.Normalizer.normalize(empresa.nombreVisible(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return "Informe-tecnico-" + (limpio.isEmpty() ? "productor" : limpio) + "-" + visita.fecha() + ".pdf";
    }

    public Path generar(long visitaId, Path destino) {
        sesion.requerir(Permiso.INFORMES_GENERAR);
        if (destino == null || destino.getFileName() == null) {
            throw new ValidacionException("Elige dónde guardar el informe.");
        }
        Path archivo = destino.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")
                ? destino : destino.resolveSibling(destino.getFileName() + ".pdf");
        Empresa empresa = empresas.empresaDeTrabajo();
        Visita visita = visitas.buscar(visitaId);
        if (visita.anulada()) {
            throw new ValidacionException("La visita está anulada.");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        ContenidoInforme contenido = redactar(empresa, visita);
        GeneradorPdf.generar(encabezado(empresa, visita, ahora), contenido, archivo);
        String usuario = sesion.usuario().nombreUsuario();
        database.enTransaccion(c -> {
            informes.registrar(c, empresa.id(), TipoInforme.INFORME_TECNICO, visita.fecha(), visita.fecha(), archivo,
                    usuario, ahora);
            return null;
        });
        return archivo;
    }

    /** Contenido del informe (público para las pruebas). */
    public ContenidoInforme redactar(Empresa empresa, Visita visita) {
        Moneda moneda = empresa.moneda();
        Periodo periodo = new Periodo(visita.fecha().minusYears(1).plusDays(1), visita.fecha());
        List<ExistenciaValorizada> hato = ganaderia.hato();
        List<IndicadoresEspecie> especies = ganaderia.indicadores(periodo);
        List<ResultadoCampania> campanias = agricultura.listarCampanias(false).stream()
                .filter(r -> !r.campania().fechaSiembra().isAfter(visita.fecha()))
                .filter(r -> r.campania().estado() == EstadoCampania.EN_CURSO || r.campania().fechaCierre() == null
                        || !r.campania().fechaCierre().isBefore(periodo.desde()))
                .toList();
        SituacionPatrimonial situacion = patrimonio.situacion();
        List<Compromiso> compromisos = visitas.compromisos();
        List<FotoVisita> fotos = visitas.fotos(visita.id());

        ContenidoInforme c = new ContenidoInforme();

        // 1. Datos de la visita
        int cabezas = hato.stream().mapToInt(ExistenciaValorizada::cabezas).sum();
        BigDecimal area = campanias.stream().filter(r -> r.campania().estado() == EstadoCampania.EN_CURSO)
                .map(r -> r.campania().area()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cumplimiento = visita.cumplimiento();
        c.indicadores("Cumplimiento en campo", cumplimiento == null ? "—" : Montos.porcentaje(cumplimiento),
                "Conclusión", visita.conclusion().descripcion(),
                "Animales", String.valueOf(cabezas),
                "Área sembrada", Montos.cantidad(area) + " ha");
        c.seccion("1. Datos de la visita");
        c.dato("Productor", empresa.razonSocial());
        if (empresa.identificacionFiscal() != null && !empresa.identificacionFiscal().isBlank()) {
            c.dato("RUC / DNI", empresa.identificacionFiscal());
        }
        if (empresa.direccion() != null && !empresa.direccion().isBlank()) {
            c.dato("Dirección", empresa.direccion());
        }
        c.dato("Fecha y tipo de visita", Fechas.paraMostrar(visita.fecha()) + " · " + visita.tipo().descripcion());
        c.dato("Inspector", visita.inspector());
        c.dato("Atendido por", Objects.requireNonNullElse(visita.atendidoPor(), "—"));
        c.dato("Lugar", Objects.requireNonNullElse(visita.lugar(), "—"));
        if (visita.coordenadas() != null) {
            c.dato("Coordenadas (lat, long)", visita.coordenadas());
        }
        if (visita.proximaVisita() != null) {
            c.dato("Próxima visita", Fechas.paraMostrar(visita.proximaVisita()));
        }

        // 2. Verificación en campo
        c.seccion("2. Verificación en campo");
        if (visita.aspectos().isEmpty()) {
            c.texto("No se registró la verificación de aspectos en esta visita.");
        }
        for (AspectoInspeccion.Area areaInspeccion : AspectoInspeccion.Area.values()) {
            List<AspectoInspeccion> evaluados = java.util.Arrays.stream(AspectoInspeccion.values())
                    .filter(a -> a.area() == areaInspeccion && visita.aspectos().containsKey(a)).toList();
            if (evaluados.isEmpty()) {
                continue;
            }
            c.encabezado(areaInspeccion.descripcion(), "Resultado");
            for (AspectoInspeccion a : evaluados) {
                ResultadoAspecto r = visita.aspectos().get(a);
                c.semaforo(a.descripcion(), r.semaforo(), r.descripcion(),
                        Objects.requireNonNullElse(visita.notas().get(a), a.guia()));
            }
        }
        if (cumplimiento != null) {
            c.nota("Cumplimiento: «cumple» vale 1, «cumple en parte» 0.5 y «no cumple» 0; no se cuentan los aspectos que no aplican.");
        }

        // 3. Compromisos
        List<Compromiso> verificados = compromisos.stream()
                .filter(k -> Objects.equals(k.visitaVerificacionId(), visita.id())).toList();
        List<Compromiso> pendientesPrevios = compromisos.stream()
                .filter(k -> k.visitaId() != visita.id() && k.estado() == EstadoCompromiso.PENDIENTE
                        && k.fechaVisita().isBefore(visita.fecha())).toList();
        List<Compromiso> nuevos = compromisos.stream().filter(k -> k.visitaId() == visita.id()).toList();
        c.seccion("3. Compromisos");
        if (verificados.isEmpty() && pendientesPrevios.isEmpty() && nuevos.isEmpty()) {
            c.texto("No hay compromisos registrados.");
        }
        if (!verificados.isEmpty() || !pendientesPrevios.isEmpty()) {
            c.encabezado("De visitas anteriores", "Acordado", "Plazo", "Estado");
            verificados.forEach(k -> c.fila(k.descripcion(), Fechas.paraMostrar(k.fechaVisita()),
                    Fechas.paraMostrar(k.plazo()), k.estado().descripcion()));
            pendientesPrevios.forEach(k -> c.fila(k.descripcion(), Fechas.paraMostrar(k.fechaVisita()),
                    Fechas.paraMostrar(k.plazo()), k.vencido(visita.fecha()) ? "Pendiente (vencido)" : "Pendiente"));
        }
        if (!nuevos.isEmpty()) {
            c.encabezado("Acordados en esta visita", "Plazo", "Estado");
            nuevos.forEach(k -> c.fila(k.descripcion(), Fechas.paraMostrar(k.plazo()), k.estado().descripcion()));
        }

        // 4. Ganadería
        if (!especies.isEmpty() || !hato.isEmpty()) {
            c.seccion("4. Ganadería (" + periodo.descripcion() + ")");
            c.indicadores("Animales hoy", String.valueOf(cabezas),
                    "Valor del hato", Montos.formatear(ExistenciaValorizada.total(hato), moneda));
            for (IndicadoresEspecie e : especies) {
                c.encabezado(e.especie().descripcion() + ": de " + e.existenciaInicial() + " a " + e.existenciaFinal()
                        + " animales", "Valor", "Estado");
                for (IndicadorGanadero i : e.evaluar(moneda)) {
                    if (i.semaforo().evaluado()) {
                        c.semaforo(i.nombre(), i.semaforo(), i.valor(), i.lectura());
                    } else {
                        c.dato(i.nombre(), i.valor() + "  ·  " + i.lectura());
                    }
                }
            }
        }

        // 5. Agricultura
        if (!campanias.isEmpty()) {
            c.seccion("5. Agricultura");
            c.encabezado("Campaña", "Área (ha)", "Rendimiento", "% referencia", "Margen");
            for (ResultadoCampania r : campanias) {
                BigDecimal rend = r.rendimiento();
                BigDecimal avance = r.avanceReferencia();
                boolean conResultado = r.produccion().signum() > 0 || r.campania().estado().cerrada();
                c.fila(r.campania().cultivo().descripcion() + " · " + r.campania().parcelaNombre() + " ("
                                + r.campania().estado().descripcion().toLowerCase() + ")",
                        Montos.cantidad(r.campania().area()),
                        rend == null ? "—" : Montos.cantidad(rend.setScale(0, RoundingMode.HALF_UP)) + " kg/ha",
                        Montos.porcentaje(avance),
                        conResultado ? Montos.formatear(r.margen(), moneda) : "En curso");
            }
            campanias.stream().filter(r -> r.campania().estado() == EstadoCampania.PERDIDA).forEach(r ->
                    c.nota(r.campania().cultivo().descripcion() + " perdido: " + r.campania().motivo() + "."));
        }

        // 6. Situación patrimonial
        BalanceGeneral b = situacion.balance();
        IndicadoresPatrimonio ip = situacion.indicadores();
        if (!b.sinRegistros()) {
            c.seccion("6. Situación patrimonial (al " + Fechas.paraMostrar(b.corte()) + ")");
            c.fila("Total activos", Montos.formatear(b.totalActivo(), moneda));
            c.fila("Total deudas", Montos.formatear(b.totalPasivo(), moneda));
            c.total("Patrimonio", Montos.formatear(b.patrimonio(), moneda));
            c.fila("Liquidez corriente", ip.liquidezCorriente() == null ? "Sin deudas de corto plazo"
                    : IndicadoresPatrimonio.veces(ip.liquidezCorriente()));
            c.fila("Endeudamiento (deudas ÷ activos)", Montos.porcentaje(ip.endeudamiento()));
            if (b.cuotasMensuales().signum() > 0) {
                c.fila("Cuotas mensuales", Montos.formatear(b.cuotasMensuales(), moneda));
            }
        }

        // 7. Observaciones y conclusión
        c.seccion("7. Observaciones y conclusión");
        c.texto(visita.observaciones() == null ? "Sin observaciones adicionales." : visita.observaciones());
        c.recomendacion(visita.conclusion().descripcion(), visita.conclusion().semaforo(), "Conclusión del inspector",
                conclusionTexto(visita));

        // 8. Fotos
        int incluidas = 0;
        for (FotoVisita f : fotos) {
            if (incluidas == MAXIMO_FOTOS) {
                break;
            }
            BufferedImage imagen = leer(visitas.archivo(f));
            if (imagen == null) {
                continue;
            }
            if (incluidas == 0) {
                c.seccion("8. Registro fotográfico");
            }
            c.grafico(imagen);
            c.nota("Foto " + (incluidas + 1) + (f.descripcion() == null ? "" : ": " + f.descripcion()));
            incluidas++;
        }

        // Firmas
        c.espacio();
        c.firmas("Inspector\n" + visita.inspector(),
                "Productor\n" + Objects.requireNonNullElse(visita.atendidoPor(), empresa.razonSocial()));
        return c;
    }

    private static String conclusionTexto(Visita visita) {
        BigDecimal cumplimiento = visita.cumplimiento();
        long noCumple = visita.aspectos().values().stream().filter(r -> r == ResultadoAspecto.NO_CUMPLE).count();
        String base = cumplimiento == null ? "No se evaluaron aspectos en campo."
                : "El productor cumple el " + Montos.porcentaje(cumplimiento) + " de los aspectos verificados"
                + (noCumple == 0 ? "." : ", con " + noCumple + (noCumple == 1 ? " aspecto que no cumple." : " aspectos que no cumplen."));
        return base + (visita.proximaVisita() == null ? "" : " Próxima visita: " + Fechas.paraMostrar(visita.proximaVisita()) + ".");
    }

    private static BufferedImage leer(Path archivo) {
        try {
            return Files.exists(archivo) ? ImageIO.read(archivo.toFile()) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private GeneradorPdf.Encabezado encabezado(Empresa empresa, Visita visita, LocalDateTime ahora) {
        StringBuilder detalle = new StringBuilder(empresa.razonSocial());
        if (empresa.identificacionFiscal() != null && !empresa.identificacionFiscal().isBlank()) {
            detalle.append("  ·  ").append(empresa.identificacionFiscal());
        }
        detalle.append("  ·  ").append(empresa.sector().descripcion());
        String subtitulo = visita.tipo().descripcion() + " del " + Fechas.paraMostrar(visita.fecha())
                + "  ·  Inspector: " + visita.inspector();
        String generado = "Generado el " + Fechas.paraMostrar(ahora.truncatedTo(ChronoUnit.MINUTES)) + " por "
                + sesion.usuario().nombreCompleto();
        return new GeneradorPdf.Encabezado(empresa.nombreVisible(), detalle.toString(),
                TipoInforme.INFORME_TECNICO.titulo(), subtitulo, generado);
    }
}
