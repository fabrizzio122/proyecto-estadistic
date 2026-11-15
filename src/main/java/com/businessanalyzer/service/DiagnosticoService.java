package com.businessanalyzer.service;

import com.businessanalyzer.analytics.EstadoResultados;
import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.RentabilidadProducto;
import com.businessanalyzer.analytics.ResumenFinanciero;
import com.businessanalyzer.analytics.ResumenOperaciones;
import com.businessanalyzer.analytics.diagnostico.DatosDiagnostico;
import com.businessanalyzer.analytics.diagnostico.Diagnostico;
import com.businessanalyzer.analytics.diagnostico.MotorDiagnostico;
import com.businessanalyzer.config.Database;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.repository.ActivoRepository;
import com.businessanalyzer.repository.DeudaRepository;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
import com.businessanalyzer.repository.ProductoRepository;
import com.businessanalyzer.seguridad.Sesion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Diagnóstico empresarial de la empresa activa: reúne los datos de los últimos meses completos y
 * los entrega a {@link MotorDiagnostico}. Requiere el permiso {@link Permiso#ANALISIS_VER}.
 */
public class DiagnosticoService {

    /** Meses completos que se evalúan (y que se comparan con los mismos meses previos). */
    public static final int MESES_EVALUADOS = 3;
    /** Historia mensual usada para la tendencia, la regularidad y la proyección. */
    public static final int MESES_HISTORIA = 24;

    private final Database database;
    private final OperacionRepository operaciones;
    private final MovimientoRepository movimientos;
    private final EmpresaService empresas;
    private final Sesion sesion;
    private final Clock reloj;
    private final ConsultaPatrimonio patrimonio;

    public DiagnosticoService(Database database, OperacionRepository operaciones, MovimientoRepository movimientos,
                              EmpresaService empresas, Sesion sesion, Clock reloj) {
        this.database = database;
        this.operaciones = operaciones;
        this.movimientos = movimientos;
        this.empresas = empresas;
        this.sesion = sesion;
        this.reloj = reloj;
        this.patrimonio = new ConsultaPatrimonio(new ActivoRepository(), new DeudaRepository(),
                new ProductoRepository(), operaciones, movimientos);
    }

    public Diagnostico diagnosticar() {
        sesion.requerir(Permiso.ANALISIS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        YearMonth ultimo = YearMonth.from(LocalDate.now(reloj)).minusMonths(1);
        Periodo periodo = new Periodo(ultimo.minusMonths(MESES_EVALUADOS - 1L).atDay(1), ultimo.atEndOfMonth());
        Periodo anterior = periodo.anterior();

        DatosDiagnostico datos = database.conConexion(c -> {
            ResumenOperaciones ops = operaciones.resumen(c, empresa.id(), periodo.desde(), periodo.hasta());
            ResumenFinanciero fin = ResumenFinanciero.de(
                    movimientos.listar(c, empresa.id(), periodo.desde(), periodo.hasta(), false));
            ResumenOperaciones opsAnterior = operaciones.resumen(c, empresa.id(), anterior.desde(), anterior.hasta());
            List<RentabilidadProducto> productos = RentabilidadProducto.agrupar(
                    operaciones.lineasVendidas(c, empresa.id(), periodo.desde(), periodo.hasta()));
            List<ConsultaMensual.Mes> historia = ConsultaMensual.porMes(c, operaciones, movimientos, empresa.id(),
                    ultimo.minusMonths(MESES_HISTORIA - 1L), ultimo);

            // Se omiten los meses iniciales sin ventas (el negocio aún no operaba en el sistema).
            int inicio = 0;
            while (inicio < historia.size() && historia.get(inicio).operaciones().ventasNetas().signum() == 0) {
                inicio++;
            }
            List<YearMonth> meses = new ArrayList<>();
            double[] ventas = new double[historia.size() - inicio];
            double[] utilidad = new double[historia.size() - inicio];
            for (int i = inicio; i < historia.size(); i++) {
                ConsultaMensual.Mes mes = historia.get(i);
                EstadoResultados estado = EstadoResultados.de(mes.operaciones(), mes.finanzas());
                meses.add(mes.mes());
                ventas[i - inicio] = estado.ventasNetas().doubleValue();
                utilidad[i - inicio] = estado.utilidadNeta().doubleValue();
            }
            return new DatosDiagnostico(empresa.sector(), empresa.moneda(), periodo, MESES_EVALUADOS, ops, fin,
                    opsAnterior, meses, ventas, utilidad, productos,
                    patrimonio.situacion(c, empresa.id(), LocalDate.now(reloj)));
        });
        return MotorDiagnostico.evaluar(datos);
    }
}
