package com.businessanalyzer.service;

import com.businessanalyzer.analytics.ResumenFinancieroFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.ComportamientoFGEM;
import com.businessanalyzer.model.DatosMovimientoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MovimientoFinancieroFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.TipoMovimientoFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.TesoreriaRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * Ingresos y gastos no comerciales de la empresa activa (alquiler, sueldos, servicios,
 * intereses...). Junto con las ventas y compras forman la base del estado de resultados
 * y del punto de equilibrio.
 * <p>
 * Los movimientos se pueden corregir mientras estén registrados; para quitarlos del análisis
 * se anulan indicando el motivo (no se eliminan).
 */
public class FinanzasServiceFGEM {

    private final DatabaseFGEM databaseFGEM;
    private final MovimientoRepositoryFGEM movimientosFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;
    private final CuentasEmpresaFGEM cuentasFGEM = new CuentasEmpresaFGEM(new TesoreriaRepositoryFGEM());

    public FinanzasServiceFGEM(DatabaseFGEM databaseFGEM, MovimientoRepositoryFGEM movimientosFGEM, EmpresaServiceFGEM empresasFGEM,
                           SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.movimientosFGEM = movimientosFGEM;
        this.empresasFGEM = empresasFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    public List<MovimientoFinancieroFGEM> listarFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM, boolean incluirAnuladosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        validarPeriodoFGEM(desdeFGEM, hastaFGEM);
        return databaseFGEM.conConexionFGEM(cFGEM -> movimientosFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), desdeFGEM, hastaFGEM, incluirAnuladosFGEM));
    }

    public ResumenFinancieroFGEM resumenFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM) {
        return ResumenFinancieroFGEM.deFGEM(listarFGEM(desdeFGEM, hastaFGEM, false));
    }

    public MovimientoFinancieroFGEM registrarFGEM(DatosMovimientoFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        DatosMovimientoFGEM limpiosFGEM = ValidadorMovimientoFGEM.normalizarFGEM(datosFGEM, LocalDate.now(relojFGEM));
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            long idFGEM = movimientosFGEM.insertarFGEM(cFGEM, empresaFGEM.idFGEM(), conCuentaFGEM(limpiosFGEM,
                    cuentasFGEM.resolverFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM.cuentaIdFGEM(), null)), usuarioFGEM, ahoraFGEM);
            return movimientosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    private static DatosMovimientoFGEM conCuentaFGEM(DatosMovimientoFGEM dFGEM, Long cuentaIdFGEM) {
        return new DatosMovimientoFGEM(dFGEM.tipoFGEM(), dFGEM.categoriaFGEM(), dFGEM.comportamientoFGEM(), dFGEM.fechaFGEM(), dFGEM.descripcionFGEM(), dFGEM.montoFGEM(),
                dFGEM.comprobanteFGEM(), cuentaIdFGEM);
    }

    /** Los intereses que generó el pago de una deuda se corrigen desde ese pago, en «Activos y deudas». */
    private static void exigirNoGeneradoPorPagoFGEM(MovimientoFinancieroFGEM mFGEM) {
        if (mFGEM.dePagoDeudaFGEM()) {
            throw new ValidacionExceptionFGEM("Este gasto lo generó el pago de una deuda. Corrígelo o anúlalo desde "
                    + "el pago, en «Activos y deudas».");
        }
    }

    public MovimientoFinancieroFGEM actualizarFGEM(long idFGEM, DatosMovimientoFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        DatosMovimientoFGEM limpiosFGEM = ValidadorMovimientoFGEM.normalizarFGEM(datosFGEM, LocalDate.now(relojFGEM));
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            MovimientoFinancieroFGEM actualFGEM = exigirMovimientoFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            if (actualFGEM.anuladoFGEM()) {
                throw new ValidacionExceptionFGEM("Un movimiento anulado no se puede modificar.");
            }
            exigirNoGeneradoPorPagoFGEM(actualFGEM);
            // Un registro anterior a la tesorería puede seguir sin cuenta si no se elige una.
            Long cuentaIdFGEM = limpiosFGEM.cuentaIdFGEM() == null && actualFGEM.cuentaIdFGEM() == null ? null
                    : cuentasFGEM.resolverFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM.cuentaIdFGEM(), actualFGEM.cuentaIdFGEM());
            movimientosFGEM.actualizarFGEM(cFGEM, idFGEM, conCuentaFGEM(limpiosFGEM, cuentaIdFGEM), usuarioFGEM, ahoraFGEM);
            return movimientosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public MovimientoFinancieroFGEM anularFGEM(long idFGEM, String motivoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        String motivoLimpioFGEM = ValidacionesFGEM.requeridoFGEM(motivoFGEM, "Motivo de la anulación", 5, 200);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            MovimientoFinancieroFGEM actualFGEM = exigirMovimientoFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            if (actualFGEM.anuladoFGEM()) {
                throw new ValidacionExceptionFGEM("El movimiento ya está anulado.");
            }
            exigirNoGeneradoPorPagoFGEM(actualFGEM);
            movimientosFGEM.anularFGEM(cFGEM, idFGEM, motivoLimpioFGEM, usuarioFGEM, ahoraFGEM);
            return movimientosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    /**
     * Copia al mes de destino los gastos fijos registrados en el mes de origen, conservando el
     * día (ajustado si el mes de destino es más corto). Omite los que ya existen en el destino
     * con la misma categoría, descripción y monto, para no duplicarlos si se repite la acción.
     *
     * @return cantidad de gastos copiados
     */
    public int copiarGastosFijosFGEM(YearMonth origenFGEM, YearMonth destinoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        Objects.requireNonNull(origenFGEM, "origen");
        Objects.requireNonNull(destinoFGEM, "destino");
        if (!destinoFGEM.isAfter(origenFGEM)) {
            throw new ValidacionExceptionFGEM("El mes de destino debe ser posterior al de origen.");
        }
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        LocalDate hoyFGEM = LocalDate.now(relojFGEM);
        if (destinoFGEM.isAfter(YearMonth.from(hoyFGEM))) {
            throw new ValidacionExceptionFGEM("Solo se pueden copiar gastos hasta el mes en curso.");
        }
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);

        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            List<MovimientoFinancieroFGEM> fijosFGEM = movimientosFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), origenFGEM.atDay(1),
                            origenFGEM.atEndOfMonth(), false).stream()
                    .filter(mFGEM -> mFGEM.tipoFGEM() == TipoMovimientoFGEM.GASTO && mFGEM.comportamientoFGEM() == ComportamientoFGEM.FIJO
                            && !mFGEM.dePagoDeudaFGEM())
                    .toList();
            List<MovimientoFinancieroFGEM> existentesFGEM = movimientosFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), destinoFGEM.atDay(1),
                    destinoFGEM.atEndOfMonth(), false);
            int copiadosFGEM = 0;
            for (MovimientoFinancieroFGEM mFGEM : fijosFGEM) {
                boolean yaExisteFGEM = existentesFGEM.stream().anyMatch(eFGEM -> eFGEM.categoriaFGEM() == mFGEM.categoriaFGEM()
                        && eFGEM.descripcionFGEM().equalsIgnoreCase(mFGEM.descripcionFGEM())
                        && eFGEM.montoFGEM().compareTo(mFGEM.montoFGEM()) == 0);
                if (yaExisteFGEM) {
                    continue;
                }
                LocalDate fechaFGEM = destinoFGEM.atDay(Math.min(mFGEM.fechaFGEM().getDayOfMonth(), destinoFGEM.lengthOfMonth()));
                Long cuentaFGEM = mFGEM.cuentaIdFGEM() != null && cuentaActivaFGEM(cFGEM, empresaFGEM.idFGEM(), mFGEM.cuentaIdFGEM()) ? mFGEM.cuentaIdFGEM() : null;
                movimientosFGEM.insertarFGEM(cFGEM, empresaFGEM.idFGEM(), new DatosMovimientoFGEM(mFGEM.tipoFGEM(), mFGEM.categoriaFGEM(),
                        mFGEM.comportamientoFGEM(), fechaFGEM, mFGEM.descripcionFGEM(), mFGEM.montoFGEM(), null,
                        cuentasFGEM.resolverFGEM(cFGEM, empresaFGEM.idFGEM(), cuentaFGEM, null)), usuarioFGEM, ahoraFGEM);
                copiadosFGEM++;
            }
            return copiadosFGEM;
        });
    }

    private boolean cuentaActivaFGEM(Connection cFGEM, long empresaIdFGEM, long cuentaIdFGEM) throws SQLException {
        try {
            cuentasFGEM.exigirFGEM(cFGEM, empresaIdFGEM, cuentaIdFGEM, null);
            return true;
        } catch (ValidacionExceptionFGEM eFGEM) {
            return false;
        }
    }

    private MovimientoFinancieroFGEM exigirMovimientoFGEM(Connection cFGEM, long idFGEM, long empresaIdFGEM) throws SQLException {
        return movimientosFGEM.buscarPorIdFGEM(cFGEM, idFGEM)
                .filter(mFGEM -> mFGEM.empresaIdFGEM() == empresaIdFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM("El movimiento no existe en la empresa activa."));
    }

    private static void validarPeriodoFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM) {
        if (desdeFGEM == null || hastaFGEM == null) {
            throw new ValidacionExceptionFGEM("Indica las fechas del período.");
        }
        if (desdeFGEM.isAfter(hastaFGEM)) {
            throw new ValidacionExceptionFGEM("La fecha inicial no puede ser posterior a la final.");
        }
    }
}
