package com.businessanalyzer.service;

import com.businessanalyzer.analytics.EstadoResultados;
import com.businessanalyzer.analytics.prediccion.Pronosticador;
import com.businessanalyzer.analytics.prediccion.ResultadoPrediccion;
import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
import com.businessanalyzer.seguridad.Sesion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Proyección de ventas y utilidad neta de la empresa activa a partir de su historia mensual.
 * Usa solo meses completos: el mes en curso es el primero que se proyecta.
 * Requiere el permiso {@link Permiso#ANALISIS_VER}.
 */
public class PrediccionService {

    /** Meses de historia que se consideran como máximo. */
    public static final int MESES_HISTORIA = 24;
    public static final int HORIZONTE_MAXIMO = 12;

    private final Database database;
    private final OperacionRepository operaciones;
    private final MovimientoRepository movimientos;
    private final EmpresaService empresas;
    private final Sesion sesion;
    private final Clock reloj;

    public PrediccionService(Database database, OperacionRepository operaciones, MovimientoRepository movimientos,
                             EmpresaService empresas, Sesion sesion, Clock reloj) {
        this.database = database;
        this.operaciones = operaciones;
        this.movimientos = movimientos;
        this.empresas = empresas;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    public ResultadoPrediccion predecir(int horizonte) {
        sesion.requerir(Permiso.ANALISIS_VER);
        if (horizonte < 1 || horizonte > HORIZONTE_MAXIMO) {
            throw new ValidacionException("El horizonte debe estar entre 1 y " + HORIZONTE_MAXIMO + " meses.");
        }
        Empresa empresa = empresas.empresaDeTrabajo();
        YearMonth ultimoCompleto = YearMonth.from(LocalDate.now(reloj)).minusMonths(1);
        YearMonth primero = ultimoCompleto.minusMonths(MESES_HISTORIA - 1L);

        List<ConsultaMensual.Mes> historia = database.conConexion(c ->
                ConsultaMensual.porMes(c, operaciones, movimientos, empresa.id(), primero, ultimoCompleto));

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

        return new ResultadoPrediccion(meses, ventas, utilidad, horizonte,
                Pronosticador.pronosticar("Ventas netas", ventas, horizonte, true),
                Pronosticador.pronosticar("Utilidad neta", utilidad, horizonte, false),
                true);
    }
}
