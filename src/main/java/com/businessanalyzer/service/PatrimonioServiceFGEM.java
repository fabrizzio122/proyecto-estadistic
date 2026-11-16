package com.businessanalyzer.service;

import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonialFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.ActivoFGEM;
import com.businessanalyzer.model.CategoriaActivoFGEM;
import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.DatosActivoFGEM;
import com.businessanalyzer.model.DatosDeudaFGEM;
import com.businessanalyzer.model.DatosMovimientoFGEM;
import com.businessanalyzer.model.TipoMovimientoFGEM;
import com.businessanalyzer.model.DeudaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.PagoDeudaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.repository.ActivoRepositoryFGEM;
import com.businessanalyzer.repository.DeudaRepositoryFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.businessanalyzer.repository.ProductoRepositoryFGEM;
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
import java.util.List;

/**
 * Activos, deudas y balance general de la empresa activa. Usa los permisos de Gestión
 * financiera: {@link PermisoFGEM#FINANZAS_VER} para consultar y {@link PermisoFGEM#FINANZAS_GESTIONAR}
 * para registrar.
 * <p>
 * Nada se elimina: los activos se dan de baja (venta, pérdida o error) y las deudas y pagos se
 * anulan, conservando quién lo hizo y cuándo.
 */
public class PatrimonioServiceFGEM {

    private final DatabaseFGEM databaseFGEM;
    private final ActivoRepositoryFGEM activosFGEM;
    private final DeudaRepositoryFGEM deudasFGEM;
    private final MovimientoRepositoryFGEM movimientosFGEM;
    private final ConsultaPatrimonioFGEM consultaFGEM;
    private final CuentasEmpresaFGEM cuentasFGEM = new CuentasEmpresaFGEM(new TesoreriaRepositoryFGEM());
    private final EmpresaServiceFGEM empresasFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public PatrimonioServiceFGEM(DatabaseFGEM databaseFGEM, ActivoRepositoryFGEM activosFGEM, DeudaRepositoryFGEM deudasFGEM,
                             ProductoRepositoryFGEM productosFGEM, OperacionRepositoryFGEM operacionesFGEM,
                             MovimientoRepositoryFGEM movimientosFGEM, EmpresaServiceFGEM empresasFGEM, SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.activosFGEM = activosFGEM;
        this.deudasFGEM = deudasFGEM;
        this.movimientosFGEM = movimientosFGEM;
        this.consultaFGEM = new ConsultaPatrimonioFGEM(activosFGEM, deudasFGEM, productosFGEM, operacionesFGEM, movimientosFGEM);
        this.empresasFGEM = empresasFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    /** Balance general a hoy con sus indicadores. */
    public SituacionPatrimonialFGEM situacionFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        LocalDate hoyFGEM = LocalDate.now(relojFGEM);
        return databaseFGEM.conConexionFGEM(cFGEM -> consultaFGEM.situacionFGEM(cFGEM, empresaFGEM.idFGEM(), hoyFGEM));
    }

    // ------------------------------------------------------------------ activos

    public List<ActivoFGEM> listarActivosFGEM(boolean incluirBajasFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> activosFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), incluirBajasFGEM));
    }

    public ActivoFGEM registrarActivoFGEM(DatosActivoFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        DatosActivoFGEM limpiosFGEM = ValidadorPatrimonioFGEM.activoFGEM(datosFGEM, LocalDate.now(relojFGEM));
        exigirNoEsEfectivoFGEM(limpiosFGEM, null);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            cuentasFGEM.opcionalFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM.cuentaIdFGEM(), null);
            long idFGEM = activosFGEM.insertarFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM, usuarioFGEM, ahoraFGEM);
            return activosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    /** El dinero se registra en «Tesorería» (cuentas con saldo calculado), ya no como un activo escrito a mano. */
    private static void exigirNoEsEfectivoFGEM(DatosActivoFGEM dFGEM, ActivoFGEM actualFGEM) {
        if (dFGEM.categoriaFGEM() == CategoriaActivoFGEM.CAJA_BANCOS
                && (actualFGEM == null || actualFGEM.categoriaFGEM() != CategoriaActivoFGEM.CAJA_BANCOS)) {
            throw new ValidacionExceptionFGEM("El efectivo y las cuentas bancarias se registran en «Tesorería»: "
                    + "crea allí la cuenta con su saldo inicial.");
        }
    }

    public ActivoFGEM actualizarActivoFGEM(long idFGEM, DatosActivoFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        DatosActivoFGEM limpiosFGEM = ValidadorPatrimonioFGEM.activoFGEM(datosFGEM, LocalDate.now(relojFGEM));
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            ActivoFGEM actualFGEM = exigirActivoFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            if (actualFGEM.dadoDeBajaFGEM()) {
                throw new ValidacionExceptionFGEM("Un activo dado de baja no se puede modificar.");
            }
            exigirNoEsEfectivoFGEM(limpiosFGEM, actualFGEM);
            cuentasFGEM.opcionalFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM.cuentaIdFGEM(), actualFGEM.cuentaIdFGEM());
            activosFGEM.actualizarFGEM(cFGEM, idFGEM, limpiosFGEM, usuarioFGEM, ahoraFGEM);
            return activosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    /** Retira el activo del balance desde la fecha indicada (venta, pérdida, desuso o registro erróneo). */
    public ActivoFGEM darDeBajaFGEM(long idFGEM, LocalDate fechaFGEM, String motivoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        LocalDate hoyFGEM = LocalDate.now(relojFGEM);
        LocalDate fechaBajaFGEM = ValidadorPatrimonioFGEM.fechaFGEM(fechaFGEM, "Fecha de baja", hoyFGEM);
        String motivoLimpioFGEM = ValidacionesFGEM.requeridoFGEM(motivoFGEM, "Motivo de la baja", 5, 200);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            ActivoFGEM actualFGEM = exigirActivoFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            if (actualFGEM.dadoDeBajaFGEM()) {
                throw new ValidacionExceptionFGEM("El activo ya fue dado de baja.");
            }
            if (fechaBajaFGEM.isBefore(actualFGEM.fechaFGEM())) {
                throw new ValidacionExceptionFGEM("La fecha de baja no puede ser anterior a la de registro del activo.");
            }
            activosFGEM.darDeBajaFGEM(cFGEM, idFGEM, fechaBajaFGEM, motivoLimpioFGEM, usuarioFGEM, ahoraFGEM);
            return activosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ deudas

    public List<DeudaFGEM> listarDeudasFGEM(boolean incluirAnuladasFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> deudasFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), incluirAnuladasFGEM));
    }

    public DeudaFGEM registrarDeudaFGEM(DatosDeudaFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        DatosDeudaFGEM limpiosFGEM = ValidadorPatrimonioFGEM.deudaFGEM(datosFGEM, LocalDate.now(relojFGEM));
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            cuentasFGEM.opcionalFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM.cuentaIdFGEM(), null);
            long idFGEM = deudasFGEM.insertarFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM, usuarioFGEM, ahoraFGEM);
            return deudasFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public DeudaFGEM actualizarDeudaFGEM(long idFGEM, DatosDeudaFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        DatosDeudaFGEM limpiosFGEM = ValidadorPatrimonioFGEM.deudaFGEM(datosFGEM, LocalDate.now(relojFGEM));
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            DeudaFGEM actualFGEM = exigirDeudaFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            if (actualFGEM.anuladaFGEM()) {
                throw new ValidacionExceptionFGEM("Una deuda anulada no se puede modificar.");
            }
            if (limpiosFGEM.montoFGEM().compareTo(actualFGEM.pagadoFGEM()) < 0) {
                throw new ValidacionExceptionFGEM("El monto no puede ser menor que lo ya pagado ("
                        + MontosFGEM.numeroFGEM(actualFGEM.pagadoFGEM()) + ").");
            }
            LocalDate primerPagoFGEM = deudasFGEM.primerPagoFGEM(cFGEM, idFGEM).orElse(null);
            if (primerPagoFGEM != null && limpiosFGEM.fechaFGEM().isAfter(primerPagoFGEM)) {
                throw new ValidacionExceptionFGEM("La fecha de inicio no puede ser posterior al primer pago registrado.");
            }
            cuentasFGEM.opcionalFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM.cuentaIdFGEM(), actualFGEM.cuentaIdFGEM());
            deudasFGEM.actualizarFGEM(cFGEM, idFGEM, limpiosFGEM, usuarioFGEM, ahoraFGEM);
            return deudasFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    /**
     * Quita la deuda del balance (por ejemplo, si se registró por error). Sus pagos se conservan como
     * historial, pero dejan de mover las cuentas, y los intereses que generaron se anulan.
     */
    public DeudaFGEM anularDeudaFGEM(long idFGEM, String motivoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        String motivoLimpioFGEM = ValidacionesFGEM.requeridoFGEM(motivoFGEM, "Motivo de la anulación", 5, 200);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            DeudaFGEM actualFGEM = exigirDeudaFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            if (actualFGEM.anuladaFGEM()) {
                throw new ValidacionExceptionFGEM("La deuda ya está anulada.");
            }
            for (long movimientoIdFGEM : deudasFGEM.movimientosDeLaDeudaFGEM(cFGEM, idFGEM)) {
                anularInteresFGEM(cFGEM, movimientoIdFGEM, "Deuda anulada: " + motivoLimpioFGEM, usuarioFGEM, ahoraFGEM);
            }
            deudasFGEM.anularFGEM(cFGEM, idFGEM, motivoLimpioFGEM, usuarioFGEM, ahoraFGEM);
            return deudasFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ pagos

    public List<PagoDeudaFGEM> listarPagosFGEM(long deudaIdFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            exigirDeudaFGEM(cFGEM, deudaIdFGEM, empresaFGEM.idFGEM());
            return deudasFGEM.listarPagosFGEM(cFGEM, deudaIdFGEM);
        });
    }

    /** Registra un pago de capital (sin intereses) desde la cuenta principal. */
    public DeudaFGEM registrarPagoFGEM(long deudaIdFGEM, LocalDate fechaFGEM, BigDecimal montoFGEM, String notaFGEM) {
        return registrarPagoFGEM(deudaIdFGEM, fechaFGEM, montoFGEM, BigDecimal.ZERO, null, notaFGEM);
    }

    /**
     * Registra el pago de una cuota y devuelve la deuda con su nuevo saldo.
     *
     * @param capitalFGEM  lo que baja el saldo de la deuda
     * @param interesFGEM  intereses de la cuota (opcional): se registran también como gasto financiero, para
     *                 que entren en el estado de resultados
     * @param cuentaIdFGEM cuenta de la que sale el dinero; {@code null} = la cuenta principal
     */
    public DeudaFGEM registrarPagoFGEM(long deudaIdFGEM, LocalDate fechaFGEM, BigDecimal capitalFGEM, BigDecimal interesFGEM, Long cuentaIdFGEM,
                               String notaFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        LocalDate fechaPagoFGEM = ValidadorPatrimonioFGEM.fechaFGEM(fechaFGEM, "Fecha del pago", LocalDate.now(relojFGEM));
        BigDecimal interesesFGEM = ValidadorPatrimonioFGEM.interesFGEM(interesFGEM);
        String notaLimpiaFGEM = ValidacionesFGEM.opcionalFGEM(notaFGEM, "Nota", 100);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            DeudaFGEM deudaFGEM = exigirDeudaFGEM(cFGEM, deudaIdFGEM, empresaFGEM.idFGEM());
            if (deudaFGEM.anuladaFGEM()) {
                throw new ValidacionExceptionFGEM("No se pueden registrar pagos en una deuda anulada.");
            }
            if (deudaFGEM.canceladaFGEM()) {
                throw new ValidacionExceptionFGEM("La deuda ya está pagada por completo.");
            }
            if (fechaPagoFGEM.isBefore(deudaFGEM.fechaFGEM())) {
                throw new ValidacionExceptionFGEM("El pago no puede ser anterior al inicio de la deuda.");
            }
            BigDecimal montoPagoFGEM = ValidadorPatrimonioFGEM.montoPagoFGEM(capitalFGEM, deudaFGEM.saldoFGEM());
            long cuentaFGEM = cuentasFGEM.resolverFGEM(cFGEM, empresaFGEM.idFGEM(), cuentaIdFGEM, null);
            Long movimientoIdFGEM = null;
            if (interesesFGEM.signum() > 0) {
                movimientoIdFGEM = movimientosFGEM.insertarFGEM(cFGEM, empresaFGEM.idFGEM(), new DatosMovimientoFGEM(TipoMovimientoFGEM.GASTO,
                        CategoriaFinancieraFGEM.INTERESES_PRESTAMOS,
                        CategoriaFinancieraFGEM.INTERESES_PRESTAMOS.comportamientoSugeridoFGEM(), fechaPagoFGEM,
                        recortarFGEM("Intereses: " + deudaFGEM.acreedorFGEM(), 150), interesesFGEM, null, cuentaFGEM), usuarioFGEM, ahoraFGEM);
            }
            deudasFGEM.insertarPagoFGEM(cFGEM, deudaIdFGEM, fechaPagoFGEM, montoPagoFGEM, interesesFGEM, cuentaFGEM, movimientoIdFGEM, notaLimpiaFGEM,
                    usuarioFGEM, ahoraFGEM);
            return deudasFGEM.buscarPorIdFGEM(cFGEM, deudaIdFGEM).orElseThrow();
        });
    }

    /** Interés sugerido para la próxima cuota: saldo × tasa anual / 12; cero si no se indicó la tasa. */
    public static BigDecimal interesSugeridoFGEM(DeudaFGEM deudaFGEM) {
        if (deudaFGEM.tasaAnualFGEM() == null || !deudaFGEM.pendienteFGEM()) {
            return BigDecimal.ZERO;
        }
        return deudaFGEM.saldoFGEM().multiply(deudaFGEM.tasaAnualFGEM())
                .divide(BigDecimal.valueOf(1200), 2, java.math.RoundingMode.HALF_UP);
    }

    /** Anula un pago registrado por error: el saldo de la deuda vuelve a subir. */
    public DeudaFGEM anularPagoFGEM(long pagoIdFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            PagoDeudaFGEM pagoFGEM = deudasFGEM.buscarPagoFGEM(cFGEM, pagoIdFGEM)
                    .orElseThrow(() -> new ValidacionExceptionFGEM("El pago no existe."));
            DeudaFGEM deudaFGEM = exigirDeudaFGEM(cFGEM, pagoFGEM.deudaIdFGEM(), empresaFGEM.idFGEM());
            if (deudaFGEM.anuladaFGEM()) {
                throw new ValidacionExceptionFGEM("La deuda está anulada; sus pagos no se pueden modificar.");
            }
            if (pagoFGEM.anuladoFGEM()) {
                throw new ValidacionExceptionFGEM("El pago ya está anulado.");
            }
            Long movimientoIdFGEM = deudasFGEM.movimientoDelPagoFGEM(cFGEM, pagoIdFGEM).orElse(null);
            if (movimientoIdFGEM != null) {
                anularInteresFGEM(cFGEM, movimientoIdFGEM, "Pago de la deuda anulado", usuarioFGEM, ahoraFGEM);
            }
            deudasFGEM.anularPagoFGEM(cFGEM, pagoIdFGEM, usuarioFGEM, ahoraFGEM);
            return deudasFGEM.buscarPorIdFGEM(cFGEM, deudaFGEM.idFGEM()).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ utilidades

    private void anularInteresFGEM(Connection cFGEM, long movimientoIdFGEM, String motivoFGEM, String usuarioFGEM, LocalDateTime ahoraFGEM)
            throws SQLException {
        if (movimientosFGEM.buscarPorIdFGEM(cFGEM, movimientoIdFGEM).filter(mFGEM -> !mFGEM.anuladoFGEM()).isPresent()) {
            movimientosFGEM.anularFGEM(cFGEM, movimientoIdFGEM, recortarFGEM(motivoFGEM, 200), usuarioFGEM, ahoraFGEM);
        }
    }

    private static String recortarFGEM(String textoFGEM, int maximoFGEM) {
        return textoFGEM.length() <= maximoFGEM ? textoFGEM : textoFGEM.substring(0, maximoFGEM);
    }

    private ActivoFGEM exigirActivoFGEM(Connection cFGEM, long idFGEM, long empresaIdFGEM) throws SQLException {
        return activosFGEM.buscarPorIdFGEM(cFGEM, idFGEM)
                .filter(aFGEM -> aFGEM.empresaIdFGEM() == empresaIdFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM("El activo no existe en la empresa activa."));
    }

    private DeudaFGEM exigirDeudaFGEM(Connection cFGEM, long idFGEM, long empresaIdFGEM) throws SQLException {
        return deudasFGEM.buscarPorIdFGEM(cFGEM, idFGEM)
                .filter(dFGEM -> dFGEM.empresaIdFGEM() == empresaIdFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM("La deuda no existe en la empresa activa."));
    }
}
