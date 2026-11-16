package com.businessanalyzer.service;

import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonial;
import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Activo;
import com.businessanalyzer.model.CategoriaActivo;
import com.businessanalyzer.model.CategoriaFinanciera;
import com.businessanalyzer.model.DatosActivo;
import com.businessanalyzer.model.DatosDeuda;
import com.businessanalyzer.model.DatosMovimiento;
import com.businessanalyzer.model.TipoMovimiento;
import com.businessanalyzer.model.Deuda;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.PagoDeuda;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.repository.ActivoRepository;
import com.businessanalyzer.repository.DeudaRepository;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.OperacionRepository;
import com.businessanalyzer.repository.ProductoRepository;
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
import java.util.List;

/**
 * Activos, deudas y balance general de la empresa activa. Usa los permisos de Gestión
 * financiera: {@link Permiso#FINANZAS_VER} para consultar y {@link Permiso#FINANZAS_GESTIONAR}
 * para registrar.
 * <p>
 * Nada se elimina: los activos se dan de baja (venta, pérdida o error) y las deudas y pagos se
 * anulan, conservando quién lo hizo y cuándo.
 */
public class PatrimonioService {

    private final Database database;
    private final ActivoRepository activos;
    private final DeudaRepository deudas;
    private final MovimientoRepository movimientos;
    private final ConsultaPatrimonio consulta;
    private final CuentasEmpresa cuentas = new CuentasEmpresa(new TesoreriaRepository());
    private final EmpresaService empresas;
    private final Sesion sesion;
    private final Clock reloj;

    public PatrimonioService(Database database, ActivoRepository activos, DeudaRepository deudas,
                             ProductoRepository productos, OperacionRepository operaciones,
                             MovimientoRepository movimientos, EmpresaService empresas, Sesion sesion, Clock reloj) {
        this.database = database;
        this.activos = activos;
        this.deudas = deudas;
        this.movimientos = movimientos;
        this.consulta = new ConsultaPatrimonio(activos, deudas, productos, operaciones, movimientos);
        this.empresas = empresas;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    /** Balance general a hoy con sus indicadores. */
    public SituacionPatrimonial situacion() {
        sesion.requerir(Permiso.FINANZAS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        LocalDate hoy = LocalDate.now(reloj);
        return database.conConexion(c -> consulta.situacion(c, empresa.id(), hoy));
    }

    // ------------------------------------------------------------------ activos

    public List<Activo> listarActivos(boolean incluirBajas) {
        sesion.requerir(Permiso.FINANZAS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        return database.conConexion(c -> activos.listar(c, empresa.id(), incluirBajas));
    }

    public Activo registrarActivo(DatosActivo datos) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        DatosActivo limpios = ValidadorPatrimonio.activo(datos, LocalDate.now(reloj));
        exigirNoEsEfectivo(limpios, null);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            cuentas.opcional(c, empresa.id(), limpios.cuentaId(), null);
            long id = activos.insertar(c, empresa.id(), limpios, usuario, ahora);
            return activos.buscarPorId(c, id).orElseThrow();
        });
    }

    /** El dinero se registra en «Tesorería» (cuentas con saldo calculado), ya no como un activo escrito a mano. */
    private static void exigirNoEsEfectivo(DatosActivo d, Activo actual) {
        if (d.categoria() == CategoriaActivo.CAJA_BANCOS
                && (actual == null || actual.categoria() != CategoriaActivo.CAJA_BANCOS)) {
            throw new ValidacionException("El efectivo y las cuentas bancarias se registran en «Tesorería»: "
                    + "crea allí la cuenta con su saldo inicial.");
        }
    }

    public Activo actualizarActivo(long id, DatosActivo datos) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        DatosActivo limpios = ValidadorPatrimonio.activo(datos, LocalDate.now(reloj));
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            Activo actual = exigirActivo(c, id, empresa.id());
            if (actual.dadoDeBaja()) {
                throw new ValidacionException("Un activo dado de baja no se puede modificar.");
            }
            exigirNoEsEfectivo(limpios, actual);
            cuentas.opcional(c, empresa.id(), limpios.cuentaId(), actual.cuentaId());
            activos.actualizar(c, id, limpios, usuario, ahora);
            return activos.buscarPorId(c, id).orElseThrow();
        });
    }

    /** Retira el activo del balance desde la fecha indicada (venta, pérdida, desuso o registro erróneo). */
    public Activo darDeBaja(long id, LocalDate fecha, String motivo) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate fechaBaja = ValidadorPatrimonio.fecha(fecha, "Fecha de baja", hoy);
        String motivoLimpio = Validaciones.requerido(motivo, "Motivo de la baja", 5, 200);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            Activo actual = exigirActivo(c, id, empresa.id());
            if (actual.dadoDeBaja()) {
                throw new ValidacionException("El activo ya fue dado de baja.");
            }
            if (fechaBaja.isBefore(actual.fecha())) {
                throw new ValidacionException("La fecha de baja no puede ser anterior a la de registro del activo.");
            }
            activos.darDeBaja(c, id, fechaBaja, motivoLimpio, usuario, ahora);
            return activos.buscarPorId(c, id).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ deudas

    public List<Deuda> listarDeudas(boolean incluirAnuladas) {
        sesion.requerir(Permiso.FINANZAS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        return database.conConexion(c -> deudas.listar(c, empresa.id(), incluirAnuladas));
    }

    public Deuda registrarDeuda(DatosDeuda datos) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        DatosDeuda limpios = ValidadorPatrimonio.deuda(datos, LocalDate.now(reloj));
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            cuentas.opcional(c, empresa.id(), limpios.cuentaId(), null);
            long id = deudas.insertar(c, empresa.id(), limpios, usuario, ahora);
            return deudas.buscarPorId(c, id).orElseThrow();
        });
    }

    public Deuda actualizarDeuda(long id, DatosDeuda datos) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        DatosDeuda limpios = ValidadorPatrimonio.deuda(datos, LocalDate.now(reloj));
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            Deuda actual = exigirDeuda(c, id, empresa.id());
            if (actual.anulada()) {
                throw new ValidacionException("Una deuda anulada no se puede modificar.");
            }
            if (limpios.monto().compareTo(actual.pagado()) < 0) {
                throw new ValidacionException("El monto no puede ser menor que lo ya pagado ("
                        + Montos.numero(actual.pagado()) + ").");
            }
            LocalDate primerPago = deudas.primerPago(c, id).orElse(null);
            if (primerPago != null && limpios.fecha().isAfter(primerPago)) {
                throw new ValidacionException("La fecha de inicio no puede ser posterior al primer pago registrado.");
            }
            cuentas.opcional(c, empresa.id(), limpios.cuentaId(), actual.cuentaId());
            deudas.actualizar(c, id, limpios, usuario, ahora);
            return deudas.buscarPorId(c, id).orElseThrow();
        });
    }

    /**
     * Quita la deuda del balance (por ejemplo, si se registró por error). Sus pagos se conservan como
     * historial, pero dejan de mover las cuentas, y los intereses que generaron se anulan.
     */
    public Deuda anularDeuda(long id, String motivo) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        String motivoLimpio = Validaciones.requerido(motivo, "Motivo de la anulación", 5, 200);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            Deuda actual = exigirDeuda(c, id, empresa.id());
            if (actual.anulada()) {
                throw new ValidacionException("La deuda ya está anulada.");
            }
            for (long movimientoId : deudas.movimientosDeLaDeuda(c, id)) {
                anularInteres(c, movimientoId, "Deuda anulada: " + motivoLimpio, usuario, ahora);
            }
            deudas.anular(c, id, motivoLimpio, usuario, ahora);
            return deudas.buscarPorId(c, id).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ pagos

    public List<PagoDeuda> listarPagos(long deudaId) {
        sesion.requerir(Permiso.FINANZAS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        return database.conConexion(c -> {
            exigirDeuda(c, deudaId, empresa.id());
            return deudas.listarPagos(c, deudaId);
        });
    }

    /** Registra un pago de capital (sin intereses) desde la cuenta principal. */
    public Deuda registrarPago(long deudaId, LocalDate fecha, BigDecimal monto, String nota) {
        return registrarPago(deudaId, fecha, monto, BigDecimal.ZERO, null, nota);
    }

    /**
     * Registra el pago de una cuota y devuelve la deuda con su nuevo saldo.
     *
     * @param capital  lo que baja el saldo de la deuda
     * @param interes  intereses de la cuota (opcional): se registran también como gasto financiero, para
     *                 que entren en el estado de resultados
     * @param cuentaId cuenta de la que sale el dinero; {@code null} = la cuenta principal
     */
    public Deuda registrarPago(long deudaId, LocalDate fecha, BigDecimal capital, BigDecimal interes, Long cuentaId,
                               String nota) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        LocalDate fechaPago = ValidadorPatrimonio.fecha(fecha, "Fecha del pago", LocalDate.now(reloj));
        BigDecimal intereses = ValidadorPatrimonio.interes(interes);
        String notaLimpia = Validaciones.opcional(nota, "Nota", 100);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            Deuda deuda = exigirDeuda(c, deudaId, empresa.id());
            if (deuda.anulada()) {
                throw new ValidacionException("No se pueden registrar pagos en una deuda anulada.");
            }
            if (deuda.cancelada()) {
                throw new ValidacionException("La deuda ya está pagada por completo.");
            }
            if (fechaPago.isBefore(deuda.fecha())) {
                throw new ValidacionException("El pago no puede ser anterior al inicio de la deuda.");
            }
            BigDecimal montoPago = ValidadorPatrimonio.montoPago(capital, deuda.saldo());
            long cuenta = cuentas.resolver(c, empresa.id(), cuentaId, null);
            Long movimientoId = null;
            if (intereses.signum() > 0) {
                movimientoId = movimientos.insertar(c, empresa.id(), new DatosMovimiento(TipoMovimiento.GASTO,
                        CategoriaFinanciera.INTERESES_PRESTAMOS,
                        CategoriaFinanciera.INTERESES_PRESTAMOS.comportamientoSugerido(), fechaPago,
                        recortar("Intereses: " + deuda.acreedor(), 150), intereses, null, cuenta), usuario, ahora);
            }
            deudas.insertarPago(c, deudaId, fechaPago, montoPago, intereses, cuenta, movimientoId, notaLimpia,
                    usuario, ahora);
            return deudas.buscarPorId(c, deudaId).orElseThrow();
        });
    }

    /** Interés sugerido para la próxima cuota: saldo × tasa anual / 12; cero si no se indicó la tasa. */
    public static BigDecimal interesSugerido(Deuda deuda) {
        if (deuda.tasaAnual() == null || !deuda.pendiente()) {
            return BigDecimal.ZERO;
        }
        return deuda.saldo().multiply(deuda.tasaAnual())
                .divide(BigDecimal.valueOf(1200), 2, java.math.RoundingMode.HALF_UP);
    }

    /** Anula un pago registrado por error: el saldo de la deuda vuelve a subir. */
    public Deuda anularPago(long pagoId) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            PagoDeuda pago = deudas.buscarPago(c, pagoId)
                    .orElseThrow(() -> new ValidacionException("El pago no existe."));
            Deuda deuda = exigirDeuda(c, pago.deudaId(), empresa.id());
            if (deuda.anulada()) {
                throw new ValidacionException("La deuda está anulada; sus pagos no se pueden modificar.");
            }
            if (pago.anulado()) {
                throw new ValidacionException("El pago ya está anulado.");
            }
            Long movimientoId = deudas.movimientoDelPago(c, pagoId).orElse(null);
            if (movimientoId != null) {
                anularInteres(c, movimientoId, "Pago de la deuda anulado", usuario, ahora);
            }
            deudas.anularPago(c, pagoId, usuario, ahora);
            return deudas.buscarPorId(c, deuda.id()).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ utilidades

    private void anularInteres(Connection c, long movimientoId, String motivo, String usuario, LocalDateTime ahora)
            throws SQLException {
        if (movimientos.buscarPorId(c, movimientoId).filter(m -> !m.anulado()).isPresent()) {
            movimientos.anular(c, movimientoId, recortar(motivo, 200), usuario, ahora);
        }
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }

    private Activo exigirActivo(Connection c, long id, long empresaId) throws SQLException {
        return activos.buscarPorId(c, id)
                .filter(a -> a.empresaId() == empresaId)
                .orElseThrow(() -> new ValidacionException("El activo no existe en la empresa activa."));
    }

    private Deuda exigirDeuda(Connection c, long id, long empresaId) throws SQLException {
        return deudas.buscarPorId(c, id)
                .filter(d -> d.empresaId() == empresaId)
                .orElseThrow(() -> new ValidacionException("La deuda no existe en la empresa activa."));
    }
}
