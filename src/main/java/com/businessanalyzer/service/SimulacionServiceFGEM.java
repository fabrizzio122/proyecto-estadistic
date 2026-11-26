package com.businessanalyzer.service;

import com.businessanalyzer.analytics.PeriodoFGEM;
import com.businessanalyzer.analytics.ResumenFinancieroFGEM;
import com.businessanalyzer.analytics.ResumenOperacionesFGEM;
import com.businessanalyzer.analytics.simulacion.BaseSimulacionFGEM;
import com.businessanalyzer.analytics.simulacion.SupuestosFGEM;
import com.businessanalyzer.analytics.simulacion.VariableFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EscenarioFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.repository.EscenarioRepositoryFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public class SimulacionServiceFGEM {

    public enum TipoBaseFGEM {
        ULTIMO_MES("Último mes completo", 1),
        PROMEDIO_3_MESES("Promedio de los últimos 3 meses", 3),
        PROMEDIO_6_MESES("Promedio de los últimos 6 meses", 6);

        private final String descripcionFGEM;
        private final int mesesFGEM;

        TipoBaseFGEM(String descripcionFGEM, int mesesFGEM) {
            this.descripcionFGEM = descripcionFGEM;
            this.mesesFGEM = mesesFGEM;
        }

        public String descripcionFGEM() {
            return descripcionFGEM;
        }

        public int mesesFGEM() {
            return mesesFGEM;
        }

        @Override
        public String toString() {
            return descripcionFGEM;
        }
    }

    private static final BigDecimal CAMBIO_MINIMO_FGEM = new BigDecimal("-1");
    private static final BigDecimal CAMBIO_MAXIMO_FGEM = new BigDecimal("5");
    private static final BigDecimal MONTO_MAXIMO_FGEM = new BigDecimal("999999999");

    private final DatabaseFGEM databaseFGEM;
    private final OperacionRepositoryFGEM operacionesFGEM;
    private final MovimientoRepositoryFGEM movimientosFGEM;
    private final EscenarioRepositoryFGEM escenariosFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public SimulacionServiceFGEM(DatabaseFGEM databaseFGEM, OperacionRepositoryFGEM operacionesFGEM, MovimientoRepositoryFGEM movimientosFGEM,
                             EscenarioRepositoryFGEM escenariosFGEM, EmpresaServiceFGEM empresasFGEM, SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.operacionesFGEM = operacionesFGEM;
        this.movimientosFGEM = movimientosFGEM;
        this.escenariosFGEM = escenariosFGEM;
        this.empresasFGEM = empresasFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    public BaseSimulacionFGEM baseFGEM(TipoBaseFGEM tipoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.ANALISIS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        YearMonth ultimoFGEM = YearMonth.from(LocalDate.now(relojFGEM)).minusMonths(1);
        PeriodoFGEM periodoFGEM = new PeriodoFGEM(ultimoFGEM.minusMonths(tipoFGEM.mesesFGEM() - 1L).atDay(1), ultimoFGEM.atEndOfMonth());
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            ResumenOperacionesFGEM opsFGEM = operacionesFGEM.resumenFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM.desdeFGEM(), periodoFGEM.hastaFGEM());
            ResumenFinancieroFGEM finFGEM = ResumenFinancieroFGEM.deFGEM(
                    movimientosFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), periodoFGEM.desdeFGEM(), periodoFGEM.hastaFGEM(), false));
            return BaseSimulacionFGEM.deFGEM(periodoFGEM, tipoFGEM.mesesFGEM(), opsFGEM, finFGEM);
        });
    }

    public List<EscenarioFGEM> listarEscenariosFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.ANALISIS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> escenariosFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM()));
    }

    public EscenarioFGEM guardarFGEM(String nombreFGEM, String descripcionFGEM, SupuestosFGEM supuestosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.ANALISIS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        String nombreLimpioFGEM = ValidacionesFGEM.requeridoFGEM(nombreFGEM, "Nombre del escenario", 2, 60);
        String descripcionLimpiaFGEM = ValidacionesFGEM.opcionalFGEM(descripcionFGEM, "Descripción", 200);
        validarFGEM(supuestosFGEM);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            Optional<EscenarioFGEM> existenteFGEM = escenariosFGEM.buscarPorNombreFGEM(cFGEM, empresaFGEM.idFGEM(), nombreLimpioFGEM);
            long idFGEM;
            if (existenteFGEM.isPresent()) {
                idFGEM = existenteFGEM.get().idFGEM();
                escenariosFGEM.actualizarFGEM(cFGEM, idFGEM, nombreLimpioFGEM, descripcionLimpiaFGEM, supuestosFGEM, usuarioFGEM, ahoraFGEM);
            } else {
                idFGEM = escenariosFGEM.insertarFGEM(cFGEM, empresaFGEM.idFGEM(), nombreLimpioFGEM, descripcionLimpiaFGEM, supuestosFGEM, usuarioFGEM, ahoraFGEM);
            }
            return escenariosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public void eliminarFGEM(long idFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.ANALISIS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        databaseFGEM.enTransaccionFGEM(cFGEM -> {
            escenariosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).filter(eFGEM -> eFGEM.empresaIdFGEM() == empresaFGEM.idFGEM())
                    .orElseThrow(() -> new ValidacionExceptionFGEM("El escenario no existe en la empresa activa."));
            escenariosFGEM.eliminarFGEM(cFGEM, idFGEM);
            return null;
        });
    }

    public static void validarFGEM(SupuestosFGEM sFGEM) {
        if (sFGEM == null) {
            throw new ValidacionExceptionFGEM("Faltan los supuestos del escenario.");
        }
        for (VariableFGEM variableFGEM : VariableFGEM.values()) {
            BigDecimal valorFGEM = sFGEM.valorFGEM(variableFGEM);
            if (valorFGEM.compareTo(CAMBIO_MINIMO_FGEM) < 0 || valorFGEM.compareTo(CAMBIO_MAXIMO_FGEM) > 0) {
                throw new ValidacionExceptionFGEM("El cambio en \"" + variableFGEM.descripcionFGEM() + "\" debe estar entre −100 % y +500 %.");
            }
        }
        if (sFGEM.costoFijoAdicionalFGEM().abs().compareTo(MONTO_MAXIMO_FGEM) > 0) {
            throw new ValidacionExceptionFGEM("El costo fijo adicional supera el máximo permitido.");
        }
        if (sFGEM.tasaImpuestoFGEM().signum() < 0 || sFGEM.tasaImpuestoFGEM().compareTo(BigDecimal.ONE) > 0) {
            throw new ValidacionExceptionFGEM("La tasa de impuesto debe estar entre 0 % y 100 %.");
        }
    }
}
