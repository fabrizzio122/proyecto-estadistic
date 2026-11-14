package com.businessanalyzer.service;

import com.businessanalyzer.analytics.EstadoResultados;
import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.RentabilidadProducto;
import com.businessanalyzer.analytics.estadistica.AnalisisAbc;
import com.businessanalyzer.analytics.estadistica.AnalisisEstadistico;
import com.businessanalyzer.analytics.estadistica.AnalisisEstadistico.PuntoMensual;
import com.businessanalyzer.analytics.estadistica.EstacionalidadSemanal;
import com.businessanalyzer.analytics.estadistica.EstadisticaDescriptiva;
import com.businessanalyzer.analytics.estadistica.Histograma;
import com.businessanalyzer.analytics.estadistica.MatrizCorrelacion;
import com.businessanalyzer.analytics.estadistica.Tendencia;
import com.businessanalyzer.config.Database;
import com.businessanalyzer.model.CategoriaFinanciera;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Operacion;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.TipoOperacion;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
import com.businessanalyzer.seguridad.Sesion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Análisis estadístico de las ventas de la empresa activa: descriptiva, distribución,
 * tendencia, estacionalidad semanal, clasificación ABC y correlaciones.
 * Requiere el permiso {@link Permiso#ANALISIS_VER}.
 */
public class EstadisticaService {

    /** Meses completos que se usan para la tendencia y las correlaciones. */
    public static final int MESES_SERIE = 12;
    private static final int MAXIMO_CLASES_HISTOGRAMA = 10;

    private final Database database;
    private final OperacionRepository operaciones;
    private final MovimientoRepository movimientos;
    private final EmpresaService empresas;
    private final Sesion sesion;
    private final Clock reloj;

    public EstadisticaService(Database database, OperacionRepository operaciones, MovimientoRepository movimientos,
                              EmpresaService empresas, Sesion sesion, Clock reloj) {
        this.database = database;
        this.operaciones = operaciones;
        this.movimientos = movimientos;
        this.empresas = empresas;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    public AnalisisEstadistico analizar(LocalDate desde, LocalDate hasta) {
        sesion.requerir(Permiso.ANALISIS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        Periodo periodo = new Periodo(desde, hasta);
        LocalDate hoy = LocalDate.now(reloj);

        return database.conConexion(c -> {
            List<Operacion> ventas = operaciones.listar(c, empresa.id(), TipoOperacion.VENTA, desde, hasta, false);
            double[] tickets = ventas.stream().mapToDouble(o -> o.subtotal().doubleValue()).toArray();
            Map<LocalDate, Double> porDia = ventasPorDia(ventas, desde, hasta.isAfter(hoy) ? hoy : hasta);

            YearMonth mesActual = YearMonth.from(hoy);
            YearMonth ultimoPedido = YearMonth.from(hasta);
            boolean excluirMesActual = !ultimoPedido.isBefore(mesActual);
            YearMonth ultimoCompleto = excluirMesActual ? mesActual.minusMonths(1) : ultimoPedido;
            List<PuntoMensual> serie = serieMensual(c, empresa.id(), ultimoCompleto);

            double[] ventasMensuales = serie.stream().mapToDouble(PuntoMensual::ventas).toArray();
            List<RentabilidadProducto> productos = RentabilidadProducto.agrupar(
                    operaciones.lineasVendidas(c, empresa.id(), desde, hasta));

            return new AnalisisEstadistico(
                    periodo,
                    EstadisticaDescriptiva.de(tickets),
                    EstadisticaDescriptiva.de(porDia.values().stream().mapToDouble(Double::doubleValue).toArray()),
                    Histograma.de(tickets, MAXIMO_CLASES_HISTOGRAMA),
                    EstacionalidadSemanal.de(porDia),
                    serie,
                    Tendencia.de(ventasMensuales),
                    MatrizCorrelacion.de(variables(serie)),
                    AnalisisAbc.de(productos),
                    excluirMesActual);
        });
    }

    /** Total vendido cada día del rango, incluidos los días sin ventas (con cero). */
    private static Map<LocalDate, Double> ventasPorDia(List<Operacion> ventas, LocalDate desde, LocalDate hasta) {
        Map<LocalDate, Double> porDia = new TreeMap<>();
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            porDia.put(dia, 0.0);
        }
        for (Operacion venta : ventas) {
            porDia.computeIfPresent(venta.fecha(), (dia, total) -> total + venta.subtotal().doubleValue());
        }
        return porDia;
    }

    /**
     * Últimos {@value #MESES_SERIE} meses completos hasta {@code ultimo}, sin los meses iniciales
     * en que todavía no había ventas (el negocio aún no operaba en el sistema).
     */
    private List<PuntoMensual> serieMensual(Connection c, long empresaId, YearMonth ultimo) throws SQLException {
        YearMonth primero = ultimo.minusMonths(MESES_SERIE - 1L);
        List<PuntoMensual> puntos = new ArrayList<>();
        for (ConsultaMensual.Mes mes : ConsultaMensual.porMes(c, operaciones, movimientos, empresaId, primero, ultimo)) {
            EstadoResultados estado = EstadoResultados.de(mes.operaciones(), mes.finanzas());
            BigDecimal margen = estado.margenBruto();
            puntos.add(new PuntoMensual(mes.mes(),
                    estado.ventasNetas().doubleValue(),
                    margen == null ? 0 : margen.doubleValue() * 100,
                    estado.gastosOperativos().doubleValue(),
                    mes.finanzas().porCategoria().getOrDefault(CategoriaFinanciera.MARKETING, BigDecimal.ZERO).doubleValue(),
                    estado.utilidadNeta().doubleValue()));
        }
        int inicio = 0;
        while (inicio < puntos.size() && puntos.get(inicio).ventas() == 0) {
            inicio++;
        }
        return puntos.subList(inicio, puntos.size());
    }

    private static Map<String, double[]> variables(List<PuntoMensual> serie) {
        Map<String, double[]> variables = new LinkedHashMap<>();
        variables.put("Ventas netas", serie.stream().mapToDouble(PuntoMensual::ventas).toArray());
        variables.put("Margen bruto %", serie.stream().mapToDouble(PuntoMensual::margenBrutoPorcentaje).toArray());
        variables.put("Gastos operativos", serie.stream().mapToDouble(PuntoMensual::gastosOperativos).toArray());
        variables.put("Publicidad", serie.stream().mapToDouble(PuntoMensual::publicidad).toArray());
        variables.put("Utilidad neta", serie.stream().mapToDouble(PuntoMensual::utilidadNeta).toArray());
        return variables;
    }
}
