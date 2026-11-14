package com.businessanalyzer.service;

import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.ResumenFinanciero;
import com.businessanalyzer.analytics.ResumenOperaciones;
import com.businessanalyzer.analytics.simulacion.BaseSimulacion;
import com.businessanalyzer.analytics.simulacion.Supuestos;
import com.businessanalyzer.analytics.simulacion.Variable;
import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Escenario;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.repository.EscenarioRepository;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
import com.businessanalyzer.seguridad.Sesion;
import com.businessanalyzer.util.Validaciones;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Simulación de escenarios de la empresa activa: obtiene la base real y administra los
 * escenarios guardados. El cálculo de cada escenario lo hace {@link com.businessanalyzer.analytics.simulacion.Simulador}.
 * Requiere el permiso {@link Permiso#ANALISIS_VER} (simular no modifica los datos del negocio).
 */
public class SimulacionService {

    /** Período que se usa como punto de partida. */
    public enum TipoBase {
        ULTIMO_MES("Último mes completo", 1),
        PROMEDIO_3_MESES("Promedio de los últimos 3 meses", 3),
        PROMEDIO_6_MESES("Promedio de los últimos 6 meses", 6);

        private final String descripcion;
        private final int meses;

        TipoBase(String descripcion, int meses) {
            this.descripcion = descripcion;
            this.meses = meses;
        }

        public String descripcion() {
            return descripcion;
        }

        public int meses() {
            return meses;
        }

        @Override
        public String toString() {
            return descripcion;
        }
    }

    private static final BigDecimal CAMBIO_MINIMO = new BigDecimal("-1");
    private static final BigDecimal CAMBIO_MAXIMO = new BigDecimal("5");
    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("999999999");

    private final Database database;
    private final OperacionRepository operaciones;
    private final MovimientoRepository movimientos;
    private final EscenarioRepository escenarios;
    private final EmpresaService empresas;
    private final Sesion sesion;
    private final Clock reloj;

    public SimulacionService(Database database, OperacionRepository operaciones, MovimientoRepository movimientos,
                             EscenarioRepository escenarios, EmpresaService empresas, Sesion sesion, Clock reloj) {
        this.database = database;
        this.operaciones = operaciones;
        this.movimientos = movimientos;
        this.escenarios = escenarios;
        this.empresas = empresas;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    /** Resultados reales (promedio mensual) de los últimos meses completos. */
    public BaseSimulacion base(TipoBase tipo) {
        sesion.requerir(Permiso.ANALISIS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        YearMonth ultimo = YearMonth.from(LocalDate.now(reloj)).minusMonths(1);
        Periodo periodo = new Periodo(ultimo.minusMonths(tipo.meses() - 1L).atDay(1), ultimo.atEndOfMonth());
        return database.conConexion(c -> {
            ResumenOperaciones ops = operaciones.resumen(c, empresa.id(), periodo.desde(), periodo.hasta());
            ResumenFinanciero fin = ResumenFinanciero.de(
                    movimientos.listar(c, empresa.id(), periodo.desde(), periodo.hasta(), false));
            return BaseSimulacion.de(periodo, tipo.meses(), ops, fin);
        });
    }

    public List<Escenario> listarEscenarios() {
        sesion.requerir(Permiso.ANALISIS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        return database.conConexion(c -> escenarios.listar(c, empresa.id()));
    }

    /** Crea el escenario o, si ya existe uno con ese nombre en la empresa, lo reemplaza. */
    public Escenario guardar(String nombre, String descripcion, Supuestos supuestos) {
        sesion.requerir(Permiso.ANALISIS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        String nombreLimpio = Validaciones.requerido(nombre, "Nombre del escenario", 2, 60);
        String descripcionLimpia = Validaciones.opcional(descripcion, "Descripción", 200);
        validar(supuestos);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            Optional<Escenario> existente = escenarios.buscarPorNombre(c, empresa.id(), nombreLimpio);
            long id;
            if (existente.isPresent()) {
                id = existente.get().id();
                escenarios.actualizar(c, id, nombreLimpio, descripcionLimpia, supuestos, usuario, ahora);
            } else {
                id = escenarios.insertar(c, empresa.id(), nombreLimpio, descripcionLimpia, supuestos, usuario, ahora);
            }
            return escenarios.buscarPorId(c, id).orElseThrow();
        });
    }

    public void eliminar(long id) {
        sesion.requerir(Permiso.ANALISIS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        database.enTransaccion(c -> {
            escenarios.buscarPorId(c, id).filter(e -> e.empresaId() == empresa.id())
                    .orElseThrow(() -> new ValidacionException("El escenario no existe en la empresa activa."));
            escenarios.eliminar(c, id);
            return null;
        });
    }

    /** Límites razonables: cada cambio entre −100 % y +500 %; impuesto entre 0 % y 100 %. */
    public static void validar(Supuestos s) {
        if (s == null) {
            throw new ValidacionException("Faltan los supuestos del escenario.");
        }
        for (Variable variable : Variable.values()) {
            BigDecimal valor = s.valor(variable);
            if (valor.compareTo(CAMBIO_MINIMO) < 0 || valor.compareTo(CAMBIO_MAXIMO) > 0) {
                throw new ValidacionException("El cambio en \"" + variable.descripcion() + "\" debe estar entre −100 % y +500 %.");
            }
        }
        if (s.costoFijoAdicional().abs().compareTo(MONTO_MAXIMO) > 0) {
            throw new ValidacionException("El costo fijo adicional supera el máximo permitido.");
        }
        if (s.tasaImpuesto().signum() < 0 || s.tasaImpuesto().compareTo(BigDecimal.ONE) > 0) {
            throw new ValidacionException("La tasa de impuesto debe estar entre 0 % y 100 %.");
        }
    }
}
