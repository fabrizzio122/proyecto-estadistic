package com.businessanalyzer.service;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.tesoreria.FlujoDineroFGEM;
import com.businessanalyzer.analytics.tesoreria.FlujoEfectivoFGEM;
import com.businessanalyzer.analytics.tesoreria.FlujoMensualFGEM;
import com.businessanalyzer.analytics.tesoreria.ProyeccionCajaFGEM;
import com.businessanalyzer.analytics.tesoreria.SaldoCuentaFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CuentaDineroFGEM;
import com.businessanalyzer.model.DatosCuentaFGEM;
import com.businessanalyzer.model.DatosMovimientoTesoreriaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MovimientoTesoreriaFGEM;
import com.businessanalyzer.model.OperacionFGEM;
import com.businessanalyzer.model.PagoOperacionFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.repository.DeudaRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.businessanalyzer.repository.TesoreriaRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
import com.businessanalyzer.util.MontosFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;

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
public class TesoreriaServiceFGEM {

    /** Meses completos que se usan para pronosticar el flujo de operación. */
    static final int MESES_HISTORIA_FGEM = 24;

    /** Resumen para el dashboard. */
    public record ResumenFGEM(BigDecimal disponibleFGEM, BigDecimal porCobrarFGEM, BigDecimal porCobrarVencidoFGEM,
                          BigDecimal porPagarFGEM, BigDecimal porPagarVencidoFGEM, int cuentasNegativasFGEM) {
    }

    private final DatabaseFGEM databaseFGEM;
    private final TesoreriaRepositoryFGEM tesoreriaFGEM;
    private final OperacionRepositoryFGEM operacionesFGEM;
    private final DeudaRepositoryFGEM deudasFGEM;
    private final CuentasEmpresaFGEM cuentasFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public TesoreriaServiceFGEM(DatabaseFGEM databaseFGEM, TesoreriaRepositoryFGEM tesoreriaFGEM, OperacionRepositoryFGEM operacionesFGEM,
                            DeudaRepositoryFGEM deudasFGEM, EmpresaServiceFGEM empresasFGEM, SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.tesoreriaFGEM = tesoreriaFGEM;
        this.operacionesFGEM = operacionesFGEM;
        this.deudasFGEM = deudasFGEM;
        this.cuentasFGEM = new CuentasEmpresaFGEM(tesoreriaFGEM);
        this.empresasFGEM = empresasFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    // ------------------------------------------------------------------ cuentas

    /** Cuentas con su saldo a hoy. */
    public List<SaldoCuentaFGEM> listarCuentasFGEM(boolean incluirInactivasFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        LocalDate hoyFGEM = hoyFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> SaldoCuentaFGEM.deFGEM(tesoreriaFGEM.listarCuentasFGEM(cFGEM, empresaFGEM.idFGEM(), incluirInactivasFGEM),
                tesoreriaFGEM.flujosFGEM(cFGEM, empresaFGEM.idFGEM()), hoyFGEM));
    }

    /** Cuentas activas, para elegir en los formularios (sin calcular saldos). */
    public List<CuentaDineroFGEM> cuentasActivasFGEM() {
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> tesoreriaFGEM.listarCuentasFGEM(cFGEM, empresaFGEM.idFGEM(), false));
    }

    public CuentaDineroFGEM registrarCuentaFGEM(DatosCuentaFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        DatosCuentaFGEM limpiosFGEM = ValidadorTesoreriaFGEM.cuentaFGEM(datosFGEM);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = ahoraFGEM();
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            exigirNombreLibreFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM.nombreFGEM(), 0);
            long idFGEM = tesoreriaFGEM.insertarCuentaFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM, usuarioFGEM, ahoraFGEM);
            return tesoreriaFGEM.buscarCuentaFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public CuentaDineroFGEM actualizarCuentaFGEM(long idFGEM, DatosCuentaFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        DatosCuentaFGEM limpiosFGEM = ValidadorTesoreriaFGEM.cuentaFGEM(datosFGEM);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = ahoraFGEM();
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            cuentasFGEM.exigirFGEM(cFGEM, empresaFGEM.idFGEM(), idFGEM, idFGEM);
            exigirNombreLibreFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM.nombreFGEM(), idFGEM);
            tesoreriaFGEM.actualizarCuentaFGEM(cFGEM, idFGEM, limpiosFGEM, usuarioFGEM, ahoraFGEM);
            return tesoreriaFGEM.buscarCuentaFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    /**
     * Activa o desactiva una cuenta. Para desactivarla debe tener saldo cero (primero se transfiere su
     * dinero) y no puede ser la única cuenta activa.
     */
    public CuentaDineroFGEM cambiarCuentaActivaFGEM(long idFGEM, boolean activaFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = ahoraFGEM();
        LocalDate hoyFGEM = hoyFGEM();
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            CuentaDineroFGEM cuentaFGEM = cuentasFGEM.exigirFGEM(cFGEM, empresaFGEM.idFGEM(), idFGEM, idFGEM);
            if (!activaFGEM && cuentaFGEM.activaFGEM()) {
                List<CuentaDineroFGEM> activasFGEM = tesoreriaFGEM.listarCuentasFGEM(cFGEM, empresaFGEM.idFGEM(), false);
                if (activasFGEM.size() <= 1) {
                    throw new ValidacionExceptionFGEM("La empresa necesita al menos una cuenta activa.");
                }
                BigDecimal saldoFGEM = SaldoCuentaFGEM.deFGEM(List.of(cuentaFGEM), tesoreriaFGEM.flujosFGEM(cFGEM, empresaFGEM.idFGEM()), hoyFGEM)
                        .get(0).saldoFGEM();
                if (saldoFGEM.signum() != 0) {
                    throw new ValidacionExceptionFGEM("La cuenta tiene un saldo de " + MontosFGEM.numeroFGEM(saldoFGEM)
                            + ". Transfiérelo a otra cuenta antes de desactivarla.");
                }
            }
            tesoreriaFGEM.cambiarActivaFGEM(cFGEM, idFGEM, activaFGEM, usuarioFGEM, ahoraFGEM);
            return tesoreriaFGEM.buscarCuentaFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ transferencias, aportes y retiros

    public List<MovimientoTesoreriaFGEM> listarMovimientosFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM, boolean incluirAnuladosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        PeriodoFGEM periodoFGEM = new PeriodoFGEM(desdeFGEM, hastaFGEM);
        return databaseFGEM.conConexionFGEM(cFGEM -> tesoreriaFGEM.listarMovimientosFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM.desdeFGEM(),
                periodoFGEM.hastaFGEM(), incluirAnuladosFGEM));
    }

    public MovimientoTesoreriaFGEM registrarMovimientoFGEM(DatosMovimientoTesoreriaFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        DatosMovimientoTesoreriaFGEM dFGEM = ValidadorTesoreriaFGEM.movimientoFGEM(datosFGEM, hoyFGEM());
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = ahoraFGEM();
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            CuentaDineroFGEM origenFGEM = dFGEM.cuentaOrigenIdFGEM() == null ? null
                    : cuentasFGEM.exigirFGEM(cFGEM, empresaFGEM.idFGEM(), dFGEM.cuentaOrigenIdFGEM(), null);
            CuentaDineroFGEM destinoFGEM = dFGEM.cuentaDestinoIdFGEM() == null ? null
                    : cuentasFGEM.exigirFGEM(cFGEM, empresaFGEM.idFGEM(), dFGEM.cuentaDestinoIdFGEM(), null);
            String descripcionFGEM = dFGEM.descripcionFGEM() != null ? dFGEM.descripcionFGEM() : switch (dFGEM.tipoFGEM()) {
                case TRANSFERENCIA -> "De " + origenFGEM.nombreFGEM() + " a " + destinoFGEM.nombreFGEM();
                case APORTE -> "Aporte del dueño a " + destinoFGEM.nombreFGEM();
                case RETIRO -> "Retiro del dueño de " + origenFGEM.nombreFGEM();
            };
            long idFGEM = tesoreriaFGEM.insertarMovimientoFGEM(cFGEM, empresaFGEM.idFGEM(), new DatosMovimientoTesoreriaFGEM(dFGEM.tipoFGEM(), dFGEM.fechaFGEM(),
                    dFGEM.cuentaOrigenIdFGEM(), dFGEM.cuentaDestinoIdFGEM(), dFGEM.montoFGEM(), descripcionFGEM), usuarioFGEM, ahoraFGEM);
            return tesoreriaFGEM.buscarMovimientoFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public MovimientoTesoreriaFGEM anularMovimientoFGEM(long idFGEM, String motivoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        String motivoLimpioFGEM = ValidacionesFGEM.requeridoFGEM(motivoFGEM, "Motivo de la anulación", 5, 200);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = ahoraFGEM();
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            MovimientoTesoreriaFGEM mFGEM = tesoreriaFGEM.buscarMovimientoFGEM(cFGEM, idFGEM)
                    .filter(xFGEM -> xFGEM.empresaIdFGEM() == empresaFGEM.idFGEM())
                    .orElseThrow(() -> new ValidacionExceptionFGEM("El movimiento no existe en la empresa activa."));
            if (mFGEM.anuladoFGEM()) {
                throw new ValidacionExceptionFGEM("El movimiento ya está anulado.");
            }
            tesoreriaFGEM.anularMovimientoFGEM(cFGEM, idFGEM, motivoLimpioFGEM, usuarioFGEM, ahoraFGEM);
            return tesoreriaFGEM.buscarMovimientoFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ cuentas por cobrar y por pagar

    /** Ventas (por cobrar) o compras (por pagar) al crédito con saldo pendiente. */
    public List<OperacionFGEM> pendientesFGEM(TipoOperacionFGEM tipoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> operacionesFGEM.listarPendientesFGEM(cFGEM, empresaFGEM.idFGEM(), tipoFGEM));
    }

    public List<PagoOperacionFGEM> listarPagosFGEM(long operacionIdFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            exigirOperacionFGEM(cFGEM, operacionIdFGEM, empresaFGEM.idFGEM());
            return tesoreriaFGEM.listarPagosFGEM(cFGEM, operacionIdFGEM);
        });
    }

    /** Registra el cobro de una venta al crédito o el pago de una compra al crédito. */
    public OperacionFGEM registrarPagoFGEM(long operacionIdFGEM, long cuentaIdFGEM, LocalDate fechaFGEM, BigDecimal montoFGEM, String notaFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.OPERACIONES_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        LocalDate diaFGEM = ValidadorTesoreriaFGEM.fechaFGEM(fechaFGEM, hoyFGEM());
        BigDecimal importeFGEM = ValidadorTesoreriaFGEM.montoPositivoFGEM(montoFGEM, "Monto");
        String notaLimpiaFGEM = ValidacionesFGEM.opcionalFGEM(notaFGEM, "Nota", 100);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = ahoraFGEM();
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            OperacionFGEM operacionFGEM = exigirOperacionFGEM(cFGEM, operacionIdFGEM, empresaFGEM.idFGEM());
            String queFGEM = operacionFGEM.tipoFGEM() == TipoOperacionFGEM.VENTA ? "cobro" : "pago";
            if (operacionFGEM.anuladaFGEM()) {
                throw new ValidacionExceptionFGEM("La operación " + operacionFGEM.numeroFGEM() + " está anulada.");
            }
            if (!operacionFGEM.alCreditoFGEM()) {
                throw new ValidacionExceptionFGEM("La operación " + operacionFGEM.numeroFGEM()
                        + " fue al contado: su dinero ya se registró.");
            }
            if (operacionFGEM.pendienteFGEM().signum() == 0) {
                throw new ValidacionExceptionFGEM("La operación " + operacionFGEM.numeroFGEM() + " ya está pagada por completo.");
            }
            if (importeFGEM.compareTo(operacionFGEM.pendienteFGEM()) > 0) {
                throw new ValidacionExceptionFGEM("El " + queFGEM + " no puede superar lo pendiente ("
                        + MontosFGEM.numeroFGEM(operacionFGEM.pendienteFGEM()) + ").");
            }
            if (diaFGEM.isBefore(operacionFGEM.fechaFGEM())) {
                throw new ValidacionExceptionFGEM("El " + queFGEM + " no puede ser anterior a la operación.");
            }
            cuentasFGEM.exigirFGEM(cFGEM, empresaFGEM.idFGEM(), cuentaIdFGEM, null);
            tesoreriaFGEM.insertarPagoFGEM(cFGEM, operacionIdFGEM, cuentaIdFGEM, diaFGEM, importeFGEM, notaLimpiaFGEM, usuarioFGEM, ahoraFGEM);
            return operacionesFGEM.buscarPorIdFGEM(cFGEM, operacionIdFGEM).orElseThrow();
        });
    }

    public OperacionFGEM anularPagoFGEM(long pagoIdFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.OPERACIONES_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = ahoraFGEM();
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            PagoOperacionFGEM pagoFGEM = tesoreriaFGEM.buscarPagoFGEM(cFGEM, pagoIdFGEM)
                    .orElseThrow(() -> new ValidacionExceptionFGEM("El registro no existe."));
            exigirOperacionFGEM(cFGEM, pagoFGEM.operacionIdFGEM(), empresaFGEM.idFGEM());
            if (pagoFGEM.anuladoFGEM()) {
                throw new ValidacionExceptionFGEM("El registro ya está anulado.");
            }
            tesoreriaFGEM.anularPagoFGEM(cFGEM, pagoIdFGEM, usuarioFGEM, ahoraFGEM);
            return operacionesFGEM.buscarPorIdFGEM(cFGEM, pagoFGEM.operacionIdFGEM()).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ análisis

    public FlujoEfectivoFGEM flujoEfectivoFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        PeriodoFGEM periodoFGEM = new PeriodoFGEM(desdeFGEM, hastaFGEM);
        return databaseFGEM.conConexionFGEM(cFGEM -> FlujoEfectivoFGEM.deFGEM(periodoFGEM, tesoreriaFGEM.listarCuentasFGEM(cFGEM, empresaFGEM.idFGEM(), true),
                tesoreriaFGEM.flujosFGEM(cFGEM, empresaFGEM.idFGEM())));
    }

    /** Entradas, salidas y saldo de los últimos {@code meses} meses, incluido el mes en curso. */
    public List<FlujoMensualFGEM> evolucionFGEM(int mesesFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_VER);
        if (mesesFGEM < 1 || mesesFGEM > 60) {
            throw new ValidacionExceptionFGEM("El rango debe estar entre 1 y 60 meses.");
        }
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        YearMonth actualFGEM = YearMonth.from(hoyFGEM());
        return databaseFGEM.conConexionFGEM(cFGEM -> FlujoMensualFGEM.serieFGEM(actualFGEM.minusMonths(mesesFGEM - 1L), actualFGEM,
                tesoreriaFGEM.listarCuentasFGEM(cFGEM, empresaFGEM.idFGEM(), true), tesoreriaFGEM.flujosFGEM(cFGEM, empresaFGEM.idFGEM())));
    }

    public ProyeccionCajaFGEM proyeccionFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        LocalDate hoyFGEM = hoyFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> proyeccionFGEM(tesoreriaFGEM, deudasFGEM, cFGEM, empresaFGEM.idFGEM(), hoyFGEM));
    }

    /** También la usa el diagnóstico empresarial. */
    static ProyeccionCajaFGEM proyeccionFGEM(TesoreriaRepositoryFGEM tesoreriaFGEM, DeudaRepositoryFGEM deudasFGEM, Connection cFGEM,
                                     long empresaIdFGEM, LocalDate hoyFGEM) throws SQLException {
        List<CuentaDineroFGEM> todasFGEM = tesoreriaFGEM.listarCuentasFGEM(cFGEM, empresaIdFGEM, true);
        List<FlujoDineroFGEM> flujosFGEM = tesoreriaFGEM.flujosFGEM(cFGEM, empresaIdFGEM);
        BigDecimal disponibleFGEM = SaldoCuentaFGEM.totalFGEM(SaldoCuentaFGEM.deFGEM(todasFGEM, flujosFGEM, hoyFGEM));
        return ProyeccionCajaFGEM.calcularFGEM(hoyFGEM, disponibleFGEM, historiaOperacionFGEM(todasFGEM, flujosFGEM, hoyFGEM),
                deudasFGEM.listarFGEM(cFGEM, empresaIdFGEM, false));
    }

    /**
     * Flujo de operación de cada mes completo, desde el primer mes con movimientos en las cuentas (como
     * máximo {@value #MESES_HISTORIA} meses).
     */
    static double[] historiaOperacionFGEM(List<CuentaDineroFGEM> cuentasFGEM, List<FlujoDineroFGEM> flujosFGEM, LocalDate hoyFGEM) {
        YearMonth ultimoFGEM = YearMonth.from(hoyFGEM).minusMonths(1);
        YearMonth primeroFGEM = flujosFGEM.stream().map(fFGEM -> YearMonth.from(fFGEM.fechaFGEM())).min(YearMonth::compareTo).orElse(null);
        if (primeroFGEM == null || primeroFGEM.isAfter(ultimoFGEM)) {
            return new double[0];
        }
        YearMonth limiteFGEM = ultimoFGEM.minusMonths(MESES_HISTORIA_FGEM - 1L);
        if (primeroFGEM.isBefore(limiteFGEM)) {
            primeroFGEM = limiteFGEM;
        }
        return FlujoMensualFGEM.serieFGEM(primeroFGEM, ultimoFGEM, cuentasFGEM, flujosFGEM).stream()
                .mapToDouble(mFGEM -> mFGEM.operacionFGEM().doubleValue()).toArray();
    }

    public ResumenFGEM resumenFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        LocalDate hoyFGEM = hoyFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            List<SaldoCuentaFGEM> saldosFGEM = SaldoCuentaFGEM.deFGEM(tesoreriaFGEM.listarCuentasFGEM(cFGEM, empresaFGEM.idFGEM(), true),
                    tesoreriaFGEM.flujosFGEM(cFGEM, empresaFGEM.idFGEM()), hoyFGEM);
            List<OperacionFGEM> cobrarFGEM = operacionesFGEM.listarPendientesFGEM(cFGEM, empresaFGEM.idFGEM(), TipoOperacionFGEM.VENTA);
            List<OperacionFGEM> pagarFGEM = operacionesFGEM.listarPendientesFGEM(cFGEM, empresaFGEM.idFGEM(), TipoOperacionFGEM.COMPRA);
            return new ResumenFGEM(SaldoCuentaFGEM.totalFGEM(saldosFGEM), pendienteFGEM(cobrarFGEM, null), pendienteFGEM(cobrarFGEM, hoyFGEM),
                    pendienteFGEM(pagarFGEM, null), pendienteFGEM(pagarFGEM, hoyFGEM),
                    (int) saldosFGEM.stream().filter(SaldoCuentaFGEM::negativoFGEM).count());
        });
    }

    // ------------------------------------------------------------------ utilidades

    /** Suma de lo pendiente; con {@code hoy}, solo lo vencido. */
    static BigDecimal pendienteFGEM(List<OperacionFGEM> operacionesFGEM, LocalDate hoyFGEM) {
        return operacionesFGEM.stream()
                .filter(oFGEM -> hoyFGEM == null || oFGEM.vencidaEnFGEM(hoyFGEM))
                .map(OperacionFGEM::pendienteFGEM)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OperacionFGEM exigirOperacionFGEM(Connection cFGEM, long idFGEM, long empresaIdFGEM) throws SQLException {
        return operacionesFGEM.buscarPorIdFGEM(cFGEM, idFGEM)
                .filter(oFGEM -> oFGEM.empresaIdFGEM() == empresaIdFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM("La operación no existe en la empresa activa."));
    }

    private void exigirNombreLibreFGEM(Connection cFGEM, long empresaIdFGEM, String nombreFGEM, long excluirIdFGEM) throws SQLException {
        if (tesoreriaFGEM.existeNombreFGEM(cFGEM, empresaIdFGEM, nombreFGEM, excluirIdFGEM)) {
            throw new ValidacionExceptionFGEM("Ya existe una cuenta llamada «" + nombreFGEM + "».");
        }
    }

    private LocalDate hoyFGEM() {
        return LocalDate.now(relojFGEM);
    }

    private LocalDateTime ahoraFGEM() {
        return LocalDateTime.now(relojFGEM);
    }
}
