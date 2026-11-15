package com.businessanalyzer.service;

import com.businessanalyzer.analytics.AnalisisFinanciero;
import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.informes.ContenidoInforme;
import com.businessanalyzer.informes.DatosInforme;
import com.businessanalyzer.informes.GeneradorPdf;
import com.businessanalyzer.informes.LibroExcel;
import com.businessanalyzer.informes.RedactorInformes;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.InformeGenerado;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.TipoInforme;
import com.businessanalyzer.model.TipoOperacion;
import com.businessanalyzer.repository.InformeRepository;
import com.businessanalyzer.seguridad.Sesion;
import com.businessanalyzer.util.Fechas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * Genera los informes en PDF (JasperReports) y el libro de Excel (Apache POI) de la empresa
 * activa, y lleva el historial de lo generado.
 * <p>
 * Requiere {@link Permiso#INFORMES_GENERAR}. Además, cada servicio consultado aplica su propio
 * permiso (por ejemplo, el libro de Excel incluye las ventas, así que exige poder verlas).
 */
public class InformeService {

    /** Informes que se muestran en el historial. */
    public static final int LIMITE_HISTORIAL = 50;
    /** Período máximo de un informe, para que el PDF y el libro sigan siendo manejables. */
    public static final int DIAS_MAXIMOS = 3 * 366;

    private static final Logger log = LoggerFactory.getLogger(InformeService.class);

    private final Database database;
    private final InformeRepository informes;
    private final AnalisisService analisis;
    private final DiagnosticoService diagnostico;
    private final OperacionService operaciones;
    private final FinanzasService finanzas;
    private final EmpresaService empresas;
    private final Sesion sesion;
    private final Clock reloj;

    public InformeService(Database database, InformeRepository informes, AnalisisService analisis,
                          DiagnosticoService diagnostico, OperacionService operaciones, FinanzasService finanzas,
                          EmpresaService empresas, Sesion sesion, Clock reloj) {
        this.database = database;
        this.informes = informes;
        this.analisis = analisis;
        this.diagnostico = diagnostico;
        this.operaciones = operaciones;
        this.finanzas = finanzas;
        this.empresas = empresas;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    /**
     * Genera el informe y lo guarda en {@code destino}; si el nombre no tiene la extensión del
     * formato, se le agrega. Devuelve la ruta final del archivo.
     */
    public Path generar(TipoInforme tipo, LocalDate desde, LocalDate hasta, Path destino) {
        sesion.requerir(Permiso.INFORMES_GENERAR);
        if (tipo == null) {
            throw new ValidacionException("Elige el informe que quieres generar.");
        }
        if (tipo.desdeVisita()) {
            throw new ValidacionException("El informe técnico se genera desde «Visitas», eligiendo la visita.");
        }
        Periodo periodo = new Periodo(desde, hasta);
        if (periodo.dias() > DIAS_MAXIMOS) {
            throw new ValidacionException("El período no puede superar los 3 años.");
        }
        if (hasta.isAfter(LocalDate.now(reloj))) {
            throw new ValidacionException("La fecha final no puede ser posterior a hoy.");
        }
        Path archivo = conExtension(destino, tipo);
        if (archivo.getParent() == null || !Files.isDirectory(archivo.getParent())) {
            throw new ValidacionException("La carpeta de destino no existe.");
        }
        Empresa empresa = empresas.empresaDeTrabajo();

        AnalisisFinanciero analisisFinanciero = analisis.analizar(desde, hasta);
        DatosInforme datos = switch (tipo) {
            case INFORME_EJECUTIVO -> new DatosInforme(empresa, analisisFinanciero, diagnostico.diagnosticar(),
                    null, null, null);
            case LIBRO_EXCEL -> new DatosInforme(empresa, analisisFinanciero, null,
                    operaciones.listar(TipoOperacion.VENTA, desde, hasta, true),
                    operaciones.listar(TipoOperacion.COMPRA, desde, hasta, true),
                    finanzas.listar(desde, hasta, true));
            default -> new DatosInforme(empresa, analisisFinanciero, null, null, null, null);
        };

        RedactorInformes redactor = new RedactorInformes(datos);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (tipo == TipoInforme.LIBRO_EXCEL) {
            try (LibroExcel libro = redactor.libroExcel()) {
                libro.guardar(archivo);
            }
        } else {
            ContenidoInforme contenido = switch (tipo) {
                case INFORME_EJECUTIVO -> redactor.informeEjecutivo();
                case ESTADO_RESULTADOS -> redactor.estadoResultados();
                default -> redactor.rentabilidadProductos();
            };
            GeneradorPdf.generar(encabezado(empresa, tipo, periodo, ahora), contenido, archivo);
        }

        String usuario = sesion.usuario().nombreUsuario();
        database.enTransaccion(c -> {
            informes.registrar(c, empresa.id(), tipo, desde, hasta, archivo, usuario, ahora);
            return null;
        });
        log.info("Informe {} generado por {} para la empresa {}: {}", tipo, usuario, empresa.id(), archivo);
        return archivo;
    }

    public List<InformeGenerado> historial() {
        sesion.requerir(Permiso.INFORMES_GENERAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        return database.conConexion(c -> informes.listar(c, empresa.id(), LIMITE_HISTORIAL));
    }

    /** Nombre de archivo sugerido, por ejemplo {@code Informe-ejecutivo_Bodega-Demo_2026-08-01_2026-10-31.pdf}. */
    public static String nombreSugerido(TipoInforme tipo, Empresa empresa, LocalDate desde, LocalDate hasta) {
        return seguro(tipo.titulo()) + "_" + seguro(empresa.nombreVisible()) + "_" + desde + "_" + hasta
                + "." + tipo.formato().extension();
    }

    static Path conExtension(Path destino, TipoInforme tipo) {
        if (destino == null || destino.getFileName() == null) {
            throw new ValidacionException("Elige dónde guardar el informe.");
        }
        String nombre = destino.getFileName().toString();
        String extension = "." + tipo.formato().extension();
        return nombre.toLowerCase(Locale.ROOT).endsWith(extension) ? destino : destino.resolveSibling(nombre + extension);
    }

    private GeneradorPdf.Encabezado encabezado(Empresa empresa, TipoInforme tipo, Periodo periodo, LocalDateTime ahora) {
        StringBuilder detalle = new StringBuilder(empresa.razonSocial());
        if (empresa.identificacionFiscal() != null && !empresa.identificacionFiscal().isBlank()) {
            detalle.append("  ·  RUC ").append(empresa.identificacionFiscal());
        }
        detalle.append("  ·  ").append(empresa.sector().descripcion()).append("  ·  Moneda: ").append(empresa.moneda().nombre());
        String subtitulo = "Período: " + periodo.descripcion()
                + "  (" + Fechas.paraMostrar(periodo.desde()) + " al "
                + Fechas.paraMostrar(periodo.hasta()) + ")";
        String generado = "Generado el " + Fechas.paraMostrar(ahora.truncatedTo(ChronoUnit.MINUTES)) + " por "
                + sesion.usuario().nombreCompleto();
        return new GeneradorPdf.Encabezado(empresa.nombreVisible(), detalle.toString(), tipo.titulo(), subtitulo, generado);
    }

    private static String seguro(String texto) {
        String limpio = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return limpio.isEmpty() ? "informe" : limpio;
    }
}
