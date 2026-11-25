package com.businessanalyzer.service;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.ResumenFinancieroFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM.VentaFGEM;
import com.businessanalyzer.analytics.simulacion.BaseSimulacionFGEM;
import com.businessanalyzer.analytics.simulacion.SimuladorFGEM;
import com.businessanalyzer.analytics.simulacion.SupuestosFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.PeriodoVentasFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.ResultadoFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.OperacionFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.businessanalyzer.repository.ProductoRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sugerencias por producto de la empresa activa: precios, reposición y productos sin movimiento.
 * El margen objetivo se recuerda por empresa.
 */
public class SugerenciasServiceFGEM {

    private static final String CLAVE_MARGEN_FGEM = "sugerencias.margen.";
    private static final BigDecimal MARGEN_MINIMO_FGEM = new BigDecimal("0.01");
    private static final BigDecimal MARGEN_MAXIMO_FGEM = new BigDecimal("0.80");

    private final DatabaseFGEM databaseFGEM;
    private final ProductoRepositoryFGEM productosFGEM;
    private final OperacionRepositoryFGEM operacionesFGEM;
    private final MovimientoRepositoryFGEM movimientosFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final ConfiguracionServiceFGEM configuracionFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public SugerenciasServiceFGEM(DatabaseFGEM databaseFGEM, ProductoRepositoryFGEM productosFGEM, OperacionRepositoryFGEM operacionesFGEM,
                                  MovimientoRepositoryFGEM movimientosFGEM, EmpresaServiceFGEM empresasFGEM, ConfiguracionServiceFGEM configuracionFGEM,
                                  SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.productosFGEM = productosFGEM;
        this.operacionesFGEM = operacionesFGEM;
        this.movimientosFGEM = movimientosFGEM;
        this.empresasFGEM = empresasFGEM;
        this.configuracionFGEM = configuracionFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    /** Margen objetivo guardado para la empresa activa (20 % si nunca se cambió). */
    public BigDecimal margenObjetivoFGEM() {
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        String guardadoFGEM = configuracionFGEM.obtenerFGEM(CLAVE_MARGEN_FGEM + empresaFGEM.idFGEM(), "");
        try {
            return guardadoFGEM.isBlank() ? SugerenciasProductoFGEM.MARGEN_OBJETIVO_PREDETERMINADO_FGEM : validarFGEM(new BigDecimal(guardadoFGEM));
        } catch (RuntimeException eFGEM) {
            return SugerenciasProductoFGEM.MARGEN_OBJETIVO_PREDETERMINADO_FGEM;
        }
    }

    /**
     * Calcula las sugerencias con el margen indicado (fracción: 0.20 = 20 %) y lo recuerda para la empresa.
     */
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
            // Si la empresa empezó a vender hace menos tiempo, los promedios diarios usan solo los días con datos.
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
            // Recordar el margen es solo una comodidad.
        }
        return resultadoFGEM;
    }

    private PeriodoVentasFGEM periodoFGEM(Connection cFGEM, EmpresaFGEM empresaFGEM, Set<Long> idsFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM)
            throws SQLException {
        // Solo las líneas del catálogo (las ventas agropecuarias usan otros identificadores).
        List<VentaFGEM> ventasFGEM = operacionesFGEM.lineasVendidasFGEM(cFGEM, empresaFGEM.idFGEM(), desdeFGEM, hastaFGEM).stream()
                .filter(vFGEM -> idsFGEM.contains(vFGEM.productoIdFGEM()) && !"AGRO".equals(vFGEM.codigoFGEM()))
                .toList();
        return new PeriodoVentasFGEM(ventasFGEM, (int) ChronoUnit.DAYS.between(desdeFGEM, hastaFGEM) + 1, desdeFGEM,
                operacionesFGEM.ultimasVentasFGEM(cFGEM, empresaFGEM.idFGEM(), desdeFGEM, hastaFGEM));
    }

    /**
     * Utilidad neta mensual con la misma base que el diagnóstico (promedio de los últimos meses completos), para
     * poner en contexto la ganancia extra de los precios. {@code null} si no hubo ventas.
     */
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
