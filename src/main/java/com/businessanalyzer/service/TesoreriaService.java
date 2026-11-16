package com.businessanalyzer.service;

import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.tesoreria.FlujoDinero;
import com.businessanalyzer.analytics.tesoreria.FlujoEfectivo;
import com.businessanalyzer.analytics.tesoreria.FlujoMensual;
import com.businessanalyzer.analytics.tesoreria.ProyeccionCaja;
import com.businessanalyzer.analytics.tesoreria.SaldoCuenta;
import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CuentaDinero;
import com.businessanalyzer.model.DatosCuenta;
import com.businessanalyzer.model.DatosMovimientoTesoreria;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.MovimientoTesoreria;
import com.businessanalyzer.model.Operacion;
import com.businessanalyzer.model.PagoOperacion;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.TipoOperacion;
import com.businessanalyzer.repository.DeudaRepository;
import com.businessanalyzer.repository.OperacionRepository;
import com.businessanalyzer.repository.TesoreriaRepository;
import com.businessanalyzer.seguridad.Sesion;
import com.businessanalyzer.util.Montos;
import com.businessanalyzer.util.Validaciones;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Tesorería: dónde está el dinero de la empresa y cómo se mueve.
 * <ul>
 *   <li>Cuentas de dinero (caja, bancos, billeteras) con su saldo calculado.</li>
 *   <li>Transferencias entre cuentas y aportes o retiros del dueño.</li>
 *   <li>Cuentas por cobrar y por pagar: ventas y compras al crédito con sus cobros y pagos.</li>
 *   <li>Estado de flujo de efectivo, evolución mensual y proyección de caja.</li>
 * </ul>
 * Lectura con {@code FINANZAS_VER}; cuentas y movimientos con {@code FINANZAS_GESTIONAR}; cobros y pagos
 * de ventas o compras con {@code OPERACIONES_GESTIONAR}.
 */
public class TesoreriaService {

    /** Meses completos que se usan para pronosticar el flujo de operación. */
    static final int MESES_HISTORIA = 24;

    /** Resumen para el dashboard. */
    public record Resumen(BigDecimal disponible, BigDecimal porCobrar, BigDecimal porCobrarVencido,
                          BigDecimal porPagar, BigDecimal porPagarVencido, int cuentasNegativas) {
    }

    private final Database database;
    private final TesoreriaRepository tesoreria;
    private final OperacionRepository operaciones;
    private final DeudaRepository deudas;
    private final CuentasEmpresa cuentas;
    private final EmpresaService empresas;
    private final Sesion sesion;
    private final Clock reloj;

    public TesoreriaService(Database database, TesoreriaRepository tesoreria, OperacionRepository operaciones,
                            DeudaRepository deudas, EmpresaService empresas, Sesion sesion, Clock reloj) {
        this.database = database;
        this.tesoreria = tesoreria;
        this.operaciones = operaciones;
        this.deudas = deudas;
        this.cuentas = new CuentasEmpresa(tesoreria);
        this.empresas = empresas;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ cuentas

    /** Cuentas con su saldo a hoy. */
    public List<SaldoCuenta> listarCuentas(boolean incluirInactivas) {
        sesion.requerir(Permiso.FINANZAS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        LocalDate hoy = hoy();
        return database.conConexion(c -> SaldoCuenta.de(tesoreria.listarCuentas(c, empresa.id(), incluirInactivas),
                tesoreria.flujos(c, empresa.id()), hoy));
    }

    /** Cuentas activas, para elegir en los formularios (sin calcular saldos). */
    public List<CuentaDinero> cuentasActivas() {
        Empresa empresa = empresas.empresaDeTrabajo();
        return database.conConexion(c -> tesoreria.listarCuentas(c, empresa.id(), false));
    }

    public CuentaDinero registrarCuenta(DatosCuenta datos) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        DatosCuenta limpios = ValidadorTesoreria.cuenta(datos);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = ahora();
        return database.enTransaccion(c -> {
            exigirNombreLibre(c, empresa.id(), limpios.nombre(), 0);
            long id = tesoreria.insertarCuenta(c, empresa.id(), limpios, usuario, ahora);
            return tesoreria.buscarCuenta(c, id).orElseThrow();
        });
    }

    public CuentaDinero actualizarCuenta(long id, DatosCuenta datos) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        DatosCuenta limpios = ValidadorTesoreria.cuenta(datos);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = ahora();
        return database.enTransaccion(c -> {
            cuentas.exigir(c, empresa.id(), id, id);
            exigirNombreLibre(c, empresa.id(), limpios.nombre(), id);
            tesoreria.actualizarCuenta(c, id, limpios, usuario, ahora);
            return tesoreria.buscarCuenta(c, id).orElseThrow();
        });
    }

    /**
     * Activa o desactiva una cuenta. Para desactivarla debe tener saldo cero (primero se transfiere su
     * dinero) y no puede ser la única cuenta activa.
     */
    public CuentaDinero cambiarCuentaActiva(long id, boolean activa) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = ahora();
        LocalDate hoy = hoy();
        return database.enTransaccion(c -> {
            CuentaDinero cuenta = cuentas.exigir(c, empresa.id(), id, id);
            if (!activa && cuenta.activa()) {
                List<CuentaDinero> activas = tesoreria.listarCuentas(c, empresa.id(), false);
                if (activas.size() <= 1) {
                    throw new ValidacionException("La empresa necesita al menos una cuenta activa.");
                }
                BigDecimal saldo = SaldoCuenta.de(List.of(cuenta), tesoreria.flujos(c, empresa.id()), hoy)
                        .get(0).saldo();
                if (saldo.signum() != 0) {
                    throw new ValidacionException("La cuenta tiene un saldo de " + Montos.numero(saldo)
                            + ". Transfiérelo a otra cuenta antes de desactivarla.");
                }
            }
            tesoreria.cambiarActiva(c, id, activa, usuario, ahora);
            return tesoreria.buscarCuenta(c, id).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ transferencias, aportes y retiros

    public List<MovimientoTesoreria> listarMovimientos(LocalDate desde, LocalDate hasta, boolean incluirAnulados) {
        sesion.requerir(Permiso.FINANZAS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        Periodo periodo = new Periodo(desde, hasta);
        return database.conConexion(c -> tesoreria.listarMovimientos(c, empresa.id(), periodo.desde(),
                periodo.hasta(), incluirAnulados));
    }

    public MovimientoTesoreria registrarMovimiento(DatosMovimientoTesoreria datos) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        DatosMovimientoTesoreria d = ValidadorTesoreria.movimiento(datos, hoy());
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = ahora();
        return database.enTransaccion(c -> {
            CuentaDinero origen = d.cuentaOrigenId() == null ? null
                    : cuentas.exigir(c, empresa.id(), d.cuentaOrigenId(), null);
            CuentaDinero destino = d.cuentaDestinoId() == null ? null
                    : cuentas.exigir(c, empresa.id(), d.cuentaDestinoId(), null);
            String descripcion = d.descripcion() != null ? d.descripcion() : switch (d.tipo()) {
                case TRANSFERENCIA -> "De " + origen.nombre() + " a " + destino.nombre();
                case APORTE -> "Aporte del dueño a " + destino.nombre();
                case RETIRO -> "Retiro del dueño de " + origen.nombre();
            };
            long id = tesoreria.insertarMovimiento(c, empresa.id(), new DatosMovimientoTesoreria(d.tipo(), d.fecha(),
                    d.cuentaOrigenId(), d.cuentaDestinoId(), d.monto(), descripcion), usuario, ahora);
            return tesoreria.buscarMovimiento(c, id).orElseThrow();
        });
    }

    public MovimientoTesoreria anularMovimiento(long id, String motivo) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        String motivoLimpio = Validaciones.requerido(motivo, "Motivo de la anulación", 5, 200);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = ahora();
        return database.enTransaccion(c -> {
            MovimientoTesoreria m = tesoreria.buscarMovimiento(c, id)
                    .filter(x -> x.empresaId() == empresa.id())
                    .orElseThrow(() -> new ValidacionException("El movimiento no existe en la empresa activa."));
            if (m.anulado()) {
                throw new ValidacionException("El movimiento ya está anulado.");
            }
            tesoreria.anularMovimiento(c, id, motivoLimpio, usuario, ahora);
            return tesoreria.buscarMovimiento(c, id).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ cuentas por cobrar y por pagar

    /** Ventas (por cobrar) o compras (por pagar) al crédito con saldo pendiente. */
    public List<Operacion> pendientes(TipoOperacion tipo) {
        sesion.requerir(Permiso.FINANZAS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        return database.conConexion(c -> operaciones.listarPendientes(c, empresa.id(), tipo));
    }

    public List<PagoOperacion> listarPagos(long operacionId) {
        sesion.requerir(Permiso.FINANZAS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        return database.conConexion(c -> {
            exigirOperacion(c, operacionId, empresa.id());
            return tesoreria.listarPagos(c, operacionId);
        });
    }

    /** Registra el cobro de una venta al crédito o el pago de una compra al crédito. */
    public Operacion registrarPago(long operacionId, long cuentaId, LocalDate fecha, BigDecimal monto, String nota) {
        sesion.requerir(Permiso.OPERACIONES_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        LocalDate dia = ValidadorTesoreria.fecha(fecha, hoy());
        BigDecimal importe = ValidadorTesoreria.montoPositivo(monto, "Monto");
        String notaLimpia = Validaciones.opcional(nota, "Nota", 100);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = ahora();
        return database.enTransaccion(c -> {
            Operacion operacion = exigirOperacion(c, operacionId, empresa.id());
            String que = operacion.tipo() == TipoOperacion.VENTA ? "cobro" : "pago";
            if (operacion.anulada()) {
                throw new ValidacionException("La operación " + operacion.numero() + " está anulada.");
            }
            if (!operacion.alCredito()) {
                throw new ValidacionException("La operación " + operacion.numero()
                        + " fue al contado: su dinero ya se registró.");
            }
            if (operacion.pendiente().signum() == 0) {
                throw new ValidacionException("La operación " + operacion.numero() + " ya está pagada por completo.");
            }
            if (importe.compareTo(operacion.pendiente()) > 0) {
                throw new ValidacionException("El " + que + " no puede superar lo pendiente ("
                        + Montos.numero(operacion.pendiente()) + ").");
            }
            if (dia.isBefore(operacion.fecha())) {
                throw new ValidacionException("El " + que + " no puede ser anterior a la operación.");
            }
            cuentas.exigir(c, empresa.id(), cuentaId, null);
            tesoreria.insertarPago(c, operacionId, cuentaId, dia, importe, notaLimpia, usuario, ahora);
            return operaciones.buscarPorId(c, operacionId).orElseThrow();
        });
    }

    public Operacion anularPago(long pagoId) {
        sesion.requerir(Permiso.OPERACIONES_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = ahora();
        return database.enTransaccion(c -> {
            PagoOperacion pago = tesoreria.buscarPago(c, pagoId)
                    .orElseThrow(() -> new ValidacionException("El registro no existe."));
            exigirOperacion(c, pago.operacionId(), empresa.id());
            if (pago.anulado()) {
                throw new ValidacionException("El registro ya está anulado.");
            }
            tesoreria.anularPago(c, pagoId, usuario, ahora);
            return operaciones.buscarPorId(c, pago.operacionId()).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ análisis

    public FlujoEfectivo flujoEfectivo(LocalDate desde, LocalDate hasta) {
        sesion.requerir(Permiso.FINANZAS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        Periodo periodo = new Periodo(desde, hasta);
        return database.conConexion(c -> FlujoEfectivo.de(periodo, tesoreria.listarCuentas(c, empresa.id(), true),
                tesoreria.flujos(c, empresa.id())));
    }

    /** Entradas, salidas y saldo de los últimos {@code meses} meses, incluido el mes en curso. */
    public List<FlujoMensual> evolucion(int meses) {
        sesion.requerir(Permiso.FINANZAS_VER);
        if (meses < 1 || meses > 60) {
            throw new ValidacionException("El rango debe estar entre 1 y 60 meses.");
        }
        Empresa empresa = empresas.empresaDeTrabajo();
        YearMonth actual = YearMonth.from(hoy());
        return database.conConexion(c -> FlujoMensual.serie(actual.minusMonths(meses - 1L), actual,
                tesoreria.listarCuentas(c, empresa.id(), true), tesoreria.flujos(c, empresa.id())));
    }

    public ProyeccionCaja proyeccion() {
        sesion.requerir(Permiso.FINANZAS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        LocalDate hoy = hoy();
        return database.conConexion(c -> proyeccion(tesoreria, deudas, c, empresa.id(), hoy));
    }

    /** También la usa el diagnóstico empresarial. */
    static ProyeccionCaja proyeccion(TesoreriaRepository tesoreria, DeudaRepository deudas, Connection c,
                                     long empresaId, LocalDate hoy) throws SQLException {
        List<CuentaDinero> todas = tesoreria.listarCuentas(c, empresaId, true);
        List<FlujoDinero> flujos = tesoreria.flujos(c, empresaId);
        BigDecimal disponible = SaldoCuenta.total(SaldoCuenta.de(todas, flujos, hoy));
        return ProyeccionCaja.calcular(hoy, disponible, historiaOperacion(todas, flujos, hoy),
                deudas.listar(c, empresaId, false));
    }

    /**
     * Flujo de operación de cada mes completo, desde el primer mes con movimientos en las cuentas (como
     * máximo {@value #MESES_HISTORIA} meses).
     */
    static double[] historiaOperacion(List<CuentaDinero> cuentas, List<FlujoDinero> flujos, LocalDate hoy) {
        YearMonth ultimo = YearMonth.from(hoy).minusMonths(1);
        YearMonth primero = flujos.stream().map(f -> YearMonth.from(f.fecha())).min(YearMonth::compareTo).orElse(null);
        if (primero == null || primero.isAfter(ultimo)) {
            return new double[0];
        }
        YearMonth limite = ultimo.minusMonths(MESES_HISTORIA - 1L);
        if (primero.isBefore(limite)) {
            primero = limite;
        }
        return FlujoMensual.serie(primero, ultimo, cuentas, flujos).stream()
                .mapToDouble(m -> m.operacion().doubleValue()).toArray();
    }

    public Resumen resumen() {
        sesion.requerir(Permiso.FINANZAS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        LocalDate hoy = hoy();
        return database.conConexion(c -> {
            List<SaldoCuenta> saldos = SaldoCuenta.de(tesoreria.listarCuentas(c, empresa.id(), true),
                    tesoreria.flujos(c, empresa.id()), hoy);
            List<Operacion> cobrar = operaciones.listarPendientes(c, empresa.id(), TipoOperacion.VENTA);
            List<Operacion> pagar = operaciones.listarPendientes(c, empresa.id(), TipoOperacion.COMPRA);
            return new Resumen(SaldoCuenta.total(saldos), pendiente(cobrar, null), pendiente(cobrar, hoy),
                    pendiente(pagar, null), pendiente(pagar, hoy),
                    (int) saldos.stream().filter(SaldoCuenta::negativo).count());
        });
    }

    // ------------------------------------------------------------------ utilidades

    /** Suma de lo pendiente; con {@code hoy}, solo lo vencido. */
    static BigDecimal pendiente(List<Operacion> operaciones, LocalDate hoy) {
        return operaciones.stream()
                .filter(o -> hoy == null || o.vencidaEn(hoy))
                .map(Operacion::pendiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Operacion exigirOperacion(Connection c, long id, long empresaId) throws SQLException {
        return operaciones.buscarPorId(c, id)
                .filter(o -> o.empresaId() == empresaId)
                .orElseThrow(() -> new ValidacionException("La operación no existe en la empresa activa."));
    }

    private void exigirNombreLibre(Connection c, long empresaId, String nombre, long excluirId) throws SQLException {
        if (tesoreria.existeNombre(c, empresaId, nombre, excluirId)) {
            throw new ValidacionException("Ya existe una cuenta llamada «" + nombre + "».");
        }
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
