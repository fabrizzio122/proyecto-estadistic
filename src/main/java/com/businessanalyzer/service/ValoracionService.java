package com.businessanalyzer.service;

import com.businessanalyzer.analytics.EstadoResultados;
import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.estadistica.Tendencia;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneral;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonial;
import com.businessanalyzer.analytics.valoracion.SupuestosValoracion;
import com.businessanalyzer.analytics.valoracion.ValoracionNegocio;
import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.informes.ContenidoInforme;
import com.businessanalyzer.informes.GeneradorPdf;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.TipoInforme;
import com.businessanalyzer.repository.ActivoRepository;
import com.businessanalyzer.repository.DeudaRepository;
import com.businessanalyzer.repository.InformeRepository;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
import com.businessanalyzer.repository.ProductoRepository;
import com.businessanalyzer.seguridad.Sesion;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;

/**
 * Valoración del negocio de la empresa activa con los datos de hoy: balance general (incluye ganado
 * y cultivos de los productores) y utilidad de los últimos 12 meses completos. Requiere
 * {@link Permiso#ANALISIS_VER}; exportar el informe, {@link Permiso#INFORMES_GENERAR}.
 * <p>
 * Los supuestos (tasa de descuento y crecimiento) se guardan por empresa. Si no se eligió un
 * crecimiento, se propone el de la tendencia de las ventas del último año, acotado entre −10 % y 10 %.
 */
public class ValoracionService {

    private static final String CLAVE = "valoracion.";
    static final BigDecimal CRECIMIENTO_MINIMO = new BigDecimal("-0.10");
    static final BigDecimal CRECIMIENTO_MAXIMO = new BigDecimal("0.10");

    /** Valoración con los datos que la sustentan. */
    public record Resultado(ValoracionNegocio valoracion, SituacionPatrimonial situacion, BigDecimal crecimientoSugerido,
                            Periodo periodoResultados) {
    }

    private final Database database;
    private final OperacionRepository operaciones;
    private final MovimientoRepository movimientos;
    private final ConsultaPatrimonio patrimonio;
    private final InformeRepository informes;
    private final EmpresaService empresas;
    private final ConfiguracionService configuracion;
    private final Sesion sesion;
    private final Clock reloj;

    public ValoracionService(Database database, OperacionRepository operaciones, MovimientoRepository movimientos,
                             InformeRepository informes, EmpresaService empresas, ConfiguracionService configuracion,
                             Sesion sesion, Clock reloj) {
        this.database = database;
        this.operaciones = operaciones;
        this.movimientos = movimientos;
        this.patrimonio = new ConsultaPatrimonio(new ActivoRepository(), new DeudaRepository(), new ProductoRepository(),
                operaciones, movimientos);
        this.informes = informes;
        this.empresas = empresas;
        this.configuracion = configuracion;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    /** Supuestos guardados para la empresa activa, o los propuestos si aún no se guardaron. */
    public SupuestosValoracion supuestos() {
        sesion.requerir(Permiso.ANALISIS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        BigDecimal tasa = leer(empresa, "tasa", SupuestosValoracion.TASA_PREDETERMINADA);
        BigDecimal crecimiento = leer(empresa, "crecimiento", null);
        if (crecimiento == null) {
            crecimiento = database.conConexion(c -> crecimientoSugerido(c, empresa.id()));
        }
        try {
            return SupuestosValoracion.con(tasa, crecimiento);
        } catch (ValidacionException e) {
            return SupuestosValoracion.con(SupuestosValoracion.TASA_PREDETERMINADA, BigDecimal.ZERO);
        }
    }

    /** Guarda los supuestos de la empresa activa ({@code null} en el crecimiento vuelve al sugerido). */
    public void guardarSupuestos(BigDecimal tasa, BigDecimal crecimiento) {
        sesion.requerir(Permiso.ANALISIS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        SupuestosValoracion.con(tasa, crecimiento == null ? BigDecimal.ZERO : crecimiento); // valida
        configuracion.guardar(CLAVE + empresa.id() + ".tasa", tasa.toPlainString());
        configuracion.guardar(CLAVE + empresa.id() + ".crecimiento", crecimiento == null ? "" : crecimiento.toPlainString());
    }

    public Resultado valorar(SupuestosValoracion supuestos) {
        sesion.requerir(Permiso.ANALISIS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        LocalDate hoy = LocalDate.now(reloj);
        return database.conConexion(c -> {
            SituacionPatrimonial s = patrimonio.situacion(c, empresa.id(), hoy);
            BalanceGeneral b = s.balance();
            ValoracionNegocio v = ValoracionNegocio.calcular(supuestos, b.patrimonio(), s.indicadores().utilidadAnual(),
                    b.depreciacionMensual(), s.indicadores().mesesResultados());
            return new Resultado(v, s, crecimientoSugerido(c, empresa.id()), s.periodoResultados());
        });
    }

    /**
     * Crecimiento anual de la tendencia de las ventas de los últimos 12 meses completos (desde el primer
     * mes con ventas). Sin tendencia clara o con menos de 6 meses, 0 %.
     */
    BigDecimal crecimientoSugerido(Connection c, long empresaId) throws SQLException {
        YearMonth ultimo = YearMonth.from(LocalDate.now(reloj)).minusMonths(1);
        double[] ventas = ConsultaMensual.porMes(c, operaciones, movimientos, empresaId, ultimo.minusMonths(11), ultimo)
                .stream().mapToDouble(m -> EstadoResultados.de(m.operaciones(), m.finanzas()).ventasNetas().doubleValue())
                .toArray();
        int inicio = 0;
        while (inicio < ventas.length && ventas[inicio] == 0) {
            inicio++;
        }
        double[] serie = Arrays.copyOfRange(ventas, inicio, ventas.length);
        if (serie.length < 6) {
            return BigDecimal.ZERO;
        }
        Tendencia t = Tendencia.de(serie);
        if (t.direccion() != Tendencia.Direccion.CRECIENTE && t.direccion() != Tendencia.Direccion.DECRECIENTE) {
            return BigDecimal.ZERO;
        }
        double anual = Math.pow(1 + t.crecimientoMensual(), 12) - 1;
        return BigDecimal.valueOf(anual).setScale(4, RoundingMode.HALF_UP).max(CRECIMIENTO_MINIMO).min(CRECIMIENTO_MAXIMO);
    }

    // ------------------------------------------------------------------ informe

    /** Informe PDF de la valoración con los supuestos indicados; queda en el historial de informes. */
    public Path exportar(SupuestosValoracion supuestos, Path destino) {
        sesion.requerir(Permiso.INFORMES_GENERAR);
        if (destino == null || destino.getFileName() == null) {
            throw new ValidacionException("Elige dónde guardar el informe.");
        }
        Path archivo = destino.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")
                ? destino : destino.resolveSibling(destino.getFileName() + ".pdf");
        Empresa empresa = empresas.empresaDeTrabajo();
        Resultado r = valorar(supuestos);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDate hoy = ahora.toLocalDate();
        GeneradorPdf.generar(new GeneradorPdf.Encabezado(empresa.nombreVisible(), empresa.razonSocial() + "  ·  "
                        + empresa.sector().descripcion(), TipoInforme.VALORACION.titulo(),
                        "Valoración al " + Fechas.paraMostrar(hoy),
                        "Generado el " + Fechas.paraMostrar(ahora.truncatedTo(ChronoUnit.MINUTES)) + " por "
                                + sesion.usuario().nombreCompleto()),
                contenido(r, empresa.moneda()), archivo);
        String usuario = sesion.usuario().nombreUsuario();
        database.enTransaccion(c -> {
            informes.registrar(c, empresa.id(), TipoInforme.VALORACION, hoy, hoy, archivo, usuario, ahora);
            return null;
        });
        return archivo;
    }

    /** Contenido del informe (público para las pruebas). */
    public static ContenidoInforme contenido(Resultado r, Moneda moneda) {
        ValoracionNegocio v = r.valoracion();
        SupuestosValoracion s = v.supuestos();
        BalanceGeneral b = r.situacion().balance();
        ContenidoInforme c = new ContenidoInforme();
        c.indicadores("Valor de referencia", Montos.formatear(v.valorReferencia(), moneda),
                "Valor patrimonial", Montos.formatear(v.patrimonial(), moneda),
                "Por rentabilidad", v.valorRentabilidad() == null ? "—" : Montos.formatear(v.valorRentabilidad(), moneda),
                "Fondo de comercio", v.fondoComercio() == null ? "—" : Montos.formatear(v.fondoComercio(), moneda));
        c.seccion("1. Resumen");
        v.lectura(moneda).forEach(c::texto);

        c.seccion("2. Valor patrimonial");
        c.fila("Total activos", Montos.formatear(b.totalActivo(), moneda));
        c.fila("(−) Total deudas", Montos.formatear(b.totalPasivo(), moneda));
        c.total("= Valor patrimonial", Montos.formatear(v.patrimonial(), moneda));
        c.nota("Activos fijos a su valor actual (descontada la depreciación); ganado a precio de mercado; cultivos en "
                + "proceso al costo; inventario al costo.");

        c.seccion("3. Valor por rentabilidad");
        c.dato("Utilidad neta anual", Montos.formatear(v.utilidadAnual(), moneda)
                + (r.periodoResultados() == null ? " (sin ventas en los últimos 12 meses)"
                : "  ·  " + r.periodoResultados().descripcion() + ", llevada a 12 meses"));
        c.dato("Depreciación anual", Montos.formatear(v.depreciacionAnual(), moneda) + "  ·  se suma porque no es salida de dinero");
        c.dato("Supuestos", "Tasa de descuento " + Montos.porcentaje(s.tasaDescuento()) + "  ·  crecimiento "
                + Montos.porcentaje(s.crecimiento()) + " anual por " + s.anios() + " años  ·  después "
                + Montos.porcentaje(s.crecimientoLargoPlazo()));
        c.fila("Capitalización de utilidades (utilidad ÷ tasa)",
                v.capitalizacion() == null ? "—" : Montos.formatear(v.capitalizacion(), moneda));
        c.fila("Flujos descontados", v.flujosDescontados() == null ? "—" : Montos.formatear(v.flujosDescontados(), moneda));
        if (!v.proyeccion().isEmpty()) {
            c.encabezado("Proyección", "Flujo", "Factor", "Valor presente");
            v.proyeccion().forEach(p -> c.fila("Año " + p.anio(), Montos.formatear(p.flujo(), moneda),
                    p.factor().toPlainString(), Montos.formatear(p.valorPresente(), moneda)));
            c.fila("Valor terminal", Montos.formatear(v.valorTerminal(), moneda), "",
                    Montos.formatear(v.valorTerminalPresente(), moneda));
            c.total("Total", "", "", Montos.formatear(v.flujosDescontados(), moneda));

            ValoracionNegocio.Sensibilidad sens = v.sensibilidad();
            c.seccion("4. Sensibilidad (flujos descontados)");
            String[] encabezado = sens.crecimientos().stream().map(g -> "Crec. " + Montos.porcentaje(g)).toArray(String[]::new);
            c.encabezado("Tasa de descuento", encabezado);
            for (int i = 0; i < sens.tasas().size(); i++) {
                String[] fila = new String[sens.crecimientos().size()];
                for (int j = 0; j < fila.length; j++) {
                    fila[j] = sens.valores()[i][j] == null ? "—" : Montos.formatear(sens.valores()[i][j], moneda);
                }
                c.fila(Montos.porcentaje(sens.tasas().get(i)), fila);
            }
        }
        c.nota("La valoración es una estimación con la información registrada en el sistema; no reemplaza una tasación "
                + "profesional. Una tasa de descuento mayor refleja más riesgo y da un valor menor.");
        return c;
    }

    // ------------------------------------------------------------------ utilidades

    private BigDecimal leer(Empresa empresa, String campo, BigDecimal porDefecto) {
        String texto = configuracion.obtener(CLAVE + empresa.id() + "." + campo, "");
        if (texto.isBlank()) {
            return porDefecto;
        }
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}
