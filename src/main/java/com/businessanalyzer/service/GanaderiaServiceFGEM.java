package com.businessanalyzer.service;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.ganaderia.EvolucionHatoFGEM;
import com.businessanalyzer.analytics.ganaderia.ExistenciaValorizadaFGEM;
import com.businessanalyzer.analytics.ganaderia.HatoFGEM;
import com.businessanalyzer.analytics.ganaderia.IndicadoresEspecieFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CategoriaGanadoFGEM;
import com.businessanalyzer.model.DatosMovimientoGanadoFGEM;
import com.businessanalyzer.model.DatosProduccionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EspecieFGEM;
import com.businessanalyzer.model.MovimientoGanadoFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.ProduccionGanaderaFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.repository.GanaderiaRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Ganadería de la empresa activa (solo empresas del sector agropecuario): existencias del hato,
 * movimientos, producción de leche, fibra y lana, precios por cabeza e indicadores técnicos.
 * <p>
 * Las existencias nunca pueden quedar negativas: un movimiento que deje sin animales a una
 * categoría en alguna fecha se rechaza. Nada se elimina; los registros se anulan.
 */
public class GanaderiaServiceFGEM {

    /** Fecha lejana para leer todos los movimientos, incluidos los posteriores a una fecha dada. */
    private static final LocalDate SIN_LIMITE_FGEM = LocalDate.of(9999, 12, 31);

    private final DatabaseFGEM databaseFGEM;
    private final GanaderiaRepositoryFGEM repositorioFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public GanaderiaServiceFGEM(DatabaseFGEM databaseFGEM, GanaderiaRepositoryFGEM repositorioFGEM, EmpresaServiceFGEM empresasFGEM,
                            SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.repositorioFGEM = repositorioFGEM;
        this.empresasFGEM = empresasFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    // ------------------------------------------------------------------ consultas

    /** Existencias de hoy por categoría, con su precio y valor. */
    public List<ExistenciaValorizadaFGEM> hatoFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_VER);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        LocalDate hoyFGEM = LocalDate.now(relojFGEM);
        return databaseFGEM.conConexionFGEM(cFGEM -> ExistenciaValorizadaFGEM.deFGEM(
                HatoFGEM.existenciasFGEM(repositorioFGEM.listarMovimientosFGEM(cFGEM, empresaFGEM.idFGEM(), hoyFGEM, false), hoyFGEM),
                repositorioFGEM.preciosFGEM(cFGEM, empresaFGEM.idFGEM())));
    }

    /** Movimientos del período, del más reciente al más antiguo. */
    public List<MovimientoGanadoFGEM> listarMovimientosFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM, boolean incluirAnuladosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_VER);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        PeriodoFGEM periodoFGEM = new PeriodoFGEM(desdeFGEM, hastaFGEM);
        return databaseFGEM.conConexionFGEM(cFGEM -> repositorioFGEM.listarMovimientosFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM.hastaFGEM(), incluirAnuladosFGEM))
                .stream()
                .filter(mFGEM -> !mFGEM.fechaFGEM().isBefore(periodoFGEM.desdeFGEM()))
                .sorted(Comparator.comparing(MovimientoGanadoFGEM::fechaFGEM).thenComparingLong(MovimientoGanadoFGEM::idFGEM).reversed())
                .toList();
    }

    public List<ProduccionGanaderaFGEM> listarProduccionFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM, boolean incluirAnuladosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_VER);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        PeriodoFGEM periodoFGEM = new PeriodoFGEM(desdeFGEM, hastaFGEM);
        return databaseFGEM.conConexionFGEM(cFGEM -> repositorioFGEM.listarProduccionFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM.desdeFGEM(),
                periodoFGEM.hastaFGEM(), incluirAnuladosFGEM));
    }

    public Map<CategoriaGanadoFGEM, BigDecimal> preciosFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_VER);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> repositorioFGEM.preciosFGEM(cFGEM, empresaFGEM.idFGEM()));
    }

    /** Indicadores del período para cada especie con animales o movimientos. */
    public List<IndicadoresEspecieFGEM> indicadoresFGEM(PeriodoFGEM periodoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_VER);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            List<MovimientoGanadoFGEM> movimientosFGEM = repositorioFGEM.listarMovimientosFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM.hastaFGEM(), false);
            List<ProduccionGanaderaFGEM> produccionFGEM = repositorioFGEM.listarProduccionFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM.desdeFGEM(),
                    periodoFGEM.hastaFGEM(), false);
            List<IndicadoresEspecieFGEM> resultadoFGEM = new ArrayList<>();
            for (EspecieFGEM especieFGEM : EspecieFGEM.values()) {
                IndicadoresEspecieFGEM iFGEM = IndicadoresEspecieFGEM.calcularFGEM(especieFGEM, periodoFGEM, movimientosFGEM, produccionFGEM);
                if (iFGEM.tieneDatosFGEM()) {
                    resultadoFGEM.add(iFGEM);
                }
            }
            return resultadoFGEM;
        });
    }

    /** Evolución mensual del hato y de la producción entre los meses indicados (máximo 5 años). */
    public EvolucionHatoFGEM evolucionFGEM(YearMonth desdeFGEM, YearMonth hastaFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_VER);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        if (desdeFGEM == null || hastaFGEM == null || desdeFGEM.isAfter(hastaFGEM) || desdeFGEM.plusYears(5).isBefore(hastaFGEM)) {
            throw new ValidacionExceptionFGEM("Elige un rango de meses válido (máximo 5 años).");
        }
        return databaseFGEM.conConexionFGEM(cFGEM -> EvolucionHatoFGEM.calcularFGEM(desdeFGEM, hastaFGEM,
                repositorioFGEM.listarMovimientosFGEM(cFGEM, empresaFGEM.idFGEM(), hastaFGEM.atEndOfMonth(), false),
                repositorioFGEM.listarProduccionFGEM(cFGEM, empresaFGEM.idFGEM(), desdeFGEM.atDay(1), hastaFGEM.atEndOfMonth(), false)));
    }

    // ------------------------------------------------------------------ registro

    public MovimientoGanadoFGEM registrarMovimientoFGEM(DatosMovimientoGanadoFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        DatosMovimientoGanadoFGEM dFGEM = ValidadorGanaderiaFGEM.movimientoFGEM(datosFGEM, LocalDate.now(relojFGEM));
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            long idFGEM = repositorioFGEM.insertarMovimientoFGEM(cFGEM, empresaFGEM.idFGEM(), dFGEM, usuarioFGEM, ahoraFGEM);
            exigirExistenciasFGEM(cFGEM, empresaFGEM.idFGEM());
            return repositorioFGEM.buscarMovimientoFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    /** Anula un movimiento registrado por error. Se rechaza si dejaría categorías con existencias negativas. */
    public MovimientoGanadoFGEM anularMovimientoFGEM(long idFGEM, String motivoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        String motivoLimpioFGEM = ValidacionesFGEM.requeridoFGEM(motivoFGEM, "Motivo de la anulación", 5, 200);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            MovimientoGanadoFGEM actualFGEM = repositorioFGEM.buscarMovimientoFGEM(cFGEM, idFGEM)
                    .filter(mFGEM -> mFGEM.empresaIdFGEM() == empresaFGEM.idFGEM())
                    .orElseThrow(() -> new ValidacionExceptionFGEM("El movimiento no existe en la empresa activa."));
            if (actualFGEM.anuladoFGEM()) {
                throw new ValidacionExceptionFGEM("El movimiento ya está anulado.");
            }
            repositorioFGEM.anularMovimientoFGEM(cFGEM, idFGEM, motivoLimpioFGEM, usuarioFGEM, ahoraFGEM);
            exigirExistenciasFGEM(cFGEM, empresaFGEM.idFGEM());
            return repositorioFGEM.buscarMovimientoFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    /**
     * Corrige un movimiento: el original queda anulado (con nota) y se registra el corregido en la misma
     * transacción. Se rechaza si el resultado deja alguna categoría con existencias negativas.
     */
    public MovimientoGanadoFGEM corregirMovimientoFGEM(long idFGEM, DatosMovimientoGanadoFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        DatosMovimientoGanadoFGEM dFGEM = ValidadorGanaderiaFGEM.movimientoFGEM(datosFGEM, LocalDate.now(relojFGEM));
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            MovimientoGanadoFGEM actualFGEM = repositorioFGEM.buscarMovimientoFGEM(cFGEM, idFGEM)
                    .filter(mFGEM -> mFGEM.empresaIdFGEM() == empresaFGEM.idFGEM() && !mFGEM.anuladoFGEM())
                    .orElseThrow(() -> new ValidacionExceptionFGEM("El movimiento no existe o está anulado. Actualiza la lista."));
            repositorioFGEM.anularMovimientoFGEM(cFGEM, actualFGEM.idFGEM(), "Corregido por el usuario", usuarioFGEM, ahoraFGEM);
            long nuevoFGEM = repositorioFGEM.insertarMovimientoFGEM(cFGEM, empresaFGEM.idFGEM(), dFGEM, usuarioFGEM, ahoraFGEM);
            exigirExistenciasFGEM(cFGEM, empresaFGEM.idFGEM());
            return repositorioFGEM.buscarMovimientoFGEM(cFGEM, nuevoFGEM).orElseThrow();
        });
    }

    /** Corrige un registro de producción (el original queda anulado). */
    public ProduccionGanaderaFGEM corregirProduccionFGEM(long idFGEM, DatosProduccionFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        DatosProduccionFGEM dFGEM = ValidadorGanaderiaFGEM.produccionFGEM(datosFGEM, LocalDate.now(relojFGEM));
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            ProduccionGanaderaFGEM actualFGEM = repositorioFGEM.buscarProduccionFGEM(cFGEM, idFGEM)
                    .filter(pFGEM -> pFGEM.empresaIdFGEM() == empresaFGEM.idFGEM() && !pFGEM.anuladoFGEM())
                    .orElseThrow(() -> new ValidacionExceptionFGEM("El registro no existe o está anulado. Actualiza la lista."));
            repositorioFGEM.anularProduccionFGEM(cFGEM, actualFGEM.idFGEM(), usuarioFGEM, ahoraFGEM);
            long nuevoFGEM = repositorioFGEM.insertarProduccionFGEM(cFGEM, empresaFGEM.idFGEM(), dFGEM, usuarioFGEM, ahoraFGEM);
            return repositorioFGEM.buscarProduccionFGEM(cFGEM, nuevoFGEM).orElseThrow();
        });
    }

    public ProduccionGanaderaFGEM registrarProduccionFGEM(DatosProduccionFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        DatosProduccionFGEM dFGEM = ValidadorGanaderiaFGEM.produccionFGEM(datosFGEM, LocalDate.now(relojFGEM));
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            long idFGEM = repositorioFGEM.insertarProduccionFGEM(cFGEM, empresaFGEM.idFGEM(), dFGEM, usuarioFGEM, ahoraFGEM);
            return repositorioFGEM.buscarProduccionFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public ProduccionGanaderaFGEM anularProduccionFGEM(long idFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            ProduccionGanaderaFGEM actualFGEM = repositorioFGEM.buscarProduccionFGEM(cFGEM, idFGEM)
                    .filter(pFGEM -> pFGEM.empresaIdFGEM() == empresaFGEM.idFGEM())
                    .orElseThrow(() -> new ValidacionExceptionFGEM("El registro no existe en la empresa activa."));
            if (actualFGEM.anuladoFGEM()) {
                throw new ValidacionExceptionFGEM("El registro ya está anulado.");
            }
            repositorioFGEM.anularProduccionFGEM(cFGEM, idFGEM, usuarioFGEM, ahoraFGEM);
            return repositorioFGEM.buscarProduccionFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    /** Guarda los precios por cabeza; un precio {@code null} borra el de esa categoría. */
    public void guardarPreciosFGEM(Map<CategoriaGanadoFGEM, BigDecimal> preciosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        preciosFGEM.values().forEach(ValidadorGanaderiaFGEM::precioFGEM);
        databaseFGEM.enTransaccionFGEM(cFGEM -> {
            for (Map.Entry<CategoriaGanadoFGEM, BigDecimal> eFGEM : preciosFGEM.entrySet()) {
                repositorioFGEM.guardarPrecioFGEM(cFGEM, empresaFGEM.idFGEM(), eFGEM.getKey(), eFGEM.getValue(), usuarioFGEM, ahoraFGEM);
            }
            return null;
        });
    }

    // ------------------------------------------------------------------ utilidades

    /** La empresa activa, siempre que sea del sector agropecuario. */
    private EmpresaFGEM empresaAgropecuariaFGEM() {
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        if (empresaFGEM.sectorFGEM() != SectorFGEM.AGROPECUARIO) {
            throw new ValidacionExceptionFGEM("La ganadería solo está disponible para empresas del sector agropecuario. "
                    + "Cambia el sector en Empresas → Editar.");
        }
        return empresaFGEM;
    }

    /** Rechaza la operación (y deshace la transacción) si alguna categoría queda con existencias negativas. */
    private void exigirExistenciasFGEM(Connection cFGEM, long empresaIdFGEM) throws SQLException {
        HatoFGEM.primerFaltanteFGEM(repositorioFGEM.listarMovimientosFGEM(cFGEM, empresaIdFGEM, SIN_LIMITE_FGEM, false)).ifPresent(fFGEM -> {
            throw new ValidacionExceptionFGEM("No alcanzan los animales: el " + com.businessanalyzer.util.FechasFGEM.paraMostrarFGEM(fFGEM.fechaFGEM())
                    + " faltarían " + fFGEM.cantidadFGEM() + " en «" + fFGEM.categoriaFGEM().nombreCompletoFGEM() + "». Registra primero "
                    + "el inventario inicial, los nacimientos o las compras de esa categoría.");
        });
    }
}
