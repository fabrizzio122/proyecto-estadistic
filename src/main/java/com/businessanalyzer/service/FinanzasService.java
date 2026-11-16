package com.businessanalyzer.service;

import com.businessanalyzer.analytics.ResumenFinanciero;
import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Comportamiento;
import com.businessanalyzer.model.DatosMovimiento;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.MovimientoFinanciero;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.TipoMovimiento;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.repository.TesoreriaRepository;
import com.businessanalyzer.seguridad.Sesion;
import com.businessanalyzer.util.Validaciones;

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
public class FinanzasService {

    private final Database database;
    private final MovimientoRepository movimientos;
    private final EmpresaService empresas;
    private final Sesion sesion;
    private final Clock reloj;
    private final CuentasEmpresa cuentas = new CuentasEmpresa(new TesoreriaRepository());

    public FinanzasService(Database database, MovimientoRepository movimientos, EmpresaService empresas,
                           Sesion sesion, Clock reloj) {
        this.database = database;
        this.movimientos = movimientos;
        this.empresas = empresas;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    public List<MovimientoFinanciero> listar(LocalDate desde, LocalDate hasta, boolean incluirAnulados) {
        sesion.requerir(Permiso.FINANZAS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        validarPeriodo(desde, hasta);
        return database.conConexion(c -> movimientos.listar(c, empresa.id(), desde, hasta, incluirAnulados));
    }

    public ResumenFinanciero resumen(LocalDate desde, LocalDate hasta) {
        return ResumenFinanciero.de(listar(desde, hasta, false));
    }

    public MovimientoFinanciero registrar(DatosMovimiento datos) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        DatosMovimiento limpios = ValidadorMovimiento.normalizar(datos, LocalDate.now(reloj));
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            long id = movimientos.insertar(c, empresa.id(), conCuenta(limpios,
                    cuentas.resolver(c, empresa.id(), limpios.cuentaId(), null)), usuario, ahora);
            return movimientos.buscarPorId(c, id).orElseThrow();
        });
    }

    private static DatosMovimiento conCuenta(DatosMovimiento d, Long cuentaId) {
        return new DatosMovimiento(d.tipo(), d.categoria(), d.comportamiento(), d.fecha(), d.descripcion(), d.monto(),
                d.comprobante(), cuentaId);
    }

    /** Los intereses que generó el pago de una deuda se corrigen desde ese pago, en «Activos y deudas». */
    private static void exigirNoGeneradoPorPago(MovimientoFinanciero m) {
        if (m.dePagoDeuda()) {
            throw new ValidacionException("Este gasto lo generó el pago de una deuda. Corrígelo o anúlalo desde "
                    + "el pago, en «Activos y deudas».");
        }
    }

    public MovimientoFinanciero actualizar(long id, DatosMovimiento datos) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        DatosMovimiento limpios = ValidadorMovimiento.normalizar(datos, LocalDate.now(reloj));
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            MovimientoFinanciero actual = exigirMovimiento(c, id, empresa.id());
            if (actual.anulado()) {
                throw new ValidacionException("Un movimiento anulado no se puede modificar.");
            }
            exigirNoGeneradoPorPago(actual);
            // Un registro anterior a la tesorería puede seguir sin cuenta si no se elige una.
            Long cuentaId = limpios.cuentaId() == null && actual.cuentaId() == null ? null
                    : cuentas.resolver(c, empresa.id(), limpios.cuentaId(), actual.cuentaId());
            movimientos.actualizar(c, id, conCuenta(limpios, cuentaId), usuario, ahora);
            return movimientos.buscarPorId(c, id).orElseThrow();
        });
    }

    public MovimientoFinanciero anular(long id, String motivo) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        String motivoLimpio = Validaciones.requerido(motivo, "Motivo de la anulación", 5, 200);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            MovimientoFinanciero actual = exigirMovimiento(c, id, empresa.id());
            if (actual.anulado()) {
                throw new ValidacionException("El movimiento ya está anulado.");
            }
            exigirNoGeneradoPorPago(actual);
            movimientos.anular(c, id, motivoLimpio, usuario, ahora);
            return movimientos.buscarPorId(c, id).orElseThrow();
        });
    }

    /**
     * Copia al mes de destino los gastos fijos registrados en el mes de origen, conservando el
     * día (ajustado si el mes de destino es más corto). Omite los que ya existen en el destino
     * con la misma categoría, descripción y monto, para no duplicarlos si se repite la acción.
     *
     * @return cantidad de gastos copiados
     */
    public int copiarGastosFijos(YearMonth origen, YearMonth destino) {
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(destino, "destino");
        if (!destino.isAfter(origen)) {
            throw new ValidacionException("El mes de destino debe ser posterior al de origen.");
        }
        Empresa empresa = empresas.empresaDeTrabajo();
        LocalDate hoy = LocalDate.now(reloj);
        if (destino.isAfter(YearMonth.from(hoy))) {
            throw new ValidacionException("Solo se pueden copiar gastos hasta el mes en curso.");
        }
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        return database.enTransaccion(c -> {
            List<MovimientoFinanciero> fijos = movimientos.listar(c, empresa.id(), origen.atDay(1),
                            origen.atEndOfMonth(), false).stream()
                    .filter(m -> m.tipo() == TipoMovimiento.GASTO && m.comportamiento() == Comportamiento.FIJO
                            && !m.dePagoDeuda())
                    .toList();
            List<MovimientoFinanciero> existentes = movimientos.listar(c, empresa.id(), destino.atDay(1),
                    destino.atEndOfMonth(), false);
            int copiados = 0;
            for (MovimientoFinanciero m : fijos) {
                boolean yaExiste = existentes.stream().anyMatch(e -> e.categoria() == m.categoria()
                        && e.descripcion().equalsIgnoreCase(m.descripcion())
                        && e.monto().compareTo(m.monto()) == 0);
                if (yaExiste) {
                    continue;
                }
                LocalDate fecha = destino.atDay(Math.min(m.fecha().getDayOfMonth(), destino.lengthOfMonth()));
                Long cuenta = m.cuentaId() != null && cuentaActiva(c, empresa.id(), m.cuentaId()) ? m.cuentaId() : null;
                movimientos.insertar(c, empresa.id(), new DatosMovimiento(m.tipo(), m.categoria(),
                        m.comportamiento(), fecha, m.descripcion(), m.monto(), null,
                        cuentas.resolver(c, empresa.id(), cuenta, null)), usuario, ahora);
                copiados++;
            }
            return copiados;
        });
    }

    private boolean cuentaActiva(Connection c, long empresaId, long cuentaId) throws SQLException {
        try {
            cuentas.exigir(c, empresaId, cuentaId, null);
            return true;
        } catch (ValidacionException e) {
            return false;
        }
    }

    private MovimientoFinanciero exigirMovimiento(Connection c, long id, long empresaId) throws SQLException {
        return movimientos.buscarPorId(c, id)
                .filter(m -> m.empresaId() == empresaId)
                .orElseThrow(() -> new ValidacionException("El movimiento no existe en la empresa activa."));
    }

    private static void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ValidacionException("Indica las fechas del período.");
        }
        if (desde.isAfter(hasta)) {
            throw new ValidacionException("La fecha inicial no puede ser posterior a la final.");
        }
    }
}
