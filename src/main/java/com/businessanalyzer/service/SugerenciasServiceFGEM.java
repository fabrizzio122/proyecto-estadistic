package com.businessanalyzer.service;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.ResumenFinancieroFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM.VentaFGEM;
import com.businessanalyzer.analytics.simulacion.BaseSimulacionFGEM;
import com.businessanalyzer.analytics.simulacion.SimuladorFGEM;
import com.businessanalyzer.analytics.simulacion.SupuestosFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.PeriodoVentasFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.ReposicionFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.ResultadoFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.informes.GeneradorPdfFGEM;
import com.businessanalyzer.informes.LibroExcelFGEM;
import com.businessanalyzer.informes.ListaComprasFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.OperacionFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoInformeFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.repository.InformeRepositoryFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.businessanalyzer.repository.ProductoRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
import com.businessanalyzer.util.FechasFGEM;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class SugerenciasServiceFGEM {

    private static final String CLAVE_MARGEN_FGEM = "sugerencias.margen.";
    private static final BigDecimal MARGEN_MINIMO_FGEM = new BigDecimal("0.01");
    private static final BigDecimal MARGEN_MAXIMO_FGEM = new BigDecimal("0.80");

    private final DatabaseFGEM databaseFGEM;
    private final ProductoRepositoryFGEM productosFGEM;
    private final OperacionRepositoryFGEM operacionesFGEM;
    private final MovimientoRepositoryFGEM movimientosFGEM;
    private final InformeRepositoryFGEM informesFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final ConfiguracionServiceFGEM configuracionFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public SugerenciasServiceFGEM(DatabaseFGEM databaseFGEM, ProductoRepositoryFGEM productosFGEM, OperacionRepositoryFGEM operacionesFGEM,
                                  MovimientoRepositoryFGEM movimientosFGEM, InformeRepositoryFGEM informesFGEM, EmpresaServiceFGEM empresasFGEM, ConfiguracionServiceFGEM configuracionFGEM,
                                  SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.productosFGEM = productosFGEM;
        this.operacionesFGEM = operacionesFGEM;
        this.movimientosFGEM = movimientosFGEM;
        this.informesFGEM = informesFGEM;
        this.empresasFGEM = empresasFGEM;
        this.configuracionFGEM = configuracionFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    public BigDecimal margenObjetivoFGEM() {
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        String guardadoFGEM = configuracionFGEM.obtenerFGEM(CLAVE_MARGEN_FGEM + empresaFGEM.idFGEM(), "");
        try {
            return guardadoFGEM.isBlank() ? SugerenciasProductoFGEM.MARGEN_OBJETIVO_PREDETERMINADO_FGEM : validarFGEM(new BigDecimal(guardadoFGEM));
        } catch (RuntimeException eFGEM) {
            return SugerenciasProductoFGEM.MARGEN_OBJETIVO_PREDETERMINADO_FGEM;
        }
    }

    public ResultadoFGEM sugerenciasFGEM(BigDecimal margenObjetivoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.ANALISIS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        BigDecimal margenFGEM = validarFGEM(margenObjetivoFGEM);
        LocalDate hoyFGEM = LocalDate.now(relojFGEM);
        LocalDate desdeRecienteFGEM = hoyFGEM.minusDays(SugerenciasProductoFGEM.DIAS_ANALISIS_FGEM - 1L);
        LocalDate desdeHistorialFGEM = hoyFGEM.minusDays(SugerenciasProductoFGEM.DIAS_HISTORIAL_FGEM - 1L);

        ResultadoFGEM resultadoFGEM = databaseFGEM.conConexionFGEM(cFGEM -> {
            List<ProductoFGEM> productosFGEM = this.productosFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), false);
            Set<Long> idsFGEM = productosFGEM.stream().map(ProductoFGEM::idFGEM).collect(Collectors.toSet());
            LocalDate primeraFGEM = operacionesFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), TipoOperacionFGEM.VENTA, desdeHistorialFGEM, hoyFGEM, false)
                    .stream().map(OperacionFGEM::fechaFGEM).min(LocalDate::compareTo).orElse(desdeRecienteFGEM);
            PeriodoVentasFGEM recientesFGEM = periodoFGEM(cFGEM, empresaFGEM, idsFGEM, maximoFGEM(desdeRecienteFGEM, primeraFGEM), hoyFGEM);
            PeriodoVentasFGEM historialFGEM = periodoFGEM(cFGEM, empresaFGEM, idsFGEM, maximoFGEM(desdeHistorialFGEM, primeraFGEM), hoyFGEM);
            return SugerenciasProductoFGEM.calcularFGEM(productosFGEM, recientesFGEM, historialFGEM, margenFGEM)
                    .conUtilidadMensualFGEM(utilidadMensualFGEM(cFGEM, empresaFGEM, hoyFGEM));
        });

        try {
            configuracionFGEM.guardarFGEM(CLAVE_MARGEN_FGEM + empresaFGEM.idFGEM(), margenFGEM.stripTrailingZeros().toPlainString());
        } catch (RuntimeException eFGEM) {
        }
        return resultadoFGEM;
    }

    public Path exportarListaComprasFGEM(BigDecimal margenObjetivoFGEM, Path destinoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.INFORMES_GENERAR);
        if (destinoFGEM == null || destinoFGEM.getFileName() == null) {
            throw new ValidacionExceptionFGEM("Elige dónde guardar la lista de compras.");
        }
        String nombreFGEM = destinoFGEM.getFileName().toString().toLowerCase(Locale.ROOT);
        boolean excelFGEM = nombreFGEM.endsWith(".xlsx");
        Path archivoFGEM = excelFGEM || nombreFGEM.endsWith(".pdf") ? destinoFGEM : destinoFGEM.resolveSibling(destinoFGEM.getFileName() + ".pdf");
        if (archivoFGEM.getParent() == null || !Files.isDirectory(archivoFGEM.getParent())) {
            throw new ValidacionExceptionFGEM("La carpeta de destino no existe.");
        }
        ResultadoFGEM resultadoFGEM = sugerenciasFGEM(margenObjetivoFGEM);
        List<ReposicionFGEM> reposicionFGEM = resultadoFGEM.reposicionFGEM();
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        LocalDate hoyFGEM = ahoraFGEM.toLocalDate();
        String periodoFGEM = "según las ventas de los últimos " + resultadoFGEM.diasAnalizadosFGEM() + " días";
        String generadoFGEM = "Generado el " + FechasFGEM.paraMostrarFGEM(ahoraFGEM.truncatedTo(ChronoUnit.MINUTES)) + " por "
                + sesionFGEM.usuarioFGEM().nombreCompletoFGEM();
        TipoInformeFGEM tipoFGEM = excelFGEM ? TipoInformeFGEM.LISTA_COMPRAS_EXCEL : TipoInformeFGEM.LISTA_COMPRAS;
        if (excelFGEM) {
            try (LibroExcelFGEM libroFGEM = ListaComprasFGEM.libroFGEM(reposicionFGEM, empresaFGEM.monedaFGEM(),
                    "Lista de compras - " + empresaFGEM.nombreVisibleFGEM(),
                    "Al " + FechasFGEM.paraMostrarFGEM(hoyFGEM) + ", " + periodoFGEM + ". " + generadoFGEM + ".")) {
                libroFGEM.guardarFGEM(archivoFGEM);
            }
        } else {
            GeneradorPdfFGEM.generarFGEM(new GeneradorPdfFGEM.EncabezadoFGEM(empresaFGEM.nombreVisibleFGEM(),
                            empresaFGEM.razonSocialFGEM() + "  ·  " + empresaFGEM.sectorFGEM().descripcionFGEM(), tipoFGEM.tituloFGEM(),
                            "Al " + FechasFGEM.paraMostrarFGEM(hoyFGEM), generadoFGEM),
                    ListaComprasFGEM.contenidoFGEM(reposicionFGEM, empresaFGEM.monedaFGEM(), periodoFGEM), archivoFGEM);
        }
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        databaseFGEM.enTransaccionFGEM(cFGEM -> {
            informesFGEM.registrarFGEM(cFGEM, empresaFGEM.idFGEM(), tipoFGEM, hoyFGEM, hoyFGEM, archivoFGEM, usuarioFGEM, ahoraFGEM);
            return null;
        });
        return archivoFGEM;
    }

    public ResultadoFGEM sugerenciasActualesFGEM() {
        return sugerenciasFGEM(margenObjetivoFGEM());
    }

    private PeriodoVentasFGEM periodoFGEM(Connection cFGEM, EmpresaFGEM empresaFGEM, Set<Long> idsFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM)
            throws SQLException {
        List<VentaFGEM> ventasFGEM = operacionesFGEM.lineasVendidasFGEM(cFGEM, empresaFGEM.idFGEM(), desdeFGEM, hastaFGEM).stream()
                .filter(vFGEM -> idsFGEM.contains(vFGEM.productoIdFGEM()) && !"AGRO".equals(vFGEM.codigoFGEM()))
                .toList();
        return new PeriodoVentasFGEM(ventasFGEM, (int) ChronoUnit.DAYS.between(desdeFGEM, hastaFGEM) + 1, desdeFGEM,
                operacionesFGEM.ultimasVentasFGEM(cFGEM, empresaFGEM.idFGEM(), desdeFGEM, hastaFGEM));
    }

    private BigDecimal utilidadMensualFGEM(Connection cFGEM, EmpresaFGEM empresaFGEM, LocalDate hoyFGEM) throws SQLException {
        YearMonth ultimoFGEM = YearMonth.from(hoyFGEM).minusMonths(1);
        int mesesFGEM = DiagnosticoServiceFGEM.MESES_EVALUADOS_FGEM;
        PeriodoFGEM periodoFGEM = new PeriodoFGEM(ultimoFGEM.minusMonths(mesesFGEM - 1L).atDay(1), ultimoFGEM.atEndOfMonth());
        BaseSimulacionFGEM baseFGEM = BaseSimulacionFGEM.deFGEM(periodoFGEM, mesesFGEM,
                operacionesFGEM.resumenFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM.desdeFGEM(), periodoFGEM.hastaFGEM()),
                ResumenFinancieroFGEM.deFGEM(movimientosFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM.desdeFGEM(), periodoFGEM.hastaFGEM(), false)));
        if (baseFGEM.sinVentasFGEM()) {
            return null;
        }
        return SimuladorFGEM.simularFGEM(baseFGEM, SupuestosFGEM.neutrosFGEM(baseFGEM.tasaImpuestoEfectivaFGEM())).utilidadNetaFGEM();
    }

    private static LocalDate maximoFGEM(LocalDate aFGEM, LocalDate bFGEM) {
        return aFGEM.isAfter(bFGEM) ? aFGEM : bFGEM;
    }

    private static BigDecimal validarFGEM(BigDecimal margenFGEM) {
        if (margenFGEM == null || margenFGEM.compareTo(MARGEN_MINIMO_FGEM) < 0 || margenFGEM.compareTo(MARGEN_MAXIMO_FGEM) > 0) {
            throw new ValidacionExceptionFGEM("El margen objetivo debe estar entre 1 % y 80 %.");
        }
        return margenFGEM;
    }
}
